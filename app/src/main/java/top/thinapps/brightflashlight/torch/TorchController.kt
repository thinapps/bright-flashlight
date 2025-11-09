package top.thinapps.brightflashlight.torch

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraAccessException
import android.os.Build

class TorchController(context: Context) {

    private val appContext = context.applicationContext
    private val cm = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var backCameraId: String? = findBackCameraWithFlash()
    private var maxIntensity: Int = 1

    private fun findBackCameraWithFlash(): String? {
        return try {
            for (id in cm.cameraIdList) {
                val chars = cm.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        maxIntensity =
                            chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                    }
                    return id
                }
            }
            // fallback to any camera with flash
            cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                if (hasFlash && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    maxIntensity =
                        chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                }
                hasFlash
            }
        } catch (_: Exception) {
            null
        }
    }

    fun isAvailable(): Boolean = backCameraId != null

    fun setTorch(on: Boolean): Boolean {
        var id = backCameraId
        if (id == null) {
            backCameraId = findBackCameraWithFlash()
            id = backCameraId ?: return false
        }
        return try {
            cm.setTorchMode(id, on)
            true
        } catch (_: CameraAccessException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    fun getMaxIntensity(): Int = maxIntensity

    fun setTorchIntensity(intensity: Int): Boolean {
        var id = backCameraId
        if (id == null) {
            backCameraId = findBackCameraWithFlash()
            id = backCameraId ?: return false
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxIntensity > 1) {
                val level = intensity.coerceIn(0, maxIntensity)
                if (level <= 0) {
                    cm.setTorchMode(id, false)
                } else {
                    cm.setTorchStrengthLevel(id, level)
                }
            } else {
                cm.setTorchMode(id, intensity > 0)
            }
            true
        } catch (_: CameraAccessException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}
