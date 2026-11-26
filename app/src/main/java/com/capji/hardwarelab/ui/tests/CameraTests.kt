@file:OptIn(ExperimentalCamera2Interop::class)

package com.capji.hardwarelab.ui.tests

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.concurrent.futures.await
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.capji.hardwarelab.core.LocalAr
import com.capji.hardwarelab.core.tr
import com.capji.hardwarelab.data.CameraLab
import com.capji.hardwarelab.data.LensInfo
import com.capji.hardwarelab.data.TestStatus
import com.capji.hardwarelab.ui.components.GlassCard
import com.capji.hardwarelab.ui.components.PermissionGate
import com.capji.hardwarelab.ui.components.StatRow
import com.capji.hardwarelab.ui.components.findActivity
import com.capji.hardwarelab.ui.theme.Cx
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun CameraTest(onMark: (TestStatus) -> Unit) {
    PermissionGate(
        listOf(Manifest.permission.CAMERA),
        tr("يحتاج فحص الكاميرات إلى إذن الكاميرا. لا يتم حفظ أي صورة.", "The camera inspector needs the camera permission. No photo is saved."),
    ) { CameraContent(onMark) }
}

@Composable
private fun CameraContent(onMark: (TestStatus) -> Unit) {
    val ctx = LocalContext.current
    val ar = LocalAr.current
    val haptic = LocalHapticFeedback.current
    val owner = ctx.findActivity() as? LifecycleOwner
    val lenses = remember { CameraLab.lenses(ctx, ar) }
    var selected by remember { mutableStateOf(lenses.firstOrNull()?.id) }
    var torch by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var bindError by remember { mutableStateOf(false) }
    val streamed = remember { mutableStateListOf<String>() }
    val failed = remember { mutableStateListOf<String>() }
    val previewView = remember { PreviewView(ctx) }
    val currentId = remember { arrayOfNulls<String>(1) }

    LaunchedEffect(selected) {
        val id = selected ?: return@LaunchedEffect
        val lo = owner ?: return@LaunchedEffect
        currentId[0] = id
        torch = false
        bindError = false
        try {
            val provider = ProcessCameraProvider.getInstance(ctx).await()
            provider.unbindAll()
            val selector = CameraSelector.Builder()
                .addCameraFilter(
                    CameraFilter { infos -> infos.filter { Camera2CameraInfo.from(it).cameraId == id } },
                )
                .build()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            camera = provider.bindToLifecycle(lo, selector, preview)
        } catch (_: Throwable) {
            camera = null
            bindError = true
            if (id !in failed) failed.add(id)
        }
    }
    DisposableEffect(owner) {
        val obs = androidx.lifecycle.Observer<PreviewView.StreamState> { st ->
            if (st == PreviewView.StreamState.STREAMING) {
                val id = currentId[0]
                if (id != null && id !in streamed) streamed.add(id)
            }
        }
        if (owner != null) previewView.previewStreamState.observe(owner, obs)
        onDispose {
            previewView.previewStreamState.removeObserver(obs)
            try {
                ProcessCameraProvider.getInstance(ctx).get().unbindAll()
            } catch (_: Throwable) {
            }
        }
    }
    LaunchedEffect(streamed.size, failed.size) {
        if (streamed.isNotEmpty() && streamed.size >= lenses.size - failed.size) onMark(TestStatus.PASSED)
    }

    if (lenses.isEmpty()) {
        Text(tr("لا توجد كاميرات في هذا الجهاز", "No cameras found on this device"), color = Cx.Amber, modifier = Modifier.padding(16.dp))
        return
    }
    val sel = lenses.firstOrNull { it.id == selected }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (l in lenses) {
                FilterChip(
                    selected = l.id == selected,
                    onClick = { selected = l.id },
                    label = { Text("${l.role} (${l.id})${if (l.id in streamed) " ✓" else ""}", fontSize = 12.sp) },
                )
            }
        }
        Box(
            Modifier.fillMaxWidth().height(340.dp).clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.ui.viewinterop.AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            if (bindError) {
                Text(
                    tr("تعذّرت معاينة هذه العدسة عبر CameraX (قد تكون عدسة فيزيائية داخلية).", "This lens cannot be previewed through CameraX (it may be an internal physical lens)."),
                    color = Cx.Amber, modifier = Modifier.padding(16.dp),
                )
            }
        }
        if (sel != null) {
            GlassCard(Modifier.fillMaxWidth(), accent = Cx.Cyan) {
                StatRow(tr("النوع", "Type"), sel.role)
                StatRow(tr("الدقة", "Resolution"), String.format(Locale.US, "%.1f MP", sel.megapixels))
                StatRow(tr("فتحة العدسة", "Aperture"), sel.aperture?.let { String.format(Locale.US, "f/%.1f", it) } ?: "-")
                StatRow(tr("البعد البؤري", "Focal length"), sel.focalMm?.let { String.format(Locale.US, "%.2f mm", it) } ?: "-")
                StatRow(tr("المكافئ 35mm", "35mm equivalent"), sel.equivMm?.let { String.format(Locale.US, "%.0f mm", it) } ?: "-")
                StatRow(tr("الفلاش", "Flash"), if (sel.flash) tr("مدعوم", "Supported") else tr("لا", "No"))
                StatRow("OIS", if (sel.ois) tr("نعم", "Yes") else tr("لا", "No"))
                if (sel.physicalIds.isNotEmpty()) StatRow(tr("عدسات فيزيائية", "Physical lenses"), sel.physicalIds.joinToString(", "))
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    torch = !torch
                    camera?.cameraControl?.enableTorch(torch)
                },
                enabled = sel.flash && camera != null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Cx.Amber, contentColor = Cx.Obsidian),
            ) {
                Icon(Icons.Filled.FlashlightOn, null)
                Text(if (torch) tr("  إطفاء الكشاف الخلفي", "  Torch off") else tr("  تشغيل الكشاف الخلفي", "  Rear torch on"), fontWeight = FontWeight.Bold)
            }
        }
        Text(
            tr("تم فحص ${streamed.size} من ${lenses.size} عدسات", "Checked ${streamed.size} of ${lenses.size} lenses"),
            color = Cx.TextLo, fontSize = 12.sp,
        )
    }
}

