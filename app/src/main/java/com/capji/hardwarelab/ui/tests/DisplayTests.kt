package com.capji.hardwarelab.ui.tests

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capji.hardwarelab.core.tr
import com.capji.hardwarelab.data.TestStatus
import com.capji.hardwarelab.ui.components.ImmersiveEffect
import com.capji.hardwarelab.ui.theme.Cx
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.sqrt

// ------------------------------------------------------------ Touch grid

@Composable
fun TouchGridTest(onMark: (TestStatus) -> Unit, onFinish: (TestStatus?) -> Unit, onBack: () -> Unit) {
    ImmersiveEffect()
    val haptic = LocalHapticFeedback.current
    var verdict by remember { mutableStateOf(false) }
    BackHandler(enabled = !verdict) { verdict = true }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val cols = 8
        val cell = wPx / cols
        val rows = kotlin.math.ceil(hPx / cell).toInt()
        val touched = remember(cols, rows) { mutableStateListOf<Boolean>().apply { repeat(cols * rows) { add(false) } } }
        var count by remember(cols, rows) { mutableIntStateOf(0) }

        fun mark(p: Offset) {
            val cx = (p.x / cell).toInt()
            val cy = (p.y / cell).toInt()
            if (cx in 0 until cols && cy in 0 until rows) {
                val i = cy * cols + cx
                if (!touched[i]) {
                    touched[i] = true
                    count++
                }
            }
        }

        fun markLine(a: Offset, b: Offset) {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val dist = sqrt(dx * dx + dy * dy)
            val steps = max(1, (dist / (cell / 2f)).toInt())
            for (s in 0..steps) {
                mark(Offset(a.x + dx * s / steps, a.y + dy * s / steps))
            }
        }

        val total = cols * rows
        LaunchedEffect(count) {
            if (count >= total && total > 0) {
                onMark(TestStatus.PASSED)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                verdict = true
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(cols, rows) {
                    awaitEachGesture {
                        val first = awaitFirstDown(requireUnconsumed = false)
                        mark(first.position)
                        do {
                            val ev = awaitPointerEvent()
                            ev.changes.forEach { ch ->
                                markLine(ch.previousPosition, ch.position)
                                ch.consume()
                            }
                        } while (ev.changes.any { it.pressed })
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val on = touched[r * cols + c]
                        val tl = Offset(c * cell + 1.5f, r * cell + 1.5f)
                        val sz = Size(cell - 3f, cell - 3f)
                        if (on) {
                            drawRoundRect(Cx.Emerald.copy(alpha = 0.85f), tl, sz, CornerRadius(8f))
                        } else {
                            drawRoundRect(Color(0xFF121A28), tl, sz, CornerRadius(8f))
                            drawRoundRect(Color(0x33FFFFFF), tl, sz, CornerRadius(8f), style = Stroke(1.5f))
                        }
                    }
                }
            }
            val pct = if (total > 0) count * 100 / total else 0
            Text(
                tr("اسحب بإصبعك فوق كل الشاشة\nالتغطية $pct% • المناطق الميتة ${total - count}", "Swipe over the whole screen\nCoverage $pct% • dead cells ${total - count}"),
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Center).alpha(if (pct < 100) 0.9f else 0f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        if (verdict) {
            VerdictOverlay(
                title = tr("نتيجة اختبار اللمس", "Touch test result"),
                onPass = { onFinish(TestStatus.PASSED) },
                onFail = { onFinish(TestStatus.FAILED) },
                onContinue = { verdict = false },
            )
        }
    }
}

// ------------------------------------------------------------ Multi-touch

@Composable
fun MultiTouchTest(onMark: (TestStatus) -> Unit, onFinish: (TestStatus?) -> Unit, onBack: () -> Unit) {
    ImmersiveEffect()
    var verdict by remember { mutableStateOf(false) }
    BackHandler(enabled = !verdict) { verdict = true }
    val points = remember { mutableStateMapOf<Long, Offset>() }
    var maxCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(maxCount) {
        if (maxCount >= 10) {
            onMark(TestStatus.PASSED)
            verdict = true
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF05080F))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val ev = awaitPointerEvent()
                        ev.changes.forEach { ch ->
                            if (ch.pressed) points[ch.id.value] = ch.position else points.remove(ch.id.value)
                            ch.consume()
                        }
                        if (points.size > maxCount) maxCount = points.size
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val colors = listOf(Cx.Cyan, Cx.Emerald, Cx.Amber, Cx.Red, Color(0xFFB388FF), Color(0xFFFF80AB), Color(0xFF80D8FF), Color(0xFFCCFF90), Color(0xFFFFD180), Color.White)
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 36f
                color = android.graphics.Color.WHITE
                textAlign = android.graphics.Paint.Align.CENTER
            }
            var idx = 0
            for ((_, p) in points.entries.toList()) {
                val c = colors[idx % colors.size]
                drawLine(c.copy(alpha = 0.25f), Offset(p.x, 0f), Offset(p.x, size.height), strokeWidth = 2f)
                drawLine(c.copy(alpha = 0.25f), Offset(0f, p.y), Offset(size.width, p.y), strokeWidth = 2f)
                drawCircle(c.copy(alpha = 0.25f), radius = 70f, center = p)
                drawCircle(c, radius = 70f, center = p, style = Stroke(5f))
                drawContext.canvas.nativeCanvas.drawText("${idx + 1}", p.x, p.y - 95f, paint)
                idx++
            }
        }
        Text(
            tr("ضع حتى 10 أصابع على الشاشة\nالحالي: ${points.size} • الأقصى: $maxCount", "Place up to 10 fingers on the screen\nNow: ${points.size} • Max: $maxCount"),
            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (verdict) {
            VerdictOverlay(
                title = tr("أقصى عدد لمسات: $maxCount", "Max simultaneous touches: $maxCount"),
                onPass = { onFinish(TestStatus.PASSED) },
                onFail = { onFinish(TestStatus.FAILED) },
                onContinue = { verdict = false },
            )
        }
    }
}

