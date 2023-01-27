package org.antibiotic.pool.main.DB

import com.mongodb.client.MongoCollection
import org.antibiotic.pool.main.PoolServer.Settings
import org.antibiotic.pool.main.PoolServer.toHexString
import org.bouncycastle.crypto.generators.BCrypt
import org.bson.Document
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.setValue
import org.litote.kmongo.updateOne
import java.util.*


// User Methods + Sessions methods
val verysecretsalt = Settings.m_propetries.getOrDefault("SecretSalt", "123456789ABCDEF-").toString() //"123456789ABCDEF-"
fun hashString(p: String) = BCrypt.generate(p.toByteArray(), verysecretsalt.toByteArray(), 4).toHexString()
class userRegistered: Exception()
data class users(val Login: String, val Password: String)
{
    companion object {
        // change to ur value. better use m_propetries
        fun checkUserPassword(l: String, p: String? = null): Boolean? {
            val col = DB.mongoDB.getCollection<Document>("users") as MongoCollection<Document>
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
            DB.createCollection("users")
            val col = DB.mongoDB.getCollection<users>("users") as MongoCollection<users>
            if (checkUserPassword(l) != null) {
                throw userRegistered()
            }
            val hashedPass = hashString(p)
            col.insertOne(users(l, Password = hashedPass))
        }
        // for administrations
        fun dropUserByLogin(l: String) {
            DB.createCollection("users")
            val col = DB.mongoDB.getCollection<users>("users") as MongoCollection<users>
            col.deleteOne(users::Login eq l)
        }
        fun changeUserPassword(l: String, np: String)
        {
            DB.createCollection("users")
            val col = DB.mongoDB.getCollection<users>("users") as MongoCollection<users>
            val hashedPass = hashString(np)
            col.updateOne(users::Login eq l, setValue(users::Password, hashedPass))
        }

        /*
            * Not was tested. will be deleted maybe.
         */
        fun modifyUser(l: String, user: users) {
            DB.createCollection("users")
            val col = DB.mongoDB.getCollection("users")
            col.updateOne(users::Login eq l, user) // https://stackoverflow.com/questions/47400942/what-does-mean-in-kotlin
        }
    }
}

