package com.technogizguy.voltra.controller

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.technogizguy.voltra.controller.protocol.VoltraControlFrames
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.roundToInt

enum class ControlModeUi {
    WEIGHT_TRAINING,
    RESISTANCE_BAND,
    DAMPER,
    ISOKINETIC,
    ISOMETRIC_TEST,
    CUSTOM_CURVE,
    ROWING,
    SKI,
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
    private val mutableGitHubUpdateState = MutableStateFlow(GitHubUpdateState())
    val githubUpdateState: StateFlow<GitHubUpdateState> = mutableGitHubUpdateState
    private var activeWorkoutDraft: ActiveWorkoutDraft? = null
    private var lastRememberedReportedDeviceKey: String? = null

    init {
        viewModelScope.launch {
            state.collect { session ->
                trackWorkoutHistory(session)
                rememberReportedDeviceName(session)
            }
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
            refreshModeTargetLoadAfterEntry()
        }
    }

    fun enterDamperMode() {
        beginWorkoutSessionFor(ControlModeUi.DAMPER)
        viewModelScope.launch {
            client.enterDamperMode()
            refreshModeTargetLoadAfterEntry()
        }
    }

    fun enterIsokineticMode() {
        beginWorkoutSessionFor(ControlModeUi.ISOKINETIC)
        viewModelScope.launch {
            client.enterIsokineticMode()
            refreshModeTargetLoadAfterEntry()
        }
    }

    fun enterIsometricMode() {
        beginWorkoutSessionFor(ControlModeUi.ISOMETRIC_TEST)
        viewModelScope.launch {
            client.enterIsometricMode()
            refreshModeTargetLoadAfterEntry()
        }
    }

    fun enterCustomCurveMode() {
        beginWorkoutSessionFor(ControlModeUi.CUSTOM_CURVE)
        viewModelScope.launch {
            client.enterCustomCurveMode()
            refreshModeTargetLoadAfterEntry()
        }
    }

    fun enterRowMode() {
        beginWorkoutSessionFor(ControlModeUi.ROWING)
        viewModelScope.launch {
            client.enterRowMode()
            refreshModeTargetLoadAfterEntry()
        }
    }

    fun enterSkiMode() {
        beginWorkoutSessionFor(ControlModeUi.SKI)
        viewModelScope.launch {
            client.enterSkiMode()
            refreshModeTargetLoadAfterEntry()
        }
    }

    fun startRow(targetMeters: Int? = null) {
        val activeCardioMode = when (mutableSelectedControlMode.value) {
            ControlModeUi.SKI -> ControlModeUi.SKI
            else -> ControlModeUi.ROWING
        }
        beginWorkoutSessionFor(activeCardioMode)
        viewModelScope.launch {
            when (activeCardioMode) {
                ControlModeUi.SKI -> client.startSki(targetMeters)
                else -> client.startRow(targetMeters)
            }
            refreshModeTargetLoadAfterEntry()
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
            client.setTargetLoad(state.value.targetLoad.copy(unit = unit).cappedForMax(state.value.maxTargetLoadLbForUi()))
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
            refreshModeTargetLoadAfterEntry()
        }
    }

    private suspend fun refreshModeTargetLoadAfterEntry() {
        delay(350)
        refreshModeFeatureStatusIfReady()
        delay(900)
        refreshModeFeatureStatusIfReady()
    }

    private suspend fun refreshModeFeatureStatusIfReady() {
        if (state.value.connectionState == VoltraConnectionState.CONNECTED && state.value.controlCommandsEnabled) {
            client.refreshModeFeatureStatus()
        }
    }

    fun load() {
        beginWorkoutSessionFor(mutableSelectedControlMode.value)
        viewModelScope.launch {
            client.load()
        }
    }

