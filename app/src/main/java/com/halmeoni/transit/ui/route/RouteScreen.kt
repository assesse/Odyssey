package com.halmeoni.transit.ui.route

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halmeoni.transit.domain.model.RealtimeArrival
import com.halmeoni.transit.domain.model.RealtimeStatus
import com.halmeoni.transit.domain.model.RouteRequest
import com.halmeoni.transit.domain.model.RouteStep
import com.halmeoni.transit.domain.model.StepType
import com.halmeoni.transit.ui.components.BigButton
import com.halmeoni.transit.ui.components.ErrorScreen
import com.halmeoni.transit.ui.components.LoadingScreen
import com.halmeoni.transit.ui.components.TopBar
import com.halmeoni.transit.ui.theme.ActionRed
import com.halmeoni.transit.ui.theme.HomeButtonGreen

@Composable
fun RouteScreen(
    request: RouteRequest,
    viewModel: RouteViewModel,
    onGoHome: () -> Unit,
    onBack: () -> Unit,
    onNavigateToAdmin: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.loadRoute(request)
        }
    }

    LaunchedEffect(request) {
        viewModel.loadRoute(request)
    }

    when (val state = uiState) {
        is RouteUiState.Idle -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }
        is RouteUiState.Loading -> {
            LoadingScreen(
                message = "${state.destinationTitle} 가는\n가장 편한 길을 찾고 있어요...",
                onCancel = {
                    viewModel.cancelSearch()
                    onGoHome()
                }
            )
        }
        is RouteUiState.Error -> {
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
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.displayLarge,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    when (state.errorType) {
                        RouteErrorType.PERMISSION_REQUIRED -> {
                            BigButton(
                                text = "📍 위치 권한 허용하기",
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
                                text = "🔄 다시 확인",
                                onClick = { viewModel.retry() },
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
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // 상단 요약 카드 (총 소요시간, 도보/환승 정보 + 실시간 새로고침 버튼)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "총 약 ${route.totalTime}분 소요",
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (state.totalRouteCount > 1) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "경로 ${state.currentRouteIndex + 1}/${state.totalRouteCount}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // 실시간 새로고침 버튼
                                    OutlinedButton(
                                        onClick = { viewModel.refreshRealtime() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        if (state.isRealtimeLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.height(16.dp).width(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text("🔄 실시간 갱신", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "도보 ${route.totalWalkDistance}m · 환승 ${route.transferCount}회",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                                color = Color.DarkGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 경로 단계별 안내 리스트
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        itemsIndexed(route.steps) { index, step ->
                            StepCard(
                                stepNumber = index + 1,
                                step = step,
                                realtimeStatus = state.realtimeStatusMap[index]
                            )
                        }
                    }

                    // 하단 버튼 바 (1/2 분할: 좌측 '다른 경로', 우측 '처음으로')
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (state.totalRouteCount > 1) {
                            Button(
                                onClick = { viewModel.toggleNextRoute() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "🔄 다른 경로",
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Button(
                            onClick = onGoHome,
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HomeButtonGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "🏠 처음으로",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getCircleNumber(number: Int): String {
    return when (number) {
        1 -> "①"
        2 -> "②"
        3 -> "③"
        4 -> "④"
        5 -> "⑤"
        6 -> "⑥"
        7 -> "⑦"
        8 -> "⑧"
        9 -> "⑨"
        10 -> "⑩"
        else -> "($number)"
    }
}

fun getTransitLineColor(step: RouteStep): Color {
    return when (step.type) {
        StepType.BUS -> {
            when (step.lineType) {
                11 -> Color(0xFF2563EB) // 간선 (파랑)
                12 -> Color(0xFF16A34A) // 지선 (초록)
                13 -> Color(0xFFCA8A04) // 순환 (노랑)
                14, 4, 6, 15 -> Color(0xFFDC2626) // 광역/직행 (빨강)
                3 -> Color(0xFF65A30D)  // 마을버스 (연두)
                else -> {
                    val busNo = step.routeName ?: ""
                    if (busNo.startsWith("M") || busNo.startsWith("9")) Color(0xFFDC2626)
                    else if (busNo.contains("마을") || busNo.contains("종로") || busNo.contains("마포") || busNo.contains("강남")) Color(0xFF65A30D)
                    else if (busNo.length == 3) Color(0xFF2563EB)
                    else if (busNo.length == 4) Color(0xFF16A34A)
                    else Color(0xFF2563EB)
                }
            }
        }
        StepType.SUBWAY -> {
            when (step.subwayCode) {
                1 -> Color(0xFF0052A4)
                2 -> Color(0xFF00A84D)
                3 -> Color(0xFFEF7C1C)
                4 -> Color(0xFF00A5DE)
                5 -> Color(0xFF996CAC)
                6 -> Color(0xFFCD7C2F)
                7 -> Color(0xFF747F00)
                8 -> Color(0xFFE6186C)
                9 -> Color(0xFFBDB092)
                101 -> Color(0xFF0090D2) // 공항철도
                102 -> Color(0xFFD4003B) // 신분당선
                104 -> Color(0xFF77C4A3) // 경의중앙선
                116 -> Color(0xFFF5A200) // 수인분당선
                else -> {
                    val name = step.routeName ?: ""
                    if (name.contains("1호선")) Color(0xFF0052A4)
                    else if (name.contains("2호선")) Color(0xFF00A84D)
                    else if (name.contains("3호선")) Color(0xFFEF7C1C)
                    else if (name.contains("4호선")) Color(0xFF00A5DE)
                    else if (name.contains("5호선")) Color(0xFF996CAC)
                    else if (name.contains("6호선")) Color(0xFFCD7C2F)
                    else if (name.contains("7호선")) Color(0xFF747F00)
                    else if (name.contains("8호선")) Color(0xFFE6186C)
                    else if (name.contains("9호선")) Color(0xFFBDB092)
                    else if (name.contains("신분당")) Color(0xFFD4003B)
                    else if (name.contains("수인분당")) Color(0xFFF5A200)
                    else if (name.contains("경의중앙")) Color(0xFF77C4A3)
                    else if (name.contains("공항철도")) Color(0xFF0090D2)
                    else Color(0xFF0052A4)
                }
            }
        }
        else -> Color.DarkGray
    }
}

@Composable
fun StepCard(
    stepNumber: Int,
    step: RouteStep,
    realtimeStatus: RealtimeStatus? = null
) {
    val circleNum = getCircleNumber(stepNumber)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            when (step.type) {
                StepType.WALK -> {
                    val distText = if (step.distance > 0) " (${Math.round(step.distance)}m)" else ""
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = circleNum,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "도보 ${step.sectionTime}분$distText",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (step.startName.isNotBlank() && step.endName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "- ${step.startName} → ${step.endName}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                            color = Color.DarkGray
                        )
                    }
                }
                StepType.BUS -> {
                    val lineColor = getTransitLineColor(step)
                    val routeNo = step.routeName ?: "버스"

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = circleNum,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = lineColor,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = " $routeNo ",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "탑승",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "탑승 정류장",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = "- ${step.startName}",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )

                    // 실시간 도착 정보 카드 배지
                    if (realtimeStatus != null && realtimeStatus !is RealtimeStatus.NotRequested) {
                        Spacer(modifier = Modifier.height(8.dp))
                        RealtimeArrivalCard(status = realtimeStatus)
                    }

                    // 중간 화살표 및 이동 정류장 수
                    val stopCountText = if (step.stationCount > 0) "+${step.stationCount}개 정류장" else "이동"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "↓ ($stopCountText)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            fontWeight = FontWeight.Bold,
                            color = lineColor
                        )
                    }

                    Text(
                        text = "하차 정류장",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = "- ${step.endName}",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
                StepType.SUBWAY -> {
                    val lineColor = getTransitLineColor(step)
                    val routeName = step.routeName ?: "지하철"

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = circleNum,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = lineColor,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = " $routeName ",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "탑승",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "탑승역",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = "- ${step.startName}",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )

                    // 실시간 도착 정보 카드 배지
                    if (realtimeStatus != null && realtimeStatus !is RealtimeStatus.NotRequested) {
                        Spacer(modifier = Modifier.height(8.dp))
                        RealtimeArrivalCard(status = realtimeStatus)
                    }

                    // 중간 화살표 및 이동 역 수
                    val stationCountText = if (step.stationCount > 0) "+${step.stationCount}개 역" else "이동"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "↓ ($stationCountText)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            fontWeight = FontWeight.Bold,
                            color = lineColor
                        )
                    }

                    Text(
                        text = "하차역",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = "- ${step.endName}",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
                StepType.UNKNOWN -> {
                    Text(
                        text = "$circleNum 이동 정보 확인 필요",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RealtimeArrivalCard(status: RealtimeStatus) {
    when (status) {
        is RealtimeStatus.Loading -> {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(14.dp).width(14.dp),
                        strokeWidth = 2.dp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "실시간 도착 정보 확인 중...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )
                }
            }
        }
        is RealtimeStatus.Available -> {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF0FDF4),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🟢 실시간 도착 정보",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    when (val arr = status.arrival) {
                        is RealtimeArrival.Bus -> {
                            Text(
                                text = "▶ ${arr.firstMessage}",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                            if (!arr.secondMessage.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = arr.secondMessage,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                        is RealtimeArrival.Subway -> {
                            val destPart = if (arr.destinationName.isNotBlank()) " (${arr.destinationName})" else ""
                            Text(
                                text = "▶ ${arr.arrivalMessage}$destPart",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                            if (arr.currentPositionMsg.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "• 현재 위치: ${arr.currentPositionMsg}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }
                }
            }
        }
        is RealtimeStatus.Stale -> {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFFBEB),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "⚠️ 정보가 오래되었어요. [새로고침]을 눌러주세요.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309)
                    )
                }
            }
        }
        is RealtimeStatus.NoData -> {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚪ ${status.message}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
        is RealtimeStatus.Unsupported -> {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚪ ${status.message}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
        is RealtimeStatus.AuthenticationRequired -> {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚪ ${status.message}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
        is RealtimeStatus.NetworkError -> {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚪ ${status.message}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
        is RealtimeStatus.NotRequested -> {}
    }
}
