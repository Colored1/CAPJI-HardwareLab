package com.capji.hardwarelab.data

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import com.capji.hardwarelab.core.pick
import kotlin.math.sqrt

data class LensInfo(
    val id: String,
    val facing: Int,
    val role: String,
    val megapixels: Float,
    val aperture: Float?,
    val focalMm: Float?,
    val equivMm: Float?,
    val flash: Boolean,
    val ois: Boolean,
    val physicalIds: List<String>,
) {
    val isFront: Boolean get() = facing == CameraCharacteristics.LENS_FACING_FRONT
}

object CameraLab {
    private const val FULL_FRAME_DIAG = 43.27f

    fun lenses(ctx: Context, ar: Boolean = true): List<LensInfo> {
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val raw = ArrayList<LensInfo>()
        val ids = try {
            cm.cameraIdList
        } catch (_: Throwable) {
            emptyArray()
        }
        for (id in ids) {
            try {
                val c = cm.getCameraCharacteristics(id)
                val facing = c.get(CameraCharacteristics.LENS_FACING) ?: -1
                val px = c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
                val mp = if (px != null) px.width * px.height / 1_000_000f else 0f
                val phys = c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                val focal = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull()
                val aperture = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.firstOrNull()
                val flash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val oisModes = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                val ois = oisModes?.any { it == CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON } == true
                val equiv = if (phys != null && focal != null) {
                    val diag = sqrt(phys.width * phys.width + phys.height * phys.height)
                    if (diag > 0f) focal * FULL_FRAME_DIAG / diag else null
                } else {
                    null
                }
                val physIds = if (Build.VERSION.SDK_INT >= 28) c.physicalCameraIds.toList() else emptyList()
                val caps = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                val depth = caps?.any { it == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT } == true
                val role = when {
                    depth -> pick(ar, "عمق", "Depth")
                    facing == CameraCharacteristics.LENS_FACING_FRONT -> pick(ar, "أمامية", "Front")
                    equiv != null && equiv <= 20f -> pick(ar, "واسعة جدًا", "Ultra-wide")
                    equiv != null && equiv >= 50f -> pick(ar, "تقريب", "Telephoto")
                    facing == CameraCharacteristics.LENS_FACING_BACK -> pick(ar, "رئيسية", "Main")
                    else -> pick(ar, "خارجية", "External")
                }
                raw.add(LensInfo(id, facing, role, mp, aperture, focal, equiv, flash, ois, physIds))
            } catch (_: Throwable) {
            }
        }
        return raw
    }

    /** First back camera that has a flash unit (for the torch). */
    fun torchId(ctx: Context): String? {
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return try {
            cm.cameraIdList.firstOrNull { id ->
                val c = cm.getCameraCharacteristics(id)
                c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
        } catch (_: Throwable) {
            null
        }
    }
}
