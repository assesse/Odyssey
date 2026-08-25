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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.sp
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Home Location Card
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
                            text = "🏠 집 위치 설정",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (settings.isHomeConfigured) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = if (settings.isHomeConfigured) "● 등록 완료" else "● 미등록",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (settings.isHomeConfigured) HomeButtonGreen else ActionRed,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = settings.homeAddressInput,
                        onValueChange = { viewModel.onHomeAddressChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("집 주소 (예: 서울시 종로구 세종대로 1)") },
                        isError = settings.homeAddressError != null,
                        supportingText = settings.homeAddressError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true
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
                            label = { Text("위도 (예: 37.5665)") },
                            isError = settings.homeLatError != null,
                            supportingText = settings.homeLatError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = settings.homeLngInput,
                            onValueChange = { viewModel.onHomeLngChanged(it) },
                            modifier = Modifier.weight(1f),
                            label = { Text("경도 (예: 126.9780)") },
                            isError = settings.homeLngError != null,
                            supportingText = settings.homeLngError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                }
            }

            // 2. Destination Management Card
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
                            text = "📍 고정 목적지 (${settings.destinations.size}/6)",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
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
                            text = "등록된 목적지가 없어요. 아래 '➕ 새 목적지 추가' 버튼을 눌러 등록해 주세요.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        settings.destinations.forEachIndexed { index, dest ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    // 1단: 목적지 이름 및 상세 정보 (전체 가로폭 확보)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}. ${dest.displayName}",
                                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "(${dest.name})",
                                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                                            color = Color.DarkGray
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "위도: ${dest.latitude} / 경도: ${dest.longitude}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = Color(0xFFE0E0E0))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // 2단: 조작 버튼 행 (우측 정렬)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.moveDestinationUp(dest.id) },
                                            enabled = index > 0
                                        ) {
                                            Text("▲", fontWeight = FontWeight.Bold)
                                        }

                                        IconButton(
                                            onClick = { viewModel.moveDestinationDown(dest.id) },
                                            enabled = index < settings.destinations.size - 1
                                        ) {
                                            Text("▼", fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        OutlinedButton(
                                            onClick = { viewModel.openEditDestinationDialog(dest) }
                                        ) {
                                            Text("수정")
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

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

                    // ➕ 새 목적지 추가 버튼 (전체 가로폭으로 시원하게 배치하여 세로 기둥 방지)
                    if (settings.destinations.size < 6) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.openAddDestinationDialog() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "➕ 새 목적지 추가",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. ODsay API Key Card
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
                            text = "🌐 ODsay API 키 설정",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (settings.isApiKeyConfigured) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = if (settings.isApiKeyConfigured) "● 키 설정됨" else "● 키 미설정",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (settings.isApiKeyConfigured) HomeButtonGreen else ActionRed,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "대중교통 길찾기를 위해 ODsay API 키가 필요합니다. 발급받은 키를 아래에 입력하고 하단 '설정 저장하기'를 눌러주세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = settings.apiKeyInput,
                        onValueChange = { viewModel.onApiKeyChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("ODsay API Key") },
                        singleLine = true
                    )
                }
            }

            // 4. Security (PIN Change) Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔒 비밀번호 (PIN) 변경",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
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
                            label = { Text("새 PIN") },
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

            // 5. API Usage Statistics
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📊 API 사용 통계",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
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

            if (settings.saveErrorMessage != null) {
                Text(
                    text = settings.saveErrorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 6. Save Button
            BigButton(
                text = "💾 설정 저장하기",
                onClick = {
                    if (viewModel.saveAllSettings()) {
                        onGoHome()
                    }
                },
                backgroundColor = HomeButtonGreen,
                minHeight = 80.dp
            )
        }
    }

    // Add / Edit Destination Dialog
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
                        label = { Text("표시 이름 (예: 병원)") },
                        isError = settings.destShortNameError != null,
                        supportingText = settings.destShortNameError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = settings.destFullNameInput,
                        onValueChange = { viewModel.onDestFullNameChanged(it) },
                        label = { Text("상세 명칭 (예: 서울대학교병원)") },
                        isError = settings.destFullNameError != null,
                        supportingText = settings.destFullNameError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = settings.destLatInput,
                        onValueChange = { viewModel.onDestLatChanged(it) },
                        label = { Text("위도 (예: 37.5796)") },
                        isError = settings.destLatError != null,
                        supportingText = settings.destLatError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = settings.destLngInput,
                        onValueChange = { viewModel.onDestLngChanged(it) },
                        label = { Text("경도 (예: 126.9990)") },
                        isError = settings.destLngError != null,
                        supportingText = settings.destLngError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
