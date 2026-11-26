package com.capji.hardwarelab.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capji.hardwarelab.ui.theme.Cx

/** 270-degree radial gauge whose value animates with spring physics. */
@Composable
fun RadialGauge(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Cx.Cyan,
    stroke: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val anim by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "gauge",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(stroke / 2)) {
            val sw = stroke.toPx()
            drawArc(
                color = Color(0x22FFFFFF),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(sw, cap = StrokeCap.Round),
            )
            if (anim > 0.001f) {
                // soft glow
                drawArc(
                    color = color.copy(alpha = 0.18f),
                    startAngle = 135f,
                    sweepAngle = 270f * anim,
                    useCenter = false,
                    style = Stroke(sw * 1.7f, cap = StrokeCap.Round),
                )
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = 270f * anim,
                    useCenter = false,
                    style = Stroke(sw, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/** Circular progress ring. progress == null -> indeterminate spinner. */
@Composable
fun ProgressRing(
    progress: Float?,
    modifier: Modifier = Modifier,
    color: Color = Cx.Emerald,
    stroke: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val spin = rememberInfiniteTransition(label = "ring")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "ringAngle",
    )
    val anim by animateFloatAsState(
        targetValue = (progress ?: 0f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ringProgress",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(stroke / 2)) {
            val sw = stroke.toPx()
            drawArc(
                color = Color(0x22FFFFFF),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(sw),
            )
            if (progress == null) {
                drawArc(
                    color = color,
                    startAngle = angle,
                    sweepAngle = 100f,
                    useCenter = false,
                    style = Stroke(sw, cap = StrokeCap.Round),
                )
            } else {
                drawArc(
                    color = color.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f * anim,
                    useCenter = false,
                    style = Stroke(sw * 1.6f, cap = StrokeCap.Round),
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * anim,
                    useCenter = false,
                    style = Stroke(sw, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/** Fixed-size ring buffer of floats, used by oscilloscopes. */
class FloatRing(val capacity: Int) {
    private val data = FloatArray(capacity)
    private var head = 0
    var size = 0
        private set

    fun add(v: Float) {
        data[head] = v
        head = (head + 1) % capacity
        if (size < capacity) size++
    }

    /** index 0 = oldest sample */
    operator fun get(i: Int): Float {
        val start = if (size < capacity) 0 else head
        return data[(start + i) % capacity]
    }

    fun clear() {
        size = 0
        head = 0
    }
}

/**
 * Live scrolling oscilloscope. [tick] must change whenever the buffers change
 * (it only exists to trigger redraw).
 */
@Composable
fun Oscilloscope(
    series: List<FloatRing>,
    colors: List<Color>,
    tick: Int,
    modifier: Modifier = Modifier,
    fixedMin: Float? = null,
    fixedMax: Float? = null,
) {
    Canvas(modifier) {
        @Suppress("UNUSED_VARIABLE")
        val t = tick
        val w = size.width
        val h = size.height
        // grid
        val gridColor = Color(0x1AFFFFFF)
        for (i in 1 until 8) {
            val x = w * i / 8f
            drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
        }
        for (i in 1 until 4) {
            val y = h * i / 4f
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }

        var lo = Float.MAX_VALUE
        var hi = -Float.MAX_VALUE
        for (s in series) {
            for (i in 0 until s.size) {
                val v = s[i]
                if (v < lo) lo = v
                if (v > hi) hi = v
            }
        }
        if (lo == Float.MAX_VALUE) {
            lo = -1f
            hi = 1f
        }
        if (fixedMin != null) lo = fixedMin
        if (fixedMax != null) hi = fixedMax
        if (hi - lo < 1e-3f) {
            hi += 0.5f
            lo -= 0.5f
        }
        val pad = (hi - lo) * 0.1f
        val yMin = if (fixedMin != null) lo else lo - pad
        val yMax = if (fixedMax != null) hi else hi + pad

        // zero line
        if (yMin < 0f && yMax > 0f) {
            val zy = h - (0f - yMin) / (yMax - yMin) * h
            drawLine(Color(0x33FFFFFF), Offset(0f, zy), Offset(w, zy), strokeWidth = 1.5f)
        }

        series.forEachIndexed { idx, s ->
            if (s.size < 2) return@forEachIndexed
            val path = Path()
            val cap = s.capacity
            for (i in 0 until s.size) {
                val x = w * (i + (cap - s.size)) / (cap - 1f)
                val y = h - (s[i] - yMin) / (yMax - yMin) * h
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val c = colors[idx % colors.size]
            drawPath(path, c.copy(alpha = 0.22f), style = Stroke(6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, c, style = Stroke(2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Suppress("unused")
private val keepSize = Size(0f, 0f)
