package com.agnesai.android.ui.screens.files

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agnesai.android.data.model.FileItem
import com.agnesai.android.ui.theme.AccentIndigo
import com.agnesai.android.ui.theme.AccentPurple
import com.agnesai.android.ui.theme.AccentTeal
import com.agnesai.android.ui.theme.DarkBackground
import com.agnesai.android.ui.theme.DarkContainer
import com.agnesai.android.ui.theme.DarkSurface
import com.agnesai.android.ui.theme.DarkSurfaceVariant
import com.agnesai.android.ui.theme.SubtleText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    viewModel: FilesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var fileToDelete by remember { mutableStateOf<FileItem?>(null) }
    val context = LocalContext.current

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val displayName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (nameIndex >= 0) cursor.getString(nameIndex) else "file_${System.currentTimeMillis()}"
            } ?: "file_${System.currentTimeMillis()}"
            viewModel.uploadFile(uri, displayName)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Files",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "GitHub Memory Storage",
                            style = MaterialTheme.typography.labelSmall,
                            color = SubtleText
                        )
                    }
                },
                navigationIcon = {
                    if (uiState.currentPath.isNotBlank()) {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại", tint = SubtleText)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Tải lại", tint = SubtleText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch("*/*") },
                containerColor = AccentPurple,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                if (uiState.isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Add, contentDescription = "Upload file")
                }
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = DarkSurfaceVariant)
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentPurple, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Đang tải files...", style = MaterialTheme.typography.bodyMedium, color = SubtleText)
                        }
                    }
                }

                uiState.files.isEmpty() && !uiState.isLoading -> {
                    EmptyFilesView()
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Breadcrumb navigation
                        if (uiState.breadcrumbs.isNotEmpty()) {
                            item {
                                BreadcrumbRow(
                                    breadcrumbs = uiState.breadcrumbs,
                                    onHomeClick = { viewModel.loadFiles("") },
                                    onBreadcrumbClick = { index -> viewModel.navigateToBreadcrumb(index) }
                                )
                            }
                        }

                        item {
                            StorageHeader(fileCount = uiState.files.size, currentPath = uiState.currentPath)
                        }

                        items(items = uiState.files, key = { it.sha.ifBlank { it.path } }) { file ->
                            FileItemCard(
                                file = file,
                                onClick = { viewModel.navigateTo(file) },
                                onDelete = { fileToDelete = file }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            containerColor = DarkSurface,
            title = { Text("Xóa File") },
            text = { Text("Bạn có chắc muốn xóa \"${file.name}\"? Không thể hoàn tác.", color = SubtleText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFile(file)
                        fileToDelete = null
                    }
                ) {
                    Text("Xóa", color = Color(0xFFEF5350))
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Hủy", color = SubtleText)
                }
            }
        )
    }
}

@Composable
private fun BreadcrumbRow(
    breadcrumbs: List<String>,
    onHomeClick: () -> Unit,
    onBreadcrumbClick: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        item {
            IconButton(onClick = onHomeClick, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Home, contentDescription = "Home", tint = AccentPurple, modifier = Modifier.size(18.dp))
            }
        }
        items(breadcrumbs.indices.toList()) { index ->
            Text(text = "/", color = SubtleText, style = MaterialTheme.typography.bodySmall)
            Text(
                text = breadcrumbs[index],
                color = if (index == breadcrumbs.lastIndex) MaterialTheme.colorScheme.onSurface else AccentPurple,
                style = MaterialTheme.typography.bodySmall,
                modifier = if (index < breadcrumbs.lastIndex) Modifier.clickable { onBreadcrumbClick(index) } else Modifier
            )
        }
    }
}

@Composable
private fun StorageHeader(fileCount: Int, currentPath: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (currentPath.isBlank()) "Tất cả Files" else currentPath.substringAfterLast('/'),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "$fileCount items",
            style = MaterialTheme.typography.labelSmall,
            color = SubtleText
        )
    }
}

@Composable
private fun FileItemCard(
    file: FileItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isDir = file.type == "dir"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isDir) Brush.linearGradient(listOf(AccentPurple, AccentIndigo)) else Brush.linearGradient(listOf(DarkContainer, DarkSurfaceVariant))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDir) Icons.Filled.Folder else getFileIcon(file),
                contentDescription = null,
                tint = if (isDir) Color.White else getFileIconTint(file),
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!isDir) {
                Text(text = file.displaySize, style = MaterialTheme.typography.labelSmall, color = SubtleText)
            } else {
                Text(text = "Thư mục", style = MaterialTheme.typography.labelSmall, color = SubtleText)
            }
        }

        if (!isDir) {
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Xóa", tint = SubtleText, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EmptyFilesView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(DarkContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Filled.Folder, contentDescription = null, tint = SubtleText, modifier = Modifier.size(40.dp))
            }
            Text(text = "Chưa có files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "Cấu hình GitHub repo trong Cài đặt để lưu trữ files và memories.",
                style = MaterialTheme.typography.bodySmall,
                color = SubtleText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

private fun getFileIcon(file: FileItem): ImageVector {
    return when {
        file.isImage -> Icons.Filled.Image
        file.isCode -> Icons.Filled.Code
        file.extension == "json" -> Icons.Filled.DataObject
        file.extension == "pdf" -> Icons.Filled.Description
        file.extension == "md" || file.extension == "txt" -> Icons.Filled.Article
        else -> Icons.Filled.InsertDriveFile
    }
}

@Composable
private fun getFileIconTint(file: FileItem): Color {
    return when {
        file.isImage -> AccentTeal
        file.isCode -> AccentPurple
        file.extension == "json" -> Color(0xFFFFB74D)
        file.extension == "pdf" -> Color(0xFFEF5350)
        else -> AccentIndigo
    }
}
