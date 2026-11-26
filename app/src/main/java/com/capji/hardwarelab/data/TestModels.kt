package com.capji.hardwarelab.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

enum class TestStatus { PENDING, PASSED, FAILED, NA }

enum class TestGroup(val ar: String, val en: String) {
    DISPLAY("الشاشة واللمس", "Display & Touch"),
    AUDIO("الصوت والاهتزاز", "Audio & Haptics"),
    CAMERA("الكاميرات والفلاش", "Cameras & Flash"),
    SENSORS("الحساسات", "Sensors"),
    SECURITY("البصمة والأزرار", "Biometrics & Keys"),
    CONNECT("الاتصال", "Connectivity"),
}

/** Order here = order of the "Full Guided Inspection". */
enum class TestId(
    val ar: String,
    val en: String,
    val icon: ImageVector,
    val group: TestGroup,
    val fullScreen: Boolean = false,
) {
    TOUCH_GRID("شبكة اللمس والمناطق الميتة", "Touch Grid & Dead Zones", Icons.Filled.GridOn, TestGroup.DISPLAY, true),
    MULTI_TOUCH("اللمس المتعدد (10 أصابع)", "10-Finger Multi-Touch", Icons.Filled.TouchApp, TestGroup.DISPLAY, true),
    COLORS("البكسلات الميتة وحرق الشاشة", "Dead Pixel & Burn-In", Icons.Filled.Palette, TestGroup.DISPLAY, true),
    EARPIECE("سماعة المكالمات", "Earpiece", Icons.Filled.PhoneInTalk, TestGroup.AUDIO),
    SPEAKER("السماعة الخارجية", "Loudspeaker", Icons.Filled.VolumeUp, TestGroup.AUDIO),
    MIC("الميكروفون", "Microphone", Icons.Filled.Mic, TestGroup.AUDIO),
    VIBRATION("محرك الاهتزاز", "Vibration Motor", Icons.Filled.Vibration, TestGroup.AUDIO),
    CAMERA("الكاميرات الأمامية والخلفية", "Front & Rear Cameras", Icons.Filled.PhotoCamera, TestGroup.CAMERA),
    FLASHLIGHT("كشاف الفلاش", "Flashlight", Icons.Filled.FlashlightOn, TestGroup.CAMERA),
    ACCEL("مقياس التسارع", "Accelerometer", Icons.Filled.ScreenRotation, TestGroup.SENSORS),
    GYRO("الجيروسكوب", "Gyroscope", Icons.Filled.Autorenew, TestGroup.SENSORS),
    PROXIMITY("حساس التقارب", "Proximity Sensor", Icons.Filled.PanTool, TestGroup.SENSORS),
    LIGHT("حساس الإضاءة", "Ambient Light", Icons.Filled.LightMode, TestGroup.SENSORS),
    MAGNETO("البوصلة (المغناطيسي)", "Magnetometer", Icons.Filled.Explore, TestGroup.SENSORS),
    BARO("البارومتر", "Barometer", Icons.Filled.Cloud, TestGroup.SENSORS),
    FINGERPRINT("البصمة والقياسات الحيوية", "Fingerprint / Biometrics", Icons.Filled.Fingerprint, TestGroup.SECURITY),
    KEYS("أزرار الصوت", "Volume Keys", Icons.Filled.Keyboard, TestGroup.SECURITY),
    CONNECTIVITY("الاتصال و GPS", "Connectivity & GPS", Icons.Filled.Wifi, TestGroup.CONNECT),
}
