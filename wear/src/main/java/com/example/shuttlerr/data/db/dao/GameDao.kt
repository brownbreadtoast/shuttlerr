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

    @Query("SELECT * FROM games WHERE matchId = :matchId ORDER BY gameNumber DESC LIMIT 1")
    suspend fun getLatestGameForMatch(matchId: String): GameEntity?

    @Query("UPDATE games SET scoreA = :scoreA, scoreB = :scoreB WHERE id = :gameId")
    suspend fun updateScore(gameId: String, scoreA: Int, scoreB: Int)

    @Query("UPDATE games SET winnerPlayer = :winner, endedAtMs = :endedAtMs WHERE id = :gameId")
    suspend fun markGameWon(gameId: String, winner: String, endedAtMs: Long)
}
