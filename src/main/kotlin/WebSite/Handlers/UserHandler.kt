package org.antibiotic.pool.main.WebSite.Handlers

import io.github.g0dkar.qrcode.QRCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.antibiotic.pool.main.CryptoCurrencies.CryptoCoins
import org.antibiotic.pool.main.DB.*
import org.antibiotic.pool.main.WebSite.Email.mail_service
import org.antibiotic.pool.main.WebSite.JSONBooleanAnswer
import org.antibiotic.pool.main.WebSite.JettyServer
import org.antibiotic.pool.main.WebSite.JettyServer.Users.money.genAdr

// https://kotlinlang.org/docs/nested-classes.html not need internal for now
class UserHandler : AbstractHandler() {
    override fun handle(target: String?, baseRequest: Request, request: HttpServletRequest?, response: HttpServletResponse?) {
        baseRequest.setHandled(true)
        response!!.setContentType("application/json; charset=UTF-8");
        // println("get username session")
        val session_raw = JettyServer.Users.getSession(response,baseRequest)
        if (session_raw == null) return response.getWriter().print(Json.encodeToString(JSONBooleanAnswer(false, "undefined session")))
        response!!.setStatus(200);
        // println("Search userdata by session $session_raw")
        val session = DB.getSession(session_raw)
        val r = JettyServer.Users.getBySession(session_raw, response) // returns UserData data class
        if (r == null || session == null) {
            return //response.getWriter().print(Json.encodeToString(JSONBooleanAnswer(false, "Session not found yet")))
        }
        val uLanguage = JettyServer.Users.language.getLangByUser(session.owner)
        val doings = request?.getParameter("w")
        try {
            val rValue = when (doings) {
                "getowninput" -> {
                    val coin = request?.getParameter("cryptocoin")
                    Json.encodeToString(JsonPrimitive(DB.getLoginInputAddress(session.owner, coin!!)))
                }

                "genAddress" -> {
                    val coin = request?.getParameter("cryptocoin")
                    genAdr(coin = coin!!, uLanguage = uLanguage, owner = session.owner)
                }

                "updateSession" -> {
                    DB.updateSession(session_raw)
                    Json.encodeToString(JSONBooleanAnswer(true, uLanguage.getString("sessionWasUpdates")))
                }

                "changePassword" -> {
                    val new_pass = request.getParameter("new_pass")
                    val last_pass = request.getParameter("last_pass")
                    if (!JettyServer.Users.OTP.check(session.owner, baseRequest))
                    {
                        Json.encodeToString(JSONBooleanAnswer(false, uLanguage.getString("OTPNotCorrect")))
                    } else if (DB.checkUserPassword(session.owner, last_pass) == true) {
                        DB.changeUserPassword(session.owner, new_pass);
                        Json.encodeToString(JSONBooleanAnswer(true, uLanguage.getString("passwordChanged")))
                    } else {
                        Json.encodeToString(JSONBooleanAnswer(false, uLanguage.getString("notCorrectLastPass")))
                    }

                }

                "setEmail" -> {
                    if (userEmails.emailExistsForUser(session.owner))
                    {
                        val e = userEmails.getForUser(session.owner)!!
                        Json.encodeToString(JSONBooleanAnswer(false, String.format(uLanguage.getString("emailExistsForUser"), e.email)))
                    } else {
                        val new_email = request.getParameter("e")
                        if (mail_service.isValid(new_email)) {
                            Json.encodeToString((JSONBooleanAnswer(true)))
                        } else {
                            Json.encodeToString(JSONBooleanAnswer(false, uLanguage.getString("notCorrectEmailAddr")))
                        }
                        // userEmails.set()
                    }

                }
                "changeLanguage" -> {
                    val lang = request.getParameter("lang")
                    userLanguage.set(session.owner, lang)
                    Json.encodeToString(JSONBooleanAnswer(true))
                }
                "setOTP" -> {
                    val otp = request.getParameter("otp")
                    val code = request.getParameter("code")
                    userOTP.set(session.owner, otp, code)
                    Json.encodeToString((JSONBooleanAnswer(true)))
                }
                // ZSM6TF66FPZ7WX57VGJQHGP22CRJZ6TV
                "genOTP" -> {
                    val secret = userOTP.generateCode().toB32()
                    Json.encodeToString(secret)
                }
                "checkOTP" -> {
                    Json.encodeToString(userOTP.getCodeForUser(session.owner))
                }
                "GetQR" -> {
                    response!!.setContentType("image/png; charset=UTF-8");
                    val otp = request.getParameter("otp")
                    if (!userOTP.isCorrectOTP(otp)){
                        Json.encodeToString((JSONBooleanAnswer(false)))
                    }
                    val imageOut = response.outputStream
                    val b32secret = otp //userOTP.getForUser(session.owner)!!.b32secret
                    val qr_code = "otpauth://totp/dev@AntidoteExchange.ru:?secret=$b32secret&issuer=AntidoteExchange"
                    // val background = Colors.css("#e6e6e6")
                    //val foreground = Colors.css("#00d4ff")
                    // brightColor = background, darkColor = foreground
                    return QRCode(qr_code).render(cellSize = 5, margin = 0).writeImage(imageOut)
                }
                "outByOTP" -> {
                    if (userOTP.userOTPExistsForUser(session.owner) ) {
                        Json.encodeToString((JSONBooleanAnswer(false, uLanguage.getString("OTPNotExists"))))
                    }
                    else if (request.getParameter("code").equals(userOTP.getCodeForUser(session.owner))) {
                        val acc = session.owner
                        val oAdr = request!!.getParameter("oAdr")
                        val cMoney = request!!.getParameter("cMoney")
                        val coinname = request!!.getParameter("coinname") ?: "gostcoin"
                        return JettyServer.Users.cryptocoins.sendMoney(acc = acc, oAdr = oAdr, coinname = coinname, cMoney = cMoney, response)
                    } else {
                        Json.encodeToString((JSONBooleanAnswer(false, uLanguage.getString("OTPNotCorrect"))))
                    }
                }

                else -> Json { encodeDefaults = true }.encodeToString(r)
            }

            // println("return value $rValue")

            response.getWriter().print(rValue)
        } catch(e: Exception) {
            response.writer.print(Json.encodeToString( JSONBooleanAnswer(false, e.toString())))
        }
    }
}
