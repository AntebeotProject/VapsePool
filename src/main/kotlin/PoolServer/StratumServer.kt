package org.antibiotic.pool.main.PoolServer

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.bitcoinj.core.*
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.script.ScriptBuilder
import org.antibiotic.pool.AsyncServer
import kotlin.concurrent.thread
import kotlin.random.Random

// For stratum server support
// TODO: to class. NOT IMPLEEMENTED FULL

object StratumServer {
    private var jobId = 1
    private val m_cl = RPCClient.m_cl

    // m_cl is RPC JSon part. So rename it
    private var prevBlockHeight: Int = m_cl.getblockcount()
    private var last_time: Long = System.currentTimeMillis() / 1000
    private var last_block: JsonElement? = null
        get() {
            val current_time = System.currentTimeMillis() / 1000
            if (current_time - last_time > PoolServer.updateTimeBlockTemplateSeconds) {
                last_time = System.currentTimeMillis() / 1000
                return m_cl.GetTemplateBlock()
            }
            return null
        }

    // Do Json
    private fun sendBoolResult(c: AsyncServer.Client, r: Boolean = true) {
        val raw = Json.encodeToString(ResponseResult(1, JsonPrimitive(r), null))
        c.write(raw)
    }

    class MethodRPCException(m: String) : Exception("JSON RPC ERROR $m")
    class NotFoundMethod(m: String) : Exception("StratumServer unknown method $m")

    fun isJSON(s: String) =
        Regex("[:,\\{\\}\\[\\]]|(\\\".*?\\\")|('.*?')|[-\\w.]+").findAll(s).map { it.groupValues[0] }
            .toList().size > 0

    fun GetJSONRPC(data: String): RPCJSON? {
        if (isJSON(data) && data.endsWith("}") && data.startsWith("{")) {
            try {
                val r = Json.decodeFromString<RPCJSON>(data)
                println("[DEBUG JSON] $r")
                return r
            } catch (e: Exception) {
                println("[DEBUG JSON (IS NOT JSON)] $data")
                return null
            }
        }
        return null
    }

    /*
    * IDK. just read https://github.com/MintPond/mtp-stratum-mining-protocol/blob/master/03_MINING.SET_TARGET.md
    * and open source code of poolserver. there is just 0000.... magic number. so. i think
    * that in future need to understand that it mean. for now i think all ok.
 */
    private fun setTarget(client: AsyncServer.Client) {
        val magic_bullshit_of_difficulty = "00000000ffff0000000000000000000000000000000000000000000000000000"
        val raw = Json.encodeToString(
            RPCJSON(1, "mining.set_target", arrayListOf(magic_bullshit_of_difficulty))
        )
        client.write(raw)
    }

    private fun getNTime(curTime: Long): String {
        // "curtime" : 1673043807, from getblocktemplate?
        val inHex = curTime.toString(16)
        val r = String.format("%8s", inHex).replace(" ", "0")
        return r
    }

    /*
 * @author https://xakep.ru/2018/02/26/java-mining-pool/
 */
    private fun generateCoinbaseTransaction(
        params: NetworkParameters,
        height: Int,
        coinbaseauxFlags: String,
        extranonce: ByteArray,
        message: String,
        // pubKeyTo: ByteArray,
        value: Coin
    ): Transaction? {
        val coinbase = Transaction(params)
        val inputBuilder = ScriptBuilder()
        inputBuilder.number(height.toLong())
        val coinbseauxFlagsData = if (coinbaseauxFlags.isNotEmpty()) Utils.HEX.decode(coinbaseauxFlags) else ByteArray(0)
        val messageData = message.toByteArray()
        val data = ByteArray(coinbseauxFlagsData.size + extranonce.size + messageData.size)
        if (coinbseauxFlagsData.size > 0) {
            System.arraycopy(coinbseauxFlagsData, 0, data, 0, coinbseauxFlagsData.size)
        }
        System.arraycopy(extranonce, 0, data, coinbseauxFlagsData.size, extranonce.size)
        System.arraycopy(messageData, 0, data, coinbseauxFlagsData.size + extranonce.size, messageData.size)
        inputBuilder.data(data)
        coinbase.addInput(TransactionInput(params, coinbase, inputBuilder.build().getProgram()))
        // TODO!!!!
        // https://github.com/GOSTSec/gostcoin/blob/51240469f0933c6efa7f02239c413fd26f528465/src/key.cpp#L57
        val pubKey = m_cl.validateaddress(miningAddress).jsonObject.toMap()["pubkey"]!!.toString()
            .deleteSquares() // 0312eddbf256a02889ff904fbdf5ba6bceb41b4aa84a8401e1ed50cd533495c3cd
        // val aFromString = Address.fromString(null, miningAddress ) // ECKey.fromPublicOnly(pubKeyTo)
        // https://github.com/GOSTSec/gostcoin/blob/51240469f0933c6efa7f02239c413fd26f528465/src/test/sigopcount_tests.cpp#L34
        // https://ru.bitcoinwiki.org/wiki/P2SH
        // val aFromString = Address.fromKey(TestNet3Params(), ECKey.fromPublicOnly(pubKey), Script.ScriptType.P2SH)
        TODO("Not release P2SH key.")
        coinbase.addOutput(
            TransactionOutput(
                params,
                coinbase,
                value,
                ScriptBuilder.createP2SHOutputScript(pubKey.toByteArray()).program
            )
        )
        return coinbase
    }

