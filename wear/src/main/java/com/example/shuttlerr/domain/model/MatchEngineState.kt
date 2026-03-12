package com.example.shuttlerr.domain.model

data class MatchEngineState(
    val match: Match,
    val currentGame: GameState,
    val currentServer: Player,
    /** Doubles: which slot on the serving team is currently serving */
    val currentServerSlot: DoublesSlot = DoublesSlot.ONE,
    val courtSideA: CourtSide,
    /** Doubles: which player slot is in the right service court for team A */
    val rightCourtSlotA: DoublesSlot = DoublesSlot.ONE,
    /** Doubles: which player slot is in the right service court for team B */
    val rightCourtSlotB: DoublesSlot = DoublesSlot.ONE,
    val hasMidGameSwitchHappened: Boolean = false,
    /** Initial server at the start of the current game — needed for undo replay */
    val gameInitialServer: Player,
    val gameInitialServerSlot: DoublesSlot = DoublesSlot.ONE,
    /** Court side for player A at the start of the current game — needed for undo replay */
    val gameInitialCourtSideA: CourtSide,
    val gameInitialRightCourtSlotA: DoublesSlot = DoublesSlot.ONE,
    val gameInitialRightCourtSlotB: DoublesSlot = DoublesSlot.ONE,
)