    fun directLoad() {
        beginWorkoutSessionFor(mutableSelectedControlMode.value)
        viewModelScope.launch {
            client.directLoad()
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
        val maxTargetLoadLb = when (preset.scope) {
            WeightPresetScope.WEIGHT_TRAINING -> state.value.maxTargetLoadLbForUi()
            WeightPresetScope.RESISTANCE_BAND -> VoltraControlFrames.MAX_RESISTANCE_BAND_FORCE_LB.toDouble()
        }
        val converted = Weight(preset.value, preset.unit).toUnit(targetUnit).cappedForMax(maxTargetLoadLb)
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

    fun checkGitHubUpdates() {
        if (mutableGitHubUpdateState.value.checking) return
        viewModelScope.launch {
            val currentVersion = appVersion()
            mutableGitHubUpdateState.value = GitHubUpdateState(
                checking = true,
                message = "Checking GitHub releases...",
            )
            runCatching { fetchLatestGitHubRelease() }
                .onSuccess { release ->
                    val updateAvailable = compareVersions(release.version, currentVersion) > 0
                    mutableGitHubUpdateState.value = GitHubUpdateState(
                        checking = false,
                        latestVersion = release.version,
                        latestTag = release.tag,
                        releaseUrl = release.releaseUrl,
                        apkDownloadUrl = release.apkDownloadUrl,
                        updateAvailable = updateAvailable,
                        checkedAtMillis = System.currentTimeMillis(),
                        message = when {
                            updateAvailable -> "Beta ${release.version} is available on GitHub."
                            release.version != null -> "You are on the latest GitHub release."
                            else -> "Latest release found, but its version label could not be parsed."
                        },
                    )
                }
                .onFailure { error ->
                    mutableGitHubUpdateState.value = GitHubUpdateState(
                        checking = false,
                        checkedAtMillis = System.currentTimeMillis(),
                        message = "Could not check GitHub releases.",
                        error = error.message ?: error::class.java.simpleName,
                    )
                }
        }
    }

    fun openGitHubRelease(context: Context, preferApk: Boolean = false) {
        val updateState = mutableGitHubUpdateState.value
        val targetUrl = when {
            preferApk -> updateState.apkDownloadUrl ?: updateState.releaseUrl ?: GITHUB_RELEASES_URL
            else -> updateState.releaseUrl ?: GITHUB_RELEASES_URL
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun diagnosticsText(): String {
        val current = state.value
        val currentMqttState = mqttState.value
        val currentHttpGatewayState = httpGatewayState.value
        val latestSessionFrames = current.rawFrames.filter { current.isLatestSessionTimestamp(it.timestampMillis) }
        val olderFrames = current.rawFrames.filterNot { current.isLatestSessionTimestamp(it.timestampMillis) }
        val latestSessionCommands = current.commandLog.filter { current.isLatestSessionTimestamp(it.timestampMillis) }
        val olderCommands = current.commandLog.filterNot { current.isLatestSessionTimestamp(it.timestampMillis) }
        return buildString {
            appendLine("Voltra Controller Diagnostics")
            appendLine("App version: ${appVersion()}")
            appendLine("Connection: ${current.connectionState}")
            appendLine("Protocol: ${current.protocolStatus}")
            appendLine("Status: ${current.statusMessage}")
            appendLine("Last disconnect: ${current.lastDisconnectReason ?: "none"}")
            appendLine("Connected at millis: ${current.connectedAtMillis ?: "unknown"}")
            appendLine("Last disconnect millis: ${current.lastDisconnectAtMillis ?: "unknown"}")
            appendLine("Connection duration millis: ${current.lastConnectionDurationMillis ?: "unknown"}")
            appendLine("Device: ${current.reading.deviceName ?: current.currentDevice?.name ?: "unknown"} ${current.currentDevice?.address.orEmpty()}")
            appendLine("Subscribed characteristics: ${current.subscribedCharacteristicCount}")
            appendLine("Control commands enabled: ${current.controlCommandsEnabled}")
            appendLine("Target load: ${current.targetLoad.display()}")
            appendLine("Max target load: ${current.safety.maxTargetLoadLb} lb")
            appendLine("Latest session frames: ${latestSessionFrames.size}")
            appendLine("Latest session commands: ${latestSessionCommands.size}")
            appendLine()
            appendLine("Integrations")
            appendLine("MQTT: ${currentMqttState.connectionState}")
            appendLine("MQTT endpoint: ${currentMqttState.brokerEndpoint ?: "unknown"}")
            appendLine("MQTT topic prefix: ${currentMqttState.topicPrefix ?: "unknown"}")
            appendLine("MQTT last error: ${currentMqttState.lastError ?: "none"}")
            appendLine("MQTT last published millis: ${currentMqttState.lastPublishedMillis ?: "unknown"}")
            appendLine("MQTT published topic count: ${currentMqttState.publishedTopicCount}")
            appendLine("HTTP gateway: ${currentHttpGatewayState.connectionState}")
            appendLine("HTTP port: ${currentHttpGatewayState.port}")
            appendLine("HTTP URLs: ${currentHttpGatewayState.urls.joinToString().ifBlank { "unknown" }}")
            appendLine("HTTP last error: ${currentHttpGatewayState.lastError ?: "none"}")
            appendLine("HTTP request count: ${currentHttpGatewayState.requestCount}")
            appendLine("HTTP last request millis: ${currentHttpGatewayState.lastRequestMillis ?: "unknown"}")
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

    private fun appVersion(): String {
        val app = getApplication<Application>()
        return runCatching {
            val packageInfo = app.packageManager.getPackageInfo(app.packageName, 0)
            packageInfo.versionName ?: "unknown"
        }.getOrDefault("unknown")
    }

    private suspend fun fetchLatestGitHubRelease(): GitHubReleaseInfo = withContext(Dispatchers.IO) {
        val connection = (URL(GITHUB_RELEASES_LATEST_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Voltra-Controller-Android")
        }
        try {
            val responseCode = connection.responseCode
            val body = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (responseCode !in 200..299) {
                error("GitHub returned HTTP $responseCode")
            }
            val json = JSONObject(body)
            val tag = json.optString("tag_name").takeIf { it.isNotBlank() }
            val name = json.optString("name").takeIf { it.isNotBlank() }
            val releaseUrl = json.optString("html_url").takeIf { it.isNotBlank() } ?: GITHUB_RELEASES_URL
            val assets = json.optJSONArray("assets")
            var apkDownloadUrl: String? = null
            if (assets != null) {
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    val assetName = asset.optString("name")
                    val assetUrl = asset.optString("browser_download_url")
                    if (assetName.endsWith(".apk", ignoreCase = true) && assetUrl.isNotBlank()) {
                        apkDownloadUrl = assetUrl
                        break
                    }
                }
            }
            GitHubReleaseInfo(
                tag = tag,
                version = parseVersionLabel(tag, name),
                releaseUrl = releaseUrl,
                apkDownloadUrl = apkDownloadUrl,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun parseVersionLabel(vararg labels: String?): String? {
        labels.forEach { label ->
            val match = VERSION_LABEL_REGEX.find(label.orEmpty())
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun compareVersions(latest: String?, current: String): Int {
        val latestParts = parseVersionParts(latest) ?: return 0
        val currentParts = parseVersionParts(current) ?: return 0
        val size = maxOf(latestParts.size, currentParts.size)
        for (index in 0 until size) {
            val left = latestParts.getOrElse(index) { 0 }
            val right = currentParts.getOrElse(index) { 0 }
            if (left != right) return left.compareTo(right)
        }
        return 0
    }

    private fun parseVersionParts(version: String?): List<Int>? {
        val match = VERSION_LABEL_REGEX.find(version.orEmpty()) ?: return null
        return match.groupValues[1]
            .split(".")
            .mapNotNull { it.toIntOrNull() }
            .takeIf { it.isNotEmpty() }
    }

    private fun StringBuilder.appendReadingLines(reading: VoltraReading) {
        appendLine("Battery: ${reading.batteryPercent?.let { "$it%" } ?: "unknown"}")
        appendLine("Device name: ${reading.deviceName ?: "unknown"}")
        appendLine("Firmware: ${reading.firmwareVersion ?: "unknown"}")
        appendLine("Serial: ${reading.serialNumber ?: "unknown"}")
        appendLine("Activation: ${reading.activationState ?: "unknown"}")
        appendLine("Lock: ${reading.lockState ?: "unknown"}")
        appendLine("Child lock: ${reading.childLock?.toString() ?: "unknown"}")
        appendLine("Cable length: ${reading.cableLengthCm?.let { "$it cm" } ?: "unknown"}")
        appendLine("Cable offset: ${reading.cableOffsetCm?.let { "$it cm" } ?: "unknown"}")
        appendLine("Force: ${reading.forceLb?.let { "$it lb" } ?: "unknown"}")
        appendLine("Weight: ${reading.weightLb?.let { "$it lb" } ?: "unknown"}")
        appendLine("Max target load: ${reading.maxTargetLoadLb?.let { "$it lb" } ?: "unknown"}")
        appendLine("Supports Overdrive 250 lb: ${reading.supportsOverdrive250Lb?.toString() ?: "unknown"}")
        appendLine("Feature list 01: ${reading.featureList01Raw?.let { "0x${it.toString(16).uppercase(Locale.US)}" } ?: "unknown"}")
        appendLine("Feature list 02: ${reading.featureList02Raw?.let { "0x${it.toString(16).uppercase(Locale.US)}" } ?: "unknown"}")
        appendLine("Overdrive available: ${reading.overdriveAvailable?.toString() ?: "unknown"}")
        appendLine("Overdrive active status: ${reading.overdriveActiveStatus ?: "unknown"}")
        appendLine("Overdrive configured max force: ${reading.overdriveUserConfiguredMaxForceLb?.let { "$it lb" } ?: "unknown"}")
        appendLine("Max allowed force: ${reading.maxAllowedForceLb?.let { "$it lb" } ?: "unknown"}")
        appendLine("Max chains percent: ${reading.maxChainsPercent ?: "unknown"}")
        appendLine("Max eccentric percent: ${reading.maxEccentricPercent ?: "unknown"}")
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
        appendLine("Screen state: ${VoltraControlFrames.screenStateLabel(reading.appCurrentScreenId, reading.fitnessOngoingUi) ?: "unknown"}")
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
        appendLine("Max target load lb: ${safety.maxTargetLoadLb}")
        appendLine("Supports Overdrive 250 lb: ${safety.supportsOverdrive250Lb}")
        appendLine("Overdrive available: ${safety.overdriveAvailable ?: "unknown"}")
        appendLine("Overdrive active status: ${safety.overdriveActiveStatus ?: "unknown"}")
        appendLine("Overdrive configured max force: ${safety.overdriveUserConfiguredMaxForceLb?.let { "$it lb" } ?: "unknown"}")
        appendLine("Reasons: ${safety.reasons.joinToString().ifBlank { "none" }}")
    }

    private fun VoltraSessionState.maxTargetLoadLbForUi(): Double {
        return safety.maxTargetLoadLb.coerceIn(
            VoltraControlFrames.MAX_TARGET_LB.toDouble(),
            VoltraControlFrames.MAX_OVERDRIVE_TARGET_LB.toDouble(),
        )
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

    private suspend fun rememberReportedDeviceName(current: VoltraSessionState) {
        val deviceId = current.currentDevice?.id ?: return
        val reportedName = current.reading.deviceName?.trim()?.takeIf { it.isNotBlank() } ?: return
        val key = "$deviceId|$reportedName"
        if (key == lastRememberedReportedDeviceKey) return
        preferencesRepository.rememberDevice(deviceId, reportedName)
        lastRememberedReportedDeviceKey = key
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
            deviceName = current.reading.deviceName ?: current.currentDevice?.name ?: preferences.value.lastDeviceName,
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
            deviceName = current.reading.deviceName ?: current.currentDevice?.name ?: draft.deviceName,
            modeLabel = draft.modeLabel,
            primarySetting = draft.primarySetting ?: primarySettingSummary(draft.mode, current),
            reps = draft.reps,
            sets = draft.sets,
            peakForceN = draft.peakForceN,
            peakForceLb = draft.peakForceLb,
            peakPowerWatts = draft.peakPowerWatts,
            timeToPeakMillis = draft.timeToPeakMillis,
            rowingDistanceMeters = draft.rowingDistanceMeters,
            rowingElapsedMillis = draft.rowingElapsedMillis,
            rowingPace500Millis = draft.rowingPace500Millis,
            rowingAveragePace500Millis = draft.rowingAveragePace500Millis,
            rowingStrokeRateSpm = draft.rowingStrokeRateSpm,
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
            appendLine(
                "started_at,ended_at,duration_seconds,device,mode,primary_setting,sets,reps," +
                    "peak_force_n,peak_force_lb,peak_power_w,time_to_peak_ms," +
                    "rowing_distance_m,rowing_elapsed_ms,rowing_pace_500_ms,rowing_avg_pace_500_ms,rowing_stroke_rate_spm," +
                    "battery_start_percent,battery_end_percent",
            )
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
                        csv(entry.peakForceLb?.let { "%.1f".format(Locale.US, it) }.orEmpty()),
                        csv(entry.peakPowerWatts?.toString().orEmpty()),
                        csv(entry.timeToPeakMillis?.toString().orEmpty()),
                        csv(entry.rowingDistanceMeters?.let { "%.1f".format(Locale.US, it) }.orEmpty()),
                        csv(entry.rowingElapsedMillis?.toString().orEmpty()),
                        csv(entry.rowingPace500Millis?.toString().orEmpty()),
                        csv(entry.rowingAveragePace500Millis?.toString().orEmpty()),
                        csv(entry.rowingStrokeRateSpm?.toString().orEmpty()),
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
            ControlModeUi.SKI -> buildList {
                reading.rowingDistanceMeters?.let { add("${trimToLabel(it)} m") }
                reading.rowingPace500Millis?.let { add("${formatPaceForHistory(it)} pace") }
            }.joinToString(" | ").ifBlank { "Ski" }
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
        ControlModeUi.ISOMETRIC_TEST -> "Isometric"
        ControlModeUi.CUSTOM_CURVE -> "Custom Curve"
        ControlModeUi.ROWING -> "Rowing"
        ControlModeUi.SKI -> "Ski"
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
        val peakForceLb: Double? = null,
        val peakPowerWatts: Int? = null,
        val timeToPeakMillis: Long? = null,
        val rowingDistanceMeters: Double? = null,
        val rowingElapsedMillis: Long? = null,
        val rowingPace500Millis: Long? = null,
        val rowingAveragePace500Millis: Long? = null,
        val rowingStrokeRateSpm: Int? = null,
    ) {
        fun updatedWith(current: VoltraSessionState): ActiveWorkoutDraft {
            val reading = current.reading
            return copy(
                deviceName = reading.deviceName ?: current.currentDevice?.name ?: deviceName,
                primarySetting = primarySetting ?: current.reading.workoutMode,
                batteryEndPercent = reading.batteryPercent ?: batteryEndPercent,
                reps = maxOf(reps, reading.repCount ?: 0),
                sets = maxOf(sets, reading.setCount ?: 0),
                peakForceN = maxNullable(peakForceN, reading.isometricPeakForceN),
                peakForceLb = maxNullable(peakForceLb, reading.workoutPeakForceLb),
                peakPowerWatts = maxNullableInt(peakPowerWatts, reading.workoutPeakPowerWatts),
                timeToPeakMillis = reading.workoutTimeToPeakMillis ?: timeToPeakMillis,
                rowingDistanceMeters = maxNullable(rowingDistanceMeters, reading.rowingDistanceMeters),
                rowingElapsedMillis = maxNullableLong(rowingElapsedMillis, reading.rowingElapsedMillis),
                rowingPace500Millis = reading.rowingPace500Millis ?: rowingPace500Millis,
                rowingAveragePace500Millis = reading.rowingAveragePace500Millis ?: rowingAveragePace500Millis,
                rowingStrokeRateSpm = maxNullableInt(rowingStrokeRateSpm, reading.rowingStrokeRateSpm),
            )
        }

        fun hasActivity(): Boolean =
            reps > 0 ||
                sets > 0 ||
                peakForceN != null ||
                peakForceLb != null ||
                peakPowerWatts != null ||
                (rowingDistanceMeters ?: 0.0) > 0.0 ||
                (rowingElapsedMillis ?: 0L) > 0L

        private fun maxNullable(left: Double?, right: Double?): Double? = when {
            left == null -> right
            right == null -> left
            else -> maxOf(left, right)
        }

        private fun maxNullableInt(left: Int?, right: Int?): Int? = when {
            left == null -> right
            right == null -> left
            else -> maxOf(left, right)
        }

        private fun maxNullableLong(left: Long?, right: Long?): Long? = when {
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

private data class GitHubReleaseInfo(
    val tag: String?,
    val version: String?,
    val releaseUrl: String,
    val apkDownloadUrl: String?,
)

private const val GITHUB_RELEASES_URL =
    "https://github.com/dylanmaniatakes/Beyond-Power-Voltra-Android/releases"
private const val GITHUB_RELEASES_LATEST_URL =
    "https://api.github.com/repos/dylanmaniatakes/Beyond-Power-Voltra-Android/releases/latest"
private val VERSION_LABEL_REGEX = Regex("""(?i)(\d+(?:\.\d+){0,2})""")
