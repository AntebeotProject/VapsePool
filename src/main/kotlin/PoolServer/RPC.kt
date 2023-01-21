package org.antibiotic.pool.main.PoolServer

import kotlinx.serialization.json.*
import org.antibiotic.pool.main.JSONRPC

const val defTXFee = 0.01
// TODO: Change default name of RPC
class RPC : JSONRPC.worker {
    companion object {
        init {
            // not need for now
            /*
            val natGostd = GOSTD_native()
            val test = natGostd.gostd("test").toHexString()
            val test1 = natGostd.gostdFromBytes(byteArrayOf(1,2,3,4,5)).toHexString()
            if (test != "8cc1b0421621df1da48b2b5213357ca1bb23be4cbdc8dacc4c472e5769482508") {
                throw Exception("Bad Hash of gostd native library")
            }
            if (test1 != "9873577c491a5edeba8ae68366a5d5d82a806d7da4c3f8a876f2acfeb4760595") {
                throw Exception("Bad hash of gostd native library (byte[]) $test1")
            }
            */
        }
        var lockOutput = false; // mutex alternative for another place. use with synchronized stuff.
    }
    // magic number 0.01
    constructor(host: String, l: String, p: String, txFee: Double = defTXFee) : super(host, l, p)

    // Main methods. (maybe here @interface thing will be better, but i think all ok for now) (in future i would to rewrite it on @StratumMethod(...)
    public fun getblockcount() =
        this.doCall("getblockcount").jsonObject.toMap()["result"].toString().toIntOrNull() ?: 0

    // not stratum call
    public fun getwork(): JsonElement {
        val r = this.doCall("getwork")
        // return Json.encodeToString(r)
        return r
    }

    public fun validateaddress(adr: String): JsonElement = this.doCall(
        "validateaddress",
        buildJsonArray { add(adr) }/*JsonPrimitive(adr)*/
    ).jsonObject.toMap()["result"]!!

    public fun getdifficulty() = this.doCall("getdifficulty").jsonObject.toMap()["result"]!!.toString().toDouble()
    public fun GetTemplateBlock() = this.doCall("getblocktemplate")
    public fun getblockhash(height: Int): String {
        val params = buildJsonArray {
            add(height)
        }
        return this.doCall("getblockhash", params).jsonObject.toMap()["result"].toString().deleteSquares()
    }

    public fun getblock(hash: String): JsonElement {
        val params = buildJsonArray {
            add(hash)
        }
        val block = this.doCall("getblock", params).jsonObject.toMap()["result"]
        // println("Block: $block")

        if (block == null) {
            throw StratumServer.MethodRPCException("Can't to found block result data")
        }
        return block
    }
    fun getConfirmationsOfTX(tx: String): Int {
        val res = getTransaction(tx)
        if (res == null) return -1
        return res.jsonObject.toMap()["confirmations"].toString().toInt()
    }
    fun getTransaction(tx: String): JsonElement? {
        val res = this.doCall("gettransaction", buildJsonArray { add(tx) }).jsonObject.toMap()["result"]
        return res
    }
    fun getTransactionDetail(tx: String): JsonElement? {
        return getTransaction(tx)?.jsonObject?.toMap()?.get("details")
    }
    data class listreceiveddata(val address: String, val account: String, val amount:String, val confirmations: Int, val txids: List<String>)
    public fun listreceivedbyaddress(): List<listreceiveddata> {
        val json = this.doCall("listreceivedbyaddress").jsonObject.toMap()["result"]!!.jsonArray
        val rList = mutableListOf<listreceiveddata>()
        for(i in json) {
            // println(i.jsonObject.toMap())
            val m = i.jsonObject.toMap()
            val adr = m?.get("address")
            if (adr != null) {
                val real_adr = adr.jsonPrimitive.toString().deleteSquares()
                val account = m["account"].toString()
                val amount = m["amount"].toString()
                val confirmations = m["confirmations"].toString().toInt()
                val txids = m["txids"]!!.jsonArray!!.map { it.toString().deleteSquares() }
                rList.add(listreceiveddata(real_adr, account, amount, confirmations, txids))
            }
        }
        return rList
    }
    fun settxfee(fee: String) {
        settxfee(fee.toDouble())
    }
    fun settxfee(fee: Double) {
        this.doCall(
            "settxfee",
            buildJsonArray { add(fee) } )
    }
}