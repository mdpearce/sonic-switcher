package io.github.mdpearce.sonicswitcher.screens.mainscreen.usecases

import io.github.mdpearce.sonicswitcher.data.ConvertedFileRepository
import javax.inject.Inject

/**
 * Clears all files from the queue.
 */
class ClearQueueUseCase
    @Inject
    constructor(
        private val repository: ConvertedFileRepository,
    ) {
        suspend operator fun invoke() {
            repository.clearAll()
        }
    }
