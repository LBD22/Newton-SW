// Root build.gradle.kts — only declares plugins available to submodules.
// Submodules declare which plugins they actually apply.
// Updated for AGP 9.0 compatibility.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        // Match Kotlin major.minor; ktlint resolves the matching ruleset.
        version.set("1.5.0")
        android.set(true)
        ignoreFailures.set(false)
        filter {
            exclude("**/generated/**")
            exclude("**/build/**")
        }
    }
}
