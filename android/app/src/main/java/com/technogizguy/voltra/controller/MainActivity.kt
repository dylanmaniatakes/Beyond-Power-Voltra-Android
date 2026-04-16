package com.technogizguy.voltra.controller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.technogizguy.voltra.controller.ui.VoltraControllerApp

class MainActivity : ComponentActivity() {
    private var permissionStateHolder: MutableState<RuntimePermissionState>? = null

    private val viewModel: VoltraViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return VoltraViewModel(application) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val permissionState = remember { mutableStateOf(buildPermissionState()) }
            permissionStateHolder = permissionState
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) {
                refreshPermissionState()
            }

            LaunchedEffect(Unit) {
                refreshPermissionState()
                val missing = permissionState.value.missingPermissions
                if (missing.isNotEmpty()) {
                    permissionLauncher.launch(missing)
                }
            }

            VoltraControllerApp(
                viewModel = viewModel,
                permissionState = permissionState.value,
                onRequestPermissions = {
                    refreshPermissionState()
                    val missing = permissionState.value.missingPermissions
                    if (missing.isNotEmpty()) {
                        permissionLauncher.launch(missing)
                    }
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
    }

    private fun refreshPermissionState() {
        permissionStateHolder?.value = buildPermissionState()
    }

    private fun buildPermissionState(): RuntimePermissionState {
        val required = requiredPermissions()
        val optional = optionalPermissions()
        return RuntimePermissionState(
            requiredPermissions = required,
            optionalPermissions = optional,
            missingRequiredPermissions = required.filterNot(::isGranted),
            missingOptionalPermissions = optional.filterNot(::isGranted),
        )
    }

    private fun requiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_SCAN
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            permissions += Manifest.permission.ACCESS_FINE_LOCATION
        }
        return permissions
    }

    private fun optionalPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyList()
        }
    }

    private fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}
