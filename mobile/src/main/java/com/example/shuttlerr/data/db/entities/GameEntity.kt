package com.example.shuttlerr.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "games",
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["matchId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("matchId")],
)
data class GameEntity(
    @PrimaryKey val id: String,
    val matchId: String,
    val gameNumber: Int,
    val scoreA: Int,
    val scoreB: Int,
    val winnerPlayer: String?,
    val startedAtMs: Long,
    val endedAtMs: Long?,
)
