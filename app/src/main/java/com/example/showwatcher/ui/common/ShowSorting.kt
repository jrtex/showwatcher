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
 * An active show is "caught up" when there's nothing watchable right now: no unwatched episode
 * left, the next one has no known air date, or it hasn't aired yet (date > [today]). Mirrors the
 * tier-1/tier-2 split in [sortedByShowOrder].
 */
fun isCaughtUp(nextEpisodeAirDate: String?, today: String): Boolean =
    nextEpisodeAirDate == null || nextEpisodeAirDate > today

/**
 * Date sorts rank in four tiers, in this order: (1) shows with no sort date sort last — the
 * `sortDate(it) == null` primary key forces this in both directions, since a plain
 * nullsLast()/nullsFirst() comparator would flip which end nulls land on depending on the
 * resolved direction; (2) among shows with a date, ones whose date hasn't happened yet
 * (date > [today]) sort after ones that have — you can't watch an episode that hasn't aired
 * (Active/Upcoming), and a "last watched episode" date (Archive) is never in the future in
 * practice, but the guard is harmless either way. This tier uses `thenBy` (not `thenByDescending`)
 * regardless of direction: unaired-after-aired holds either way; (3) the date itself, in the
 * direction [order] resolves to for [kind] (see [ShowListKind]); (4) finally, title (lowercase,
 * alphabetical) as a deterministic tie-break for shows sharing tiers 1-3 exactly — this also
 * guarantees the sort is stable and independent of whatever input order the caller's data source
 * happens to hand back, instead of e.g. shows with no date silently sorting by last-modified time.
 */
fun <T> List<T>.sortedByShowOrder(
    order: SortOrder,
    kind: ShowListKind,
    today: String,
    title: (T) -> String,
    sortDate: (T) -> String?,
): List<T> = when (order) {
    SortOrder.ALPHABETICAL_ASC -> sortedBy { title(it).lowercase() }
    SortOrder.ALPHABETICAL_DESC -> sortedByDescending { title(it).lowercase() }
    SortOrder.NEXT_EPISODE_CLOSEST, SortOrder.NEXT_EPISODE_FURTHEST -> {
        val closest = order == SortOrder.NEXT_EPISODE_CLOSEST
        val ascending = if (kind == ShowListKind.UPCOMING) closest else !closest
        val base = compareBy<T> { sortDate(it) == null }
            .thenBy { sortDate(it).let { date -> date != null && date > today } }
        sortedWith(
            if (ascending) base.thenBy { sortDate(it) }.thenBy { title(it).lowercase() }
            else base.thenByDescending { sortDate(it) }.thenBy { title(it).lowercase() },
        )
    }
}
