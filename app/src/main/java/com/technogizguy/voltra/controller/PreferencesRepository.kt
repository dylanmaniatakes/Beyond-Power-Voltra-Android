package com.technogizguy.voltra.controller

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.technogizguy.voltra.controller.model.WeightUnit
import com.technogizguy.voltra.controller.protocol.VoltraControlFrames
import java.util.UUID
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.voltraDataStore by preferencesDataStore(name = "voltra_local")

enum class AccentColor(val label: String) {
    WHITE("White"),
    DARK_BLUE("Dark Blue"),
    ORANGE("Orange"),
    RAVENS_PURPLE("Ravens Purple"),
    LIME_GREEN("Lime Green"),
    ELECTRIC_BLUE("Electric Blue"),
    EMBER_RED("Ember Red"),
    GOLD("Gold"),
    RED("Neon Red"),
}

data class LocalPreferences(
    val lastDeviceId: String? = null,
    val lastDeviceName: String? = null,
    val unit: WeightUnit = WeightUnit.LB,
    val accentColor: AccentColor = AccentColor.WHITE,
    val weightIncrement: Int = DEFAULT_WEIGHT_INCREMENT,
    val instantWeightApplyDefault: Boolean = false,
    val developerModeEnabled: Boolean = false,
    val mqtt: MqttPreferences = MqttPreferences(),
    val httpGateway: HttpGatewayPreferences = HttpGatewayPreferences(),
)

data class MqttPreferences(
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int = DEFAULT_MQTT_PORT,
    val username: String = "",
    val password: String = "",
    val topicPrefix: String = DEFAULT_MQTT_TOPIC_PREFIX,
    val discoveryEnabled: Boolean = true,
    val discoveryPrefix: String = DEFAULT_HOME_ASSISTANT_DISCOVERY_PREFIX,
)

data class HttpGatewayPreferences(
    val enabled: Boolean = false,
    val port: Int = DEFAULT_HTTP_GATEWAY_PORT,
    val accessKey: String = "",
)

