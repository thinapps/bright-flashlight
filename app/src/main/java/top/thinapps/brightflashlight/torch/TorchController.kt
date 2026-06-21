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

    private val appContext = context.applicationContext
    private val cameraManager by lazy { appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager }

    private var backCameraId: String? = null
    private var maxStrengthLevel: Int = 1
    private var strengthSupported: Boolean = false
    private var probed = false

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun ensureCameraReady(): Boolean {
        if (backCameraId != null && probed) return true
        return try {
            val ids = cameraManager.cameraIdList

            val chosenId = ids.firstOrNull { id ->
                try {
                    val c = cameraManager.getCameraCharacteristics(id)
                    c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                        c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                } catch (_: Throwable) {
                    false
                }
            } ?: ids.firstOrNull { id ->
                try {
                    cameraManager.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } catch (_: Throwable) {
                    false
                }
            }

            backCameraId = chosenId
            probeStrength(chosenId)
            backCameraId != null
        } catch (_: Throwable) {
            resetCameraState()
            false
        }
    }

    private fun probeStrength(id: String?) {
        if (id == null) {
            strengthSupported = false
            maxStrengthLevel = 1
            probed = true
            return
        }
        if (probed) return
        try {
            val c = cameraManager.getCameraCharacteristics(id)
            maxStrengthLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                c.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
            } else {
                1
            }
            strengthSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxStrengthLevel > 1
        } catch (_: Throwable) {
            strengthSupported = false
            maxStrengthLevel = 1
        } finally {
            probed = true
        }
    }

    fun isAvailable(): Boolean = ensureCameraReady()

    fun getStrengthSupport(): Pair<Boolean, Int> {
        ensureCameraReady()
        return strengthSupported to getMaxStrength()
    }

    fun getMaxStrength(): Int = if (maxStrengthLevel >= 1) maxStrengthLevel else 1

    @SuppressLint("MissingPermission")
    fun setTorch(on: Boolean, level: Int = getMaxStrength()): Boolean {
        if (!ensureCameraReady()) return false
        val id = backCameraId ?: return false
        if (!hasCameraPermission()) return false

        return try {
            if (on && strengthSupported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val clamped = level.coerceIn(1, getMaxStrength())
                try {
                    setTorchStrength(id, clamped)
                } catch (_: Throwable) {
                    cameraManager.setTorchMode(id, true)
                }
            } else {
                cameraManager.setTorchMode(id, on)
            }
            true
        } catch (_: CameraAccessException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            resetCameraState()
            false
        } catch (_: IllegalStateException) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun setTorchIntensity(intensity: Int): Boolean {
        if (!ensureCameraReady()) return false
        val id = backCameraId ?: return false
        if (!hasCameraPermission()) return false

        return try {
            val max = getMaxStrength()
            val level = intensity.coerceIn(0, max)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && strengthSupported) {
                if (level <= 0) {
                    cameraManager.setTorchMode(id, false)
                } else {
                    try {
                        setTorchStrength(id, level)
                    } catch (_: Throwable) {
                        cameraManager.setTorchMode(id, true)
                    }
                }
            } else {
                cameraManager.setTorchMode(id, level > 0)
            }
            true
        } catch (_: CameraAccessException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            resetCameraState()
            false
        } catch (_: IllegalStateException) {
            false
        } catch (_: Throwable) {
            try {
                cameraManager.setTorchMode(backCameraId ?: return false, intensity > 0)
            } catch (_: Throwable) {}
            false
        }
    }

    private fun resetCameraState() {
        backCameraId = null
        strengthSupported = false
        maxStrengthLevel = 1
        probed = false
    }

    private fun setTorchStrength(id: String, level: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            cameraManager.turnOnTorchWithStrengthLevel(id, level)
        }
    }
}
