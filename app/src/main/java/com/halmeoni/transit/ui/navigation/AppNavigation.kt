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
import com.halmeoni.transit.data.location.LocationProvider
import com.halmeoni.transit.data.repository.DestinationRepository
import com.halmeoni.transit.data.repository.SettingsRepository
import com.halmeoni.transit.domain.ApiUsageTracker
import com.halmeoni.transit.ui.admin.AdminScreen
import com.halmeoni.transit.ui.admin.AdminViewModel
import com.halmeoni.transit.ui.admin.PinInputScreen
import com.halmeoni.transit.ui.confirm.ConfirmScreen
import com.halmeoni.transit.ui.home.HomeScreen
import com.halmeoni.transit.ui.home.HomeViewModel
import com.halmeoni.transit.ui.route.RouteScreen
import com.halmeoni.transit.ui.route.RouteViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object Routes {
    const val HOME = "home"
    const val CONFIRM = "confirm/{destinationName}"
    const val ROUTE = "route/{destinationName}"
    const val PIN_INPUT = "pin_input"
    const val ADMIN = "admin"

    fun buildConfirmRoute(destinationName: String) = "confirm/$destinationName"
    fun buildRouteRoute(destinationName: String) = "route/$destinationName"
}

class AppViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    private val sharedPrefs by lazy {
        context.getSharedPreferences("halmeoni_transit_prefs", Context.MODE_PRIVATE)
    }
    private val destinationRepository by lazy { DestinationRepository(sharedPrefs) }
    private val settingsRepository by lazy { SettingsRepository(sharedPrefs) }
    private val apiUsageTracker by lazy { ApiUsageTracker(sharedPrefs) }
    private val locationProvider by lazy {
        LocationProvider(context, LocationServices.getFusedLocationProviderClient(context))
    }

    private val odsayApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.odsay.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OdsayApiService::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(destinationRepository) as T
            }
            modelClass.isAssignableFrom(RouteViewModel::class.java) -> {
                RouteViewModel(
                    odsayApiService = odsayApiService,
                    locationProvider = locationProvider,
                    destinationRepository = destinationRepository,
                    settingsRepository = settingsRepository,
                    apiUsageTracker = apiUsageTracker
                ) as T
            }
            modelClass.isAssignableFrom(AdminViewModel::class.java) -> {
                AdminViewModel(settingsRepository, apiUsageTracker) as T
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
                    navController.navigate(Routes.buildConfirmRoute(dest.displayName))
                },
                onGoHomeSelected = {
                    navController.navigate(Routes.buildConfirmRoute("우리 집"))
                },
                onNavigateToPin = {
                    navController.navigate(Routes.PIN_INPUT)
                }
            )
        }

        composable(
            route = Routes.CONFIRM,
            arguments = listOf(navArgument("destinationName") { type = NavType.StringType })
        ) { backStackEntry ->
            val destinationName = backStackEntry.arguments?.getString("destinationName") ?: ""
            ConfirmScreen(
                destinationName = destinationName,
                onConfirm = {
                    navController.navigate(Routes.buildRouteRoute(destinationName))
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
            route = Routes.ROUTE,
            arguments = listOf(navArgument("destinationName") { type = NavType.StringType })
        ) { backStackEntry ->
            val destinationName = backStackEntry.arguments?.getString("destinationName") ?: ""
            RouteScreen(
                destinationName = destinationName,
                viewModel = routeViewModel,
                onBack = {
                    navController.popBackStack()
                },
                onGoHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
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