    // TODO: rename
    private fun getWork(c: AsyncServer.Client, cleanJob: Boolean = false) {
        WriteDebug("getwork")
        val block = last_block ?: m_cl.GetTemplateBlock()
        val bMap = block.jsonObject.toMap()["result"]!!.jsonObject.toMap()

        val version = bMap["version"]!!.toString().toLong()
        WriteDebug("Version: $version")
        val prevHash = bMap["previousblockhash"]!!.toString().deleteSquares() // will be reversed? every 4 bytes?
        WriteDebug("prevHash: $prevHash")
        val time = bMap["curtime"]!!.toString().deleteSquares().toLong()
        WriteDebug("time: $time")
        // Util.Reverse(Util.ASCIIToBin(
        val bits = bMap["bits"]!!.toString().deleteSquares()
        //) //.toLong(16).toString(2).reversed() // there is hex already. ?
        WriteDebug("bits: $bits")
        val coin = Coin.valueOf(
            bMap["coinbasevalue"]!!.toString().toLong()
        ) //blocktemplate.getJsonObject("result").getLong("coinbasevalue"))
        val height: Int =
            bMap["height"]!!.toString().toInt() // blocktemplate.getJsonObject("result").getInteger("height")
        val coinbaseauxFlags: String =
            bMap["coinbaseaux"]!!.jsonObject.toMap()["flags"]!!.toString().deleteSquares()
        // c.extranonce
        val extranonce = ByteArray(8)
        val message = "Vapse prosto"
        val params_ = TestNet3Params.get()
        // NetworkParameters
        // val pubKeyTo = ECKey().pubKey
        val coinbase =
            generateCoinbaseTransaction(
                params_,
                height,
                coinbaseauxFlags,
                extranonce,
                message,
                coin
            )!!.bitcoinSerialize()
        WriteDebug("Build JsonArray")
        /*

    if (!job_id || !prevhash || !coinb1 || !coinb2 || !version || !nbits || !ntime ||
         || strlen(version) != 8 ||
        strlen(nbits) != 8 || strlen(ntime) != 8) {
*/
        // https://github.com/jgarzik/pushpool/blob/6df465fbd6932b8db8ac0584c7e87a35ce62e7f2/msg.c#L259 ; maybe we too need to save prevhash

        WriteDebug(prevHash.length.toString())
        val coinbase1 = ByteArray(5)
        // coinbase[0] + coinbase[1] + coinbase[2] + coinbase[3] + coinbase[4]
        for (i in 0 until 5) {
            coinbase1[i] = coinbase[i]
        }
        val coinbase_extranonce = ByteArray(8)
        for (i in 5 until 5 + 8) {
            coinbase_extranonce[i - 5] = coinbase[i]
        }
        val coinbase2 = ByteArray(17)
        for (i in 5 + 8 until (5 + 8) + 17) {
            coinbase2[i - (5 + 8)] = coinbase[i]
        }
        val params = buildJsonArray {
            add(jobId.toString(16))
            add(prevHash)
            add(coinbase1.toHexString())
            add(coinbase_extranonce.toHexString())
            add(coinbase2.toHexString())
            add(buildJsonArray { }) // merkleBranch here
            add(String.format("%8s", version.toString(16)).replace(" ", "0"))
            // bits.toString().toByteArray().toHexString() // is weird. we sure that we need it? IS ALREADY HEX STRING IS NOT NEED I THINK
            val b = bits  // nonce?
            add(String.format("%8s", b).replace(" ", "0"))
            add(String.format("%8s", time.toString(16)).replace(" ", "0").reversed())
            add(cleanJob)
        }
        val ret = buildJsonObject {
            put("id", null)
            put("method", "mining.notify")
            put("params", params)
        }
        val retStr = Json.encodeToString(ret)
        WriteDebug(retStr)
        jobId++
        c.write(retStr)
    }

