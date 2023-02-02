package org.antibiotic.pool.main.WebSite.Handlers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.antibiotic.pool.main.DB.*
import org.antibiotic.pool.main.DB.trade.Companion.comissionPercent
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.antibiotic.pool.main.WebSite.JSONBooleanAnswer
import org.antibiotic.pool.main.WebSite.JettyServer
import org.antibiotic.pool.main.WebSite.delHTML
import org.antibiotic.pool.main.i18n.i18n

class TradeHandler : AbstractHandler() {
    companion object
    {
        fun createOrder(tIsBuyer: Boolean,
                        owner: String, toSellName: String, toBuyName: String,
                        toSellLMax: String, toSellLMin: String,
                        uLanguage: i18n,
                        ordMsg: String
        ): String
        {
            TODO("nomarl implementation")

        }
    }
    override fun handle(target: String?, baseRequest: Request, request: HttpServletRequest?, response: HttpServletResponse?) {
        baseRequest.setHandled(true)
        response!!.setContentType("application/json; charset=UTF-8");
        val session_raw = JettyServer.Users.getSession(response, baseRequest)
        if (session_raw == null) return response.getWriter()
            .print(Json.encodeToString(JSONBooleanAnswer(false, "undefined session")))
        response!!.setStatus(200);

        val session = UserSession.getSession(session_raw)
        val r = JettyServer.Users.getBySession(session_raw, response) // returns UserData data class

        if (r == null || session == null) {
            return
        }

        val own = r.Login
        // for(not in notifications)
        // {
        //     println(not)
        // }
        val uLanguage = JettyServer.Users.language.getLangByUser(own)
        try {
            val doings = request?.getParameter("w")
            val rValue = when (doings) {
                "addOrderToSellCoin2Coin" -> {
                    try {
                        println("addOrder")
                        val toGiveName = request.getParameter("toGiveName")
                        val toGetName = request.getParameter("toGetName")
                        //
                        val Price = request.getParameter("Price")
                        val VolumeStart = request.getParameter("VolumeStart")
                        val VolumeMax = request.getParameter("VolumeMax")
                        // order.addOrder(ord.owner, ord.info, ord.orderMSG, ord.isCoin2CoinTrade, ord.isFiat2CoinTrade, ord.ownerIsBuyer)
                        val info = cryptoOrderInfo(toGiveName = toGiveName, toGetName = toGetName, priceRatio = Price, minVolume = VolumeStart, maxVolume = VolumeMax)
                        order.addOrder(session.owner, info = info, isCoin2CoinTrade = true)
                        return response.writer.print(Json.encodeToString(JSONBooleanAnswer(true, "order was create")))
                        // TODO("nomarl implementation")
                        
                    } catch (e: notAllowedOrder) {
                        response.writer.print(Json.encodeToString(JSONBooleanAnswer(false, e.toString())))
                    } catch (e: java.lang.NumberFormatException) {
                        response.writer.print(Json.encodeToString(JSONBooleanAnswer(false, "WrongNumber")))
                    }
                }

                "getOrders" -> {
                    // /exchange/?w=getOrders&a=true
                    // /exchange/?w=getOrders&a=false
                    // by default false
                    val a = request.getParameter("a").toBoolean()
                    val (offset, lim) = JettyServer.getOffsetLimit(baseRequest)
                    val orders = order.getOrdersByActivity(a, lim = lim, skip = offset)
                    return response.writer.print(Json.encodeToString(orders))
                }
                "getComissionPercent" ->
                {
                    return response.writer.print(Json.encodeToString(comissionPercent));
                }
                "getOwnOrders" ->
                {
                    val (offset, lim) = JettyServer.getOffsetLimit(baseRequest)
                    val r = order.getOrdersByOwner(session.owner, lim = lim, skip = offset)
                    return response.writer.print(Json.encodeToString(r))
                }
                "removeMyOrderByID" -> {
                    // /exchange/?w=removeMyOrderByID&id=63cebdcb77a1a83abb3f7d9a
                    val id = request.getParameter("id")
                    order.remOrderByIDAndOwner(id, session.owner)
                    return response.writer.print(
                        Json.encodeToString(
                            JSONBooleanAnswer(
                                true,
                                uLanguage.getString("orderDeletedIfItsUrs")
                            )
                        )
                    )
                }
                "changeActiveOrder" -> {
                    // /exchange/?w=changeActiveOrder&id=63cebdd377a1a83abb3f7d9d&s=true
                    val id = request.getParameter("id")
                    val s = request.getParameter("s").toBoolean()
                    order.changeOrderActivityByIdAndOwner(id, session.owner, s)
                    return response.writer.print(Json.encodeToString(JSONBooleanAnswer(s, String.format(uLanguage.getString("activeWasChanged"), s))))
                }

                // DO TRADE

                "doTrade" -> {
                    // /exchange/?w=doTrade&count=1&id=63cee412d2fda05780b23c33
                    val buyer = session.owner
                    val count = request.getParameter("count").trim()
                    val id = request.getParameter("id").trim()
                    try {
                        val id_ = trade.doTrade(buyer, count, id)
                        response.writer.print(Json.encodeToString(JSONBooleanAnswer(true, String.format(uLanguage.getString("tradeDoneID"), id_))))
                    } catch (e: notAllowedOrder) {
                        response.writer.print(Json.encodeToString(JSONBooleanAnswer(false, e.toString())))
                    }
                }
                "addReview" -> {
                        // /?w=addReview&id=63cee8febece8102ab4a0da5&text="All fine!"&isPositive=true

                        val id = request.getParameter("id").toString()
                        val text = request.getParameter("text").toString().delHTML()
                        val isPositive = request.getParameter("isPositive").toBoolean()
                        val trade = trade.getDoneTradeByID(id).first()
                        if (trade.isCrypto2Crypto) return JettyServer.sendJSONAnswer(false, "only fiat 2 fiat allows reviews", response)
                        if (!session.owner.equals(trade.seller) && !session.owner.equals(trade.buyer))
                        {
                            return JettyServer.sendJSONAnswer(true, uLanguage.getString("notAllowed"), response)
                        }
                        val about = if (session.owner != trade.seller) trade.seller else trade.buyer
                        review.addReview(reviewer = session.owner, about = about, text = text, tradeID = trade.key, isPositive = isPositive)
                        return JettyServer.sendJSONAnswer(true, uLanguage.getString("reviewAdded"), response)
                }
                "getTraderStats" -> {
                    val who = request.getParameter("who")
                    return response.writer.print( Json {encodeDefaults=true}.encodeToString(traderStats.getTraderStatsByOwner(who)))
                }
                "getMyReviews" -> {
                    // /exchange/?w=getMyReviews
                    val (offset,lim) = JettyServer.getOffsetLimit(baseRequest)
                    return response.writer.print(Json.encodeToString(review.getReviewsByWho(session.owner, skip = offset, lim = lim)))
                }
                "getMyDoneTrade" ->
                {
                    // /exchange/?w=getMyDoneTrade
                    val (offset,lim) = JettyServer.getOffsetLimit(baseRequest)
                    return response.writer.print(Json.encodeToString(trade.getDoneTradeByBuyerOrSeller(session.owner, skip = offset, lim = lim)))
                }
                "getLastDoneTrades" ->
                {
                    val (offset,lim) = JettyServer.getOffsetLimit(baseRequest)
                    return response.writer.print(Json.encodeToString(trade.getLastDoneTrades(skip = offset, lim = lim)))
                }
                else -> {
                    return response.writer.print(Json.encodeToString(JSONBooleanAnswer(true)))
                }
            }
            //response.getWriter().print(rValue)
        } catch(e: notAllowedOrder) {
            return response.writer.print(Json.encodeToString(JSONBooleanAnswer(false, e.toString().split(":")[1])))
        } catch(e: Exception) {
            System.out.println(e.toString())
            return response.writer.print(Json.encodeToString(JSONBooleanAnswer(false, "Not trade exc: " + e.toString())))
        }
    }
}

