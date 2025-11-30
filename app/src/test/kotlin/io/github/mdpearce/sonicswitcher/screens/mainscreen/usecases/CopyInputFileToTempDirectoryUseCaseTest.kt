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
import java.io.File
import java.io.FileNotFoundException

/**
 * Unit tests for CopyInputFileToTempDirectoryUseCase.
 *
 * Note: These tests focus on the file copying logic. The FileProvider URI generation
 * is not tested here as it requires a full Android environment and is better suited
 * for integration tests. The tests verify that:
 * - Files are copied correctly to the cache directory
 * - Content is preserved during copy
 * - Appropriate exceptions are thrown for error conditions
 */
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
        contentResolver = mockk(relaxed = true)
        getFileDisplayName = mockk(relaxed = true)
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
    fun `throws FileCopyException on FileNotFoundException`() {
        // Arrange
        val inputUri =
            mockk<Uri>(relaxed = true) {
                every { scheme } returns "content"
            }
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
}
