package com.technogizguy.voltra.controller

import com.technogizguy.voltra.controller.model.WeightUnit
import kotlinx.serialization.Serializable

@Serializable
enum class WeightPresetScope(val label: String) {
    WEIGHT_TRAINING("Weight Training"),
    RESISTANCE_BAND("Resistance Band"),
}

@Serializable
data class WeightPreset(
    val id: String,
    val name: String,
    val scope: WeightPresetScope,
    val value: Double,
    val unit: WeightUnit,
    val createdAtMillis: Long,
)

@Serializable
data class WorkoutHistoryEntry(
    val id: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val deviceName: String? = null,
    val modeLabel: String,
    val primarySetting: String? = null,
    val reps: Int = 0,
    val sets: Int = 0,
    val peakForceN: Double? = null,
    val batteryStartPercent: Int? = null,
    val batteryEndPercent: Int? = null,
)
