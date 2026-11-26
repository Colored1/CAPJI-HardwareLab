package com.capji.hardwarelab.report

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.capji.hardwarelab.core.pick
import com.capji.hardwarelab.data.BatteryLab
import com.capji.hardwarelab.data.BatteryReading
import com.capji.hardwarelab.data.BenchResult
import com.capji.hardwarelab.data.CpuLab
import com.capji.hardwarelab.data.DisplayData
import com.capji.hardwarelab.data.DisplayLab
import com.capji.hardwarelab.data.GpuData
import com.capji.hardwarelab.data.GpuLab
import com.capji.hardwarelab.data.HealthScore
import com.capji.hardwarelab.data.MemLab
import com.capji.hardwarelab.data.ScoreResult
import com.capji.hardwarelab.data.TestId
import com.capji.hardwarelab.data.TestStatus
import com.capji.hardwarelab.data.formatBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReportData(
    val deviceName: String,
    val androidVersion: String,
    val timestamp: Long,
    val certId: String,
    val battery: BatteryReading,
    val socName: String,
    val cores: Int,
    val abi: String,
    val gpu: GpuData,
    val ramTotal: Long,
    val ramAvail: Long,
    val storageTotal: Long,
    val storageFree: Long,
    val display: DisplayData,
    val bench: BenchResult?,
    val results: List<Pair<TestId, TestStatus>>,
    val score: ScoreResult,
)

object ReportGenerator {

    const val COPYRIGHT_AR = "جميع الحقوق محفوظة لـ CAPJI © 2026"
    const val COPYRIGHT_EN = "All rights reserved to CAPJI © 2026"
    const val TITLE = "تقرير فحص جهاز معتمد - CAPJI Hardware Lab"

    suspend fun collect(
        ctx: Context,
        results: Map<TestId, TestStatus>,
        bench: BenchResult?,
    ): ReportData = withContext(Dispatchers.IO) {
        val bat = BatteryLab.read(ctx)
        val cpu = CpuLab.snapshot()
        val gpu = GpuLab.query()
        val ram = MemLab.ram(ctx)
        val st = MemLab.storage()
        val disp = DisplayLab.read(ctx)
        val now = System.currentTimeMillis()
        val name = (Build.MANUFACTURER.replaceFirstChar { it.uppercase() } + " " + Build.MODEL).trim()
        val id = "CAPJI-" + Integer.toHexString((name + now).hashCode()).uppercase(Locale.US)
        ReportData(
            deviceName = name,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            timestamp = now,
            certId = id,
            battery = bat,
            socName = cpu.soc,
            cores = cpu.cores,
            abi = cpu.abi,
            gpu = gpu,
            ramTotal = ram.totalBytes,
            ramAvail = ram.availBytes,
            storageTotal = st.totalBytes,
            storageFree = st.freeBytes,
            display = disp,
            bench = bench,
            results = TestId.entries.map { it to (results[it] ?: TestStatus.PENDING) },
            score = HealthScore.compute(results, bat, bench),
        )
    }

    private fun fmtDate(ts: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(ts))

    private fun statusText(s: TestStatus, ar: Boolean): String = when (s) {
        TestStatus.PASSED -> pick(ar, "ناجح", "Passed")
        TestStatus.FAILED -> pick(ar, "فاشل", "Failed")
        TestStatus.NA -> pick(ar, "غير متوفر", "N/A")
        TestStatus.PENDING -> pick(ar, "لم يُفحص", "Not tested")
    }

    private fun statusColor(s: TestStatus): Int = when (s) {
        TestStatus.PASSED -> Color.parseColor("#00A152")
        TestStatus.FAILED -> Color.parseColor("#D50032")
        TestStatus.NA -> Color.parseColor("#E08A00")
        TestStatus.PENDING -> Color.parseColor("#6B7A8C")
    }

    private fun gradeColor(g: String): Int = when (g) {
        "A+", "A" -> Color.parseColor("#00C853")
        "B" -> Color.parseColor("#FFB300")
        else -> Color.parseColor("#FF3D57")
    }

    private fun tp(size: Float, color: Int, bold: Boolean = false): TextPaint {
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
        p.textSize = size
        p.color = color
        p.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        return p
    }

    /** Draws single-line text (bidi-aware). rtl => anchored on the right edge. */
    private fun line(
        c: Canvas, text: String, p: TextPaint, left: Float, right: Float, baseline: Float,
        alignEnd: Boolean, maxWidth: Float = right - left,
    ) {
        val t = TextUtils.ellipsize(text, p, maxWidth, TextUtils.TruncateAt.END).toString()
        val w = p.measureText(t)
        val x = if (alignEnd) right - w else left
        c.drawText(t, x, baseline, p)
    }

