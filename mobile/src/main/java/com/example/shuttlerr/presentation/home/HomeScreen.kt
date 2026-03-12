package com.example.shuttlerr.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.shuttlerr.domain.model.Match
import com.example.shuttlerr.domain.model.Player
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMatchClicked: (matchId: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val matches by viewModel.matches.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Shuttlerr") })
        },
    ) { padding ->
        if (matches.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No matches yet.", style = MaterialTheme.typography.bodyLarge)
                Text("Record a match on your watch!", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(matches) { match ->
                    MatchSummaryCard(match = match, onClick = { onMatchClicked(match.id) })
                }
            }
        }
    }
}

@Composable
private fun MatchSummaryCard(match: Match, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val dateStr = SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault())
                .format(Date(match.startedAtMs))
            Text(text = dateStr, style = MaterialTheme.typography.labelSmall)

            val gamesNeeded = (match.totalGames / 2) + 1
            val gamesWonA = match.games.count { it.winnerPlayer == Player.A }
            val gamesWonB = match.games.count { it.winnerPlayer == Player.B }
            val resultStr = "A $gamesWonA – $gamesWonB B"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = resultStr, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = match.format.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val duration = (match.endedAtMs ?: 0L) - match.startedAtMs
            if (duration > 0) {
                val mins = TimeUnit.MILLISECONDS.toMinutes(duration)
                Text(text = "${mins}m", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
