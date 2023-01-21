package org.antibiotic.pool.main.WebSite

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.server.handler.gzip.GzipHandler
import org.eclipse.jetty.util.Callback
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.resource.ResourceCollection
import org.antibiotic.pool.main.CryptoCurrencies.CryptoCoins
import org.antibiotic.pool.main.CryptoCurrencies.ElectrumRPC
import org.antibiotic.pool.main.PoolServer.*
import org.antibiotic.pool.main.WebSite.Handlers.*
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


//import kotlinx.coroutines.sync.Mutex
//import kotlinx.coroutines.sync.withLock
const val defCipherKeyAlgo = "AES"
const val defCipherInstance = "AES/CBC/PKCS5Padding"
const val defaultPathOfHTTPFiles = "HTTPServer"

const val defRPCTXFee = 0.01
@Serializable
data class JSONBooleanAnswer(val result: Boolean, val reason: String? = null)

class JettyServer(host: String = "0.0.0.0", port: Int = 8081) {
    private val m_cl = RPCClient.m_cl
    object Encryption {
        // maybe change key fun?
        private val KeyAESForCookie = Settings.m_propetries.getOrDefault("SecretAESKeyForCookie", "123456789ABCDEF-").toString()
        fun strToKey(key: String): SecretKey {
            println("KEY FOR DECRYPT $key")
            var wkey = String(key.toByteArray(), Charsets.US_ASCII)
            if (wkey.length != 16) {
                if (wkey.length > 16) wkey = wkey.substring(0,16) // todo? salt or etc? 128 bit = 16 bytes?
                else while(wkey.length < 16) wkey += "0"
            }
            return SecretKeySpec(wkey.toByteArray(), defCipherKeyAlgo)
        }
        fun getCipher(mode: Int = Cipher.ENCRYPT_MODE): Cipher {
            val cipher: Cipher = Cipher.getInstance(defCipherInstance)
           // var KEY = KeyAESForCookie
           // if (KEY.size != 16) {
           //     if (KEY.size < 16) {
           //     }
           // }
            val secretKey: SecretKey = strToKey(KeyAESForCookie) //SecretKeySpec(KeyAESForCookie, defCipherKeyAlgo)

            // println("Init cipher with $mode and secretKey $secretKey")
            cipher.init(mode, secretKey, IvParameterSpec( ByteArray(16) )); // IV size is 16
            return cipher
        }
        fun encryptData(s: String) = getCipher().doFinal(s.toByteArray())
        fun decryptData(s: ByteArray) = String(getCipher(mode = Cipher.DECRYPT_MODE).doFinal(s))
    }// obj Encryption
    object Cookie {
        const val userSessionCookieName = "usession"
        const val authSeparator = ':'
        const val defCookieTime = 3600
        const val encCookieEnabled = true
        fun addCookie(n: String, v: String, response: HttpServletResponse, encrypt: Boolean = encCookieEnabled, maxAge: Int = defCookieTime) {
            val encData = String(Base64.getEncoder().encode(Encryption.encryptData(v)))
            // println("enc data: $encData")
            val cookie = Cookie(n, if(!encrypt) v else encData)
            cookie.path = "/" // ?
            cookie.isHttpOnly = false //
            cookie.maxAge = maxAge
            response.addCookie(cookie)
        }
        fun delCookie(n: String, response: HttpServletResponse) {
            val cookie = Cookie(n, "")
            cookie.path = "/"
            cookie.maxAge = 0
            response.addCookie(cookie)
        }
        fun getCookie(n: String, req: Request, encrypt: Boolean = encCookieEnabled): String? {
            if (req.cookies == null) return null
            for(c in req.cookies) {
                if (c.name == n) {
                    if (encrypt) {
                        val b64ToByteArray = Base64.getDecoder().decode(c.value)
                        val d = Encryption.decryptData(b64ToByteArray)
                        // println("d = $d")
                        return d
                    }
                    return c.value
                }
            }
            return null
        }
        fun addAuthCookie(l: String, p: String, response: HttpServletResponse) {
            val authData = l + Cookie.authSeparator + p
            // println("Add auth data: $authData")
            Cookie.addCookie("udata", authData, response)
        } // [[deprecated]] [[can be used for servers with small disk size]]
        fun addSessionCookie(l: String, p: String, response: HttpServletResponse): String? {
            val session = DB.addSession(l,p)
            // println("Add session $session")
            if(session != null) {
                Cookie.addCookie(userSessionCookieName, session, response)
                return null
            }
            return "Can't to create a session"
        }

