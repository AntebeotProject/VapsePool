package org.antibiotic.pool.main.CryptoCurrencies

import kotlinx.serialization.json.*
import org.antibiotic.pool.main.JSONRPC
// import org.antibiotic.pool.main.PoolServer.RPCClient
import org.antibiotic.pool.main.PoolServer.defTXFee
import org.antibiotic.pool.main.PoolServer.deleteSquares
import java.math.BigDecimal

class ElectrumRPC : JSONRPC.worker {
    companion object {
        var lockOutput = false; // mutex alternative for another place. use with synchronized stuff.
        fun txSatoshiToBTC(v: Double) = (v.toBigDecimal() * (0.00000001).toBigDecimal()).toString()
    }
    constructor(port: Int, l: String, p: String, txFee: Double = defTXFee, host: String = "http://127.0.0.1") : super("$host:$port", l, p, isElectrum = true)
    fun version() = this.doCall("version" )
    fun validateaddress(adr: String) = this.doCall("validateaddress", buildJsonArray { add(adr) })
    fun getservers() = this.doCall("getservers")
    fun getunusedaddress() = this.doCall("getunusedaddress")
    fun ismines(adr: String) = this.doCall("ismine", buildJsonArray { add(adr) })
    fun gettransaction(tx: String) = this.doCall("gettransaction", buildJsonArray { add(tx) })
    fun getseed() = this.doCall("getseed")
    fun listaddresses() = this.doCall("listaddresses")
    fun onchain_history() = this.doCall("onchain_history")
    fun getfeerate() = this.doCall("getfeerate") // fee; 1 satoshi = 0.00000001; 1013 satoshi ~ 0.00001013 satoshi
    fun getaddresshistory(adr: String) = this.doCall("getaddresshistory", buildJsonArray { add(adr) })
    // maybe abstract/open/... but for now is ok
    fun satoshiToBTC(satoshi: Double): BigDecimal {
        return satoshiToBTC(satoshi.toString())
    }
    fun satoshiToBTC(satoshi: String): BigDecimal {
        return satoshi.toBigDecimal() * BigDecimal("0.00000001")
    }
    fun get_tx_status(tx: String) = this.doCall("get_tx_status", buildJsonArray { add(tx) }).jsonObject.toMap()?.get("result")?.jsonObject?.toMap()?.get("confirmations").toString().toIntOrNull() ?: -1
    override fun getbalance() = super.getbalance()?.jsonObject?.get("confirmed")
    override fun getaddressbalance(adr: String) = this.doCall("getaddressbalance", buildJsonArray { add(adr) } )?.jsonObject?.toMap()?.get("result")
    override fun createnewaddress( ) = this.doCall("createnewaddress" )
    fun broadcast(tx: String) = this.doCall("broadcast", buildJsonArray { add(tx) } )
    override fun sendMoney(outAddr: String, cMoney: BigDecimal, optionalString: String ): JsonElement{
            val tx_ = this.doCall("payto",  buildJsonArray { add(outAddr); add(  cMoney  );})
            val tx = tx_.jsonObject.toMap()["result"].toString().deleteSquares()
            return broadcast( tx )
    }
}