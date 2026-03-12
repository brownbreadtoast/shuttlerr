package com.example.shuttlerr.di

import android.content.Context
import androidx.room.Room
import com.example.shuttlerr.data.db.MobileDatabase
import com.example.shuttlerr.data.db.dao.GameDao
import com.example.shuttlerr.data.db.dao.MatchDao
import com.example.shuttlerr.data.db.dao.RallyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MobileDatabase =
        Room.databaseBuilder(context, MobileDatabase::class.java, "shuttlerr_mobile.db").build()

    @Provides
    fun provideMatchDao(db: MobileDatabase): MatchDao = db.matchDao()

    @Provides
    fun provideGameDao(db: MobileDatabase): GameDao = db.gameDao()

    @Provides
    fun provideRallyDao(db: MobileDatabase): RallyDao = db.rallyDao()
}
