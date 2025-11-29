package io.github.mdpearce.sonicswitcher.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ConvertedFileEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SonicSwitcherDatabase : RoomDatabase() {
    abstract fun convertedFileDao(): ConvertedFileDao
}
