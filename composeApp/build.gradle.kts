import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Qwen API key for local dev: read from local.properties (qwen.apiKey, gitignored)
// or the QWEN_API_KEY env var. Empty when unset — users enter it via the in-app dialog.
val qwenApiKey: String = run {
    val props = Properties()
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { props.load(it) }
    props.getProperty("qwen.apiKey") ?: System.getenv("QWEN_API_KEY") ?: ""
}

// iOS has no BuildConfig; generate the key into an iosMain source instead.
// Web is intentionally excluded — a key in the JS bundle is readable by anyone.
val generateIosSecrets by tasks.registering {
    val outDir = layout.buildDirectory.dir("generated/iosSecrets/kotlin")
    val key = qwenApiKey
    inputs.property("qwenApiKey", key)
    outputs.dir(outDir)
    doLast {
        val pkgDir = outDir.get().asFile.resolve("com/mettyoung/deconstructchinese/config")
        pkgDir.mkdirs()
        pkgDir.resolve("SecretsGenerated.kt").writeText(
            """
            package com.mettyoung.deconstructchinese.config

            internal const val iosDefaultApiKey: String = "$key"
            """.trimIndent() + "\n"
        )
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    js {
        browser()
        binaries.executable()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    
    // Default hierarchy template wires iosMain -> ios{Arm64,SimulatorArm64}Main
    // and a webMain grouping js + wasmJs, matching the src/iosMain and src/webMain dirs.
    applyDefaultHierarchyTemplate()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.mlkit.text.recognition)
            implementation(libs.mlkit.text.recognition.chinese)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.serialization)
        }
        
        iosMain {
            kotlin.srcDir(generateIosSecrets)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.mettyoung.deconstructchinese"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.mettyoung.deconstructchinese"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "QWEN_API_KEY", "\"$qwenApiKey\"")
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}
