package com.technogizguy.voltra.controller.protocol

import com.technogizguy.voltra.controller.model.RawFrameDirection
import com.technogizguy.voltra.controller.model.RawVoltraFrame
import com.technogizguy.voltra.controller.model.VoltraReading
import com.technogizguy.voltra.controller.model.VoltraSafetyState

object VoltraNotificationParser {
    fun rawFrame(
        serviceUuid: String?,
        characteristicUuid: String,
        value: ByteArray,
        direction: RawFrameDirection = RawFrameDirection.NOTIFY,
        nowMillis: Long = System.currentTimeMillis(),
    ): RawVoltraFrame {
        val parsedPacket = VoltraPacketParser.parse(value)
        return RawVoltraFrame(
            timestampMillis = nowMillis,
            serviceUuid = serviceUuid,
            characteristicUuid = characteristicUuid,
            direction = direction,
            hex = value.toHexString(),
            asciiPreview = value.asciiPreview(),
            parsedSummary = parsedPacket?.shortSummary() ?: "Unparsed VOLTRA payload (${value.size} bytes)",
        )
    }

    fun mergeReading(
        current: VoltraReading,
        value: ByteArray,
        nowMillis: Long = System.currentTimeMillis(),
    ): VoltraReading {
        val packet = VoltraPacketParser.parse(value) ?: return current
        val params = packet.decodeParams()

        // ASCII extraction: parse real printable runs from the payload. The
        // diagnostic preview replaces null bytes with '.', which is helpful for
        // humans but too easy for regexes to over-match.
        val printableSegments = packet.payload.printableAsciiSegments()
        val serial = printableSegments.asSequence()
            .mapNotNull { segment -> SERIAL_REGEX.find(segment)?.value }
            .firstOrNull()
        val firmwareParts = printableSegments.asSequence()
            .flatMap { segment -> FIRMWARE_REGEX.findAll(segment).map { match -> match.value } }
            .distinct()
            .toList()

        // Binary extraction: battery and activation state keyed on command ID
        val batteryPct = parseBattery(packet, params)
        val activationState = parseActivationState(packet)
        val baseWeightLb = params.uint16(PARAM_BP_BASE_WEIGHT)?.toDouble()
        val chainsWeightLb = params.uint16(PARAM_BP_CHAINS_WEIGHT)?.toDouble()
        val eccentricWeightLb = params.signedInt16FromStoredUint16(PARAM_BP_ECCENTRIC_WEIGHT)?.toDouble()
        val inverseChains = params.uint8(PARAM_FITNESS_INVERSE_CHAIN)?.let { it == 1 }
        val wireWeightLb = params.int16(PARAM_BP_RUNTIME_WIRE_WEIGHT_LBS)?.toDouble()
        val cableLengthCm = params.int16(PARAM_BP_RUNTIME_POSITION_CM)?.toDouble()
        val cableOffsetCm = params.uint16(PARAM_MC_DEFAULT_OFFLEN_CM)?.toDouble()
        val resistanceBandMaxForceLb = params.uint16(PARAM_RESISTANCE_BAND_MAX_FORCE)?.toDouble()
        val resistanceBandLengthCm = params.uint16(PARAM_RESISTANCE_BAND_LEN)?.toDouble()
        val resistanceBandByRangeOfMotion = params.uint8(PARAM_RESISTANCE_BAND_LEN_BY_ROM)?.let { it == 1 }
        val resistanceBandInverse = params.uint8(PARAM_EP_RESISTANCE_BAND_INVERSE)?.let { it == 1 }
        val resistanceBandCurveLogarithm = params.uint8(PARAM_RESISTANCE_BAND_ALGORITHM)?.let {
            when (it) {
                0 -> false
                1 -> true
                else -> null
            }
        }
        val resistanceExperienceIntense = params.uint8(PARAM_RESISTANCE_EXPERIENCE)?.let {
            when (it) {
                0 -> true
                1 -> false
                else -> null
            }
        }
        val quickCableAdjustment = params.uint8(PARAM_QUICK_CABLE_ADJUSTMENT)?.let { it == 1 }
        val damperLevelIndex = params.uint8(PARAM_FITNESS_DAMPER_RATIO_IDX)
        val assistModeEnabled = params.uint8(PARAM_FITNESS_ASSIST_MODE)?.let {
            when (it) {
                1 -> true
                0, 8 -> false
                else -> null
            }
        }
        val weightTrainingExtraMode = params.uint8(PARAM_WEIGHT_TRAINING_EXTRA_MODE)
        val isokineticMode = params.uint8(PARAM_ISOKINETIC_ECC_MODE)
        val isokineticTargetSpeedMmS = params.uint32(PARAM_EP_ISOKINETIC_TARGET_SPEED_MM_S)
        val isokineticSpeedLimitMmS = params.uint16(PARAM_ISOKINETIC_ECC_SPEED_LIMIT)
        val isokineticConstantResistanceLb = params.uint16(PARAM_ISOKINETIC_ECC_CONST_WEIGHT)?.toDouble()
        val isokineticMaxEccentricLoadLb = params.uint16(PARAM_ISOKINETIC_ECC_OVERLOAD_WEIGHT)?.toDouble()
        val isometricMaxForceLb = params.uint16(PARAM_ISOMETRIC_MAX_FORCE)?.toDouble()
        val isometricMaxDurationSeconds = params.uint16(PARAM_ISOMETRIC_MAX_DURATION)
        val fitnessMode = params.uint16(PARAM_BP_SET_FITNESS_MODE)
        val workoutState = params.uint8(PARAM_FITNESS_WORKOUT_STATE)
        val isometricTelemetry = parseIsometricTelemetry(packet, current, nowMillis)
        val workoutMode = workoutModeLabel(
            mode = fitnessMode,
            workoutState = workoutState,
        )
        val repTelemetry = parseRepTelemetry(packet)

        if (
            serial == null &&
            firmwareParts.isEmpty() &&
            batteryPct == null &&
            activationState == null &&
            baseWeightLb == null &&
            chainsWeightLb == null &&
            eccentricWeightLb == null &&
            inverseChains == null &&
            wireWeightLb == null &&
            cableLengthCm == null &&
            cableOffsetCm == null &&
            resistanceBandMaxForceLb == null &&
            resistanceBandLengthCm == null &&
            resistanceBandByRangeOfMotion == null &&
            resistanceBandInverse == null &&
            resistanceBandCurveLogarithm == null &&
            resistanceExperienceIntense == null &&
            quickCableAdjustment == null &&
            damperLevelIndex == null &&
            assistModeEnabled == null &&
            weightTrainingExtraMode == null &&
            isokineticMode == null &&
            isokineticTargetSpeedMmS == null &&
            isokineticSpeedLimitMmS == null &&
            isokineticConstantResistanceLb == null &&
            isokineticMaxEccentricLoadLb == null &&
            isometricMaxForceLb == null &&
            isometricMaxDurationSeconds == null &&
            isometricTelemetry == null &&
            workoutMode == null &&
            repTelemetry == null
        ) {
            return current
        }

        val leavingIsometric = workoutState != null && workoutState != VoltraControlFrames.WORKOUT_STATE_ISOMETRIC

        return current.copy(
            serialNumber = serial ?: current.serialNumber,
            firmwareVersion = current.firmwareVersion.mergeFirmwareParts(firmwareParts),
            batteryPercent = batteryPct ?: current.batteryPercent,
            activationState = activationState ?: current.activationState,
            cableLengthCm = cableLengthCm ?: current.cableLengthCm,
            cableOffsetCm = cableOffsetCm ?: current.cableOffsetCm,
            forceLb = wireWeightLb ?: current.forceLb,
            weightLb = baseWeightLb ?: current.weightLb,
            resistanceBandMaxForceLb = resistanceBandMaxForceLb ?: current.resistanceBandMaxForceLb,
            resistanceBandLengthCm = resistanceBandLengthCm ?: current.resistanceBandLengthCm,
            resistanceBandByRangeOfMotion = resistanceBandByRangeOfMotion ?: current.resistanceBandByRangeOfMotion,
            resistanceBandInverse = resistanceBandInverse ?: current.resistanceBandInverse,
            resistanceBandCurveLogarithm = resistanceBandCurveLogarithm ?: current.resistanceBandCurveLogarithm,
            resistanceExperienceIntense = resistanceExperienceIntense ?: current.resistanceExperienceIntense,
            quickCableAdjustment = quickCableAdjustment ?: current.quickCableAdjustment,
            damperLevelIndex = damperLevelIndex ?: current.damperLevelIndex,
            assistModeEnabled = assistModeEnabled ?: current.assistModeEnabled,
            weightTrainingExtraMode = weightTrainingExtraMode ?: current.weightTrainingExtraMode,
            chainsWeightLb = chainsWeightLb ?: current.chainsWeightLb,
            eccentricWeightLb = eccentricWeightLb ?: current.eccentricWeightLb,
            inverseChains = inverseChains ?: current.inverseChains,
            isokineticMode = isokineticMode ?: current.isokineticMode,
            isokineticTargetSpeedMmS = isokineticTargetSpeedMmS ?: current.isokineticTargetSpeedMmS,
            isokineticSpeedLimitMmS = isokineticSpeedLimitMmS ?: current.isokineticSpeedLimitMmS,
            isokineticConstantResistanceLb = isokineticConstantResistanceLb ?: current.isokineticConstantResistanceLb,
            isokineticMaxEccentricLoadLb = isokineticMaxEccentricLoadLb ?: current.isokineticMaxEccentricLoadLb,
            isometricMaxForceLb = isometricMaxForceLb ?: current.isometricMaxForceLb,
            isometricMaxDurationSeconds = isometricMaxDurationSeconds ?: current.isometricMaxDurationSeconds,
            isometricCurrentForceN = when {
                leavingIsometric -> null
                isometricTelemetry != null -> isometricTelemetry.currentForceN
                else -> current.isometricCurrentForceN
            },
            isometricPeakForceN = when {
                leavingIsometric -> null
                isometricTelemetry != null -> isometricTelemetry.peakForceN
                else -> current.isometricPeakForceN
            },
            isometricElapsedMillis = when {
                leavingIsometric -> null
                isometricTelemetry != null -> isometricTelemetry.elapsedMillis
                else -> current.isometricElapsedMillis
            },
            isometricTelemetryTick = when {
                leavingIsometric -> null
                isometricTelemetry != null -> isometricTelemetry.tick
                else -> current.isometricTelemetryTick
            },
            isometricTelemetryStartTick = when {
                leavingIsometric -> null
                isometricTelemetry != null -> isometricTelemetry.startTick
                else -> current.isometricTelemetryStartTick
            },
            setCount = repTelemetry?.setCount ?: current.setCount,
            repCount = repTelemetry?.count ?: current.repCount,
            repPhase = repTelemetry?.phase ?: current.repPhase,
            workoutMode = workoutMode ?: current.workoutMode,
            lastUpdatedMillis = nowMillis,
        )
    }

