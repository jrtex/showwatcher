package com.example.showwatcher.ui.common

import com.example.showwatcher.data.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Test

private data class Show(val title: String, val nextEpisodeAirDate: String?)

private const val TODAY = "2026-07-25"

class ShowSortingTest {

    private val shows = listOf(
        Show("NoDate", null),
        Show("Unaired", "2026-08-01"),
        Show("AiredLate", "2026-07-20"),
        Show("AiredEarly", "2026-07-01"),
        Show("AiredToday", TODAY),
    )

    private fun List<Show>.sortByOrder(order: SortOrder): List<String> =
        sortedByShowOrder(order, today = TODAY, title = { it.title }, nextEpisodeAirDate = { it.nextEpisodeAirDate })
            .map { it.title }

    @Test
    fun `ascending sort puts aired shows before unaired shows before no-date shows`() {
        assertEquals(
            listOf("AiredEarly", "AiredLate", "AiredToday", "Unaired", "NoDate"),
            shows.sortByOrder(SortOrder.NEXT_EPISODE_ASC),
        )
    }

    @Test
    fun `descending sort still puts aired shows before unaired shows before no-date shows`() {
        assertEquals(
            listOf("AiredToday", "AiredLate", "AiredEarly", "Unaired", "NoDate"),
            shows.sortByOrder(SortOrder.NEXT_EPISODE_DESC),
        )
    }

    @Test
    fun `today counts as aired`() {
        val result = listOf(Show("Today", TODAY), Show("Tomorrow", "2026-07-26")).sortByOrder(SortOrder.NEXT_EPISODE_ASC)
        assertEquals(listOf("Today", "Tomorrow"), result)
    }

    @Test
    fun `alphabetical order is unaffected by air dates`() {
        assertEquals(
            listOf("AiredEarly", "AiredLate", "AiredToday", "NoDate", "Unaired"),
            shows.sortByOrder(SortOrder.ALPHABETICAL_ASC),
        )
    }
}
