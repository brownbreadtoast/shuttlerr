package com.example.shuttlerr.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Match(
    val id: String,
    val format: GameFormat,
    val totalGames: Int,
    val startedAtMs: Long,
    val endedAtMs: Long? = null,
    val games: List<GameState> = emptyList(),
    val initialServerPlayer: Player,
    val initialCourtSideA: CourtSide,
    val synced: Boolean = false,
    // Doubles-specific: which slot (player 1 or 2) on the initial serving team serves first
    val initialServerSlot: DoublesSlot = DoublesSlot.ONE,
    val teamAPlayer1: String = "A1",
    val teamAPlayer2: String = "A2",
    val teamBPlayer1: String = "B1",
    val teamBPlayer2: String = "B2",
)
