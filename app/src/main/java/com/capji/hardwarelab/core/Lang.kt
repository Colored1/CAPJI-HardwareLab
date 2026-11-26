package com.capji.hardwarelab.core

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/** true = Arabic (RTL), false = English (LTR). Arabic is the default. */
val LocalAr = staticCompositionLocalOf { true }

@Composable
@ReadOnlyComposable
fun tr(ar: String, en: String): String = if (LocalAr.current) ar else en

fun pick(ar: Boolean, arText: String, enText: String): String = if (ar) arText else enText

object AppPrefs {
    private const val FILE = "capji_prefs"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isArabic(ctx: Context): Boolean = sp(ctx).getBoolean("ar", true)

    fun setArabic(ctx: Context, value: Boolean) {
        sp(ctx).edit().putBoolean("ar", value).apply()
    }

    fun getStatus(ctx: Context, key: String): String? = sp(ctx).getString("test_$key", null)

    fun setStatus(ctx: Context, key: String, value: String) {
        sp(ctx).edit().putString("test_$key", value).apply()
    }

    fun clearStatuses(ctx: Context) {
        val e = sp(ctx).edit()
        sp(ctx).all.keys.filter { it.startsWith("test_") }.forEach { e.remove(it) }
        e.apply()
    }

    fun notifAsked(ctx: Context): Boolean = sp(ctx).getBoolean("notif_asked", false)

    fun setNotifAsked(ctx: Context) {
        sp(ctx).edit().putBoolean("notif_asked", true).apply()
    }
}