    fun mergeSafety(
        current: VoltraSafetyState,
        reading: VoltraReading,
        value: ByteArray,
    ): VoltraSafetyState {
        val packet = VoltraPacketParser.parse(value) ?: return current
        val params = packet.decodeParams()
        if (params.isEmpty()) return current

        val batteryPercent = params.uint8(BMS_RSOC_PARAM_ID) ?: reading.batteryPercent
        val fitnessMode = params.uint16(PARAM_BP_SET_FITNESS_MODE) ?: current.fitnessMode
        val workoutState = params.uint8(PARAM_FITNESS_WORKOUT_STATE) ?: current.workoutState
        val targetLoadLb = params.uint16(PARAM_BP_BASE_WEIGHT)?.toDouble() ?: current.targetLoadLb
        val inResistanceBand = workoutState == VoltraControlFrames.WORKOUT_STATE_RESISTANCE_BAND
        val workoutSessionActive = workoutState?.let(::isWorkoutSessionActive) == true
        val lowBattery = batteryPercent?.let { it < LOW_BATTERY_THRESHOLD_PERCENT } ?: current.lowBattery
        val parsedDeviceState = fitnessMode != null && workoutState != null

        val reasons = buildList {
            if (lowBattery == true) add("Battery is below $LOW_BATTERY_THRESHOLD_PERCENT%.")
            when (reading.activationState) {
                "Activated" -> Unit
                "Not activated" -> add("VOLTRA is not activated.")
                else -> add("Activation state unknown.")
            }
            when (fitnessMode) {
                null -> add("Fitness mode unknown.")
                else -> when {
                    VoltraControlFrames.isLoadEngagedForWorkoutState(fitnessMode, workoutState) -> add("VOLTRA appears loaded; unload before loading again.")
                    VoltraControlFrames.isReadyForWorkoutState(fitnessMode, workoutState) -> Unit
                    else -> add("Current mode is not ready for load (mode=$fitnessMode).")
                }
            }
            when (workoutState) {
                null -> add("Workout state unknown.")
                else -> if (!workoutSessionActive) {
                    add("Workout session is inactive. Choose a mode first.")
                }
            }
            if (inResistanceBand) {
                when (val bandForce = reading.resistanceBandMaxForceLb) {
                    null -> add("Resistance Band force is unknown.")
                    else -> {
                        if (bandForce < VoltraControlFrames.MIN_RESISTANCE_BAND_FORCE_LB) add("Resistance Band force is below ${VoltraControlFrames.MIN_RESISTANCE_BAND_FORCE_LB} lb.")
                        if (bandForce > VoltraControlFrames.MAX_RESISTANCE_BAND_FORCE_LB) add("Resistance Band force is above ${VoltraControlFrames.MAX_RESISTANCE_BAND_FORCE_LB} lb.")
                    }
                }
            } else {
                when {
                    targetLoadLb == null -> add("Target load is not set on the VOLTRA.")
                    targetLoadLb < VoltraControlFrames.MIN_TARGET_LB -> add("Target load is below ${VoltraControlFrames.MIN_TARGET_LB} lb.")
                    targetLoadLb > VoltraControlFrames.MAX_TARGET_LB -> add("Target load is above ${VoltraControlFrames.MAX_TARGET_LB} lb.")
                }
            }
            if (current.locked == true) add("VOLTRA lock is active.")
            if (current.childLocked == true) add("Child lock is active.")
            if (current.activeOta == true) add("OTA/update state is active.")
        }

        return current.copy(
            canLoad = reasons.isEmpty(),
            reasons = if (reasons.isEmpty()) listOf("Ready for current mode load.") else reasons,
            lowBattery = lowBattery,
            parsedDeviceState = parsedDeviceState,
            workoutState = workoutState,
            fitnessMode = fitnessMode,
            targetLoadLb = targetLoadLb,
        )
    }

