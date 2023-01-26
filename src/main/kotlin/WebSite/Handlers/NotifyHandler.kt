package org.antibiotic.pool.main.WebSite.Handlers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.antibiotic.pool.main.DB.DB
import org.antibiotic.pool.main.DB.notification
import org.antibiotic.pool.main.WebSite.JSONBooleanAnswer
import org.antibiotic.pool.main.WebSite.JettyServer

class NotifyHandler : AbstractHandler() {
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
        val notifications = DB.getNotificationsByOwnerAndTimestamp(own, System.currentTimeMillis() - (24*60*60)) // less then one day
       // for(not in notifications)
       // {
       //     println(not)
       // }

        val defTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val doings = request?.getParameter("w")
        try {
            val rValue = when (doings) {
                "clean" -> {
                    // TODO: drop notification is will be vuln?
                    // DB.dropNotificationByTimeAndOwner(own, System.currentTimeMillis())
                    Json { encodeDefaults = true }.encodeToString(
                        JSONBooleanAnswer(
                            false,
                            "Is vulnerability. is disabled. use getByTime"
                        )
                    )
                }

                "getByTime" -> {
                    val (offset,lim) = JettyServer.getOffsetLimit(baseRequest)
                    val t = request.getParameter("t").toString().toLongOrNull() ?: System.currentTimeMillis()
                    Json { encodeDefaults = true }.encodeToString(notification.getNotificationsByOwnerAndTimestamp(own, t, lim = lim, offset = offset))
                }

                else -> {
                    val (offset,lim) = JettyServer.getOffsetLimit(baseRequest)
                    Json { encodeDefaults = true }.encodeToString(
                        notification.getNotificationsByOwnerAndTimestamp(
                            own,
                            defTime, lim = lim, offset = offset
                        )
                    )
                } // 1 day
            }

            response.getWriter().print(rValue)
        }catch (e: Exception)
        {
            response.getWriter().print(Json.encodeToString(JSONBooleanAnswer(false, e.toString())))
        }
    }
}
