package com.example.showwatcher.data.local

import androidx.room.withTransaction
import javax.inject.Inject

/** Indirection over Room's transaction boundary so repository tests can fake it without a real DB. */
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

class RoomTransactionRunner @Inject constructor(
    private val database: ShowWatcherDatabase,
) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = database.withTransaction { block() }
}
