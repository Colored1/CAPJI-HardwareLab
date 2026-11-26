package com.capji.hardwarelab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capji.hardwarelab.core.LocalAr
import com.capji.hardwarelab.core.tr
import com.capji.hardwarelab.data.BatteryLab
import com.capji.hardwarelab.data.BatteryReading
import com.capji.hardwarelab.data.CpuLab
import com.capji.hardwarelab.data.CpuSnapshot
import com.capji.hardwarelab.data.DisplayLab
import com.capji.hardwarelab.data.GpuLab
import com.capji.hardwarelab.data.MemLab
import com.capji.hardwarelab.data.ThermalLab
import com.capji.hardwarelab.data.formatBytes
import com.capji.hardwarelab.ui.components.FloatRing
import com.capji.hardwarelab.ui.components.GlassCard
import com.capji.hardwarelab.ui.components.Oscilloscope
import com.capji.hardwarelab.ui.components.ProgressRing
import com.capji.hardwarelab.ui.components.RadialGauge
import com.capji.hardwarelab.ui.components.SectionTitle
import com.capji.hardwarelab.ui.components.StatRow
import com.capji.hardwarelab.ui.theme.Cx
import com.capji.hardwarelab.vm.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SpecsScreen(vm: MainViewModel) {
    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text(tr("العتاد الداخلي والبطارية", "Internal hardware & battery"), color = Cx.TextHi, fontWeight = FontWeight.Black, fontSize = 22.sp) }
        item { BatteryCard() }
        item { CpuCard() }
        item { MemoryCard(vm) }
        item { DisplayCard() }
        item { Box(Modifier.height(8.dp)) }
    }
}

@Composable
private fun BatteryCard() {
    val ctx = LocalContext.current
    val ar = LocalAr.current
    var bat by remember { mutableStateOf<BatteryReading?>(null) }
    val ring = remember { FloatRing(120) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (isActive) {
            val r = withContext(Dispatchers.IO) { BatteryLab.read(ctx) }
            bat = r
            ring.add(r.currentMa.toFloat())
            tick++
            delay(500)
        }
    }
    GlassCard(Modifier.fillMaxWidth(), accent = Cx.Emerald) {
        SectionTitle(tr("محلل البطارية والشحن", "Battery & charging analyzer"), color = Cx.Emerald)
        val b = bat
        if (b == null) {
            Text(tr("جارٍ القراءة…", "Reading…"), color = Cx.TextLo)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadialGauge(
                    progress = b.percent / 100f,
                    modifier = Modifier.size(120.dp),
                    color = if (b.percent > 20) Cx.Emerald else Cx.Red,
                ) {
                    Text("${b.percent}%", color = Cx.TextHi, fontWeight = FontWeight.Black, fontSize = 24.sp)
                }
                Column(Modifier.padding(start = 14.dp)) {
                    Text(
                        if (b.currentSupported) "${b.currentMa} mA" else tr("غير مدعوم", "N/A"),
                        color = if (b.currentMa >= 0) Cx.Emerald else Cx.Amber,
                        fontWeight = FontWeight.Black, fontSize = 28.sp,
                    )
                    Text(BatteryLab.statusLabel(b.status, ar), color = Cx.TextLo, fontSize = 13.sp)
                    Text(BatteryLab.sourceLabel(b.plugged, b.fastCharge, ar), color = Cx.Cyan, fontSize = 12.sp)
                }
            }
            Text(
                tr("منحنى التيار الحي (mA) — اختبر جودة الشاحن والكيبل", "Live current graph (mA) — test charger & cable quality"),
                color = Cx.TextLo, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
            )
            Oscilloscope(
                listOf(ring), listOf(Cx.Emerald), tick,
                Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x33000000)),
            )
            Box(Modifier.height(8.dp))
            StatRow(tr("الجهد", "Voltage"), "${b.voltageMv} mV")
            StatRow(tr("الحرارة", "Temperature"), String.format(Locale.US, "%.1f °C", b.tempC), if (b.tempC >= 43f) Cx.Red else Cx.TextHi)
            StatRow(tr("الحالة الصحية", "Health"), BatteryLab.healthLabel(b.health, ar), if (b.health == 2) Cx.Emerald else Cx.Red)
            StatRow(tr("القدرة اللحظية", "Power"), String.format(Locale.US, "%.1f W", b.powerW))
            StatRow(tr("عداد الشحن", "Charge counter"), if (b.chargeCounterMah > 0) "${b.chargeCounterMah} mAh" else "-")
            StatRow(tr("السعة التقديرية", "Estimated capacity"), if (b.estCapacityMah > 0) "${b.estCapacityMah} mAh" else "-")
            StatRow(tr("التقنية", "Technology"), b.technology)
            if (b.cycleCount > 0) StatRow(tr("دورات الشحن", "Charge cycles"), b.cycleCount.toString())
        }
    }
}

