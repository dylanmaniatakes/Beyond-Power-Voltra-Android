package com.technogizguy.voltra.controller.protocol

/**
 * Confirmed VOLTRA v1 control parameters recovered from official iOS
 * PacketLogger/sysdiagnose captures on 2026-04-14.
 */
object VoltraControlFrames {
    const val CMD_PARAM_READ = 0x0F
    const val CMD_PARAM_WRITE = 0x11
    const val CMD_VENDOR = 0xAA
    const val CMD_BULK_PARAM_WRITE = 0xAF
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
    const val PARAM_APP_CUR_SCR_ID = 0x5011
    const val PARAM_EP_FITNESS_DATA_NOTIFY_HZ = 0x5182
    const val PARAM_EP_FITNESS_DATA_NOTIFY_SUBSCRIBE = 0x5183
    const val PARAM_FITNESS_ONGOING_UI = 0x5467
    const val PARAM_EP_SCR_SWITCH = 0x5165
    const val PARAM_EP_LOGO_APPLY_ACTION = 0x51F8
    const val PARAM_RESISTANCE_EXPERIENCE = 0x52CA
    const val PARAM_EP_RESISTANCE_BAND_INVERSE = 0x52E3
    const val PARAM_EP_MAX_ALLOWED_FORCE = 0x5314
    const val PARAM_POWER_OFF_LOGO_EN = 0x53A6
    const val PARAM_FITNESS_INVERSE_CHAIN = 0x53B0
    const val PARAM_RESISTANCE_BAND_LEN_BY_ROM = 0x53B6
    const val PARAM_RESISTANCE_BAND_LEN = 0x53B7
    const val PARAM_RESISTANCE_BAND_ALGORITHM = 0x5361
    const val PARAM_RESISTANCE_BAND_MAX_FORCE = 0x5362
    const val PARAM_WEIGHT_TRAINING_EXTRA_MODE = 0x53C6
    const val PARAM_ISOMETRIC_METRICS_TYPE = 0x53D1
    const val PARAM_ISOMETRIC_MAX_DURATION = 0x53D2
    const val PARAM_EP_ISOMETRIC_TESTING_BODY_WEIGHT_N = 0x535A
    const val PARAM_EP_ISOMETRIC_TESTING_BODY_WEIGHT_100G = 0x535B
    const val PARAM_EP_ISOMETRIC_TESTING_BODY_WEIGHT_LBS = 0x535C
    const val PARAM_EP_ISOKINETIC_TARGET_SPEED_MM_S = 0x5350
    const val PARAM_QUICK_CABLE_ADJUSTMENT = 0x54BC
    const val PARAM_CUSTOM_LOGO_X = 0x5448
    const val PARAM_CUSTOM_LOGO_Y = 0x5449
    const val PARAM_CUSTOM_LOGO_BG_COLOR = 0x544A
    const val PARAM_ISOKINETIC_ECC_MODE = 0x5410
    const val PARAM_ISOKINETIC_ECC_SPEED_LIMIT = 0x5411
    const val PARAM_ISOKINETIC_ECC_CONST_WEIGHT = 0x5412
    const val PARAM_ISOKINETIC_ECC_OVERLOAD_WEIGHT = 0x5413
    const val PARAM_ISOMETRIC_MAX_FORCE = 0x5431
    const val PARAM_FITNESS_ROWING_DAMPER_RATIO_IDX = 0x53A7
    const val PARAM_EP_ROW_CHAIN_GEAR = 0x53AE
    const val MIN_ROWING_SELECTOR_LEVEL = 1
    const val MAX_ROWING_SELECTOR_LEVEL = 10
    const val DEFAULT_ROWING_RESISTANCE_LEVEL = 4
    const val DEFAULT_ROWING_SIMULATED_WEAR_LEVEL = 8

