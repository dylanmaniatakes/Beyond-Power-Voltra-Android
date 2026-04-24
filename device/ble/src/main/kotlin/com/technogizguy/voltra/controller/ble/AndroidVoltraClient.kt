package com.technogizguy.voltra.controller.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.technogizguy.voltra.controller.model.GattProperty
import com.technogizguy.voltra.controller.model.RawFrameDirection
import com.technogizguy.voltra.controller.model.VoltraClient
import com.technogizguy.voltra.controller.model.VoltraCharacteristicRole
import com.technogizguy.voltra.controller.model.VoltraCommandResult
import com.technogizguy.voltra.controller.model.VoltraCommandStatus
import com.technogizguy.voltra.controller.model.VoltraConnectionState
import com.technogizguy.voltra.controller.model.VoltraControlCommand
import com.technogizguy.voltra.controller.model.VoltraDevice
import com.technogizguy.voltra.controller.model.VoltraGattCharacteristic
import com.technogizguy.voltra.controller.model.VoltraGattService
import com.technogizguy.voltra.controller.model.VoltraGattSnapshot
import com.technogizguy.voltra.controller.model.VoltraProtocolStatus
import com.technogizguy.voltra.controller.model.VoltraReading
import com.technogizguy.voltra.controller.model.VoltraSafetyState
import com.technogizguy.voltra.controller.model.VoltraScanResult
import com.technogizguy.voltra.controller.model.VoltraSessionState
import com.technogizguy.voltra.controller.model.Weight
import com.technogizguy.voltra.controller.model.WeightUnit
import com.technogizguy.voltra.controller.protocol.VoltraBootstrapPacket
import com.technogizguy.voltra.controller.protocol.VoltraControlFrames
import com.technogizguy.voltra.controller.protocol.VoltraFrameAssembler
import com.technogizguy.voltra.controller.protocol.VoltraFrameBuilder
import com.technogizguy.voltra.controller.protocol.VoltraPacketParser
import com.technogizguy.voltra.controller.protocol.toHexString
import com.technogizguy.voltra.controller.protocol.VoltraNotificationParser
import com.technogizguy.voltra.controller.protocol.VoltraOfficialReadOnlyBootstrap
import com.technogizguy.voltra.controller.protocol.VoltraUuidRegistry
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

