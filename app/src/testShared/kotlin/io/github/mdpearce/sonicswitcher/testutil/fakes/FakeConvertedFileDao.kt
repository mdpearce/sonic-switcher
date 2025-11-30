package io.github.mdpearce.sonicswitcher.testutil.fakes

import io.github.mdpearce.sonicswitcher.data.ConvertedFileDao
import io.github.mdpearce.sonicswitcher.data.ConvertedFileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake implementation of [ConvertedFileDao] for testing.
 * Uses an in-memory list to simulate database operations.
 */
class FakeConvertedFileDao : ConvertedFileDao {
    private val files = mutableListOf<ConvertedFileEntity>()
    private val filesFlow = MutableStateFlow(files.toList())

    override fun getAllFiles(): Flow<List<ConvertedFileEntity>> = filesFlow

    override suspend fun insertFile(file: ConvertedFileEntity) {
        val newFile =
            if (file.id == 0L) {
                file.copy(id = (files.maxOfOrNull { it.id } ?: 0) + 1)
            } else {
                file
            }
        files.add(newFile)
        filesFlow.value = files.sortedByDescending { it.timestampMillis }
    }

    override suspend fun clearAll() {
        files.clear()
        filesFlow.value = emptyList()
    }

    override fun getCount(): Flow<Int> = filesFlow.map { it.size }

    /**
     * Test-only method to get current snapshot of files for assertions.
     */
    fun getAllFilesSnapshot(): List<ConvertedFileEntity> = files.toList()

    /**
     * Test-only method to reset the DAO state.
     */
    fun reset() {
        files.clear()
        filesFlow.value = emptyList()
    }
}
