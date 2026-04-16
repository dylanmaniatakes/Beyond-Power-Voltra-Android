package com.technogizguy.voltra.controller.protocol

import com.technogizguy.voltra.controller.model.GattProperty
import com.technogizguy.voltra.controller.model.VoltraCharacteristicRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VoltraUuidRegistryTest {
    @Test
    fun mapsConfirmedVoltraCharacteristicRoles() {
        assertEquals(
            VoltraCharacteristicRole.VOLTRA_COMMAND,
            VoltraUuidRegistry.classifyCharacteristic(
                VoltraUuidRegistry.VOLTRA_COMMAND_CHARACTERISTIC_UUID,
                listOf(GattProperty.READ, GattProperty.WRITE, GattProperty.NOTIFY, GattProperty.INDICATE),
            ),
        )
        assertEquals(
            VoltraCharacteristicRole.VOLTRA_NOTIFY,
            VoltraUuidRegistry.classifyCharacteristic(
                VoltraUuidRegistry.VOLTRA_NOTIFY_CHARACTERISTIC_UUID.lowercase(),
                listOf(GattProperty.NOTIFY, GattProperty.INDICATE),
            ),
        )
        assertEquals(
            VoltraCharacteristicRole.VOLTRA_TRANSPORT,
            VoltraUuidRegistry.classifyCharacteristic(
                VoltraUuidRegistry.VOLTRA_TRANSPORT_CHARACTERISTIC_UUID,
                listOf(GattProperty.READ, GattProperty.WRITE, GattProperty.NOTIFY, GattProperty.INDICATE),
            ),
        )
        assertEquals(
            VoltraCharacteristicRole.VOLTRA_JUST_WRITE,
            VoltraUuidRegistry.classifyCharacteristic(
                VoltraUuidRegistry.VOLTRA_JUST_WRITE_CHARACTERISTIC_UUID,
                listOf(GattProperty.WRITE_NO_RESPONSE),
            ),
        )
    }

    @Test
    fun marksAdvertisedVoltraServiceAsLikelyVoltra() {
        assertTrue(
            VoltraUuidRegistry.isLikelyVoltra(
                name = null,
                advertisedServiceUuids = listOf(VoltraUuidRegistry.VOLTRA_SERVICE_UUID.lowercase()),
            ),
        )
    }
}
