package com.neaniesoft.sonicswitcher.di

import com.neaniesoft.sonicswitcher.screens.mainscreen.MainScreen
import me.tatarka.inject.annotations.Component

@Component
abstract class ApplicationComponent {
    abstract val mainScreen: MainScreen
}
