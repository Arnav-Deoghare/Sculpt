package com.arnav.sculpt.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A pose the user can shoot. Built-in poses have fixed ids ("front", "side", ...); custom
 * poses get "custom_<random>" ids. A removed custom pose is archived, not deleted, so photos
 * taken in it keep their name.
 */
@Entity(tableName = "poses")
data class PoseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val builtIn: Boolean,
    val enabled: Boolean,
    val archived: Boolean,
    val sortOrder: Int
)
