package org.antibiotic.pool.main.PoolServer

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