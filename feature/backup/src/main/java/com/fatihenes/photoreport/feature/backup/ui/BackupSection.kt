@file:Suppress("LocalContextGetResourceValueCall")
package com.fatihenes.photoreport.feature.backup.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatihenes.photoreport.core.common.util.result.OperationResult
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.core.ui.navigation.LocalSnackbarHostState
import com.fatihenes.photoreport.feature.backup.viewmodel.BackupViewModel

@Composable
fun BackupSection(
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val snackbarHost = LocalSnackbarHostState.current

    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    val restoreState by viewModel.restoreState.collectAsStateWithLifecycle()

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.createBackup(it) } }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.restoreBackup(it) } }

    HandleOperationResult(
        state = backupState,
        onSuccess = {
            snackbarHost.showSnackbar(context.getString(R.string.backup_success))
            viewModel.resetBackupState()
        },
        onError = { message ->
            snackbarHost.showSnackbar(message ?: "Bilinmeyen Hata")
            viewModel.resetBackupState()
        }
    )

    HandleOperationResult(
        state = restoreState,
        onSuccess = {
            snackbarHost.showSnackbar(
                message = context.getString(R.string.backup_success),
                duration = SnackbarDuration.Short
            )
            viewModel.resetRestoreState()
        },
        onError = { message ->
            snackbarHost.showSnackbar(message ?: "Geri yükleme hatası")
            viewModel.resetRestoreState()
        }
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_backup_title).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 1.5f,
            modifier = Modifier.padding(
                start = FotoRaporTokens.SpacingXS,
                bottom = FotoRaporTokens.SpacingS
            )
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                FotoRaporTokens.CardBorderWidth,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(modifier = Modifier.padding(FotoRaporTokens.SpacingXS)) {
                BackupProgressIndicator(backupState)
                BackupProgressIndicator(restoreState)

                BackupItemRow(
                    icon = Icons.Default.Backup,
                    title = stringResource(R.string.settings_backup_create),
                    description = stringResource(R.string.settings_backup_create_desc),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        createBackupLauncher.launch("FotoRapor_Yedek.zip")
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = FotoRaporTokens.SpacingL),
                    thickness = FotoRaporTokens.DividerThickness,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )

                BackupItemRow(
                    icon = Icons.Default.Restore,
                    title = stringResource(R.string.settings_backup_restore),
                    description = stringResource(R.string.settings_backup_restore_desc),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        restoreBackupLauncher.launch(arrayOf("application/zip"))
                    }
                )
            }
        }
    }
}

@Composable
private fun HandleOperationResult(
    state: OperationResult<*>?,
    onSuccess: suspend () -> Unit,
    onError: suspend (String?) -> Unit
) {
    LaunchedEffect(state) {
        when (state) {
            is OperationResult.Success -> onSuccess()
            is OperationResult.Error -> onError(state.message)
            else -> {}
        }
    }
}

@Composable
private fun BackupProgressIndicator(state: OperationResult<*>?) {
    if (state is OperationResult.Loading) {
        val progress = state.progress ?: 0
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = FotoRaporTokens.SpacingL,
                    vertical = FotoRaporTokens.SpacingS
                ),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

@Composable
private fun BackupItemRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = FotoRaporTokens.SpacingL,
                vertical = FotoRaporTokens.SpacingM
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(FotoRaporTokens.IconSizeS)
        )
        Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingL))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXS))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
