package com.example.showwatcher.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/** Indirection over "now" so the §4.4 staleness math can be driven deterministically in tests. */
interface Clock {
    fun nowMillis(): Long
}

class SystemClock @Inject constructor() : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

/** UTC "YYYY-MM-DD", matching the format TMDB's `air_date` fields use, for lexicographic comparison. */
fun Clock.today(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(nowMillis()))
