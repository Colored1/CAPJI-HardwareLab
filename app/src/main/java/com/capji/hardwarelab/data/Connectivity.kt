package com.capji.hardwarelab.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.capji.hardwarelab.core.pick
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class ConnSnapshot(
    val wifiPresent: Boolean,
    val wifiEnabled: Boolean,
    val wifiConnected: Boolean,
    val ssid: String,
    val rssi: Int,
    val freqMhz: Int,
    val linkMbps: Int,
    val telephony: Boolean,
    val simState: Int,
    val operator: String,
    val cellDbm: Int?,
    val cellLevel: Int?,
    val dataType: String,
    val cellularActive: Boolean,
    val btPresent: Boolean,
    val btEnabled: Boolean,
    val nfcPresent: Boolean,
    val nfcEnabled: Boolean,
    val gpsPresent: Boolean,
    val gpsEnabled: Boolean,
)

data class GnssState(
    val satsVisible: Int = 0,
    val satsUsed: Int = 0,
    val avgCn0: Float = 0f,
    val accuracyM: Float? = null,
    val hasFix: Boolean = false,
    val permissionDenied: Boolean = false,
)

object ConnectivityLab {
    fun bandText(freq: Int): String = when {
        freq <= 0 -> "-"
        freq < 3000 -> "2.4 GHz"
        freq < 5925 -> "5 GHz"
        else -> "6 GHz"
    }

    fun simLabel(state: Int, ar: Boolean): String = when (state) {
        TelephonyManager.SIM_STATE_READY -> pick(ar, "جاهزة", "Ready")
        TelephonyManager.SIM_STATE_ABSENT -> pick(ar, "لا توجد شريحة", "No SIM")
        TelephonyManager.SIM_STATE_PIN_REQUIRED -> pick(ar, "تتطلب PIN", "PIN required")
        TelephonyManager.SIM_STATE_PUK_REQUIRED -> pick(ar, "تتطلب PUK", "PUK required")
        TelephonyManager.SIM_STATE_NETWORK_LOCKED -> pick(ar, "مقفلة بالشبكة", "Network locked")
        else -> pick(ar, "غير معروفة", "Unknown")
    }

    private fun typeName(t: Int): String = when (t) {
        TelephonyManager.NETWORK_TYPE_LTE -> "LTE (4G)"
        20 -> "NR (5G)"
        TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_HSDPA,
        TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
        TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
        else -> "-"
    }

    private fun has(ctx: Context, perm: String): Boolean =
        ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission", "HardwareIds")
    fun snapshot(ctx: Context): ConnSnapshot {
        val app = ctx.applicationContext
        val pm = app.packageManager

        // ---- Wi-Fi
        val wifiPresent = pm.hasSystemFeature(PackageManager.FEATURE_WIFI)
        var wifiEnabled = false
        var wifiConnected = false
        var ssid = "-"
        var rssi = 0
        var freq = 0
        var link = 0
        try {
            val wm = app.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiEnabled = wm.isWifiEnabled
            val info = wm.connectionInfo
            if (info != null && info.networkId != -1 && info.rssi > -127) {
                wifiConnected = true
                rssi = info.rssi
                freq = info.frequency
                link = info.linkSpeed
                ssid = info.ssid?.trim('"') ?: "-"
                if (ssid == "<unknown ssid>") ssid = "-"
            }
        } catch (_: Throwable) {
        }

        // ---- Cellular / SIM
        val telephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        var simState = TelephonyManager.SIM_STATE_UNKNOWN
        var operator = "-"
        var dbm: Int? = null
        var level: Int? = null
        var dataType = "-"
        try {
            val tm = app.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            simState = tm.simState
            operator = tm.networkOperatorName.takeIf { !it.isNullOrBlank() } ?: "-"
            if (has(app, Manifest.permission.READ_PHONE_STATE)) {
                try {
                    dataType = typeName(tm.dataNetworkType)
                } catch (_: Throwable) {
                }
                if (Build.VERSION.SDK_INT >= 29) {
                    try {
                        val ss = tm.signalStrength
                        if (ss != null) {
                            level = ss.level
                            dbm = ss.cellSignalStrengths.firstOrNull()?.dbm
                        }
                    } catch (_: Throwable) {
                    }
                }
            }
        } catch (_: Throwable) {
        }

        var cellularActive = false
        try {
            val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            cellularActive = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        } catch (_: Throwable) {
        }

        // ---- Bluetooth
        var btPresent = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        var btEnabled = false
        try {
            val bm = app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bm?.adapter
            if (adapter != null) {
                btPresent = true
                btEnabled = adapter.isEnabled
            }
        } catch (_: Throwable) {
        }

        // ---- NFC
        var nfcPresent = pm.hasSystemFeature(PackageManager.FEATURE_NFC)
        var nfcEnabled = false
        try {
            val nfc = NfcAdapter.getDefaultAdapter(app)
            if (nfc != null) {
                nfcPresent = true
                nfcEnabled = nfc.isEnabled
            }
        } catch (_: Throwable) {
        }

        // ---- GPS
        val gpsPresent = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
        var gpsEnabled = false
        try {
            val lm = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (_: Throwable) {
        }

        return ConnSnapshot(
            wifiPresent, wifiEnabled, wifiConnected, ssid, rssi, freq, link,
            telephony, simState, operator, dbm, level, dataType, cellularActive,
            btPresent, btEnabled, nfcPresent, nfcEnabled, gpsPresent, gpsEnabled,
        )
    }

    /** Live GNSS satellite + fix readout. Must be collected on the main thread. */
    @SuppressLint("MissingPermission")
    fun gnssFlow(ctx: Context): Flow<GnssState> = callbackFlow {
        val app = ctx.applicationContext
        if (!has(app, Manifest.permission.ACCESS_FINE_LOCATION)) {
            trySend(GnssState(permissionDenied = true))
            awaitClose { }
            return@callbackFlow
        }
        val lm = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var visible = 0
        var used = 0
        var cn0 = 0f
        var acc: Float? = null
        var fix = false

        fun push() {
            trySend(GnssState(visible, used, cn0, acc, fix, false))
        }

        val statusCb = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                visible = status.satelliteCount
                var u = 0
                var sum = 0f
                for (i in 0 until status.satelliteCount) {
                    if (status.usedInFix(i)) u++
                    sum += status.getCn0DbHz(i)
                }
                used = u
                cn0 = if (visible > 0) sum / visible else 0f
                push()
            }

            override fun onFirstFix(ttffMillis: Int) {
                fix = true
                push()
            }
        }

        @Suppress("OVERRIDE_DEPRECATION")
        val locListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                fix = true
                acc = if (location.hasAccuracy()) location.accuracy else null
                push()
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            lm.registerGnssStatusCallback(statusCb, Handler(Looper.getMainLooper()))
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, locListener, Looper.getMainLooper())
        } catch (_: Throwable) {
            trySend(GnssState(permissionDenied = true))
        }
        push()
        awaitClose {
            try {
                lm.unregisterGnssStatusCallback(statusCb)
                lm.removeUpdates(locListener)
            } catch (_: Throwable) {
            }
        }
    }
}
