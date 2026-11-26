package com.capji.hardwarelab.ui.tests

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.capji.hardwarelab.core.LocalAr
import com.capji.hardwarelab.core.tr
import com.capji.hardwarelab.data.ConnSnapshot
import com.capji.hardwarelab.data.ConnectivityLab
import com.capji.hardwarelab.data.GnssState
import com.capji.hardwarelab.data.KeyBus
import com.capji.hardwarelab.data.TestStatus
import com.capji.hardwarelab.ui.components.GlassCard
import com.capji.hardwarelab.ui.components.StatRow
import com.capji.hardwarelab.ui.components.findActivity
import com.capji.hardwarelab.ui.components.hasPermission
import com.capji.hardwarelab.ui.theme.Cx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

// ------------------------------------------------------------ Fingerprint / biometrics

@Composable
fun FingerprintTest(onMark: (TestStatus) -> Unit) {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val bm = remember { BiometricManager.from(ctx) }
    val code = remember { bm.canAuthenticate(Authenticators.BIOMETRIC_WEAK) }
    val hasFp = remember { ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) }
    var result by remember { mutableStateOf("") }
    var ok by remember { mutableStateOf<Boolean?>(null) }

    val noHw = code == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE || code == BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED
    LaunchedEffect(noHw) { if (noHw) onMark(TestStatus.NA) }

    val sOk = tr("تمت المصادقة بنجاح ✓", "Authentication succeeded ✓")
    val sErr = tr("فشل التحقق: ", "Failed: ")
    val sTitle = tr("اختبار البصمة - CAPJI", "Fingerprint test - CAPJI")
    val sSub = tr("المس مستشعر البصمة", "Touch the fingerprint sensor")
    val sCancel = tr("إلغاء", "Cancel")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(Modifier.fillMaxWidth(), accent = Cx.Cyan) {
            StatRow(tr("مستشعر بصمة (Feature)", "Fingerprint feature"), if (hasFp) tr("موجود", "Present") else tr("غير معلن", "Not declared"))
            StatRow(
                tr("حالة القياسات الحيوية", "Biometric status"),
                when (code) {
                    BiometricManager.BIOMETRIC_SUCCESS -> tr("جاهزة", "Ready")
                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> tr("لا توجد بصمة مسجّلة", "None enrolled")
                    BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> tr("الحساس غير متاح الآن", "Hardware unavailable")
                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE, BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> tr("لا يوجد حساس", "No hardware")
                    else -> tr("غير معروفة", "Unknown")
                },
            )
        }
        if (code == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            Text(tr("سجّل بصمة من إعدادات الجهاز ثم أعد الاختبار.", "Enroll a fingerprint in device settings, then retry."), color = Cx.Amber)
        }
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val act = ctx.findActivity() as? FragmentActivity ?: return@Button
                val prompt = BiometricPrompt(
                    act, ContextCompat.getMainExecutor(ctx),
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(r: BiometricPrompt.AuthenticationResult) {
                            ok = true
                            result = sOk
                            onMark(TestStatus.PASSED)
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_CANCELED) {
                                ok = false
                                result = sErr + errString
                                onMark(TestStatus.FAILED)
                            } else {
                                result = errString.toString()
                            }
                        }
                    },
                )
                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(sTitle)
                    .setSubtitle(sSub)
                    .setAllowedAuthenticators(Authenticators.BIOMETRIC_WEAK)
                    .setNegativeButtonText(sCancel)
                    .build()
                prompt.authenticate(info)
            },
            enabled = code == BiometricManager.BIOMETRIC_SUCCESS,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Cx.Cyan, contentColor = Cx.Obsidian),
        ) { Text(tr("ابدأ اختبار البصمة", "Start biometric test"), fontWeight = FontWeight.Bold) }
        if (result.isNotEmpty()) {
            Text(result, color = if (ok == true) Cx.Emerald else Cx.Red, fontWeight = FontWeight.Bold)
        }
    }
}

// ------------------------------------------------------------ Volume keys

@Composable
fun KeysTest(onMark: (TestStatus) -> Unit) {
    var up by remember { mutableStateOf(false) }
    var down by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        KeyBus.capturing = true
        onDispose { KeyBus.capturing = false }
    }
    LaunchedEffect(Unit) {
        KeyBus.events.collect { code ->
            if (code == android.view.KeyEvent.KEYCODE_VOLUME_UP) up = true
            if (code == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) down = true
        }
    }
    LaunchedEffect(up, down) { if (up && down) onMark(TestStatus.PASSED) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(tr("اضغط زر رفع الصوت ثم زر خفض الصوت.", "Press Volume Up, then Volume Down."), color = Cx.TextHi)
        KeyTile(tr("رفع الصوت (Volume +)", "Volume Up (+)"), up)
        KeyTile(tr("خفض الصوت (Volume −)", "Volume Down (−)"), down)
        Text(
            tr("زر التشغيل لا يمكن اعتراضه من التطبيقات.", "The power button cannot be intercepted by apps."),
            color = Cx.TextLo, fontSize = 12.sp,
        )
    }
}

@Composable
private fun KeyTile(label: String, pressed: Boolean) {
    val c by animateColorAsState(if (pressed) Cx.Emerald else Color(0x22FFFFFF), label = "key")
    Box(
        Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(22.dp)).background(c.copy(alpha = if (pressed) 0.85f else 1f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            (if (pressed) "✓  " else "") + label,
            color = if (pressed) Cx.Obsidian else Cx.TextHi, fontWeight = FontWeight.Black, fontSize = 20.sp, textAlign = TextAlign.Center,
        )
    }
}

