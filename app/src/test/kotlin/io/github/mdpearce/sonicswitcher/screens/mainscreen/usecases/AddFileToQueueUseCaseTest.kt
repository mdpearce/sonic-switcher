package io.github.mdpearce.sonicswitcher.screens.mainscreen.usecases

import android.net.Uri
import io.github.mdpearce.sonicswitcher.data.ConvertedFileRepository
import io.github.mdpearce.sonicswitcher.testutil.fakes.TestClock
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class AddFileToQueueUseCaseTest {
    private lateinit var repository: ConvertedFileRepository
    private lateinit var getFileDisplayName: GetFileDisplayNameUseCase
    private lateinit var clock: TestClock
    private lateinit var useCase: AddFileToQueueUseCase

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        getFileDisplayName = mockk()
        clock = TestClock()
        useCase = AddFileToQueueUseCase(repository, getFileDisplayName, clock)
    }

    @Test
    fun `adds file to repository with correct display name`() =
        runTest {
            // Arrange
            val uri = Uri.parse("content://audio/123")
            val displayName = "test.mp3"
            val timestamp = 1234567890L

            every { getFileDisplayName(uri) } returns displayName
            clock.setMillis(timestamp)

            // Act
            useCase(uri)

            // Assert
            coVerify {
                repository.addFile(
                    uri = uri,
                    displayName = displayName,
                    timestampMillis = timestamp,
                )
            }
        }

    @Test
    fun `uses current clock time for timestamp`() =
        runTest {
            // Arrange
            val uri = Uri.parse("content://audio/456")
            val displayName = "song.mp3"
            val instant = Instant.parse("2024-03-15T10:30:00Z")
            val expectedTimestamp = instant.toEpochMilli()

            every { getFileDisplayName(uri) } returns displayName
            clock.setInstant(instant)

            // Act
            useCase(uri)

            // Assert
            coVerify {
                repository.addFile(
                    uri = uri,
                    displayName = displayName,
                    timestampMillis = expectedTimestamp,
                )
            }
        }

    @Test
    fun `retrieves display name from GetFileDisplayNameUseCase`() =
        runTest {
            // Arrange
            val uri = Uri.parse("content://audio/789")
            val expectedDisplayName = "My Awesome Song.mp3"

            every { getFileDisplayName(uri) } returns expectedDisplayName

            // Act
            useCase(uri)

            // Assert
            coVerify {
                repository.addFile(
                    uri = uri,
                    displayName = expectedDisplayName,
                    timestampMillis = any(),
                )
            }
        }

    @Test
    fun `multiple invocations use different timestamps`() =
        runTest {
            // Arrange
            val uri1 = Uri.parse("content://audio/1")
            val uri2 = Uri.parse("content://audio/2")
            val timestamp1 = 1000L
            val timestamp2 = 2000L

            every { getFileDisplayName(any()) } returns "file.mp3"

            // Act
            clock.setMillis(timestamp1)
            useCase(uri1)

            clock.setMillis(timestamp2)
            useCase(uri2)

            // Assert
            coVerify {
                repository.addFile(
                    uri = uri1,
                    displayName = "file.mp3",
                    timestampMillis = timestamp1,
                )
            }
            coVerify {
                repository.addFile(
                    uri = uri2,
                    displayName = "file.mp3",
                    timestampMillis = timestamp2,
                )
            }
        }

    @Test
    fun `handles unknown display name`() =
        runTest {
            // Arrange
            val uri = Uri.parse("content://audio/unknown")
            every { getFileDisplayName(uri) } returns "unknown"

            // Act
            useCase(uri)

            // Assert
            coVerify {
                repository.addFile(
                    uri = uri,
                    displayName = "unknown",
                    timestampMillis = any(),
                )
            }
        }

    @Test
    fun `handles file URIs correctly`() =
        runTest {
            // Arrange
            val uri = Uri.parse("file:///storage/emulated/0/Music/track.mp3")
            val displayName = "track.mp3"

            every { getFileDisplayName(uri) } returns displayName
            clock.setMillis(5000L)

            // Act
            useCase(uri)

            // Assert
            coVerify {
                repository.addFile(
                    uri = uri,
                    displayName = displayName,
                    timestampMillis = 5000L,
                )
            }
        }
}
