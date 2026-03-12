package com.example.shuttlerr.domain.engine

import com.example.shuttlerr.domain.model.ActiveMatchUiState
import com.example.shuttlerr.domain.model.CourtSide
import com.example.shuttlerr.domain.model.DoublesSlot
import com.example.shuttlerr.domain.model.GameFormat
import com.example.shuttlerr.domain.model.GameState
import com.example.shuttlerr.domain.model.Match
import com.example.shuttlerr.domain.model.MatchEngineState
import com.example.shuttlerr.domain.model.Player
import com.example.shuttlerr.domain.model.PlayerNames
import com.example.shuttlerr.domain.model.Rally
import com.example.shuttlerr.domain.model.ServiceSide
import com.example.shuttlerr.domain.model.opposite
import com.example.shuttlerr.domain.model.other
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface BwfScoringEngine {
    fun applyRally(state: MatchEngineState, winner: Player, hr: Int?, durationMs: Long): MatchEngineState
    fun undoLastRally(state: MatchEngineState): MatchEngineState
    fun shouldSwitchSidesAtMidGame(state: MatchEngineState): Boolean
    fun toUiState(state: MatchEngineState, heartRateBpm: Int?): ActiveMatchUiState
    /** Reconstruct full engine state by replaying all rallies from the match record. */
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
        val isDoubles = state.match.format == GameFormat.DOUBLES

        val serverBeforeRally = state.currentServer
        val serverSlotBeforeRally = state.currentServerSlot
        val serverCourtSide = deriveServerCourtSide(state.currentServer, state.courtSideA)
        val serviceSide = deriveServiceSide(state.currentServer, game.scoreA, game.scoreB)

        val newScoreA = if (winner == Player.A) game.scoreA + 1 else game.scoreA
        val newScoreB = if (winner == Player.B) game.scoreB + 1 else game.scoreB

        val rally = Rally(
            id = UUID.randomUUID().toString(),
            gameId = game.id,
            rallyNumber = game.rallies.size + 1,
            winnerPlayer = winner,
            serverBeforeRally = serverBeforeRally,
            serverCourtSide = serverCourtSide,
            serviceSide = serviceSide,
            heartRateBpm = hr,
            durationMs = durationMs,
            timestampMs = System.currentTimeMillis(),
            serverSlot = serverSlotBeforeRally,
        )

        val gameWinner = checkGameWinner(newScoreA, newScoreB)

        val isDecidingGame = game.gameNumber == state.match.totalGames
        val maxScore = maxOf(newScoreA, newScoreB)
        val midGameSwitchTriggered = isDecidingGame && maxScore >= 11 && !state.hasMidGameSwitchHappened
        val newCourtSideA = if (midGameSwitchTriggered) state.courtSideA.opposite() else state.courtSideA

        // Doubles rotation logic
        val newServer: Player
        val newServerSlot: DoublesSlot
        val newRightCourtSlotA: DoublesSlot
        val newRightCourtSlotB: DoublesSlot

        if (isDoubles) {
            if (winner == state.currentServer) {
                // Serving team wins: same player serves again, both players swap service courts
                newServer = winner
                newServerSlot = state.currentServerSlot
                newRightCourtSlotA = if (winner == Player.A) state.rightCourtSlotA.other() else state.rightCourtSlotA
                newRightCourtSlotB = if (winner == Player.B) state.rightCourtSlotB.other() else state.rightCourtSlotB
            } else {
                // Receiving team wins: no position change; player in correct court for new score serves
                newServer = winner
                val winnerScore = if (winner == Player.A) newScoreA else newScoreB
                val winnerRightSlot = if (winner == Player.A) state.rightCourtSlotA else state.rightCourtSlotB
                newServerSlot = if (winnerScore % 2 == 0) winnerRightSlot else winnerRightSlot.other()
                newRightCourtSlotA = state.rightCourtSlotA
                newRightCourtSlotB = state.rightCourtSlotB
            }
        } else {
            newServer = winner
            newServerSlot = DoublesSlot.ONE
            newRightCourtSlotA = state.rightCourtSlotA
            newRightCourtSlotB = state.rightCourtSlotB
        }

        val updatedGame = game.copy(
            scoreA = newScoreA,
            scoreB = newScoreB,
            winnerPlayer = gameWinner,
            endedAtMs = if (gameWinner != null) System.currentTimeMillis() else null,
            rallies = game.rallies + rally,
        )

        return state.copy(
            currentGame = updatedGame,
            currentServer = newServer,
            currentServerSlot = newServerSlot,
            courtSideA = newCourtSideA,
            rightCourtSlotA = newRightCourtSlotA,
            rightCourtSlotB = newRightCourtSlotB,
            hasMidGameSwitchHappened = state.hasMidGameSwitchHappened || midGameSwitchTriggered,
        )
    }

    override fun undoLastRally(state: MatchEngineState): MatchEngineState {
        val game = state.currentGame
        if (game.rallies.isEmpty()) return state

        val remainingRallies = game.rallies.dropLast(1)
        val isDoubles = state.match.format == GameFormat.DOUBLES

        // Replay from the game's initial state
        var currentServer = state.gameInitialServer
        var currentServerSlot = state.gameInitialServerSlot
        var courtSideA = state.gameInitialCourtSideA
        var rightCourtSlotA = state.gameInitialRightCourtSlotA
        var rightCourtSlotB = state.gameInitialRightCourtSlotB
        var scoreA = 0
        var scoreB = 0
        var hasMidGameSwitch = false

        for (rally in remainingRallies) {
            val rWinner = rally.winnerPlayer
            if (rWinner == Player.A) scoreA++ else scoreB++

            if (isDoubles) {
                if (rWinner == currentServer) {
                    // Serving team wins: flip their right court slot
                    if (rWinner == Player.A) rightCourtSlotA = rightCourtSlotA.other()
                    else rightCourtSlotB = rightCourtSlotB.other()
                    // currentServerSlot unchanged
                } else {
                    // Receiving team wins: no position change, derive new server slot
                    val winnerScore = if (rWinner == Player.A) scoreA else scoreB
                    val winnerRightSlot = if (rWinner == Player.A) rightCourtSlotA else rightCourtSlotB
                    currentServerSlot = if (winnerScore % 2 == 0) winnerRightSlot else winnerRightSlot.other()
                }
                currentServer = rWinner
            } else {
                currentServer = rWinner
            }

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
            currentServerSlot = currentServerSlot,
            courtSideA = courtSideA,
            rightCourtSlotA = rightCourtSlotA,
            rightCourtSlotB = rightCourtSlotB,
            hasMidGameSwitchHappened = hasMidGameSwitch,
        )
    }

    override fun shouldSwitchSidesAtMidGame(state: MatchEngineState): Boolean {
        val isDecidingGame = state.currentGame.gameNumber == state.match.totalGames
        if (!isDecidingGame) return false
        val maxScore = maxOf(state.currentGame.scoreA, state.currentGame.scoreB)
        if (maxScore != 11 || !state.hasMidGameSwitchHappened) return false
        val lastRally = state.currentGame.rallies.lastOrNull() ?: return false
        val prevScoreA = state.currentGame.scoreA - if (lastRally.winnerPlayer == Player.A) 1 else 0
        val prevScoreB = state.currentGame.scoreB - if (lastRally.winnerPlayer == Player.B) 1 else 0
        return maxOf(prevScoreA, prevScoreB) < 11
    }

    override fun toUiState(state: MatchEngineState, heartRateBpm: Int?): ActiveMatchUiState {
        val game = state.currentGame
        val isDoubles = state.match.format == GameFormat.DOUBLES
        val serviceSide = deriveServiceSide(state.currentServer, game.scoreA, game.scoreB)
        // Service box (left/right) is determined by score parity, not the physical court end
        val serverCourtSide = if (serviceSide == ServiceSide.EVEN) CourtSide.RIGHT else CourtSide.LEFT

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

        val isDeuce = game.scoreA >= 20 && game.scoreB >= 20 &&
            game.scoreA == game.scoreB && game.scoreA < 30

        // Doubles: receiver is in the court diagonally opposite the server
        val receiverSlot = if (isDoubles) {
            val serverScore = if (state.currentServer == Player.A) game.scoreA else game.scoreB
            val receiverRightSlot = if (state.currentServer == Player.A) state.rightCourtSlotB else state.rightCourtSlotA
            if (serverScore % 2 == 0) receiverRightSlot else receiverRightSlot.other()
        } else DoublesSlot.ONE

        return ActiveMatchUiState(
            scoreA = game.scoreA,
            scoreB = game.scoreB,
            currentGameNumber = game.gameNumber,
            totalGames = state.match.totalGames,
            currentServer = state.currentServer,
            serverCourtSide = serverCourtSide,
            serviceSide = serviceSide,
            heartRateBpm = heartRateBpm,
            isDeuce = isDeuce,
            gameWinner = game.winnerPlayer,
            matchWinner = matchWinner,
            canUndo = game.rallies.isNotEmpty(),
            showMidGameSwitchPrompt = shouldSwitchSidesAtMidGame(state),
            isDoubles = isDoubles,
            currentServerSlot = state.currentServerSlot,
            currentReceiverSlot = receiverSlot,
            rightCourtSlotA = state.rightCourtSlotA,
            rightCourtSlotB = state.rightCourtSlotB,
            playerNames = PlayerNames(
                a1 = state.match.teamAPlayer1,
                a2 = state.match.teamAPlayer2,
                b1 = state.match.teamBPlayer1,
                b2 = state.match.teamBPlayer2,
            ),
        )
    }

    override fun replayFromStart(match: Match): MatchEngineState {
        val allGames = match.games.sortedBy { it.gameNumber }
        val completedGames = allGames.filter { it.winnerPlayer != null }
        val activeGame = allGames.firstOrNull { it.winnerPlayer == null }
            ?: return initialEngineState(match.copy(games = emptyList()))

        val isDoubles = match.format == GameFormat.DOUBLES

        // Build game-1 initial court slot state
        val initServerSlot = match.initialServerSlot
        val initRightCourtSlotA = if (match.initialServerPlayer == Player.A) initServerSlot else DoublesSlot.ONE
        val initRightCourtSlotB = if (match.initialServerPlayer == Player.B) initServerSlot else DoublesSlot.ONE
        val initCourtSideA = computeCourtSideForGame(match.initialCourtSideA, 1)
        val game1 = allGames.firstOrNull { it.gameNumber == 1 } ?: activeGame

        var state = MatchEngineState(
            match = match.copy(games = emptyList()),
            currentGame = game1,
            currentServer = match.initialServerPlayer,
            currentServerSlot = initServerSlot,
            courtSideA = initCourtSideA,
            rightCourtSlotA = initRightCourtSlotA,
            rightCourtSlotB = initRightCourtSlotB,
            gameInitialServer = match.initialServerPlayer,
            gameInitialServerSlot = initServerSlot,
            gameInitialCourtSideA = initCourtSideA,
            gameInitialRightCourtSlotA = initRightCourtSlotA,
            gameInitialRightCourtSlotB = initRightCourtSlotB,
        )

        // Replay each completed game and advance
        for (completedGame in completedGames) {
            state = state.copy(currentGame = completedGame)
            for (rally in completedGame.rallies) {
                state = applyRally(state, rally.winnerPlayer, rally.heartRateBpm, rally.durationMs)
            }

            val nextGameNumber = completedGame.gameNumber + 1
            val nextCourtSideA = computeCourtSideForGame(match.initialCourtSideA, nextGameNumber)
            val nextServer = state.currentGame.winnerPlayer ?: state.currentServer
            val nextServerSlot = if (isDoubles) {
                if (nextServer == Player.A) state.rightCourtSlotA else state.rightCourtSlotB
            } else DoublesSlot.ONE
            val nextGame = allGames.firstOrNull { it.gameNumber == nextGameNumber } ?: activeGame

            state = MatchEngineState(
                match = state.match.copy(games = state.match.games + state.currentGame),
                currentGame = nextGame,
                currentServer = nextServer,
                currentServerSlot = nextServerSlot,
                courtSideA = nextCourtSideA,
                rightCourtSlotA = state.rightCourtSlotA,
                rightCourtSlotB = state.rightCourtSlotB,
                hasMidGameSwitchHappened = false,
                gameInitialServer = nextServer,
                gameInitialServerSlot = nextServerSlot,
                gameInitialCourtSideA = nextCourtSideA,
                gameInitialRightCourtSlotA = state.rightCourtSlotA,
                gameInitialRightCourtSlotB = state.rightCourtSlotB,
            )
        }

        // Capture pre-active-game state (used for undo replay)
        val gameInitServer = state.currentServer
        val gameInitServerSlot = state.currentServerSlot
        val gameInitCourtSideA = state.courtSideA
        val gameInitRcsA = state.rightCourtSlotA
        val gameInitRcsB = state.rightCourtSlotB

        // Replay active game rallies
        state = state.copy(currentGame = activeGame)
        for (rally in activeGame.rallies) {
            state = applyRally(state, rally.winnerPlayer, rally.heartRateBpm, rally.durationMs)
        }

        return state.copy(
            gameInitialServer = gameInitServer,
            gameInitialServerSlot = gameInitServerSlot,
            gameInitialCourtSideA = gameInitCourtSideA,
            gameInitialRightCourtSlotA = gameInitRcsA,
            gameInitialRightCourtSlotB = gameInitRcsB,
        )
    }

    // --- Helpers ---

    private fun deriveServerCourtSide(server: Player, courtSideA: CourtSide): CourtSide =
        if (server == Player.A) courtSideA else courtSideA.opposite()

    private fun deriveServiceSide(server: Player, scoreA: Int, scoreB: Int): ServiceSide {
        val serverScore = if (server == Player.A) scoreA else scoreB
        return if (serverScore % 2 == 0) ServiceSide.EVEN else ServiceSide.ODD
    }

    internal fun checkGameWinner(scoreA: Int, scoreB: Int): Player? = when {
        scoreA >= 21 && scoreA - scoreB >= 2 -> Player.A
        scoreB >= 21 && scoreB - scoreA >= 2 -> Player.B
        scoreA >= 30 -> Player.A
        scoreB >= 30 -> Player.B
        else -> null
    }

    companion object {
        fun initialEngineState(
            match: Match,
            gameNumber: Int = 1,
        ): MatchEngineState {
            val courtSideA = computeCourtSideForGame(match.initialCourtSideA, gameNumber)
            val server = match.initialServerPlayer
            val serverSlot = match.initialServerSlot
            val rightCourtSlotA = if (server == Player.A) serverSlot else DoublesSlot.ONE
            val rightCourtSlotB = if (server == Player.B) serverSlot else DoublesSlot.ONE
            val gameId = match.games.getOrNull(gameNumber - 1)?.id
                ?: UUID.randomUUID().toString()
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
                currentServerSlot = serverSlot,
                courtSideA = courtSideA,
                rightCourtSlotA = rightCourtSlotA,
                rightCourtSlotB = rightCourtSlotB,
                gameInitialServer = server,
                gameInitialServerSlot = serverSlot,
                gameInitialCourtSideA = courtSideA,
                gameInitialRightCourtSlotA = rightCourtSlotA,
                gameInitialRightCourtSlotB = rightCourtSlotB,
            )
        }

        /** For each game, sides flip from the initial value. Odd games → initial, even games → flipped. */
        fun computeCourtSideForGame(initialCourtSideA: CourtSide, gameNumber: Int): CourtSide =
            if (gameNumber % 2 == 1) initialCourtSideA else initialCourtSideA.opposite()
    }
}
