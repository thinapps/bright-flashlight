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

    // defaults to 1; we try to read real max on API 33+ via reflection
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

    /**
     * Reflective read of FLASH_INFO_STRENGTH_MAXIMUM_LEVEL (API 33+).
     * Avoids compile-time reference so this builds even if compileSdk < 33.
     */
    @Suppress("UNCHECKED_CAST")
    private fun getMaximumIntensityReflective(chars: CameraCharacteristics): Int {
        if (Build.VERSION.SDK_INT < 33) return 1
        return try {
            val keyField = CameraCharacteristics::class.java.getField(
                "FLASH_INFO_STRENGTH_MAXIMUM_LEVEL"
            )
            val keyObj = keyField.get(null) as? CameraCharacteristics.Key<Int>
            if (keyObj != null) (chars.get(keyObj) ?: 1) else 1
        } catch (_: Throwable) {
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
                    maxIntensity = getMaximumIntensityReflective(chars)
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
                    maxIntensity = getMaximumIntensityReflective(chars)
                }
                hasFlash
            }
        } catch (_: Exception) {
            null
        }
    }

    fun isAvailable(): Boolean = ensureCameraSelected()

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

    /**
     * Reflective call to CameraManager.setTorchStrengthLevel(String, int) (API 33+).
     * Returns true if the reflective call succeeded.
     */
    private fun setTorchStrengthLevelReflective(id: String, level: Int): Boolean {
        if (Build.VERSION.SDK_INT < 33) return false
        return try {
            val method = CameraManager::class.java.getMethod(
                "setTorchStrengthLevel",
                String::class.java,
                Int::class.javaPrimitiveType
            )
            method.invoke(cm, id, level)
            true
        } catch (_: Throwable) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun setTorchIntensity(intensity: Int): Boolean {
        if (!ensureCameraSelected()) return false
        val id = backCameraId ?: return false

        if (!hasCameraPermission()) return false

        return try {
            // try variable strength if available, else fall back
            if (maxIntensity > 1) {
                val level = intensity.coerceIn(0, maxIntensity)
                if (level <= 0) {
                    cm.setTorchMode(id, false)
                } else {
                    // reflectively call API 33+ method; returns false on older compileSdks/devices
                    if (!setTorchStrengthLevelReflective(id, level)) {
                        // fallback if reflection failed
                        cm.setTorchMode(id, true)
                    }
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
