package org.antibiotic.pool.main;

import org.antibiotic.pool.AsyncServer
import org.antibiotic.pool.main.CryptoCurrencies.CryptoCoins
import org.antibiotic.pool.main.PoolServer.*
import org.antibiotic.pool.main.WebSite.JettyServer
import java.util.*

// import com.github.jleskovar.*



fun main(args: Array<String>) {
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