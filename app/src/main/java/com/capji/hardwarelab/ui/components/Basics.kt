package com.capji.hardwarelab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capji.hardwarelab.core.tr
import com.capji.hardwarelab.data.TestStatus
import com.capji.hardwarelab.ui.theme.Cx

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val a = accent ?: Color.White
    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0x1CFFFFFF), Color(0x08FFFFFF))))
            .border(1.dp, Brush.linearGradient(listOf(a.copy(alpha = 0.35f), Color(0x10FFFFFF))), shape)
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, color: Color = Cx.Cyan) {
    Text(
        text = text,
        modifier = modifier.padding(vertical = 6.dp),
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
    )
}

@Composable
fun StatRow(label: String, value: String, valueColor: Color = Cx.TextHi, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Cx.TextLo, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1.3f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
fun StatusChip(status: TestStatus, modifier: Modifier = Modifier) {
    val (color, text) = when (status) {
        TestStatus.PASSED -> Cx.Emerald to tr("ناجح", "Passed")
        TestStatus.FAILED -> Cx.Red to tr("فاشل", "Failed")
        TestStatus.NA -> Cx.Amber to tr("غير متوفر", "N/A")
        TestStatus.PENDING -> Cx.TextLo to tr("لم يُفحص", "Pending")
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

fun statusColor(s: TestStatus): Color = when (s) {
    TestStatus.PASSED -> Cx.Emerald
    TestStatus.FAILED -> Cx.Red
    TestStatus.NA -> Cx.Amber
    TestStatus.PENDING -> Cx.TextLo
}

@Composable
fun screenTitleStyle() = MaterialTheme.typography.titleLarge
