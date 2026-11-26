package com.capji.hardwarelab.ui.tests

import android.Manifest
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capji.hardwarelab.core.tr
import com.capji.hardwarelab.data.AudioRoute
import com.capji.hardwarelab.data.MicLab
import com.capji.hardwarelab.data.TestStatus
import com.capji.hardwarelab.data.ToneChannel
import com.capji.hardwarelab.data.ToneGen
import com.capji.hardwarelab.data.TonePlayer
import com.capji.hardwarelab.data.VibrationLab
import com.capji.hardwarelab.ui.components.FloatRing
import com.capji.hardwarelab.ui.components.GlassCard
import com.capji.hardwarelab.ui.components.Oscilloscope
import com.capji.hardwarelab.ui.components.PermissionGate
import com.capji.hardwarelab.ui.components.StatRow
import com.capji.hardwarelab.ui.theme.Cx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

// ------------------------------------------------------------ Earpiece / Loudspeaker

@Composable
fun SpeakerTest(earpiece: Boolean, onMark: (TestStatus) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val player = remember { TonePlayer(ctx) }
    var playing by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf("") }
    DisposableEffect(Unit) { onDispose { player.stop(); player.restore() } }

    val route = if (earpiece) AudioRoute.EARPIECE else AudioRoute.SPEAKER
    fun play(name: String, samples: ShortArray) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        label = name
        playing = true
        player.play(scope, route, samples) { playing = false }
    }

    val t = rememberInfiniteTransition(label = "wave")
    val ph by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "ph")

    val sSweep = tr("مسح ترددات 200Hz → 4kHz", "Frequency sweep 200Hz → 4kHz")
    val sTone = tr("نغمة 1 kHz", "1 kHz tone")
    val sL = tr("القناة اليسرى", "Left channel")
    val sR = tr("القناة اليمنى", "Right channel")
    val sBoth = tr("القناتان", "Both channels")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(Modifier.fillMaxWidth(), accent = Cx.Cyan) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        if (playing) {
                            for (k in 0..2) {
                                val p = (ph + k / 3f) % 1f
                                drawCircle(Cx.Cyan.copy(alpha = (1f - p) * 0.6f), radius = size.minDimension * (0.3f + 0.2f * p), center = Offset(size.width / 2, size.height / 2), style = Stroke(3f))
                            }
                        }
                    }
                    Icon(if (earpiece) Icons.Filled.PhoneInTalk else Icons.Filled.VolumeUp, contentDescription = null, tint = Cx.Cyan, modifier = Modifier.size(38.dp))
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        if (earpiece) tr("المسار: سماعة المكالمات (MODE_IN_COMMUNICATION)", "Route: earpiece (MODE_IN_COMMUNICATION)")
                        else tr("المسار: السماعة الخارجية (MODE_NORMAL)", "Route: loudspeaker (MODE_NORMAL)"),
                        color = Cx.TextHi, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (playing) tr("يعمل الآن: $label", "Playing: $label") else tr("متوقف", "Stopped"),
                        color = if (playing) Cx.Emerald else Cx.TextLo, fontSize = 13.sp,
                    )
                }
            }
        }
        Text(
            if (earpiece) tr("ضع الجهاز على أذنك واستمع. ارفع صوت المكالمة إن لزم.", "Hold the phone to your ear and listen. Raise the call volume if needed.")
            else tr("ارفع الصوت واستمع للنغمات. تأكد من عدم تشويه أو خشخشة.", "Raise the volume and listen. Check for distortion or crackling."),
            color = Cx.TextLo, fontSize = 12.sp,
        )
        ToneButton(sSweep) { play(sSweep, ToneGen.sweep(3f, 200f, 4000f)) }
        ToneButton(sTone) { play(sTone, ToneGen.tone(2f, 1000f)) }
        if (!earpiece) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { play(sL, ToneGen.tone(2f, 880f, ToneChannel.LEFT)) }, modifier = Modifier.weight(1f)) { Text(sL, fontSize = 12.sp) }
                OutlinedButton(onClick = { play(sR, ToneGen.tone(2f, 880f, ToneChannel.RIGHT)) }, modifier = Modifier.weight(1f)) { Text(sR, fontSize = 12.sp) }
            }
            ToneButton(sBoth) { play(sBoth, ToneGen.tone(2f, 660f, ToneChannel.BOTH)) }
        }
        OutlinedButton(onClick = { player.stop(); playing = false }, modifier = Modifier.fillMaxWidth(), enabled = playing) {
            Icon(Icons.Filled.Stop, null)
            Text(tr(" إيقاف", " Stop"))
        }
    }
}

@Composable
private fun ToneButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Cx.Cyan, contentColor = Cx.Obsidian),
    ) {
        Icon(Icons.Filled.PlayArrow, null)
        Text(" $text", fontWeight = FontWeight.Bold)
    }
}

// ------------------------------------------------------------ Microphone

@Composable
fun MicTest(onMark: (TestStatus) -> Unit) {
    PermissionGate(
        listOf(Manifest.permission.RECORD_AUDIO),
        tr("يحتاج اختبار الميكروفون إلى إذن التسجيل. لا يتم حفظ أو إرسال أي صوت.", "The microphone test needs the record permission. Nothing is saved or uploaded."),
    ) { MicContent(onMark) }
}

