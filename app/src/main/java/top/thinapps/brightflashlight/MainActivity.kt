package top.thinapps.brightflashlight

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import top.thinapps.brightflashlight.torch.TorchService
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_SOS_START
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_SOS_STOP
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_STROBE_START
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_STROBE_STOP
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_STROBE_UPDATE
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_TORCH_OFF
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_TORCH_ON
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_TORCH_UPDATE_INTENSITY // added
import top.thinapps.brightflashlight.ui.ScreenLightActivity

class MainActivity : ComponentActivity() {

    private enum class Mode { TORCH, STROBE, SOS }

    private lateinit var btnToggle: Button
    private lateinit var btnScreenLight: Button
    private lateinit var sliderStrobe: Slider
    private lateinit var sliderBrightness: Slider // added
    private lateinit var groupMode: MaterialButtonToggleGroup

    private var selectedMode = Mode.TORCH
    private var torchOn = false
    private var strobeRunning = false
    private var sosRunning = false

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onPowerClicked(btnToggle)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnToggle = findViewById(R.id.btnToggle)
        btnScreenLight = findViewById(R.id.btnScreenLight)
        sliderStrobe = findViewById(R.id.sliderStrobe)
        sliderBrightness = findViewById(R.id.sliderBrightness) // added
        groupMode = findViewById(R.id.groupMode)

        btnToggle.setOnClickListener(::onPowerClicked)
        btnScreenLight.setOnClickListener {
            startActivity(Intent(this, ScreenLightActivity::class.java))
        }

        groupMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedMode = when (checkedId) {
                R.id.btnModeStrobe -> Mode.STROBE
                R.id.btnModeSos -> Mode.SOS
                else -> Mode.TORCH
            }
            stopAllModes()
            setPowerLabel(off = true)
        }

        sliderStrobe.addOnChangeListener { _, value, fromUser ->
            if (fromUser && strobeRunning) {
                sendToService(ACTION_STROBE_UPDATE, strobeSpeed = value.toInt())
            }
        }

        // added: brightness live updates while torch is on (TORCH mode)
        sliderBrightness.addOnChangeListener { _, value, fromUser ->
            if (fromUser && torchOn && selectedMode == Mode.TORCH) {
                sendToService(ACTION_TORCH_UPDATE_INTENSITY, torchIntensity = value.toInt())
            }
        }
    }

    private fun ensurePermissionThen(block: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) block() else permLauncher.launch(Manifest.permission.CAMERA)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onPowerClicked(v: View) {
        when (selectedMode) {
            Mode.TORCH -> ensurePermissionThen {
                if (torchOn) {
                    sendToService(ACTION_TORCH_OFF)
                    torchOn = false
                    setPowerLabel(true)
                } else {
                    stopAllModes()
                    val intensity = sliderBrightness.value.toInt() // added
                    sendToService(ACTION_TORCH_ON, torchIntensity = intensity) // added
                    torchOn = true
                    setPowerLabel(false)
                }
            }
            Mode.STROBE -> ensurePermissionThen {
                if (strobeRunning) {
                    sendToService(ACTION_STROBE_STOP)
                    strobeRunning = false
                    setPowerLabel(true)
                } else {
                    stopAllModes()
                    val speed = sliderStrobe.value.toInt()
                    sendToService(ACTION_STROBE_START, strobeSpeed = speed)
                    strobeRunning = true
                    setPowerLabel(false)
                }
            }
            Mode.SOS -> ensurePermissionThen {
                if (sosRunning) {
                    sendToService(ACTION_SOS_STOP)
                    sosRunning = false
                    setPowerLabel(true)
                } else {
                    stopAllModes()
                    sendToService(ACTION_SOS_START)
                    sosRunning = true
                    setPowerLabel(false)
                }
            }
        }
    }

    private fun stopAllModes() {
        if (torchOn) {
            sendToService(ACTION_TORCH_OFF)
            torchOn = false
        }
        if (strobeRunning) {
            sendToService(ACTION_STROBE_STOP)
            strobeRunning = false
        }
        if (sosRunning) {
            sendToService(ACTION_SOS_STOP)
            sosRunning = false
        }
    }

    private fun setPowerLabel(off: Boolean) {
        btnToggle.setText(if (off) R.string.action_torch_on else R.string.action_torch_off)
    }

    private fun sendToService(
        action: String?,
        strobeSpeed: Int? = null,
        torchIntensity: Int? = null // added
    ) {
        val i = Intent(this, TorchService::class.java)
        if (action != null) i.action = action
        strobeSpeed?.let { i.putExtra("strobeSpeed", it) }
        torchIntensity?.let { i.putExtra(TorchService.EXTRA_TORCH_INTENSITY, it) } // added
        ContextCompat.startForegroundService(this, i)
    }
}
