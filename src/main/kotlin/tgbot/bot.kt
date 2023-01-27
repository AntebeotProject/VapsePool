package org.antibiotic.pool.main.tgbot

import com.google.common.io.BaseEncoding
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.InlineQuery
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.*
import com.pengrad.telegrambot.request.AnswerInlineQuery
import com.pengrad.telegrambot.request.GetUpdates
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.request.SendPhoto
import io.github.g0dkar.qrcode.QRCode
import jakarta.servlet.http.HttpServletResponse
import org.antibiotic.pool.main.CryptoCurrencies.CryptoCoins
import org.antibiotic.pool.main.DB.*
import org.antibiotic.pool.main.PoolServer.RPC
import org.antibiotic.pool.main.WebSite.Captcha
import org.antibiotic.pool.main.WebSite.JettyServer
import org.antibiotic.pool.main.i18n.i18n
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import kotlin.random.Random



internal object prostaVapseTelegaBotSet {
    const val defConfigPath = "local.config"
}

/**
 * readUpdates - проверяет обновления из телеграмма. далее вызывает внутренние приватные методы.
 * пустой конструктор использует данные из local.config
 */
class prostaVapseTelegaBot {
    private var mBot: TelegramBot? = null
    private var mProp: Properties? = null
    private var mLastID = 0
    private fun initBot(token: String) {
        if (mBot == null) mProp = Properties()
        mBot = TelegramBot(token) //.Builder(token).debug().build();;
        mProp!!.setProperty("BOT_TOKEN", token)
        val _lastUpdateID = mProp!!.getProperty("lastUpdateID")
        val _lastID = Integer.valueOf(_lastUpdateID ?: "0")
        mLastID = _lastID
    }

    /**
     * обновить конфиг
     * @throws IOException if can't to write config
     */
    @Throws(IOException::class)
    fun updateProp() {
        mProp!!.setProperty("lastUpdateID", Integer.valueOf(mLastID).toString())
        mProp!!.store(FileOutputStream(filePropetry), null)
    }

    @get:Throws(IOException::class)
    private val filePropetry: File
        /**
         * читает конфиг
         * @return File
         * @throws IOException
         */
        private get() {
            val f = File(prostaVapseTelegaBotSet.defConfigPath)
            if (!f.exists()) {
                f.createNewFile()
            }
            return f
        }

    constructor() {
        mProp = Properties()
        val botToken: String
        botToken = try {
            // InputStream in = getClass().getResourceAsStream(prostaVapseTelegaBotSet.defConfigPath);
            mProp!!.load(FileInputStream(filePropetry))
            mProp!!.getProperty("BOT_TOKEN")
        } catch (e: IOException) {
            ""
        }
        initBot(botToken)
    }
    fun userInfoToDBName(uid: Long) = String.format("TELEGRAM USER %s", uid)

    fun getLangForUid(uid: Long): i18n {
        val own = userInfoToDBName(uid)
        val l = userLanguage.getForUser(own)?.language ?: defUserLanguage
        return i18n(locale = JettyServer.Users.language.geti18nByLocale(l))
    }

    fun setLanguage(update: Update, uid: Long) {
        val uLanguage = getLangForUid(uid)
        val msg = update.message()
        val replyKeyboardMarkup: Keyboard = ReplyKeyboardMarkup(allowedLangauges.toTypedArray())
            .oneTimeKeyboard(true) // optional
            .resizeKeyboard(true) // optional
            .selective(true) // optional
        val request = SendMessage(msg.chat().id(), String.format(uLanguage.getString("SelectLanguage"))).replyMarkup(replyKeyboardMarkup)
        mBot!!.execute(request)
    }
    fun registration(update: Update, uid: Long) {
        val uLanguage = getLangForUid(uid)
        val msg = update.message()
        val usr = userInfoToDBName(msg.chat().id())
        if (users.checkUserPassword(usr) != null) {
            val request = SendMessage(msg.chat().id(), String.format(uLanguage.getString("UAlreadyRegistered")) )
            mBot!!.execute(request)
        } else {
            val pass = BaseEncoding.base16().encode( Random.nextBytes(8) )
            users.addUser(usr, pass)
            val request = SendMessage(msg.chat().id(), String.format(uLanguage.getString("SaveTheData"), usr, pass) )
            mBot!!.execute(request)
        }
    }
    fun sendCaptcha(update: Update, uid: Long) {
        val mc = Captcha(200, 150)
        val text = mc.RandText()
        val id = Captcha.genCaptchaID()
        Captcha.addLastCaptcha(id, text)
        mc.drawTextLight(text)
        if (uLastCaptchas[uid.toString()] != null) Captcha.delCaptchaById(uLastCaptchas[uid.toString()]!!)
        uLastCaptchas[uid.toString()] = id
        val bOutStream = ByteArrayOutputStream()
        ImageIO.write(mc.m_bufferedImage, "png", bOutStream)
        val req = SendPhoto(uid, bOutStream.toByteArray())
        mBot!!.execute(req)
    }

