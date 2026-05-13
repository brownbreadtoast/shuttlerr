package com.example.shuttlerr.presentation.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shuttlerr.data.repository.MatchRepository
import com.example.shuttlerr.domain.model.CourtSide
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

    fun createMatch(onCreated: (String) -> Unit) {
        val matchId = UUID.randomUUID().toString()
        viewModelScope.launch {
            matchRepository.createMatch(
                matchId = matchId,
                format = GameFormat.SIMPLIFIED,
                totalGames = 1,
                initialServer = Player.A,
                initialServerSlot = DoublesSlot.ONE,
                teamAPlayer1 = "Me",
                teamAPlayer2 = "A2",
                teamBPlayer1 = "Op",
                teamBPlayer2 = "B2",
            )
            onCreated(matchId)
        }
    }
}
