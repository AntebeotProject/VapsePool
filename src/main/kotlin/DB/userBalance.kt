package org.antibiotic.pool.main.DB

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.setValue
import java.math.BigDecimal
import java.math.RoundingMode

data class userBalance(val Login: String,
                       val Balance: BigDecimal,
                       val inputAddress: String? = null,
                       val coinname: String,
                       val outputBlocked: Boolean = false,
    // @BsonId val key: Id<userBalance> = newId() // key for future. not for now.  https://litote.org/kmongo/object-mapping/
)
{
    companion object {
        init {
            DB.createCollection("balances")
        }
        private val m_col = DB.mongoDB.getCollection<userBalance>("balances")
        fun createUserBalanceIfNotExists(Login: String, coinname: String, col: MongoCollection<userBalance> = m_col) {
            if (DB.getLoginBalance(Login)?.get(coinname) == null) {
                col.insertOne(userBalance(Login, 0.0.toBigDecimal(), null, coinname) )
            }
        }
        fun changeLoginBalance(Login: String, Balance: BigDecimal, coinname: String) {
            // val col = DB.mongoDB.getCollection<userBalance>("balances")
            createUserBalanceIfNotExists(Login, coinname)
            m_col.updateOne(Filters.and(userBalance::Login eq Login, userBalance::coinname eq coinname), setValue(userBalance::Balance, Balance))
        }

        fun setBlockOutputForCoinName(Login: String, coinName: String, isBlocked: Boolean = true)
        {
            createUserBalanceIfNotExists(Login, coinName)
            m_col.updateOne(Filters.and(userBalance::Login eq Login, userBalance::coinname eq coinName), setValue(userBalance::outputBlocked, isBlocked));
        }

        fun changeLoginBalance(Login: String, Balance: Double, coinName: String) = changeLoginBalance(Login, BigDecimal(Balance.toString()), coinName)
        const val toSatoshiNums = 8
        // toString and after to BigDecimal better
        fun addToBalance(l: String, b_: BigDecimal, coinName: String = defCoinName) {
            val b = if (b_ < BigDecimal.ZERO)  b_.setScale(10, RoundingMode.CEILING); else b_.setScale(10, RoundingMode.FLOOR);
            val cur_balance = DB.getLoginBalance(l)?.get(coinName)?.balance?.toBigDecimal() ?: 0.0.toBigDecimal()
            val withPowCurBalance = cur_balance * BigDecimal.TEN.pow(toSatoshiNums)
            val withPowB = b  * BigDecimal.TEN.pow(toSatoshiNums)
            val newBalance = withPowCurBalance.add(withPowB).divide( BigDecimal.TEN.pow(toSatoshiNums) )

            changeLoginBalance(l, newBalance, coinName)
        }
        fun addToBalance(l: String, b: Double, coinName: String) = addToBalance(l, BigDecimal(b.toString()), coinName)

    }
}