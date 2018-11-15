package com.polidea.rxandroidble2.sample.util

import org.assertj.core.api.Assertions.*
import org.junit.*

class HexStringTest {

    @Test
    fun `bytes to hex`() {
        val hex = HexString.bytesToHex(byteArrayOf(0, 0, 0))
        assertThat(hex).isEqualTo("000000")

        val hex1 = HexString.bytesToHex(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()))
        assertThat(hex1).isEqualTo("FFFFFFFF")

        val hex2 = HexString.bytesToHex(byteArrayOf(0xF0.toByte(), 0x01.toByte(), 0x10.toByte(), 0x0F.toByte()))
        assertThat(hex2).isEqualTo("F001100F")

        val hex3 = HexString.bytesToHex(byteArrayOf(0xAC.toByte(), 0x83.toByte(), 0x1D.toByte(), 0xB5.toByte()))
        assertThat(hex3).isEqualTo("AC831DB5")

        val hex4 = HexString.bytesToHex(byteArrayOf(0x01.toByte(), 0x00.toByte(), 0xB4.toByte(), 0x10.toByte()))
        assertThat(hex4).isEqualTo("0100B410")
    }

    @Test
    fun `hex to bytes`() {
        val bytes = HexString.hexToBytes("000000")
        assertThat(bytes).isEqualTo(byteArrayOf(0, 0, 0))

        val bytes1 = HexString.hexToBytes("FFFFFFFF")
        assertThat(bytes1).isEqualTo(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()))

        val bytes2 = HexString.hexToBytes("F001100F")
        assertThat(bytes2).isEqualTo(byteArrayOf(0xF0.toByte(), 0x01.toByte(), 0x10.toByte(), 0x0F.toByte()))

        val bytes3 = HexString.hexToBytes("AC831DB5")
        assertThat(bytes3).isEqualTo(byteArrayOf(0xAC.toByte(), 0x83.toByte(), 0x1D.toByte(), 0xB5.toByte()))

        val bytes4 = HexString.hexToBytes("0100B410")
        assertThat(bytes4).isEqualTo(byteArrayOf(0x01.toByte(), 0x00.toByte(), 0xB4.toByte(), 0x10.toByte()))

        assertThatIllegalArgumentException().isThrownBy { HexString.hexToBytes("0") }
    }
}