    fun genNewOTP(update: Update, uid: Long) {
        val uLanguage = getLangForUid(uid)
        val uidForDB = userInfoToDBName(uid)
        if (userOTP.userOTPExistsForUser(uidForDB))
        {
            val request = SendMessage(update.message().chat().id(), uLanguage.getString("OTPExists")).replyMarkup(getMainMenuKeyboard())
            mBot!!.execute(request)
            return
        }
        val secret = userOTP.generateCode().toB32()
        val b32secret = secret //userOTP.getForUser(session.owner)!!.b32secret
        val qr_code = "otpauth://totp/dev@AntidoteExchange.ru:?secret=$b32secret&issuer=AntidoteExchange"
        val b = ByteArrayOutputStream()
        QRCode(qr_code).render(cellSize = 5, margin = 0).writeImage(b)
        val req = SendPhoto(uid, b.toByteArray())
        userTemporaryData[uidForDB] = b32secret
        mBot!!.execute(req)
    }


    fun dropOTP(update: Update, uid: Long)
    {
        val uLanguage = getLangForUid(uid)
        val uidForDB = userInfoToDBName(uid)
        if (!userOTP.userOTPExistsForUser(uidForDB)) {
            val request = SendMessage(update.message().chat().id(), uLanguage.getString("OTPNotExists")).replyMarkup(getMainMenuKeyboard())
            mBot!!.execute(request)
            uLastAction[uidForDB] = ""
            return
        }
        val request = SendMessage(update.message().chat().id(), uLanguage.getString("WriteCurrentOTP")).replyMarkup(getMainMenuKeyboard())
        mBot!!.execute(request)
    }
    fun getAllowInputToArray(): Array<String> {
        val l = mutableListOf<String>()
        for(c in CryptoCoins.coins) {
            l.add(c.key)
        }
        return l.toTypedArray()
    }
    fun getInput(update: Update, uid: Long) {
        val uLanguage = getLangForUid(uid)
        val keyboard = ReplyKeyboardMarkup(
            getAllowInputToArray(), arrayOf("menu")
        ).resizeKeyboard(true).selective(true)
        mBot!!.execute(SendMessage(update.message().chat().id(), uLanguage.getString("SelectCryptocoin")).replyMarkup(keyboard))
    }

    fun passfun(update: Update, uid: Long) {
        // pass
    }
    fun getAllowInput(update: Update, uid: Long) {
        for(c in getAllowInputToArray()) {
            mBot!!.execute(SendMessage(update.message().chat().id(), "${c}"))
        }
    }
    fun output(update: Update, uid: Long)
    {
        val uLanguage = getLangForUid(uid)
        val keyboard = ReplyKeyboardMarkup(
            getAllowInputToArray(), arrayOf("menu")
        ).resizeKeyboard(true).selective(true)
        mBot!!.execute(SendMessage(update.message().chat().id(), uLanguage.getString("SelectCryptocoin")).replyMarkup(keyboard))
    }

    val u_on = mutableMapOf("setLanguage" to ::setLanguage,
        "registration" to ::registration,
        "sendCaptcha" to ::sendCaptcha,
        "genNewOTP" to ::genNewOTP,
        "dropOTP" to ::dropOTP,
        "getAllowInput" to ::getAllowInput,
        "getInput" to ::getInput,
        "genInput" to ::getInput,
        "output" to ::output
    )
    val uLastAction = mutableMapOf<String, String>()
    val uLastCaptchas = mutableMapOf<String, String>()
    val userTemporaryData = mutableMapOf<String, String>()
    fun getMainMenuKeyboard() = ReplyKeyboardMarkup(
        arrayOf("registration", "setLanguage"),
        arrayOf("getInput", "genInput"),
        arrayOf("fullList"), arrayOf("website")
    ).resizeKeyboard(true).selective(true)