class PreferencesRepository(
    private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val preferences: Flow<LocalPreferences> = context.voltraDataStore.data.map { prefs ->
        LocalPreferences(
            lastDeviceId = prefs[LAST_DEVICE_ID],
            lastDeviceName = prefs[LAST_DEVICE_NAME],
            unit = prefs[UNIT]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() } ?: WeightUnit.LB,
            accentColor = prefs[ACCENT_COLOR]?.let { runCatching { AccentColor.valueOf(it) }.getOrNull() } ?: AccentColor.WHITE,
            weightIncrement = prefs[WEIGHT_INCREMENT]?.takeIf { it in WEIGHT_INCREMENT_OPTIONS } ?: DEFAULT_WEIGHT_INCREMENT,
            instantWeightApplyDefault = prefs[INSTANT_WEIGHT_APPLY_DEFAULT] ?: false,
            developerModeEnabled = prefs[DEVELOPER_MODE_ENABLED] ?: false,
            mqtt = MqttPreferences(
                enabled = prefs[MQTT_ENABLED] ?: false,
                host = prefs[MQTT_HOST].orEmpty(),
                port = prefs[MQTT_PORT]?.takeIf { it in 1..65535 } ?: DEFAULT_MQTT_PORT,
                username = prefs[MQTT_USERNAME].orEmpty(),
                password = prefs[MQTT_PASSWORD].orEmpty(),
                topicPrefix = prefs[MQTT_TOPIC_PREFIX]?.takeIf { it.isNotBlank() } ?: DEFAULT_MQTT_TOPIC_PREFIX,
                discoveryEnabled = prefs[MQTT_DISCOVERY_ENABLED] ?: true,
                discoveryPrefix = prefs[MQTT_DISCOVERY_PREFIX]?.takeIf { it.isNotBlank() } ?: DEFAULT_HOME_ASSISTANT_DISCOVERY_PREFIX,
            ),
            httpGateway = HttpGatewayPreferences(
                enabled = prefs[HTTP_GATEWAY_ENABLED] ?: false,
                port = prefs[HTTP_GATEWAY_PORT]?.takeIf { it in 1..65535 } ?: DEFAULT_HTTP_GATEWAY_PORT,
                accessKey = prefs[HTTP_GATEWAY_ACCESS_KEY].orEmpty(),
            ),
        )
    }

    val weightPresets: Flow<List<WeightPreset>> = context.voltraDataStore.data.map { prefs ->
        decodeWeightPresets(prefs[WEIGHT_PRESETS_JSON])
    }

    val customCurvePresets: Flow<List<CustomCurvePreset>> = context.voltraDataStore.data.map { prefs ->
        decodeCustomCurvePresets(prefs[CUSTOM_CURVE_PRESETS_JSON])
    }

    val workoutHistory: Flow<List<WorkoutHistoryEntry>> = context.voltraDataStore.data.map { prefs ->
        decodeWorkoutHistory(prefs[WORKOUT_HISTORY_JSON])
    }

    suspend fun rememberDevice(deviceId: String, deviceName: String?) {
        context.voltraDataStore.edit { prefs ->
            prefs[LAST_DEVICE_ID] = deviceId
            if (deviceName != null) {
                prefs[LAST_DEVICE_NAME] = deviceName
            } else {
                prefs.remove(LAST_DEVICE_NAME)
            }
        }
    }

    suspend fun setUnit(unit: WeightUnit) {
        context.voltraDataStore.edit { prefs ->
            prefs[UNIT] = unit.name
        }
    }

    suspend fun setAccentColor(accent: AccentColor) {
        context.voltraDataStore.edit { prefs ->
            prefs[ACCENT_COLOR] = accent.name
        }
    }

    suspend fun setWeightIncrement(increment: Int) {
        context.voltraDataStore.edit { prefs ->
            prefs[WEIGHT_INCREMENT] = increment.takeIf { it in WEIGHT_INCREMENT_OPTIONS } ?: DEFAULT_WEIGHT_INCREMENT
        }
    }

    suspend fun setInstantWeightApplyDefault(enabled: Boolean) {
        context.voltraDataStore.edit { prefs ->
            prefs[INSTANT_WEIGHT_APPLY_DEFAULT] = enabled
        }
    }

    suspend fun setDeveloperModeEnabled(enabled: Boolean) {
        context.voltraDataStore.edit { prefs ->
            prefs[DEVELOPER_MODE_ENABLED] = enabled
        }
    }

    suspend fun setMqttEnabled(enabled: Boolean) {
        context.voltraDataStore.edit { prefs ->
            prefs[MQTT_ENABLED] = enabled
        }
    }

    suspend fun setMqttSettings(settings: MqttPreferences) {
        context.voltraDataStore.edit { prefs ->
            prefs[MQTT_HOST] = settings.host.trim()
            prefs[MQTT_PORT] = settings.port.coerceIn(1, 65535)
            prefs[MQTT_USERNAME] = settings.username
            prefs[MQTT_PASSWORD] = settings.password
            prefs[MQTT_TOPIC_PREFIX] = settings.topicPrefix.trim().trim('/').ifBlank { DEFAULT_MQTT_TOPIC_PREFIX }
            prefs[MQTT_DISCOVERY_ENABLED] = settings.discoveryEnabled
            prefs[MQTT_DISCOVERY_PREFIX] = settings.discoveryPrefix.trim().trim('/').ifBlank { DEFAULT_HOME_ASSISTANT_DISCOVERY_PREFIX }
        }
    }

    suspend fun setHttpGatewayEnabled(enabled: Boolean) {
        context.voltraDataStore.edit { prefs ->
            prefs[HTTP_GATEWAY_ENABLED] = enabled
            if (enabled && prefs[HTTP_GATEWAY_ACCESS_KEY].isNullOrBlank()) {
                prefs[HTTP_GATEWAY_ACCESS_KEY] = generateGatewayAccessKey()
            }
        }
    }

    suspend fun setHttpGatewaySettings(settings: HttpGatewayPreferences) {
        context.voltraDataStore.edit { prefs ->
            prefs[HTTP_GATEWAY_PORT] = settings.port.coerceIn(1, 65535)
            prefs[HTTP_GATEWAY_ACCESS_KEY] = settings.accessKey.ifBlank { generateGatewayAccessKey() }
        }
    }

    suspend fun rotateHttpGatewayAccessKey() {
        context.voltraDataStore.edit { prefs ->
            prefs[HTTP_GATEWAY_ACCESS_KEY] = generateGatewayAccessKey()
        }
    }

    suspend fun upsertWeightPreset(
        name: String,
        scope: WeightPresetScope,
        value: Double,
        unit: WeightUnit,
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        context.voltraDataStore.edit { prefs ->
            val current = decodeWeightPresets(prefs[WEIGHT_PRESETS_JSON])
            val next = buildList {
                add(
                    WeightPreset(
                        id = UUID.randomUUID().toString(),
                        name = trimmedName,
                        scope = scope,
                        value = value,
                        unit = unit,
                        createdAtMillis = System.currentTimeMillis(),
                    ),
                )
                addAll(current.filterNot { it.scope == scope && it.name.equals(trimmedName, ignoreCase = true) })
            }.take(MAX_WEIGHT_PRESETS)
            prefs[WEIGHT_PRESETS_JSON] = json.encodeToString(ListSerializer(WeightPreset.serializer()), next)
        }
    }

    suspend fun deleteWeightPreset(id: String) {
        context.voltraDataStore.edit { prefs ->
            val current = decodeWeightPresets(prefs[WEIGHT_PRESETS_JSON])
            val next = current.filterNot { it.id == id }
            if (next.isEmpty()) {
                prefs.remove(WEIGHT_PRESETS_JSON)
            } else {
                prefs[WEIGHT_PRESETS_JSON] = json.encodeToString(ListSerializer(WeightPreset.serializer()), next)
            }
        }
    }

    suspend fun upsertCustomCurvePreset(
        name: String,
        points: List<Float>,
        resistanceMinLb: Int,
        resistanceLimitLb: Int,
        rangeOfMotionIn: Int,
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        val normalizedPoints = points.normalizedCustomCurvePoints()
        if (normalizedPoints.size != VoltraControlFrames.CUSTOM_CURVE_POINT_COUNT) return
        val (normalizedResistanceMin, normalizedResistanceMax) =
            normalizedCustomCurveResistanceRange(resistanceMinLb, resistanceLimitLb)
        val normalizedRangeOfMotion = rangeOfMotionIn.normalizedCustomCurveRangeOfMotion()
        context.voltraDataStore.edit { prefs ->
            val current = decodeCustomCurvePresets(prefs[CUSTOM_CURVE_PRESETS_JSON])
            val next = buildList {
                add(
                    CustomCurvePreset(
                        id = UUID.randomUUID().toString(),
                        name = trimmedName,
                        points = normalizedPoints,
                        resistanceMinLb = normalizedResistanceMin,
                        resistanceLimitLb = normalizedResistanceMax,
                        rangeOfMotionIn = normalizedRangeOfMotion,
                        createdAtMillis = System.currentTimeMillis(),
                    ),
                )
                addAll(current.filterNot { it.name.equals(trimmedName, ignoreCase = true) })
            }.take(MAX_CUSTOM_CURVE_PRESETS)
            prefs[CUSTOM_CURVE_PRESETS_JSON] = json.encodeToString(ListSerializer(CustomCurvePreset.serializer()), next)
        }
    }

    suspend fun deleteCustomCurvePreset(id: String) {
        context.voltraDataStore.edit { prefs ->
            val current = decodeCustomCurvePresets(prefs[CUSTOM_CURVE_PRESETS_JSON])
            val next = current.filterNot { it.id == id }
            if (next.isEmpty()) {
                prefs.remove(CUSTOM_CURVE_PRESETS_JSON)
            } else {
                prefs[CUSTOM_CURVE_PRESETS_JSON] = json.encodeToString(ListSerializer(CustomCurvePreset.serializer()), next)
            }
        }
    }

    suspend fun appendWorkoutHistory(entry: WorkoutHistoryEntry) {
        context.voltraDataStore.edit { prefs ->
            val current = decodeWorkoutHistory(prefs[WORKOUT_HISTORY_JSON])
            val next = buildList {
                add(entry)
                addAll(current.filterNot { it.id == entry.id })
            }.take(MAX_WORKOUT_HISTORY_ENTRIES)
            prefs[WORKOUT_HISTORY_JSON] = json.encodeToString(ListSerializer(WorkoutHistoryEntry.serializer()), next)
        }
    }

    suspend fun clearWorkoutHistory() {
        context.voltraDataStore.edit { prefs ->
            prefs.remove(WORKOUT_HISTORY_JSON)
        }
    }

    private fun decodeWeightPresets(raw: String?): List<WeightPreset> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(WeightPreset.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun decodeWorkoutHistory(raw: String?): List<WorkoutHistoryEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(WorkoutHistoryEntry.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun decodeCustomCurvePresets(raw: String?): List<CustomCurvePreset> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(CustomCurvePreset.serializer()), raw)
                .mapNotNull { preset ->
                    val normalizedPoints = preset.points.normalizedCustomCurvePoints()
                    if (normalizedPoints.size == VoltraControlFrames.CUSTOM_CURVE_POINT_COUNT) {
                        val (normalizedResistanceMin, normalizedResistanceMax) =
                            normalizedCustomCurveResistanceRange(
                                preset.resistanceMinLb,
                                preset.resistanceLimitLb,
                            )
                        preset.copy(
                            points = normalizedPoints,
                            resistanceMinLb = normalizedResistanceMin,
                            resistanceLimitLb = normalizedResistanceMax,
                            rangeOfMotionIn = preset.rangeOfMotionIn.normalizedCustomCurveRangeOfMotion(),
                        )
                    } else {
                        null
                    }
                }
        }.getOrDefault(emptyList())
    }

    private companion object {
        val LAST_DEVICE_ID = stringPreferencesKey("last_device_id")
        val LAST_DEVICE_NAME = stringPreferencesKey("last_device_name")
        val UNIT = stringPreferencesKey("unit")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val WEIGHT_INCREMENT = intPreferencesKey("weight_increment")
        val INSTANT_WEIGHT_APPLY_DEFAULT = booleanPreferencesKey("instant_weight_apply_default")
        val DEVELOPER_MODE_ENABLED = booleanPreferencesKey("developer_mode_enabled")
        val MQTT_ENABLED = booleanPreferencesKey("mqtt_enabled")
        val MQTT_HOST = stringPreferencesKey("mqtt_host")
        val MQTT_PORT = intPreferencesKey("mqtt_port")
        val MQTT_USERNAME = stringPreferencesKey("mqtt_username")
        val MQTT_PASSWORD = stringPreferencesKey("mqtt_password")
        val MQTT_TOPIC_PREFIX = stringPreferencesKey("mqtt_topic_prefix")
        val MQTT_DISCOVERY_ENABLED = booleanPreferencesKey("mqtt_discovery_enabled")
        val MQTT_DISCOVERY_PREFIX = stringPreferencesKey("mqtt_discovery_prefix")
        val HTTP_GATEWAY_ENABLED = booleanPreferencesKey("http_gateway_enabled")
        val HTTP_GATEWAY_PORT = intPreferencesKey("http_gateway_port")
        val HTTP_GATEWAY_ACCESS_KEY = stringPreferencesKey("http_gateway_access_key")
        val WEIGHT_PRESETS_JSON = stringPreferencesKey("weight_presets_json")
        val CUSTOM_CURVE_PRESETS_JSON = stringPreferencesKey("custom_curve_presets_json")
        val WORKOUT_HISTORY_JSON = stringPreferencesKey("workout_history_json")
    }
}

