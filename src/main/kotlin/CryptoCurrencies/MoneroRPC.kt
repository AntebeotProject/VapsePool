package org.antibiotic.pool.main.CryptoCurrencies

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.antibiotic.pool.main.JSONRPC
import org.antibiotic.pool.main.PoolServer.RPC
import org.antibiotic.pool.main.PoolServer.defTXFee
import org.antibiotic.pool.main.PoolServer.deleteSquares
import org.litote.kmongo.json
import java.math.BigDecimal
import java.math.BigInteger

class MoneroRPC : JSONRPC.worker {
    /*{"account_index":0,
        "balance":0,
        "base_address":"9thnrw5xBDna78fWh4d7tzSiFrcSqfjnrRprQXohLm6Af3Bxtk7eUHpHgLspAhUtnjG9CivVCku25NHT8KZibWwtEWxuXck",
        "label":"Primary account",
        "tag":"",
        "unlocked_balance":0}
    */
    @Serializable
    data class subaddress_account(val account_index: Int, val balance: String, val base_address: String, val label: String, val tag: String, val unlocked_balance: String)
    constructor(
        port: Int,
        l: String,
        p: String,
        txFee: Double = defTXFee,
        host: String = "http://127.0.0.1"
    ) : super("$host:$port/json_rpc", l, p, isElectrum = false, isMonero = true)

    fun get_accounts(): List<subaddress_account>? {
        val tmp = this.doCall("get_accounts").jsonObject.toMap()["result"]?.jsonObject?.toMap()?.get("subaddress_accounts")
        if (tmp == null) return null
        return Json{isLenient = true}.decodeFromJsonElement<List<subaddress_account>>(tmp!!)
    }
    override fun getaddressbalance(adr: String): JsonElement? {
        val tmp = getInfoAboutAddress(adr)
        if (tmp == null) return  null
        return Json{isLenient = true}.encodeToJsonElement(tmp.balance)
    }
    fun getInfoAboutAddress(adr: String): subaddress_account?
    {
        val accounts = get_accounts()
        if (accounts == null) return  null
        for (account in accounts)
        {
            // println(account)
            if (account.base_address.equals(adr)) return account
        }
        return null
    }
    override fun getbalance(): JsonElement? {
        return get_balance(0).jsonObject.toMap()["result"]?.jsonObject?.toMap()?.get("balance")
    }
    override fun sendMoney(outAddr: String, cMoney: BigDecimal, optionalString: String ): JsonElement {
       if (false) {
           val r = this as RPC
           r.listreceivedbyaddress().forEach() {
               if (outAddr == it.address) ; // return buildJsonObject { }
           }
       } // TODO()
        return transfer(outAddr, ammount = cMoney)
    }

    fun refresh() = this.doCall("refresh")
    fun auto_refresh_enable() = this.doCall("auto_refresh")

    fun get_transfers(adr: String, income: Boolean = true): JsonElement?
    {
        val t = getInfoAboutAddress(adr)
        //println("found address $t")
        val ret = this.doCall("get_transfers", buildJsonObject{put("in",income);put("account_index", t!!.account_index)})?.jsonObject?.toMap()
            ?.get("result")
        return ret
    }
    fun get_balance(idx: Int) = this.doCall("get_balance", buildJsonObject { put("account_index ", JsonPrimitive(idx) ) })
    override fun createnewaddress( ) = this.doCall("create_account")
    val atomic_unit = 0.000000000001.toBigDecimal()
    fun toAtomic(c: BigDecimal): BigInteger
    {
        val ret = (c / atomic_unit).toBigInteger()
        //println("toAtomic $c = $ret")
        return ret
    }
    fun fromAtomic(c: BigDecimal): BigDecimal
    {
        val ret = c * atomic_unit
        //println("fromAtomic $c = $ret")
        return ret
    }
    fun validate_address(adr: String): Boolean
    {
        val map = this.doCall("validate_address", buildJsonObject { put("address", adr) })!!.jsonObject!!.toMap()["result"]!!.jsonObject
        //println(map)
        val ret =  map["valid"].toString().toBoolean() || map["integrated"].toString().toBoolean() || map["subaddress"].toString().toBoolean() || map["openalias_address"].toString().toBoolean()
        //println(ret)
        return ret
    }
    fun relay_tx(tx: String): JsonElement {
        val params = buildJsonObject { put("hex", tx) }
        // val params_ = buildJsonObject { put("params", params) }
        return this.doCall("relay_tx", params)
    }
    fun make_integrated_address( ) = this.doCall("make_integrated_address", params = buildJsonObject {  })
    fun sweep_all(idx: Int = 1, subaddr_indices_all: Boolean = false, ring_size: Int = 10, unlock_time: Int = 0): JsonElement {
        val bAdr = this.get_accounts()!![0].base_address
        val params = buildJsonObject { put("address", bAdr); put("account_index", idx);
            put("ring_size", ring_size);put("unlock_time", unlock_time); put("subaddr_indices_all", subaddr_indices_all)}
        return this.doCall("sweep_all", params)
    }
    fun getAllIndicesJSon() = (0..(this.get_accounts()?.size ?: 0)).toMutableList().toIntArray().json
    fun sweep_all(): JsonElement {
        //val count = this.get_accounts()?.size ?: 0
        //for(i in 0..count) this.sweep_all(i)
        //println("sweep")
        val bAdr = this.get_accounts()!![0].base_address
        val indices = getAllIndicesJSon()
        val params = buildJsonObject { put("address", bAdr); put("subaddr_indices ", indices) }
        println(params)
        return this.doCall("sweep_all", params)
    }
    fun transfer(dest: String, ammount: BigDecimal, do_not_relay: Boolean = false): JsonElement
    {
        /*
            if you got a bug with send money. You can to do this things:
            "monero-wallet-cli.exe" --restore-deterministic-wallet --daemon-address  http://monerujods7mbghwe6cobdr6ujih6c22zu5rl7zshmizz2udf7v7fsad.onion:18081  --proxy 127.0.0.1:9050 --trusted-daemon --daemon-ssl-allow-any-cert  --use-english-language-names
            after when you restore by your seed phrase.
            you can get this though getSeed().
            (is write on every run)
            so. you can to use gui instead.
            please stand by...
            and just send all your money to your primary address.
         */
        //println(getSeed())
        println(this.getbalance())
        //println("our balance: ${this.get_balance(0)}")
        val ammount_big = this.toAtomic(ammount)
        println("amount is $ammount_big")
        val destinations = buildJsonArray { add(buildJsonObject { put("amount", ammount_big); put("address", dest) }) }
        val params = buildJsonObject { put("destinations", destinations); put("do_not_relay", do_not_relay); put("get_tx_metadata", do_not_relay);
            put("subaddr_indices", getAllIndicesJSon()); put("subaddr_indices_all", true)}
        println(params)
        return this.doCall("transfer", params)
    }
    fun getSeed(): JsonElement{
        return this.doCall("query_key", buildJsonObject { put("key_type", "mnemonic") })
    }
}
