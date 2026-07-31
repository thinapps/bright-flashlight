package top.thinapps.brightflashlight.torch

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.core.content.ContextCompat

class TorchController(context: Context) {

    private val appContext = context.applicationContext
    private val cameraManager by lazy { appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager }

    private var backCameraId: String? = null
    private var maxStrengthLevel: Int = 1
    private var strengthSupported: Boolean = false
    private var probed = false
    private var torchCallback: CameraManager.TorchCallback? = null

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
                } catch (e: Throwable) {
                    Log.w(TAG, "Unable to read back camera characteristics", e)
                    false
                }
            } ?: ids.firstOrNull { id ->
                try {
                    cameraManager.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } catch (e: Throwable) {
                    Log.w(TAG, "Unable to read flash camera characteristics", e)
                    false
                }
            }

            backCameraId = chosenId
            probeStrength(chosenId)
            backCameraId != null
        } catch (e: Throwable) {
            Log.w(TAG, "Unable to find a usable torch camera", e)
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
        } catch (e: Throwable) {
            Log.w(TAG, "Unable to read torch strength support", e)
            strengthSupported = false
            maxStrengthLevel = 1
        } finally {
            probed = true
        }
    }

    fun isAvailable(): Boolean = ensureCameraReady()

    fun registerTorchCallback(
        handler: Handler,
        onModeChanged: (Boolean) -> Unit,
        onUnavailable: () -> Unit
    ): Boolean {
        if (torchCallback != null) return true
        if (!ensureCameraReady()) return false
        val selectedCameraId = backCameraId ?: return false

        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId == selectedCameraId) onModeChanged(enabled)
            }

            override fun onTorchModeUnavailable(cameraId: String) {
                if (cameraId == selectedCameraId) onUnavailable()
            }
        }

        return try {
            cameraManager.registerTorchCallback(callback, handler)
            torchCallback = callback
            true
        } catch (e: Throwable) {
            Log.w(TAG, "Unable to register torch state callback", e)
            false
        }
    }

    fun unregisterTorchCallback() {
        val callback = torchCallback ?: return
        torchCallback = null
        try {
            cameraManager.unregisterTorchCallback(callback)
        } catch (e: Throwable) {
            Log.w(TAG, "Unable to unregister torch state callback", e)
        }
    }

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
                } catch (e: Throwable) {
                    Log.w(TAG, "Unable to set torch strength, falling back to default torch", e)
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
                    } catch (e: Throwable) {
                        Log.w(TAG, "Unable to set torch intensity, falling back to default torch", e)
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
        } catch (e: Throwable) {
            Log.w(TAG, "Unexpected torch intensity failure", e)
            try {
                cameraManager.setTorchMode(backCameraId ?: return false, intensity > 0)
            } catch (fallbackError: Throwable) {
                Log.w(TAG, "Fallback torch mode update failed", fallbackError)
            }
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

    private companion object {
        const val TAG = "TorchController"
    }
}