    const val FITNESS_MODE_ISOMETRIC_ARMED = 0x0001
    const val FITNESS_MODE_STRENGTH_READY = 0x0004
    const val FITNESS_MODE_STRENGTH_LOADED = 0x0005
    const val FITNESS_MODE_ROWING_ACTIVE = 0x0015
    const val FITNESS_MODE_TEST_SCREEN = 0x0085
    const val ISOKINETIC_MENU_ISOKINETIC = 0x00
    const val ISOKINETIC_MENU_CONSTANT_RESISTANCE = 0x01

    const val WORKOUT_STATE_INACTIVE = 0x00
    const val WORKOUT_STATE_ACTIVE = 0x01
    const val WORKOUT_STATE_RESISTANCE_BAND = 0x02
    const val WORKOUT_STATE_ROWING = 0x03
    const val WORKOUT_STATE_DAMPER = 0x04
    const val WORKOUT_STATE_CUSTOM_CURVE = 0x06
    const val WORKOUT_STATE_ISOKINETIC = 0x07
    const val WORKOUT_STATE_ISOMETRIC = 0x08
    const val STARTUP_IMAGE_SIZE_PX = 720
    const val STARTUP_IMAGE_HEADER_FIXED_FLAGS = 0x0001
    const val STARTUP_IMAGE_HEADER_UNKNOWN_MARKER = 0xFFFF
    // Official captures show two startup-image header trailers: compact
    // transfers around 54 KB use 0x0000, while larger cropped photos use 0x0002.
    // AD03 media chunks carry 464 image bytes; chunks whose full frame length
    // exceeds one byte are sent as frame type 0x05, while short control frames
    // stay type 0x04.
    const val STARTUP_IMAGE_HEADER_COMPACT_TRAILER = 0x0000
    const val STARTUP_IMAGE_HEADER_CUSTOM_PHOTO_TRAILER = 0x0002
    const val STARTUP_IMAGE_CHUNK_DATA_BYTES = 464
    const val DEVICE_NAME_MAX_BYTES = 21
    const val CUSTOM_CURVE_POINT_COUNT = 4
    private const val CUSTOM_CURVE_WIRE_POINT_COUNT = 6
    const val MIN_CUSTOM_CURVE_RESISTANCE_LIMIT_LB = 5
    const val MAX_CUSTOM_CURVE_RESISTANCE_LIMIT_LB = 200
    const val MIN_CUSTOM_CURVE_RESISTANCE_SPAN_LB = 20
    const val DEFAULT_CUSTOM_CURVE_RESISTANCE_MIN_LB = 5
    const val DEFAULT_CUSTOM_CURVE_RESISTANCE_LIMIT_LB = 100
    const val MIN_CUSTOM_CURVE_RANGE_OF_MOTION_IN = 20
    const val MAX_CUSTOM_CURVE_RANGE_OF_MOTION_IN = 118
    const val DEFAULT_CUSTOM_CURVE_RANGE_OF_MOTION_IN = 117
    private const val MAX_CUSTOM_CURVE_WIRE_RANGE_OF_MOTION_TENTHS_IN = 1170
    private const val CUSTOM_CURVE_WIRE_FULL_SCALE_SPAN_LB = 120.0f
    const val ROWING_SCREEN_ID = 0x3E
    const val ROWING_ONGOING_UI = 0x0303
    private const val ROW_ACTION_START_JUST_ROW = 0x03
    private const val ROW_ACTION_SELECT_JUST_ROW = 0x04
    private const val ROW_ACTION_START_SELECTED_DISTANCE = 0x06
    private val CUSTOM_CURVE_WIRE_X_POINTS = listOf(
        0.16903418f,
        0.33806837f,
        0.50473505f,
        0.6714017f,
        0.83570087f,
        1.0f,
    )
    private val CUSTOM_CURVE_UI_X_POINTS = listOf(
        0.0f,
        CUSTOM_CURVE_WIRE_X_POINTS[1],
        CUSTOM_CURVE_WIRE_X_POINTS[3],
        1.0f,
    )
    private val CUSTOM_CURVE_CAPTURED_WIRE_Y_POINTS = listOf(
        0.123481624f,
        0.24696325f,
        0.41362992f,
        0.5802966f,
        0.7901483f,
        1.0f,
    )
    val DEFAULT_CUSTOM_CURVE_POINTS = listOf(0.0f, 0.24696325f, 0.5802966f, 1.0f)

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

