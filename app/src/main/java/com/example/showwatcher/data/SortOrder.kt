package com.example.showwatcher.data

/** [storageKey] is persisted independently of the enum name so a rename can't corrupt saved data. */
enum class SortOrder(val storageKey: String, val label: String) {
    ALPHABETICAL_ASC("alphabetical_asc", "Alphabetical (A–Z)"),
    ALPHABETICAL_DESC("alphabetical_desc", "Alphabetical (Z–A)"),
    NEXT_EPISODE_ASC("next_episode_asc", "Next episode date (soonest first)"),
    NEXT_EPISODE_DESC("next_episode_desc", "Next episode date (latest first)"),
    ;

    companion object {
        val DEFAULT = ALPHABETICAL_ASC

        fun fromStorageKey(key: String?): SortOrder = entries.find { it.storageKey == key } ?: DEFAULT
    }
}
