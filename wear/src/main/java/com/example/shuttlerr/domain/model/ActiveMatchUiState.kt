package com.example.shuttlerr.domain.model

data class PlayerNames(
    val a1: String = "A1",
    val a2: String = "A2",
    val b1: String = "B1",
    val b2: String = "B2",
)

data class ActiveMatchUiState(
    val scoreA: Int = 0,
    val scoreB: Int = 0,
    val currentGameNumber: Int = 1,
    val totalGames: Int = 3,
    val currentServer: Player = Player.A,
    val serverCourtSide: CourtSide = CourtSide.RIGHT,
    val serviceSide: ServiceSide = ServiceSide.EVEN,
    val heartRateBpm: Int? = null,
    val isDeuce: Boolean = false,
    val gameWinner: Player? = null,
    val matchWinner: Player? = null,
    val canUndo: Boolean = false,
    val showMidGameSwitchPrompt: Boolean = false,
    // Doubles-specific
    val isDoubles: Boolean = false,
    val currentServerSlot: DoublesSlot = DoublesSlot.ONE,
    val currentReceiverSlot: DoublesSlot = DoublesSlot.ONE,
    val rightCourtSlotA: DoublesSlot = DoublesSlot.ONE,
    val rightCourtSlotB: DoublesSlot = DoublesSlot.ONE,
    val playerNames: PlayerNames = PlayerNames(),
)
