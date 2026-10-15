// =====================================================================
// 04-FontCustomization.kt
// ---------------------------------------------------------------------
// Adds a multi-font typography pipeline to Abstra Pro, modeled after
// Lune's Type.kt + SettingsManager pattern.
//
// Files added/updated:
//
//   - app/src/main/java/dev/abstra/pro/ui/theme/AppFont.kt           (new)
//   - app/src/main/java/dev/abstra/pro/ui/theme/Type.kt              (updated)
//   - app/src/main/java/dev/abstra/pro/data/prefs/AppPreferences.kt  (new)
//   - app/src/main/res/font/quicksand_regular.xml                    (new)
//   - app/src/main/res/font/quicksand_bold.xml                       (new)
//   - app/src/main/res/font/inter_regular.xml                        (new)
//   - app/src/main/res/font/inter_bold.xml                           (new)
//
// Why:
//   Abstra's existing typography was hard-coded to SansSerif +
//   Monospace.  Lune uses a custom `quicksand` family.  We extend the
//   system to a small, opinionated palette (System Sans, Quicksand,
//   Inter, Serif, Monospace) that the user can pick from a Settings
//   screen.  Settings are persisted via SharedPreferences and exposed
//   as a StateFlow so the Compose theme reacts immediately.
// =====================================================================

// =====================================================================
// A. AppFont.kt (new file)
// =====================================================================
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
        fontFamily = FontFamily(
            Font(R.font.quicksand_regular, FontWeight.Normal),
            Font(R.font.quicksand_bold, FontWeight.Bold),
        ),
    ),
    INTER(
        displayName = "Inter",
        fontFamily = FontFamily(
            Font(R.font.inter_regular, FontWeight.Normal),
            Font(R.font.inter_bold, FontWeight.Bold),
        ),
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

// =====================================================================
// B. Type.kt (replaces the existing AbstraTypography)
// =====================================================================
package dev.abstra.pro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Reactive typography: every TextStyle uses the user's chosen font.
 * Switch fonts in Settings and the entire UI updates.
 */
val AbstraTypography: Typography
    @Composable
    @ReadOnlyComposable
    get() {
        val font = LocalAppFont.current.fontFamily
        return Typography(
            headlineLarge = TextStyle(
                fontFamily = font, fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp, lineHeight = 34.sp
            ),
            headlineMedium = TextStyle(
                fontFamily = font, fontWeight = FontWeight.Bold,
                fontSize = 22.sp, lineHeight = 28.sp
            ),
            titleLarge = TextStyle(
                fontFamily = font, fontWeight = FontWeight.Bold,
                fontSize = 18.sp, lineHeight = 24.sp
            ),
            titleMedium = TextStyle(
                fontFamily = font, fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp, lineHeight = 20.sp
            ),
            titleSmall = TextStyle(
                fontFamily = font, fontWeight = FontWeight.Medium,
                fontSize = 13.sp, lineHeight = 18.sp
            ),
            bodyLarge = TextStyle(
                fontFamily = font, fontWeight = FontWeight.Normal,
                fontSize = 15.sp, lineHeight = 20.sp
            ),
            bodyMedium = TextStyle(
                fontFamily = font, fontWeight = FontWeight.Normal,
                fontSize = 13.sp, lineHeight = 18.sp
            ),
            bodySmall = TextStyle(
                fontFamily = font, fontWeight = FontWeight.Normal,
                fontSize = 12.sp, lineHeight = 16.sp
            ),
            labelLarge = TextStyle(
                fontFamily = font, fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            labelMedium = TextStyle(
                fontFamily = font, fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            ),
            labelSmall = TextStyle(
                fontFamily = font, fontWeight = FontWeight.Normal,
                fontSize = 10.sp
            ),
        )
    }

// =====================================================================
// C. AppPreferences.kt — single source of truth for live UI settings.
// =====================================================================
package dev.abstra.pro.data.prefs

import android.content.Context
import android.content.SharedPreferences
import dev.abstra.pro.ui.theme.AppFont
import dev.abstra.pro.ui.theme.GreetingStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lightweight SharedPreferences wrapper that exposes the values the UI
 * cares about as StateFlows.  Composables can `collectAsState()` so the
 * entire app re-themes instantly when the user picks a new value.
 *
 * Why a separate object:
 *   - `SettingsDao`/`SettingsRepository` is async and Room-driven.  For
 *     hot values like font & name style we want immediate, in-memory
 *     reads and writes.
 *   - Mirrors Lune's `SettingsManager.getInstance(context)` pattern.
 */
object AppPreferences {

    private const val FILE = "abstra_ui_prefs"
    private const val KEY_FONT = "selected_font"
    private const val KEY_GREETING_STYLE = "greeting_style"

    private lateinit var prefs: SharedPreferences

    private val _fontFlow = MutableStateFlow(AppFont.DEFAULT)
    val fontFlow: StateFlow<AppFont> = _fontFlow.asStateFlow()

    private val _greetingStyleFlow = MutableStateFlow(GreetingStyle.DEFAULT)
    val greetingStyleFlow: StateFlow<GreetingStyle> = _greetingStyleFlow.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
        // Prime flows from disk
        _fontFlow.value = AppFont.fromKey(prefs.getString(KEY_FONT, null))
        _greetingStyleFlow.value =
            GreetingStyle.fromKey(prefs.getString(KEY_GREETING_STYLE, null))

        // Listen for cross-process changes (rare but possible if a
        // widget updates prefs).
        prefs.registerOnSharedPreferenceChangeListener { _, key ->
            when (key) {
                KEY_FONT -> _fontFlow.value = AppFont.fromKey(prefs.getString(key, null))
                KEY_GREETING_STYLE -> _greetingStyleFlow.value =
                    GreetingStyle.fromKey(prefs.getString(key, null))
            }
        }
    }

    var selectedFont: AppFont
        get() = _fontFlow.value
        set(value) {
            prefs.edit().putString(KEY_FONT, value.name).apply()
            _fontFlow.value = value
        }

    var greetingStyle: GreetingStyle
        get() = _greetingStyleFlow.value
        set(value) {
            prefs.edit().putString(KEY_GREETING_STYLE, value.name).apply()
            _greetingStyleFlow.value = value
        }
}

// =====================================================================
// D. Wire it into Application / MainActivity
// =====================================================================
/*
    In LuneApplication.onCreate() (or wherever you bootstrap), call:
        AppPreferences.init(this)

    In your root Composable, wrap content in:
        CompositionLocalProvider(
            LocalAppFont provides AppPreferences.selectedFont,
        ) {
            AbstraTheme { ... }
        }
*/

// =====================================================================
// E. res/font/quicksand_regular.xml — example downloadable font def
// =====================================================================
/*
<?xml version="1.0" encoding="utf-8"?>
<font-family xmlns:app="http://schemas.android.com/apk/res-auto"
    app:fontProviderAuthority="com.google.android.gms.fonts"
    app:fontProviderPackage="com.google.android.gms"
    app:fontProviderQuery="Quicksand"
    app:fontProviderCerts="@array/com_google_android_gms_fonts_certs">
</font-family>
*/
// Drop the matching quicksand_bold.xml, inter_regular.xml, inter_bold.xml
// in the same folder using the same provider scheme (or bundle .ttf
// files directly under res/font/ for a guaranteed offline experience).

// =====================================================================
// F. Where to declare `GreetingStyle` — see file 05-NAME_STYLE_CUSTOMIZATION.kt
// =====================================================================