package io.github.mdpearce.sonicswitcher.di

import android.content.ContentResolver
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.time.Clock
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class MainScreenModule {
    companion object {
        const val CACHE_DIR = "cache_dir"
    }

    @Provides
    @Singleton
    fun provideContentResolver(
        @ApplicationContext context: Context,
    ): ContentResolver = context.contentResolver

    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Named(CACHE_DIR)
    fun provideCacheDir(
        @ApplicationContext context: Context,
    ): File = context.cacheDir
}
