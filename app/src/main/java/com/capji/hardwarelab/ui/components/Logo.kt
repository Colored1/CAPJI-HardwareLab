package com.capji.hardwarelab.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.capji.hardwarelab.ui.theme.Cx

/** Animated CAPJI emblem: microchip + "C" monogram + diagnostic pulse, with radar sweep and pulsing waves. */
@Composable
fun CapjiHardwareLogo(modifier: Modifier = Modifier.size(120.dp), animated: Boolean = true) {
    val t = rememberInfiniteTransition(label = "logo")
    val spin by t.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing)),
        label = "spin",
    )
    val pulse by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearOutSlowInEasing)),
        label = "pulse",
    )
    val trace by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "trace",
    )
    Canvas(modifier) {
        val s = size.minDimension / 100f
        val c = Offset(size.width / 2f, size.height / 2f)
        val pulseV = if (animated) pulse else 0.4f
        val spinV = if (animated) spin else 40f
        val traceV = if (animated) trace else 1f

        // pulsing circuit waves
        for (k in 0..1) {
            val ph = (pulseV + k * 0.5f) % 1f
            drawCircle(
                color = Cx.Cyan.copy(alpha = (1f - ph) * 0.45f),
                radius = (30f + 20f * ph) * s,
                center = c,
                style = Stroke(1.6f * s),
            )
        }
        // radar sweep
        rotate(spinV, pivot = c) {
            drawCircle(
                brush = Brush.sweepGradient(listOf(Color.Transparent, Cx.Cyan.copy(alpha = 0.32f)), center = c),
                radius = 46f * s,
                center = c,
            )
        }
        drawCircle(Cx.Cyan.copy(alpha = 0.35f), radius = 46f * s, center = c, style = Stroke(1.2f * s))

        // chip body
        val body = 40f * s
        val tl = Offset(c.x - body / 2f, c.y - body / 2f)
        drawRoundRect(Color(0xE60B1624), tl, Size(body, body), CornerRadius(4f * s))
        drawRoundRect(Cx.Cyan, tl, Size(body, body), CornerRadius(4f * s), style = Stroke(2.6f * s))
        // pins
        val pinStroke = Stroke(2.4f * s, cap = StrokeCap.Round)
        for (k in 0..2) {
            val off = (-10f + 10f * k) * s
            drawLine(Cx.Cyan, Offset(c.x + off, tl.y), Offset(c.x + off, tl.y - 7f * s), strokeWidth = pinStroke.width, cap = StrokeCap.Round)
            drawLine(Cx.Cyan, Offset(c.x + off, tl.y + body), Offset(c.x + off, tl.y + body + 7f * s), strokeWidth = pinStroke.width, cap = StrokeCap.Round)
            drawLine(Cx.Cyan, Offset(tl.x, c.y + off), Offset(tl.x - 7f * s, c.y + off), strokeWidth = pinStroke.width, cap = StrokeCap.Round)
            drawLine(Cx.Cyan, Offset(tl.x + body, c.y + off), Offset(tl.x + body + 7f * s, c.y + off), strokeWidth = pinStroke.width, cap = StrokeCap.Round)
        }
        // C monogram (opening to the right)
        val r = 9f * s
        drawArc(
            color = Cx.TextHi,
            startAngle = 40f,
            sweepAngle = 280f,
            useCenter = false,
            topLeft = Offset(c.x - 5f * s - r, c.y - r),
            size = Size(2 * r, 2 * r),
            style = Stroke(4f * s, cap = StrokeCap.Round),
        )
        // diagnostic pulse trace
        val path = Path().apply {
            moveTo(c.x - 5f * s, c.y)
            lineTo(c.x + 4f * s, c.y)
            lineTo(c.x + 7f * s, c.y - 10f * s)
            lineTo(c.x + 12f * s, c.y + 10f * s)
            lineTo(c.x + 15f * s, c.y)
            lineTo(c.x + 34f * s, c.y)
        }
        val pm = PathMeasure()
        pm.setPath(path, false)
        val len = pm.length
        val end = len * traceV
        val start = (end - len * 0.55f).coerceAtLeast(0f)
        val seg = Path()
        pm.getSegment(start, end, seg, true)
        drawPath(seg, Cx.Emerald.copy(alpha = 0.3f), style = Stroke(6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(seg, Cx.Emerald, style = Stroke(2.6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