    fun enterRowPayload(): ByteArray {
        return paramWritePayload(PARAM_FITNESS_WORKOUT_STATE, byteArrayOf(WORKOUT_STATE_ROWING.toByte()))
    }

    fun loadRowPayload(): ByteArray {
        return paramWritePayload(PARAM_BP_SET_FITNESS_MODE, uint16Le(FITNESS_MODE_ROWING_ACTIVE))
    }

    fun startJustRowPayload(): ByteArray = loadRowPayload()

    fun selectJustRowScreenPayload(): ByteArray {
        return paramWritePayload(
            PARAM_EP_SCR_SWITCH,
            byteArrayOf(ROW_ACTION_SELECT_JUST_ROW.toByte(), ROWING_SCREEN_ID.toByte(), 0x00, 0x01),
        )
    }

    fun triggerRowStartScreenPayload(targetMeters: Int? = null): ByteArray {
        return paramWritePayload(
            PARAM_EP_SCR_SWITCH,
            byteArrayOf(rowStartActionCode(targetMeters).toByte(), ROWING_SCREEN_ID.toByte(), 0x00, 0x01),
        )
    }

    fun rowStartActionCode(targetMeters: Int?): Int {
        return when (targetMeters) {
            null -> ROW_ACTION_START_JUST_ROW
            50 -> ROW_ACTION_START_SELECTED_DISTANCE
            100 -> 0x07
            500 -> 0x08
            1000 -> 0x09
            2000 -> 0x0A
            5000 -> 0x0B
            else -> error("Unsupported Row target distance: $targetMeters")
        }
    }

    fun enterCustomCurvePayload(): ByteArray {
        return paramWritePayload(PARAM_FITNESS_WORKOUT_STATE, byteArrayOf(WORKOUT_STATE_CUSTOM_CURVE.toByte()))
    }

    fun readIsometricCablePositionPayload(): ByteArray {
        return readParamsPayload(
            PARAM_MC_DEFAULT_OFFLEN_CM,
            PARAM_BP_RUNTIME_POSITION_CM,
        )
    }

    fun setFitnessDataNotifySubscribePayload(): ByteArray {
        return paramWritePayload(
            PARAM_EP_FITNESS_DATA_NOTIFY_SUBSCRIBE,
            byteArrayOf(0xF5.toByte(), 0x7B, 0x65, 0x00),
        )
    }

    fun resetFitnessDataNotifySubscribePayload(): ByteArray {
        return paramWritePayload(
            PARAM_EP_FITNESS_DATA_NOTIFY_SUBSCRIBE,
            byteArrayOf(0x00, 0x00, 0x00, 0x00),
        )
    }

    fun setFitnessDataNotifyHzPayload(): ByteArray {
        return paramWritePayload(PARAM_EP_FITNESS_DATA_NOTIFY_HZ, byteArrayOf(0x28))
    }

    fun resetFitnessDataNotifyHzPayload(): ByteArray {
        return paramWritePayload(PARAM_EP_FITNESS_DATA_NOTIFY_HZ, byteArrayOf(0x00))
    }

    fun setDamperLevelPayload(level: Int): ByteArray {
        require(level in 0..9) {
            "Damper level index must be between 0 and 9, got $level."
        }
        return paramWritePayload(PARAM_FITNESS_DAMPER_RATIO_IDX, byteArrayOf(level.toByte()))
    }

    fun setRowingResistanceLevelPayload(level: Int): ByteArray {
        return paramWritePayload(
            PARAM_FITNESS_ROWING_DAMPER_RATIO_IDX,
            byteArrayOf(rowingSelectorWireIndex(level).toByte()),
        )
    }

