package com.technogizguy.voltra.controller.protocol

private val HEX = "0123456789ABCDEF".toCharArray()

fun ByteArray.toHexString(): String {
    val out = CharArray(size * 2)
    forEachIndexed { index, byte ->
        val unsigned = byte.toInt() and 0xFF
        out[index * 2] = HEX[unsigned ushr 4]
        out[index * 2 + 1] = HEX[unsigned and 0x0F]
    }
    return String(out)
}

fun String.hexToByteArray(): ByteArray {
    val compact = filterNot { it.isWhitespace() || it == ':' || it == '-' }
    require(compact.length % 2 == 0) { "Hex string must have an even number of digits." }

    return ByteArray(compact.length / 2) { index ->
        val high = compact[index * 2].digitToInt(16)
        val low = compact[index * 2 + 1].digitToInt(16)
        ((high shl 4) or low).toByte()
    }
}

fun ByteArray.asciiPreview(): String? {
    if (isEmpty()) return null
    val printable = map { byte ->
        val value = byte.toInt() and 0xFF
        if (value in 32..126) value.toChar() else '.'
    }.joinToString("")
    return printable.takeIf { text -> text.any { it != '.' } }
}
