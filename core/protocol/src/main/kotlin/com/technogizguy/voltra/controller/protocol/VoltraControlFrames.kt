package com.technogizguy.voltra.controller.protocol

/**
 * Confirmed VOLTRA v1 control parameters recovered from official iOS
 * PacketLogger/sysdiagnose captures on 2026-04-14.
 */
object VoltraControlFrames {
    const val CMD_PARAM_READ = 0x0F
    const val CMD_PARAM_WRITE = 0x11
    const val CMD_VENDOR = 0xAA
    const val CMD_SET_DEVICE_NAME = 0x4E
    const val CMD_READ_DEVICE_NAME = 0x4F
    const val CMD_STARTUP_IMAGE = 0xAD

    const val MIN_TARGET_LB = 5
    const val MAX_TARGET_LB = 200
    const val MIN_EXTRA_WEIGHT_LB = 0
    const val MAX_EXTRA_WEIGHT_LB = 200
    const val MIN_ECCENTRIC_WEIGHT_LB = -200
    const val MAX_ECCENTRIC_WEIGHT_LB = 200
    const val MIN_RESISTANCE_BAND_FORCE_LB = 15
    const val MAX_RESISTANCE_BAND_FORCE_LB = 200
    const val MIN_RESISTANCE_BAND_LENGTH_CM = 50
    const val MAX_RESISTANCE_BAND_LENGTH_CM = 260
    const val MIN_ISOKINETIC_CONSTANT_RESISTANCE_LB = 5
    const val MAX_ISOKINETIC_CONSTANT_RESISTANCE_LB = 100
    const val MIN_ISOKINETIC_MAX_ECCENTRIC_LOAD_LB = 5
    const val MAX_ISOKINETIC_MAX_ECCENTRIC_LOAD_LB = 200
    const val AUTO_ISOKINETIC_SPEED_MM_S = 0
    const val MIN_ISOKINETIC_SPEED_MM_S = 100
    const val MAX_ISOKINETIC_SPEED_MM_S = 2000
    const val MIN_CABLE_OFFSET_CM = 0
    const val MAX_CABLE_OFFSET_CM = 260

    const val PARAM_BP_BASE_WEIGHT = 0x3E86
    const val PARAM_BP_RUNTIME_POSITION_CM = 0x3E82
    const val PARAM_BP_CHAINS_WEIGHT = 0x3E87
    const val PARAM_BP_ECCENTRIC_WEIGHT = 0x3E88
    const val PARAM_BP_SET_FITNESS_MODE = 0x3E89
    const val PARAM_MC_DEFAULT_OFFLEN_CM = 0x506A
    const val PARAM_BMS_RSOC = 0x4E2D
    const val PARAM_BMS_RSOC_LEGACY = 0x1B5D
    const val PARAM_FITNESS_WORKOUT_STATE = 0x4FB0
    const val PARAM_FITNESS_DAMPER_RATIO_IDX = 0x5103
    const val PARAM_FITNESS_ASSIST_MODE = 0x5106
    const val PARAM_EP_SCR_SWITCH = 0x5165
    const val PARAM_RESISTANCE_EXPERIENCE = 0x52CA
    const val PARAM_EP_RESISTANCE_BAND_INVERSE = 0x52E3
    const val PARAM_FITNESS_INVERSE_CHAIN = 0x53B0
    const val PARAM_RESISTANCE_BAND_LEN_BY_ROM = 0x53B6
    const val PARAM_RESISTANCE_BAND_LEN = 0x53B7
    const val PARAM_RESISTANCE_BAND_ALGORITHM = 0x5361
    const val PARAM_RESISTANCE_BAND_MAX_FORCE = 0x5362
    const val PARAM_WEIGHT_TRAINING_EXTRA_MODE = 0x53C6
    const val PARAM_ISOMETRIC_MAX_DURATION = 0x53D2
    const val PARAM_EP_ISOKINETIC_TARGET_SPEED_MM_S = 0x5350
    const val PARAM_QUICK_CABLE_ADJUSTMENT = 0x54BC
    const val PARAM_ISOKINETIC_ECC_MODE = 0x5410
    const val PARAM_ISOKINETIC_ECC_SPEED_LIMIT = 0x5411
    const val PARAM_ISOKINETIC_ECC_CONST_WEIGHT = 0x5412
    const val PARAM_ISOKINETIC_ECC_OVERLOAD_WEIGHT = 0x5413
    const val PARAM_ISOMETRIC_MAX_FORCE = 0x5431

