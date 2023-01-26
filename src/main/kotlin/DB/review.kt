package org.antibiotic.pool.main.DB

import com.mongodb.client.model.Filters
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection

// reviews
class notAllowedReview(w: String) : Exception("not allowed review: $w")
@Serializable
data class review(val reviewer: String, val about: String, val text: String, val tradeID: String, val isPositive: Boolean,
                  val key: String = (ObjectId().toHexString()) ) // @BsonId val key: Id<review>
{
    companion object
    {
        //
        init {
            DB.createCollection("traderStatsReview")
        }
        fun addReview(reviewer: String, about: String, text: String, tradeID: String, isPositive: Boolean = false)
        {
            synchronized(DB)
            {
                if (getReviewsBytradeIDAndReviewer(tradeID, reviewer).size > 0) throw notAllowedReview("review for this trade already exists")
                val col = DB.mongoDB.getCollection<review>("traderStatsReview")
                col.insertOne(review(reviewer, about, text, tradeID, isPositive))
                val aboutStat = if (isPositive) traderStats.getTraderStatsByOwner(about).stats.addSuccesfully() else traderStats.getTraderStatsByOwner(
                    about
                ).stats.addWrong()
                traderStats.changeTraderStatsByOwner(about, aboutStat)
            }
        }
        fun remReview(oID: ObjectId) = remReview(oID.toHexString())
        fun remReview(oID: String)
        {
            // val id = ObjectId(idInHex)
            val col = DB.mongoDB.getCollection<review>("traderStatsReview")
            col.deleteOne(review::key eq oID)
        }
        fun getReviewsBytradeIDAndReviewer(id: String, rev: String, lim: Int = 5, skip: Int = 0): List<review>
        {
            val col = DB.mongoDB.getCollection<review>("traderStatsReview")
            val s = col.find(Filters.and(review::tradeID eq id, review::reviewer eq rev)).skip(skip).limit(lim)
            return s.iterator().asSequence().toList()
        }
        fun getReviewsByReviewer(r: String, lim: Int = 5, skip: Int = 0): List<review>
        {
            val col = DB.mongoDB.getCollection<review>("traderStatsReview")
            val s = col.find(review::reviewer eq r).skip(skip).limit(lim)
            return  s.iterator().asSequence().toList()
        }
        fun getReviewsByAbout(r: String, lim: Int = 5, skip: Int = 0): List<review>
        {
            val col = DB.mongoDB.getCollection<review>("traderStatsReview")
            val s = col.find(review::about eq r).skip(skip).limit(lim)
            val id  = s.iterator()
            val r = mutableListOf<review>()
            while(id.hasNext())
            {
                r.add(id.next())
            }
            return r.toList()
        }
        fun getReviewsByWho(who: String, lim: Int = 5, skip: Int = 0): List<review>
        {
            val col = DB.mongoDB.getCollection<review>("traderStatsReview")
            val s = col.find(Filters.or(review::reviewer eq who, review::about eq who)).skip(skip).limit(lim)
            return  s.iterator().asSequence().toList()
        }

    }
}