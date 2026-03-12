package com.example.shuttlerr.data.repository

import com.example.shuttlerr.data.db.dao.GameDao
import com.example.shuttlerr.data.db.dao.MatchDao
import com.example.shuttlerr.data.db.dao.RallyDao
import com.example.shuttlerr.data.db.entities.GameEntity
import com.example.shuttlerr.data.db.entities.MatchEntity
import com.example.shuttlerr.data.db.entities.RallyEntity
import com.example.shuttlerr.domain.model.CourtSide
import com.example.shuttlerr.domain.model.DoublesSlot
import com.example.shuttlerr.domain.model.GameFormat
import com.example.shuttlerr.domain.model.GameState
import com.example.shuttlerr.domain.model.Match
import com.example.shuttlerr.domain.model.MatchEngineState
import com.example.shuttlerr.domain.model.Player
import com.example.shuttlerr.domain.model.Rally
import com.example.shuttlerr.domain.model.ServiceSide
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchRepositoryImpl @Inject constructor(
    private val matchDao: MatchDao,
    private val gameDao: GameDao,
    private val rallyDao: RallyDao,
) : MatchRepository {

    override suspend fun createMatch(
        matchId: String,
        format: GameFormat,
        totalGames: Int,
        initialServer: Player,
        initialServerSlot: DoublesSlot,
    ) {
        val now = System.currentTimeMillis()
        matchDao.insert(
            MatchEntity(
                id = matchId,
                format = format.name,
                totalGames = totalGames,
                startedAtMs = now,
                endedAtMs = null,
                initialServerPlayer = initialServer.name,
                initialCourtSideA = CourtSide.RIGHT.name, // BWF: score 0 (even) → right service court
                initialServerSlot = initialServerSlot.name,
            )
        )
        gameDao.insert(
            GameEntity(
                id = UUID.randomUUID().toString(),
                matchId = matchId,
                gameNumber = 1,
                scoreA = 0,
                scoreB = 0,
                winnerPlayer = null,
                startedAtMs = now,
                endedAtMs = null,
            )
        )
    }

    override suspend fun getMatchWithGamesAndRallies(matchId: String): Match? {
        val matchEntity = matchDao.getMatch(matchId) ?: return null
        val gameEntities = gameDao.getGamesForMatch(matchId)
        val games = gameEntities.map { gameEntity ->
            val rallyEntities = rallyDao.getRalliesForGame(gameEntity.id)
            GameState(
                id = gameEntity.id,
                matchId = matchId,
                gameNumber = gameEntity.gameNumber,
                scoreA = gameEntity.scoreA,
                scoreB = gameEntity.scoreB,
                winnerPlayer = gameEntity.winnerPlayer?.let { Player.valueOf(it) },
                startedAtMs = gameEntity.startedAtMs,
                endedAtMs = gameEntity.endedAtMs,
                rallies = rallyEntities.map { it.toDomain() },
            )
        }
        return Match(
            id = matchEntity.id,
            format = GameFormat.valueOf(matchEntity.format),
            totalGames = matchEntity.totalGames,
            startedAtMs = matchEntity.startedAtMs,
            endedAtMs = matchEntity.endedAtMs,
            games = games,
            initialServerPlayer = Player.valueOf(matchEntity.initialServerPlayer),
            initialCourtSideA = CourtSide.valueOf(matchEntity.initialCourtSideA),
            synced = matchEntity.synced,
            initialServerSlot = DoublesSlot.valueOf(matchEntity.initialServerSlot),
            teamAPlayer1 = matchEntity.teamAPlayer1,
            teamAPlayer2 = matchEntity.teamAPlayer2,
            teamBPlayer1 = matchEntity.teamBPlayer1,
            teamBPlayer2 = matchEntity.teamBPlayer2,
        )
    }

    override suspend fun recordRally(rally: Rally, scoreA: Int, scoreB: Int, gameWinner: Player?) {
        rallyDao.insert(rally.toEntity())
        gameDao.updateScore(rally.gameId, scoreA, scoreB)
        if (gameWinner != null) {
            gameDao.markGameWon(rally.gameId, gameWinner.name, System.currentTimeMillis())
        }
    }

    override suspend fun deleteLastRally(gameId: String) {
        rallyDao.deleteLastRallyForGame(gameId)
    }

    override suspend fun resetGame(gameId: String) {
        rallyDao.deleteAllRalliesForGame(gameId)
        gameDao.updateScore(gameId, 0, 0)
    }

    override suspend fun startNewGame(matchId: String, gameNumber: Int, gameId: String) {
        gameDao.insert(
            GameEntity(
                id = gameId,
                matchId = matchId,
                gameNumber = gameNumber,
                scoreA = 0,
                scoreB = 0,
                winnerPlayer = null,
                startedAtMs = System.currentTimeMillis(),
                endedAtMs = null,
            )
        )
    }

    override suspend fun finalizeMatch(matchId: String, endedAtMs: Long) {
        matchDao.updateEndTime(matchId, endedAtMs)
    }

    override suspend fun getUnsyncedMatches(): List<Match> {
        return matchDao.getUnsyncedMatches().mapNotNull { getMatchWithGamesAndRallies(it.id) }
    }

    override suspend fun markSynced(matchId: String) {
        matchDao.markSynced(matchId)
    }

    // --- Mapping helpers ---

    private fun RallyEntity.toDomain() = Rally(
        id = id,
        gameId = gameId,
        rallyNumber = rallyNumber,
        winnerPlayer = Player.valueOf(winnerPlayer),
        serverBeforeRally = Player.valueOf(serverBeforeRally),
        serverCourtSide = CourtSide.valueOf(serverCourtSide),
        serviceSide = ServiceSide.valueOf(serviceSide),
        heartRateBpm = heartRateBpm,
        durationMs = durationMs,
        timestampMs = timestampMs,
        serverSlot = DoublesSlot.valueOf(serverSlot),
    )

    private fun Rally.toEntity() = RallyEntity(
        id = id,
        gameId = gameId,
        rallyNumber = rallyNumber,
        winnerPlayer = winnerPlayer.name,
        serverBeforeRally = serverBeforeRally.name,
        serverCourtSide = serverCourtSide.name,
        serviceSide = serviceSide.name,
        heartRateBpm = heartRateBpm,
        durationMs = durationMs,
        timestampMs = timestampMs,
        serverSlot = serverSlot.name,
    )
}
