package com.example.showwatcher.data

import javax.inject.Inject

/** Indirection over "now" so the §4.4 staleness math can be driven deterministically in tests. */
interface Clock {
    fun nowMillis(): Long
}

class SystemClock @Inject constructor() : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
