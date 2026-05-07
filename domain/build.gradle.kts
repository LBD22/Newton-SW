plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core:common"))

    implementation(libs.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    // JSR-330 annotations: pure-Kotlin modules that take part in the Hilt graph
    // need `javax.inject` directly, since they don't pull in `hilt-android`.
    api(libs.javax.inject)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
}

tasks.test {
    useJUnitPlatform()
}
