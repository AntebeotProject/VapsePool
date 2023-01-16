package ru.xmagi.pool.main
import ru.xmagi.pool.main.PoolServer.toHexString
import java.nio.*
// @author Roland
class BlockWorker {
    class NotCorrectBlockData : Exception("Not correct block data")
    data class Block(val version: String, val prevHash: String, val transMerkle: String, val time: String, val difficulty: String, val nonce: String)
    {
        fun modifyDifficulty(dif: String) = if (dif.length != 8) throw NotCorrectBlockData() else Block(this.version, this.prevHash, this.transMerkle, this.time, dif, this.nonce)
        fun modifyNonce(nNonce: String) = if (nNonce.length != 8) throw NotCorrectBlockData() else Block(this.version, this.prevHash, this.transMerkle, this.time, this.difficulty, nNonce)
        fun toGetWork(): String {
            val _data = version + prevHash + transMerkle + time + difficulty + nonce

            val version = _data.substring(0, 8) // version data
            val fPrev = _data.substring(8, 8+64)
            //println("000000eda37d040d4432f982098f40a962643a026e38b41d6568be3250a612a8")
            // println(fPrev)
            val prevHashNums = mutableListOf<String>()
            // TODO: to NReadBytes
            for (i in 8 until 8+64 step 8) {
                //println("Byte: ${_data.substring(i, i + 8)}")
                val inLong = _data.substring(i, i + 8).toLong(16)
                //@author Vyacheslav
                val inByteorder = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(_data.substring(i, i + 8).toLong(16))
                val inHex = inByteorder.array().toHexString()
                prevHashNums.add( inHex.substring(8, inHex.length) )

            }
            val CorrectPrevHash =  prevHashNums.reversed().joinToString("")
            val merkleHash =  mutableListOf<String>()
            // todo to another func
            for (i in 8+64 until (8+64+64) step 8) {
                //println("Byte: ${_data.substring(i, i + 8)}")
                val inLong = _data.substring(i, i + 8).toLong(16)
                //@author Vyacheslav
                val inByteorder = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(_data.substring(i, i + 8).toLong(16))
                val inHex = inByteorder.array().toHexString()
                merkleHash.add( inHex.substring(8, inHex.length) )

            }
            val CorrectMerkle = merkleHash.reversed().joinToString("")
            val time = _data.substring((8+64+64), (8+64+64) + 8)
            val difficulty = _data.substring((8+64+64+8), (8+64+64+8) + 8)
            val nonce = _data.substring((8+64+64+8+8), _data.length)
            return version + CorrectPrevHash + CorrectMerkle + time + difficulty + nonce
        }

    }
    companion object {
        fun hex2Bin(d: String): String {
            var ret = mutableListOf<Byte>()
            for(i in 0 until d.length step 2) {
                val byte = buildString {
                    append(d[i])
                    append(d[i + 1])
                }
                ret.add(byte.toInt(16).toByte())
            }
            val bArray = ret.toByteArray()
            return String(bArray)
        }
        fun bin2Hex(hex: String) : String {
            hex.let {
                var ret = ""
                it.forEach {
                    ret = ret + it.code.toString(16).padStart(2, '0')
                }
                return ret
            }
        }
        //object getwork {
        fun toBlock(_data: String): Block {
            // be specific, data = 20 x 4 byte numbers:
            if (_data.length != 256) throw NotCorrectBlockData()
            // for ( step by 8)
            val version = _data.substring(0, 8) // version data
            val fPrev = _data.substring(8, 8+64)
            //println("000000eda37d040d4432f982098f40a962643a026e38b41d6568be3250a612a8")
            // println(fPrev)
            val prevHashNums = mutableListOf<String>()

            for (i in 8 until 8+64 step 8) {
                //println("Byte: ${_data.substring(i, i + 8)}")
                val inLong = _data.substring(i, i + 8).toLong(16)
                //@author Vyacheslav
                val inByteorder = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(_data.substring(i, i + 8).toLong(16))
                val inHex = inByteorder.array().toHexString()
                prevHashNums.add( inHex.substring(8, inHex.length) )

            }
            val CorrectPrevHash =  prevHashNums.reversed().joinToString("")
            val merkleHash =  mutableListOf<String>()
            // todo to another func
            for (i in 8+64 until (8+64+64) step 8) {
                //println("Byte: ${_data.substring(i, i + 8)}")
                val inLong = _data.substring(i, i + 8).toLong(16)
                //@author Vyacheslav
                val inByteorder = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(_data.substring(i, i + 8).toLong(16))
                val inHex = inByteorder.array().toHexString()
                merkleHash.add( inHex.substring(8, inHex.length) )

            }
            val CorrectMerkle = merkleHash.reversed().joinToString("")
            val time = _data.substring((8+64+64), (8+64+64) + 8)
            val difficulty = _data.substring((8+64+64+8), (8+64+64+8) + 8)
            val nonce = _data.substring((8+64+64+8+8), _data.length)
            return Block(version, CorrectPrevHash, CorrectMerkle, time, difficulty, nonce  )
        }
        //}
    }//
}