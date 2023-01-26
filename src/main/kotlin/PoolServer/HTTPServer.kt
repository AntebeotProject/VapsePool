package org.antibiotic.pool.main.PoolServer

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.antibiotic.pool.AsyncServer
import org.antibiotic.pool.main.BlockWorker
import org.antibiotic.pool.main.DB.DB
import java.util.*

/*
    *(for getwork pools. like to works, but not implemented check hash... etc before submit. but some another logic for reward. will be added. so. gostd native is
    * can be deleted for now
    * there
    * just for fun some implementation
    * yet is works
    * need to understand coinbase and another things.
    * 'longpoll' will be works
    * if we want to one longpool then we need to check return hash is implementation of https://github.com/jgarzik/pushpool/blob/6df465fbd6932b8db8ac0584c7e87a35ce62e7f2/msg.c#L320
 */
object PrevWorkData {
    var prevHashes = mutableSetOf<String>()
}
const val PoolCoinname = "gostcoin"
// TODO: to class
object HTTPServer {

    //data class FoundBlock(val owner: String, val hash: String, val time: Long)
    //private val foundedBlocks = mutableSetOf<FoundBlock>()

    private var lastBlockTime: Long = 0
    private val m_cl = RPCClient.m_cl

    //public var needSleepTimeSec = 30
    private var threadOfCheckEnabled = false

    //private var last_difficulty = 0.0
    // block that not sended to network
    // https://github.com/jgarzik/pushpool/blob/6df465fbd6932b8db8ac0584c7e87a35ce62e7f2/msg.c#L382
    private fun addBlock(found_block: String, Login: String): Boolean {
        AddBlock@ do {
            val prevHash = BlockWorker.toBlock(found_block).prevHash
            if (PrevWorkData.prevHashes.contains(prevHash)) {
                break@AddBlock
            }
            // TODO: check if authorized
            // TODO: gost256 check or gostd or gost512
            val answ = m_cl.doCall("getwork", buildJsonArray { add(found_block) }).jsonObject.toMap()
            WriteDebug("Answ is?")
            val res = answ["result"]!!.toString().toBooleanStrictOrNull() ?: false
            if (res) {
                logger.add_log("$Login found block with 4.2 GST. We will to him (4.2/2)GST")
                logger.add_log("Now all miners will wait ")
                lastBlockTime = System.currentTimeMillis() / 1000
                DB.addShare(Login, found_block)
                PrevWorkData.prevHashes.add(prevHash)
                if (PrevWorkData.prevHashes.size > 10) PrevWorkData.prevHashes.clear() // TODO: magic number
                DB.addToBalance(Login, 1.2, PoolCoinname)
                return true
            }
        } while (false);
        if (System.currentTimeMillis() / 1000 - lastBlockTime < 60) {
            DB.addToBalance(Login, 0.25, PoolCoinname) // TODO: magic numbers
        }

        DB.addShare(Login, found_block, isGoodShare = false)
        return false
    }


    fun doHTTP(data: String, client: AsyncServer.Client) {

        // todo...
        WriteDebug("HTTP ask $data")
        val userAndPassword =
            Regex("Authorization: Basic \\w+").findAll(data).map { it.groupValues[0] }.toList()[0].split(" ")[2]
        val decoded_userAndPassword = String(Base64.getDecoder().decode(userAndPassword)).split(":")
        val Login = decoded_userAndPassword.getOrNull(0) ?: ""
        val Password = decoded_userAndPassword.getOrNull(1) ?: ""
        if (DB.checkUserPassword(Login, Password) == false) {
            client.write("HTTP/1.1 401 Unauthorized\r\n\r\n")
            client.closeConnection()
        }
        // todo: login password
        val Miner = MinerData.getMiner(Login)
        var json_data = ""
        logger.add_log("$Login connected with $Password")
        //
        try {
            // is weird for now is ok.
            getJsonData@ do {
                try {
                    json_data = "{" + data.split("{")[1]
                } catch (_: java.lang.IndexOutOfBoundsException) {
                    val aData = client.read()
                    json_data = "{" + aData.split("{")[1]
                }
            } while (false);
            WriteDebug("JSON_DATA: $json_data")
            val js = Json.decodeFromString<RPCJSON>(json_data)
            Miner.updateLastActive()
            when (js.method) {
                "getwork" -> {
                    if (js.params.size == 0) {
                        onSoloGetwork(Miner, client)
                    } else {
                        val found_block = js.params[0].replace("\"", "").replace("\\", "")
                        WriteDebug("found block is")
                        logger.add_log("$Login like to found block $found_block")
                        sendBooleanResult(client, addBlock(found_block, Login))
                        client.closeConnection() // TODO:
                    }
                }
            }
        } catch (_: java.lang.IndexOutOfBoundsException) {
            val aData = client.read()
            println(aData)
        } catch (e: Exception) {
            WriteDebug("Some exception ${e.toString()}")
            client.write("500 Internal Server Error")
            client.closeConnection()
        }
    }

