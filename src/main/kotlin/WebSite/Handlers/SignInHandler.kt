package ru.xmagi.pool.main.WebSite.Handlers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import ru.xmagi.pool.main.PoolServer.DB
import ru.xmagi.pool.main.WebSite.JSONBooleanAnswer
import ru.xmagi.pool.main.WebSite.JettyServer

class SignInHandler : AbstractHandler() {
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
                } else if (DB.checkUserPassword(workname, workpass) != true) { //maybe weird logic.
                    Json.encodeToString(JSONBooleanAnswer(false, "bad login or password"))
                } else {
                    // magic character can be changed I think to SEPARATOR?
                    // we can to use session instead from DB
                    // Cookie.addAuthCookie(workname, workpass, response)
                    JettyServer.Cookie.createUSession(workname, workpass, response)
                }
            response.getWriter().print(answer);
        }
    }
}