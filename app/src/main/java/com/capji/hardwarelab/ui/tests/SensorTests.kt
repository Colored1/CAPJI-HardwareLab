package com.capji.hardwarelab.ui.tests

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capji.hardwarelab.core.tr
import com.capji.hardwarelab.data.SensorHub
import com.capji.hardwarelab.data.TestStatus
import com.capji.hardwarelab.ui.components.FloatRing
import com.capji.hardwarelab.ui.components.GlassCard
import com.capji.hardwarelab.ui.components.Oscilloscope
import com.capji.hardwarelab.ui.components.StatRow
import com.capji.hardwarelab.ui.theme.Cx
import java.util.Locale
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class SensorKind(val type: Int, val unit: String) {
    ACCEL(Sensor.TYPE_ACCELEROMETER, "m/s²"),
    GYRO(Sensor.TYPE_GYROSCOPE, "rad/s"),
    PROXIMITY(Sensor.TYPE_PROXIMITY, "cm"),
    LIGHT(Sensor.TYPE_LIGHT, "lx"),
    MAGNETO(Sensor.TYPE_MAGNETIC_FIELD, "µT"),
    BARO(Sensor.TYPE_PRESSURE, "hPa"),
}

@Composable
fun SensorTest(kind: SensorKind, onMark: (TestStatus) -> Unit) {
    val ctx = LocalContext.current
    val meta = remember { SensorHub.meta(ctx, kind.type) }
    if (meta == null) {
        LaunchedEffect(Unit) { onMark(TestStatus.NA) }
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text(
                tr("هذا الحساس غير موجود في الجهاز، تم تسجيله كـ «غير متوفر».", "This sensor is not present on the device; recorded as N/A."),
                color = Cx.Amber, fontWeight = FontWeight.Bold,
            )
        }
        return
    }

    val rings = remember { List(3) { FloatRing(200) } }
    var tick by remember { mutableIntStateOf(0) }
    var latest by remember { mutableStateOf(FloatArray(3)) }
    var verified by remember { mutableStateOf(false) }
    var magnitude by remember { mutableFloatStateOf(0f) }
    var near by remember { mutableStateOf(false) }
    val track = remember { floatArrayOf(Float.MAX_VALUE, -Float.MAX_VALUE, 0f, 0f, 0f) } // min,max,count,sawNear,sawFar

    LaunchedEffect(kind) {
        SensorHub.flow(ctx, kind.type).collect { v ->
            latest = v
            val n = min(3, v.size)
            for (i in 0 until n) rings[i].add(v[i])
            tick++
            val x = v.getOrElse(0) { 0f }
            val y = v.getOrElse(1) { 0f }
            val z = v.getOrElse(2) { 0f }
            val mag = when (kind) {
                SensorKind.ACCEL, SensorKind.GYRO, SensorKind.MAGNETO -> sqrt(x * x + y * y + z * z)
                else -> x
            }
            magnitude = mag
            track[0] = min(track[0], mag)
            track[1] = max(track[1], mag)
            track[2] += 1f
            var ok = false
            when (kind) {
                SensorKind.ACCEL -> ok = track[2] > 20 && (track[1] - track[0]) > 4f && track[1] in 8f..60f
                SensorKind.GYRO -> ok = track[1] > 1.0f
                SensorKind.PROXIMITY -> {
                    val isNear = x < min(5f, meta.maxRange)
                    near = isNear
                    if (isNear) track[3] = 1f else track[4] = 1f
                    ok = track[3] > 0f && track[4] > 0f
                }
                SensorKind.LIGHT -> ok = track[2] > 5 && (track[1] - track[0]) > 10f && track[1] / (track[0] + 1f) > 3f
                SensorKind.MAGNETO -> ok = track[2] > 10 && mag in 15f..100f
                SensorKind.BARO -> ok = track[2] > 5 && mag in 300f..1100f
            }
            if (ok && !verified) {
                verified = true
                onMark(TestStatus.PASSED)
            }
        }
    }

    val hint = when (kind) {
        SensorKind.ACCEL -> tr("حرّك الجهاز وهزّه بقوة.", "Move and shake the device.")
        SensorKind.GYRO -> tr("أدِر الجهاز بسرعة في جميع الاتجاهات.", "Rotate the device quickly in all directions.")
        SensorKind.PROXIMITY -> tr("غطِّ منطقة السماعة العلوية بيدك ثم ارفعها.", "Cover the top of the phone with your hand, then uncover it.")
        SensorKind.LIGHT -> tr("غطِّ الحساس بإصبعك ثم وجّهه نحو الضوء.", "Cover the sensor, then point it at a light.")
        SensorKind.MAGNETO -> tr("أدِر الجهاز أفقيًا وراقب البوصلة. ابتعد عن المعادن.", "Rotate the device flat and watch the compass. Stay away from metal.")
        SensorKind.BARO -> tr("يكفي وجود قراءة ضغط منطقية (300–1100 hPa).", "A plausible pressure reading (300–1100 hPa) is enough.")
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(Modifier.fillMaxWidth(), accent = if (verified) Cx.Emerald else Cx.Cyan) {
            Text(
                if (verified) tr("✓ تم التحقق تلقائيًا", "✓ Verified automatically") else tr("بانتظار التحقق…", "Waiting for verification…"),
                color = if (verified) Cx.Emerald else Cx.Amber, fontWeight = FontWeight.Bold,
            )
            Text(hint, color = Cx.TextLo, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }

        when (kind) {
            SensorKind.PROXIMITY -> {
                val scale by animateFloatAsState(if (near) 1f else 0.45f, spring(Spring.DampingRatioMediumBouncy), label = "prox")
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(150.dp)) {
                        drawCircle((if (near) Cx.Red else Cx.Emerald).copy(alpha = 0.25f), radius = size.minDimension / 2f * scale)
                        drawCircle(if (near) Cx.Red else Cx.Emerald, radius = size.minDimension / 2f * scale, style = Stroke(6f))
                    }
                    Text(if (near) tr("قريب", "NEAR") else tr("بعيد", "FAR"), color = Cx.TextHi, fontWeight = FontWeight.Black, fontSize = 24.sp)
                }
            }
            SensorKind.MAGNETO -> Compass(kind, rings, tick, latest)
            SensorKind.LIGHT -> {
                val lux = latest.getOrElse(0) { 0f }
                val frac = (ln(1f + lux) / ln(1f + 30000f)).coerceIn(0f, 1f)
                GlassCard(Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(50)).background(Color(0x22FFFFFF))) {
                        Box(Modifier.fillMaxWidth(frac).height(18.dp).clip(RoundedCornerShape(50)).background(Cx.Amber))
                    }
                }
            }
            else -> {}
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text(
                String.format(Locale.US, "%.2f %s", magnitude, kind.unit),
                color = Cx.TextHi, fontWeight = FontWeight.Black, fontSize = 30.sp,
            )
            if (kind == SensorKind.ACCEL || kind == SensorKind.GYRO || kind == SensorKind.MAGNETO) {
                Text(
                    String.format(Locale.US, "X %.2f   Y %.2f   Z %.2f", latest.getOrElse(0) { 0f }, latest.getOrElse(1) { 0f }, latest.getOrElse(2) { 0f }),
                    color = Cx.TextLo, fontSize = 13.sp,
                )
            }
            val series = if (kind == SensorKind.ACCEL || kind == SensorKind.GYRO || kind == SensorKind.MAGNETO) rings else listOf(rings[0])
            Oscilloscope(
                series, listOf(Cx.Cyan, Cx.Emerald, Cx.Amber), tick,
                Modifier.fillMaxWidth().height(150.dp).padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x33000000)),
            )
        }

        GlassCard(Modifier.fillMaxWidth()) {
            StatRow(tr("الاسم", "Name"), meta.name)
            StatRow(tr("المصنّع", "Vendor"), meta.vendor)
            StatRow(tr("المدى الأقصى", "Max range"), String.format(Locale.US, "%.2f %s", meta.maxRange, kind.unit))
            StatRow(tr("الدقة", "Resolution"), String.format(Locale.US, "%.4f", meta.resolution))
            StatRow(tr("استهلاك الطاقة", "Power"), String.format(Locale.US, "%.2f mA", meta.powerMa))
        }
    }
}

