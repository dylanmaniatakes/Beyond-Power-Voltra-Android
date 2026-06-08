package com.technogizguy.voltra.controller.protocol

import com.technogizguy.voltra.controller.model.GattProperty
import com.technogizguy.voltra.controller.model.VoltraCharacteristicRole

object VoltraUuidRegistry {
    const val VOLTRA_SERVICE_UUID = "E4DADA34-0867-8783-9F70-2CA29216C7E4"
    const val VOLTRA_COMMAND_CHARACTERISTIC_UUID = "55CA1E52-7354-25DE-6AFC-B7DF1E8816AC"
    const val VOLTRA_NOTIFY_CHARACTERISTIC_UUID = "CA94658C-0525-5046-E78B-5391B65F47AD"
    const val VOLTRA_TRANSPORT_CHARACTERISTIC_UUID = "A010891D-F50F-44F0-901F-9A2421A9E050"
    const val VOLTRA_JUST_WRITE_CHARACTERISTIC_UUID = "19DE84ED-0A69-482C-A8A6-C75CB5BB4389"

    val knownVoltraUuids = setOf(
        VOLTRA_SERVICE_UUID,
        VOLTRA_COMMAND_CHARACTERISTIC_UUID,
        VOLTRA_NOTIFY_CHARACTERISTIC_UUID,
        VOLTRA_TRANSPORT_CHARACTERISTIC_UUID,
        VOLTRA_JUST_WRITE_CHARACTERISTIC_UUID,
    )

    val knownPm5Uuids = setOf(
        "CE060000-43E5-11E4-916C-0800200C9A66",
        "CE060010-43E5-11E4-916C-0800200C9A66",
        "CE060013-43E5-11E4-916C-0800200C9A66",
        "CE060014-43E5-11E4-916C-0800200C9A66",
        "CE060016-43E5-11E4-916C-0800200C9A66",
        "CE060017-43E5-11E4-916C-0800200C9A66",
        "CE060018-43E5-11E4-916C-0800200C9A66",
        "CE060020-43E5-11E4-916C-0800200C9A66",
        "CE060021-43E5-11E4-916C-0800200C9A66",
        "CE060022-43E5-11E4-916C-0800200C9A66",
        "CE060030-43E5-11E4-916C-0800200C9A66",
        "CE060031-43E5-11E4-916C-0800200C9A66",
        "CE060032-43E5-11E4-916C-0800200C9A66",
        "CE060033-43E5-11E4-916C-0800200C9A66",
        "CE060034-43E5-11E4-916C-0800200C9A66",
        "CE060035-43E5-11E4-916C-0800200C9A66",
        "CE060036-43E5-11E4-916C-0800200C9A66",
        "CE060037-43E5-11E4-916C-0800200C9A66",
        "CE060038-43E5-11E4-916C-0800200C9A66",
        "CE060039-43E5-11E4-916C-0800200C9A66",
        "CE06003A-43E5-11E4-916C-0800200C9A66",
        "CE06003B-43E5-11E4-916C-0800200C9A66",
        "CE06003D-43E5-11E4-916C-0800200C9A66",
        "CE06003E-43E5-11E4-916C-0800200C9A66",
        "CE06003F-43E5-11E4-916C-0800200C9A66",
        "CE060080-43E5-11E4-916C-0800200C9A66",
    )

    fun isLikelyVoltra(name: String?, advertisedServiceUuids: List<String>): Boolean {
        val normalizedName = name.orEmpty().uppercase()
        return "VOLTRA" in normalizedName ||
            advertisedServiceUuids.map(String::uppercase).any { it in knownVoltraUuids }
    }

    fun classifyCharacteristic(
        uuid: String,
        properties: List<GattProperty>,
    ): VoltraCharacteristicRole {
        val normalized = uuid.uppercase()
        if (normalized in knownPm5Uuids) return VoltraCharacteristicRole.PM5_KNOWN
        when (normalized) {
            VOLTRA_COMMAND_CHARACTERISTIC_UUID -> return VoltraCharacteristicRole.VOLTRA_COMMAND
            VOLTRA_NOTIFY_CHARACTERISTIC_UUID -> return VoltraCharacteristicRole.VOLTRA_NOTIFY
            VOLTRA_TRANSPORT_CHARACTERISTIC_UUID -> return VoltraCharacteristicRole.VOLTRA_TRANSPORT
            VOLTRA_JUST_WRITE_CHARACTERISTIC_UUID -> return VoltraCharacteristicRole.VOLTRA_JUST_WRITE
        }
        if (normalized !in knownVoltraUuids) return VoltraCharacteristicRole.UNKNOWN

        val canNotify = GattProperty.NOTIFY in properties || GattProperty.INDICATE in properties
        val canWrite = GattProperty.WRITE in properties || GattProperty.WRITE_NO_RESPONSE in properties
        return when {
            canNotify && canWrite -> VoltraCharacteristicRole.TRANSPORT_CANDIDATE
            canNotify -> VoltraCharacteristicRole.NOTIFY_CANDIDATE
            GattProperty.WRITE_NO_RESPONSE in properties -> VoltraCharacteristicRole.JUST_WRITE_CANDIDATE
            canWrite -> VoltraCharacteristicRole.COMMAND_CANDIDATE
            else -> VoltraCharacteristicRole.UNKNOWN
        }
    }
}
