package org.antibiotic.pool.main.DB

import com.mongodb.client.model.Filters
import org.bson.Document
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.setValue
import org.litote.kmongo.updateOne

// @Serializable
data class tx(val owner: String, val coinname: String, val hash: String, val firstFound: Long = System.currentTimeMillis(), val isConfirmed: Boolean = false)
{

    companion object {
        init {
            DB.createCollection("transactions")

        }
        private val col = DB.mongoDB.getCollection<Document>("transactions")
        fun getTX(hash: String): tx? {
            val list: List<Document> = col.find(tx::hash eq hash).toList()
            if (list.size == 0) return null
            val owner = list.first().get("owner").toString()
            val coinname = list.first().get("coinname").toString()
            val hash = list.first().get("hash").toString()
            val firstFound = list.first().get("firstFound").toString().toLong()
            val isConfirmed = list.first().get("isConfirmed").toString().toBoolean()
            return tx(owner, coinname, hash, firstFound, isConfirmed)
        }

        fun userHaveNotConfirmedTXOnCoinName(o: String, cn: String): Boolean {
            val list: List<Document> =
                col.find(Filters.and(tx::owner eq o, tx::isConfirmed eq false, tx::coinname eq cn)).toList()
            return list.size > 0
        }

        // not tested too. will be deleted in future realisations. not need in future
        fun getTXsByConfirm(status: Boolean = false): List<tx> {
            val list: List<Document> = col.find(tx::isConfirmed eq status).toList()
            if (list.size == 0) return return listOf<tx>()
            val r = mutableListOf<tx>()
            for (i in list) {
                val owner = list.first().get("owner").toString()
                val coinname = list.first().get("coinname").toString()
                val hash = list.first().get("hash").toString()
                val firstFound = list.first().get("firstFound").toString().toLong()
                val isConfirmed = list.first().get("isConfirmed").toString().toBoolean()
                r.add(tx(owner, coinname, hash, firstFound, isConfirmed))
            }
            return r
        }
        fun deleteTX(hash: String) {
            col.deleteOne(tx::hash eq hash)
        }
        fun setTXConfirmed(hash: String, status: Boolean) {
            // col.updateOne(Filters.and(userBalance::Login eq Login, userBalance::coinname eq coinname), setValue(userBalance::Balance, Balance))
            col.updateOne(tx::hash eq hash, setValue(tx::isConfirmed, status))
        }

    }
}
