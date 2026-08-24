package com.halmeoni.transit.ui.route

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.halmeoni.transit.domain.model.RouteRequest
import com.halmeoni.transit.domain.model.RouteStep
import com.halmeoni.transit.domain.model.StepType
import com.halmeoni.transit.ui.components.BigButton
import com.halmeoni.transit.ui.components.ErrorScreen
import com.halmeoni.transit.ui.components.LoadingScreen
import com.halmeoni.transit.ui.components.TopBar
import com.halmeoni.transit.ui.theme.HomeButtonGreen

@Composable
fun RouteScreen(
    request: RouteRequest,
    viewModel: RouteViewModel,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
    onNavigateToAdmin: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.loadRoute(request)
        }
    }

    LaunchedEffect(request) {
        if (request is RouteRequest.GoHome) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        viewModel.loadRoute(request)
    }

    when (val state = uiState) {
        is RouteUiState.Idle, is RouteUiState.Loading -> {
            val title = if (state is RouteUiState.Loading) state.destinationTitle else ""
            LoadingScreen(
                message = if (title.isNotBlank()) "$title 가는 길을\n찾고 있어요..." else "길을 찾고 있어요...",
                onCancel = onBack
            )
        }
        is RouteUiState.Error -> {
            Scaffold(
                topBar = {
                    TopBar(
                        title = if (state.destinationTitle.isNotBlank()) "${state.destinationTitle} 길찾기" else "길찾기 안내",
                        onBackClick = onBack,
                        onHomeClick = onGoHome
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    when (state.errorType) {
                        RouteErrorType.PERMISSION_REQUIRED -> {
                            BigButton(
                                text = "위치 권한 허용하기",
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                backgroundColor = MaterialTheme.colorScheme.primary,
                                minHeight = 88.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        RouteErrorType.LOCATION_SERVICE_DISABLED -> {
                            BigButton(
                                text = "위치 설정 열기",
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                },
                                backgroundColor = MaterialTheme.colorScheme.primary,
                                minHeight = 88.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        RouteErrorType.CONFIGURATION_REQUIRED -> {
                            if (onNavigateToAdmin != null) {
                                BigButton(
                                    text = "⚙️ 보호자 설정 열기",
                                    onClick = onNavigateToAdmin,
                                    backgroundColor = MaterialTheme.colorScheme.primary,
                                    minHeight = 88.dp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                        else -> {
                            BigButton(
                                text = "🔄 다시 시도",
                                onClick = { viewModel.retry() },
                                backgroundColor = MaterialTheme.colorScheme.primary,
                                minHeight = 88.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    BigButton(
                        text = "🏠 처음으로 돌아가기",
                        onClick = onGoHome,
                        backgroundColor = HomeButtonGreen,
                        minHeight = 88.dp
                    )
                }
            }
        }
        is RouteUiState.Success -> {
            val route = state.currentDisplayRoute

            Scaffold(
                topBar = {
                    TopBar(
                        title = "${state.destinationTitle} 가는 길",
                        onBackClick = onBack,
                        onHomeClick = onGoHome
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "총 약 ${route.totalTime}분 소요",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "도보 ${route.totalWalkDistance}m / 환승 ${route.transferCount}회",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            if (state.totalRouteCount > 1) {
                                Text(
                                    text = "경로 ${state.currentRouteIndex + 1}/${state.totalRouteCount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(route.steps) { index, step ->
                            StepCard(stepNumber = index + 1, step = step)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.totalRouteCount > 1) {
                            BigButton(
                                text = "🔄 다른 경로 보기",
                                onClick = { viewModel.toggleNextRoute() },
                                backgroundColor = MaterialTheme.colorScheme.secondary,
                                minHeight = 80.dp
                            )
                        }

                        BigButton(
                            text = "🏠 처음으로 돌아가기",
                            onClick = onGoHome,
                            backgroundColor = HomeButtonGreen,
                            minHeight = 88.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepCard(stepNumber: Int, step: RouteStep) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (step.type) {
                StepType.BUS -> "🚌"
                StepType.SUBWAY -> "🚇"
                StepType.WALK -> "🚶"
            }

            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${stepNumber}단계",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.padding(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$icon ${step.stepName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                when (step.type) {
                    StepType.BUS -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        if (step.startName.isNotBlank()) {
                            Text(text = "탑승 정류장: ${step.startName}", style = MaterialTheme.typography.bodyLarge)
                        }
                        if (!step.routeName.isNullOrBlank()) {
                            Text(text = "버스 번호: ${step.routeName}", style = MaterialTheme.typography.bodyLarge)
                        }
                        if (step.endName.isNotBlank()) {
                            val stopInfo = if (step.stationCount > 0) " (${step.stationCount}개 정거장)" else ""
                            Text(
                                text = "하차 정류장: ${step.endName}$stopInfo",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    StepType.SUBWAY -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        if (step.startName.isNotBlank()) {
                            Text(text = "탑승역: ${step.startName}", style = MaterialTheme.typography.bodyLarge)
                        }
                        if (!step.routeName.isNullOrBlank()) {
                            Text(text = "노선명: ${step.routeName}", style = MaterialTheme.typography.bodyLarge)
                        }
                        if (step.endName.isNotBlank()) {
                            val stopInfo = if (step.stationCount > 0) " (${step.stationCount}개 정거장)" else ""
                            Text(
                                text = "하차역: ${step.endName}$stopInfo",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    StepType.WALK -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        if (step.startName.isNotBlank() && step.endName.isNotBlank()) {
                            Text(text = "이동: ${step.startName} → ${step.endName}", style = MaterialTheme.typography.bodyLarge)
                        }
                        val timeOrDist = buildString {
                            if (step.sectionTime > 0) append("${step.sectionTime}분")
                            if (step.distance > 0) {
                                if (isNotEmpty()) append(" / ")
                                append("${step.distance}m")
                            }
                        }
                        if (timeOrDist.isNotBlank()) {
                            Text(text = timeOrDist, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
