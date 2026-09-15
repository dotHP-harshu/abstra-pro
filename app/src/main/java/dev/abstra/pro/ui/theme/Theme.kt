package dev.abstra.pro.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

data class AbstraColors(
    val bgBase: Color,
    val bgSurface: Color,
    val surfaceAlt: Color,
    val primary: Color,
    val primaryStrong: Color,
    val textHeading: Color,
    val textBody: Color,
    val textMuted: Color,
    val accentHeart: Color,
    val border: Color,
    val white: Color
) {
    val bgBackground: Color get() = bgBase
    val bgCard: Color get() = bgSurface
    val textPrimary: Color get() = textHeading
    val textSecondary: Color get() = textBody
    val accent: Color get() = primary
}

enum class AppTheme(val displayName: String, val isDark: Boolean) {
    COTTON_CANDY("Cotton Candy", false),
    PEACH_BLOSSOM("Peach Blossom", false),
    MILKY_PINK("Milky Pink", false),
    NIGHT_BUBBLE("Night Bubble", true),
    VINTAGE_TAPE("Vintage Tape", true),
    MIDNIGHT_ROOM("Midnight Room", true),
    AMOLED_BLACK("Amoled Black", true);

    companion object {
        fun fromStored(name: String?): AppTheme {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true) }
                ?: COTTON_CANDY
        }
    }
}

val LocalAbstraColors = staticCompositionLocalOf {
    AbstraColors(
        bgBase = CottonCandyPalette.BgBase,
        bgSurface = CottonCandyPalette.BgSurface,
        surfaceAlt = CottonCandyPalette.SurfaceAlt,
        primary = CottonCandyPalette.Primary,
        primaryStrong = CottonCandyPalette.PrimaryStrong,
        textHeading = CottonCandyPalette.TextHeading,
        textBody = CottonCandyPalette.TextBody,
        textMuted = CottonCandyPalette.TextMuted,
        accentHeart = CottonCandyPalette.AccentHeart,
        border = CottonCandyPalette.Border,
        white = CottonCandyPalette.White
    )
}

