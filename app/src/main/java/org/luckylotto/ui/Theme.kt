// SPDX-License-Identifier: GPL-3.0-or-later
package org.luckylotto.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Forest = Color(0xFF145C43)
val Lime = Color(0xFFD3F5B4)
private val light = lightColorScheme(
    primary = Forest, onPrimary = Color.White,
    primaryContainer = Color(0xFFD3EDD8), onPrimaryContainer = Color(0xFF103B2B),
    secondary = Color(0xFF536450), secondaryContainer = Color(0xFFE4EDDA),
    tertiary = Color(0xFF806028), tertiaryContainer = Color(0xFFFFE7B5),
    background = Color(0xFFF8FAF4), surface = Color(0xFFF8FAF4),
    surfaceContainer = Color(0xFFEEF2E9), surfaceContainerLow = Color(0xFFF1F5ED),
    surfaceContainerLowest = Color.White, surfaceContainerHigh = Color(0xFFE7EDE2),
    onSurface = Color(0xFF19241D), onSurfaceVariant = Color(0xFF5C685E),
    outlineVariant = Color(0xFFD7DFD3),
)
private val dark = darkColorScheme(
    primary = Color(0xFFA7D7B2), onPrimary = Color(0xFF123925),
    primaryContainer = Color(0xFF2D503C), onPrimaryContainer = Color(0xFFCFEDD5),
    secondaryContainer = Color(0xFF384C3A), background = Color(0xFF111812),
    surface = Color(0xFF111812), surfaceContainer = Color(0xFF1D271F),
    surfaceContainerLow = Color(0xFF172119), surfaceContainerLowest = Color(0xFF0C130E),
    surfaceContainerHigh = Color(0xFF29342A), onSurface = Color(0xFFE0EADF),
    onSurfaceVariant = Color(0xFFBAC8B8), outlineVariant = Color(0xFF3D4B3D),
)
@Composable
fun LuckyTheme(theme: String, dynamic: Boolean, content: @Composable () -> Unit) {
    val isDark = if (theme == "system") isSystemInDarkTheme() else theme == "dark"
    val context = LocalContext.current
    val colors = if (dynamic && Build.VERSION.SDK_INT >= 31) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (isDark) dark else light
    MaterialTheme(
        colorScheme = colors,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(32.dp)),
        typography = Typography(
            displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-1).sp),
            headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp),
            headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
            titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
        ), content = content
    )
}
