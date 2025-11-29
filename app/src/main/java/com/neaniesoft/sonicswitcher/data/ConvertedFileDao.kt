package com.neaniesoft.sonicswitcher.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConvertedFileDao {
    @Query("SELECT * FROM converted_files ORDER BY timestampMillis DESC")
    fun getAllFiles(): Flow<List<ConvertedFileEntity>>

    @Insert
    suspend fun insertFile(file: ConvertedFileEntity)

    @Query("DELETE FROM converted_files")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM converted_files")
    fun getCount(): Flow<Int>
}
