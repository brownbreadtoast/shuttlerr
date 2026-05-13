package com.example.shuttlerr.domain.engine

import com.example.shuttlerr.domain.model.ActiveMatchUiState
import com.example.shuttlerr.domain.model.CourtSide
import com.example.shuttlerr.domain.model.DoublesSlot
import com.example.shuttlerr.domain.model.GameState
import com.example.shuttlerr.domain.model.Match
import com.example.shuttlerr.domain.model.MatchEngineState
import com.example.shuttlerr.domain.model.Player
import com.example.shuttlerr.domain.model.Rally
import com.example.shuttlerr.domain.model.ServiceSide
import com.example.shuttlerr.domain.model.opposite
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface BwfScoringEngine {
    fun applyRally(state: MatchEngineState, winner: Player, hr: Int?, durationMs: Long): MatchEngineState
    fun undoLastRally(state: MatchEngineState): MatchEngineState
    fun toUiState(state: MatchEngineState): ActiveMatchUiState
    fun replayFromStart(match: Match): MatchEngineState
}

@Singleton
class BwfScoringEngineImpl @Inject constructor() : BwfScoringEngine {

    override fun applyRally(
        state: MatchEngineState,
        winner: Player,
        hr: Int?,
        durationMs: Long,
    ): MatchEngineState {
        val game = state.currentGame
        val serverCourtSide = if (state.currentServer == Player.A) state.courtSideA else state.courtSideA.opposite()
        val serverScore = if (state.currentServer == Player.A) game.scoreA else game.scoreB
        val serviceSide = if (serverScore % 2 == 0) ServiceSide.EVEN else ServiceSide.ODD

        val newScoreA = if (winner == Player.A) game.scoreA + 1 else game.scoreA
        val newScoreB = if (winner == Player.B) game.scoreB + 1 else game.scoreB

        val rally = Rally(
            id = UUID.randomUUID().toString(),
            gameId = game.id,
            rallyNumber = game.rallies.size + 1,
            winnerPlayer = winner,
            serverBeforeRally = state.currentServer,
            serverCourtSide = serverCourtSide,
            serviceSide = serviceSide,
            heartRateBpm = hr,
            durationMs = durationMs,
            timestampMs = System.currentTimeMillis(),
            serverSlot = state.currentServerSlot,
        )

        val gameWinner = checkGameWinner(newScoreA, newScoreB)

        val isDecidingGame = game.gameNumber == state.match.totalGames
        val maxScore = maxOf(newScoreA, newScoreB)
        val midGameSwitchTriggered = isDecidingGame && maxScore >= 11 && !state.hasMidGameSwitchHappened
        val newCourtSideA = if (midGameSwitchTriggered) state.courtSideA.opposite() else state.courtSideA

        val updatedGame = game.copy(
            scoreA = newScoreA,
            scoreB = newScoreB,
            winnerPlayer = gameWinner,
            endedAtMs = if (gameWinner != null) System.currentTimeMillis() else null,
            rallies = game.rallies + rally,
        )

        return state.copy(
            currentGame = updatedGame,
            currentServer = winner,
            courtSideA = newCourtSideA,
            hasMidGameSwitchHappened = state.hasMidGameSwitchHappened || midGameSwitchTriggered,
        )
    }

    override fun undoLastRally(state: MatchEngineState): MatchEngineState {
        val game = state.currentGame
        if (game.rallies.isEmpty()) return state

        val remainingRallies = game.rallies.dropLast(1)

        var currentServer = state.gameInitialServer
        var courtSideA = state.gameInitialCourtSideA
        var scoreA = 0
        var scoreB = 0
        var hasMidGameSwitch = false

        for (rally in remainingRallies) {
            val rWinner = rally.winnerPlayer
            if (rWinner == Player.A) scoreA++ else scoreB++
            currentServer = rWinner

            val isDecidingGame = game.gameNumber == state.match.totalGames
            val maxScore = maxOf(scoreA, scoreB)
            if (isDecidingGame && maxScore >= 11 && !hasMidGameSwitch) {
                courtSideA = courtSideA.opposite()
                hasMidGameSwitch = true
            }
        }

        return state.copy(
            currentGame = game.copy(
                scoreA = scoreA,
                scoreB = scoreB,
                winnerPlayer = null,
                endedAtMs = null,
                rallies = remainingRallies,
            ),
            currentServer = currentServer,
            courtSideA = courtSideA,
            hasMidGameSwitchHappened = hasMidGameSwitch,
        )
    }