    const val FITNESS_MODE_ISOMETRIC_ARMED = 0x0001
    const val FITNESS_MODE_STRENGTH_READY = 0x0004
    const val FITNESS_MODE_STRENGTH_LOADED = 0x0005
    const val FITNESS_MODE_TEST_SCREEN = 0x0085
    const val ISOKINETIC_MENU_ISOKINETIC = 0x00
    const val ISOKINETIC_MENU_CONSTANT_RESISTANCE = 0x01

    const val WORKOUT_STATE_INACTIVE = 0x00
    const val WORKOUT_STATE_ACTIVE = 0x01
    const val WORKOUT_STATE_RESISTANCE_BAND = 0x02
    const val WORKOUT_STATE_DAMPER = 0x04
    const val WORKOUT_STATE_ISOKINETIC = 0x07
    const val WORKOUT_STATE_ISOMETRIC = 0x08
    const val STARTUP_IMAGE_SIZE_PX = 720
    const val STARTUP_IMAGE_HEADER_FIXED_FLAGS = 0x0001
    const val STARTUP_IMAGE_HEADER_UNKNOWN_MARKER = 0xFFFF
    const val STARTUP_IMAGE_HEADER_CUSTOM_PHOTO_TRAILER = 0x0000
    const val STARTUP_IMAGE_CHUNK_DATA_BYTES = 208
    const val DEVICE_NAME_MAX_BYTES = 21

    fun setBaseWeightPayload(weightLb: Int): ByteArray {
        require(weightLb in MIN_TARGET_LB..MAX_TARGET_LB) {
            "Target load must be between $MIN_TARGET_LB and $MAX_TARGET_LB lb, got $weightLb."
        }
        return paramWritePayload(PARAM_BP_BASE_WEIGHT, uint16Le(weightLb))
    }

    fun setChainsWeightPayload(weightLb: Int): ByteArray {
        require(weightLb in MIN_EXTRA_WEIGHT_LB..MAX_EXTRA_WEIGHT_LB) {
            "Chains load must be between $MIN_EXTRA_WEIGHT_LB and $MAX_EXTRA_WEIGHT_LB lb, got $weightLb."
        }
        return paramWritePayload(PARAM_BP_CHAINS_WEIGHT, uint16Le(weightLb))
    }

    fun setAssistModePayload(enabled: Boolean): ByteArray {
        return paramWritePayload(PARAM_FITNESS_ASSIST_MODE, byteArrayOf((if (enabled) 1 else 0).toByte()))
    }

    fun setEccentricWeightPayload(weightLb: Int): ByteArray {
        require(weightLb in MIN_ECCENTRIC_WEIGHT_LB..MAX_ECCENTRIC_WEIGHT_LB) {
            "Eccentric load must be between $MIN_ECCENTRIC_WEIGHT_LB and $MAX_ECCENTRIC_WEIGHT_LB lb, got $weightLb."
        }
        return paramWritePayload(PARAM_BP_ECCENTRIC_WEIGHT, int16Le(weightLb))
    }

    fun setInverseChainsPayload(enabled: Boolean): ByteArray {
        return paramWritePayload(PARAM_FITNESS_INVERSE_CHAIN, byteArrayOf((if (enabled) 1 else 0).toByte()))
    }

    fun enterResistanceBandPayload(): ByteArray {
        return paramWritePayload(PARAM_FITNESS_WORKOUT_STATE, byteArrayOf(WORKOUT_STATE_RESISTANCE_BAND.toByte()))
    }

    fun enterDamperPayload(): ByteArray {
        return paramWritePayload(PARAM_FITNESS_WORKOUT_STATE, byteArrayOf(WORKOUT_STATE_DAMPER.toByte()))
    }

    fun enterIsokineticPayload(): ByteArray {
        return paramWritePayload(PARAM_FITNESS_WORKOUT_STATE, byteArrayOf(WORKOUT_STATE_ISOKINETIC.toByte()))
    }

    fun enterIsometricPayload(): ByteArray {
        return paramWritePayload(PARAM_FITNESS_WORKOUT_STATE, byteArrayOf(WORKOUT_STATE_ISOMETRIC.toByte()))
    }

