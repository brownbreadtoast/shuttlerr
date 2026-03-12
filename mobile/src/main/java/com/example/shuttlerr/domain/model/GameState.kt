package com.example.shuttlerr.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val id: String,
    val matchId: String,
    val gameNumber: Int,
    val scoreA: Int = 0,
    val scoreB: Int = 0,
    val winnerPlayer: Player? = null,
    val startedAtMs: Long,
    val endedAtMs: Long? = null,
    val rallies: List<Rally> = emptyList(),
)
