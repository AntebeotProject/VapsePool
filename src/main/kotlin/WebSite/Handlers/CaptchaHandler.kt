package ru.xmagi.pool.main.WebSite.Handlers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import ru.xmagi.pool.main.PoolServer.DB
import ru.xmagi.pool.main.WebSite.Captcha
import ru.xmagi.pool.main.WebSite.JSONBooleanAnswer
import ru.xmagi.pool.main.WebSite.JettyServer
import javax.imageio.ImageIO


class CaptchaHandler : AbstractHandler() {
    override fun handle(target: String?, baseRequest: Request, request: HttpServletRequest?, response: HttpServletResponse) {
        baseRequest.setHandled(true)


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
        val mc = Captcha(200, 150)
        val text = mc.RandText()
        mc.drawTextLight(text)
        try {
            response!!.setContentType("image/png; charset=UTF-8");
            ImageIO.write(mc.m_bufferedImage, "png", response.outputStream)
           // response.getWriter().print(mc.getBuffer().)
        } catch (e: Exception) {
            response!!.setContentType("text; charset=UTF-8");
            response.writer.print(e.toString())
        }
        val doings = request?.getParameter("w")
        //val rValue = when (doings) {
        //    else -> TODO("not implemented yet ")// Json{encodeDefaults=true}.encodeToString(notifications)
       // }
        //response.getWriter().print(rValue)
    }
}
