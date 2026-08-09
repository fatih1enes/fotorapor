@file:Suppress("LocalContextGetResourceValueCall", "MaxLineLength", "FunctionName", "TooManyFunctions")
package com.fatihenes.photoreport.feature.settings.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.ui.navigation.LocalSnackbarHostState
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.feature.settings.ui.components.ImageCropperDialog
import com.fatihenes.photoreport.feature.settings.viewmodel.SettingsViewModel
import com.fatihenes.photoreport.core.media.CompanyLogoManager
import com.fatihenes.photoreport.feature.backup.ui.BackupSection

private data class SettingsContentParams(
    val padding: PaddingValues,
    val themeMode: String,
    val language: String,
    val gpsWatermarkEnabled: Boolean,
    val logoUri: Uri?,
    val logoVersion: Long,
    val onThemeSelected: (String) -> Unit,
    val onLanguageSelected: (String) -> Unit,
    val onPickLogo: () -> Unit,
    val onRemoveLogo: () -> Unit,
    val onToggleGps: (Boolean) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionNaming")
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val themeMode = settings?.themeMode ?: "system"
    val language = settings?.language ?: "tr"
    val gpsWatermarkEnabled = settings?.gpsWatermarkEnabled ?: false

    var logoUri by remember { mutableStateOf(CompanyLogoManager.getLogoUri(context)) }
    var logoVersion by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var imageToCropUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { imageToCropUri = it } }

    SettingsLogoCropHandler(
        imageToCropUri = imageToCropUri,
        onDismiss = { imageToCropUri = null },
        onLogoSaved = { bitmap ->
            CompanyLogoManager.saveLogo(context, bitmap)
            logoUri = CompanyLogoManager.getLogoUri(context)
            logoVersion = System.currentTimeMillis()
            imageToCropUri = null
        },
    )

    Scaffold(
        topBar = { SettingsTopBar(onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        SettingsContent(
            SettingsContentParams(
                padding = padding,
                themeMode = themeMode,
                language = language,
                gpsWatermarkEnabled = gpsWatermarkEnabled,
                logoUri = logoUri,
                logoVersion = logoVersion,
                onThemeSelected = { viewModel.setThemeMode(it) },
                onLanguageSelected = { viewModel.setLanguage(it) },
                onPickLogo = {
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                        ),
                    )
                },
                onRemoveLogo = {
                    scope.launch {
                        CompanyLogoManager.deleteLogo(context)
                        logoUri = null
                        logoVersion = System.currentTimeMillis()
                    }
                },
                onToggleGps = { viewModel.setGpsWatermarkEnabled(it) },
            ),
        )
    }
}

@Composable
private fun SettingsLogoCropHandler(
    imageToCropUri: Uri?,
    onDismiss: () -> Unit,
    onLogoSaved: suspend (android.graphics.Bitmap) -> Unit,
) {
    val scope = rememberCoroutineScope()
    if (imageToCropUri != null) {
        ImageCropperDialog(imageUri = imageToCropUri, onDismiss = onDismiss) { bitmap ->
            scope.launch {
                onLogoSaved(bitmap)
                bitmap.recycle()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    stringResource(R.string.back_label),
                    modifier = Modifier.size(FotoRaporTokens.IconSizeS),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
private fun SettingsContent(p: SettingsContentParams) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(p.padding)
            .padding(horizontal = FotoRaporTokens.ScreenPaddingHorizontal)
            .verticalScroll(scrollState),
    ) {
        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingS))

        ThemeSection(p.themeMode, p.onThemeSelected)
        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXL))

        LanguageSection(p.language, p.onLanguageSelected)
        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXL))

        LogoSection(p.logoUri, p.logoVersion, p.onPickLogo, p.onRemoveLogo)
        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXL))

        GpsWatermarkSection(p.gpsWatermarkEnabled, p.onToggleGps)
        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXL))

        BackupSection()
        Spacer(modifier = Modifier.weight(1f))
        VersionFooter(context)
    }
}

