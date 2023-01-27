package org.antibiotic.pool.main.WebSite.Handlers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.antibiotic.pool.main.CryptoCurrencies.CryptoCoins
import org.antibiotic.pool.main.CryptoCurrencies.ElectrumRPC
import org.antibiotic.pool.main.DB.DB
import org.antibiotic.pool.main.DB.allowedLangauges
import org.antibiotic.pool.main.PoolServer.RPC
import org.antibiotic.pool.main.PoolServer.deleteSquares
import org.antibiotic.pool.main.WebSite.Captcha
import org.antibiotic.pool.main.WebSite.JSONBooleanAnswer
import org.antibiotic.pool.main.WebSite.JettyServer

class RESTHandler : AbstractHandler() {

    override fun handle(target: String?, baseRequest: Request, request: HttpServletRequest?, response: HttpServletResponse?) {
        baseRequest.setHandled(true)
        // Implement the REST APIs.
        response!!.setStatus(200);
        response!!.setContentType("text/html; charset=UTF-8");
        val uLanguage = JettyServer.Users.language.getLangWithoutSession(request)
        when(request!!.getParameter("w")) {
            "getbalance" -> {
                val ac = request!!.getParameter("ac")
                response.getWriter().print(DB.getLoginBalance(ac).toString())
            }
            "getallowcoins" ->
            {
                response!!.setContentType("application/json; charset=UTF-8");
                val allowed_coin_list = mutableListOf<String>()
                for(c in CryptoCoins.coins) {
                    allowed_coin_list.add(c.key)
                }
                response.getWriter().print(Json.encodeToString(allowed_coin_list))
                // TODO("Not implemented")
            }
            "getSupportsLanguage" ->
            {
                // println(allowedLangauges)
                response.writer.print(Json.encodeToString(allowedLangauges))
            }
            "output" -> {
                synchronized(DB) {
                    response!!.setContentType("application/json; charset=UTF-8");
                    val acc = request!!.getParameter("acc")
                    val pass = request!!.getParameter("pass")
                    val oAdr = request!!.getParameter("oAdr")
                    val cMoney = request!!.getParameter("cMoney")
                    val coinname = request!!.getParameter("coinname") ?: "gostcoin"
                    val answ = Captcha.checkCaptcha("captchaText", baseRequest, request, response, delCaptchaAfter = true)
                    if (answ == false) {
                        return JettyServer.sendJSONAnswer(false, uLanguage.getString("notCorrectCaptcha"), response)
                    }
                    if (acc == null || pass == null) {
                        return response.getWriter().print(Json.encodeToString(JSONBooleanAnswer(false, uLanguage.getString("uWillAuthEverytime"))))
                    }
                    if (DB.checkUserPassword(acc, pass!!) != true) {
                        return response.getWriter().print(Json.encodeToString(JSONBooleanAnswer(false, uLanguage.getString("notCorrectLoginOrPass"))))
                    }
                    JettyServer.Users.cryptocoins.sendMoney(acc = acc, oAdr = oAdr, coinname = coinname, cMoney = cMoney, response)
                }
            }
            "BitcoinBroadcast" ->
            {
                if (CryptoCoins.coins.get("bitcoin") == null) {
                    return response.getWriter().print(Json.encodeToString(JSONBooleanAnswer(false, "Bitcoin disabled")))
                }
                (CryptoCoins.coins.get("bitcoin") as ElectrumRPC).broadcast(request.getParameter("tx"))
            }
            "ReserveGet" -> {
                val oStr = buildString {
                    for (coin in CryptoCoins.coins) {
                        append(coin.key + "\n")
                        append(coin.value.getbalance().toString().deleteSquares() + "\n")
                        if (coin.value.getisElectrum()) {
                            val eRPC = coin.value as ElectrumRPC

                        } else {
                            val bRPC = coin.value as RPC

                        }
                    }
                }
                response.getWriter().print("<pre>$oStr</pre>")
            }
            else -> {
                response!!.setStatus(404);
            }
        }
    }
}
