package org.antibiotic.pool.main.DB

import com.mongodb.client.FindIterable
import com.mongodb.client.model.Filters
import kotlinx.serialization.Serializable
import org.antibiotic.pool.main.CryptoCurrencies.CryptoCoins
import org.antibiotic.pool.main.WebSite.JettyServer
import org.bson.types.ObjectId
import org.litote.kmongo.eq
import org.litote.kmongo.find
import org.litote.kmongo.getCollection
import org.litote.kmongo.setValue
import java.math.BigDecimal

class notAllowedOrder(w: String): Exception("Not allowed order. $w");
// zero limit is not limits.
/*
    Что отдаем: название
    Что принимаем: название
    Цена за единицу того что отдаем. Например цена 0.000025, тогда то что получаем. цена в getName.
    countAmountForPrice сколько дается getName за giveнейм.

 */
@Serializable
data class cryptoOrderInfo(val toGiveName: String, val toGetName: String, val priceRatio: String, var minVolume: String, var maxVolume: String)
{
    init
    {
        // println("init toSellStruct with $name $price $lmin $lmax $isCrypto")
        if (!CryptoCoins.coins.contains(toGiveName) || !CryptoCoins.coins.contains(toGetName))
        {
            throw notAllowedOrder("cryptocurrency is not allowed")
        }
        if (BigDecimal(maxVolume) <= BigDecimal.ZERO || BigDecimal(priceRatio) <= BigDecimal.ZERO)
        {
            throw notAllowedOrder("not correct coin price")
        }
      //  if (lmin.toBigDecimal() == BigDecimal.ZERO && lmax.toBigDecimal() == BigDecimal.ZERO || lmax.toBigDecimal() < BigDecimal.ZERO || lmin.toBigDecimal() < BigDecimal.ZERO)
         //   throw notAllowedOrder("bad limits")
    }
}

