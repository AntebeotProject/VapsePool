package org.antibiotic.pool.main.CryptoCurrencies

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.antibiotic.pool.main.JSONRPC
import org.antibiotic.pool.main.PoolServer.defTXFee
import org.antibiotic.pool.main.PoolServer.deleteSquares
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
    fun transfer(dest: String, ammount: BigDecimal, do_not_relay: Boolean = false): JsonElement
    {
        //println("our balance: ${this.get_balance(0)}")
        val ammount_big = this.toAtomic(ammount)
        val destinations = buildJsonArray { add(buildJsonObject { put("amount", ammount_big); put("address", dest) }) }
        return this.doCall("transfer", buildJsonObject { put("destinations", destinations); put("do_not_relay", do_not_relay); put("get_tx_metadata", do_not_relay) })
    }
}
