package com.technogizguy.voltra.controller

data class RuntimePermissionState(
    val requiredPermissions: List<String> = emptyList(),
    val optionalPermissions: List<String> = emptyList(),
    val missingRequiredPermissions: List<String> = emptyList(),
    val missingOptionalPermissions: List<String> = emptyList(),
) {
    val requiredGranted: Boolean
        get() = missingRequiredPermissions.isEmpty()

    val allGranted: Boolean
        get() = missingRequiredPermissions.isEmpty() && missingOptionalPermissions.isEmpty()

    val missingPermissions: Array<String>
        get() = (missingRequiredPermissions + missingOptionalPermissions).toTypedArray()
}