@Composable
fun AbstraTheme(
    appTheme: AppTheme = AppTheme.COTTON_CANDY,
    content: @Composable () -> Unit
) {
    val abstraColors = when (appTheme) {
        AppTheme.COTTON_CANDY -> AbstraColors(
            bgBase = CottonCandyPalette.BgBase,
            bgSurface = CottonCandyPalette.BgSurface,
            surfaceAlt = CottonCandyPalette.SurfaceAlt,
            primary = CottonCandyPalette.Primary,
            primaryStrong = CottonCandyPalette.PrimaryStrong,
            textHeading = CottonCandyPalette.TextHeading,
            textBody = CottonCandyPalette.TextBody,
            textMuted = CottonCandyPalette.TextMuted,
            accentHeart = CottonCandyPalette.AccentHeart,
            border = CottonCandyPalette.Border,
            white = CottonCandyPalette.White
        )
        AppTheme.PEACH_BLOSSOM -> AbstraColors(
            bgBase = PeachBlossomPalette.BgBase,
            bgSurface = PeachBlossomPalette.BgSurface,
            surfaceAlt = PeachBlossomPalette.SurfaceAlt,
            primary = PeachBlossomPalette.Primary,
            primaryStrong = PeachBlossomPalette.PrimaryStrong,
            textHeading = PeachBlossomPalette.TextHeading,
            textBody = PeachBlossomPalette.TextBody,
            textMuted = PeachBlossomPalette.TextMuted,
            accentHeart = PeachBlossomPalette.AccentHeart,
            border = PeachBlossomPalette.Border,
            white = PeachBlossomPalette.White
        )
        AppTheme.MILKY_PINK -> AbstraColors(
            bgBase = MilkyPinkPalette.BgBase,
            bgSurface = MilkyPinkPalette.BgSurface,
            surfaceAlt = MilkyPinkPalette.SurfaceAlt,
            primary = MilkyPinkPalette.Primary,
            primaryStrong = MilkyPinkPalette.PrimaryStrong,
            textHeading = MilkyPinkPalette.TextHeading,
            textBody = MilkyPinkPalette.TextBody,
            textMuted = MilkyPinkPalette.TextMuted,
            accentHeart = MilkyPinkPalette.AccentHeart,
            border = MilkyPinkPalette.Border,
            white = MilkyPinkPalette.White
        )
        AppTheme.NIGHT_BUBBLE -> AbstraColors(
            bgBase = NightBubblePalette.BgBase,
            bgSurface = NightBubblePalette.BgSurface,
            surfaceAlt = NightBubblePalette.SurfaceAlt,
            primary = NightBubblePalette.Primary,
            primaryStrong = NightBubblePalette.PrimaryStrong,
            textHeading = NightBubblePalette.TextHeading,
            textBody = NightBubblePalette.TextBody,
            textMuted = NightBubblePalette.TextMuted,
            accentHeart = NightBubblePalette.AccentHeart,
            border = NightBubblePalette.Border,
            white = NightBubblePalette.White
        )
        AppTheme.VINTAGE_TAPE -> AbstraColors(
            bgBase = VintageTapePalette.BgBase,
            bgSurface = VintageTapePalette.BgSurface,
            surfaceAlt = VintageTapePalette.SurfaceAlt,
            primary = VintageTapePalette.Primary,
            primaryStrong = VintageTapePalette.PrimaryStrong,
            textHeading = VintageTapePalette.TextHeading,
            textBody = VintageTapePalette.TextBody,
            textMuted = VintageTapePalette.TextMuted,
            accentHeart = VintageTapePalette.AccentHeart,
            border = VintageTapePalette.Border,
            white = VintageTapePalette.White
        )
        AppTheme.MIDNIGHT_ROOM -> AbstraColors(
            bgBase = MidnightRoomPalette.BgBase,
            bgSurface = MidnightRoomPalette.BgSurface,
            surfaceAlt = MidnightRoomPalette.SurfaceAlt,
            primary = MidnightRoomPalette.Primary,
            primaryStrong = MidnightRoomPalette.PrimaryStrong,
            textHeading = MidnightRoomPalette.TextHeading,
            textBody = MidnightRoomPalette.TextBody,
            textMuted = MidnightRoomPalette.TextMuted,
            accentHeart = MidnightRoomPalette.AccentHeart,
            border = MidnightRoomPalette.Border,
            white = MidnightRoomPalette.White
        )
        AppTheme.AMOLED_BLACK -> AbstraColors(
            bgBase = AmoledBlackPalette.BgBase,
            bgSurface = AmoledBlackPalette.BgSurface,
            surfaceAlt = AmoledBlackPalette.SurfaceAlt,
            primary = AmoledBlackPalette.Primary,
            primaryStrong = AmoledBlackPalette.PrimaryStrong,
            textHeading = AmoledBlackPalette.TextHeading,
            textBody = AmoledBlackPalette.TextBody,
            textMuted = AmoledBlackPalette.TextMuted,
            accentHeart = AmoledBlackPalette.AccentHeart,
            border = AmoledBlackPalette.Border,
            white = AmoledBlackPalette.White
        )
    }

    val colorScheme = if (appTheme.isDark) {
        darkColorScheme(
            primary = abstraColors.primary,
            onPrimary = abstraColors.white,
            surface = abstraColors.bgSurface,
            onSurface = abstraColors.textHeading,
            background = abstraColors.bgBase,
            onBackground = abstraColors.textBody,
            surfaceVariant = abstraColors.surfaceAlt,
            onSurfaceVariant = abstraColors.textMuted
        )
    } else {
        lightColorScheme(
            primary = abstraColors.primary,
            onPrimary = abstraColors.white,
            surface = abstraColors.bgSurface,
            onSurface = abstraColors.textHeading,
            background = abstraColors.bgBase,
            onBackground = abstraColors.textBody,
            surfaceVariant = abstraColors.surfaceAlt,
            onSurfaceVariant = abstraColors.textMuted
        )
    }

    CompositionLocalProvider(LocalAbstraColors provides abstraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AbstraTypography,
            content = content
        )
    }
}