    // cmd 0x74 is a common-state response, but the apparent 0x32 "50%" byte in
    // early captures is the first CRC byte, not payload. Keep battery unknown
    // until a payload field is validated from hardware.
    private fun parseBattery(
        packet: ParsedVoltraPacket,
        params: Map<Int, Number>,
    ): Int? {
        if (packet.commandId == CMD_ASYNC_STATE || packet.commandId == CMD_BULK_REGISTER) {
            return listOf(BMS_RSOC_PARAM_ID, BMS_RSOC_LEGACY_PARAM_ID)
                .firstNotNullOfOrNull { paramId -> params.uint8(paramId) }
                ?.takeIf { it in 0..100 }
        }
        if (packet.commandId != CMD_COMMON_STATE) return null
        return null
    }

    // cmd 0xAB response: payload[0]=return code (0=success), payload[1]=activated flag (1=yes).
    private fun parseActivationState(packet: ParsedVoltraPacket): String? {
        if (packet.commandId != CMD_ACTIVATION) return null
        val payload = packet.payload
        if (payload.size < ACTIVATION_PAYLOAD_MIN) return null
        if ((payload[0].toInt() and 0xFF) != RETURN_CODE_SUCCESS) return null
        return when (payload[1].toInt() and 0xFF) {
            1 -> "Activated"
            0 -> "Not activated"
            else -> null
        }
    }

