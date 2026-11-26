package com.capji.hardwarelab.data

import android.app.ActivityManager
import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import com.capji.hardwarelab.core.pick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

// ---------------------------------------------------------------- CPU

data class CpuSnapshot(
    val cores: Int,
    val curMhz: List<Int>,
    val maxMhz: List<Int>,
    val loadPct: Int,
    /** 0 = /proc/stat, 1 = estimated from frequencies, 2 = unavailable */
    val loadSource: Int,
    val abi: String,
    val soc: String,
    val hardware: String,
)

object CpuLab {
    private var lastTotal = 0L
    private var lastIdle = 0L

    private fun readLongFile(path: String): Long? {
        return try {
            File(path).readText().trim().toLongOrNull()
        } catch (_: Exception) {
            null
        }
    }

    private fun coreCount(): Int {
        try {
            val txt = File("/sys/devices/system/cpu/possible").readText().trim()
            val parts = txt.split("-")
            if (parts.size == 2) {
                val a = parts[0].toInt()
                val b = parts[1].toInt()
                if (b >= a) return b - a + 1
            }
        } catch (_: Exception) {
        }
        return Runtime.getRuntime().availableProcessors()
    }

    private fun readStat(): Pair<Long, Long>? {
        try {
            val line = File("/proc/stat").bufferedReader().use { it.readLine() } ?: return null
            val p = line.trim().split(Regex("\\s+"))
            if (p.size < 8 || p[0] != "cpu") return null
            val v = p.drop(1).mapNotNull { it.toLongOrNull() }
            if (v.size < 5) return null
            val idle = v[3] + v[4]
            val total = v.sum()
            return Pair(total, idle)
        } catch (_: Exception) {
            return null
        }
    }

    fun socName(): String {
        val sdkSoc = if (Build.VERSION.SDK_INT >= 31) {
            val m = Build.SOC_MANUFACTURER
            val n = Build.SOC_MODEL
            if (n.isNotBlank() && n != Build.UNKNOWN) "$m $n".trim() else ""
        } else {
            ""
        }
        return sdkSoc.ifBlank { Build.HARDWARE }
    }

    fun snapshot(): CpuSnapshot {
        val n = coreCount()
        val cur = ArrayList<Int>()
        val max = ArrayList<Int>()
        for (i in 0 until n) {
            val c = readLongFile("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
            val m = readLongFile("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
            cur.add(if (c != null) (c / 1000L).toInt() else 0)
            max.add(if (m != null) (m / 1000L).toInt() else 0)
        }

        var load = 0
        var source = 2
        val stat = readStat()
        if (stat != null) {
            if (lastTotal > 0L && stat.first > lastTotal) {
                val dt = stat.first - lastTotal
                val di = stat.second - lastIdle
                load = (((dt - di) * 100f) / dt).toInt().coerceIn(0, 100)
                source = 0
            } else {
                source = 0
            }
            lastTotal = stat.first
            lastIdle = stat.second
        } else {
            val ratios = ArrayList<Float>()
            for (i in 0 until n) {
                if (cur[i] > 0 && max[i] > 0) ratios.add(cur[i].toFloat() / max[i])
            }
            if (ratios.isNotEmpty()) {
                load = (ratios.average() * 100).toInt().coerceIn(0, 100)
                source = 1
            }
        }

        return CpuSnapshot(
            cores = n,
            curMhz = cur,
            maxMhz = max,
            loadPct = load,
            loadSource = source,
            abi = Build.SUPPORTED_ABIS.joinToString(", "),
            soc = socName(),
            hardware = Build.HARDWARE + " / " + Build.BOARD,
        )
    }
}

// ---------------------------------------------------------------- GPU

data class GpuData(val renderer: String, val vendor: String, val version: String)

object GpuLab {
    /** Creates a tiny off-screen OpenGL ES 2.0 context to read the real GPU strings. */
    fun query(): GpuData {
        var display = EGL14.EGL_NO_DISPLAY
        var context = EGL14.EGL_NO_CONTEXT
        var surface = EGL14.EGL_NO_SURFACE
        try {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val ver = IntArray(2)
            if (!EGL14.eglInitialize(display, ver, 0, ver, 1)) return GpuData("-", "-", "-")
            val attribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE,
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val num = IntArray(1)
            EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, num, 0)
            val cfg = configs[0] ?: return GpuData("-", "-", "-")
            val ctxAttr = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            context = EGL14.eglCreateContext(display, cfg, EGL14.EGL_NO_CONTEXT, ctxAttr, 0)
            val surfAttr = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
            surface = EGL14.eglCreatePbufferSurface(display, cfg, surfAttr, 0)
            EGL14.eglMakeCurrent(display, surface, surface, context)
            val r = GLES20.glGetString(GLES20.GL_RENDERER) ?: "-"
            val v = GLES20.glGetString(GLES20.GL_VENDOR) ?: "-"
            val ve = GLES20.glGetString(GLES20.GL_VERSION) ?: "-"
            return GpuData(r, v, ve)
        } catch (_: Throwable) {
            return GpuData("-", "-", "-")
        } finally {
            try {
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface)
                if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
                if (display != EGL14.EGL_NO_DISPLAY) EGL14.eglTerminate(display)
            } catch (_: Throwable) {
            }
        }
    }
}

// ---------------------------------------------------------------- Thermal

object ThermalLab {
    /** -1 = unsupported (Android < 10). */
    fun status(ctx: Context): Int {
        if (Build.VERSION.SDK_INT < 29) return -1
        return try {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.currentThermalStatus
        } catch (_: Throwable) {
            -1
        }
    }

