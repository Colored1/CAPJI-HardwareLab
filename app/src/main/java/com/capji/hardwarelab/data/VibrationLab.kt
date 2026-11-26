package com.capji.hardwarelab.data

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibrationLab {
    @Suppress("DEPRECATION")
    fun vibrator(ctx: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= 31) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun pulse(v: Vibrator) {
        v.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun heartbeat(v: Vibrator) {
        val t = longArrayOf(0, 90, 110, 90, 700, 90, 110, 90, 700)
        v.vibrate(VibrationEffect.createWaveform(t, -1))
    }

    fun wave(v: Vibrator) {
        if (v.hasAmplitudeControl()) {
            val t = longArrayOf(0, 120, 120, 120, 120, 120, 120, 120, 120, 120, 120, 120)
            val a = intArrayOf(0, 40, 90, 140, 200, 255, 200, 140, 90, 40, 0, 0)
            v.vibrate(VibrationEffect.createWaveform(t, a, -1))
        } else {
            val t = longArrayOf(0, 150, 100, 250, 100, 400, 100, 250, 100, 150)
            v.vibrate(VibrationEffect.createWaveform(t, -1))
        }
    }

    fun continuous(v: Vibrator) {
        v.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun cancel(v: Vibrator) {
        try {
            v.cancel()
        } catch (_: Throwable) {
        }
    }
}
