package com.example.shuttlerr.presentation.setup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shuttlerr.data.repository.MatchRepository
import com.example.shuttlerr.data.repository.PlayerRepository
import com.example.shuttlerr.domain.model.DoublesSlot
import com.example.shuttlerr.domain.model.GameFormat
import com.example.shuttlerr.domain.model.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val matchRepository: MatchRepository,
    private val playerRepository: PlayerRepository,
) : ViewModel() {

    var currentStep by mutableIntStateOf(0)
        private set

    var format by mutableStateOf(GameFormat.SINGLES)
    var totalGames by mutableStateOf(3)

    // Player selections (a2/b2 only used for doubles)
    var selectedA1 by mutableStateOf("")
    var selectedA2 by mutableStateOf("")
    var selectedB1 by mutableStateOf("")
    var selectedB2 by mutableStateOf("")

    var initialServer by mutableStateOf(Player.A)
    var initialServerSlot by mutableStateOf(DoublesSlot.ONE)

    val players = playerRepository.getAllPlayers()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Step layout:
    //   Singles : 0=Format 1=BestOf 2=PickA 3=PickB 4=Server          → 5 steps
    //   Doubles : 0=Format 1=BestOf 2=PickA1 3=PickA2 4=PickB1 5=PickB2 6=Server 7=Slot → 8 steps
    val maxStep: Int get() = if (format == GameFormat.DOUBLES) 7 else 4

    fun nextStep() { if (currentStep < maxStep) currentStep++ }
    fun prevStep() { if (currentStep > 0) currentStep-- }

    fun isPlayerStep(step: Int): Boolean = when (format) {
        GameFormat.SINGLES -> step in 2..3
        GameFormat.DOUBLES -> step in 2..5
    }

    fun serverStep(): Int = if (format == GameFormat.DOUBLES) 6 else 4

    /** Label shown above the player picker for the current step. */
    fun playerStepLabel(step: Int): String = when {
        format == GameFormat.SINGLES && step == 2 -> "Team A?"
        format == GameFormat.SINGLES && step == 3 -> "Team B?"
        step == 2 -> "A1?"
        step == 3 -> "A2?"
        step == 4 -> "B1?"
        else -> "B2?"
    }

    /** Which slot is being picked at this step. */
    fun currentPickSlot(step: Int): PickSlot = when {
        format == GameFormat.SINGLES && step == 2 -> PickSlot.A1
        format == GameFormat.SINGLES && step == 3 -> PickSlot.B1
        step == 2 -> PickSlot.A1
        step == 3 -> PickSlot.A2
        step == 4 -> PickSlot.B1
        else -> PickSlot.B2
    }

    fun selectPlayer(slot: PickSlot, name: String) {
        when (slot) {
            PickSlot.A1 -> selectedA1 = name
            PickSlot.A2 -> selectedA2 = name
            PickSlot.B1 -> selectedB1 = name
            PickSlot.B2 -> selectedB2 = name
        }
    }

    fun addPlayer(name: String) {
        viewModelScope.launch { playerRepository.addPlayer(name.trim()) }
    }

    fun createMatch(onCreated: (String) -> Unit) {
        val matchId = UUID.randomUUID().toString()
        viewModelScope.launch {
            matchRepository.createMatch(
                matchId = matchId,
                format = format,
                totalGames = totalGames,
                initialServer = initialServer,
                initialServerSlot = if (format == GameFormat.DOUBLES) initialServerSlot else DoublesSlot.ONE,
                teamAPlayer1 = selectedA1.ifBlank { "A1" },
                teamAPlayer2 = selectedA2.ifBlank { "A2" },
                teamBPlayer1 = selectedB1.ifBlank { "B1" },
                teamBPlayer2 = selectedB2.ifBlank { "B2" },
            )
            onCreated(matchId)
        }
    }
}

enum class PickSlot { A1, A2, B1, B2 }