    fun sendToUid(uid: Long, msg: String) {
        mBot!!.execute(SendMessage(uid, "$msg"))
    }
    private fun updateDo(update: Update) {
        try {
           // println("Update is:")
           // println(update.toString())
            val msg = update.message()
            val uid = msg.chat().id()
            val uLanguage = getLangForUid(uid)
            val uidForDB = userInfoToDBName(uid)
            if (u_on.containsKey(msg.text())) {
                println("load fun")
                uLastAction[uidForDB] = msg.text()
                u_on[msg.text()]!!(update, uid)
            }else when (msg.text())
            {
                "website" -> {
                    val apiref = "http://80.64.173.196:8081/ru/ApiReference.html"
                    val request = mBot!!.execute(SendMessage(msg.chat().id(), "$apiref"))
                }
                "testbutton" -> {
                    val replyKeyboardMarkup: Keyboard = ReplyKeyboardMarkup(
                        arrayOf("first row button1", "first row button2"),
                        arrayOf("second row button1", "second row button2")
                    )
                        .oneTimeKeyboard(true) // optional
                        .resizeKeyboard(true) // optional
                        .selective(true) // optional

                    val request = SendMessage(msg.chat().id(), "Выбери действие ${msg.chat().toString()}")
                    .parseMode(ParseMode.HTML).
                    disableWebPagePreview(true).
                    disableNotification(true).replyMarkup(replyKeyboardMarkup)
                    val sendResponse = mBot!!.execute(request)
                    val ok = sendResponse.isOk
                    println("send update is $ok")
                }
                "menu" -> {
                    val request = SendMessage(msg.chat().id(), uLanguage.getString("SelectChoice")).replyMarkup(getMainMenuKeyboard())
                    mBot!!.execute(request)
                    uLastAction[uidForDB] = ""
                    userTemporaryData[uidForDB] = ""
                }
                else -> {
                    when(uLastAction[uidForDB])
                    {
                        // OUTPUT
                        "SendMoney0" -> {
                            userTemporaryData[uidForDB] = userTemporaryData[uidForDB] + ";" + msg.text()
                            uLastAction[uidForDB] = "SendMoney1"
                            mBot!!.execute(SendMessage(msg.chat().id(), uLanguage.getString("WriteAmmount")))
                        }
                        "SendMoney1" -> {
                            userTemporaryData[uidForDB] = userTemporaryData[uidForDB] + ";" + msg.text()
                            uLastAction[uidForDB] = "SendMoney2"
                            val sp = userTemporaryData[uidForDB]!!.split(";")
                            println(sp)
                            mBot!!.execute(SendMessage(msg.chat().id(), String.format(uLanguage.getString("ConfirmSend"),  sp[0], sp[1], sp[2])))
                            println("was execute?")
                        }
                        "SendMoney2" -> {
                            val sends = msg.text().equals("yes")
                            if (sends)
                            {
                                val sp = userTemporaryData[uidForDB]!!.split(";")
                                mBot!!.execute(SendMessage(msg.chat().id(), String.format(uLanguage.getString("MoneySends"),  sp[0], sp[1], sp[2])))
                                val resp = JettyServer.Users.cryptocoins.sendMoney(acc = uidForDB, oAdr = sp[1].trimIndent(), coinname = sp[0].trimIndent(), cMoney = sp[2].trimIndent())
                                mBot!!.execute(SendMessage(msg.chat().id(), resp))
                                uLastAction[uidForDB] = ""
                                userTemporaryData[uidForDB] = ""
                            }
                            else
                            {
                                mBot!!.execute(SendMessage(msg.chat().id(), uLanguage.getString("Canceled")))
                            }
                        }
                        "output" -> {
                            val i = UserCoinBalance.getLoginBalance(uidForDB)
                            val c = msg.text()
                            if (i?.containsKey(c) != true)
                            {
                                mBot!!.execute(SendMessage(msg.chat().id(), uLanguage.getString("CryptocoinNotFound")))
                            } else {
                                val UCB = i[c]!!
                                uLastAction[uidForDB] = "SendMoney0"
                                userTemporaryData[uidForDB] = UCB.CoinName
                                mBot!!.execute(SendMessage(msg.chat().id(), uLanguage.getString("WriteAddr")))
                                //WriteAddr = Write Addr
                                        //WriteAmmount = Write ammount
                            }

                        }
                        // END OUTPUT
                        "setLanguage" -> {
                            val newLang = msg.text()
                            if ( allowedLangauges.contains(newLang) ) {
                                userLanguage.set(uidForDB, newLang)
                                val request = SendMessage(msg.chat().id(), uLanguage.getString("LanguageChanged")).replyMarkup(getMainMenuKeyboard())
                                mBot!!.execute(request)
                            } else {
                                val request = SendMessage(msg.chat().id(), uLanguage.getString("LanguageChanged")).replyMarkup(getMainMenuKeyboard())
                                mBot!!.execute(request)

                            }
                        }
                        "genNewOTP" -> {
                            val code = msg.text()
                            val secret = userTemporaryData[uidForDB]!!
                            val rcode = userOTP.getCode(secret)
                            if (code != rcode) {
                                val request = SendMessage(msg.chat().id(), uLanguage.getString("OTPNotCorrect")).replyMarkup(getMainMenuKeyboard())
                                mBot!!.execute(request)
                            } else {
                                val request = SendMessage(msg.chat().id(), uLanguage.getString("OTPWasChanged")).replyMarkup(getMainMenuKeyboard())
                                mBot!!.execute(request)
                                userOTP.set(uidForDB, secret, null) // last par its for last code
                            }
                        }
                        "dropOTP" -> {
                            val code = msg.text()
                            val rcode = userOTP.getCodeForUser(uidForDB)
                            if (code != rcode) {
                                val request =
                                    SendMessage(msg.chat().id(), uLanguage.getString("OTPNotCorrect")).replyMarkup(
                                        getMainMenuKeyboard()
                                    )
                                mBot!!.execute(request)
                            } else {
                                val request =
                                    SendMessage(msg.chat().id(), uLanguage.getString("OTPWasChanged")).replyMarkup(
                                        getMainMenuKeyboard()
                                    )
                                mBot!!.execute(request)
                                userOTP.dropFor(uidForDB)
                            }
                        }
                        "getInput" -> {
                            val i = UserCoinBalance.getLoginBalance(uidForDB)
                            val c = msg.text()
                            if (i?.containsKey(c) != true)
                            {
                                mBot!!.execute(SendMessage(msg.chat().id(), uLanguage.getString("CryptocoinNotFound")))
                            } else {
                                mBot!!.execute(SendMessage(msg.chat().id(), String.format(uLanguage.getString("YourInputAddress"), i[c]?.inputAddress, i[c]?.CoinName, i[c]?.balance) ))
                            }
                        } // get input
                        "genInput" -> {
                            val nadr = JettyServer.Users.genNewAddrForUser(uidForDB, msg.text(), search_unused = true)
                            if (nadr != null) {
                                val m = String.format(uLanguage.getString("urNewAddrIs"), nadr).also { UserCoinBalance.setLoginInputAddress(uidForDB, nadr!!, msg.text()) }
                                mBot!!.execute(SendMessage(msg.chat().id(), "$m"))
                            } else {
                                mBot!!.execute(SendMessage(msg.chat().id(), uLanguage.getString("ErrorWithGenerateNewAddress")))
                            }
                        } // there last actions
                        else -> {
                            if (msg.text().startsWith("getNotify")) {
                                mBot!!.execute(SendMessage(msg.chat().id(), uLanguage.getString("UrNotifications")))
                                val m = msg.text().split(" ")
                                try {
                                    val offset = m[1].toIntOrNull() ?: 0
                                    val lim = m[2].toIntOrNull() ?: 5
                                    val defTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                                    val notifications = notification.getNotificationsByOwnerAndTimestamp(uidForDB, defTime, lim = lim, offset = offset)
                                    mBot!!.execute(SendMessage(msg.chat().id(), "$offset $lim"))
                                    for(not in notifications)
                                    {
                                        mBot!!.execute(SendMessage(msg.chat().id(), "${not.msg}"))
                                    }
                                    return
                                }catch(_:Exception){}
                            } else if(msg.text().equals("fullList")) {
                                mBot!!.execute(SendMessage(msg.chat().id(),"getNotify 0 5, genInput, getInput, dropOTP, genNewOTP, testbutton, registration, setLanguage"))
                            } else { // not found command
                                val request = SendMessage(msg.chat().id(), uLanguage.getString("CommandNotFound"))
                                mBot!!.execute(request)
                                // ?
                            }
                        }// else
                    }// when
                }// else
            }// else
        } catch (_e: Exception) {
        }
    }

