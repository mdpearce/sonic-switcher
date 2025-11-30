package io.github.mdpearce.sonicswitcher.data

import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.mdpearce.sonicswitcher.testutil.fakes.FakeConvertedFileDao
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConvertedFileRepositoryTest {
    private lateinit var dao: FakeConvertedFileDao
    private lateinit var repository: ConvertedFileRepository

    @Before
    fun setup() {
        dao = FakeConvertedFileDao()
        repository = ConvertedFileRepository(dao)
    }

    @Test
    fun `getAllFiles emits empty list initially`() =
        runTest {
            // Act & Assert
            repository.getAllFiles().test {
                val files = awaitItem()
                assertThat(files).isEmpty()
            }
        }

    @Test
    fun `getAllFiles maps entities to domain models`() =
        runTest {
            // Arrange
            dao.insertFile(
                ConvertedFileEntity(
                    id = 1,
                    uri = "content://audio/123",
                    displayName = "test.mp3",
                    timestampMillis = 123456L,
                ),
            )

            // Act & Assert
            repository.getAllFiles().test {
                val files = awaitItem()
                assertThat(files).hasSize(1)
                assertThat(files[0].id).isEqualTo(1)
                assertThat(files[0].uri).isEqualTo(Uri.parse("content://audio/123"))
                assertThat(files[0].displayName).isEqualTo("test.mp3")
                assertThat(files[0].timestampMillis).isEqualTo(123456L)
            }
        }

    @Test
    fun `getAllFiles returns files in descending timestamp order`() =
        runTest {
            // Arrange
            dao.insertFile(
                ConvertedFileEntity(
                    uri = "content://audio/1",
                    displayName = "first.mp3",
                    timestampMillis = 100L,
                ),
            )
            dao.insertFile(
                ConvertedFileEntity(
                    uri = "content://audio/2",
                    displayName = "second.mp3",
                    timestampMillis = 200L,
                ),
            )
            dao.insertFile(
                ConvertedFileEntity(
                    uri = "content://audio/3",
                    displayName = "third.mp3",
                    timestampMillis = 150L,
                ),
            )

            // Act & Assert
            repository.getAllFiles().test {
                val files = awaitItem()
                assertThat(files).hasSize(3)
                assertThat(files[0].displayName).isEqualTo("second.mp3") // Newest
                assertThat(files[1].displayName).isEqualTo("third.mp3")
                assertThat(files[2].displayName).isEqualTo("first.mp3") // Oldest
            }
        }

    @Test
    fun `getAllFiles emits updates when new file is added`() =
        runTest {
            // Act & Assert
            repository.getAllFiles().test {
                // Initial empty state
                assertThat(awaitItem()).isEmpty()

                // Add file
                repository.addFile(
                    uri = Uri.parse("content://audio/123"),
                    displayName = "new.mp3",
                    timestampMillis = 999L,
                )

                // Verify update
                val files = awaitItem()
                assertThat(files).hasSize(1)
                assertThat(files[0].displayName).isEqualTo("new.mp3")
            }
        }

    @Test
    fun `getFileCount returns correct count`() =
        runTest {
            // Arrange & Act
            repository.getFileCount().test {
                // Initial count
                assertThat(awaitItem()).isEqualTo(0)

                // Add files
                repository.addFile(Uri.parse("content://1"), "file1.mp3", 100L)
                assertThat(awaitItem()).isEqualTo(1)

                repository.addFile(Uri.parse("content://2"), "file2.mp3", 200L)
                assertThat(awaitItem()).isEqualTo(2)

                repository.addFile(Uri.parse("content://3"), "file3.mp3", 300L)
                assertThat(awaitItem()).isEqualTo(3)
            }
        }

    @Test
    fun `addFile inserts entity with correct data`() =
        runTest {
            // Arrange
            val uri = Uri.parse("content://audio/456")
            val displayName = "song.mp3"
            val timestamp = 555555L

            // Act
            repository.addFile(uri, displayName, timestamp)

            // Assert
            val entities = dao.getAllFilesSnapshot()
            assertThat(entities).hasSize(1)
            assertThat(entities[0].uri).isEqualTo(uri.toString())
            assertThat(entities[0].displayName).isEqualTo(displayName)
            assertThat(entities[0].timestampMillis).isEqualTo(timestamp)
        }

    @Test
    fun `addFile generates auto-incrementing IDs`() =
        runTest {
            // Act
            repository.addFile(Uri.parse("content://1"), "file1.mp3", 100L)
            repository.addFile(Uri.parse("content://2"), "file2.mp3", 200L)
            repository.addFile(Uri.parse("content://3"), "file3.mp3", 300L)

            // Assert
            val entities = dao.getAllFilesSnapshot()
            assertThat(entities[0].id).isEqualTo(1)
            assertThat(entities[1].id).isEqualTo(2)
            assertThat(entities[2].id).isEqualTo(3)
        }

    @Test
    fun `clearAll removes all files`() =
        runTest {
            // Arrange
            repository.addFile(Uri.parse("content://1"), "file1.mp3", 100L)
            repository.addFile(Uri.parse("content://2"), "file2.mp3", 200L)

            repository.getAllFiles().test {
                // Verify files exist
                assertThat(awaitItem()).hasSize(2)

                // Act
                repository.clearAll()

                // Assert
                assertThat(awaitItem()).isEmpty()
            }
        }

    @Test
    fun `clearAll emits zero count`() =
        runTest {
            // Act & Assert
            repository.getFileCount().test {
                // Initial 0
                assertThat(awaitItem()).isEqualTo(0)

                // Add files
                repository.addFile(Uri.parse("content://1"), "file1.mp3", 100L)
                assertThat(awaitItem()).isEqualTo(1)

                repository.addFile(Uri.parse("content://2"), "file2.mp3", 200L)
                assertThat(awaitItem()).isEqualTo(2)

                // Act: Clear all
                repository.clearAll()

                // Assert
                assertThat(awaitItem()).isEqualTo(0)
            }
        }

    @Test
    fun `multiple subscribers receive same updates`() =
        runTest {
            // Act & Assert
            val flow = repository.getAllFiles()

            flow.test {
                assertThat(awaitItem()).isEmpty()

                repository.addFile(Uri.parse("content://1"), "file.mp3", 100L)
                assertThat(awaitItem()).hasSize(1)
            }

            // Second subscriber should see current state
            flow.test {
                assertThat(awaitItem()).hasSize(1)

                repository.addFile(Uri.parse("content://2"), "file2.mp3", 200L)
                assertThat(awaitItem()).hasSize(2)
            }
        }

    @Test
    fun `handles special characters in display names`() =
        runTest {
            // Arrange
            val specialName = "Test (Mix) [2024] - Artist's Song.mp3"

            // Act
            repository.addFile(
                uri = Uri.parse("content://audio/special"),
                displayName = specialName,
                timestampMillis = 12345L,
            )

            // Assert
            repository.getAllFiles().test {
                val files = awaitItem()
                assertThat(files[0].displayName).isEqualTo(specialName)
            }
        }

    @Test
    fun `handles file URIs correctly`() =
        runTest {
            // Arrange
            val fileUri = Uri.parse("file:///storage/emulated/0/Music/track.mp3")

            // Act
            repository.addFile(
                uri = fileUri,
                displayName = "track.mp3",
                timestampMillis = 99999L,
            )

            // Assert
            repository.getAllFiles().test {
                val files = awaitItem()
                assertThat(files[0].uri).isEqualTo(fileUri)
                assertThat(files[0].uri.scheme).isEqualTo("file")
            }
        }
}
