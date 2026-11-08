package com.arnav.sculpt.data

import java.io.File
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoFilesTest {

    @Test
    fun takenAt_isReadFromTheFileName() {
        val millis = PhotoFiles.takenAt(File("sculpt_20260107_153012_123.jpg"))
        val cal = Calendar.getInstance().apply { timeInMillis = millis }

        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH))
        assertEquals(7, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(15, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
        assertEquals(12, cal.get(Calendar.SECOND))
        assertEquals(123, cal.get(Calendar.MILLISECOND))
    }

    @Test
    fun takenAt_fallsBackToFileTimeWhenTheNameHasNoStamp() {
        // A file that does not exist reports lastModified() == 0.
        assertEquals(0L, PhotoFiles.takenAt(File("holiday.jpg")))
    }
}

class PoseRepositoryTest {

    @Test
    fun builtInPoses_haveUniqueIdsAndNames() {
        val poses = PoseRepository.BUILT_INS
        assertEquals(poses.size, poses.map { it.id }.toSet().size)
        assertEquals(poses.size, poses.map { it.name.lowercase() }.toSet().size)
    }

    @Test
    fun builtInPoses_areOrderedAndAllStartEnabled() {
        val poses = PoseRepository.BUILT_INS
        assertEquals(poses.indices.toList(), poses.map { it.sortOrder })
        assertTrue(poses.all { it.builtIn && it.enabled && !it.archived })
    }

    @Test
    fun builtInPoseNames_fitTheCustomNameLimit() {
        assertTrue(PoseRepository.BUILT_INS.all { it.name.length <= PoseRepository.MAX_NAME_LENGTH })
    }
}
