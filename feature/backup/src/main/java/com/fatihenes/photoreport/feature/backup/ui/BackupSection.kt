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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatihenes.photoreport.core.common.util.result.OperationResult
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
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
    val haptic = LocalHapticFeedback.current
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
            scope.launch { snackbarHost.showSnackbar((backupState as OperationResult.Error).message ?: context.getString(R.string.backup_error_unknown)) }
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
            snackbarHost.showSnackbar((restoreState as OperationResult.Error).message ?: context.getString(R.string.backup_restore_error))
            viewModel.resetRestoreState()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section title (matching Settings pattern)
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
                // Progress indicators
                if (backupState is OperationResult.Loading) {
                    val progress = (backupState as OperationResult.Loading).progress ?: 0
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
                if (restoreState is OperationResult.Loading) {
                    val progress = (restoreState as OperationResult.Loading).progress ?: 0
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

                // Create Backup Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            createBackupLauncher.launch("FotoRapor_Yedek.zip")
                        }
                        .padding(
                            horizontal = FotoRaporTokens.SpacingL,
                            vertical = FotoRaporTokens.SpacingM
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Backup,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(FotoRaporTokens.IconSizeS)
                    )
                    Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingL))
                    Column {
                        Text(
                            stringResource(R.string.settings_backup_create),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXS))
                        Text(
                            stringResource(R.string.settings_backup_create_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = FotoRaporTokens.SpacingL),
                    thickness = FotoRaporTokens.DividerThickness,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )

                // Restore Backup Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            restoreBackupLauncher.launch(arrayOf("application/zip"))
                        }
                        .padding(
                            horizontal = FotoRaporTokens.SpacingL,
                            vertical = FotoRaporTokens.SpacingM
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Restore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(FotoRaporTokens.IconSizeS)
                    )
                    Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingL))
                    Column {
                        Text(
                            stringResource(R.string.settings_backup_restore),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXS))
                        Text(
                            stringResource(R.string.settings_backup_restore_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