// ------------------------------------------------------------ Dead pixel / burn-in colors

private class Swatch(val name: String, val ar: String, val color: Color?, val brush: Brush?)

@Composable
fun ColorsTest(onFinish: (TestStatus?) -> Unit, onBack: () -> Unit) {
    ImmersiveEffect()
    val swatches = remember {
        listOf(
            Swatch("Red", "أحمر", Color(0xFFFF0000), null),
            Swatch("Green", "أخضر", Color(0xFF00FF00), null),
            Swatch("Blue", "أزرق", Color(0xFF0000FF), null),
            Swatch("White", "أبيض", Color(0xFFFFFFFF), null),
            Swatch("Black", "أسود", Color(0xFF000000), null),
            Swatch("50% Gray", "رمادي 50%", Color(0xFF808080), null),
            Swatch("Gradient", "تدرج", null, Brush.horizontalGradient(listOf(Color.Black, Color.White))),
        )
    }
    var index by remember { mutableIntStateOf(0) }
    var verdict by remember { mutableStateOf(false) }
    var hintTick by remember { mutableIntStateOf(0) }
    var hintVisible by remember { mutableStateOf(true) }
    BackHandler(enabled = !verdict) { verdict = true }

    LaunchedEffect(index, hintTick) {
        hintVisible = true
        delay(1500)
        hintVisible = false
    }
    val hintAlpha by animateFloatAsState(if (hintVisible) 1f else 0f, tween(400), label = "hint")

    fun next() {
        if (index < swatches.size - 1) index++ else verdict = true
        hintTick++
    }

    fun prev() {
        if (index > 0) index--
        hintTick++
    }

    val s = swatches[index]
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { next() }) }
            .pointerInput(Unit) {
                var acc = 0f
                detectHorizontalDragGestures(
                    onDragStart = { acc = 0f },
                    onDragEnd = {
                        if (acc < -80f) next() else if (acc > 80f) prev()
                    },
                ) { _, dx -> acc += dx }
            }
            .then(if (s.brush != null) Modifier.background(s.brush) else Modifier.background(s.color ?: Color.Black)),
    ) {
        val fg = if (s.name == "White" || s.name == "Gradient") Color.Black else Color.White
        Text(
            "${tr(s.ar, s.name)}  ${index + 1}/${swatches.size}\n" +
                tr("اضغط للتالي • اسحب للتنقل • رجوع للنتيجة", "Tap = next • swipe to navigate • Back = result"),
            color = fg,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp).alpha(hintAlpha),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
    if (verdict) {
        VerdictOverlay(
            title = tr("هل ظهرت بكسلات ميتة أو تسرب إضاءة أو حرق؟", "Any dead pixels, backlight bleed or burn-in?"),
            onPass = { onFinish(TestStatus.PASSED) },
            onFail = { onFinish(TestStatus.FAILED) },
            onContinue = { verdict = false; index = 0 },
        )
    }
}
