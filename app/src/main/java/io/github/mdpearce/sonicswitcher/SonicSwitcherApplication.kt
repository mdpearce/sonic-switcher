package io.github.mdpearce.sonicswitcher

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SonicSwitcherApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Enable Crashlytics collection
        // In debug builds, you might want to disable it: BuildConfig.DEBUG
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
    }
}
