package com.technogizguy.voltra.controller.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VoltraPacketParserTest {
    @Test
    fun parsesCapturedHandshakeHello() {
        val packet = assertNotNull(
            VoltraPacketParser.parse(
                "552904c90110000020004f69506164000000000000000000000000000000000084ab1a5f292001ea4f".hexToByteArray(),
            ),
        )

        assertEquals(0x29, packet.declaredLength)
        assertEquals(0x04, packet.packetType)
        assertEquals(0x01, packet.senderId)
        assertEquals(0x10, packet.receiverId)
        assertEquals(0x4F, packet.commandId)
        assertTrue(packet.lengthMatches)
        assertTrue(packet.shortSummary().contains("cmd=0x4F"))
    }

    @Test
    fun parsesCapturedDeviceNameResponse() {
        val packet = assertNotNull(
            VoltraPacketParser.parse(
                "552a083f1001000020004f00566f6c74726120556e6974203031000000000000001020ba9705da027346".hexToByteArray(),
            ),
        )

        assertEquals(0x08, packet.packetType)
        assertEquals(0x10, packet.senderId)
        assertEquals(0x01, packet.receiverId)
        assertEquals(0x4F, packet.commandId)
        assertTrue(packet.payloadAscii.orEmpty().contains("Voltra Unit 01"))
    }

    @Test
    fun readOnlyBootstrapPacketsAreWellFormedCapturedFramesAndSafeReads() {
        assertEquals(10, VoltraOfficialReadOnlyBootstrap.packets.size)
        VoltraOfficialReadOnlyBootstrap.packets.forEach { bootstrapPacket ->
            val parsed = assertNotNull(VoltraPacketParser.parse(bootstrapPacket.bytes), bootstrapPacket.label)
            assertTrue(parsed.lengthMatches, bootstrapPacket.label)
        }
        assertEquals(
            "55130403AA10050020000F02002D4E5D1B8E20",
            VoltraOfficialReadOnlyBootstrap.packets[8].hex,
        )
        assertEquals("read mode feature state", VoltraOfficialReadOnlyBootstrap.packets.last().label)
        assertTrue(VoltraOfficialReadOnlyBootstrap.packets.last().hex.contains("B053"))
    }

    @Test
    fun labelsKnownCommandAndDisplaysWideSequence() {
        val packet = assertNotNull(
            VoltraPacketParser.parse(
                "553A047010AA08022000AA812B00010000000000000000000300000000000000000000000000218439000000000000000000000000003400CA73"
                    .hexToByteArray(),
            ),
        )

        assertEquals(0xAA, packet.commandId)
        assertEquals(0x0208, packet.sequence16)
        assertTrue(packet.shortSummary().contains("cmd=0xAA telemetry-state"))
        assertTrue(packet.shortSummary().contains("seq=520"))
        assertTrue(packet.shortSummary().contains("payload0=812B00010000000000000000"))
    }
}