    // M-prefix observed in hardware capture: MB10267A2509130256
    private val SERIAL_REGEX = Regex("""M?B[0-9A-Z]{10,}""")
    // BP-prefixed versions are supported for later captures; page 0 already exposes BP module markers.
    private val FIRMWARE_REGEX = Regex("""(?:EP|BP|MainControlv|MotorControl|BMS|PMU)[0-9A-Za-z.-]*\d+\.\d+""")

    private const val CMD_ASYNC_STATE = 0x10
    private const val CMD_BULK_REGISTER = 0x0F
    private const val CMD_COMMON_STATE = 0x74
    private const val CMD_TELEMETRY = 0xAA
    private const val CMD_ACTIVATION = 0xAB
    private const val CMD_ISOMETRIC_STREAM = 0xB4
    private const val BMS_RSOC_PARAM_ID = VoltraControlFrames.PARAM_BMS_RSOC
    private const val BMS_RSOC_LEGACY_PARAM_ID = VoltraControlFrames.PARAM_BMS_RSOC_LEGACY
    private const val PARAM_BP_RUNTIME_POSITION_CM = VoltraControlFrames.PARAM_BP_RUNTIME_POSITION_CM
    private const val PARAM_BP_RUNTIME_WIRE_WEIGHT_LBS = 0x3E83
    private const val PARAM_BP_BASE_WEIGHT = VoltraControlFrames.PARAM_BP_BASE_WEIGHT
    private const val PARAM_BP_CHAINS_WEIGHT = VoltraControlFrames.PARAM_BP_CHAINS_WEIGHT
    private const val PARAM_BP_ECCENTRIC_WEIGHT = VoltraControlFrames.PARAM_BP_ECCENTRIC_WEIGHT
    private const val PARAM_BP_SET_FITNESS_MODE = VoltraControlFrames.PARAM_BP_SET_FITNESS_MODE
    private const val PARAM_MC_DEFAULT_OFFLEN_CM = VoltraControlFrames.PARAM_MC_DEFAULT_OFFLEN_CM
    private const val PARAM_FITNESS_WORKOUT_STATE = VoltraControlFrames.PARAM_FITNESS_WORKOUT_STATE
    private const val PARAM_FITNESS_DAMPER_RATIO_IDX = VoltraControlFrames.PARAM_FITNESS_DAMPER_RATIO_IDX
    private const val PARAM_FITNESS_ASSIST_MODE = VoltraControlFrames.PARAM_FITNESS_ASSIST_MODE
    private const val PARAM_FITNESS_INVERSE_CHAIN = VoltraControlFrames.PARAM_FITNESS_INVERSE_CHAIN
    private const val PARAM_RESISTANCE_BAND_MAX_FORCE = VoltraControlFrames.PARAM_RESISTANCE_BAND_MAX_FORCE
    private const val PARAM_RESISTANCE_BAND_LEN = VoltraControlFrames.PARAM_RESISTANCE_BAND_LEN
    private const val PARAM_RESISTANCE_BAND_LEN_BY_ROM = VoltraControlFrames.PARAM_RESISTANCE_BAND_LEN_BY_ROM
    private const val PARAM_RESISTANCE_BAND_ALGORITHM = VoltraControlFrames.PARAM_RESISTANCE_BAND_ALGORITHM
    private const val PARAM_RESISTANCE_EXPERIENCE = VoltraControlFrames.PARAM_RESISTANCE_EXPERIENCE
    private const val PARAM_EP_RESISTANCE_BAND_INVERSE = VoltraControlFrames.PARAM_EP_RESISTANCE_BAND_INVERSE
    private const val PARAM_QUICK_CABLE_ADJUSTMENT = VoltraControlFrames.PARAM_QUICK_CABLE_ADJUSTMENT
    private const val PARAM_WEIGHT_TRAINING_EXTRA_MODE = VoltraControlFrames.PARAM_WEIGHT_TRAINING_EXTRA_MODE
    private const val PARAM_ISOKINETIC_ECC_MODE = VoltraControlFrames.PARAM_ISOKINETIC_ECC_MODE
    private const val PARAM_EP_ISOKINETIC_TARGET_SPEED_MM_S = VoltraControlFrames.PARAM_EP_ISOKINETIC_TARGET_SPEED_MM_S
    private const val PARAM_ISOKINETIC_ECC_SPEED_LIMIT = VoltraControlFrames.PARAM_ISOKINETIC_ECC_SPEED_LIMIT
    private const val PARAM_ISOKINETIC_ECC_CONST_WEIGHT = VoltraControlFrames.PARAM_ISOKINETIC_ECC_CONST_WEIGHT
    private const val PARAM_ISOKINETIC_ECC_OVERLOAD_WEIGHT = VoltraControlFrames.PARAM_ISOKINETIC_ECC_OVERLOAD_WEIGHT
    private const val PARAM_ISOMETRIC_MAX_DURATION = VoltraControlFrames.PARAM_ISOMETRIC_MAX_DURATION
    private const val PARAM_ISOMETRIC_MAX_FORCE = VoltraControlFrames.PARAM_ISOMETRIC_MAX_FORCE
    private const val BULK_RESPONSE_MIN_PARAM_BYTES = 5
    private const val ACTIVATION_PAYLOAD_MIN = 2
    private const val RETURN_CODE_SUCCESS = 0x00
    private const val LOW_BATTERY_THRESHOLD_PERCENT = 15
    private const val TELEMETRY_REP_TYPE = 0x81
    private const val TELEMETRY_REP_LENGTH_MARKER = 0x2B
    private const val TELEMETRY_REP_PHASE_OFFSET = 2
    private const val TELEMETRY_SET_COUNT_OFFSET = 3
    private const val TELEMETRY_REP_COUNT_OFFSET = 4
    private const val TELEMETRY_REP_MIN_BYTES = 6
    private const val TELEMETRY_ISOMETRIC_MIN_BYTES = 45
    private const val TELEMETRY_ISOMETRIC_STATUS_PRIMARY_OFFSET = 11
    private const val TELEMETRY_ISOMETRIC_STATUS_SECONDARY_OFFSET = 13
    private const val TELEMETRY_ISOMETRIC_TICK_OFFSET = 27
    private const val TELEMETRY_ISOMETRIC_FORCE_OFFSET = 43
    private const val TELEMETRY_ISOMETRIC_ACTIVE_MARKER = 2
    private const val TELEMETRY_ISOMETRIC_READY_MARKER = 4
    private const val TELEMETRY_ISOMETRIC_ARMED_MARKER = 10
    private const val MAX_REASONABLE_SET_COUNT = 1_000
    private const val MAX_REASONABLE_REP_COUNT = 10_000
    private const val ISOMETRIC_SAMPLE_RATE_MIN = 40
    private const val ISOMETRIC_SAMPLE_RATE_MAX = 60
    private const val MAX_REASONABLE_ISOMETRIC_FORCE_LB = 220
    private const val LB_TO_NEWTONS = 4.4482216152605
    private val ISOMETRIC_STREAM_VARIANTS = setOf(1, 2)

