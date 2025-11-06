package top.thinapps.brightflashlight.torch

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log

class TorchController(context: Context) {

    private val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val backCameraId: String? = findBackCameraWithFlash()

    private fun findBackCameraWithFlash(): String? {
        return try {
            for (id in cm.cameraIdList) {
                val chars = cm.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) return id
            }
            // fallback to any with flash
            cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }
    }

    fun isAvailable(): Boolean = backCameraId != null

    fun setTorch(on: Boolean): Boolean {
        val id = backCameraId ?: return false
        return try {
            cm.setTorchMode(id, on)
            true
        } catch (e: CameraAccessException) {
            Log.e("TorchController", "Camera access error", e)
            false
        } catch (e: SecurityException) {
            Log.e("TorchController", "No permission", e)
            false
        }
    }
}
