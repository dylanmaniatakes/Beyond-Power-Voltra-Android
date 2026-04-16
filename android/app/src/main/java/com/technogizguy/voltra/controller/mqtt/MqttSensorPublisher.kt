package com.technogizguy.voltra.controller.mqtt

import android.content.Context
import com.technogizguy.voltra.controller.LocalPreferences
import com.technogizguy.voltra.controller.MqttPreferences
import com.technogizguy.voltra.controller.model.VoltraConnectionState
import com.technogizguy.voltra.controller.model.VoltraSessionState
import com.technogizguy.voltra.controller.model.WeightUnit
import com.technogizguy.voltra.controller.protocol.VoltraControlFrames
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.EOFException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets.UTF_8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class MqttPublisherConnectionState {
    DISABLED,
    CONNECTING,
    CONNECTED,
    ERROR,
}

data class MqttPublisherState(
    val connectionState: MqttPublisherConnectionState = MqttPublisherConnectionState.DISABLED,
    val brokerEndpoint: String? = null,
    val topicPrefix: String? = null,
    val lastError: String? = null,
    val lastPublishedMillis: Long? = null,
    val publishedTopicCount: Int = 0,
)

@OptIn(FlowPreview::class)
class MqttSensorPublisher(
    context: Context,
    preferencesFlow: Flow<LocalPreferences>,
    sessionFlow: StateFlow<VoltraSessionState>,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(MqttPublisherState())
    private var mqttClient: SimpleMqttClient? = null
    private var connectionKey: MqttConnectionKey? = null
    private var discoveryKey: String? = null
    private var publishedSnapshotKey: String? = null
    private var lastAvailabilityTopic: String? = null
    private var lastBaseTopic: String? = null
    private var latestPreferences: MqttPreferences = MqttPreferences()

    val state: StateFlow<MqttPublisherState> = mutableState.asStateFlow()

    init {
        scope.launch {
            combine(preferencesFlow, sessionFlow) { preferences, session ->
                preferences to session
            }
                .debounce(250)
                .collect { (preferences, session) ->
                    latestPreferences = preferences.mqtt
                    handleUpdate(preferences.mqtt, session)
                }
        }
    }

    fun close() {
        scope.cancel()
    }

    fun publishNow(session: VoltraSessionState) {
        scope.launch {
            handleUpdate(latestPreferences, session, forcePublish = true)
        }
    }

    private suspend fun handleUpdate(
        mqtt: MqttPreferences,
        session: VoltraSessionState,
        forcePublish: Boolean = false,
    ) {
        if (!mqtt.enabled) {
            disconnectClient(publishOffline = true)
            mutableState.value = MqttPublisherState(connectionState = MqttPublisherConnectionState.DISABLED)
            return
        }

        val validationError = validate(mqtt)
        if (validationError != null) {
            disconnectClient(publishOffline = true)
            mutableState.value = MqttPublisherState(
                connectionState = MqttPublisherConnectionState.ERROR,
                brokerEndpoint = mqtt.endpoint(),
                topicPrefix = mqtt.topicPrefix,
                lastError = validationError,
            )
            return
        }

        val topics = MqttTopicSet.from(mqtt = mqtt, session = session)
        val deviceConfig = MqttDeviceConfig.from(session, topics.deviceKey)
        val key = MqttConnectionKey(
            host = mqtt.host.trim(),
            port = mqtt.port,
            username = mqtt.username.takeIf { it.isNotBlank() },
            password = mqtt.password.takeIf { it.isNotBlank() },
            clientId = buildClientId(topics.deviceKey),
        )

        val client = ensureConnected(key, mqtt, topics.baseTopic) ?: return
        lastAvailabilityTopic = topics.availabilityTopic
        lastBaseTopic = topics.baseTopic

        val discoveryFingerprint = "${mqtt.discoveryEnabled}|${mqtt.discoveryPrefix}|${topics.baseTopic}|${topics.deviceKey}|${deviceConfig.name}"
        val discoveryTopicsPublished = if (mqtt.discoveryEnabled && discoveryKey != discoveryFingerprint) {
            publishDiscoveryConfigs(
                client = client,
                mqtt = mqtt,
                topics = topics,
                device = deviceConfig,
            )
            discoveryKey = discoveryFingerprint
            true
        } else {
            false
        }

        val statePayload = buildStatePayload(session)
        val snapshotKey = "${topics.baseTopic}|${statePayload.toString()}"
        if (!forcePublish && publishedSnapshotKey == snapshotKey && !discoveryTopicsPublished) {
            mutableState.value = mutableState.value.copy(
                connectionState = MqttPublisherConnectionState.CONNECTED,
                brokerEndpoint = mqtt.endpoint(),
                topicPrefix = topics.baseTopic,
                lastError = null,
            )
            return
        }

        val scalarTopics = buildScalarTopics(topics = topics, session = session)
        try {
            client.publish(topics.availabilityTopic, "online".encodeToByteArray(), retained = true)
            client.publish(topics.jsonStateTopic, statePayload.toString().encodeToByteArray(), retained = true)
            scalarTopics.forEach { (topic, value) ->
                client.publish(topic, value.encodeToByteArray(), retained = true)
            }
        } catch (error: Exception) {
            handleUnexpectedDisconnect("MQTT publish failed: ${error.message ?: error::class.java.simpleName}")
            return
        }

        publishedSnapshotKey = snapshotKey
        mutableState.value = MqttPublisherState(
            connectionState = MqttPublisherConnectionState.CONNECTED,
            brokerEndpoint = mqtt.endpoint(),
            topicPrefix = topics.baseTopic,
            lastError = null,
            lastPublishedMillis = System.currentTimeMillis(),
            publishedTopicCount = scalarTopics.size + 2,
        )
    }

    private suspend fun ensureConnected(
        key: MqttConnectionKey,
        mqtt: MqttPreferences,
        baseTopic: String,
    ): SimpleMqttClient? {
        val existingClient = mqttClient
        if (existingClient != null && connectionKey == key && existingClient.isConnected) {
            return existingClient
        }

        disconnectClient(publishOffline = true)
        mutableState.value = MqttPublisherState(
            connectionState = MqttPublisherConnectionState.CONNECTING,
            brokerEndpoint = mqtt.endpoint(),
            topicPrefix = baseTopic,
        )

        return try {
            val client = SimpleMqttClient(
                connection = key,
                onUnexpectedDisconnect = ::handleUnexpectedDisconnect,
            )
            client.connect()
            mqttClient = client
            connectionKey = key
            discoveryKey = null
            publishedSnapshotKey = null
            mutableState.value = MqttPublisherState(
                connectionState = MqttPublisherConnectionState.CONNECTED,
                brokerEndpoint = mqtt.endpoint(),
                topicPrefix = baseTopic,
            )
            client
        } catch (error: Exception) {
            mqttClient = null
            connectionKey = null
            mutableState.value = MqttPublisherState(
                connectionState = MqttPublisherConnectionState.ERROR,
                brokerEndpoint = mqtt.endpoint(),
                topicPrefix = baseTopic,
                lastError = "MQTT connect failed: ${error.message ?: error::class.java.simpleName}",
            )
            null
        }
    }

    private suspend fun disconnectClient(publishOffline: Boolean) {
        val client = mqttClient ?: return
        if (publishOffline) {
            runCatching {
                lastAvailabilityTopic?.let { topic ->
                    client.publish(topic, "offline".encodeToByteArray(), retained = true)
                }
            }
        }
        client.disconnect()
        mqttClient = null
        connectionKey = null
        discoveryKey = null
        publishedSnapshotKey = null
        lastAvailabilityTopic = null
        lastBaseTopic = null
    }

    private fun handleUnexpectedDisconnect(message: String) {
        mqttClient = null
        connectionKey = null
        discoveryKey = null
        publishedSnapshotKey = null
        mutableState.value = mutableState.value.copy(
            connectionState = if (latestPreferences.enabled) MqttPublisherConnectionState.ERROR else MqttPublisherConnectionState.DISABLED,
            lastError = message,
        )
    }

    private suspend fun publishDiscoveryConfigs(
        client: SimpleMqttClient,
        mqtt: MqttPreferences,
        topics: MqttTopicSet,
        device: MqttDeviceConfig,
    ) {
        DISCOVERY_SENSORS.forEach { sensor ->
            val topic = "${mqtt.discoveryPrefix.trim().trim('/')}/sensor/${topics.deviceKey}/${sensor.objectId}/config"
            val payload = buildJsonObject {
                put("name", sensor.name)
                put("unique_id", "${topics.deviceKey}_${sensor.objectId}")
                put("state_topic", "${topics.baseTopic}/${sensor.stateTopicSuffix}")
                put("availability_topic", topics.availabilityTopic)
                put("payload_available", "online")
                put("payload_not_available", "offline")
                sensor.unit?.let { put("unit_of_measurement", it) }
                sensor.deviceClass?.let { put("device_class", it) }
                sensor.stateClass?.let { put("state_class", it) }
                sensor.icon?.let { put("icon", it) }
                put("device", device.asJson())
            }
            client.publish(topic, payload.toString().encodeToByteArray(), retained = true)
        }

        DISCOVERY_BINARY_SENSORS.forEach { sensor ->
            val topic = "${mqtt.discoveryPrefix.trim().trim('/')}/binary_sensor/${topics.deviceKey}/${sensor.objectId}/config"
            val payload = buildJsonObject {
                put("name", sensor.name)
                put("unique_id", "${topics.deviceKey}_${sensor.objectId}")
                put("state_topic", "${topics.baseTopic}/${sensor.stateTopicSuffix}")
                put("availability_topic", topics.availabilityTopic)
                put("payload_available", "online")
                put("payload_not_available", "offline")
                put("payload_on", "ON")
                put("payload_off", "OFF")
                sensor.icon?.let { put("icon", it) }
                put("device", device.asJson())
            }
            client.publish(topic, payload.toString().encodeToByteArray(), retained = true)
        }
    }

    private fun buildStatePayload(session: VoltraSessionState): JsonObject {
        fun Boolean?.asJsonText(): String? = this?.let { if (it) "ON" else "OFF" }
        fun numberOrBlank(value: Number?) = value?.let(::JsonPrimitive) ?: JsonPrimitive("")
        fun textOrBlank(value: String?) = JsonPrimitive(value.orEmpty())

        return buildJsonObject {
            put("connection_state", textOrBlank(session.connectionState.name))
            put("protocol_status", textOrBlank(session.protocolStatus.name))
            put("status_message", textOrBlank(session.statusMessage))
            put("device_name", textOrBlank(session.currentDevice?.name))
            put("device_address", textOrBlank(session.currentDevice?.address))
            put("battery_percent", numberOrBlank(session.reading.batteryPercent))
            put("target_load_lb", numberOrBlank(session.reading.weightLb))
            put("force_lb", numberOrBlank(session.reading.forceLb))
            put("chains_lb", numberOrBlank(session.reading.chainsWeightLb))
            put("eccentric_lb", numberOrBlank(session.reading.eccentricWeightLb))
            put("inverse_chains", textOrBlank(session.reading.inverseChains.asJsonText()))
            put("resistance_band_force_lb", numberOrBlank(session.reading.resistanceBandMaxForceLb))
            put("cable_length_cm", numberOrBlank(session.reading.cableLengthCm))
            put("cable_offset_cm", numberOrBlank(session.reading.cableOffsetCm))
            put("rep_count", numberOrBlank(session.reading.repCount))
            put("set_count", numberOrBlank(session.reading.setCount))
            put("rep_phase", textOrBlank(session.reading.repPhase))
            put("workout_mode", textOrBlank(session.reading.workoutMode))
            put("control_validated", textOrBlank(if (session.controlCommandsEnabled) "ON" else "OFF"))
            put("loaded", textOrBlank(if (VoltraControlFrames.isLoadedFitnessMode(session.safety.fitnessMode)) "ON" else "OFF"))
        }
    }

    private fun buildScalarTopics(
        topics: MqttTopicSet,
        session: VoltraSessionState,
    ): Map<String, String> {
        fun text(value: Any?): String = when (value) {
            null -> ""
            is Double -> if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
            else -> value.toString()
        }

        return linkedMapOf(
            "${topics.baseTopic}/connection_state" to session.connectionState.name,
            "${topics.baseTopic}/protocol_status" to session.protocolStatus.name,
            "${topics.baseTopic}/status_message" to session.statusMessage,
            "${topics.baseTopic}/device_name" to (session.currentDevice?.name ?: ""),
            "${topics.baseTopic}/device_address" to (session.currentDevice?.address ?: ""),
            "${topics.baseTopic}/battery_percent" to text(session.reading.batteryPercent),
            "${topics.baseTopic}/target_load_lb" to text(session.reading.weightLb ?: session.targetLoad.takeIf { it.unit == WeightUnit.LB }?.value),
            "${topics.baseTopic}/force_lb" to text(session.reading.forceLb),
            "${topics.baseTopic}/chains_lb" to text(session.reading.chainsWeightLb),
            "${topics.baseTopic}/eccentric_lb" to text(session.reading.eccentricWeightLb),
            "${topics.baseTopic}/inverse_chains" to if (session.reading.inverseChains == true) "ON" else "OFF",
            "${topics.baseTopic}/resistance_band_force_lb" to text(session.reading.resistanceBandMaxForceLb),
            "${topics.baseTopic}/cable_length_cm" to text(session.reading.cableLengthCm),
            "${topics.baseTopic}/cable_offset_cm" to text(session.reading.cableOffsetCm),
            "${topics.baseTopic}/rep_count" to text(session.reading.repCount),
            "${topics.baseTopic}/set_count" to text(session.reading.setCount),
            "${topics.baseTopic}/rep_phase" to (session.reading.repPhase ?: ""),
            "${topics.baseTopic}/workout_mode" to (session.reading.workoutMode ?: ""),
            "${topics.baseTopic}/connected" to if (session.connectionState == VoltraConnectionState.CONNECTED) "ON" else "OFF",
            "${topics.baseTopic}/loaded" to if (VoltraControlFrames.isLoadedFitnessMode(session.safety.fitnessMode)) "ON" else "OFF",
            "${topics.baseTopic}/control_validated" to if (session.controlCommandsEnabled) "ON" else "OFF",
        )
    }

    private fun validate(mqtt: MqttPreferences): String? = when {
        mqtt.host.isBlank() -> "Broker host is required."
        mqtt.topicPrefix.isBlank() -> "Topic prefix is required."
        mqtt.discoveryEnabled && mqtt.discoveryPrefix.isBlank() -> "Discovery prefix is required."
        mqtt.password.isNotBlank() && mqtt.username.isBlank() -> "Username is required when a password is set."
        else -> null
    }

    private fun buildClientId(deviceKey: String): String {
        val seed = "voltra-control-$deviceKey".hashCode().toUInt().toString(16)
        return "voltra-control-$seed".take(23)
    }

    private fun MqttPreferences.endpoint(): String = "${host.trim()}:${port}"
}

