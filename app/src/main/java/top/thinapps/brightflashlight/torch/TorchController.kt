package top.thinapps.brightflashlight.torch

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.annotation.RequiresApi
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
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // pre-android 13 torch apis do not require the camera permission
            true
        }
    }

    private fun ensureCameraSelected(): Boolean {
        if (backCameraId != null) return true
        backCameraId = findBackCameraWithFlash()
        return backCameraId != null
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getMaximumIntensityTiramisu(chars: CameraCharacteristics): Int {
        // Reads the new characteristic value, which is restricted to API 33+
        return chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
    }

    private fun findBackCameraWithFlash(): String? {
        return try {
            // prefer back camera with flash
            val primary = cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        maxIntensity = getMaximumIntensityTiramisu(chars)
                    }
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
                if (hasFlash && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    maxIntensity = getMaximumIntensityTiramisu(chars)
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

    fun setTorch(on: Boolean): Boolean {
        if (!ensureCameraSelected()) return false
        val id = backCameraId ?: return false

        // android 13+ requires CAMERA permission for torch apis
        if (!hasCameraPermission()) return false

        return try {
            cm.setTorchMode(id, on)
            true
        } catch (_: CameraAccessException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            // id might be invalidated if camera stack changed
            backCameraId = null
            false
        } catch (_: IllegalStateException) {
            // rare: device busy
            false
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setTorchIntensityTiramisu(id: String, intensity: Int): Boolean {
        val level = intensity.coerceIn(0, maxIntensity)
        return try {
            if (level <= 0) {
                cm.setTorchMode(id, false)
            } else {
                cm.setTorchStrengthLevel(id, level) // The API 33+ call
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun setTorchIntensity(intensity: Int): Boolean {
        if (!ensureCameraSelected()) return false
        val id = backCameraId ?: return false

        if (!hasCameraPermission()) return false

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxIntensity > 1) {
                // Delegate to the annotated function if the API is available
                if (setTorchIntensityTiramisu(id, intensity)) return true
            }

            // Fallback for pre-android 13 or devices without variable strength,
            // or if the Tiramisu function failed.
            cm.setTorchMode(id, intensity > 0)
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
