package com.example.shuttlerr.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String,
    val format: String,
    val totalGames: Int,
    val startedAtMs: Long,
    val endedAtMs: Long?,
    val initialServerPlayer: String,
    val initialCourtSideA: String,
    val synced: Boolean = false,
    val initialServerSlot: String = "ONE",
    val teamAPlayer1: String = "A1",
    val teamAPlayer2: String = "A2",
    val teamBPlayer1: String = "B1",
    val teamBPlayer2: String = "B2",
)
