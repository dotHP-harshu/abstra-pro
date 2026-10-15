package dev.abstra.pro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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
