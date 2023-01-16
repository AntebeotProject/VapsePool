package ru.xmagi.pool.main.PoolServer

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import java.math.BigDecimal

object RPCClient {
    // there is main RPC Client for POOL
    public val m_cl by lazy() {
        RPC(

            Settings.m_propetries.getOrDefault("pool_rpchost", PoolServer.defHost)
                .toString(), // ?: , // is throw exception is there is null.
            Settings.m_propetries.getOrDefault("pool_rpcuser", PoolServer.defUser).toString(), // ?: ,
            Settings.m_propetries.getOrDefault("pool_rpcpass", PoolServer.defPass).toString() // ?:
        )
    }  // m_cl is RPC JSon part. So rename it
    // WILL BE CALLed WITH SYNCHRONIZED DATABASE AND ANOTHER STUFF
    public fun sendMoney(outAddr: String, cMoney: BigDecimal, optionalString: String = "From pool" ) {
        synchronized(RPCClient.m_cl) {
            RPCClient.m_cl.doCall(
                "sendtoaddress",
                buildJsonArray { add(outAddr); add(cMoney); add(optionalString) })
        }
    }
}