@Composable
private fun CpuCard() {
    val ctx = LocalContext.current
    val ar = LocalAr.current
    var cpu by remember { mutableStateOf<CpuSnapshot?>(null) }
    var thermal by remember { mutableIntStateOf(-1) }
    val gpu by produceState<com.capji.hardwarelab.data.GpuData?>(null) {
        value = withContext(Dispatchers.IO) { GpuLab.query() }
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            withContext(Dispatchers.IO) {
                cpu = CpuLab.snapshot()
                thermal = ThermalLab.status(ctx)
            }
            delay(1000)
        }
    }
    GlassCard(Modifier.fillMaxWidth(), accent = Cx.Cyan) {
        SectionTitle(tr("المعالج والرسوميات والحرارة", "CPU, GPU & thermal"))
        val c = cpu
        if (c == null) {
            Text(tr("جارٍ القراءة…", "Reading…"), color = Cx.TextLo)
        } else {
            StatRow("SoC", c.soc)
            StatRow(tr("الأنوية", "Cores"), c.cores.toString())
            StatRow(tr("المعمارية", "Architecture"), c.abi)
            StatRow(tr("كرت الشاشة (GPU)", "GPU renderer"), gpu?.renderer ?: "…")
            StatRow(tr("مصنّع GPU", "GPU vendor"), gpu?.vendor ?: "…")
            StatRow("OpenGL ES", gpu?.version ?: "…")
            StatRow(
                tr("حالة الخنق الحراري", "Thermal throttling"),
                ThermalLab.label(thermal, ar),
                if (thermal >= 2) Cx.Red else Cx.Emerald,
            )
            Box(Modifier.height(6.dp))
            val loadText = when (c.loadSource) {
                0 -> tr("حمل المعالج", "CPU load")
                1 -> tr("حمل المعالج (تقدير من التردد)", "CPU load (estimated from clocks)")
                else -> tr("حمل المعالج (غير متاح)", "CPU load (unavailable)")
            }
            Text(loadText + ": ${c.loadPct}%", color = Cx.TextHi, fontSize = 13.sp)
            Box(
                Modifier.fillMaxWidth().padding(top = 4.dp).height(10.dp)
                    .clip(RoundedCornerShape(50)).background(Color(0x22FFFFFF)),
            ) {
                Box(
                    Modifier.fillMaxWidth(c.loadPct / 100f).height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (c.loadPct > 80) Cx.Red else Cx.Cyan),
                )
            }
            Box(Modifier.height(10.dp))
            Text(tr("تردد كل نواة (MHz)", "Per-core frequency (MHz)"), color = Cx.TextLo, fontSize = 12.sp)
            for (i in 0 until c.cores) {
                val cur = c.curMhz[i]
                val mx = c.maxMhz[i]
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("CPU$i", color = Cx.TextLo, fontSize = 11.sp, modifier = Modifier.size(width = 44.dp, height = 16.dp))
                    Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(50)).background(Color(0x22FFFFFF))) {
                        val frac = if (mx > 0) (cur.toFloat() / mx).coerceIn(0f, 1f) else 0f
                        Box(Modifier.fillMaxWidth(frac).height(8.dp).clip(RoundedCornerShape(50)).background(Cx.Emerald))
                    }
                    Text(
                        if (cur > 0) "$cur" else tr("متوقف/محجوب", "off/locked"),
                        color = Cx.TextHi, fontSize = 11.sp,
                        modifier = Modifier.padding(start = 8.dp).size(width = 70.dp, height = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryCard(vm: MainViewModel) {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var ramTotal by remember { mutableStateOf(0L) }
    var ramAvail by remember { mutableStateOf(0L) }
    var stTotal by remember { mutableStateOf(0L) }
    var stFree by remember { mutableStateOf(0L) }
    val bench by vm.bench.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        while (isActive) {
            withContext(Dispatchers.IO) {
                val r = MemLab.ram(ctx)
                ramTotal = r.totalBytes; ramAvail = r.availBytes
                val s = MemLab.storage()
                stTotal = s.totalBytes; stFree = s.freeBytes
            }
            delay(1500)
        }
    }
    GlassCard(Modifier.fillMaxWidth(), accent = Cx.Cyan) {
        SectionTitle(tr("الذاكرة والتخزين", "RAM & storage"))
        StatRow(tr("إجمالي الذاكرة", "Total RAM"), formatBytes(ramTotal))
        StatRow(tr("المتاح", "Available"), formatBytes(ramAvail))
        UsageBar(if (ramTotal > 0) 1f - ramAvail.toFloat() / ramTotal else 0f)
        Box(Modifier.height(8.dp))
        StatRow(tr("إجمالي التخزين الداخلي", "Internal storage"), formatBytes(stTotal))
        StatRow(tr("المتاح", "Free"), formatBytes(stFree))
        UsageBar(if (stTotal > 0) 1f - stFree.toFloat() / stTotal else 0f)
        Box(Modifier.height(12.dp))
        Text(tr("اختبار سرعة الذاكرة الداخلية (قراءة/كتابة)", "Storage read/write speed benchmark"), color = Cx.TextHi, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ProgressRing(
                progress = if (bench.running) bench.progress else if (bench.result != null) 1f else 0f,
                modifier = Modifier.size(88.dp),
            ) { Text("${(bench.progress * 100).roundToInt()}%", color = Cx.TextHi, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            Column {
                val r = bench.result
                if (r != null) {
                    Text(String.format(Locale.US, "%s %.0f MB/s", tr("كتابة", "Write"), r.writeMBs), color = Cx.Emerald, fontWeight = FontWeight.Bold)
                    Text(String.format(Locale.US, "%s %.0f MB/s", tr("قراءة", "Read"), r.readMBs), color = Cx.Cyan, fontWeight = FontWeight.Bold)
                    Text(
                        tr("القراءة قد تتأثر بكاش النظام", "Read may be boosted by the OS cache"),
                        color = Cx.TextLo, fontSize = 10.sp,
                    )
                } else if (bench.error) {
                    Text(tr("فشل الاختبار", "Benchmark failed"), color = Cx.Red)
                } else {
                    Text(tr("لم يُجرَ بعد", "Not run yet"), color = Cx.TextLo)
                }
            }
        }
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.runBenchmark()
            },
            enabled = !bench.running,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cx.Cyan, contentColor = Cx.Obsidian),
        ) { Text(if (bench.running) tr("جارٍ الاختبار…", "Running…") else tr("ابدأ الاختبار", "Start benchmark")) }
    }
}

