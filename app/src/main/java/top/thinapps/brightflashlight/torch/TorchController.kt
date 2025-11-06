package top.thinapps.brightflashlight.torch

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log

class TorchController(context: Context) {

    companion object { private const val TAG = "TorchController" }

    private val appContext = context.applicationContext
    private val cm: CameraManager =
        appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    // cached, but we can re-resolve once if needed
    private var backCameraId: String? = findBackCameraWithFlash()

    private fun findBackCameraWithFlash(): String? = try {
        for (id in cm.cameraIdList) {
            val chars = cm.getCameraCharacteristics(id)
            val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            val facing = chars.get(CameraCharacteristics.LENS_FACING)
            if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) return id
        }
        // fallback to any camera with flash
        cm.cameraIdList.firstOrNull { id ->
            cm.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to enumerate cameras", e)
        null
    }

    fun isAvailable(): Boolean = backCameraId != null

    fun setTorch(on: Boolean): Boolean {
        var id = backCameraId
        if (id == null) {
            // rare OEM quirk: try resolving once more
            backCameraId = findBackCameraWithFlash()
            id = backCameraId ?: return false
        }
        return try {
            cm.setTorchMode(id, on)
            true
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Camera access error", e)
            false
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing CAMERA permission", e)
            false
        } catch (e: IllegalArgumentException) {
            // some devices throw if torch not supported on that ID
            Log.e(TAG, "Torch not supported on cameraId=$id", e)
            false
        }
    }
}
