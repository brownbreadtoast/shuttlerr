package com.example.shuttlerr.presentation.matchdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    matchId: String,
    onBack: () -> Unit,
    viewModel: MatchDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Match Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            item {
                Spacer(Modifier.height(8.dp))
                val winner = uiState.winner
                Text(
                    text = if (winner != null) "${winner.name} wins  ${uiState.gamesWonA}–${uiState.gamesWonB}"
                    else "In progress",
                    style = MaterialTheme.typography.titleLarge,
                )
                val mins = TimeUnit.MILLISECONDS.toMinutes(uiState.durationMs)
                Text(
                    text = "${uiState.totalRallies} rallies · ${mins}m",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Per-game scores
            item {
                SectionHeader("Games")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    uiState.match?.games.orEmpty().forEach { game ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Game ${game.gameNumber}", style = MaterialTheme.typography.bodyMedium)
                            Text("${game.scoreA} – ${game.scoreB}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = game.winnerPlayer?.let { "${it.name} wins" } ?: "–",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            // Rally duration histogram
            if (uiState.rallyDurationBuckets.isNotEmpty()) {
                item {
                    SectionHeader("Rally Duration Distribution")
                    val modelProducer = remember { CartesianChartModelProducer() }
                    val counts = uiState.rallyDurationBuckets.map { it.count.toFloat() }
                    LaunchedEffect(counts) {
                        modelProducer.runTransaction {
                            columnSeries { series(counts) }
                        }
                    }
                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberColumnCartesianLayer(),
                            startAxis = VerticalAxis.rememberStart(),
                            bottomAxis = HorizontalAxis.rememberBottom(),
                        ),
                        modelProducer = modelProducer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                    )
                }
            }

            // HR over time
            if (uiState.hrData.isNotEmpty()) {
                item {
                    SectionHeader("Heart Rate")
                    val modelProducer = remember { CartesianChartModelProducer() }
                    val hrValues = uiState.hrData.map { it.second.toFloat() }
                    LaunchedEffect(hrValues) {
                        modelProducer.runTransaction {
                            lineSeries { series(hrValues) }
                        }
                    }
                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberLineCartesianLayer(),
                            startAxis = VerticalAxis.rememberStart(),
                            bottomAxis = HorizontalAxis.rememberBottom(),
                        ),
                        modelProducer = modelProducer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                    )
                }
            }

            // Service win stats
            uiState.serverWinStats?.let { stats ->
                item {
                    SectionHeader("Service Win Stats")
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatRow("A won as server", stats.aWonAsServer)
                        StatRow("A won as receiver", stats.aWonAsReceiver)
                        StatRow("B won as server", stats.bWonAsServer)
                        StatRow("B won as receiver", stats.bWonAsReceiver)
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    }
}

@Composable
private fun StatRow(label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text("$value", style = MaterialTheme.typography.bodyMedium)
    }
}