    private fun ParsedVoltraPacket.decodeParams(): Map<Int, Number> {
        if (commandId != CMD_ASYNC_STATE && commandId != CMD_BULK_REGISTER) return emptyMap()
        val packetPayload = this.payload
        val paramStart = paramListStartOffset(packetPayload)
        if (packetPayload.size < paramStart + PARAM_UPDATE_HEADER_BYTES) return emptyMap()
        val count = packetPayload[paramStart].u8() or (packetPayload[paramStart + 1].u8() shl 8)
        var offset = paramStart + PARAM_UPDATE_HEADER_BYTES
        val values = mutableMapOf<Int, Number>()
        repeat(count) {
            if (offset + PARAM_ID_BYTES > packetPayload.size) return values
            val paramId = packetPayload[offset].u8() or (packetPayload[offset + 1].u8() shl 8)
            offset += PARAM_ID_BYTES
            val definition = VoltraParamRegistry.byId[paramId] ?: return values
            if (definition.length <= 0 || offset + definition.length > packetPayload.size) return values
            val valueBytes = packetPayload.copyOfRange(offset, offset + definition.length)
            runCatching {
                VoltraValueCodec.decodeLittleEndian(definition.valueType, valueBytes)
            }.getOrNull()?.let { decoded ->
                values[paramId] = decoded
            }
            offset += definition.length
        }
        return values
    }

