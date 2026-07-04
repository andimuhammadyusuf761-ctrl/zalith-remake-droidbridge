package com.movtery.zalithlauncher.ui.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val ZalithTeal = Color(0xFF2DE2C4)
val ZalithDeep = Color(0xFF071111)
val ZalithCard = Color(0xCC10201F)
val ZalithWarn = Color(0xFFFFB74D)

@Immutable
data class PremiumMotion(
    val quick: Int = 180,
    val standard: Int = 320,
    val expressive: Int = 520
) {
    fun <T> standardTween() = tween<T>(durationMillis = standard, easing = FastOutSlowInEasing)
}

object ZalithPremiumTheme {
    val colorScheme = darkColorScheme(
        primary = ZalithTeal,
        onPrimary = Color(0xFF00201B),
        primaryContainer = Color(0xFF005047),
        secondary = Color(0xFF8FDAD0),
        tertiary = Color(0xFFB7C7FF),
        background = ZalithDeep,
        surface = Color(0xFF0B1716),
        surfaceVariant = Color(0xFF18302E),
        onSurface = Color(0xFFE5F3F1),
        onSurfaceVariant = Color(0xFFB8CCC8),
        error = Color(0xFFFFB4AB)
    )
}

@Composable
fun ZalithTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ZalithPremiumTheme.colorScheme, content = content)
}

@Composable
fun PremiumBackground(contentPadding: PaddingValues = PaddingValues(0.dp), content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF06100F), Color(0xFF0A2523), Color(0xFF020706)),
                    start = Offset.Zero,
                    end = Offset(1200f, 1800f)
                )
            )
            .padding(contentPadding)
    ) { content() }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 28.dp,
    elevated: Boolean = true,
    warning: Boolean = false,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(radius)
    Surface(
        modifier = modifier
            .then(if (elevated) Modifier.shadow(18.dp, shape, ambientColor = ZalithTeal.copy(alpha = 0.18f), spotColor = Color.Black) else Modifier)
            .border(
                BorderStroke(1.dp, if (warning) ZalithWarn.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.12f)),
                shape
            ),
        shape = shape,
        color = if (warning) Color(0x332A1800) else ZalithCard,
        tonalElevation = if (elevated) 6.dp else 0.dp,
        shadowElevation = if (elevated) 10.dp else 0.dp,
        content = content
    )
}

object ZalithDesignPlan {
    val improvementPlan = listOf(
        "Adopt one Compose Material 3 theme with dynamic teal accents, 8dp spacing rhythm, and large touch targets.",
        "Move Home, Versions, Play, and Settings to shared PremiumBackground, GlassCard, and motion primitives.",
        "Use fade/scale transitions for screen swaps, parallax hero panels on tablets, and shared version artwork between list and detail.",
        "Standardize empty, loading, success, and warning states with illustrations, haptics, and accessible labels.",
        "Keep advanced/risky actions visually isolated with amber outlines and confirmation copy."
    )
}
