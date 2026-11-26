package com.capji.hardwarelab.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.capji.hardwarelab.core.tr
import com.capji.hardwarelab.ui.theme.Cx

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

fun Context.openUrl(url: String) {
    try {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
    } catch (_: Exception) {
    }
}

fun Context.dial(number: String) {
    try {
        val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
    } catch (_: Exception) {
    }
}

fun Context.hasPermission(p: String): Boolean =
    ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

/** Hides the system bars while a full-screen test is visible. */
@Composable
fun ImmersiveEffect() {
    val view = LocalView.current
    val ctx = LocalContext.current
    DisposableEffect(Unit) {
        val window = ctx.findActivity()?.window
        if (window != null) {
            val c = WindowCompat.getInsetsController(window, view)
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (window != null) {
                WindowCompat.getInsetsController(window, view).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

/** Requests runtime permissions and only shows [content] once all are granted. */
@Composable
fun PermissionGate(
    permissions: List<String>,
    rationale: String,
    content: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    fun granted() = permissions.all { ctx.hasPermission(it) }
    var ok by remember { mutableStateOf(granted()) }
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        ok = granted()
        asked = true
    }
    LaunchedEffect(Unit) {
        if (!ok) launcher.launch(permissions.toTypedArray())
    }
    if (ok) {
        content()
    } else {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(rationale, color = Cx.TextHi, textAlign = TextAlign.Center)
            Column(Modifier.padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { launcher.launch(permissions.toTypedArray()) }) {
                    Text(tr("منح الإذن", "Grant permission"))
                }
                if (asked) {
                    OutlinedButton(onClick = {
                        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName))
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ctx.startActivity(i)
                    }) { Text(tr("فتح إعدادات التطبيق", "Open app settings")) }
                }
            }
        }
    }
}
