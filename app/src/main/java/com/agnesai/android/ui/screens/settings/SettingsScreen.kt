package com.agnesai.android.ui.screens.settings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GitHub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agnesai.android.ui.theme.AccentIndigo
import com.agnesai.android.ui.theme.AccentPurple
import com.agnesai.android.ui.theme.DarkBackground
import com.agnesai.android.ui.theme.DarkContainer
import com.agnesai.android.ui.theme.DarkSurface
import com.agnesai.android.ui.theme.DarkSurfaceVariant
import com.agnesai.android.ui.theme.SubtleText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("✅ Đã lưu cài đặt thành công!")
            viewModel.clearSaveSuccess()
        }
    }

    LaunchedEffect(uiState.testSuccess) {
        uiState.testSuccess?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearTestSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cài đặt",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cấu hình Agnes AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = SubtleText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = DarkSurfaceVariant)
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Configuration Section
            item {
                SettingsSectionHeader(
                    icon = Icons.Filled.AutoAwesome,
                    title = "Cấu hình AI",
                    subtitle = "Cấu hình nhà cung cấp AI"
                )
            }
            item {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SettingsTextField(
                            label = "Agnes AI API Key",
                            value = uiState.apiKey,
                            onValueChange = viewModel::onApiKeyChange,
                            placeholder = "sk-...",
                            leadingIcon = Icons.Filled.Key,
                            isPassword = !uiState.showApiKey,
                            trailingIcon = {
                                IconButton(onClick = viewModel::toggleApiKeyVisibility) {
                                    Icon(
                                        imageVector = if (uiState.showApiKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = "Hiện/ẩn",
                                        tint = SubtleText
                                    )
                                }
                            }
                        )
                        SettingsTextField(
                            label = "Base URL",
                            value = uiState.baseUrl,
                            onValueChange = viewModel::onBaseUrlChange,
                            placeholder = "https://apihub.agnes-ai.com/v1",
                            leadingIcon = Icons.Filled.Language
                        )
                        ModelSelector(
                            selectedModel = uiState.selectedModel,
                            models = uiState.availableModels,
                            onModelSelect = viewModel::onModelSelect
                        )
                        // Test AI Connection button
                        OutlinedButton(
                            onClick = viewModel::testAiConnection,
                            enabled = !uiState.isTesting && uiState.apiKey.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AccentPurple
                            )
                        ) {
                            if (uiState.isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentPurple, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Đang kiểm tra...")
                            } else {
                                Icon(Icons.Filled.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Test kết nối AI & tải models")
                            }
                        }
                    }
                }
            }

            // GitHub Configuration Section
            item {
                SettingsSectionHeader(
                    icon = Icons.Filled.GitHub,
                    title = "GitHub Memory",
                    subtitle = "Lưu trữ hội thoại và files trên GitHub"
                )
            }
            item {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SettingsTextField(
                            label = "GitHub Personal Access Token",
                            value = uiState.githubToken,
                            onValueChange = viewModel::onGithubTokenChange,
                            placeholder = "ghp_...",
                            leadingIcon = Icons.Filled.Key,
                            isPassword = !uiState.showGithubToken,
                            trailingIcon = {
                                IconButton(onClick = viewModel::toggleGithubTokenVisibility) {
                                    Icon(
                                        imageVector = if (uiState.showGithubToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = "Hiện/ẩn",
                                        tint = SubtleText
                                    )
                                }
                            }
                        )
                        SettingsTextField(
                            label = "GitHub Username",
                            value = uiState.githubUsername,
                            onValueChange = viewModel::onGithubUsernameChange,
                            placeholder = "your-username",
                            leadingIcon = Icons.Filled.Person
                        )
                        SettingsTextField(
                            label = "Memory Repository",
                            value = uiState.githubRepo,
                            onValueChange = viewModel::onGithubRepoChange,
                            placeholder = "agnes-memory",
                            leadingIcon = Icons.Filled.Code
                        )
                        // Test GitHub Connection button
                        OutlinedButton(
                            onClick = viewModel::testGitHubConnection,
                            enabled = !uiState.isTesting && uiState.githubToken.isNotBlank() && uiState.githubUsername.isNotBlank() && uiState.githubRepo.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AccentIndigo
                            )
                        ) {
                            if (uiState.isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentIndigo, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Đang kiểm tra...")
                            } else {
                                Icon(Icons.Filled.GitHub, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Test kết nối GitHub")
                            }
                        }
                    }
                }
            }

            // Save Button
            item {
                Button(
                    onClick = viewModel::saveSettings,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đang lưu...")
                    } else {
                        Icon(imageVector = Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lưu Cài đặt", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(colors = listOf(AccentPurple, AccentIndigo))),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = SubtleText)
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(DarkSurface).padding(16.dp)
    ) { content() }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = SubtleText, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = placeholder, color = SubtleText.copy(alpha = 0.5f)) },
            leadingIcon = leadingIcon?.let { { Icon(imageVector = it, contentDescription = null, tint = SubtleText, modifier = Modifier.size(18.dp)) } },
            trailingIcon = trailingIcon,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkContainer,
                unfocusedContainerColor = DarkContainer,
                focusedBorderColor = AccentPurple,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSelector(selectedModel: String, models: List<String>, onModelSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = "Model", style = MaterialTheme.typography.labelMedium, color = SubtleText, fontWeight = FontWeight.Medium)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedModel,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.Check else Icons.Filled.Check,
                        contentDescription = null,
                        tint = AccentPurple
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkContainer,
                    unfocusedContainerColor = DarkContainer,
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(DarkSurface)) {
                models.forEach { model ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(text = model, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                if (model == selectedModel) {
                                    Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        onClick = { onModelSelect(model); expanded = false },
                        colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.onSurface)
                    )
                }
            }
        }
    }
}
