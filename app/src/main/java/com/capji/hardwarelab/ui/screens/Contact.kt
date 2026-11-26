package com.capji.hardwarelab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capji.hardwarelab.core.tr
import com.capji.hardwarelab.ui.components.CapjiHardwareLogo
import com.capji.hardwarelab.ui.components.GlassCard
import com.capji.hardwarelab.ui.components.dial
import com.capji.hardwarelab.ui.components.openUrl
import com.capji.hardwarelab.ui.theme.Cx

const val URL_INSTAGRAM = "https://instagram.com/axh.d."
const val URL_TIKTOK = "https://tiktok.com/@c1fq_7"
const val URL_WHATSAPP = "https://wa.me/967772678571"
const val TEL_NUMBER = "+967772678571"

@Composable
fun ContactCard(modifier: Modifier = Modifier, showLogo: Boolean = true) {
    val ctx = LocalContext.current
    GlassCard(modifier.fillMaxWidth(), accent = Cx.Cyan) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (showLogo) CapjiHardwareLogo(Modifier.size(64.dp))
            Column {
                Text(
                    tr("حول كابجي وتواصل مع المطوّر", "About CAPJI & contact the developer"),
                    color = Cx.TextHi, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                )
                Text(
                    tr("CAPJI — إبداع • إعلام • تكنولوجيا", "CAPJI — Creative • Media • Technology"),
                    color = Cx.TextLo, fontSize = 12.sp,
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BrandButton(Modifier.weight(1f), Icons.Filled.CameraAlt, "Instagram", Color(0xFFE1306C)) { ctx.openUrl(URL_INSTAGRAM) }
            BrandButton(Modifier.weight(1f), Icons.Filled.MusicNote, "TikTok", Color(0xFF25F4EE)) { ctx.openUrl(URL_TIKTOK) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BrandButton(Modifier.weight(1f), Icons.Filled.ChatBubble, "WhatsApp", Color(0xFF25D366)) { ctx.openUrl(URL_WHATSAPP) }
            BrandButton(Modifier.weight(1f), Icons.Filled.Call, tr("اتصال مباشر", "Call"), Cx.Cyan) { ctx.dial(TEL_NUMBER) }
        }
        Text(
            tr("جميع الحقوق محفوظة لـ CAPJI © 2026", "All rights reserved to CAPJI © 2026"),
            color = Cx.TextLo, fontSize = 12.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        )
    }
}

@Composable
private fun BrandButton(modifier: Modifier, icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        Box(Modifier.size(8.dp))
        Text(label, color = Cx.TextHi, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