internal data class MqttConnectionKey(
    val host: String,
    val port: Int,
    val username: String?,
    val password: String?,
    val clientId: String,
)

private data class MqttTopicSet(
    val deviceKey: String,
    val baseTopic: String,
    val availabilityTopic: String,
    val jsonStateTopic: String,
) {
    companion object {
        fun from(
            mqtt: MqttPreferences,
            session: VoltraSessionState,
        ): MqttTopicSet {
            val deviceKey = sanitizeSegment(
                session.currentDevice?.address
                    ?: session.currentDevice?.id
                    ?: "voltra",
            )
            val prefix = mqtt.topicPrefix.trim().trim('/').ifBlank { "voltra_control" }
            val baseTopic = "$prefix/$deviceKey"
            return MqttTopicSet(
                deviceKey = deviceKey,
                baseTopic = baseTopic,
                availabilityTopic = "$baseTopic/availability",
                jsonStateTopic = "$baseTopic/state/json",
            )
        }
    }
}

private data class MqttDeviceConfig(
    val id: String,
    val name: String,
) {
    fun asJson(): JsonObject = buildJsonObject {
        put("identifiers", buildJsonArray { add(JsonPrimitive(id)) })
        put("name", name)
        put("manufacturer", "Voltra")
        put("model", "Voltra")
        put("sw_version", "Voltra Controller")
    }

    companion object {
        fun from(
            session: VoltraSessionState,
            deviceKey: String,
        ): MqttDeviceConfig {
            return MqttDeviceConfig(
                id = deviceKey,
                name = session.currentDevice?.name?.ifBlank { "Voltra" } ?: "Voltra",
            )
        }
    }
}