    fun readIsometricCablePositionPayload(): ByteArray {
        return readParamsPayload(
            PARAM_MC_DEFAULT_OFFLEN_CM,
            PARAM_BP_RUNTIME_POSITION_CM,
        )
    }

    fun setDamperLevelPayload(level: Int): ByteArray {
        require(level in 0..9) {
            "Damper level index must be between 0 and 9, got $level."
        }
        return paramWritePayload(PARAM_FITNESS_DAMPER_RATIO_IDX, byteArrayOf(level.toByte()))
    }

    fun setResistanceBandMaxForcePayload(weightLb: Int): ByteArray {
        require(weightLb in MIN_RESISTANCE_BAND_FORCE_LB..MAX_RESISTANCE_BAND_FORCE_LB) {
            "Resistance Band force must be between $MIN_RESISTANCE_BAND_FORCE_LB and $MAX_RESISTANCE_BAND_FORCE_LB lb, got $weightLb."
        }
        return paramWritePayload(PARAM_RESISTANCE_BAND_MAX_FORCE, uint16Le(weightLb))
    }

    fun setResistanceBandLengthByRomPayload(enabled: Boolean): ByteArray {
        return paramWritePayload(PARAM_RESISTANCE_BAND_LEN_BY_ROM, byteArrayOf((if (enabled) 1 else 0).toByte()))
    }

    fun setResistanceBandInversePayload(enabled: Boolean): ByteArray {
        return paramWritePayload(PARAM_EP_RESISTANCE_BAND_INVERSE, byteArrayOf((if (enabled) 1 else 0).toByte()))
    }

    fun setResistanceBandAlgorithmPayload(logarithm: Boolean): ByteArray {
        return paramWritePayload(PARAM_RESISTANCE_BAND_ALGORITHM, byteArrayOf((if (logarithm) 1 else 0).toByte()))
    }

    fun setResistanceExperiencePayload(intense: Boolean): ByteArray {
        return paramWritePayload(PARAM_RESISTANCE_EXPERIENCE, byteArrayOf((if (intense) 0 else 1).toByte()))
    }

    fun setIsokineticMenuPayload(mode: Int): ByteArray {
        require(mode == ISOKINETIC_MENU_CONSTANT_RESISTANCE || mode == ISOKINETIC_MENU_ISOKINETIC) {
            "Isokinetic menu mode must be $ISOKINETIC_MENU_CONSTANT_RESISTANCE or $ISOKINETIC_MENU_ISOKINETIC, got $mode."
        }
        return paramWritePayload(PARAM_ISOKINETIC_ECC_MODE, byteArrayOf(mode.toByte()))
    }

    fun setIsokineticTargetSpeedPayload(speedMmS: Int): ByteArray {
        require(speedMmS in MIN_ISOKINETIC_SPEED_MM_S..MAX_ISOKINETIC_SPEED_MM_S) {
            "Isokinetic target speed must be between $MIN_ISOKINETIC_SPEED_MM_S and $MAX_ISOKINETIC_SPEED_MM_S mm/s, got $speedMmS."
        }
        return paramWritePayload(PARAM_EP_ISOKINETIC_TARGET_SPEED_MM_S, uint32Le(speedMmS))
    }

    fun setIsokineticSpeedLimitPayload(speedMmS: Int): ByteArray {
        require(
            speedMmS == AUTO_ISOKINETIC_SPEED_MM_S ||
                speedMmS in MIN_ISOKINETIC_SPEED_MM_S..MAX_ISOKINETIC_SPEED_MM_S,
        ) {
            "Isokinetic speed must be Auto (0) or between $MIN_ISOKINETIC_SPEED_MM_S and $MAX_ISOKINETIC_SPEED_MM_S mm/s, got $speedMmS."
        }
        return paramWritePayload(PARAM_ISOKINETIC_ECC_SPEED_LIMIT, uint16Le(speedMmS))
    }

    fun setIsokineticConstantResistancePayload(weightLb: Int): ByteArray {
        require(weightLb in MIN_ISOKINETIC_CONSTANT_RESISTANCE_LB..MAX_ISOKINETIC_CONSTANT_RESISTANCE_LB) {
            "Isokinetic constant resistance must be between $MIN_ISOKINETIC_CONSTANT_RESISTANCE_LB and $MAX_ISOKINETIC_CONSTANT_RESISTANCE_LB lb, got $weightLb."
        }
        return paramWritePayload(PARAM_ISOKINETIC_ECC_CONST_WEIGHT, uint16Le(weightLb))
    }

