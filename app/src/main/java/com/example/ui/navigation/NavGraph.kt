package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.MawaeeViewModel
import com.example.ui.screens.AddAppointmentScreen
import com.example.ui.screens.MainScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AddAppointment : Screen("add_appointment")
}

@Composable
fun AppNavGraph(
    viewModel: MawaeeViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            MainScreen(
                viewModel = viewModel,
                navController = navController
            )
        }
        composable(Screen.AddAppointment.route) {
            AddAppointmentScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
    }
}
