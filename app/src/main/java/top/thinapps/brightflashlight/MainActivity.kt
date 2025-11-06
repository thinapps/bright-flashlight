package top.thinapps.brightflashlight.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.slider.Slider
import top.thinapps.brightflashlight.R
import top.thinapps.brightflashlight.torch.TorchService
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_SOS_START
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_SOS_STOP
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_STROBE_START
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_STROBE_STOP
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_TORCH_OFF
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_TORCH_ON

class MainActivity : ComponentActivity() {

    private lateinit var btnToggle: Button
    private lateinit var btnStrobeStart: Button
    private lateinit var btnStrobeStop: Button
    private lateinit var btnSosStart: Button
    private lateinit var btnSosStop: Button
    private lateinit var btnScreenLight: Button
    private lateinit var sliderStrobe: Slider
    private lateinit var sliderAutoOff: Slider
    private lateinit var tvTimer: TextView

    private var torchOn = false

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) toggleTorch()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnToggle = findViewById(R.id.btnToggle)
        btnStrobeStart = findViewById(R.id.btnStrobeStart)
        btnStrobeStop = findViewById(R.id.btnStrobeStop)
        btnSosStart = findViewById(R.id.btnSosStart)
        btnSosStop = findViewById(R.id.btnSosStop)
        btnScreenLight = findViewById(R.id.btnScreenLight)
        sliderStrobe = findViewById(R.id.sliderStrobe)
        sliderAutoOff = findViewById(R.id.sliderAutoOff)
        tvTimer = findViewById(R.id.tvTimer)

        btnToggle.setOnClickListener { ensurePermissionThen { toggleTorch() } }
        btnStrobeStart.setOnClickListener { ensurePermissionThen { startStrobe() } }
        btnStrobeStop.setOnClickListener { sendToService(ACTION_STROBE_STOP) }
        btnSosStart.setOnClickListener { ensurePermissionThen { sendToService(ACTION_SOS_START) } }
        btnSosStop.setOnClickListener { sendToService(ACTION_SOS_STOP) }
        btnScreenLight.setOnClickListener {
            startActivity(Intent(this, ScreenLightActivity::class.java))
        }

        sliderAutoOff.addOnChangeListener { _, value, _ ->
            val minutes = value.toInt()
            tvTimer.text = getString(R.string.timer_label) + ": " + minutes
            // pass timer to service as an extra for auto-stop
            sendToService(null, autoOffMinutes = minutes)
        }
    }

    private fun ensurePermissionThen(block: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) block() else permLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun toggleTorch() {
        torchOn = !torchOn
        sendToService(if (torchOn) ACTION_TORCH_ON else ACTION_TORCH_OFF)
        btnToggle.setText(if (torchOn) R.string.action_torch_off else R.string.action_torch_on)
    }

    private fun startStrobe() {
        val speed = sliderStrobe.value.toInt() // 5..20 (higher = faster)
        sendToService(ACTION_STROBE_START, strobeSpeed = speed)
    }

    private fun sendToService(
        action: String?,
        strobeSpeed: Int? = null,
        autoOffMinutes: Int? = null
    ) {
        val i = Intent(this, TorchService::class.java)
        if (action != null) i.action = action
        strobeSpeed?.let { i.putExtra("strobeSpeed", it) }
        autoOffMinutes?.let { i.putExtra("autoOffMinutes", it) }
        ContextCompat.startForegroundService(this, i)
    }
}