    fun setRowingSimulatedWearLevelPayload(level: Int): ByteArray {
        return paramWritePayload(
            PARAM_EP_ROW_CHAIN_GEAR,
            byteArrayOf(rowingSelectorWireIndex(level).toByte()),
        )
    }

    fun rowingSelectorWireIndex(level: Int): Int {
        require(level in MIN_ROWING_SELECTOR_LEVEL..MAX_ROWING_SELECTOR_LEVEL) {
            "Rowing selector level must be between $MIN_ROWING_SELECTOR_LEVEL and $MAX_ROWING_SELECTOR_LEVEL, got $level."
        }
        return level - 1
    }

    fun rowingSelectorDisplayLevel(wireIndex: Int?): Int? {
        return wireIndex?.takeIf { it in 0..9 }?.plus(1)
    }

    fun setResistanceBandMaxForcePayload(weightLb: Int): ByteArray {
        require(weightLb in MIN_RESISTANCE_BAND_FORCE_LB..MAX_RESISTANCE_BAND_FORCE_LB) {
            "Resistance Band force must be between $MIN_RESISTANCE_BAND_FORCE_LB and $MAX_RESISTANCE_BAND_FORCE_LB lb, got $weightLb."
        }
        return paramWritePayload(PARAM_RESISTANCE_BAND_MAX_FORCE, uint16Le(weightLb))
    }

    fun setMaxAllowedForcePayload(weightLb: Int): ByteArray {
        require(weightLb in MIN_CUSTOM_CURVE_RESISTANCE_LIMIT_LB..MAX_CUSTOM_CURVE_RESISTANCE_LIMIT_LB) {
            "Max allowed force must be between $MIN_CUSTOM_CURVE_RESISTANCE_LIMIT_LB and $MAX_CUSTOM_CURVE_RESISTANCE_LIMIT_LB lb, got $weightLb."
        }
        return paramWritePayload(PARAM_EP_MAX_ALLOWED_FORCE, uint16Le(weightLb))
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
        trailer: Int = startupImageHeaderTrailer(imageBytes, chunkCount),
    ): ByteArray {
        require(chunkCount in 1..0xFFFF) { "Startup image chunk count must be between 1 and 65535." }
        require(trailer in 0..0xFFFF) { "Startup image header trailer must fit uint16." }
        return byteArrayOf(0x02, STARTUP_IMAGE_HEADER_FIXED_FLAGS.toByte()) +
            uint16Le(STARTUP_IMAGE_HEADER_UNKNOWN_MARKER) +
            uint32Le(0) +
            uint16Le(width) +
            uint16Le(height) +
            uint32Le(0) +
            uint32Le(startupImageFingerprint(imageBytes)) +
            uint16Le(trailer) +
            uint16Le(chunkCount)
    }

