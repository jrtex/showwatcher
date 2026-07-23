package com.example.showwatcher.di

import com.example.showwatcher.data.Clock
import com.example.showwatcher.data.SettingsRepository
import com.example.showwatcher.data.SettingsRepositoryImpl
import com.example.showwatcher.data.SystemClock
import com.example.showwatcher.data.local.RoomTransactionRunner
import com.example.showwatcher.data.local.TransactionRunner
import com.example.showwatcher.data.remote.TmdbRemoteDataSource
import com.example.showwatcher.data.remote.TmdbRemoteDataSourceImpl
import com.example.showwatcher.data.repository.ShowRepository
import com.example.showwatcher.data.repository.ShowRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindShowRepository(impl: ShowRepositoryImpl): ShowRepository

    @Binds
    @Singleton
    abstract fun bindTmdbRemoteDataSource(impl: TmdbRemoteDataSourceImpl): TmdbRemoteDataSource

    @Binds
    abstract fun bindClock(impl: SystemClock): Clock

    @Binds
    abstract fun bindTransactionRunner(impl: RoomTransactionRunner): TransactionRunner

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
