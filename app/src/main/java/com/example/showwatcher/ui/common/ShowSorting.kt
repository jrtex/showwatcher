package com.example.showwatcher.ui.common

import com.example.showwatcher.data.SortOrder

/**
 * Date sorts rank in three tiers, in this order: (1) shows with no next episode sort last — the
 * `nextEpisodeAirDate(it) == null` primary key forces this in both directions, since a plain
 * nullsLast()/nullsFirst() comparator would flip which end nulls land on depending on ascending
 * vs. descending; (2) among shows with a date, ones whose next unwatched episode hasn't aired yet
 * (date > [today]) sort after ones that have already aired — you can't watch an episode that
 * hasn't aired, so it shouldn't outrank one you're ready for. This tier uses `thenBy` (not
 * `thenByDescending`) in both branches deliberately: unaired-after-aired holds regardless of sort
 * direction; (3) finally, the date itself, ascending or descending per [order].
 */
fun <T> List<T>.sortedByShowOrder(
    order: SortOrder,
    today: String,
    title: (T) -> String,
    nextEpisodeAirDate: (T) -> String?,
): List<T> = when (order) {
    SortOrder.ALPHABETICAL_ASC -> sortedBy { title(it).lowercase() }
    SortOrder.ALPHABETICAL_DESC -> sortedByDescending { title(it).lowercase() }
    SortOrder.NEXT_EPISODE_ASC -> sortedWith(
        compareBy<T> { nextEpisodeAirDate(it) == null }
            .thenBy { nextEpisodeAirDate(it).let { date -> date != null && date > today } }
            .thenBy { nextEpisodeAirDate(it) },
    )
    SortOrder.NEXT_EPISODE_DESC -> sortedWith(
        compareBy<T> { nextEpisodeAirDate(it) == null }
            .thenBy { nextEpisodeAirDate(it).let { date -> date != null && date > today } }
            .thenByDescending { nextEpisodeAirDate(it) },
    )
}
