package org.antibiotic.pool.main.DB

import com.mongodb.client.model.Filters
import kotlinx.serialization.Serializable
import org.antibiotic.pool.main.WebSite.JettyServer
import org.antibiotic.pool.main.i18n.i18n
import org.antibiotic.pool.main.telegabot
import org.bson.types.ObjectId
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import java.math.BigDecimal

// trade_stata
@Serializable
data class trade(val buyer: String, val seller: String, val toSell: toSellStruct, val toBuy: toSellStruct, val key: String = (ObjectId().toHexString()))
{
    companion object
    {
        init {
            DB.createCollection("doneTrade")
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
            val coinToSell = ord.whatSell.name
            val coinToBuy = ord.whatBuy.name
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
                   // let for_one_count_sell = (ord.whatSell.price / ord.whatSell.lmin) * ord.whatSell.price
                   // let for_one_count_buy = (ord.whatBuy.price / ord.whatBuy.lmin) * ord.whatBuy.price
                   val forOneCountSell = (ord.whatSell.price.toBigDecimal() / ord.whatSell.lmin.toBigDecimal() ) * ord.whatSell.price.toBigDecimal()
                   val forOneCountBuy = (ord.whatBuy.price.toBigDecimal()  / ord.whatBuy.lmin.toBigDecimal() ) * ord.whatBuy.price.toBigDecimal()
                //
                val cInDecimal = count.toBigDecimal()
                val ownBalanceInDecimal = ownBalance.balance.toBigDecimal()
                // checks for seller
                val ownerBalanceLessThanCountToBuy = ownBalanceInDecimal < forOneCountSell
                val countMoreThanLimit = (forOneCountSell * cInDecimal) > ord.whatSell.lmax.toBigDecimal()
                val countLessThanMinLimit = (forOneCountSell * cInDecimal) < ord.whatSell.lmin.toBigDecimal()
               // println(ownerBalanceLessThanCountToBuy)
               // println(countMoreThanLimit)
               // println(countLessThanMinLimit)
               // println("cInDecimal: ${(forOneCountSell * cInDecimal)}, ${ord.whatSell.lmin}")
                if (ownerBalanceLessThanCountToBuy || countMoreThanLimit || countLessThanMinLimit) throw  notAllowedOrder(uLanguage.getString("countMoreThanLimit"))
                // checks for buyer
                if (whoBuyBalance == null) throw notAllowedOrder(uLanguage.getString("uDontHaveBalance"))
                println("${ord.whatSell.price.toBigDecimal()} and ${ord.whatBuy.price.toBigDecimal()}")
                // val fPrice = ord.whatSell.price.toBigDecimal() / ord.whatBuy.price.toBigDecimal()
                // val fPrice_ = ord.whatBuy.price.toBigDecimal() / ord.whatSell.price.toBigDecimal()
                val countForSell = ord.whatBuy.price.toBigDecimal() *  cInDecimal
                val countForBuy  = ord.whatSell.price.toBigDecimal() * cInDecimal
                println("$countForSell and $countForBuy")
                if (whoBuyBalance.balance.toBigDecimal() < countForSell) throw notAllowedOrder(
                    String.format(uLanguage.getString("buyBalanceLessThanCount"), whoBuyBalance.balance, countForBuy, ord.whatBuy.price )
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
                val notDoneTrade = trade(whoBuy, ord.owner, ord.whatSell, ord.whatBuy)
                col.insertOne(notDoneTrade)
                return notDoneTrade.key
            }

        }
        private fun changeOrderParameters(ord: order, countForBuy: BigDecimal, countForSell: BigDecimal)
        {
            // drop old. change lmax.
            ord.whatBuy.lmax = (ord.whatBuy.lmax.toBigDecimal() - countForBuy).toString()
            ord.whatSell.lmax = (ord.whatSell.lmax.toBigDecimal() - countForSell).toString()
            order.remOrder(ord.key)
            // if (ord.whatBuy.lmax.toBigDecimal() > BigDecimal.ZERO && ord.whatSell.lmax.toBigDecimal() > BigDecimal.ZERO)
            // {
            //    order.addOrder(ord.owner, ord.whatSell, ord.whatBuy, ord.orderMSG, ord.isCoin2CoinTrade, ord.isFiat2CoinTrade, ord.ownerIsBuyer)
            // }
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
                notification.createNewNotification(
                    whoBuy,
                    String.format(uLanguage.getString("UBuy"), coinToSell, coinToBuy, ord.whatSell.price, countForBuy )
                )
                notification.createNewNotification(
                    ord.owner,
                    String.format(uLanguage.getString("USell"), coinToSell, coinToBuy, ord.whatSell.price, countForSell )
                )

                q@if(Regex("TELEGRAM USER \\d+").matches(ord.owner))
                {
                    val s = ord.owner.split(" ")
                    val uid = s[2].toLongOrNull()?: 0L
                    if (uid != 0L) {
                        telegabot.sendMsg(uid, String.format(uLanguage.getString("USell"), coinToSell, coinToBuy, ord.whatSell.price, countForSell ))
                    }
                }

                println("notification was created")
                userBalance.addToBalance(whoBuy, -countForSell, coinToBuy)
                userBalance.addToBalance(ord.owner, -countForBuy, coinToSell)
                userBalance.addToBalance(ord.owner, countForSell, coinToBuy)
                userBalance.addToBalance(whoBuy, countForBuy, coinToSell)
                DB.createCollection("doneTrade")
                val col = DB.mongoDB.getCollection<trade>("doneTrade")
                val doneTrade = trade(whoBuy, ord.owner, ord.whatSell, ord.whatBuy)
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
            val col = DB.mongoDB.getCollection<trade>("doneTrade")
            return col.find(Filters.or(trade::buyer eq who, trade::seller eq who)).skip(skip).limit(lim).iterator().asSequence().toList() // use it instead big code
        }
        fun getDoneTradeByID(id: String): List<trade> {
            val col = DB.mongoDB.getCollection<trade>("doneTrade")
            return col.find(trade::key eq id).iterator().asSequence().toList() // use it instead big code
        }
    }
}