@Composable
private fun UsageBar(frac: Float) {
    val f = frac.coerceIn(0f, 1f)
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(Color(0x22FFFFFF))) {
        Box(
            Modifier.fillMaxWidth(f).height(8.dp).clip(RoundedCornerShape(50))
                .background(if (f > 0.9f) Cx.Red else if (f > 0.75f) Cx.Amber else Cx.Cyan),
        )
    }
}

@Composable
private fun DisplayCard() {
    val ctx = LocalContext.current
    val d = remember { DisplayLab.read(ctx) }
    var fps by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        var frames = 0
        var start = androidx.compose.runtime.withFrameNanos { it }
        while (true) {
            val now = androidx.compose.runtime.withFrameNanos { it }
            frames++
            if (now - start >= 1_000_000_000L) {
                fps = (frames * 1e9 / (now - start)).roundToInt()
                frames = 0
                start = now
            }
        }
    }
    GlassCard(Modifier.fillMaxWidth(), accent = Cx.Cyan) {
        SectionTitle(tr("مواصفات الشاشة", "Display specs"))
        StatRow(tr("الدقة", "Resolution"), "${d.widthPx} × ${d.heightPx}")
        StatRow(tr("الكثافة (DPI)", "Density (DPI)"), "${d.densityDpi} (${d.xdpi.roundToInt()}×${d.ydpi.roundToInt()} PPI)")
        StatRow(tr("الحجم الفيزيائي", "Physical size"), String.format(Locale.US, "%.2f %s", d.inches, tr("بوصة", "inch")))
        StatRow("HDR", if (d.hdr.isEmpty()) tr("غير مدعوم", "Not supported") else d.hdr.joinToString(", "))
        StatRow(tr("ألوان واسعة", "Wide color gamut"), if (d.wideColor) tr("نعم", "Yes") else tr("لا", "No"))
        StatRow(tr("الأوضاع المدعومة", "Supported modes"), d.supportedHz.joinToString(" / ") { "${it.roundToInt()}Hz" })
        Box(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RadialGauge(
                progress = (fps / 144f),
                modifier = Modifier.size(110.dp),
                color = if (abs(fps - d.refreshHz) < 6f) Cx.Emerald else Cx.Amber,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$fps", color = Cx.TextHi, fontWeight = FontWeight.Black, fontSize = 26.sp)
                    Text("FPS", color = Cx.TextLo, fontSize = 11.sp)
                }
            }
            Column {
                Text(tr("معدل التحديث الحي", "Live refresh rate"), color = Cx.TextLo, fontSize = 12.sp)
                Text(DisplayLab.hzCategory(fps.toFloat()), color = Cx.Cyan, fontWeight = FontWeight.Black, fontSize = 24.sp)
                Text(
                    tr("الشاشة تعلن: ${d.refreshHz.roundToInt()}Hz", "Panel reports: ${d.refreshHz.roundToInt()}Hz"),
                    color = Cx.TextHi, fontSize = 12.sp,
                )
            }
        }
    }
}
