package org.antibiotic.pool.main.DB

import com.mongodb.client.model.Filters
import kotlinx.serialization.Serializable
import org.antibiotic.pool.main.PoolServer.Settings
import org.antibiotic.pool.main.WebSite.JSONBooleanAnswer
import org.antibiotic.pool.main.WebSite.JettyServer
import org.antibiotic.pool.main.i18n.i18n
import org.antibiotic.pool.main.telegabotEs
import org.bson.types.ObjectId
import org.litote.kmongo.*
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

// trade_stata
@Serializable
data class trade(val buyer: String, val seller: String, val info: cryptoOrderInfo,
                 val isCrypto2Crypto: Boolean = true, val key: String = (ObjectId().toHexString()),
                 val buyerAllowOutput: Boolean = false, val sellerAllowOutput: Boolean = false,
                 val createTime: Long = System.currentTimeMillis(),
                 val countForSell: String? = null)
{
    companion object {
        init {
            DB.createCollection("cryptodoneTrade")
            DB.createCollection("p2pTrade")
            DB.createCollection("p2pDoneTrade")
        }

        // use forum on phpbb for reviews maybe?
        // 63d2e276dc3ff75fba814547
        fun doTrade(whoBuy: String, count: String, orderID: String): String {
            val uLanguage = JettyServer.Users.language.getLangByUser(whoBuy)
            val ord = DB.getOrderByID(orderID)
            if (ord == null) throw notAllowedOrder(uLanguage.getString("orderNotFound"))
            if (ord.owner == whoBuy) throw notAllowedOrder(uLanguage.getString("oNotAOwn"))
            val coinToSell = ord.info.toGiveName
            val coinToBuy = ord.info.toGetName
            val ownBalance = DB.getLoginBalance(ord.owner)?.get(coinToSell)
            val whoBuyBalance = DB.getLoginBalance(whoBuy)?.get(coinToBuy)

            if (!ord.isActive) throw notAllowedOrder(uLanguage.getString("orderNotActive"))
            if (ownBalance == null) {
                DB.remOrder(ord.key)
                throw notAllowedOrder(uLanguage.getString("sellerNotHavenEvenBalance"))
            }

            val cInDecimal = count.trim().toBigDecimal()
            val ownBalanceInDecimal = ownBalance.balance.toBigDecimal() // own is seller
            val forOneCountSell = cInDecimal * BigDecimal.ONE

            val ownerBalanceLessThanCountToBuy = (ownBalanceInDecimal < forOneCountSell)
            val countMoreThanLimit = (forOneCountSell > ord.info.maxVolume.toBigDecimal())
            val countLessThanLimit = (forOneCountSell < ord.info.minVolume.toBigDecimal())

            if (ownerBalanceLessThanCountToBuy || countMoreThanLimit || countLessThanLimit) throw notAllowedOrder(uLanguage.getString("countMoreThanLimit"))

            if (ord.isCoin2CoinTrade) {
                // is auto
                //
                //ord.info.priceRatio.toBigDecimal() //(ord.whatSell.price.toBigDecimal() / ord.whatSell.lmin.toBigDecimal() ) * ord.whatSell.price.toBigDecimal()
                //
                // checks for buyer
                if (whoBuyBalance == null) throw notAllowedOrder(uLanguage.getString("uDontHaveBalance"))
                // val whoBuyBalanceInDecimal  = whoBuyBalance!!.balance.toBigDecimal()
                val countForSell = ord.info.priceRatio.toBigDecimal() * cInDecimal
                val countForBuy =
                    cInDecimal * BigDecimal.ONE // тут возможно нарушена логика. но это то что у нас отдается
                println("$countForSell and $countForBuy")
                if (whoBuyBalance.balance.toBigDecimal() < countForSell) throw notAllowedOrder(
                    String.format(
                        uLanguage.getString("buyBalanceLessThanCount"),
                        whoBuyBalance.balance,
                        countForBuy,
                        ord.info.priceRatio
                    )
                )
                return doCoin2CoinTrade(
                    whoBuy = whoBuy,
                    countForSell = countForSell,
                    countForBuy = countForBuy,
                    coinToSell = coinToSell,
                    coinToBuy = coinToBuy,
                    ord = ord,
                    uLanguage = uLanguage
                )
            }// else if not coin2tocoin
            else {
                val col = DB.mongoDB.getCollection<trade>("p2pTrade")
                val notDoneTrade = trade(
                    whoBuy,
                    ord.owner,
                    ord.info,
                    isCrypto2Crypto = false,
                    countForSell = forOneCountSell.toString()
                )
                col.insertOne(notDoneTrade)
                // block output
                userBalance.setBlockOutputForCoinName(ord.owner, ord.info.toGiveName, isBlocked = true)
                return notDoneTrade.key
            }
        }

        fun getP2PTrades() = DB.mongoDB.getCollection<trade>("p2pTrade").find()
        private var tradeForCleanOldTradesRuns: Boolean = false;
        fun startTradeForClearOldTrades() {
            if (tradeForCleanOldTradesRuns == true) return
            tradeForCleanOldTradesRuns = true
            thread {
                while (true) {
                    val sTime =
                        Settings.m_propetries.getOrDefault("ThreadSleepForGetInput", 30000).toString().toLongOrNull()
                            ?: 30000;
                    val trades = getP2PTrades()
                    for (trade in trades) {
                        if (trade.isCrypto2Crypto == true) {
                            System.err.println("Check to bug our code")
                            continue
                        }; // weird?
                        val times = System.currentTimeMillis() - trade.createTime
                        val timeIsOut = TimeUnit.MINUTES.toMillis(15) < times
                        if (trade.buyerAllowOutput == false && trade.sellerAllowOutput == false && timeIsOut) {
                            notification.createNewNotification(
                                trade.buyer,
                                "trade ${trade.key} time out. drop / Время ордера вышло. Он удален."
                            )
                            notification.createNewNotification(
                                trade.seller,
                                "trade ${trade.key} time out. drop / Время ордера вышло. Он удален."
                            )
                            dropP2PTradeById(trade.key)
                        }
                        if (timeIsOut) {
                            if (trade.buyerAllowOutput == false) notification.createNewNotification(
                                trade.buyer,
                                "Подтвердите перевод. иначе будет открыть апеляция по ${trade.key}"
                            )
                            if (trade.sellerAllowOutput == false) notification.createNewNotification(
                                trade.seller,
                                "Подтвердите перевод. иначе будет открыта апеляция по ${trade.key}"
                            )
                        }
                    }
                    Thread.sleep(sTime)
                }
            }
        }//

        fun getP2PTradeById(id: String) =
            DB.mongoDB.getCollection<trade>("p2pTrade").findOne(Filters.and(trade::key eq id))

        fun changeAllowOutputBuyer(id: String, s: Boolean = true) =
            DB.mongoDB.getCollection<trade>("p2pTrade")
                .updateOne(Filters.and(trade::key eq id), setValue(trade::buyerAllowOutput, s))

        fun changeAllowOutputSeller(id: String, s: Boolean = true) =
            DB.mongoDB.getCollection<trade>("p2pTrade")
                .updateOne(Filters.and(trade::key eq id), setValue(trade::sellerAllowOutput, s))

        fun dropP2PTradeById(id: String) =
            DB.mongoDB.getCollection<trade>("p2pTrade").deleteOne(Filters.and(trade::key eq id))

        private fun changeOrderParameters(ord: order, countForBuy: BigDecimal, countForSell: BigDecimal) {
            // drop old. change lmax.
            ord.info.maxVolume = (ord.info.maxVolume.toBigDecimal() - countForBuy).toString()
            order.remOrder(ord.key)
            if (ord.info.maxVolume.toBigDecimal() > BigDecimal.ZERO && ord.info.minVolume.toBigDecimal() >= BigDecimal.ZERO) {
                order.addOrder(
                    ord.owner,
                    ord.info,
                    ord.orderMSG,
                    ord.isCoin2CoinTrade,
                    ord.isFiat2CoinTrade,
                    ord.ownerIsBuyer,
                    isActive = ord.isActive
                )

            }
        }

        const val comissionPercent = 0.0001
        const val NAME_OF_SERVER_BALANCE = "!_ SERVER BALANCE _!"
        private fun doCoin2CoinTrade(
            whoBuy: String,
            countForSell: BigDecimal,
            countForBuy: BigDecimal,
            coinToSell: String,
            coinToBuy: String,
            ord: order,
            uLanguage: i18n
        ): String {
            println("Теперь баланс $whoBuy должен понизиться на $countForSell для монеты $coinToBuy")
            println("Теперь баланс ${ord.owner} должен понизиться на $countForBuy для монеты $coinToSell")

            println("Теперь баланс ${ord.owner} должен повыситься на $countForSell для монеты $coinToBuy")
            println("Теперь баланс $whoBuy должен повыситься на $countForBuy для монеты $coinToSell")

            synchronized(DB)
            {
                //synchronized(CryptoCoins.coins)
                //{
                // Not tested yet
                // println("DB check")
                if (DB.getLoginBalance(ord.owner)!!
                        .get(coinToSell)!!.balance.toBigDecimal() - countForBuy < BigDecimal.ZERO
                ) {
                    throw notAllowedOrder("bad balance")
                }
                if (DB.getLoginBalance(whoBuy)!!
                        .get(coinToBuy)!!.balance.toBigDecimal() - countForSell < BigDecimal.ZERO
                ) {
                    throw notAllowedOrder("bad balance")
                }
                // println("create notifications")
                notification.createNewNotification(
                    whoBuy,
                    String.format(uLanguage.getString("UBuy"), coinToSell, coinToBuy, ord.info.priceRatio, countForBuy)
                )
                notification.createNewNotification(
                    ord.owner,
                    String.format(
                        uLanguage.getString("USell"),
                        coinToSell,
                        coinToBuy,
                        ord.info.priceRatio,
                        countForSell
                    )
                )
                // println("notification was created")
                userBalance.addToBalance(whoBuy, -countForSell, coinToBuy)
                userBalance.addToBalance(ord.owner, -countForBuy, coinToSell)
                userBalance.addToBalance(ord.owner, countForSell, coinToBuy)
                userBalance.addToBalance(whoBuy, countForBuy, coinToSell)
                // DB.createCollection("cryptodoneTrade")
                val col = DB.mongoDB.getCollection<doneOrders>("cryptodoneTrade")
                val doneTrade_ = trade(whoBuy, ord.owner, ord.info, isCrypto2Crypto = true)
                col.insertOne(
                    doneOrders(
                        doneTrade_.buyer,
                        doneTrade_.seller,
                        doneTrade_.info,
                        doneTrade_.isCrypto2Crypto,
                        doneTrade_.key
                    )
                )
                notification.createNewNotification(
                    whoBuy,
                    String.format(uLanguage.getString("orderIsSucc"), doneTrade_.key)
                )
                notification.createNewNotification(
                    ord.owner,
                    String.format(uLanguage.getString("orderIsSucc"), doneTrade_.key)
                )
                changeOrderParameters(ord, countForBuy, countForSell)
                println("Comission")
                // Comission
                println("С учетом комиссии у всех снимает баланс на аккаунт сервера с каждой валюты по ${comissionPercent * 100}% за сделку")
                userBalance.addToBalance(ord.owner, -(countForSell * comissionPercent.toBigDecimal()), coinToBuy)
                userBalance.addToBalance(
                    NAME_OF_SERVER_BALANCE,
                    (countForSell * comissionPercent.toBigDecimal()),
                    coinToBuy
                )
                userBalance.addToBalance(whoBuy, -(countForBuy * comissionPercent.toBigDecimal()), coinToSell)
                userBalance.addToBalance(
                    NAME_OF_SERVER_BALANCE,
                    (countForBuy * comissionPercent.toBigDecimal()),
                    coinToSell
                )
                // END Commision
                //
                return doneTrade_.key
                //}
            }
        }

        fun getDoneTradeByBuyerOrSeller(who: String, skip: Int = 0, lim: Int = 5): List<doneOrders> {
            val col = DB.mongoDB.getCollection<doneOrders>("cryptodoneTrade")
            return col.find(Filters.or(doneOrders::buyer eq who, doneOrders::seller eq who)).skip(skip).limit(lim)
                .iterator().asSequence().toList() // use it instead big code
        }

        fun getDoneTradeByID(id: String): List<doneOrders> {
            val col = DB.mongoDB.getCollection<doneOrders>("cryptodoneTrade")
            return col.find(doneOrders::key eq id).iterator().asSequence().toList() // use it instead big code
        }

        fun getLastDoneTrades(skip: Int = 0, lim: Int = 5): List<doneOrders> {
            val col = DB.mongoDB.getCollection<doneOrders>("cryptodoneTrade")
            try {
                return col.find().iterator().asSequence().toList().reversed()
                    .subList(skip, skip + lim) // use it instead big code
            } catch (_: Exception) {
                return listOf()
            }
        }

        fun checkTrade(id: String, uLanguage: i18n): JSONBooleanAnswer {
            val trade = getP2PTradeById(id)!!
            if (trade.sellerAllowOutput && trade.buyerAllowOutput) {
                return doFiat2CoinTrade(trade, uLanguage)
            }
            return JSONBooleanAnswer(false, "не все участники подтвердили переводы")
        } // check

        private fun doFiat2CoinTrade(t: trade, uLanguage: i18n): JSONBooleanAnswer {
            val c = t.countForSell!!.toBigDecimal()
            val seller = t.seller
            val buyer = t.buyer
            val coin = t.info.toGiveName
            println("Теперь баланс $seller должен понизиться на $c для монеты $coin")
            println("Теперь баланс ${buyer} должен повыситься на $c для монеты $coin")
            println("С учетом комиссии у покупающего снимает баланс на аккаунт сервера с $coin по ${comissionPercent * 100}% за сделку")
            synchronized(DB)
            {
                userBalance.addToBalance(seller, -c, coin)
                userBalance.addToBalance(buyer, c, coin)
                //
                userBalance.addToBalance(buyer, -(c * comissionPercent.toBigDecimal()), coin)
                userBalance.addToBalance(NAME_OF_SERVER_BALANCE, (c * comissionPercent.toBigDecimal()), coin)

                // DB.createCollection("p2pDoneTrade")
                val col = DB.mongoDB.getCollection<doneOrders>("p2pDoneTrade")
                val doneTrade_ = trade(
                    buyer,
                    seller,
                    t.info,
                    isCrypto2Crypto = false,
                    buyerAllowOutput = true,
                    sellerAllowOutput = true,
                    countForSell = c.toString()
                ) // трата энергии
                col.insertOne(
                    doneOrders(
                        doneTrade_.buyer,
                        doneTrade_.seller,
                        doneTrade_.info,
                        doneTrade_.isCrypto2Crypto,
                        doneTrade_.key
                    )
                )
                notification.createNewNotification(
                    buyer,
                    String.format(uLanguage.getString("orderIsSucc"), doneTrade_.key)
                )
                notification.createNewNotification(
                    seller,
                    String.format(uLanguage.getString("orderIsSucc"), doneTrade_.key)
                )
                dropP2PTradeById(t.key)
                return JSONBooleanAnswer(true, "перевод осуществлен")
            }


        }
    }
}
// TODO: ... not duplicate of code
@Serializable
data class doneOrders(
    var buyer: String,
    var seller: String,
    val info: cryptoOrderInfo,
    val isCrypto2Crypto: Boolean = true,
    val key: String,
    val time:Long = System.currentTimeMillis()
                    )