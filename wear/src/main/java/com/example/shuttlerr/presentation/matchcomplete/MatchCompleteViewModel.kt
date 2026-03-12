package com.example.shuttlerr.presentation.matchcomplete

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.shuttlerr.data.repository.MatchRepository
import com.example.shuttlerr.data.sync.SyncWorker
import com.example.shuttlerr.domain.model.Match
import com.example.shuttlerr.domain.model.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MatchSummaryUiState(
    val match: Match? = null,
    val gamesWonA: Int = 0,
    val gamesWonB: Int = 0,
    val winner: Player? = null,
    val totalRallies: Int = 0,
    val durationMs: Long = 0L,
    val isSyncing: Boolean = false,
    val syncDone: Boolean = false,
)

@HiltViewModel
class MatchCompleteViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val matchRepository: MatchRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val matchId: String = checkNotNull(savedStateHandle["matchId"])

    private val _uiState = MutableStateFlow(MatchSummaryUiState())
    val uiState: StateFlow<MatchSummaryUiState> = _uiState.asStateFlow()

    init {
        loadMatchSummary()
    }

    private fun loadMatchSummary() {
        viewModelScope.launch {
            val match = matchRepository.getMatchWithGamesAndRallies(matchId) ?: return@launch
            val gamesWonA = match.games.count { it.winnerPlayer == Player.A }
            val gamesWonB = match.games.count { it.winnerPlayer == Player.B }
            val gamesNeeded = (match.totalGames / 2) + 1
            val winner = when {
                gamesWonA >= gamesNeeded -> Player.A
                gamesWonB >= gamesNeeded -> Player.B
                else -> null
            }
            val totalRallies = match.games.sumOf { it.rallies.size }
            val duration = (match.endedAtMs ?: System.currentTimeMillis()) - match.startedAtMs

            _uiState.value = MatchSummaryUiState(
                match = match,
                gamesWonA = gamesWonA,
                gamesWonB = gamesWonB,
                winner = winner,
                totalRallies = totalRallies,
                durationMs = duration,
            )
        }
    }

    fun saveAndSync(onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            matchRepository.finalizeMatch(matchId, System.currentTimeMillis())
            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(SyncWorker.WORK_NAME, ExistingWorkPolicy.REPLACE, syncRequest)
            _uiState.value = _uiState.value.copy(isSyncing = false, syncDone = true)
            onDone()
        }
    }
}