/*
OLD CODE
 "addOrderToSellCoin2Coin" -> {
                    try {
                        println("addOrder")
                        val toSellName = request.getParameter("toSellName").trimIndent() // coin to sell name
                        val toSellPrice = request.getParameter("toSellPrice").trimIndent() // coin to sell price
                        // we can to put the checks to constructor
                        val toSellLMin = request.getParameter("toSellLimitMin").trimIndent()
                        val toSellLMax = request.getParameter("toSellLimitMax").trimIndent()
                        println("'$toSellPrice', $toSellName , $toSellLMin , $toSellLMax")
                        val toS =
                            toSellStruct(toSellName, toSellPrice, lmin = toSellLMin, lmax = toSellLMax)
                        println("Struct created")
                        val toBuyName = request.getParameter("toBuyName").trimIndent() // coin for buy name
                        val toBuyPrice = request.getParameter("toBuyPrice").trimIndent() // coin for buy price

                        val toBuyLMin = request.getParameter("toBuyLimitMin").trimIndent()
                        val toBuyLMax = request.getParameter("toBuyLimitMax").trimIndent()

                        val toB = toSellStruct(toBuyName, toBuyPrice, lmin = toBuyLMin, lmax = toBuyLMax)
                        println("Struct 2 created")
                        val tIsBuyer = request.getParameter("tIsBuyer").toBoolean() // trader is buyer or seller?
                        //
                        // /exchange/?w=addOrderToSellCoin2Coin&toSellName=gostcoin&toSellPrice=0&toSellLimitMin=1&toSellLimitMax=1&toBuyName=bitcoin&toBuyPrice=0.0001&toBuyLimitMin=0&toBuyLimitMax=1&tIsBuyer=true&msg=GostcoinNaBitcoin
                        println("tISBuyer: $tIsBuyer")
                        val ordMsg = request.getParameter("msg").toString().delHTML()

                        val usrBalance =
                            if (!tIsBuyer) UserCoinBalance.getLoginBalance(session.owner)
                                ?.get(toSellName)?.balance?.toBigDecimal()
                            else UserCoinBalance.getLoginBalance(session.owner)?.get(toBuyName)?.balance?.toBigDecimal()
                        if (usrBalance == null || usrBalance < toSellLMax.toBigDecimal() || usrBalance < toSellLMin.toBigDecimal()) {
                            println(usrBalance)
                            println(usrBalance!! < toSellLMax.toBigDecimal())
                            println(usrBalance!! < toSellLMin.toBigDecimal())
                            throw notAllowedOrder(uLanguage.getString("uBalanceSmallerThanLimit"))
                        }
                        order.addOrder(
                            session.owner,
                            toSell = toS,
                            toBuy = toB,
                            orderMSG = ordMsg,
                            isFiat2CoinTrade = false,
                            isCoin2CoinTrade = true,
                            ownerIsBuyer = tIsBuyer
                        )
                        return response.writer.print(
                            Json.encodeToString(
                                JSONBooleanAnswer(
                                    true,
                                    uLanguage.getString("orderWasCreated")
                                )
                            )
                        )
                    } catch (e: notAllowedOrder) {
                        response.writer.print(Json.encodeToString(JSONBooleanAnswer(false, e.toString())))
                    } catch (e: java.lang.NumberFormatException) {
                        response.writer.print(Json.encodeToString(JSONBooleanAnswer(false, "WrongNumber")))
                    }
                }
 */