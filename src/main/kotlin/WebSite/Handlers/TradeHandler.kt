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
import org.antibiotic.pool.main.WebSite.delHTML
import java.math.BigDecimal

class TradeHandler : AbstractHandler() {
    override fun handle(target: String?, baseRequest: Request, request: HttpServletRequest?, response: HttpServletResponse?) {
        baseRequest.setHandled(true)
        response!!.setContentType("application/json; charset=UTF-8");

        val session_raw = JettyServer.Users.getSession(response, baseRequest)
        if (session_raw == null) return response.getWriter()
            .print(Json.encodeToString(JSONBooleanAnswer(false, "undefined session")))
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
        try {
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
                        //
                        // /exchange/?w=addOrderToSellCoin2Coin&toSellName=gostcoin&toSellPrice=0&toSellLimitMin=1&toSellLimitMax=1&toBuyName=bitcoin&toBuyPrice=0.0001&toBuyLimitMin=0&toBuyLimitMax=1&tIsBuyer=true&msg=GostcoinNaBitcoin
                        println("tISBuyer: $tIsBuyer")
                        val ordMsg = request.getParameter("msg").toString().delHTML()

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

                "getOrders" -> {
                    // /exchange/?w=getOrders&a=true
                    // /exchange/?w=getOrders&a=false
                    // by default false
                    val a = request.getParameter("a").toBoolean()
                    val orders = DB.getOrdersByActivity(a)
                    return response.writer.print(Json.encodeToString(orders))
                }

                "getOrderByName" -> {
                    // /exchange/?w=getOrderByName&who=testusername
                    val who = request.getParameter("who")
                    val orders = DB.getOrdersByOwner(who)
                    return response.writer.print(Json.encodeToString(orders))
                }

                "removeMyOrderByID" -> {
                    // /exchange/?w=removeMyOrderByID&id=63cebdcb77a1a83abb3f7d9a
                    val id = request.getParameter("id")
                    DB.remOrderByIDAndOwner(id, session.owner)
                    return response.writer.print(
                        Json.encodeToString(
                            JSONBooleanAnswer(
                                true,
                                "if it is was your order you delete it"
                            )
                        )
                    )
                }

                "removeOrderByID" -> {
                    // /exchange/?w=removeOrderByID&id=63cebdd377a1a83abb3f7d9d
                    // TODO: privileged
                    val id = request.getParameter("id")
                    DB.remOrder(id)
                    return response.writer.print(
                        Json.encodeToString(
                            JSONBooleanAnswer(
                                true,
                                "access is allowed. order was drop"
                            )
                        )
                    )
                }

                "changeActiveOrder" -> {
                    // /exchange/?w=changeActiveOrder&id=63cebdd377a1a83abb3f7d9d&s=true
                    val id = request.getParameter("id")
                    val s = request.getParameter("s").toBoolean()
                    DB.changeOrderActivityByIdAndOwner(id, session.owner, s)
                    return response.writer.print(Json.encodeToString(JSONBooleanAnswer(s, "Active was changed to $s")))
                }

                // DO TRADE

                "doTrade" -> {
                    // /exchange/?w=doTrade&count=1&id=63cee412d2fda05780b23c33
                    val buyer = session.owner
                    val count = request.getParameter("count")
                    val id = request.getParameter("id")
                    try {
                        val id_ = DB.doTrade(buyer, count, id)
                        response.writer.print(Json.encodeToString(JSONBooleanAnswer(true, "trade was done. id ${id_}")))
                    } catch (e: DB.notAllowedOrder) {
                        response.writer.print(Json.encodeToString(JSONBooleanAnswer(false, e.toString())))
                    }
                }
                "addReview" -> {
                        // /?w=addReview&id=63cee8febece8102ab4a0da5&text="All fine!"&isPositive=true
                        val id = request.getParameter("id").toString()
                        val text = request.getParameter("text").toString().delHTML()
                        val isPositive = request.getParameter("isPositive").toBoolean()
                        val trade = DB.getDoneTradeByID(id).first()
                        val about = if (session.owner != trade.seller) trade.seller else trade.buyer
                        DB.addReview(reviewer = session.owner, about = about, text = text, tradeID = trade.key, isPositive = isPositive)
                        return JettyServer.sendJSONAnswer(true, "review added", response)
                }
                "getReviewsByAbout" -> {
                    // /exchange/?w=getReviewsByAbout&who=testusername_
                    val who = request.getParameter("who")
                    return response.writer.print(Json.encodeToString(DB.getReviewsByAbout(who)))
                }
                "getReviewsByReviewer" -> {
                    // /exchange/?w=getReviewsByReviewer&who=testusername
                    val who = request.getParameter("who")
                    return response.writer.print(Json.encodeToString(DB.getReviewsByReviewer(who)))
                }
                "getMyReviews" -> {
                    // /exchange/?w=getMyReviews
                    return response.writer.print(Json.encodeToString(DB.getReviewsByWho(session.owner)))
                }
                "getMyDoneTrade" ->
                {
                    // /exchange/?w=getMyDoneTrade
                    return response.writer.print(Json.encodeToString(DB.getDoneTradeByBuyerOrSeller(session.owner)))
                }

                else -> TODO("not fully implemented yet ")// Json{encodeDefaults=true}.encodeToString(notifications)
            }
            //response.getWriter().print(rValue)
        } catch(e: DB.notAllowedOrder) {
            return response.writer.print(Json.encodeToString(JSONBooleanAnswer(false, e.toString())))
        } catch(e: Exception) {
            return response.writer.print(Json.encodeToString(JSONBooleanAnswer(false, "Not trade exc: " + e.toString())))
        }
    }
}
