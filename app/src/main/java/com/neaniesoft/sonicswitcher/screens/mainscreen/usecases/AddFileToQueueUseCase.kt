package com.neaniesoft.sonicswitcher.screens.mainscreen.usecases

import android.net.Uri
import com.neaniesoft.sonicswitcher.data.ConvertedFileRepository
import java.time.Clock
import javax.inject.Inject

/**
 * Adds a converted file to the queue with current timestamp.
 */
class AddFileToQueueUseCase
    @Inject
    constructor(
        private val repository: ConvertedFileRepository,
        private val getFileDisplayName: GetFileDisplayNameUseCase,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(uri: Uri) {
            val displayName = getFileDisplayName(uri)
            val timestamp = clock.millis()
            repository.addFile(uri, displayName, timestamp)
        }
    }
