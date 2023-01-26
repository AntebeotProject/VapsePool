package org.antibiotic.pool.main.DB

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.setValue
import java.math.BigDecimal

data class userBalance(val Login: String,
                       val Balance: BigDecimal,
                       val inputAddress: String? = null,
                       val coinname: String,
                       val outputBlocked: Boolean = false,
    // @BsonId val key: Id<userBalance> = newId() // key for future. not for now.  https://litote.org/kmongo/object-mapping/
)
{
    companion object {
        fun createUserBalanceIfNotExists(Login: String, coinname: String, col: MongoCollection<userBalance>) {
            if (DB.getLoginBalance(Login)?.get(coinname) == null) {
                col.insertOne(userBalance(Login, 0.0.toBigDecimal(), null, coinname) )
            }
        }
        fun changeLoginBalance(Login: String, Balance: BigDecimal, coinname: String) {
            DB.createCollection("balances")
            val col = DB.mongoDB.getCollection<userBalance>("balances")
            createUserBalanceIfNotExists(Login, coinname, col = col as MongoCollection<userBalance>)
            col.updateOne(Filters.and(userBalance::Login eq Login, userBalance::coinname eq coinname), setValue(userBalance::Balance, Balance))
        }

        fun changeLoginBalance(Login: String, Balance: Double, coinName: String) = changeLoginBalance(Login, BigDecimal(Balance.toString()), coinName)

        // toString and after to BigDecimal better
        fun addToBalance(l: String, b: BigDecimal, coinName: String = defCoinName) {
            val cur_balance = DB.getLoginBalance(l)?.get(coinName)?.balance?.toBigDecimal() ?: 0.0.toBigDecimal()
            changeLoginBalance(l, cur_balance.add(b), coinName)
        }
        fun addToBalance(l: String, b: Double, coinName: String) = addToBalance(l, BigDecimal(b.toString()), coinName)

    }
}