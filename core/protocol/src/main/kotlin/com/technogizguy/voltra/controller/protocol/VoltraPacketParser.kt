package com.technogizguy.voltra.controller.protocol

data class ParsedVoltraPacket(
    val declaredLength: Int,
    val totalLength: Int,
    val packetType: Int,
    val headerChecksum: Int,
    val senderId: Int,
    val receiverId: Int,
    val sequence: Int,
    val channel: Int,
    val protocol: Int,
    val commandId: Int,
    val payload: ByteArray,
    val crc16: Int,
    val lengthMatches: Boolean,
) {
    val sequence16: Int
        get() = sequence or (channel shl 8)

    val payloadAscii: String?
        get() = payload.asciiPreview()

    fun shortSummary(): String {
        val commandName = VoltraCommandCatalog.name(commandId)?.let { " $it" }.orEmpty()
        val sequenceText = if (channel == 0) {
            sequence.toString()
        } else {
            "$sequence16 (lo=0x${sequence.hexByte()} hi=0x${channel.hexByte()})"
        }
        val payloadPreview = payload
            .copyOfRange(0, minOf(payload.size, PAYLOAD_PREVIEW_BYTES))
            .takeIf { it.isNotEmpty() }
            ?.toHexString()
            ?.let { " payload0=$it" }
            .orEmpty()
        val base = "VOLTRA frame type=0x${packetType.hexByte()} sender=0x${senderId.hexByte()} receiver=0x${receiverId.hexByte()} seq=$sequenceText cmd=0x${commandId.hexByte()}$commandName payload=${payload.size} bytes$payloadPreview"
        val suffix = payloadAscii?.let { " ascii=$it" }.orEmpty()
        val lengthWarning = if (lengthMatches) "" else " length-mismatch"
        return base + suffix + lengthWarning
    }

    private companion object {
        const val PAYLOAD_PREVIEW_BYTES = 12
    }
}

object VoltraCommandCatalog {
    fun name(commandId: Int): String? = when (commandId) {
        0x0F -> "bulk-register"
        0x10 -> "async-state"
        0x11 -> "param-write-ack"
        0x19 -> "serial-info"
        0x27 -> "handshake-check"
        0x4F -> "device-name"
        0x74 -> "common-state"
        0x77 -> "firmware-info"
        0xA7 -> "device-state"
        0xAA -> "telemetry-state"
        0xAB -> "activation-security"
        0xAF -> "bulk-param-write"
        0xFF -> "connect-broadcast"
        else -> null
    }
}

object VoltraPacketParser {
    private const val HEADER_BYTE = 0x55
    private const val MIN_PACKET_BYTES = 13

    fun parse(value: ByteArray): ParsedVoltraPacket? {
        if (value.size < MIN_PACKET_BYTES) return null
        if ((value[0].toInt() and 0xFF) != HEADER_BYTE) return null

        val declaredLength = value[1].u8()
        val packetType = value[2].u8()
        val totalLength = expectedFrameLength(declaredLength, packetType)
        return ParsedVoltraPacket(
            declaredLength = declaredLength,
            totalLength = totalLength,
            packetType = packetType,
            headerChecksum = value[3].u8(),
            senderId = value[4].u8(),
            receiverId = value[5].u8(),
            sequence = value[6].u8(),
            channel = value[7].u8(),
            protocol = value[8].u8() or (value[9].u8() shl 8),
            commandId = value[10].u8(),
            payload = value.copyOfRange(11, value.size - 2),
            crc16 = value[value.size - 2].u8() or (value[value.size - 1].u8() shl 8),
            lengthMatches = value.size == totalLength,
        )
    }

    fun expectedFrameLength(header: ByteArray): Int? {
        if (header.size < HEADER_BYTES_NEEDED_FOR_LENGTH) return null
        if ((header[0].toInt() and 0xFF) != HEADER_BYTE) return null
        return expectedFrameLength(
            declaredLength = header[1].u8(),
            packetType = header[2].u8(),
        )
    }

    private fun expectedFrameLength(declaredLength: Int, packetType: Int): Int {
        return if (packetType == EXTENDED_RESPONSE_PACKET_TYPE) {
            EXTENDED_RESPONSE_LENGTH_OFFSET + declaredLength
        } else {
            declaredLength
        }
    }

    private const val HEADER_BYTES_NEEDED_FOR_LENGTH = 3
    private const val EXTENDED_RESPONSE_PACKET_TYPE = 0x09
    private const val EXTENDED_RESPONSE_LENGTH_OFFSET = 0x100
}

private fun Byte.u8(): Int = toInt() and 0xFF

private fun Int.hexByte(): String = toString(16).uppercase().padStart(2, '0')
