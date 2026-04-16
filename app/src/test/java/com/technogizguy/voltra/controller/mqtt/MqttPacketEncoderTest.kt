package com.technogizguy.voltra.controller.mqtt

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class MqttPacketEncoderTest {
    @Test
    fun connectPacketIncludesClientAndCredentials() {
        val packet = MqttPacketEncoder.connectPacket(
            MqttConnectionKey(
                host = "192.168.1.2",
                port = 1883,
                username = "dylan",
                password = "secret",
                clientId = "voltra-test",
            ),
        )

        assertEquals(0x10, packet.first().toInt() and 0xFF)
        assertEquals('M'.code, packet[4].toInt())
        assertEquals('Q'.code, packet[5].toInt())
        assertEquals('T'.code, packet[6].toInt())
        assertEquals('T'.code, packet[7].toInt())
        assertEquals("voltra-test", packet.decodeMqttString(at = 12))
    }

    @Test
    fun publishPacketSetsRetainFlagAndTopic() {
        val packet = MqttPacketEncoder.publishPacket(
            topic = "voltra_control/voltra/state",
            payload = "online".encodeToByteArray(),
            retained = true,
        )

        assertEquals(0x31, packet.first().toInt() and 0xFF)
        assertEquals("voltra_control/voltra/state", packet.decodeMqttString(at = 2))
        assertArrayEquals("online".encodeToByteArray(), packet.copyOfRange(packet.size - 6, packet.size))
    }

    @Test
    fun remainingLengthHandlesMultiByteValues() {
        assertArrayEquals(byteArrayOf(0x7F), MqttPacketEncoder.encodeRemainingLength(127))
        assertArrayEquals(byteArrayOf(0x80.toByte(), 0x01), MqttPacketEncoder.encodeRemainingLength(128))
        assertArrayEquals(byteArrayOf(0xFF.toByte(), 0x7F), MqttPacketEncoder.encodeRemainingLength(16_383))
    }
}

private fun ByteArray.decodeMqttString(at: Int): String {
    val length = ((this[at].toInt() and 0xFF) shl 8) or (this[at + 1].toInt() and 0xFF)
    return decodeToString(startIndex = at + 2, endIndex = at + 2 + length)
}
