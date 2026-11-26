package com.capji.hardwarelab.firebase

import com.capji.hardwarelab.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class RemoteState(
    val latestVersionCode: Long = 0L,
    val forceUpdate: Boolean = false,
    val updateMessage: String = "",
    val updateUrl: String = "",
    val bannerEnabled: Boolean = false,
    val bannerText: String = "",
)

/**
 * Firebase Remote Config wrapper. Safe when Firebase is not configured
 * (no google-services.json): it simply keeps the default values.
 */
object RemoteConfigManager {
    private val _state = MutableStateFlow(RemoteState())
    val state: StateFlow<RemoteState> = _state

    private val defaults: Map<String, Any> = mapOf(
        "latest_version_code" to 0L,
        "force_update" to false,
        "update_message" to "",
        "update_url" to "",
        "announcement_banner_enabled" to false,
        "announcement_text" to "",
    )

    fun init(app: android.content.Context) {
        try {
            if (FirebaseApp.getApps(app).isEmpty()) return
            val rc = FirebaseRemoteConfig.getInstance()
            val settings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0L else 3600L)
                .build()
            rc.setConfigSettingsAsync(settings)
            rc.setDefaultsAsync(defaults)
            publish(rc)
            rc.fetchAndActivate().addOnCompleteListener { publish(rc) }
        } catch (_: Throwable) {
            // Firebase not configured - keep defaults
        }
    }

    private fun publish(rc: FirebaseRemoteConfig) {
        try {
            _state.value = RemoteState(
                latestVersionCode = rc.getLong("latest_version_code"),
                forceUpdate = rc.getBoolean("force_update"),
                updateMessage = rc.getString("update_message"),
                updateUrl = rc.getString("update_url"),
                bannerEnabled = rc.getBoolean("announcement_banner_enabled"),
                bannerText = rc.getString("announcement_text"),
            )
        } catch (_: Throwable) {
        }
    }
}
