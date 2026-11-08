package com.arnav.sculpt.data

import android.content.Context
import com.arnav.sculpt.data.db.PhotoDao
import com.arnav.sculpt.data.db.PhotoEntity
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Photos: metadata in the database, image files in private storage. */
class PhotoRepository(
    private val context: Context,
    private val dao: PhotoDao
) {
    /** All photos, newest first. Updates live as photos are added or deleted. */
    val photos: Flow<List<PhotoEntity>> = dao.observeAll()

    /** Number of photos per pose id. */
    val countsByPose: Flow<Map<String, Int>> = photos.map { list ->
        list.mapNotNull { it.poseId }.groupingBy { it }.eachCount()
    }

    fun file(photo: PhotoEntity): File = File(PhotoFiles.dir(context), photo.fileName)

    /** A fresh file for the camera to write into. Call [add] once the image is saved. */
    fun newFile(): File = PhotoFiles.newFile(context)

    suspend fun add(file: File, poseId: String?) {
        dao.insert(PhotoEntity(fileName = file.name, takenAt = PhotoFiles.takenAt(file), poseId = poseId))
    }

    suspend fun delete(photos: List<PhotoEntity>) = withContext(Dispatchers.IO) {
        photos.forEach { runCatching { file(it).delete() } }
        dao.deleteByIds(photos.map { it.id })
    }

    /**
     * Makes the database match the photos folder: files without a row get one (with no pose),
     * and rows whose file is gone are removed.
     */
    suspend fun reconcile() = withContext(Dispatchers.IO) {
        val files = PhotoFiles.allFiles(context)
        val rows = dao.getAll()
        val known = rows.map { it.fileName }.toSet()
        val present = files.map { it.name }.toSet()

        files.filter { it.name !in known }.forEach { f ->
            dao.insert(
                PhotoEntity(fileName = f.name, takenAt = PhotoFiles.takenAt(f), poseId = null)
            )
        }

        val orphanIds = rows.filter { it.fileName !in present }.map { it.id }
        if (orphanIds.isNotEmpty()) dao.deleteByIds(orphanIds)
    }
}
