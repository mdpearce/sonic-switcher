package io.github.mdpearce.sonicswitcher.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for ConvertedFileDao using an in-memory Room database.
 * These tests verify the DAO's CRUD operations and Flow emissions with a real database.
 */
@RunWith(AndroidJUnit4::class)
class ConvertedFileDaoTest {
    private lateinit var database: SonicSwitcherDatabase
    private lateinit var dao: ConvertedFileDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(
                    context,
                    SonicSwitcherDatabase::class.java,
                ).build()
        dao = database.convertedFileDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getAllFiles_emitsEmptyListInitially() =
        runTest {
            // Act & Assert
            dao.getAllFiles().test {
                val files = awaitItem()
                assertThat(files).isEmpty()
            }
        }

    @Test
    fun insertFile_emitsNewFile() =
        runTest {
            // Arrange
            val file =
                ConvertedFileEntity(
                    uri = "content://audio/123",
                    displayName = "test.mp3",
                    timestampMillis = 1000L,
                )

            // Act & Assert
            dao.getAllFiles().test {
                // Initial empty state
                assertThat(awaitItem()).isEmpty()

                dao.insertFile(file)

                // Verify file was inserted
                val files = awaitItem()
                assertThat(files).hasSize(1)
                assertThat(files[0].uri).isEqualTo("content://audio/123")
                assertThat(files[0].displayName).isEqualTo("test.mp3")
                assertThat(files[0].timestampMillis).isEqualTo(1000L)
            }
        }

    @Test
    fun insertFile_generatesAutoIncrementingIds() =
        runTest {
            // Arrange
            val file1 =
                ConvertedFileEntity(
                    uri = "content://1",
                    displayName = "first.mp3",
                    timestampMillis = 100L,
                )
            val file2 =
                ConvertedFileEntity(
                    uri = "content://2",
                    displayName = "second.mp3",
                    timestampMillis = 200L,
                )

            // Act
            dao.insertFile(file1)
            dao.insertFile(file2)

            // Assert
            dao.getAllFiles().test {
                val files = awaitItem()
                assertThat(files).hasSize(2)
                assertThat(files[0].id).isEqualTo(2) // Newest first (DESC order)
                assertThat(files[1].id).isEqualTo(1)
            }
        }

    @Test
    fun getAllFiles_returnsFilesInDescendingTimestampOrder() =
        runTest {
            // Arrange - Insert in random order
            val file1 =
                ConvertedFileEntity(
                    uri = "content://1",
                    displayName = "old.mp3",
                    timestampMillis = 100L,
                )
            val file2 =
                ConvertedFileEntity(
                    uri = "content://2",
                    displayName = "newest.mp3",
                    timestampMillis = 300L,
                )
            val file3 =
                ConvertedFileEntity(
                    uri = "content://3",
                    displayName = "middle.mp3",
                    timestampMillis = 200L,
                )

            dao.insertFile(file1)
            dao.insertFile(file2)
            dao.insertFile(file3)

            // Act & Assert
            dao.getAllFiles().test {
                val files = awaitItem()
                assertThat(files).hasSize(3)
                // Should be ordered newest to oldest
                assertThat(files[0].displayName).isEqualTo("newest.mp3")
                assertThat(files[1].displayName).isEqualTo("middle.mp3")
                assertThat(files[2].displayName).isEqualTo("old.mp3")
            }
        }

    @Test
    fun clearAll_removesAllFiles() =
        runTest {
            // Arrange
            dao.insertFile(
                ConvertedFileEntity(
                    uri = "content://1",
                    displayName = "file1.mp3",
                    timestampMillis = 100L,
                ),
            )
            dao.insertFile(
                ConvertedFileEntity(
                    uri = "content://2",
                    displayName = "file2.mp3",
                    timestampMillis = 200L,
                ),
            )

            // Act & Assert
            dao.getAllFiles().test {
                // Verify files exist
                assertThat(awaitItem()).hasSize(2)

                dao.clearAll()

                // Verify all files removed
                assertThat(awaitItem()).isEmpty()
            }
        }

