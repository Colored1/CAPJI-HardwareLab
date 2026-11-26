package com.capji.hardwarelab.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capji.hardwarelab.core.tr
import com.capji.hardwarelab.data.TestGroup
import com.capji.hardwarelab.data.TestId
import com.capji.hardwarelab.data.TestStatus
import com.capji.hardwarelab.ui.components.GlassCard
import com.capji.hardwarelab.ui.components.StatusChip
import com.capji.hardwarelab.ui.theme.Cx
import com.capji.hardwarelab.vm.MainViewModel

@Composable
fun TestsListScreen(vm: MainViewModel) {
    val results by vm.results.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val passed = results.values.count { it == TestStatus.PASSED }
    val failed = results.values.count { it == TestStatus.FAILED }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(tr("الاختبارات التفاعلية", "Interactive tests"), color = Cx.TextHi, fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    tr("ناجح $passed • فاشل $failed • من ${TestId.entries.size}", "Passed $passed • Failed $failed • of ${TestId.entries.size}"),
                    color = Cx.TextLo, fontSize = 13.sp,
                )
                OutlinedButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.resetResults()
                }) { Text(tr("تصفير", "Reset")) }
            }
        }
        for (g in TestGroup.entries) {
            val ids = TestId.entries.filter { it.group == g }
            if (ids.isEmpty()) continue
            item(key = "h_${g.name}") {
                Text(tr(g.ar, g.en), color = Cx.Cyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }
            for (id in ids) {
                item(key = id.name) {
                    val st = results[id] ?: TestStatus.PENDING
                    GlassCard(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.openTest(id)
                        },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(id.icon, contentDescription = null, tint = Cx.Cyan, modifier = Modifier.size(26.dp))
                            Text(
                                tr(id.ar, id.en), color = Cx.TextHi, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            )
                            StatusChip(st)
                        }
                    }
                }
            }
        }
        item { Box(Modifier.height(8.dp)) }
    }
}
