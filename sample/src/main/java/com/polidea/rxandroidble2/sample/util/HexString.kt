package com.polidea.rxandroidble2.sample.util

object HexString {

    private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)

        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = HEX_ARRAY[v ushr 4]
            hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
        }

        return String(hexChars)
    }

    fun hexToBytes(hexRepresentation: String): ByteArray {
        if (hexRepresentation.length % 2 == 1) {
            throw IllegalArgumentException("hexToBytes requires an even-length String parameter")
        }

        val len = hexRepresentation.length
        val data = ByteArray(len / 2)

        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hexRepresentation[i], 16) shl 4) + Character.digit(
                hexRepresentation[i + 1],
                16
            )).toByte()
            i += 2
        }

        return data
    }
}
