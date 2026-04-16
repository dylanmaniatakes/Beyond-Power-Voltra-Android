package com.technogizguy.voltra.controller.protocol

class VoltraFrameAssembler {
    private var pending = ByteArray(0)

    fun accept(fragment: ByteArray): List<ByteArray> {
        if (fragment.isEmpty()) return emptyList()

        var buffer = pending + fragment
        pending = ByteArray(0)
        val frames = mutableListOf<ByteArray>()

        while (buffer.isNotEmpty()) {
            if ((buffer[0].toInt() and 0xFF) != VOLTRA_MAGIC) {
                frames += buffer
                return frames
            }
            if (buffer.size < HEADER_BYTES_NEEDED_FOR_LENGTH) {
                pending = buffer
                return frames
            }

            val expectedLength = VoltraPacketParser.expectedFrameLength(buffer) ?: run {
                frames += buffer
                return frames
            }
            if (expectedLength < MIN_FRAME_LENGTH) {
                frames += buffer
                return frames
            }
            if (buffer.size < expectedLength) {
                pending = buffer
                return frames
            }

            frames += buffer.copyOfRange(0, expectedLength)
            buffer = buffer.copyOfRange(expectedLength, buffer.size)
        }

        return frames
    }

    fun clear() {
        pending = ByteArray(0)
    }

    private companion object {
        private const val VOLTRA_MAGIC = 0x55
        private const val HEADER_BYTES_NEEDED_FOR_LENGTH = 3
        private const val MIN_FRAME_LENGTH = 13
    }
}