private data class DiscoverySensor(
    val objectId: String,
    val name: String,
    val stateTopicSuffix: String,
    val unit: String? = null,
    val deviceClass: String? = null,
    val stateClass: String? = null,
    val icon: String? = null,
)

private data class DiscoveryBinarySensor(
    val objectId: String,
    val name: String,
    val stateTopicSuffix: String,
    val icon: String? = null,
)

private val DISCOVERY_SENSORS = listOf(
    DiscoverySensor("battery", "Battery", "battery_percent", unit = "%", deviceClass = "battery", stateClass = "measurement"),
    DiscoverySensor("target_load", "Target Load", "target_load_lb", unit = "lb", stateClass = "measurement", icon = "mdi:dumbbell"),
    DiscoverySensor("force", "Force", "force_lb", unit = "lb", stateClass = "measurement", icon = "mdi:scale"),
    DiscoverySensor("chains", "Chains", "chains_lb", unit = "lb", stateClass = "measurement", icon = "mdi:link-variant"),
    DiscoverySensor("eccentric", "Eccentric", "eccentric_lb", unit = "lb", stateClass = "measurement", icon = "mdi:arrow-expand-vertical"),
    DiscoverySensor("resistance_band_force", "Resistance Band Force", "resistance_band_force_lb", unit = "lb", stateClass = "measurement", icon = "mdi:resistor"),
    DiscoverySensor("cable_length", "Cable Length", "cable_length_cm", unit = "cm", stateClass = "measurement", icon = "mdi:ruler"),
    DiscoverySensor("cable_offset", "Cable Offset", "cable_offset_cm", unit = "cm", stateClass = "measurement", icon = "mdi:tape-measure"),
    DiscoverySensor("rep_count", "Rep Count", "rep_count", stateClass = "measurement", icon = "mdi:counter"),
    DiscoverySensor("set_count", "Set Count", "set_count", stateClass = "measurement", icon = "mdi:counter"),
    DiscoverySensor("rep_phase", "Rep Phase", "rep_phase", icon = "mdi:waves-arrow-right"),
    DiscoverySensor("workout_mode", "Workout Mode", "workout_mode", icon = "mdi:arm-flex"),
    DiscoverySensor("connection_state", "Connection State", "connection_state", icon = "mdi:bluetooth-connect"),
    DiscoverySensor("protocol_status", "Protocol Status", "protocol_status", icon = "mdi:bluetooth-audio"),
)

