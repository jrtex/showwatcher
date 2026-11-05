package com.example.showwatcher.ui.common

import com.example.showwatcher.data.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private data class Item(val id: Int, val date: String?)

class ShowListFormattingTest {

    private fun List<Item>.render(prefix: String = "p"): List<String> =
        withMonthHeaders(prefix, itemKey = { it.id }, date = { it.date }).map {
            when (it) {
                is ShowListEntry.Header -> "[${it.label}]"
                is ShowListEntry.Row -> it.item.id.toString()
            }
        }

    @Test
    fun `inserts a header only when the month changes`() {
        val items = listOf(
            Item(1, "2026-11-03"),
            Item(2, "2026-11-01"),
            Item(3, "2026-10-20"),
            Item(4, "2025-10-05"),
        )
        assertEquals(
            listOf("[November 2026]", "1", "2", "[October 2026]", "3", "[October 2025]", "4"),
            items.render(),
        )
    }

    @Test
    fun `null and unparseable dates group under Date TBA`() {
        val items = listOf(Item(1, "2026-11-03"), Item(2, null), Item(3, "garbage"))
        assertEquals(listOf("[November 2026]", "1", "[Date TBA]", "2", "3"), items.render())
    }

    @Test
    fun `header keys include the prefix so sections sharing a list stay unique`() {
        val items = listOf(Item(1, "2026-11-03"))
        val a = items.withMonthHeaders("towatch", { it.id }, { it.date }).first().key
        val b = items.withMonthHeaders("caughtup", { it.id }, { it.date }).first().key
        assertEquals("towatch-month-2026-11", a)
        assertTrue(a != b)
    }

    @Test
    fun `empty list yields no entries`() {
        assertEquals(emptyList<String>(), emptyList<Item>().render())
    }

    @Test
    fun `episodeLine joins the known parts`() {
        assertEquals("S2E4 · Pilot · Nov 3, 2026", episodeLine(2, 4, "Pilot", "2026-11-03"))
        assertEquals("S2E4 · Date TBA", episodeLine(2, 4, null, null))
        assertEquals("Pilot · Nov 3, 2026", episodeLine(null, null, "Pilot", "2026-11-03"))
        assertEquals("Nothing", episodeLine(null, null, " ", null, fallback = "Nothing"))
    }

    @Test
    fun `only air-date sorts show month headers`() {
        assertTrue(SortOrder.NEXT_EPISODE_CLOSEST.isDateSort())
        assertTrue(SortOrder.NEXT_EPISODE_FURTHEST.isDateSort())
        assertFalse(SortOrder.ALPHABETICAL_ASC.isDateSort())
        assertFalse(SortOrder.ALPHABETICAL_DESC.isDateSort())
    }
}
