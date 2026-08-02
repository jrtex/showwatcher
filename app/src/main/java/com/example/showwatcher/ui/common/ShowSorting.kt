package com.example.showwatcher.ui.common

import com.example.showwatcher.data.SortOrder

/**
 * Which direction "closest" resolves to depends on the screen: Active/Archive shows are
 * predominantly in the past (next unwatched episode already aired, or ready to watch), so
 * "closest" there means most-recent-first (descending). Upcoming shows are always in the future,
 * so "closest" there means soonest-first (ascending).
 */
enum class ShowListKind { ACTIVE_OR_ARCHIVE, UPCOMING }

/**
 * Date sorts rank in three tiers, in this order: (1) shows with no next episode sort last — the
 * `nextEpisodeAirDate(it) == null` primary key forces this in both directions, since a plain
 * nullsLast()/nullsFirst() comparator would flip which end nulls land on depending on the
 * resolved direction; (2) among shows with a date, ones whose next unwatched episode hasn't aired
 * yet (date > [today]) sort after ones that have already aired — you can't watch an episode that
 * hasn't aired, so it shouldn't outrank one you're ready for. This tier uses `thenBy` (not
 * `thenByDescending`) regardless of direction: unaired-after-aired holds either way; (3) finally,
 * the date itself, in the direction [order] resolves to for [kind] (see [ShowListKind]).
 */
fun <T> List<T>.sortedByShowOrder(
    order: SortOrder,
    kind: ShowListKind,
    today: String,
    title: (T) -> String,
    nextEpisodeAirDate: (T) -> String?,
): List<T> = when (order) {
    SortOrder.ALPHABETICAL_ASC -> sortedBy { title(it).lowercase() }
    SortOrder.ALPHABETICAL_DESC -> sortedByDescending { title(it).lowercase() }
    SortOrder.NEXT_EPISODE_CLOSEST, SortOrder.NEXT_EPISODE_FURTHEST -> {
        val closest = order == SortOrder.NEXT_EPISODE_CLOSEST
        val ascending = if (kind == ShowListKind.UPCOMING) closest else !closest
        val base = compareBy<T> { nextEpisodeAirDate(it) == null }
            .thenBy { nextEpisodeAirDate(it).let { date -> date != null && date > today } }
        sortedWith(if (ascending) base.thenBy { nextEpisodeAirDate(it) } else base.thenByDescending { nextEpisodeAirDate(it) })
    }
}
