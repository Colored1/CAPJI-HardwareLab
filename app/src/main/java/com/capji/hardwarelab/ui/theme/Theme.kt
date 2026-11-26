package com.capji.hardwarelab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Cx {
    val Obsidian = Color(0xFF090D16)
    val Surface = Color(0xFF0F1624)
    val SurfaceHi = Color(0xFF162033)
    val Cyan = Color(0xFF00E5FF)
    val Emerald = Color(0xFF00E676)
    val Red = Color(0xFFFF3D57)
    val Amber = Color(0xFFFFB300)
    val TextHi = Color(0xFFE8F1F8)
    val TextLo = Color(0xFF8A9BB0)
    val Stroke = Color(0x22FFFFFF)
}

private val CapjiScheme = darkColorScheme(
    primary = Cx.Cyan,
    onPrimary = Cx.Obsidian,
    secondary = Cx.Emerald,
    onSecondary = Cx.Obsidian,
    background = Cx.Obsidian,
    onBackground = Cx.TextHi,
    surface = Cx.Surface,
    onSurface = Cx.TextHi,
    surfaceVariant = Cx.SurfaceHi,
    onSurfaceVariant = Cx.TextLo,
    error = Cx.Red,
    outline = Cx.Stroke,
)

@Composable
fun CapjiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CapjiScheme, typography = Typography(), content = content)
}
