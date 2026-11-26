package com.capji.hardwarelab.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capji.hardwarelab.core.tr
import com.capji.hardwarelab.ui.components.CapjiHardwareLogo
import com.capji.hardwarelab.ui.theme.Cx
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val fade = remember { Animatable(0f) }
    val t = rememberInfiniteTransition(label = "splash")
    val wave by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "wave",
    )
    LaunchedEffect(Unit) {
        fade.animateTo(1f, tween(900))
        delay(1700)
        onDone()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF0E2236), Cx.Obsidian))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f - 40f)
            for (k in 0..3) {
                val ph = (wave + k * 0.25f) % 1f
                drawCircle(
                    color = Cx.Cyan.copy(alpha = (1f - ph) * 0.35f),
                    radius = 90.dp.toPx() + ph * size.minDimension * 0.55f,
                    center = c,
                    style = Stroke(2.dp.toPx()),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.alpha(fade.value),
        ) {
            CapjiHardwareLogo(Modifier.size(190.dp))
            Text("CAPJI", color = Cx.TextHi, fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp)
            Text("HARDWARE LAB", color = Cx.Cyan, fontSize = 16.sp, letterSpacing = 6.sp, fontWeight = FontWeight.SemiBold)
            Text(
                tr("مختبر فحص العتاد الاحترافي", "Professional hardware inspection lab"),
                color = Cx.TextLo,
                fontSize = 13.sp,
            )
        }
    }
}
