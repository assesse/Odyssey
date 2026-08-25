package com.halmeoni.transit.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.location.LocationServices
import com.halmeoni.transit.data.api.OdsayApiService
import com.halmeoni.transit.data.location.FusedLocationProvider
import com.halmeoni.transit.data.location.LocationProvider
import com.halmeoni.transit.data.repository.DestinationRepository
import com.halmeoni.transit.data.repository.OdsayRouteRepository
import com.halmeoni.transit.data.repository.SettingsRepository
import com.halmeoni.transit.domain.ApiUsageTracker
import com.halmeoni.transit.domain.model.RouteRequest
import com.halmeoni.transit.ui.admin.AdminScreen
import com.halmeoni.transit.ui.admin.AdminViewModel
import com.halmeoni.transit.ui.admin.PinInputScreen
import com.halmeoni.transit.ui.confirm.ConfirmScreen
import com.halmeoni.transit.ui.home.HomeScreen
import com.halmeoni.transit.ui.home.HomeViewModel
import com.halmeoni.transit.ui.route.RouteScreen
import com.halmeoni.transit.ui.route.RouteViewModel
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object Routes {
    const val HOME = "home"
    const val CONFIRM_DESTINATION = "confirm/destination/{destinationId}"
    const val CONFIRM_HOME = "confirm/home"
    const val ROUTE_DESTINATION = "route/destination/{destinationId}"
    const val ROUTE_HOME = "route/home"
    const val PIN_INPUT = "pin_input"
    const val ADMIN = "admin"

    fun buildConfirmDestination(destinationId: String) = "confirm/destination/$destinationId"
    fun buildConfirmHome() = CONFIRM_HOME
    fun buildRouteDestination(destinationId: String) = "route/destination/$destinationId"
    fun buildRouteHome() = ROUTE_HOME
}

class AppViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    private val sharedPrefs by lazy {
        context.getSharedPreferences("halmeoni_transit_prefs", Context.MODE_PRIVATE)
    }
    val destinationRepository by lazy { DestinationRepository(sharedPrefs) }
    val settingsRepository by lazy { SettingsRepository(sharedPrefs) }
    val apiUsageTracker by lazy { ApiUsageTracker(sharedPrefs) }
    val locationProvider by lazy {
        FusedLocationProvider(context, LocationServices.getFusedLocationProviderClient(context))
    }

    private val odsayApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.odsay.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OdsayApiService::class.java)
    }

    val routeRepository by lazy {
        OdsayRouteRepository(odsayApiService)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(destinationRepository) as T
            }
            modelClass.isAssignableFrom(RouteViewModel::class.java) -> {
                RouteViewModel(
                    routeRepository = routeRepository,
                    locationProvider = locationProvider,
                    destinationRepository = destinationRepository,
                    settingsRepository = settingsRepository,
                    apiUsageTracker = apiUsageTracker
                ) as T
            }
            modelClass.isAssignableFrom(AdminViewModel::class.java) -> {
                AdminViewModel(
                    settingsRepository = settingsRepository,
                    destinationRepository = destinationRepository,
                    apiUsageTracker = apiUsageTracker
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val factory = AppViewModelFactory(context)

    val homeViewModel: HomeViewModel = viewModel(factory = factory)
    val routeViewModel: RouteViewModel = viewModel(factory = factory)
    val adminViewModel: AdminViewModel = viewModel(factory = factory)

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = homeViewModel,
                onSelectDestination = { dest ->
                    navController.navigate(Routes.buildConfirmDestination(dest.id))
                },
                onGoHomeSelected = {
                    navController.navigate(Routes.buildConfirmHome())
                },
                onNavigateToPin = {
                    navController.navigate(Routes.PIN_INPUT)
                }
            )
        }

        composable(
            route = Routes.CONFIRM_DESTINATION,
            arguments = listOf(navArgument("destinationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val destinationId = backStackEntry.arguments?.getString("destinationId") ?: ""
            val destination = factory.destinationRepository.getDestinationById(destinationId)
            val displayName = destination?.displayName ?: "목적지"

            ConfirmScreen(
                destinationName = displayName,
                onConfirm = {
                    navController.navigate(Routes.buildRouteDestination(destinationId))
                },
                onCancel = {
                    navController.popBackStack()
                },
                onGoHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }

        composable(Routes.CONFIRM_HOME) {
            ConfirmScreen(
                destinationName = "우리 집",
                onConfirm = {
                    navController.navigate(Routes.buildRouteHome())
                },
                onCancel = {
                    navController.popBackStack()
                },
                onGoHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }

        composable(
            route = Routes.ROUTE_DESTINATION,
            arguments = listOf(navArgument("destinationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val destinationId = backStackEntry.arguments?.getString("destinationId") ?: ""
            RouteScreen(
                request = RouteRequest.ToDestination(destinationId),
                viewModel = routeViewModel,
                onBack = {
                    navController.popBackStack()
                },
                onGoHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onNavigateToAdmin = {
                    navController.navigate(Routes.PIN_INPUT)
                }
            )
        }

        composable(Routes.ROUTE_HOME) {
            RouteScreen(
                request = RouteRequest.GoHome,
                viewModel = routeViewModel,
                onBack = {
                    navController.popBackStack()
                },
                onGoHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onNavigateToAdmin = {
                    navController.navigate(Routes.PIN_INPUT)
                }
            )
        }

        composable(Routes.PIN_INPUT) {
            PinInputScreen(
                viewModel = adminViewModel,
                onAuthSuccess = {
                    navController.navigate(Routes.ADMIN) {
                        popUpTo(Routes.PIN_INPUT) { inclusive = true }
                    }
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.ADMIN) {
            AdminScreen(
                viewModel = adminViewModel,
                onGoHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }
    }
}
