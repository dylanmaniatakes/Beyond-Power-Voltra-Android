package com.technogizguy.voltra.controller.http

import android.content.Context
import com.technogizguy.voltra.controller.DEFAULT_HTTP_GATEWAY_PORT
import com.technogizguy.voltra.controller.HttpGatewayPreferences
import com.technogizguy.voltra.controller.LocalPreferences
import com.technogizguy.voltra.controller.model.VoltraClient
import com.technogizguy.voltra.controller.model.VoltraCommandResult
import com.technogizguy.voltra.controller.model.VoltraConnectionState
import com.technogizguy.voltra.controller.model.VoltraSessionState
import com.technogizguy.voltra.controller.model.Weight
import com.technogizguy.voltra.controller.model.WeightUnit
import com.technogizguy.voltra.controller.protocol.VoltraControlFrames
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.EOFException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

enum class HttpGatewayConnectionState {
    DISABLED,
    STARTING,
    RUNNING,
    ERROR,
}

data class HttpGatewayState(
    val connectionState: HttpGatewayConnectionState = HttpGatewayConnectionState.DISABLED,
    val port: Int = DEFAULT_HTTP_GATEWAY_PORT,
    val urls: List<String> = emptyList(),
    val lastError: String? = null,
    val requestCount: Int = 0,
    val lastRequestMillis: Long? = null,
)

@Serializable
private data class HttpGatewayCommandDescriptor(
    val method: String,
    val path: String,
    val description: String,
    val body: JsonObject? = null,
)

