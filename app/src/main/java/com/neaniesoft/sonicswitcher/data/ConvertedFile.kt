package com.neaniesoft.sonicswitcher.data

import android.net.Uri

/**
 * Domain model for a converted file in the queue.
 */
data class ConvertedFile(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val timestampMillis: Long,
)

/**
 * Converts entity to domain model.
 */
fun ConvertedFileEntity.toDomainModel() =
    ConvertedFile(
        id = id,
        uri = Uri.parse(uri),
        displayName = displayName,
        timestampMillis = timestampMillis,
    )
