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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.halmeoni.transit.domain.model.Destination
import com.halmeoni.transit.ui.components.BigButton
import com.halmeoni.transit.ui.theme.HomeButtonGreen

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSelectDestination: (Destination) -> Unit,
    onGoHomeSelected: () -> Unit,
    onNavigateToPin: () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadDestinations()
    }

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { viewModel.onLogoTapped(onNavigateToPin) },
            color = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🚌 할머니 길찾기",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        BigButton(
            text = "🏠 집으로 가기",
            onClick = onGoHomeSelected,
            backgroundColor = HomeButtonGreen,
            minHeight = 96.dp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "어디로 가시나요?",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.destinations.isEmpty()) {
            Text(
                text = "등록된 목적지가 없어요.\n보호자 설정에서 목적지를 등록해 주세요.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(uiState.destinations) { destination ->
                    BigButton(
                        text = "${destination.displayName} (${destination.name})",
                        onClick = { onSelectDestination(destination) },
                        backgroundColor = MaterialTheme.colorScheme.primary,
                        minHeight = 88.dp
                    )
                }
            }
        }
    }
}
