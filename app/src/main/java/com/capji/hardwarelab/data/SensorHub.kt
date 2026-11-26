package com.capji.hardwarelab.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

data class SensorMeta(
    val name: String,
    val vendor: String,
    val maxRange: Float,
    val resolution: Float,
    val powerMa: Float,
)

object SensorHub {
    fun sensor(ctx: Context, type: Int): Sensor? {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        return sm.getDefaultSensor(type)
    }

    fun meta(ctx: Context, type: Int): SensorMeta? {
        val s = sensor(ctx, type) ?: return null
        return SensorMeta(s.name, s.vendor, s.maximumRange, s.resolution, s.power)
    }

    /** Emits a copy of the raw values for each event. */
    fun flow(ctx: Context, type: Int, delay: Int = SensorManager.SENSOR_DELAY_GAME): Flow<FloatArray> =
        callbackFlow {
            val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val s = sm.getDefaultSensor(type)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    trySend(event.values.copyOf())
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            if (s != null) sm.registerListener(listener, s, delay)
            awaitClose { sm.unregisterListener(listener) }
        }.conflate()
}
