package com.example.ui.theme

import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.util.Locale

private val LightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = OnCobalt,
    primaryContainer = CobaltContainer,
    onPrimaryContainer = OnCobaltContainer,
    secondary = EmeraldSecondary,
    onSecondary = OnEmerald,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = OnEmeraldContainer,
    tertiary = AmberTertiary,
    onTertiary = OnAmber,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = ParchmentBackground,
    onBackground = CharcoalInk,
    surface = CrispSurface,
    onSurface = CharcoalInk,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = SlateInkSecondary,
    outline = WarmOutline,
    outlineVariant = WarmOutline.copy(alpha = 0.6f),
    error = CrimsonError,
    errorContainer = CrimsonContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = SkyCobaltDark,
    onPrimary = OnCobaltDark,
    primaryContainer = CobaltContainerDark,
    onPrimaryContainer = OnCobaltContainerDark,
    secondary = MintEmeraldDark,
    onSecondary = OnEmeraldDark,
    secondaryContainer = EmeraldContainerDark,
    onSecondaryContainer = OnEmeraldContainerDark,
    tertiary = GoldAmberDark,
    onTertiary = OnGoldDark,
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = OnAmberContainerDark,
    background = MidnightBackground,
    onBackground = IvoryInkDark,
    surface = SlateSurfaceDark,
    onSurface = IvoryInkDark,
    surfaceVariant = ElevatedSlateDark,
    onSurfaceVariant = MutedSlateDark,
    outline = SubtleOutlineDark,
    outlineVariant = SubtleOutlineDark.copy(alpha = 0.6f),
    error = CoralErrorDark,
    errorContainer = CoralContainerDark,
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val LocalAppResources = staticCompositionLocalOf<Resources> {
    error("LocalAppResources not provided")
}

@Composable
@ReadOnlyComposable
fun tr(@StringRes id: Int): String = LocalAppResources.current.getString(id)

@Composable
@ReadOnlyComposable
fun tr(@StringRes id: Int, vararg formatArgs: Any): String =
    LocalAppResources.current.getString(id, *formatArgs)

@Composable
fun EnglishHelperTheme(
    languageCode: String = "en",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val isArabic = languageCode == "ar"
    val baseContext = LocalContext.current
    val localizedResources = remember(baseContext, languageCode) {
        val locale = Locale.forLanguageTag(languageCode)
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        baseContext.createConfigurationContext(config).resources
    }
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val typography = remember(isArabic) { buildAppTypography(isArabic) }

    CompositionLocalProvider(
        LocalAppResources provides localizedResources,
        LocalLayoutDirection provides layoutDirection,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = AppShapes,
            content = content,
        )
    }
}
