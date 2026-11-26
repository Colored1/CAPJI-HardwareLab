package com.capji.hardwarelab.ui.tests

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capji.hardwarelab.core.tr
import com.capji.hardwarelab.data.TestId
import com.capji.hardwarelab.data.TestStatus
import com.capji.hardwarelab.ui.components.GlassCard
import com.capji.hardwarelab.ui.components.StatusChip
import com.capji.hardwarelab.ui.components.sharedCard
import com.capji.hardwarelab.ui.theme.Cx
import com.capji.hardwarelab.vm.MainViewModel

@Composable
fun TestHost(vm: MainViewModel, id: TestId) {
    val haptic = LocalHapticFeedback.current
    val results by vm.results.collectAsStateWithLifecycle()
    val gi by vm.guidedIndex.collectAsStateWithLifecycle()
    val status = results[id] ?: TestStatus.PENDING
    val onMark: (TestStatus) -> Unit = { vm.mark(id, it) }
    val onFinish: (TestStatus?) -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        vm.finish(id, it)
    }

    if (id.fullScreen) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            when (id) {
                TestId.TOUCH_GRID -> TouchGridTest(onMark, onFinish, vm::back)
                TestId.MULTI_TOUCH -> MultiTouchTest(onMark, onFinish, vm::back)
                else -> ColorsTest(onFinish, vm::back)
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.back() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Cx.TextHi)
            }
            Icon(id.icon, contentDescription = null, tint = Cx.Cyan)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(tr(id.ar, id.en), color = Cx.TextHi, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (gi >= 0) {
                    Text(
                        tr("الخطوة ${gi + 1} من ${TestId.entries.size}", "Step ${gi + 1} of ${TestId.entries.size}"),
                        color = Cx.Cyan, fontSize = 12.sp,
                    )
                }
            }
            StatusChip(status)
            Box(Modifier.padding(end = 8.dp))
        }
        Box(Modifier.weight(1f).fillMaxWidth().sharedCard("test-${id.name}")) {
            when (id) {
                TestId.EARPIECE -> SpeakerTest(earpiece = true, onMark = onMark)
                TestId.SPEAKER -> SpeakerTest(earpiece = false, onMark = onMark)
                TestId.MIC -> MicTest(onMark)
                TestId.VIBRATION -> VibrationTest()
                TestId.CAMERA -> CameraTest(onMark)
                TestId.FLASHLIGHT -> FlashlightTest()
                TestId.ACCEL -> SensorTest(SensorKind.ACCEL, onMark)
                TestId.GYRO -> SensorTest(SensorKind.GYRO, onMark)
                TestId.PROXIMITY -> SensorTest(SensorKind.PROXIMITY, onMark)
                TestId.LIGHT -> SensorTest(SensorKind.LIGHT, onMark)
                TestId.MAGNETO -> SensorTest(SensorKind.MAGNETO, onMark)
                TestId.BARO -> SensorTest(SensorKind.BARO, onMark)
                TestId.FINGERPRINT -> FingerprintTest(onMark)
                TestId.KEYS -> KeysTest(onMark)
                TestId.CONNECTIVITY -> ConnectivityTest()
                else -> {}
            }
        }
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = { onFinish(TestStatus.FAILED) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Cx.Red, contentColor = Color.White),
            ) { Icon(Icons.Filled.Close, null); Text(tr(" فشل", " Fail"), fontWeight = FontWeight.Bold) }
            Button(
                onClick = { onFinish(TestStatus.PASSED) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Cx.Emerald, contentColor = Cx.Obsidian),
            ) { Icon(Icons.Filled.Check, null); Text(tr(" نجاح", " Pass"), fontWeight = FontWeight.Bold) }
            if (gi >= 0) {
                OutlinedButton(onClick = { onFinish(null) }) { Icon(Icons.Filled.SkipNext, null) }
            }
        }
    }
}

/** Pass / Fail / Continue overlay used by the full-screen tests. */
@Composable
fun VerdictOverlay(
    title: String,
    onPass: () -> Unit,
    onFail: () -> Unit,
    onContinue: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(Color(0xE6090D16)),
        contentAlignment = Alignment.Center,
    ) {
        GlassCard(Modifier.padding(24.dp).fillMaxWidth(), accent = Cx.Cyan) {
            Text(title, color = Cx.TextHi, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onFail, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Cx.Red, contentColor = Color.White),
                ) { Text(tr("فشل", "Fail")) }
                Button(
                    onClick = onPass, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Cx.Emerald, contentColor = Cx.Obsidian),
                ) { Text(tr("نجاح", "Pass")) }
            }
            OutlinedButton(onClick = onContinue, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(tr("متابعة الاختبار", "Continue test"))
            }
        }
    }
}

@Suppress("unused")
@Composable
private fun unusedBack(b: () -> Unit) {
    BackHandler(enabled = false, onBack = b)
    val x by remember { mutableStateOf(0) }
    x.hashCode()
}
