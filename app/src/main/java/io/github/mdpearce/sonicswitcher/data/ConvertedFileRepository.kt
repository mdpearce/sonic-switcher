package io.github.mdpearce.sonicswitcher.data

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing the queue of converted files.
 * Provides a clean API for UI layer to interact with persisted queue data.
 */
@Singleton
class ConvertedFileRepository
    @Inject
    constructor(
        private val dao: ConvertedFileDao,
    ) {
        /**
         * Flow of all converted files in the queue, ordered by newest first.
         */
        fun getAllFiles(): Flow<List<ConvertedFile>> =
            dao.getAllFiles().map {
                    entities ->
                entities.map { it.toDomainModel() }
            }

        /**
         * Flow of the count of files in the queue.
         */
        fun getFileCount(): Flow<Int> = dao.getCount()

        /**
         * Add a file to the queue.
         */
        suspend fun addFile(
            uri: Uri,
            displayName: String,
            timestampMillis: Long,
        ) {
            dao.insertFile(
                ConvertedFileEntity(
                    uri = uri.toString(),
                    displayName = displayName,
                    timestampMillis = timestampMillis,
                ),
            )
        }

        /**
         * Clear all files from the queue.
         */
        suspend fun clearAll() {
            dao.clearAll()
        }
    }
