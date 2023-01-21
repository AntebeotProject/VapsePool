package org.antibiotic.pool.main.PoolServer

import org.antibiotic.pool.main.PoolServer.PoolServer.maxIdleSecond
import kotlin.concurrent.thread

data class MinerData(var LastActiveTimeSec: Long, val Login: String, var IdleTries: Int = 0, val isHTTPMiner: Boolean = true) {
    companion object {
        val currentMiners = mutableListOf<MinerData>()
        fun getMinerIDX(Login: String): Int {
            for (minerIDX in 0 until currentMiners.size) {
                if (currentMiners[minerIDX].Login.equals(Login)) return minerIDX
            }
            return -1
        }

        /*
          *
          * is authomatic add miner too! for now
         */
        fun getMiner(Login: String, isHTTPMiner: Boolean = true): MinerData {
            val minerIDX = getMinerIDX(Login)
            if (minerIDX == -1) {
                currentMiners.add(MinerData(LastActiveTimeSec = System.currentTimeMillis() / 1000, Login = Login, isHTTPMiner = isHTTPMiner))
                return currentMiners.last()
            }
            return currentMiners.get(minerIDX)
        }

        fun deleteMiner(Miner: MinerData) {
            // println("remove miner")
            // currentMiners.removeIf() {Miner.Login == it.Login}
            val it = currentMiners.iterator()
            while(it.hasNext()) {
                val item = it.next()
                if (item.Login == Miner.Login) it.remove()
            }
        }

        fun cleanNotWorksMiners() {
            synchronized(currentMiners) {
                currentMiners.removeIf() { it.idleMoreThan(maxIdleSecond) }
            }
        }

        fun startThreadForClean() {
                thread {
                        // println("thread for clean not works miners")
                        if (maxIdleSecond <= 0) maxIdleSecond = PoolServer.defMaxIdleSecond
                        Thread.sleep(maxIdleSecond.toLong() * 1000)
                        cleanNotWorksMiners()
                        startThreadForClean()
                    }
        }
    }

    fun updateLastActive() {
        LastActiveTimeSec = System.currentTimeMillis() / 1000
        IdleTries = 0
    }

    fun idleMoreThan(seconds: Int): Boolean {
        return (System.currentTimeMillis() / 1000 - LastActiveTimeSec > seconds)
    }
}
