plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.technogizguy.voltra.controller.ble"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:protocol"))
    implementation(libs.core.ktx)
    implementation(libs.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Kept as the planned Nordic BLE dependency boundary; native Android GATT is used
    // for the initial diagnostic layer until the VOLTRA command protocol is proven.
    implementation(libs.nordic.ble.client.android)
}
