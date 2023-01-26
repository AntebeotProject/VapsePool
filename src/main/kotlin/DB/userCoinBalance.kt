package org.antibiotic.pool.main.DB

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import kotlinx.serialization.Serializable
import org.bson.Document
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.setValue

@Serializable
data class UserCoinBalance(val owner: String, val CoinName: String, val inputAddress: String, val balance: String, val isBlocked: Boolean = false)
{
    companion object {
        // key from the list not equal to real key value
        fun getBalancesByCoinName(coinname: String): List<UserCoinBalance> {
            DB.createCollection("balances")
            val col = DB.mongoDB.getCollection<Document>("balances")
            val l = col.find(userBalance::coinname eq coinname).toList()
            val r = mutableListOf<UserCoinBalance>()
            for (e in l) {
                val owner = e.get("login").toString() // is broken some time
                val CoinName = e.get("coinname").toString()
                val inputAddress = e.get("inputAddress").toString()
                val balance = e.get("balance").toString()
                val isBlocked = e.get("outputBlocked").toString().toBoolean()
                r.add(
                    UserCoinBalance(
                        owner = owner,
                        CoinName = CoinName,
                        inputAddress = inputAddress,
                        balance = balance,
                        isBlocked = isBlocked
                    )
                )
            }
            return r
        }
        // Return Login balances with input adddresses. TODO: create another for just get addresses maybe
        public fun getLoginBalance(Login: String): Map<String, UserCoinBalance>? {
            DB.createCollection("balances")
            val col = DB.mongoDB.getCollection<Document>("balances")
            try {
                val list : List<Document> = col.find(userBalance::Login eq Login).toList()
                if(list.size == 0) {
                    return null // user not found
                }
                val rMap = mutableMapOf<String, UserCoinBalance>()
                for (e in list) {
                    val balance = e.get("balance").toString()//.toBigDecimal()
                    val coinName = e.get("coinname").toString()
                    val inputAddress = e.get("inputAddress").toString()
                    val outputBlocked = e.get("outputBlocked").toString().toBoolean()
                    rMap.put(coinName,
                        UserCoinBalance(
                            Login,
                            CoinName = coinName,
                            balance = balance,
                            inputAddress = inputAddress,
                            isBlocked = outputBlocked
                        )
                    )
                }
                return rMap
            } catch(_: Exception) {
                return null;
            }
        }
        fun filerByUsernameAndCoinname(Login: String, coinname: String) = Filters.and(userBalance::Login eq Login, userBalance::coinname eq coinname)
        fun getOwnerOfAddress(adr: String): String? {
            val filter = (userBalance::inputAddress eq adr)
            DB.createCollection("balances")
            val col = DB.mongoDB.getCollection<Document>("balances")
            val list: List<Document> = col.find(  filter  ).toList()
            if (list.size == 0) return null
            // println(list)
            // println(list.first().get(userBalance::Login.name.lowercase()).toString())
            return list.first().get(userBalance::Login.name.lowercase()).toString() // not use the mechanism in future just change typing lowerUpperUpperUpper...
        }
        fun isUsedAddress(adr: String): Boolean = getOwnerOfAddress(adr) != null
        public fun getLoginInputAddress(Login: String, coinname: String): String?  {
            DB.createCollection("balances")
            val col = DB.mongoDB.getCollection<Document>("balances")
            try {
                val filter = filerByUsernameAndCoinname(Login, coinname)
                val list: List<Document> = col.find( filter  ).toList()
                if (list.size == 0) return null
                return list.first().get("inputAddress").toString()
            } catch(_: Exception) {
                return null
            }
        }
        public fun setLoginInputAddress(Login: String, inputAddress: String, coinname: String) {
            DB.createCollection("balances")
            val col = DB.mongoDB.getCollection<userBalance>("balances")
            DB.createUserBalanceIfNotExists(Login, coinname, col = col as MongoCollection<userBalance>)
            // println("$Login:$inputAddress:$coinname")
            // col.find(filerByUsernameAndCoinname(Login, coinname)).toList().forEach() {
            //     println("FILTER" + it)
            // }
            col.updateOne(filerByUsernameAndCoinname(Login, coinname), setValue(userBalance::inputAddress, inputAddress))
        }

        public fun setLoginOutputBlocked(Login: String, coinname: String, isBlocked: Boolean = false) {
            DB.createCollection("balances")
            val col = DB.mongoDB.getCollection<userBalance>("balances")
            DB.createUserBalanceIfNotExists(Login, coinname, col = col as MongoCollection<userBalance>)
            col.updateOne(filerByUsernameAndCoinname(Login, coinname), setValue(userBalance::outputBlocked, isBlocked))
        }
    }
}