    // ------------------------------------------------------------------ PDF

    private class Pager(val doc: PdfDocument, val w: Int, val h: Int) {
        var pageNo = 0
        lateinit var page: PdfDocument.Page
        lateinit var canvas: Canvas
        var y = 0f

        fun next(): Canvas {
            if (pageNo > 0) doc.finishPage(page)
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, pageNo).create())
            canvas = page.canvas
            y = 40f
            return canvas
        }

        fun finish() {
            doc.finishPage(page)
        }
    }

    fun createPdf(ctx: Context, d: ReportData, ar: Boolean): File {
        val W = 595
        val H = 842
        val M = 36f
        val L = M
        val R = W - M
        val doc = PdfDocument()
        val pg = Pager(doc, W, H)

        val ink = Color.parseColor("#0B1624")
        val soft = Color.parseColor("#5B6B7E")
        val cyan = Color.parseColor("#00A9C4")
        val rule = Color.parseColor("#D8E1EA")
        val titleP = tp(18f, Color.WHITE, true)
        val h2 = tp(13f, cyan, true)
        val labelP = tp(10f, soft)
        val valueP = tp(10f, ink, true)
        val line1 = Paint().apply { color = rule; strokeWidth = 0.8f }

        fun footer(c: Canvas) {
            val f = tp(8.5f, soft)
            line(c, pick(ar, COPYRIGHT_AR, COPYRIGHT_EN), f, L, R, (H - 34).toFloat(), ar)
            line(
                c, "Instagram: axh.d.  |  TikTok: @c1fq_7  |  WhatsApp / Call: +967772678571", f,
                L, R, (H - 20).toFloat(), ar,
            )
        }

        fun header(c: Canvas, first: Boolean) {
            val bar = Paint().apply {
                shader = LinearGradient(0f, 0f, W.toFloat(), 0f, Color.parseColor("#090D16"), Color.parseColor("#0E2A3C"), Shader.TileMode.CLAMP)
            }
            val hh = if (first) 118f else 46f
            c.drawRect(0f, 0f, W.toFloat(), hh, bar)
            val accent = Paint().apply { color = Color.parseColor("#00E5FF") }
            c.drawRect(0f, hh - 3f, W.toFloat(), hh, accent)
            if (first) {
                line(c, TITLE, titleP, L, R, 40f, ar)
                line(c, "CAPJI Hardware Lab  •  " + pick(ar, "شهادة فحص عتاد", "Hardware Inspection Certificate"), tp(10.5f, Color.parseColor("#A9C4D6")), L, R, 60f, ar)
                line(c, d.deviceName + "  •  " + d.androidVersion, tp(10f, Color.parseColor("#7FE9F7")), L, R, 80f, ar)
                line(c, fmtDate(d.timestamp) + "  •  ID: " + d.certId, tp(9f, Color.parseColor("#A9C4D6")), L, R, 98f, ar)
                // grade badge
                val gx = if (ar) L + 46f else R - 46f
                val gp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gradeColor(d.score.grade) }
                c.drawCircle(gx, 62f, 34f, gp)
                val gt = tp(26f, Color.WHITE, true)
                val gw = gt.measureText(d.score.grade)
                c.drawText(d.score.grade, gx - gw / 2f, 71f, gt)
            } else {
                line(c, TITLE, tp(11f, Color.WHITE, true), L, R, 29f, ar)
            }
            pg.y = hh + 22f
        }

        fun ensure(c0: Canvas, need: Float): Canvas {
            if (pg.y + need > H - 56) {
                footer(c0)
                val c = pg.next()
                header(c, false)
                return c
            }
            return c0
        }

        var c = pg.next()
        header(c, true)

        fun section(title: String) {
            c = ensure(c, 40f)
            line(c, title, h2, L, R, pg.y + 4f, ar)
            c.drawLine(L, pg.y + 10f, R, pg.y + 10f, line1)
            pg.y += 26f
        }

        fun row(label: String, value: String, vColor: Int? = null) {
            c = ensure(c, 18f)
            val half = (R - L) * 0.42f
            val vp = if (vColor != null) tp(10f, vColor, true) else valueP
            if (ar) {
                line(c, label, labelP, R - half, R, pg.y, true, half)
                line(c, value, vp, L, R - half - 8f, pg.y, false)
            } else {
                line(c, label, labelP, L, L + half, pg.y, false, half)
                line(c, value, vp, L + half + 8f, R, pg.y, true)
            }
            pg.y += 16f
        }

        // score summary
        section(pick(ar, "الملخص العام", "Overall summary"))
        row(pick(ar, "درجة الصحة", "Health score"), if (d.score.score != null) "${d.score.score}/100  (${d.score.grade})" else "-", gradeColor(d.score.grade))
        row(pick(ar, "اختبارات ناجحة", "Tests passed"), d.score.passed.toString(), statusColor(TestStatus.PASSED))
        row(pick(ar, "اختبارات فاشلة", "Tests failed"), d.score.failed.toString(), statusColor(TestStatus.FAILED))
        row(pick(ar, "غير متوفرة في الجهاز", "Not available"), d.score.na.toString())
        pg.y += 6f

        // battery
        val b = d.battery
        section(pick(ar, "البطارية والشحن", "Battery & charging"))
        row(pick(ar, "النسبة", "Level"), "${b.percent}%")
        row(pick(ar, "الحالة الصحية", "Health"), BatteryLab.healthLabel(b.health, ar), if (b.health == 2) statusColor(TestStatus.PASSED) else statusColor(TestStatus.FAILED))
        row(pick(ar, "التيار اللحظي", "Current now"), if (b.currentSupported) "${b.currentMa} mA" else pick(ar, "غير مدعوم", "Unsupported"))
        row(pick(ar, "الجهد", "Voltage"), "${b.voltageMv} mV")
        row(pick(ar, "الحرارة", "Temperature"), String.format(Locale.US, "%.1f °C", b.tempC))
        row(pick(ar, "الحالة", "Status"), BatteryLab.statusLabel(b.status, ar))
        row(pick(ar, "مصدر الشحن", "Charging source"), BatteryLab.sourceLabel(b.plugged, b.fastCharge, ar))
        row(pick(ar, "السعة التقديرية", "Estimated capacity"), if (b.estCapacityMah > 0) "${b.estCapacityMah} mAh" else "-")
        if (b.cycleCount > 0) row(pick(ar, "دورات الشحن", "Charge cycles"), b.cycleCount.toString())
        pg.y += 6f

        // cpu etc
        section(pick(ar, "المعالج والذاكرة والتخزين", "CPU, memory & storage"))
        row(pick(ar, "المعالج (SoC)", "SoC"), d.socName)
        row(pick(ar, "عدد الأنوية", "CPU cores"), d.cores.toString())
        row(pick(ar, "المعمارية", "ABI"), d.abi)
        row(pick(ar, "كرت الشاشة", "GPU"), d.gpu.renderer)
        row(pick(ar, "الذاكرة العشوائية", "RAM"), formatBytes(d.ramTotal) + " (" + formatBytes(d.ramAvail) + " " + pick(ar, "متاح", "free") + ")")
        row(pick(ar, "التخزين الداخلي", "Storage"), formatBytes(d.storageTotal) + " (" + formatBytes(d.storageFree) + " " + pick(ar, "متاح", "free") + ")")
        if (d.bench != null) {
            row(pick(ar, "سرعة الكتابة", "Write speed"), String.format(Locale.US, "%.0f MB/s", d.bench.writeMBs))
            row(pick(ar, "سرعة القراءة", "Read speed"), String.format(Locale.US, "%.0f MB/s", d.bench.readMBs))
        }
        pg.y += 6f

        section(pick(ar, "الشاشة", "Display"))
        val ds = d.display
        row(pick(ar, "الدقة", "Resolution"), "${ds.widthPx} × ${ds.heightPx}")
        row(pick(ar, "الكثافة", "Density"), "${ds.densityDpi} dpi")
        row(pick(ar, "الحجم الفيزيائي", "Screen size"), String.format(Locale.US, "%.2f\"", ds.inches))
        row(pick(ar, "معدل التحديث", "Refresh rate"), DisplayLab.hzCategory(ds.refreshHz) + "  (" + ds.supportedHz.joinToString("/") { Math.round(it).toString() } + ")")
        row("HDR", if (ds.hdr.isEmpty()) pick(ar, "غير مدعوم", "No") else ds.hdr.joinToString(", "))
        pg.y += 6f

        section(pick(ar, "نتائج الاختبارات", "Test results"))
        for ((id, st) in d.results) {
            row(pick(ar, id.ar, id.en), statusText(st, ar), statusColor(st))
        }

        c = ensure(c, 40f)
        pg.y += 8f
        val note = tp(8.5f, soft)
        line(c, pick(ar, "تم إنشاء هذا التقرير محليًا على الجهاز بقراءات حقيقية من واجهات أندرويد الرسمية.", "Generated locally on-device from real Android hardware APIs."), note, L, R, pg.y, ar)

        footer(c)
        pg.finish()

        val file = outFile(ctx, "CAPJI-Report-${d.certId}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    // ------------------------------------------------------------------ PNG card

    fun createPng(ctx: Context, d: ReportData, ar: Boolean): File {
        val W = 1080
        val H = 1620
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        val bg = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, H.toFloat(), Color.parseColor("#0B1424"), Color.parseColor("#090D16"), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), bg)

        // glow circuit lines
        val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1400E5FF"); style = Paint.Style.STROKE; strokeWidth = 3f }
        val path = Path()
        path.moveTo(0f, 220f); path.lineTo(260f, 220f); path.lineTo(320f, 160f); path.lineTo(W.toFloat(), 160f)
        path.moveTo(0f, H - 260f); path.lineTo(420f, H - 260f); path.lineTo(480f, H - 200f); path.lineTo(W.toFloat(), H - 200f)
        c.drawPath(path, lp)

        // border
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 5f
            shader = LinearGradient(0f, 0f, W.toFloat(), H.toFloat(), Color.parseColor("#00E5FF"), Color.parseColor("#00E676"), Shader.TileMode.CLAMP)
        }
        c.drawRoundRect(RectF(30f, 30f, W - 30f, H - 30f), 44f, 44f, border)

        val white = Color.WHITE
        val lo = Color.parseColor("#8A9BB0")
        val cyan = Color.parseColor("#00E5FF")
        val L = 80f
        val R = W - 80f

        line(c, "CAPJI Hardware Lab", tp(46f, cyan, true), L, R, 130f, ar)
        line(c, pick(ar, "شهادة فحص جهاز معتمد", "Certified Device Inspection"), tp(36f, white, true), L, R, 190f, ar)

        // chip emblem (simple)
        val ex = if (ar) 150f else W - 150f
        val ey = 150f
        val ep = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cyan; style = Paint.Style.STROKE; strokeWidth = 6f }
        c.drawRoundRect(RectF(ex - 48f, ey - 48f, ex + 48f, ey + 48f), 12f, 12f, ep)
        for (k in -1..1) {
            c.drawLine(ex + k * 26f, ey - 48f, ex + k * 26f, ey - 68f, ep)
            c.drawLine(ex + k * 26f, ey + 48f, ex + k * 26f, ey + 68f, ep)
            c.drawLine(ex - 48f, ey + k * 26f, ex - 68f, ey + k * 26f, ep)
            c.drawLine(ex + 48f, ey + k * 26f, ex + 68f, ey + k * 26f, ep)
        }
        val pulse = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#00E676"); style = Paint.Style.STROKE; strokeWidth = 6f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        val pp = Path()
        pp.moveTo(ex - 30f, ey); pp.lineTo(ex - 10f, ey); pp.lineTo(ex, ey - 26f); pp.lineTo(ex + 12f, ey + 26f); pp.lineTo(ex + 22f, ey); pp.lineTo(ex + 32f, ey)
        c.drawPath(pp, pulse)

        // device name
        line(c, d.deviceName, tp(44f, white, true), L, R, 330f, ar)
        line(c, d.androidVersion + "  •  " + d.socName, tp(28f, lo), L, R, 380f, ar)

        // grade ring
        val cx = W / 2f
        val cy = 620f
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 34f; color = Color.parseColor("#22FFFFFF"); strokeCap = Paint.Cap.ROUND }
        c.drawArc(RectF(cx - 170f, cy - 170f, cx + 170f, cy + 170f), 135f, 270f, false, ring)
        val sc = d.score.score
        if (sc != null) {
            ring.color = gradeColorLight(d.score.grade)
            c.drawArc(RectF(cx - 170f, cy - 170f, cx + 170f, cy + 170f), 135f, 270f * sc / 100f, false, ring)
        }
        val gp = tp(130f, white, true)
        val gw = gp.measureText(d.score.grade)
        c.drawText(d.score.grade, cx - gw / 2f, cy + 38f, gp)
        val sp = tp(36f, lo)
        val st = if (sc != null) "$sc / 100" else "-"
        c.drawText(st, cx - sp.measureText(st) / 2f, cy + 100f, sp)

        // stat tiles
        fun tile(x: Float, y: Float, w: Float, label: String, value: String, color: Int) {
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.parseColor("#14FFFFFF") }
            c.drawRoundRect(RectF(x, y, x + w, y + 130f), 26f, 26f, fill)
            val vp = tp(46f, color, true)
            c.drawText(value, x + w / 2f - vp.measureText(value) / 2f, y + 62f, vp)
            val lp2 = tp(24f, lo)
            val t = TextUtils.ellipsize(label, lp2, w - 20f, TextUtils.TruncateAt.END).toString()
            c.drawText(t, x + w / 2f - lp2.measureText(t) / 2f, y + 106f, lp2)
        }
        val tw = (R - L - 40f) / 3f
        val ty = 860f
        tile(L, ty, tw, pick(ar, "ناجح", "Passed"), d.score.passed.toString(), Color.parseColor("#00E676"))
        tile(L + tw + 20f, ty, tw, pick(ar, "فاشل", "Failed"), d.score.failed.toString(), Color.parseColor("#FF3D57"))
        tile(L + 2 * (tw + 20f), ty, tw, pick(ar, "غير متوفر", "N/A"), d.score.na.toString(), Color.parseColor("#FFB300"))

        // key readings
        val b = d.battery
        val rows = listOf(
            pick(ar, "صحة البطارية", "Battery health") to BatteryLab.healthLabel(b.health, ar),
            pick(ar, "التيار اللحظي", "Current") to (if (b.currentSupported) "${b.currentMa} mA" else "-"),
            pick(ar, "الحرارة", "Temperature") to String.format(Locale.US, "%.1f °C", b.tempC),
            pick(ar, "الذاكرة العشوائية", "RAM") to formatBytes(d.ramTotal),
            pick(ar, "التخزين", "Storage") to formatBytes(d.storageTotal),
            pick(ar, "الشاشة", "Display") to "${d.display.widthPx}×${d.display.heightPx} • " + DisplayLab.hzCategory(d.display.refreshHz),
            pick(ar, "سرعة التخزين", "Storage speed") to (if (d.bench != null) String.format(Locale.US, "W %.0f / R %.0f MB/s", d.bench.writeMBs, d.bench.readMBs) else "-"),
        )
        var y = 1070f
        val lab = tp(28f, lo)
        val valp = tp(30f, white, true)
        val half = (R - L) * 0.45f
        val rule = Paint().apply { color = Color.parseColor("#22FFFFFF"); strokeWidth = 2f }
        for ((k, v) in rows) {
            if (ar) {
                line(c, k, lab, R - half, R, y, true, half)
                line(c, v, valp, L, R - half - 10f, y, false)
            } else {
                line(c, k, lab, L, L + half, y, false, half)
                line(c, v, valp, L + half + 10f, R, y, true)
            }
            c.drawLine(L, y + 16f, R, y + 16f, rule)
            y += 56f
        }

        // footer
        line(c, fmtDate(d.timestamp) + "  •  " + d.certId, tp(26f, lo), L, R, H - 170f, ar)
        line(c, pick(ar, COPYRIGHT_AR, COPYRIGHT_EN), tp(28f, white, true), L, R, H - 120f, ar)
        line(c, "IG axh.d.  •  TikTok @c1fq_7  •  WhatsApp +967772678571", tp(24f, cyan), L, R, H - 76f, ar)

        val file = outFile(ctx, "CAPJI-Certificate-${d.certId}.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        return file
    }

    private fun gradeColorLight(g: String): Int = when (g) {
        "A+", "A" -> Color.parseColor("#00E676")
        "B" -> Color.parseColor("#FFB300")
        else -> Color.parseColor("#FF3D57")
    }

    private fun outFile(ctx: Context, name: String): File {
        val dir = File(ctx.cacheDir, "reports")
        dir.mkdirs()
        return File(dir, name)
    }

    // ------------------------------------------------------------------ share / save

    fun share(ctx: Context, file: File, mime: String, whatsapp: Boolean, caption: String) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (whatsapp) {
            for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
                try {
                    val i = Intent(send).setPackage(pkg)
                    ctx.startActivity(i)
                    return
                } catch (_: Exception) {
                }
            }
        }
        ctx.startActivity(Intent.createChooser(send, caption))
    }

    /** Copies the file to Downloads/CAPJI. Returns true on success. */
    fun saveToDownloads(ctx: Context, file: File, mime: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CAPJI")
                }
                val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
                ctx.contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                } ?: return false
                true
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CAPJI")
                dir.mkdirs()
                file.copyTo(File(dir, file.name), overwrite = true)
                true
            }
        } catch (_: Throwable) {
            false
        }
    }
}
