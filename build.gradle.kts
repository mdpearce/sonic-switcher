
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.dagger.hilt) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.ktlint.gradle)
    alias(libs.plugins.kover)
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint") // Version should be inherited from parent
}

// Kover configuration for aggregated coverage reporting
dependencies {
    kover(project(":app"))
    kover(project(":converter"))
}

kover {
    reports {
        filters {
            excludes {
                // Exclude generated code
                classes(
                    "*_Hilt*",
                    "*Hilt_*",
                    "*_Factory",
                    "*_MembersInjector",
                    "*.databinding.*",
                    "*.BuildConfig",
                    "*ComposableSingletons*",
                    // Exclude Android framework
                    "*.R",
                    "*.R$*",
                    // Exclude Firebase/third-party
                    "*.FirebaseModule*",
                )

                packages(
                    "dagger.hilt.internal.aggregatedroot.codegen",
                    "hilt_aggregated_deps",
                )
            }
        }
    }
}
