package org.antibiotic.pool.main.WebSite

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.antibiotic.pool.main.PoolServer.MinerData
import org.antibiotic.pool.main.PoolServer.PoolServer
import org.antibiotic.pool.main.PoolServer.toHexString
import org.eclipse.jetty.server.Request
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.lang.StringBuilder
import java.util.Base64
import kotlin.concurrent.thread
import kotlin.random.Random

const val DotSize = 5
const val DotsCountRand = 25
const val LinesCountRand = 25
const val defCaptchaCookie = "captcha_id"
// typealias CaptchaID = Pair<String, Long> // text ID and timemilliseconds
class Captcha(width: Int, height: Int, type: Int = BufferedImage.TYPE_INT_RGB) {
    companion object {
        data class CaptchaData(val id: String, val answer: String, val timemillis: Long = System.currentTimeMillis())
        const val maxCaptchaLifeTimeMillis = 60_000L * 5 // 1 minute
        const val maxCaptches = 150; // in 5 minutes. more is can be flood to server for now. in future maybe nope
        private var floodDetected = false
        fun serverOnFloodThoughCaptcha() = floodDetected
        private val lastCatches = mutableSetOf<CaptchaData>()
        private var threadForCleanWorks = false
        fun checkCaptcha(par: String, baseRequest: Request, request: HttpServletRequest?, response: HttpServletResponse, delCaptchaAfter: Boolean = true): Boolean {
            try {
                val cookie_id = JettyServer.Cookie.getCookie(defCaptchaCookie, baseRequest, encrypt = false)
                if (cookie_id == null) JettyServer.sendJSONAnswer(
                    false,
                    "Not found cookie_id. ask before though ?w=get",
                    response
                )
                val answer = request?.getParameter(par)
                val isCorrect =
                    Captcha.isCaptchaCorrect(cookie_id!!, answer = answer!!, delCaptchaAfter = delCaptchaAfter)
                return isCorrect
            } catch(_: Exception)
            {
                JettyServer.sendJSONAnswer(
                    false,
                    "Not found cookie_id. ask before though ?w=get",
                    response
                )
                return false;
            }
        }
        fun runThreadToCleanLastCaptches() {
            if (threadForCleanWorks) return
            threadForCleanWorks = true
            thread {
                while (threadForCleanWorks) {
                    synchronized(lastCatches)
                    {
                        lastCatches.removeIf({ System.currentTimeMillis() - it.timemillis > maxCaptchaLifeTimeMillis })
                    }
                    Thread.sleep(maxCaptchaLifeTimeMillis)
                }
            }
        }
        fun stopThreadForClean() {
            threadForCleanWorks = false
        }
        fun addLastCaptcha(id: String, answer: String) {
            synchronized(lastCatches)
            {
                if (lastCatches.size + 1 >= maxCaptches)
                {
                    JettyServer.pWarning("like to flood on server though captcha")
                    floodDetected = true
                } else floodDetected = false
                lastCatches.add(CaptchaData(id, answer))
            }
        }
        fun isCaptchaCorrect(id: String, answer: String, delCaptchaAfter: Boolean): Boolean
        {
            var found = false
            lastCatches.forEach() {
                if (it.id == id) {
                    found = answer.equals(it.answer)// it.answer == answer
                    return@forEach
                }
            }
            if (found && delCaptchaAfter)
            {
                delCaptchaById(id)
            }
            return found
        }
        fun delCaptchaById(id: String) {
            synchronized(lastCatches)
            {
                lastCatches.removeIf() {it.id == id}
            }
        }
        fun genCaptchaID(bS: Int = 6) = String(Base64.getEncoder().encode(Random.nextBytes(bS)))
    }
    val m_width = width
    val m_height = height
    val m_type = type
    val m_bufferedImage: BufferedImage = BufferedImage(width, height, type)
    val m_g2d = m_bufferedImage.createGraphics()
    // get() / set() ? no need
    fun RandText(s: Int = 9): String {
        var r = StringBuilder()
        for (`_` in 0..9) {
            if (Random.nextBoolean())  r .append( Random.nextInt('a'.code, 'z'.code).toChar() )
            else r .append(Random.nextInt('A'.code, 'Z'.code).toChar())
        }
        return r.toString()
    }
    fun getRandColor() = Color(Random.nextInt(0,255), Random.nextInt(0,255), Random.nextInt(0,255))
    fun drawRandomLines(count: Int) {
        for (`_` in 0..count) {
            m_g2d.color = getRandColor()
            m_g2d.drawLine(Random.nextInt(0, m_width),Random.nextInt(0, m_height), Random.nextInt(0, m_width), Random.nextInt(0, m_height))
        }
    }

    fun drawRandomDots(count: Int) {
        for (`_` in 0..count) {
            m_g2d.color = Color(Random.nextInt(0,255), Random.nextInt(0,255), Random.nextInt(0,255))
            m_g2d.drawRect(Random.nextInt(0, m_width),Random.nextInt(0, m_height), DotSize, DotSize)
        }
    }
    fun drawTextLight(text: String)
    {
        drawRandomDots(DotsCountRand)
        drawRandomLines(LinesCountRand)
        val allowedColor = listOf(Color.WHITE, Color.CYAN, Color.GREEN, Color.MAGENTA)
        m_g2d.setColor(Color.WHITE)
        m_g2d.font = Font("Arial", Font.BOLD, 20)
        // println("draw text $text")
        var pointX = 15
        var pointY = m_height / 2
        for(l in text) {
            if (pointX > m_width - 20) {
                pointY += if (Random.nextBoolean()) Random.nextInt(20,25) else Random.nextInt(-25, -20)
                pointX = 15
            }
            m_g2d.setColor( allowedColor.random() )//getRandColor())
            m_g2d.drawString(l.toString(), pointX, pointY )
            pointX += Random.nextInt(20, 25)
            // pointY += kotlin.random.Random.nextInt(20,25)
        }
        // g2d.drawRect(0,0, m_width, m_height)
        m_g2d.drawLine(0,0,m_width, m_height)
        m_g2d.dispose()
    }
    fun getBuffer() = m_bufferedImage
}