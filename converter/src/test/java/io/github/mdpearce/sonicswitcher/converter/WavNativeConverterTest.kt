package io.github.mdpearce.sonicswitcher.converter

import android.content.Context
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
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
            // Setup input/output files
            val inputAssetFile = "test.mp3"
            val inputFile = File(context.cacheDir, "test.mp3")

            javaClass.classLoader?.getResourceAsStream("test.mp3")?.use { input ->
                inputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val outputFile = File(context.cacheDir, "output.wav")

            val inputUri = Uri.fromFile(inputFile)
            val outputUri = Uri.fromFile(outputFile)

            val updates = mutableListOf<Float>()

            try {
                classUnderTest.convertAudioFile(inputUri, outputUri) { update ->
                    if (update is io.github.mdpearce.sonicswitcher.converter.results.Processing) {
                        updates.add(update.complete)
                    }
                }
            } catch (e: io.github.mdpearce.sonicswitcher.converter.results.ConversionException) {
                assertThat(e.message).contains("No audio track found")
            }
        }
}