// ------------------------------------------------------------ Connectivity & GPS

@Composable
fun ConnectivityTest() {
    val ctx = LocalContext.current
    val ar = LocalAr.current
    var locGranted by remember { mutableStateOf(ctx.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        locGranted = ctx.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    LaunchedEffect(Unit) {
        val need = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE)
            .filter { !ctx.hasPermission(it) }
        if (need.isNotEmpty()) launcher.launch(need.toTypedArray())
    }

    var snap by remember { mutableStateOf<ConnSnapshot?>(null) }
    LaunchedEffect(locGranted) {
        while (isActive) {
            snap = withContext(Dispatchers.IO) { ConnectivityLab.snapshot(ctx) }
            delay(1000)
        }
    }
    var gnss by remember { mutableStateOf(GnssState()) }
    LaunchedEffect(locGranted) {
        if (locGranted) ConnectivityLab.gnssFlow(ctx).collect { gnss = it }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val s = snap
        if (s == null) {
            Text(tr("جارٍ الفحص…", "Scanning…"), color = Cx.TextLo)
            return@Column
        }
        GlassCard(Modifier.fillMaxWidth(), accent = Cx.Cyan) {
            Text("Wi-Fi", color = Cx.Cyan, fontWeight = FontWeight.Bold)
            StatRow(tr("الحالة", "State"), if (!s.wifiPresent) tr("لا يوجد", "Not present") else if (s.wifiConnected) tr("متصل", "Connected") else if (s.wifiEnabled) tr("مفعّل - غير متصل", "On - not connected") else tr("مُطفأ", "Off"))
            if (s.wifiConnected) {
                StatRow(tr("الشبكة", "Network"), s.ssid)
                StatRow(tr("قوة الإشارة (RSSI)", "Signal (RSSI)"), "${s.rssi} dBm", if (s.rssi > -70) Cx.Emerald else if (s.rssi > -80) Cx.Amber else Cx.Red)
                StatRow(tr("النطاق", "Band"), ConnectivityLab.bandText(s.freqMhz) + " (${s.freqMhz} MHz)")
                StatRow(tr("سرعة الوصلة", "Link speed"), "${s.linkMbps} Mbps")
            }
        }
        GlassCard(Modifier.fillMaxWidth(), accent = Cx.Emerald) {
            Text(tr("الشبكة الخلوية و SIM", "Cellular & SIM"), color = Cx.Emerald, fontWeight = FontWeight.Bold)
            StatRow(tr("الهاتف", "Telephony"), if (s.telephony) tr("موجود", "Present") else tr("لا يوجد", "Not present"))
            StatRow("SIM", ConnectivityLab.simLabel(s.simState, ar))
            StatRow(tr("المشغّل", "Operator"), s.operator)
            StatRow(tr("نوع الشبكة", "Network type"), s.dataType)
            StatRow(tr("الإشارة", "Signal"), if (s.cellDbm != null) "${s.cellDbm} dBm" + (s.cellLevel?.let { " (${it}/4)" } ?: "") else tr("غير متاح (امنح إذن الهاتف)", "N/A (grant phone permission)"))
            StatRow(tr("بيانات الجوال نشطة", "Mobile data active"), if (s.cellularActive) tr("نعم", "Yes") else tr("لا", "No"))
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Bluetooth / NFC", color = Cx.Cyan, fontWeight = FontWeight.Bold)
            StatRow("Bluetooth", if (!s.btPresent) tr("لا يوجد", "Not present") else if (s.btEnabled) tr("موجود - مفعّل", "Present - on") else tr("موجود - مُطفأ", "Present - off"))
            StatRow("NFC", if (!s.nfcPresent) tr("لا يوجد", "Not present") else if (s.nfcEnabled) tr("موجود - مفعّل", "Present - on") else tr("موجود - مُطفأ", "Present - off"))
        }
        GlassCard(Modifier.fillMaxWidth(), accent = Cx.Amber) {
            Text("GPS", color = Cx.Amber, fontWeight = FontWeight.Bold)
            StatRow(tr("المستقبِل", "Receiver"), if (s.gpsPresent) tr("موجود", "Present") else tr("لا يوجد", "Not present"))
            StatRow(tr("خدمة الموقع", "Location service"), if (s.gpsEnabled) tr("مفعّلة", "On") else tr("مُطفأة - فعّلها", "Off - enable it"))
            if (!locGranted) {
                Text(tr("امنح إذن الموقع لقراءة الأقمار الصناعية.", "Grant location permission to read satellites."), color = Cx.Amber, fontSize = 12.sp)
            } else {
                StatRow(tr("أقمار مرئية", "Satellites in view"), gnss.satsVisible.toString())
                StatRow(tr("أقمار مستخدمة في التثبيت", "Used in fix"), gnss.satsUsed.toString())
                StatRow(tr("متوسط قوة الإشارة", "Avg C/N0"), String.format("%.1f dB-Hz", gnss.avgCn0))
                StatRow(tr("التثبيت (Fix)", "Fix"), if (gnss.hasFix) tr("تم ✓", "Locked ✓") else tr("جارٍ البحث… (جرّب بالخارج)", "Searching… (try outdoors)"), if (gnss.hasFix) Cx.Emerald else Cx.Amber)
                gnss.accuracyM?.let { StatRow(tr("الدقة", "Accuracy"), String.format("%.0f m", it)) }
            }
        }
    }
}
