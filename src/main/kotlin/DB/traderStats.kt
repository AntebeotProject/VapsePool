package org.antibiotic.pool.main.DB

import kotlinx.serialization.Serializable
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.setValue

@Serializable
data class traderStatsStruct(val successfullyTrade: Int = 0, val wrongTrade: Int = 0)
{
    fun addSuccesfully(x: Int = 1) = traderStatsStruct(this.successfullyTrade + x, this.wrongTrade)
    fun addWrong(x: Int = 1) = traderStatsStruct(this.successfullyTrade, this.wrongTrade + x)
}


data class traderStats(val owner: String, val stats: traderStatsStruct = traderStatsStruct())
{
    companion object
    {
        init {
            DB.createCollection("traderStats")
        }
        private val col = DB.mongoDB.getCollection<traderStats>("traderStats")
        fun initTraderStats(o: String)
        {
            col.insertOne(traderStats(o))
        }
        fun getTraderStatsByOwner(o: String): traderStats
        {
            synchronized(DB)
            {
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
            col.updateOne(traderStats::owner eq o, setValue(traderStats::stats, stats))
        }
    }
}
