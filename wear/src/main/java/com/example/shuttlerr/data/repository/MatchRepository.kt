package com.example.shuttlerr.data.repository

import com.example.shuttlerr.domain.model.DoublesSlot
import com.example.shuttlerr.domain.model.GameFormat
import com.example.shuttlerr.domain.model.Match
import com.example.shuttlerr.domain.model.MatchEngineState
import com.example.shuttlerr.domain.model.Player
import com.example.shuttlerr.domain.model.Rally

interface MatchRepository {
    suspend fun createMatch(
        matchId: String,
        format: GameFormat,
        totalGames: Int,
        initialServer: Player,
        initialServerSlot: DoublesSlot = DoublesSlot.ONE,
    )

    suspend fun getMatchWithGamesAndRallies(matchId: String): Match?

    suspend fun recordRally(rally: Rally, scoreA: Int, scoreB: Int, gameWinner: Player?)

    suspend fun deleteLastRally(gameId: String)

    suspend fun resetGame(gameId: String)

    suspend fun startNewGame(matchId: String, gameNumber: Int, gameId: String)

    suspend fun finalizeMatch(matchId: String, endedAtMs: Long)

    suspend fun getUnsyncedMatches(): List<Match>

    suspend fun markSynced(matchId: String)
}