private val DISCOVERY_BINARY_SENSORS = listOf(
    DiscoveryBinarySensor("connected", "Connected", "connected", icon = "mdi:bluetooth"),
    DiscoveryBinarySensor("loaded", "Loaded", "loaded", icon = "mdi:weight-lifter"),
    DiscoveryBinarySensor("control_validated", "Control Validated", "control_validated", icon = "mdi:shield-check"),
    DiscoveryBinarySensor("inverse_chains", "Inverse Chains", "inverse_chains", icon = "mdi:link"),
)

private fun sanitizeSegment(value: String): String {
    val cleaned = buildString(value.length) {
        value.lowercase().forEach { char ->
            when {
                char.isLetterOrDigit() -> append(char)
                else -> append('_')
            }
        }
    }.trim('_')
    return cleaned.ifBlank { "voltra" }
}

internal object MqttPacketEncoder {
    fun connectPacket(connection: MqttConnectionKey): ByteArray {
        val flags = buildConnectFlags(connection)
        val variableHeader = mqttString("MQTT") +
            byteArrayOf(0x04, flags, 0x00, MQTT_KEEPALIVE_SECONDS.toByte())
        val payloadParts = buildList {
            add(mqttString(connection.clientId))
            connection.username?.let { add(mqttString(it)) }
            connection.password?.let { add(mqttString(it)) }
        }
        val payload = payloadParts.fold(ByteArray(0)) { acc, bytes -> acc + bytes }
        val remaining = variableHeader + payload
        return byteArrayOf(0x10) + encodeRemainingLength(remaining.size) + remaining
    }