class AndroidVoltraClient(
    context: Context,
) : VoltraClient {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scanDevices = ConcurrentHashMap<String, VoltraScanResult>()
    private val nativeDevices = ConcurrentHashMap<String, BluetoothDevice>()
    private val descriptorQueue = ArrayDeque<DescriptorWrite>()
    private val readQueue = ArrayDeque<CharacteristicRead>()
    private val writeQueue = ArrayDeque<CharacteristicWrite>()
    private val notificationAssemblers = ConcurrentHashMap<String, VoltraFrameAssembler>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var showAllScanResults = true
    private var gatt: BluetoothGatt? = null
    private var runReadOnlyBootstrapAfterSubscribe = false
    private var serviceDiscoveryStarted = false
    private var controlSeq: Int = 0
    private var inFlightWrite: CharacteristicWrite? = null
    private var isometricVendorRefreshRunnable: Runnable? = null
    private var pendingIsometricAutoLoad = false
    private var pendingIsometricAutoLoadRunnable: Runnable? = null
    private var pendingIsometricAutoLoadAttempts = 0
    private var pendingIsometricLoadIssued = false
    private var lastIsometricEnterAtMillis = 0L
    private var lastIsometricLoadAttemptAtMillis = 0L
    private var lastIsometricVendorRefreshAtMillis = 0L
    private var isometricVendorRefreshUntilMillis = 0L
    private var pendingStartupImageChunkCount = 0
    private var startupImageAckedChunkCount = 0
    private val startupImageStatePollRunnables = mutableListOf<Runnable>()

    private val mutableState = MutableStateFlow(VoltraSessionState())
    override val state: StateFlow<VoltraSessionState> = mutableState

    fun setShowAllScanResults(showAll: Boolean) {
        showAllScanResults = showAll
    }

    @SuppressLint("MissingPermission")
    override fun scan(): Flow<List<VoltraScanResult>> = callbackFlow {
        if (!hasScanPermission()) {
            mutableState.update {
                it.copy(
                    connectionState = VoltraConnectionState.FAILED,
                    statusMessage = "Required Bluetooth scan permission is missing. Grant Nearby devices and try again.",
                )
            }
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        if (adapter?.isEnabled != true) {
            mutableState.update {
                it.copy(
                    connectionState = VoltraConnectionState.FAILED,
                    statusMessage = "Bluetooth is off. Turn Bluetooth on, then scan again.",
                )
            }
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            mutableState.update {
                it.copy(
                    connectionState = VoltraConnectionState.FAILED,
                    statusMessage = "Bluetooth LE scanner is not available on this device right now.",
                )
            }
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                handleScanResult(result)
                trySend(sortedScanResults())
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach(::handleScanResult)
                trySend(sortedScanResults())
            }

            override fun onScanFailed(errorCode: Int) {
                mutableState.update {
                    it.copy(
                        connectionState = VoltraConnectionState.FAILED,
                        statusMessage = "BLE scan failed with code $errorCode.",
                    )
                }
                close()
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        mutableState.update { it.copy(connectionState = VoltraConnectionState.SCANNING, statusMessage = "Scanning for VOLTRA devices.") }
        scanner.startScan(null, settings, callback)
        trySend(sortedScanResults())

        awaitClose {
            runCatching { scanner.stopScan(callback) }
            mutableState.update { current ->
                if (current.connectionState == VoltraConnectionState.SCANNING) {
                    current.copy(connectionState = VoltraConnectionState.IDLE, statusMessage = "Scan stopped.")
                } else {
                    current
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(deviceId: String) {
        if (!hasConnectPermission()) {
            mutableState.update { it.copy(connectionState = VoltraConnectionState.FAILED, statusMessage = "Bluetooth connect permission is missing.") }
            return
        }

        val bluetoothDevice = nativeDevices[deviceId] ?: runCatching { adapter?.getRemoteDevice(deviceId) }.getOrNull()
        if (bluetoothDevice == null) {
            mutableState.update { it.copy(connectionState = VoltraConnectionState.FAILED, statusMessage = "Device $deviceId is not known yet.") }
            return
        }

        val scanDevice = scanDevices[deviceId]?.device ?: bluetoothDevice.toVoltraDevice(null, emptyList(), null)
        mutableState.update {
            it.copy(
                connectionState = VoltraConnectionState.CONNECTING,
                currentDevice = scanDevice,
                statusMessage = "Connecting to ${scanDevice.name ?: scanDevice.address}.",
                lastDisconnectReason = null,
                connectedAtMillis = null,
                lastDisconnectAtMillis = null,
                lastConnectionDurationMillis = null,
                controlCommandsEnabled = false,
                reading = VoltraReading(),
                safety = VoltraSafetyState(),
            )
        }

        gatt?.close()
        notificationAssemblers.clear()
        serviceDiscoveryStarted = false
        controlSeq = 0
        gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothDevice.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            @Suppress("DEPRECATION")
            bluetoothDevice.connectGatt(appContext, false, gattCallback)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        mutableState.update { it.copy(connectionState = VoltraConnectionState.DISCONNECTING, statusMessage = "Disconnecting.") }
        runCatching { gatt?.disconnect() }
        runCatching { gatt?.close() }
        gatt = null
        descriptorQueue.clear()
        readQueue.clear()
        writeQueue.clear()
        notificationAssemblers.clear()
        serviceDiscoveryStarted = false
        cancelIsometricVendorRefreshBurst()
        mainHandler.removeCallbacksAndMessages(null)
        runReadOnlyBootstrapAfterSubscribe = false
        val disconnectedAt = System.currentTimeMillis()
        mutableState.update {
            it.copy(
                connectionState = VoltraConnectionState.DISCONNECTED,
                statusMessage = "Disconnected.",
                lastDisconnectReason = "Disconnected by app.",
                lastDisconnectAtMillis = disconnectedAt,
                lastConnectionDurationMillis = it.connectedAtMillis?.let { connectedAt ->
                    disconnectedAt - connectedAt
                },
                controlCommandsEnabled = false,
            )
        }
    }

    override suspend fun setTargetLoad(weight: Weight): VoltraCommandResult {
        val capped = weight.cappedForV1()
        mutableState.update { it.copy(targetLoad = capped) }
        val current = mutableState.value
        val currentGatt = gatt
        val targetLb = capped.toCommandPounds(
            min = VoltraControlFrames.MIN_TARGET_LB,
            max = VoltraControlFrames.MAX_TARGET_LB,
        )
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.SET_TARGET_LOAD, "Target stored locally as ${capped.display()}; control protocol has not been validated in this session.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.SET_TARGET_LOAD, "Cannot set target while the VOLTRA is not connected.")
            currentGatt == null ->
                blocked(VoltraControlCommand.SET_TARGET_LOAD, "No active GATT connection.")
            else -> sendParamWriteCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.SET_TARGET_LOAD,
                paramId = VoltraControlFrames.PARAM_BP_BASE_WEIGHT,
                valueBytes = targetLb.uint16Le(),
                label = "set target load (BP_BASE_WEIGHT=0x${VoltraControlFrames.PARAM_BP_BASE_WEIGHT.toString(16).uppercase()}, ${targetLb} lb)",
            )
        }
    }

    override suspend fun setAssistMode(enabled: Boolean): VoltraCommandResult {
        return sendExperimentalStrengthSetting(
            command = VoltraControlCommand.SET_ASSIST_MODE,
            paramId = VoltraControlFrames.PARAM_FITNESS_ASSIST_MODE,
            valueBytes = byteArrayOf((if (enabled) 1 else 0).toByte()),
            label = "set assist (${if (enabled) "on" else "off"})",
        )
    }

    override suspend fun setChainsWeight(weight: Weight): VoltraCommandResult {
        val current = mutableState.value
        val baseWeightLb = current.baseWeightLbForStrengthFeatures()
            ?: return blocked(
                command = VoltraControlCommand.SET_CHAINS_WEIGHT,
                message = "Chains command needs a parsed base weight first.",
            )
        val maxChainsLb = current.maxChainsWeightLb(baseWeightLb)
        val targetLb = weight.toCommandPounds(
            min = VoltraControlFrames.MIN_EXTRA_WEIGHT_LB,
            max = maxChainsLb,
        )
        return sendExperimentalStrengthSetting(
            command = VoltraControlCommand.SET_CHAINS_WEIGHT,
            paramId = VoltraControlFrames.PARAM_BP_CHAINS_WEIGHT,
            valueBytes = targetLb.uint16Le(),
            label = "set chains weight (BP_CHAINS_WEIGHT=$targetLb lb, max=$maxChainsLb lb at base $baseWeightLb lb)",
        )
    }

    override suspend fun setEccentricWeight(weight: Weight): VoltraCommandResult {
        val current = mutableState.value
        val baseWeightLb = current.baseWeightLbForStrengthFeatures()
            ?: return blocked(
                command = VoltraControlCommand.SET_ECCENTRIC_WEIGHT,
                message = "Eccentric command needs a parsed base weight first.",
        )
        val minEccentricLb = current.minEccentricWeightLb(baseWeightLb)
        val maxEccentricLb = current.maxEccentricWeightLb(baseWeightLb)
        val targetLb = weight.toCommandPounds(
            min = minEccentricLb,
            max = maxEccentricLb,
        )
        return sendExperimentalStrengthSetting(
            command = VoltraControlCommand.SET_ECCENTRIC_WEIGHT,
            paramId = VoltraControlFrames.PARAM_BP_ECCENTRIC_WEIGHT,
            valueBytes = targetLb.uint16Le(),
            label = "set eccentric weight (BP_ECCENTRIC_WEIGHT=$targetLb lb, range=$minEccentricLb..$maxEccentricLb lb at base $baseWeightLb lb)",
        )
    }

    override suspend fun setInverseChainsEnabled(enabled: Boolean): VoltraCommandResult {
        return sendExperimentalStrengthSetting(
            command = VoltraControlCommand.SET_INVERSE_CHAINS,
            paramId = VoltraControlFrames.PARAM_FITNESS_INVERSE_CHAIN,
            valueBytes = byteArrayOf((if (enabled) 1 else 0).toByte()),
            label = "set inverse chains (${if (enabled) "on" else "off"})",
        )
    }

    override suspend fun setResistanceExperience(intense: Boolean): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.SET_RESISTANCE_EXPERIENCE, "Resistance Experience is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.SET_RESISTANCE_EXPERIENCE, "Cannot change Resistance Experience while the VOLTRA is not connected.")
            current.safety.workoutState == null || current.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_INACTIVE ->
                blocked(VoltraControlCommand.SET_RESISTANCE_EXPERIENCE, "Enter a workout mode before changing Resistance Experience.")
            currentGatt == null ->
                blocked(VoltraControlCommand.SET_RESISTANCE_EXPERIENCE, "No active GATT connection.")
            else -> sendParamWriteCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.SET_RESISTANCE_EXPERIENCE,
                paramId = VoltraControlFrames.PARAM_RESISTANCE_EXPERIENCE,
                valueBytes = byteArrayOf((if (intense) 0 else 1).toByte()),
                label = "set resistance experience (${if (intense) "Intense" else "Standard"})",
                followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
            )
        }
    }

    override suspend fun setResistanceBandInverse(enabled: Boolean): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_INVERSE, "Resistance Mode is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_INVERSE, "Cannot change Resistance Mode while the VOLTRA is not connected.")
            current.safety.workoutState != VoltraControlFrames.WORKOUT_STATE_RESISTANCE_BAND ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_INVERSE, "Enter Resistance Band before changing Resistance Mode.")
            currentGatt == null ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_INVERSE, "No active GATT connection.")
            else -> sendParamWriteCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.SET_RESISTANCE_BAND_INVERSE,
                paramId = VoltraControlFrames.PARAM_EP_RESISTANCE_BAND_INVERSE,
                valueBytes = byteArrayOf((if (enabled) 1 else 0).toByte()),
                label = "set Resistance Band mode (${if (enabled) "Inverse" else "Standard"})",
                followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
            )
        }
    }

    override suspend fun setResistanceBandCurveAlgorithm(logarithm: Boolean): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_CURVE, "Resistance Curve is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_CURVE, "Cannot change Resistance Curve while the VOLTRA is not connected.")
            current.safety.workoutState != VoltraControlFrames.WORKOUT_STATE_RESISTANCE_BAND ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_CURVE, "Enter Resistance Band before changing Resistance Curve.")
            currentGatt == null ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_CURVE, "No active GATT connection.")
            else -> sendParamWriteCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.SET_RESISTANCE_BAND_CURVE,
                paramId = VoltraControlFrames.PARAM_RESISTANCE_BAND_ALGORITHM,
                valueBytes = byteArrayOf((if (logarithm) 1 else 0).toByte()),
                label = "set Resistance Curve (${if (logarithm) "Logarithm" else "Power Law"})",
                followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
            )
        }
    }

    override suspend fun enterResistanceBandMode(): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_MODE, "Resistance Band mode is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_MODE, "Cannot enter Resistance Band while the VOLTRA is not connected.")
            currentGatt == null ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_MODE, "No active GATT connection.")
            else -> sendParamWriteCommands(
                gatt = currentGatt,
                command = VoltraControlCommand.SET_RESISTANCE_BAND_MODE,
                specs = listOf(
                    ParamWriteSpec(
                        paramId = VoltraControlFrames.PARAM_FITNESS_WORKOUT_STATE,
                        valueBytes = byteArrayOf(VoltraControlFrames.WORKOUT_STATE_RESISTANCE_BAND.toByte()),
                        label = "enter Resistance Band (FITNESS_WORKOUT_STATE=2)",
                    ),
                    ParamWriteSpec(
                        paramId = VoltraControlFrames.PARAM_BP_SET_FITNESS_MODE,
                        valueBytes = VoltraControlFrames.FITNESS_MODE_STRENGTH_READY.uint16Le(),
                        label = "ready current mode (BP_SET_FITNESS_MODE=4)",
                    ),
                ),
                label = "enter Resistance Band mode",
                followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
            )
        }
    }

    override suspend fun enterDamperMode(): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.ENTER_DAMPER_MODE, "Damper mode is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.ENTER_DAMPER_MODE, "Cannot enter Damper while the VOLTRA is not connected.")
            currentGatt == null ->
                blocked(VoltraControlCommand.ENTER_DAMPER_MODE, "No active GATT connection.")
            else -> sendParamWriteCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.ENTER_DAMPER_MODE,
                paramId = VoltraControlFrames.PARAM_FITNESS_WORKOUT_STATE,
                valueBytes = byteArrayOf(VoltraControlFrames.WORKOUT_STATE_DAMPER.toByte()),
                label = "enter Damper (FITNESS_WORKOUT_STATE=4)",
                followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
            )
        }
    }

    override suspend fun enterIsokineticMode(): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.ENTER_ISOKINETIC_MODE, "Isokinetic mode is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.ENTER_ISOKINETIC_MODE, "Cannot enter Isokinetic while the VOLTRA is not connected.")
            currentGatt == null ->
                blocked(VoltraControlCommand.ENTER_ISOKINETIC_MODE, "No active GATT connection.")
            else -> sendParamWriteCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.ENTER_ISOKINETIC_MODE,
                paramId = VoltraControlFrames.PARAM_FITNESS_WORKOUT_STATE,
                valueBytes = byteArrayOf(VoltraControlFrames.WORKOUT_STATE_ISOKINETIC.toByte()),
                label = "enter Isokinetic (FITNESS_WORKOUT_STATE=7)",
                followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
            )
        }
    }

    override suspend fun enterIsometricMode(): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.ENTER_ISOMETRIC_MODE, "Isometric Test is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.ENTER_ISOMETRIC_MODE, "Cannot enter Isometric Test while the VOLTRA is not connected.")
            currentGatt == null ->
                blocked(VoltraControlCommand.ENTER_ISOMETRIC_MODE, "No active GATT connection.")
            hasPendingCommand(VoltraControlCommand.ENTER_ISOMETRIC_MODE) ->
                logCommand(
                    VoltraCommandResult(
                        command = VoltraControlCommand.ENTER_ISOMETRIC_MODE,
                        status = VoltraCommandStatus.QUEUED,
                        message = "Isometric Test entry is already queued.",
                        timestampMillis = System.currentTimeMillis(),
                    ),
                )
            else -> {
                cancelIsometricVendorRefreshBurst()
                stopPendingIsometricAutoLoadLoop(resetLoadIssued = true)
                mutableState.update {
                    it.copy(
                        reading = it.reading.clearIsometricTestState().copy(
                            workoutMode = "Isometric Test, Ready",
                        ),
                        safety = it.safety.copy(
                            canLoad = true,
                            reasons = listOf("Ready for current mode load."),
                            parsedDeviceState = true,
                            workoutState = VoltraControlFrames.WORKOUT_STATE_ISOMETRIC,
                            fitnessMode = VoltraControlFrames.FITNESS_MODE_STRENGTH_READY,
                            targetLoadLb = it.safety.targetLoadLb ?: it.reading.weightLb,
                        ),
                    )
                }
                val result = sendTransportFrames(
                    gatt = currentGatt,
                    command = VoltraControlCommand.ENTER_ISOMETRIC_MODE,
                    frames = buildList {
                        add(
                            QueuedFrameSpec(
                                label = "enter Isometric Test (FITNESS_WORKOUT_STATE=8)",
                                bytes = VoltraFrameBuilder.build(
                                    cmd = VoltraControlFrames.CMD_PARAM_WRITE,
                                    payload = VoltraControlFrames.enterIsometricPayload(),
                                    seq = controlSeq++,
                                ),
                            ),
                        )
                    },
                    label = "enter Isometric Test (FITNESS_WORKOUT_STATE=8)",
                )
                pendingIsometricAutoLoad = true
                if (result.status != VoltraCommandStatus.BLOCKED && result.status != VoltraCommandStatus.FAILED) {
                    lastIsometricEnterAtMillis = System.currentTimeMillis()
                    pendingIsometricLoadIssued = false
                    schedulePendingIsometricAutoLoad(currentGatt)
                } else {
                    pendingIsometricAutoLoad = false
                    lastIsometricEnterAtMillis = 0L
                }
                result
            }
        }
    }

    override suspend fun enterCustomCurveMode(): VoltraCommandResult {
        return queueCustomCurveMode(
            command = VoltraControlCommand.ENTER_CUSTOM_CURVE_MODE,
            curvePoints = VoltraControlFrames.DEFAULT_CUSTOM_CURVE_POINTS,
            resistanceMinLb = VoltraControlFrames.DEFAULT_CUSTOM_CURVE_RESISTANCE_MIN_LB,
            resistanceLimitLb = VoltraControlFrames.DEFAULT_CUSTOM_CURVE_RESISTANCE_LIMIT_LB,
            rangeOfMotionIn = VoltraControlFrames.DEFAULT_CUSTOM_CURVE_RANGE_OF_MOTION_IN,
            duplicateMessage = "Custom Curve entry is already queued.",
            label = "apply Custom Curve",
            vendorFrameLabel = "upload Custom Curve",
        )
    }

    override suspend fun applyCustomCurve(
        points: List<Float>,
        resistanceMinLb: Int,
        resistanceLimitLb: Int,
        rangeOfMotionIn: Int,
    ): VoltraCommandResult {
        return queueCustomCurveMode(
            command = VoltraControlCommand.APPLY_CUSTOM_CURVE,
            curvePoints = points,
            resistanceMinLb = resistanceMinLb,
            resistanceLimitLb = resistanceLimitLb,
            rangeOfMotionIn = rangeOfMotionIn,
            duplicateMessage = "Custom Curve apply is already queued.",
            label = "apply Custom Curve builder graph",
            vendorFrameLabel = "upload Custom Curve builder graph",
        )
    }

    private fun queueCustomCurveMode(
        command: VoltraControlCommand,
        curvePoints: List<Float>,
        resistanceMinLb: Int,
        resistanceLimitLb: Int,
        rangeOfMotionIn: Int,
        duplicateMessage: String,
        label: String,
        vendorFrameLabel: String,
    ): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        return when {
            !current.controlCommandsEnabled ->
                blocked(command, "Custom Curve is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(command, "Cannot enter Custom Curve while the VOLTRA is not connected.")
            currentGatt == null ->
                blocked(command, "No active GATT connection.")
            hasPendingCommand(command) ->
                logCommand(
                    VoltraCommandResult(
                        command = command,
                        status = VoltraCommandStatus.QUEUED,
                        message = duplicateMessage,
                        timestampMillis = System.currentTimeMillis(),
                    ),
                )
            else -> {
                cancelIsometricVendorRefreshBurst()
                pendingIsometricAutoLoad = false
                stopPendingIsometricAutoLoadLoop(resetLoadIssued = true)
                mutableState.update {
                    it.copy(
                        reading = it.reading.clearIsometricTestState().copy(
                            workoutMode = "Custom Curve, Ready",
                            forceLb = null,
                            setCount = 0,
                            repCount = 0,
                            repPhase = "Ready",
                        ),
                        safety = it.safety.copy(
                            canLoad = true,
                            reasons = listOf("Ready for current mode load."),
                            parsedDeviceState = true,
                            workoutState = VoltraControlFrames.WORKOUT_STATE_CUSTOM_CURVE,
                            fitnessMode = VoltraControlFrames.FITNESS_MODE_STRENGTH_READY,
                            targetLoadLb = it.safety.targetLoadLb ?: it.reading.weightLb,
                        ),
                    )
                }
                sendTransportFrames(
                    gatt = currentGatt,
                    command = command,
                    frames = buildList {
                        add(
                            QueuedFrameSpec(
                                label = "subscribe Custom Curve fitness data stream",
                                bytes = VoltraFrameBuilder.build(
                                    cmd = VoltraControlFrames.CMD_PARAM_WRITE,
                                    payload = VoltraControlFrames.setFitnessDataNotifySubscribePayload(),
                                    seq = controlSeq++,
                                ),
                            ),
                        )
                        add(
                            QueuedFrameSpec(
                                label = "bulk subscribe Custom Curve params",
                                bytes = VoltraFrameBuilder.build(
                                    cmd = VoltraControlFrames.CMD_BULK_PARAM_WRITE,
                                    payload = VoltraControlFrames.customCurveBulkSubscribePayload(),
                                    seq = controlSeq++,
                                ),
                            ),
                        )
                        add(
                            QueuedFrameSpec(
                                label = "set Custom Curve fitness data notify hz",
                                bytes = VoltraFrameBuilder.build(
                                    cmd = VoltraControlFrames.CMD_PARAM_WRITE,
                                    payload = VoltraControlFrames.setFitnessDataNotifyHzPayload(),
                                    seq = controlSeq++,
                                ),
                            ),
                        )
                        add(
                            QueuedFrameSpec(
                                label = "set Custom Curve resistance min (BP_BASE_WEIGHT=$resistanceMinLb lb)",
                                bytes = VoltraFrameBuilder.build(
                                    cmd = VoltraControlFrames.CMD_PARAM_WRITE,
                                    payload = VoltraControlFrames.setBaseWeightPayload(resistanceMinLb),
                                    seq = controlSeq++,
                                ),
                            ),
                        )
                        add(
                            QueuedFrameSpec(
                                label = vendorFrameLabel,
                                bytes = VoltraFrameBuilder.build(
                                    cmd = VoltraControlFrames.CMD_VENDOR,
                                    payload = VoltraControlFrames.customCurveVendorPresetPayload(
                                        points = curvePoints,
                                        resistanceMinLb = resistanceMinLb,
                                        resistanceLimitLb = resistanceLimitLb,
                                        rangeOfMotionIn = rangeOfMotionIn,
                                    ),
                                    seq = controlSeq++,
                                ),
                            ),
                        )
                        add(
                            QueuedFrameSpec(
                                label = "enter Custom Curve (FITNESS_WORKOUT_STATE=6)",
                                bytes = VoltraFrameBuilder.build(
                                    cmd = VoltraControlFrames.CMD_PARAM_WRITE,
                                    payload = VoltraControlFrames.enterCustomCurvePayload(),
                                    seq = controlSeq++,
                                ),
                            ),
                        )
                        add(
                            QueuedFrameSpec(
                                label = "read back Custom Curve mode feature state",
                                bytes = VoltraFrameBuilder.build(
                                    cmd = VoltraControlFrames.CMD_PARAM_READ,
                                    payload = VoltraControlFrames.readParamsPayload(*MODE_FEATURE_STATUS_PARAMS.toIntArray()),
                                    seq = controlSeq++,
                                ),
                            ),
                        )
                    },
                    label = label,
                )
            }
        }
    }

    override suspend fun setDamperLevel(level: Int): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        val levelIndex = level.coerceIn(0, 9)
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.SET_DAMPER_LEVEL, "Damper level is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.SET_DAMPER_LEVEL, "Cannot set Damper while the VOLTRA is not connected.")
            current.safety.workoutState != VoltraControlFrames.WORKOUT_STATE_DAMPER ->
                blocked(VoltraControlCommand.SET_DAMPER_LEVEL, "Enter Damper before setting its level.")
            currentGatt == null ->
                blocked(VoltraControlCommand.SET_DAMPER_LEVEL, "No active GATT connection.")
            else -> sendParamWriteCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.SET_DAMPER_LEVEL,
                paramId = VoltraControlFrames.PARAM_FITNESS_DAMPER_RATIO_IDX,
                valueBytes = byteArrayOf(levelIndex.toByte()),
                label = "set Damper level (FITNESS_DAMPER_RATIO_IDX=$levelIndex)",
                followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
            )
        }
    }

    override suspend fun setIsokineticMenu(mode: Int): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        val normalizedMode = when (mode) {
            VoltraControlFrames.ISOKINETIC_MENU_CONSTANT_RESISTANCE -> VoltraControlFrames.ISOKINETIC_MENU_CONSTANT_RESISTANCE
            else -> VoltraControlFrames.ISOKINETIC_MENU_ISOKINETIC
        }
        val modeLabel = if (normalizedMode == VoltraControlFrames.ISOKINETIC_MENU_CONSTANT_RESISTANCE) {
            "Constant Resistance"
        } else {
            "Isokinetic"
        }
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.SET_ISOKINETIC_MENU, "Isokinetic submenu is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.SET_ISOKINETIC_MENU, "Cannot change the Isokinetic submenu while the VOLTRA is not connected.")
            !VoltraControlFrames.isIsokineticWorkoutState(current.safety.workoutState) ->
                blocked(VoltraControlCommand.SET_ISOKINETIC_MENU, "Enter Isokinetic before changing its submenu.")
            currentGatt == null ->
                blocked(VoltraControlCommand.SET_ISOKINETIC_MENU, "No active GATT connection.")
            else -> sendParamWriteCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.SET_ISOKINETIC_MENU,
                paramId = VoltraControlFrames.PARAM_ISOKINETIC_ECC_MODE,
                valueBytes = byteArrayOf(normalizedMode.toByte()),
                label = "set Isokinetic submenu ($modeLabel)",
                followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
            )
        }
    }

    override suspend fun setIsokineticSpeedLimitMmS(speedMmS: Int): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        val normalizedSpeed = when {
            speedMmS <= 0 -> VoltraControlFrames.AUTO_ISOKINETIC_SPEED_MM_S
            else -> speedMmS.coerceIn(
                VoltraControlFrames.MIN_ISOKINETIC_SPEED_MM_S,
                VoltraControlFrames.MAX_ISOKINETIC_SPEED_MM_S,
            )
        }
        val speedLabel = if (normalizedSpeed == VoltraControlFrames.AUTO_ISOKINETIC_SPEED_MM_S) {
            "Auto"
        } else {
            "${normalizedSpeed / 1000.0} m/s"
        }
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.SET_ISOKINETIC_SPEED, "Isokinetic speed is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.SET_ISOKINETIC_SPEED, "Cannot change Isokinetic speed while the VOLTRA is not connected.")
            !VoltraControlFrames.isIsokineticWorkoutState(current.safety.workoutState) ->
                blocked(VoltraControlCommand.SET_ISOKINETIC_SPEED, "Enter Isokinetic before changing its speed.")
            currentGatt == null ->
                blocked(VoltraControlCommand.SET_ISOKINETIC_SPEED, "No active GATT connection.")
            else -> sendParamWriteCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.SET_ISOKINETIC_SPEED,
                paramId = VoltraControlFrames.PARAM_ISOKINETIC_ECC_SPEED_LIMIT,
                valueBytes = normalizedSpeed.uint16Le(),
                label = "set Isokinetic eccentric speed limit ($speedLabel)",
                followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
            )
        }
    }

    override suspend fun setIsokineticTargetSpeedMmS(speedMmS: Int): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        val normalizedSpeed = speedMmS.coerceIn(
            VoltraControlFrames.MIN_ISOKINETIC_SPEED_MM_S,
            VoltraControlFrames.MAX_ISOKINETIC_SPEED_MM_S,
        )
        val speedLabel = "${normalizedSpeed / 1000.0} m/s"
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.SET_ISOKINETIC_TARGET_SPEED, "Isokinetic target speed is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.SET_ISOKINETIC_TARGET_SPEED, "Cannot change Isokinetic target speed while the VOLTRA is not connected.")
            !VoltraControlFrames.isIsokineticWorkoutState(current.safety.workoutState) ->
                blocked(VoltraControlCommand.SET_ISOKINETIC_TARGET_SPEED, "Enter Isokinetic before changing its target speed.")
            currentGatt == null ->
                blocked(VoltraControlCommand.SET_ISOKINETIC_TARGET_SPEED, "No active GATT connection.")
            else -> sendParamWriteCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.SET_ISOKINETIC_TARGET_SPEED,
                paramId = VoltraControlFrames.PARAM_EP_ISOKINETIC_TARGET_SPEED_MM_S,
                valueBytes = normalizedSpeed.uint32Le(),
                label = "set Isokinetic target speed ($speedLabel)",
                followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
            )
        }
    }

    override suspend fun setIsokineticConstantResistance(weight: Weight): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        val targetLb = weight.toCommandPounds(
            min = VoltraControlFrames.MIN_ISOKINETIC_CONSTANT_RESISTANCE_LB,
            max = VoltraControlFrames.MAX_ISOKINETIC_CONSTANT_RESISTANCE_LB,
        )
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.SET_ISOKINETIC_CONSTANT_RESISTANCE, "Isokinetic constant resistance is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.SET_ISOKINETIC_CONSTANT_RESISTANCE, "Cannot change constant resistance while the VOLTRA is not connected.")
            !VoltraControlFrames.isIsokineticWorkoutState(current.safety.workoutState) ->
                blocked(VoltraControlCommand.SET_ISOKINETIC_CONSTANT_RESISTANCE, "Enter Isokinetic before changing constant resistance.")
            currentGatt == null ->
                blocked(VoltraControlCommand.SET_ISOKINETIC_CONSTANT_RESISTANCE, "No active GATT connection.")
            else -> sendParamWriteCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.SET_ISOKINETIC_CONSTANT_RESISTANCE,
                paramId = VoltraControlFrames.PARAM_ISOKINETIC_ECC_CONST_WEIGHT,
                valueBytes = targetLb.uint16Le(),
                label = "set Isokinetic constant resistance ($targetLb lb)",
                followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
            )
        }
    }

    override suspend fun setIsokineticMaxEccentricLoad(weight: Weight): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        val targetLb = weight.toCommandPounds(
            min = VoltraControlFrames.MIN_ISOKINETIC_MAX_ECCENTRIC_LOAD_LB,
            max = VoltraControlFrames.MAX_ISOKINETIC_MAX_ECCENTRIC_LOAD_LB,
        )
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.SET_ISOKINETIC_MAX_ECCENTRIC_LOAD, "Isokinetic max eccentric load is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.SET_ISOKINETIC_MAX_ECCENTRIC_LOAD, "Cannot change max eccentric load while the VOLTRA is not connected.")
            !VoltraControlFrames.isIsokineticWorkoutState(current.safety.workoutState) ->
                blocked(VoltraControlCommand.SET_ISOKINETIC_MAX_ECCENTRIC_LOAD, "Enter Isokinetic before changing max eccentric load.")
            currentGatt == null ->
                blocked(VoltraControlCommand.SET_ISOKINETIC_MAX_ECCENTRIC_LOAD, "No active GATT connection.")
            else -> sendParamWriteCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.SET_ISOKINETIC_MAX_ECCENTRIC_LOAD,
                paramId = VoltraControlFrames.PARAM_ISOKINETIC_ECC_OVERLOAD_WEIGHT,
                valueBytes = targetLb.uint16Le(),
                label = "set Isokinetic max eccentric load ($targetLb lb)",
                followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
            )
        }
    }

    override suspend fun setResistanceBandMaxForce(weight: Weight): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        val targetLb = weight.toCommandPounds(
            min = VoltraControlFrames.MIN_RESISTANCE_BAND_FORCE_LB,
            max = VoltraControlFrames.MAX_RESISTANCE_BAND_FORCE_LB,
        )
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_FORCE, "Resistance Band force is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_FORCE, "Cannot set Resistance Band force while the VOLTRA is not connected.")
            current.safety.workoutState != VoltraControlFrames.WORKOUT_STATE_RESISTANCE_BAND ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_FORCE, "Enter Resistance Band before setting its force.")
            currentGatt == null ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_FORCE, "No active GATT connection.")
            else -> sendParamWriteCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.SET_RESISTANCE_BAND_FORCE,
                paramId = VoltraControlFrames.PARAM_RESISTANCE_BAND_MAX_FORCE,
                valueBytes = targetLb.uint16Le(),
                label = "set Resistance Band max force (RESISTANCE_BAND_MAX_FORCE=$targetLb lb)",
                followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
            )
        }
    }

    override suspend fun setResistanceBandByRangeOfMotion(enabled: Boolean): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_PROGRESSIVE_LENGTH_MODE, "Resistance progressive length mode is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_PROGRESSIVE_LENGTH_MODE, "Cannot change Resistance progressive length mode while the VOLTRA is not connected.")
            current.safety.workoutState != VoltraControlFrames.WORKOUT_STATE_RESISTANCE_BAND ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_PROGRESSIVE_LENGTH_MODE, "Enter Resistance Band before changing progressive length mode.")
            currentGatt == null ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_PROGRESSIVE_LENGTH_MODE, "No active GATT connection.")
            else -> sendParamWriteCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.SET_RESISTANCE_BAND_PROGRESSIVE_LENGTH_MODE,
                paramId = VoltraControlFrames.PARAM_RESISTANCE_BAND_LEN_BY_ROM,
                valueBytes = byteArrayOf((if (enabled) 1 else 0).toByte()),
                label = "set Resistance progressive length (${if (enabled) "ROM" else "Band Length"})",
                followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
            )
        }
    }

    override suspend fun setResistanceBandLengthCm(lengthCm: Int): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        val targetCm = lengthCm.coerceIn(
            VoltraControlFrames.MIN_RESISTANCE_BAND_LENGTH_CM,
            VoltraControlFrames.MAX_RESISTANCE_BAND_LENGTH_CM,
        )
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_LENGTH, "Resistance Band length is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_LENGTH, "Cannot set Resistance Band length while the VOLTRA is not connected.")
            current.safety.workoutState != VoltraControlFrames.WORKOUT_STATE_RESISTANCE_BAND ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_LENGTH, "Enter Resistance Band before setting its length.")
            currentGatt == null ->
                blocked(VoltraControlCommand.SET_RESISTANCE_BAND_LENGTH, "No active GATT connection.")
            else -> sendParamWriteCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.SET_RESISTANCE_BAND_LENGTH,
                paramId = VoltraControlFrames.PARAM_RESISTANCE_BAND_LEN,
                valueBytes = targetCm.uint16Le(),
                label = "set Resistance Band length (RESISTANCE_BAND_LEN=$targetCm cm)",
                followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
            )
        }
    }

    override suspend fun loadResistanceBand(): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.LOAD_RESISTANCE_BAND, "Resistance Band load is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.LOAD_RESISTANCE_BAND, "Cannot load Resistance Band while the VOLTRA is not connected.")
            current.safety.workoutState != VoltraControlFrames.WORKOUT_STATE_RESISTANCE_BAND ->
                blocked(VoltraControlCommand.LOAD_RESISTANCE_BAND, "Enter Resistance Band before loading.")
            VoltraControlFrames.isLoadedFitnessMode(current.safety.fitnessMode) ->
                blocked(VoltraControlCommand.LOAD_RESISTANCE_BAND, "Resistance Band already appears loaded.")
            currentGatt == null ->
                blocked(VoltraControlCommand.LOAD_RESISTANCE_BAND, "No active GATT connection.")
            else -> sendParamWriteCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.LOAD_RESISTANCE_BAND,
                paramId = VoltraControlFrames.PARAM_BP_SET_FITNESS_MODE,
                valueBytes = VoltraControlFrames.FITNESS_MODE_STRENGTH_LOADED.uint16Le(),
                label = "load Resistance Band (BP_SET_FITNESS_MODE=5)",
                followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
            )
        }
    }

    override suspend fun triggerCableLengthMode(): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.TRIGGER_CABLE_LENGTH_MODE, "Cable length trigger is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.TRIGGER_CABLE_LENGTH_MODE, "Cannot trigger cable length mode while the VOLTRA is not connected.")
            current.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_INACTIVE ->
                blocked(VoltraControlCommand.TRIGGER_CABLE_LENGTH_MODE, "Enter Weight Training or Resistance Band before opening Cable Length.")
            currentGatt == null ->
                blocked(VoltraControlCommand.TRIGGER_CABLE_LENGTH_MODE, "No active GATT connection.")
            else -> sendParamWriteCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.TRIGGER_CABLE_LENGTH_MODE,
                paramId = VoltraControlFrames.PARAM_EP_SCR_SWITCH,
                valueBytes = byteArrayOf(0x00, 0x10, 0x00, 0x01),
                label = "trigger cable length mode (EP_SCR_SWITCH=00 10 00 01)",
                followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
            )
        }
    }

    override suspend fun setCableOffsetCm(offsetCm: Int): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        val targetCm = offsetCm.coerceIn(
            VoltraControlFrames.MIN_CABLE_OFFSET_CM,
            VoltraControlFrames.MAX_CABLE_OFFSET_CM,
        )
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.SET_CABLE_OFFSET, "Cable offset testing is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.SET_CABLE_OFFSET, "Cannot set cable offset while the VOLTRA is not connected.")
            currentGatt == null ->
                blocked(VoltraControlCommand.SET_CABLE_OFFSET, "No active GATT connection.")
            else -> sendParamWriteCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.SET_CABLE_OFFSET,
                paramId = VoltraControlFrames.PARAM_MC_DEFAULT_OFFLEN_CM,
                valueBytes = targetCm.uint16Le(),
                label = "set cable offset (MC_DEFAULT_OFFLEN_CM=$targetCm cm)",
                followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
            )
        }
    }

    override suspend fun setDeviceName(name: String): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        val trimmed = name.trim()
        return when {
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.SET_DEVICE_NAME, "Cannot rename the VOLTRA while it is not connected.")
            currentGatt == null ->
                blocked(VoltraControlCommand.SET_DEVICE_NAME, "No active GATT connection.")
            else -> {
                val frameBytes = VoltraFrameBuilder.build(
                    cmd = VoltraControlFrames.CMD_SET_DEVICE_NAME,
                    payload = VoltraControlFrames.setDeviceNamePayload(trimmed),
                    seq = controlSeq++,
                )
                val result = sendCommandFrames(
                    gatt = currentGatt,
                    command = VoltraControlCommand.SET_DEVICE_NAME,
                    frames = listOf(
                        QueuedFrameSpec(
                            label = "set device name ($trimmed)",
                            bytes = frameBytes,
                        ),
                    ),
                    label = "set device name",
                )
                if (result.status != VoltraCommandStatus.BLOCKED && result.status != VoltraCommandStatus.FAILED) {
                    mutableState.update { session ->
                        session.copy(
                            currentDevice = session.currentDevice?.copy(name = trimmed),
                            statusMessage = "Queued device rename to \"$trimmed\".",
                        )
                    }
                }
                result
            }
        }
    }

    override suspend fun uploadStartupImage(jpegBytes: ByteArray): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        return when {
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.UPLOAD_STARTUP_IMAGE, "Cannot upload a startup image while the VOLTRA is not connected.")
            currentGatt == null ->
                blocked(VoltraControlCommand.UPLOAD_STARTUP_IMAGE, "No active GATT connection.")
            else -> {
                val chunks = jpegBytes
                    .asList()
                    .chunked(VoltraControlFrames.STARTUP_IMAGE_CHUNK_DATA_BYTES)
                    .map { it.toByteArray() }
                pendingStartupImageChunkCount = chunks.size
                startupImageAckedChunkCount = 0
                Log.d(
                    STARTUP_DEBUG_TAG,
                    "queue startup image bytes=${jpegBytes.size} chunks=${chunks.size} chunkBytes=${VoltraControlFrames.STARTUP_IMAGE_CHUNK_DATA_BYTES}",
                )
                val queuedFrames = buildList {
                    add(
                        QueuedFrameSpec(
                            label = "startup image header",
                            bytes = VoltraFrameBuilder.build(
                                cmd = VoltraControlFrames.CMD_STARTUP_IMAGE,
                                payload = VoltraControlFrames.startupImageHeaderPayload(
                                    imageBytes = jpegBytes,
                                    chunkCount = chunks.size,
                                ),
                                seq = controlSeq++,
                            ),
                        ),
                    )
                    chunks.forEachIndexed { index, chunk ->
                        add(
                            QueuedFrameSpec(
                                label = "startup image chunk ${index + 1}/${chunks.size}",
                                bytes = VoltraFrameBuilder.build(
                                    cmd = VoltraControlFrames.CMD_STARTUP_IMAGE,
                                    payload = VoltraControlFrames.startupImageChunkPayload(
                                        chunkIndex = index + 1,
                                        chunkBytes = chunk,
                                    ),
                                    seq = controlSeq++,
                                ),
                            ),
                        )
                    }
                    add(
                        QueuedFrameSpec(
                            label = "startup image finalize",
                            bytes = VoltraFrameBuilder.build(
                                cmd = VoltraControlFrames.CMD_STARTUP_IMAGE,
                                payload = VoltraControlFrames.startupImageFinalizePayload(),
                                seq = controlSeq++,
                            ),
                        ),
                    )
                    add(
                        QueuedFrameSpec(
                            label = "startup image apply",
                            bytes = VoltraFrameBuilder.build(
                                cmd = VoltraControlFrames.CMD_STARTUP_IMAGE,
                                payload = VoltraControlFrames.startupImageApplyPayload(),
                                seq = controlSeq++,
                            ),
                        ),
                    )
                }
                val result = sendTransportFrames(
                    gatt = currentGatt,
                    command = VoltraControlCommand.UPLOAD_STARTUP_IMAGE,
                    frames = queuedFrames,
                    label = "upload startup image (${jpegBytes.size} bytes, ${chunks.size} chunks)",
                )
                if (result.status != VoltraCommandStatus.BLOCKED && result.status != VoltraCommandStatus.FAILED) {
                    mutableState.update {
                        it.copy(statusMessage = "Queued startup image upload (${chunks.size} chunks).")
                    }
                }
                result
            }
        }
    }

    override suspend fun refreshModeFeatureStatus(): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.REFRESH_MODE_FEATURE_STATUS, "Mode refresh is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.REFRESH_MODE_FEATURE_STATUS, "Cannot refresh mode state while the VOLTRA is not connected.")
            currentGatt == null ->
                blocked(VoltraControlCommand.REFRESH_MODE_FEATURE_STATUS, "No active GATT connection.")
            else -> queueParamReadCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.REFRESH_MODE_FEATURE_STATUS,
                paramIds = MODE_FEATURE_STATUS_PARAMS,
                label = "refresh mode feature state",
            )
        }
    }

    private fun sendExperimentalStrengthSetting(
        command: VoltraControlCommand,
        paramId: Int,
        valueBytes: ByteArray,
        label: String,
    ): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        return when {
            !current.controlCommandsEnabled ->
                blocked(command, "$label is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(command, "Cannot $label while the VOLTRA is not connected.")
            current.safety.workoutState != VoltraControlFrames.WORKOUT_STATE_ACTIVE ->
                blocked(command, "Enter Weight Training before changing experimental strength features.")
            currentGatt == null ->
                blocked(command, "No active GATT connection.")
            else -> sendParamWriteCommand(
                gatt = currentGatt,
                command = command,
                paramId = paramId,
                valueBytes = valueBytes,
                label = label,
                followUpReadParamIds = STRENGTH_FEATURE_STATUS_PARAMS,
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendCommandFrames(
        gatt: BluetoothGatt,
        command: VoltraControlCommand,
        frames: List<QueuedFrameSpec>,
        label: String,
    ): VoltraCommandResult {
        return sendFramesToCharacteristic(
            gatt = gatt,
            command = command,
            frames = frames,
            label = label,
            characteristicUuid = VoltraUuidRegistry.VOLTRA_COMMAND_CHARACTERISTIC_UUID,
            missingMessage = "VOLTRA command characteristic not found.",
            notWritableMessage = "VOLTRA command characteristic is not writable.",
        )
    }

    @SuppressLint("MissingPermission")
    private fun sendTransportFrames(
        gatt: BluetoothGatt,
        command: VoltraControlCommand,
        frames: List<QueuedFrameSpec>,
        label: String,
    ): VoltraCommandResult {
        return sendFramesToCharacteristic(
            gatt = gatt,
            command = command,
            frames = frames,
            label = label,
            characteristicUuid = VoltraUuidRegistry.VOLTRA_TRANSPORT_CHARACTERISTIC_UUID,
            missingMessage = "VOLTRA transport characteristic not found.",
            notWritableMessage = "VOLTRA transport characteristic is not writable.",
        )
    }

    @SuppressLint("MissingPermission")
    private fun sendFramesToCharacteristic(
        gatt: BluetoothGatt,
        command: VoltraControlCommand,
        frames: List<QueuedFrameSpec>,
        label: String,
        characteristicUuid: String,
        missingMessage: String,
        notWritableMessage: String,
    ): VoltraCommandResult {
        val commandCharacteristic = gatt.findVoltraCharacteristic(characteristicUuid)
            ?: return logCommand(
                VoltraCommandResult(
                    command = command,
                    status = VoltraCommandStatus.BLOCKED,
                    message = missingMessage,
                    timestampMillis = System.currentTimeMillis(),
                ),
            )

        val properties = commandCharacteristic.properties.toGattProperties()
        if (GattProperty.WRITE !in properties && GattProperty.WRITE_NO_RESPONSE !in properties) {
            return logCommand(
                VoltraCommandResult(
                    command = command,
                    status = VoltraCommandStatus.BLOCKED,
                    message = notWritableMessage,
                    timestampMillis = System.currentTimeMillis(),
                ),
            )
        }

        var lastFrameSize = 0
        frames.forEach { frame ->
            lastFrameSize = frame.bytes.size
            writeQueue += CharacteristicWrite(
                gatt = gatt,
                characteristic = commandCharacteristic,
                packet = VoltraBootstrapPacket(label = frame.label, hex = frame.bytes.toHexString()),
                characteristicUuid = commandCharacteristic.uuid.toString().uppercase(),
                command = command,
            )
        }
        startWriteQueueIfIdle()
        return logCommand(
            VoltraCommandResult(
                command = command,
                status = VoltraCommandStatus.QUEUED,
                message = "Queued $label frame${if (frames.size == 1) "" else "s"} (${frames.size} total, last len=$lastFrameSize bytes).",
                timestampMillis = System.currentTimeMillis(),
            ),
        )
    }

    @SuppressLint("MissingPermission")
    private fun queueParamReadCommand(
        gatt: BluetoothGatt,
        command: VoltraControlCommand,
        paramIds: List<Int>,
        label: String,
    ): VoltraCommandResult {
        val transportCharacteristic = gatt.findVoltraCharacteristic(VoltraUuidRegistry.VOLTRA_TRANSPORT_CHARACTERISTIC_UUID)
            ?: return logCommand(
                VoltraCommandResult(
                    command = command,
                    status = VoltraCommandStatus.BLOCKED,
                    message = "VOLTRA transport characteristic not found — run the handshake probe first.",
                    timestampMillis = System.currentTimeMillis(),
                ),
            )

        val startSeq = controlSeq
        val payload = VoltraControlFrames.readParamsPayload(*paramIds.toIntArray())
        val frameBytes = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_PARAM_READ,
            payload = payload,
            seq = controlSeq++,
        )
        writeQueue += CharacteristicWrite(
            gatt = gatt,
            characteristic = transportCharacteristic,
            packet = VoltraBootstrapPacket(label = label, hex = frameBytes.toHexString()),
            characteristicUuid = transportCharacteristic.uuid.toString().uppercase(),
            command = command,
        )
        startWriteQueueIfIdle()
        return logCommand(
            VoltraCommandResult(
                command = command,
                status = VoltraCommandStatus.QUEUED,
                message = "Queued $label frame (seq=$startSeq, len=${frameBytes.size} bytes).",
                timestampMillis = System.currentTimeMillis(),
            ),
        )
    }

    private fun VoltraSessionState.baseWeightLbForStrengthFeatures(): Int? {
        return listOfNotNull(
            safety.targetLoadLb,
            reading.weightLb,
            targetLoad.takeIf { it.unit == WeightUnit.LB }?.value,
        )
            .firstOrNull { it in VoltraControlFrames.MIN_TARGET_LB.toDouble()..VoltraControlFrames.MAX_TARGET_LB.toDouble() }
            ?.roundToInt()
    }

    private fun VoltraSessionState.maxChainsWeightLb(baseWeightLb: Int): Int {
        val headroomToDeviceMax = VoltraControlFrames.MAX_TARGET_LB - baseWeightLb
        return minOf(baseWeightLb, headroomToDeviceMax)
            .coerceIn(VoltraControlFrames.MIN_EXTRA_WEIGHT_LB, VoltraControlFrames.MAX_EXTRA_WEIGHT_LB)
    }

    private fun VoltraSessionState.minEccentricWeightLb(baseWeightLb: Int): Int {
        return (-baseWeightLb)
            .coerceIn(VoltraControlFrames.MIN_ECCENTRIC_WEIGHT_LB, VoltraControlFrames.MAX_ECCENTRIC_WEIGHT_LB)
    }

    private fun VoltraSessionState.maxEccentricWeightLb(baseWeightLb: Int): Int {
        return baseWeightLb
            .coerceIn(VoltraControlFrames.MIN_ECCENTRIC_WEIGHT_LB, VoltraControlFrames.MAX_ECCENTRIC_WEIGHT_LB)
    }

    @SuppressLint("MissingPermission")
    private fun enqueueIsometricVendorRefresh(gatt: BluetoothGatt) {
        val transportCharacteristic = gatt.findVoltraCharacteristic(VoltraUuidRegistry.VOLTRA_TRANSPORT_CHARACTERISTIC_UUID)
            ?: return
        val frameBytes = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_VENDOR,
            payload = VoltraControlFrames.vendorStateRefreshPayload(),
            seq = controlSeq++,
        )
        writeQueue += CharacteristicWrite(
            gatt = gatt,
            characteristic = transportCharacteristic,
            packet = VoltraBootstrapPacket(
                label = "refresh vendor state stream (AA13 01)",
                hex = frameBytes.toHexString(),
            ),
            characteristicUuid = transportCharacteristic.uuid.toString().uppercase(),
            command = VoltraControlCommand.REFRESH_MODE_FEATURE_STATUS,
        )
        startWriteQueueIfIdle()
    }

    private fun shouldRunIsometricVendorRefresh(current: VoltraSessionState): Boolean {
        val refreshWindowOpen = System.currentTimeMillis() < isometricVendorRefreshUntilMillis
        val livePullInProgress = current.reading.isometricCurrentForceN != null
        return current.connectionState == VoltraConnectionState.CONNECTED &&
            current.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_ISOMETRIC &&
            (refreshWindowOpen || pendingIsometricLoadIssued || livePullInProgress)
    }

    private fun reconcileIsometricVendorRefreshLoop() {
        val current = mutableState.value
        if (shouldRunIsometricVendorRefresh(current)) {
            if (pendingIsometricAutoLoad) {
                pendingIsometricAutoLoad = false
                stopPendingIsometricAutoLoadLoop()
            }
            if (isometricVendorRefreshRunnable == null) {
                startIsometricVendorRefreshLoop()
            }
        } else {
            stopIsometricVendorRefreshLoop()
        }
    }

    private fun startIsometricVendorRefreshLoop() {
        stopIsometricVendorRefreshLoop()
        val currentGatt = gatt
        val current = mutableState.value
        if (currentGatt == null || !shouldRunIsometricVendorRefresh(current)) {
            return
        }
        enqueueIsometricVendorRefresh(currentGatt)
        val runnable = object : Runnable {
            override fun run() {
                val currentGatt = gatt
                val current = mutableState.value
                if (currentGatt == null || !shouldRunIsometricVendorRefresh(current)) {
                    stopIsometricVendorRefreshLoop()
                    return
                }
                enqueueIsometricVendorRefresh(currentGatt)
                mainHandler.postDelayed(this, ISOMETRIC_VENDOR_REFRESH_INTERVAL_MILLIS)
            }
        }
        isometricVendorRefreshRunnable = runnable
        mainHandler.postDelayed(runnable, ISOMETRIC_VENDOR_REFRESH_INTERVAL_MILLIS)
    }

    private fun stopIsometricVendorRefreshLoop() {
        isometricVendorRefreshRunnable?.let(mainHandler::removeCallbacks)
        isometricVendorRefreshRunnable = null
    }

    private fun requestIsometricVendorRefreshBurst(durationMillis: Long = ISOMETRIC_VENDOR_REFRESH_BURST_MILLIS) {
        val untilMillis = System.currentTimeMillis() + durationMillis
        if (untilMillis > isometricVendorRefreshUntilMillis) {
            isometricVendorRefreshUntilMillis = untilMillis
        }
        reconcileIsometricVendorRefreshLoop()
    }

    private fun activeIsometricRefreshBurstMillis(): Long {
        val configuredSeconds = mutableState.value.reading.isometricMaxDurationSeconds
            ?: DEFAULT_ISOMETRIC_MAX_DURATION_SECONDS
        return (configuredSeconds.coerceIn(3, MAX_ISOMETRIC_REFRESH_BURST_SECONDS) * 1_000L) +
            ISOMETRIC_VENDOR_REFRESH_TAIL_MILLIS
    }

    private fun cancelIsometricVendorRefreshBurst() {
        isometricVendorRefreshUntilMillis = 0L
        stopIsometricVendorRefreshLoop()
    }

    private fun stopPendingIsometricAutoLoadLoop(resetLoadIssued: Boolean = false) {
        pendingIsometricAutoLoadRunnable?.let(mainHandler::removeCallbacks)
        pendingIsometricAutoLoadRunnable = null
        pendingIsometricAutoLoadAttempts = 0
        if (resetLoadIssued) {
            pendingIsometricLoadIssued = false
        }
    }

    private fun isIsometricEnterSettled(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val enteredAtMillis = lastIsometricEnterAtMillis
        return enteredAtMillis == 0L || nowMillis - enteredAtMillis >= ISOMETRIC_ENTER_SETTLE_MILLIS
    }

    private fun schedulePendingIsometricAutoLoad(currentGatt: BluetoothGatt) {
        stopPendingIsometricAutoLoadLoop()
        val runnable = object : Runnable {
            override fun run() {
                if (!pendingIsometricAutoLoad) {
                    stopPendingIsometricAutoLoadLoop()
                    return
                }
                maybeAutoLoadIsometric(currentGatt)
                if (!pendingIsometricAutoLoad) {
                    stopPendingIsometricAutoLoadLoop()
                    return
                }
                pendingIsometricAutoLoadAttempts += 1
                if (pendingIsometricAutoLoadAttempts >= ISOMETRIC_AUTO_LOAD_MAX_ATTEMPTS) {
                    pendingIsometricAutoLoad = false
                    stopPendingIsometricAutoLoadLoop(resetLoadIssued = true)
                    logCommand(
                        VoltraCommandResult(
                            command = VoltraControlCommand.LOAD,
                            status = VoltraCommandStatus.BLOCKED,
                            message = "Automatic Isometric load timed out while waiting for the VOLTRA to stay ready.",
                            timestampMillis = System.currentTimeMillis(),
                        ),
                    )
                    return
                }
                mainHandler.postDelayed(this, ISOMETRIC_AUTO_LOAD_RETRY_MILLIS)
            }
        }
        pendingIsometricAutoLoadRunnable = runnable
        mainHandler.postDelayed(runnable, ISOMETRIC_AUTO_LOAD_INITIAL_DELAY_MILLIS)
    }

    private fun queueIsometricLoad(currentGatt: BluetoothGatt): VoltraCommandResult {
        lastIsometricLoadAttemptAtMillis = System.currentTimeMillis()
        mutableState.update {
            it.copy(
                reading = it.reading.clearIsometricTestState().copy(
                    workoutMode = it.reading.workoutMode ?: "Isometric Test, Ready",
                ),
            )
        }
        val result = sendTransportFrames(
            gatt = currentGatt,
            command = VoltraControlCommand.LOAD,
            frames = buildList {
                add(
                    QueuedFrameSpec(
                        label = "read Isometric cable position (MC_DEFAULT_OFFLEN_CM + BP_RUNTIME_POSITION_CM)",
                        bytes = VoltraFrameBuilder.build(
                            cmd = VoltraControlFrames.CMD_PARAM_READ,
                            payload = VoltraControlFrames.readIsometricCablePositionPayload(),
                            seq = controlSeq++,
                        ),
                    ),
                )
                add(
                    QueuedFrameSpec(
                        label = "arm Isometric Test (BP_SET_FITNESS_MODE=1)",
                        bytes = VoltraFrameBuilder.build(
                            cmd = VoltraControlFrames.CMD_PARAM_WRITE,
                            payload = VoltraControlFrames.loadIsometricPayload(),
                            seq = controlSeq++,
                        ),
                    ),
                )
                add(
                    QueuedFrameSpec(
                        label = "refresh Isometric vendor state stream (AA13 01)",
                        bytes = VoltraFrameBuilder.build(
                            cmd = VoltraControlFrames.CMD_VENDOR,
                            payload = VoltraControlFrames.vendorStateRefreshPayload(),
                            seq = controlSeq++,
                        ),
                    ),
                )
            },
            label = "arm Isometric Test (BP_SET_FITNESS_MODE=1)",
        )
        pendingIsometricLoadIssued =
            result.status != VoltraCommandStatus.BLOCKED && result.status != VoltraCommandStatus.FAILED
        lastIsometricVendorRefreshAtMillis = lastIsometricLoadAttemptAtMillis
        if (pendingIsometricLoadIssued) {
            requestIsometricVendorRefreshBurst(activeIsometricRefreshBurstMillis())
        }
        return result
    }

    override suspend fun load(): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        val safetyReasons = current.safety.reasons.joinToString()
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.LOAD, "Load is locked: controlCommandsEnabled is false.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.LOAD, "Cannot load while the VOLTRA is not connected.")
            !current.safety.canLoad ->
                blocked(VoltraControlCommand.LOAD, "Cannot load: $safetyReasons")
            currentGatt == null ->
                blocked(VoltraControlCommand.LOAD, "No active GATT connection.")
            current.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_ISOMETRIC -> {
                val isIsometricLiveScreen = VoltraControlFrames.isIsometricScreenMode(current.safety.fitnessMode) ||
                    VoltraControlFrames.isLoadEngagedForWorkoutState(
                        current.safety.fitnessMode,
                        current.safety.workoutState,
                    )
                if (!isIsometricLiveScreen && hasPendingCommand(VoltraControlCommand.LOAD)) {
                    return logCommand(
                        VoltraCommandResult(
                            command = VoltraControlCommand.LOAD,
                            status = VoltraCommandStatus.QUEUED,
                            message = "Isometric load is already queued.",
                            timestampMillis = System.currentTimeMillis(),
                        ),
                    )
                }
                if (isIsometricLiveScreen && hasPendingCommand(VoltraControlCommand.REFRESH_MODE_FEATURE_STATUS)) {
                    return logCommand(
                        VoltraCommandResult(
                            command = VoltraControlCommand.LOAD,
                            status = VoltraCommandStatus.QUEUED,
                            message = "Isometric live refresh is already queued.",
                            timestampMillis = System.currentTimeMillis(),
                        ),
                    )
                }
                val now = System.currentTimeMillis()
                if (!isIsometricLiveScreen && !isIsometricEnterSettled(now)) {
                    pendingIsometricAutoLoad = true
                    schedulePendingIsometricAutoLoad(currentGatt)
                    return logCommand(
                        VoltraCommandResult(
                            command = VoltraControlCommand.LOAD,
                            status = VoltraCommandStatus.QUEUED,
                            message = "Waiting for Isometric Test to settle before loading.",
                            timestampMillis = now,
                        ),
                    )
                }
                if (!isIsometricLiveScreen && (inFlightWrite != null || writeQueue.isNotEmpty())) {
                    pendingIsometricAutoLoad = true
                    schedulePendingIsometricAutoLoad(currentGatt)
                    return logCommand(
                        VoltraCommandResult(
                            command = VoltraControlCommand.LOAD,
                            status = VoltraCommandStatus.QUEUED,
                            message = "Waiting for current Isometric commands to finish before loading.",
                            timestampMillis = now,
                        ),
                    )
                }
                if (!isIsometricLiveScreen) {
                    pendingIsometricAutoLoad = true
                    stopPendingIsometricAutoLoadLoop(resetLoadIssued = true)
                    return queueIsometricLoad(currentGatt)
                }
                mutableState.update {
                    it.copy(
                        reading = it.reading.clearIsometricTestState().copy(
                            workoutMode = it.reading.workoutMode ?: "Isometric Test, Ready",
                        ),
                    )
                }
                pendingIsometricLoadIssued = true
                lastIsometricVendorRefreshAtMillis = now
                requestIsometricVendorRefreshBurst(activeIsometricRefreshBurstMillis())
                logCommand(
                    VoltraCommandResult(
                        command = VoltraControlCommand.LOAD,
                        status = VoltraCommandStatus.QUEUED,
                        message = "Queued Isometric live refresh.",
                        timestampMillis = now,
                    ),
                )
            }
            current.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_CUSTOM_CURVE ->
                sendTransportFrames(
                    gatt = currentGatt,
                    command = VoltraControlCommand.LOAD,
                    frames = buildList {
                        add(
                            QueuedFrameSpec(
                                label = "read Custom Curve cable position (MC_DEFAULT_OFFLEN_CM + BP_RUNTIME_POSITION_CM)",
                                bytes = VoltraFrameBuilder.build(
                                    cmd = VoltraControlFrames.CMD_PARAM_READ,
                                    payload = VoltraControlFrames.readIsometricCablePositionPayload(),
                                    seq = controlSeq++,
                                ),
                            ),
                        )
                        add(
                            QueuedFrameSpec(
                                label = "load Custom Curve (BP_SET_FITNESS_MODE=5)",
                                bytes = VoltraFrameBuilder.build(
                                    cmd = VoltraControlFrames.CMD_PARAM_WRITE,
                                    payload = VoltraControlFrames.loadPayload(),
                                    seq = controlSeq++,
                                ),
                            ),
                        )
                        add(
                            QueuedFrameSpec(
                                label = "refresh Custom Curve vendor state stream (AA13 01)",
                                bytes = VoltraFrameBuilder.build(
                                    cmd = VoltraControlFrames.CMD_VENDOR,
                                    payload = VoltraControlFrames.vendorStateRefreshPayload(),
                                    seq = controlSeq++,
                                ),
                            ),
                        )
                    },
                    label = "load Custom Curve",
                )
            else -> sendParamWriteCommands(
                gatt = currentGatt,
                command = VoltraControlCommand.LOAD,
                specs = buildList {
                    if (
                        current.safety.workoutState == null ||
                        current.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_INACTIVE
                    ) {
                        add(
                            ParamWriteSpec(
                                paramId = VoltraControlFrames.PARAM_FITNESS_WORKOUT_STATE,
                                valueBytes = byteArrayOf(VoltraControlFrames.WORKOUT_STATE_ACTIVE.toByte()),
                                label = "re-enter weight training (FITNESS_WORKOUT_STATE=1)",
                            ),
                        )
                    }
                    add(
                        ParamWriteSpec(
                            paramId = VoltraControlFrames.PARAM_BP_SET_FITNESS_MODE,
                            valueBytes = VoltraControlFrames.FITNESS_MODE_STRENGTH_LOADED.uint16Le(),
                            label = "load (BP_SET_FITNESS_MODE=5)",
                        ),
                    )
                },
                label = "load",
                followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
            )
        }
    }

    override suspend fun unload(): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.UNLOAD, "Unload is locked: controlCommandsEnabled is false.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.UNLOAD, "Cannot unload while the VOLTRA is not connected.")
            currentGatt == null ->
                blocked(VoltraControlCommand.UNLOAD, "No active GATT connection.")
            else -> {
                cancelIsometricVendorRefreshBurst()
                pendingIsometricAutoLoad = false
                stopPendingIsometricAutoLoadLoop(resetLoadIssued = true)
                if (
                    current.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_ISOMETRIC ||
                    current.reading.workoutMode?.startsWith("Isometric Test") == true
                ) {
                    mutableState.update {
                        it.copy(
                            reading = it.reading.copy(workoutMode = "Isometric Test, Ready"),
                            safety = it.safety.copy(
                                canLoad = true,
                                reasons = listOf("Ready for current mode load."),
                                parsedDeviceState = true,
                                workoutState = VoltraControlFrames.WORKOUT_STATE_ISOMETRIC,
                                fitnessMode = VoltraControlFrames.FITNESS_MODE_STRENGTH_READY,
                            ),
                        )
                    }
                }
                sendParamWriteCommand(
                    gatt = currentGatt,
                    command = VoltraControlCommand.UNLOAD,
                    paramId = VoltraControlFrames.PARAM_BP_SET_FITNESS_MODE,
                    valueBytes = VoltraControlFrames.FITNESS_MODE_STRENGTH_READY.uint16Le(),
                    label = "unload (BP_SET_FITNESS_MODE=4)",
                    followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendParamWriteCommand(
        gatt: BluetoothGatt,
        command: VoltraControlCommand,
        paramId: Int,
        valueBytes: ByteArray,
        label: String,
        followUpReadParamIds: List<Int> = emptyList(),
    ): VoltraCommandResult {
        return sendParamWriteCommands(
            gatt = gatt,
            command = command,
            specs = listOf(
                ParamWriteSpec(
                    paramId = paramId,
                    valueBytes = valueBytes,
                    label = label,
                ),
            ),
            label = label,
            followUpReadParamIds = followUpReadParamIds,
        )
    }

    @SuppressLint("MissingPermission")
    private fun sendParamWriteCommands(
        gatt: BluetoothGatt,
        command: VoltraControlCommand,
        specs: List<ParamWriteSpec>,
        label: String,
        followUpReadParamIds: List<Int> = emptyList(),
    ): VoltraCommandResult {
        val transportCharacteristic = gatt.findVoltraCharacteristic(VoltraUuidRegistry.VOLTRA_TRANSPORT_CHARACTERISTIC_UUID)
            ?: return logCommand(
                VoltraCommandResult(
                    command = command,
                    status = VoltraCommandStatus.BLOCKED,
                    message = "VOLTRA transport characteristic not found — run the handshake probe first.",
                    timestampMillis = System.currentTimeMillis(),
                ),
            )

        val startSeq = controlSeq
        var lastFrameSize = 0
        specs.forEach { spec ->
            val payload = VoltraControlFrames.paramWritePayload(spec.paramId, spec.valueBytes)
            val frameBytes = VoltraFrameBuilder.build(
                cmd = VoltraControlFrames.CMD_PARAM_WRITE,
                payload = payload,
                seq = controlSeq++,
            )
            lastFrameSize = frameBytes.size
            val packet = VoltraBootstrapPacket(label = spec.label, hex = frameBytes.toHexString())
            writeQueue += CharacteristicWrite(
                gatt = gatt,
                characteristic = transportCharacteristic,
                packet = packet,
                characteristicUuid = transportCharacteristic.uuid.toString().uppercase(),
                command = command,
            )
        }
        if (followUpReadParamIds.isNotEmpty()) {
            val payload = VoltraControlFrames.readParamsPayload(*followUpReadParamIds.toIntArray())
            val frameBytes = VoltraFrameBuilder.build(
                cmd = VoltraControlFrames.CMD_PARAM_READ,
                payload = payload,
                seq = controlSeq++,
            )
            lastFrameSize = frameBytes.size
            writeQueue += CharacteristicWrite(
                gatt = gatt,
                characteristic = transportCharacteristic,
                packet = VoltraBootstrapPacket(
                    label = "read back strength feature state",
                    hex = frameBytes.toHexString(),
                ),
                characteristicUuid = transportCharacteristic.uuid.toString().uppercase(),
                command = command,
            )
        }
        startWriteQueueIfIdle()
        val frameCount = specs.size + if (followUpReadParamIds.isEmpty()) 0 else 1
        val seqText = if (frameCount == 1) startSeq.toString() else "$startSeq..${controlSeq - 1}"
        return logCommand(
            VoltraCommandResult(
                command = command,
                status = VoltraCommandStatus.QUEUED,
                message = "Queued $label frame${if (frameCount == 1) "" else "s"} (seq=$seqText, last len=$lastFrameSize bytes).",
                timestampMillis = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun setStrengthMode(): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.SET_STRENGTH_MODE, "Strength mode is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.SET_STRENGTH_MODE, "Cannot set strength mode while the VOLTRA is not connected.")
            currentGatt == null ->
                blocked(VoltraControlCommand.SET_STRENGTH_MODE, "No active GATT connection.")
            else -> sendParamWriteCommands(
                gatt = currentGatt,
                command = VoltraControlCommand.SET_STRENGTH_MODE,
                specs = listOf(
                    ParamWriteSpec(
                        paramId = VoltraControlFrames.PARAM_FITNESS_WORKOUT_STATE,
                        valueBytes = byteArrayOf(VoltraControlFrames.WORKOUT_STATE_ACTIVE.toByte()),
                        label = "enter weight training (FITNESS_WORKOUT_STATE=1)",
                    ),
                    ParamWriteSpec(
                        paramId = VoltraControlFrames.PARAM_BP_SET_FITNESS_MODE,
                        valueBytes = VoltraControlFrames.FITNESS_MODE_STRENGTH_READY.uint16Le(),
                        label = "set strength mode (BP_SET_FITNESS_MODE=4)",
                    ),
                ),
                label = "enter strength mode",
            )
        }
    }

    override suspend fun exitWorkout(): VoltraCommandResult {
        val current = mutableState.value
        val currentGatt = gatt
        return when {
            !current.controlCommandsEnabled ->
                blocked(VoltraControlCommand.EXIT_WORKOUT, "Exit workout is locked until this session receives a valid VOLTRA notification frame.")
            current.connectionState != VoltraConnectionState.CONNECTED ->
                blocked(VoltraControlCommand.EXIT_WORKOUT, "Cannot exit the workout while the VOLTRA is not connected.")
            currentGatt == null ->
                blocked(VoltraControlCommand.EXIT_WORKOUT, "No active GATT connection.")
            else -> {
                cancelIsometricVendorRefreshBurst()
                pendingIsometricAutoLoad = false
                stopPendingIsometricAutoLoadLoop(resetLoadIssued = true)
                if (
                    current.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_ISOMETRIC ||
                    current.reading.workoutMode?.startsWith("Isometric Test") == true
                ) {
                    mutableState.update {
                        it.copy(
                            reading = it.reading.copy(workoutMode = "Strength ready, session inactive"),
                            safety = it.safety.copy(
                                canLoad = false,
                                reasons = listOf("Workout session is inactive. Choose a mode first."),
                                parsedDeviceState = true,
                                workoutState = VoltraControlFrames.WORKOUT_STATE_INACTIVE,
                                fitnessMode = VoltraControlFrames.FITNESS_MODE_STRENGTH_READY,
                            ),
                        )
                    }
                }
                sendParamWriteCommands(
                    gatt = currentGatt,
                    command = VoltraControlCommand.EXIT_WORKOUT,
                    label = "exit active workout (FITNESS_WORKOUT_STATE=0)",
                    specs = listOf(
                        ParamWriteSpec(
                            paramId = VoltraControlFrames.PARAM_FITNESS_WORKOUT_STATE,
                            valueBytes = byteArrayOf(VoltraControlFrames.WORKOUT_STATE_INACTIVE.toByte()),
                            label = "exit active workout (FITNESS_WORKOUT_STATE=0)",
                        ),
                    ),
                    followUpReadParamIds = MODE_FEATURE_STATUS_PARAMS,
                )
            }
        }
    }

    fun enableCandidateNotifications(): VoltraCommandResult {
        val currentGatt = gatt
        if (currentGatt == null || mutableState.value.connectionState != VoltraConnectionState.CONNECTED) {
            return logCommand(
                VoltraCommandResult(
                    command = VoltraControlCommand.ENABLE_NOTIFICATIONS,
                    status = VoltraCommandStatus.BLOCKED,
                    message = "Connect to the VOLTRA GATT first, then enable notifications.",
                    timestampMillis = System.currentTimeMillis(),
                ),
            )
        }

        mutableState.update {
            it.copy(
                connectionState = VoltraConnectionState.SUBSCRIBING,
                statusMessage = "Enabling official VOLTRA notification channels.",
            )
        }
        subscribeToNotifications(currentGatt)
        return logCommand(
            VoltraCommandResult(
                command = VoltraControlCommand.ENABLE_NOTIFICATIONS,
                status = VoltraCommandStatus.QUEUED,
                message = "Queued VOLTRA notification subscriptions for command, notify, and transport channels.",
                timestampMillis = System.currentTimeMillis(),
            ),
        )
    }

    fun runReadOnlyHandshakeProbe(): VoltraCommandResult {
        val currentGatt = gatt
        if (currentGatt == null || mutableState.value.connectionState != VoltraConnectionState.CONNECTED) {
            return logCommand(
                VoltraCommandResult(
                    command = VoltraControlCommand.READ_ONLY_HANDSHAKE_PROBE,
                    status = VoltraCommandStatus.BLOCKED,
                    message = "Connect to the VOLTRA GATT first, then run the read-only handshake probe.",
                    timestampMillis = System.currentTimeMillis(),
                ),
            )
        }

        return queueReadOnlyHandshakeProbe(
            currentGatt = currentGatt,
            automatic = false,
        )
    }

    private fun queueReadOnlyHandshakeProbe(
        currentGatt: BluetoothGatt,
        automatic: Boolean,
    ): VoltraCommandResult {
        if (descriptorQueue.isNotEmpty() || readQueue.isNotEmpty() || writeQueue.isNotEmpty() || inFlightWrite != null) {
            return logCommand(
                VoltraCommandResult(
                    command = VoltraControlCommand.READ_ONLY_HANDSHAKE_PROBE,
                    status = VoltraCommandStatus.BLOCKED,
                    message = "Another BLE diagnostic operation is still in progress.",
                    timestampMillis = System.currentTimeMillis(),
                ),
            )
        }

        val transportCharacteristic = currentGatt.findVoltraCharacteristic(VoltraUuidRegistry.VOLTRA_TRANSPORT_CHARACTERISTIC_UUID)
        if (transportCharacteristic == null) {
            return logCommand(
                VoltraCommandResult(
                    command = VoltraControlCommand.READ_ONLY_HANDSHAKE_PROBE,
                    status = VoltraCommandStatus.BLOCKED,
                    message = "VOLTRA transport characteristic was not found.",
                    timestampMillis = System.currentTimeMillis(),
                ),
            )
        }

        val properties = transportCharacteristic.properties.toGattProperties()
        if (GattProperty.WRITE !in properties && GattProperty.WRITE_NO_RESPONSE !in properties) {
            return logCommand(
                VoltraCommandResult(
                    command = VoltraControlCommand.READ_ONLY_HANDSHAKE_PROBE,
                    status = VoltraCommandStatus.BLOCKED,
                    message = "VOLTRA transport characteristic is not writable.",
                    timestampMillis = System.currentTimeMillis(),
                ),
            )
        }

        runReadOnlyBootstrapAfterSubscribe = true
        mutableState.update {
            it.copy(
                connectionState = VoltraConnectionState.SUBSCRIBING,
                statusMessage = if (automatic) {
                    "Subscribing and sending the local VOLTRA handshake."
                } else {
                    "Subscribing before the captured read-only handshake probe."
                },
            )
        }
        subscribeToNotifications(currentGatt)
        return logCommand(
            VoltraCommandResult(
                command = VoltraControlCommand.READ_ONLY_HANDSHAKE_PROBE,
                status = VoltraCommandStatus.QUEUED,
                message = if (automatic) {
                    "Auto-queued official read-only handshake after GATT discovery."
                } else {
                    "Queued captured official read-only handshake probe. Control writes remain locked."
                },
                timestampMillis = System.currentTimeMillis(),
            ),
        )
    }

    fun readVoltraCharacteristics(): VoltraCommandResult {
        val currentGatt = gatt
        if (currentGatt == null || mutableState.value.connectionState != VoltraConnectionState.CONNECTED) {
            return logCommand(
                VoltraCommandResult(
                    command = VoltraControlCommand.READ_VOLTRA_CHARACTERISTICS,
                    status = VoltraCommandStatus.BLOCKED,
                    message = "Connect to the VOLTRA GATT first, then read VOLTRA characteristics.",
                    timestampMillis = System.currentTimeMillis(),
                ),
            )
        }

        if (descriptorQueue.isNotEmpty() || readQueue.isNotEmpty()) {
            return logCommand(
                VoltraCommandResult(
                    command = VoltraControlCommand.READ_VOLTRA_CHARACTERISTICS,
                    status = VoltraCommandStatus.BLOCKED,
                    message = "Another BLE diagnostic operation is still in progress.",
                    timestampMillis = System.currentTimeMillis(),
                ),
            )
        }

        val readableCharacteristics = currentGatt.services
            .firstOrNull { it.uuid.toString().uppercase() == VoltraUuidRegistry.VOLTRA_SERVICE_UUID }
            ?.characteristics
            .orEmpty()
            .filter { characteristic ->
                val properties = characteristic.properties.toGattProperties()
                val role = VoltraUuidRegistry.classifyCharacteristic(
                    uuid = characteristic.uuid.toString(),
                    properties = properties,
                )
                role in VOLTRA_SAFE_READ_ROLES && GattProperty.READ in properties
            }

        if (readableCharacteristics.isEmpty()) {
            return logCommand(
                VoltraCommandResult(
                    command = VoltraControlCommand.READ_VOLTRA_CHARACTERISTICS,
                    status = VoltraCommandStatus.BLOCKED,
                    message = "No read-capable VOLTRA diagnostic characteristics were found.",
                    timestampMillis = System.currentTimeMillis(),
                ),
            )
        }

        readQueue.clear()
        readableCharacteristics.forEach { characteristic ->
            readQueue += CharacteristicRead(
                gatt = currentGatt,
                characteristic = characteristic,
                characteristicUuid = characteristic.uuid.toString().uppercase(),
            )
        }
        mutableState.update {
            it.copy(statusMessage = "Reading VOLTRA command/transport characteristics without subscribing.")
        }
        readNextCharacteristic()
        return logCommand(
            VoltraCommandResult(
                command = VoltraControlCommand.READ_VOLTRA_CHARACTERISTICS,
                status = VoltraCommandStatus.QUEUED,
                message = "Queued ${readableCharacteristics.size} VOLTRA read probe(s).",
                timestampMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun emergencyDisconnect(): VoltraCommandResult {
        disconnect()
        return logCommand(
            VoltraCommandResult(
                command = VoltraControlCommand.EMERGENCY_DISCONNECT,
                status = VoltraCommandStatus.CONFIRMED,
                message = "Bluetooth session disconnected.",
                timestampMillis = System.currentTimeMillis(),
            ),
        )
    }

    private fun handleScanResult(result: ScanResult) {
        val serviceUuids = result.scanRecord
            ?.serviceUuids
            ?.map { it.uuid.toString().uppercase() }
            .orEmpty()
        val device = result.device.toVoltraDevice(
            rssi = result.rssi,
            advertisedServiceUuids = serviceUuids,
            nameOverride = result.scanRecord?.deviceName,
        )
        if (!showAllScanResults && !device.isLikelyVoltra) return

        nativeDevices[device.id] = result.device
        scanDevices[device.id] = VoltraScanResult(
            device = device,
            connectable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) result.isConnectable else null,
        )
    }

    private fun sortedScanResults(): List<VoltraScanResult> {
        return scanDevices.values
            .sortedWith(compareByDescending<VoltraScanResult> { it.device.isLikelyVoltra }.thenByDescending { it.device.rssi ?: -200 })
    }

    private fun BluetoothDevice.toVoltraDevice(
        rssi: Int?,
        advertisedServiceUuids: List<String>,
        nameOverride: String?,
    ): VoltraDevice {
        val safeName = nameOverride ?: runCatching {
            if (hasConnectPermission()) name else null
        }.getOrNull()
        return VoltraDevice(
            id = address,
            name = safeName,
            address = address,
            rssi = rssi,
            advertisedServiceUuids = advertisedServiceUuids,
            lastSeenMillis = System.currentTimeMillis(),
            isLikelyVoltra = VoltraUuidRegistry.isLikelyVoltra(safeName, advertisedServiceUuids),
        )
    }

    @SuppressLint("MissingPermission")
    private fun discoverServicesOnce(gatt: BluetoothGatt, statusMessage: String) {
        if (serviceDiscoveryStarted || this@AndroidVoltraClient.gatt !== gatt) return
        serviceDiscoveryStarted = true
        mutableState.update { it.copy(statusMessage = statusMessage) }
        runCatching { gatt.discoverServices() }.onFailure { error ->
            mutableState.update {
                it.copy(
                    connectionState = VoltraConnectionState.FAILED,
                    statusMessage = "Service discovery could not start: ${error.message}",
                )
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when {
                status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED -> {
                    val connectedAt = System.currentTimeMillis()
                    mutableState.update {
                        it.copy(
                            connectionState = VoltraConnectionState.DISCOVERING_SERVICES,
                            statusMessage = "Connected. Requesting larger BLE MTU.",
                            connectedAtMillis = connectedAt,
                            lastDisconnectAtMillis = null,
                            lastConnectionDurationMillis = null,
                        )
                    }
                    val mtuRequestStarted = runCatching { gatt.requestMtu(VOLTRA_MTU) }.getOrDefault(false)
                    if (mtuRequestStarted) {
                        mainHandler.postDelayed(
                            { discoverServicesOnce(gatt, "MTU callback did not arrive; discovering services.") },
                            MTU_FALLBACK_DELAY_MILLIS,
                        )
                    } else {
                        discoverServicesOnce(gatt, "MTU request could not start; discovering services.")
                    }
                }
                newState == BluetoothProfile.STATE_DISCONNECTED -> {
                    gatt.close()
                    if (this@AndroidVoltraClient.gatt === gatt) this@AndroidVoltraClient.gatt = null
                    descriptorQueue.clear()
                    readQueue.clear()
                    writeQueue.clear()
                    notificationAssemblers.clear()
                    serviceDiscoveryStarted = false
                    cancelIsometricVendorRefreshBurst()
                    pendingIsometricAutoLoad = false
                    stopPendingIsometricAutoLoadLoop(resetLoadIssued = true)
                    mainHandler.removeCallbacksAndMessages(null)
                    runReadOnlyBootstrapAfterSubscribe = false
                    val disconnectedAt = System.currentTimeMillis()
                    val disconnectReason = "Bluetooth disconnected: ${status.describeGattStatus()}."
                    mutableState.update {
                        it.copy(
                            connectionState = VoltraConnectionState.DISCONNECTED,
                            statusMessage = disconnectReason,
                            lastDisconnectReason = disconnectReason,
                            lastDisconnectAtMillis = disconnectedAt,
                            lastConnectionDurationMillis = it.connectedAtMillis?.let { connectedAt ->
                                disconnectedAt - connectedAt
                            },
                            controlCommandsEnabled = false,
                        )
                    }
                }
                else -> {
                    mutableState.update {
                        it.copy(
                            connectionState = VoltraConnectionState.FAILED,
                            statusMessage = "Bluetooth connection failed with status $status.",
                        )
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                mutableState.update {
                    it.copy(
                        connectionState = VoltraConnectionState.FAILED,
                        statusMessage = "Service discovery failed with status $status.",
                    )
                }
                return
            }

            val snapshot = gatt.services.toGattSnapshot()
            val protocolStatus = snapshot.protocolStatus()
            mutableState.update {
                it.copy(
                    connectionState = VoltraConnectionState.CONNECTED,
                    gattSnapshot = snapshot,
                    protocolStatus = protocolStatus,
                    subscribedCharacteristicCount = 0,
                    statusMessage = if (protocolStatus == VoltraProtocolStatus.VOLTRA_GATT_MATCH) {
                        "VOLTRA GATT matched. Starting local handshake."
                    } else {
                        "BLE connected, but VOLTRA GATT has not been identified."
                    },
                )
            }
            if (protocolStatus == VoltraProtocolStatus.VOLTRA_GATT_MATCH) {
                queueReadOnlyHandshakeProbe(
                    currentGatt = gatt,
                    automatic = true,
                )
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val message = if (status == BluetoothGatt.GATT_SUCCESS) {
                "BLE MTU is $mtu. Discovering services."
            } else {
                "MTU request failed with ${status.describeGattStatus()}; discovering services."
            }
            discoverServicesOnce(gatt, message)
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            @Suppress("DEPRECATION")
            appendFrame(gatt, characteristic, characteristic.value ?: ByteArray(0))
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            appendFrame(gatt, characteristic, value)
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                @Suppress("DEPRECATION")
                appendFrame(gatt, characteristic, characteristic.value ?: ByteArray(0), RawFrameDirection.READ)
            } else {
                mutableState.update {
                    it.copy(statusMessage = "Read failed for ${characteristic.uuid}: ${status.describeGattStatus()}.")
                }
            }
            readNextCharacteristic()
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                appendFrame(gatt, characteristic, value, RawFrameDirection.READ)
            } else {
                mutableState.update {
                    it.copy(statusMessage = "Read failed for ${characteristic.uuid}: ${status.describeGattStatus()}.")
                }
            }
            readNextCharacteristic()
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                mutableState.update {
                    it.copy(statusMessage = "Descriptor write failed with ${status.describeGattStatus()}.")
                }
            }
            writeNextDescriptor()
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            handleCharacteristicWrite(characteristic, status)
        }
    }

    @SuppressLint("MissingPermission")
    private fun subscribeToNotifications(gatt: BluetoothGatt) {
        descriptorQueue.clear()
        val candidates = gatt.services
            .flatMap { service -> service.characteristics }
            .filter { characteristic ->
                val properties = characteristic.properties.toGattProperties()
                val role = VoltraUuidRegistry.classifyCharacteristic(
                    uuid = characteristic.uuid.toString(),
                    properties = properties,
                )
                role in VOLTRA_OFFICIAL_NOTIFY_ROLES &&
                    (GattProperty.NOTIFY in properties || GattProperty.INDICATE in properties)
            }
        candidates.forEach { characteristic ->
            val properties = characteristic.properties.toGattProperties()
            runCatching { gatt.setCharacteristicNotification(characteristic, true) }
            val cccd = characteristic.getDescriptor(CCCD_UUID)
            if (cccd != null) {
                val value = if (GattProperty.NOTIFY in properties) {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else {
                    BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                }
                descriptorQueue += DescriptorWrite(
                    gatt = gatt,
                    descriptor = cccd,
                    value = value,
                    characteristicUuid = characteristic.uuid.toString().uppercase(),
                )
            }
        }

        mutableState.update { it.copy(subscribedCharacteristicCount = descriptorQueue.size) }

        if (descriptorQueue.isEmpty()) {
            if (runReadOnlyBootstrapAfterSubscribe) {
                enqueueReadOnlyBootstrap(gatt)
            } else {
                mutableState.update {
                    it.copy(
                        connectionState = VoltraConnectionState.CONNECTED,
                        statusMessage = if (it.protocolStatus == VoltraProtocolStatus.VOLTRA_GATT_MATCH) {
                            "VOLTRA GATT matched, but official notify descriptors were not writable."
                        } else {
                            "BLE connected for diagnostics. VOLTRA notify characteristics were not found."
                        },
                    )
                }
            }
        } else {
            writeNextDescriptor()
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeNextDescriptor() {
        val item = descriptorQueue.pollFirst()
        if (item == null) {
            if (runReadOnlyBootstrapAfterSubscribe) {
                enqueueReadOnlyBootstrap(gatt ?: return)
                return
            }
            mutableState.update {
                it.copy(
                    connectionState = VoltraConnectionState.CONNECTED,
                    statusMessage = when (it.protocolStatus) {
                        VoltraProtocolStatus.VOLTRA_GATT_MATCH -> "VOLTRA GATT matched. Waiting for notification frames."
                        VoltraProtocolStatus.RAW_FRAMES_SEEN -> "VOLTRA raw frames seen."
                        else -> "BLE connected for diagnostics. VOLTRA protocol not identified."
                    },
                )
            }
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = item.gatt.writeDescriptor(item.descriptor, item.value)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                mutableState.update {
                    it.copy(statusMessage = "Descriptor write could not start for ${item.characteristicUuid}: $status")
                }
                writeNextDescriptor()
            }
        } else {
            @Suppress("DEPRECATION")
            item.descriptor.value = item.value
            @Suppress("DEPRECATION")
            if (!item.gatt.writeDescriptor(item.descriptor)) {
                mutableState.update {
                    it.copy(statusMessage = "Descriptor write could not start for ${item.characteristicUuid}.")
                }
                writeNextDescriptor()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enqueueReadOnlyBootstrap(gatt: BluetoothGatt) {
        runReadOnlyBootstrapAfterSubscribe = false
        val transportCharacteristic = gatt.findVoltraCharacteristic(VoltraUuidRegistry.VOLTRA_TRANSPORT_CHARACTERISTIC_UUID)
        if (transportCharacteristic == null) {
            logCommand(
                VoltraCommandResult(
                    command = VoltraControlCommand.READ_ONLY_HANDSHAKE_PROBE,
                    status = VoltraCommandStatus.FAILED,
                    message = "VOLTRA transport characteristic disappeared before the read-only probe could start.",
                    timestampMillis = System.currentTimeMillis(),
                ),
            )
            return
        }

        writeQueue.clear()
        VoltraOfficialReadOnlyBootstrap.packets.forEach { packet ->
            writeQueue += CharacteristicWrite(
                gatt = gatt,
                characteristic = transportCharacteristic,
                packet = packet,
                characteristicUuid = transportCharacteristic.uuid.toString().uppercase(),
            )
        }
        mutableState.update {
            it.copy(
                connectionState = VoltraConnectionState.CONNECTED,
                statusMessage = "Sending captured read-only VOLTRA bootstrap packets.",
            )
        }
        writeNextCharacteristic()
    }

    @SuppressLint("MissingPermission")
    private fun startWriteQueueIfIdle() {
        if (inFlightWrite == null) {
            writeNextCharacteristic()
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeNextCharacteristic() {
        val item = writeQueue.pollFirst()
        if (item == null) {
            val finishedCommand = inFlightWrite?.command ?: VoltraControlCommand.READ_ONLY_HANDSHAKE_PROBE
            inFlightWrite = null
            logCommand(
                VoltraCommandResult(
                    command = finishedCommand,
                    status = VoltraCommandStatus.SENT,
                    message = "Write queue empty. Watch for VOLTRA notifications.",
                    timestampMillis = System.currentTimeMillis(),
                ),
            )
            return
        }
        inFlightWrite = item

        appendFrame(item.gatt, item.characteristic, item.packet.bytes, RawFrameDirection.WRITE)
        mutableState.update {
            it.copy(statusMessage = "Writing ${item.packet.label}.")
        }
        if (item.command == VoltraControlCommand.UPLOAD_STARTUP_IMAGE) {
            Log.d(STARTUP_DEBUG_TAG, "write ${item.packet.label}")
        }

        val writeStarted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val writeType = if (GattProperty.WRITE in item.characteristic.properties.toGattProperties()) {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            item.gatt.writeCharacteristic(item.characteristic, item.packet.bytes, writeType) == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            item.characteristic.value = item.packet.bytes
            item.characteristic.writeType = if (GattProperty.WRITE in item.characteristic.properties.toGattProperties()) {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            @Suppress("DEPRECATION")
            item.gatt.writeCharacteristic(item.characteristic)
        }

        if (!writeStarted) {
            logCommand(
                VoltraCommandResult(
                    command = item.command,
                    status = VoltraCommandStatus.FAILED,
                    message = "Could not start write for ${item.packet.label}.",
                    timestampMillis = System.currentTimeMillis(),
                    rawHex = item.packet.hex,
                ),
            )
            writeNextCharacteristic()
        }
    }

    private fun handleCharacteristicWrite(
        characteristic: BluetoothGattCharacteristic,
        status: Int,
    ) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            val activeCommand = inFlightWrite?.command ?: VoltraControlCommand.READ_ONLY_HANDSHAKE_PROBE
            logCommand(
                VoltraCommandResult(
                    command = activeCommand,
                    status = VoltraCommandStatus.FAILED,
                    message = "Write failed for ${characteristic.uuid}: ${status.describeGattStatus()}.",
                    timestampMillis = System.currentTimeMillis(),
                ),
            )
            writeNextCharacteristic()
            return
        }
        mainHandler.postDelayed(
            { writeNextCharacteristic() },
            BOOTSTRAP_WRITE_PACING_MILLIS,
        )
    }

    @SuppressLint("MissingPermission")
    private fun readNextCharacteristic() {
        val item = readQueue.pollFirst()
        if (item == null) {
            mutableState.update {
                it.copy(statusMessage = "VOLTRA read probe complete.")
            }
            return
        }

        if (!item.gatt.readCharacteristic(item.characteristic)) {
            mutableState.update {
                it.copy(statusMessage = "Read could not start for ${item.characteristicUuid}.")
            }
            readNextCharacteristic()
        }
    }

    private fun appendFrame(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        direction: RawFrameDirection = RawFrameDirection.NOTIFY,
    ) {
        val values = if (direction == RawFrameDirection.NOTIFY) {
            notificationAssemblers
                .getOrPut(characteristic.uuid.toString().uppercase()) { VoltraFrameAssembler() }
                .accept(value)
        } else {
            listOf(value)
        }
        values.forEach { completeValue ->
            appendCompleteFrame(gatt, characteristic, completeValue, direction)
        }
    }

    private fun appendCompleteFrame(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        direction: RawFrameDirection,
    ) {
        val parsedPacket = VoltraPacketParser.parse(value)
        val inboundFrame = direction == RawFrameDirection.NOTIFY || direction == RawFrameDirection.READ
        val validatesControlProtocol = direction == RawFrameDirection.NOTIFY &&
            parsedPacket != null &&
            characteristic.isConfirmedVoltraResponseChannel()
        val frame = VoltraNotificationParser.rawFrame(
            serviceUuid = characteristic.service?.uuid?.toString()?.uppercase(),
            characteristicUuid = characteristic.uuid.toString().uppercase(),
            value = value,
            direction = direction,
        )
        val now = frame.timestampMillis
        var nextReadingSnapshot: VoltraReading? = null
        var nextSafetySnapshot: VoltraSafetyState? = null
        var priorSafetySnapshot: VoltraSafetyState? = null
        var clearedFreshIsometricAttempt = false
        mutableState.update {
            priorSafetySnapshot = it.safety
            val previousReading = it.reading
            val previousSafety = it.safety
            var nextReading = if (inboundFrame) {
                VoltraNotificationParser.mergeReading(it.reading, value, now)
            } else {
                it.reading
            }
            val nextSafety = if (inboundFrame) {
                VoltraNotificationParser.mergeSafety(it.safety, nextReading, value)
            } else {
                it.safety
            }
            val wasIsometricLoaded = VoltraControlFrames.isLoadEngagedForWorkoutState(
                previousSafety.fitnessMode,
                previousSafety.workoutState,
            )
            val isIsometricLoaded = VoltraControlFrames.isLoadEngagedForWorkoutState(
                nextSafety.fitnessMode,
                nextSafety.workoutState,
            )
            val enteringFreshIsometricLoad =
                nextSafety.workoutState == VoltraControlFrames.WORKOUT_STATE_ISOMETRIC &&
                    isIsometricLoaded &&
                    !wasIsometricLoaded
            if (enteringFreshIsometricLoad) {
                nextReading = nextReading.clearIsometricTestState().copy(
                    workoutMode = nextReading.workoutMode ?: previousReading.workoutMode,
                )
                clearedFreshIsometricAttempt = true
            }
            nextReadingSnapshot = nextReading
            nextSafetySnapshot = nextSafety
            it.copy(
                reading = nextReading,
                safety = nextSafety,
                rawFrames = (it.rawFrames + frame).takeLast(MAX_CAPTURED_FRAMES),
                protocolStatus = nextProtocolStatus(
                    current = it.protocolStatus,
                    inboundFrame = inboundFrame,
                    parsedPacketSeen = parsedPacket != null,
                    validatesControlProtocol = validatesControlProtocol,
                ),
                controlCommandsEnabled = it.controlCommandsEnabled || validatesControlProtocol,
                statusMessage = "Captured ${value.size} bytes from ${characteristic.uuid}.",
            )
        }
        if (clearedFreshIsometricAttempt) {
            Log.d(ISO_DEBUG_TAG, "fresh Isometric load detected; cleared retained graph/result state")
        }
        if (inboundFrame) {
            maybeHandleStartupImageAck(parsedPacket)
            maybeTraceStartupImageStatePacket(parsedPacket)
        }
        traceIsometricFrame(
            characteristicUuid = characteristic.uuid.toString().uppercase(),
            parsedPacket = parsedPacket,
            direction = direction,
            value = value,
            beforeSafety = priorSafetySnapshot,
            afterSafety = nextSafetySnapshot,
            afterReading = nextReadingSnapshot,
        )
        if (
            inboundFrame &&
            nextSafetySnapshot?.workoutState == VoltraControlFrames.WORKOUT_STATE_ISOMETRIC &&
            nextReadingSnapshot?.hasLiveIsometricAttempt() == true
        ) {
            pendingIsometricAutoLoad = false
            pendingIsometricLoadIssued = false
            stopPendingIsometricAutoLoadLoop()
        }
        reconcileIsometricVendorRefreshLoop()
        maybeAutoLoadIsometric(gatt)
    }

    private fun maybeHandleStartupImageAck(parsedPacket: Any?) {
        val packet = parsedPacket as? com.technogizguy.voltra.controller.protocol.ParsedVoltraPacket ?: return
        if (packet.commandId != VoltraControlFrames.CMD_STARTUP_IMAGE || packet.payload.size < 2) return
        val ackCode = packet.payload[1].toInt() and 0xFF
        when (ackCode) {
            0x03 -> {
                startupImageAckedChunkCount += 1
                Log.d(
                    STARTUP_DEBUG_TAG,
                    "ack chunk ${startupImageAckedChunkCount}/${pendingStartupImageChunkCount} seq=${packet.sequence16}",
                )
            }
            0x04 -> {
                Log.d(STARTUP_DEBUG_TAG, "ack finalize seq=${packet.sequence16}")
                logCommand(
                    VoltraCommandResult(
                        command = VoltraControlCommand.UPLOAD_STARTUP_IMAGE,
                        status = VoltraCommandStatus.CONFIRMED,
                        message = "VOLTRA acknowledged startup image finalize.",
                        timestampMillis = System.currentTimeMillis(),
                        rawHex = packet.payload.toHexString(),
                    ),
                )
            }
            0x05 -> {
                Log.d(STARTUP_DEBUG_TAG, "ack apply seq=${packet.sequence16}")
                cancelStartupImageStatePolls()
                logCommand(
                    VoltraCommandResult(
                        command = VoltraControlCommand.UPLOAD_STARTUP_IMAGE,
                        status = VoltraCommandStatus.CONFIRMED,
                        message = "VOLTRA accepted startup image transfer. Check the device for an on-unit confirmation prompt.",
                        timestampMillis = System.currentTimeMillis(),
                        rawHex = packet.payload.toHexString(),
                    ),
                )
            }
        }
    }

    private fun queueStartupImageStateRead(label: String) {
        gatt?.let { currentGatt ->
            Log.d(STARTUP_DEBUG_TAG, "queue $label")
            queueParamReadCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.UPLOAD_STARTUP_IMAGE,
                paramIds = listOf(
                    VoltraControlFrames.PARAM_EP_LOGO_APPLY_ACTION,
                    VoltraControlFrames.PARAM_POWER_OFF_LOGO_EN,
                    VoltraControlFrames.PARAM_CUSTOM_LOGO_X,
                    VoltraControlFrames.PARAM_CUSTOM_LOGO_Y,
                    VoltraControlFrames.PARAM_CUSTOM_LOGO_BG_COLOR,
                ),
                label = label,
            )
        }
    }

    private fun queueStartupImageEnableWrite(label: String) {
        gatt?.let { currentGatt ->
            Log.d(STARTUP_DEBUG_TAG, "queue $label")
            sendParamWriteCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.UPLOAD_STARTUP_IMAGE,
                paramId = VoltraControlFrames.PARAM_POWER_OFF_LOGO_EN,
                valueBytes = byteArrayOf(0x01),
                label = label,
            )
        }
    }

    private fun queueStartupImageApplyActionWrite(label: String) {
        gatt?.let { currentGatt ->
            Log.d(STARTUP_DEBUG_TAG, "queue $label")
            sendParamWriteCommand(
                gatt = currentGatt,
                command = VoltraControlCommand.UPLOAD_STARTUP_IMAGE,
                paramId = VoltraControlFrames.PARAM_EP_LOGO_APPLY_ACTION,
                valueBytes = byteArrayOf(0x01),
                label = label,
            )
        }
    }

    private fun scheduleStartupImageStatePoll(delayMillis: Long, label: String) {
        val runnable = object : Runnable {
            override fun run() {
                startupImageStatePollRunnables.remove(this)
                queueStartupImageStateRead(label)
            }
        }
        startupImageStatePollRunnables += runnable
        mainHandler.postDelayed(runnable, delayMillis)
    }

    private fun cancelStartupImageStatePolls() {
        startupImageStatePollRunnables.forEach { mainHandler.removeCallbacks(it) }
        startupImageStatePollRunnables.clear()
    }

    private fun maybeTraceStartupImageStatePacket(parsedPacket: Any?) {
        val packet = parsedPacket as? com.technogizguy.voltra.controller.protocol.ParsedVoltraPacket ?: return
        if (packet.commandId != VoltraControlFrames.CMD_PARAM_READ &&
            packet.commandId != VoltraControlFrames.CMD_PARAM_WRITE &&
            packet.commandId != 0x10
        ) return
        val trackedParamIds = intArrayOf(
            VoltraControlFrames.PARAM_EP_LOGO_APPLY_ACTION,
            VoltraControlFrames.PARAM_POWER_OFF_LOGO_EN,
            VoltraControlFrames.PARAM_CUSTOM_LOGO_X,
            VoltraControlFrames.PARAM_CUSTOM_LOGO_Y,
            VoltraControlFrames.PARAM_CUSTOM_LOGO_BG_COLOR,
        )
        val payload = packet.payload
        val matches = trackedParamIds.any { paramId ->
            val lo = (paramId and 0xFF).toByte()
            val hi = ((paramId shr 8) and 0xFF).toByte()
            (0 until (payload.size - 1)).any { i -> payload[i] == lo && payload[i + 1] == hi }
        }
        if (matches) {
            Log.d(
                STARTUP_DEBUG_TAG,
                "state packet cmd=0x${packet.commandId.toString(16)} seq=${packet.sequence16} payload=${payload.toHexString()}",
            )
        }
    }

    private fun traceIsometricFrame(
        characteristicUuid: String,
        parsedPacket: Any?,
        direction: RawFrameDirection,
        value: ByteArray,
        beforeSafety: VoltraSafetyState?,
        afterSafety: VoltraSafetyState?,
        afterReading: VoltraReading?,
    ) {
        val isIsometricState = beforeSafety?.workoutState == VoltraControlFrames.WORKOUT_STATE_ISOMETRIC ||
            afterSafety?.workoutState == VoltraControlFrames.WORKOUT_STATE_ISOMETRIC
        val packet = parsedPacket as? com.technogizguy.voltra.controller.protocol.ParsedVoltraPacket
        val payload0 = packet?.payload?.firstOrNull()?.toInt()?.and(0xFF)
        val isIsometricControlWrite = packet?.let {
            it.commandId == VoltraControlFrames.CMD_PARAM_WRITE && it.payload.size >= 4 && run {
                val paramId = (it.payload[2].toInt() and 0xFF) or ((it.payload[3].toInt() and 0xFF) shl 8)
                paramId == VoltraControlFrames.PARAM_BP_SET_FITNESS_MODE ||
                    paramId == VoltraControlFrames.PARAM_FITNESS_WORKOUT_STATE ||
                    paramId == VoltraControlFrames.PARAM_EP_SCR_SWITCH
            }
        } == true
        val isIsometricPacket = packet?.let {
            it.commandId == 0xB4 ||
                (it.commandId == VoltraControlFrames.CMD_VENDOR && payload0 in setOf(0x13, 0x80, 0x81, 0x92, 0x93))
        } == true
        val isIsometricCommandWindow =
            hasPendingCommand(VoltraControlCommand.ENTER_ISOMETRIC_MODE) ||
                hasPendingCommand(VoltraControlCommand.LOAD) ||
                hasPendingCommand(VoltraControlCommand.REFRESH_MODE_FEATURE_STATUS) ||
                inFlightWrite?.command in setOf(
                    VoltraControlCommand.ENTER_ISOMETRIC_MODE,
                    VoltraControlCommand.LOAD,
                    VoltraControlCommand.REFRESH_MODE_FEATURE_STATUS,
                )
        val shouldTraceRawFrame = direction == RawFrameDirection.NOTIFY &&
            characteristicUuid in CONFIRMED_RESPONSE_CHARACTERISTICS.union(
                setOf(VoltraUuidRegistry.VOLTRA_NOTIFY_CHARACTERISTIC_UUID),
            ) &&
            (isIsometricState || isIsometricCommandWindow)
        if (packet == null) {
            if (shouldTraceRawFrame) {
                Log.d(
                    ISO_DEBUG_TAG,
                    "dir=$direction char=$characteristicUuid raw len=${value.size} head=${value.take(8).toByteArray().toHexString()} hex=${value.toHexString()}",
                )
            }
            return
        }
        if (!isIsometricState && !isIsometricControlWrite && !isIsometricPacket && !isIsometricCommandWindow) return
        val legacyTelemetryDetail =
            if (packet.commandId == 0xAA && payload0 == 0x81) {
                buildLegacyIsometricTelemetryDetail(packet.payload)
            } else {
                null
            }
        val detailSuffix = when {
            legacyTelemetryDetail != null -> " $legacyTelemetryDetail"
            packet.commandId == 0x10 && payload0 == 0x01 ->
                " asyncWords=${packet.payload.toWordHexString()}"
            packet.commandId == 0xA7 && payload0 == 0x41 ->
                " deviceWords=${packet.payload.toWordHexString()}"
            packet.commandId == 0xB4 ->
                " b4Words=${packet.payload.toWordHexString()}"
            packet.commandId == VoltraControlFrames.CMD_VENDOR && payload0 in setOf(0x80, 0x92, 0x93) ->
                " vendorWords=${packet.payload.toWordHexString()}"
            else -> ""
        }
        Log.d(
            ISO_DEBUG_TAG,
            "dir=$direction char=$characteristicUuid cmd=0x${packet.commandId.toString(16)} p0=${payload0?.let { "0x" + it.toString(16) } ?: "--"} " +
                "mode=${afterSafety?.fitnessMode} workout=${afterSafety?.workoutState} loaded=${afterSafety?.let { VoltraControlFrames.isLoadEngagedForWorkoutState(it.fitnessMode, it.workoutState) }} " +
                "force=${afterReading?.isometricCurrentForceN} peak=${afterReading?.isometricPeakForceN} rel=${afterReading?.isometricPeakRelativeForcePercent} " +
                "elapsed=${afterReading?.isometricElapsedMillis} samples=${afterReading?.isometricWaveformSamplesN?.size} " +
                "carrier=${afterReading?.isometricCarrierForceN} carrierP=${afterReading?.isometricCarrierStatusPrimary} carrierS=${afterReading?.isometricCarrierStatusSecondary} " +
                "hex=${value.toHexString()}$detailSuffix"
        )
    }

    private fun buildLegacyIsometricTelemetryDetail(payload: ByteArray): String? {
        if (payload.size < LEGACY_ISO_DEBUG_FORCE_WORD_OFFSET + 2) return null
        val primary = payload.u16le(LEGACY_ISO_DEBUG_STATUS_PRIMARY_OFFSET)
        val secondary = payload.u16le(LEGACY_ISO_DEBUG_STATUS_SECONDARY_OFFSET)
        val tick = payload.u32le(LEGACY_ISO_DEBUG_TICK_OFFSET)
        val forceWord = payload.u16le(LEGACY_ISO_DEBUG_FORCE_WORD_OFFSET)
        val branch = when {
            secondary == 12 -> "live12"
            primary == 0 && secondary == 1 -> "coarse1"
            secondary == 10 -> "armed10"
            secondary == 11 -> "done11"
            secondary == 2 -> "ready2"
            secondary == 3 -> "progress3"
            secondary == 4 -> "active4"
            else -> "other"
        }
        return "legacyP=$primary legacyS=$secondary legacyTick=$tick legacyForceWord=$forceWord legacyBranch=$branch"
    }

    private fun ByteArray.toWordHexString(): String =
        indices
            .step(2)
            .map { index ->
                if (index + 1 < size) {
                    "%02X%02X".format(this[index + 1].toInt() and 0xFF, this[index].toInt() and 0xFF)
                } else {
                    "%02X".format(this[index].toInt() and 0xFF)
                }
            }
            .joinToString(",")

    private fun ByteArray.u16le(offset: Int): Int {
        if (offset + 1 >= size) return 0
        return (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun ByteArray.u32le(offset: Int): Long {
        if (offset + 3 >= size) return 0L
        return (this[offset].toLong() and 0xFFL) or
            ((this[offset + 1].toLong() and 0xFFL) shl 8) or
            ((this[offset + 2].toLong() and 0xFFL) shl 16) or
            ((this[offset + 3].toLong() and 0xFFL) shl 24)
    }

    private fun maybeAutoLoadIsometric(currentGatt: BluetoothGatt) {
        if (!pendingIsometricAutoLoad) return
        val current = mutableState.value
        if (current.reading.hasLiveIsometricAttempt()) {
            pendingIsometricAutoLoad = false
            pendingIsometricLoadIssued = false
            stopPendingIsometricAutoLoadLoop()
            return
        }
        if (shouldRunIsometricVendorRefresh(current)) {
            pendingIsometricAutoLoad = false
            stopPendingIsometricAutoLoadLoop()
            return
        }
        if (current.connectionState != VoltraConnectionState.CONNECTED) return
        if (current.safety.workoutState != VoltraControlFrames.WORKOUT_STATE_ISOMETRIC) return
        if (hasPendingCommand(VoltraControlCommand.ENTER_ISOMETRIC_MODE)) return
        val now = System.currentTimeMillis()
        if (!pendingIsometricLoadIssued) {
            if (!isIsometricEnterSettled(now)) return
            if (!current.safety.canLoad) return
            if (inFlightWrite != null || writeQueue.isNotEmpty()) return
            if (hasPendingCommand(VoltraControlCommand.LOAD)) return
            queueIsometricLoad(currentGatt)
            return
        }
        if (now - lastIsometricVendorRefreshAtMillis < ISOMETRIC_AUTO_LOAD_RETRY_MILLIS) {
            return
        }
        if (inFlightWrite != null) return
        if (writeQueue.any { it.command == VoltraControlCommand.REFRESH_MODE_FEATURE_STATUS }) return
        enqueueIsometricVendorRefresh(currentGatt)
        lastIsometricVendorRefreshAtMillis = now
    }

    private fun hasPendingCommand(command: VoltraControlCommand): Boolean {
        return inFlightWrite?.command == command || writeQueue.any { it.command == command }
    }

    private fun nextProtocolStatus(
        current: VoltraProtocolStatus,
        inboundFrame: Boolean,
        parsedPacketSeen: Boolean,
        validatesControlProtocol: Boolean,
    ): VoltraProtocolStatus {
        return when {
            validatesControlProtocol -> VoltraProtocolStatus.COMMAND_PROTOCOL_VALIDATED
            current == VoltraProtocolStatus.VOLTRA_GATT_MATCH && inboundFrame && parsedPacketSeen -> VoltraProtocolStatus.RAW_FRAMES_SEEN
            else -> current
        }
    }

    private fun BluetoothGattCharacteristic.isConfirmedVoltraResponseChannel(): Boolean {
        return uuid.toString().uppercase() in CONFIRMED_RESPONSE_CHARACTERISTICS
    }

    private fun BluetoothGatt.findVoltraCharacteristic(uuid: String): BluetoothGattCharacteristic? {
        val normalized = uuid.uppercase()
        return services
            .firstOrNull { it.uuid.toString().uppercase() == VoltraUuidRegistry.VOLTRA_SERVICE_UUID }
            ?.characteristics
            ?.firstOrNull { it.uuid.toString().uppercase() == normalized }
    }

    private fun List<BluetoothGattService>.toGattSnapshot(): VoltraGattSnapshot {
        return VoltraGattSnapshot(
            services = map { service ->
                VoltraGattService(
                    uuid = service.uuid.toString().uppercase(),
                    characteristics = service.characteristics.map { characteristic ->
                        val properties = characteristic.properties.toGattProperties()
                        VoltraGattCharacteristic(
                            serviceUuid = service.uuid.toString().uppercase(),
                            uuid = characteristic.uuid.toString().uppercase(),
                            properties = properties,
                            descriptors = characteristic.descriptors.map { it.uuid.toString().uppercase() },
                            candidateRole = VoltraUuidRegistry.classifyCharacteristic(
                                uuid = characteristic.uuid.toString(),
                                properties = properties,
                            ),
                        )
                    },
                )
            },
            capturedAtMillis = System.currentTimeMillis(),
        )
    }

    private fun VoltraGattSnapshot.protocolStatus(): VoltraProtocolStatus {
        val hasVoltraService = services.any { it.uuid == VoltraUuidRegistry.VOLTRA_SERVICE_UUID }
        val roles = services
            .flatMap { it.characteristics }
            .map { it.candidateRole }
            .toSet()
        val hasConfirmedVoltraChannels =
            VoltraCharacteristicRole.VOLTRA_COMMAND in roles &&
                VoltraCharacteristicRole.VOLTRA_NOTIFY in roles &&
                VoltraCharacteristicRole.VOLTRA_JUST_WRITE in roles &&
                VoltraCharacteristicRole.VOLTRA_TRANSPORT in roles
        return if (hasVoltraService && hasConfirmedVoltraChannels) {
            VoltraProtocolStatus.VOLTRA_GATT_MATCH
        } else {
            VoltraProtocolStatus.BLE_ONLY
        }
    }

    private fun Int.toGattProperties(): List<GattProperty> {
        val props = mutableListOf<GattProperty>()
        if (this and BluetoothGattCharacteristic.PROPERTY_READ != 0) props += GattProperty.READ
        if (this and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) props += GattProperty.WRITE
        if (this and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) props += GattProperty.WRITE_NO_RESPONSE
        if (this and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) props += GattProperty.NOTIFY
        if (this and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) props += GattProperty.INDICATE
        if (this and BluetoothGattCharacteristic.PROPERTY_BROADCAST != 0) props += GattProperty.BROADCAST
        if (this and BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE != 0) props += GattProperty.SIGNED_WRITE
        if (this and BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS != 0) props += GattProperty.EXTENDED
        return props
    }

    private fun Int.describeGattStatus(): String = when (this) {
        BluetoothGatt.GATT_SUCCESS -> "success"
        8 -> "connection timeout"
        19 -> "device terminated the connection (status 19)"
        22 -> "local host terminated the connection"
        62 -> "connection failed to establish"
        133 -> "Android GATT error 133"
        else -> "status $this"
    }

    private fun blocked(
        command: VoltraControlCommand,
        message: String,
    ): VoltraCommandResult {
        return logCommand(
            VoltraCommandResult(
                command = command,
                status = VoltraCommandStatus.BLOCKED,
                message = message,
                timestampMillis = System.currentTimeMillis(),
            ),
        )
    }

    private fun logCommand(result: VoltraCommandResult): VoltraCommandResult {
        mutableState.update {
            it.copy(
                commandLog = (it.commandLog + result).takeLast(MAX_COMMAND_LOG),
                statusMessage = if (
                    result.status == VoltraCommandStatus.BLOCKED &&
                    it.connectionState == VoltraConnectionState.DISCONNECTED
                ) {
                    it.statusMessage
                } else {
                    result.message
                },
            )
        }
        if (result.command in setOf(
                VoltraControlCommand.ENTER_CUSTOM_CURVE_MODE,
                VoltraControlCommand.ENTER_ISOMETRIC_MODE,
                VoltraControlCommand.LOAD,
                VoltraControlCommand.UNLOAD,
                VoltraControlCommand.EXIT_WORKOUT,
                VoltraControlCommand.REFRESH_MODE_FEATURE_STATUS,
            )
        ) {
            Log.d(ISO_DEBUG_TAG, "command=${result.command} status=${result.status} msg=${result.message}")
        }
        return result
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasConnectPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val MAX_CAPTURED_FRAMES = 500
        private const val MAX_COMMAND_LOG = 100
        private const val BOOTSTRAP_WRITE_PACING_MILLIS = 90L
        private const val ISOMETRIC_VENDOR_REFRESH_INTERVAL_MILLIS = 500L
        private const val ISOMETRIC_VENDOR_REFRESH_BURST_MILLIS = 3_000L
        private const val ISOMETRIC_VENDOR_REFRESH_TAIL_MILLIS = 1_500L
        private const val ISOMETRIC_AUTO_LOAD_INITIAL_DELAY_MILLIS = 850L
        private const val ISOMETRIC_ENTER_SETTLE_MILLIS = 850L
        private const val ISOMETRIC_AUTO_LOAD_RETRY_MILLIS = 650L
        private const val ISOMETRIC_AUTO_LOAD_MAX_ATTEMPTS = 8
        private const val DEFAULT_ISOMETRIC_MAX_DURATION_SECONDS = 15
        private const val MAX_ISOMETRIC_REFRESH_BURST_SECONDS = 20
        private const val ISO_DEBUG_TAG = "VoltraIsoDebug"
        private const val STARTUP_DEBUG_TAG = "VoltraStartupDebug"
        private const val LEGACY_ISO_DEBUG_STATUS_PRIMARY_OFFSET = 11
        private const val LEGACY_ISO_DEBUG_STATUS_SECONDARY_OFFSET = 13
        private const val LEGACY_ISO_DEBUG_TICK_OFFSET = 27
        private const val LEGACY_ISO_DEBUG_FORCE_WORD_OFFSET = 43
        private const val VOLTRA_MTU = 517
        private const val MTU_FALLBACK_DELAY_MILLIS = 1_500L
        private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private val CONFIRMED_RESPONSE_CHARACTERISTICS = setOf(
            VoltraUuidRegistry.VOLTRA_COMMAND_CHARACTERISTIC_UUID,
            VoltraUuidRegistry.VOLTRA_TRANSPORT_CHARACTERISTIC_UUID,
        )
        private val VOLTRA_OFFICIAL_NOTIFY_ROLES = setOf(
            VoltraCharacteristicRole.VOLTRA_COMMAND,
            VoltraCharacteristicRole.VOLTRA_NOTIFY,
            VoltraCharacteristicRole.VOLTRA_TRANSPORT,
        )
        private val VOLTRA_SAFE_READ_ROLES = setOf(
            VoltraCharacteristicRole.VOLTRA_COMMAND,
            VoltraCharacteristicRole.VOLTRA_TRANSPORT,
        )
        private val MODE_FEATURE_STATUS_PARAMS = listOf(
            VoltraControlFrames.PARAM_BP_BASE_WEIGHT,
            VoltraControlFrames.PARAM_RESISTANCE_BAND_MAX_FORCE,
            VoltraControlFrames.PARAM_RESISTANCE_BAND_ALGORITHM,
            VoltraControlFrames.PARAM_RESISTANCE_BAND_LEN,
            VoltraControlFrames.PARAM_RESISTANCE_BAND_LEN_BY_ROM,
            VoltraControlFrames.PARAM_RESISTANCE_EXPERIENCE,
            VoltraControlFrames.PARAM_FITNESS_ASSIST_MODE,
            VoltraControlFrames.PARAM_EP_RESISTANCE_BAND_INVERSE,
            VoltraControlFrames.PARAM_EP_MAX_ALLOWED_FORCE,
            VoltraControlFrames.PARAM_FITNESS_DAMPER_RATIO_IDX,
            VoltraControlFrames.PARAM_ISOKINETIC_ECC_MODE,
            VoltraControlFrames.PARAM_EP_ISOKINETIC_TARGET_SPEED_MM_S,
            VoltraControlFrames.PARAM_ISOKINETIC_ECC_SPEED_LIMIT,
            VoltraControlFrames.PARAM_ISOKINETIC_ECC_CONST_WEIGHT,
            VoltraControlFrames.PARAM_ISOKINETIC_ECC_OVERLOAD_WEIGHT,
            VoltraControlFrames.PARAM_ISOMETRIC_MAX_FORCE,
            VoltraControlFrames.PARAM_ISOMETRIC_MAX_DURATION,
            VoltraControlFrames.PARAM_BP_CHAINS_WEIGHT,
            VoltraControlFrames.PARAM_BP_ECCENTRIC_WEIGHT,
            VoltraControlFrames.PARAM_FITNESS_INVERSE_CHAIN,
            VoltraControlFrames.PARAM_WEIGHT_TRAINING_EXTRA_MODE,
            VoltraControlFrames.PARAM_BP_SET_FITNESS_MODE,
            VoltraControlFrames.PARAM_FITNESS_WORKOUT_STATE,
            VoltraControlFrames.PARAM_BP_RUNTIME_POSITION_CM,
            VoltraControlFrames.PARAM_MC_DEFAULT_OFFLEN_CM,
            VoltraControlFrames.PARAM_QUICK_CABLE_ADJUSTMENT,
        )
        private val STRENGTH_FEATURE_STATUS_PARAMS = MODE_FEATURE_STATUS_PARAMS
    }
}

private data class DescriptorWrite(
    val gatt: BluetoothGatt,
    val descriptor: BluetoothGattDescriptor,
    val value: ByteArray,
    val characteristicUuid: String,
)

private data class CharacteristicRead(
    val gatt: BluetoothGatt,
    val characteristic: BluetoothGattCharacteristic,
    val characteristicUuid: String,
)

private data class CharacteristicWrite(
    val gatt: BluetoothGatt,
    val characteristic: BluetoothGattCharacteristic,
    val packet: VoltraBootstrapPacket,
    val characteristicUuid: String,
    val command: VoltraControlCommand = VoltraControlCommand.READ_ONLY_HANDSHAKE_PROBE,
)

private data class ParamWriteSpec(
    val paramId: Int,
    val valueBytes: ByteArray,
    val label: String,
)

private data class QueuedFrameSpec(
    val label: String,
    val bytes: ByteArray,
)

private fun Weight.toCommandPounds(min: Int, max: Int): Int {
    val pounds = when (unit) {
        WeightUnit.LB -> value
        WeightUnit.KG -> value * KG_TO_LB
    }
    return pounds.roundToInt().coerceIn(min, max)
}

private fun Int.uint16Le(): ByteArray {
    return byteArrayOf(
        (this and 0xFF).toByte(),
        ((this shr 8) and 0xFF).toByte(),
    )
}

private fun Int.uint32Le(): ByteArray {
    return byteArrayOf(
        (this and 0xFF).toByte(),
        ((this shr 8) and 0xFF).toByte(),
        ((this shr 16) and 0xFF).toByte(),
        ((this shr 24) and 0xFF).toByte(),
    )
}

private fun VoltraReading.clearIsometricTestState(): VoltraReading {
    return copy(
        isometricCurrentForceN = null,
        isometricPeakForceN = null,
        isometricPeakRelativeForcePercent = null,
        isometricElapsedMillis = null,
        isometricTelemetryTick = null,
        isometricTelemetryStartTick = null,
        isometricCarrierForceN = null,
        isometricCarrierStatusPrimary = null,
        isometricCarrierStatusSecondary = null,
        isometricWaveformSamplesN = emptyList(),
        isometricWaveformLastChunkIndex = null,
    )
}

private fun VoltraReading.hasLiveIsometricAttempt(): Boolean {
    return isometricCurrentForceN != null ||
        isometricWaveformSamplesN.isNotEmpty() ||
        (isometricPeakForceN != null && isometricElapsedMillis != null)
}

private const val KG_TO_LB = 2.2046226218
