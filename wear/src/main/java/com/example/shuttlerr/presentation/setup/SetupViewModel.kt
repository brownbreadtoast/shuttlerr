package com.example.shuttlerr.presentation.setup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shuttlerr.data.repository.MatchRepository
import com.example.shuttlerr.domain.model.DoublesSlot
import com.example.shuttlerr.domain.model.GameFormat
import com.example.shuttlerr.domain.model.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val matchRepository: MatchRepository,
) : ViewModel() {

    var currentStep by mutableIntStateOf(0)
        private set

    var format by mutableStateOf(GameFormat.SINGLES)
    var totalGames by mutableStateOf(3)
    var initialServer by mutableStateOf(Player.A)
    var initialServerSlot by mutableStateOf(DoublesSlot.ONE)

    val maxStep: Int get() = if (format == GameFormat.DOUBLES) 3 else 2

    fun nextStep() { if (currentStep < maxStep) currentStep++ }
    fun prevStep() { if (currentStep > 0) currentStep-- }

    fun createMatch(onCreated: (String) -> Unit) {
        val matchId = UUID.randomUUID().toString()
        viewModelScope.launch {
            matchRepository.createMatch(
                matchId = matchId,
                format = format,
                totalGames = totalGames,
                initialServer = initialServer,
                initialServerSlot = if (format == GameFormat.DOUBLES) initialServerSlot else DoublesSlot.ONE,
            )
            onCreated(matchId)
        }
    }
}