    fun publishPacket(
        topic: String,
        payload: ByteArray,
        retained: Boolean,
    ): ByteArray {
        val fixedHeader = if (retained) 0x31 else 0x30
        val variableHeader = mqttString(topic)
        val remaining = variableHeader + payload
        return byteArrayOf(fixedHeader.toByte()) + encodeRemainingLength(remaining.size) + remaining
    }

    fun pingRequestPacket(): ByteArray = byteArrayOf(0xC0.toByte(), 0x00)

    fun disconnectPacket(): ByteArray = byteArrayOf(0xE0.toByte(), 0x00)

    internal fun encodeRemainingLength(length: Int): ByteArray {
        require(length >= 0) { "MQTT remaining length must be non-negative." }
        var value = length
        val bytes = mutableListOf<Byte>()
        do {
            var encoded = value % 128
            value /= 128
            if (value > 0) encoded = encoded or 0x80
            bytes += encoded.toByte()
        } while (value > 0)
        return bytes.toByteArray()
    }

    private fun mqttString(value: String): ByteArray {
        val bytes = value.toByteArray(UTF_8)
        require(bytes.size <= 0xFFFF) { "MQTT string is too long." }
        return byteArrayOf(
            ((bytes.size shr 8) and 0xFF).toByte(),
            (bytes.size and 0xFF).toByte(),
        ) + bytes
    }

