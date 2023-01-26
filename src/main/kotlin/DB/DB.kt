package org.antibiotic.pool.main.DB

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import org.antibiotic.pool.main.PoolServer.*
import org.bson.types.ObjectId
import org.litote.kmongo.*
import java.math.BigDecimal

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

    // fun addShareWithMinimalDifficulty(Login: String, Data: String) = min_diff_share.add(Login, Data)
    fun addShare(Login: String, Block: String, isGoodShare: Boolean = true) = share.add(Login, Block, isGoodShare)
    // UserBalance And coins methods
    // TODO: maybe disable private
    fun createUserBalanceIfNotExists(Login: String, coinname: String, col: MongoCollection<userBalance>) = userBalance.createUserBalanceIfNotExists(Login, coinname, col)
    // fun changeLoginBalance(Login: String, Balance: BigDecimal, coinname: String) = userBalance.changeLoginBalance(Login, Balance, coinname)
    fun changeLoginBalance(Login: String, Balance: Double, coinName: String) = userBalance.changeLoginBalance(Login, Balance, coinName)
    fun addToBalance(l: String, b: BigDecimal, coinName: String = defCoinName) = userBalance.addToBalance(l, b, coinName)
    fun addToBalance(l: String, b: Double, coinName: String) = userBalance.addToBalance(l, b, coinName)

    // TODO: Double of code? refactor all code maybe
    // key from the list not equal to real key value
    fun getBalancesByCoinName(coinname: String): List<UserCoinBalance> = UserCoinBalance.getBalancesByCoinName(coinname)
    // Return Login balances with input adddresses. TODO: create another for just get addresses maybe
    fun getLoginBalance(Login: String): Map<String, UserCoinBalance>? = UserCoinBalance.getLoginBalance(Login)
    // fun filerByUsernameAndCoinname(Login: String, coinname: String) = Filters.and(userBalance::Login eq Login, userBalance::coinname eq coinname)
    fun getOwnerOfAddress(adr: String): String? = UserCoinBalance.getOwnerOfAddress(adr)
    fun isUsedAddress(adr: String): Boolean = UserCoinBalance.isUsedAddress(adr)
    fun getLoginInputAddress(Login: String, coinname: String): String?  = UserCoinBalance.getLoginInputAddress(Login, coinname)
    fun setLoginInputAddress(Login: String, inputAddress: String, coinname: String) = UserCoinBalance.setLoginInputAddress(Login, inputAddress, coinname)
    // fun setLoginOutputBlocked(Login: String, coinname: String, isBlocked: Boolean = false) = UserCoinBalance.setLoginOutputBlocked(Login, coinname, isBlocked)

    fun checkUserPassword(l: String, p: String? = null): Boolean? = users.checkUserPassword(l, p)
    fun addUser(l: String, p: String) = users.addUser(l, p)
    // for administrations
    // fun dropUserByLogin(l: String)  = users.dropUserByLogin(l)
    fun changeUserPassword(l: String, np: String) = users.changeUserPassword(l, np)
    /*
        * Not was tested. will be deleted maybe.
     */
   // fun modifyUser(l: String, user: users) = users.modifyUser(l, user)
    fun addSession(l: String, p: String): String? = UserSession.addSession(l, p)
    fun updateSession(id: String) = UserSession.updateSession(id)
    // fun getOwnerSessions(owner: String): List<UserSession> = UserSession.getOwnerSessions(owner)
    fun removeSession(s: String)  = UserSession.removeSession(s)
    fun getSession(s: String): UserSession? = UserSession.getSession(s)
    // fun sessionExists(s: String): Boolean = UserSession.sessionExists(s)

    fun getTX(hash: String): tx? = tx.getTX(hash)
    fun userHaveNotConfirmedTXOnCoinName(o: String, cn: String): Boolean = tx.userHaveNotConfirmedTXOnCoinName(o, cn)
    // not tested too. will be deleted in future realisations. not need in future
    fun getTXsByConfirm(status: Boolean = false): List<tx> = tx.getTXsByConfirm(status)
    /*
    * Not was tested. will be deleted maybe.
 */
   // public fun deleteTX(hash: String) = tx.deleteTX(hash)

    public fun setTXConfirmed(hash: String, status: Boolean) = tx.setTXConfirmed(hash, status)
    /*
        * Not was tested. will be deleted maybe.
     */
    public fun modifyTX(hash: String, tx: tx) {
        DB.createCollection("transactions")
        val col = DB.mongoDB.getCollection("transactions")
        // col.updateOne(Filters.and(userBalance::Login eq Login, userBalance::coinname eq coinname), setValue(userBalance::Balance, Balance))
        col.updateOne(tx::hash eq hash, tx)
    }
    public fun addTX(t: tx) {
            DB.createCollection("transactions")
            val col = DB.mongoDB.getCollection<tx>("transactions")
            col.insertOne(t)
    }
    // notifications
    fun createNewNotification(o: String, m: String, t: Long = System.currentTimeMillis()) = notification.createNewNotification(o, m, t)
   /* public fun dropNotificationByKey(@BsonId key: Id<notification>) {
        createCollection("notifications")
        val col = mongoDB.getCollection<notification>("notifications")
        col.deleteOne(notification::key eq key)
    }*/
    // Will to delele notifications that less by time than tLimit
    // fun dropNotificationByTimeAndOwner(o: String, tLimit: Long) = notification.dropNotificationByTimeAndOwner(o, tLimit)
    /*
        Return notifications that more than tLimit. so if you
     */
    fun getNotificationsByOwnerAndTimestamp(o: String, tLimit: Long): List<notification> = notification.getNotificationsByOwnerAndTimestamp(o, tLimit)
    // Orders
    // fun addOrder(owner: String, toSell: toSellStruct, toBuy: toSellStruct, orderMSG: String? = null, isCoin2CoinTrade: Boolean = false, isFiat2CoinTrade: Boolean = false, ownerIsBuyer: Boolean = false)
    // = order.addOrder(owner, toSell, toBuy, orderMSG, isCoin2CoinTrade, isFiat2CoinTrade, ownerIsBuyer)

    // http://mongodb.github.io/mongo-java-driver/3.4/javadoc/org/bson/types/ObjectId.html
    /*
        toHexString() Converts this instance into a 24-byte hexadecimal string representation.
        ObjectId(byte[] bytes) Constructs a new instance from the given byte array
     */
    fun remOrder(oID: String) = order.remOrder(oID)
    /*
    fun remOrderByIDAndOwner(oID: String, o: String) = order.remOrderByIDAndOwner(oID, o)
    fun remOrder(oID: ObjectId) = order.remOrder(oID.toHexString())
    fun changeOrderActivityById(id: String, activity: Boolean) = order.changeOrderActivityById(id, activity)
    fun changeOrderActivityByIdAndOwner(id: String, o: String, activity: Boolean) = order.changeOrderActivityByIdAndOwner(id, o, activity)
    fun ordersExistsForOwner(o: String, ord: order): Boolean = order.ordersExistsForOwner(o, ord)
    fun getOrdersByOwner(o: String): List<order> = order.getOrdersByOwner(o)
     */
    fun getOrderByID(id: String): order? = order.getOrderByID(id)
    fun getOrdersByActivity(s: Boolean = true): List<order> = order.getOrdersByActivity(s)
    // trades
    // use forum on phpbb for reviews maybe?
    fun doTrade(whoBuy: String, count: String, orderID: String): String = trade.doTrade(whoBuy, count, orderID)
    fun getDoneTradeByBuyerOrSeller(who: String): List<trade> = trade.getDoneTradeByBuyerOrSeller(who)
    fun getDoneTradeByID(id: String): List<trade> = trade.getDoneTradeByID(id)
    // reviews
    fun addReview(reviewer: String, about: String, text: String, tradeID: String, isPositive: Boolean = false)
    = review.addReview(reviewer, about, text, tradeID, isPositive)
    fun remReview(oID: ObjectId) = review.remReview(oID.toHexString())
    fun remReview(oID: String) = review.remReview(oID)
    fun getReviewsBytradeIDAndReviewer(id: String, rev: String): List<review> = review.getReviewsBytradeIDAndReviewer(id, rev)
    fun getReviewsByReviewer(r: String): List<review> = review.getReviewsByReviewer(r)
    fun getReviewsByAbout(r: String): List<review> = review.getReviewsByAbout(r)
    fun getReviewsByWho(who: String): List<review> = review.getReviewsByWho(who)

    // fun initTraderStats(o: String) = traderStats.initTraderStats(o)
    fun getTraderStatsByOwner(o: String): traderStats = traderStats.getTraderStatsByOwner(o)
    fun changeTraderStatsByOwner(o: String, stats: traderStatsStruct) = traderStats.changeTraderStatsByOwner(o, stats)
    // All that above is old style. короче говоря, всё что выше лишь для того что бы было меньше заморочки. теперь всё что связанно с БД лучше искать отдельно.
    // можно через grep
    // новый стиль должен быть как в userSettings. так же notification должны +- поменяться
    // с другой стороны это больше памяти во время не использования БД.
    // с одной быстрее чем каждый раз создавать
}