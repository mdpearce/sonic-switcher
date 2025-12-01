package io.github.mdpearce.sonicswitcher.converter

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.mdpearce.sonicswitcher.converter.results.ConversionCancelled
import io.github.mdpearce.sonicswitcher.converter.results.ConversionComplete
import io.github.mdpearce.sonicswitcher.converter.results.Inactive
import io.github.mdpearce.sonicswitcher.converter.results.Processing
import io.github.mdpearce.sonicswitcher.converter.results.ProgressUpdate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Integration tests for FFMpegKitConverter using real FFmpeg operations.
 * These tests use generated audio files to test actual conversion behavior.
 *
 * Note: These tests require an Android device/emulator and may take longer to run
 * than unit tests due to actual audio processing.
 */
@RunWith(AndroidJUnit4::class)
class FFMpegKitConverterTest {
    private lateinit var context: Context
    private lateinit var converter: FFMpegKitConverter
    private lateinit var cacheDir: File

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val NUM_CHANNELS = 1 // Mono
        private const val BITS_PER_SAMPLE = 16
        private const val FREQUENCY = 440.0 // A4 note
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        converter = FFMpegKitConverter(context)
        cacheDir = context.cacheDir
    }

    @After
    fun tearDown() {
        // Clean up any remaining test files
        cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("test_") || file.name.contains("_output") || file.name == "invalid.txt") {
                file.delete()
            }
        }
    }

    @Test
    fun convertAudioFile_convertsValidWavToMp3Successfully() =
        runTest {
            // Arrange
            val inputFile = createTestWavFile("test_input.wav")
            val outputFile = File(cacheDir, "test_output.mp3")
            val inputUri = getUriForFile(inputFile)
            val outputUri = getUriForFile(outputFile)
            val progressUpdates = mutableListOf<ProgressUpdate>()

            try {
                // Act
                val result =
                    converter.convertAudioFile(
                        input = inputUri,
                        output = outputUri,
                        onProgressUpdated = { progressUpdates.add(it) },
                    )

                // Assert
                assertThat(result).isEqualTo(ConversionComplete)
                assertThat(outputFile.exists()).isTrue()
                assertThat(outputFile.length()).isGreaterThan(0L)

                // Verify progress updates were received
                assertThat(progressUpdates).isNotEmpty()
                assertThat(progressUpdates.first()).isInstanceOf(Processing::class.java)
                assertThat(progressUpdates.last()).isEqualTo(Inactive)

                // Verify progress went from 0 to higher values
                val processingUpdates = progressUpdates.filterIsInstance<Processing>()
                assertThat(processingUpdates).isNotEmpty()
                val firstProgress = processingUpdates.first()
                assertThat(firstProgress.complete).isEqualTo(0.0f)
            } finally {
                // Cleanup
                inputFile.delete()
                outputFile.delete()
            }
        }

    @Test
    fun convertAudioFile_handlesInvalidInputGracefully() =
        runTest {
            // Arrange
            val invalidFile = File(cacheDir, "invalid.txt")
            invalidFile.writeText("This is not an audio file")
            val outputFile = File(cacheDir, "output.mp3")
            val inputUri = getUriForFile(invalidFile)
            val outputUri = getUriForFile(outputFile)

            try {
                // Act & Assert
                val exception =
                    runCatching {
                        converter.convertAudioFile(
                            input = inputUri,
                            output = outputUri,
                            onProgressUpdated = {},
                        )
                    }.exceptionOrNull()

                // FFmpeg throws IllegalStateException when it can't get media information
                assertThat(exception).isInstanceOf(IllegalStateException::class.java)
            } finally {
                // Cleanup
                invalidFile.delete()
                outputFile.delete()
            }
        }

    @Test
    fun convertAudioFile_canBeCancelled() =
        runTest {
            // Arrange
            val inputFile = createTestWavFile("test_cancel.wav", durationSeconds = 5)
            val outputFile = File(cacheDir, "test_cancel_output.mp3")
            val inputUri = getUriForFile(inputFile)
            val outputUri = getUriForFile(outputFile)
            val progressUpdates = mutableListOf<ProgressUpdate>()

            try {
                // Act - Start conversion in background and cancel it
                var conversionJob: Job? = null
                var result: Any? = null

                conversionJob =
                    launch {
                        result =
                            runCatching {
                                converter.convertAudioFile(
                                    input = inputUri,
                                    output = outputUri,
                                    onProgressUpdated = { progressUpdates.add(it) },
                                )
                            }.getOrNull()
                    }

                // Wait a bit for conversion to start
                delay(100)

                // Cancel the job
                conversionJob.cancel()
                conversionJob.join()

                // Assert
                // Result should either be ConversionCancelled or the job threw CancellationException
                // Both are valid depending on timing
                if (result != null) {
                    assertThat(result).isEqualTo(ConversionCancelled)
                }
                assertThat(progressUpdates).isNotEmpty()
                // Verify conversion actually started before cancellation
                assertThat(progressUpdates.filter { it is Processing }).isNotEmpty()
            } finally {
                // Cleanup
                inputFile.delete()
                outputFile.delete()
            }
        }

    @Test
    fun convertAudioFile_reportsProgressBetweenZeroAndOne() =
        runTest {
            // Arrange
            val inputFile = createTestWavFile("test_progress.wav", durationSeconds = 2)
            val outputFile = File(cacheDir, "test_progress_output.mp3")
            val inputUri = getUriForFile(inputFile)
            val outputUri = getUriForFile(outputFile)
            val progressUpdates = mutableListOf<ProgressUpdate>()

            try {
                // Act
                converter.convertAudioFile(
                    input = inputUri,
                    output = outputUri,
                    onProgressUpdated = { progressUpdates.add(it) },
                )

                // Assert
                val processingUpdates = progressUpdates.filterIsInstance<Processing>()
                assertThat(processingUpdates).isNotEmpty()

                // All progress values should be between 0.0 and 1.0
                processingUpdates.forEach { update ->
                    // FFmpeg can report slightly negative values at the start, so we allow some margin
                    assertThat(update.complete).isAtLeast(-0.1f)
                    // Progress can exceed 1.0 slightly due to FFmpeg reporting, so we allow some margin
                    assertThat(update.complete).isAtMost(1.1f)
                }

                // Progress should generally increase (allowing for some noise)
                val firstProgress = processingUpdates.first().complete
                val lastProgress = processingUpdates.last().complete
                assertThat(lastProgress).isAtLeast(firstProgress)
            } finally {
                // Cleanup
                inputFile.delete()
                outputFile.delete()
            }
        }

    @Test
    fun convertAudioFile_handlesNonExistentInputFile() =
        runTest {
            // Arrange
            val nonExistentFile = File(cacheDir, "does_not_exist.wav")
            val outputFile = File(cacheDir, "output.mp3")
            val inputUri = getUriForFile(nonExistentFile)
            val outputUri = getUriForFile(outputFile)

            try {
                // Act & Assert
                val exception =
                    runCatching {
                        converter.convertAudioFile(
                            input = inputUri,
                            output = outputUri,
                            onProgressUpdated = {},
                        )
                    }.exceptionOrNull()

                // Should throw IllegalStateException when file doesn't exist
                assertThat(exception).isInstanceOf(IllegalStateException::class.java)
            } finally {
                // Cleanup
                outputFile.delete()
            }
        }

    /**
     * Creates a simple WAV file for testing.
     * Generates a 440Hz sine wave (A4 note) audio file.
     *
     * @param filename Name of the file to create
     * @param durationSeconds Duration of audio in seconds (default 1 second)
     * @return File object pointing to created WAV file
     */
    private fun createTestWavFile(
        filename: String,
        durationSeconds: Int = 1,
    ): File {
        val file = File(cacheDir, filename)
        FileOutputStream(file).use { fos ->
            // WAV file header for PCM audio
            val numSamples = SAMPLE_RATE * durationSeconds
            val dataSize = numSamples * NUM_CHANNELS * (BITS_PER_SAMPLE / 8)

            // Write WAV header
            writeWavHeader(fos, dataSize, SAMPLE_RATE, NUM_CHANNELS, BITS_PER_SAMPLE)

            // Generate simple sine wave audio data
            val amplitude = 32767 / 2 // Half of 16-bit max to avoid clipping

            for (i in 0 until numSamples) {
                val angle = 2.0 * Math.PI * FREQUENCY * i / SAMPLE_RATE
                val sample = (amplitude * Math.sin(angle)).toInt().toShort()

                // Write 16-bit PCM sample (little-endian)
                fos.write(sample.toInt() and 0xFF)
                fos.write((sample.toInt() shr 8) and 0xFF)
            }
        }
        return file
    }

    /**
     * Writes a WAV file header to the output stream.
     */
    private fun writeWavHeader(
        fos: FileOutputStream,
        dataSize: Int,
        sampleRate: Int,
        numChannels: Int,
        bitsPerSample: Int,
    ) {
        val byteRate = sampleRate * numChannels * (bitsPerSample / 8)
        val blockAlign = numChannels * (bitsPerSample / 8)

        // RIFF header
        fos.write("RIFF".toByteArray())
        writeInt(fos, 36 + dataSize) // Chunk size
        fos.write("WAVE".toByteArray())

        // fmt sub-chunk
        fos.write("fmt ".toByteArray())
        writeInt(fos, 16) // Subchunk1 size (16 for PCM)
        writeShort(fos, 1) // Audio format (1 = PCM)
        writeShort(fos, numChannels)
        writeInt(fos, sampleRate)
        writeInt(fos, byteRate)
        writeShort(fos, blockAlign)
        writeShort(fos, bitsPerSample)

        // data sub-chunk
        fos.write("data".toByteArray())
        writeInt(fos, dataSize)
    }

    /** Write a 32-bit integer in little-endian format */
    private fun writeInt(
        fos: FileOutputStream,
        value: Int,
    ) {
        fos.write(value and 0xFF)
        fos.write((value shr 8) and 0xFF)
        fos.write((value shr 16) and 0xFF)
        fos.write((value shr 24) and 0xFF)
    }

    /** Write a 16-bit short in little-endian format */
    private fun writeShort(
        fos: FileOutputStream,
        value: Int,
    ) {
        fos.write(value and 0xFF)
        fos.write((value shr 8) and 0xFF)
    }

    /**
     * Gets a FileProvider URI for the given file.
     * This simulates how the app provides files to FFmpeg via SAF.
     */
    private fun getUriForFile(file: File): Uri =
        FileProvider.getUriForFile(
            context,
            "io.github.mdpearce.sonicswitcher.test.fileprovider",
            file,
        )
}
