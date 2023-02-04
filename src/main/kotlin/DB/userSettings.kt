package org.antibiotic.pool.main.DB

import com.google.common.io.BaseEncoding
import com.mongodb.client.model.Filters
import dev.turingcomplete.kotlinonetimepassword.GoogleAuthenticator
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.RandomSecretGenerator
import kotlinx.serialization.Serializable
import org.antibiotic.pool.main.WebSite.JettyServer
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.setValue

// users settings (TODO^^^ to another files. maybe class better instead singleton
data class userEmails(val owner: String, val email: String)
{
    init {
        DB.createCollection("userEmails")
    }
    companion object {
        private val col = DB.mongoDB.getCollection<userEmails>("userEmails")
        fun emailExistsForUser(u: String): Boolean
        {
            if (getForUser(u) == null) return false
            return true
        }
        fun emailRegistered(e: String): Boolean {
            if (getForUser(e) == null) return false
            return true
        }
        fun getForUser(w: String): userEmails? {
            val s = col.find(Filters.or(userEmails::email eq w, userEmails::owner eq w))
            if (s.iterator().hasNext()) return s.iterator().next()
            return null

        }
        fun set(o: String, e: String)
        {
            if (emailExistsForUser(o))
            {
                col.updateOne(userEmails::owner eq o, setValue(userEmails::email, e))
            } else {
                col.insertOne(userEmails(o, e))
            }
        }
    }
}
const val defUserLanguage = "ru_RU"
val allowedLangauges = listOf("ru_RU", "en_US")
class notAllowedLanguage(w: String): Exception("$w - ${allowedLangauges.joinToString { "," }}")
@Serializable
data class userLanguage(val owner: String, val language: String = defUserLanguage)
{
    companion object
    {
        init {
            DB.createCollection("userLanguage")
        }
        private val col = DB.mongoDB.getCollection<userLanguage>("userLanguage")
        fun userLanguageExistsForUser(u: String): Boolean
        {
            if (getForUser(u) == null) return false
            return true
        }
        fun set(o: String, l: String)
        {
            if (!allowedLangauges.contains(l)) {
                val curLangauge = JettyServer.Users.language.getLangByUser(o)
                throw notAllowedLanguage( String.format(curLangauge.getString("NotAllowedLang"), l) )
            }
            if (userLanguageExistsForUser(o))
            {
                col.updateOne(userLanguage::owner eq o, setValue(userLanguage::language, l))
            } else {
                col.insertOne(userLanguage(o, l))
            }
        }

        fun getForUser(u: String): userLanguage?
        {
            val s = col.find(userLanguage::owner eq u)
            if (s.iterator().hasNext()) {
               return s.iterator().next()
            }
           return null
        }
    }
}

// interface have to be more good idea for set, exists, etc

fun ByteArray.toB32(): String {
    return BaseEncoding.base32().encode(this)
}

@Serializable
data class userPrivilegies(val owner: String, val level: Int) {
    enum class privilegies(n: Int) {
        NOT_PRIVILEGED(0),
        EDIT_ORDERS(1.shl(1)),
        DELETE_ORDERS(1.shl(2)),
        SHOW_ORDERS(1.shl(3)),
        GET_USER_BALANCE(1.shl(4)),
        EDIT_USER_PRIVILEGIES(1.shl(5)),
        SHOW_REVIEWS(1.shl(6));
        open fun isAllowed(l: Int, privilegie: privilegies) = l.and(privilegie.ordinal) == privilegie.ordinal
    }

    companion object {
        init {
            DB.createCollection("userPrivilegies")
        }
        private val col = DB.mongoDB.getCollection<userPrivilegies>("userPrivilegies")
        fun createUserPrivilegeis(o: String)
        {
            col.insertOne(userPrivilegies(o, privilegies.NOT_PRIVILEGED.ordinal))
        }
        fun changeUserPrivilegies(o: String, lvl: Int)
        {
            if (getUserPrivilegies(o) == null) createUserPrivilegeis(o);
            col.updateOne(userPrivilegies::owner eq o, setValue(userPrivilegies::level, lvl))
        }
        fun getUserPrivilegies(o: String): userPrivilegies? {
            val w = col.findOne { userPrivilegies::owner eq o }
            return w;
        }
        fun addPrivilegie(o: String, pr: privilegies)
        {
            val tmp = getUserPrivilegies(o)
            if (tmp == null) {
                createUserPrivilegeis(o)
                return addPrivilegie(o, pr)
            }
            val nLvl = pr.ordinal.or(tmp.level)
            changeUserPrivilegies(o, nLvl)
        }
        fun delPrivilegie(o: String, pr: privilegies)
        {
            val tmp = getUserPrivilegies(o)
            if (tmp == null) {
                createUserPrivilegeis(o)
                return addPrivilegie(o, pr)
            }
            val nLvl = pr.ordinal.xor(tmp.level)
            changeUserPrivilegies(o, nLvl)
        }
        fun checkUserPrivilegies(o: String, pr: privilegies): Boolean{
            val p = getUserPrivilegies(o) ?: userPrivilegies(o, privilegies.NOT_PRIVILEGED.ordinal)
            return pr.isAllowed(p.level, pr)
        }
    }
}

class notcorrectb32(u:String): Exception( JettyServer.Users.language.getLangByUser(u).getString("notCorrectB32")  )
class notcorrectoldb32(u:String): Exception( JettyServer.Users.language.getLangByUser(u).getString("notCorrectOLDB32")  )

@Serializable
data class userOTP(val owner: String, val b32secret: String)
{
    companion object
    {
        init {
            DB.createCollection("userOTP")
        }
        private val col = DB.mongoDB.getCollection<userOTP>("userOTP")
        fun userOTPExistsForUser(u: String): Boolean
        {
            if (getForUser(u) == null) return false
            return true
        }
        fun getCode(b32: String) = GoogleAuthenticator(b32).generate()
        fun getCode(b32: ByteArray) = GoogleAuthenticator(b32).generate()
        fun generateCode() = RandomSecretGenerator().createRandomSecret(HmacAlgorithm.SHA1)
        fun isCorrectOTP(code: String): Boolean {
            if (!BaseEncoding.base32().canDecode(code))
            {
                return false
            }
            return true

        }
        fun set(o: String, l: String, code: String?)
        {
            if (!isCorrectOTP(l))
            {
                throw notcorrectb32(o)
            }

            if (userOTPExistsForUser(o))
            {
                if (code?.equals(getCodeForUser(o)) != true)
                {
                    throw notcorrectoldb32(o)
                }
                col.updateOne(userOTP::owner eq o, setValue(userOTP::b32secret, l))
            } else {
                col.insertOne(userOTP(o, l))
            }
        }
        fun dropFor(u:String)
        {
            col.deleteOne(userOTP::owner eq u)
        }
        fun getCodeForUser(u: String) = getCode(getForUser(u)!!.b32secret)

        fun getForUser(u: String): userOTP?
        {
            val s = col.find(userOTP::owner eq u)
            if (s.iterator().hasNext()) {
                return s.iterator().next()
            }
            return null
        }
    }
}