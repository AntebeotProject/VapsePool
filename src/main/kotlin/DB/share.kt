package org.antibiotic.pool.main.DB

import org.antibiotic.pool.main.PoolServer.PoolServer
import org.antibiotic.pool.main.PoolServer.logger
import org.litote.kmongo.getCollection

internal class
// Share methods
data class share(
    val Login: String,
    val Block: String,
    // @BsonId val key: Id<share> = newId(),
    val isBadShare: Boolean = false
)
{
    companion object {
        fun add(Login: String, Block: String, isGoodShare: Boolean = true) {
            PoolServer.sharesUptime++
            if (DB.isMongoDB) {
                // println("add SHARE")
                // mongoDB.insertOne(share("",""))
                DB.createCollection("shares")
                DB.mongoDB.getCollection<share>("shares")
                    .insertOne(share(Login, Block, isBadShare = !isGoodShare)) //.bulkWrite( )
            } else {
                logger.add_log("[SHARE] $Login $Block")
            }
        }
    }
}
data class min_diff_share(val Login: String, val Data: String) //, @BsonId val key: Id<share> = newId())
{
    companion object {
        fun add(Login: String, Data: String) {
            // println("add SHARE with MINIMAL")
            if (DB.isMongoDB) {
                try {
                    DB.mongoDB.createCollection("min_diff_share")
                } catch (_: com.mongodb.MongoCommandException) {
                }
                DB.mongoDB.getCollection<min_diff_share>("min_diff_share").insertOne(min_diff_share(Login, Data))
            } else {
                logger.add_log("[SHARE MIG_DIF] $Login $Data")
            }
        }
    }
}