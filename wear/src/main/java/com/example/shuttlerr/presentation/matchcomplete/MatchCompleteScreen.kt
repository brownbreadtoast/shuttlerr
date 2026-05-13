package com.example.shuttlerr.presentation.matchcomplete

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.shuttlerr.domain.model.Player

@Composable
fun MatchCompleteScreen(
    matchId: String,
    winner: Player,
    onSavedAndSynced: () -> Unit,
    viewModel: MatchCompleteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isWin = winner == Player.A

    AppScaffold {
        ScreenScaffold { contentPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 52.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isWin) "You Win!" else "You Lose!",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isWin) Color(0xFF4CAF50) else Color(0xFFF44336),
                        textAlign = TextAlign.Center,
                    )
                }
                EdgeButton(
                    onClick = { viewModel.saveAndSync(onSavedAndSynced) },
                    enabled = !uiState.isSyncing,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(if (uiState.isSyncing) "Saving…" else "Play Again!")
                }
            }
        }
    }
}
