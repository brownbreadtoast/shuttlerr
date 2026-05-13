package com.example.shuttlerr.presentation.match

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shuttlerr.data.health.HeartRateMonitor
import com.example.shuttlerr.data.repository.MatchRepository
import com.example.shuttlerr.domain.engine.BwfScoringEngine
import com.example.shuttlerr.domain.model.ActiveMatchUiState
import com.example.shuttlerr.domain.model.MatchEngineState
import com.example.shuttlerr.domain.model.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveMatchViewModel @Inject constructor(
    private val matchRepository: MatchRepository,
    private val engine: BwfScoringEngine,
    private val heartRateMonitor: HeartRateMonitor,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val matchId: String = checkNotNull(savedStateHandle["matchId"])

    private var engineState: MatchEngineState? = null
    private var rallyStartMs: Long = System.currentTimeMillis()

    private val _uiState = MutableStateFlow<ActiveMatchUiState?>(null)
    val uiState: StateFlow<ActiveMatchUiState?> = _uiState.asStateFlow()

    private val heartRate: StateFlow<Int?> = heartRateMonitor.heartRateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        loadMatch()
    }

    fun resetRallyTimer() {
        rallyStartMs = System.currentTimeMillis()
    }

    fun recordPoint(winner: Player) {
        val state = engineState ?: return
        val durationMs = System.currentTimeMillis() - rallyStartMs
        val newState = engine.applyRally(state, winner, heartRate.value, durationMs)
        engineState = newState

        val rally = newState.currentGame.rallies.last()
        viewModelScope.launch {
            matchRepository.recordRally(
                rally = rally,
                scoreA = newState.currentGame.scoreA,
                scoreB = newState.currentGame.scoreB,
                gameWinner = newState.currentGame.winnerPlayer,
            )
        }

        rallyStartMs = System.currentTimeMillis()
        refreshUiState()
    }

    fun undoLastPoint() {
        val state = engineState ?: return
        if (state.currentGame.rallies.isEmpty()) return
        val newState = engine.undoLastRally(state)
        engineState = newState

        viewModelScope.launch {
            matchRepository.deleteLastRally(state.currentGame.id)
        }

        refreshUiState()
    }

    private fun loadMatch() {
        viewModelScope.launch {
            val match = matchRepository.getMatchWithGamesAndRallies(matchId) ?: return@launch
            engineState = engine.replayFromStart(match)
            refreshUiState()
        }
    }

    private fun refreshUiState() {
        val state = engineState ?: return
        _uiState.update { engine.toUiState(state) }
    }
}
