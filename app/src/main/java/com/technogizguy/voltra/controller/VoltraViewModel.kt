package com.technogizguy.voltra.controller

import android.app.Application
import android.content.Context
import android.content.Intent
import com.technogizguy.voltra.controller.http.HttpGatewayState
import com.technogizguy.voltra.controller.mqtt.MqttPublisherState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.technogizguy.voltra.controller.model.VoltraControlCommand
import com.technogizguy.voltra.controller.model.RawVoltraFrame
import com.technogizguy.voltra.controller.model.VoltraCommandResult
import com.technogizguy.voltra.controller.model.VoltraConnectionState
import com.technogizguy.voltra.controller.model.VoltraReading
import com.technogizguy.voltra.controller.model.VoltraSafetyState
import com.technogizguy.voltra.controller.model.VoltraScanResult
import com.technogizguy.voltra.controller.model.VoltraSessionState
import com.technogizguy.voltra.controller.model.Weight
import com.technogizguy.voltra.controller.model.WeightUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class ControlModeUi {
    WEIGHT_TRAINING,
    RESISTANCE_BAND,
    DAMPER,
    ISOKINETIC,
    ISOMETRIC_TEST,
    CUSTOM_CURVE,
    ROWING,
}

class VoltraViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val client = AppGraph.client
    private val mqttSensorPublisher = AppGraph.mqttSensorPublisher
    private val httpGatewayServer = AppGraph.httpGatewayServer
    private val preferencesRepository = AppGraph.preferencesRepository
    private var scanJob: Job? = null

    val state: StateFlow<VoltraSessionState> = client.state
    val mqttState: StateFlow<MqttPublisherState> = mqttSensorPublisher.state
    val httpGatewayState: StateFlow<HttpGatewayState> = httpGatewayServer.state
    val preferences: StateFlow<LocalPreferences> = preferencesRepository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LocalPreferences(),
    )
    val weightPresets: StateFlow<List<WeightPreset>> = preferencesRepository.weightPresets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )
    val customCurvePresets: StateFlow<List<CustomCurvePreset>> = preferencesRepository.customCurvePresets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )
    val workoutHistory: StateFlow<List<WorkoutHistoryEntry>> = preferencesRepository.workoutHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val mutableScanResults = MutableStateFlow<List<VoltraScanResult>>(emptyList())
    val scanResults: StateFlow<List<VoltraScanResult>> = mutableScanResults

    private val mutableShowAllDevices = MutableStateFlow(false)
    val showAllDevices: StateFlow<Boolean> = mutableShowAllDevices

    private val mutableSelectedControlMode = MutableStateFlow(ControlModeUi.WEIGHT_TRAINING)
    val selectedControlMode: StateFlow<ControlModeUi> = mutableSelectedControlMode
    private var activeWorkoutDraft: ActiveWorkoutDraft? = null

    init {
        viewModelScope.launch {
            state.collect(::trackWorkoutHistory)
        }
    }

    fun startScan() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            client.setShowAllScanResults(mutableShowAllDevices.value)
            client.scan().collect { results ->
                mutableScanResults.value = results
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        mutableScanResults.update { it }
    }

    fun setShowAllDevices(showAll: Boolean) {
        mutableShowAllDevices.value = showAll
        client.setShowAllScanResults(showAll)
        if (state.value.connectionState == VoltraConnectionState.SCANNING) {
            startScan()
        }
    }

    fun selectControlMode(mode: ControlModeUi) {
        mutableSelectedControlMode.value = mode
    }

    fun connect(result: VoltraScanResult) {
        viewModelScope.launch {
            preferencesRepository.rememberDevice(result.device.id, result.device.name)
            VoltraConnectionService.start(getApplication())
            client.connect(result.device.id)
        }
    }

    fun connectLastDevice() {
        val deviceId = preferences.value.lastDeviceId ?: return
        viewModelScope.launch {
            VoltraConnectionService.start(getApplication())
            client.connect(deviceId)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            client.disconnect()
        }
    }

    fun emergencyDisconnect() {
        viewModelScope.launch {
            client.emergencyDisconnect()
        }
    }

    fun setTargetLoad(value: Double) {
        val unit = preferences.value.unit
        beginWorkoutSessionFor(mutableSelectedControlMode.value)
        viewModelScope.launch {
            client.setTargetLoad(Weight(value = value, unit = unit))
        }
    }

    fun setAssistMode(enabled: Boolean) {
        beginWorkoutSessionFor(ControlModeUi.WEIGHT_TRAINING)
        viewModelScope.launch {
            client.setAssistMode(enabled)
        }
    }

    fun setChainsWeight(value: Double) {
        val unit = preferences.value.unit
        beginWorkoutSessionFor(ControlModeUi.WEIGHT_TRAINING)
        viewModelScope.launch {
            client.setChainsWeight(Weight(value = value, unit = unit))
        }
    }

    fun setEccentricWeight(value: Double) {
        val unit = preferences.value.unit
        beginWorkoutSessionFor(ControlModeUi.WEIGHT_TRAINING)
        viewModelScope.launch {
            client.setEccentricWeight(Weight(value = value, unit = unit))
        }
    }

    fun setInverseChainsEnabled(enabled: Boolean) {
        beginWorkoutSessionFor(ControlModeUi.WEIGHT_TRAINING)
        viewModelScope.launch {
            client.setInverseChainsEnabled(enabled)
        }
    }

    fun setResistanceExperience(intense: Boolean) {
        beginWorkoutSessionFor(mutableSelectedControlMode.value)
        viewModelScope.launch {
            client.setResistanceExperience(intense)
        }
    }

    fun setResistanceBandInverse(enabled: Boolean) {
        beginWorkoutSessionFor(ControlModeUi.RESISTANCE_BAND)
        viewModelScope.launch {
            client.setResistanceBandInverse(enabled)
        }
    }

    fun setResistanceBandCurveLogarithm(enabled: Boolean) {
        beginWorkoutSessionFor(ControlModeUi.RESISTANCE_BAND)
        viewModelScope.launch {
            client.setResistanceBandCurveAlgorithm(enabled)
        }
    }

    fun enterResistanceBandMode() {
        beginWorkoutSessionFor(ControlModeUi.RESISTANCE_BAND)
        viewModelScope.launch {
            client.enterResistanceBandMode()
        }
    }

    fun enterDamperMode() {
        beginWorkoutSessionFor(ControlModeUi.DAMPER)
        viewModelScope.launch {
            client.enterDamperMode()
        }
    }

    fun enterIsokineticMode() {
        beginWorkoutSessionFor(ControlModeUi.ISOKINETIC)
        viewModelScope.launch {
            client.enterIsokineticMode()
        }
    }

    fun enterIsometricMode() {
        beginWorkoutSessionFor(ControlModeUi.ISOMETRIC_TEST)
        viewModelScope.launch {
            client.enterIsometricMode()
        }
    }

    fun enterCustomCurveMode() {
        beginWorkoutSessionFor(ControlModeUi.CUSTOM_CURVE)
        viewModelScope.launch {
            client.enterCustomCurveMode()
        }
    }

    fun enterRowMode() {
        beginWorkoutSessionFor(ControlModeUi.ROWING)
        viewModelScope.launch {
            client.enterRowMode()
        }
    }

    fun startRow(targetMeters: Int? = null) {
        beginWorkoutSessionFor(ControlModeUi.ROWING)
        viewModelScope.launch {
            client.startRow(targetMeters)
        }
    }

    fun setRowingResistanceLevel(level: Int) {
        beginWorkoutSessionFor(ControlModeUi.ROWING)
        viewModelScope.launch {
            client.setRowingResistanceLevel(level)
        }
    }

    fun setRowingSimulatedWearLevel(level: Int) {
        beginWorkoutSessionFor(ControlModeUi.ROWING)
        viewModelScope.launch {
            client.setRowingSimulatedWearLevel(level)
        }
    }

    fun applyCustomCurve(
        points: List<Float>,
        resistanceMinLb: Int,
        resistanceLimitLb: Int,
        rangeOfMotionIn: Int,
    ) {
        beginWorkoutSessionFor(ControlModeUi.CUSTOM_CURVE)
        viewModelScope.launch {
            client.applyCustomCurve(
                points = points,
                resistanceMinLb = resistanceMinLb,
                resistanceLimitLb = resistanceLimitLb,
                rangeOfMotionIn = rangeOfMotionIn,
            )
        }
    }

    fun setDamperLevel(level: Int) {
        beginWorkoutSessionFor(ControlModeUi.DAMPER)
        viewModelScope.launch {
            client.setDamperLevel(level)
        }
    }

    fun setResistanceBandMaxForce(value: Double) {
        val unit = preferences.value.unit
        beginWorkoutSessionFor(ControlModeUi.RESISTANCE_BAND)
        viewModelScope.launch {
            client.setResistanceBandMaxForce(Weight(value = value, unit = unit))
        }
    }

    fun setResistanceBandByRangeOfMotion(enabled: Boolean) {
        beginWorkoutSessionFor(ControlModeUi.RESISTANCE_BAND)
        viewModelScope.launch {
            client.setResistanceBandByRangeOfMotion(enabled)
        }
    }

    fun setResistanceBandLengthInches(valueInches: Double) {
        beginWorkoutSessionFor(ControlModeUi.RESISTANCE_BAND)
        viewModelScope.launch {
            client.setResistanceBandLengthCm((valueInches * 2.54).roundToInt())
        }
    }

    fun setIsokineticMenu(mode: Int) {
        beginWorkoutSessionFor(ControlModeUi.ISOKINETIC)
        viewModelScope.launch {
            client.setIsokineticMenu(mode)
        }
    }

    fun setIsokineticTargetSpeedMmS(speedMmS: Int) {
        beginWorkoutSessionFor(ControlModeUi.ISOKINETIC)
        viewModelScope.launch {
            client.setIsokineticTargetSpeedMmS(speedMmS)
        }
    }

    fun setIsokineticSpeedLimitMmS(speedMmS: Int) {
        beginWorkoutSessionFor(ControlModeUi.ISOKINETIC)
        viewModelScope.launch {
            client.setIsokineticSpeedLimitMmS(speedMmS)
        }
    }

    fun setIsokineticConstantResistance(value: Double) {
        val unit = preferences.value.unit
        beginWorkoutSessionFor(ControlModeUi.ISOKINETIC)
        viewModelScope.launch {
            client.setIsokineticConstantResistance(Weight(value = value, unit = unit))
        }
    }

    fun setIsokineticMaxEccentricLoad(value: Double) {
        val unit = preferences.value.unit
        beginWorkoutSessionFor(ControlModeUi.ISOKINETIC)
        viewModelScope.launch {
            client.setIsokineticMaxEccentricLoad(Weight(value = value, unit = unit))
        }
    }

    fun loadResistanceBand() {
        beginWorkoutSessionFor(ControlModeUi.RESISTANCE_BAND)
        viewModelScope.launch {
            client.loadResistanceBand()
        }
    }

    fun triggerCableLengthMode() {
        viewModelScope.launch {
            client.triggerCableLengthMode()
        }
    }

    fun setCableOffsetCm(offsetCm: Int) {
        viewModelScope.launch {
            client.setCableOffsetCm(offsetCm)
        }
    }

    fun setDeviceName(name: String) {
        viewModelScope.launch {
            val trimmed = name.trim()
            val result = client.setDeviceName(trimmed)
            if (result.status != com.technogizguy.voltra.controller.model.VoltraCommandStatus.BLOCKED &&
                result.status != com.technogizguy.voltra.controller.model.VoltraCommandStatus.FAILED
            ) {
                val currentDeviceId = state.value.currentDevice?.id ?: preferences.value.lastDeviceId
                if (currentDeviceId != null) {
                    preferencesRepository.rememberDevice(currentDeviceId, trimmed)
                }
            }
        }
    }

    fun uploadStartupImage(jpegBytes: ByteArray) {
        viewModelScope.launch {
            client.uploadStartupImage(jpegBytes)
        }
    }

    fun refreshModeFeatureStatus() {
        viewModelScope.launch {
            client.refreshModeFeatureStatus()
        }
    }

    fun setUnit(unit: WeightUnit) {
        viewModelScope.launch {
            preferencesRepository.setUnit(unit)
            client.setTargetLoad(state.value.targetLoad.copy(unit = unit).cappedForV1())
        }
    }

    fun setAccentColor(accent: AccentColor) {
        viewModelScope.launch {
            preferencesRepository.setAccentColor(accent)
        }
    }

    fun setWeightIncrement(increment: Int) {
        viewModelScope.launch {
            preferencesRepository.setWeightIncrement(increment)
        }
    }

    fun setInstantWeightApplyDefault(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setInstantWeightApplyDefault(enabled)
        }
    }

    fun setDeveloperModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDeveloperModeEnabled(enabled)
        }
    }

    fun setMqttEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setMqttEnabled(enabled)
        }
    }

    fun saveMqttSettings(settings: MqttPreferences) {
        viewModelScope.launch {
            preferencesRepository.setMqttSettings(settings)
        }
    }

    fun setHttpGatewayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHttpGatewayEnabled(enabled)
        }
    }

    fun saveHttpGatewaySettings(settings: HttpGatewayPreferences) {
        viewModelScope.launch {
            preferencesRepository.setHttpGatewaySettings(settings)
        }
    }

    fun rotateHttpGatewayAccessKey() {
        viewModelScope.launch {
            preferencesRepository.rotateHttpGatewayAccessKey()
        }
    }

    fun publishMqttNow() {
        mqttSensorPublisher.publishNow(state.value)
    }

    fun setStrengthMode() {
        beginWorkoutSessionFor(ControlModeUi.WEIGHT_TRAINING)
        viewModelScope.launch {
            client.setStrengthMode()
        }
    }

    fun load() {
        beginWorkoutSessionFor(mutableSelectedControlMode.value)
        viewModelScope.launch {
            client.load()
        }
    }

    fun unload() {
        viewModelScope.launch {
            client.unload()
        }
    }

    fun exitWorkout() {
        finalizeActiveWorkoutIfNeeded(state.value)
        mutableSelectedControlMode.value = ControlModeUi.WEIGHT_TRAINING
        viewModelScope.launch {
            client.exitWorkout()
        }
    }

    fun saveWeightPreset(name: String, scope: WeightPresetScope, value: Double) {
        val unit = preferences.value.unit
        viewModelScope.launch {
            preferencesRepository.upsertWeightPreset(
                name = name,
                scope = scope,
                value = value,
                unit = unit,
            )
        }
    }

    fun deleteWeightPreset(id: String) {
        viewModelScope.launch {
            preferencesRepository.deleteWeightPreset(id)
        }
    }

    fun saveCustomCurvePreset(
        name: String,
        points: List<Float>,
        resistanceMinLb: Int,
        resistanceLimitLb: Int,
        rangeOfMotionIn: Int,
    ) {
        viewModelScope.launch {
            preferencesRepository.upsertCustomCurvePreset(
                name = name,
                points = points,
                resistanceMinLb = resistanceMinLb,
                resistanceLimitLb = resistanceLimitLb,
                rangeOfMotionIn = rangeOfMotionIn,
            )
        }
    }

    fun deleteCustomCurvePreset(id: String) {
        viewModelScope.launch {
            preferencesRepository.deleteCustomCurvePreset(id)
        }
    }

    fun applyWeightPreset(preset: WeightPreset) {
        val targetUnit = preferences.value.unit
        val converted = Weight(preset.value, preset.unit).toUnit(targetUnit).cappedForV1()
        when (preset.scope) {
            WeightPresetScope.WEIGHT_TRAINING -> {
                beginWorkoutSessionFor(ControlModeUi.WEIGHT_TRAINING)
                viewModelScope.launch {
                    client.setTargetLoad(converted)
                }
            }
            WeightPresetScope.RESISTANCE_BAND -> {
                beginWorkoutSessionFor(ControlModeUi.RESISTANCE_BAND)
                viewModelScope.launch {
                    client.setResistanceBandMaxForce(converted)
                }
            }
        }
    }

    fun shareWorkoutHistoryCsv(context: Context) {
        val text = workoutHistoryCsv()
        context.openFileOutput("voltra-workout-history.csv", Context.MODE_PRIVATE).use { output ->
            output.write(text.toByteArray())
        }
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/csv")
            .putExtra(Intent.EXTRA_SUBJECT, "Voltra workout history")
            .putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(intent, "Share Voltra workout history"))
    }

    fun clearWorkoutHistory() {
        viewModelScope.launch {
            preferencesRepository.clearWorkoutHistory()
        }
    }

    fun enableCandidateNotifications() {
        client.enableCandidateNotifications()
    }

    fun readVoltraCharacteristics() {
        client.readVoltraCharacteristics()
    }

    fun runReadOnlyHandshakeProbe() {
        client.runReadOnlyHandshakeProbe()
    }

    fun shareDiagnostics(context: Context) {
        val text = diagnosticsText()
        context.openFileOutput("voltra-diagnostics.txt", Context.MODE_PRIVATE).use { output ->
            output.write(text.toByteArray())
        }
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, "VOLTRA diagnostics")
            .putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(intent, "Share VOLTRA diagnostics"))
    }

    fun diagnosticsText(): String {
        val current = state.value
        val latestSessionFrames = current.rawFrames.filter { current.isLatestSessionTimestamp(it.timestampMillis) }
        val olderFrames = current.rawFrames.filterNot { current.isLatestSessionTimestamp(it.timestampMillis) }
        val latestSessionCommands = current.commandLog.filter { current.isLatestSessionTimestamp(it.timestampMillis) }
        val olderCommands = current.commandLog.filterNot { current.isLatestSessionTimestamp(it.timestampMillis) }
        return buildString {
            appendLine("Voltra Controller Diagnostics")
            appendLine("Connection: ${current.connectionState}")
            appendLine("Protocol: ${current.protocolStatus}")
            appendLine("Status: ${current.statusMessage}")
            appendLine("Last disconnect: ${current.lastDisconnectReason ?: "none"}")
            appendLine("Connected at millis: ${current.connectedAtMillis ?: "unknown"}")
            appendLine("Last disconnect millis: ${current.lastDisconnectAtMillis ?: "unknown"}")
            appendLine("Connection duration millis: ${current.lastConnectionDurationMillis ?: "unknown"}")
            appendLine("Device: ${current.currentDevice?.name ?: "unknown"} ${current.currentDevice?.address.orEmpty()}")
            appendLine("Subscribed characteristics: ${current.subscribedCharacteristicCount}")
            appendLine("Control commands enabled: ${current.controlCommandsEnabled}")
            appendLine("Target load: ${current.targetLoad.display()}")
            appendLine()
            appendLine("Readings")
            appendReadingLines(current.reading)
            appendLine()
            appendLine("Safety")
            appendSafetyLines(current.safety)
            appendLine()
            appendLine("GATT")
            val snapshot = current.gattSnapshot
            if (snapshot == null) {
                appendLine("No GATT snapshot captured yet.")
            } else {
                snapshot.services.forEach { service ->
                    appendLine("Service ${service.uuid}")
                    service.characteristics.forEach { characteristic ->
                        appendLine("  ${characteristic.uuid} ${characteristic.properties} ${characteristic.candidateRole}")
                    }
                }
            }
            appendLine()
            appendLine("Latest Session Frames")
            appendFrameLines(latestSessionFrames.takeLast(80))
            appendLine()
            appendLine("Earlier Frames")
            appendFrameLines(olderFrames.takeLast(80))
            appendLine()
            appendLine("Latest Session Command Log")
            appendCommandLines(latestSessionCommands.takeLast(80))
            appendLine()
            appendLine("Earlier Command Log")
            appendCommandLines(olderCommands.takeLast(80))
        }
    }

    private fun StringBuilder.appendReadingLines(reading: VoltraReading) {
        appendLine("Battery: ${reading.batteryPercent?.let { "$it%" } ?: "unknown"}")
        appendLine("Firmware: ${reading.firmwareVersion ?: "unknown"}")
        appendLine("Serial: ${reading.serialNumber ?: "unknown"}")
        appendLine("Activation: ${reading.activationState ?: "unknown"}")
        appendLine("Lock: ${reading.lockState ?: "unknown"}")
        appendLine("Child lock: ${reading.childLock?.toString() ?: "unknown"}")
        appendLine("Cable length: ${reading.cableLengthCm?.let { "$it cm" } ?: "unknown"}")
        appendLine("Cable offset: ${reading.cableOffsetCm?.let { "$it cm" } ?: "unknown"}")
        appendLine("Force: ${reading.forceLb?.let { "$it lb" } ?: "unknown"}")
        appendLine("Weight: ${reading.weightLb?.let { "$it lb" } ?: "unknown"}")
        appendLine("Resistance Band max force: ${reading.resistanceBandMaxForceLb?.let { "$it lb" } ?: "unknown"}")
        appendLine("Resistance Band length: ${reading.resistanceBandLengthCm?.let { "$it cm" } ?: "unknown"}")
        appendLine("Resistance Band ROM length: ${reading.resistanceBandByRangeOfMotion?.toString() ?: "unknown"}")
        appendLine("Resistance Band inverse: ${reading.resistanceBandInverse?.toString() ?: "unknown"}")
        appendLine("Resistance Band curve: ${reading.resistanceBandCurveLogarithm?.let { if (it) "Logarithm" else "Power Law" } ?: "unknown"}")
        appendLine("Quick cable adjustment: ${reading.quickCableAdjustment?.toString() ?: "unknown"}")
        appendLine("Chains weight: ${reading.chainsWeightLb?.let { "$it lb" } ?: "unknown"}")
        appendLine("Eccentric weight: ${reading.eccentricWeightLb?.let { "$it lb" } ?: "unknown"}")
        appendLine("Inverse chains: ${reading.inverseChains?.toString() ?: "unknown"}")
        appendLine("Weight training extra mode: ${reading.weightTrainingExtraMode ?: "unknown"}")
        appendLine("App current screen id: ${reading.appCurrentScreenId ?: "unknown"}")
        appendLine("Fitness ongoing UI: ${reading.fitnessOngoingUi ?: "unknown"}")
        appendLine("Isokinetic target speed: ${reading.isokineticTargetSpeedMmS?.let { "${it / 1000.0} m/s" } ?: "unknown"}")
        appendLine("Isokinetic eccentric speed limit: ${reading.isokineticSpeedLimitMmS?.let { if (it == 0) "Auto" else "${it / 1000.0} m/s" } ?: "unknown"}")
        appendLine("Isokinetic constant resistance: ${reading.isokineticConstantResistanceLb?.let { "$it lb" } ?: "unknown"}")
        appendLine("Isokinetic max eccentric load: ${reading.isokineticMaxEccentricLoadLb?.let { "$it lb" } ?: "unknown"}")
        appendLine("Isometric max force limit: ${reading.isometricMaxForceLb?.let { "$it lb" } ?: "unknown"}")
        appendLine("Isometric max duration: ${reading.isometricMaxDurationSeconds?.let { "$it s" } ?: "unknown"}")
        appendLine("Isometric metrics type: ${reading.isometricMetricsType?.let(::formatIsometricMetricsType) ?: "unknown"}")
        appendLine("Isometric body weight: ${reading.isometricBodyWeightN?.let { "$it N" } ?: "unknown"}")
        appendLine("Isometric body weight (100g): ${reading.isometricBodyWeight100g ?: "unknown"}")
        appendLine("Isometric body weight (lb): ${reading.isometricBodyWeightLb?.let { "$it lb" } ?: "unknown"}")
        appendLine("Isometric live force: ${reading.isometricCurrentForceN?.let { "$it N" } ?: "unknown"}")
        appendLine("Isometric peak force: ${reading.isometricPeakForceN?.let { "$it N" } ?: "unknown"}")
        appendLine("Isometric peak relative force: ${reading.isometricPeakRelativeForcePercent?.let { "$it %" } ?: "unknown"}")
        appendLine("Isometric elapsed: ${reading.isometricElapsedMillis?.let { "$it ms" } ?: "unknown"}")
        appendLine("Rowing distance: ${reading.rowingDistanceMeters?.let { "$it m" } ?: "unknown"}")
        appendLine("Rowing elapsed: ${reading.rowingElapsedMillis?.let { "$it ms" } ?: "unknown"}")
        appendLine("Rowing pace /500m: ${reading.rowingPace500Millis?.let { "$it ms" } ?: "unknown"}")
        appendLine("Rowing average pace /500m: ${reading.rowingAveragePace500Millis?.let { "$it ms" } ?: "unknown"}")
        appendLine("Rowing stroke rate: ${reading.rowingStrokeRateSpm?.let { "$it spm" } ?: "unknown"}")
        appendLine("Rowing drive force: ${reading.rowingDriveForceLb?.let { "$it lb" } ?: "unknown"}")
        appendLine("Rowing resistance level: ${reading.rowingResistanceLevel ?: "unknown"}")
        appendLine("Rowing simulated wear: ${reading.rowingSimulatedWearLevel ?: "unknown"}")
        appendLine("Rowing distance samples: ${reading.rowingDistanceSamplesMeters.size}")
        appendLine("Set count: ${reading.setCount ?: "unknown"}")
        appendLine("Rep count: ${reading.repCount ?: "unknown"}")
        appendLine("Rep phase: ${reading.repPhase ?: "unknown"}")
        appendLine("Workout mode: ${reading.workoutMode ?: "unknown"}")
        appendLine("Last reading millis: ${reading.lastUpdatedMillis ?: "unknown"}")
    }

    private fun StringBuilder.appendSafetyLines(safety: VoltraSafetyState) {
        appendLine("Can load: ${safety.canLoad}")
        appendLine("Low battery: ${safety.lowBattery ?: "unknown"}")
        appendLine("Locked: ${safety.locked ?: "unknown"}")
        appendLine("Child locked: ${safety.childLocked ?: "unknown"}")
        appendLine("Active OTA: ${safety.activeOta ?: "unknown"}")
        appendLine("Parsed device state: ${safety.parsedDeviceState}")
        appendLine("Workout state: ${safety.workoutState ?: "unknown"}")
        appendLine("Fitness mode: ${safety.fitnessMode ?: "unknown"}")
        appendLine("Target load lb: ${safety.targetLoadLb ?: "unknown"}")
        appendLine("Reasons: ${safety.reasons.joinToString().ifBlank { "none" }}")
    }

    private fun StringBuilder.appendFrameLines(frames: List<RawVoltraFrame>) {
        if (frames.isEmpty()) {
            appendLine("none")
            return
        }
        frames.forEach { frame ->
            appendLine("${frame.timestampMillis} ${frame.direction} ${frame.characteristicUuid} ${frame.hex} ${frame.parsedSummary.orEmpty()} ${frame.asciiPreview.orEmpty()}")
        }
    }

    private fun StringBuilder.appendCommandLines(commands: List<VoltraCommandResult>) {
        if (commands.isEmpty()) {
            appendLine("none")
            return
        }
        commands.forEach { command ->
            appendLine("${command.timestampMillis} ${command.command} ${command.status}: ${command.message}")
        }
    }

    override fun onCleared() {
        scanJob?.cancel()
        super.onCleared()
    }

    private fun trackWorkoutHistory(current: VoltraSessionState) {
        activeWorkoutDraft = activeWorkoutDraft?.updatedWith(current)
        if (current.connectionState == VoltraConnectionState.DISCONNECTED ||
            current.connectionState == VoltraConnectionState.FAILED ||
            current.connectionState == VoltraConnectionState.IDLE
        ) {
            finalizeActiveWorkoutIfNeeded(current)
        }
    }

    private fun beginWorkoutSessionFor(mode: ControlModeUi) {
        if (activeWorkoutDraft?.mode == mode) return
        finalizeActiveWorkoutIfNeeded(state.value)
        val current = state.value
        activeWorkoutDraft = ActiveWorkoutDraft(
            id = UUID.randomUUID().toString(),
            startedAtMillis = System.currentTimeMillis(),
            mode = mode,
            modeLabel = mode.displayLabel(),
            deviceName = current.currentDevice?.name ?: preferences.value.lastDeviceName,
            primarySetting = primarySettingSummary(mode, current),
            batteryStartPercent = current.reading.batteryPercent,
        ).updatedWith(current)
    }

    private fun finalizeActiveWorkoutIfNeeded(current: VoltraSessionState) {
        val draft = activeWorkoutDraft ?: return
        activeWorkoutDraft = null
        if (!draft.hasActivity()) return
        val entry = WorkoutHistoryEntry(
            id = draft.id,
            startedAtMillis = draft.startedAtMillis,
            endedAtMillis = current.reading.lastUpdatedMillis ?: current.lastDisconnectAtMillis ?: System.currentTimeMillis(),
            deviceName = current.currentDevice?.name ?: draft.deviceName,
            modeLabel = draft.modeLabel,
            primarySetting = draft.primarySetting ?: primarySettingSummary(draft.mode, current),
            reps = draft.reps,
            sets = draft.sets,
            peakForceN = draft.peakForceN,
            batteryStartPercent = draft.batteryStartPercent,
            batteryEndPercent = current.reading.batteryPercent ?: draft.batteryEndPercent,
        )
        viewModelScope.launch {
            preferencesRepository.appendWorkoutHistory(entry)
        }
    }

    private fun workoutHistoryCsv(): String {
        val history = workoutHistory.value
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return buildString {
            appendLine("started_at,ended_at,duration_seconds,device,mode,primary_setting,sets,reps,peak_force_n,battery_start_percent,battery_end_percent")
            history.forEach { entry ->
                val durationSeconds = ((entry.endedAtMillis - entry.startedAtMillis).coerceAtLeast(0L) / 1000.0)
                appendLine(
                    listOf(
                        csv(formatter.format(Date(entry.startedAtMillis))),
                        csv(formatter.format(Date(entry.endedAtMillis))),
                        csv("%.1f".format(Locale.US, durationSeconds)),
                        csv(entry.deviceName.orEmpty()),
                        csv(entry.modeLabel),
                        csv(entry.primarySetting.orEmpty()),
                        csv(entry.sets.toString()),
                        csv(entry.reps.toString()),
                        csv(entry.peakForceN?.let { "%.1f".format(Locale.US, it) }.orEmpty()),
                        csv(entry.batteryStartPercent?.toString().orEmpty()),
                        csv(entry.batteryEndPercent?.toString().orEmpty()),
                    ).joinToString(","),
                )
            }
        }
    }

    private fun csv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun primarySettingSummary(mode: ControlModeUi, current: VoltraSessionState): String? {
        val reading = current.reading
        val targetLoad = current.targetLoad.display()
        return when (mode) {
            ControlModeUi.WEIGHT_TRAINING -> buildList {
                add("Weight $targetLoad")
                reading.chainsWeightLb?.takeIf { it > 0 }?.let { add("Chains ${trimToLabel(it)} lb") }
                reading.eccentricWeightLb?.takeIf { it != 0.0 }?.let { add("Eccentric ${trimToLabel(it)} lb") }
            }.joinToString(" | ")
            ControlModeUi.RESISTANCE_BAND -> buildList {
                reading.resistanceBandMaxForceLb?.let { add("Band Force ${trimToLabel(it)} lb") }
                reading.resistanceBandByRangeOfMotion?.let { add(if (it) "ROM" else "Band Length") }
            }.joinToString(" | ").ifBlank { null }
            ControlModeUi.DAMPER -> reading.damperLevelIndex?.let { "Damper factor ${damperFactorLabel(it)}" }
            ControlModeUi.ISOKINETIC -> buildList {
                reading.isokineticTargetSpeedMmS?.let { add("Target ${it / 1000.0} m/s") }
                reading.isokineticConstantResistanceLb?.let { add("Const ${trimToLabel(it)} lb") }
                reading.isokineticMaxEccentricLoadLb?.let { add("Max Ecc ${trimToLabel(it)} lb") }
            }.joinToString(" | ").ifBlank { null }
            ControlModeUi.ISOMETRIC_TEST -> buildList {
                reading.isometricMaxForceLb?.let { add("Force Limit ${trimToLabel(it)} lb") }
                reading.isometricMaxDurationSeconds?.let { add("Duration ${it}s") }
            }.joinToString(" | ").ifBlank { null }
            ControlModeUi.CUSTOM_CURVE -> "Custom Curve"
            ControlModeUi.ROWING -> buildList {
                reading.rowingDistanceMeters?.let { add("${trimToLabel(it)} m") }
                reading.rowingPace500Millis?.let { add("${formatPaceForHistory(it)} /500m") }
            }.joinToString(" | ").ifBlank { "Just Row" }
        }
    }

    private fun trimToLabel(value: Double): String {
        val rounded = kotlin.math.round(value * 10.0) / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    }

    private fun formatIsometricMetricsType(type: Int): String {
        return when (type) {
            0 -> "Force"
            1 -> "Weight"
            else -> type.toString()
        }
    }

    private fun damperFactorLabel(levelIndex: Int): String {
        return when ((levelIndex + 1).coerceIn(1, 10)) {
            1 -> "5"
            2 -> "8"
            3 -> "11"
            4 -> "14"
            5 -> "17"
            6 -> "21"
            7 -> "30"
            8 -> "33"
            9 -> "41"
            else -> "50"
        }
    }

    private fun formatPaceForHistory(paceMillis: Long): String {
        val totalSeconds = ((paceMillis + 500L) / 1000L).coerceAtLeast(0L)
        return String.format(Locale.US, "%d:%02d", totalSeconds / 60L, totalSeconds % 60L)
    }

    private fun ControlModeUi.displayLabel(): String = when (this) {
        ControlModeUi.WEIGHT_TRAINING -> "Weight Training"
        ControlModeUi.RESISTANCE_BAND -> "Resistance Band"
        ControlModeUi.DAMPER -> "Damper"
        ControlModeUi.ISOKINETIC -> "Isokinetic"
        ControlModeUi.ISOMETRIC_TEST -> "Isometric Test"
        ControlModeUi.CUSTOM_CURVE -> "Custom Curve"
        ControlModeUi.ROWING -> "Rowing"
    }

    private data class ActiveWorkoutDraft(
        val id: String,
        val startedAtMillis: Long,
        val mode: ControlModeUi,
        val modeLabel: String,
        val deviceName: String?,
        val primarySetting: String?,
        val batteryStartPercent: Int?,
        val batteryEndPercent: Int? = batteryStartPercent,
        val reps: Int = 0,
        val sets: Int = 0,
        val peakForceN: Double? = null,
    ) {
        fun updatedWith(current: VoltraSessionState): ActiveWorkoutDraft {
            val reading = current.reading
            return copy(
                deviceName = current.currentDevice?.name ?: deviceName,
                primarySetting = primarySetting ?: current.reading.workoutMode,
                batteryEndPercent = reading.batteryPercent ?: batteryEndPercent,
                reps = maxOf(reps, reading.repCount ?: 0),
                sets = maxOf(sets, reading.setCount ?: 0),
                peakForceN = maxNullable(peakForceN, reading.isometricPeakForceN),
            )
        }

        fun hasActivity(): Boolean = reps > 0 || sets > 0 || peakForceN != null

        private fun maxNullable(left: Double?, right: Double?): Double? = when {
            left == null -> right
            right == null -> left
            else -> maxOf(left, right)
        }
    }
}

private fun VoltraSessionState.isLatestSessionTimestamp(timestampMillis: Long): Boolean {
    val start = connectedAtMillis ?: return false
    val end = lastDisconnectAtMillis
    return timestampMillis >= start && (end == null || timestampMillis <= end)
}
