import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.play.publisher)
    alias(libs.plugins.kover)
}

// Load keystore properties from local.properties
val keystorePropertiesFile = rootProject.file("local.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "io.github.mdpearce.sonicswitcher"
    compileSdk =
        libs.versions.compile.sdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "io.github.mdpearce.sonicswitcher"
        minSdk =
            libs.versions.min.sdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.target.sdk
                .get()
                .toInt()
        versionCode = 14
        versionName = "0.0.1-SNAPSHOT"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = keystoreProperties.getProperty("RELEASE_KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
            }
            storePassword = keystoreProperties.getProperty("RELEASE_KEYSTORE_PASSWORD")
            keyAlias = keystoreProperties.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = keystoreProperties.getProperty("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            // Disable Crashlytics for debug builds
            manifestPlaceholders["crashlyticsEnabled"] = false
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
        release {
            // Enable Crashlytics for release builds
            manifestPlaceholders["crashlyticsEnabled"] = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_17)
        targetCompatibility(JavaVersion.VERSION_17)
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Exclude duplicate license files from JUnit Jupiter
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
        }
    }

    // Configure testShared source set for shared test utilities
    sourceSets {
        getByName("test") {
            java.srcDirs("src/testShared/kotlin")
        }
        getByName("androidTest") {
            java.srcDirs("src/testShared/kotlin")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

play {
    // Service account credentials file (JSON) - keep this file private!
    // Download from Google Play Console -> Setup -> API access
    serviceAccountCredentials.set(file("play-store-credentials.json"))

    // Default track for publishing (can be overridden with --track parameter)
    track.set("internal")

    // Release status: completed, draft, halted, inProgress
    releaseStatus.set(com.github.triplet.gradle.androidpublisher.ReleaseStatus.COMPLETED)
}

dependencies {
    implementation(project(":converter"))

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.material)
    implementation(libs.dagger.hilt.android)
    implementation(libs.kotlin.result)
    implementation(libs.kotlin.reflect)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.dagger.hilt.compiler)

    // Instrumentation Testing
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.coroutines.test)
    kspAndroidTest(libs.dagger.hilt.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
