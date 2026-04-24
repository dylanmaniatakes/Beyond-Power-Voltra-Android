package com.technogizguy.voltra.controller.model

import kotlinx.serialization.Serializable

@Serializable
data class Weight(
    val value: Double,
    val unit: WeightUnit,
) {
    fun cappedForV1(): Weight {
        val range = when (unit) {
            WeightUnit.LB -> 5.0..200.0
            WeightUnit.KG -> 2.5..90.7
        }
        return copy(value = value.coerceIn(range.start, range.endInclusive))
    }

    fun display(): String = "${trim(value)} ${unit.label}"

    fun toUnit(targetUnit: WeightUnit): Weight {
        if (unit == targetUnit) return this
        val pounds = when (unit) {
            WeightUnit.LB -> value
            WeightUnit.KG -> value * POUNDS_PER_KILOGRAM
        }
        val converted = when (targetUnit) {
            WeightUnit.LB -> pounds
            WeightUnit.KG -> pounds / POUNDS_PER_KILOGRAM
        }
        return Weight(converted, targetUnit)
    }

    private fun trim(number: Double): String {
        val rounded = kotlin.math.round(number * 10.0) / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    }

    private companion object {
        const val POUNDS_PER_KILOGRAM = 2.2046226218
    }
}

@Serializable
enum class WeightUnit(val label: String) {
    LB("lb"),
    KG("kg"),
}

@Serializable
data class VoltraDevice(
    val id: String,
    val name: String?,
    val address: String,
    val rssi: Int? = null,
    val advertisedServiceUuids: List<String> = emptyList(),
    val lastSeenMillis: Long = 0L,
    val isLikelyVoltra: Boolean = false,
)

@Serializable
data class VoltraScanResult(
    val device: VoltraDevice,
    val connectable: Boolean? = null,
)

@Serializable
enum class VoltraConnectionState {
    IDLE,
    SCANNING,
    CONNECTING,
    DISCOVERING_SERVICES,
    SUBSCRIBING,
    CONNECTED,
    DISCONNECTING,
    DISCONNECTED,
    FAILED,
}

@Serializable
data class VoltraReading(
    val batteryPercent: Int? = null,
    val firmwareVersion: String? = null,
    val serialNumber: String? = null,
    val activationState: String? = null,
    val lockState: String? = null,
    val childLock: Boolean? = null,
    val cableLengthCm: Double? = null,
    val cableOffsetCm: Double? = null,
    val forceLb: Double? = null,
    val weightLb: Double? = null,
    val resistanceBandMaxForceLb: Double? = null,
    val resistanceBandLengthCm: Double? = null,
    val resistanceBandByRangeOfMotion: Boolean? = null,
    val resistanceBandInverse: Boolean? = null,
    val resistanceBandCurveLogarithm: Boolean? = null,
    val resistanceExperienceIntense: Boolean? = null,
    val quickCableAdjustment: Boolean? = null,
    val damperLevelIndex: Int? = null,
    val assistModeEnabled: Boolean? = null,
    val chainsWeightLb: Double? = null,
    val eccentricWeightLb: Double? = null,
    val inverseChains: Boolean? = null,
    val weightTrainingExtraMode: Int? = null,
    val appCurrentScreenId: Int? = null,
    val fitnessOngoingUi: Int? = null,
    val isokineticMode: Int? = null,
    val isokineticTargetSpeedMmS: Int? = null,
    val isokineticSpeedLimitMmS: Int? = null,
    val isokineticConstantResistanceLb: Double? = null,
    val isokineticMaxEccentricLoadLb: Double? = null,
    val isometricMaxForceLb: Double? = null,
    val isometricMaxDurationSeconds: Int? = null,
    val isometricMetricsType: Int? = null,
    val isometricBodyWeightN: Double? = null,
    val isometricBodyWeight100g: Int? = null,
    val isometricBodyWeightLb: Double? = null,
    val isometricCurrentForceN: Double? = null,
    val isometricPeakForceN: Double? = null,
    val isometricPeakRelativeForcePercent: Double? = null,
    val isometricElapsedMillis: Long? = null,
    val isometricTelemetryTick: Long? = null,
    val isometricTelemetryStartTick: Long? = null,
    val isometricCarrierForceN: Double? = null,
    val isometricCarrierStatusPrimary: Int? = null,
    val isometricCarrierStatusSecondary: Int? = null,
    val isometricWaveformSamplesN: List<Double> = emptyList(),
    val isometricWaveformLastChunkIndex: Int? = null,
    val setCount: Int? = null,
    val repCount: Int? = null,
    val repPhase: String? = null,
    val workoutMode: String? = null,
    val lastUpdatedMillis: Long? = null,
)

@Serializable
data class VoltraSafetyState(
    val canLoad: Boolean = false,
    val reasons: List<String> = listOf("Device state has not been parsed yet."),
    val lowBattery: Boolean? = null,
    val locked: Boolean? = null,
    val childLocked: Boolean? = null,
    val activeOta: Boolean? = null,
    val parsedDeviceState: Boolean = false,
    val workoutState: Int? = null,
    val fitnessMode: Int? = null,
    val targetLoadLb: Double? = null,
)

