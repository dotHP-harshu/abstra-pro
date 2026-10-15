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