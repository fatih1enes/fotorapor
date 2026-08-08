package com.fatihenes.photoreport.core.designsystem.theme

import android.app.Activity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
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
//
// Space Grotesk (Display/Headline) + Manrope (Body/Title/Label).
// Bkz. Font.kt — her iki font da APK içine gömülü, çevrimdışı güvenli.

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = (-1.2).sp
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 35.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.15.sp
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.sp
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp
    )
)

// ─── Shapes ─────────────────────────────────────────────────────────
//
// Hafifçe daha "kesin/mimari" bir köşe skalası: küçük bileşenlerde
// daha az yuvarlak (teknik/hassas bir alet hissi), büyük yüzeylerde
// yumuşak ve premium.

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
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
    val RadiusXS: Dp = 4.dp
    val RadiusS: Dp = 8.dp
    val RadiusM: Dp = 12.dp
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

    // ── Tinted Shadow Renkleri ──────────────────────────────────
    // Salt siyah gölge yerine markaya ait, çok düşük opaklıklı bir
    // "ink" gölgesi — premium ürünlerin (Linear, Stripe vb.) imzası.
    val ShadowColorLight: Color = Indigo900.copy(alpha = 0.16f)
    val ShadowColorDark: Color = Color.Black.copy(alpha = 0.55f)

    // ── Proje Etiket Paleti ─────────────────────────────────────
    // Aynı ailenin üyesi hissettiren, birbirinden net ayrışan 8 ton.
    val ProjectColors = listOf(
        Indigo500,
        Brass500,
        PinePrimary,
        TerracottaPrimary,
        WinePrimary,
        PetrolPrimary,
        PlumPrimary,
        OlivePrimary,
    )
}

// ─── Gradients ──────────────────────────────────────────────────────

/**
 * FotoRapor imza gradyanları.
 * Kullanım alanı bilinçli olarak sınırlı tutulur: hero başlıkları,
 * splash ve öne çıkan rozet/kartlar gibi "marka anları". Genel arayüz
 * yüzeyleri düz renk kalır — gradyan enflasyonu premium hissi zayıflatır.
 */
@Immutable
object FotoRaporGradients {
    /** Koyu, otoriter hero yüzeyi (splash, öne çıkan başlık bandı). */
    val InkHero = Brush.linearGradient(listOf(Indigo900, Graphite950))

    /** Light tema hero yüzeyi — kağıt üstü derin indigo. */
    val InkHeroLight = Brush.linearGradient(listOf(Indigo700, Indigo900))

    /** Brass parıltısı — rozet, öne çıkan istatistik, "pro" vurgusu. */
    val BrassSheen = Brush.linearGradient(listOf(Brass300, Brass600))
}

// ─── Motion ─────────────────────────────────────────────────────────

/**
 * Tutarlı hareket dili: Her animasyonun bir sebebi olmalı.
 * Hızlı ve minimal — profesyonel ekipman hissi.
 */
object FotoRaporMotion {

    // ── Easing ───────────────────────────────────────────────────
    /** Micro-interaction easing'i (butonlar, togglelar) */
    val EasingStandard = FastOutSlowInEasing

    /** Ekran geçişleri için easing — Material 3 Emphasized */
    val EasingEmphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    // ── Duration Tiers ──────────────────────────────────────────
    /** Kısa, anlık feedback */
    const val DurationShort = 150
    /** Standart UI animasyonları */
    const val DurationMedium = 250
    /** Büyük geçişler */
    const val DurationLong = 350

    // ── Navigation Duration Tiers ───────────────────────────────
    // Simetrik: ileri = geri aynı hız. Ekran tipine göre kademelendirilmiş.
    /** Yardımcı/hafif ekranlar: Settings, Trash — hızlı giriş/çıkış */
    const val NavDurationFast = 250
    /** İçerik ekranları: Project Detail — biraz daha dramatik */
    const val NavDurationStandard = 300
    /** Overlay / dialog geçişleri */
    const val NavDurationOverlay = 200

    // ── Parallax Oranı ──────────────────────────────────────────
    /** Arka planda kalan ekranın kayma yüzdesi (iOS-tarzı parallax) */
    private const val PARALLAX_FRACTION = 0.30f

    // ── Spring Presets ──────────────────────────────────────────
    /** Hafif basma geri bildirimi */
    fun <T> pressSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = 600f
    )

    // ── Tween Presets ───────────────────────────────────────────
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

    // ── Navigation Transition Factories ─────────────────────────
    // Her tier simetrik: enter ve popExit aynı yöne, exit ve popEnter karşı yöne.

    /**
     * Forward navigasyon enter: sağdan slide-in + fade-in.
     * @param durationMs geçiş süresi
     */
    fun navEnter(durationMs: Int = NavDurationStandard): EnterTransition =
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = durationMs, easing = EasingEmphasized)
        ) + fadeIn(
            animationSpec = tween(durationMillis = (durationMs * 0.5f).toInt(), easing = EasingStandard)
        )

    /**
     * Forward navigasyon exit: sola parallax slide + fade-out.
     * @param durationMs geçiş süresi
     */
    fun navExit(durationMs: Int = NavDurationStandard): ExitTransition =
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> -(fullWidth * PARALLAX_FRACTION).toInt() },
            animationSpec = tween(durationMillis = durationMs, easing = EasingEmphasized)
        ) + fadeOut(
            animationSpec = tween(durationMillis = (durationMs * 0.4f).toInt(), easing = EasingStandard)
        )

    /**
     * Pop (geri) navigasyon enter: soldan parallax slide-in + fade-in.
     * @param durationMs geçiş süresi
     */
    fun navPopEnter(durationMs: Int = NavDurationStandard): EnterTransition =
        slideInHorizontally(
            initialOffsetX = { fullWidth -> -(fullWidth * PARALLAX_FRACTION).toInt() },
            animationSpec = tween(durationMillis = durationMs, easing = EasingEmphasized)
        ) + fadeIn(
            animationSpec = tween(durationMillis = (durationMs * 0.5f).toInt(), easing = EasingStandard)
        )

    /**
     * Pop (geri) navigasyon exit: sağa slide-out + fade-out.
     * @param durationMs geçiş süresi
     */
    fun navPopExit(durationMs: Int = NavDurationStandard): ExitTransition =
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = durationMs, easing = EasingEmphasized)
        ) + fadeOut(
            animationSpec = tween(durationMillis = (durationMs * 0.4f).toInt(), easing = EasingStandard)
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
