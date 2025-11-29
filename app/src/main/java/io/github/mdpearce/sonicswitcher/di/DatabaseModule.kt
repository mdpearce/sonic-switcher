package io.github.mdpearce.sonicswitcher.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.mdpearce.sonicswitcher.data.ConvertedFileDao
import io.github.mdpearce.sonicswitcher.data.SonicSwitcherDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): SonicSwitcherDatabase =
        Room
            .databaseBuilder(
                context,
                SonicSwitcherDatabase::class.java,
                "sonic_switcher_database",
            )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideConvertedFileDao(database: SonicSwitcherDatabase): ConvertedFileDao = database.convertedFileDao()
}
