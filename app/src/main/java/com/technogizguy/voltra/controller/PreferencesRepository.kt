package com.technogizguy.voltra.controller

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.technogizguy.voltra.controller.model.WeightUnit
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

class PreferencesRepository(
    private val context: Context,
) {
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
        )
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
    }
}

val WEIGHT_INCREMENT_OPTIONS = listOf(1, 5, 10, 15, 20)
const val DEFAULT_WEIGHT_INCREMENT = 5
const val DEFAULT_MQTT_PORT = 1883
const val DEFAULT_MQTT_TOPIC_PREFIX = "voltra_control"
const val DEFAULT_HOME_ASSISTANT_DISCOVERY_PREFIX = "homeassistant"
