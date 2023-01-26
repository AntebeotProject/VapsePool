package org.antibiotic.pool.main.WebSite.Handlers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.antibiotic.pool.main.DB.DB
import org.antibiotic.pool.main.DB.userOTP
import org.antibiotic.pool.main.WebSite.Captcha
import org.antibiotic.pool.main.WebSite.JSONBooleanAnswer
import org.antibiotic.pool.main.WebSite.JettyServer

class SignInHandler : AbstractHandler() {
    override fun handle(target: String?, baseRequest: Request, request: HttpServletRequest?, response: HttpServletResponse?) {
        baseRequest.setHandled(true)
        val uLanguage = JettyServer.Users.language.getLangWithoutSession(request)
        synchronized(DB) {
            response!!.setStatus(200);
            response!!.setContentType("application/json; charset=UTF-8");

            val answ = Captcha.checkCaptcha("captchaText", baseRequest, request, response, delCaptchaAfter = true)
            if (answ == false) {
                return JettyServer.sendJSONAnswer(false, uLanguage.getString("notCorrectCaptcha"), response)
            }
            val workname = request!!.getParameter("workname")
            val workpass = request!!.getParameter("workpass")
            val otp_code = request!!.getParameter("otpcode")

            val answer =
                if (workname == null || workpass == null || !(workname.length > 0 && workpass.length > 0)) {
                    Json.encodeToString(JSONBooleanAnswer(false, uLanguage.getString("notCorrectLoginOrPass"))) // we can to use small size for passwords for some hacks
                } else if (DB.checkUserPassword(workname, workpass) != true) { //maybe weird logic.
                    Json.encodeToString(JSONBooleanAnswer(false, uLanguage.getString("notCorrectLoginOrPass")))
                } else if (userOTP.userOTPExistsForUser(workname) && !userOTP.getCodeForUser(workname).equals(otp_code)) {
                    Json.encodeToString(JSONBooleanAnswer(false, uLanguage.getString("OTPNotCorrect")))
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