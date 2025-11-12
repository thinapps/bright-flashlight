package top.thinapps.brightflashlight

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.slider.Slider
import top.thinapps.brightflashlight.databinding.ActivityMainBinding
import top.thinapps.brightflashlight.torch.TorchController
import top.thinapps.brightflashlight.torch.TorchService
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_SOS_START
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_SOS_STOP
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_STROBE_START
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_STROBE_STOP
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_STROBE_UPDATE
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_TORCH_OFF
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_TORCH_ON
import top.thinapps.brightflashlight.torch.TorchService.Companion.ACTION_TORCH_UPDATE_INTENSITY
import top.thinapps.brightflashlight.ui.ScreenLightActivity

class MainActivity : ComponentActivity() {

    private enum class Mode { TORCH, STROBE, SOS }

    private lateinit var binding: ActivityMainBinding

    private var sliderBrightness: Slider? = null

    private var selectedMode = Mode.TORCH
    private var torchOn = false
    private var strobeRunning = false
    private var sosRunning = false

    private var torch: TorchController? = null
    private var strengthSupported = false
    private var maxStrength = 1

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        syncUiEnabledState(granted)
        if (granted) {
            ensureTorch()
            setupBrightnessUi()
            if (selectedMode == Mode.TORCH) onPowerClicked(binding.btnToggle)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sliderBrightness = binding.root.findViewById(R.id.sliderBrightness)

        binding.btnToggle.setOnClickListener(::onPowerClicked)

        binding.btnScreenLight.setOnClickListener {
            startActivity(Intent(this, ScreenLightActivity::class.java))
        }

        binding.groupMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedMode = when (checkedId) {
                R.id.btnModeStrobe -> Mode.STROBE
                R.id.btnModeSos -> Mode.SOS
                else -> Mode.TORCH
            }
            stopAllModes()
            setPowerLabel(off = true)
        }

        binding.sliderStrobe.addOnChangeListener { _, value, fromUser ->
            if (fromUser && strobeRunning) {
                sendToService(ACTION_STROBE_UPDATE, strobeSpeed = value.toInt())
            }
        }

        sliderBrightness?.addOnChangeListener { _, value, fromUser ->
            if (fromUser && torchOn && selectedMode == Mode.TORCH && strengthSupported) {
                sendToService(ACTION_TORCH_UPDATE_INTENSITY, torchIntensity = value.toInt())
            }
        }

        val hasCam = hasCameraPermission()
        syncUiEnabledState(hasCam)
        if (hasCam) {
            ensureTorch()
            setupBrightnessUi()
        } else {
            requestCameraPermission()
        }
    }

    private fun ensureTorch() {
        if (torch == null) torch = TorchController(applicationContext)
        val (supported, max) = torch!!.getStrengthSupport()
        strengthSupported = supported
        maxStrength = max.coerceAtLeast(1)
    }

    private fun setupBrightnessUi() {
        val sb = sliderBrightness ?: return
        if (!strengthSupported) {
            sb.isEnabled = false
            sb.valueFrom = 1f
            sb.valueTo = 1f
            sb.stepSize = 1f
            sb.value = 1f
        } else {
            sb.isEnabled = true
            sb.valueFrom = 1f
            sb.valueTo = maxStrength.toFloat()
            sb.stepSize = 1f
            sb.value = maxStrength.toFloat()
        }
    }

    private fun hasCameraPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permLauncher.launch(Manifest.permission.CAMERA)
        }
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
                    val intensity = (sliderBrightness?.value ?: 1f).toInt().coerceAtLeast(1)
                    sendToService(ACTION_TORCH_ON, torchIntensity = intensity)
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
                    val speed = binding.sliderStrobe.value.toInt()
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

    private fun ensurePermissionThen(block: () -> Unit) {
        if (hasCameraPermission()) {
            block()
        } else {
            requestCameraPermission()
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
        binding.btnToggle.setText(if (off) R.string.action_torch_on else R.string.action_torch_off)
    }

    private fun syncUiEnabledState(enabled: Boolean) {
        binding.btnToggle.isEnabled = enabled
        binding.sliderStrobe.isEnabled = enabled
        sliderBrightness?.isEnabled = enabled && strengthSupported
        binding.groupMode.isEnabled = enabled
        binding.btnScreenLight.isEnabled = true
    }

    private fun sendToService(
        action: String?,
        strobeSpeed: Int? = null,
        torchIntensity: Int? = null
    ) {
        val i = Intent(this, TorchService::class.java)
        if (action != null) i.action = action
        strobeSpeed?.let { i.putExtra("strobeSpeed", it) }
        torchIntensity?.let { i.putExtra(TorchService.EXTRA_TORCH_INTENSITY, it) }
        ContextCompat.startForegroundService(this, i)
    }
}
