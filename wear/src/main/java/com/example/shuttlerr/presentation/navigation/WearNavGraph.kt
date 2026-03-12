package com.example.shuttlerr.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.shuttlerr.domain.model.Player
import com.example.shuttlerr.presentation.gamewon.GameWonScreen
import com.example.shuttlerr.presentation.match.ActiveMatchScreen
import com.example.shuttlerr.presentation.match.ActiveMatchViewModel
import com.example.shuttlerr.presentation.matchcomplete.MatchCompleteScreen
import com.example.shuttlerr.presentation.setup.SetupScreen

@Composable
fun WearNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "setup") {

        composable("setup") {
            SetupScreen(
                onMatchStarted = { matchId ->
                    navController.navigate("match/$matchId") {
                        popUpTo("setup") { inclusive = false }
                    }
                },
            )
        }

        composable(
            route = "match/{matchId}",
            arguments = listOf(navArgument("matchId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: return@composable
            ActiveMatchScreen(
                matchId = matchId,
                onGameWon = { gameNumber, winner, scoreA, scoreB, totalGames ->
                    navController.navigate(
                        "gamewon/$matchId/$gameNumber/${winner.name}/$scoreA/$scoreB/$totalGames"
                    )
                },
                onMatchWon = { mid, winner ->
                    navController.navigate("matchcomplete/$mid/${winner.name}") {
                        popUpTo("match/$mid") { inclusive = true }
                    }
                },
                onQuit = {
                    navController.navigate("setup") {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = "gamewon/{matchId}/{gameNumber}/{winner}/{scoreA}/{scoreB}/{totalGames}",
            arguments = listOf(
                navArgument("matchId") { type = NavType.StringType },
                navArgument("gameNumber") { type = NavType.IntType },
                navArgument("winner") { type = NavType.StringType },
                navArgument("scoreA") { type = NavType.IntType },
                navArgument("scoreB") { type = NavType.IntType },
                navArgument("totalGames") { type = NavType.IntType },
            ),
        ) { backStackEntry ->
            val args = backStackEntry.arguments!!
            val matchId = args.getString("matchId")!!
            val gameNumber = args.getInt("gameNumber")
            val winner = Player.valueOf(args.getString("winner")!!)
            val scoreA = args.getInt("scoreA")
            val scoreB = args.getInt("scoreB")
            val totalGames = args.getInt("totalGames")

            // Reuse the existing ActiveMatchViewModel — do NOT recreate it on continue,
            // otherwise advanceToNextGame() can't persist game N+1 to Room.
            val matchEntry = remember(navController) {
                navController.getBackStackEntry("match/$matchId")
            }
            val matchViewModel: ActiveMatchViewModel = hiltViewModel(matchEntry)

            GameWonScreen(
                gameNumber = gameNumber,
                winner = winner,
                scoreA = scoreA,
                scoreB = scoreB,
                totalGames = totalGames,
                onContinue = {
                    matchViewModel.advanceToNextGame()
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = "matchcomplete/{matchId}/{winner}",
            arguments = listOf(
                navArgument("matchId") { type = NavType.StringType },
                navArgument("winner") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val args = backStackEntry.arguments!!
            val matchId = args.getString("matchId")!!
            val winner = Player.valueOf(args.getString("winner")!!)
            MatchCompleteScreen(
                matchId = matchId,
                winner = winner,
                onSavedAndSynced = {
                    navController.navigate("setup") {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
