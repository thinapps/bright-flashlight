package top.thinapps.brightflashlight.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import top.thinapps.brightflashlight.torch.TorchController
import top.thinapps.brightflashlight.torch.TorchService
import android.content.Intent

class TorchTileService : TileService() {

    private lateinit var controller: TorchController
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
        startForegroundService(i)
        qsTile?.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }
}
