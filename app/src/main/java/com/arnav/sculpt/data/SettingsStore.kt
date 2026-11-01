package com.arnav.sculpt.data

import android.content.Context

/** Simple persisted app settings. Photos and poses live in the database, not here. */
object SettingsStore {
    private const val PREFS = "sculpt_settings"
    private const val KEY_DARK = "dark_mode"
    private const val KEY_SELECTED_POSE = "selected_pose"
    private const val KEY_GHOST_ON = "ghost_on"
    private const val KEY_GHOST_ALPHA = "ghost_alpha"
    private const val KEY_TIMER = "timer_seconds"
    private const val KEY_FLASH = "flash_mode"

    // Only read by the one-time migration from the pre-database version.
    private const val KEY_LEGACY_CUSTOM_NAME = "custom_pose_name"
    private const val KEY_MIGRATED = "db_migrated"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- Appearance -------------------------------------------------------

    /** Dark is the default. */
    fun isDark(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DARK, true)

    fun setDark(context: Context, dark: Boolean) {
        prefs(context).edit().putBoolean(KEY_DARK, dark).apply()
    }

    // ---- Camera -----------------------------------------------------------

    fun selectedPoseId(context: Context): String? =
        prefs(context).getString(KEY_SELECTED_POSE, null)

    fun setSelectedPoseId(context: Context, id: String) {
        prefs(context).edit().putString(KEY_SELECTED_POSE, id).apply()
    }

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

    /** One of ImageCapture.FLASH_MODE_OFF / AUTO / ON. Defaults to off (value 2). */
    fun flashMode(context: Context): Int =
        prefs(context).getInt(KEY_FLASH, 2)

    fun setFlashMode(context: Context, mode: Int) {
        prefs(context).edit().putInt(KEY_FLASH, mode).apply()
    }

    // ---- One-time migration -----------------------------------------------

    fun legacyCustomName(context: Context): String =
        prefs(context).getString(KEY_LEGACY_CUSTOM_NAME, "") ?: ""

    fun isMigrated(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MIGRATED, false)

    fun setMigrated(context: Context) {
        prefs(context).edit().putBoolean(KEY_MIGRATED, true).apply()
    }
}
