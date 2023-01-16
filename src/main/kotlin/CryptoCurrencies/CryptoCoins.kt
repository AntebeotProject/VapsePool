package ru.xmagi.pool.main.CryptoCurrencies

import ru.xmagi.pool.main.JSONRPC
import ru.xmagi.pool.main.PoolServer.RPC
import ru.xmagi.pool.main.PoolServer.defTXFee
import java.io.File
import java.util.*
enum class mode(val mode: String) {
    electrum("electrum"){
        override fun init(user: String, pass: String, txFee: Double, host: String, port: Int): JSONRPC.worker {
            return ElectrumRPC(port, user, pass)
      }
    },
    bitcoin("bitcoin") {
        override fun init(user: String, pass: String, txFee: Double, host: String, port: Int): JSONRPC.worker {
            return RPC(host, user, pass, txFee)
        }
    };
    abstract fun init(user: String, pass: String, txFee: Double = 0.01, host: String = "http://127.0.0.1", port: Int = 0): JSONRPC.worker
    companion object {
        public fun modeExists(w: String): Boolean {
            for (m in values()) {

                if (m.mode.equals(w)) return true
            }
            return false
        }
    }
}
val defMode = mode.bitcoin

class CryptoCoins {
    companion object {
        private fun wDebug(w: String) = println("[WARNING] $w")
        val coins = mutableMapOf<String,JSONRPC.worker>()
        fun initCoins() {
           val dir = File(".${File.separator}CryptoCoins").walk()
            for(f in dir) {
                if (f.name.endsWith(".config")) {
                    val tPropetries = Properties()
                    tPropetries.load(f.inputStream())
                    val mode_ = tPropetries.getOrDefault("mode", "electrum").toString()
                    val cMode = if( !mode.modeExists(mode_) ) mode.valueOf("bitcoin") else mode.valueOf(mode_)
                    val cUser = tPropetries.getProperty("user").toString()
                    val cPass = tPropetries.getProperty("pass").toString()
                    val cTXFee = tPropetries.getOrDefault("paytxfee", defTXFee).toString().toDouble()
                    when (cMode.mode) {
                        "bitcoin" -> {
                            val host = tPropetries.getProperty("host")
                            val rpc = cMode.init(cUser, cPass, host=host, txFee = cTXFee)
                            coins.put(f.name.split(".")[0], rpc)
                        }
                        "electrum" -> {
                            val cPort = tPropetries.getProperty("port").toInt()
                            val rpc = cMode.init(cUser, cPass, port = cPort)
                            coins.put(f.name.split(".")[0], rpc)
                        }
                    } // @when
                }// @if
            } // @for
        }// @fun
    }
}