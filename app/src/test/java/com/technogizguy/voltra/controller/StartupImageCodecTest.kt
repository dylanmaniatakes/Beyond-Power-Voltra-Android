package com.technogizguy.voltra.controller

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupImageCodecTest {

    @Test
    fun addsIpadLikeMetadataAfterJfifSegment() {
        val baseJpeg = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(),
            0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10,
            0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, 0x01, 0x00, 0x00, 0x48, 0x00, 0x48, 0x00, 0x00,
            0xFF.toByte(), 0xDB.toByte(), 0x00, 0x43, 0x00,
        )

        val result = addIpadLikeStartupPhotoMetadata(baseJpeg, width = 720, height = 720)

        assertEquals(0xFF.toByte(), result[0])
        assertEquals(0xD8.toByte(), result[1])
        val app1Index = result.indexOfSubsequence(byteArrayOf(0xFF.toByte(), 0xE1.toByte()))
        val app13Index = result.indexOfSubsequence(byteArrayOf(0xFF.toByte(), 0xED.toByte()))
        val dqtIndex = result.indexOfSubsequence(byteArrayOf(0xFF.toByte(), 0xDB.toByte()))
        assertTrue(app1Index > 2)
        assertTrue(app13Index > app1Index)
        assertTrue(dqtIndex > app13Index)
        assertTrue(result.size > baseJpeg.size)
    }

    private fun ByteArray.indexOfSubsequence(target: ByteArray): Int {
        if (target.isEmpty() || target.size > size) return -1
        for (start in 0..(size - target.size)) {
            var matched = true
            for (offset in target.indices) {
                if (this[start + offset] != target[offset]) {
                    matched = false
                    break
                }
            }
            if (matched) return start
        }
        return -1
    }
}
