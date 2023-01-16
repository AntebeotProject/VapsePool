package ru.xmagi.pool.main.CryptoCurrencies

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import ru.xmagi.pool.main.JSONRPC
import ru.xmagi.pool.main.PoolServer.*
import java.io.File
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
           println("init cryptocurrecies")
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
                    } // @when
                }// @if
            } // @for
        }// @fun
    }
    // private data class electrumtx(val hash: String, val height: Int, val fee: Int)
    object CheckerOfInputTransacations {
        fun runUpdaterOfTransactions() {
            thread {
                while(true) {
                    // println("Thread for update input transactions")
                    for(c in coins) {
                        val coinname = c.key
                        // println("Coinname $coinname")
                        val balances = DB.getBalancesByCoinName(coinname)
                        for (balance in balances) {
                            // println("balance: $balance")
                            // var txids = mutableListOf<String>()
                            if(balance.inputAddress.isEmpty() || balance.inputAddress == null) continue
                            if (c.value.getisElectrum()) {
                                val rpc = c.value as ElectrumRPC
                                // println(balance.inputAddress)
                                val r = rpc.getaddresshistory(balance.inputAddress).jsonObject.toMap()["result"]
                                val history = rpc.onchain_history().jsonObject.toMap()["result"]?.jsonObject?.toMap()?.get("transactions")
                                if (r == null || history == null) continue
                                for(tx in r.jsonArray) {
                                    val m = tx.jsonObject.toMap()
                                    val tx_hash = m["tx_hash"].toString().deleteSquares()
                                    // val height = m["height"].toString().toInt()
                                    // val fee = m["fee"].toString().toInt()
                                    // electrumtx(tx_hash, height, fee)
                                    // txids.add(tx_hash)
                                    for(h in history.jsonArray) {
                                        val txid = h.jsonObject.toMap()["txid"].toString().deleteSquares()
                                        // println("$tx_hash test equal $txid")
                                        if (!txid.equals(tx_hash)) continue
                                        val isIncoming = h.jsonObject.toMap()["incoming"].toString().deleteSquares().toBoolean()
                                        val bc_value = h.jsonObject.toMap()["bc_value"].toString().deleteSquares().toBigDecimal()
                                        val confirmations = (rpc.get_tx_status(txid))
                                        // wDebug("Found tx of addr: $tx_hash is incoming $isIncoming val: $bc_value confirmations $confirmations")
                                        // if (isIncoming) println(DB.getTX(tx_hash))
                                        if (confirmations > 1 && isIncoming && DB.getTX(tx_hash) == null) {
                                            wDebug("Add it tx ${balance.owner}, ${coinname}")
                                                DB.addTX(DB.tx(balance.owner, coinname, tx_hash))
                                                DB.addToBalance(balance.owner, bc_value, coinname)
                                        }
                                    } // todo:
                                }
                            } else {
                                val rpc = c.value as RPC
                                val data = rpc.listreceivedbyaddress()
                                // println("txs: $txs")
                                for(d in data) {
                                    if (!d.address.equals(balance.inputAddress)) continue
                                    // txids.addAll(tx.txids)
                                    for(tx in d.txids) {
                                        val confirmations = rpc.getConfirmationsOfTX(tx)
                                        val amount = rpc.getTransaction(tx)?.jsonObject?.toMap()?.get("amount").toString().toBigDecimal()
                                        if (confirmations > 1 && amount > 0.toBigDecimal() && DB.getTX(tx) == null) {
                                            wDebug("Add not electrum tx $tx ${balance.owner}, ${coinname}")
                                            DB.addTX(DB.tx(balance.owner, coinname, tx))
                                            DB.addToBalance(balance.owner, amount, coinname)
                                        }
                                    }
                                }
                            }
                            // txids
                        }
                    }
                    Thread.sleep(30000)
                }
            }
        }
    }
}
