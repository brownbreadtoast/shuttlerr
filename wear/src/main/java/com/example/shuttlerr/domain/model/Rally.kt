package com.example.shuttlerr.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Rally(
    val id: String,
    val gameId: String,
    val rallyNumber: Int,
    val winnerPlayer: Player,
    val serverBeforeRally: Player,
    val serverCourtSide: CourtSide,
    val serviceSide: ServiceSide,
    val heartRateBpm: Int? = null,
    val durationMs: Long,
    val timestampMs: Long,
    val serverSlot: DoublesSlot = DoublesSlot.ONE,  // doubles: which slot on the serving team was serving
)
