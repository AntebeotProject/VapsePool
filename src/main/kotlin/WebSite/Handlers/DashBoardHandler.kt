package org.antibiotic.pool.main.WebSite.Handlers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.antibiotic.pool.main.PoolServer.DB
import org.antibiotic.pool.main.PoolServer.MinerData
import org.antibiotic.pool.main.PoolServer.PoolServer
import java.util.*


class DashBoardHandler : AbstractHandler() {
    override fun handle(target: String?, baseRequest: Request, request: HttpServletRequest?, response: HttpServletResponse?) {
        baseRequest.setHandled(true)
        val listOfMiners = buildString{
            append("<ul>")
            MinerData.currentMiners.forEach() {
                var balances = ""
                DB.getLoginBalance(it.Login)?.forEach {
                    balances += buildString {
                        append(it.value.CoinName)
                        append(":")
                        append(it.value.balance)
                        append("<br/>")
                    }.toString()
                }
                append("<li>${it.Login} isHTTP: ${it.isHTTPMiner}, lastActiveTime: ${Date(it.LastActiveTimeSec * 1000).toString()} Balance: ${ balances }</li>")
            }
            append("</ul>")
        }
        response!!.setStatus(200);
        response!!.setContentType("text/html; charset=UTF-8");
        response.getWriter().print("" +
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "  <title>ProstoPoolDashboard</title>" +
                "</head>" +
                "<body>" +
                "  <p>ServerUptime (days:hour:minutes:seconds): ${PoolServer.getServerUptimeSeconds() / 60 / 60 / 24}:${PoolServer.getServerUptimeSeconds() / 60 / 60 % 24}:${PoolServer.getServerUptimeSeconds() / 60 % 60}:${PoolServer.getServerUptimeSeconds() % 60}</p>" +
                "  <p>Current workers on server: ${PoolServer.currentWorkers()}</p>" +
                "  <p>Shares on all time: ${PoolServer.sharesUptime}</p>" +
                "  <p>List Of Workers: ${listOfMiners}</p>" +
                "</body>" +
                "</html>" +
                "");
    }
}