val WEIGHT_INCREMENT_OPTIONS = listOf(1, 5, 10, 15, 20)
const val DEFAULT_WEIGHT_INCREMENT = 5
const val DEFAULT_MQTT_PORT = 1883
const val DEFAULT_MQTT_TOPIC_PREFIX = "voltra_control"
const val DEFAULT_HOME_ASSISTANT_DISCOVERY_PREFIX = "homeassistant"
const val DEFAULT_HTTP_GATEWAY_PORT = 8788
const val MAX_WEIGHT_PRESETS = 18
const val MAX_CUSTOM_CURVE_PRESETS = 24
const val MAX_WORKOUT_HISTORY_ENTRIES = 120

private fun generateGatewayAccessKey(): String = UUID.randomUUID().toString().replace("-", "")

private fun List<Float>.normalizedCustomCurvePoints(): List<Float> =
    take(VoltraControlFrames.CUSTOM_CURVE_POINT_COUNT).map { it.coerceIn(0f, 1f) }

private fun normalizedCustomCurveResistanceRange(minLb: Int, maxLb: Int): Pair<Int, Int> {
    val max = maxLb.coerceIn(
        VoltraControlFrames.MIN_CUSTOM_CURVE_RESISTANCE_LIMIT_LB + VoltraControlFrames.MIN_CUSTOM_CURVE_RESISTANCE_SPAN_LB,
        VoltraControlFrames.MAX_CUSTOM_CURVE_RESISTANCE_LIMIT_LB,
    )
    val min = minLb.coerceIn(
        VoltraControlFrames.MIN_CUSTOM_CURVE_RESISTANCE_LIMIT_LB,
        max - VoltraControlFrames.MIN_CUSTOM_CURVE_RESISTANCE_SPAN_LB,
    )
    return min to max
}

private fun Int.normalizedCustomCurveRangeOfMotion(): Int =
    coerceIn(
        VoltraControlFrames.MIN_CUSTOM_CURVE_RANGE_OF_MOTION_IN,
        VoltraControlFrames.MAX_CUSTOM_CURVE_RANGE_OF_MOTION_IN,
    )
