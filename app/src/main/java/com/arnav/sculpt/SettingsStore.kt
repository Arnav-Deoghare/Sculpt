package com.arnav.sculpt

import android.content.Context

/** Simple persisted app settings. Photos are never touched by this. */
object SettingsStore {
    private const val PREFS = "sculpt_settings"
    private const val KEY_DARK = "dark_mode"
    private const val KEY_POSES = "enabled_poses"
    private const val KEY_CUSTOM_NAME = "custom_pose_name"
    private const val KEY_SELECTED_POSE = "selected_pose"
    private const val KEY_GHOST_ON = "ghost_on"
    private const val KEY_GHOST_ALPHA = "ghost_alpha"
    private const val KEY_TIMER = "timer_seconds"

    private val DEFAULT_POSES = listOf(Pose.FRONT, Pose.BACK, Pose.LEFT, Pose.RIGHT)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- Appearance -------------------------------------------------------

    /** Dark is the default. */
    fun isDark(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DARK, true)

    fun setDark(context: Context, dark: Boolean) {
        prefs(context).edit().putBoolean(KEY_DARK, dark).apply()
    }

    // ---- Poses ------------------------------------------------------------

    /** Poses shown in the camera, always in the fixed Pose order, never empty. */
    fun enabledPoses(context: Context): List<Pose> {
        val saved = prefs(context).getString(KEY_POSES, null) ?: return DEFAULT_POSES
        val ids = saved.split(",").filter { it.isNotBlank() }.toSet()
        return Pose.entries.filter { it.id in ids }.ifEmpty { DEFAULT_POSES }
    }

    fun setEnabledPoses(context: Context, poses: List<Pose>) {
        prefs(context).edit()
            .putString(KEY_POSES, poses.joinToString(",") { it.id })
            .apply()
    }

    fun customPoseName(context: Context): String =
        prefs(context).getString(KEY_CUSTOM_NAME, "") ?: ""

    fun setCustomPoseName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_CUSTOM_NAME, name).apply()
    }

    fun selectedPoseId(context: Context): String? =
        prefs(context).getString(KEY_SELECTED_POSE, null)

    fun setSelectedPoseId(context: Context, id: String) {
        prefs(context).edit().putString(KEY_SELECTED_POSE, id).apply()
    }

    // ---- Camera helpers ---------------------------------------------------

    fun ghostOn(context: Context): Boolean =
        prefs(context).getBoolean(KEY_GHOST_ON, true)

    fun setGhostOn(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean(KEY_GHOST_ON, on).apply()
    }

    fun ghostAlpha(context: Context): Float =
        prefs(context).getFloat(KEY_GHOST_ALPHA, 0.35f)

    fun setGhostAlpha(context: Context, alpha: Float) {
        prefs(context).edit().putFloat(KEY_GHOST_ALPHA, alpha).apply()
    }

    /** 0 means the timer is off. */
    fun timerSeconds(context: Context): Int =
        prefs(context).getInt(KEY_TIMER, 0)

    fun setTimerSeconds(context: Context, seconds: Int) {
        prefs(context).edit().putInt(KEY_TIMER, seconds).apply()
    }
}
