package com.neaniesoft.sonicswitcher.converter

import android.content.Context
import android.media.MediaExtractor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ConverterModule {
    @Provides
    @Singleton
    fun provideMediaExtractorFactory(): () -> MediaExtractor = { MediaExtractor() }

    @Provides
    fun provideAudioFileConverter(
        @ApplicationContext context: Context
    ): AudioFileConverter = FFMpegKitDecoder(context)
}
