package org.antibiotic.pool.main.CryptoCurrencies

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.antibiotic.pool.main.DB.DB
import org.antibiotic.pool.main.DB.defUserLanguage
import org.antibiotic.pool.main.DB.tx
import org.antibiotic.pool.main.DB.userLanguage
import org.antibiotic.pool.main.JSONRPC
import org.antibiotic.pool.main.PoolServer.*
import org.antibiotic.pool.main.WebSite.JettyServer
import org.antibiotic.pool.main.i18n.i18n
import org.antibiotic.pool.main.telegabotEs
import java.io.File
import java.math.BigDecimal
import java.util.*
import kotlin.concurrent.thread

enum class mode(val mode: String) {
    electrum("electrum"){
        override fun init(user: String, pass: String, txFee: Double, host: String, port: Int): JSONRPC.worker {
            return ElectrumRPC(port, user, pass)
      }
    },
    bitcoin("bitcoin") {
        override fun init(user: String, pass: String, txFee: Double, host: String, port: Int): JSONRPC.worker {
            return RPC(host, user, pass, txFee)
        }
    },
    monero("monero") {
        override fun init(user: String, pass: String, txFee: Double, host: String, port: Int): JSONRPC.worker {
            return MoneroRPC(port, user, pass)
        }
    };
    abstract fun init(user: String, pass: String, txFee: Double = 0.01, host: String = "http://127.0.0.1", port: Int = 0): JSONRPC.worker
    companion object {
        public fun modeExists(w: String): Boolean {
            for (m in values()) {

                if (m.mode.equals(w)) return true
            }
            return false
        }
    }
}
val defMode = mode.bitcoin
class CryptoCoins {
    companion object {
        private fun wDebug(w: String) = println("[WARNING] $w")
        val coins = mutableMapOf<String,JSONRPC.worker>()
        fun initCoins(refresh: Boolean = false) {
           // println("init cryptocurrecies")
           if(coins.size > 0 && !refresh ) return
           else if (coins.size > 0 && refresh) coins.clear()

           val dir = File(".${File.separator}CryptoCoins").walk()
            for(f in dir) {
                if (f.name.endsWith(".config")) {
                    val tPropetries = Properties()
                    tPropetries.load(f.inputStream())
                    val mode_ = tPropetries.getOrDefault("mode", "electrum").toString()
                    val cMode = if( !mode.modeExists(mode_) ) mode.valueOf("bitcoin") else mode.valueOf(mode_)
                    val cUser = tPropetries.getProperty("user").toString()
                    val cPass = tPropetries.getProperty("pass").toString()
                    val cTXFee = tPropetries.getOrDefault("paytxfee", defTXFee).toString().toDouble()
                    when (cMode.mode) {
                        "bitcoin" -> {
                            val host = tPropetries.getProperty("host")
                            val rpc = cMode.init(cUser, cPass, host=host, txFee = cTXFee)
                            coins.put(f.name.split(".")[0], rpc)
                        }
                        "electrum" -> {
                            val cPort = tPropetries.getProperty("port").toInt()
                            val rpc = cMode.init(cUser, cPass, port = cPort)
                            coins.put(f.name.split(".")[0], rpc)
                        }
                        "monero" -> {
                            // println("is monero. $cMode")
                            val cPort = tPropetries.getProperty("port").toInt()
                            val rpc = cMode.init(cUser, cPass, port = cPort)
                            coins.put(f.name.split(".")[0], rpc)
                        }
                    } // @when
                }// @if
            } // @for
        }// @fun
    }
    // private data class electrumtx(val hash: String, val height: Int, val fee: Int)
    object CheckerOfInputTransacations {
        fun addTX(owner: String, cname: String, hash: String)
        {
            // coinname
            synchronized(DB) {

                val uLanguage = JettyServer.Users.language.getLangByUser(owner)

                wDebug("Add it tx ${owner}, ${cname}")
                DB.addTX(tx(owner, cname, hash))
                DB.createNewNotification(owner, String.format(uLanguage.getString("newUnconfirmedTX"), hash, cname))
            }
        }
        fun confirmTX(hash: String, owner: String, cname: String, bc_value: BigDecimal)
        {
            synchronized(DB) {
            val uLanguage = JettyServer.Users.language.getLangByUser(owner)
            val tx = DB.getTX(hash)
                if (tx!!.isConfirmed == false) { // in DB not in network
                    if (cname == "monero") {
                        val monero = CryptoCoins.coins["monero"]!! as MoneroRPC
                        val count = monero.get_accounts()?.size ?: 0
                        for(i in 0..count) monero.swapAll(i) // TODO: from 1 account is better
                    }
                    DB.setTXConfirmed(hash, status = true)
                    wDebug("add balance $owner $bc_value")
                    DB.addToBalance(owner, bc_value, cname)

                    DB.createNewNotification(owner, String.format(uLanguage.getString("balanceChanged"), bc_value, DB.getLoginBalance(owner)?.get(cname)?.balance, cname))
                }
            }
        }
        // fun unconfirmTX() = {}
        fun runUpdaterOfTransactions() {
            thread {
                while(true) {
                    // println("Thread for update input transactions")
                    for (c in coins) {
                    thread {
                        try {
                            val coinname = c.key
                            // println("Coinname $coinname")
                            val balances = DB.getBalancesByCoinName(coinname)
                            for (balance in balances) {
                                // println("balance: $balance")
                                // var txids = mutableListOf<String>()
                                if (balance.inputAddress.isEmpty() || balance.inputAddress == null) continue
                                if (c.value.getisElectrum()) {
                                    val rpc = c.value as ElectrumRPC
                                    // println(balance.inputAddress)
                                    val r = rpc.getaddresshistory(balance.inputAddress).jsonObject.toMap()["result"]
                                    val history =
                                        rpc.onchain_history().jsonObject.toMap()["result"]?.jsonObject?.toMap()
                                            ?.get("transactions")
                                    if (r == null || history == null) continue
                                    for (tx in r.jsonArray) {
                                        val m = tx.jsonObject.toMap()
                                        val tx_hash = m["tx_hash"].toString().deleteSquares()
                                        // val height = m["height"].toString().toInt()
                                        // val fee = m["fee"].toString().toInt()
                                        // electrumtx(tx_hash, height, fee)
                                        // txids.add(tx_hash)
                                        for (h in history.jsonArray) {
                                            val txid = h.jsonObject.toMap()["txid"].toString().deleteSquares()
                                            // println("$tx_hash test equal $txid")
                                            if (!txid.equals(tx_hash)) continue
                                            val isIncoming =
                                                h.jsonObject.toMap()["incoming"].toString().deleteSquares().toBoolean()
                                            val confirmations = (rpc.get_tx_status(txid))
                                            // wDebug("Found tx of addr: $tx_hash is incoming $isIncoming val: $bc_value confirmations $confirmations")
                                            // if (isIncoming) println(DB.getTX(tx_hash))
                                            val _TX = DB.getTX(tx_hash)
                                            if (isIncoming && _TX == null) {
                                                addTX(balance.owner, coinname, tx_hash)
                                            } else if (_TX != null && _TX.isConfirmed == false && confirmations > 0) {
                                                val bc_value =
                                                    h.jsonObject.toMap()["bc_value"].toString().deleteSquares()
                                                        .toBigDecimal()
                                                confirmTX(tx_hash, balance.owner, coinname, bc_value)
                                            }
                                        } // todo:
                                    }
                                } else if (c.value.getisMonero()) {
                                    // println("is monero")
                                    // if is monero
                                    val rpc = c.value as MoneroRPC
                                    // println(rpc.refresh())
                                    // println(rpc.getbalance())
                                    try {
                                        val transfers = rpc.get_transfers(balance.inputAddress)
                                        if (transfers == null) {
                                            throw NullPointerException()
                                        } // some times can be broken
                                        if (transfers != transfers?.jsonObject?.toMap()?.get("in")) {
                                            try {
                                                for (transfer in transfers!!.jsonObject!!.toMap()!!
                                                    .get("in")!!.jsonArray) {
                                                    val obj = transfer.jsonObject.toMap()
                                                    val adr = obj["address"]
                                                    val amount = rpc.fromAtomic(obj["amount"].toString().toBigDecimal())
                                                    val amounts = obj["amounts"]!!.jsonArray
                                                    val confirmations = obj["confirmations"].toString().toInt()
                                                    val txid = obj["txid"].toString()
                                                    // println("$obj")
                                                    // println("$adr")
                                                    // println("$amount")
                                                    // println("$confirmations")
                                                    // println("$txid")
                                                    val _TX = DB.getTX(txid)
                                                    if (amount > BigDecimal.ZERO && confirmations > 0 && _TX != null) {
                                                        confirmTX(txid, balance.owner, balance.CoinName, amount)
                                                    } else if (amount > BigDecimal.ZERO) {
                                                        wDebug("Add monero tx $txid ${balance.owner}, ${coinname}")
                                                        addTX(balance.owner, coinname, txid)
                                                    }
                                                }
                                            } catch (_: NullPointerException) {
                                            }
                                        } else {
                                            System.err.println(balance.inputAddress + " - null transfers. monero")
                                        }
                                        // println(rpc.get_balance(0))
                                        // println(rpc.get_accounts())
                                        // println(rpc.createnewaddress())
                                    } catch (_: NullPointerException) {
                                        c.value.createnewaddress()
                                        //DB.createNewNotification(balance.owner, "INTERNAL ERROR WITH MONERO INPUT SUBADDRESS. PLEASE BE CAREFUL OR CHANGE INPUT ADDRESS. Your address will be found in some a another time. but if you can, then change your input address is more faster for server. But, if u want use only ur input monero adress, just a wait for confirmations. for now RPC server even not found your address by local. is ok some times, it's will found your address later. for now you can ignore it. your balance anyway exists yet. Внутренняя ошибка сервера для вашего адреса, нету возможности проверить входящие транзакции по этому адресу в настоящий момент времени. Если имеется возможность смените входящий адрес или, пожалуйста, ожидайте как адрес восстановится локально системой 'Monero'")
                                        continue
                                        /*
                                        println("$balance")
                                        val uLanguage = i18n(
                                            locale = JettyServer.Users.language.geti18nByLocale(
                                                userLanguage.getForUser(balance.owner)?.language ?: defUserLanguage
                                            )
                                        )
                                        val res = JettyServer.Users.money.genAdr(
                                            coin = balance.CoinName,
                                            uLanguage = uLanguage,
                                            owner = balance.owner
                                        )
                                        DB.createNewNotification(
                                            balance.owner,
                                            String.format(uLanguage.getString("moneroAddressWasChanged"), res)
                                        )
                                        */
                                    }// catch
                                } else { // if is not E wallet RPC (bitcoin)
                                    val rpc = c.value as RPC
                                    val data = rpc.listreceivedbyaddress()
                                    // println("txs: $txs")
                                    for (d in data) {
                                        if (!d.address.equals(balance.inputAddress)) continue
                                        // txids.addAll(tx.txids)
                                        // println("Found tx with the input address")
                                        for (tx in d.txids) {
                                            val confirmations = rpc.getConfirmationsOfTX(tx)
                                            val amount =
                                                rpc.getTransaction(tx)?.jsonObject?.toMap()?.get("amount").toString()
                                                    .toBigDecimal()
                                            val _TX = DB.getTX(tx)
                                            // println("$_TX($tx)($amount) for ${balance.inputAddress} ${balance.owner}")
                                            if (_TX == null && amount > 0.toBigDecimal()) {
                                                wDebug("Add not electrum tx $tx ${balance.owner}, ${coinname}")
                                                addTX(balance.owner, coinname, tx)
                                                // DB.addTX(DB.tx(balance.owner, coinname, tx))
                                            } else if (_TX != null && amount > 0.toBigDecimal() && confirmations > 0) {
                                                confirmTX(tx, balance.owner, coinname, bc_value = amount)
                                            }
                                        } // for TX
                                    }// for data
                                }// end else
                            }// end cycle in balances of DB
                        } catch(e: Exception) { System.err.println(e.toString());  }
                    } // end thread
                }// end Cycle of coins
                    val sTime = Settings.m_propetries.getOrDefault("ThreadSleepForGetInput", 30000).toString().toLongOrNull()?:30000;
                    Thread.sleep(sTime) // 60000 = 1 minute
                }// End a while(true)
            }// thread
        }// fun
    }// object end
}// class end
