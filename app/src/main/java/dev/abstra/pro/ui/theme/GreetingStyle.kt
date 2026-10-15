package dev.abstra.pro.ui.theme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * How the user's name is rendered on the home hero card.
 *
 *   CLASSIC   — heart prefix, italic + gradient underline (current default)
 *   MINIMAL   — no decoration, plain bold text
 *   BOLD      — uppercase, extra-bold, letter-spaced
 *   GRADIENT  — italic + filled gradient via Brush text style
 *   ELEGANT   — serif, wide letter-spacing, no decoration
 *
 * Add a new style by:
 *   1. Adding an enum constant
 *   2. Implementing the `textStyle` override below
 *   3. Handling the visual quirks in GreetingHeroCard (the file ships a
 *      helper switch that covers all five built-in styles).
 */
enum class GreetingStyle(
    val displayName: String,
    val description: String,
) {
    CLASSIC(
        displayName = "Classic",
        description = "Heart prefix, italic + soft gradient underline",
    ),
    MINIMAL(
        displayName = "Minimal",
        description = "Clean and simple, no decorations",
    ),
    BOLD(
        displayName = "Bold",
        description = "Uppercase, extra-bold, letter-spaced",
    ),
    GRADIENT(
        displayName = "Gradient",
        description = "Italic with vibrant gradient fill",
    ),
    ELEGANT(
        displayName = "Elegant",
        description = "Serif typeface with wide tracking",
    );

    companion object {
        val DEFAULT = CLASSIC

        fun fromKey(key: String?): GreetingStyle =
            values().firstOrNull { it.name == key } ?: DEFAULT
    }
}