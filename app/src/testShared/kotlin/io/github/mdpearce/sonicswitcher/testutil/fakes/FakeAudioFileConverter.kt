package io.github.mdpearce.sonicswitcher.testutil.fakes

import android.net.Uri
import io.github.mdpearce.sonicswitcher.converter.AudioFileConverter
import io.github.mdpearce.sonicswitcher.converter.results.ConversionComplete
import io.github.mdpearce.sonicswitcher.converter.results.ConversionException
import io.github.mdpearce.sonicswitcher.converter.results.ConversionResult
import io.github.mdpearce.sonicswitcher.converter.results.Inactive
import io.github.mdpearce.sonicswitcher.converter.results.Processing
import io.github.mdpearce.sonicswitcher.converter.results.ProgressUpdate
import kotlinx.coroutines.delay

/**
 * Fake implementation of [AudioFileConverter] for testing.
 * Simulates audio conversion without actually using FFmpegKit.
 */
class FakeAudioFileConverter : AudioFileConverter {
    /**
     * Controls whether the next conversion should succeed or fail.
     */
    var shouldSucceed = true

    /**
     * The total simulated conversion time in milliseconds.
     */
    var conversionDelayMs = 100L

    /**
     * The error message to use when [shouldSucceed] is false.
     */
    var errorMessage = "Test conversion failure"

    /**
     * Records the last input/output URIs used for conversion (for test assertions).
     */
    var lastConversionInput: Uri? = null
    var lastConversionOutput: Uri? = null

    /**
     * Count of how many times convertAudioFile was called.
     */
    var conversionCallCount = 0

    override suspend fun convertAudioFile(
        input: Uri,
        output: Uri,
        onProgressUpdated: (ProgressUpdate) -> Unit,
    ): ConversionResult {
        conversionCallCount++
        lastConversionInput = input
        lastConversionOutput = output

        // Simulate conversion with progress updates
        onProgressUpdated(Processing(0.0f))
        delay(conversionDelayMs / 4)

        onProgressUpdated(Processing(0.25f))
        delay(conversionDelayMs / 4)

        onProgressUpdated(Processing(0.5f))
        delay(conversionDelayMs / 4)

        onProgressUpdated(Processing(0.75f))
        delay(conversionDelayMs / 4)

        onProgressUpdated(Inactive)

        return if (shouldSucceed) {
            ConversionComplete
        } else {
            throw ConversionException(errorMessage)
        }
    }

    /**
     * Test-only method to reset the converter state.
     */
    fun reset() {
        shouldSucceed = true
        conversionDelayMs = 100L
        errorMessage = "Test conversion failure"
        lastConversionInput = null
        lastConversionOutput = null
        conversionCallCount = 0
    }
}
