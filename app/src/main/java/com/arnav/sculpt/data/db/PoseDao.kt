package com.arnav.sculpt.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PoseDao {
    @Query("SELECT * FROM poses ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<PoseEntity>>

    @Query("SELECT * FROM poses ORDER BY sortOrder ASC")
    suspend fun getAll(): List<PoseEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(poses: List<PoseEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(pose: PoseEntity)

    @Update
    suspend fun update(pose: PoseEntity)
}
