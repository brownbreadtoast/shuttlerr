package com.example.shuttlerr.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.shuttlerr.data.db.dao.GameDao
import com.example.shuttlerr.data.db.dao.MatchDao
import com.example.shuttlerr.data.db.dao.PlayerDao
import com.example.shuttlerr.data.db.dao.RallyDao
import com.example.shuttlerr.data.db.entities.GameEntity
import com.example.shuttlerr.data.db.entities.MatchEntity
import com.example.shuttlerr.data.db.entities.PlayerEntity
import com.example.shuttlerr.data.db.entities.RallyEntity

@Database(
    entities = [MatchEntity::class, GameEntity::class, RallyEntity::class, PlayerEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class ShuttlerrDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao
    abstract fun gameDao(): GameDao
    abstract fun rallyDao(): RallyDao
    abstract fun playerDao(): PlayerDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE matches ADD COLUMN initialServerSlot TEXT NOT NULL DEFAULT 'ONE'")
                db.execSQL("ALTER TABLE matches ADD COLUMN teamAPlayer1 TEXT NOT NULL DEFAULT 'A1'")
                db.execSQL("ALTER TABLE matches ADD COLUMN teamAPlayer2 TEXT NOT NULL DEFAULT 'A2'")
                db.execSQL("ALTER TABLE matches ADD COLUMN teamBPlayer1 TEXT NOT NULL DEFAULT 'B1'")
                db.execSQL("ALTER TABLE matches ADD COLUMN teamBPlayer2 TEXT NOT NULL DEFAULT 'B2'")
                db.execSQL("ALTER TABLE rallies ADD COLUMN serverSlot TEXT NOT NULL DEFAULT 'ONE'")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS players (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL)")
            }
        }
    }
}
