package org.antibiotic.pool.main.WebSite.Handlers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.antibiotic.pool.main.DB.*
import org.antibiotic.pool.main.DB.trade.Companion.comissionPercent
import org.antibiotic.pool.main.DB.userPrivilegies.Companion.checkUserPrivilegies
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.antibiotic.pool.main.WebSite.JSONBooleanAnswer
import org.antibiotic.pool.main.WebSite.JettyServer
import org.antibiotic.pool.main.WebSite.delHTML
import org.antibiotic.pool.main.i18n.i18n

class privilegiesHandlers : AbstractHandler() {
    companion object
    {

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

                "getbalance" -> {
                    if (!checkUserPrivilegies(session.owner, userPrivilegies.privilegies.GET_USER_BALANCE))
                        return response.writer.print(Json.encodeToString(JSONBooleanAnswer(false, uLanguage.getString("notprivilegied"))))
                    val ac = request!!.getParameter("ac")
                    response.getWriter().print(DB.getLoginBalance(ac).toString())
                }
                "getReviewsByAbout" -> {
                    if (!checkUserPrivilegies(session.owner, userPrivilegies.privilegies.SHOW_REVIEWS))
                        return response.writer.print(Json.encodeToString(JSONBooleanAnswer(false, uLanguage.getString("notprivilegied"))))

                    // /exchange/?w=getReviewsByAbout&who=testusername_
                    val who = request.getParameter("who")
                    val (offset,lim) = JettyServer.getOffsetLimit(baseRequest)
                    return response.writer.print(Json.encodeToString(review.getReviewsByAbout(who, lim = lim, skip = offset)))
                }
                "getReviewsByReviewer" -> {
                    if (!checkUserPrivilegies(session.owner, userPrivilegies.privilegies.SHOW_REVIEWS))
                        return response.writer.print(Json.encodeToString(JSONBooleanAnswer(false, uLanguage.getString("notprivilegied"))))

                    // /exchange/?w=getReviewsByReviewer&who=testusername
                    val who = request.getParameter("who")
                    val (offset,lim) = JettyServer.getOffsetLimit(baseRequest)
                    return response.writer.print(Json.encodeToString(review.getReviewsByReviewer(who, skip = offset, lim = lim)))
                }
                "getOrderByName" -> {
                    if (!checkUserPrivilegies(session.owner, userPrivilegies.privilegies.SHOW_REVIEWS))
                        return response.writer.print(Json.encodeToString(JSONBooleanAnswer(false, uLanguage.getString("notprivilegied"))))

                    // /exchange/?w=getOrderByName&who=testusername
                    val who = request.getParameter("who")
                    val orders = order.getOrdersByOwner(who)
                    return response.writer.print(Json.encodeToString(orders))
                }
                "removeOrderByID" -> {
                    if (!checkUserPrivilegies(session.owner, userPrivilegies.privilegies.EDIT_ORDERS))
                        return response.writer.print(Json.encodeToString(JSONBooleanAnswer(false, uLanguage.getString("notprivilegied"))))
                    // /exchange/?w=removeOrderByID&id=63cebdd377a1a83abb3f7d9d
                    // TODO: privileged
                    val id = request.getParameter("id")
                    order.remOrder(id)
                    return response.writer.print(
                        Json.encodeToString(
                            JSONBooleanAnswer(
                                true,
                                uLanguage.getString("accessIsAllowed")
                            )
                        )
                    )
                }
                "changeUserPriv" -> {
                    if (!checkUserPrivilegies(session.owner, userPrivilegies.privilegies.EDIT_USER_PRIVILEGIES))
                        return response.writer.print(Json.encodeToString(JSONBooleanAnswer(false, uLanguage.getString("notprivilegied"))))
                    val who = request.getParameter("who")
                    val lvl = request.getParameter("lvl").toInt()
                    userPrivilegies.changeUserPrivilegies(who, lvl)
                    Json.encodeToString(JSONBooleanAnswer(true))
                }
                else -> TODO("not fully implemented yet ")// Json{encodeDefaults=true}.encodeToString(notifications)
            }
            //response.getWriter().print(rValue)
        } catch(e: notAllowedOrder) {
            return response.writer.print(Json.encodeToString(JSONBooleanAnswer(false, e.toString().split(":")[1])))
        } catch(e: Exception) {
            return response.writer.print(Json.encodeToString(JSONBooleanAnswer(false, "Not trade exc: " + e.toString())))
        }
    }
}