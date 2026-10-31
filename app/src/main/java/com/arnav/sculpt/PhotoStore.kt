package com.arnav.sculpt

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A saved photo, the moment it was taken (epoch millis) and its pose id (null if untagged). */
data class PhotoItem(val file: File, val takenAt: Long, val poseId: String?)

/**
 * Photos live in the app's private internal storage (filesDir), so they never
 * show up in the system gallery and other apps cannot read them.
 *
 * File name format: sculpt_yyyyMMdd_HHmmss_SSS_<poseId>.jpg
 * (older photos without the pose suffix are treated as untagged).
 */
object PhotoStore {
    private const val DIR_NAME = "photos"
    private const val NAME_PATTERN = "yyyyMMdd_HHmmss_SSS"
    private val TIME_REGEX = Regex("sculpt_(\\d{8}_\\d{6}_\\d{3})")
    private val POSE_REGEX = Regex("sculpt_\\d{8}_\\d{6}_\\d{3}_([a-z]+)\\.jpg")

    fun dir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { mkdirs() }

    fun newFile(context: Context, poseId: String? = null): File {
        val stamp = SimpleDateFormat(NAME_PATTERN, Locale.US).format(Date())
        val suffix = if (poseId.isNullOrBlank()) "" else "_$poseId"
        return File(dir(context), "sculpt_$stamp$suffix.jpg")
    }

    /** The moment the photo was taken, read from its file name (falls back to file time). */
    fun takenAt(file: File): Long {
        val stamp = TIME_REGEX.find(file.name)?.groupValues?.get(1)
        val parsed = stamp?.let {
            runCatching { SimpleDateFormat(NAME_PATTERN, Locale.US).parse(it)?.time }.getOrNull()
        }
        return parsed ?: file.lastModified()
    }

    fun poseIdOf(file: File): String? = POSE_REGEX.find(file.name)?.groupValues?.get(1)

    /** All photos, newest first. */
    fun all(context: Context): List<PhotoItem> =
        dir(context)
            .listFiles { f -> f.extension.equals("jpg", ignoreCase = true) }
            .orEmpty()
            .map { PhotoItem(it, takenAt(it), poseIdOf(it)) }
            .sortedByDescending { it.takenAt }

    fun latest(context: Context): File? = all(context).firstOrNull()?.file

    /** Most recent photo of the given pose, or null if there is none. */
    fun latestFor(context: Context, poseId: String): File? =
        all(context).firstOrNull { it.poseId == poseId }?.file

    fun delete(files: List<File>) {
        files.forEach { runCatching { it.delete() } }
    }
}
