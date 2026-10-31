package com.arnav.sculpt.data

/**
 * The poses a user can shoot. [id] is stored in each photo's file name,
 * so never change an id once photos exist.
 */
enum class Pose(val id: String, val defaultLabel: String) {
    FRONT("front", "Front"),
    BACK("back", "Back"),
    LEFT("left", "Left"),
    RIGHT("right", "Right"),
    FLEXED("flexed", "Flexed"),
    UNFLEXED("unflexed", "Unflexed"),
    CUSTOM("custom", "Custom");

    /** Display name; the custom pose uses the name the user typed in Settings. */
    fun label(customName: String): String =
        if (this == CUSTOM && customName.isNotBlank()) customName.trim() else defaultLabel

    companion object {
        fun fromId(id: String?): Pose? = entries.firstOrNull { it.id == id }
    }
}
