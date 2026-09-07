package com.dailycheckin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF4B3FE3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5EAFF),
    onPrimaryContainer = Color(0xFF1A1759),
    secondaryContainer = Color(0xFFEFEFF2),
    onSecondaryContainer = Color(0xFF171717),
    surface = Color(0xFFFDFDFF),
    surfaceVariant = Color(0xFFF2F3F6),
    background = Color(0xFFF7F7F8)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6054F1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3C2ECA),
    onPrimaryContainer = Color(0xFFCFD8FF),
    secondaryContainer = Color(0xFF262626),
    onSecondaryContainer = Color(0xFFE5E5E5),
    surface = Color(0xFF171717),
    surfaceVariant = Color(0xFF262626),
    background = Color(0xFF111113)
)

/** 任务强调色板：任务卡片头像与标签使用 */
val TaskColors = listOf(
    Color(0xFF4B3FE3),
    Color(0xFF1DC981),
    Color(0xFFF59E0B),
    Color(0xFF22A5F7),
    Color(0xFFE8463A),
    Color(0xFF14B8A6)
)

fun taskColor(index: Int): Color = TaskColors[index.mod(TaskColors.size)]

@Composable
fun DailyCheckinTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
