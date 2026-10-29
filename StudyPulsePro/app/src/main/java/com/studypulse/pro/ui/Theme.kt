package com.studypulse.pro.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Black = Color(0xFF000000)
val GlassC = Color(0xFF171717)
val Yellow = Color(0xFFFFD600)
val TextP = Color(0xFFFFFFFF)
val TextS = Color(0xFFAAAAAA)

private val Scheme = darkColorScheme(
    primary = Yellow, onPrimary = Black, secondary = Yellow,
    background = Black, onBackground = TextP,
    surface = GlassC, onSurface = TextP,
    surfaceVariant = GlassC, onSurfaceVariant = TextS
)

@Composable
fun AppTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = Scheme, typography = Typography(), content = content)
