@file:Suppress("LocalContextGetResourceValueCall")
package com.fatihenes.photoreport.feature.settings.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.ui.navigation.LocalSnackbarHostState
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.feature.settings.ui.components.ImageCropperDialog
import com.fatihenes.photoreport.feature.settings.viewmodel.SettingsViewModel
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
    ) { uri ->
        uri?.let {
            imageToCropUri = it
        }
    }

    imageToCropUri?.let { uri ->
        ImageCropperDialog(
            imageUri = uri,
            onDismiss = { imageToCropUri = null },
            onCropSuccess = { bitmap ->
                scope.launch {
                    CompanyLogoManager.saveLogo(context, bitmap)
                    logoUri = CompanyLogoManager.getLogoUri(context)
                    logoVersion = System.currentTimeMillis()
                    imageToCropUri = null
                    bitmap.recycle() // OOM önlemi
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_label),
                            modifier = Modifier.size(FotoRaporTokens.IconSizeS)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = FotoRaporTokens.ScreenPaddingHorizontal)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingS))

            // ── Theme ─────────────────────────────────────
            SettingsSectionTitle(stringResource(R.string.settings_appearance))
            SettingsCard {
                Column {
                    ThemeOption(
                        title = stringResource(R.string.settings_theme_system),
                        icon = Icons.Default.BrightnessAuto,
                        isSelected = themeMode == "system",
                        onClick = { viewModel.setThemeMode("system") }
                    )
                    SettingsDivider()
                    ThemeOption(
                        title = stringResource(R.string.settings_theme_light),
                        icon = Icons.Default.LightMode,
                        isSelected = themeMode == "light",
                        onClick = { viewModel.setThemeMode("light") }
                    )
                    SettingsDivider()
                    ThemeOption(
                        title = stringResource(R.string.settings_theme_dark),
                        icon = Icons.Default.DarkMode,
                        isSelected = themeMode == "dark",
                        onClick = { viewModel.setThemeMode("dark") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXL))

            // ── Language ──────────────────────────────────
            SettingsSectionTitle(stringResource(R.string.settings_language))
            SettingsCard {
                Column {
                    LanguageOption(
                        title = "Türkçe",
                        isSelected = language == "tr",
                        onClick = { viewModel.setLanguage("tr") }
                    )
                    SettingsDivider()
                    LanguageOption(
                        title = "English",
                        isSelected = language == "en",
                        onClick = { viewModel.setLanguage("en") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXL))

            // ── Company Logo ──────────────────────────────
            SettingsSectionTitle(stringResource(R.string.settings_report))
            SettingsCard {
                Column(modifier = Modifier.padding(FotoRaporTokens.SpacingL)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier
                                .size(FotoRaporTokens.AvatarSizeL)
                                .clip(RoundedCornerShape(FotoRaporTokens.RadiusM))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(FotoRaporTokens.RadiusM)
                        ) {
                            if (logoUri != null) {
                                AsyncImage(
                                    model = "$logoUri?v=$logoVersion",
                                    contentDescription = stringResource(R.string.settings_company_logo),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        Icons.Default.AddAPhoto,
                                        contentDescription = stringResource(R.string.settings_company_logo),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(FotoRaporTokens.IconSizeL)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingL))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_company_logo),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXS))
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
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = FotoRaporTokens.SpacingS),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                stringResource(R.string.settings_remove_logo),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXL))

            // ── GPS Watermark ─────────────────────────────
            SettingsSectionTitle(stringResource(R.string.settings_gps_watermark))
            SettingsCard {
                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val isGranted = permissions.values.any { it }
                    if (isGranted) {
                        viewModel.setGpsWatermarkEnabled(enabled = true)
                    } else {
                        scope.launch { snackbarHost.showSnackbar(context.getString(R.string.location_permission_required)) }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(FotoRaporTokens.SpacingL),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_gps_watermark_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXS))
                        Text(
                            stringResource(R.string.settings_gps_watermark_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingM))
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
                                viewModel.setGpsWatermarkEnabled(enabled = false)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXL))

            // ── Backup ────────────────────────────────────
            BackupSection()

            Spacer(modifier = Modifier.weight(1f))

            // ── Version Footer ────────────────────────────
            Text(
                "FotoRapor v$versionName",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = FotoRaporTokens.Spacing3XL,
                        bottom = FotoRaporTokens.SpacingXXL
                    ),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

// ─── Reusable Settings Components ────────────────────────────────

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 1.5f,
        modifier = Modifier.padding(
            start = FotoRaporTokens.SpacingXS,
            bottom = FotoRaporTokens.SpacingS
        )
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            FotoRaporTokens.CardBorderWidth,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = FotoRaporTokens.SpacingL),
        thickness = FotoRaporTokens.DividerThickness,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}

@Composable
private fun LanguageOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
        animationSpec = FotoRaporMotion.enterTween(),
        label = "lang_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(FotoRaporTokens.SpacingL),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(FotoRaporTokens.IconSizeS)
            )
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
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
        animationSpec = FotoRaporMotion.enterTween(),
        label = "theme_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(FotoRaporTokens.SpacingL),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(FotoRaporTokens.IconSizeS)
            )
            Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingL))
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(FotoRaporTokens.IconSizeS)
            )
        }
    }
}
