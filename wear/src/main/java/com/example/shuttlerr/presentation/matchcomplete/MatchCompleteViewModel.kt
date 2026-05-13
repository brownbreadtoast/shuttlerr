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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MatchSummaryUiState(
    val isSyncing: Boolean = false,
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

    fun saveAndSync(onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            matchRepository.finalizeMatch(matchId, System.currentTimeMillis())
            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(SyncWorker.WORK_NAME, ExistingWorkPolicy.REPLACE, syncRequest)
            _uiState.value = _uiState.value.copy(isSyncing = false)
            onDone()
        }
    }
}