        fun createUSession(workname: String, workpass: String, response: HttpServletResponse): String {
            val e = Cookie.addSessionCookie(workname, workpass, response)
            if (e == null) return Json.encodeToString(JSONBooleanAnswer(true)) // {encodeDefaults=true}?
            else return Json.encodeToString(JSONBooleanAnswer(false, e))
        }

    } // obj Cookie
    object Users {
        @Serializable
        data class UserData(val Login: String, val Balances: Map<String, DB.UserCoinBalance>)
        const val sessionLifeLimitSec = 43200 // 12 hours
        fun getBySession(session_raw: String, response: HttpServletResponse): UserData? {
            val session = DB.getSession(session_raw)
            if (session == null) {
                response!!.setStatus(500);
                response.getWriter().print(Json.encodeToString(JSONBooleanAnswer(false, "not found session")))
                return null
            }
            val timelife_session_in_second = (System.currentTimeMillis() - session.createTimeStamp) / 1000
            if (timelife_session_in_second > Users.sessionLifeLimitSec) {
                response!!.setStatus(500);
                response.getWriter().print(Json.encodeToString(JSONBooleanAnswer(false, "Session outdated")))
                DB.removeSession(session_raw)
                Cookie.delCookie(Cookie.userSessionCookieName, response) // TODO: cron/thread for check
                return null
            }
            val owner = session.owner
            val mp = DB.getLoginBalance(owner)
            if (mp == null) {
                // response!!.setStatus(500);
                return UserData(owner, mapOf<String, DB.UserCoinBalance>())
                // response.getWriter().print(Json.encodeToString(JSONBooleanAnswer(false, "Not found balances for $owner")))
                // return null
            }
            return UserData(owner, mp)
        }
        fun getSession(response: HttpServletResponse, request: Request): String?
        {
            val UsernameSession = JettyServer.Cookie.getCookie(JettyServer.Cookie.userSessionCookieName, request)

            val _session = request?.getParameter("session")
            var session_raw = ""
            println("Username session like to got now check")
            if (UsernameSession == null && _session == null) {
                response!!.setStatus(401);
                return null
            } else if (_session != null) {
                session_raw = _session
            } else {
                session_raw = UsernameSession!!
            }
            return session_raw
        }
        fun foundUnusedAddress(eRpc: ElectrumRPC): String? {
            eRpc.listaddresses().jsonObject.toMap()["result"]?.jsonArray?.shuffled()?.forEach() {
                val adr = it.jsonPrimitive.toString().deleteSquares()
                if (!DB.isUsedAddress(adr)) {
                    return adr
                }
            }
            return null
        }
        fun foundUnusedAddress(bRPC: RPC): String? {
            bRPC.listreceivedbyaddress().shuffled().forEach() {
                if (!DB.isUsedAddress(it.address)) {
                    return it.address
                }
            }

            return null
        }
        fun genNewAddrForUser(Login: String, coinname: String?, search_unused: Boolean = false): String? {
            if (coinname == null) return null
            val rpc = CryptoCoins.coins.get(coinname)
            if (rpc == null) return null
            if (search_unused) {
                if (rpc.getisElectrum()) { // TODO: us double code of one thing. rewrite it later. but for now is not very double code. because is not used much times more than 2
                    val eRpc = rpc as ElectrumRPC
                    val adr = foundUnusedAddress(eRpc)
                    if (adr == null) return genNewAddrForUser(Login, coinname, search_unused= true)
                    return adr
                } else {
                    val bRpc = rpc as RPC
                    val adr = foundUnusedAddress(bRpc)
                    if (adr == null) return genNewAddrForUser(Login, coinname, search_unused= true)
                    return adr
                }
            } else {
                val newAdr = rpc!!.createnewaddress().jsonObject.toMap()["result"]?.jsonPrimitive.toString().deleteSquares() // TODO: check if exists not used address.
                DB.changeLoginBalance(Login, 0.0, coinname)
                return newAdr
            }
            return null
        }
    }
    // is bad idea. but why not
    val server = Server()
    val connector: Connector = ServerConnector(server).also {
        it.port = port
        it.host = host
        it
    }
    private val m_contextCollection = ContextHandlerCollection()
    private fun addRegisterHandler()  {
        val regContext = ContextHandler("/registration")
        regContext.handler = RegistrationHandler()
        m_contextCollection.addHandler(regContext)
    }
    private fun addUserHandler()  {
        val userContext = ContextHandler("/user")
        userContext.handler = UserHandler()
        m_contextCollection.addHandler(userContext)
    }
    private fun addSignInHandler()  {
        val sigIntContx = ContextHandler("/signin")
        sigIntContx.handler = SignInHandler()
        m_contextCollection.addHandler(sigIntContx)
    }
    private fun addNotifyHander() {
        val notifyContx = ContextHandler("/notify")
        notifyContx.handler = NotifyHandler()
        m_contextCollection.addHandler(notifyContx)
    }
    private fun addTradeHander() {
        val exchangeContx = ContextHandler("/exchange")
        exchangeContx.handler = TradeHandler()
        m_contextCollection.addHandler(exchangeContx)
    }
    private fun addStaticDirectoriesHandler() {
        val directories = ResourceCollection()
        directories.setResources(defaultPathOfHTTPFiles)
        directories.addPath(".") // there we can to add Directories


        val reshandler = ResourceHandler()
        // reshandler.baseResource = directories
        reshandler.setResourceBase(defaultPathOfHTTPFiles)
        reshandler.baseResource = Resource.newResource(defaultPathOfHTTPFiles)
        reshandler.isDirectoriesListed = true
        reshandler.welcomeFiles = arrayOf("index.html")
        reshandler.isAcceptRanges = true

        val resCtx = ContextHandler()
        resCtx.handler = reshandler
        resCtx.contextPath = "/"

        m_contextCollection.addHandler(resCtx)
    }
    private fun addApiHandler() {
        // Create the context for the API web application.
        val apiContext = ContextHandler("/api")
        apiContext.handler = RESTHandler()
        // Web applications can be deployed after the Server is started.
        // Web applications can be deployed after the Server is started.
        m_contextCollection.deployHandler(apiContext, Callback.NOOP)
        m_contextCollection.addHandler(apiContext)

    }
    private fun EnableGzip(minGzipSize: Int = 1024) {
        val gzipHandler = GzipHandler()
        gzipHandler.minGzipSize = 1024
        gzipHandler.addIncludedMethods("POST");
        // gzipHandler.addExcludedMimeTypes("font/ttf"); // image/jpeg for graph?
        gzipHandler.handler = m_contextCollection

    }
    private fun addCaptchaHandler() {
        val captchaContx = ContextHandler("/captcha")
        captchaContx.handler = CaptchaHandler()
        m_contextCollection.addHandler(captchaContx)
    }
    private fun EnableHandlers() {
        addRegisterHandler()
        addSignInHandler()
        addStaticDirectoriesHandler()
        addApiHandler()
        addDashBoardHandler()
        addUserHandler()
        addNotifyHander()
        addTradeHander()
        addCaptchaHandler()
        EnableGzip()
    }
    private fun addDashBoardHandler()  {
        val regContext = ContextHandler("/dashboard")
        regContext.handler = DashBoardHandler()
        m_contextCollection.addHandler(regContext)
    }
    init {
        server.addConnector(connector)
        EnableHandlers()
        server.setHandler(m_contextCollection);

        server.start()
        CryptoCoins.initCoins() //

    }

}
