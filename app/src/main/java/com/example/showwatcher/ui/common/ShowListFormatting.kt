package com.example.showwatcher.ui.common

import com.example.showwatcher.data.SortOrder
import java.text.SimpleDateFormat
import java.util.Locale

/** An entry in a list-view show list: either a month divider or a show row. */
sealed interface ShowListEntry<out T> {
    val key: Any

    data class Header(override val key: String, val label: String) : ShowListEntry<Nothing>

    data class Row<T>(override val key: Any, val item: T) : ShowListEntry<T>
}

/** Month dividers only make sense when the list is ordered by date. */
fun SortOrder.isDateSort(): Boolean =
    this == SortOrder.NEXT_EPISODE_CLOSEST || this == SortOrder.NEXT_EPISODE_FURTHEST

private const val NO_DATE_LABEL = "Date TBA"

// SimpleDateFormat isn't thread-safe, so build one per call rather than sharing instances.
private fun isoParser() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

/** "2026-11-03" → "Nov 3, 2026"; returns the input unchanged if it can't be parsed. */
fun formatAirDate(isoDate: String): String {
    val parsed = runCatching { isoParser().parse(isoDate) }.getOrNull() ?: return isoDate
    return SimpleDateFormat("MMM d, yyyy", Locale.US).format(parsed)
}

/** "2026-11-03" → "November 2026"; null or unparseable → [NO_DATE_LABEL]. */
fun formatMonthHeader(isoDate: String?): String {
    if (isoDate == null) return NO_DATE_LABEL
    val parsed = runCatching { isoParser().parse(isoDate) }.getOrNull() ?: return NO_DATE_LABEL
    return SimpleDateFormat("MMMM yyyy", Locale.US).format(parsed)
}

/** "yyyy-MM" bucket for an ISO date, or null when there's no usable date. */
private fun monthBucket(isoDate: String?): String? =
    isoDate?.takeIf { it.length >= 7 && it[4] == '-' }?.substring(0, 7)

/**
 * Wraps each item in a [ShowListEntry.Row], inserting a [ShowListEntry.Header] whenever the
 * month of [date] changes from the previous item. Assumes the list is already sorted by date.
 * [keyPrefix] keeps header keys unique when several sections share one LazyColumn.
 */
fun <T> List<T>.withMonthHeaders(
    keyPrefix: String,
    itemKey: (T) -> Any,
    date: (T) -> String?,
): List<ShowListEntry<T>> {
    val result = ArrayList<ShowListEntry<T>>(size + 4)
    var first = true
    var previous: String? = null
    for (item in this) {
        val itemDate = date(item)
        val bucket = monthBucket(itemDate)
        if (first || bucket != previous) {
            result += ShowListEntry.Header(
                key = "$keyPrefix-month-${bucket ?: "none"}",
                label = formatMonthHeader(itemDate.takeIf { bucket != null }),
            )
        }
        result += ShowListEntry.Row(itemKey(item), item)
        first = false
        previous = bucket
    }
    return result
}

/** Rows only, no headers. */
fun <T> List<T>.asListEntries(itemKey: (T) -> Any): List<ShowListEntry<T>> =
    map { ShowListEntry.Row(itemKey(it), it) }

/**
 * "S2E4 · Episode name · Nov 3, 2026", omitting any missing part. Falls back to
 * [fallback] when nothing is known.
 */
fun episodeLine(
    season: Int?,
    episode: Int?,
    name: String?,
    airDate: String?,
    fallback: String = "No episode info",
): String {
    if (season == null && episode == null && name.isNullOrBlank() && airDate == null) return fallback
    return buildList {
        if (season != null && episode != null) add("S${season}E$episode")
        if (!name.isNullOrBlank()) add(name)
        add(airDate?.let(::formatAirDate) ?: NO_DATE_LABEL)
    }.joinToString(" · ")
}
