package com.capji.hardwarelab.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capji.hardwarelab.BuildConfig
import com.capji.hardwarelab.core.AppPrefs
import com.capji.hardwarelab.core.LocalAr
import com.capji.hardwarelab.core.tr
import com.capji.hardwarelab.firebase.RemoteConfigManager
import com.capji.hardwarelab.ui.components.LocalAnimScope
import com.capji.hardwarelab.ui.components.LocalSharedScope
import com.capji.hardwarelab.ui.components.openUrl
import com.capji.hardwarelab.ui.screens.DashboardScreen
import com.capji.hardwarelab.ui.screens.ReportsScreen
import com.capji.hardwarelab.ui.screens.SettingsScreen
import com.capji.hardwarelab.ui.screens.SpecsScreen
import com.capji.hardwarelab.ui.screens.SplashScreen
import com.capji.hardwarelab.ui.screens.TestsListScreen
import com.capji.hardwarelab.ui.tests.TestHost
import com.capji.hardwarelab.ui.theme.Cx
import com.capji.hardwarelab.vm.MainViewModel
import com.capji.hardwarelab.vm.Screen

@Composable
fun CapjiRoot(vm: MainViewModel = viewModel()) {
    val ar by vm.ar.collectAsStateWithLifecycle()
    var splash by rememberSaveable { mutableStateOf(true) }
    CompositionLocalProvider(
        LocalAr provides ar,
        LocalLayoutDirection provides if (ar) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        Box(Modifier.fillMaxSize().background(Cx.Obsidian)) {
            if (splash) {
                SplashScreen(onDone = { splash = false })
            } else {
                AppContent(vm)
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppContent(vm: MainViewModel) {
    val screen by vm.screen.collectAsStateWithLifecycle()
    BackHandler(enabled = screen !is Screen.Main) { vm.back() }

    SharedTransitionLayout {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                if (targetState is Screen.Settings) {
                    (slideInHorizontally(tween(320)) { it / 4 } + fadeIn(tween(320))) togetherWith
                        fadeOut(tween(200))
                } else {
                    fadeIn(tween(320)) togetherWith fadeOut(tween(220))
                }
            },
            label = "screens",
        ) { target ->
            CompositionLocalProvider(
                LocalSharedScope provides this@SharedTransitionLayout,
                LocalAnimScope provides this@AnimatedContent,
            ) {
                when (target) {
                    is Screen.Main -> MainShell(vm)
                    is Screen.Test -> TestHost(vm, target.id)
                    is Screen.Settings -> SettingsScreen(vm)
                }
            }
        }
    }
}

@Composable
fun MainShell(vm: MainViewModel) {
    val tab by vm.tab.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val remote by RemoteConfigManager.state.collectAsStateWithLifecycle()
    var dismissed by rememberSaveable { mutableStateOf(false) }

    // Ask for notification permission once (Android 13+) so FCM announcements can be shown.
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !AppPrefs.notifAsked(ctx)) {
            AppPrefs.setNotifAsked(ctx)
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).statusBarsPadding()) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(280)) { dir * it / 5 } + fadeIn(tween(280))) togetherWith
                        (slideOutHorizontally(tween(280)) { -dir * it / 5 } + fadeOut(tween(180)))
                },
                label = "tabs",
            ) { t ->
                when (t) {
                    0 -> DashboardScreen(vm)
                    1 -> SpecsScreen(vm)
                    2 -> TestsListScreen(vm)
                    else -> ReportsScreen(vm)
                }
            }
        }
        NavigationBar(containerColor = Cx.Surface) {
            val items = listOf(
                Triple(0, Icons.Filled.Dashboard, tr("لوحة الفحص", "Dashboard")),
                Triple(1, Icons.Filled.Memory, tr("العتاد والبطارية", "Hardware")),
                Triple(2, Icons.Filled.Tune, tr("الاختبارات", "Tests")),
                Triple(3, Icons.Filled.Description, tr("التقارير", "Reports")),
            )
            for ((i, icon, label) in items) {
                NavigationBarItem(
                    selected = tab == i,
                    onClick = { vm.selectTab(i) },
                    icon = { Icon(icon, contentDescription = label) },
                    label = { Text(label, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Cx.Obsidian,
                        selectedTextColor = Cx.Cyan,
                        indicatorColor = Cx.Cyan,
                        unselectedIconColor = Cx.TextLo,
                        unselectedTextColor = Cx.TextLo,
                    ),
                )
            }
        }
    }

    if (remote.latestVersionCode > BuildConfig.VERSION_CODE && (remote.forceUpdate || !dismissed)) {
        AlertDialog(
            onDismissRequest = { if (!remote.forceUpdate) dismissed = true },
            title = { Text(tr("تحديث جديد متاح", "Update available")) },
            text = {
                Text(
                    remote.updateMessage.ifBlank {
                        tr("يتوفر إصدار أحدث من التطبيق.", "A newer version of the app is available.")
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { if (remote.updateUrl.isNotBlank()) ctx.openUrl(remote.updateUrl) }) {
                    Text(tr("تحديث", "Update"))
                }
            },
            dismissButton = {
                if (!remote.forceUpdate) {
                    TextButton(onClick = { dismissed = true }) { Text(tr("لاحقًا", "Later")) }
                }
            },
            containerColor = Cx.Surface,
        )
    }
}
