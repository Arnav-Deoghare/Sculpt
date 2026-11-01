package com.arnav.sculpt.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PhotoEntity::class, PoseEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SculptDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
    abstract fun poseDao(): PoseDao

    companion object {
        @Volatile
        private var instance: SculptDatabase? = null

        fun get(context: Context): SculptDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SculptDatabase::class.java,
                    "sculpt.db"
                ).build().also { instance = it }
            }
    }
}
