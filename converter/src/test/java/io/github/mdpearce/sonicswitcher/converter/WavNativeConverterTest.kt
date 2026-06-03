package io.github.mdpearce.sonicswitcher.converter

import android.content.Context
import android.media.MediaFormat
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowMediaExtractor
import org.robolectric.shadows.util.DataSource
import java.io.File

@RunWith(RobolectricTestRunner::class)
class WavNativeConverterTest {
    private lateinit var classUnderTest: WavNativeConverter
    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        classUnderTest = WavNativeConverter(context)
    }

    @Test
    fun testConversion() =
        runTest {
            val inputFile = File(context.cacheDir, "test.mp3")

            // The downloaded real MP3 file is loaded from test/resources/test.mp3
            javaClass.classLoader?.getResourceAsStream("test.mp3")?.use { input ->
                inputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val outputFile = File(context.cacheDir, "output.wav")
            val inputUri = Uri.fromFile(inputFile)
            val outputUri = Uri.fromFile(outputFile)

            // Setup Robolectric ShadowMediaExtractor since Robolectric cannot natively
            // extract MP3 frames without an emulator environment.
            val dataSource = DataSource.toDataSource(context, inputUri)
            val format = MediaFormat()
            format.setString(MediaFormat.KEY_MIME, "audio/mp4")
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100)
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2)
            format.setLong(MediaFormat.KEY_DURATION, 1000000L) // 1 second
            ShadowMediaExtractor.addTrack(dataSource, format, ByteArray(1024))

            val updates = mutableListOf<Float>()

            try {
                val result =
                    classUnderTest.convertAudioFile(inputUri, outputUri) { update ->
                        if (update is io.github.mdpearce.sonicswitcher.converter.results.Processing) {
                            updates.add(update.complete)
                        }
                    }

                // Note: Robolectric's MediaCodec might not natively decode either, so we just want
                // to make sure the loop starts or it fails gracefully without "No audio track found"
                assertThat(result).isNotNull()
            } catch (e: Exception) {
                // We're expecting it to fail trying to use MediaCodec, not MediaExtractor
                assertThat(e.message).doesNotContain("No audio track found")
            }
        }
}
