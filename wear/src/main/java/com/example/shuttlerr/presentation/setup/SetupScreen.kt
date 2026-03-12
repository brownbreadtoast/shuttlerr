package com.example.shuttlerr.presentation.setup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.shuttlerr.domain.model.DoublesSlot
import com.example.shuttlerr.domain.model.GameFormat
import com.example.shuttlerr.domain.model.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(
    onMatchStarted: (matchId: String) -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val totalSteps = if (viewModel.format == GameFormat.DOUBLES) 4 else 3
    val onFinalStep = viewModel.currentStep == totalSteps - 1

    BackHandler(enabled = viewModel.currentStep > 0) {
        viewModel.prevStep()
    }

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
                        .padding(bottom = if (onFinalStep) 72.dp else 0.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StepDots(currentStep = viewModel.currentStep, totalSteps = totalSteps)

                        AnimatedContent(
                            targetState = viewModel.currentStep,
                            transitionSpec = {
                                val dir = if (targetState > initialState) 1 else -1
                                slideInHorizontally { w -> w * dir } togetherWith
                                    slideOutHorizontally { w -> -w * dir }
                            },
                            label = "setup_step",
                        ) { step ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                val question = when (step) {
                                    0 -> "Format?"
                                    1 -> "Best of?"
                                    2 -> "First server?"
                                    else -> "Who serves?"
                                }
                                Text(question, style = MaterialTheme.typography.titleMedium)

                                fun autoAdvance() {
                                    if (step < totalSteps - 1) scope.launch {
                                        delay(350)
                                        viewModel.nextStep()
                                    }
                                }

                                val buttonHeight = if (step == totalSteps - 1) 48.dp else 68.dp

                                when (step) {
                                    0 -> OptionRow(
                                        options = listOf("Singles", "Doubles"),
                                        selected = if (viewModel.format == GameFormat.SINGLES) 0 else 1,
                                        buttonHeight = buttonHeight,
                                        onSelect = {
                                            viewModel.format = if (it == 0) GameFormat.SINGLES else GameFormat.DOUBLES
                                            autoAdvance()
                                        },
                                    )
                                    1 -> GamesGrid(viewModel, onSelected = { autoAdvance() })
                                    2 -> OptionRow(
                                        options = listOf("A", "B"),
                                        selected = if (viewModel.initialServer == Player.A) 0 else 1,
                                        buttonHeight = buttonHeight,
                                        onSelect = {
                                            viewModel.initialServer = if (it == 0) Player.A else Player.B
                                            autoAdvance()
                                        },
                                    )
                                    else -> {
                                        // Doubles only: pick which player on the serving team serves first
                                        val teamLabel = viewModel.initialServer.name
                                        OptionRow(
                                            options = listOf("${teamLabel}1", "${teamLabel}2"),
                                            selected = if (viewModel.initialServerSlot == DoublesSlot.ONE) 0 else 1,
                                            buttonHeight = buttonHeight,
                                            onSelect = {
                                                viewModel.initialServerSlot =
                                                    if (it == 0) DoublesSlot.ONE else DoublesSlot.TWO
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (onFinalStep) {
                    EdgeButton(
                        onClick = { viewModel.createMatch(onMatchStarted) },
                        modifier = Modifier.align(Alignment.BottomCenter),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text("→", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepDots(currentStep: Int, totalSteps: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceContainer
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until totalSteps) {
            Box(
                modifier = Modifier
                    .size(if (i == currentStep) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            i == currentStep -> primary
                            i < currentStep -> primary.copy(alpha = 0.4f)
                            else -> surface
                        }
                    ),
            )
        }
    }
}

@Composable
private fun OptionRow(
    options: List<String>,
    selected: Int,
    buttonHeight: Dp,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEachIndexed { index, label ->
            OptionButton(
                label = label,
                selected = selected == index,
                onClick = { onSelect(index) },
                modifier = Modifier
                    .weight(1f)
                    .height(buttonHeight),
            )
        }
    }
}

@Composable
private fun GamesGrid(viewModel: SetupViewModel, onSelected: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (n in 1..3) {
                OptionButton(
                    label = "$n",
                    selected = viewModel.totalGames == n,
                    onClick = {
                        viewModel.totalGames = n
                        onSelected()
                    },
                    modifier = Modifier.size(44.dp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (n in 4..5) {
                OptionButton(
                    label = "$n",
                    selected = viewModel.totalGames == n,
                    onClick = {
                        viewModel.totalGames = n
                        onSelected()
                    },
                    modifier = Modifier.size(44.dp),
                )
            }
        }
    }
}

@Composable
private fun OptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = if (selected) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        },
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
