package ru.xmagi.pool.main.PoolServer

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import ru.xmagi.pool.main.JSONRPC
import java.util.ArrayList

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
    public fun listreceivedbyaddress(): List<String> {
        val json = this.doCall("listreceivedbyaddress").jsonObject.toMap()["result"]!!.jsonArray
        val rList = mutableListOf<String>()
        for(i in json) {
            val adr = i.jsonObject.toMap()["address"]
            if (adr != null) rList.add(adr.jsonPrimitive.toString().deleteSquares())
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
    // abstract

    // [[deprecated part. have to be deleted]]
    private fun RPCJSONTest(): Nothing {
        throw Exception("Is only for test [[deprecated]]")
        var arr = ArrayList<String>()
        arr.add("cpuminer/2.3.2")
        val data = RPCJSON(1, "mining.subscribe", arr)
        val string = Json.encodeToString(data)
        println(string)
    }
}