    @Test
    fun clearAll_resetsAutoIncrementCounter() =
        runTest {
            // Arrange - Insert and clear
            dao.insertFile(
                ConvertedFileEntity(
                    uri = "content://1",
                    displayName = "file1.mp3",
                    timestampMillis = 100L,
                ),
            )
            dao.clearAll()

            // Act - Insert new file after clear
            dao.insertFile(
                ConvertedFileEntity(
                    uri = "content://2",
                    displayName = "file2.mp3",
                    timestampMillis = 200L,
                ),
            )

            // Assert - ID should continue from previous counter (not reset to 1)
            dao.getAllFiles().test {
                val files = awaitItem()
                // SQLite doesn't reset auto-increment on DELETE, so ID continues
                assertThat(files[0].id).isGreaterThan(1L)
            }
        }

    @Test
    fun getCount_returnsZeroInitially() =
        runTest {
            // Act & Assert
            dao.getCount().test {
                assertThat(awaitItem()).isEqualTo(0)
            }
        }

    @Test
    fun getCount_returnsCorrectCountAfterInserts() =
        runTest {
            // Act & Assert
            dao.getCount().test {
                // Initial count
                assertThat(awaitItem()).isEqualTo(0)

                // Add files
                dao.insertFile(
                    ConvertedFileEntity(
                        uri = "content://1",
                        displayName = "file1.mp3",
                        timestampMillis = 100L,
                    ),
                )
                assertThat(awaitItem()).isEqualTo(1)

                dao.insertFile(
                    ConvertedFileEntity(
                        uri = "content://2",
                        displayName = "file2.mp3",
                        timestampMillis = 200L,
                    ),
                )
                assertThat(awaitItem()).isEqualTo(2)

                dao.insertFile(
                    ConvertedFileEntity(
                        uri = "content://3",
                        displayName = "file3.mp3",
                        timestampMillis = 300L,
                    ),
                )
                assertThat(awaitItem()).isEqualTo(3)
            }
        }

    @Test
    fun getCount_emitsZeroAfterClearAll() =
        runTest {
            // Arrange
            dao.insertFile(
                ConvertedFileEntity(
                    uri = "content://1",
                    displayName = "file1.mp3",
                    timestampMillis = 100L,
                ),
            )
            dao.insertFile(
                ConvertedFileEntity(
                    uri = "content://2",
                    displayName = "file2.mp3",
                    timestampMillis = 200L,
                ),
            )

            // Act & Assert
            dao.getCount().test {
                assertThat(awaitItem()).isEqualTo(2)

                dao.clearAll()

                assertThat(awaitItem()).isEqualTo(0)
            }
        }

    @Test
    fun insertFile_handlesSpecialCharactersInDisplayName() =
        runTest {
            // Arrange
            val specialName = "Test (Mix) [2024] - Artist's Song.mp3"
            val file =
                ConvertedFileEntity(
                    uri = "content://audio/special",
                    displayName = specialName,
                    timestampMillis = 12345L,
                )

            // Act
            dao.insertFile(file)

            // Assert
            dao.getAllFiles().test {
                val files = awaitItem()
                assertThat(files[0].displayName).isEqualTo(specialName)
            }
        }

    @Test
    fun insertFile_handlesLongUriStrings() =
        runTest {
            // Arrange
            val longUri =
                "content://com.android.providers.media.documents/document/audio%3A" +
                    "1234567890/with/very/long/path/that/exceeds/normal/uri/length"
            val file =
                ConvertedFileEntity(
                    uri = longUri,
                    displayName = "test.mp3",
                    timestampMillis = 999L,
                )

            // Act
            dao.insertFile(file)

            // Assert
            dao.getAllFiles().test {
                val files = awaitItem()
                assertThat(files[0].uri).isEqualTo(longUri)
            }
        }

    @Test
    fun multipleSubscribers_receiveTheSameUpdates() =
        runTest {
            // Arrange
            val filesFlow = dao.getAllFiles()

            // Act & Assert - First subscriber
            filesFlow.test {
                assertThat(awaitItem()).isEmpty()

                dao.insertFile(
                    ConvertedFileEntity(
                        uri = "content://1",
                        displayName = "file1.mp3",
                        timestampMillis = 100L,
                    ),
                )

                assertThat(awaitItem()).hasSize(1)
            }

            // Second subscriber should see current state
            filesFlow.test {
                val files = awaitItem()
                assertThat(files).hasSize(1)

                dao.insertFile(
                    ConvertedFileEntity(
                        uri = "content://2",
                        displayName = "file2.mp3",
                        timestampMillis = 200L,
                    ),
                )

                assertThat(awaitItem()).hasSize(2)
            }
        }
}
