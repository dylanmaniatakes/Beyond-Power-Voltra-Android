package com.technogizguy.voltra.controller.protocol

/**
 * Constructs valid VOLTRA 0x55-framed packets with correct CRC8 and CRC16.
 *
 * CRC8 — covers bytes [magic, len, type] (first 3 bytes):
 *   poly=0x31, init=0xEE, reflect_in=true, reflect_out=true, xor_out=0x00
 *
 * CRC16 — covers the entire frame body excluding the trailing 2 CRC bytes:
 *   poly=0x1021, init=0x496C, reflect_in=true, reflect_out=true, xor_out=0x0000
 *   stored little-endian [lo, hi] at the end of the frame.
 *
 * Cracked against 17 known app-write frames captured from the official iOS app
 * via PacketLogger / sysdiagnose (sysdiagnose_2026.04.14_20-44-34-0400).
 * All 17 frames verify to [OK] with these parameters.
 *
 * Frame layout:
 *   [55][len][type][crc8][sender][receiver][seq_lo][seq_hi][proto_lo][proto_hi][cmd][payload...][crc16_lo][crc16_hi]
 */
object VoltraFrameBuilder {

    private const val MAGIC: Byte = 0x55.toByte()
    const val FRAME_TYPE_APP_WRITE: Int = 0x04
    const val FRAME_TYPE_EXTENDED_APP_WRITE: Int = 0x05
    private const val FIXED_HEADER_BYTES: Int = 11   // magic+len+type+crc8+sender+recv+seq_lo+seq_hi+proto_lo+proto_hi+cmd
    private const val CRC16_BYTES: Int = 2
    /** Sender ID used by the app in all confirmed write frames. */
    const val APP_SENDER: Int = 0xAA

    /** Receiver ID for the main VOLTRA device. */
    const val DEVICE_RECEIVER: Int = 0x10

    /** Protocol/version field observed in all captured frames (0x0020 stored LE). */
    const val PROTO: Int = 0x0020

    /**
     * Build a complete VOLTRA frame ready to write to VOLTRA_TRANSPORT.
     *
     * @param sender    Sender byte (default [APP_SENDER]).
     * @param receiver  Receiver byte (default [DEVICE_RECEIVER]).
     * @param seq       16-bit sequence number, stored little-endian.
     * @param proto     16-bit protocol field (default [PROTO]).
     * @param cmd       Command byte.
     * @param payload   Payload bytes after the command byte (may be empty).
     * @return Complete frame bytes including magic, length, CRC8 header, and CRC16 trailer.
     */
    fun build(
        cmd: Int,
        payload: ByteArray,
        seq: Int,
        sender: Int = APP_SENDER,
        receiver: Int = DEVICE_RECEIVER,
        proto: Int = PROTO,
        frameType: Int = FRAME_TYPE_APP_WRITE,
    ): ByteArray {
        require(frameType in 0x00..0xFF) { "VOLTRA frame type must fit in one byte: $frameType" }
        val totalLength = FIXED_HEADER_BYTES + payload.size + CRC16_BYTES
        require(totalLength <= 0xFFFF) { "VOLTRA frame payload too large: length=$totalLength > 65535" }
        // Official startup-image media chunks exceed 255 bytes on transport.
        // The device still expects the frame length byte to carry the low byte of
        // the real total length, paired with frame type 0x05 for those large writes.
        val encodedLength = totalLength and 0xFF

        val headerForCrc8 = byteArrayOf(MAGIC, encodedLength.toByte(), frameType.toByte())
        val crc8Val = crc8(headerForCrc8)

        val body = ByteArray(totalLength - CRC16_BYTES).also { buf ->
            buf[0] = MAGIC
            buf[1] = encodedLength.toByte()
            buf[2] = frameType.toByte()
            buf[3] = crc8Val.toByte()
            buf[4] = sender.toByte()
            buf[5] = receiver.toByte()
            buf[6] = (seq and 0xFF).toByte()
            buf[7] = ((seq shr 8) and 0xFF).toByte()
            buf[8] = (proto and 0xFF).toByte()
            buf[9] = ((proto shr 8) and 0xFF).toByte()
            buf[10] = cmd.toByte()
            payload.copyInto(buf, destinationOffset = 11)
        }

        val crc16Val = crc16(body)
        return body + byteArrayOf((crc16Val and 0xFF).toByte(), ((crc16Val shr 8) and 0xFF).toByte())
    }

    // ── CRC8: poly=0x31, init=0xEE, reflect_in=true, reflect_out=true, xor_out=0x00 ──

    private fun crc8(data: ByteArray): Int {
        var crc = 0xEE
        for (byte in data) {
            crc = crc xor reflect8(byte.toInt() and 0xFF)
            repeat(8) {
                crc = if (crc and 0x80 != 0) ((crc shl 1) xor 0x31) and 0xFF else (crc shl 1) and 0xFF
            }
        }
        return reflect8(crc)
    }

    // ── CRC16: poly=0x1021, init=0x496C, reflect_in=true, reflect_out=true, xor_out=0x0000 ──

    private fun crc16(data: ByteArray): Int {
        var crc = 0x496C
        for (byte in data) {
            crc = crc xor (reflect8(byte.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) ((crc shl 1) xor 0x1021) and 0xFFFF else (crc shl 1) and 0xFFFF
            }
        }
        return reflect16(crc)
    }

    private fun reflect8(value: Int): Int {
        var v = value and 0xFF
        var r = 0
        repeat(8) {
            r = (r shl 1) or (v and 1)
            v = v shr 1
        }
        return r
    }

    private fun reflect16(value: Int): Int {
        var v = value and 0xFFFF
        var r = 0
        repeat(16) {
            r = (r shl 1) or (v and 1)
            v = v shr 1
        }
        return r
    }
}
