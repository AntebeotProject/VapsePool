package org.antibiotic.pool.main.DB

import com.mongodb.client.model.Filters
import kotlinx.serialization.Serializable
import org.antibiotic.pool.main.CryptoCurrencies.CryptoCoins
import org.bson.Document
import org.litote.kmongo.*
import java.math.BigDecimal

@Serializable
data class notification(val owner: String, val msg: String, val time: Long) // , @BsonId val key: Id<notification> = newId()
{
    companion object
    {
        init {
            DB.createCollection("notifications")
        }
        private val col = DB.mongoDB.getCollection<notification>("notifications")
        fun createNewNotification(o: String, m: String, t: Long = System.currentTimeMillis())
        {
            col.insertOne(notification(o, m, t))
        }
        /* public fun dropNotificationByKey(@BsonId key: Id<notification>) {
             createCollection("notifications")
             val col = mongoDB.getCollection<notification>("notifications")
             col.deleteOne(notification::key eq key)
         }*/
        // Will to delele notifications that less by time than tLimit
        fun dropNotificationByTimeAndOwner(o: String, tLimit: Long) {
            col.deleteOne(Filters.and(notification::time lt tLimit, notification::owner eq o))
        }
        /*
            Return notifications that more than tLimit. so if you
         */
        fun getNotificationsByOwnerAndTimestamp(o: String, tLimit: Long): List<notification> {
            val col = DB.mongoDB.getCollection<Document>("notifications")
            val l = col.find( Filters.or(notification::owner eq o, notification::owner eq "_ANYUSER_"), notification::time gt tLimit).toList()
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
    }
}