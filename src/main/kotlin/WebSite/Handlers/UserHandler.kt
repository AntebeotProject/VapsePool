package ru.xmagi.pool.main.WebSite.Handlers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import ru.xmagi.pool.main.CryptoCurrencies.CryptoCoins
import ru.xmagi.pool.main.PoolServer.DB
import ru.xmagi.pool.main.WebSite.JSONBooleanAnswer
import ru.xmagi.pool.main.WebSite.JettyServer

// https://kotlinlang.org/docs/nested-classes.html not need internal for now
class UserHandler : AbstractHandler() {
    override fun handle(target: String?, baseRequest: Request, request: HttpServletRequest?, response: HttpServletResponse?) {
        baseRequest.setHandled(true)

        response!!.setContentType("application/json; charset=UTF-8");
        println("get username session")
        val UsernameSession = JettyServer.Cookie.getCookie(JettyServer.Cookie.userSessionCookieName, baseRequest)

        val _session = request?.getParameter("session")
        var session_raw = ""
        println("Username session like to got now check")
        if (UsernameSession == null && _session == null) {
            response!!.setStatus(401);
            return response.getWriter().print(Json.encodeToString(JSONBooleanAnswer(false, "undefined session")))
        } else if (_session != null) {
            session_raw = _session
        } else {
            session_raw = UsernameSession!!
        }
        response!!.setStatus(200);
        println("Search userdata by session $session_raw")
        val session = DB.getSession(session_raw)
        val r = JettyServer.Users.getBySession(session_raw, response) // returns UserData data class
        if (r == null || session == null) {
            return //response.getWriter().print(Json.encodeToString(JSONBooleanAnswer(false, "Session not found yet")))
        }
        val doings = request?.getParameter("w")
        val rValue = when (doings) {
            "genAddress" -> {
                val coin = request?.getParameter("cryptocoin")
                val rpc = CryptoCoins.coins[coin]
                if (rpc == null && coin == null) {
                    Json{encodeDefaults=true}.encodeToString(JSONBooleanAnswer(false, "Not found cryptocurence. use /api?w=getallowcoins"))
                } else {
                    val nadr = JettyServer.Users.genNewAddrForUser(session.owner, coin, search_unused = true)
                    Json.encodeToString(JSONBooleanAnswer(true, "Your new address is ${nadr}"))
                }
            }
            else -> Json{encodeDefaults=true}.encodeToString(r)
        }
        // println("return value $rValue")
        response.getWriter().print(rValue)
    }
}
