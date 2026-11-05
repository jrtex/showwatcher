package com.example.showwatcher.data

/** How show lists are displayed. [storageKey] is persisted independently of the enum name. */
enum class ViewMode(val storageKey: String, val label: String) {
    CARD("card", "Card view"),
    LIST("list", "List view"),
    ;

    companion object {
        val DEFAULT = CARD

        fun fromStorageKey(key: String?): ViewMode = entries.find { it.storageKey == key } ?: DEFAULT
    }
}
