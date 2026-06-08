package com.technogizguy.voltra.controller.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VoltraFrameAssemblerTest {
    @Test
    fun joinsFragmentedNotificationsIntoOneFrame() {
        val frame = "552A083F1001000020004F0044796C616E20566F6C7472612031000000000000001020BA9705DA027346"
            .hexToByteArray()
        val assembler = VoltraFrameAssembler()

        assertTrue(assembler.accept(frame.copyOfRange(0, 20)).isEmpty())
        assertTrue(assembler.accept(frame.copyOfRange(20, 40)).isEmpty())
        assertEquals(
            listOf(frame.toHexString()),
            assembler.accept(frame.copyOfRange(40, frame.size)).map { it.toHexString() },
        )
    }

    @Test
    fun emitsMultipleCompleteFramesFromOneChunk() {
        val first = "550E08C510AA00002000270001DB".hexToByteArray()
        val second = "5510045610AA1900200027820100505E".hexToByteArray()
        val assembler = VoltraFrameAssembler()

        assertEquals(
            listOf(first.toHexString(), second.toHexString()),
            assembler.accept(first + second).map { it.toHexString() },
        )
    }

    @Test
    fun waitsForExtendedType09FrameTail() {
        val firstChunk = "5577099F10AA0200200077000145500000000000000000000000000000425000000000000000000000000000004D61696E436F6E74726F6C76312E360009000801000000010702000145500000000000000000000000000000425000000000000000000000000000004D6F746F72436F6E74726F6C312E"
            .hexToByteArray()
        val tail = "36004007000107000001070200014550000000000000000000000000000042500000000000000000000000000000424D53312E3500000000000000000000300100011100000107020001455000000000000000000000000000004250000000000000000000000000000045535033322D43334D494E492D3100000002040200000000000303040000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004550000000000000000000000000000042500000000000000000000000000000504D55312E3000000000000000000000600000010700000106020001B9AD"
            .hexToByteArray()
        val assembler = VoltraFrameAssembler()

        assertTrue(assembler.accept(firstChunk).isEmpty())
        val frames = assembler.accept(tail)

        assertEquals(1, frames.size)
        assertEquals(0x177, frames.single().size)
        assertTrue(frames.single().asciiPreview().orEmpty().contains("BMS1.5"))
    }
}