@Serializable
data class order(var owner: String, val info: cryptoOrderInfo, val orderMSG: String?,
                 val isCoin2CoinTrade: Boolean = false, val isFiat2CoinTrade: Boolean = false, val ownerIsBuyer: Boolean = false,
                 val isActive: Boolean = false, val key: String = (ObjectId().toHexString())
) {
    init {
        if ((!isFiat2CoinTrade && !isCoin2CoinTrade) || isCoin2CoinTrade == isFiat2CoinTrade) throw notAllowedOrder("Is will be coin2tocointrade or coin2fiatrade")
    }
    // not factic equals. but equals by some parameters
    override fun equals(other: Any?): Boolean {
        val sEq =  super.equals(other)
        if (other == null) return false
        if (other.javaClass != this.javaClass) return false //instanceof
        val o = other as order
        return o.owner == this.owner &&
                o.orderMSG == o.orderMSG &&
                this.isCoin2CoinTrade == o.isCoin2CoinTrade &&
                this.isFiat2CoinTrade == o.isFiat2CoinTrade &&
                this.ownerIsBuyer == o.ownerIsBuyer
                && this.info.toGiveName == this.info.toGiveName
                && this.info.toGetName == this.info.toGetName
                && this.info.priceRatio == this.info.priceRatio
                && this.info.maxVolume == this.info.maxVolume
                && this.info.minVolume == this.info.minVolume
        // this.key == o.key
        // return  sEq
    }
    companion object {
        init {
            DB.createCollection("orders")
        }
        fun addOrder(owner: String, info: cryptoOrderInfo, orderMSG: String? = null, isCoin2CoinTrade: Boolean = false, isFiat2CoinTrade: Boolean = false, ownerIsBuyer: Boolean = false, isActive: Boolean = false)
        {
            val col = DB.mongoDB.getCollection<order>("orders")
            val ord = order(owner, info, orderMSG, isCoin2CoinTrade, isFiat2CoinTrade, ownerIsBuyer, isActive = isActive)
            if (order.getOrdersByOwner(owner).size > 10) throw notAllowedOrder("you have much orders. remove some firstly")
            //if (ordersExistsForOwner(owner,ord)) throw notAllowedOrder("simillar order exists already. try without another settings")
            val ownBalance = DB.getLoginBalance(ord.owner)?.get(info.toGiveName)!!
            if (ownBalance.balance.toBigDecimal() < info.maxVolume.toBigDecimal()) throw notAllowedOrder("bad balance")
            if (info.toGiveName == info.toGetName) throw notAllowedOrder("toGiveName not will be equal toGetName")
            col.insertOne(ord)
        }

        // http://mongodb.github.io/mongo-java-driver/3.4/javadoc/org/bson/types/ObjectId.html
        /*
            toHexString() Converts this instance into a 24-byte hexadecimal string representation.
            ObjectId(byte[] bytes) Constructs a new instance from the given byte array
         */
        fun remOrder(oID: String)
        {
            // val id = ObjectId(idInHex)
            val col = DB.mongoDB.getCollection<order>("orders")
            col.deleteOne(order::key eq oID)
        }
        fun remOrderByIDAndOwner(oID: String, o: String)
        {
            // val id = ObjectId(idInHex)
            val col = DB.mongoDB.getCollection<order>("orders")
            col.deleteOne(Filters.and(order::key eq oID, order::owner eq o))
        }
        fun remOrder(oID: ObjectId) = remOrder(oID.toHexString())
        fun changeOrderActivityById(id: String, activity: Boolean)
        {
            val col = DB.mongoDB.getCollection<order>("orders")
            col.updateOne(Filters.and(order::key eq id), setValue(order::isActive, activity))
        }
        fun changeOrderActivityByIdAndOwner(id: String, o: String, activity: Boolean)
        {
            val col = DB.mongoDB.getCollection<order>("orders")
            col.updateOne(Filters.and(order::key eq id, order::owner eq o), setValue(order::isActive, activity))
        }
        fun ordersExistsForOwner(o: String, ord: order): Boolean
        {
            val orders = getOrdersByOwner(o)
            orders.forEach() {
                if (it.equals(ord)) return true
            }
            return false
        }
        fun getOrdersByOwner(o: String, lim: Int = 25, skip: Int = 0): List<order>
        {
            val col = DB.mongoDB.getCollection<order>("orders")
            val act = col.find(order::owner eq o).skip(skip).limit(lim)
            val it = act.iterator()
            val r = mutableListOf<order>()
            while(it.hasNext())
            {
                val i = it.next()
                r.add(i)
            }
            return r.toList()
        }
        fun getOrderByID(id: String): order?
        {
            val col = DB.mongoDB.getCollection<order>("orders")
            val it = col.find(order::key eq id).iterator()
            if (!it.hasNext()) return null
            return it.next()
        }
        fun FindIterable<order>.toList_(): List<order>
        {
            val it = this.iterator()
            val r = mutableListOf<order>()
            while(it.hasNext())
            {
                val i = it.next()
                r.add(i)
            }
            return r.toList()
        }
        fun getOrdersByActivityAndCoin(coin: String, s: Boolean = true, lim: Int = 25, skip: Int = 0): List<order>
        {
            val col = DB.mongoDB.getCollection<order>("orders")
            //
            if (!CryptoCoins.coins.contains(coin))
            {
                throw notAllowedOrder("cryptocurrency is not allowed")
            }
            val act = col.find("{isActive: $s, \$or: [{\"info.toGiveName\": \"$coin\"}, {\"info.toGetName\": \"$coin\"}] }")
                .skip(skip).limit(lim)
            val r = act.toList_()
            return r.toList().sortedBy { it.info.priceRatio.toBigDecimal() }
        }
        fun getOrdersByCoinPairAndActivity(coins: Pair<String, String>, s: Boolean = true, lim: Int = 25, skip: Int = 0): List<order>
        {
            val (coin1, coin2) = coins;
            val col = DB.mongoDB.getCollection<order>("orders")
            //
            if (!CryptoCoins.coins.contains(coin1) || !CryptoCoins.coins.contains(coin2))
            {
                throw notAllowedOrder("cryptocurrency is not allowed")
            }
            val act = col.find("{isActive: $s, \$or: [\n" +
                    "  {\$and: [{\"info.toGiveName\": \"$coin1\"}, {\"info.toGetName\": \"$coin2\"}]},\n" +
                    "  {\$and: [{\"info.toGiveName\": \"$coin2\"}, {\"info.toGetName\": \"$coin1\"}]}\n" +
                    "    ] }")
                .skip(skip).limit(lim)
            val r = act.toList_()
            return r.toList().sortedBy { it.info.priceRatio.toBigDecimal() }
        }
        fun getOrdersByActivity(s: Boolean = true, lim: Int = 25, skip: Int = 0): List<order>
        {
            val col = DB.mongoDB.getCollection<order>("orders")
            val act = col.find(order::isActive eq s).skip(skip).limit(lim)
            val r = act.toList_()
            return r.toList()
        }
    }
}
