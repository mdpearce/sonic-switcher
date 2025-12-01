package io.github.mdpearce.sonicswitcher.screens.mainscreen

import android.net.Uri
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.mdpearce.sonicswitcher.converter.results.Inactive
import io.github.mdpearce.sonicswitcher.data.ConvertedFile
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import io.github.mdpearce.sonicswitcher.converter.results.Processing as ProgressProcessing

/**
 * UI tests for MainScreen composable.
 * Tests critical user journeys and UI state rendering without full integration.
 */
@RunWith(AndroidJUnit4::class)
class MainScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_showsSelectFileMessage() {
        // Arrange & Act
        composeTestRule.setContent {
            MainScreenContent(
                onOpenFileChooserClicked = {},
                onConvertClicked = {},
                onShareClicked = {},
                onAddToQueueClicked = {},
                onShareAllQueuedClicked = {},
                onClearQueueClicked = {},
                screenState = Empty,
                queuedFiles = emptyList(),
                queueCount = 0,
            )
        }

        // Assert
        composeTestRule
            .onNodeWithText("Please select a file to begin")
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_showsChooseFileButton() {
        // Arrange & Act
        composeTestRule.setContent {
            MainScreenContent(
                onOpenFileChooserClicked = {},
                onConvertClicked = {},
                onShareClicked = {},
                onAddToQueueClicked = {},
                onShareAllQueuedClicked = {},
                onClearQueueClicked = {},
                screenState = Empty,
                queuedFiles = emptyList(),
                queueCount = 0,
            )
        }

        // Assert
        composeTestRule
            .onNodeWithText("Choose file")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun emptyState_clickChooseFileButton_triggersCallback() {
        // Arrange
        var callbackTriggered = false
        composeTestRule.setContent {
            MainScreenContent(
                onOpenFileChooserClicked = { callbackTriggered = true },
                onConvertClicked = {},
                onShareClicked = {},
                onAddToQueueClicked = {},
                onShareAllQueuedClicked = {},
                onClearQueueClicked = {},
                screenState = Empty,
                queuedFiles = emptyList(),
                queueCount = 0,
            )
        }

        // Act
        composeTestRule
            .onNodeWithText("Choose file")
            .performClick()

        // Assert
        assertThat(callbackTriggered).isTrue()
    }

    @Test
    fun inputFileChosenState_displaysFileName() {
        // Arrange
        val testUri = mockk<Uri>(relaxed = true)
        val displayName = "test_audio.mp3"

        // Act
        composeTestRule.setContent {
            MainScreenContent(
                onOpenFileChooserClicked = {},
                onConvertClicked = {},
                onShareClicked = {},
                onAddToQueueClicked = {},
                onShareAllQueuedClicked = {},
                onClearQueueClicked = {},
                screenState = InputFileChosen(testUri, displayName),
                queuedFiles = emptyList(),
                queueCount = 0,
            )
        }

        // Assert
        composeTestRule
            .onNodeWithText(displayName)
            .assertIsDisplayed()
    }

    @Test
    fun inputFileChosenState_showsConvertButton() {
        // Arrange
        val testUri = mockk<Uri>(relaxed = true)

        // Act
        composeTestRule.setContent {
            MainScreenContent(
                onOpenFileChooserClicked = {},
                onConvertClicked = {},
                onShareClicked = {},
                onAddToQueueClicked = {},
                onShareAllQueuedClicked = {},
                onClearQueueClicked = {},
                screenState = InputFileChosen(testUri, "test.mp3"),
                queuedFiles = emptyList(),
                queueCount = 0,
            )
        }

        // Assert
        composeTestRule
            .onNodeWithText("Convert")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun inputFileChosenState_clickConvert_triggersCallbackWithUri() {
        // Arrange
        val testUri = mockk<Uri>(relaxed = true)
        var callbackUri: Uri? = null

        composeTestRule.setContent {
            MainScreenContent(
                onOpenFileChooserClicked = {},
                onConvertClicked = { callbackUri = it },
                onShareClicked = {},
                onAddToQueueClicked = {},
                onShareAllQueuedClicked = {},
                onClearQueueClicked = {},
                screenState = InputFileChosen(testUri, "test.mp3"),
                queuedFiles = emptyList(),
                queueCount = 0,
            )
        }

        // Act
        composeTestRule
            .onNodeWithText("Convert")
            .performClick()

        // Assert
        assertThat(callbackUri).isEqualTo(testUri)
    }

    @Test
    fun processingState_inactive_convertButtonStillVisible() {
        // Arrange & Act
        composeTestRule.setContent {
            MainScreenContent(
                onOpenFileChooserClicked = {},
                onConvertClicked = {},
                onShareClicked = {},
                onAddToQueueClicked = {},
                onShareAllQueuedClicked = {},
                onClearQueueClicked = {},
                screenState = Processing(Inactive),
                queuedFiles = emptyList(),
                queueCount = 0,
            )
        }

        // Assert - verify convert button exists (even if disabled)
        composeTestRule
            .onNodeWithText("Convert")
            .assertIsDisplayed()
    }

    @Test
    fun processingState_withProgress_convertButtonStillVisible() {
        // Arrange & Act
        composeTestRule.setContent {
            MainScreenContent(
                onOpenFileChooserClicked = {},
                onConvertClicked = {},
                onShareClicked = {},
                onAddToQueueClicked = {},
                onShareAllQueuedClicked = {},
                onClearQueueClicked = {},
                screenState = Processing(ProgressProcessing(0.5f)),
                queuedFiles = emptyList(),
                queueCount = 0,
            )
        }

        // Assert - verify convert button exists (even if disabled)
        composeTestRule
            .onNodeWithText("Convert")
            .assertIsDisplayed()
    }

    @Test
    fun completeState_showsShareButton() {
        // Arrange
        val outputUri = mockk<Uri>(relaxed = true)

        // Act
        composeTestRule.setContent {
            MainScreenContent(
                onOpenFileChooserClicked = {},
                onConvertClicked = {},
                onShareClicked = {},
                onAddToQueueClicked = {},
                onShareAllQueuedClicked = {},
                onClearQueueClicked = {},
                screenState = Complete(outputUri),
                queuedFiles = emptyList(),
                queueCount = 0,
            )
        }

        // Assert
        composeTestRule
            .onNodeWithText("Share file")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun completeState_showsAddToQueueButton() {
        // Arrange
        val outputUri = mockk<Uri>(relaxed = true)

        // Act
        composeTestRule.setContent {
            MainScreenContent(
                onOpenFileChooserClicked = {},
                onConvertClicked = {},
                onShareClicked = {},
                onAddToQueueClicked = {},
                onShareAllQueuedClicked = {},
                onClearQueueClicked = {},
                screenState = Complete(outputUri),
                queuedFiles = emptyList(),
                queueCount = 0,
            )
        }

        // Assert
        composeTestRule
            .onNodeWithText("Add to queue")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun completeState_clickShareButton_triggersCallbackWithUri() {
        // Arrange
        val outputUri = mockk<Uri>(relaxed = true)
        var callbackUri: Uri? = null

        composeTestRule.setContent {
            MainScreenContent(
                onOpenFileChooserClicked = {},
                onConvertClicked = {},
                onShareClicked = { callbackUri = it },
                onAddToQueueClicked = {},
                onShareAllQueuedClicked = {},
                onClearQueueClicked = {},
                screenState = Complete(outputUri),
                queuedFiles = emptyList(),
                queueCount = 0,
            )
        }

        // Act
        composeTestRule
            .onNodeWithText("Share file")
            .performClick()

        // Assert
        assertThat(callbackUri).isEqualTo(outputUri)
    }

    @Test
    fun completeState_clickAddToQueueButton_triggersCallbackWithUri() {
        // Arrange
        val outputUri = mockk<Uri>(relaxed = true)
        var callbackUri: Uri? = null

        composeTestRule.setContent {
            MainScreenContent(
                onOpenFileChooserClicked = {},
                onConvertClicked = {},
                onShareClicked = {},
                onAddToQueueClicked = { callbackUri = it },
                onShareAllQueuedClicked = {},
                onClearQueueClicked = {},
                screenState = Complete(outputUri),
                queuedFiles = emptyList(),
                queueCount = 0,
            )
        }

        // Act
        composeTestRule
            .onNodeWithText("Add to queue")
            .performClick()

        // Assert
        assertThat(callbackUri).isEqualTo(outputUri)
    }

    @Test
    fun errorState_displaysErrorMessage() {
        // Arrange
        val errorMessage = "Conversion failed: Invalid file format"

        // Act
        composeTestRule.setContent {
            MainScreenContent(
                onOpenFileChooserClicked = {},
                onConvertClicked = {},
                onShareClicked = {},
                onAddToQueueClicked = {},
                onShareAllQueuedClicked = {},
                onClearQueueClicked = {},
                screenState = Error(errorMessage),
                queuedFiles = emptyList(),
                queueCount = 0,
            )
        }

        // Assert
        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
    }

    @Test
    fun queueSection_withNoFiles_doesNotDisplay() {
        // Arrange & Act
        composeTestRule.setContent {
            MainScreenContent(
                onOpenFileChooserClicked = {},
                onConvertClicked = {},
                onShareClicked = {},
                onAddToQueueClicked = {},
                onShareAllQueuedClicked = {},
                onClearQueueClicked = {},
                screenState = Empty,
                queuedFiles = emptyList(),
                queueCount = 0,
            )
        }

        // Assert
        composeTestRule
            .onNodeWithText("Queue (0 files)")
            .assertDoesNotExist()
    }

    @Test
    fun queueSection_withFiles_displaysFileNames() {
        // Arrange
        val mockFiles =
            listOf(
                ConvertedFile(
                    id = 1,
                    uri = mockk(relaxed = true),
                    displayName = "song1.mp3",
                    timestampMillis = 100L,
                ),
                ConvertedFile(
                    id = 2,
                    uri = mockk(relaxed = true),
                    displayName = "song2.mp3",
                    timestampMillis = 200L,
                ),
            )

        // Act
        composeTestRule.setContent {
            MainScreenContent(
                onOpenFileChooserClicked = {},
                onConvertClicked = {},
                onShareClicked = {},
                onAddToQueueClicked = {},
                onShareAllQueuedClicked = {},
                onClearQueueClicked = {},
                screenState = Empty,
                queuedFiles = mockFiles,
                queueCount = 2,
            )
        }

        // Assert
        composeTestRule
            .onNodeWithText("Queue (2 files)")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("song1.mp3")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("song2.mp3")
            .assertIsDisplayed()
    }

    @Test
    fun queueSection_withFiles_showsShareAllButton() {
        // Arrange
        val mockFiles =
            listOf(
                ConvertedFile(
                    id = 1,
                    uri = mockk(relaxed = true),
                    displayName = "test.mp3",
                    timestampMillis = 100L,
                ),
            )

        // Act
        composeTestRule.setContent {
            MainScreenContent(
                onOpenFileChooserClicked = {},
                onConvertClicked = {},
                onShareClicked = {},
                onAddToQueueClicked = {},
                onShareAllQueuedClicked = {},
                onClearQueueClicked = {},
                screenState = Empty,
                queuedFiles = mockFiles,
                queueCount = 1,
            )
        }

        // Assert - button shows "Share all (1)" with count
        composeTestRule
            .onNodeWithText("Share all (1)")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun queueSection_withFiles_showsClearQueueButton() {
        // Arrange
        val mockFiles =
            listOf(
                ConvertedFile(
                    id = 1,
                    uri = mockk(relaxed = true),
                    displayName = "test.mp3",
                    timestampMillis = 100L,
                ),
            )

        // Act
        composeTestRule.setContent {
            MainScreenContent(
                onOpenFileChooserClicked = {},
                onConvertClicked = {},
                onShareClicked = {},
                onAddToQueueClicked = {},
                onShareAllQueuedClicked = {},
                onClearQueueClicked = {},
                screenState = Empty,
                queuedFiles = mockFiles,
                queueCount = 1,
            )
        }

        // Assert - IconButton with delete icon
        composeTestRule
            .onNodeWithContentDescription("Delete all queued files")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun queueSection_clickShareAll_triggersCallback() {
        // Arrange
        var callbackTriggered = false
        val mockFiles =
            listOf(
                ConvertedFile(
                    id = 1,
                    uri = mockk(relaxed = true),
                    displayName = "test.mp3",
                    timestampMillis = 100L,
                ),
            )

        composeTestRule.setContent {
            MainScreenContent(
                onOpenFileChooserClicked = {},
                onConvertClicked = {},
                onShareClicked = {},
                onAddToQueueClicked = {},
                onShareAllQueuedClicked = { callbackTriggered = true },
                onClearQueueClicked = {},
                screenState = Empty,
                queuedFiles = mockFiles,
                queueCount = 1,
            )
        }

        // Act
        composeTestRule
            .onNodeWithText("Share all (1)")
            .performClick()

        // Assert
        assertThat(callbackTriggered).isTrue()
    }

    @Test
    fun queueSection_clickClearQueue_triggersCallback() {
        // Arrange
        var callbackTriggered = false
        val mockFiles =
            listOf(
                ConvertedFile(
                    id = 1,
                    uri = mockk(relaxed = true),
                    displayName = "test.mp3",
                    timestampMillis = 100L,
                ),
            )

        composeTestRule.setContent {
            MainScreenContent(
                onOpenFileChooserClicked = {},
                onConvertClicked = {},
                onShareClicked = {},
                onAddToQueueClicked = {},
                onShareAllQueuedClicked = {},
                onClearQueueClicked = { callbackTriggered = true },
                screenState = Empty,
                queuedFiles = mockFiles,
                queueCount = 1,
            )
        }

        // Act
        composeTestRule
            .onNodeWithContentDescription("Delete all queued files")
            .performClick()

        // Assert
        assertThat(callbackTriggered).isTrue()
    }
}
