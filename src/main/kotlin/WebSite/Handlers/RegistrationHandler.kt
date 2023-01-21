package org.antibiotic.pool.main.WebSite.Handlers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.antibiotic.pool.main.PoolServer.DB
import org.antibiotic.pool.main.WebSite.JSONBooleanAnswer
import org.antibiotic.pool.main.WebSite.JettyServer

class RegistrationHandler : AbstractHandler() {
    // @Override
    // TODO: we can use JSON answer instead of HTTP?
    override fun handle(target: String?, baseRequest: Request, request: HttpServletRequest?, response: HttpServletResponse?) {
        baseRequest.setHandled(true)
        synchronized(DB) {
            response!!.setStatus(200);
            response!!.setContentType("application/json; charset=UTF-8");
            val workname = request!!.getParameter("workname")
            val workpass = request!!.getParameter("workpass")
            val answer =
                if (workname == null || workpass == null || !(workname.length > 0 && workpass.length > 0)) {
                    Json.encodeToString(JSONBooleanAnswer(false, "Workpass and WorkLogin will be correct size. more than 0."))
                } else if (DB.checkUserPassword(workname) != null) { //maybe weird logic.
                    Json.encodeToString(JSONBooleanAnswer(false, "The user already registered"))
                } else {
                    DB.addUser(workname, workpass)
                    // magic character can be changed I think to SEPARATOR?
                    // we can to use session instead from DB
                    // Cookie.addAuthCookie(workname, workpass, response)
                    JettyServer.Cookie.createUSession(workname, workpass, response)
                }
            response.getWriter().print(answer);
        }
    }
}