@Composable
private fun MicContent(onMark: (TestStatus) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val ring = remember { FloatRing(256) }
    var tick by remember { mutableIntStateOf(0) }
    var peakDb by remember { mutableStateOf(-90f) }
    var rmsDb by remember { mutableStateOf(-90f) }
    var error by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val capture = remember { ArrayList<ShortArray>() }

    val sRec = tr("جارٍ التسجيل 3 ثوانٍ… تكلّم", "Recording 3 s… speak")
    val sPlay = tr("جارٍ التشغيل…", "Playing back…")
    val sDone = tr("اكتمل", "Done")

    LaunchedEffect(Unit) {
        MicLab.frames()
            .catch { error = true }
            .collect { f ->
                for (v in f.wave) ring.add(v)
                tick++
                peakDb = f.peakDb
                rmsDb = f.rmsDb
                if (f.peakDb > -30f) onMark(TestStatus.PASSED)
                if (recording) synchronized(capture) { capture.add(f.raw) }
            }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (error) {
            Text(tr("تعذّر فتح الميكروفون", "Could not open the microphone"), color = Cx.Red, fontWeight = FontWeight.Bold)
        }
        GlassCard(Modifier.fillMaxWidth(), accent = Cx.Emerald) {
            Text(tr("راسم الذبذبات الحي", "Live oscilloscope"), color = Cx.Emerald, fontWeight = FontWeight.Bold)
            Oscilloscope(
                listOf(ring), listOf(Cx.Emerald), tick,
                Modifier.fillMaxWidth().height(150.dp).padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x33000000)),
                fixedMin = -1f, fixedMax = 1f,
            )
        }
        GlassCard(Modifier.fillMaxWidth()) {
            StatRow(tr("ذروة المستوى", "Peak level"), String.format("%.1f dBFS", peakDb))
            StatRow("RMS", String.format("%.1f dBFS", rmsDb))
            val frac = ((peakDb + 90f) / 90f).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(12.dp).clip(RoundedCornerShape(50)).background(Color(0x22FFFFFF))) {
                Box(
                    Modifier.fillMaxWidth(frac).height(12.dp).clip(RoundedCornerShape(50))
                        .background(if (peakDb > -6f) Cx.Red else if (peakDb > -20f) Cx.Amber else Cx.Emerald),
                )
            }
            Text(
                tr("تحدث أو صفّق — يُحتسب النجاح تلقائيًا عند التقاط صوت واضح.", "Speak or clap — it passes automatically when clear sound is detected."),
                color = Cx.TextLo, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp),
            )
        }
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    synchronized(capture) { capture.clear() }
                    recording = true
                    status = sRec
                    delay(3000)
                    recording = false
                    val all: ShortArray = synchronized(capture) {
                        val n = capture.sumOf { it.size }
                        val out = ShortArray(n)
                        var p = 0
                        for (a in capture) { a.copyInto(out, p); p += a.size }
                        out
                    }
                    if (all.isNotEmpty()) {
                        status = sPlay
                        withContext(Dispatchers.IO) { MicLab.playMono(ctx, all) }
                    }
                    status = sDone
                }
            },
            enabled = !recording && !error,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Cx.Cyan, contentColor = Cx.Obsidian),
        ) { Text(tr("تسجيل 3 ثوانٍ وتشغيل فوري (Loopback)", "Record 3 s & play back (loopback)"), fontWeight = FontWeight.Bold) }
        if (status.isNotEmpty()) Text(status, color = Cx.TextLo, fontSize = 13.sp)
    }
}

// ------------------------------------------------------------ Vibration

@Composable
fun VibrationTest() {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val vib = remember { VibrationLab.vibrator(ctx) }
    DisposableEffect(Unit) { onDispose { vib?.let { VibrationLab.cancel(it) } } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (vib == null || !vib.hasVibrator()) {
            Text(tr("لا يوجد محرك اهتزاز في هذا الجهاز", "No vibration motor in this device"), color = Cx.Amber, fontWeight = FontWeight.Bold)
        } else {
            GlassCard(Modifier.fillMaxWidth(), accent = Cx.Cyan) {
                StatRow(tr("التحكم بالشدة", "Amplitude control"), if (vib.hasAmplitudeControl()) tr("مدعوم", "Supported") else tr("غير مدعوم", "Not supported"))
            }
            val items: List<Triple<String, Int, () -> Unit>> = listOf(
                Triple(tr("نبضة قصيرة", "Short pulse"), 0, { VibrationLab.pulse(vib) }),
                Triple(tr("نبض القلب", "Heartbeat"), 1, { VibrationLab.heartbeat(vib) }),
                Triple(tr("موجة متدرجة", "Wave"), 2, { VibrationLab.wave(vib) }),
                Triple(tr("اهتزاز مستمر (2 ثانية)", "Continuous (2 s)"), 3, { VibrationLab.continuous(vib) }),
            )
            for ((name, _, action) in items) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        action()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Cx.Cyan, contentColor = Cx.Obsidian),
                ) { Text(name, fontWeight = FontWeight.Bold) }
            }
            OutlinedButton(onClick = { VibrationLab.cancel(vib) }, modifier = Modifier.fillMaxWidth()) { Text(tr("إيقاف", "Stop")) }
        }
    }
}

@Suppress("unused")
private suspend fun unusedTimeout() { withTimeoutOrNull(1) { } }
