package ru.xmagi.pool.main.CryptoCurrencies

import kotlinx.serialization.json.*
import ru.xmagi.pool.main.JSONRPC
// import ru.xmagi.pool.main.PoolServer.RPCClient
import ru.xmagi.pool.main.PoolServer.defTXFee
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
    fun getfeerate() = this.doCall("getfeerate") // fee; 1 satoshi = 0.00000001; 1013 satoshi ~ 0.00001013 satoshi

    // maybe abstract/open/... but for now is ok
    override fun getbalance() = this.doCall("getbalance", buildJsonArray {  })
    override fun getaddressbalance(adr: String) = this.doCall("getaddressbalance", buildJsonArray { add(adr) } )
    override fun createnewaddress( ) = this.doCall("createnewaddress" )
}