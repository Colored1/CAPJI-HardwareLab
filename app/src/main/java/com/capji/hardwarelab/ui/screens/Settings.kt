package com.capji.hardwarelab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capji.hardwarelab.BuildConfig
import com.capji.hardwarelab.core.tr
import com.capji.hardwarelab.ui.components.GlassCard
import com.capji.hardwarelab.ui.components.SectionTitle
import com.capji.hardwarelab.ui.components.StatRow
import com.capji.hardwarelab.ui.theme.Cx
import com.capji.hardwarelab.vm.MainViewModel

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val ar by vm.ar.collectAsStateWithLifecycle()
    Row(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Cx.TextHi)
                    }
                    Text(tr("الإعدادات", "Settings"), color = Cx.TextHi, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }
            }
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    SectionTitle(tr("اللغة", "Language"))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(selected = ar, onClick = { vm.setLanguage(true) }, label = { Text("العربية (RTL)") })
                        FilterChip(selected = !ar, onClick = { vm.setLanguage(false) }, label = { Text("English (LTR)") })
                    }
                }
            }
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    SectionTitle(tr("حول التطبيق", "About"))
                    StatRow(tr("الإصدار", "Version"), BuildConfig.VERSION_NAME)
                    StatRow(tr("الحزمة", "Package"), "com.capji.hardwarelab")
                    Text(
                        tr(
                            "جميع الفحوصات تُنفَّذ محليًا على جهازك بقراءات حقيقية من واجهات أندرويد الرسمية. لا تُرسل أي بيانات فحص إلى أي خادم.",
                            "All tests run locally on your device using real Android APIs. No inspection data is sent to any server.",
                        ),
                        color = Cx.TextLo, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    SectionTitle(tr("بيانات الاختبارات", "Test data"))
                    OutlinedButton(onClick = { vm.resetResults() }) { Text(tr("مسح كل نتائج الاختبارات", "Clear all test results")) }
                }
            }
            item { ContactCard(Modifier.fillMaxWidth()) }
        }
    }
}
