package org.antibiotic.pool.main.PoolServer;

// import GOSTD_native

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.util.*


const val debugEnabled = true
fun WriteDebug(msg: String) = if (debugEnabled) println("[STRATUM DEBUG] $msg") else {}
/* *
    * there will be stratum things
    * https://en.bitcoin.it/wiki/Stratum_mining_protocol
 */

// https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json.md#array-wrapping

@Serializable
data class RPCJSON(val id: Int, val method: String, val params: ArrayList<String>)
// https://github.com/aeternity/protocol/blob/master/STRATUM.md#mining-subscribe
@Serializable
data class ResponseResult(val id: Int, val result: JsonElement, val error: String?)

/*
https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json.md
 */

// external fun gostd(input: String?): ByteArray?
// external fun gostdFromBytes(input: ByteArray?): ByteArray?

/*
    * @Author Roland https://stackoverflow.com/questions/52225212/converting-a-byte-array-into-a-hex-string
 */
@ExperimentalUnsignedTypes // just to make it clear that the experimental unsigned types are used
fun ByteArray.toHexString() = asUByteArray().joinToString("") { it.toString(16).padStart(2, '0') }
fun String.deleteSquares() = this.replace("\\", "").replace("\"", "")
const val miningAddress = "mmeA7fLqJ7x8hujXWV33vtYjiPgzQMKfvM"

// TODO: to another files some classes. for now is yet ok. i think
object PoolServer {
    var maxIdleSecond = 0
    var maxIdleTries = 0



    val statrtTimeOfServerWork = System.currentTimeMillis() / 1000
    var sharesUptime = 0
    fun getServerUptimeSeconds() =  System.currentTimeMillis() / 1000 - statrtTimeOfServerWork
    val currentWorkers: () -> Int = fun(): Int { return MinerData.currentMiners.size }


    // typealias JsonParserMethod = (json: JsonElement?) -> JsonElement?
    const val updateTimeBlockTemplateSeconds = 30;
    const val defHost = "http://127.0.0.1:19376"
    const val defUser = "gostcoinrpc"
    const val defPass = "57YHtzt"
    const val defMaxIdleSecond = 30
    const val defMaxIdleTries = 30
}