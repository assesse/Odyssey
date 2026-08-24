package com.halmeoni.transit.ui.confirm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.halmeoni.transit.ui.components.BigButton
import com.halmeoni.transit.ui.components.TopBar
import com.halmeoni.transit.ui.theme.ActionRed
import com.halmeoni.transit.ui.theme.HomeButtonGreen

@Composable
fun ConfirmScreen(
    destinationName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onGoHome: () -> Unit
) {
    Scaffold(
        topBar = {
            TopBar(
                title = "목적지 확인",
                onBackClick = onCancel,
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
                text = "${destinationName}(으)로",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "갈까요?",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(64.dp))

            BigButton(
                text = "네 (길 찾기)",
                onClick = onConfirm,
                backgroundColor = HomeButtonGreen,
                minHeight = 96.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            BigButton(
                text = "아니오 (돌아가기)",
                onClick = onCancel,
                backgroundColor = ActionRed,
                minHeight = 88.dp
            )
        }
    }
}