@Composable
private fun ThemeSection(themeMode: String, onThemeSelected: (String) -> Unit) {
    SettingsSectionTitle(stringResource(R.string.settings_appearance))
    SettingsCard {
        Column {
            ThemeOption(
                stringResource(R.string.settings_theme_system),
                Icons.Default.BrightnessAuto,
                themeMode == "system",
            ) { onThemeSelected("system") }
            SettingsDivider()
            ThemeOption(
                stringResource(R.string.settings_theme_light),
                Icons.Default.LightMode,
                themeMode == "light",
            ) { onThemeSelected("light") }
            SettingsDivider()
            ThemeOption(
                stringResource(R.string.settings_theme_dark),
                Icons.Default.DarkMode,
                themeMode == "dark",
            ) { onThemeSelected("dark") }
        }
    }
}

@Composable
private fun LanguageSection(language: String, onLanguageSelected: (String) -> Unit) {
    SettingsSectionTitle(stringResource(R.string.settings_language))
    SettingsCard {
        Column {
            LanguageOption("Türkçe", language == "tr") { onLanguageSelected("tr") }
            SettingsDivider()
            LanguageOption("English", language == "en") { onLanguageSelected("en") }
        }
    }
}

@Composable
private fun LogoSection(
    logoUri: Uri?,
    logoVersion: Long,
    onPickLogo: () -> Unit,
    onRemoveLogo: () -> Unit,
) {
    SettingsSectionTitle(stringResource(R.string.settings_report))
    SettingsCard {
        Column(modifier = Modifier.padding(FotoRaporTokens.SpacingL)) {
            LogoSectionHeader(logoUri, logoVersion, onPickLogo)
            if (logoUri != null) {
                TextButton(
                    onClick = onRemoveLogo,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = FotoRaporTokens.SpacingS),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(
                        stringResource(R.string.settings_remove_logo),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun LogoSectionHeader(
    logoUri: Uri?,
    logoVersion: Long,
    onPickLogo: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LogoImage(logoUri, logoVersion, onPickLogo)
        Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingL))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.settings_company_logo),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXS))
            Text(
                stringResource(R.string.settings_company_logo_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LogoImage(
    logoUri: Uri?,
    logoVersion: Long,
    onPickLogo: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(FotoRaporTokens.AvatarSizeL)
            .clip(RoundedCornerShape(FotoRaporTokens.RadiusM))
            .clickable { onPickLogo() },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
    ) {
        if (logoUri != null) {
            AsyncImage(
                model = "$logoUri?v=$logoVersion",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    Icons.Default.AddAPhoto,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(FotoRaporTokens.IconSizeL),
                )
            }
        }
    }
}

@Composable
private fun GpsWatermarkSection(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHostState.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { perms ->
        if (perms.values.any { it }) {
            onToggle(true)
        } else {
            scope.launch {
                snackbarHost.showSnackbar(context.getString(R.string.location_permission_required))
            }
        }
    }

    SettingsSectionTitle(stringResource(R.string.settings_gps_watermark))
    SettingsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(FotoRaporTokens.SpacingL),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_gps_watermark_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXS))
                Text(
                    stringResource(R.string.settings_gps_watermark_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingM))
            Switch(
                checked = enabled,
                onCheckedChange = {
                    if (it) {
                        permissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    } else {
                        onToggle(false)
                    }
                },
            )
        }
    }
}

@Composable
private fun VersionFooter(context: android.content.Context) {
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }
    Text(
        text = "FotoRapor v$versionName",
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = FotoRaporTokens.Spacing3XL,
                bottom = FotoRaporTokens.SpacingXXL,
            ),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
    )
}

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
            bottom = FotoRaporTokens.SpacingS,
        ),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            FotoRaporTokens.CardBorderWidth,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = FotoRaporTokens.SpacingL),
        thickness = FotoRaporTokens.DividerThickness,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    )
}

@Composable
private fun LanguageOption(title: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        },
        label = "lang_bg",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(FotoRaporTokens.SpacingL),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(FotoRaporTokens.IconSizeS),
            )
        }
    }
}

@Composable
private fun ThemeOption(title: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        },
        label = "theme_bg",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(FotoRaporTokens.SpacingL),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(FotoRaporTokens.IconSizeS),
            )
            Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingL))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(FotoRaporTokens.IconSizeS),
            )
        }
    }
}
