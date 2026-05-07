plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ru.newton.fieldapp.features.cad"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()
    defaultConfig {
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:logging"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":crs"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    // TODO Sprint 7 (CAD-001): re-enable kabeja with correct Maven coordinates.
    //   The artifact `org.kabeja:kabeja:0.4` does not exist on any public Maven repo.
    //   Real coordinates are likely `de.miethxml:kabeja-*` or a custom build.
    //   Investigate and update libs.versions.toml accordingly.
    // implementation(libs.kabeja)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.coroutines.core)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
}

tasks.withType<Test> { useJUnitPlatform() }
// kabeja for DXF parsing
