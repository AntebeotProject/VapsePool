package ru.xmagi.pool.main.PoolServer

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import kotlinx.serialization.Serializable
import org.bitcoinj.core.Coin
import org.bouncycastle.crypto.generators.BCrypt
import org.bson.Document
import org.bson.codecs.pojo.annotations.BsonId
import org.litote.kmongo.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/*
    WARNING USE synchronized EVERYWHERE WHERE (possible) IS CHANGES OF THE DB
 */
const val defCoinName = "GST"
object DB {

    // interface DB {}
    // enum class type {
    //     FILE, MongoDB
    // }
    val createCollection = fun(Name: String) { try { mongoDB.createCollection(Name) } catch (_: com.mongodb.MongoCommandException) { } }

    val MongoDatabase by lazy() {
        Settings.m_propetries.getOrDefault("DatabaseName", defDBName).toString()
    }
    val mongoDB by lazy() {
        val cl = KMongo.createClient()//.coroutine // TODO: add async support?
        cl.getDatabase(MongoDatabase)
        //cl
    }
    // not Implemented yet (FILE)
    val isMongoDB = when (Settings.m_propetries.getOrDefault("DBType", "FILE").toString().lowercase()) {
        "mongodb" -> true //type.MongoDB
        // "file" -> type.FILE,
        else -> false //type.FILE
    }
// Share methods
    private data class share(
        val Login: String,
        val Block: String,
        @BsonId val key: Id<share> = newId(),
        val isBadShare: Boolean = false
    )
    private class min_diff_share(val Login: String, val Data: String, @BsonId val key: Id<share> = newId())
    public fun addShareWithMinimalDifficulty(Login: String, Data: String) {
        println("add SHARE with MINIMAL")
        if (isMongoDB) {
            try {
                mongoDB.createCollection("min_diff_share")
            } catch (_: com.mongodb.MongoCommandException) {
            }
            mongoDB.getCollection<min_diff_share>("min_diff_share").insertOne(min_diff_share(Login, Data))
        } else {
            logger.add_log("[SHARE MIG_DIF] $Login $Data")
        }
    }
    public fun addShare(Login: String, Block: String, isGoodShare: Boolean = true) {
        PoolServer.sharesUptime++
        if (isMongoDB) {
            // println("add SHARE")
            // mongoDB.insertOne(share("",""))
            createCollection("shares")
            mongoDB.getCollection<share>("shares")
                .insertOne(share(Login, Block, isBadShare = !isGoodShare)) //.bulkWrite( )
        } else {
            logger.add_log("[SHARE] $Login $Block")
        }
    }
    // UserBalance And coins methods
    // val uBalances = mutableMapOf<String, BigDecimal>  // Coin Balance Address
    // TODO: maybe disable private
    // userBalance = balances!
    private data class userBalance(val Login: String, val Balance: BigDecimal, val inputAddress: String? = null, val coinname: String, val outputBlocked: Boolean = false, @BsonId val key: Id<userBalance> = newId())
    private fun createUserBalanceIfNotExists(Login: String, coinname: String, col: MongoCollection<userBalance>) {
        if (getLoginBalance(Login)?.get(coinname) == null) {
            col.insertOne(userBalance(Login, 0.0.toBigDecimal(), null, coinname) )
        }
    }
    public fun changeLoginBalance(Login: String, Balance: BigDecimal, coinname: String) {
        createCollection("balances")
        val col = mongoDB.getCollection<userBalance>("balances")
        createUserBalanceIfNotExists(Login, coinname, col = col)
        col.updateOne(Filters.and(userBalance::Login eq Login, userBalance::coinname eq coinname), setValue(userBalance::Balance, Balance))
    }
    public fun changeLoginBalance(Login: String, Balance: Double, coinName: String) = DB.changeLoginBalance(Login, Balance.toBigDecimal(), coinName)
    public fun addToBalance(l: String, b: BigDecimal, coinName: String = defCoinName) {
        val cur_balance = getLoginBalance(l)?.get(coinName)?.balance?.toBigDecimal() ?: 0.0.toBigDecimal()
        changeLoginBalance(l, cur_balance + b, coinName)
    }
    public fun addToBalance(l: String, b: Double, coinName: String) = DB.addToBalance(l, b.toBigDecimal(), coinName)
    // TODO: Double of code? refactor all code maybe
    @Serializable
    data class UserCoinBalance(val owner: String, val CoinName: String, val inputAddress: String, val balance: String, val isBlocked: Boolean = false)
    // Return Login balances with input adddresses. TODO: create another for just get addresses maybe
    public fun getLoginBalance(Login: String): Map<String,UserCoinBalance>? {
        createCollection("balances")
        val col = mongoDB.getCollection<Document>("balances")
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
                rMap.put(coinName, UserCoinBalance(Login, CoinName = coinName, balance = balance, inputAddress = inputAddress, isBlocked = outputBlocked))
            }
            return rMap
        } catch(_: Exception) {
            return null;
        }
    }
    fun filerByUsernameAndCoinname(Login: String, coinname: String) = Filters.and(Filters.eq(userBalance::Login eq Login), userBalance::coinname eq coinname)
    fun getOwnerOfAddress(adr: String): String? {
        val filter = (userBalance::inputAddress eq adr)
        createCollection("balances")
        val col = mongoDB.getCollection<Document>("balances")
        val list: List<Document> = col.find( filter  ).toList()
        if (list.size == 0) return null
        return list.first().get("Login").toString()
    }
    fun isUsedAddress(adr: String): Boolean = getOwnerOfAddress(adr) != null
    public fun getLoginInputAddress(Login: String, coinname: String): String?  {
        createCollection("balances")
        val col = mongoDB.getCollection<Document>("balances")
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
        createCollection("balances")
        val col = mongoDB.getCollection<userBalance>("balances")
        createUserBalanceIfNotExists(Login, coinname, col = col)
        col.updateOne(filerByUsernameAndCoinname(Login, coinname), setValue(userBalance::inputAddress, inputAddress))
    }
    public fun setLoginOutputBlocked(Login: String, coinname: String, isBlocked: Boolean = false) {
        createCollection("balances")
        val col = mongoDB.getCollection<userBalance>("balances")
        createUserBalanceIfNotExists(Login, coinname, col = col)
        col.updateOne(filerByUsernameAndCoinname(Login, coinname), setValue(userBalance::outputBlocked, isBlocked))
    }
    // User Methods + Sessions methods
    private data class users(val Login: String, val Password: String)
    public data class UserSession(val id: String, val owner: String, val createTimeStamp: Long = System.currentTimeMillis())
    private fun hashString(p: String) = BCrypt.generate(p.toByteArray(), verysecretsalt.toByteArray(), 4).toHexString()
    // change to ur value. better use m_propetries
    val verysecretsalt = Settings.m_propetries.getOrDefault("SecretSalt", "123456789ABCDEF-").toString() //"123456789ABCDEF-"
    fun checkUserPassword(l: String, p: String? = null): Boolean? {
        val col = mongoDB.getCollection<Document>("users") as MongoCollection<Document>
        val list : List<Document> = col.find(users::Login eq l).toList()
        if (list.size == 0) return null
        if (p == null) return true

        val login = list.first().get("login").toString()
        val passsword = list.first().get("password").toString()
        val hashedPass = hashString(p)
        if(hashedPass.equals(passsword)) return true
        // println("$hashedPass not equals $passsword")
        return false
    }
    fun addUser(l: String, p: String) {
        createCollection("users")
        val col = mongoDB.getCollection<users>("users") as MongoCollection<users>
        val hashedPass = hashString(p)
        col.insertOne(users(l, Password = hashedPass))
    }

    fun addSession(l: String, p: String): String? {
        if(checkUserPassword(l,p) != true) return null
        createCollection("sessions")
        val col = mongoDB.getCollection<UserSession>("sessions")
        // Session is Login hashed Password Hashed + Time hashed to b64 string is good i think
        // is Just hex string instead create String from byte data like String(....toByteArray())
        val Session = Base64.getEncoder().encode(hashString("$l:$p:${System.currentTimeMillis()}").toByteArray()).toHexString()
        col.insertOne(UserSession(Session, l))
        return Session
    }
    fun getOwnerSessions(owner: String): List<UserSession> {
        createCollection("sessions")
        val col = mongoDB.getCollection<Document>("sessions")
        val list : List<Document> = col.find(UserSession::owner eq owner).toList() // eq is infix function i think. like overloading but infix.
        val rList = mutableListOf<UserSession>()
        for (e in list) {
            val id = e.get("id").toString()
            val owner = e.get("owner").toString()
            val createTimeStamp = e.get("createTimeStamp").toString().toLongOrNull() ?: 0L
            rList.add(UserSession(id, owner, createTimeStamp))
        }
        return rList.toList()
        //return list as List<UserSession>
    }
    fun removeSession(s: String) {
        createCollection("sessions")
        val col = mongoDB.getCollection<UserSession>("sessions")
        col.deleteOne(UserSession::id eq s)
    }
    fun getSession(s: String): UserSession? {
        createCollection("sessions")
        val col = mongoDB.getCollection<Document>("sessions")
        val list : List<Document> = col.find(UserSession::id eq s).toList() // eq is infix function i think. like overloading but infix.
        if (list.size == 0) return null
        val id = list.first().get("id").toString()
        val owner = list.first().get("owner").toString()
        val createTimeStamp = list.first().get("createTimeStamp").toString().toLongOrNull() ?: 0L
        return UserSession(id, owner, createTimeStamp)
    }

    fun sessionExists(s: String): Boolean {
        if (getSession(s) != null) return true
        return false
    }






}