    fun setIsokineticMaxEccentricLoadPayload(weightLb: Int): ByteArray {
        require(weightLb in MIN_ISOKINETIC_MAX_ECCENTRIC_LOAD_LB..MAX_ISOKINETIC_MAX_ECCENTRIC_LOAD_LB) {
            "Isokinetic max eccentric load must be between $MIN_ISOKINETIC_MAX_ECCENTRIC_LOAD_LB and $MAX_ISOKINETIC_MAX_ECCENTRIC_LOAD_LB lb, got $weightLb."
        }
        return paramWritePayload(PARAM_ISOKINETIC_ECC_OVERLOAD_WEIGHT, uint16Le(weightLb))
    }

    fun setCableOffsetPayload(offsetCm: Int): ByteArray {
        require(offsetCm in MIN_CABLE_OFFSET_CM..MAX_CABLE_OFFSET_CM) {
            "Cable offset must be between $MIN_CABLE_OFFSET_CM and $MAX_CABLE_OFFSET_CM cm, got $offsetCm."
        }
        return paramWritePayload(PARAM_MC_DEFAULT_OFFLEN_CM, uint16Le(offsetCm))
    }

    fun triggerCableLengthModePayload(): ByteArray {
        return paramWritePayload(
            PARAM_EP_SCR_SWITCH,
            byteArrayOf(0x00, 0x10, 0x00, 0x01),
        )
    }

    fun triggerIsometricScreenPayload(): ByteArray {
        return paramWritePayload(
            PARAM_EP_SCR_SWITCH,
            byteArrayOf(0x03, 0x3E, 0x00),
        )
    }

    fun setStrengthModePayload(): ByteArray {
        return paramWritePayload(PARAM_BP_SET_FITNESS_MODE, uint16Le(FITNESS_MODE_STRENGTH_READY))
    }

    fun enterWeightTrainingPayload(): ByteArray {
        return paramWritePayload(PARAM_FITNESS_WORKOUT_STATE, byteArrayOf(WORKOUT_STATE_ACTIVE.toByte()))
    }

    fun exitWeightTrainingPayload(): ByteArray {
        return paramWritePayload(PARAM_FITNESS_WORKOUT_STATE, byteArrayOf(WORKOUT_STATE_INACTIVE.toByte()))
    }

    fun loadIsometricPayload(): ByteArray {
        return paramWritePayload(PARAM_BP_SET_FITNESS_MODE, uint16Le(FITNESS_MODE_ISOMETRIC_ARMED))
    }

    fun loadPayload(): ByteArray {
        return paramWritePayload(PARAM_BP_SET_FITNESS_MODE, uint16Le(FITNESS_MODE_STRENGTH_LOADED))
    }

    fun unloadPayload(): ByteArray {
        return setStrengthModePayload()
    }

    fun setDeviceNamePayload(name: String): ByteArray {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Device name must not be blank." }
        require(trimmed.length <= 20) { "Device name must be 20 characters or fewer." }
        require(trimmed.first().isLetter()) { "Device name must start with a letter." }
        require(trimmed.all { char ->
            char.code in 0x20..0x7E && char != ':' && char != '\\' && char != '|'
        }) {
            "Device name must use plain ASCII and cannot include :, \\\\, or |."
        }
        val ascii = trimmed.encodeToByteArray()
        require(ascii.size <= DEVICE_NAME_MAX_BYTES) {
            "Device name exceeds the VOLTRA payload size."
        }
        return ascii + ByteArray(DEVICE_NAME_MAX_BYTES - ascii.size)
    }

    fun startupImageHeaderPayload(
        imageBytes: ByteArray,
        chunkCount: Int,
        width: Int = STARTUP_IMAGE_SIZE_PX,
        height: Int = STARTUP_IMAGE_SIZE_PX,
    ): ByteArray {
        require(chunkCount in 1..0xFFFF) { "Startup image chunk count must be between 1 and 65535." }
        return byteArrayOf(0x02, STARTUP_IMAGE_HEADER_FIXED_FLAGS.toByte()) +
            uint16Le(STARTUP_IMAGE_HEADER_UNKNOWN_MARKER) +
            uint32Le(0) +
            uint16Le(width) +
            uint16Le(height) +
            uint32Le(0) +
            uint32Le(startupImageFingerprint(imageBytes)) +
            uint16Le(STARTUP_IMAGE_HEADER_CUSTOM_PHOTO_TRAILER) +
            uint16Le(chunkCount)
    }

