plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.6" apply false
}
