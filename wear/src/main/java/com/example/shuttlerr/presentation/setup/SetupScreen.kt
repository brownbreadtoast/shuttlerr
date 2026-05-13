package com.example.shuttlerr.presentation.setup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

@Composable
fun SetupScreen(
    onMatchStarted: (matchId: String) -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
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
                    Text("Shuttlerr", style = MaterialTheme.typography.titleLarge)
                }
                EdgeButton(
                    onClick = { viewModel.createMatch(onMatchStarted) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("Play!", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
