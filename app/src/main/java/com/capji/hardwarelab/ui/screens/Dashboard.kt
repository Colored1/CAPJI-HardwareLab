package com.capji.hardwarelab.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capji.hardwarelab.core.LocalAr
import com.capji.hardwarelab.core.tr
import com.capji.hardwarelab.data.BatteryLab
import com.capji.hardwarelab.data.BatteryReading
import com.capji.hardwarelab.data.HealthScore
import com.capji.hardwarelab.data.MemLab
import com.capji.hardwarelab.data.TestId
import com.capji.hardwarelab.data.TestStatus
import com.capji.hardwarelab.firebase.RemoteConfigManager
import com.capji.hardwarelab.ui.components.CapjiHardwareLogo
import com.capji.hardwarelab.ui.components.GlassCard
import com.capji.hardwarelab.ui.components.RadialGauge
import com.capji.hardwarelab.ui.components.StatusChip
import com.capji.hardwarelab.ui.components.sharedCard
import com.capji.hardwarelab.ui.components.statusColor
import com.capji.hardwarelab.ui.theme.Cx
import com.capji.hardwarelab.vm.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun DashboardScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val results by vm.results.collectAsStateWithLifecycle()
    val bench by vm.bench.collectAsStateWithLifecycle()
    val ar = LocalAr.current
    val remote by RemoteConfigManager.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    var bat by remember { mutableStateOf<BatteryReading?>(null) }
    var ramUsedPct by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withContext(Dispatchers.IO) {
                bat = BatteryLab.read(ctx)
                val r = MemLab.ram(ctx)
                if (r.totalBytes > 0) ramUsedPct = (100 - r.availBytes * 100 / r.totalBytes).toInt()
            }
            delay(2000)
        }
    }

    val score = HealthScore.compute(results, bat, bench.result)
    val gaugeColor = when {
        score.score == null -> Cx.Cyan
        score.score >= 85 -> Cx.Emerald
        score.score >= 70 -> Cx.Amber
        else -> Cx.Red
    }

    val pulse = rememberInfiniteTransition(label = "cta")
    val scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "ctaScale",
    )

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CapjiHardwareLogo(Modifier.size(52.dp))
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text("CAPJI Hardware Lab", color = Cx.TextHi, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text(tr("مختبر فحص العتاد", "Hardware inspection lab"), color = Cx.TextLo, fontSize = 12.sp)
                }
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(Cx.Cyan.copy(alpha = 0.16f))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.setLanguage(!ar)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) { Text(if (ar) "EN" else "ع", color = Cx.Cyan, fontWeight = FontWeight.Bold) }
                IconButton(onClick = { vm.openSettings() }) {
                    Icon(Icons.Filled.Settings, contentDescription = null, tint = Cx.TextHi)
                }
            }
        }
        if (remote.bannerEnabled && remote.bannerText.isNotBlank()) {
            item {
                GlassCard(Modifier.fillMaxWidth(), accent = Cx.Amber) {
                    Text(remote.bannerText, color = Cx.TextHi, fontSize = 13.sp)
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth(), accent = gaugeColor) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadialGauge(
                        progress = (score.score ?: 0) / 100f,
                        modifier = Modifier.size(150.dp),
                        color = gaugeColor,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(score.grade, color = gaugeColor, fontSize = 38.sp, fontWeight = FontWeight.Black)
                            Text(if (score.score != null) "${score.score}/100" else "--", color = Cx.TextLo, fontSize = 13.sp)
                        }
                    }
                    Column(Modifier.padding(start = 16.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(tr("درجة صحة الجهاز", "Device health score"), color = Cx.TextHi, fontWeight = FontWeight.Bold)
                        Text(
                            tr("${score.tested} من ${TestId.entries.size} اختبارًا مكتمل", "${score.tested} of ${TestId.entries.size} tests done"),
                            color = Cx.TextLo, fontSize = 12.sp,
                        )
                        val b = bat
                        if (b != null) {
                            Text(tr("البطارية ${b.percent}% • ${"%.1f".format(b.tempC)}°C", "Battery ${b.percent}% • ${"%.1f".format(b.tempC)}°C"), color = Cx.TextHi, fontSize = 12.sp)
                        }
                        Text(tr("الذاكرة المستخدمة $ramUsedPct%", "RAM used $ramUsedPct%"), color = Cx.TextHi, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(Cx.Cyan, Cx.Emerald)))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.startGuided()
                    }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Cx.Obsidian)
                    Text(
                        tr("فحص شامل لجميع القطع", "Start Full Guided Inspection"),
                        color = Cx.Obsidian, fontWeight = FontWeight.Black, fontSize = 17.sp,
                    )
                }
            }
        }
        item {
            Text(tr("تشغيل سريع لكل قطعة", "Quick launch"), color = Cx.Cyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        items(TestId.entries.chunked(2)) { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (id in pair) {
                    TestCard(id, results[id] ?: TestStatus.PENDING, Modifier.weight(1f)) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.openTest(id)
                    }
                }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
        item { Box(Modifier.height(8.dp)) }
    }
}

@Composable
private fun TestCard(id: TestId, status: TestStatus, modifier: Modifier, onClick: () -> Unit) {
    val c = statusColor(status)
    Box(modifier.sharedCard("test-${id.name}").clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick)) {
        GlassCard(Modifier.fillMaxWidth(), accent = if (status == TestStatus.PENDING) null else c) {
            Icon(id.icon, contentDescription = null, tint = Cx.Cyan, modifier = Modifier.size(30.dp))
            Box(Modifier.height(8.dp))
            Text(tr(id.ar, id.en), color = Cx.TextHi, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, minLines = 2, maxLines = 2)
            Box(Modifier.height(8.dp))
            StatusChip(status)
        }
    }
}
