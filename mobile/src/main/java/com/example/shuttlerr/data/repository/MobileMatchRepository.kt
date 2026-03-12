package com.example.shuttlerr.data.repository

import com.example.shuttlerr.data.db.dao.GameDao
import com.example.shuttlerr.data.db.dao.MatchDao
import com.example.shuttlerr.data.db.dao.RallyDao
import com.example.shuttlerr.data.db.entities.GameEntity
import com.example.shuttlerr.data.db.entities.MatchEntity
import com.example.shuttlerr.data.db.entities.RallyEntity
import com.example.shuttlerr.domain.model.CourtSide
import com.example.shuttlerr.domain.model.GameFormat
import com.example.shuttlerr.domain.model.GameState
import com.example.shuttlerr.domain.model.Match
import com.example.shuttlerr.domain.model.Player
import com.example.shuttlerr.domain.model.Rally
import com.example.shuttlerr.domain.model.ServiceSide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MobileMatchRepository @Inject constructor(
    private val matchDao: MatchDao,
    private val gameDao: GameDao,
    private val rallyDao: RallyDao,
) {
    fun getAllMatchSummaries(): Flow<List<Match>> =
        matchDao.getAllMatches().map { entities ->
            entities.map { entity ->
                Match(
                    id = entity.id,
                    format = GameFormat.valueOf(entity.format),
                    totalGames = entity.totalGames,
                    startedAtMs = entity.startedAtMs,
                    endedAtMs = entity.endedAtMs,
                    initialServerPlayer = Player.valueOf(entity.initialServerPlayer),
                    initialCourtSideA = CourtSide.valueOf(entity.initialCourtSideA),
                )
            }
        }

    suspend fun getMatchWithDetail(matchId: String): Match? {
        val entity = matchDao.getMatch(matchId) ?: return null
        val gameEntities = gameDao.getGamesForMatch(matchId)
        val games = gameEntities.map { gameEntity ->
            val rallies = rallyDao.getRalliesForGame(gameEntity.id).map { it.toDomain() }
            GameState(
                id = gameEntity.id,
                matchId = matchId,
                gameNumber = gameEntity.gameNumber,
                scoreA = gameEntity.scoreA,
                scoreB = gameEntity.scoreB,
                winnerPlayer = gameEntity.winnerPlayer?.let { Player.valueOf(it) },
                startedAtMs = gameEntity.startedAtMs,
                endedAtMs = gameEntity.endedAtMs,
                rallies = rallies,
            )
        }
        return Match(
            id = entity.id,
            format = GameFormat.valueOf(entity.format),
            totalGames = entity.totalGames,
            startedAtMs = entity.startedAtMs,
            endedAtMs = entity.endedAtMs,
            games = games,
            initialServerPlayer = Player.valueOf(entity.initialServerPlayer),
            initialCourtSideA = CourtSide.valueOf(entity.initialCourtSideA),
        )
    }

    /** Upsert a fully-synced match received from the wear device. */
    suspend fun upsertMatch(match: Match) {
        matchDao.insert(
            MatchEntity(
                id = match.id,
                format = match.format.name,
                totalGames = match.totalGames,
                startedAtMs = match.startedAtMs,
                endedAtMs = match.endedAtMs,
                initialServerPlayer = match.initialServerPlayer.name,
                initialCourtSideA = match.initialCourtSideA.name,
            )
        )
        for (game in match.games) {
            gameDao.insert(
                GameEntity(
                    id = game.id,
                    matchId = match.id,
                    gameNumber = game.gameNumber,
                    scoreA = game.scoreA,
                    scoreB = game.scoreB,
                    winnerPlayer = game.winnerPlayer?.name,
                    startedAtMs = game.startedAtMs,
                    endedAtMs = game.endedAtMs,
                )
            )
            for (rally in game.rallies) {
                rallyDao.insert(
                    RallyEntity(
                        id = rally.id,
                        gameId = game.id,
                        rallyNumber = rally.rallyNumber,
                        winnerPlayer = rally.winnerPlayer.name,
                        serverBeforeRally = rally.serverBeforeRally.name,
                        serverCourtSide = rally.serverCourtSide.name,
                        serviceSide = rally.serviceSide.name,
                        heartRateBpm = rally.heartRateBpm,
                        durationMs = rally.durationMs,
                        timestampMs = rally.timestampMs,
                    )
                )
            }
        }
    }

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
    )
}
