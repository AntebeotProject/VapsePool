package org.antibiotic.pool.main.WebSite

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.antibiotic.pool.main.CryptoCurrencies.CryptoCoins
import org.antibiotic.pool.main.CryptoCurrencies.ElectrumRPC
import org.antibiotic.pool.main.CryptoCurrencies.MoneroRPC
import org.antibiotic.pool.main.DB.*
import org.antibiotic.pool.main.PoolServer.RPC
import org.antibiotic.pool.main.PoolServer.Settings
import org.antibiotic.pool.main.PoolServer.debugEnabled
import org.antibiotic.pool.main.PoolServer.deleteSquares
import org.antibiotic.pool.main.WebSite.Handlers.*
import org.antibiotic.pool.main.i18n.i18n

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
import org.eclipse.jetty.util.ssl.SslContextFactory
import java.math.BigDecimal
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

const val defRPCTXFee = 0.01 // I think is ok. for now.
fun printIfDebug(w: String) = if (debugEnabled) println("[DEBUG JETTY] $w") else {}

@Serializable
data class JSONBooleanAnswer(val result: Boolean, val reason: String? = null)

fun String.delHTML(notAl: List<Char> = listOf('<', '>','"', '\'')): String
{
    var rString = this
    for(ch in notAl)
    {
        rString = rString.replace(ch, '_')
    }
    return rString
}

