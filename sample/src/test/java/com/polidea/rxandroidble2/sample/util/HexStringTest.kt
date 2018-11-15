package com.polidea.rxandroidble2.sample.util

import org.assertj.core.api.Assertions.*
import org.junit.*

class HexStringTest {

    @Test
    fun `bytes to hex`() {
        val hex = byteArrayOf(0, 0, 0).bytesToHex()
        assertThat(hex).isEqualTo("000000")

        val hex1 = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()).bytesToHex()
        assertThat(hex1).isEqualTo("FFFFFFFF")

        val hex2 = byteArrayOf(0xF0.toByte(), 0x01.toByte(), 0x10.toByte(), 0x0F.toByte()).bytesToHex()
        assertThat(hex2).isEqualTo("F001100F")

        val hex3 = byteArrayOf(0xAC.toByte(), 0x83.toByte(), 0x1D.toByte(), 0xB5.toByte()).bytesToHex()
        assertThat(hex3).isEqualTo("AC831DB5")

        val hex4 = byteArrayOf(0x01.toByte(), 0x00.toByte(), 0xB4.toByte(), 0x10.toByte()).bytesToHex()
        assertThat(hex4).isEqualTo("0100B410")
    }

    @Test
    fun `hex to bytes`() {
        val bytes = "000000".hexToBytes()
        assertThat(bytes).isEqualTo(byteArrayOf(0, 0, 0))

        val bytes1 = "FFFFFFFF".hexToBytes()
        assertThat(bytes1).isEqualTo(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()))

        val bytes2 = "F001100F".hexToBytes()
        assertThat(bytes2).isEqualTo(byteArrayOf(0xF0.toByte(), 0x01.toByte(), 0x10.toByte(), 0x0F.toByte()))

        val bytes3 = "AC831DB5".hexToBytes()
        assertThat(bytes3).isEqualTo(byteArrayOf(0xAC.toByte(), 0x83.toByte(), 0x1D.toByte(), 0xB5.toByte()))

        val bytes4 = "0100B410".hexToBytes()
        assertThat(bytes4).isEqualTo(byteArrayOf(0x01.toByte(), 0x00.toByte(), 0xB4.toByte(), 0x10.toByte()))

        assertThatIllegalArgumentException().isThrownBy { "8".hexToBytes() }
    }
}