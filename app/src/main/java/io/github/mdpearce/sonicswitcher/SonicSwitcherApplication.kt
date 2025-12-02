package io.github.mdpearce.sonicswitcher

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SonicSwitcherApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Enable Crashlytics only for release builds
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }
}