class JettyServer(host: String = "0.0.0.0", port: Int = 8081) {
    companion object {
        public fun sendJSONAnswer(res: Boolean, text: String, response: HttpServletResponse) = response.writer.print(Json.encodeToString(JSONBooleanAnswer(res, text)))
        fun pWarning(m: String) = System.err.println("[WARNING OF PART OF JETTY SERVER] $m")
        fun getOffsetLimit(request: Request): Pair<Int, Int> {
            try {
                val offset = request.getParameter("offset").toIntOrNull() ?: 0
                val lim = request.getParameter("lim").toIntOrNull() ?: 5
                return Pair(offset, lim)
            } catch (_: Exception){
                return Pair(0,5)
            }
        }
    }
    object Encryption {
        // maybe change key fun?
        private val KeyAESForCookie = Settings.m_propetries.getOrDefault("SecretAESKeyForCookie", "123456789ABCDEF-").toString()
        fun strToKey(key: String): SecretKey {
            // println("KEY FOR DECRYPT $key")
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
        object money
        {
            fun genAdr(coin: String, uLanguage: i18n, owner: String): String
            {

                val rpc = CryptoCoins.coins.get(coin)
                if (rpc == null || coin == null) {
                    return Json { encodeDefaults = true }.encodeToString(
                         JSONBooleanAnswer(
                            false,
                            uLanguage.getString("notFoundCryptoCur")
                        )
                    )
                } else {
                    if (DB.userHaveNotConfirmedTXOnCoinName(owner, coin)) {
                        tx.clearOldTxs(1)
                        return Json.encodeToString(JSONBooleanAnswer(false, uLanguage.getString("uHaveNotConfirmedTx")))
                    } else {
                        val nadr = JettyServer.Users.genNewAddrForUser(owner, coin, search_unused = true)
                        if (nadr == null) return Json.encodeToString(JSONBooleanAnswer(false, uLanguage.getString("internal_serv_error")))
                        return Json.encodeToString(
                             JSONBooleanAnswer(
                                true,
                                String.format(uLanguage.getString("urNewAddrIs"), nadr)
                            )
                        )
                            .also { UserCoinBalance.setLoginInputAddress(owner, nadr!!, coin!!) }
                    }
                }
            }
        }
        object OTP
        {
            fun check(u: String, request: Request, parName: String = "otpcode"): Boolean
            {
                if (!userOTP.userOTPExistsForUser(u)) return true
                val code = request.getParameter(parName)
                val rcode = userOTP.getCodeForUser(u)
                return code.equals(rcode)
            }
        }
        object language
        {
            fun geti18nByLocale(s: String): Locale
            {
                val sp = s.split("_")
                // println("locale is ${sp[0]}_${sp[1]}")
                return Locale(sp[0], sp[1])
            }
            fun getLangWithoutSession(request: HttpServletRequest?): i18n
            {
                var curLanguage = defUserLanguage
                try {
                    val lang = request!!.getParameter("lang")
                    curLanguage = lang
                } catch(_: Exception) {}
                return i18n(locale = geti18nByLocale(curLanguage))
            }
            fun getLangByUser(o: String)  = i18n( locale = geti18nByLocale(userLanguage.getForUser(o)?.language?: defUserLanguage) )
        }
        object cryptocoins
        {
            fun getJSONAnswer(b: Boolean, text: String) = Json.encodeToString(JSONBooleanAnswer(b, text))
            fun sendMoney(acc: String, oAdr: String, coinname: String, cMoney: String): String {
                synchronized(DB) {
                    val oAddrInDBOwner =
                        DB.getOwnerOfAddress(oAdr) /// weird name . so is null if is not local address of our self... TODO: check by listaddress
                    // validate address before send! TODO
                    if (isValidAddress(oAdr=oAdr, coinname = coinname) == false) {
                        return getJSONAnswer(false, "$oAdr Is not valid address for $coinname ")
                    }
                    printIfDebug("now synchronized")
                    var txMonero: JsonElement? = null
                    synchronized(CryptoCoins.coins[coinname]!!) {
                        // do User not blocked. output is enabled. coinname is enabled?
                        val balances = DB.getLoginBalance(acc)
                        val UserBalanceOfCoin = balances?.get(coinname)
                        if (UserBalanceOfCoin?.isBlocked == true) return getJSONAnswer(false, "Your account is blocked for a while. write to administration")
                        if (RPC.lockOutput) return getJSONAnswer(false, "Some user do output for now. wait a while")
                        if (CryptoCoins.coins.get(coinname) == null) return getJSONAnswer(false, "The coinname is disabled for now")
                        // get TX Fee or set.
                        printIfDebug("txFee")
                        val (txFee, txMonero_) = getTXFee(coinname, oAdr, cMoney)
                        txMonero = txMonero_
                        printIfDebug("txmonero now is $txMonero; $cMoney")
                        // Do user have enough money?
                        val sendMoneyCount = cMoney.toBigDecimal()
                        val userBalance = UserBalanceOfCoin?.balance?.toBigDecimal() ?: 0.0.toBigDecimal()
                        val isUserNotHaveEnoughMoney = if (oAddrInDBOwner != null) {
                            sendMoneyCount > userBalance
                        } else {
                            (sendMoneyCount + txFee) > userBalance || userBalance < txFee || sendMoneyCount < txFee
                        }
                        if (isUserNotHaveEnoughMoney)
                            return getJSONAnswer(false, "Not correct count of money ${cMoney.toBigDecimal() + txFee} and ${UserBalanceOfCoin?.balance} maybe txfee is big")
                        // send money
                        RPC.lockOutput = true
                        val result = if (oAddrInDBOwner != null) {
                            // if local transaction
                            DB.createNewNotification(oAddrInDBOwner, "input local $coinname +$cMoney")
                            DB.addToBalance(oAddrInDBOwner, cMoney.toBigDecimal(), coinname)


                            DB.createNewNotification(acc, "output local $coinname -$cMoney")
                            DB.addToBalance(acc, -cMoney.toBigDecimal(), coinname)
                            Pair(true, "local $cMoney without fee was send on address $oAdr")
                        } else {
                            printIfDebug("else")
                            if (txMonero != null)
                            {
                                printIfDebug("$txMonero")
                                printIfDebug("txMonero not equal null")
                                val monero = CryptoCoins.coins[coinname]!! as MoneroRPC
                                // println("send tx_metadata from $txMonero")
                                val tx_metadata = txMonero!!.jsonObject.toMap()["result"]!!.jsonObject.toMap()["tx_metadata"].toString().deleteSquares()
                                printIfDebug(tx_metadata)
                                val ret = monero.relay_tx(tx_metadata)
                                if (ret?.jsonObject?.toMap()?.get("error") != null)
                                {
                                    val message = ret!!.jsonObject!!.toMap()!!.get("error")!!.jsonObject!!.toMap()!!.get("message")!!
                                    printIfDebug("ret internal error")
                                    Pair(false, "internal error: $message")
                                }else {
                                    val txAmount_Atomic = monero.toAtomic(cMoney.toBigDecimal())
                                    val txAmount_fee_Atomic = monero.toAtomic(txFee)
                                    val atomic_amount = (txAmount_Atomic + txAmount_fee_Atomic)
                                    //
                                    val not_atomic_value = monero.fromAtomic(atomic_amount.toBigDecimal())
                                    printIfDebug("add to balance $acc; -$atomic_amount , -$not_atomic_value")
                                    DB.addToBalance(acc, -not_atomic_value, coinname)
                                    Pair(true, "${not_atomic_value} with fee was send on address $oAdr; $ret")
                                }
                            } else {
                                printIfDebug("tx monero is equal null")
                                // if not local transaction
                                CryptoCoins.coins[coinname]!!.sendMoney(oAdr, (cMoney.toBigDecimal()), "$acc from pool")
                                DB.addToBalance(acc, -(cMoney.toBigDecimal() + txFee), coinname)
                                Pair(true, "${cMoney.toBigDecimal() + txFee} with fee was send on address $oAdr")
                            }
                        }
                        RPC.lockOutput = false // TODO: fix logic. without double of code
                        val (res, text) = result
                        return getJSONAnswer(res, text)
                        //"Money was send! <meta http-equiv=\"refresh\" content=\"5; url=/\">")
                    } // synchronized(RPC) CryptoCoins.coins[coinname]!!
                }// synchronized(DB)
            }
            fun sendMoney(acc: String, oAdr: String, coinname: String, cMoney: String, response: HttpServletResponse) {
                return response.writer.print(sendMoney(acc, oAdr, coinname, cMoney))
            }// FUN
            // tx + transaction
            fun getTXFee(coinname: String, oAdr: String? = null, cMoney: String? = null): Pair<BigDecimal, JsonElement?> {
                var txFee = defRPCTXFee.toBigDecimal() // magicNumber
                if (CryptoCoins.coins[coinname]!!.getisElectrum()) {
                    val tx_ =
                        (CryptoCoins.coins[coinname]!! as ElectrumRPC).getfeerate().jsonObject.toMap()["result"]!!.jsonPrimitive.toString()
                    txFee =
                        (CryptoCoins.coins[coinname]!! as ElectrumRPC).satoshiToBTC(tx_) //tx_.toBigDecimal() //?: defRPCTXFee.toBigDecimal() // 0.01 is very big for Electrum. so
                } else if (CryptoCoins.coins[coinname]!!.getisMonero()) {
                    //
                    val adr = oAdr!!
                    val rpc = CryptoCoins.coins[coinname]!! as MoneroRPC
                    val tx = rpc.transfer(adr, cMoney!!.toBigDecimal(), do_not_relay = true)
                    printIfDebug(tx.toString())
                    val fee = rpc.fromAtomic( tx.jsonObject.toMap()["result"]?.jsonObject?.toMap()?.get("fee")!!.toString().toBigDecimal() )
                    printIfDebug("fee ${fee}") // can be wrong? maybe better tx_relay
                    return Pair(fee, tx)
                }
                else {
                    (CryptoCoins.coins[coinname]!! as RPC).settxfee(txFee.toString()) // TODO: change the value? from nethowrk
                }
                return Pair(txFee, null)
            }
            // we also can to check ismine.
            fun isValidAddress(coinname: String, oAdr: String): Boolean {
                if (CryptoCoins!!.coins!!.get(coinname)!!.getisElectrum()) {
                    val rpc = CryptoCoins!!.coins!!.get(coinname)!! as ElectrumRPC
                    return rpc.validateaddress(oAdr).jsonObject.toMap()["result"].toString().deleteSquares().toBoolean()
                } else if(CryptoCoins!!.coins!!.get(coinname)!!.getisMonero())
                {
                    val rpc = CryptoCoins!!.coins!!.get(coinname)!! as MoneroRPC
                    return rpc.validate_address(oAdr)
                } else {
                    val rpc = CryptoCoins!!.coins!!.get(coinname)!! as RPC
                    return rpc.validateaddress(oAdr).jsonObject.toMap()["isvalid"].toString().deleteSquares().toBoolean()
                }
            }
        }
        @Serializable
        data class UserData(val Login: String, val Balances: Map<String, UserCoinBalance>)
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
                return UserData(owner, mapOf<String, UserCoinBalance>())
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
            printIfDebug("Username session like to got now check")
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
        private fun foundUnusedAddress(rpc: MoneroRPC): String? {
            val mRPC = rpc as MoneroRPC
            mRPC.get_accounts()?.shuffled()?.forEach()
            {
                //printIfDebug(it)
                //printIfDebug(it.base_address)
                if (!DB.isUsedAddress(it.base_address)) {
                    return it.base_address
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
                    if (adr == null) return genNewAddrForUser(Login, coinname, search_unused= false)
                    return adr
                } else if (rpc.getisMonero()) {
                    //printIfDebug("get RPC")
                    val mRpc = rpc as MoneroRPC
                    val adr = foundUnusedAddress(rpc)
                    if (adr == null) return genNewAddrForUser(Login, coinname, search_unused= false)
                    return adr
                } else {
                    val bRpc = rpc as RPC
                    val adr = foundUnusedAddress(bRpc)
                    if (adr == null) return genNewAddrForUser(Login, coinname, search_unused= false)
                    return adr
                }
            } else {

                val newAdr = if (!rpc.getisMonero()) rpc!!.createnewaddress().jsonObject.toMap()["result"]?.jsonPrimitive.toString().deleteSquares() else
                    rpc.createnewaddress().jsonObject.toMap()["result"]?.jsonObject?.toMap()?.get("address").toString().deleteSquares() // TODO: check if exists not used address.
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
    // Handlers starts there
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
        Captcha.runThreadToCleanLastCaptches()
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
        addPrivilegiesHandler()
        EnableGzip()
    }
    private fun addDashBoardHandler()  {
        val regContext = ContextHandler("/dashboard")
        regContext.handler = DashBoardHandler()
        m_contextCollection.addHandler(regContext)
    }
    private fun addPrivilegiesHandler()
    {
        val privcontx = ContextHandler("/priv")
        privcontx.handler = privilegiesHandlers()
        m_contextCollection.addHandler(privcontx)
    }
    /*private  fun enableSSL()
    {
        val s = SslContextFactory.Server();
        s.setKeyStorePath("C:\\wacs\\crt\\antebeot.ru-key.pem");
        // https://www.eclipse.org/jetty/documentation/jetty-11/programming-guide/index.html
        // https://stackoverflow.com/questions/42675033/how-to-build-a-sslsocketfactory-from-pem-certificate-and-key-without-converting
        // very big things that can be implemented though simple reverse proxy of apache as example.
    }*/
    init {
        server.addConnector(connector)
        EnableHandlers()
        server.setHandler(m_contextCollection);

        server.start()
        CryptoCoins.initCoins() //


    }

}