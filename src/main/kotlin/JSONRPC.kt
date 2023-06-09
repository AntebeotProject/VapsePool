package org.antibiotic.pool.main

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.antibiotic.pool.main.PoolServer.deleteSquares
import java.math.BigDecimal

typealias login_pair = Pair<String,String>
@Serializable
private data class JSMethodReq(val method: String, val params:Array<String>, val id: Int)

/*
* Is just singleton JSONRPC
* getRawRequest create json for metrhod with params and id.
* method will be like getwork
* params can be empty
* id too can be empty
* example of code: [[deprecated]]
*    val raw = JSONRPC.getRawRequest("getblocktemplate")
*    val anw = JSONRPC.doRequest("http://127.0.0.1:19376", raw, Pair("gostcoinrpc","mycoolpassword"))
*   println(anw)
*
* fun GetTemplateBlock(): String {
    val cl = JSONRPC.worker("http://127.0.0.1:19376", "gostcoinrpc", "57YHtzt")
    val m = cl.doCall("getblocktemplate")
    println(m)
    return m.toString()
}
 */


const val DEBUG_JSON = false
fun printIfDebug(w: String) = if (DEBUG_JSON) println("[DEBUG JSONRPC] $w") else {}

object JSONRPC {
    class NullAnswerFromRPC : NullPointerException("Null answer from RPC")

    @Serializable
    data class _RPCAsk(val method: String, val params: JsonElement = buildJsonArray {  }, val id: Int = 1)
    //enum class type
    //{
    //    BITCOIN,
    //    ELECTRUM,
    //    MONERO
    //}
    open class worker {
        private lateinit var sHost: String
        private lateinit var sLogin: String
        private lateinit var sPassword: String
        //private var mType = type.BITCOIN
        private var isElectrum: Boolean = false
        private var isMonero: Boolean = false
            //get() {
             //   return isElectrum
           // }
        public fun getisElectrum(): Boolean {
            return isElectrum
        }
        public fun getisMonero(): Boolean {
            return isMonero
        }
        private val logPair: Pair<String, String> by lazy() {
            return@lazy Pair(sLogin, sPassword)
        }

        constructor(host: String, l: String, p: String, isElectrum: Boolean = false, isMonero: Boolean = false) {
            sHost = host
            sLogin = l
            sPassword = p
            this.isElectrum = isElectrum
            this.isMonero = isMonero
            if (isMonero && isElectrum) throw Exception("isMonero both isElectrum, and bitcoin mode not allowed")
        }

        public fun doCall(method: String, params: JsonElement = buildJsonArray {  }, id: Int = 1): JsonElement {
            val p = this.logPair
            val data = Json{encodeDefaults = true}.encodeToString( _RPCAsk(method, params, id) )

            printIfDebug("[DEBUG JSONRPC] $data $params on $sHost")
            val r = JSONRPC.executePost(sHost, data, p);
            if (r == null) throw NullAnswerFromRPC()
            // https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json.md
            val map = Json.parseToJsonElement(r)
            printIfDebug("Map is: $map")
            return  map
        }
        fun JsonElement.primitiveToString() {
            this?.jsonPrimitive.toString().deleteSquares()
        }
        fun JsonElement.toString_() {
            this?.toString()?.deleteSquares()
        }
        // open things
        open fun getbalance() = this.doCall("getbalance")?.jsonObject?.toMap()?.get("result")//.toString().deleteSquares()
        open fun getaddressbalance(adr: String) = this.doCall("getbalance", buildJsonArray { add(adr) } )?.jsonObject?.toMap()?.get("result")
        open fun createnewaddress( ) = this.doCall("getnewaddress" )

        // private fun listtransactions(count: Int, account: String = "*", skip: Int? = null) = this.doCall("listtransactions", buildJsonArray { add(account); add(count); if(skip != null) add(skip) })
        //open fun getaddresshistory(adr: String) {

        //}
        // WILL BE CALLed WITH SYNCHRONIZED DATABASE AND ANOTHER STUFF
        open fun sendMoney(outAddr: String, cMoney: BigDecimal, optionalString: String = "From pool" ): JsonElement {
            synchronized(this) {
                return this.doCall(
                    "sendtoaddress",
                    buildJsonArray { add(outAddr); add( cMoney  ); add(optionalString) })
            }
        }


    }

    private fun getRawRequest(method: String, params:Array<String> = emptyArray(), id: Int = 0): String {
        val r = JSMethodReq(method,params,id)
        val string = Json.encodeToString(r)
        return string
    }
    /* *
        * @param targetURL
        * @param post request
        * @log is login and password
     */
    private fun doRequest(targetURL: String, req: String, log: login_pair): String? {
        // val r = executePost("http://localhost:19376", "{\"method\": \"getwork\", \"params\": [], \"id\":0}", p);
        return executePost(targetURL, req, log)
    }

    private fun executePost(targetURL: String?, data: String, log: login_pair): String? {
        var connection: HttpURLConnection? = null
        /*
        POST / HTTP/1.1
        Host: 127.0.0.1:19379
        Authorization: Basic Z29zdGNvaW5ycGM6NTdZSHR6dA==
        Accept-Encoding: deflate, gzip
        Content-Type: application/json
        Content-Length: 45
        User-Agent: cpuminer/2.3.2
        X-Mining-Extensions: midstate

        {"method": "getwork", "params": [], "id":0}
         */
        // println("$data")
        return try {
            //Create connection
            val (l,p) = log
            val encoding: String = Base64.getEncoder().encodeToString(("$l:$p").toByteArray())
            /*Authenticator.setDefault(object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(l, p.toCharArray())
                }
            })*/

            val url = URL(targetURL)
            HttpURLConnection.setFollowRedirects(false)
            connection = url.openConnection() as HttpURLConnection
            connection.setInstanceFollowRedirects(false)

            // connection.setAuthenticator(Authenticator.getDefault())
            // connection.setAuthenticator(Authenticator.getDefault())
            connection.setRequestMethod("POST")
            connection.setRequestProperty("Accept-Encoding", "deflate, gzip")
            connection.setRequestProperty("Authorization", "Basic $encoding")
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )
            connection.setRequestProperty(
                "Content-Length",
                Integer.toString(data.toByteArray().size)
            )
            connection.setDoOutput(true)

            //Send request
            val wr = DataOutputStream(
                connection.getOutputStream()
            )
            wr.writeBytes(data)
            wr.close()
            //Get Response
            val `is`: InputStream = connection.getInputStream()
            val rd = BufferedReader(InputStreamReader(`is`))
            val response = StringBuilder() // or StringBuffer if Java version 5+
            var line: String?
            while (rd.readLine().also { line = it } != null) {
                response.append(line)
                response.append('\r')
            }
            rd.close()
            response.toString()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        } finally {
            if (connection != null) {
                connection.disconnect()
            }
        }
    }
}