    private fun ParsedVoltraPacket.paramListStartOffset(packetPayload: ByteArray): Int {
        if (commandId != CMD_BULK_REGISTER) return 0
        if (packetPayload.size < BULK_RESPONSE_MIN_PARAM_BYTES) return 0
        if (packetPayload[0].u8() != RETURN_CODE_SUCCESS) return 0
        val possibleFirstParamId = packetPayload[3].u8() or (packetPayload[4].u8() shl 8)
        return if (possibleFirstParamId in VoltraParamRegistry.byId) 1 else 0
    }

    private fun workoutModeLabel(mode: Int?, workoutState: Int?): String? {
        if (mode == null && workoutState == null) return null
        val normalizedMode = VoltraControlFrames.normalizedFitnessMode(mode)
        val stateLabel = when (workoutState) {
            VoltraControlFrames.WORKOUT_STATE_RESISTANCE_BAND -> "Resistance Band"
            VoltraControlFrames.WORKOUT_STATE_DAMPER -> "Damper"
            VoltraControlFrames.WORKOUT_STATE_ISOKINETIC -> "Isokinetic"
            VoltraControlFrames.WORKOUT_STATE_ISOMETRIC -> "Isometric Test"
            else -> null
        }
        if (stateLabel != null) {
            val resistanceModeText = when {
                workoutState == VoltraControlFrames.WORKOUT_STATE_ISOMETRIC &&
                    VoltraControlFrames.isLoadEngagedForWorkoutState(mode, workoutState) -> "Loaded"
                VoltraControlFrames.isReadyForWorkoutState(mode, workoutState) -> "Ready"
                VoltraControlFrames.isLoadEngagedForWorkoutState(mode, workoutState) -> "Loaded"
                mode == null -> "state unknown"
                else -> "mode $mode"
            }
            return "$stateLabel, $resistanceModeText"
        }
        val modeText = when (normalizedMode) {
            VoltraControlFrames.FITNESS_MODE_STRENGTH_READY -> "Strength ready"
            VoltraControlFrames.FITNESS_MODE_STRENGTH_LOADED -> "Strength loaded"
            null -> "Unknown mode"
            else -> "Fitness mode $mode"
        }
        val stateText = when (workoutState) {
            VoltraControlFrames.WORKOUT_STATE_INACTIVE -> "session inactive"
            VoltraControlFrames.WORKOUT_STATE_ACTIVE -> "session active"
            null -> "state unknown"
            else -> if (isWorkoutSessionActive(workoutState)) {
                "session active (state $workoutState)"
            } else {
                "state $workoutState"
            }
        }
        return "$modeText, $stateText"
    }

