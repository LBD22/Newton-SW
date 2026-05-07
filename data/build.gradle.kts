plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ru.newton.fieldapp.data"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()
    defaultConfig {
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        // Required for connectedAndroidTest — Room MigrationTestHelper runs as
        // an instrumented test on a device/emulator.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

ksp {
    // Commit schemas in VCS for migration testing
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Room MigrationTestHelper reads schemas from the androidTest assets folder.
// AGP 9 broke the classic `sourceSets["androidTest"].assets.srcDir(...)` syntax
// (DefaultAndroidLibrarySourceSet_Decorated cast error), so we wire schemas into
// the variant via the AndroidComponents API instead.
androidComponents {
    onVariants { variant ->
        variant.androidTest?.sources?.assets?.addStaticSourceDirectory(
            file("$projectDir/schemas").absolutePath,
        )
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:logging"))
    // SET-021 — StaticNmeaRecorder taps the DataSPP byte stream.
    implementation(project(":core:bluetooth"))
    implementation(project(":domain"))
    // Apply orchestration (PatchToCommands, ApplyReceiverConfigUseCase) lives here
    // because it needs both `:domain` (ReceiverConfigPatch) and `:gnss:command`
    // (NewtonCommandBuilder, CommandSession). The architecture rule keeps
    // `:gnss:command` from depending on `:domain`, so the bridge sits in `:data`.
    implementation(project(":gnss:command"))
    // NTRIP profile persistence (Room) reuses NtripProfile from `:gnss:ntrip`.
    implementation(project(":gnss:ntrip"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.coroutines.test)

    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
}

tasks.withType<Test> { useJUnitPlatform() }