    private fun buildConnectFlags(connection: MqttConnectionKey): Byte {
        var flags = 0x02
        if (connection.username != null) flags = flags or 0x80
        if (connection.password != null) flags = flags or 0x40
        return flags.toByte()
    }

    private const val MQTT_KEEPALIVE_SECONDS = 30
}

private class SimpleMqttClient(
    private val connection: MqttConnectionKey,
    private val onUnexpectedDisconnect: (String) -> Unit,
) {
    private val writeMutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var socket: Socket? = null
    private var input: BufferedInputStream? = null
    private var output: BufferedOutputStream? = null
    private var readerJob: Job? = null
    private var pingJob: Job? = null

    val isConnected: Boolean
        get() = socket?.isConnected == true && socket?.isClosed == false

    suspend fun connect() {
        val newSocket = Socket()
        newSocket.tcpNoDelay = true
        newSocket.connect(InetSocketAddress(connection.host, connection.port), 5_000)
        val newInput = BufferedInputStream(newSocket.getInputStream())
        val newOutput = BufferedOutputStream(newSocket.getOutputStream())

        newOutput.write(MqttPacketEncoder.connectPacket(connection))
        newOutput.flush()

        val packetType = newInput.read()
        if (packetType < 0) {
            throw EOFException("Broker closed the connection during CONNECT.")
        }
        val remainingLength = readRemainingLength(newInput)
        val payload = ByteArray(remainingLength)
        newInput.readFully(payload)
        if ((packetType shr 4) != 0x02 || payload.size < 2) {
            throw IllegalStateException("Expected CONNACK from MQTT broker.")
        }
        val returnCode = payload[1].toInt() and 0xFF
        if (returnCode != 0) {
            throw IllegalStateException("MQTT broker refused the connection (code=$returnCode).")
        }

        socket = newSocket
        input = newInput
        output = newOutput
        readerJob = scope.launch {
            try {
                readLoop()
            } catch (error: Exception) {
                if (isConnected) {
                    onUnexpectedDisconnect("MQTT connection lost: ${error.message ?: error::class.java.simpleName}")
                }
            }
        }
        pingJob = scope.launch {
            while (true) {
                delay(20_000)
                publishPacket(MqttPacketEncoder.pingRequestPacket())
            }
        }
    }

    suspend fun publish(
        topic: String,
        payload: ByteArray,
        retained: Boolean,
    ) {
        publishPacket(MqttPacketEncoder.publishPacket(topic, payload, retained))
    }

    suspend fun disconnect() {
        runCatching {
            publishPacket(MqttPacketEncoder.disconnectPacket())
        }
        readerJob?.cancel()
        pingJob?.cancel()
        scope.cancel()
        runCatching { input?.close() }
        runCatching { output?.close() }
        runCatching { socket?.close() }
        input = null
        output = null
        socket = null
    }

    private suspend fun publishPacket(packet: ByteArray) {
        val currentOutput = output ?: throw IllegalStateException("MQTT output stream is unavailable.")
        writeMutex.withLock {
            currentOutput.write(packet)
            currentOutput.flush()
        }
    }

    private fun readLoop() {
        val currentInput = input ?: return
        while (true) {
            val header = currentInput.read()
            if (header < 0) {
                throw EOFException("MQTT broker closed the socket.")
            }
            val remainingLength = readRemainingLength(currentInput)
            val payload = ByteArray(remainingLength)
            currentInput.readFully(payload)
            when ((header shr 4) and 0x0F) {
                0x0D -> Unit
                else -> Unit
            }
        }
    }

    private fun readRemainingLength(input: InputStream): Int {
        var multiplier = 1
        var value = 0
        while (true) {
            val encodedByte = input.read()
            if (encodedByte < 0) {
                throw EOFException("Unexpected EOF while reading MQTT remaining length.")
            }
            value += (encodedByte and 0x7F) * multiplier
            if ((encodedByte and 0x80) == 0) {
                return value
            }
            multiplier *= 128
            if (multiplier > 128 * 128 * 128) {
                throw IllegalStateException("Malformed MQTT remaining length.")
            }
        }
    }

    private fun InputStream.readFully(buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val readCount = read(buffer, offset, buffer.size - offset)
            if (readCount < 0) {
                throw EOFException("Unexpected EOF while reading MQTT packet.")
            }
            offset += readCount
        }
    }
}
