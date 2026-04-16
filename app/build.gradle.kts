import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val uploadSigningProperties = Properties().apply {
    val propertiesFile = rootProject.file("keystore.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}

fun signingValue(propertyName: String, envName: String): String? {
    return uploadSigningProperties.getProperty(propertyName)
        ?.takeIf { it.isNotBlank() }
        ?: System.getenv(envName)?.takeIf { it.isNotBlank() }
}

val uploadStoreFilePath = signingValue("storeFile", "VOLTRA_UPLOAD_STORE_FILE")
val uploadStorePassword = signingValue("storePassword", "VOLTRA_UPLOAD_STORE_PASSWORD")
val uploadKeyAlias = signingValue("keyAlias", "VOLTRA_UPLOAD_KEY_ALIAS")
val uploadKeyPassword = signingValue("keyPassword", "VOLTRA_UPLOAD_KEY_PASSWORD")
val hasPlayUploadSigning = listOf(
    uploadStoreFilePath,
    uploadStorePassword,
    uploadKeyAlias,
    uploadKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.technogizguy.voltra.controller"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.technogizguy.voltra.controller"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 104
        versionName = "1.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    signingConfigs {
        create("upload") {
            if (hasPlayUploadSigning) {
                storeFile = file(uploadStoreFilePath!!)
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = if (hasPlayUploadSigning) {
                signingConfigs.getByName("upload")
            } else {
                signingConfigs.getByName("debug")
            }
        }
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
    implementation(project(":device:ble"))

    implementation(libs.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.foundation.layout)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.core.ktx)
    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.material3)
    implementation(libs.navigation.compose)

    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

tasks.register("validatePlayUploadSigning") {
    group = "distribution"
    description = "Checks that upload signing is configured for Play beta builds."
    doLast {
        check(hasPlayUploadSigning) {
            buildString {
                appendLine("Play upload signing is not configured.")
                appendLine("Create keystore.properties from keystore.properties.example")
                appendLine("or set VOLTRA_UPLOAD_STORE_FILE / VOLTRA_UPLOAD_STORE_PASSWORD / VOLTRA_UPLOAD_KEY_ALIAS / VOLTRA_UPLOAD_KEY_PASSWORD.")
            }
        }
    }
}

tasks.register("bundlePlayRelease") {
    group = "distribution"
    description = "Builds a Play-ready signed Android App Bundle for beta upload."
    dependsOn("validatePlayUploadSigning", ":app:bundleRelease")
}
