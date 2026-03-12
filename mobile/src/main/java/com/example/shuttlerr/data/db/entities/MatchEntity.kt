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
)
