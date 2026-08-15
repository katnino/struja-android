package ba.noniboy.struja.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ba.noniboy.struja.ui.screens.BillDetailScreen
import ba.noniboy.struja.ui.screens.DashboardScreen
import ba.noniboy.struja.ui.screens.MeterDetailScreen
import ba.noniboy.struja.ui.screens.NewReadingScreen
import ba.noniboy.struja.ui.screens.SettingsScreen
import kotlinx.serialization.Serializable

/**
 * Navigation destinations.
 * Each screen has a unique route with optional parameters.
 */
@Serializable
sealed class Screen {
    @Serializable
    data object Dashboard : Screen()

    @Serializable
    data class MeterDetail(val meterId: String) : Screen()

    @Serializable
    data class NewReading(val meterId: String) : Screen()

    @Serializable
    data class BillDetail(val billId: String) : Screen()

    @Serializable
    data object Settings : Screen()
}

/**
 * Root navigation graph for the app.
 */
@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    startDestination: Screen = Screen.Dashboard,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<Screen.Dashboard> {
            DashboardScreen(
                onMeterClick = { meterId ->
                    navController.navigate(Screen.MeterDetail(meterId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings)
                }
            )
        }

        composable<Screen.MeterDetail> { backStackEntry ->
            val meterDetail = backStackEntry.toRoute<Screen.MeterDetail>()
            MeterDetailScreen(
                meterId = meterDetail.meterId,
                onBack = { navController.popBackStack() },
                onAddReading = { navController.navigate(Screen.NewReading(meterDetail.meterId)) },
                onBillClick = { billId -> navController.navigate(Screen.BillDetail(billId)) },
                onSettingsClick = { navController.navigate(Screen.Settings) }
            )
        }

        composable<Screen.NewReading> { backStackEntry ->
            val newReading = backStackEntry.toRoute<Screen.NewReading>()
            NewReadingScreen(
                meterId = newReading.meterId,
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.BillDetail> { backStackEntry ->
            val billDetail = backStackEntry.toRoute<Screen.BillDetail>()
            BillDetailScreen(
                billId = billDetail.billId,
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
