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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.example.shuttlerr.domain.model.initials
import com.example.shuttlerr.domain.model.teamName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(
    onMatchStarted: (matchId: String) -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val totalSteps = viewModel.maxStep + 1
    val onFinalStep = viewModel.currentStep == viewModel.maxStep
    val players by viewModel.players.collectAsState()

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
                                fun autoAdvance() {
                                    if (step < viewModel.maxStep) scope.launch {
                                        delay(350)
                                        viewModel.nextStep()
                                    }
                                }

                                when {
                                    step == 0 -> {
                                        Text("Format?", style = MaterialTheme.typography.titleMedium)
                                        OptionRow(
                                            options = listOf("Singles", "Doubles"),
                                            selected = if (viewModel.format == GameFormat.SINGLES) 0 else 1,
                                            buttonHeight = 68.dp,
                                            onSelect = {
                                                viewModel.format = if (it == 0) GameFormat.SINGLES else GameFormat.DOUBLES
                                                autoAdvance()
                                            },
                                        )
                                    }
                                    step == 1 -> {
                                        Text("Best of?", style = MaterialTheme.typography.titleMedium)
                                        GamesGrid(viewModel, onSelected = { autoAdvance() })
                                    }
                                    viewModel.isPlayerStep(step) -> {
                                        Text(viewModel.playerStepLabel(step), style = MaterialTheme.typography.titleMedium)
                                        val slot = viewModel.currentPickSlot(step)
                                        val currentSelection = when (slot) {
                                            PickSlot.A1 -> viewModel.selectedA1
                                            PickSlot.A2 -> viewModel.selectedA2
                                            PickSlot.B1 -> viewModel.selectedB1
                                            PickSlot.B2 -> viewModel.selectedB2
                                        }
                                        PlayerPicker(
                                            players = players,
                                            selected = currentSelection,
                                            onSelect = { name ->
                                                viewModel.selectPlayer(slot, name)
                                                autoAdvance()
                                            },
                                            onAddPlayer = { name -> viewModel.addPlayer(name) },
                                        )
                                    }
                                    step == viewModel.serverStep() -> {
                                        Text("First server?", style = MaterialTheme.typography.titleMedium)
                                        val nameA = if (viewModel.format == GameFormat.DOUBLES)
                                            teamName(viewModel.selectedA1.ifBlank { "A1" }, viewModel.selectedA2.ifBlank { "A2" })
                                        else
                                            viewModel.selectedA1.ifBlank { "A1" }.initials()
                                        val nameB = if (viewModel.format == GameFormat.DOUBLES)
                                            teamName(viewModel.selectedB1.ifBlank { "B1" }, viewModel.selectedB2.ifBlank { "B2" })
                                        else
                                            viewModel.selectedB1.ifBlank { "B1" }.initials()
                                        OptionRow(
                                            options = listOf(nameA, nameB),
                                            selected = if (viewModel.initialServer == Player.A) 0 else 1,
                                            buttonHeight = 48.dp,
                                            onSelect = {
                                                viewModel.initialServer = if (it == 0) Player.A else Player.B
                                                autoAdvance()
                                            },
                                        )
                                    }
                                    else -> {
                                        // Doubles only: pick which player on serving team serves first
                                        Text("Who serves?", style = MaterialTheme.typography.titleMedium)
                                        val (p1, p2) = if (viewModel.initialServer == Player.A)
                                            viewModel.selectedA1.ifBlank { "A1" } to viewModel.selectedA2.ifBlank { "A2" }
                                        else
                                            viewModel.selectedB1.ifBlank { "B1" } to viewModel.selectedB2.ifBlank { "B2" }
                                        OptionRow(
                                            options = listOf(p1.initials(), p2.initials()),
                                            selected = if (viewModel.initialServerSlot == DoublesSlot.ONE) 0 else 1,
                                            buttonHeight = 48.dp,
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
private fun PlayerPicker(
    players: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onAddPlayer: (String) -> Unit,
) {
    var addingNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Auto-focus the text field whenever the add-player input appears
    LaunchedEffect(addingNew) {
        if (addingNew) focusRequester.requestFocus()
    }

    fun submit() {
        if (newName.isNotBlank()) {
            onAddPlayer(newName)
            onSelect(newName.trim())
            newName = ""
            addingNew = false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (addingNew) {
            // Inline text input — use keyboard Done as primary submit action
            val inputBackground = MaterialTheme.colorScheme.surfaceContainer
            val inputContent = MaterialTheme.colorScheme.onSurface
            BasicTextField(
                value = newName,
                onValueChange = { newName = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.labelMedium.copy(color = inputContent),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(inputBackground)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
                decorationBox = { inner ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        if (newName.isEmpty()) {
                            Text("Name…", style = MaterialTheme.typography.labelMedium, color = inputContent.copy(alpha = 0.4f))
                        }
                        inner()
                    }
                },
            )
            OptionButton(
                label = "Cancel",
                selected = false,
                onClick = { newName = ""; addingNew = false },
                modifier = Modifier.fillMaxWidth().height(36.dp),
            )
        } else {
            // Scrollable player list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    OptionButton(
                        label = "+ New",
                        selected = false,
                        onClick = { addingNew = true },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                    )
                }
                items(players) { name ->
                    OptionButton(
                        label = name,
                        selected = name == selected,
                        onClick = { onSelect(name) },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                    )
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
internal fun OptionButton(
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
