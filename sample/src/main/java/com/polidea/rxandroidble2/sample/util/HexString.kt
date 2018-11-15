package com.polidea.rxandroidble2.sample.util

private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()

internal fun ByteArray.bytesToHex(): String =
    CharArray(size * 2).let { hexChars ->
        for (j in indices) {
            val v = this[j].toInt() and 0xFF
            hexChars[j * 2] = HEX_ARRAY[v ushr 4]
            hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
        }
        String(hexChars)
    }

internal fun String.hexToBytes(): ByteArray {
    if (length % 2 == 1) throw IllegalArgumentException("hexToBytes requires an even-length String parameter")

    return ByteArray(length / 2).also { data ->
        for (i in 0 until length step 2) {
            data[i / 2] = ((this[i].hexValue() shl 4) + this[i + 1].hexValue()).toByte()
        }
    }
}

private fun Char.hexValue(): Int = Character.digit(this, 16)
