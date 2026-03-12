package com.example.shuttlerr.presentation.matchcomplete

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.shuttlerr.domain.model.Player
import java.util.concurrent.TimeUnit

@Composable
fun MatchCompleteScreen(
    matchId: String,
    winner: Player,
    onSavedAndSynced: () -> Unit,
    viewModel: MatchCompleteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberTransformingLazyColumnState()

    AppScaffold {
        ScreenScaffold(
            scrollState = listState,
            edgeButton = {
                EdgeButton(
                    onClick = { viewModel.saveAndSync(onSavedAndSynced) },
                    enabled = !uiState.isSyncing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(if (uiState.isSyncing) "Syncing…" else "Save →")
                }
            },
        ) { contentPadding ->
            TransformingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
            ) {
                item {
                    Text(
                        text = "Match over",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        textAlign = TextAlign.Center,
                    )
                }

                item {
                    Text(
                        text = "${winner.name} wins",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }

                item {
                    Text(
                        text = "${uiState.gamesWonA} – ${uiState.gamesWonB}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        textAlign = TextAlign.Center,
                    )
                }

                val games = uiState.match?.games.orEmpty()
                items(games.size) { index ->
                    val game = games[index]
                    val gameWinner = game.winnerPlayer
                    Text(
                        text = buildString {
                            append("G${game.gameNumber}  ")
                            if (gameWinner == Player.A) append("● ") else append("  ")
                            append("${game.scoreA}")
                            append(" – ")
                            append("${game.scoreB}")
                            if (gameWinner == Player.B) append(" ●")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        textAlign = TextAlign.Center,
                    )
                }

                item {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(uiState.durationMs)
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(uiState.durationMs) % 60
                    Text(
                        text = "${uiState.totalRallies} rallies · ${minutes}m ${seconds}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
