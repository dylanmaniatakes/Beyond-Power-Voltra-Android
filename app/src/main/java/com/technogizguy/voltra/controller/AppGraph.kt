package com.technogizguy.voltra.controller

import android.content.Context
import com.technogizguy.voltra.controller.http.HttpGatewayServer
import com.technogizguy.voltra.controller.ble.AndroidVoltraClient
import com.technogizguy.voltra.controller.mqtt.MqttSensorPublisher

object AppGraph {
    lateinit var client: AndroidVoltraClient
        private set

    lateinit var preferencesRepository: PreferencesRepository
        private set

    lateinit var mqttSensorPublisher: MqttSensorPublisher
        private set

    lateinit var httpGatewayServer: HttpGatewayServer
        private set

    fun init(context: Context) {
        if (::client.isInitialized) return
        client = AndroidVoltraClient(context)
        preferencesRepository = PreferencesRepository(context)
        mqttSensorPublisher = MqttSensorPublisher(
            context = context,
            preferencesFlow = preferencesRepository.preferences,
            sessionFlow = client.state,
        )
        httpGatewayServer = HttpGatewayServer(
            context = context,
            preferencesFlow = preferencesRepository.preferences,
            sessionFlow = client.state,
            client = client,
        )
    }
}
