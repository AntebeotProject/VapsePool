package org.antibiotic.pool.main.DB

import com.mongodb.client.model.Filters
import kotlinx.serialization.Serializable
import org.antibiotic.pool.main.WebSite.JettyServer
import org.antibiotic.pool.main.i18n.i18n
import org.antibiotic.pool.main.telegabotEs
import org.bson.types.ObjectId
import org.litote.kmongo.eq
import org.litote.kmongo.find
import org.litote.kmongo.getCollection
import org.litote.kmongo.gt
import java.math.BigDecimal

// trade_stata
@Serializable
data class trade(val buyer: String, val seller: String, val info: cryptoOrderInfo, val isCrypto2Crypto: Boolean = true, val key: String = (ObjectId().toHexString()))
{
    companion object
    {
        init {
            DB.createCollection("cryptodoneTrade")
            DB.createCollection("notDoneTrade")
        }
        // use forum on phpbb for reviews maybe?
        // 63d2e276dc3ff75fba814547
        fun doTrade(whoBuy: String, count: String, orderID: String): String
        {
            val uLanguage = JettyServer.Users.language.getLangByUser(whoBuy)
            val ord = DB.getOrderByID(orderID)
            if (ord == null) throw notAllowedOrder(uLanguage.getString("orderNotFound"))
            if (ord.owner == whoBuy) throw notAllowedOrder(uLanguage.getString("oNotAOwn"))
            val coinToSell = ord.info.toGiveName
            val coinToBuy = ord.info.toGetName
            val ownBalance = DB.getLoginBalance(ord.owner)?.get(coinToSell)
            val whoBuyBalance = DB.getLoginBalance(whoBuy)?.get(coinToBuy)
            if (!ord.isActive) throw notAllowedOrder(uLanguage.getString("orderNotActive"))
            if (ord.isCoin2CoinTrade)
            {
                // is auto
                if (ownBalance == null) {
                    DB.remOrder(ord.key)
                    throw notAllowedOrder(uLanguage.getString("sellerNotHavenEvenBalance"))
                }
                //
                   val cInDecimal = count.trim().toBigDecimal()
                   val forOneCountSell = cInDecimal * BigDecimal.ONE
                   //ord.info.priceRatio.toBigDecimal() //(ord.whatSell.price.toBigDecimal() / ord.whatSell.lmin.toBigDecimal() ) * ord.whatSell.price.toBigDecimal()
                //

                val ownBalanceInDecimal = ownBalance.balance.toBigDecimal()

                val ownerBalanceLessThanCountToBuy = (ownBalanceInDecimal < forOneCountSell)
                val countMoreThanLimit = (forOneCountSell > ord.info.maxVolume.toBigDecimal())

                if (ownerBalanceLessThanCountToBuy || countMoreThanLimit) throw  notAllowedOrder(uLanguage.getString("countMoreThanLimit"))
                // checks for buyer
                if (whoBuyBalance == null) throw notAllowedOrder(uLanguage.getString("uDontHaveBalance"))
                val whoBuyBalanceInDecimal  = whoBuyBalance!!.balance.toBigDecimal()

                val countForSell = ord.info.priceRatio.toBigDecimal() * cInDecimal
                val countForBuy  = cInDecimal * BigDecimal.ONE
                println("$countForSell and $countForBuy")
                if (whoBuyBalance.balance.toBigDecimal() < countForSell) throw notAllowedOrder(
                    String.format(uLanguage.getString("buyBalanceLessThanCount"), whoBuyBalance.balance, countForBuy, ord.info.priceRatio )
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
                val col = DB.mongoDB.getCollection<trade>("notDoneTrade")
                val notDoneTrade = trade(whoBuy, ord.owner, ord.info)
                col.insertOne(notDoneTrade)
                return notDoneTrade.key
            }

        }
        private fun changeOrderParameters(ord: order, countForBuy: BigDecimal, countForSell: BigDecimal)
        {
            // drop old. change lmax.
            ord.info.maxVolume = (ord.info.maxVolume.toBigDecimal() - countForBuy).toString()
            order.remOrder(ord.key)
            if (ord.info.maxVolume.toBigDecimal() > BigDecimal.ZERO && ord.info.minVolume.toBigDecimal() >= BigDecimal.ZERO)
            {
                order.addOrder(ord.owner, ord.info, ord.orderMSG, ord.isCoin2CoinTrade, ord.isFiat2CoinTrade, ord.ownerIsBuyer, isActive = ord.isActive)

            }
        }
        const val comissionPercent = 0.01
        const val NAME_OF_SERVER_BALANCE = "!_ SERVER BALANCE _!"
        private fun doCoin2CoinTrade(whoBuy: String, countForSell: BigDecimal, countForBuy: BigDecimal, coinToSell: String, coinToBuy: String, ord: order, uLanguage: i18n): String
        {
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
                if (DB.getLoginBalance(ord.owner)!!.get(coinToSell)!!.balance.toBigDecimal() - countForBuy < BigDecimal.ZERO)
                {
                    throw notAllowedOrder("bad balance")
                }
                if (DB.getLoginBalance(whoBuy)!!.get(coinToBuy)!!.balance.toBigDecimal() - countForSell < BigDecimal.ZERO)
                {
                    throw notAllowedOrder("bad balance")
                }
                // println("create notifications")
                notification.createNewNotification(
                    whoBuy,
                    String.format(uLanguage.getString("UBuy"), coinToSell, coinToBuy, ord.info.priceRatio, countForBuy )
                )
                notification.createNewNotification(
                    ord.owner,
                    String.format(uLanguage.getString("USell"), coinToSell, coinToBuy, ord.info.priceRatio, countForSell )
                )
                // println("notification was created")
                userBalance.addToBalance(whoBuy, -countForSell, coinToBuy)
                userBalance.addToBalance(ord.owner, -countForBuy, coinToSell)
                userBalance.addToBalance(ord.owner, countForSell, coinToBuy)
                userBalance.addToBalance(whoBuy, countForBuy, coinToSell)
                DB.createCollection("cryptodoneTrade")
                val col = DB.mongoDB.getCollection<trade>("cryptodoneTrade")
                val doneTrade = trade(whoBuy, ord.owner, ord.info, isCrypto2Crypto = true)
                col.insertOne(doneTrade)
                notification.createNewNotification(whoBuy,
                    String.format(uLanguage.getString("orderIsSucc"), doneTrade.key)
                )
                notification.createNewNotification(ord.owner, String.format(uLanguage.getString("orderIsSucc"), doneTrade.key))
                changeOrderParameters(ord, countForBuy, countForSell)
                println("Comission")
                // Comission
                println("С учетом комиссии у всех снимает баланс на аккаунт сервера с каждой валюты по ${comissionPercent}% за сделку")
                userBalance.addToBalance(ord.owner, -(countForSell * comissionPercent.toBigDecimal()), coinToBuy)
                userBalance.addToBalance(whoBuy,    -(countForBuy * comissionPercent.toBigDecimal()),  coinToSell)
                userBalance.addToBalance(NAME_OF_SERVER_BALANCE, (countForSell * comissionPercent.toBigDecimal()), coinToSell)
                userBalance.addToBalance(NAME_OF_SERVER_BALANCE,    (countForBuy * comissionPercent.toBigDecimal()),  coinToBuy)
                // END Commision
                //
                return doneTrade.key
                //}
            }
        }
        fun getDoneTradeByBuyerOrSeller(who: String, skip: Int = 0, lim: Int = 5): List<trade> {
            val col = DB.mongoDB.getCollection<trade>("cryptodoneTrade")
            return col.find(Filters.or(trade::buyer eq who, trade::seller eq who)).skip(skip).limit(lim).iterator().asSequence().toList() // use it instead big code
        }
        fun getDoneTradeByID(id: String): List<trade> {
            val col = DB.mongoDB.getCollection<trade>("cryptodoneTrade")
            return col.find(trade::key eq id).iterator().asSequence().toList() // use it instead big code
        }
        fun getLastDoneTrades(skip: Int = 0, lim: Int = 5): List<trade> {
            val col = DB.mongoDB.getCollection<trade>("cryptodoneTrade")
            return col.find().iterator().asSequence().toList().reversed().subList(skip, skip + lim) // use it instead big code
        }
    }
}
