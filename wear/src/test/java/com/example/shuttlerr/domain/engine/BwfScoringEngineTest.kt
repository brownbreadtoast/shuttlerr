package com.example.shuttlerr.domain.engine

import com.example.shuttlerr.domain.model.CourtSide
import com.example.shuttlerr.domain.model.GameFormat
import com.example.shuttlerr.domain.model.GameState
import com.example.shuttlerr.domain.model.Match
import com.example.shuttlerr.domain.model.MatchEngineState
import com.example.shuttlerr.domain.model.Player
import com.example.shuttlerr.domain.model.ServiceSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BwfScoringEngineTest {

    private lateinit var engine: BwfScoringEngineImpl
    private lateinit var initialState: MatchEngineState

    @Before
    fun setUp() {
        engine = BwfScoringEngineImpl()
        initialState = buildInitialState(
            totalGames = 3,
            initialServer = Player.A,
            initialCourtSideA = CourtSide.LEFT,
        )
    }

    // ---- checkGameWinner ----

    @Test
    fun `winner at 21 with 2 lead`() {
        assertEquals(Player.A, engine.checkGameWinner(21, 19))
        assertEquals(Player.B, engine.checkGameWinner(14, 21))
    }

    @Test
    fun `no winner at 20 all`() {
        assertNull(engine.checkGameWinner(20, 20))
    }

    @Test
    fun `deuce requires 2 point lead`() {
        assertNull(engine.checkGameWinner(21, 20))
        assertEquals(Player.A, engine.checkGameWinner(22, 20))
        assertNull(engine.checkGameWinner(22, 21))
        assertEquals(Player.B, engine.checkGameWinner(21, 23))
    }

    @Test
    fun `hard cap at 30`() {
        assertEquals(Player.A, engine.checkGameWinner(30, 29))
        assertEquals(Player.B, engine.checkGameWinner(29, 30))
    }

    @Test
    fun `no winner before 21`() {
        assertNull(engine.checkGameWinner(20, 18))
    }

    // ---- Score progression ----

    @Test
    fun `applyRally increments winner score`() {
        val state = engine.applyRally(initialState, Player.A, null, 1000)
        assertEquals(1, state.currentGame.scoreA)
        assertEquals(0, state.currentGame.scoreB)
    }

    @Test
    fun `winner serves next`() {
        val state = engine.applyRally(initialState, Player.B, null, 1000)
        assertEquals(Player.B, state.currentServer)
    }

    // ---- Service side (recorded in rally) ----

    @Test
    fun `server at even score serves from EVEN (right box)`() {
        // A serves, A's score is 0 (even) — service side is recorded in the rally
        val state = engine.applyRally(initialState, Player.A, null, 1000)
        assertEquals(ServiceSide.EVEN, state.currentGame.rallies.last().serviceSide)
    }

    @Test
    fun `server at odd score serves from ODD (left box)`() {
        // A scores once → A's score = 1 (odd), A serves next
        var state = engine.applyRally(initialState, Player.A, null, 1000)
        state = engine.applyRally(state, Player.A, null, 1000)
        assertEquals(ServiceSide.ODD, state.currentGame.rallies.last().serviceSide)
    }

    // ---- Undo ----

    @Test
    fun `undo removes last rally`() {
        var state = engine.applyRally(initialState, Player.A, null, 1000)
        state = engine.applyRally(state, Player.B, null, 1000)
        state = engine.undoLastRally(state)
        assertEquals(1, state.currentGame.rallies.size)
        assertEquals(1, state.currentGame.scoreA)
        assertEquals(0, state.currentGame.scoreB)
        assertEquals(Player.A, state.currentServer)
    }

    @Test
    fun `undo on empty rallies is a no-op`() {
        val state = engine.undoLastRally(initialState)
        assertEquals(initialState, state)
    }

    @Test
    fun `undo restores correct server`() {
        // A, B, A — after undo of last rally server should be B
        var state = engine.applyRally(initialState, Player.A, null, 1000)
        state = engine.applyRally(state, Player.B, null, 1000)
        state = engine.applyRally(state, Player.A, null, 1000)
        val undoneState = engine.undoLastRally(state)
        assertEquals(Player.B, undoneState.currentServer)
    }

    // ---- Game winner in UI state ----

    @Test
    fun `game winner detected in toUiState`() {
        var state = initialState
        repeat(21) { state = engine.applyRally(state, Player.A, null, 1000) }
        val uiState = engine.toUiState(state)
        assertEquals(Player.A, uiState.gameWinner)
    }

    // ---- Deuce (no winner at 20-20) ----

    @Test
    fun `no game winner at 20-20`() {
        var state = initialState
        repeat(20) {
            state = engine.applyRally(state, Player.A, null, 1000)
            state = engine.applyRally(state, Player.B, null, 1000)
        }
        val uiState = engine.toUiState(state)
        assertNull(uiState.gameWinner)
    }

    @Test
    fun `game ends at 30-29 (cap)`() {
        var state = initialState
        repeat(29) {
            state = engine.applyRally(state, Player.A, null, 1000)
            state = engine.applyRally(state, Player.B, null, 1000)
        }
        // 29-29 — A scores once more → 30-29
        state = engine.applyRally(state, Player.A, null, 1000)
        val uiState = engine.toUiState(state)
        assertEquals(Player.A, uiState.gameWinner)
    }

    // ---- Mid-game switch ----

    @Test
    fun `mid-game switch triggered in deciding game at 11`() {
        val state3 = buildInitialState(
            totalGames = 3,
            initialServer = Player.A,
            initialCourtSideA = CourtSide.LEFT,
            gameNumber = 3,
        )
        val initialCourtSideA = state3.courtSideA
        var state = state3
        repeat(10) { state = engine.applyRally(state, Player.A, null, 1000) }
        assertFalse(state.hasMidGameSwitchHappened)
        state = engine.applyRally(state, Player.A, null, 1000)
        assertTrue(state.hasMidGameSwitchHappened)
        assertEquals(initialCourtSideA.let { if (it == CourtSide.LEFT) CourtSide.RIGHT else CourtSide.LEFT }, state.courtSideA)
    }

    @Test
    fun `mid-game switch does not trigger outside deciding game`() {
        var state = initialState // game 1 of 3
        repeat(11) { state = engine.applyRally(state, Player.A, null, 1000) }
        assertFalse(state.hasMidGameSwitchHappened)
    }

    // ---- Rally history ----

    @Test
    fun `no rallies on fresh state`() {
        assertTrue(initialState.currentGame.rallies.isEmpty())
    }

    @Test
    fun `rally recorded after first point`() {
        val state = engine.applyRally(initialState, Player.A, null, 1000)
        assertTrue(state.currentGame.rallies.isNotEmpty())
    }

    // ---- Helpers ----

    private fun buildInitialState(
        totalGames: Int,
        initialServer: Player,
        initialCourtSideA: CourtSide,
        gameNumber: Int = 1,
    ): MatchEngineState {
        val matchId = "match-test"
        val gameId = "game-$gameNumber"
        val match = Match(
            id = matchId,
            format = GameFormat.SIMPLIFIED,
            totalGames = totalGames,
            startedAtMs = 0L,
            initialServerPlayer = initialServer,
            initialCourtSideA = initialCourtSideA,
        )
        val courtSideA = BwfScoringEngineImpl.computeCourtSideForGame(initialCourtSideA, gameNumber)
        val game = GameState(id = gameId, matchId = matchId, gameNumber = gameNumber, startedAtMs = 0L)
        return MatchEngineState(
            match = match,
            currentGame = game,
            currentServer = initialServer,
            courtSideA = courtSideA,
            gameInitialServer = initialServer,
            gameInitialCourtSideA = courtSideA,
        )
    }
}
