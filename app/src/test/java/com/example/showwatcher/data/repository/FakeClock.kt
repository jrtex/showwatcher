package com.example.showwatcher.data.repository

import com.example.showwatcher.data.Clock

class FakeClock(var time: Long = 0L) : Clock {
    override fun nowMillis(): Long = time
}