    private fun isWorkoutSessionActive(workoutState: Int): Boolean {
        return workoutState != VoltraControlFrames.WORKOUT_STATE_INACTIVE
    }

    private fun parseRepTelemetry(packet: ParsedVoltraPacket): RepTelemetry? {
        if (packet.commandId != CMD_TELEMETRY) return null
        val payload = packet.payload
        if (payload.size < TELEMETRY_REP_MIN_BYTES) return null
        if (payload[0].u8() != TELEMETRY_REP_TYPE || payload[1].u8() != TELEMETRY_REP_LENGTH_MARKER) return null
        val setCount = payload[TELEMETRY_SET_COUNT_OFFSET].u8()
        val count = payload.u16be(TELEMETRY_REP_COUNT_OFFSET)
        if (setCount !in 0..MAX_REASONABLE_SET_COUNT) return null
        if (count !in 0..MAX_REASONABLE_REP_COUNT) return null
        return RepTelemetry(
            setCount = setCount,
            count = count,
            phase = repPhaseLabel(payload[TELEMETRY_REP_PHASE_OFFSET].u8()),
        )
    }

    private fun parseIsometricTelemetry(
        packet: ParsedVoltraPacket,
        current: VoltraReading,
        nowMillis: Long,
    ): IsometricTelemetry? {
        return parseLegacyIsometricTelemetry(packet, current)
            ?: parseB4IsometricTelemetry(packet, current, nowMillis)
    }

    private fun parseLegacyIsometricTelemetry(
        packet: ParsedVoltraPacket,
        current: VoltraReading,
    ): IsometricTelemetry? {
        if (packet.commandId != CMD_TELEMETRY) return null
        val payload = packet.payload
        if (payload.size < TELEMETRY_ISOMETRIC_MIN_BYTES) return null
        if (payload[0].u8() != TELEMETRY_REP_TYPE || payload[1].u8() != TELEMETRY_REP_LENGTH_MARKER) return null

        val prefixLooksIsometric = payload.copyOfRange(2, 11).all { it == 0.toByte() }
        val suffixLooksIsometric = payload.copyOfRange(31, 43).all { it == 0.toByte() }
        if (!prefixLooksIsometric || !suffixLooksIsometric) return null

        val tick = payload.u32le(TELEMETRY_ISOMETRIC_TICK_OFFSET).toLong()
        val statusPrimary = payload.u16le(TELEMETRY_ISOMETRIC_STATUS_PRIMARY_OFFSET)
        val statusSecondary = payload.u16le(TELEMETRY_ISOMETRIC_STATUS_SECONDARY_OFFSET)
        val activeFrame = statusSecondary == TELEMETRY_ISOMETRIC_ACTIVE_MARKER && statusPrimary in 0..6

        if (!activeFrame) {
            return when (statusSecondary) {
                TELEMETRY_ISOMETRIC_READY_MARKER,
                TELEMETRY_ISOMETRIC_ARMED_MARKER,
                -> IsometricTelemetry(
                    currentForceN = null,
                    peakForceN = current.isometricPeakForceN,
                    elapsedMillis = current.isometricElapsedMillis,
                    tick = tick,
                    startTick = null,
                )

                else -> null
            }
        }

        val currentForceN = payload.u16le(TELEMETRY_ISOMETRIC_FORCE_OFFSET) / 10.0
        val startingNewAttempt = current.isometricCurrentForceN == null || current.isometricTelemetryStartTick == null
        val startTick = if (startingNewAttempt) tick else current.isometricTelemetryStartTick ?: tick
        val elapsedMillis = (tick - startTick).coerceAtLeast(0L)
        val peakForceN = if (startingNewAttempt) {
            currentForceN
        } else {
            maxOf(current.isometricPeakForceN ?: currentForceN, currentForceN)
        }

        return IsometricTelemetry(
            currentForceN = currentForceN,
            peakForceN = peakForceN,
            elapsedMillis = elapsedMillis,
            tick = tick,
            startTick = startTick,
        )
    }

