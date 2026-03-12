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
)
