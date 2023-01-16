package ru.xmagi.pool.main.PoolServer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import java.math.BigDecimal
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.internal.*

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
}