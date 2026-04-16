package com.technogizguy.voltra.controller.protocol

import com.technogizguy.voltra.controller.model.VoltraParamValueType
import java.nio.ByteBuffer
import java.nio.ByteOrder

object VoltraValueCodec {
    fun decodeLittleEndian(type: VoltraParamValueType, bytes: ByteArray): Number {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return when (type) {
            VoltraParamValueType.UINT8 -> bytes.requireSize(1).first().toInt() and 0xFF
            VoltraParamValueType.INT8 -> bytes.requireSize(1).first().toInt()
            VoltraParamValueType.UINT16 -> buffer.requireRemaining(2).short.toInt() and 0xFFFF
            VoltraParamValueType.INT16 -> buffer.requireRemaining(2).short.toInt()
            VoltraParamValueType.UINT32 -> buffer.requireRemaining(4).int.toLong() and 0xFFFF_FFFFL
            VoltraParamValueType.INT32 -> buffer.requireRemaining(4).int
            VoltraParamValueType.UINT64 -> buffer.requireRemaining(8).long
            VoltraParamValueType.FLOAT -> buffer.requireRemaining(4).float
            VoltraParamValueType.UNKNOWN -> error("Cannot decode unknown VOLTRA parameter value type.")
        }
    }

    fun encodeLittleEndian(type: VoltraParamValueType, value: Number): ByteArray {
        return when (type) {
            VoltraParamValueType.UINT8,
            VoltraParamValueType.INT8 -> byteArrayOf(value.toInt().toByte())
            VoltraParamValueType.UINT16,
            VoltraParamValueType.INT16 -> ByteBuffer.allocate(2)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(value.toInt().toShort())
                .array()
            VoltraParamValueType.UINT32,
            VoltraParamValueType.INT32 -> ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(value.toInt())
                .array()
            VoltraParamValueType.UINT64 -> ByteBuffer.allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(value.toLong())
                .array()
            VoltraParamValueType.FLOAT -> ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putFloat(value.toFloat())
                .array()
            VoltraParamValueType.UNKNOWN -> error("Cannot encode unknown VOLTRA parameter value type.")
        }
    }

    private fun ByteArray.requireSize(size: Int): ByteArray {
        require(this.size >= size) { "Expected at least $size bytes, got ${this.size}." }
        return this
    }

    private fun ByteBuffer.requireRemaining(size: Int): ByteBuffer {
        require(remaining() >= size) { "Expected at least $size bytes, got ${remaining()}." }
        return this
    }
}
