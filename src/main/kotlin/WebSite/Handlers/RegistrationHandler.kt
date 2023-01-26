package org.antibiotic.pool.main.WebSite.Handlers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.antibiotic.pool.main.DB.DB
import org.antibiotic.pool.main.WebSite.Captcha
import org.antibiotic.pool.main.WebSite.JSONBooleanAnswer
import org.antibiotic.pool.main.WebSite.JettyServer
import org.antibiotic.pool.main.WebSite.JettyServer.Users.language.getLangWithoutSession
import org.antibiotic.pool.main.i18n.i18n

class RegistrationHandler : AbstractHandler() {
    // @Override
    // TODO: we can use JSON answer instead of HTTP?
    override fun handle(target: String?, baseRequest: Request, request: HttpServletRequest?, response: HttpServletResponse?) {
        baseRequest.setHandled(true)
        synchronized(DB) {
            response!!.setStatus(200);
            response!!.setContentType("application/json; charset=UTF-8");
            val answ = Captcha.checkCaptcha("captchaText", baseRequest, request, response, delCaptchaAfter = true)
            if (answ == false) {
                return JettyServer.sendJSONAnswer(false, "not correct captcha", response)
            }
            val uLanguage = getLangWithoutSession(request)
            val workname = request!!.getParameter("workname")

            if (workname == "_ANYUSER_" || !Regex("[A-Za-z0-9_а-яА-Я]{1,32}").matches(workname))
            {
                return JettyServer.sendJSONAnswer(false, uLanguage.getString("notCorrectWorkerName"), response)
            }
            val workpass = request!!.getParameter("workpass")
            val workpass2 = request!!.getParameter("workpass2")
            if (!workpass.equals(workpass2)) return JettyServer.sendJSONAnswer(false, uLanguage.getString("notCorrectWorkPass1And2"), response)
            val answer =
                if (workname == null || workpass == null || !(workname.length > 0 && workpass.length > 0)) {
                    Json.encodeToString(JSONBooleanAnswer(false, "Workpass and WorkLogin will be correct size. more than 1.")) //
                } else if (DB.checkUserPassword(workname) != null) { //maybe weird logic.
                    Json.encodeToString(JSONBooleanAnswer(false, uLanguage.getString("userRegisteredAlready")))
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