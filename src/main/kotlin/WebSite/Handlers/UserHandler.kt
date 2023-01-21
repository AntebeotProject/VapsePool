package org.antibiotic.pool.main.WebSite.Handlers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.antibiotic.pool.main.CryptoCurrencies.CryptoCoins
import org.antibiotic.pool.main.PoolServer.DB
import org.antibiotic.pool.main.WebSite.JSONBooleanAnswer
import org.antibiotic.pool.main.WebSite.JettyServer

// https://kotlinlang.org/docs/nested-classes.html not need internal for now
class UserHandler : AbstractHandler() {
    override fun handle(target: String?, baseRequest: Request, request: HttpServletRequest?, response: HttpServletResponse?) {
        baseRequest.setHandled(true)
        response!!.setContentType("application/json; charset=UTF-8");
        // println("get username session")
        val session_raw = JettyServer.Users.getSession(response,baseRequest)
        if (session_raw == null) return response.getWriter().print(Json.encodeToString(JSONBooleanAnswer(false, "undefined session")))
        response!!.setStatus(200);
        // println("Search userdata by session $session_raw")
        val session = DB.getSession(session_raw)
        val r = JettyServer.Users.getBySession(session_raw, response) // returns UserData data class
        if (r == null || session == null) {
            return //response.getWriter().print(Json.encodeToString(JSONBooleanAnswer(false, "Session not found yet")))
        }
        val doings = request?.getParameter("w")
        val rValue = when (doings) {
            "getowninput" ->
            {
                val coin = request?.getParameter("cryptocoin")
                Json.encodeToString(JsonPrimitive(DB.getLoginInputAddress(session.owner, coin!!)))
            }
            "genAddress" -> {
                val coin = request?.getParameter("cryptocoin")
                val rpc = CryptoCoins.coins.get(coin)
                if (rpc == null || coin == null) {
                    Json{encodeDefaults=true}.encodeToString(JSONBooleanAnswer(false, "Not found cryptocurence. use /api?w=getallowcoins"))
                } else {
                    if(DB.userHaveNotConfirmedTXOnCoinName(session.owner, coin)) {
                        Json.encodeToString(JSONBooleanAnswer(false, "You have not confirmed transactions for the address"))
                    } else {
                        val nadr = JettyServer.Users.genNewAddrForUser(session.owner, coin, search_unused = true)
                        Json.encodeToString(JSONBooleanAnswer(true, "Your new address is ${nadr}"))
                            .also { DB.setLoginInputAddress(session.owner, nadr!!, coin!!) }
                    }
                }
            }
            "updateSession" -> {
                DB.updateSession(session_raw)
                Json.encodeToString(JSONBooleanAnswer(true, "sessions was updated"))
            }
            else -> Json{encodeDefaults=true}.encodeToString(r)
        }

        // println("return value $rValue")

        response.getWriter().print(rValue)
    }
}
