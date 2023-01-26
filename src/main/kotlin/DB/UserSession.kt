package org.antibiotic.pool.main.DB

import org.antibiotic.pool.main.PoolServer.toHexString
import org.bson.Document
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.setValue
import java.util.*

data class UserSession(val id: String, val owner: String, val createTimeStamp: Long = System.currentTimeMillis())
{
    companion object {
        fun addSession(l: String, p: String): String? {
            if(users.checkUserPassword(l,p) != true) return null
            DB.createCollection("sessions")
            val col = DB.mongoDB.getCollection<UserSession>("sessions")
            // Session is Login hashed Password Hashed + Time hashed to b64 string is good i think
            // is Just hex string instead create String from byte data like String(....toByteArray())
            val Session = Base64.getEncoder().encode(hashString("$l:$p:${System.currentTimeMillis()}").toByteArray()).toHexString()
            col.insertOne(UserSession(Session, l))
            return Session
        }
        fun updateSession(id: String) {
            DB.createCollection("sessions")
            val col = DB.mongoDB.getCollection<UserSession>("sessions")
            col.updateOne(UserSession::id eq id, setValue(UserSession::createTimeStamp, System.currentTimeMillis()))
        }
        fun getOwnerSessions(owner: String): List<UserSession> {
            DB.createCollection("sessions")
            val col = DB.mongoDB.getCollection<Document>("sessions")
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
            DB.createCollection("sessions")
            val col = DB.mongoDB.getCollection<UserSession>("sessions")
            col.deleteOne(UserSession::id eq s)
        }
        fun getSession(s: String): UserSession? {
            DB.createCollection("sessions")
            val col = DB.mongoDB.getCollection<Document>("sessions")
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
}