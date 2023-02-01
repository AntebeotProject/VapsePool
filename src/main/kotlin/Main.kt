package org.antibiotic.pool.main;

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.antibiotic.pool.AsyncServer
import org.antibiotic.pool.main.CryptoCurrencies.CryptoCoins
import org.antibiotic.pool.main.CryptoCurrencies.MoneroRPC
import org.antibiotic.pool.main.PoolServer.*
import org.antibiotic.pool.main.WebSite.JettyServer
import org.antibiotic.pool.main.tgbot.prostaVapseTelegaBot
import org.antibiotic.pool.main.tgbot.prostaVapseTelegaBotSet
import java.util.*
import kotlin.concurrent.thread

// import com.github.jleskovar.*


// const val tg_bot_token = "5961057014:AAF4kpngMlC0DCnmJYvvlz0_XkkcK4F0j6c"
val telegabot = prostaVapseTelegaBot( /*tg_bot_token*/ )
fun main(args: Array<String>) {
    thread {
        while(true) {
            telegabot.readUpdatesAndSaveProps()
            Thread.sleep(3000);
        }
    }
    AsyncServer.DebugEnabled = true
    var s = AsyncServer("0.0.0.0", 3334) // async
    Settings.load_propetries()
    val myServ = JettyServer("0.0.0.0", 8081) // thread
    MinerData.Companion.startThreadForClean() // thread
    CryptoCoins.CheckerOfInputTransacations.runUpdaterOfTransactions() // thread
    while(true) {
        try {
            var updates = s.update()
            for (update in updates) {
                val last_msg = update.getLast_msg().trimIndent()
                try {
                    val json_data = StratumServer.GetJSONRPC(last_msg)
                    if (json_data != null && !last_msg.contains("HTTP")) {
                        // var arr = ArrayList<String>()
                        // val answ = RPCJSON(1, "mining.subscribe", arr)
                        StratumServer.doJSON(json_data, update)
                        // update.write(answ.toString())
                    } else if (last_msg.contains("HTTP")) {
                        HTTPServer.doHTTP(last_msg, update)
                    } else {
                        update.closeConnection()
                    }
                } catch (e: Exception) {
                    update.write(e.toString())
                }
            }
        } catch(_: Exception){} //
    }
}