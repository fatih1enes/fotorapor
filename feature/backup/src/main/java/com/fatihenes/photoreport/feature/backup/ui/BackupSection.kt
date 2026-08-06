@file:Suppress("LocalContextGetResourceValueCall")
package com.fatihenes.photoreport.feature.backup.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatihenes.photoreport.core.common.util.result.OperationResult
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.core.ui.navigation.LocalSnackbarHostState
import com.fatihenes.photoreport.feature.backup.viewmodel.BackupViewModel
import kotlinx.coroutines.launch

@Composable
fun BackupSection(
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHostState.current

    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    val restoreState by viewModel.restoreState.collectAsStateWithLifecycle()

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.createBackup(uri)
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreBackup(uri)
        }
    }

    LaunchedEffect(backupState) {
        if (backupState is OperationResult.Success) {
            scope.launch { snackbarHost.showSnackbar(context.getString(R.string.backup_success)) }
            viewModel.resetBackupState()
        } else if (backupState is OperationResult.Error) {
            scope.launch { snackbarHost.showSnackbar((backupState as OperationResult.Error).message ?: "Bilinmeyen Hata") }
            viewModel.resetBackupState()
        }
    }

    LaunchedEffect(restoreState) {
        if (restoreState is OperationResult.Success) {
            snackbarHost.showSnackbar(
                message = context.getString(R.string.backup_success),
                duration = SnackbarDuration.Short
            )
            viewModel.resetRestoreState()
        } else if (restoreState is OperationResult.Error) {
            snackbarHost.showSnackbar((restoreState as OperationResult.Error).message ?: "Geri yükleme hatası")
            viewModel.resetRestoreState()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_backup_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (backupState is OperationResult.Loading) {
                    val progress = (backupState as OperationResult.Loading).progress ?: 0
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                }
                if (restoreState is OperationResult.Loading) {
                    val progress = (restoreState as OperationResult.Loading).progress ?: 0
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { createBackupLauncher.launch("PhotoReport_Yedek.zip") }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Backup, contentDescription = stringResource(R.string.acc_expand), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.settings_backup_create), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.settings_backup_create_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { restoreBackupLauncher.launch(arrayOf("application/zip")) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Restore, contentDescription = stringResource(R.string.acc_expand), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.settings_backup_restore), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.settings_backup_restore_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
