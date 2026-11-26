package com.capji.hardwarelab.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class AudioRoute { EARPIECE, SPEAKER }

enum class ToneChannel { BOTH, LEFT, RIGHT }

object ToneGen {
    const val SR = 44100

    /** Stereo interleaved 16-bit samples. Logarithmic sweep f0 -> f1. */
    fun sweep(seconds: Float, f0: Float, f1: Float, ch: ToneChannel = ToneChannel.BOTH): ShortArray {
        val n = (seconds * SR).toInt()
        val out = ShortArray(n * 2)
        var phase = 0.0
        val ratio = (f1 / f0).toDouble()
        for (i in 0 until n) {
            val t = i.toDouble() / n
            val f = f0 * ratio.pow(t)
            phase += 2.0 * PI * f / SR
            val v = (sin(phase) * 0.7 * fade(i, n)).toFloat()
            put(out, i, v, ch)
        }
        return out
    }

    fun tone(seconds: Float, freq: Float, ch: ToneChannel = ToneChannel.BOTH): ShortArray {
        val n = (seconds * SR).toInt()
        val out = ShortArray(n * 2)
        for (i in 0 until n) {
            val v = (sin(2.0 * PI * freq * i / SR) * 0.7 * fade(i, n)).toFloat()
            put(out, i, v, ch)
        }
        return out
    }

    private fun fade(i: Int, n: Int): Double {
        val edge = (0.03 * SR).toInt()
        return when {
            i < edge -> i.toDouble() / edge
            i > n - edge -> (n - i).toDouble() / edge
            else -> 1.0
        }
    }

    private fun put(out: ShortArray, i: Int, v: Float, ch: ToneChannel) {
        val s = (v * Short.MAX_VALUE).toInt().toShort()
        out[2 * i] = if (ch == ToneChannel.RIGHT) 0 else s
        out[2 * i + 1] = if (ch == ToneChannel.LEFT) 0 else s
    }
}

/** Plays generated PCM through the earpiece (MODE_IN_COMMUNICATION) or the loudspeaker (MODE_NORMAL). */
class TonePlayer(private val ctx: Context) {
    private var job: Job? = null
    private val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val isPlaying: Boolean get() = job?.isActive == true

    @Suppress("DEPRECATION")
    private fun applyRoute(route: AudioRoute) {
        if (route == AudioRoute.EARPIECE) {
            am.mode = AudioManager.MODE_IN_COMMUNICATION
            if (Build.VERSION.SDK_INT >= 31) {
                val dev = am.availableCommunicationDevices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                }
                if (dev != null) am.setCommunicationDevice(dev)
            } else {
                am.isSpeakerphoneOn = false
            }
        } else {
            am.mode = AudioManager.MODE_NORMAL
            if (Build.VERSION.SDK_INT >= 31) {
                am.clearCommunicationDevice()
            } else {
                am.isSpeakerphoneOn = true
            }
        }
    }

    @Suppress("DEPRECATION")
    fun restore() {
        try {
            am.mode = AudioManager.MODE_NORMAL
            if (Build.VERSION.SDK_INT >= 31) am.clearCommunicationDevice() else am.isSpeakerphoneOn = false
        } catch (_: Throwable) {
        }
    }

    fun play(scope: CoroutineScope, route: AudioRoute, samples: ShortArray, onDone: () -> Unit = {}) {
        stop()
        job = scope.launch(Dispatchers.IO) {
            var track: AudioTrack? = null
            try {
                applyRoute(route)
                val attrs = AudioAttributes.Builder()
                    .setUsage(
                        if (route == AudioRoute.EARPIECE) AudioAttributes.USAGE_VOICE_COMMUNICATION
                        else AudioAttributes.USAGE_MEDIA,
                    )
                    .setContentType(
                        if (route == AudioRoute.EARPIECE) AudioAttributes.CONTENT_TYPE_SPEECH
                        else AudioAttributes.CONTENT_TYPE_MUSIC,
                    )
                    .build()
                val fmt = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(ToneGen.SR)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
                val minBuf = AudioTrack.getMinBufferSize(
                    ToneGen.SR, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                )
                track = AudioTrack.Builder()
                    .setAudioAttributes(attrs)
                    .setAudioFormat(fmt)
                    .setBufferSizeInBytes(max(minBuf, 32768))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                track.play()
                var pos = 0
                while (pos < samples.size && isActive) {
                    val n = minOf(4096, samples.size - pos)
                    val w = track.write(samples, pos, n)
                    if (w <= 0) break
                    pos += w
                }
                if (isActive) {
                    // let the tail drain
                    kotlinx.coroutines.delay(200)
                }
            } catch (_: Throwable) {
            } finally {
                try {
                    track?.stop()
                } catch (_: Throwable) {
                }
                try {
                    track?.release()
                } catch (_: Throwable) {
                }
                restore()
                onDone()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}

data class MicFrame(
    /** a few evenly spaced samples (-1..1) for the scrolling oscilloscope */
    val wave: FloatArray,
    val rmsDb: Float,
    val peakDb: Float,
    val raw: ShortArray,
)

object MicLab {
    const val SR = 44100

    private fun db(x: Double): Float = if (x <= 1e-9) -90f else (20.0 * log10(x)).toFloat().coerceAtLeast(-90f)

    /** Throws IllegalStateException if the microphone cannot be opened. */
    fun frames(): Flow<MicFrame> = flow {
        val minBuf = AudioRecord.getMinBufferSize(SR, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC, SR,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            max(minBuf, 4096) * 2,
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            throw IllegalStateException("Microphone not available")
        }
        try {
            rec.startRecording()
            val buf = ShortArray(2048)
            while (currentCoroutineContext().isActive) {
                val n = rec.read(buf, 0, buf.size)
                if (n <= 0) continue
                var sum = 0.0
                var peak = 0
                for (i in 0 until n) {
                    val s = buf[i].toInt()
                    sum += s.toDouble() * s
                    val a = if (s < 0) -s else s
                    if (a > peak) peak = a
                }
                val rms = sqrt(sum / n) / 32768.0
                val wave = FloatArray(32) { k -> buf[(k * n) / 32].toFloat() / 32768f }
                emit(MicFrame(wave, db(rms), db(peak / 32768.0), buf.copyOf(n)))
            }
        } finally {
            try {
                rec.stop()
            } catch (_: Throwable) {
            }
            rec.release()
        }
    }.flowOn(Dispatchers.IO)

    /** Plays recorded mono PCM through the loudspeaker (blocking; call from IO). */
    fun playMono(ctx: Context, samples: ShortArray) {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.mode = AudioManager.MODE_NORMAL
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val fmt = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SR)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val minBuf = AudioTrack.getMinBufferSize(SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(fmt)
            .setBufferSizeInBytes(max(minBuf, 16384))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        try {
            track.play()
            var pos = 0
            while (pos < samples.size) {
                val n = minOf(4096, samples.size - pos)
                val w = track.write(samples, pos, n)
                if (w <= 0) break
                pos += w
            }
            Thread.sleep(250)
        } finally {
            try {
                track.stop()
            } catch (_: Throwable) {
            }
            track.release()
        }
    }
}
