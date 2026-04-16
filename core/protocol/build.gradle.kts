import java.util.Locale

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
    sourceSets {
        main {
            kotlin.srcDir(layout.buildDirectory.dir("generated/source/voltraParams/main"))
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.kotlin.test)
}

fun parseCsvLine(line: String): List<String> {
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

fun String.kotlinLiteral(): String = buildString {
    append('"')
    this@kotlinLiteral.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(char)
        }
    }
    append('"')
}

fun valueTypeName(raw: String): String = when (raw.lowercase(Locale.US)) {
    "uint8" -> "UINT8"
    "int8" -> "INT8"
    "uint16" -> "UINT16"
    "int16" -> "INT16"
    "uint32" -> "UINT32"
    "int32" -> "INT32"
    "uint64" -> "UINT64"
    "float" -> "FLOAT"
    else -> "UNKNOWN"
}

val generateVoltraParamRegistry by tasks.registering {
    val csvFile = rootProject.layout.projectDirectory.file("../ios/BPBLECommunicator_BPBLECommunicator.bundle/paramInfo.csv")
    val outputFile = layout.buildDirectory.file(
        "generated/source/voltraParams/main/com/technogizguy/voltra/controller/protocol/VoltraParamRegistry.kt",
    )

    inputs.file(csvFile)
    outputs.file(outputFile)

    doLast {
        val rows = csvFile.asFile
            .readLines()
            .drop(1)
            .filter { it.isNotBlank() }
            .map { parseCsvLine(it) }
            .filter { it.size >= 14 }

        val generated = buildString {
            appendLine("package com.technogizguy.voltra.controller.protocol")
            appendLine()
            appendLine("import com.technogizguy.voltra.controller.model.VoltraParamDefinition")
            appendLine("import com.technogizguy.voltra.controller.model.VoltraParamValueType")
            appendLine()
            appendLine("/** Generated from ios/BPBLECommunicator_BPBLECommunicator.bundle/paramInfo.csv. */")
            appendLine("public object VoltraParamRegistry {")
            appendLine("    public val all: List<VoltraParamDefinition> = buildList {")
            rows.chunked(100).indices.forEach { chunkIndex ->
                appendLine("        addAll(chunk$chunkIndex())")
            }
            appendLine("    }")
            appendLine()
            appendLine("    public val byId: Map<Int, VoltraParamDefinition> = all.associateBy { it.id }")
            appendLine("    public val byName: Map<String, VoltraParamDefinition> = all.associateBy { it.name }")
            appendLine("}")
            appendLine()
            rows.chunked(100).forEachIndexed { chunkIndex, chunk ->
                appendLine("private fun chunk$chunkIndex(): List<VoltraParamDefinition> = listOf(")
                chunk.forEachIndexed { index, fields ->
                    val suffix = if (index == chunk.lastIndex) "" else ","
                    appendLine(
                        "    VoltraParamDefinition(" +
                            "id = ${fields[0].toInt()}, " +
                            "name = ${fields[1].kotlinLiteral()}, " +
                            "description = ${fields[2].kotlinLiteral()}, " +
                            "unit = ${fields[3].kotlinLiteral()}, " +
                            "valueType = VoltraParamValueType.${valueTypeName(fields[4])}, " +
                            "length = ${fields[5].toInt()}, " +
                            "defaultValue = ${fields[6].kotlinLiteral()}, " +
                            "min = ${fields[7].kotlinLiteral()}, " +
                            "max = ${fields[8].kotlinLiteral()}, " +
                            "requiresReboot = ${fields[9] == "1"}, " +
                            "category = ${fields[10].kotlinLiteral()}, " +
                            "writable = ${fields[11] == "1"}, " +
                            "volatile = ${fields[12] == "1"}, " +
                            "submodule = ${fields[13].kotlinLiteral()}" +
                            ")$suffix",
                    )
                }
                appendLine(")")
                appendLine()
            }
        }

        outputFile.get().asFile.parentFile.mkdirs()
        outputFile.get().asFile.writeText(generated)
    }
}

tasks.named("compileKotlin") {
    dependsOn(generateVoltraParamRegistry)
}

tasks.named("compileTestKotlin") {
    dependsOn(generateVoltraParamRegistry)
}
