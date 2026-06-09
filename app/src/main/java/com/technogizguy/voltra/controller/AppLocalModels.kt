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
data class CustomCurvePreset(
    val id: String,
    val name: String,
    val points: List<Float>,
    val resistanceMinLb: Int = 5,
    val resistanceLimitLb: Int = 100,
    val rangeOfMotionIn: Int = 117,
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
    val peakForceLb: Double? = null,
    val peakPowerWatts: Int? = null,
    val timeToPeakMillis: Long? = null,
    val rowingDistanceMeters: Double? = null,
    val rowingElapsedMillis: Long? = null,
    val rowingPace500Millis: Long? = null,
    val rowingAveragePace500Millis: Long? = null,
    val rowingStrokeRateSpm: Int? = null,
    val batteryStartPercent: Int? = null,
    val batteryEndPercent: Int? = null,
)

data class GitHubUpdateState(
    val checking: Boolean = false,
    val latestVersion: String? = null,
    val latestTag: String? = null,
    val releaseUrl: String? = null,
    val apkDownloadUrl: String? = null,
    val updateAvailable: Boolean = false,
    val checkedAtMillis: Long? = null,
    val message: String = "Not checked yet.",
    val error: String? = null,
)
