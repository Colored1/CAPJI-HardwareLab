package com.capji.hardwarelab.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.capji.hardwarelab.core.pick
import kotlin.math.abs

data class BatteryReading(
    val percent: Int,
    val currentMa: Int,
    val currentSupported: Boolean,
    val voltageMv: Int,
    val tempC: Float,
    val health: Int,
    val status: Int,
    val charging: Boolean,
    val plugged: Int,
    val chargeCounterMah: Int,
    val estCapacityMah: Int,
    val technology: String,
    val cycleCount: Int,
    val powerW: Float,
    val fastCharge: Boolean,
)

object BatteryLab {
    fun read(ctx: Context): BatteryReading {
        val intent: Intent? = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0 && scale > 0) {
            (level * 100f / scale).toInt()
        } else {
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            (status == BatteryManager.BATTERY_STATUS_FULL && plugged != 0)

        val raw = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val supported = raw != Int.MIN_VALUE
        val mag = if (supported) abs(raw.toLong()) else 0L
        // Most devices report microamps, a few report milliamps.
        val ma = if (mag >= 20000L) (mag / 1000L).toInt() else mag.toInt()
        val signedMa = if (charging) ma else -ma

        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val temp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val tech = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "-"
        val cycles = if (Build.VERSION.SDK_INT >= 34) {
            intent?.getIntExtra("android.os.extra.CYCLE_COUNT", -1) ?: -1
        } else {
            -1
        }

        val counterRaw = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val counterMah = if (counterRaw != Int.MIN_VALUE && counterRaw > 0) counterRaw / 1000 else -1
        val est = if (counterMah > 0 && pct in 5..100) (counterMah * 100f / pct).toInt() else -1

        val powerW = (voltage / 1000f) * (ma / 1000f)
        val fast = charging && powerW >= 15f

        return BatteryReading(
            percent = pct,
            currentMa = signedMa,
            currentSupported = supported,
            voltageMv = voltage,
            tempC = temp,
            health = health,
            status = status,
            charging = charging,
            plugged = plugged,
            chargeCounterMah = counterMah,
            estCapacityMah = est,
            technology = tech,
            cycleCount = cycles,
            powerW = powerW,
            fastCharge = fast,
        )
    }

    fun healthLabel(h: Int, ar: Boolean): String = when (h) {
        BatteryManager.BATTERY_HEALTH_GOOD -> pick(ar, "جيدة", "Good")
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> pick(ar, "ارتفاع حرارة", "Overheat")
        BatteryManager.BATTERY_HEALTH_DEAD -> pick(ar, "تالفة", "Dead")
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> pick(ar, "جهد زائد", "Over voltage")
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> pick(ar, "عطل غير محدد", "Unspecified failure")
        BatteryManager.BATTERY_HEALTH_COLD -> pick(ar, "باردة", "Cold")
        else -> pick(ar, "غير معروفة", "Unknown")
    }

    fun healthScore(h: Int): Int = when (h) {
        BatteryManager.BATTERY_HEALTH_GOOD -> 100
        BatteryManager.BATTERY_HEALTH_COLD -> 70
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> 45
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> 40
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> 30
        BatteryManager.BATTERY_HEALTH_DEAD -> 0
        else -> 80
    }

    fun statusLabel(s: Int, ar: Boolean): String = when (s) {
        BatteryManager.BATTERY_STATUS_CHARGING -> pick(ar, "يشحن", "Charging")
        BatteryManager.BATTERY_STATUS_DISCHARGING -> pick(ar, "يفرغ", "Discharging")
        BatteryManager.BATTERY_STATUS_FULL -> pick(ar, "ممتلئة", "Full")
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> pick(ar, "لا يشحن", "Not charging")
        else -> pick(ar, "غير معروفة", "Unknown")
    }

    fun sourceLabel(plugged: Int, fast: Boolean, ar: Boolean): String {
        val base = when {
            plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0 -> pick(ar, "لاسلكي", "Wireless")
            plugged and BatteryManager.BATTERY_PLUGGED_AC != 0 -> pick(ar, "شاحن جداري (AC)", "AC charger")
            plugged and BatteryManager.BATTERY_PLUGGED_USB != 0 -> "USB"
            plugged != 0 -> pick(ar, "قاعدة شحن", "Dock")
            else -> pick(ar, "غير متصل بشاحن", "Not plugged")
        }
        return if (fast) base + pick(ar, " • شحن سريع ⚡", " • Fast charge ⚡") else base
    }
}