    private fun parseB4IsometricTelemetry(
        packet: ParsedVoltraPacket,
        current: VoltraReading,
        nowMillis: Long,
    ): IsometricTelemetry? {
        if (packet.commandId != CMD_ISOMETRIC_STREAM) return null
        val payload = packet.payload
        if (payload.size != 8) return null
        if (payload.u16le(2) !in ISOMETRIC_STREAM_VARIANTS) return null
        if (payload.u16le(6) !in ISOMETRIC_SAMPLE_RATE_MIN..ISOMETRIC_SAMPLE_RATE_MAX) return null

        val currentForceLb = payload.u16le(0).toDouble()
        if (currentForceLb !in 0.0..MAX_REASONABLE_ISOMETRIC_FORCE_LB.toDouble()) return null

        val currentForceN = currentForceLb * LB_TO_NEWTONS
        val startingNewAttempt = current.isometricCurrentForceN == null || current.isometricTelemetryStartTick == null
        val startTick = if (startingNewAttempt) nowMillis else current.isometricTelemetryStartTick ?: nowMillis
        val elapsedMillis = (nowMillis - startTick).coerceAtLeast(0L)
        val peakForceN = if (startingNewAttempt) {
            currentForceN
        } else {
            maxOf(current.isometricPeakForceN ?: currentForceN, currentForceN)
        }

        return IsometricTelemetry(
            currentForceN = currentForceN,
            peakForceN = peakForceN,
            elapsedMillis = elapsedMillis,
            tick = nowMillis,
            startTick = startTick,
        )
    }

    private fun repPhaseLabel(phase: Int): String = when (phase) {
        0 -> "Idle"
        1 -> "Pull"
        2 -> "Transition"
        3 -> "Return"
        else -> "Phase $phase"
    }

    private fun Map<Int, Number>.uint8(paramId: Int): Int? {
        return this[paramId]?.toInt()?.takeIf { it in 0..0xFF }
    }

    private fun Map<Int, Number>.uint16(paramId: Int): Int? {
        return this[paramId]?.toInt()?.takeIf { it in 0..0xFFFF }
    }

    private fun Map<Int, Number>.uint32(paramId: Int): Int? {
        return this[paramId]?.toLong()?.takeIf { it in 0L..0xFFFF_FFFFL }?.toInt()
    }

    private fun Map<Int, Number>.int16(paramId: Int): Int? {
        return this[paramId]?.toInt()?.takeIf { it in Short.MIN_VALUE..Short.MAX_VALUE }
    }

    private fun Map<Int, Number>.signedInt16FromStoredUint16(paramId: Int): Int? {
        return this[paramId]
            ?.toInt()
            ?.takeIf { it in 0..0xFFFF }
            ?.let { value -> if (value > Short.MAX_VALUE) value - 0x10000 else value }
    }

    private fun ByteArray.u16be(offset: Int): Int {
        if (offset + 1 >= size) return -1
        return (this[offset].u8() shl 8) or this[offset + 1].u8()
    }

    private fun ByteArray.u16le(offset: Int): Int {
        if (offset + 1 >= size) return -1
        return this[offset].u8() or (this[offset + 1].u8() shl 8)
    }

    private fun ByteArray.u32le(offset: Int): Int {
        if (offset + 3 >= size) return -1
        return this[offset].u8() or
            (this[offset + 1].u8() shl 8) or
            (this[offset + 2].u8() shl 16) or
            (this[offset + 3].u8() shl 24)
    }

    private fun ByteArray.printableAsciiSegments(): List<String> {
        val segments = mutableListOf<String>()
        val current = StringBuilder()
        fun flush() {
            if (current.length >= MIN_PRINTABLE_SEGMENT_CHARS) {
                segments += current.toString()
            }
            current.clear()
        }
        forEach { byte ->
            val value = byte.u8()
            if (value in PRINTABLE_ASCII_RANGE) {
                current.append(value.toChar())
            } else {
                flush()
            }
        }
        flush()
        return segments
    }

    private fun String?.mergeFirmwareParts(newParts: List<String>): String? {
        val merged = buildList {
            this@mergeFirmwareParts
                ?.split(" / ")
                ?.filterTo(this) { it.isNotBlank() }
            newParts.filterTo(this) { it.isNotBlank() }
        }.distinct()
        return merged.takeIf { it.isNotEmpty() }?.joinToString(" / ")
    }

    private fun Byte.u8(): Int = toInt() and 0xFF

    private const val PARAM_UPDATE_HEADER_BYTES = 2
    private const val PARAM_ID_BYTES = 2
    private const val MIN_PRINTABLE_SEGMENT_CHARS = 2
    private val PRINTABLE_ASCII_RANGE = 32..126

    private data class RepTelemetry(
        val setCount: Int,
        val count: Int,
        val phase: String,
    )

    private data class IsometricTelemetry(
        val currentForceN: Double?,
        val peakForceN: Double?,
        val elapsedMillis: Long?,
        val tick: Long,
        val startTick: Long?,
    )
}
