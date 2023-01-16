package ru.xmagi.pool.main;

import ru.xmagi.pool.AsyncServer
import ru.xmagi.pool.main.PoolServer.*
import ru.xmagi.pool.main.WebSite.JettyServer

// import com.github.jleskovar.*



fun main(args: Array<String>) {
    AsyncServer.DebugEnabled = true
    var s = AsyncServer("0.0.0.0", 3334)

    Settings.load_propetries()
    val myServ = JettyServer("0.0.0.0", 8081)
    MinerData.Companion.startThreadForClean()
    // DB.addUser("gostcoinrpc", "123")
    // println( DB.checkUserPassword("gostcoinrpc", "123") )//DB.getLoginBalance("gostcoinrpc"))

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