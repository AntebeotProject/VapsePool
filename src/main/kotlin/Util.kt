package org.antibiotic.pool.main


object  Util {
    /*
        @author I DONT AUTHOR OF THIS. I GOT IT FROM POOLSERVER. YET I DONT UNDERSTAND WHAT AUTHOR WANT
        FOR ASCII TO BIN WE WANT SOME SIMILAR
        val ret =buildString  {
            str.forEach {
                val hex = it.code.toString(16)
                append(String.format("%2s", hex).replace(" ", "0"))
            }
        }
        BUT I THINK THAT IS NOT NEED CODE. IS WILL BE FULL DELETED. I THINK THAT JSON-RPC validateaddress MORE HELPFUL THAN IT
        i did rewrite it. but not sure that i full understand that author want
     */

    fun ASCIIToHex(chr: Char): String
    {
        return chr.code.toString(16)
    }
    fun ASCIIToBin( str: String ): CharArray
    {
        val ret = buildString  {
            str.forEach {
                val bin = it.code.toString(2).padStart(2, '0')
                append(bin)
            }
        }
        return ret.toCharArray();
    }

    fun BinToASCII( data: ByteArray ): String
    {
        var str = ""
        for(i in 0 until data.size) {
            str += "0123456789abcdef"[data[i]/16];
            str += "0123456789abcdef"[data[i]%16];
        }
        return str;
    }

    //  fun Reverse(data: CharArray): CharArray = data.reversed().toCharArray()
    // is not C part code. but i tested by php documentation
    // is very need https://en.bitcoin.it/wiki/Block_hashing_algorithm
    /*
    fun hex2bin(d: String): String {
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
    fun bin2hex(hex: String) : String {
        hex.let {
            var ret = ""
            it.forEach {
                ret = ret + it.code.toString(16).padStart(2, '0')
            }
            return ret
        }
    }
    */
    fun reverseHex(originalHex: String): String {
        // TODO: Validation that the length is even
        val lengthInBytes = originalHex.length / 2
        val chars = CharArray(lengthInBytes * 2)
        for (index in 0 until lengthInBytes) {
            val reversedIndex = lengthInBytes - 1 - index
            chars[reversedIndex * 2] = originalHex[index * 2]
            chars[reversedIndex * 2 + 1] = originalHex[index * 2 + 1]
        }
        return String(chars)
    }

    fun Hex2Difficulty(n: String): Double {
        //val l = n.toLong(16)
        //println(l)
        //val bits = java.lang.Double.longBitsToDouble(l)
        //println(bits)
        //return bits
        return 0.0
    }
}