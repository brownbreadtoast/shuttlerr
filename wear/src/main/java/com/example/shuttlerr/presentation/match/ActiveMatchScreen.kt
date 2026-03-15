package com.example.shuttlerr.presentation.match

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.shuttlerr.domain.model.DoublesSlot
import com.example.shuttlerr.domain.model.Player
import com.example.shuttlerr.domain.model.initials
import com.example.shuttlerr.domain.model.teamName

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun ActiveMatchScreen(
    matchId: String,
    onGameWon: (gameNumber: Int, winner: Player, scoreA: Int, scoreB: Int, totalGames: Int) -> Unit,
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

    LaunchedEffect(uiState?.gameWinner, uiState?.matchWinner) {
        val state = uiState ?: return@LaunchedEffect
        when {
            state.matchWinner != null -> onMatchWon(matchId, state.matchWinner)
            state.gameWinner != null -> onGameWon(
                state.currentGameNumber, state.gameWinner,
                state.scoreA, state.scoreB, state.totalGames,
            )
        }
    }

    val state = uiState ?: return

    // Compute display labels per team
    val names = state.playerNames
    val labelA = if (state.isDoubles) teamName(names.a1, names.a2) else names.a1.initials()
    val labelB = if (state.isDoubles) teamName(names.b1, names.b2) else names.b1.initials()

    val serverName = if (state.isDoubles) {
        when {
            state.currentServer == Player.A && state.currentServerSlot == DoublesSlot.ONE -> names.a1.initials()
            state.currentServer == Player.A -> names.a2.initials()
            state.currentServerSlot == DoublesSlot.ONE -> names.b1.initials()
            else -> names.b2.initials()
        }
    } else names.a1.initials().takeIf { state.currentServer == Player.A } ?: names.b1.initials()

    // Court position rows: left-court player → right-court player for each team
    val courtRowA: Pair<String, String>? = if (state.isDoubles) {
        val leftA = if (state.rightCourtSlotA == DoublesSlot.ONE) names.a2.initials() else names.a1.initials()
        val rightA = if (state.rightCourtSlotA == DoublesSlot.ONE) names.a1.initials() else names.a2.initials()
        leftA to rightA
    } else null

    val courtRowB: Pair<String, String>? = if (state.isDoubles) {
        val leftB = if (state.rightCourtSlotB == DoublesSlot.ONE) names.b2.initials() else names.b1.initials()
        val rightB = if (state.rightCourtSlotB == DoublesSlot.ONE) names.b1.initials() else names.b2.initials()
        leftB to rightB
    } else null

    AppScaffold {
        ScreenScaffold { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                // Compact top row: game · status · HR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "G${state.currentGameNumber}/${state.totalGames}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = when {
                            state.showMidGameSwitchPrompt -> "Switch sides!"
                            state.isDeuce -> "DEUCE"
                            state.isDoubles -> "$serverName serves · ${state.serviceSide.name.lowercase()}"
                            else -> "${state.serviceSide.name.lowercase()} · ${state.serverCourtSide.name.lowercase()}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            state.showMidGameSwitchPrompt -> MaterialTheme.colorScheme.tertiary
                            state.isDeuce -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        state.heartRateBpm?.let {
                            Text(
                                text = "❤ $it",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } ?: Spacer(Modifier.width(1.dp))
                    }
                }

                // Score buttons — side by side
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ScoreButton(
                        playerLabel = labelA,
                        score = state.scoreA,
                        isServer = state.currentServer == Player.A,
                        courtRow = courtRowA,
                        onClick = { viewModel.recordPoint(Player.A) },
                        onLongClick = { viewModel.undoLastPoint() },
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                    )
                    ScoreButton(
                        playerLabel = labelB,
                        score = state.scoreB,
                        isServer = state.currentServer == Player.B,
                        courtRow = courtRowB,
                        onClick = { viewModel.recordPoint(Player.B) },
                        onLongClick = { viewModel.undoLastPoint() },
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                    )
                }

                // Bottom action strip: reset | quit
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .padding(horizontal = 24.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Button(
                        onClick = { viewModel.resetGame() },
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
private fun ScoreButton(
    playerLabel: String,
    score: Int,
    isServer: Boolean,
    courtRow: Pair<String, String>?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor: Color
    val contentColor: Color
    if (isServer) {
        containerColor = MaterialTheme.colorScheme.primaryContainer
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        containerColor = MaterialTheme.colorScheme.surfaceContainer
        contentColor = MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (isServer) "● $playerLabel" else playerLabel,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
            AnimatedContent(
                targetState = score,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInVertically { h -> h } + fadeIn() togetherWith
                            slideOutVertically { h -> -h } + fadeOut()
                    } else {
                        slideInVertically { h -> -h } + fadeIn() togetherWith
                            slideOutVertically { h -> h } + fadeOut()
                    }
                },
                label = "score_$playerLabel",
            ) { s ->
                Text(
                    text = "$s",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor,
                )
            }
            // Doubles: show left ← player | player → right court positions
            if (courtRow != null) {
                val (leftPlayer, rightPlayer) = courtRow
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "←$leftPlayer",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f),
                    )
                    Text(
                        text = "$rightPlayer→",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}
