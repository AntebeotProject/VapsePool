package org.antibiotic.pool.main.WebSite.Handlers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.antibiotic.pool.main.CryptoCurrencies.CryptoCoins
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.antibiotic.pool.main.PoolServer.DB
import org.antibiotic.pool.main.WebSite.JSONBooleanAnswer
import org.antibiotic.pool.main.WebSite.JettyServer
import java.math.BigDecimal

class TradeHandler : AbstractHandler() {
    override fun handle(target: String?, baseRequest: Request, request: HttpServletRequest?, response: HttpServletResponse?) {
        baseRequest.setHandled(true)
        response!!.setContentType("application/json; charset=UTF-8");

        val session_raw = JettyServer.Users.getSession(response,baseRequest)
        if (session_raw == null) return response.getWriter().print(Json.encodeToString(JSONBooleanAnswer(false, "undefined session")))
        response!!.setStatus(200);

        val session = DB.getSession(session_raw)
        val r = JettyServer.Users.getBySession(session_raw, response) // returns UserData data class

        if (r == null || session == null) {
            return
        }

        val own = r.Login
       // for(not in notifications)
       // {
       //     println(not)
       // }
        val doings = request?.getParameter("w")
        val rValue = when (doings) {
            "addOrderToSellCoin2Coin" -> {
                try {
                    val toSellName = request.getParameter("toSellName") // coin to sell name
                    val toSellPrice = request.getParameter("toSellPrice") // coin to sell price
                    // we can to put the checks to constructor
                    val toSellLMin = request.getParameter("toSellLimitMin")
                    val toSellLMax = request.getParameter("toSellLimitMax")

                    val toS =
                        DB.toSellStruct(toSellName, toSellPrice, lmin = toSellLMin, lmax = toSellLMax)

                    val toBuyName = request.getParameter("toBuyName") // coin for buy name
                    val toBuyPrice = request.getParameter("toBuyPrice") // coin for buy price

                    val toBuyLMin = request.getParameter("toBuyLimitMin")
                    val toBuyLMax = request.getParameter("toBuyLimitMax")

                    val toB = DB.toSellStruct(toBuyName, toBuyPrice, lmin = toBuyLMin, lmax = toBuyLMax)

                    val tIsBuyer = request.getParameter("tIsBuyer").toBoolean() // trader is buyer or seller?
                    // /exchange/?w=addOrderToSellCoin2Coin&toSellName=gostcoin&toSellPrice=1&toSellLimitMin=1&toSellLimitMax=1&toBuyName=bitcoin&toBuyPrice=0.0000028&toBuyLimitMin=1&toBuyLimitMax=1&tIsBuyer=true&msg=GostcoinNaBitcoin
                    println("tISBuyer: $tIsBuyer")
                    val ordMsg = request.getParameter("msg").toString()
                    val usrBalance =
                        if (tIsBuyer) DB.getLoginBalance(session.owner)?.get(toSellName)?.balance?.toBigDecimal()
                        else DB.getLoginBalance(session.owner)?.get(toBuyName)?.balance?.toBigDecimal()
                    if (usrBalance == null || usrBalance < toSellLMax.toBigDecimal() || usrBalance < toSellLMin.toBigDecimal()) {
                        println(usrBalance)
                        println(usrBalance!! < toSellLMax.toBigDecimal())
                        println(usrBalance!! < toSellLMin.toBigDecimal())
                        throw DB.notAllowedOrder("user balance is smaller than limit")
                    }
                    DB.addOrder(
                        session.owner,
                        toSell = toS,
                        toBuy = toB,
                        orderMSG = ordMsg,
                        isFiat2CoinTrade = false,
                        isCoin2CoinTrade = true,
                        ownerIsBuyer = tIsBuyer
                    )
                    return response.writer.print(Json.encodeToString(JSONBooleanAnswer(true, "order was created")))
                } catch (e: DB.notAllowedOrder) {
                    response.writer.print(Json.encodeToString(JSONBooleanAnswer(false, e.toString())))
                }
            }
            "getOrders" ->
            {
                // /exchange/?w=getOrders&a=true
                // /exchange/?w=getOrders&a=false
                // by default false
                val a = request.getParameter("a").toBoolean()
                val orders = DB.getOrdersByActivity(a)
                return response.writer.print(Json.encodeToString(orders))
            }
            "getOrderByName" ->
            {
                // /exchange/?w=getOrderByName&who=testusername
                val who = request.getParameter("who")
                val orders = DB.getOrdersByOwner(who)
                return response.writer.print(Json.encodeToString(orders))
            }
            "removeMyOrderByID" ->
            {
                // /exchange/?w=removeMyOrderByID&id=63cebdcb77a1a83abb3f7d9a
                val id = request.getParameter("id")
                DB.remOrderByIDAndOwner(id, session.owner)
                return response.writer.print(Json.encodeToString(JSONBooleanAnswer(true, "if it is was your order you delete it")))
            }
            "removeOrderByID" ->
            {
                // /exchange/?w=removeOrderByID&id=63cebdd377a1a83abb3f7d9d
                // TODO: privileged
                val id = request.getParameter("id")
                DB.remOrder(id)
                return response.writer.print(Json.encodeToString(JSONBooleanAnswer(true, "access is allowed. order was drop")))
            }
            "changeActiveOrder" ->
            {
                // /exchange/?w=changeActiveOrder&id=63cebdd377a1a83abb3f7d9d&s=true
                val id = request.getParameter("id")
                val s = request.getParameter("s").toBoolean()
                DB.changeOrderActivityByIdAndOwner(id, session.owner, s)
                return response.writer.print(Json.encodeToString(JSONBooleanAnswer(s, "Active was changed to $s")))
            }
            else -> TODO("not implemented yet ")// Json{encodeDefaults=true}.encodeToString(notifications)
        }
        //response.getWriter().print(rValue)
    }
}
