package com.example.shuttlerr.di

import android.content.Context
import androidx.room.Room
import com.example.shuttlerr.data.db.ShuttlerrDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): ShuttlerrDatabase =
        Room.databaseBuilder(context, ShuttlerrDatabase::class.java, "shuttlerr.db")
            .addMigrations(ShuttlerrDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideMatchDao(db: ShuttlerrDatabase): MatchDao = db.matchDao()

    @Provides
    fun provideGameDao(db: ShuttlerrDatabase): GameDao = db.gameDao()

    @Provides
    fun provideRallyDao(db: ShuttlerrDatabase): RallyDao = db.rallyDao()
}
