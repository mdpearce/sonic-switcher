package com.neaniesoft.sonicswitcher.converter

import android.media.MediaExtractor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ConverterModule {
    @Provides
    @Singleton
    fun provideMediaExtractorFactory(): () -> MediaExtractor = { MediaExtractor() }

    @Provides
    fun providePcmDecoder(mediaExtractorFactory: () -> MediaExtractor): PcmDecoder =
        PcmDecoderImpl(mediaExtractorFactory)
}
