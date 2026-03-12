package com.example.shuttlerr.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.shuttlerr.presentation.home.HomeScreen
import com.example.shuttlerr.presentation.matchdetail.MatchDetailScreen

@Composable
fun MobileNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(
                onMatchClicked = { matchId ->
                    navController.navigate("matchdetail/$matchId")
                },
            )
        }

        composable(
            route = "matchdetail/{matchId}",
            arguments = listOf(navArgument("matchId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: return@composable
            MatchDetailScreen(
                matchId = matchId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
