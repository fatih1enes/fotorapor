package com.fatihenes.photoreport.core.designsystem.theme

import android.app.Activity
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ─── Color Schemes ──────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight,
    scrim = Color(0xFF000000)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark_,
    surfaceContainerHigh = SurfaceContainerHighDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,
    scrim = Color(0xFF000000)
)

// ─── Typography ─────────────────────────────────────────────────────

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.0).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.15).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.15.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp
    )
)

// ─── Shapes ─────────────────────────────────────────────────────────

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// ─── Design Tokens ──────────────────────────────────────────────────

/**
 * FotoRapor kurumsal Design Token'ları.
 * Tüm UI bileşenlerinde tutarlı görsel dil sağlar.
 *
 * Kullanım:
 * ```kotlin
 * modifier = Modifier.padding(FotoRaporTokens.SpacingM)
 * ```
 */
@Immutable
object FotoRaporTokens {

    // ── Spacing (8pt grid) ──────────────────────────────────────
    val SpacingXXS: Dp = 2.dp
    val SpacingXS: Dp = 4.dp
    val SpacingS: Dp = 8.dp
    val SpacingM: Dp = 12.dp
    val SpacingL: Dp = 16.dp
    val SpacingXL: Dp = 20.dp
    val SpacingXXL: Dp = 24.dp
    val Spacing3XL: Dp = 32.dp
    val Spacing4XL: Dp = 40.dp
    val Spacing5XL: Dp = 48.dp

    // ── Screen Edge Padding ─────────────────────────────────────
    val ScreenPaddingHorizontal: Dp = 20.dp
    val ScreenPaddingTop: Dp = 16.dp

    // ── Corner Radius ───────────────────────────────────────────
    val RadiusXS: Dp = 6.dp
    val RadiusS: Dp = 10.dp
    val RadiusM: Dp = 14.dp
    val RadiusL: Dp = 20.dp
    val RadiusXL: Dp = 28.dp

    // ── Elevation ───────────────────────────────────────────────
    val ElevationNone: Dp = 0.dp
    val ElevationXS: Dp = 0.5.dp
    val ElevationS: Dp = 1.dp
    val ElevationM: Dp = 2.dp
    val ElevationL: Dp = 4.dp

    // ── Component Sizes ─────────────────────────────────────────
    val ButtonHeightL: Dp = 52.dp
    val ButtonHeightM: Dp = 44.dp
    val ButtonHeightS: Dp = 36.dp

    val IconSizeXS: Dp = 16.dp
    val IconSizeS: Dp = 20.dp
    val IconSizeM: Dp = 24.dp
    val IconSizeL: Dp = 32.dp
    val IconSizeXL: Dp = 48.dp

    val TouchTarget: Dp = 48.dp
    val FabSize: Dp = 56.dp

    val AvatarSizeS: Dp = 40.dp
    val AvatarSizeM: Dp = 56.dp
    val AvatarSizeL: Dp = 80.dp

    val CardBorderWidth: Dp = 0.5.dp
    val DividerThickness: Dp = 0.5.dp

    val TopBarHeight: Dp = 64.dp

    // ── Project Color Picker Colors ─────────────────────────────
    val ProjectColors = listOf(
        Color(0xFF2563EB), // Blue 600
        Color(0xFF7C3AED), // Violet 600
        Color(0xFFDB2777), // Pink 600
        Color(0xFFDC2626), // Red 600
        Color(0xFFEA580C), // Orange 600
        Color(0xFF059669), // Emerald 600
        Color(0xFF0891B2), // Cyan 600
        Color(0xFF4F46E5), // Indigo 600
    )
}

// ─── Motion ─────────────────────────────────────────────────────────

/**
 * Tutarlı hareket dili: Her animasyonun bir sebebi olmalı.
 * Hızlı ve minimal — profesyonel ekipman hissi.
 */
object FotoRaporMotion {

    /** Micro-interaction easing'i (butonlar, togglelar) */
    val EasingStandard = FastOutSlowInEasing

    /** Ekran geçişleri için easing */
    val EasingEmphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    /** Kısa, anlık feedback */
    val DurationShort = 150
    /** Standart UI animasyonları */
    val DurationMedium = 250
    /** Büyük geçişler */
    val DurationLong = 350

    /** Hafif basma geri bildirimi */
    fun <T> pressSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = 600f
    )

    /** Yumuşak açılma animasyonu */
    fun <T> enterTween() = tween<T>(
        durationMillis = DurationMedium,
        easing = EasingEmphasized
    )

    /** Hızlı kapanma animasyonu */
    fun <T> exitTween() = tween<T>(
        durationMillis = DurationShort,
        easing = EasingStandard
    )
}

// ─── Theme ──────────────────────────────────────────────────────────

@Composable
fun PhotoReportTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
