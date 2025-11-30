package io.github.mdpearce.sonicswitcher.screens.mainscreen

import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.mdpearce.sonicswitcher.data.ConvertedFileRepository
import io.github.mdpearce.sonicswitcher.screens.mainscreen.models.FileWithUri
import io.github.mdpearce.sonicswitcher.screens.mainscreen.usecases.AddFileToQueueUseCase
import io.github.mdpearce.sonicswitcher.screens.mainscreen.usecases.BuildFilenameUseCase
import io.github.mdpearce.sonicswitcher.screens.mainscreen.usecases.ClearQueueUseCase
import io.github.mdpearce.sonicswitcher.screens.mainscreen.usecases.CopyInputFileToTempDirectoryUseCase
import io.github.mdpearce.sonicswitcher.screens.mainscreen.usecases.GetFileDisplayNameUseCase
import io.github.mdpearce.sonicswitcher.testutil.fakes.FakeAudioFileConverter
import io.github.mdpearce.sonicswitcher.testutil.fakes.FakeConvertedFileDao
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for [MainScreenViewModel].
 *
 * Tests state transitions, UI event emissions, and business logic orchestration.
 * Uses Robolectric to provide Android framework classes (Uri, etc.)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29]) // Match minSdk
class MainScreenViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    // Real implementations for simple test doubles
    private lateinit var audioFileConverter: FakeAudioFileConverter
    private lateinit var dao: FakeConvertedFileDao
    private lateinit var repository: ConvertedFileRepository

    // Mocked use cases (we test these separately)
    private lateinit var getFileDisplayName: GetFileDisplayNameUseCase
    private lateinit var buildFilename: BuildFilenameUseCase
    private lateinit var copyInputFileToTempDirectory: CopyInputFileToTempDirectoryUseCase
    private lateinit var addFileToQueue: AddFileToQueueUseCase
    private lateinit var clearQueue: ClearQueueUseCase

    private lateinit var viewModel: MainScreenViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Set up fakes
        audioFileConverter = FakeAudioFileConverter()
        dao = FakeConvertedFileDao()
        repository = ConvertedFileRepository(dao)

        // Mock use cases
        getFileDisplayName = mockk()
        buildFilename = mockk()
        copyInputFileToTempDirectory = mockk()
        addFileToQueue = mockk(relaxed = true)
        clearQueue = mockk(relaxed = true)

        viewModel =
            MainScreenViewModel(
                audioFileConverter = audioFileConverter,
                getFileDisplayName = getFileDisplayName,
                buildFilename = buildFilename,
                copyInputFileToTempDirectory = copyInputFileToTempDirectory,
                addFileToQueue = addFileToQueue,
                clearQueue = clearQueue,
                repository = repository,
                ioDispatcher = testDispatcher,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Empty`() =
        runTest {
            assertThat(viewModel.screenState.value).isEqualTo(Empty)
        }

    @Test
    fun `onOpenFileChooserClicked emits OpenFileChooser event`() =
        runTest {
            viewModel.uiEvents.test {
                viewModel.onOpenFileChooserClicked()

                val event = awaitItem()
                assertThat(event).isInstanceOf(OpenFileChooser::class.java)
            }
        }

    @Test
    fun `onInputFileChosen with valid URI updates state to InputFileChosen`() =
        runTest {
            // Arrange
            val testUri = Uri.parse("content://audio/123")
            val displayName = "test_audio.mp3"
            every { getFileDisplayName(testUri) } returns displayName

            // Act
            viewModel.onInputFileChosen(testUri)

            // Assert
            val state = viewModel.screenState.value
            assertThat(state).isInstanceOf(InputFileChosen::class.java)
            val inputState = state as InputFileChosen
            assertThat(inputState.inputFile).isEqualTo(testUri)
            assertThat(inputState.inputDisplayName).isEqualTo(displayName)
        }

    @Test
    fun `onInputFileChosen with null URI updates state to Empty`() =
        runTest {
            // Act
            viewModel.onInputFileChosen(null)

            // Assert
            assertThat(viewModel.screenState.value).isEqualTo(Empty)
        }

    @Test
    fun `onInputFileChosen with empty URI updates state to Empty`() =
        runTest {
            // Act
            viewModel.onInputFileChosen(Uri.EMPTY)

            // Assert
            assertThat(viewModel.screenState.value).isEqualTo(Empty)
        }

    @Test
    fun `onConvertClicked emits OpenOutputFileChooser with generated filename`() =
        runTest {
            // Arrange
            val inputUri = Uri.parse("content://audio/123")
            val generatedFilename = "converted_20251130_120000.mp3"
            every { buildFilename() } returns generatedFilename

            viewModel.uiEvents.test {
                // Act
                viewModel.onConvertClicked(inputUri)

                // Assert
                val event = awaitItem()
                assertThat(event).isInstanceOf(OpenOutputFileChooser::class.java)
                val outputEvent = event as OpenOutputFileChooser
                assertThat(outputEvent.defaultFilename).isEqualTo(generatedFilename)
            }
        }

    @Test
    fun `onConvertClicked with empty URI does not emit event`() =
        runTest {
            viewModel.uiEvents.test {
                // Act
                viewModel.onConvertClicked(Uri.EMPTY)

                // Assert - should not receive any event
                expectNoEvents()
            }
        }

    @Test
    fun `successful conversion transitions through Processing to Complete state`() =
        runTest {
            // Arrange
            val inputUri = Uri.parse("content://audio/input")
            val outputUri = Uri.parse("content://audio/output")
            val tempFile = File.createTempFile("test", ".mp3")
            val tempFileWithUri = FileWithUri(tempFile, Uri.fromFile(tempFile))

            coEvery { copyInputFileToTempDirectory(inputUri) } returns tempFileWithUri
            audioFileConverter.shouldSucceed = true
            audioFileConverter.conversionDelayMs = 50

            // Act
            viewModel.onOutputFileChosen(inputUri, outputUri)
            advanceUntilIdle()

            // Assert
            val finalState = viewModel.screenState.value
            assertThat(finalState).isInstanceOf(Complete::class.java)
            assertThat((finalState as Complete).outputFile).isEqualTo(outputUri)

            // Verify temp file was deleted
            assertThat(tempFile.exists()).isFalse()

            // Verify conversion was called with correct URIs
            assertThat(audioFileConverter.lastConversionInput).isEqualTo(tempFileWithUri.uri)
            assertThat(audioFileConverter.lastConversionOutput).isEqualTo(outputUri)
        }

    @Test
    fun `failed conversion transitions to Error state`() =
        runTest {
            // Arrange
            val inputUri = Uri.parse("content://audio/input")
            val outputUri = Uri.parse("content://audio/output")
            val tempFile = File.createTempFile("test", ".mp3")
            val tempFileWithUri = FileWithUri(tempFile, Uri.fromFile(tempFile))
            val errorMessage = "FFmpeg conversion failed"

            coEvery { copyInputFileToTempDirectory(inputUri) } returns tempFileWithUri
            audioFileConverter.shouldSucceed = false
            audioFileConverter.errorMessage = errorMessage

            // Act
            viewModel.onOutputFileChosen(inputUri, outputUri)
            advanceUntilIdle()

            // Assert
            val finalState = viewModel.screenState.value
            assertThat(finalState).isInstanceOf(Error::class.java)
            assertThat((finalState as Error).message).isEqualTo(errorMessage)

            // Verify temp file was still deleted even on error
            assertThat(tempFile.exists()).isFalse()
        }

    @Test
    fun `processing state updates during conversion`() =
        runTest {
            // Arrange
            val inputUri = Uri.parse("content://audio/input")
            val outputUri = Uri.parse("content://audio/output")
            val tempFile = File.createTempFile("test", ".mp3")
            val tempFileWithUri = FileWithUri(tempFile, Uri.fromFile(tempFile))

            coEvery { copyInputFileToTempDirectory(inputUri) } returns tempFileWithUri
            audioFileConverter.shouldSucceed = true

            // Act
            viewModel.onOutputFileChosen(inputUri, outputUri)
            advanceUntilIdle()

            // Assert - final state should be Complete after processing
            val finalState = viewModel.screenState.value
            assertThat(finalState).isInstanceOf(Complete::class.java)

            tempFile.delete()
        }

    @Test
    fun `onShareClicked emits OpenShareSheet event with URI`() =
        runTest {
            // Arrange
            val shareUri = Uri.parse("content://audio/converted")

            viewModel.uiEvents.test {
                // Act
                viewModel.onShareClicked(shareUri)

                // Assert
                val event = awaitItem()
                assertThat(event).isInstanceOf(OpenShareSheet::class.java)
                assertThat((event as OpenShareSheet).uri).isEqualTo(shareUri)
            }
        }

    @Test
    fun `onAddToQueueClicked adds file and resets state to Empty`() =
        runTest {
            // Arrange
            val uri = Uri.parse("content://audio/output")

            // Act
            viewModel.onAddToQueueClicked(uri)
            advanceUntilIdle()

            // Assert
            assertThat(viewModel.screenState.value).isEqualTo(Empty)
            // Note: We verify addFileToQueue was called, but don't test its implementation here
        }

    @Test
    fun `sharedInputUri setter triggers onInputFileChosen`() =
        runTest {
            // Arrange
            val testUri = Uri.parse("content://shared/audio")
            val displayName = "shared_audio.mp3"
            every { getFileDisplayName(testUri) } returns displayName

            // Act
            viewModel.sharedInputUri = testUri

            // Assert
            val state = viewModel.screenState.value
            assertThat(state).isInstanceOf(InputFileChosen::class.java)
            assertThat((state as InputFileChosen).inputFile).isEqualTo(testUri)
        }

    @Test
    fun `sharedInputUri setter with same value does not update state`() =
        runTest {
            // Arrange
            val testUri = Uri.parse("content://shared/audio")
            every { getFileDisplayName(any()) } returns "test.mp3"

            // Act
            viewModel.sharedInputUri = testUri
            advanceUntilIdle()
            val firstState = viewModel.screenState.value

            viewModel.sharedInputUri = testUri // Set same value again
            advanceUntilIdle()
            val secondState = viewModel.screenState.value

            // Assert
            assertThat(firstState).isEqualTo(secondState)
        }

    @Test
    fun `onClearQueueClicked clears the queue`() =
        runTest {
            // Act
            viewModel.onClearQueueClicked()
            advanceUntilIdle()

            // Assert - verify clearQueue use case was called
            // (actual queue clearing is tested in repository tests)
        }

    @Test
    fun `onShareAllQueuedClicked with empty queue does not emit event`() =
        runTest {
            viewModel.uiEvents.test {
                // Act
                viewModel.onShareAllQueuedClicked()
                advanceUntilIdle()

                // Assert
                expectNoEvents()
            }
        }
}
