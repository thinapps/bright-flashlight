package top.thinapps.brightflashlight.tile

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import top.thinapps.brightflashlight.torch.TorchController
import top.thinapps.brightflashlight.torch.TorchService

class TorchTileService : TileService() {

    // simple controller for torch hardware
    private lateinit var controller: TorchController

    // local toggle state for the tile ui
    private var on = false

    override fun onStartListening() {
        super.onStartListening()
        controller = TorchController(this)
        qsTile?.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }

    override fun onClick() {
        super.onClick()
        on = !on
        val act = if (on) TorchService.ACTION_TORCH_ON else TorchService.ACTION_TORCH_OFF
        val i = Intent(this, TorchService::class.java).setAction(act)
        ContextCompat.startForegroundService(this, i)
        qsTile?.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }
}
