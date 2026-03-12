package com.example.shuttlerr.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.shuttlerr.data.db.entities.RallyEntity

@Dao
interface RallyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rally: RallyEntity)

    @Query("SELECT * FROM rallies WHERE gameId = :gameId ORDER BY rallyNumber")
    suspend fun getRalliesForGame(gameId: String): List<RallyEntity>

    @Query(
        "DELETE FROM rallies WHERE id = " +
            "(SELECT id FROM rallies WHERE gameId = :gameId ORDER BY rallyNumber DESC LIMIT 1)"
    )
    suspend fun deleteLastRallyForGame(gameId: String)

    @Query("DELETE FROM rallies WHERE gameId = :gameId")
    suspend fun deleteAllRalliesForGame(gameId: String)

    @Query("SELECT COUNT(*) FROM rallies WHERE gameId = :gameId")
    suspend fun getRallyCount(gameId: String): Int
}
