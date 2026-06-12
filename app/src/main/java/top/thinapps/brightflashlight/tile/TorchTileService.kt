package top.thinapps.brightflashlight.tile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import top.thinapps.brightflashlight.torch.TorchService

class TorchTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        syncTileState()
    }

    override fun onClick() {
        super.onClick()
        if (!hasCameraPermission()) {
            syncTileState()
            return
        }

        val currentlyActive = TorchService.isActive(this)
        val action = if (currentlyActive) TorchService.ACTION_TORCH_OFF else TorchService.ACTION_TORCH_ON
        val i = Intent(this, TorchService::class.java).setAction(action)
        ContextCompat.startForegroundService(this, i)

        qsTile?.state = if (currentlyActive) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        qsTile?.updateTile()
    }

    private fun syncTileState() {
        val tile = qsTile ?: return
        tile.state = when {
            !hasCameraPermission() -> Tile.STATE_UNAVAILABLE
            TorchService.isActive(this) -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }
}
