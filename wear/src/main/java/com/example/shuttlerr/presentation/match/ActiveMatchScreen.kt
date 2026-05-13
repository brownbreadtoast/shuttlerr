package com.example.shuttlerr.presentation.match

import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.shuttlerr.domain.model.Player

@Composable
fun ActiveMatchScreen(
    matchId: String,
    onMatchWon: (matchId: String, winner: Player) -> Unit,
    onQuit: () -> Unit,
    viewModel: ActiveMatchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        viewModel.resetRallyTimer()
        onDispose { }
    }

    val window = (LocalContext.current as? android.app.Activity)?.window
    DisposableEffect(Unit) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    LaunchedEffect(uiState?.matchWinner) {
        val state = uiState ?: return@LaunchedEffect
        if (state.matchWinner != null) onMatchWon(matchId, state.matchWinner)
    }

    val state = uiState ?: return

    AppScaffold {
        ScreenScaffold { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                Text(
                    text = "Me  ${state.scoreA} – ${state.scoreB}  Op",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Button(
                        onClick = { viewModel.recordPoint(Player.A) },
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B5E20),
                            contentColor = Color.White,
                        ),
                    ) {
                        Text("W", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Button(
                        onClick = { viewModel.recordPoint(Player.B) },
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB71C1C),
                            contentColor = Color.White,
                        ),
                    ) {
                        Text("L", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .padding(horizontal = 24.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Button(
                        onClick = { viewModel.undoLastPoint() },
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Text("↺", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = onQuit,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Text("⌂", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
