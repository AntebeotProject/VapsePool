package ru.xmagi.pool.main.PoolServer

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*


const val defSalt = "123456789ABCDEF-"
const val defDBType = "mongodb"
const val defDBName = "VAPSEPOOL"
object Settings {
    public val m_propetries = Properties()
    public fun load_propetries(fPath: String = "propetries.config") {
        val configFile = File(fPath)
        if (configFile.exists()) {

            m_propetries.load(FileInputStream(fPath))
        } else {

            m_propetries.setProperty("rpcuser", PoolServer.defUser)
            m_propetries.setProperty("rpcpass", PoolServer.defPass)
            m_propetries.setProperty("rpchost", PoolServer.defHost)
            m_propetries.setProperty("idleSecond", PoolServer.defMaxIdleSecond.toString())
            m_propetries.setProperty("IdleTries", PoolServer.defMaxIdleTries.toString())
            m_propetries.setProperty("DBType", defDBType)
            m_propetries.setProperty("DatabaseName", defDBName) // magic name. for now is ok i think.
            m_propetries.setProperty("SecretSalt", defSalt)
            m_propetries.store(FileOutputStream(fPath), null)
        }
    }
}
