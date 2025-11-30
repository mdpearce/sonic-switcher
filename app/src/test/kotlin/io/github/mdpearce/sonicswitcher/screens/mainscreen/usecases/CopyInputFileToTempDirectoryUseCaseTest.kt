package io.github.mdpearce.sonicswitcher.screens.mainscreen.usecases

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import io.github.mdpearce.sonicswitcher.screens.mainscreen.errors.FileCopyException
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException

@RunWith(RobolectricTestRunner::class)
class CopyInputFileToTempDirectoryUseCaseTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var getFileDisplayName: GetFileDisplayNameUseCase
    private lateinit var cacheDir: File
    private lateinit var useCase: CopyInputFileToTempDirectoryUseCase

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        contentResolver = mockk()
        getFileDisplayName = mockk()
        cacheDir = tempFolder.newFolder("cache")

        every { context.packageName } returns "io.github.mdpearce.sonicswitcher"
        every { context.filesDir } returns tempFolder.newFolder("files")

        useCase =
            CopyInputFileToTempDirectoryUseCase(
                context,
                contentResolver,
                getFileDisplayName,
                cacheDir,
            )
    }

    @Test
    fun `successfully copies file to cache directory`() {
        // Arrange
        val inputUri = Uri.parse("content://audio/123")
        val fileName = "test.mp3"
        val testContent = "test audio content".toByteArray()

        every { getFileDisplayName(inputUri) } returns fileName
        every { contentResolver.openInputStream(inputUri) } returns ByteArrayInputStream(testContent)

        // Act
        val result = useCase(inputUri)

        // Assert
        assertThat(result.file.exists()).isTrue()
        assertThat(result.file.name).isEqualTo(fileName)
        assertThat(result.file.parentFile).isEqualTo(cacheDir)
        assertThat(result.file.readBytes()).isEqualTo(testContent)
        assertThat(result.uri).isNotNull()
    }

    @Test
    fun `copies file with correct content`() {
        // Arrange
        val inputUri = Uri.parse("content://audio/456")
        val fileName = "audio.mp3"
        val expectedContent = ByteArray(1024) { it.toByte() }

        every { getFileDisplayName(inputUri) } returns fileName
        every { contentResolver.openInputStream(inputUri) } returns ByteArrayInputStream(expectedContent)

        // Act
        val result = useCase(inputUri)

        // Assert
        assertThat(result.file.readBytes()).isEqualTo(expectedContent)
    }

    @Test
    fun `throws FileCopyException on FileNotFoundException`() {
        // Arrange
        val inputUri = Uri.parse("content://audio/missing")
        every { getFileDisplayName(inputUri) } returns "missing.mp3"
        every { contentResolver.openInputStream(inputUri) } throws FileNotFoundException("File not found")

        // Act & Assert
        try {
            useCase(inputUri)
            assertThat(false).isTrue() // Should not reach here
        } catch (e: FileCopyException) {
            assertThat(e.message).contains("File not found")
            assertThat(e.cause).isInstanceOf(FileNotFoundException::class.java)
        }
    }

    @Test
    fun `creates file with display name from use case`() {
        // Arrange
        val inputUri = Uri.parse("content://audio/123")
        val expectedFileName = "My Song.mp3"

        every { getFileDisplayName(inputUri) } returns expectedFileName
        every { contentResolver.openInputStream(inputUri) } returns ByteArrayInputStream(ByteArray(10))

        // Act
        val result = useCase(inputUri)

        // Assert
        assertThat(result.file.name).isEqualTo(expectedFileName)
    }
}
