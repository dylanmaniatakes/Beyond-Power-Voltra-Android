package com.technogizguy.voltra.controller.protocol

import com.technogizguy.voltra.controller.model.VoltraParamValueType
import kotlin.test.Test
import kotlin.test.assertEquals

class VoltraValueCodecTest {
    @Test
    fun decodesLittleEndianUnsignedValues() {
        assertEquals(513, VoltraValueCodec.decodeLittleEndian(VoltraParamValueType.UINT16, byteArrayOf(0x01, 0x02)))
        assertEquals(65_536L, VoltraValueCodec.decodeLittleEndian(VoltraParamValueType.UINT32, byteArrayOf(0x00, 0x00, 0x01, 0x00)))
    }

    @Test
    fun encodesLittleEndianSignedValues() {
        assertEquals("FEFF", VoltraValueCodec.encodeLittleEndian(VoltraParamValueType.INT16, -2).toHexString())
    }
}
