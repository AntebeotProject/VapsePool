package ru.xmagi.pool.main.WebSite.Handlers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import ru.xmagi.pool.main.CryptoCurrencies.CryptoCoins
import ru.xmagi.pool.main.CryptoCurrencies.ElectrumRPC
import ru.xmagi.pool.main.PoolServer.DB
import ru.xmagi.pool.main.PoolServer.RPC
import ru.xmagi.pool.main.PoolServer.RPCClient
import ru.xmagi.pool.main.PoolServer.deleteSquares
import ru.xmagi.pool.main.WebSite.JSONBooleanAnswer
import ru.xmagi.pool.main.WebSite.defRPCTXFee
import kotlin.concurrent.thread

class RESTHandler : AbstractHandler() {

    override fun handle(target: String?, baseRequest: Request, request: HttpServletRequest?, response: HttpServletResponse?) {
        baseRequest.setHandled(true)
        // Implement the REST APIs.
        response!!.setStatus(200);
        response!!.setContentType("text/html; charset=UTF-8");
        when(request!!.getParameter("w")) {
            "getbalance" -> {
                val ac = request!!.getParameter("ac")
                response.getWriter().print(DB.getLoginBalance(ac).toString())
            }
            "getallowcoins" ->
            {
                response!!.setContentType("application/json; charset=UTF-8");
                val allowed_coin_list = mutableListOf<String>()
                for(c in CryptoCoins.coins) {
                    allowed_coin_list.add(c.key)
                }
                response.getWriter().print(Json.encodeToString(allowed_coin_list))
                // TODO("Not implemented")
            }
            "output" -> {
                synchronized(DB) {
                    response!!.setContentType("application/json; charset=UTF-8");
                    val acc = request!!.getParameter("acc")
                    val pass = request!!.getParameter("pass")
                    val oAdr = request!!.getParameter("oAdr")
                    val cMoney = request!!.getParameter("cMoney")
                    val coinname = request!!.getParameter("coinname") ?: "gostcoin"
                    if (DB.checkUserPassword(acc, pass) != true) {
                        return response.getWriter().print(Json.encodeToString(JSONBooleanAnswer(false, "Not correct logn and password")))
                    }
                        synchronized(RPCClient.m_cl) {
                            println("Do thread")
                            println("[debug] output from $acc ${cMoney}")
                            val balances = DB.getLoginBalance(acc)
                            println("Balances($coinname): $balances")
                            val UserBalanceOfCoin = balances?.get(coinname)
                            if (UserBalanceOfCoin == null) {

                            }
                            if (UserBalanceOfCoin?.isBlocked == true) {
                                return response.getWriter().print(Json.encodeToString(JSONBooleanAnswer(false, "Your account is blocked for a while. write to administration")))
                            }

                            if (RPC.lockOutput) return
                            if (CryptoCoins.coins.get(coinname) == null) {
                                return response.getWriter()
                                    .print( Json.encodeToString(JSONBooleanAnswer(false, "The coinname is disabled for now")))
                            }
                            var txFee = defRPCTXFee.toBigDecimal() // magicNumber
                            println("Okey now get tx")
                            if (CryptoCoins.coins[coinname]!!.getisElectrum()) {
                                val tx_ = (CryptoCoins.coins[coinname]!! as ElectrumRPC).getfeerate().jsonObject.toMap()["result"]!!.jsonPrimitive.toString()
                                txFee = (CryptoCoins.coins[coinname]!! as ElectrumRPC).satoshiToBTC(tx_) //tx_.toBigDecimal() //?: defRPCTXFee.toBigDecimal() // 0.01 is very big for Electrum. so
                            } else {
                                (CryptoCoins.coins[coinname]!! as RPC).settxfee(txFee.toString()) // TODO: change the value? from nethowrk
                            }
                            if (cMoney.toBigDecimal() + txFee > UserBalanceOfCoin?.balance?.toBigDecimal() ?: 0.0.toBigDecimal() || cMoney.toBigDecimal() < txFee) {
                                // println("Returns error")
                                // DB.addToBalance(acc, 5.0, coinname) // for test only!
                                println("txFee: $txFee")
                                println(cMoney.toBigDecimal() < txFee)
                                return response.getWriter()
                                    .print( Json.encodeToString(JSONBooleanAnswer(false, "Not correct count of money ${cMoney.toBigDecimal() + txFee} and ${UserBalanceOfCoin?.balance}")) )
                            }
                            println("Tx fee $txFee")
                            RPC.lockOutput = true
                            CryptoCoins.coins[coinname]!!.sendMoney(oAdr, (cMoney.toBigDecimal()), "$acc from pool")
                            DB.addToBalance(acc, -(cMoney.toBigDecimal() + txFee))
                            RPC.lockOutput = false
                            return response.getWriter()
                                .print(Json.encodeToString(JSONBooleanAnswer(true, "${cMoney.toBigDecimal() + txFee} with fee was send on address $oAdr")))//"Money was send! <meta http-equiv=\"refresh\" content=\"5; url=/\">")
                        }
                }
            }
            "BitcoinBroadcast" ->
            {
                if (CryptoCoins.coins.get("bitcoin") == null) {
                    return response.getWriter().print(Json.encodeToString(JSONBooleanAnswer(false, "Bitcoin disabled")))
                }
                (CryptoCoins.coins.get("bitcoin") as ElectrumRPC).broadcast(request.getParameter("tx"))
            }
            "ReserveGet" -> {
                val oStr = buildString {
                    for (coin in CryptoCoins.coins) {
                        append(coin.key + "\n")
                        append(coin.value.getbalance().toString().deleteSquares() + "\n")
                        if (coin.value.getisElectrum()) {
                            val eRPC = coin.value as ElectrumRPC

                        } else {
                            val bRPC = coin.value as RPC

                        }
                    }
                }
                response.getWriter().print("<pre>$oStr</pre>")
            }
            else -> {
                response!!.setStatus(404);
            }
        }
    }
}