    override fun toUiState(state: MatchEngineState): ActiveMatchUiState {
        val game = state.currentGame
        val gamesNeeded = (state.match.totalGames / 2) + 1
        val completedGames = state.match.games
        val gamesWonA = completedGames.count { it.winnerPlayer == Player.A } +
            if (game.winnerPlayer == Player.A) 1 else 0
        val gamesWonB = completedGames.count { it.winnerPlayer == Player.B } +
            if (game.winnerPlayer == Player.B) 1 else 0
        val matchWinner = when {
            gamesWonA >= gamesNeeded -> Player.A
            gamesWonB >= gamesNeeded -> Player.B
            else -> null
        }
        return ActiveMatchUiState(
            scoreA = game.scoreA,
            scoreB = game.scoreB,
            gameWinner = game.winnerPlayer,
            matchWinner = matchWinner,
        )
    }

    override fun replayFromStart(match: Match): MatchEngineState {
        val allGames = match.games.sortedBy { it.gameNumber }
        val completedGames = allGames.filter { it.winnerPlayer != null }
        val activeGame = allGames.firstOrNull { it.winnerPlayer == null }
            ?: return initialEngineState(match.copy(games = emptyList()))

        val initCourtSideA = computeCourtSideForGame(match.initialCourtSideA, 1)
        val game1 = allGames.firstOrNull { it.gameNumber == 1 } ?: activeGame

        var state = MatchEngineState(
            match = match.copy(games = emptyList()),
            currentGame = game1,
            currentServer = match.initialServerPlayer,
            courtSideA = initCourtSideA,
            gameInitialServer = match.initialServerPlayer,
            gameInitialCourtSideA = initCourtSideA,
        )

        for (completedGame in completedGames) {
            state = state.copy(currentGame = completedGame)
            for (rally in completedGame.rallies) {
                state = applyRally(state, rally.winnerPlayer, rally.heartRateBpm, rally.durationMs)
            }

            val nextGameNumber = completedGame.gameNumber + 1
            val nextCourtSideA = computeCourtSideForGame(match.initialCourtSideA, nextGameNumber)
            val nextServer = state.currentGame.winnerPlayer ?: state.currentServer
            val nextGame = allGames.firstOrNull { it.gameNumber == nextGameNumber } ?: activeGame

            state = MatchEngineState(
                match = state.match.copy(games = state.match.games + state.currentGame),
                currentGame = nextGame,
                currentServer = nextServer,
                courtSideA = nextCourtSideA,
                hasMidGameSwitchHappened = false,
                gameInitialServer = nextServer,
                gameInitialCourtSideA = nextCourtSideA,
            )
        }

        val gameInitServer = state.currentServer
        val gameInitCourtSideA = state.courtSideA

        state = state.copy(currentGame = activeGame)
        for (rally in activeGame.rallies) {
            state = applyRally(state, rally.winnerPlayer, rally.heartRateBpm, rally.durationMs)
        }

        return state.copy(
            gameInitialServer = gameInitServer,
            gameInitialCourtSideA = gameInitCourtSideA,
        )
    }

    internal fun checkGameWinner(scoreA: Int, scoreB: Int): Player? = when {
        scoreA >= 21 && scoreA - scoreB >= 2 -> Player.A
        scoreB >= 21 && scoreB - scoreA >= 2 -> Player.B
        scoreA >= 30 -> Player.A
        scoreB >= 30 -> Player.B
        else -> null
    }

    companion object {
        fun initialEngineState(match: Match, gameNumber: Int = 1): MatchEngineState {
            val courtSideA = computeCourtSideForGame(match.initialCourtSideA, gameNumber)
            val server = match.initialServerPlayer
            val gameId = match.games.getOrNull(gameNumber - 1)?.id ?: UUID.randomUUID().toString()
            val game = match.games.getOrNull(gameNumber - 1) ?: GameState(
                id = gameId,
                matchId = match.id,
                gameNumber = gameNumber,
                startedAtMs = System.currentTimeMillis(),
            )
            return MatchEngineState(
                match = match,
                currentGame = game,
                currentServer = server,
                courtSideA = courtSideA,
                gameInitialServer = server,
                gameInitialCourtSideA = courtSideA,
            )
        }

        fun computeCourtSideForGame(initialCourtSideA: CourtSide, gameNumber: Int): CourtSide =
            if (gameNumber % 2 == 1) initialCourtSideA else initialCourtSideA.opposite()
    }
}