@Composable
fun FlashlightTest() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val cm = remember { ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val id = remember { CameraLab.torchId(ctx) }
    var on by remember { mutableStateOf(false) }
    var strobing by remember { mutableStateOf(false) }

    fun set(v: Boolean) {
        val i = id ?: return
        try {
            cm.setTorchMode(i, v)
            on = v
        } catch (_: Throwable) {
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            try {
                id?.let { cm.setTorchMode(it, false) }
            } catch (_: Throwable) {
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (id == null) {
            Text(tr("لا يوجد فلاش خلفي في هذا الجهاز", "No rear flash on this device"), color = Cx.Amber, fontWeight = FontWeight.Bold)
        } else {
            GlassCard(Modifier.fillMaxWidth(), accent = if (on) Cx.Amber else null) {
                Text(
                    if (on) tr("الكشاف يعمل", "Torch is ON") else tr("الكشاف مُطفأ", "Torch is OFF"),
                    color = if (on) Cx.Amber else Cx.TextLo, fontWeight = FontWeight.Black, fontSize = 22.sp,
                )
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    set(!on)
                },
                enabled = !strobing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Cx.Amber, contentColor = Cx.Obsidian),
            ) { Text(tr("تشغيل / إطفاء", "Toggle"), fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        strobing = true
                        repeat(10) {
                            set(true); delay(150)
                            set(false); delay(150)
                        }
                        strobing = false
                    }
                },
                enabled = !strobing,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(tr("وميض سريع (اختبار)", "Strobe test")) }
        }
    }
}

