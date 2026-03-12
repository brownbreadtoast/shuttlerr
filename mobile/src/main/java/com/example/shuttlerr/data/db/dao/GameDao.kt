package com.example.shuttlerr.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.shuttlerr.data.db.entities.GameEntity

@Dao
interface GameDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(game: GameEntity)

    @Query("SELECT * FROM games WHERE matchId = :matchId ORDER BY gameNumber")
    suspend fun getGamesForMatch(matchId: String): List<GameEntity>
}
