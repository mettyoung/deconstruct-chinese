import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Qwen (Alibaba DashScope) API key for local dev: read from local.properties
// (qwen.apiKey, gitignored) or the QWEN_API_KEY env var. Bundled into the app.
val qwenApiKey: String = run {
    val props = Properties()
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { props.load(it) }
    props.getProperty("qwen.apiKey") ?: System.getenv("QWEN_API_KEY") ?: ""
}

// Release signing — read from keystore.properties (gitignored). Absent => release
// builds are unsigned (debug/CI still work).
val keystoreProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val hasReleaseSigning = keystoreProps.getProperty("storeFile") != null

// Auto-increment versionCode from the git commit count — monotonic, no manual
// bumps. Release builds need full git history (not a shallow clone); falls back
// to 1 if git is unavailable. Avoid squashing already-released history, or the
// count can regress below a code Play has already accepted.
val gitCommitCount: Int = run {
    try {
        providers.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            workingDir = rootProject.projectDir
        }.standardOutput.asText.get().trim().toIntOrNull() ?: 1
    } catch (e: Exception) {
        1
    }
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

// Desktop (JVM) also has no BuildConfig; generate the key into a desktopMain source.
val generateDesktopSecrets by tasks.registering {
    val outDir = layout.buildDirectory.dir("generated/desktopSecrets/kotlin")
    val key = qwenApiKey
    inputs.property("qwenApiKey", key)
    outputs.dir(outDir)
    doLast {
        val pkgDir = outDir.get().asFile.resolve("com/mettyoung/deconstructchinese/config")
        pkgDir.mkdirs()
        pkgDir.resolve("SecretsGenerated.kt").writeText(
            """
            package com.mettyoung.deconstructchinese.config

            internal const val desktopDefaultApiKey: String = "$key"
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
    
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
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

        val desktopMain by getting {
            kotlin.srcDir(generateDesktopSecrets)
            dependencies {
                implementation(compose.desktop.currentOs)
                // OkHttp (not the Java engine) — same engine as Android, which
                // streams SSE incrementally so the two-phase stage-1 flow completes.
                implementation(libs.ktor.client.okhttp)
                implementation(libs.kotlinx.coroutines.swing)
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
        versionCode = gitCommitCount
        versionName = "1.0.1"
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
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            // Bundle native debug symbols (Compose/Skia .so libs) into the AAB so
            // Play can symbolicate native crashes — clears the upload warning.
            ndk {
                debugSymbolLevel = "FULL"
            }
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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

compose.desktop {
    application {
        mainClass = "com.mettyoung.deconstructchinese.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "DeconstructChinese"
            packageVersion = "1.0.1"
        }
    }
}
