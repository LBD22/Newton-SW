plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ru.newton.fieldapp.gnss.data"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:logging"))
    implementation(project(":core:bluetooth"))
    // The foreground service starts the CommandSession reader loop alongside the
    // NMEA pipeline so SET-080 Apply works as soon as both SPPs are up.
    implementation(project(":gnss:command"))

    // NotificationCompat for the foreground service.
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
}

tasks.withType<Test> { useJUnitPlatform() }
