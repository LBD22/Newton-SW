import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Release signing — keystore.properties is git-ignored. If absent (or missing any
// key), fall back to debug signing so `:app:assembleRelease` still produces an
// installable APK for QA. Production builds in CI must populate keystore.properties.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps: Properties? = if (keystorePropsFile.exists()) {
    Properties().apply { keystorePropsFile.inputStream().use { load(it) } }
} else {
    null
}

fun Properties.hasAll(vararg keys: String) = keys.all { getProperty(it).orEmpty().isNotBlank() }

android {
    namespace = "ru.newton.fieldapp"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "ru.newton.fieldapp"
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProps != null && keystoreProps.hasAll("storeFile", "storePassword", "keyAlias", "keyPassword")) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // All feature modules
    implementation(project(":features:project"))
    implementation(project(":features:settings"))
    implementation(project(":features:survey"))
    implementation(project(":features:cad"))
    implementation(project(":features:export"))

    // Core
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:logging"))
    implementation(project(":core:bluetooth"))
    implementation(project(":gnss:data"))

    // Domain/data for Hilt wiring in :app
    implementation(project(":domain"))
    implementation(project(":data"))

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // OSMDroid Configuration must be set once at process start.
    implementation(libs.osmdroid)

    // Timber
    implementation(libs.timber)
}