class HttpGatewayServer(
    context: Context,
    preferencesFlow: Flow<LocalPreferences>,
    private val sessionFlow: StateFlow<VoltraSessionState>,
    private val client: VoltraClient,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(HttpGatewayState())
    private val serverMutex = Mutex()
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private var latestPreferences = HttpGatewayPreferences()
    private var latestLocalPreferences = LocalPreferences()
    private var acceptJob: Job? = null
    private var serverSocket: ServerSocket? = null

    val state: StateFlow<HttpGatewayState> = mutableState.asStateFlow()

    init {
        scope.launch {
            preferencesFlow.collectLatest { preferences ->
                latestLocalPreferences = preferences
            }
        }
        scope.launch {
            preferencesFlow
                .map { it.httpGateway }
                .collectLatest { gatewayPreferences ->
                    latestPreferences = gatewayPreferences
                    updateServer(gatewayPreferences)
                }
        }
    }

    fun close() {
        scope.cancel()
        runCatching { serverSocket?.close() }
    }

    private suspend fun updateServer(preferences: HttpGatewayPreferences) {
        serverMutex.withLock {
            if (!preferences.enabled) {
                stopServerLocked()
                mutableState.value = HttpGatewayState(
                    connectionState = HttpGatewayConnectionState.DISABLED,
                    port = preferences.port,
                )
                return
            }

            val existingServer = serverSocket
            if (existingServer != null && existingServer.localPort == preferences.port && acceptJob?.isActive == true) {
                mutableState.value = mutableState.value.copy(
                    connectionState = HttpGatewayConnectionState.RUNNING,
                    port = preferences.port,
                    urls = buildGatewayUrls(preferences.port),
                    lastError = null,
                )
                return
            }

            stopServerLocked()
            mutableState.value = HttpGatewayState(
                connectionState = HttpGatewayConnectionState.STARTING,
                port = preferences.port,
            )

            try {
                val socket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(preferences.port))
                }
                serverSocket = socket
                acceptJob = scope.launch {
                    acceptLoop(socket)
                }
                mutableState.value = HttpGatewayState(
                    connectionState = HttpGatewayConnectionState.RUNNING,
                    port = preferences.port,
                    urls = buildGatewayUrls(preferences.port),
                )
            } catch (error: Exception) {
                mutableState.value = HttpGatewayState(
                    connectionState = HttpGatewayConnectionState.ERROR,
                    port = preferences.port,
                    lastError = "HTTP gateway failed to start: ${error.message ?: error::class.java.simpleName}",
                )
            }
        }
    }

    private suspend fun stopServerLocked() {
        acceptJob?.cancel()
        acceptJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    private suspend fun acceptLoop(server: ServerSocket) {
        while (true) {
            val socket = try {
                server.accept()
            } catch (_: SocketException) {
                break
            } catch (_: EOFException) {
                break
            }
            scope.launch {
                handleSocket(socket)
            }
        }
    }

    private suspend fun handleSocket(socket: Socket) {
        socket.use { clientSocket ->
            runCatching {
                clientSocket.soTimeout = 5_000
                val input = BufferedInputStream(clientSocket.getInputStream())
                val output = BufferedOutputStream(clientSocket.getOutputStream())
                val request = readRequest(input)
                val response = route(request)
                writeResponse(output, response)
                mutableState.value = mutableState.value.copy(
                    requestCount = mutableState.value.requestCount + 1,
                    lastRequestMillis = System.currentTimeMillis(),
                )
            }.onFailure { error ->
                runCatching {
                    BufferedOutputStream(clientSocket.getOutputStream()).use { output ->
                        writeResponse(
                            output,
                            HttpResponse(
                                statusCode = 500,
                                body = buildJsonObject {
                                    put("ok", false)
                                    put("error", "HTTP gateway error: ${error.message ?: error::class.java.simpleName}")
                                },
                            ),
                        )
                    }
                }
            }
        }
    }

    private suspend fun route(request: HttpRequest): HttpResponse {
        val path = request.path.substringBefore('?')
        if (path == "/health") {
            return HttpResponse(
                statusCode = 200,
                body = buildJsonObject {
                    put("ok", true)
                    put("service", "voltra-http-gateway")
                    put("status", mutableState.value.connectionState.name.lowercase(Locale.US))
                    put("port", latestPreferences.port)
                },
            )
        }

        if (!authorized(request)) {
            return HttpResponse(
                statusCode = 401,
                body = buildJsonObject {
                    put("ok", false)
                    put("error", "Missing or invalid HTTP gateway key.")
                },
            )
        }

        return when {
            request.method == "GET" && path == "/v1/info" -> gatewayInfoResponse()
            request.method == "GET" && path == "/v1/state" -> stateResponse()
            request.method == "POST" && path.startsWith("/v1/commands") -> commandResponse(path, request.body)
            else -> HttpResponse(
                statusCode = 404,
                body = buildJsonObject {
                    put("ok", false)
                    put("error", "Endpoint not found.")
                },
            )
        }
    }

    private fun authorized(request: HttpRequest): Boolean {
        val configuredKey = latestPreferences.accessKey
        if (configuredKey.isBlank()) return false
        val headerKey = request.headers["x-voltra-key"]
        if (headerKey == configuredKey) return true
        val authorization = request.headers["authorization"] ?: return false
        return authorization == "Bearer $configuredKey"
    }

    private fun gatewayInfoResponse(): HttpResponse {
        val commands = listOf(
            HttpGatewayCommandDescriptor("POST", "/v1/commands/load", "Load or arm the active mode."),
            HttpGatewayCommandDescriptor("POST", "/v1/commands/unload", "Unload the active mode."),
            HttpGatewayCommandDescriptor("POST", "/v1/commands/exit", "Exit the current workout mode."),
            HttpGatewayCommandDescriptor("POST", "/v1/commands/mode/weight-training", "Switch the Voltra into Weight Training."),
            HttpGatewayCommandDescriptor("POST", "/v1/commands/mode/resistance-band", "Switch the Voltra into Resistance Band."),
            HttpGatewayCommandDescriptor("POST", "/v1/commands/mode/damper", "Switch the Voltra into Damper."),
            HttpGatewayCommandDescriptor("POST", "/v1/commands/mode/isokinetic", "Switch the Voltra into Isokinetic."),
            HttpGatewayCommandDescriptor("POST", "/v1/commands/mode/isometric", "Switch the Voltra into Isometric Test."),
            HttpGatewayCommandDescriptor(
                "POST",
                "/v1/commands/target-load",
                "Set the Weight Training target load.",
                buildJsonObject {
                    put("value", 25)
                    put("unit", "lb")
                },
            ),
            HttpGatewayCommandDescriptor(
                "POST",
                "/v1/commands/assist",
                "Enable or disable Assist.",
                buildJsonObject { put("enabled", true) },
            ),
            HttpGatewayCommandDescriptor(
                "POST",
                "/v1/commands/chains",
                "Set the chains amount.",
                buildJsonObject {
                    put("value", 10)
                    put("unit", "lb")
                },
            ),
            HttpGatewayCommandDescriptor(
                "POST",
                "/v1/commands/eccentric",
                "Set the eccentric amount.",
                buildJsonObject {
                    put("value", 10)
                    put("unit", "lb")
                },
            ),
            HttpGatewayCommandDescriptor(
                "POST",
                "/v1/commands/inverse-chains",
                "Enable or disable inverse chains.",
                buildJsonObject { put("enabled", true) },
            ),
            HttpGatewayCommandDescriptor(
                "POST",
                "/v1/commands/resistance-band/force",
                "Set Resistance Band force.",
                buildJsonObject {
                    put("value", 30)
                    put("unit", "lb")
                },
            ),
            HttpGatewayCommandDescriptor(
                "POST",
                "/v1/commands/resistance-band/mode",
                "Set Resistance Band standard or inverse mode.",
                buildJsonObject { put("inverse", true) },
            ),
            HttpGatewayCommandDescriptor(
                "POST",
                "/v1/commands/resistance-band/curve",
                "Set Resistance Band Power Law or Logarithm.",
                buildJsonObject { put("logarithm", true) },
            ),
            HttpGatewayCommandDescriptor(
                "POST",
                "/v1/commands/resistance-band/progressive-length",
                "Set Resistance Band progressive length mode.",
                buildJsonObject { put("rom", false) },
            ),
            HttpGatewayCommandDescriptor(
                "POST",
                "/v1/commands/resistance-band/length",
                "Set Resistance Band band length in inches.",
                buildJsonObject { put("inches", 42) },
            ),
            HttpGatewayCommandDescriptor(
                "POST",
                "/v1/commands/damper-level",
                "Set the damper level from 1 to 10.",
                buildJsonObject { put("level", 7) },
            ),
            HttpGatewayCommandDescriptor(
                "POST",
                "/v1/commands/isokinetic/menu",
                "Set Isokinetic submenu.",
                buildJsonObject { put("mode", "isokinetic") },
            ),
            HttpGatewayCommandDescriptor(
                "POST",
                "/v1/commands/isokinetic/target-speed",
                "Set the Isokinetic target speed in m/s.",
                buildJsonObject { put("mps", 0.5) },
            ),
            HttpGatewayCommandDescriptor(
                "POST",
                "/v1/commands/isokinetic/speed-limit",
                "Set the Isokinetic speed limit in m/s.",
                buildJsonObject { put("mps", 1.0) },
            ),
            HttpGatewayCommandDescriptor(
                "POST",
                "/v1/commands/isokinetic/constant-resistance",
                "Set Constant Resistance in Isokinetic settings.",
                buildJsonObject {
                    put("value", 15)
                    put("unit", "lb")
                },
            ),
            HttpGatewayCommandDescriptor(
                "POST",
                "/v1/commands/isokinetic/max-eccentric-load",
                "Set Isokinetic max eccentric load.",
                buildJsonObject {
                    put("value", 50)
                    put("unit", "lb")
                },
            ),
        )
        return HttpResponse(
            statusCode = 200,
            body = buildJsonObject {
                put("ok", true)
                put("service", "voltra-http-gateway")
                put("version", appVersion())
                put("urls", buildJsonArray {
                    buildGatewayUrls(latestPreferences.port).forEach { add(JsonPrimitive(it)) }
                })
                put("commands", buildJsonArray {
                    commands.forEach { add(json.encodeToJsonElement(HttpGatewayCommandDescriptor.serializer(), it)) }
                })
            },
        )
    }

    private fun stateResponse(): HttpResponse {
        val session = sessionFlow.value
        val reading = session.reading
        val currentUnit = latestLocalPreferences.unit
        return HttpResponse(
            statusCode = 200,
            body = buildJsonObject {
                put("ok", true)
                put("connection_state", session.connectionState.name)
                put("protocol_status", session.protocolStatus.name)
                put("status_message", session.statusMessage)
                put("control_commands_enabled", session.controlCommandsEnabled)
                put("current_unit", currentUnit.name.lowercase(Locale.US))
                put("device", buildJsonObject {
                    put("id", session.currentDevice?.id.orEmpty())
                    put("name", session.currentDevice?.name.orEmpty())
                    put("address", session.currentDevice?.address.orEmpty())
                })
                put("target_load", buildJsonObject {
                    put("value", session.targetLoad.value)
                    put("unit", session.targetLoad.unit.name.lowercase(Locale.US))
                })
                put("reading", buildJsonObject {
                    putNullable("battery_percent", reading.batteryPercent)
                    putNullable("weight_lb", reading.weightLb)
                    putNullable("force_lb", reading.forceLb)
                    putNullable("resistance_band_force_lb", reading.resistanceBandMaxForceLb)
                    putNullable("damper_level", reading.damperLevelIndex)
                    putNullable("cable_length_cm", reading.cableLengthCm)
                    putNullable("cable_offset_cm", reading.cableOffsetCm)
                    putNullable("assist_enabled", reading.assistModeEnabled)
                    putNullable("chains_lb", reading.chainsWeightLb)
                    putNullable("eccentric_lb", reading.eccentricWeightLb)
                    putNullable("inverse_chains", reading.inverseChains)
                    putNullable("resistance_experience_intense", reading.resistanceExperienceIntense)
                    putNullable("resistance_band_inverse", reading.resistanceBandInverse)
                    putNullable("resistance_band_curve_logarithm", reading.resistanceBandCurveLogarithm)
                    putNullable("resistance_band_by_rom", reading.resistanceBandByRangeOfMotion)
                    putNullable("resistance_band_length_cm", reading.resistanceBandLengthCm)
                    putNullable("isokinetic_mode", reading.isokineticMode)
                    putNullable("isokinetic_target_speed_mps", reading.isokineticTargetSpeedMmS?.div(1000.0))
                    putNullable("isokinetic_speed_limit_mps", reading.isokineticSpeedLimitMmS?.div(1000.0))
                    putNullable("isokinetic_constant_resistance_lb", reading.isokineticConstantResistanceLb)
                    putNullable("isokinetic_max_eccentric_load_lb", reading.isokineticMaxEccentricLoadLb)
                    putNullable("isometric_current_force_n", reading.isometricCurrentForceN)
                    putNullable("isometric_peak_force_n", reading.isometricPeakForceN)
                    putNullable("isometric_peak_relative_force_percent", reading.isometricPeakRelativeForcePercent)
                    putNullable("isometric_elapsed_ms", reading.isometricElapsedMillis)
                    putNullable("set_count", reading.setCount)
                    putNullable("rep_count", reading.repCount)
                    putNullable("rep_phase", reading.repPhase)
                    putNullable("workout_mode", reading.workoutMode)
                })
                put("safety", buildJsonObject {
                    put("can_load", session.safety.canLoad)
                    put("reasons", buildJsonArray {
                        session.safety.reasons.forEach { add(JsonPrimitive(it)) }
                    })
                    putNullable("workout_state", session.safety.workoutState)
                    putNullable("fitness_mode", session.safety.fitnessMode)
                })
            },
        )
    }

    private suspend fun commandResponse(path: String, rawBody: String): HttpResponse {
        val result = try {
            val body = parseJsonBody(rawBody)
            when (path) {
                "/v1/commands/load" -> client.load()
                "/v1/commands/unload" -> client.unload()
                "/v1/commands/exit" -> client.exitWorkout()
                "/v1/commands/mode/weight-training" -> client.setStrengthMode()
                "/v1/commands/mode/resistance-band" -> client.enterResistanceBandMode()
                "/v1/commands/mode/damper" -> client.enterDamperMode()
                "/v1/commands/mode/isokinetic" -> client.enterIsokineticMode()
                "/v1/commands/mode/isometric" -> client.enterIsometricMode()
                "/v1/commands/target-load" -> client.setTargetLoad(weightFromBody(body))
                "/v1/commands/assist" -> client.setAssistMode(booleanField(body, "enabled"))
                "/v1/commands/chains" -> client.setChainsWeight(weightFromBody(body))
                "/v1/commands/eccentric" -> client.setEccentricWeight(weightFromBody(body))
                "/v1/commands/inverse-chains" -> client.setInverseChainsEnabled(booleanField(body, "enabled"))
                "/v1/commands/resistance-band/force" -> client.setResistanceBandMaxForce(weightFromBody(body))
                "/v1/commands/resistance-band/mode" -> client.setResistanceBandInverse(booleanField(body, "inverse"))
                "/v1/commands/resistance-band/curve" -> client.setResistanceBandCurveAlgorithm(booleanField(body, "logarithm"))
                "/v1/commands/resistance-band/progressive-length" -> client.setResistanceBandByRangeOfMotion(booleanField(body, "rom"))
                "/v1/commands/resistance-band/length" -> client.setResistanceBandLengthCm((doubleField(body, "inches") * 2.54).toInt())
                "/v1/commands/damper-level" -> client.setDamperLevel(intField(body, "level").coerceIn(1, 10))
                "/v1/commands/isokinetic/menu" -> client.setIsokineticMenu(isokineticMenuValue(stringField(body, "mode")))
                "/v1/commands/isokinetic/target-speed" -> client.setIsokineticTargetSpeedMmS(mpsToMmS(doubleField(body, "mps")))
                "/v1/commands/isokinetic/speed-limit" -> client.setIsokineticSpeedLimitMmS(mpsToMmS(doubleField(body, "mps")))
                "/v1/commands/isokinetic/constant-resistance" -> client.setIsokineticConstantResistance(weightFromBody(body))
                "/v1/commands/isokinetic/max-eccentric-load" -> client.setIsokineticMaxEccentricLoad(weightFromBody(body))
                else -> null
            } ?: return HttpResponse(
                statusCode = 404,
                body = buildJsonObject {
                    put("ok", false)
                    put("error", "Command endpoint not found.")
                },
            )
        } catch (error: IllegalArgumentException) {
            return HttpResponse(
                statusCode = 400,
                body = buildJsonObject {
                    put("ok", false)
                    put("error", error.message ?: "Invalid request body.")
                },
            )
        }

        return HttpResponse(
            statusCode = 200,
            body = buildJsonObject {
                put("ok", true)
                put("result", json.encodeToJsonElement(VoltraCommandResult.serializer(), result))
            },
        )
    }

    private fun parseJsonBody(rawBody: String): JsonObject {
        if (rawBody.isBlank()) return JsonObject(emptyMap())
        return json.parseToJsonElement(rawBody).jsonObject
    }

    private fun weightFromBody(body: JsonObject): Weight {
        val value = doubleField(body, "value")
        val unitName = body["unit"]?.jsonPrimitive?.content?.trim()?.uppercase(Locale.US)
        val unit = unitName?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() } ?: latestLocalPreferences.unit
        return Weight(value = value, unit = unit)
    }

    private fun booleanField(body: JsonObject, name: String): Boolean {
        return body[name]?.jsonPrimitive?.content?.toBooleanStrictOrNull()
            ?: throw IllegalArgumentException("Expected boolean field '$name'.")
    }

    private fun doubleField(body: JsonObject, name: String): Double {
        return body[name]?.jsonPrimitive?.doubleOrNull
            ?: throw IllegalArgumentException("Expected numeric field '$name'.")
    }

    private fun intField(body: JsonObject, name: String): Int {
        return body[name]?.jsonPrimitive?.intOrNull
            ?: throw IllegalArgumentException("Expected integer field '$name'.")
    }

    private fun stringField(body: JsonObject, name: String): String {
        return body[name]?.jsonPrimitive?.content?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Expected text field '$name'.")
    }

    private fun mpsToMmS(value: Double): Int = (value * 1000.0).toInt().coerceIn(
        VoltraControlFrames.MIN_ISOKINETIC_SPEED_MM_S,
        VoltraControlFrames.MAX_ISOKINETIC_SPEED_MM_S,
    )

    private fun isokineticMenuValue(mode: String): Int = when (mode.lowercase(Locale.US)) {
        "isokinetic" -> VoltraControlFrames.ISOKINETIC_MENU_ISOKINETIC
        "constant_resistance", "constant-resistance", "constant resistance" -> VoltraControlFrames.ISOKINETIC_MENU_CONSTANT_RESISTANCE
        else -> throw IllegalArgumentException("Unknown isokinetic menu '$mode'.")
    }

    private fun appVersion(): String {
        return runCatching {
            val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            packageInfo.versionName ?: "unknown"
        }.getOrDefault("unknown")
    }

    private fun buildGatewayUrls(port: Int): List<String> {
        val urls = linkedSetOf("http://127.0.0.1:$port")
        urls += "http://localhost:$port"
        urls += discoverLocalIpv4Addresses().map { "http://$it:$port" }
        return urls.toList()
    }

    private fun discoverLocalIpv4Addresses(): List<String> {
        return runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .asSequence()
                .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                .flatMap { iface ->
                    iface.inetAddresses.toList().asSequence()
                        .filterIsInstance<Inet4Address>()
                        .map(InetAddress::getHostAddress)
                }
                .filterNotNull()
                .sorted()
                .toList()
        }.getOrDefault(emptyList())
    }

    private fun readRequest(input: BufferedInputStream): HttpRequest {
        val requestLine = input.readHttpLine()?.takeIf { it.isNotBlank() }
            ?: throw EOFException("No HTTP request line.")
        val requestParts = requestLine.split(' ')
        if (requestParts.size < 2) throw IllegalArgumentException("Malformed HTTP request line.")
        val headers = linkedMapOf<String, String>()
        while (true) {
            val line = input.readHttpLine() ?: break
            if (line.isBlank()) break
            val separator = line.indexOf(':')
            if (separator <= 0) continue
            headers[line.substring(0, separator).trim().lowercase(Locale.US)] =
                line.substring(separator + 1).trim()
        }
        val contentLength = headers["content-length"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val body = if (contentLength > 0) {
            val bytes = ByteArray(contentLength)
            var totalRead = 0
            while (totalRead < contentLength) {
                val read = input.read(bytes, totalRead, contentLength - totalRead)
                if (read == -1) break
                totalRead += read
            }
            bytes.copyOf(totalRead).toString(UTF_8)
        } else {
            ""
        }
        return HttpRequest(
            method = requestParts[0].uppercase(Locale.US),
            path = requestParts[1],
            headers = headers,
            body = body,
        )
    }

    private fun writeResponse(output: BufferedOutputStream, response: HttpResponse) {
        val bodyText = json.encodeToString(JsonObject.serializer(), response.body)
        val bodyBytes = bodyText.toByteArray(UTF_8)
        val statusText = when (response.statusCode) {
            200 -> "OK"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            500 -> "Internal Server Error"
            else -> "OK"
        }
        val headers = buildString {
            append("HTTP/1.1 ${response.statusCode} $statusText\r\n")
            append("Content-Type: application/json; charset=utf-8\r\n")
            append("Content-Length: ${bodyBytes.size}\r\n")
            append("Connection: close\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("\r\n")
        }
        output.write(headers.toByteArray(UTF_8))
        output.write(bodyBytes)
        output.flush()
    }
}

private data class HttpRequest(
    val method: String,
    val path: String,
    val headers: Map<String, String>,
    val body: String,
)

private data class HttpResponse(
    val statusCode: Int,
    val body: JsonObject,
)

private fun JsonObjectBuilder.putNullable(key: String, value: Any?) {
    when (value) {
        null -> put(key, JsonPrimitive(""))
        is Boolean -> put(key, value)
        is Int -> put(key, value)
        is Long -> put(key, value)
        is Double -> put(key, value)
        is Float -> put(key, value.toDouble())
        is String -> put(key, value)
        else -> put(key, value.toString())
    }
}

private fun BufferedInputStream.readHttpLine(): String? {
    val bytes = mutableListOf<Byte>()
    while (true) {
        val value = read()
        if (value == -1) {
            if (bytes.isEmpty()) return null
            break
        }
        if (value == '\n'.code) {
            break
        }
        if (value != '\r'.code) {
            bytes += value.toByte()
        }
    }
    return bytes.toByteArray().toString(UTF_8)
}