    private fun updateInline(iq: InlineQuery) {
        println("is inline query")
        println(iq.toString())
        val text = iq.query()
        val translited = ""//translit(text)
        val r2: InlineQueryResult<*> = InlineQueryResultArticle("id", translited, translited)
        mBot!!.execute(
            AnswerInlineQuery(iq.id(), *arrayOf(r2))
                .cacheTime(10)
                .isPersonal(false)
                .nextOffset("offset")
                .switchPmParameter("pmParam")
                .switchPmText(text)
        )
    }

    /**
     * Тут происходит чтение и вызов методов updateInline для inline сообщения/ updateDo для обычного текста отправленного боту.
     */
    fun readUpdates() {
        // System.out.printf("Last ID %d\n~~~~~~~~~~\n", mLastID);
        val _updates = GetUpdates().limit(100).offset(mLastID).timeout(0)
        val updatesResponse = mBot!!.execute(_updates)
        val updates = updatesResponse.updates()
        updateHandler@ for (update in updates) {
            val updateId = update.updateId()
            //val inlineQuery = update.inlineQuery()
            //if (inlineQuery != null) updateInline(inlineQuery) else
            updateDo(update)
            if (mLastID <= updateId) mLastID = updateId + 1
        }
        // System.out.println("~~~~~~~~~");
    }
    fun readUpdatesAndSaveProps()
    {
        this.readUpdates()
        try {
            this.updateProp()
        } catch (e: IOException) {
            println("Can't to update bot propetries: $e")
        }
    }

    /**
     * @param token String
     * inited bot by token
     */
    constructor(token: String) {
        initBot(token)
    }
}