    fun startupImageHeaderTrailer(imageBytes: ByteArray, chunkCount: Int): Int {
        require(chunkCount in 1..0xFFFF) { "Startup image chunk count must be between 1 and 65535." }
        return if (
            imageBytes.size >= STARTUP_IMAGE_LARGE_CUSTOM_PHOTO_MIN_BYTES ||
            chunkCount >= STARTUP_IMAGE_LARGE_CUSTOM_PHOTO_MIN_CHUNKS
        ) {
            STARTUP_IMAGE_HEADER_CUSTOM_PHOTO_TRAILER
        } else {
            STARTUP_IMAGE_HEADER_COMPACT_TRAILER
        }
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

    fun customCurveBulkSubscribePayload(): ByteArray =
        (
            "024100b04f015053018153015153016e50017f5301a85101c45301115401883e01a75301065101645301853e" +
                "01315401cf5301145401873e016a5001825201155401e14e01835101de5401525301823e01675401863e0155" +
                "53018c5401e552012d4e011150011853015b53011353010f5401d25301245101195301893e01035101b65301" +
                "4154018b5401ae53016f5001625301b05301c95301c85301df54010f5201025101145301b75301c753011254" +
                "01215401c65301d45401c553018d5301135401105401"
            ).hexToByteArray()

    fun rowBulkSubscribePayload(): ByteArray =
        (
            "0241007f5301505301515301a85101525301c75301145301835101245101105401ae5301145401675401df54" +
                "01b04f010f5401065101c85301cf5301823e01645301e55201185301125401c45301315401155401a75301" +
                "863e012d4e01135301893e018252015b5301135401195301815301b65301555301883e01625301215401b0" +
                "5301c95301de5401873e01e14e010f52018b5401c553018d5301025101415401d454011154016a5001c653" +
                "01035101853e016f5001115001d253018c54016e5001b75301"
            ).hexToByteArray()

    fun customCurveVendorPresetPayload(
        points: List<Float> = DEFAULT_CUSTOM_CURVE_POINTS,
        resistanceMinLb: Int = DEFAULT_CUSTOM_CURVE_RESISTANCE_MIN_LB,
        resistanceLimitLb: Int = DEFAULT_CUSTOM_CURVE_RESISTANCE_LIMIT_LB,
        rangeOfMotionIn: Int = DEFAULT_CUSTOM_CURVE_RANGE_OF_MOTION_IN,
    ): ByteArray {
        require(points.size == CUSTOM_CURVE_POINT_COUNT) {
            "Custom Curve requires $CUSTOM_CURVE_POINT_COUNT points, got ${points.size}."
        }
        require(resistanceMinLb in MIN_CUSTOM_CURVE_RESISTANCE_LIMIT_LB..MAX_CUSTOM_CURVE_RESISTANCE_LIMIT_LB) {
            "Custom Curve resistance minimum must be between $MIN_CUSTOM_CURVE_RESISTANCE_LIMIT_LB and $MAX_CUSTOM_CURVE_RESISTANCE_LIMIT_LB lb, got $resistanceMinLb."
        }
        require(resistanceLimitLb in MIN_CUSTOM_CURVE_RESISTANCE_LIMIT_LB..MAX_CUSTOM_CURVE_RESISTANCE_LIMIT_LB) {
            "Custom Curve resistance limit must be between $MIN_CUSTOM_CURVE_RESISTANCE_LIMIT_LB and $MAX_CUSTOM_CURVE_RESISTANCE_LIMIT_LB lb, got $resistanceLimitLb."
        }
        require(resistanceLimitLb - resistanceMinLb >= MIN_CUSTOM_CURVE_RESISTANCE_SPAN_LB) {
            "Custom Curve resistance range must span at least $MIN_CUSTOM_CURVE_RESISTANCE_SPAN_LB lb, got $resistanceMinLb..$resistanceLimitLb."
        }
        require(rangeOfMotionIn in MIN_CUSTOM_CURVE_RANGE_OF_MOTION_IN..MAX_CUSTOM_CURVE_RANGE_OF_MOTION_IN) {
            "Custom Curve range of motion must be between $MIN_CUSTOM_CURVE_RANGE_OF_MOTION_IN and $MAX_CUSTOM_CURVE_RANGE_OF_MOTION_IN in, got $rangeOfMotionIn."
        }
        val header = byteArrayOf(0x06, 0x02, 0x00, 0x00) +
            uint16Le(customCurveWireRangeOfMotionTenthsIn(rangeOfMotionIn)) +
            uint16Le(resistanceLimitLb) +
            byteArrayOf(resistanceLimitLb.toByte(), resistanceMinLb.toByte()) +
            "e64e9cea030000000000000000".hexToByteArray()
        val wirePoints = customCurveWirePoints(
            points = points,
            resistanceMinLb = resistanceMinLb,
            resistanceLimitLb = resistanceLimitLb,
        )
        require(wirePoints.size == CUSTOM_CURVE_WIRE_POINT_COUNT) {
            "Custom Curve payload requires $CUSTOM_CURVE_WIRE_POINT_COUNT wire points, got ${wirePoints.size}."
        }
        val firstHalf = wirePoints.take(3).flatMap { (x, y) -> float32Le(x).toList() + float32Le(y).toList() }
        val secondHalf = wirePoints.drop(3).flatMap { (x, y) -> float32Le(x).toList() + float32Le(y).toList() }
        return header + firstHalf.toByteArray() + byteArrayOf(0x0D) + secondHalf.toByteArray()
    }

    private fun customCurveWirePoints(
        points: List<Float>,
        resistanceMinLb: Int,
        resistanceLimitLb: Int,
    ): List<Pair<Float, Float>> {
        val normalized = points.map { it.coerceIn(0.0f, 1.0f) }
        if (
            normalized == DEFAULT_CUSTOM_CURVE_POINTS &&
            resistanceMinLb == DEFAULT_CUSTOM_CURVE_RESISTANCE_MIN_LB &&
            resistanceLimitLb == DEFAULT_CUSTOM_CURVE_RESISTANCE_LIMIT_LB
        ) {
            return CUSTOM_CURVE_WIRE_X_POINTS.zip(CUSTOM_CURVE_CAPTURED_WIRE_Y_POINTS)
        }
        val yScale = (resistanceLimitLb - resistanceMinLb).toFloat() / CUSTOM_CURVE_WIRE_FULL_SCALE_SPAN_LB
        return CUSTOM_CURVE_WIRE_X_POINTS.map { x ->
            x to interpolateCustomCurveY(x, normalized) * yScale
        }
    }

    private fun interpolateCustomCurveY(x: Float, points: List<Float>): Float {
        if (x <= CUSTOM_CURVE_UI_X_POINTS.first()) return points.first()
        for (index in 0 until CUSTOM_CURVE_UI_X_POINTS.lastIndex) {
            val startX = CUSTOM_CURVE_UI_X_POINTS[index]
            val endX = CUSTOM_CURVE_UI_X_POINTS[index + 1]
            if (x <= endX) {
                val span = endX - startX
                val t = if (span == 0.0f) 0.0f else (x - startX) / span
                return points[index] + ((points[index + 1] - points[index]) * t)
            }
        }
        return points.last()
    }

    private fun customCurveWireRangeOfMotionTenthsIn(rangeOfMotionIn: Int): Int {
        // The iPad UI exposes 118 in, but the captured AA06 preset tops out at
        // 0x0492 (117.0 in). Sending 1180 can overrun the high end on hardware.
        return (rangeOfMotionIn * 10).coerceAtMost(MAX_CUSTOM_CURVE_WIRE_RANGE_OF_MOTION_TENTHS_IN)
    }

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
            WORKOUT_STATE_ROWING -> normalizedFitnessMode(mode) == FITNESS_MODE_ROWING_ACTIVE
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

    private fun float32Le(value: Float): ByteArray {
        return uint32Le(value.toRawBits())
    }

    private fun int16Le(value: Int): ByteArray {
        require(value in Short.MIN_VALUE..Short.MAX_VALUE) {
            "Value must fit in signed int16, got $value."
        }
        return uint16Le(value)
    }

    private fun startupImageFingerprint(imageBytes: ByteArray): Int {
        val sizeLow16 = imageBytes.size and 0xFFFF
        return (sizeLow16 shl 16) or startupImageCrc16(imageBytes)
    }

    private fun startupImageCrc16(imageBytes: ByteArray): Int {
        var crc = 0x496C
        for (byte in imageBytes) {
            crc = crc xor (reflect8(byte.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) {
                    ((crc shl 1) xor 0x1021) and 0xFFFF
                } else {
                    (crc shl 1) and 0xFFFF
                }
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

    private const val STARTUP_IMAGE_LARGE_CUSTOM_PHOTO_MIN_BYTES = 80_000
    private const val STARTUP_IMAGE_LARGE_CUSTOM_PHOTO_MIN_CHUNKS = 200

}
