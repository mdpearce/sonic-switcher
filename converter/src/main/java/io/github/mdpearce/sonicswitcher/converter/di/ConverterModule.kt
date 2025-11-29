package io.github.mdpearce.sonicswitcher.converter.di

import android.content.Context
import android.media.MediaExtractor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.mdpearce.sonicswitcher.converter.AudioFileConverter
import io.github.mdpearce.sonicswitcher.converter.FFMpegKitConverter
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ConverterModule {
    @Provides
    @Singleton
    fun provideMediaExtractorFactory(): () -> MediaExtractor = { MediaExtractor() }

    @Provides
    fun provideAudioFileConverter(
        @ApplicationContext context: Context,
    ): AudioFileConverter = FFMpegKitConverter(context)
}
