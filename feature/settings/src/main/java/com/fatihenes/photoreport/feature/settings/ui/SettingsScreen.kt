@file:Suppress("LocalContextGetResourceValueCall")
package com.fatihenes.photoreport.feature.settings.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import com.fatihenes.photoreport.core.ui.navigation.LocalSnackbarHostState
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.feature.settings.ui.components.ImageCropperDialog
import com.fatihenes.photoreport.feature.settings.viewmodel.SettingsViewModel
import com.fatihenes.photoreport.core.common.util.result.OperationResult
import com.fatihenes.photoreport.core.media.CompanyLogoManager
import com.fatihenes.photoreport.feature.backup.ui.BackupSection


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHostState.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    
    val themeMode = settings?.themeMode ?: "system"
    val language = settings?.language ?: "tr"
    val gpsWatermarkEnabled = settings?.gpsWatermarkEnabled ?: false

    var logoUri by remember { mutableStateOf(CompanyLogoManager.getLogoUri(context)) }
    var logoVersion by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var imageToCropUri by remember { mutableStateOf<Uri?>(null) }

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) { "1.0.0" }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                imageToCropUri = uri
            }
        }
    )

    if (imageToCropUri != null) {
        ImageCropperDialog(
            imageUri = imageToCropUri!!,
            onDismiss = { imageToCropUri = null },
            onCropSuccess = { bitmap ->
                scope.launch {
                    CompanyLogoManager.saveLogo(context, bitmap)
                    logoUri = CompanyLogoManager.getLogoUri(context)
                    logoVersion = System.currentTimeMillis()
                    imageToCropUri = null
                    bitmap.recycle() // OOM önlemi
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_label))
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Theme Section
            SettingsSectionTitle(stringResource(R.string.settings_appearance))
            SettingsCard {
                Column {
                    ThemeOption(
                        title = stringResource(R.string.settings_theme_system),
                        icon = Icons.Default.BrightnessAuto,
                        isSelected = themeMode == "system",
                        onClick = { viewModel.setThemeMode("system") }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    ThemeOption(
                        title = stringResource(R.string.settings_theme_light),
                        icon = Icons.Default.LightMode,
                        isSelected = themeMode == "light",
                        onClick = { viewModel.setThemeMode("light") }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    ThemeOption(
                        title = stringResource(R.string.settings_theme_dark),
                        icon = Icons.Default.DarkMode,
                        isSelected = themeMode == "dark",
                        onClick = { viewModel.setThemeMode("dark") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Language Section
            SettingsSectionTitle(stringResource(R.string.settings_language))
            SettingsCard {
                Column {
                    LanguageOption(
                        title = "Türkçe",
                        isSelected = language == "tr",
                        onClick = { viewModel.setLanguage("tr") }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    LanguageOption(
                        title = "English",
                        isSelected = language == "en",
                        onClick = { viewModel.setLanguage("en") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Report Section
            SettingsSectionTitle(stringResource(R.string.settings_report))
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (logoUri != null) {
                                AsyncImage(
                                    model = "$logoUri?v=$logoVersion",
                                    contentDescription = stringResource(R.string.settings_company_logo),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    contentDescription = stringResource(R.string.settings_company_logo),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_company_logo),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.settings_company_logo_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (logoUri != null) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    CompanyLogoManager.deleteLogo(context)
                                    logoUri = null
                                    logoVersion = System.currentTimeMillis()
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.settings_remove_logo))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // GPS Watermark Section
            SettingsSectionTitle(stringResource(R.string.settings_gps_watermark))
            SettingsCard {
                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val isGranted = permissions.values.any { it }
                    if (isGranted) {
                        viewModel.setGpsWatermarkEnabled(true)
                    } else {
                        scope.launch { snackbarHost.showSnackbar(context.getString(R.string.location_permission_required)) }
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_gps_watermark_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.settings_gps_watermark_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = gpsWatermarkEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                } else {
                                    viewModel.setGpsWatermarkEnabled(false)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Backup Section
            BackupSection()

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "PhotoReport v$versionName",
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        content()
    }
}

@Composable
private fun LanguageOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.acc_close), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.acc_close), tint = MaterialTheme.colorScheme.primary)
        }
    }
}
