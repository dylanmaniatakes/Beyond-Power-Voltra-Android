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
        val appCurrentScreenId = params.uint8(PARAM_APP_CUR_SCR_ID)
        val fitnessOngoingUi = params.uint16(PARAM_FITNESS_ONGOING_UI)
        val isokineticMode = params.uint8(PARAM_ISOKINETIC_ECC_MODE)
        val isokineticTargetSpeedMmS = params.uint32(PARAM_EP_ISOKINETIC_TARGET_SPEED_MM_S)
        val isokineticSpeedLimitMmS = params.uint16(PARAM_ISOKINETIC_ECC_SPEED_LIMIT)
        val isokineticConstantResistanceLb = params.uint16(PARAM_ISOKINETIC_ECC_CONST_WEIGHT)?.toDouble()
        val isokineticMaxEccentricLoadLb = params.uint16(PARAM_ISOKINETIC_ECC_OVERLOAD_WEIGHT)?.toDouble()
        val isometricMaxForceLb = params.uint16(PARAM_ISOMETRIC_MAX_FORCE)?.toDouble()
        val isometricMaxDurationSeconds = params.uint16(PARAM_ISOMETRIC_MAX_DURATION)
        val isometricMetricsType = params.uint8(PARAM_ISOMETRIC_METRICS_TYPE)
        val isometricBodyWeightN = params.uint32(PARAM_EP_ISOMETRIC_TESTING_BODY_WEIGHT_N)?.toDouble()
        val isometricBodyWeight100g = params.uint16(PARAM_EP_ISOMETRIC_TESTING_BODY_WEIGHT_100G)
        val isometricBodyWeightLb = params.uint16(PARAM_EP_ISOMETRIC_TESTING_BODY_WEIGHT_LBS)?.toDouble()
        val fitnessMode = params.uint16(PARAM_BP_SET_FITNESS_MODE)
        val workoutState = params.uint8(PARAM_FITNESS_WORKOUT_STATE)
        val currentWasInIsometric = current.workoutMode?.startsWith("Isometric Test") == true
        val currentWasInCustomCurve = current.workoutMode?.startsWith("Custom Curve") == true
        val currentModeIsKnownNonIsometric = current.workoutMode != null && !currentWasInIsometric
        val packetIsIsometric = workoutState == VoltraControlFrames.WORKOUT_STATE_ISOMETRIC ||
            (workoutState == null && !currentModeIsKnownNonIsometric)
        val packetIsCustomCurve = workoutState == VoltraControlFrames.WORKOUT_STATE_CUSTOM_CURVE ||
            (workoutState == null && currentWasInCustomCurve)
        val isometricTelemetry = if (packetIsIsometric) {
            parseIsometricTelemetry(packet, current, nowMillis)
        } else {
            null
        }
        val isometricWaveform = if (packetIsIsometric) {
            parseIsometricWaveform(packet, current)
        } else {
            null
        }
        val workoutMode = workoutModeLabel(
            mode = fitnessMode,
            workoutState = workoutState,
        )
        val repTelemetry = parseRepTelemetry(packet)
        val customCurveTelemetry = if (packetIsCustomCurve) {
            parseCustomCurveTelemetry(packet, current)
        } else {
            null
        }

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
            appCurrentScreenId == null &&
            fitnessOngoingUi == null &&
            isokineticMode == null &&
            isokineticTargetSpeedMmS == null &&
            isokineticSpeedLimitMmS == null &&
            isokineticConstantResistanceLb == null &&
            isokineticMaxEccentricLoadLb == null &&
            isometricMaxForceLb == null &&
            isometricMaxDurationSeconds == null &&
            isometricMetricsType == null &&
            isometricBodyWeightN == null &&
            isometricBodyWeight100g == null &&
            isometricBodyWeightLb == null &&
            isometricTelemetry == null &&
            isometricWaveform == null &&
            workoutMode == null &&
            repTelemetry == null &&
            customCurveTelemetry == null
        ) {
            return current
        }

        val leavingIsometric = workoutState != null && workoutState != VoltraControlFrames.WORKOUT_STATE_ISOMETRIC
        val retainCompletedIsometricAttempt = current.isometricWaveformSamplesN.isNotEmpty() ||
            current.isometricPeakRelativeForcePercent != null
        val hasCollectedIsometricLiveSample = current.isometricWaveformSamplesN.isNotEmpty() ||
            current.isometricPeakRelativeForcePercent != null ||
            current.isometricTelemetryStartTick != null
        val enteringFreshIsometricScreen =
            workoutState == VoltraControlFrames.WORKOUT_STATE_ISOMETRIC &&
                !currentWasInIsometric &&
                current.isometricCurrentForceN == null &&
                current.isometricTelemetryStartTick == null &&
                current.isometricWaveformSamplesN.isEmpty() &&
                isometricTelemetry == null &&
                isometricWaveform == null
        val readyIsometricWithoutTelemetry =
            workoutState == VoltraControlFrames.WORKOUT_STATE_ISOMETRIC &&
                VoltraControlFrames.isReadyForWorkoutState(fitnessMode, workoutState) &&
                isometricTelemetry == null &&
                !hasCollectedIsometricLiveSample
        val completedLegacyIsometricAttempt =
            isometricTelemetry?.currentForceN == null &&
                isometricTelemetry?.carrierStatusSecondary == TELEMETRY_ISOMETRIC_COMPLETED_MARKER
        val currentPeakForceForSummaryReconciliation = current.isometricPeakForceN
        val summaryPeakForceForReconciliation = isometricTelemetry?.peakForceN
        val shouldRescaleSparseWaveformToSummary = if (
            isometricTelemetry?.peakRelativeForcePercent != null &&
            isometricTelemetry.currentForceN == null &&
            current.isometricCurrentForceN == null &&
            currentPeakForceForSummaryReconciliation != null &&
            summaryPeakForceForReconciliation != null &&
            current.isometricWaveformSamplesN.size in 1..MAX_SUMMARY_RECONCILIATION_SPARSE_SAMPLES
        ) {
            currentPeakForceForSummaryReconciliation >
                summaryPeakForceForReconciliation + STALE_ISOMETRIC_SUMMARY_FORCE_TOLERANCE_N
        } else {
            false
        }
        val rescaledSparseWaveformSamples = if (shouldRescaleSparseWaveformToSummary) {
            val currentPeak = current.isometricPeakForceN ?: 0.0
            val summaryPeak = isometricTelemetry?.peakForceN ?: 0.0
            if (currentPeak > 0.0 && summaryPeak > 0.0) {
                val scale = summaryPeak / currentPeak
                current.isometricWaveformSamplesN.map { (it * scale).coerceAtLeast(0.0) }
            } else {
                emptyList()
            }
        } else {
            current.isometricWaveformSamplesN
        }
        val isometricWaveformSamples = when {
            enteringFreshIsometricScreen -> emptyList()
            leavingIsometric && retainCompletedIsometricAttempt -> current.isometricWaveformSamplesN
            leavingIsometric -> emptyList()
            isometricWaveform != null -> isometricWaveform.samplesN
            isometricTelemetry?.currentForceN != null -> {
                val baseSamples = if (isometricTelemetry.startingNewAttempt) {
                    emptyList()
                } else {
                    current.isometricWaveformSamplesN
                }
                (baseSamples + isometricTelemetry.currentForceN).takeLast(MAX_ISOMETRIC_WAVEFORM_SAMPLES)
            }
            shouldRescaleSparseWaveformToSummary -> rescaledSparseWaveformSamples
            else -> current.isometricWaveformSamplesN
        }
        val isometricWaveformLastChunkIndex = when {
            enteringFreshIsometricScreen -> null
            leavingIsometric && retainCompletedIsometricAttempt -> current.isometricWaveformLastChunkIndex
            leavingIsometric -> null
            isometricWaveform != null -> isometricWaveform.lastChunkIndex
            isometricTelemetry?.startingNewAttempt == true -> null
            else -> current.isometricWaveformLastChunkIndex
        }
        val mergedIsometricMetricsType = isometricMetricsType ?: current.isometricMetricsType
        val mergedIsometricBodyWeightN = isometricBodyWeightN ?: current.isometricBodyWeightN
        val mergedIsometricBodyWeight100g = isometricBodyWeight100g ?: current.isometricBodyWeight100g
        val mergedIsometricBodyWeightLb = isometricBodyWeightLb ?: current.isometricBodyWeightLb
        val mergedIsometricPeakForceN = when {
            enteringFreshIsometricScreen -> null
            leavingIsometric && retainCompletedIsometricAttempt -> current.isometricPeakForceN
            leavingIsometric -> null
            isometricTelemetry?.peakForceN != null -> isometricTelemetry.peakForceN
            isometricTelemetry != null -> if (hasCollectedIsometricLiveSample) current.isometricPeakForceN else null
            else -> current.isometricPeakForceN
        }
        val mergedIsometricPeakRelativeForcePercent = when {
            enteringFreshIsometricScreen -> null
            leavingIsometric && retainCompletedIsometricAttempt -> current.isometricPeakRelativeForcePercent
            leavingIsometric -> null
            isometricTelemetry != null -> when {
                isometricTelemetry.startingNewAttempt -> isometricTelemetry.peakRelativeForcePercent
                isometricTelemetry.peakRelativeForcePercent != null -> isometricTelemetry.peakRelativeForcePercent
                else -> current.isometricPeakRelativeForcePercent
            }
            else -> current.isometricPeakRelativeForcePercent
        } ?: deriveIsometricPeakRelativeForcePercent(
            peakForceN = mergedIsometricPeakForceN,
            bodyWeightN = mergedIsometricBodyWeightN,
            metricsType = mergedIsometricMetricsType,
        )

        return current.copy(
            serialNumber = serial ?: current.serialNumber,
            firmwareVersion = current.firmwareVersion.mergeFirmwareParts(firmwareParts),
            batteryPercent = batteryPct ?: current.batteryPercent,
            activationState = activationState ?: current.activationState,
            cableLengthCm = cableLengthCm ?: current.cableLengthCm,
            cableOffsetCm = cableOffsetCm ?: current.cableOffsetCm,
            forceLb = customCurveTelemetry?.forceLb ?: wireWeightLb ?: current.forceLb,
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
            appCurrentScreenId = appCurrentScreenId ?: current.appCurrentScreenId,
            fitnessOngoingUi = fitnessOngoingUi ?: current.fitnessOngoingUi,
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
            isometricMetricsType = mergedIsometricMetricsType,
            isometricBodyWeightN = mergedIsometricBodyWeightN,
            isometricBodyWeight100g = mergedIsometricBodyWeight100g,
            isometricBodyWeightLb = mergedIsometricBodyWeightLb,
            isometricCurrentForceN = when {
                enteringFreshIsometricScreen -> null
                leavingIsometric -> null
                readyIsometricWithoutTelemetry -> null
                completedLegacyIsometricAttempt -> null
                isometricTelemetry?.currentForceN != null -> isometricTelemetry.currentForceN
                isometricTelemetry != null -> if (hasCollectedIsometricLiveSample) current.isometricCurrentForceN else null
                else -> current.isometricCurrentForceN
            },
            isometricPeakForceN = mergedIsometricPeakForceN,
            isometricPeakRelativeForcePercent = mergedIsometricPeakRelativeForcePercent,
            isometricElapsedMillis = when {
                enteringFreshIsometricScreen -> null
                leavingIsometric && retainCompletedIsometricAttempt -> current.isometricElapsedMillis
                leavingIsometric -> null
                isometricTelemetry?.elapsedMillis != null -> isometricTelemetry.elapsedMillis
                isometricTelemetry != null -> if (hasCollectedIsometricLiveSample) current.isometricElapsedMillis else null
                else -> current.isometricElapsedMillis
            },
            isometricTelemetryTick = when {
                enteringFreshIsometricScreen -> null
                leavingIsometric -> null
                isometricTelemetry != null -> isometricTelemetry.tick
                else -> current.isometricTelemetryTick
            },
            isometricTelemetryStartTick = when {
                enteringFreshIsometricScreen -> null
                leavingIsometric -> null
                isometricTelemetry != null -> isometricTelemetry.startTick
                else -> current.isometricTelemetryStartTick
            },
            isometricCarrierForceN = when {
                enteringFreshIsometricScreen -> null
                leavingIsometric -> null
                isometricTelemetry != null -> isometricTelemetry.rawCarrierForceN
                else -> current.isometricCarrierForceN
            },
            isometricCarrierStatusPrimary = when {
                enteringFreshIsometricScreen -> null
                leavingIsometric -> null
                isometricTelemetry != null -> isometricTelemetry.carrierStatusPrimary
                else -> current.isometricCarrierStatusPrimary
            },
            isometricCarrierStatusSecondary = when {
                enteringFreshIsometricScreen -> null
                leavingIsometric -> null
                isometricTelemetry != null -> isometricTelemetry.carrierStatusSecondary
                else -> current.isometricCarrierStatusSecondary
            },
            isometricWaveformSamplesN = isometricWaveformSamples,
            isometricWaveformLastChunkIndex = isometricWaveformLastChunkIndex,
            setCount = customCurveTelemetry?.setCount ?: repTelemetry?.setCount ?: current.setCount,
            repCount = customCurveTelemetry?.repCount ?: repTelemetry?.count ?: current.repCount,
            repPhase = customCurveTelemetry?.phase ?: repTelemetry?.phase ?: current.repPhase,
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
    private const val PARAM_APP_CUR_SCR_ID = VoltraControlFrames.PARAM_APP_CUR_SCR_ID
    private const val PARAM_FITNESS_ONGOING_UI = VoltraControlFrames.PARAM_FITNESS_ONGOING_UI
    private const val PARAM_FITNESS_INVERSE_CHAIN = VoltraControlFrames.PARAM_FITNESS_INVERSE_CHAIN
    private const val PARAM_RESISTANCE_BAND_MAX_FORCE = VoltraControlFrames.PARAM_RESISTANCE_BAND_MAX_FORCE
    private const val PARAM_RESISTANCE_BAND_LEN = VoltraControlFrames.PARAM_RESISTANCE_BAND_LEN
    private const val PARAM_RESISTANCE_BAND_LEN_BY_ROM = VoltraControlFrames.PARAM_RESISTANCE_BAND_LEN_BY_ROM
    private const val PARAM_RESISTANCE_BAND_ALGORITHM = VoltraControlFrames.PARAM_RESISTANCE_BAND_ALGORITHM
    private const val PARAM_RESISTANCE_EXPERIENCE = VoltraControlFrames.PARAM_RESISTANCE_EXPERIENCE
    private const val PARAM_EP_RESISTANCE_BAND_INVERSE = VoltraControlFrames.PARAM_EP_RESISTANCE_BAND_INVERSE
    private const val PARAM_QUICK_CABLE_ADJUSTMENT = VoltraControlFrames.PARAM_QUICK_CABLE_ADJUSTMENT
    private const val PARAM_WEIGHT_TRAINING_EXTRA_MODE = VoltraControlFrames.PARAM_WEIGHT_TRAINING_EXTRA_MODE
    private const val PARAM_ISOMETRIC_METRICS_TYPE = VoltraControlFrames.PARAM_ISOMETRIC_METRICS_TYPE
    private const val PARAM_ISOKINETIC_ECC_MODE = VoltraControlFrames.PARAM_ISOKINETIC_ECC_MODE
    private const val PARAM_EP_ISOKINETIC_TARGET_SPEED_MM_S = VoltraControlFrames.PARAM_EP_ISOKINETIC_TARGET_SPEED_MM_S
    private const val PARAM_ISOKINETIC_ECC_SPEED_LIMIT = VoltraControlFrames.PARAM_ISOKINETIC_ECC_SPEED_LIMIT
    private const val PARAM_ISOKINETIC_ECC_CONST_WEIGHT = VoltraControlFrames.PARAM_ISOKINETIC_ECC_CONST_WEIGHT
    private const val PARAM_ISOKINETIC_ECC_OVERLOAD_WEIGHT = VoltraControlFrames.PARAM_ISOKINETIC_ECC_OVERLOAD_WEIGHT
    private const val PARAM_EP_ISOMETRIC_TESTING_BODY_WEIGHT_N = VoltraControlFrames.PARAM_EP_ISOMETRIC_TESTING_BODY_WEIGHT_N
    private const val PARAM_EP_ISOMETRIC_TESTING_BODY_WEIGHT_100G = VoltraControlFrames.PARAM_EP_ISOMETRIC_TESTING_BODY_WEIGHT_100G
    private const val PARAM_EP_ISOMETRIC_TESTING_BODY_WEIGHT_LBS = VoltraControlFrames.PARAM_EP_ISOMETRIC_TESTING_BODY_WEIGHT_LBS
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
    private const val TELEMETRY_ISOMETRIC_SUMMARY_BYTES = 39
    private const val TELEMETRY_ISOMETRIC_STATUS_PRIMARY_OFFSET = 11
    private const val TELEMETRY_ISOMETRIC_STATUS_SECONDARY_OFFSET = 13
    private const val TELEMETRY_ISOMETRIC_TICK_OFFSET = 27
    private const val TELEMETRY_ISOMETRIC_FORCE_OFFSET = 43
    private const val TELEMETRY_ISOMETRIC_ACTIVE_MARKER = 2
    private const val TELEMETRY_ISOMETRIC_PROGRESS_MARKER = 3
    private const val TELEMETRY_ISOMETRIC_READY_MARKER = 4
    private const val TELEMETRY_ISOMETRIC_COARSE_LIVE_FORCE_MARKER = 1
    private const val TELEMETRY_ISOMETRIC_ARMED_MARKER = 10
    private const val TELEMETRY_ISOMETRIC_COMPLETED_MARKER = 11
    private const val TELEMETRY_ISOMETRIC_LIVE_FORCE_MARKER = 12
    private val TELEMETRY_ISOMETRIC_EXTENDED_LIVE_FORCE_MARKERS = 12..15
    private const val TELEMETRY_ISOMETRIC_SUMMARY_TYPE = 0x80
    private const val TELEMETRY_ISOMETRIC_SUMMARY_LENGTH_MARKER = 0x25
    private const val TELEMETRY_ISOMETRIC_SUMMARY_PEAK_FORCE_OFFSET = 23
    private const val TELEMETRY_ISOMETRIC_SUMMARY_PEAK_RELATIVE_FORCE_OFFSET = 29
    private const val TELEMETRY_ISOMETRIC_SUMMARY_DURATION_SECONDS_OFFSET = 33
    private const val TELEMETRY_ISOMETRIC_WAVEFORM_TYPE = 0x93
    private const val CUSTOM_CURVE_B4_SHORT_BYTES = 8
    private const val CUSTOM_CURVE_B4_EXTENDED_BYTES = 12
    private const val CUSTOM_CURVE_B4_FORCE_MIRROR_TOLERANCE_TENTHS_LB = 2
    private const val CUSTOM_CURVE_VENDOR_STATUS_BYTES = 39
    private const val CUSTOM_CURVE_VENDOR_FORCE_OFFSET = 5
    private const val CUSTOM_CURVE_FORCE_TENTHS_PER_LB = 10.0
    private const val CUSTOM_CURVE_REP_ACTIVE_MARGIN_LB = 3.0
    private const val CUSTOM_CURVE_REP_ACTIVE_MULTIPLIER = 1.2
    private const val CUSTOM_CURVE_REP_RESET_MARGIN_LB = 1.0
    private const val CUSTOM_CURVE_REP_DIRECTION_DEADBAND_LB = 0.4
    private const val MAX_REASONABLE_SET_COUNT = 1_000
    private const val MAX_REASONABLE_REP_COUNT = 10_000
    private const val MAX_REASONABLE_CUSTOM_CURVE_FORCE_TENTHS_LB = 2_000
    private const val ISOMETRIC_SAMPLE_RATE_MIN = 40
    private const val ISOMETRIC_SAMPLE_RATE_MAX = 60
    private const val MAX_REASONABLE_ISOMETRIC_DURATION_SECONDS = 60
    private const val MAX_REASONABLE_ISOMETRIC_RELATIVE_FORCE_TENTHS_PERCENT = 1_000
    private const val MAX_REASONABLE_ISOMETRIC_FORCE_LB = 450
    private const val MAX_REASONABLE_ISOMETRIC_FORCE_N = 2_000.0
    private const val MAX_REASONABLE_ISOMETRIC_GRAPH_FORCE_N = 2_000.0
    private const val MIN_REASONABLE_ISOMETRIC_BODY_WEIGHT_N = 100.0
    private const val MAX_REASONABLE_ISOMETRIC_BODY_WEIGHT_N = 5_000.0
    private const val ISOMETRIC_METRICS_TYPE_FORCE = 0
    private const val MAX_REASONABLE_ISOMETRIC_STATUS_WORD = 0x0200
    private const val MAX_REASONABLE_ISOMETRIC_AUX_WORD = 4_000
    private const val MAX_SUMMARY_RECONCILIATION_SPARSE_SAMPLES = 16
    private const val LEGACY_ISOMETRIC_SENTINEL_FORCE_N = 390.0
    private const val STALE_ISOMETRIC_SUMMARY_FORCE_TOLERANCE_N = 5.0
    private const val STALE_ISOMETRIC_SUMMARY_ELAPSED_TOLERANCE_MILLIS = 250L
    // The legacy AA81 Isometric live branch reports force in tenths of pounds.
    // Successful Android traces use statusSecondary 12..15 as one continuous
    // live pull window.
    private const val LEGACY_ISOMETRIC_PULL_FORCE_SCALE = 0.44482216152605
    private const val LEGACY_ISOMETRIC_COARSE_FORCE_SCALE = 1.067
    private const val ISOMETRIC_WAVEFORM_HEADER_BYTES = 6
    private const val MAX_ISOMETRIC_WAVEFORM_SAMPLES = 2_400
    private const val LB_TO_NEWTONS = 4.4482216152605
    private val LEGACY_ISOMETRIC_STREAM_VARIANTS = setOf(1, 2, 3)
    private val TELEMETRY_ISOMETRIC_COARSE_LIVE_FORCE_RANGE_N = 1.0..MAX_REASONABLE_ISOMETRIC_FORCE_N
    private val TELEMETRY_ISOMETRIC_WAVEFORM_MARKERS = setOf(0xCC, 0x82, 0xA8)
    private val CUSTOM_CURVE_ACTIVE_PHASES = setOf("Pull", "Return")

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

    private fun workoutModeLabel(
        mode: Int?,
        workoutState: Int?,
    ): String? {
        if (mode == null && workoutState == null) return null
        val normalizedMode = VoltraControlFrames.normalizedFitnessMode(mode)
        val stateLabel = when (workoutState) {
            VoltraControlFrames.WORKOUT_STATE_RESISTANCE_BAND -> "Resistance Band"
            VoltraControlFrames.WORKOUT_STATE_DAMPER -> "Damper"
            VoltraControlFrames.WORKOUT_STATE_CUSTOM_CURVE -> "Custom Curve"
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

    private fun parseCustomCurveTelemetry(
        packet: ParsedVoltraPacket,
        current: VoltraReading,
    ): CustomCurveTelemetry? {
        val forceTenthsLb = when (packet.commandId) {
            CMD_ISOMETRIC_STREAM -> parseCustomCurveB4ForceTenthsLb(packet.payload)
            CMD_TELEMETRY -> parseCustomCurveVendorForceTenthsLb(packet.payload)
            else -> null
        } ?: return null
        if (forceTenthsLb !in 0..MAX_REASONABLE_CUSTOM_CURVE_FORCE_TENTHS_LB) return null

        val forceLb = forceTenthsLb / CUSTOM_CURVE_FORCE_TENTHS_PER_LB
        val baseLb = current.weightLb
            ?.takeIf { it in 0.0..VoltraControlFrames.MAX_CUSTOM_CURVE_RESISTANCE_LIMIT_LB.toDouble() }
            ?: VoltraControlFrames.DEFAULT_CUSTOM_CURVE_RESISTANCE_MIN_LB.toDouble()
        val activeThresholdLb = maxOf(
            baseLb + CUSTOM_CURVE_REP_ACTIVE_MARGIN_LB,
            baseLb * CUSTOM_CURVE_REP_ACTIVE_MULTIPLIER,
        )
        val resetThresholdLb = baseLb + CUSTOM_CURVE_REP_RESET_MARGIN_LB
        val wasInRep = current.repPhase in CUSTOM_CURVE_ACTIVE_PHASES
        val completedRep = wasInRep && forceLb <= resetThresholdLb
        val previousForceLb = current.forceLb
        val phase = when {
            completedRep -> "Ready"
            forceLb < activeThresholdLb -> "Ready"
            previousForceLb != null &&
                forceLb < previousForceLb - CUSTOM_CURVE_REP_DIRECTION_DEADBAND_LB -> "Return"
            else -> "Pull"
        }
        val repCount = ((current.repCount ?: 0) + if (completedRep) 1 else 0)
            .coerceIn(0, MAX_REASONABLE_REP_COUNT)
        val setCount = when {
            repCount > 0 -> maxOf(current.setCount ?: 1, 1)
            current.setCount != null -> current.setCount ?: 0
            else -> 0
        }
        return CustomCurveTelemetry(
            forceLb = forceLb,
            setCount = setCount,
            repCount = repCount,
            phase = phase,
        )
    }

    private fun parseCustomCurveB4ForceTenthsLb(payload: ByteArray): Int? {
        if (payload.size != CUSTOM_CURVE_B4_SHORT_BYTES && payload.size != CUSTOM_CURVE_B4_EXTENDED_BYTES) {
            return null
        }
        val leadingForce = payload.u16le(0)
        val trailingForce = payload.u16le(payload.size - 2)
        if (kotlin.math.abs(leadingForce - trailingForce) > CUSTOM_CURVE_B4_FORCE_MIRROR_TOLERANCE_TENTHS_LB) {
            return null
        }
        return leadingForce
    }

    private fun parseCustomCurveVendorForceTenthsLb(payload: ByteArray): Int? {
        if (payload.size != CUSTOM_CURVE_VENDOR_STATUS_BYTES) return null
        if (payload[0].u8() != TELEMETRY_ISOMETRIC_SUMMARY_TYPE) return null
        if (payload[1].u8() != TELEMETRY_ISOMETRIC_SUMMARY_LENGTH_MARKER) return null
        if (payload[2].u8() != VoltraControlFrames.WORKOUT_STATE_CUSTOM_CURVE) return null
        return payload.u16le(CUSTOM_CURVE_VENDOR_FORCE_OFFSET)
    }

    private fun parseIsometricTelemetry(
        packet: ParsedVoltraPacket,
        current: VoltraReading,
        nowMillis: Long,
    ): IsometricTelemetry? {
        return parseLegacyIsometricTelemetry(packet, current)
            ?: parseIsometricSummaryTelemetry(packet, current, nowMillis)
            ?: parseB4IsometricTelemetry(packet, current, nowMillis)
    }

    private fun parseIsometricWaveform(
        packet: ParsedVoltraPacket,
        current: VoltraReading,
    ): IsometricWaveform? {
        if (packet.commandId != CMD_TELEMETRY) return null
        val payload = packet.payload
        if (payload.size < ISOMETRIC_WAVEFORM_HEADER_BYTES) return null
        if (payload[0].u8() != TELEMETRY_ISOMETRIC_WAVEFORM_TYPE) return null
        if (payload[1].u8() !in TELEMETRY_ISOMETRIC_WAVEFORM_MARKERS) return null

        val chunkIndex = payload[2].u8()
        val declaredSampleCount = payload.u16le(4)
        val availableSampleCount = (payload.size - ISOMETRIC_WAVEFORM_HEADER_BYTES) / 2
        val sampleCount = minOf(declaredSampleCount, availableSampleCount)
        if (sampleCount <= 0) return null

        val parsedSamples = buildList(sampleCount) {
            repeat(sampleCount) { index ->
                val offset = ISOMETRIC_WAVEFORM_HEADER_BYTES + (index * 2)
                val sampleN = (payload.u16le(offset) / 10.0) * LB_TO_NEWTONS
                if (sampleN !in 0.0..MAX_REASONABLE_ISOMETRIC_GRAPH_FORCE_N) return null
                add(sampleN)
            }
        }
        if (parsedSamples.isEmpty()) return null

        val lastChunkIndex = current.isometricWaveformLastChunkIndex
        val shouldReset = chunkIndex <= 1 ||
            lastChunkIndex == null ||
            chunkIndex <= lastChunkIndex
        val mergedSamples = if (shouldReset) {
            parsedSamples
        } else {
            (current.isometricWaveformSamplesN + parsedSamples).takeLast(MAX_ISOMETRIC_WAVEFORM_SAMPLES)
        }
        return IsometricWaveform(
            samplesN = mergedSamples,
            lastChunkIndex = chunkIndex,
        )
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
        val rawCarrierForceN = (payload.u16le(TELEMETRY_ISOMETRIC_FORCE_OFFSET) / 10.0) *
            LEGACY_ISOMETRIC_COARSE_FORCE_SCALE
        if (statusSecondary in TELEMETRY_ISOMETRIC_EXTENDED_LIVE_FORCE_MARKERS) {
            val currentForceN = statusPrimary.toDouble() * LEGACY_ISOMETRIC_PULL_FORCE_SCALE
            if (currentForceN !in 0.0..MAX_REASONABLE_ISOMETRIC_FORCE_N) return null
            val startingNewAttempt =
                current.isometricCurrentForceN == null ||
                    current.isometricTelemetryStartTick == null ||
                    current.isometricCarrierStatusSecondary !in TELEMETRY_ISOMETRIC_EXTENDED_LIVE_FORCE_MARKERS
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
                peakRelativeForcePercent = current.isometricPeakRelativeForcePercent,
                elapsedMillis = elapsedMillis,
                tick = tick,
                startTick = startTick,
                startingNewAttempt = startingNewAttempt,
                rawCarrierForceN = rawCarrierForceN,
                carrierStatusPrimary = statusPrimary,
                carrierStatusSecondary = statusSecondary,
            )
        }
        if (
            statusPrimary == 0 &&
            statusSecondary == TELEMETRY_ISOMETRIC_COARSE_LIVE_FORCE_MARKER &&
            rawCarrierForceN in TELEMETRY_ISOMETRIC_COARSE_LIVE_FORCE_RANGE_N
        ) {
            val currentForceN = rawCarrierForceN
            val startingNewAttempt =
                current.isometricCurrentForceN == null ||
                    current.isometricTelemetryStartTick == null ||
                    current.isometricCarrierStatusSecondary != TELEMETRY_ISOMETRIC_COARSE_LIVE_FORCE_MARKER
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
                peakRelativeForcePercent = current.isometricPeakRelativeForcePercent,
                elapsedMillis = elapsedMillis,
                tick = tick,
                startTick = startTick,
                startingNewAttempt = startingNewAttempt,
                rawCarrierForceN = rawCarrierForceN,
                carrierStatusPrimary = statusPrimary,
                carrierStatusSecondary = statusSecondary,
            )
        }
        if (
            statusSecondary != TELEMETRY_ISOMETRIC_ACTIVE_MARKER &&
            statusSecondary != TELEMETRY_ISOMETRIC_PROGRESS_MARKER &&
            statusSecondary != TELEMETRY_ISOMETRIC_READY_MARKER &&
            statusSecondary != TELEMETRY_ISOMETRIC_ARMED_MARKER &&
            statusSecondary != TELEMETRY_ISOMETRIC_COMPLETED_MARKER
        ) {
            return null
        }

        val retainCompletedAttempt =
            current.isometricWaveformSamplesN.isNotEmpty() ||
                current.isometricPeakRelativeForcePercent != null ||
                current.isometricTelemetryStartTick != null

        return IsometricTelemetry(
            currentForceN = null,
            peakForceN = if (retainCompletedAttempt) current.isometricPeakForceN else null,
            peakRelativeForcePercent = if (retainCompletedAttempt) current.isometricPeakRelativeForcePercent else null,
            elapsedMillis = if (retainCompletedAttempt) current.isometricElapsedMillis else null,
            tick = tick,
            startTick = if (retainCompletedAttempt) current.isometricTelemetryStartTick else null,
            startingNewAttempt = false,
            rawCarrierForceN = rawCarrierForceN,
            carrierStatusPrimary = statusPrimary,
            carrierStatusSecondary = statusSecondary,
        )
    }

    private fun parseIsometricSummaryTelemetry(
        packet: ParsedVoltraPacket,
        current: VoltraReading,
        nowMillis: Long,
    ): IsometricTelemetry? {
        if (packet.commandId != CMD_TELEMETRY) return null
        val payload = packet.payload
        if (payload.size != TELEMETRY_ISOMETRIC_SUMMARY_BYTES) return null
        if (payload[0].u8() != TELEMETRY_ISOMETRIC_SUMMARY_TYPE) return null
        if (payload[1].u8() != TELEMETRY_ISOMETRIC_SUMMARY_LENGTH_MARKER) return null

        val hasCollectedAttemptEvidence =
            current.isometricTelemetryStartTick != null ||
                current.isometricCurrentForceN != null ||
                current.isometricWaveformSamplesN.isNotEmpty()
        if (!hasCollectedAttemptEvidence) return null

        val peakForceTenthsN = payload.u16le(TELEMETRY_ISOMETRIC_SUMMARY_PEAK_FORCE_OFFSET)
        val peakRelativeTenthsPercent = payload.u16le(TELEMETRY_ISOMETRIC_SUMMARY_PEAK_RELATIVE_FORCE_OFFSET)
        val durationSeconds = payload.u16le(TELEMETRY_ISOMETRIC_SUMMARY_DURATION_SECONDS_OFFSET)
        if (peakForceTenthsN !in 0..(MAX_REASONABLE_ISOMETRIC_FORCE_N * 10.0).toInt()) return null
        if (peakRelativeTenthsPercent !in 0..MAX_REASONABLE_ISOMETRIC_RELATIVE_FORCE_TENTHS_PERCENT) return null
        if (durationSeconds !in 0..MAX_REASONABLE_ISOMETRIC_DURATION_SECONDS) return null
        val peakForceN = peakForceTenthsN / 10.0
        val elapsedMillis = durationSeconds * 1_000L
        val currentPeakForceN = current.isometricPeakForceN
        val currentElapsedMillis = current.isometricElapsedMillis
        val summaryShouldOverrideSparseCarrierTrace =
            current.isometricCurrentForceN == null &&
                current.isometricPeakRelativeForcePercent == null &&
                current.isometricWaveformSamplesN.size in 1..MAX_SUMMARY_RECONCILIATION_SPARSE_SAMPLES &&
                current.isometricCarrierStatusSecondary == TELEMETRY_ISOMETRIC_ARMED_MARKER
        val summaryLooksStale =
            (currentPeakForceN != null &&
                peakForceN + STALE_ISOMETRIC_SUMMARY_FORCE_TOLERANCE_N < currentPeakForceN) ||
                (currentElapsedMillis != null &&
                    elapsedMillis + STALE_ISOMETRIC_SUMMARY_ELAPSED_TOLERANCE_MILLIS < currentElapsedMillis)
        if (summaryLooksStale && !summaryShouldOverrideSparseCarrierTrace) return null

        return IsometricTelemetry(
            currentForceN = null,
            peakForceN = peakForceN,
            peakRelativeForcePercent = peakRelativeTenthsPercent / 10.0,
            elapsedMillis = elapsedMillis,
            tick = current.isometricTelemetryTick ?: nowMillis,
            startTick = current.isometricTelemetryStartTick,
            startingNewAttempt = false,
            rawCarrierForceN = current.isometricCarrierForceN,
            carrierStatusPrimary = current.isometricCarrierStatusPrimary,
            carrierStatusSecondary = current.isometricCarrierStatusSecondary,
        )
    }

    private fun parseB4IsometricTelemetry(
        packet: ParsedVoltraPacket,
        current: VoltraReading,
        nowMillis: Long,
    ): IsometricTelemetry? {
        if (packet.commandId != CMD_ISOMETRIC_STREAM) return null
        val payload = packet.payload
        return when (payload.size) {
            8 -> parseLegacyB4IsometricTelemetry(payload, current, nowMillis)
                ?: parseModernB4IsometricTelemetry(payload, current, nowMillis)
            12 -> parseExtendedModernB4IsometricTelemetry(payload, current, nowMillis)
            else -> null
        }
    }

    private fun parseLegacyB4IsometricTelemetry(
        payload: ByteArray,
        current: VoltraReading,
        nowMillis: Long,
    ): IsometricTelemetry? {
        if (payload.u16le(2) !in LEGACY_ISOMETRIC_STREAM_VARIANTS) return null
        if (payload.u16le(6) !in ISOMETRIC_SAMPLE_RATE_MIN..ISOMETRIC_SAMPLE_RATE_MAX) return null

        val currentForceN = payload.u16le(0).toDouble() * LB_TO_NEWTONS
        if (currentForceN !in 0.0..MAX_REASONABLE_ISOMETRIC_FORCE_N) return null
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
            peakRelativeForcePercent = null,
            elapsedMillis = elapsedMillis,
            tick = nowMillis,
            startTick = startTick,
            startingNewAttempt = startingNewAttempt,
            rawCarrierForceN = currentForceN,
            carrierStatusPrimary = payload.u16le(2),
            carrierStatusSecondary = null,
        )
    }

    private fun parseModernB4IsometricTelemetry(
        payload: ByteArray,
        current: VoltraReading,
        nowMillis: Long,
    ): IsometricTelemetry? {
        val currentForceN = payload.u16le(0).toDouble()
        val statusWord = payload.u16le(2)
        val reserved = payload.u16le(4)
        val trailingWord = payload.u16le(6)
        if (currentForceN !in 0.0..MAX_REASONABLE_ISOMETRIC_FORCE_N) return null
        if (statusWord !in 0..MAX_REASONABLE_ISOMETRIC_STATUS_WORD) return null
        if (reserved !in 0..MAX_REASONABLE_ISOMETRIC_AUX_WORD) return null
        if (trailingWord !in 0..MAX_REASONABLE_ISOMETRIC_AUX_WORD) return null

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
            peakRelativeForcePercent = null,
            elapsedMillis = elapsedMillis,
            tick = nowMillis,
            startTick = startTick,
            startingNewAttempt = startingNewAttempt,
            rawCarrierForceN = currentForceN,
            carrierStatusPrimary = statusWord,
            carrierStatusSecondary = null,
        )
    }

    private fun parseExtendedModernB4IsometricTelemetry(
        payload: ByteArray,
        current: VoltraReading,
        nowMillis: Long,
    ): IsometricTelemetry? {
        val currentForceN = payload.u16le(0).toDouble()
        val auxPeakWord = payload.u16le(2)
        val auxElapsedWord = payload.u16le(4)
        val statusWord = payload.u16le(6)
        val statusAuxWord = payload.u16le(8)
        val trailingWord = payload.u16le(10)
        if (currentForceN !in 0.0..MAX_REASONABLE_ISOMETRIC_FORCE_N) return null
        if (auxPeakWord !in 0..MAX_REASONABLE_ISOMETRIC_AUX_WORD) return null
        if (auxElapsedWord !in 0..MAX_REASONABLE_ISOMETRIC_AUX_WORD) return null
        if (statusWord !in 0..MAX_REASONABLE_ISOMETRIC_AUX_WORD) return null
        if (statusAuxWord !in 0..MAX_REASONABLE_ISOMETRIC_AUX_WORD) return null
        if (trailingWord !in 0..MAX_REASONABLE_ISOMETRIC_AUX_WORD) return null

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
            peakRelativeForcePercent = null,
            elapsedMillis = elapsedMillis,
            tick = nowMillis,
            startTick = startTick,
            startingNewAttempt = startingNewAttempt,
            rawCarrierForceN = currentForceN,
            carrierStatusPrimary = statusWord,
            carrierStatusSecondary = statusAuxWord,
        )
    }

    private fun repPhaseLabel(phase: Int): String = when (phase) {
        0 -> "Idle"
        1 -> "Pull"
        2 -> "Transition"
        3 -> "Return"
        else -> "Phase $phase"
    }

    private fun deriveIsometricPeakRelativeForcePercent(
        peakForceN: Double?,
        bodyWeightN: Double?,
        metricsType: Int?,
    ): Double? {
        if (metricsType != null && metricsType != ISOMETRIC_METRICS_TYPE_FORCE) return null
        val safePeakForceN = peakForceN?.takeIf { it in 0.0..MAX_REASONABLE_ISOMETRIC_FORCE_N } ?: return null
        val safeBodyWeightN =
            bodyWeightN?.takeIf { it in MIN_REASONABLE_ISOMETRIC_BODY_WEIGHT_N..MAX_REASONABLE_ISOMETRIC_BODY_WEIGHT_N }
                ?: return null
        if (safeBodyWeightN == 0.0) return null
        return ((safePeakForceN / safeBodyWeightN) * 1000.0).toInt() / 10.0
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

    private data class CustomCurveTelemetry(
        val forceLb: Double,
        val setCount: Int,
        val repCount: Int,
        val phase: String,
    )

    private data class IsometricTelemetry(
        val currentForceN: Double?,
        val peakForceN: Double?,
        val peakRelativeForcePercent: Double?,
        val elapsedMillis: Long?,
        val tick: Long,
        val startTick: Long?,
        val startingNewAttempt: Boolean,
        val rawCarrierForceN: Double?,
        val carrierStatusPrimary: Int?,
        val carrierStatusSecondary: Int?,
    )

    private data class IsometricWaveform(
        val samplesN: List<Double>,
        val lastChunkIndex: Int,
    )
}
