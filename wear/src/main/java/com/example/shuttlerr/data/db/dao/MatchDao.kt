package com.example.shuttlerr.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.shuttlerr.data.db.entities.MatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(match: MatchEntity)

    @Query("SELECT * FROM matches WHERE id = :matchId")
    suspend fun getMatch(matchId: String): MatchEntity?

    @Query("SELECT * FROM matches ORDER BY startedAtMs DESC")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE synced = 0")
    suspend fun getUnsyncedMatches(): List<MatchEntity>

    @Query("UPDATE matches SET synced = 1 WHERE id = :matchId")
    suspend fun markSynced(matchId: String)

    @Query("UPDATE matches SET endedAtMs = :endedAtMs WHERE id = :matchId")
    suspend fun updateEndTime(matchId: String, endedAtMs: Long)
}
