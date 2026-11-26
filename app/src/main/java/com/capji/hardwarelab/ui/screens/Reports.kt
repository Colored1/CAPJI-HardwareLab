package com.capji.hardwarelab.ui.screens

import android.Manifest
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capji.hardwarelab.core.LocalAr
import com.capji.hardwarelab.core.tr
import com.capji.hardwarelab.report.ReportData
import com.capji.hardwarelab.report.ReportGenerator
import com.capji.hardwarelab.ui.components.GlassCard
import com.capji.hardwarelab.ui.components.StatRow
import com.capji.hardwarelab.ui.components.hasPermission
import com.capji.hardwarelab.ui.theme.Cx
import com.capji.hardwarelab.vm.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ReportsScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val ar = LocalAr.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val results by vm.results.collectAsStateWithLifecycle()
    val bench by vm.bench.collectAsStateWithLifecycle()

    val sFailGen = tr("تعذّر إنشاء التقرير", "Could not create the report")
    val sSaved = tr("تم الحفظ في Downloads/CAPJI", "Saved to Downloads/CAPJI")
    val sSaveFail = tr("فشل الحفظ", "Save failed")

    var busy by remember { mutableStateOf(false) }
    var data by remember { mutableStateOf<ReportData?>(null) }
    var pdf by remember { mutableStateOf<File?>(null) }
    var png by remember { mutableStateOf<File?>(null) }
    var preview by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var pendingSave by remember { mutableStateOf(false) }

    suspend fun generate() {
        busy = true
        try {
            val d = ReportGenerator.collect(ctx, results, bench.result)
            val (p, i) = withContext(Dispatchers.IO) {
                ReportGenerator.createPdf(ctx, d, ar) to ReportGenerator.createPng(ctx, d, ar)
            }
            val bmp = withContext(Dispatchers.IO) {
                val o = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeFile(i.absolutePath, o)
            }
            data = d
            pdf = p
            png = i
            preview = bmp?.asImageBitmap()
        } catch (_: Throwable) {
            Toast.makeText(ctx, sFailGen, Toast.LENGTH_SHORT).show()
        } finally {
            busy = false
        }
    }

    fun saveAll() {
        val ok1 = pdf?.let { ReportGenerator.saveToDownloads(ctx, it, "application/pdf") } ?: false
        val ok2 = png?.let { ReportGenerator.saveToDownloads(ctx, it, "image/png") } ?: false
        Toast.makeText(
            ctx,
            if (ok1 || ok2) sSaved else sSaveFail,
            Toast.LENGTH_LONG,
        ).show()
    }

    val storagePerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && pendingSave) saveAll()
        pendingSave = false
    }

    val caption = tr("تقرير فحص جهاز معتمد - CAPJI Hardware Lab", "Certified device inspection report - CAPJI Hardware Lab")

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(tr("التقارير والحقوق", "Reports & credits"), color = Cx.TextHi, fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
        item {
            GlassCard(Modifier.fillMaxWidth(), accent = Cx.Emerald) {
                Text(tr("شهادة فحص CAPJI المعتمدة", "Official CAPJI inspection certificate"), color = Cx.Emerald, fontWeight = FontWeight.Bold)
                Text(
                    tr(
                        "تجمع كل مواصفات العتاد وقراءات البطارية ونتائج الاختبارات في تقرير PDF وبطاقة PNG مع تقدير عام (A+ / A / B / C).",
                        "Compiles specs, battery readings and test results into a PDF and a PNG card with an overall grade (A+ / A / B / C).",
                    ),
                    color = Cx.TextLo, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp),
                )
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch { generate() }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Cx.Emerald, contentColor = Cx.Obsidian),
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                    Text(
                        if (busy) tr("  جارٍ الإنشاء…", "  Generating…") else tr("  إنشاء الشهادة (PDF + PNG)", "  Generate certificate (PDF + PNG)"),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        val d = data
        if (d != null) {
            item {
                GlassCard(Modifier.fillMaxWidth(), accent = Cx.Cyan) {
                    StatRow(tr("الجهاز", "Device"), d.deviceName)
                    StatRow(tr("التقدير العام", "Overall grade"), d.score.grade + (d.score.score?.let { "  ($it/100)" } ?: ""))
                    StatRow(tr("ناجح / فاشل", "Passed / failed"), "${d.score.passed} / ${d.score.failed}")
                    StatRow(tr("رقم الشهادة", "Certificate ID"), d.certId)
                }
            }
            val pv = preview
            if (pv != null) {
                item {
                    Image(
                        bitmap = pv,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { pdf?.let { ReportGenerator.share(ctx, it, "application/pdf", true, caption) } },
                        modifier = Modifier.weight(1f),
                    ) { Icon(Icons.Filled.Share, null); Text(" PDF • WhatsApp", fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = { png?.let { ReportGenerator.share(ctx, it, "image/png", true, caption) } },
                        modifier = Modifier.weight(1f),
                    ) { Icon(Icons.Filled.Image, null); Text(" PNG • WhatsApp", fontSize = 12.sp) }
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT <= 28 && !ctx.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            pendingSave = true
                            storagePerm.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            saveAll()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Icon(Icons.Filled.Download, null); Text(tr("  حفظ في التنزيلات", "  Save to Downloads")) }
            }
        }
        item { ContactCard(Modifier.fillMaxWidth()) }
        item { Box(Modifier.height(8.dp)) }
    }
}
