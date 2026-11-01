package com.arnav.sculpt.data

import com.arnav.sculpt.data.db.PoseDao
import com.arnav.sculpt.data.db.PoseEntity
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Poses: the built-in ones plus any number of user-made custom poses. */
class PoseRepository(private val dao: PoseDao) {

    private val all: Flow<List<PoseEntity>> = dao.observeAll()

    /** Poses shown in the camera, in wheel order. Never empty once the app has started. */
    val enabledPoses: Flow<List<PoseEntity>> =
        all.map { list -> list.filter { it.enabled && !it.archived } }

    /** Every pose the user can manage in Settings (removed custom poses excluded). */
    val managedPoses: Flow<List<PoseEntity>> =
        all.map { list -> list.filter { !it.archived } }

    /** Pose id to name, including removed custom poses, for labelling old photos. */
    val namesById: Flow<Map<String, String>> =
        all.map { list -> list.associate { it.id to it.name } }

    suspend fun ensureBuiltIns() {
        dao.insertIgnore(BUILT_INS)
    }

    /**
     * Turns a pose on or off. Returns false (and changes nothing) if that would leave the
     * camera with no poses at all.
     */
    suspend fun setEnabled(id: String, enabled: Boolean): Boolean {
        val active = dao.getAll().filter { !it.archived }
        val target = active.firstOrNull { it.id == id } ?: return false
        if (!enabled && target.enabled && active.count { it.enabled } <= 1) return false
        dao.update(target.copy(enabled = enabled))
        return true
    }

    /** Adds a custom pose. Returns null if the name is empty or already used. */
    suspend fun addCustom(name: String): PoseEntity? {
        val clean = cleanName(name) ?: return null
        val active = dao.getAll().filter { !it.archived }
        if (active.any { it.name.equals(clean, ignoreCase = true) }) return null
        val pose = PoseEntity(
            id = "custom_" + UUID.randomUUID().toString().replace("-", "").take(8),
            name = clean,
            builtIn = false,
            enabled = true,
            archived = false,
            sortOrder = (dao.getAll().maxOfOrNull { it.sortOrder } ?: -1) + 1
        )
        dao.insert(pose)
        return pose
    }

    /** Renames a custom pose. Returns false if the name is invalid or already used. */
    suspend fun rename(id: String, name: String): Boolean {
        val clean = cleanName(name) ?: return false
        val active = dao.getAll().filter { !it.archived }
        val target = active.firstOrNull { it.id == id && !it.builtIn } ?: return false
        if (active.any { it.id != id && it.name.equals(clean, ignoreCase = true) }) return false
        dao.update(target.copy(name = clean))
        return true
    }

    /**
     * Removes a custom pose from the camera and Settings. Its photos stay in the gallery with
     * the pose's name. Returns false if it is the only pose left in the camera.
     */
    suspend fun removeCustom(id: String): Boolean {
        val active = dao.getAll().filter { !it.archived }
        val target = active.firstOrNull { it.id == id && !it.builtIn } ?: return false
        if (target.enabled && active.count { it.enabled } <= 1) return false
        dao.update(target.copy(enabled = false, archived = true))
        return true
    }

    private fun cleanName(name: String): String? =
        name.trim().take(MAX_NAME_LENGTH).takeIf { it.isNotEmpty() }

    companion object {
        const val MAX_NAME_LENGTH = 20

        val BUILT_INS = listOf(
            PoseEntity("front", "Front", builtIn = true, enabled = true, archived = false, sortOrder = 0),
            PoseEntity("front_bicep", "Front bicep", builtIn = true, enabled = true, archived = false, sortOrder = 1),
            PoseEntity("side", "Side", builtIn = true, enabled = true, archived = false, sortOrder = 2),
            PoseEntity("back", "Back", builtIn = true, enabled = true, archived = false, sortOrder = 3),
            PoseEntity("back_flexed", "Back flexed", builtIn = true, enabled = true, archived = false, sortOrder = 4)
        )
    }
}
