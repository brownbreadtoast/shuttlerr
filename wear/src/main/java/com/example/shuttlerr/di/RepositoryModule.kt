package com.example.shuttlerr.di

import com.example.shuttlerr.data.repository.MatchRepository
import com.example.shuttlerr.data.repository.MatchRepositoryImpl
import com.example.shuttlerr.domain.engine.BwfScoringEngine
import com.example.shuttlerr.domain.engine.BwfScoringEngineImpl
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
    abstract fun bindMatchRepository(impl: MatchRepositoryImpl): MatchRepository

    @Binds
    @Singleton
    abstract fun bindBwfScoringEngine(impl: BwfScoringEngineImpl): BwfScoringEngine
}
