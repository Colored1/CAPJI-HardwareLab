package com.capji.hardwarelab

import android.app.Application
import com.capji.hardwarelab.firebase.Push
import com.capji.hardwarelab.firebase.RemoteConfigManager
import com.google.firebase.FirebaseApp

class CapjiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Returns null / does nothing when google-services.json is absent.
            if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this)
        } catch (_: Throwable) {
        }
        Push.ensureChannel(this)
        Push.subscribe(this)
        RemoteConfigManager.init(this)
    }
}
