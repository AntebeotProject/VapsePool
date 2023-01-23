package org.antibiotic.pool.main.PoolServer

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import kotlinx.serialization.Serializable
import org.bouncycastle.crypto.generators.BCrypt
import org.bson.Document
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import org.litote.kmongo.*
import org.litote.kmongo.id.toId
import java.math.BigDecimal
import java.util.*

/*
    WARNING USE synchronized EVERYWHERE WHERE (possible) IS CHANGES OF THE DB
 */
// https://mongodb.github.io/mongo-java-driver/3.5/javadoc/com/mongodb/client/model/Filters.html
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
        // @BsonId val key: Id<share> = newId(),
        val isBadShare: Boolean = false
    )
    private class min_diff_share(val Login: String, val Data: String) //, @BsonId val key: Id<share> = newId())
    public fun addShareWithMinimalDifficulty(Login: String, Data: String) {
        // println("add SHARE with MINIMAL")
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
    private data class userBalance(val Login: String,
                                   val Balance: BigDecimal,
                                   val inputAddress: String? = null,
                                   val coinname: String,
                                   val outputBlocked: Boolean = false,
                                   // @BsonId val key: Id<userBalance> = newId() // key for future. not for now.  https://litote.org/kmongo/object-mapping/
    )
    private fun createUserBalanceIfNotExists(Login: String, coinname: String, col: MongoCollection<userBalance>) {
        if (getLoginBalance(Login)?.get(coinname) == null) {
            col.insertOne(userBalance(Login, 0.0.toBigDecimal(), null, coinname) )
        }
    }
    public fun changeLoginBalance(Login: String, Balance: BigDecimal, coinname: String) {
        createCollection("balances")
        val col = mongoDB.getCollection<userBalance>("balances")
        createUserBalanceIfNotExists(Login, coinname, col = col as MongoCollection<userBalance>)
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
    // key from the list not equal to real key value
    fun getBalancesByCoinName(coinname: String): List<UserCoinBalance> {
        createCollection("balances")
        val col = mongoDB.getCollection<Document>("balances")
        val l = col.find(userBalance::coinname eq coinname).toList()
        val r = mutableListOf<UserCoinBalance>()
        for (e in l) {
            val owner = e.get("login").toString() // is broken some time
            val CoinName = e.get("coinname").toString()
            val inputAddress = e.get("inputAddress").toString()
            val balance = e.get("balance").toString()
            val isBlocked = e.get("outputBlocked").toString().toBoolean()
            r.add(UserCoinBalance(owner = owner, CoinName = CoinName, inputAddress = inputAddress, balance = balance, isBlocked = isBlocked ))
        }
        return r
    }
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
    fun filerByUsernameAndCoinname(Login: String, coinname: String) = Filters.and(userBalance::Login eq Login, userBalance::coinname eq coinname)
    fun getOwnerOfAddress(adr: String): String? {
        val filter = (userBalance::inputAddress eq adr)
        createCollection("balances")
        val col = mongoDB.getCollection<Document>("balances")
        val list: List<Document> = col.find(  filter  ).toList()
        if (list.size == 0) return null
        // println(list)
       // println(list.first().get(userBalance::Login.name.lowercase()).toString())
        return list.first().get(userBalance::Login.name.lowercase()).toString() // not use the mechanism in future just change typing lowerUpperUpperUpper...
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
        createUserBalanceIfNotExists(Login, coinname, col = col as MongoCollection<userBalance>)
       // println("$Login:$inputAddress:$coinname")
       // col.find(filerByUsernameAndCoinname(Login, coinname)).toList().forEach() {
       //     println("FILTER" + it)
       // }
        col.updateOne(filerByUsernameAndCoinname(Login, coinname), setValue(userBalance::inputAddress, inputAddress))
    }

    public fun setLoginOutputBlocked(Login: String, coinname: String, isBlocked: Boolean = false) {
        createCollection("balances")
        val col = mongoDB.getCollection<userBalance>("balances")
        createUserBalanceIfNotExists(Login, coinname, col = col as MongoCollection<userBalance>)
        col.updateOne(filerByUsernameAndCoinname(Login, coinname), setValue(userBalance::outputBlocked, isBlocked))
    }
    // User Methods + Sessions methods
    data class users(val Login: String, val Password: String)
    data class UserSession(val id: String, val owner: String, val createTimeStamp: Long = System.currentTimeMillis())
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
    // for administrations
    fun dropUserByLogin(l: String) {
        createCollection("users")
        val col = mongoDB.getCollection<users>("users") as MongoCollection<users>
        col.deleteOne(users::Login eq l)
    }
    fun changeUserPassword(l: String, np: String)
    {
        createCollection("users")
        val col = mongoDB.getCollection<users>("users") as MongoCollection<users>
        val hashedPass = hashString(np)
        col.updateOne(users::Login eq l, setValue(users::Password, hashedPass))
    }

    /*
        * Not was tested. will be deleted maybe.
     */
    fun modifyUser(l: String, user: users) {
            createCollection("users")
            val col = mongoDB.getCollection("users")
            col.updateOne(users::Login eq l, user) // https://stackoverflow.com/questions/47400942/what-does-mean-in-kotlin
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
    fun updateSession(id: String) {
        createCollection("sessions")
        val col = mongoDB.getCollection<UserSession>("sessions")
        col.updateOne(UserSession::id eq id, setValue(UserSession::createTimeStamp, System.currentTimeMillis()))
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

    // @Serializable
    data class tx(val owner: String, val coinname: String, val hash: String, val firstFound: Long = System.currentTimeMillis(), val isConfirmed: Boolean = false)
    public fun getTX(hash: String): tx? {
        createCollection("transactions")
        val col = mongoDB.getCollection<Document>("transactions")
        val list : List<Document> = col.find(tx::hash eq hash).toList()
        if (list.size == 0) return null
        val owner = list.first().get("owner").toString()
        val coinname = list.first().get("coinname").toString()
        val hash = list.first().get("hash").toString()
        val firstFound = list.first().get("firstFound").toString().toLong()
        val isConfirmed = list.first().get("isConfirmed").toString().toBoolean()
        return tx(owner,coinname, hash, firstFound, isConfirmed)
    }
    fun userHaveNotConfirmedTXOnCoinName(o: String, cn: String): Boolean {
        val col = mongoDB.getCollection<Document>("transactions")
        val list : List<Document> = col.find(Filters.and(tx::owner eq o, tx::isConfirmed eq false, tx::coinname eq cn)).toList()
        return list.size > 0
    }
    // not tested too. will be deleted in future realisations. not need in future
    fun getTXsByConfirm(status: Boolean = false): List<tx>
    {
        val col = mongoDB.getCollection<Document>("transactions")
        val list : List<Document> = col.find(tx::isConfirmed eq status).toList()
        if (list.size == 0) return return listOf<tx>()
        val r = mutableListOf<tx>()
        for(i in list) {
            val owner = list.first().get("owner").toString()
            val coinname = list.first().get("coinname").toString()
            val hash = list.first().get("hash").toString()
            val firstFound = list.first().get("firstFound").toString().toLong()
            val isConfirmed = list.first().get("isConfirmed").toString().toBoolean()
            r.add(tx(owner,coinname, hash, firstFound, isConfirmed))
        }
        return r
    }
    /*
    * Not was tested. will be deleted maybe.
 */
    public fun deleteTX(hash: String) {
        createCollection("transactions")
        val col = mongoDB.getCollection<Document>("transactions")
        col.deleteOne(tx::hash eq hash)
    }

    public fun setTXConfirmed(hash: String, status: Boolean) {
        createCollection("transactions")
        val col = mongoDB.getCollection("transactions")
        // col.updateOne(Filters.and(userBalance::Login eq Login, userBalance::coinname eq coinname), setValue(userBalance::Balance, Balance))
        col.updateOne(tx::hash eq hash, setValue(tx::isConfirmed, status))
    }
    /*
        * Not was tested. will be deleted maybe.
     */
    public fun modifyTX(hash: String, tx: tx) {
        createCollection("transactions")
        val col = mongoDB.getCollection("transactions")
        // col.updateOne(Filters.and(userBalance::Login eq Login, userBalance::coinname eq coinname), setValue(userBalance::Balance, Balance))
        col.updateOne(tx::hash eq hash, tx)
    }
    public fun addTX(t: tx) {
        createCollection("transactions")
        val col = mongoDB.getCollection<tx>("transactions")
        col.insertOne(t)
    }
    @Serializable
    data class notification(val owner: String, val msg: String, val time: Long) // , @BsonId val key: Id<notification> = newId()

    public fun createNewNotification(o: String, m: String, t: Long = System.currentTimeMillis())
    {
        createCollection("notifications")
        val col = mongoDB.getCollection<notification>("notifications")
        col.insertOne(notification(o, m, t))

    }
   /* public fun dropNotificationByKey(@BsonId key: Id<notification>) {
        createCollection("notifications")
        val col = mongoDB.getCollection<notification>("notifications")
        col.deleteOne(notification::key eq key)
    }*/
    // Will to delele notifications that less by time than tLimit
    public fun dropNotificationByTimeAndOwner(o: String, tLimit: Long) {
        createCollection("notifications")
        val col = mongoDB.getCollection<notification>("notifications")
        col.deleteOne(Filters.and(notification::time lt tLimit, notification::owner eq o))
    }
    /*
        Return notifications that more than tLimit. so if you
     */
    fun getNotificationsByOwnerAndTimestamp(o: String, tLimit: Long): List<notification> {
        createCollection("notifications")
        val col = mongoDB.getCollection<Document>("notifications")
        val l = col.find(notification::owner eq o, notification::time gt tLimit).toList()
        val r = mutableListOf<notification>()
        for(n in l) {
            val owner = n.get("owner").toString()
            val msg = n.get("msg").toString()
            val time = n.get("time").toString().toLong()
            // val key = ObjectId(n.get("_id").toString()).toId<notification>()
            r.add(notification(owner,msg,time)) //,key))
        }
        return r
    }
    // Orders
    class notAllowedOrder(w: String): Exception("Not allowed order. $w");
    data class toSellStruct(val name: String, val price: BigDecimal)
    data class order(val owner: String, val whatSell: toSellStruct, val whatBuy: toSellStruct,
                     val isCoin2CoinTrade: Boolean = false, val isFiat2CoinTrade: Boolean = false, val ownerIsBuyer: Boolean = false,
                     val isActive: Boolean = false, @BsonId val key: Id<order> = newId()
    ) {
        init {
            if ((!isFiat2CoinTrade && !isCoin2CoinTrade) || isCoin2CoinTrade == isFiat2CoinTrade) throw notAllowedOrder("Is will be coin2tocointrade or coin2fiatrade")
        }
    }
    fun addOrder(owner: String, toSell: toSellStruct, toBuy: toSellStruct, isCoin2CoinTrade: Boolean = false, ownerIsBuyer: Boolean = false)
    {
        createCollection("orders")
        val col = mongoDB.getCollection<order>("orders")
        col.insertOne(order(owner, toSell, toBuy, isCoin2CoinTrade, ownerIsBuyer))
    }

    // http://mongodb.github.io/mongo-java-driver/3.4/javadoc/org/bson/types/ObjectId.html
    /*
        toHexString() Converts this instance into a 24-byte hexadecimal string representation.
        ObjectId(byte[] bytes) Constructs a new instance from the given byte array
     */
    fun remOrder(oID: ObjectId)
    {
        // val id = ObjectId(idInHex)
        val col = mongoDB.getCollection<order>("orders")
        col.deleteOne(order::key eq oID.toId())
    }

    fun changeOrderActivityByIdAndOwner(owner: String, id: Id<order>, activity: Boolean)
    {
        val col = mongoDB.getCollection<order>("orders")
        col.updateOne(Filters.and(order::owner eq owner, order::key eq id), setValue(order::isActive, activity))
    }
    fun getOrdersByActivity(s: Boolean = true): List<DB.order>
    {
        val col = mongoDB.getCollection<order>("orders")
        val act = col.find(order::isActive eq s)
        val it = act.iterator()
        val r = mutableListOf<DB.order>()
        while(it.hasNext())
        {
            val i = it.next()
            r.add(i)
        }
        return r.toList()
    }
    // trade_stata
    // use forum on phpbb for reviews maybe?
    data class review(val reviewer: String, val about: String, val text: String, val isPositive: Boolean, @BsonId val key: Id<review> = newId())
    //
    fun addReview(reviewer: String, about: String, text: String, isPositive: Boolean = false)
    {
        synchronized(DB)
        {
            createCollection("traderStatsReview")
            val col = mongoDB.getCollection<review>("traderStatsReview")
            col.insertOne(review(reviewer, about, text, isPositive))
        }
    }
    fun remReview(oID: ObjectId)
    {
        // val id = ObjectId(idInHex)
        val col = mongoDB.getCollection<review>("traderStatsReview")
        col.deleteOne(review::key eq oID.toId<review>())
    }
    fun getReviewsByReviewer(r: String): List<review>
    {
        val col = mongoDB.getCollection<review>("traderStatsReview")
        val s = col.find(review::reviewer eq r)
        val id  = s.iterator()
        val r = mutableListOf<review>()
        while(id.hasNext())
        {
            r.add(id.next())
        }
        return r.toList()
    }
    fun getReviewsByAbot(r: String): List<review>
    {
        val col = mongoDB.getCollection<review>("traderStatsReview")
        val s = col.find(review::about eq r)
        val id  = s.iterator()
        val r = mutableListOf<review>()
        while(id.hasNext())
        {
            r.add(id.next())
        }
        return r.toList()
    }
    data class traderStatsStruct(val successfullyTrade: Int = 0, val wrongTrade: Int = 0)
    {
        fun addSuccesfully(x: Int = 1) = traderStatsStruct(this.successfullyTrade + x, this.wrongTrade)
        fun addWrong(x: Int = 1) = traderStatsStruct(this.successfullyTrade, this.wrongTrade + x)
    }
    data class traderStats(val owner: String, val stats: traderStatsStruct = traderStatsStruct())
    fun initTraderStats(o: String)
    {
        createCollection("traderStats")
        val col = mongoDB.getCollection<traderStats>("traderStats")
        col.insertOne(traderStats(o))
    }
    fun getTraderStatsByOwner(o: String): traderStats
    {
        synchronized(DB)
        {
            createCollection("traderStats")
            val col = mongoDB.getCollection<traderStats>("traderStats")
            val s = col.find(traderStats::owner eq o)
            if (!s.iterator().hasNext()) {
                initTraderStats(o)
                return getTraderStatsByOwner(o)
            }
            val it = s.iterator()
            return it.next()
            // todo: if trader have more statts then is there is bug.
        }
    }
    fun changeTraderStatsByOwner(o: String, stats: traderStatsStruct)
    {
        createCollection("traderStats")
        val col = mongoDB.getCollection<traderStats>("traderStats")
        col.updateOne(traderStats::owner eq o, setValue(traderStats::stats, stats))
    }

}