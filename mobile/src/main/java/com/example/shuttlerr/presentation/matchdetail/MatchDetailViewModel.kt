package com.example.shuttlerr.presentation.matchdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shuttlerr.data.repository.MobileMatchRepository
import com.example.shuttlerr.domain.model.Match
import com.example.shuttlerr.domain.model.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RallyDurationBucket(val bucketLabel: String, val count: Int)

data class MatchDetailUiState(
    val match: Match? = null,
    val gamesWonA: Int = 0,
    val gamesWonB: Int = 0,
    val winner: Player? = null,
    val totalRallies: Int = 0,
    val durationMs: Long = 0L,
    val rallyDurationBuckets: List<RallyDurationBucket> = emptyList(),
    val hrData: List<Pair<Long, Int>> = emptyList(),   // (timestampMs, bpm)
    val serverWinStats: ServerWinStats? = null,
)

data class ServerWinStats(
    val aWonAsServer: Int,
    val aWonAsReceiver: Int,
    val bWonAsServer: Int,
    val bWonAsReceiver: Int,
)

@HiltViewModel
class MatchDetailViewModel @Inject constructor(
    private val matchRepository: MobileMatchRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val matchId: String = checkNotNull(savedStateHandle["matchId"])

    private val _uiState = MutableStateFlow(MatchDetailUiState())
    val uiState: StateFlow<MatchDetailUiState> = _uiState.asStateFlow()

    init {
        loadMatchDetail()
    }

    private fun loadMatchDetail() {
        viewModelScope.launch {
            val match = matchRepository.getMatchWithDetail(matchId) ?: return@launch
            val allRallies = match.games.flatMap { it.rallies }

            val gamesNeeded = (match.totalGames / 2) + 1
            val gamesWonA = match.games.count { it.winnerPlayer == Player.A }
            val gamesWonB = match.games.count { it.winnerPlayer == Player.B }
            val winner = when {
                gamesWonA >= gamesNeeded -> Player.A
                gamesWonB >= gamesNeeded -> Player.B
                else -> null
            }

            // Rally duration histogram in 5-second buckets
            val bucketMap = mutableMapOf<Int, Int>()
            for (rally in allRallies) {
                val bucket = (rally.durationMs / 5_000).toInt()
                bucketMap[bucket] = (bucketMap[bucket] ?: 0) + 1
            }
            val maxBucket = bucketMap.keys.maxOrNull() ?: 0
            val buckets = (0..maxBucket).map { b ->
                RallyDurationBucket("${b * 5}s", bucketMap[b] ?: 0)
            }

            // HR data (only rallies with HR reading)
            val hrData = allRallies
                .filter { it.heartRateBpm != null }
                .map { it.timestampMs to it.heartRateBpm!! }

            // Service win stats
            val aWonAsServer = allRallies.count {
                it.winnerPlayer == Player.A && it.serverBeforeRally == Player.A
            }
            val aWonAsReceiver = allRallies.count {
                it.winnerPlayer == Player.A && it.serverBeforeRally == Player.B
            }
            val bWonAsServer = allRallies.count {
                it.winnerPlayer == Player.B && it.serverBeforeRally == Player.B
            }
            val bWonAsReceiver = allRallies.count {
                it.winnerPlayer == Player.B && it.serverBeforeRally == Player.A
            }

            val duration = (match.endedAtMs ?: System.currentTimeMillis()) - match.startedAtMs

            _uiState.value = MatchDetailUiState(
                match = match,
                gamesWonA = gamesWonA,
                gamesWonB = gamesWonB,
                winner = winner,
                totalRallies = allRallies.size,
                durationMs = duration,
                rallyDurationBuckets = buckets,
                hrData = hrData,
                serverWinStats = ServerWinStats(aWonAsServer, aWonAsReceiver, bWonAsServer, bWonAsReceiver),
            )
        }
    }
}
