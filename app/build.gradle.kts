import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
}

android {
    namespace = "com.scryme.notes"
    compileSdk = 34

    signingConfigs {
        val keystoreFile = file("signing-key.jks")
        val base64File = file("signing-key.base64")
        if (!keystoreFile.exists() && base64File.exists()) {
            try {
                val base64Content = base64File.readText().trim()
                val decodedBytes = Base64.getDecoder().decode(base64Content)
                keystoreFile.writeBytes(decodedBytes)
            } catch (e: Exception) {
                // ignore
            }
        }

        if (keystoreFile.exists()) {
            create("release") {
                storeFile = keystoreFile
                val decodedPass = String(Base64.getDecoder().decode("c2NyeW1lbm90ZXM="))
                storePassword = System.getenv("SCRYME_RELEASE_STORE_PASSWORD") ?: decodedPass
                keyAlias = System.getenv("SCRYME_RELEASE_KEY_ALIAS") ?: "scryme"
                keyPassword = System.getenv("SCRYME_RELEASE_KEY_PASSWORD") ?: decodedPass
            }
        }
    }

    defaultConfig {
        applicationId = "com.scryme.notes"
        minSdk = 24
        targetSdk = 34

        val apkVersionCode = project.findProperty("apkVersionCode")?.toString()?.toIntOrNull() ?: 1
        val apkVersionName = project.findProperty("apkVersionName")?.toString() ?: "1.0.0"
        versionCode = apkVersionCode
        versionName = apkVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val releaseConfig = signingConfigs.findByName("release")
            if (releaseConfig != null) {
                signingConfig = releaseConfig
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Gson
    implementation(libs.gson)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

detekt {
    ignoreFailures = true
}