    fun label(status: Int, ar: Boolean): String = when (status) {
        -1 -> pick(ar, "غير مدعوم", "Not supported")
        0 -> pick(ar, "طبيعي (لا يوجد خنق)", "None (no throttling)")
        1 -> pick(ar, "خفيف", "Light")
        2 -> pick(ar, "متوسط - خنق أداء", "Moderate - throttling")
        3 -> pick(ar, "شديد - خنق قوي", "Severe throttling")
        4 -> pick(ar, "حرج", "Critical")
        5 -> pick(ar, "طوارئ", "Emergency")
        else -> pick(ar, "إيقاف وشيك", "Shutdown")
    }
}

// ---------------------------------------------------------------- RAM / Storage

data class MemData(val totalBytes: Long, val availBytes: Long, val lowMemory: Boolean)

data class StorageData(val totalBytes: Long, val freeBytes: Long)

object MemLab {
    fun ram(ctx: Context): MemData {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return MemData(mi.totalMem, mi.availMem, mi.lowMemory)
    }

    fun storage(): StorageData {
        return try {
            val st = StatFs(Environment.getDataDirectory().path)
            StorageData(st.totalBytes, st.availableBytes)
        } catch (_: Throwable) {
            StorageData(0L, 0L)
        }
    }
}

fun formatBytes(b: Long): String {
    if (b <= 0L) return "-"
    val gb = b / 1024.0 / 1024.0 / 1024.0
    return if (gb >= 1.0) String.format("%.2f GB", gb) else String.format("%.0f MB", b / 1024.0 / 1024.0)
}

// ---------------------------------------------------------------- Benchmark

data class BenchResult(val writeMBs: Float, val readMBs: Float, val sizeMb: Int, val timestamp: Long)

object StorageBench {
    /**
     * Writes a temporary file in the app cache (fsync'ed), reads it back, and deletes it.
     * Returns real measured throughput in MB/s.
     */
    suspend fun run(ctx: Context, sizeMb: Int = 96, onProgress: (Float) -> Unit): BenchResult =
        withContext(Dispatchers.IO) {
            val file = File(ctx.cacheDir, "capji_bench.tmp")
            try {
                val chunk = ByteArray(1024 * 1024)
                Random(42).nextBytes(chunk)

                val t0 = System.nanoTime()
                FileOutputStream(file).use { out ->
                    for (i in 0 until sizeMb) {
                        out.write(chunk)
                        onProgress(0.5f * (i + 1) / sizeMb)
                    }
                    out.flush()
                    out.fd.sync()
                }
                val writeSec = (System.nanoTime() - t0) / 1e9

                val buf = ByteArray(1024 * 1024)
                var total = 0L
                val t1 = System.nanoTime()
                FileInputStream(file).use { inp ->
                    while (true) {
                        val n = inp.read(buf)
                        if (n <= 0) break
                        total += n
                        onProgress(0.5f + 0.5f * total / (sizeMb * 1024f * 1024f))
                    }
                }
                val readSec = (System.nanoTime() - t1) / 1e9

                BenchResult(
                    writeMBs = (sizeMb / writeSec).toFloat(),
                    readMBs = (total / 1024.0 / 1024.0 / readSec).toFloat(),
                    sizeMb = sizeMb,
                    timestamp = System.currentTimeMillis(),
                )
            } finally {
                try {
                    file.delete()
                } catch (_: Throwable) {
                }
            }
        }
}

// ---------------------------------------------------------------- Display

data class DisplayData(
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val xdpi: Float,
    val ydpi: Float,
    val inches: Double,
    val refreshHz: Float,
    val supportedHz: List<Float>,
    val hdr: List<String>,
    val wideColor: Boolean,
)

object DisplayLab {
    @Suppress("DEPRECATION")
    fun read(ctx: Context): DisplayData {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val d: Display = wm.defaultDisplay
        val m = DisplayMetrics()
        d.getRealMetrics(m)
        val xdpi = if (m.xdpi > 0f) m.xdpi else m.densityDpi.toFloat()
        val ydpi = if (m.ydpi > 0f) m.ydpi else m.densityDpi.toFloat()
        val wIn = m.widthPixels / xdpi.toDouble()
        val hIn = m.heightPixels / ydpi.toDouble()
        val inches = sqrt(wIn * wIn + hIn * hIn)

        val rates = try {
            d.supportedModes.map { it.refreshRate }.distinctBy { Math.round(it) }.sorted()
        } catch (_: Throwable) {
            listOf(d.refreshRate)
        }

        val hdr = ArrayList<String>()
        try {
            val types = d.hdrCapabilities?.supportedHdrTypes
            if (types != null) {
                for (t in types) {
                    when (t) {
                        Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> hdr.add("Dolby Vision")
                        Display.HdrCapabilities.HDR_TYPE_HDR10 -> hdr.add("HDR10")
                        Display.HdrCapabilities.HDR_TYPE_HLG -> hdr.add("HLG")
                        Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> hdr.add("HDR10+")
                    }
                }
            }
        } catch (_: Throwable) {
        }

        return DisplayData(
            widthPx = m.widthPixels,
            heightPx = m.heightPixels,
            densityDpi = m.densityDpi,
            xdpi = xdpi,
            ydpi = ydpi,
            inches = inches,
            refreshHz = d.refreshRate,
            supportedHz = rates,
            hdr = hdr,
            wideColor = d.isWideColorGamut,
        )
    }

    fun hzCategory(hz: Float): String {
        val std = listOf(60, 72, 90, 120, 144, 165)
        val near = std.minByOrNull { abs(it - hz) } ?: 60
        return "$near Hz"
    }
}
