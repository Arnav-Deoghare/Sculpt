package com.arnav.sculpt.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * File handling for photos. Images live in the app's private internal storage (filesDir),
 * so they never show up in the system gallery and other apps cannot read them.
 *
 * File names are sculpt_yyyyMMdd_HHmmss_SSS.jpg.
 */
object PhotoFiles {
    private const val DIR_NAME = "photos"
    private const val NAME_PATTERN = "yyyyMMdd_HHmmss_SSS"
    private val TIME_REGEX = Regex("sculpt_(\\d{8}_\\d{6}_\\d{3})")

    fun dir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { mkdirs() }

    fun newFile(context: Context): File {
        val stamp = SimpleDateFormat(NAME_PATTERN, Locale.US).format(Date())
        return File(dir(context), "sculpt_$stamp.jpg")
    }

    fun allFiles(context: Context): List<File> =
        dir(context)
            .listFiles { f -> f.extension.equals("jpg", ignoreCase = true) }
            .orEmpty()
            .toList()

    /** The moment the photo was taken, read from its file name (falls back to file time). */
    fun takenAt(file: File): Long {
        val stamp = TIME_REGEX.find(file.name)?.groupValues?.get(1)
        val parsed = stamp?.let {
            runCatching { SimpleDateFormat(NAME_PATTERN, Locale.US).parse(it)?.time }.getOrNull()
        }
        return parsed ?: file.lastModified()
    }
}
