package ru.xmagi.pool.main.WebSite

import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.lang.StringBuilder
import kotlin.random.Random

const val DotSize = 5
const val DotsCountRand = 250
const val LinesCountRand = 250
class Captcha(width: Int, height: Int, type: Int = BufferedImage.TYPE_INT_RGB) {
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
        println("draw text $text")
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