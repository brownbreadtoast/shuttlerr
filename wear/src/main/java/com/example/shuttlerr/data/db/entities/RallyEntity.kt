package com.example.shuttlerr.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rallies",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("gameId")],
)
data class RallyEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val rallyNumber: Int,
    val winnerPlayer: String,
    val serverBeforeRally: String,
    val serverCourtSide: String,
    val serviceSide: String,
    val heartRateBpm: Int?,
    val durationMs: Long,
    val timestampMs: Long,
    val serverSlot: String = "ONE",
)
