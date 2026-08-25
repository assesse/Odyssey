package com.halmeoni.transit.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halmeoni.transit.domain.model.Destination
import com.halmeoni.transit.ui.components.BigButton
import com.halmeoni.transit.ui.theme.PrimaryBlue
import com.halmeoni.transit.ui.theme.HomeButtonGreen

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSelectDestination: (Destination) -> Unit,
    onGoHomeSelected: () -> Unit,
    onNavigateToPin: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.loadDestinations()
    }

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 상단 앱 제목 영역 (세련된 제목 헤더 UI, 5회 연속 탭 시 관리자 설정 진입)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { viewModel.onLogoTapped(onNavigateToPin) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Odyssey 길찾기",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 집으로 가기 버튼 (가장 크고 눈에 띄게)
        BigButton(
            text = "🏠 집으로 가기",
            onClick = onGoHomeSelected,
            backgroundColor = HomeButtonGreen,
            minHeight = 92.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 고정 목적지 버튼 목록
        if (uiState.destinations.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = Color(0xFFF5F7FA),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "등록된 목적지가 없어요.\n상단 제목을 5번 눌러\n목적지를 등록해 주세요.",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        lineHeight = 30.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(uiState.destinations) { destination ->
                    BigButton(
                        text = destination.displayName,
                        onClick = { onSelectDestination(destination) },
                        backgroundColor = PrimaryBlue,
                        minHeight = 84.dp
                    )
                }
            }
        }
    }
}
