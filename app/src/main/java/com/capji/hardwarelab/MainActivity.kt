package com.capji.hardwarelab

import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.capji.hardwarelab.data.KeyBus
import com.capji.hardwarelab.ui.CapjiRoot
import com.capji.hardwarelab.ui.theme.CapjiTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        requestHighestRefreshRate()
        setContent {
            CapjiTheme {
                CapjiRoot()
            }
        }
    }

    /** Ask the system for the display's highest refresh-rate mode so the Hz detector sees the real maximum. */
    @Suppress("DEPRECATION")
    private fun requestHighestRefreshRate() {
        try {
            val d = windowManager.defaultDisplay
            val cur = d.mode
            val best = d.supportedModes
                .filter { it.physicalWidth == cur.physicalWidth && it.physicalHeight == cur.physicalHeight }
                .maxByOrNull { it.refreshRate }
            if (best != null) {
                val lp: WindowManager.LayoutParams = window.attributes
                lp.preferredDisplayModeId = best.modeId
                window.attributes = lp
            }
        } catch (_: Throwable) {
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (KeyBus.capturing &&
            (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        ) {
            if (event.action == KeyEvent.ACTION_DOWN) KeyBus.events.tryEmit(event.keyCode)
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
