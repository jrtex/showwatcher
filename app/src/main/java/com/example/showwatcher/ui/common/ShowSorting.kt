package com.example.showwatcher.ui.common

import com.example.showwatcher.data.SortOrder

/**
 * The `nextEpisodeAirDate(it) == null` primary key forces shows with no next episode to sort last
 * in both directions — a plain nullsLast()/nullsFirst() comparator would flip which end nulls land
 * on depending on ascending vs. descending, which isn't what we want here.
 */
fun <T> List<T>.sortedByShowOrder(
    order: SortOrder,
    title: (T) -> String,
    nextEpisodeAirDate: (T) -> String?,
): List<T> = when (order) {
    SortOrder.ALPHABETICAL_ASC -> sortedBy { title(it).lowercase() }
    SortOrder.ALPHABETICAL_DESC -> sortedByDescending { title(it).lowercase() }
    SortOrder.NEXT_EPISODE_ASC -> sortedWith(
        compareBy<T> { nextEpisodeAirDate(it) == null }.thenBy { nextEpisodeAirDate(it) },
    )
    SortOrder.NEXT_EPISODE_DESC -> sortedWith(
        compareBy<T> { nextEpisodeAirDate(it) == null }.thenByDescending { nextEpisodeAirDate(it) },
    )
}
