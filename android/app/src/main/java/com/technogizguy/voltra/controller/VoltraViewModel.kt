package com.technogizguy.voltra.controller

import android.app.Application
import android.content.Context
import android.content.Intent
import com.technogizguy.voltra.controller.mqtt.MqttPublisherState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.technogizguy.voltra.controller.model.RawVoltraFrame
import com.technogizguy.voltra.controller.model.VoltraCommandResult
import com.technogizguy.voltra.controller.model.VoltraConnectionState
import com.technogizguy.voltra.controller.model.VoltraReading
import com.technogizguy.voltra.controller.model.VoltraSafetyState
import com.technogizguy.voltra.controller.model.VoltraScanResult
import com.technogizguy.voltra.controller.model.VoltraSessionState
import com.technogizguy.voltra.controller.model.Weight
import com.technogizguy.voltra.controller.model.WeightUnit
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
}

class VoltraViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val client = AppGraph.client
    private val mqttSensorPublisher = AppGraph.mqttSensorPublisher
    private val preferencesRepository = AppGraph.preferencesRepository
    private var scanJob: Job? = null

    val state: StateFlow<VoltraSessionState> = client.state
    val mqttState: StateFlow<MqttPublisherState> = mqttSensorPublisher.state
    val preferences: StateFlow<LocalPreferences> = preferencesRepository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LocalPreferences(),
    )

    private val mutableScanResults = MutableStateFlow<List<VoltraScanResult>>(emptyList())
    val scanResults: StateFlow<List<VoltraScanResult>> = mutableScanResults

    private val mutableShowAllDevices = MutableStateFlow(false)
    val showAllDevices: StateFlow<Boolean> = mutableShowAllDevices

    private val mutableSelectedControlMode = MutableStateFlow(ControlModeUi.WEIGHT_TRAINING)
    val selectedControlMode: StateFlow<ControlModeUi> = mutableSelectedControlMode

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
        viewModelScope.launch {
            client.setTargetLoad(Weight(value = value, unit = unit))
        }
    }

    fun setAssistMode(enabled: Boolean) {
        viewModelScope.launch {
            client.setAssistMode(enabled)
        }
    }

    fun setChainsWeight(value: Double) {
        val unit = preferences.value.unit
        viewModelScope.launch {
            client.setChainsWeight(Weight(value = value, unit = unit))
        }
    }

    fun setEccentricWeight(value: Double) {
        val unit = preferences.value.unit
        viewModelScope.launch {
            client.setEccentricWeight(Weight(value = value, unit = unit))
        }
    }

    fun setInverseChainsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            client.setInverseChainsEnabled(enabled)
        }
    }

    fun setResistanceExperience(intense: Boolean) {
        viewModelScope.launch {
            client.setResistanceExperience(intense)
        }
    }

    fun setResistanceBandInverse(enabled: Boolean) {
        viewModelScope.launch {
            client.setResistanceBandInverse(enabled)
        }
    }

    fun setResistanceBandCurveLogarithm(enabled: Boolean) {
        viewModelScope.launch {
            client.setResistanceBandCurveAlgorithm(enabled)
        }
    }

    fun enterResistanceBandMode() {
        viewModelScope.launch {
            client.enterResistanceBandMode()
        }
    }

    fun enterDamperMode() {
        viewModelScope.launch {
            client.enterDamperMode()
        }
    }

    fun enterIsokineticMode() {
        viewModelScope.launch {
            client.enterIsokineticMode()
        }
    }

    fun enterIsometricMode() {
        viewModelScope.launch {
            client.enterIsometricMode()
        }
    }

    fun setDamperLevel(level: Int) {
        viewModelScope.launch {
            client.setDamperLevel(level)
        }
    }

    fun setResistanceBandMaxForce(value: Double) {
        val unit = preferences.value.unit
        viewModelScope.launch {
            client.setResistanceBandMaxForce(Weight(value = value, unit = unit))
        }
    }

    fun setResistanceBandByRangeOfMotion(enabled: Boolean) {
        viewModelScope.launch {
            client.setResistanceBandByRangeOfMotion(enabled)
        }
    }

    fun setResistanceBandLengthInches(valueInches: Double) {
        viewModelScope.launch {
            client.setResistanceBandLengthCm((valueInches * 2.54).roundToInt())
        }
    }

    fun setIsokineticMenu(mode: Int) {
        viewModelScope.launch {
            client.setIsokineticMenu(mode)
        }
    }

    fun setIsokineticTargetSpeedMmS(speedMmS: Int) {
        viewModelScope.launch {
            client.setIsokineticTargetSpeedMmS(speedMmS)
        }
    }

    fun setIsokineticSpeedLimitMmS(speedMmS: Int) {
        viewModelScope.launch {
            client.setIsokineticSpeedLimitMmS(speedMmS)
        }
    }

    fun setIsokineticConstantResistance(value: Double) {
        val unit = preferences.value.unit
        viewModelScope.launch {
            client.setIsokineticConstantResistance(Weight(value = value, unit = unit))
        }
    }

    fun setIsokineticMaxEccentricLoad(value: Double) {
        val unit = preferences.value.unit
        viewModelScope.launch {
            client.setIsokineticMaxEccentricLoad(Weight(value = value, unit = unit))
        }
    }

    fun loadResistanceBand() {
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

    fun publishMqttNow() {
        mqttSensorPublisher.publishNow(state.value)
    }

    fun setStrengthMode() {
        viewModelScope.launch {
            client.setStrengthMode()
        }
    }

    fun load() {
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
        viewModelScope.launch {
            client.exitWorkout()
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
        appendLine("Isokinetic target speed: ${reading.isokineticTargetSpeedMmS?.let { "${it / 1000.0} m/s" } ?: "unknown"}")
        appendLine("Isokinetic eccentric speed limit: ${reading.isokineticSpeedLimitMmS?.let { if (it == 0) "Auto" else "${it / 1000.0} m/s" } ?: "unknown"}")
        appendLine("Isokinetic constant resistance: ${reading.isokineticConstantResistanceLb?.let { "$it lb" } ?: "unknown"}")
        appendLine("Isokinetic max eccentric load: ${reading.isokineticMaxEccentricLoadLb?.let { "$it lb" } ?: "unknown"}")
        appendLine("Isometric max force limit: ${reading.isometricMaxForceLb?.let { "$it lb" } ?: "unknown"}")
        appendLine("Isometric max duration: ${reading.isometricMaxDurationSeconds?.let { "$it s" } ?: "unknown"}")
        appendLine("Isometric live force: ${reading.isometricCurrentForceN?.let { "$it N" } ?: "unknown"}")
        appendLine("Isometric peak force: ${reading.isometricPeakForceN?.let { "$it N" } ?: "unknown"}")
        appendLine("Isometric elapsed: ${reading.isometricElapsedMillis?.let { "$it ms" } ?: "unknown"}")
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
}

private fun VoltraSessionState.isLatestSessionTimestamp(timestampMillis: Long): Boolean {
    val start = connectedAtMillis ?: return false
    val end = lastDisconnectAtMillis
    return timestampMillis >= start && (end == null || timestampMillis <= end)
}
