package org.antibiotic.pool.main.WebSite.Handlers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.antibiotic.pool.main.PoolServer.DB
import org.antibiotic.pool.main.WebSite.Captcha
import org.antibiotic.pool.main.WebSite.JSONBooleanAnswer
import org.antibiotic.pool.main.WebSite.JettyServer
import javax.imageio.ImageIO


class CaptchaHandler : AbstractHandler() {
    companion object {
        // val last_captchas =
    }
    override fun handle(target: String?, baseRequest: Request, request: HttpServletRequest?, response: HttpServletResponse) {
        baseRequest.setHandled(true)
        val doings = request?.getParameter("w")
        when(doings)
        {
            "get" ->
            {
                // TODO: 150 is can be 150 users. in future version is will be more than 150. change it in future version. increase the limit
                if (Captcha.serverOnFloodThoughCaptcha()) { return JettyServer.sendJSONAnswer(false, "Big requests in last time though captcha. wait a while please", response) }
                val mc = Captcha(200, 150)
                val text = mc.RandText()
                val id = Captcha.genCaptchaID()
                Captcha.addLastCaptcha(id, text)
                mc.drawTextLight(text)
                val befCaptcha = JettyServer.Cookie.getCookie("cookie_id", baseRequest, encrypt = false)
                if (befCaptcha != null) Captcha.delCaptchaById(befCaptcha) // can be vuln in some theory... but is hard for real
                JettyServer.Cookie.addCookie("cookie_id", id, response, encrypt = false)
                try {
                    response!!.setContentType("image/png; charset=UTF-8");
                    ImageIO.write(mc.m_bufferedImage, "png", response.outputStream)
                    // response.getWriter().print(mc.getBuffer().)
                } catch (e: Exception) {
                    response!!.setContentType("text; charset=UTF-8");
                    response.writer.print(e.toString())
                }
            }
            "answerTest" ->
            {
                val answ = Captcha.checkCaptcha("a", baseRequest, request, response, delCaptchaAfter = true)
                if (answ)
                {
                    response.writer.print("{\"result\":\"test pass\"}")
                } else {
                    response.writer.print("{\"result\":\"test not (or not correct answer) pass\"}")
                }
            }
        }
    } // end handle
}
