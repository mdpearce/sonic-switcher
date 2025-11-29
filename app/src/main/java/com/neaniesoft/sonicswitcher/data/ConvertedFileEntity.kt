package com.neaniesoft.sonicswitcher.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a converted file in the queue.
 * The URI is stored as a string to persist across process death.
 */
@Entity(tableName = "converted_files")
data class ConvertedFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uri: String,
    val displayName: String,
    val timestampMillis: Long,
)
