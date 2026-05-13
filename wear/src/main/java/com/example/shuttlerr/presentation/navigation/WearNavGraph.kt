package com.example.shuttlerr.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.shuttlerr.domain.model.Player
import com.example.shuttlerr.presentation.match.ActiveMatchScreen
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
