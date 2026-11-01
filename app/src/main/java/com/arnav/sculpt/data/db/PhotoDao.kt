package com.arnav.sculpt.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY takenAt DESC")
    fun observeAll(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos")
    suspend fun getAll(): List<PhotoEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(photo: PhotoEntity): Long

    @Query("DELETE FROM photos WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
