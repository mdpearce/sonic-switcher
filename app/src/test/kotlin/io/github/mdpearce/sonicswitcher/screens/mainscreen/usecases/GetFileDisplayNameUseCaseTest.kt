package io.github.mdpearce.sonicswitcher.screens.mainscreen.usecases

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GetFileDisplayNameUseCaseTest {
    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var useCase: GetFileDisplayNameUseCase

    @Before
    fun setup() {
        context = mockk()
        contentResolver = mockk()
        useCase = GetFileDisplayNameUseCase(context, contentResolver)
    }

    @Test
    fun `returns display name from content resolver for content URI`() {
        // Arrange
        val uri = Uri.parse("content://media/audio/123")
        val expectedName = "song.mp3"
        val cursor =
            mockk<Cursor> {
                every { moveToFirst() } returns true
                every { getColumnIndex(OpenableColumns.DISPLAY_NAME) } returns 0
                every { getString(0) } returns expectedName
                every { close() } just runs
            }
        every { contentResolver.query(uri, null, null, null, null) } returns cursor

        // Act
        val result = useCase(uri)

        // Assert
        assertThat(result).isEqualTo(expectedName)
        verify { cursor.close() }
    }

    @Test
    fun `returns unknown for null URI`() {
        // Act
        val result = useCase(null)

        // Assert
        assertThat(result).isEqualTo("unknown")
    }

    @Test
    fun `returns unknown when content resolver query returns null cursor`() {
        // Arrange
        val uri = Uri.parse("content://media/audio/123")
        every { contentResolver.query(uri, null, null, null, null) } returns null

        // Act
        val result = useCase(uri)

        // Assert
        // When cursor is null, falls back to path segments, and "123" is the last segment
        assertThat(result).isEqualTo("123")
    }

    @Test
    fun `returns path segment when cursor is empty`() {
        // Arrange
        val uri = Uri.parse("content://media/audio/123")
        val cursor =
            mockk<Cursor> {
                every { moveToFirst() } returns false
                every { close() } just runs
            }
        every { contentResolver.query(uri, null, null, null, null) } returns cursor

        // Act
        val result = useCase(uri)

        // Assert
        // Falls back to last path segment
        assertThat(result).isEqualTo("123")
        verify { cursor.close() }
    }

    @Test
    fun `returns path segment when DISPLAY_NAME column index is invalid`() {
        // Arrange
        val uri = Uri.parse("content://media/audio/456")
        val cursor =
            mockk<Cursor> {
                every { moveToFirst() } returns true
                every { getColumnIndex(OpenableColumns.DISPLAY_NAME) } returns -1
                every { close() } just runs
            }
        every { contentResolver.query(uri, null, null, null, null) } returns cursor

        // Act
        val result = useCase(uri)

        // Assert
        // Falls back to last path segment
        assertThat(result).isEqualTo("456")
        verify { cursor.close() }
    }

    @Test
    fun `falls back to last path segment for file URI`() {
        // Arrange
        val uri = Uri.parse("file:///storage/emulated/0/Music/test.mp3")

        // Act
        val result = useCase(uri)

        // Assert
        assertThat(result).isEqualTo("test.mp3")
    }

    @Test
    fun `falls back to last path segment when content resolver returns null display name`() {
        // Arrange
        val uri = Uri.parse("content://media/audio/123/song.mp3")
        val cursor =
            mockk<Cursor> {
                every { moveToFirst() } returns true
                every { getColumnIndex(OpenableColumns.DISPLAY_NAME) } returns 0
                every { getString(0) } returns null
                every { close() } just runs
            }
        every { contentResolver.query(uri, null, null, null, null) } returns cursor

        // Act
        val result = useCase(uri)

        // Assert
        assertThat(result).isEqualTo("song.mp3")
        verify { cursor.close() }
    }

    @Test
    fun `returns unknown when URI has no path segments and no display name`() {
        // Arrange
        val uri = Uri.parse("content://")
        every { contentResolver.query(uri, null, null, null, null) } returns null

        // Act
        val result = useCase(uri)

        // Assert
        assertThat(result).isEqualTo("unknown")
    }
}