    private fun sendBooleanResult(client: AsyncServer.Client, r: Boolean) {
        val w = Json.encodeToString(ResponseResult(1, JsonPrimitive(r), null))
        val retStr = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Server: vapsepool\r\n")
            append("Content-Type: application/json\r\n")
            append("Content-Length: ${w.length}\r\n")
            append("Connection: keep-alive\r\n\r\n")
            append(w)
        }
        WriteDebug("Send to client (HTTP) $w")
        client.write(retStr)
    }

    //    /
    //

    private fun onSoloGetwork(Miner: MinerData, client: AsyncServer.Client) {
        val w = m_cl.getwork()
        var newWork = Json.encodeToString(w)
        if (PoolServer.maxIdleTries <= 0 || PoolServer.maxIdleSecond <= 0) {
            Settings.load_propetries()
            Settings.m_propetries.setProperty("idleSecond", PoolServer.defMaxIdleSecond.toString())
            Settings.m_propetries.setProperty("IdleTries", PoolServer.defMaxIdleTries.toString())

            PoolServer.maxIdleSecond = Settings.m_propetries.getOrDefault("idleSecond", PoolServer.defMaxIdleSecond).toString()
                .toInt() //("idleSecond").toInt()
            PoolServer.maxIdleTries = Settings.m_propetries.getOrDefault("IdleTries", PoolServer.defMaxIdleTries).toString().toInt()
        }
        if (Miner.idleMoreThan(PoolServer.maxIdleSecond)) {
            logger.add_log("[IDLE TIME] Miner ${Miner.Login} very slow. more than 30 seconds. we are will to test him")
            val data = w.jsonObject.toMap()["result"]!!.jsonObject!!.toMap()["data"]!!
            val block = BlockWorker.toBlock(data.toString().deleteSquares())
            // println(Hex2Difficulty(block.difficulty))
            val nData = block.modifyDifficulty("00000000").toGetWork()
            newWork = Json.encodeToString(w).replace(Regex("\"data\":\"\\w+\""), "\"data\":\"\\$nData\"")
            Miner.IdleTries++;
            // DB.addToBalance(Miner.Login, 0.0) // TODO:magic numbers
            logger.add_log("[IDLE TIME] Miner ${Miner.Login} tries ${Miner.IdleTries}")


            // m_propetries.setProperty("IdleTries", maxIdleTries.toString())
            if (Miner.IdleTries > PoolServer.maxIdleTries) {
                logger.add_log("[WARNING] Miner ${Miner.Login} NOT GIVE SHARES")
                client.closeConnection()
                Miner.IdleTries = 0
                // Miner.shares -= 660;
            }
            // println("[DEBUG DIFFICULTY WAS CHANGED TO ZEROS]")
        }
        //println("block: ${block}")
        // println("new work: \n$newWork")
        // println(Json.encodeToString(w) == newWork)
        //val block = BlockWorker.toBlock(data.jsonObject.toString())
        //println("data $data")

        val retStr = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Server: vapsepool\r\n")
            append("Content-Type: application/json\r\n")
            append("Content-Length: ${newWork.length}\r\n")
            append("Connection: keep-alive\r\n\r\n")
            append(newWork)
        }
        client.write(retStr)
        // X-Long-Polling:
        // X-Blocknum:

        //client.closeConnection()
    }
}