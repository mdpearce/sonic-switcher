package com.neaniesoft.sonicswitcher.di

import android.media.MediaExtractor
import com.neaniesoft.sonicswitcher.converter.PcmDecoder
import com.neaniesoft.sonicswitcher.converter.PcmDecoderImpl
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@Component
abstract class ConverterComponent {
    @Provides
    protected fun provideMediaExtractorFactory(): () -> MediaExtractor = { MediaExtractor() }

    @Provides
    protected fun providePcmDecoder(mediaExtractorFactory: () -> MediaExtractor): PcmDecoder =
        PcmDecoderImpl(mediaExtractorFactory)
}
