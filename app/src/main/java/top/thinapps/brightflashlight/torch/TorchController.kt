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

    // max steps for variable brightness; 1 means on/off only
    private var maxIntensity: Int = 1

    // camera permission is required on android 13+
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

    // find a camera with flash; prefer back camera. also read max intensity on api 33+
    private fun findBackCameraWithFlash(): String? {
        return try {
            // prefer back camera with flash
            val preferred = cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    maxIntensity = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                    } else {
                        1
                    }
                    true
                } else {
                    false
                }
            }
            if (preferred != null) return preferred

            // fallback: any camera with flash
            cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                if (hasFlash) {
                    maxIntensity = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                    } else {
                        1
                    }
                }
                hasFlash
            }
        } catch (_: Exception) {
            null
        }
    }

    // expose availability to ui
    fun isAvailable(): Boolean = ensureCameraSelected()

    // expose max steps to ui (use for slider max); 1 means only on/off
    fun getMaxIntensity(): Int = if (maxIntensity >= 1) maxIntensity else 1

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

    // intensity: 0 turns off; 1..max turns on with given strength on api 33+
    @SuppressLint("MissingPermission")
    fun setTorchIntensity(intensity: Int): Boolean {
        if (!ensureCameraSelected()) return false
        val id = backCameraId ?: return false
        if (!hasCameraPermission()) return false

        return try {
            val max = if (maxIntensity >= 1) maxIntensity else 1
            val level = intensity.coerceIn(0, max)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (level <= 0) {
                    cm.setTorchMode(id, false)
                } else {
                    cm.setTorchStrengthLevel(id, level)
                }
            } else {
                // pre-33 does not support variable strength
                cm.setTorchMode(id, level > 0)
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
        } catch (_: Throwable) {
            // last-resort fallback to simple on/off
            try {
                cm.setTorchMode(backCameraId ?: return false, intensity > 0)
            } catch (_: Throwable) { }
            false
        }
    }
}