@Composable
private fun Compass(kind: SensorKind, rings: List<FloatRing>, tick: Int, mag: FloatArray) {
    val ctx = LocalContext.current
    var gravity by remember { mutableStateOf(floatArrayOf(0f, 0f, 9.8f)) }
    LaunchedEffect(Unit) {
        SensorHub.flow(ctx, Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_UI).collect { gravity = it }
    }
    val unwrapped = remember { floatArrayOf(0f, 0f) } // last raw, unwrapped
    var azimuth = 0f
    if (mag.size >= 3) {
        val r = FloatArray(9)
        val o = FloatArray(3)
        if (SensorManager.getRotationMatrix(r, null, gravity, mag)) {
            SensorManager.getOrientation(r, o)
            azimuth = ((Math.toDegrees(o[0].toDouble()).toFloat()) + 360f) % 360f
        }
    }
    val delta = ((azimuth - unwrapped[0] + 540f) % 360f) - 180f
    unwrapped[1] += delta
    unwrapped[0] = azimuth
    val target = unwrapped[1]
    val anim by animateFloatAsState(target, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow), label = "needle")

    Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(210.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f - 6f
            drawCircle(Color(0x22FFFFFF), r, c)
            drawCircle(Cx.Cyan.copy(alpha = 0.6f), r, c, style = Stroke(3f))
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 34f
                textAlign = android.graphics.Paint.Align.CENTER
            }
            rotate(-anim, pivot = c) {
                for (k in 0 until 36) {
                    val len = if (k % 9 == 0) 18f else 8f
                    rotate(k * 10f, pivot = c) {
                        drawLine(Color.White.copy(alpha = 0.6f), Offset(c.x, c.y - r), Offset(c.x, c.y - r + len), strokeWidth = 2f)
                    }
                }
                drawContext.canvas.nativeCanvas.drawText("N", c.x, c.y - r + 52f, paint)
            }
            // fixed needle pointing up (device heading)
            val needle = Path().apply {
                moveTo(c.x, c.y - r + 20f)
                lineTo(c.x - 12f, c.y + 10f)
                lineTo(c.x + 12f, c.y + 10f)
                close()
            }
            drawPath(needle, Cx.Red)
            drawCircle(Color.White, 8f, c)
        }
        Text("${azimuth.toInt()}°", color = Cx.TextHi, fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.padding(top = 120.dp))
    }
    @Suppress("UNUSED_VARIABLE") val keep = Triple(kind, rings, tick)
}
