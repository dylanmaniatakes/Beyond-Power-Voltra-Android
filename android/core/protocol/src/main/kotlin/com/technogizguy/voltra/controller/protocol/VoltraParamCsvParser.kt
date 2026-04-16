package com.technogizguy.voltra.controller.protocol

import com.technogizguy.voltra.controller.model.VoltraParamDefinition
import com.technogizguy.voltra.controller.model.VoltraParamValueType

object VoltraParamCsvParser {
    fun parse(csv: String): List<VoltraParamDefinition> {
        return csv
            .lineSequence()
            .drop(1)
            .filter { it.isNotBlank() }
            .map(::parseLine)
            .filter { it.size >= 14 }
            .map { fields ->
                VoltraParamDefinition(
                    id = fields[0].toInt(),
                    name = fields[1],
                    description = fields[2],
                    unit = fields[3],
                    valueType = fields[4].toParamValueType(),
                    length = fields[5].toInt(),
                    defaultValue = fields[6],
                    min = fields[7],
                    max = fields[8],
                    requiresReboot = fields[9] == "1",
                    category = fields[10],
                    writable = fields[11] == "1",
                    volatile = fields[12] == "1",
                    submodule = fields[13],
                )
            }
            .toList()
    }

    private fun parseLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i += 1
                }
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    fields += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
            i += 1
        }

        fields += current.toString()
        return fields
    }
}

fun String.toParamValueType(): VoltraParamValueType = when (lowercase()) {
    "uint8" -> VoltraParamValueType.UINT8
    "int8" -> VoltraParamValueType.INT8
    "uint16" -> VoltraParamValueType.UINT16
    "int16" -> VoltraParamValueType.INT16
    "uint32" -> VoltraParamValueType.UINT32
    "int32" -> VoltraParamValueType.INT32
    "uint64" -> VoltraParamValueType.UINT64
    "float" -> VoltraParamValueType.FLOAT
    else -> VoltraParamValueType.UNKNOWN
}
