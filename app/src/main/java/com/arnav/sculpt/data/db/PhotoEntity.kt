package com.arnav.sculpt.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One progress photo. The image itself stays in the app's private photos folder; this row
 * holds the metadata. [poseId] is null for photos that have no pose.
 */
@Entity(
    tableName = "photos",
    indices = [
        Index(value = ["fileName"], unique = true),
        Index(value = ["takenAt"]),
        Index(value = ["poseId"])
    ]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val takenAt: Long,
    val poseId: String?
)
