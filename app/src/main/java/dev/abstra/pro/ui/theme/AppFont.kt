package dev.abstra.pro.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.abstra.pro.R

/**
 * User-selectable font families.  Each entry bundles a regular + bold
 * weight so we can keep using FontWeight.Normal / FontWeight.Bold
 * throughout the codebase.
 *
 * To add a new font:
 *   1. Drop the .ttf/.otf file into `res/font/`
 *   2. Add a FontFamily entry below
 *   3. Add a label in `fontDisplayName()`
 */
enum class AppFont(
    val displayName: String,
    val fontFamily: FontFamily,
) {
    SYSTEM(
        displayName = "System Sans",
        fontFamily = FontFamily.Default,
    ),
    QUICKSAND(
        displayName = "Quicksand",
        // Loaded from res/font/quicksand_*.xml
        fontFamily = FontFamily.Default,
    ),
    INTER(
        displayName = "Inter",
        fontFamily = FontFamily.Default,
    ),
    SERIF(
        displayName = "Serif",
        fontFamily = FontFamily.Serif,
    ),
    MONOSPACE(
        displayName = "Monospace",
        fontFamily = FontFamily.Monospace,
    );

    companion object {
        val DEFAULT = SYSTEM

        fun fromKey(key: String?): AppFont =
            values().firstOrNull { it.name == key } ?: DEFAULT
    }
}

/**
 * Composition local for the currently-selected font.  Theme composables
 * read this to rebuild typography on the fly when the user picks a new
 * font in Settings.
 */
val LocalAppFont = staticCompositionLocalOf { AppFont.DEFAULT }