@Serializable
enum class VoltraControlCommand {
    ENABLE_NOTIFICATIONS,
    READ_VOLTRA_CHARACTERISTICS,
    READ_ONLY_HANDSHAKE_PROBE,
    SET_STRENGTH_MODE,
    EXIT_WORKOUT,
    SET_TARGET_LOAD,
    SET_ASSIST_MODE,
    SET_CHAINS_WEIGHT,
    SET_ECCENTRIC_WEIGHT,
    SET_INVERSE_CHAINS,
    SET_RESISTANCE_EXPERIENCE,
    SET_RESISTANCE_BAND_INVERSE,
    SET_RESISTANCE_BAND_CURVE,
    SET_RESISTANCE_BAND_MODE,
    ENTER_DAMPER_MODE,
    ENTER_ISOKINETIC_MODE,
    ENTER_ISOMETRIC_MODE,
    ENTER_CUSTOM_CURVE_MODE,
    APPLY_CUSTOM_CURVE,
    SET_DAMPER_LEVEL,
    SET_RESISTANCE_BAND_FORCE,
    SET_RESISTANCE_BAND_PROGRESSIVE_LENGTH_MODE,
    SET_RESISTANCE_BAND_LENGTH,
    SET_ISOKINETIC_MENU,
    SET_ISOKINETIC_TARGET_SPEED,
    SET_ISOKINETIC_SPEED,
    SET_ISOKINETIC_CONSTANT_RESISTANCE,
    SET_ISOKINETIC_MAX_ECCENTRIC_LOAD,
    SET_DEVICE_NAME,
    UPLOAD_STARTUP_IMAGE,
    LOAD_RESISTANCE_BAND,
    TRIGGER_CABLE_LENGTH_MODE,
    SET_CABLE_OFFSET,
    REFRESH_MODE_FEATURE_STATUS,
    LOAD,
    UNLOAD,
    WEIGHT_OFF,
    EMERGENCY_DISCONNECT,
}

@Serializable
enum class VoltraCommandStatus {
    QUEUED,
    SENT,
    CONFIRMED,
    BLOCKED,
    TIMED_OUT,
    FAILED,
    CANCELLED,
}

@Serializable
data class VoltraCommandResult(
    val command: VoltraControlCommand,
    val status: VoltraCommandStatus,
    val message: String,
    val timestampMillis: Long,
    val rawHex: String? = null,
)

@Serializable
enum class VoltraParamValueType {
    UINT8,
    INT8,
    UINT16,
    INT16,
    UINT32,
    INT32,
    UINT64,
    FLOAT,
    UNKNOWN,
}

@Serializable
data class VoltraParamDefinition(
    val id: Int,
    val name: String,
    val description: String,
    val unit: String,
    val valueType: VoltraParamValueType,
    val length: Int,
    val defaultValue: String,
    val min: String,
    val max: String,
    val requiresReboot: Boolean,
    val category: String,
    val writable: Boolean,
    val volatile: Boolean,
    val submodule: String,
)

@Serializable
data class VoltraGattSnapshot(
    val services: List<VoltraGattService> = emptyList(),
    val capturedAtMillis: Long = 0L,
) {
    val characteristicCount: Int
        get() = services.sumOf { it.characteristics.size }
}

@Serializable
data class VoltraGattService(
    val uuid: String,
    val characteristics: List<VoltraGattCharacteristic>,
)

@Serializable
data class VoltraGattCharacteristic(
    val serviceUuid: String,
    val uuid: String,
    val properties: List<GattProperty>,
    val descriptors: List<String> = emptyList(),
    val candidateRole: VoltraCharacteristicRole = VoltraCharacteristicRole.UNKNOWN,
)

@Serializable
enum class GattProperty {
    READ,
    WRITE,
    WRITE_NO_RESPONSE,
    NOTIFY,
    INDICATE,
    BROADCAST,
    SIGNED_WRITE,
    EXTENDED,
}

@Serializable
enum class VoltraCharacteristicRole {
    VOLTRA_COMMAND,
    VOLTRA_NOTIFY,
    VOLTRA_TRANSPORT,
    VOLTRA_JUST_WRITE,
    COMMAND_CANDIDATE,
    NOTIFY_CANDIDATE,
    TRANSPORT_CANDIDATE,
    JUST_WRITE_CANDIDATE,
    PM5_KNOWN,
    UNKNOWN,
}

@Serializable
enum class VoltraProtocolStatus {
    UNKNOWN,
    BLE_ONLY,
    VOLTRA_GATT_MATCH,
    RAW_FRAMES_SEEN,
    COMMAND_PROTOCOL_VALIDATED,
}

@Serializable
enum class RawFrameDirection {
    NOTIFY,
    READ,
    WRITE,
    RESPONSE,
}

@Serializable
data class RawVoltraFrame(
    val timestampMillis: Long,
    val serviceUuid: String?,
    val characteristicUuid: String,
    val direction: RawFrameDirection,
    val hex: String,
    val asciiPreview: String? = null,
    val parsedSummary: String? = null,
)

@Serializable
data class VoltraSessionState(
    val connectionState: VoltraConnectionState = VoltraConnectionState.IDLE,
    val currentDevice: VoltraDevice? = null,
    val reading: VoltraReading = VoltraReading(),
    val safety: VoltraSafetyState = VoltraSafetyState(),
    val gattSnapshot: VoltraGattSnapshot? = null,
    val rawFrames: List<RawVoltraFrame> = emptyList(),
    val commandLog: List<VoltraCommandResult> = emptyList(),
    val protocolStatus: VoltraProtocolStatus = VoltraProtocolStatus.UNKNOWN,
    val subscribedCharacteristicCount: Int = 0,
    val controlCommandsEnabled: Boolean = false,
    val targetLoad: Weight = Weight(0.0, WeightUnit.LB),
    val statusMessage: String = "Ready to scan.",
    val lastDisconnectReason: String? = null,
    val connectedAtMillis: Long? = null,
    val lastDisconnectAtMillis: Long? = null,
    val lastConnectionDurationMillis: Long? = null,
)