    fun startupImageChunkPayload(
        chunkIndex: Int,
        chunkBytes: ByteArray,
    ): ByteArray {
        require(chunkIndex in 1..0xFFFF) { "Startup image chunk index must be between 1 and 65535." }
        require(chunkBytes.isNotEmpty()) { "Startup image chunk cannot be empty." }
        require(chunkBytes.size <= STARTUP_IMAGE_CHUNK_DATA_BYTES) {
            "Startup image chunk must be ${STARTUP_IMAGE_CHUNK_DATA_BYTES} bytes or smaller."
        }
        return byteArrayOf(0x03) + uint16Le(chunkIndex) + chunkBytes
    }

    fun startupImageFinalizePayload(): ByteArray = byteArrayOf(0x04)

    fun startupImageApplyPayload(): ByteArray = byteArrayOf(0x05, 0x01)

    fun vendorStateRefreshPayload(): ByteArray = byteArrayOf(0x13, 0x01)

    fun normalizedFitnessMode(mode: Int?): Int? {
        return mode?.and(0xFF)
    }

    fun isReadyFitnessMode(mode: Int?): Boolean {
        return normalizedFitnessMode(mode) == FITNESS_MODE_STRENGTH_READY
    }

    fun isLoadedFitnessMode(mode: Int?): Boolean {
        return normalizedFitnessMode(mode) == FITNESS_MODE_STRENGTH_LOADED
    }

    fun isIsometricScreenMode(mode: Int?): Boolean {
        return normalizedFitnessMode(mode) == FITNESS_MODE_TEST_SCREEN
    }

    fun isLoadEngagedForWorkoutState(mode: Int?, workoutState: Int?): Boolean {
        return when (workoutState) {
            WORKOUT_STATE_ISOMETRIC -> {
                val normalizedMode = normalizedFitnessMode(mode)
                normalizedMode == FITNESS_MODE_ISOMETRIC_ARMED ||
                    normalizedMode == FITNESS_MODE_STRENGTH_LOADED ||
                    normalizedMode == FITNESS_MODE_TEST_SCREEN
            }
            else -> isLoadedFitnessMode(mode)
        }
    }

    fun isIsokineticWorkoutState(workoutState: Int?): Boolean {
        return workoutState == WORKOUT_STATE_ISOKINETIC
    }

    fun isReadyForWorkoutState(mode: Int?, workoutState: Int?): Boolean {
        val normalizedMode = normalizedFitnessMode(mode)
        return when (workoutState) {
            WORKOUT_STATE_ISOMETRIC ->
                normalizedMode == FITNESS_MODE_STRENGTH_READY
            else -> normalizedMode == FITNESS_MODE_STRENGTH_READY
        }
    }

    fun readParamsPayload(vararg paramIds: Int): ByteArray {
        require(paramIds.isNotEmpty()) { "At least one parameter id is required." }
        require(paramIds.size <= 0xFFFF) { "Too many parameter ids: ${paramIds.size}." }
        return uint16Le(paramIds.size) + paramIds.flatMap { paramId ->
            listOf(
                (paramId and 0xFF).toByte(),
                ((paramId shr 8) and 0xFF).toByte(),
            )
        }.toByteArray()
    }

    fun paramWritePayload(paramId: Int, value: ByteArray): ByteArray {
        return byteArrayOf(
            0x01,
            0x00,
            (paramId and 0xFF).toByte(),
            ((paramId shr 8) and 0xFF).toByte(),
        ) + value
    }

    private fun uint16Le(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
        )
    }

    private fun uint32Le(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte(),
        )
    }

    private fun int16Le(value: Int): ByteArray {
        require(value in Short.MIN_VALUE..Short.MAX_VALUE) {
            "Value must fit in signed int16, got $value."
        }
        return uint16Le(value)
    }

    private fun startupImageFingerprint(imageBytes: ByteArray): Int {
        val crc = java.util.zip.CRC32()
        crc.update(imageBytes)
        return crc.value.toInt()
    }

}
