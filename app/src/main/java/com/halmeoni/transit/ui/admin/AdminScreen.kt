package com.halmeoni.transit.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.halmeoni.transit.ui.components.BigButton
import com.halmeoni.transit.ui.components.TopBar
import com.halmeoni.transit.ui.theme.ActionRed
import com.halmeoni.transit.ui.theme.HomeButtonGreen

@Composable
fun AdminScreen(
    viewModel: AdminViewModel,
    onGoHome: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopBar(title = "보호자 / 관리자 설정", onBackClick = onGoHome, onHomeClick = onGoHome)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (settings.saveErrorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = "⚠️ ${settings.saveErrorMessage}",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 1. Home Location Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🏠 집 위치 설정",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = settings.homeAddressInput,
                        onValueChange = { viewModel.onHomeAddressChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        label = { Text("집 주소 또는 메모") },
                        isError = settings.homeAddressError != null,
                        supportingText = settings.homeAddressError?.let { { Text(it) } }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = settings.homeLatInput,
                            onValueChange = { viewModel.onHomeLatChanged(it) },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            label = { Text("위도 (Latitude)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = settings.homeLatError != null,
                            supportingText = settings.homeLatError?.let { { Text(it) } }
                        )

                        OutlinedTextField(
                            value = settings.homeLngInput,
                            onValueChange = { viewModel.onHomeLngChanged(it) },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            label = { Text("경도 (Longitude)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = settings.homeLngError != null,
                            supportingText = settings.homeLngError?.let { { Text(it) } }
                        )
                    }
                }
            }

            // 2. Destinations Management Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📍 목적지 관리 (${settings.destinations.size}/6)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (settings.destinations.size < 6) {
                            Button(
                                onClick = { viewModel.openAddDestinationDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("➕ 추가")
                            }
                        }
                    }

                    if (settings.destGeneralError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = settings.destGeneralError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (settings.destinations.isEmpty()) {
                        Text(
                            text = "등록된 목적지가 없어요. '➕ 추가' 버튼을 눌러 등록해 주세요.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        settings.destinations.forEachIndexed { index, dest ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${index + 1}. ${dest.displayName} (${dest.name})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "좌표: ${dest.latitude}, ${dest.longitude}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { viewModel.moveDestinationUp(dest.id) },
                                            enabled = index > 0
                                        ) {
                                            Text("▲")
                                        }

                                        IconButton(
                                            onClick = { viewModel.moveDestinationDown(dest.id) },
                                            enabled = index < settings.destinations.size - 1
                                        ) {
                                            Text("▼")
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.openEditDestinationDialog(dest) }
                                        ) {
                                            Text("수정")
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        OutlinedButton(
                                            onClick = { viewModel.deleteDestination(dest.id) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ActionRed)
                                        ) {
                                            Text("삭제")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Security (PIN Change) Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔒 비밀번호 (PIN) 변경",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "비밀번호를 변경하려면 아래에 새 4자리 숫자를 입력해 주세요. (변경하지 않으려면 빈칸으로 둠)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = settings.newPinInput,
                            onValueChange = { viewModel.onNewPinChanged(it) },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            label = { Text("새 PIN (4자리)") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                        )

                        OutlinedTextField(
                            value = settings.confirmPinInput,
                            onValueChange = { viewModel.onConfirmPinChanged(it) },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            label = { Text("새 PIN 확인") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                        )
                    }

                    if (settings.pinChangeError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = settings.pinChangeError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // 4. API Usage Statistics
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📊 API 사용 통계",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "오늘 경로 조회 호출 횟수: ${settings.apiCallCount} 회",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // 5. Save All Settings Button
            BigButton(
                text = "💾 설정 저장하기",
                onClick = {
                    val isSuccess = viewModel.saveAllSettings()
                    if (isSuccess) {
                        onGoHome()
                    }
                },
                backgroundColor = HomeButtonGreen,
                minHeight = 88.dp
            )
        }
    }

    // Destination Add/Edit Dialog
    if (settings.isDestDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.closeDestinationDialog() },
            title = {
                Text(
                    text = if (settings.editingDestId != null) "목적지 수정" else "새 목적지 추가",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = settings.destShortNameInput,
                        onValueChange = { viewModel.onDestShortNameChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("짧은 이름 (예: 병원, 시장)") },
                        isError = settings.destShortNameError != null,
                        supportingText = settings.destShortNameError?.let { { Text(it) } }
                    )

                    OutlinedTextField(
                        value = settings.destFullNameInput,
                        onValueChange = { viewModel.onDestFullNameChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("실제 장소 이름 (예: 서울성모병원)") },
                        isError = settings.destFullNameError != null,
                        supportingText = settings.destFullNameError?.let { { Text(it) } }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = settings.destLatInput,
                            onValueChange = { viewModel.onDestLatChanged(it) },
                            modifier = Modifier.weight(1f),
                            label = { Text("위도") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = settings.destLatError != null,
                            supportingText = settings.destLatError?.let { { Text(it) } }
                        )

                        OutlinedTextField(
                            value = settings.destLngInput,
                            onValueChange = { viewModel.onDestLngChanged(it) },
                            modifier = Modifier.weight(1f),
                            label = { Text("경도") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = settings.destLngError != null,
                            supportingText = settings.destLngError?.let { { Text(it) } }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveDestinationFromDialog() }
                ) {
                    Text("저장")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.closeDestinationDialog() }
                ) {
                    Text("취소")
                }
            }
        )
    }
}
