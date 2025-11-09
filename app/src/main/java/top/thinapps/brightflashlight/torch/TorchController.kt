package top.thinapps.brightflashlight.torch

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.core.content.ContextCompat

class TorchController(context: Context) {

    // use app context to avoid leaking activities
    private val appContext = context.applicationContext
    private val cm = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    // lazily discovered camera id with flash
    private var backCameraId: String? = null

    // defaults to 1; on android 13+ we read the max strength level if available
    private var maxIntensity: Int = 1

    // camera permission is required on android 13+ for torch apis
    private fun hasCameraPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun ensureCameraSelected(): Boolean {
        if (backCameraId != null) return true
        backCameraId = findBackCameraWithFlash()
        return backCameraId != null
    }

    private fun getMaximumIntensity(chars: CameraCharacteristics): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
        } else {
            1
        }
    }

    private fun findBackCameraWithFlash(): String? {
        return try {
            // prefer back camera with flash
            val primary = cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    maxIntensity = getMaximumIntensity(chars)
                    true
                } else {
                    false
                }
            }
            if (primary != null) return primary

            // fallback: any camera with flash
            cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                if (hasFlash) {
                    maxIntensity = getMaximumIntensity(chars)
                }
                hasFlash
            }
        } catch (_: Exception) {
            null
        }
    }

    fun isAvailable(): Boolean {
        if (!ensureCameraSelected()) return false
        return true
    }

    fun getMaxIntensity(): Int = maxIntensity

    @SuppressLint("MissingPermission")
    fun setTorch(on: Boolean): Boolean {
        if (!ensureCameraSelected()) return false
        val id = backCameraId ?: return false

        if (!hasCameraPermission()) return false

        return try {
            cm.setTorchMode(id, on)
            true
        } catch (_: CameraAccessException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            backCameraId = null
            false
        } catch (_: IllegalStateException) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun setTorchIntensity(intensity: Int): Boolean {
        if (!ensureCameraSelected()) return false
        val id = backCameraId ?: return false

        if (!hasCameraPermission()) return false

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
            backCameraId = null
            false
        } catch (_: IllegalStateException) {
            false
        }
    }
}
