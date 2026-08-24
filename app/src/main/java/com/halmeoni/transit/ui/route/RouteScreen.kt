package com.halmeoni.transit.ui.route

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.halmeoni.transit.domain.model.RouteStep
import com.halmeoni.transit.domain.model.StepType
import com.halmeoni.transit.ui.components.BigButton
import com.halmeoni.transit.ui.components.ErrorScreen
import com.halmeoni.transit.ui.components.LoadingScreen
import com.halmeoni.transit.ui.components.TopBar
import com.halmeoni.transit.ui.theme.HomeButtonGreen

@Composable
fun RouteScreen(
    destinationName: String,
    viewModel: RouteViewModel,
    onBack: () -> Unit,
    onGoHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(destinationName) {
        viewModel.loadRoute(destinationName)
    }

    if (uiState.isLoading) {
        LoadingScreen(
            message = "$destinationName 가는 길을\n찾고 있어요...",
            onCancel = onBack
        )
        return
    }

    if (uiState.errorMessage != null) {
        ErrorScreen(
            errorMessage = uiState.errorMessage!!,
            onRetry = { viewModel.loadRoute(destinationName) },
            onGoHome = onGoHome
        )
        return
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "$destinationName 가는 길",
                onBackClick = onBack,
                onHomeClick = onGoHome
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
        ) {
            val route = uiState.currentDisplayRoute

            if (route != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                        if (uiState.totalRouteCount > 1) {
                            Text(
                                text = "경로 ${uiState.currentRouteIndex + 1}/${uiState.totalRouteCount}",
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
                    if (uiState.totalRouteCount > 1) {
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
