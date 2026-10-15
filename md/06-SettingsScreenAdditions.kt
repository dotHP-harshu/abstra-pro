// =====================================================================
// 06-SettingsScreenAdditions.kt
// ---------------------------------------------------------------------
// Two new preference rows + the Compose helper they share. Designed to
// be appended to your existing Settings screen — either inside the
// existing "Appearance" / "Customization" section, or as a brand new
// section called "Home Page Style".
//
// What this gives the user:
//   - Font family picker (5 options, see AppFont)
//   - Name style picker (5 options, see GreetingStyle)
//   - Live preview tile so the user sees their choice before saving
//
// Why a preview tile:
//   Lune's CustomizationActivity shows each option as a chip with its
//   own preview typography.  We adopt that pattern so the choice is
//   "WYSIWYG" — no surprises after applying.
// =====================================================================
package dev.abstra.pro.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abstra.pro.data.prefs.AppPreferences
import dev.abstra.pro.ui.theme.AbstraColors
import dev.abstra.pro.ui.theme.AppFont
import dev.abstra.pro.ui.theme.GreetingStyle
import dev.abstra.pro.ui.theme.LocalAbstraColors

/**
 * The combined customization surface for the home page.
 *
 * Add this composable to your settings tree (typically right under the
 * "Greeting" toggle):
 *
 *     SettingsSection("Home Page Style") {
 *         FontFamilyPicker()
 *         NameStylePicker()
 *     }
 */
@Composable
fun HomePageStyleSection() {
    val colors = LocalAbstraColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        FontFamilyPicker(colors)
        NameStylePicker(colors)
    }
}

// ---------------------------------------------------------------------
// Font family picker
// ---------------------------------------------------------------------
@Composable
fun FontFamilyPicker(colors: AbstraColors) {
    val selected by AppPreferences.fontFlow.collectAsStateWithLifecycle(
        initialValue = AppFont.DEFAULT
    )

    SectionRow(
        title = "Font Family",
        subtitle = "Choose how text looks across the app.",
        icon = Icons.Default.FormatSize,
        colors = colors,
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(AppFont.values()) { font ->
                FontChip(
                    font = font,
                    selected = font == selected,
                    colors = colors,
                    onClick = { AppPreferences.selectedFont = font }
                )
            }
        }
    }
}

@Composable
private fun FontChip(
    font: AppFont,
    selected: Boolean,
    colors: AbstraColors,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) colors.primary else colors.bgCard,
        contentColor = if (selected) Color.White else colors.textPrimary,
        modifier = Modifier
            .width(120.dp)
            .height(72.dp)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.primary else colors.border,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Aa",
                fontFamily = font.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = font.displayName,
                    fontFamily = font.fontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                if (selected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// Name style picker
// ---------------------------------------------------------------------
@Composable
fun NameStylePicker(colors: AbstraColors) {
    val selected by AppPreferences.greetingStyleFlow.collectAsStateWithLifecycle(
        initialValue = GreetingStyle.DEFAULT
    )

    SectionRow(
        title = "Name Style",
        subtitle = "How your name appears on the home greeting.",
        icon = Icons.Default.Person,
        colors = colors,
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(GreetingStyle.values()) { style ->
                StyleChip(
                    style = style,
                    selected = style == selected,
                    colors = colors,
                    onClick = { AppPreferences.greetingStyle = style }
                )
            }
        }
    }
}

@Composable
private fun StyleChip(
    style: GreetingStyle,
    selected: Boolean,
    colors: AbstraColors,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) colors.bgCard else colors.bgCard.copy(alpha = 0.5f),
        modifier = Modifier
            .width(140.dp)
            .height(120.dp)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.primary else colors.border,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Alex",
                color = colors.primary,
                style = when (style) {
                    GreetingStyle.CLASSIC -> MaterialTheme.typography.titleLarge.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                    )
                    GreetingStyle.MINIMAL -> MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium,
                    )
                    GreetingStyle.BOLD -> MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                    )
                    GreetingStyle.GRADIENT -> MaterialTheme.typography.headlineSmall.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                    )
                    GreetingStyle.ELEGANT -> MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                        letterSpacing = 3.sp,
                    )
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = style.displayName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )
                if (selected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = colors.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// Shared visual primitive (header + content slot).  If you already
// have an equivalent in your codebase, replace this with your helper.
// ---------------------------------------------------------------------
@Composable
fun SectionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colors: AbstraColors,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textHeading,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        content()
    }
}