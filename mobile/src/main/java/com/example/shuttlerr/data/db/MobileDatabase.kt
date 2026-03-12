package com.example.shuttlerr.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.shuttlerr.data.db.dao.GameDao
import com.example.shuttlerr.data.db.dao.MatchDao
import com.example.shuttlerr.data.db.dao.RallyDao
import com.example.shuttlerr.data.db.entities.GameEntity
import com.example.shuttlerr.data.db.entities.MatchEntity
import com.example.shuttlerr.data.db.entities.RallyEntity

@Database(
    entities = [MatchEntity::class, GameEntity::class, RallyEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MobileDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao
    abstract fun gameDao(): GameDao
    abstract fun rallyDao(): RallyDao
}
