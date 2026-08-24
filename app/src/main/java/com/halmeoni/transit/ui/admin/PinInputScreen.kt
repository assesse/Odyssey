package com.halmeoni.transit.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.halmeoni.transit.ui.components.TopBar
import com.halmeoni.transit.ui.theme.ActionRed

@Composable
fun PinInputScreen(
    viewModel: AdminViewModel,
    onAuthSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val enteredPin by viewModel.enteredPin.collectAsState()
    val pinError by viewModel.pinError.collectAsState()

    Scaffold(
        topBar = { TopBar(title = "관리자 인증", onBackClick = onCancel, onHomeClick = null) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "PIN 4자리를 입력하세요", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (i in 0 until 4) {
                    val isFilled = i < enteredPin.length
                    Text(
                        text = if (isFilled) "●" else "○",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (pinError) {
                Text(text = "비밀번호가 올바르지 않습니다.", style = MaterialTheme.typography.titleMedium, color = ActionRed)
            } else {
                Spacer(modifier = Modifier.height(38.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            val digits = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("지우기", "0", "")
            )

            for (row in digits) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (item in row) {
                        if (item.isEmpty()) {
                            Spacer(modifier = Modifier.weight(1f).height(72.dp))
                        } else {
                            Button(
                                onClick = {
                                    if (item == "지우기") {
                                        viewModel.onPinClear()
                                    } else {
                                        viewModel.onPinDigitEntered(item, onAuthSuccess)
                                    }
                                },
                                modifier = Modifier.weight(1f).height(72.dp)
                            ) {
                                Text(text = item, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
