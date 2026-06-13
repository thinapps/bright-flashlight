package top.thinapps.brightflashlight.tile

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import top.thinapps.brightflashlight.MainActivity
import top.thinapps.brightflashlight.torch.TorchService

class TorchTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        syncTileState()
    }

    override fun onClick() {
        super.onClick()
        if (!hasCameraPermission()) {
            openMainActivity()
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
            TorchService.isActive(this) -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }
}
