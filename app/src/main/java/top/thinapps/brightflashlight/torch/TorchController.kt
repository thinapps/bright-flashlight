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
    private val cm by lazy { appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager }

    private var backCameraId: String? = null
    private var maxStrengthLevel: Int = 1
    private var strengthSupported: Boolean = false
    private var probed = false

    private fun hasCameraPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun ensureCameraReady(): Boolean {
        if (backCameraId != null && probed) return true
        return try {
            val ids = cm.cameraIdList

            val chosenId = ids.firstOrNull { id ->
                try {
                    val c = cm.getCameraCharacteristics(id)
                    c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                        c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                } catch (_: Throwable) {
                    false
                }
            } ?: ids.firstOrNull { id ->
                try {
                    cm.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } catch (_: Throwable) {
                    false
                }
            }

            backCameraId = chosenId
            probeStrength(chosenId)
            backCameraId != null
        } catch (_: Throwable) {
            backCameraId = null
            strengthSupported = false
            maxStrengthLevel = 1
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
            val c = cm.getCameraCharacteristics(id)
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

    // compatibility shim for older callers (e.g., TorchService)
    fun getMaxIntensity(): Int = getMaxStrength()

    @SuppressLint("MissingPermission")
    fun setTorch(on: Boolean, level: Int = getMaxStrength()): Boolean {
        if (!ensureCameraReady()) return false
        val id = backCameraId ?: return false
        if (!hasCameraPermission()) return false

        return try {
            if (on && strengthSupported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val clamped = level.coerceIn(1, getMaxStrength())
                try {
                    setTorchStrengthCompat(cm, id, clamped)
                } catch (_: Throwable) {
                    cm.setTorchMode(id, true)
                }
            } else {
                cm.setTorchMode(id, on)
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
                    cm.setTorchMode(id, false)
                } else {
                    try {
                        setTorchStrengthCompat(cm, id, level)
                    } catch (_: Throwable) {
                        cm.setTorchMode(id, true)
                    }
                }
            } else {
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
            try {
                cm.setTorchMode(backCameraId ?: return false, intensity > 0)
            } catch (_: Throwable) {}
            false
        }
    }

    private fun setTorchStrengthCompat(cm: CameraManager, id: String, level: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            throw NoSuchMethodError("torch strength not available")
        }
        val m = CameraManager::class.java.getMethod(
            "setTorchStrengthLevel",
            String::class.java,
            Int::class.javaPrimitiveType
        )
        m.invoke(cm, id, level)
    }
}
