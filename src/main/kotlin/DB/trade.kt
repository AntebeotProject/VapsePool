package org.antibiotic.pool.main.DB

import com.mongodb.client.model.Filters
import kotlinx.serialization.Serializable
import org.antibiotic.pool.main.WebSite.JettyServer
import org.antibiotic.pool.main.i18n.i18n
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
                val cInDecimal = count.toBigDecimal()
                val ownBalanceInDecimal = ownBalance.balance.toBigDecimal()
                // checks for seller
                val ownerBalanceLessThanCountToBuy = ownBalanceInDecimal < cInDecimal
                val countMoreThanLimit = cInDecimal > ord.whatSell.lmax.toBigDecimal()
                val countLessThanMinLimit = cInDecimal < ord.whatSell.lmin.toBigDecimal()
                println(ownerBalanceLessThanCountToBuy)
                println(countMoreThanLimit)
                println(countLessThanMinLimit)
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
            if (ord.whatBuy.lmax.toBigDecimal() > BigDecimal.ZERO && ord.whatSell.lmax.toBigDecimal() > BigDecimal.ZERO)
            {
                order.addOrder(ord.owner, ord.whatSell, ord.whatBuy, ord.orderMSG, ord.isCoin2CoinTrade, ord.isFiat2CoinTrade, ord.ownerIsBuyer)
            }
        }
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
                DB.createNewNotification(
                    whoBuy,
                    String.format(uLanguage.getString("UBuy"), coinToSell, coinToBuy, ord.whatSell.price )
                )
                DB.createNewNotification(
                    ord.owner,
                    String.format(uLanguage.getString("USell"), coinToSell, coinToBuy, ord.whatSell.price )
                )
                DB.addToBalance(whoBuy, -countForSell, coinToBuy)
                DB.addToBalance(ord.owner, -countForBuy, coinToSell)
                DB.addToBalance(ord.owner, countForSell, coinToBuy)
                DB.addToBalance(whoBuy, countForBuy, coinToSell)
                DB.createCollection("doneTrade")
                val col = DB.mongoDB.getCollection<trade>("doneTrade")
                val doneTrade = trade(whoBuy, ord.owner, ord.whatSell, ord.whatBuy)
                col.insertOne(doneTrade)
                DB.createNewNotification(whoBuy,
                    String.format(uLanguage.getString("orderIsSucc"), doneTrade.key)
                )
                DB.createNewNotification(ord.owner, String.format(uLanguage.getString("orderIsSucc"), doneTrade.key))
                changeOrderParameters(ord, countForBuy, countForSell)
                //
                return doneTrade.key
                //}
            }
        }
        fun getDoneTradeByBuyerOrSeller(who: String): List<trade> {
            val col = DB.mongoDB.getCollection<trade>("doneTrade")
            return col.find(Filters.or(trade::buyer eq who, trade::seller eq who)).iterator().asSequence().toList() // use it instead big code
        }
        fun getDoneTradeByID(id: String): List<trade> {
            val col = DB.mongoDB.getCollection<trade>("doneTrade")
            return col.find(trade::key eq id).iterator().asSequence().toList() // use it instead big code
        }
    }
}