    // will be refactored in future version. not works yet?
    // https://www.researchgate.net/publication/315456462/figure/fig1/AS:669966533668885@1536743877675/Coinbase-transaction-format-The-fields-in-the-table-are-the-underlying-Bitcoin.jpg

    private fun miningNotify(client: AsyncServer.Client) {
        // thread for getWork notify
        thread {
            val threadSleepMillis = 5000L;
            while (client.authorize && client.getIsActive()) {
                getWork(client)
                Thread.sleep(threadSleepMillis)
            }
            WriteDebug("Client closed connection. stop thread of getWork")
        }
    }

    private fun onMiningAuthorize(data: RPCJSON, client: AsyncServer.Client) {
        WriteDebug("onMiningAuthorize")
        val params = data.params
        val login = params[0]
        val password = params[1]
        WriteDebug("$login want to authorize with $password password")
        if (true) {
            client.setAuthorize(true)
            sendBoolResult(client, r = true)
            setTarget(client)
            miningNotify(client)
        } // TODO: Check to authorize
        else {
            sendBoolResult(client, r = false)
            client.closeConnection()
        }
    }

    private fun onMiningSubscribe(client: AsyncServer.Client) {
        WriteDebug("onMiningSubscribe")

        val nonce = with(m_cl) {
            val blockCount = getblockcount()
            if (blockCount == 0) {
                throw MethodRPCException("Can't to get block count of RPC")
            }
            val blockHash = getblockhash(blockCount)
            val block = getblock(blockHash)
            val nonce = block.jsonObject.toMap()["nonce"].toString().toLong() // & 0xfffffffff ?
            String.format("%8s", nonce.toString(16)).replace(" ", "0")
        }
        /*
     This nonce sent by the server
     is usually referred to as the extranonce, i.e. nonce =
     minernonce || extranonce. The server MAY freely choose the length of the extranonce
     and clients SHOULD NOT expect it to be always of the same length. extranonce and minernonce MUST be 8 bytes long together.
    */

        // WriteDebug("nonce: $nonce")
        //
        WriteDebug("Mining subscribe")
        val m = last_block ?: m_cl.GetTemplateBlock()
        // WriteDebug(m)
        // TODO: If the server supports session resumption, then this SHOULD be a unique session id, null otherwise
        val getSessionId = fun(): String {
            val ethallon = "deadbeefcafebabeef2e000000000000";
            // val R = Random.nextBytes(6).toString(StandardCharsets.)
            var SessionID: String = ""; // Random.nextInt(0,9);
            while (SessionID.length < ethallon.length) {
                val c = if (Random.nextBoolean()) Random.nextInt('0'.code, '9'.code) else Random.nextInt(
                    'a'.code,
                    'f'.code
                )
                SessionID += c.toChar()
            }
            client.setSessionId(SessionID)
            return SessionID // TODO: read NORMAL specs or do reverse enginering of cpuminer
        }
        val session_id = getSessionId()
        val params = buildJsonArray {
            add(buildJsonArray {
                add(buildJsonArray { add("mining.subscribe"); add(session_id) })
                add(buildJsonArray { add("mining.notify"); add(session_id) })
            })
            val extraNonce = 4 //Random.nextInt(4,4) // maybe change to 0 9 but...
            client.setExtranonce(session_id)
            client.setExtranonce2Size(extraNonce) // weird name. is fact not extraNonce.
            add(nonce); add(extraNonce)
        }
        val answ = Json.encodeToString(ResponseResult(1, params, null))
        client.write(answ)
    }

    // methods without arguments.
    private val mMethods = mutableMapOf("mining.subscribe" to StratumServer::onMiningSubscribe)

    // RPCJSON can be changed to JsonElement
    fun doJSON(data: RPCJSON, client: AsyncServer.Client) {
        when (data.method) {
            "mining.authorize" -> {
                onMiningAuthorize(data, client)
            }

            else -> {
                if (!mMethods.contains(data.method)) {
                    if (data.method.equals("getwork")) {
                        if (data.params.size == 0) {
                            client.write(Json.encodeToString(m_cl.getwork()))
                        } else {
                            TODO("there is bug")
                        }
                    }
                    throw NotFoundMethod(data.method)
                }
                mMethods[data.method]!!(client)
            }
        }
    }
}