package top.thinapps.brightflashlight

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.slider.Slider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import top.thinapps.brightflashlight.databinding.ActivityMainBinding
import top.thinapps.brightflashlight.prefs.AppPreferences
import top.thinapps.brightflashlight.prefs.SavedPreferences
import top.thinapps.brightflashlight.torch.StrobeSpeedPreset
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
import top.thinapps.brightflashlight.torch.TorchService.Companion.EXTRA_AUTO_OFF_MINUTES
import top.thinapps.brightflashlight.torch.TorchService.Companion.EXTRA_STROBE_SPEED
import top.thinapps.brightflashlight.ui.ScreenLightActivity

class MainActivity : ComponentActivity() {
  private enum class Mode { TORCH, STROBE, SOS }
  private lateinit var binding: ActivityMainBinding
  private lateinit var appPreferences: AppPreferences
  private var sliderBrightness: Slider? = null
  private var selectedMode = Mode.TORCH
  private var selectedAutoOffMinutes = 0
  private var torchOn = false
  private var strobeRunning = false
  private var sosRunning = false
  private var restoringPreferences = false
  private var torch: TorchController? = null
  private var torchAvailable = false
  private var strengthSupported = false
  private var maxStrength = 1

  private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    if (granted) {
      showAccessNotice(false)
      ensureTorch()
      refreshTorchUi()
      syncUiEnabledState(true)
    } else {
      showAccessNotice(true)
      syncUiEnabledState(false)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    appPreferences = AppPreferences(applicationContext)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    applyStandardGutter()
    sliderBrightness = binding.root.findViewById(R.id.sliderBrightness)

    binding.btnToggle.setOnClickListener(::onPowerClicked)
    binding.btnToggle.setOnTouchListener { view, event ->
      if (event.actionMasked == MotionEvent.ACTION_DOWN && view.isEnabled) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
      }
      false
    }
    binding.btnAccessNotice.setOnClickListener { requestCameraPermission() }
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
      refreshTorchUi()
      if (!restoringPreferences) saveModePreference()
    }
    binding.groupAutoOff.addOnButtonCheckedListener { _, checkedId, isChecked ->
      if (!isChecked) return@addOnButtonCheckedListener
      selectedAutoOffMinutes = when (checkedId) {
        R.id.btnAutoOff5 -> 5
        R.id.btnAutoOff15 -> 15
        R.id.btnAutoOff30 -> 30
        R.id.btnAutoOff60 -> 60
        else -> 0
      }
      if (!restoringPreferences) {
        saveAutoOffPreference()
        updateAutoOffIfRunning()
      }
    }
    binding.sliderStrobe.addOnChangeListener { _, value, fromUser ->
      val speedHz = StrobeSpeedPreset.hzForSliderValue(value.toInt())
      updateStrobeSpeedLabel(speedHz)
      if (fromUser && !restoringPreferences) saveStrobeSpeedPreference(speedHz)
      if (fromUser && strobeRunning) sendToService(ACTION_STROBE_UPDATE, strobeSpeed = speedHz)
    }
    sliderBrightness?.addOnChangeListener { _, value, fromUser ->
      if (fromUser && torchOn && selectedMode == Mode.TORCH && strengthSupported) {
        sendToService(ACTION_TORCH_UPDATE_INTENSITY, torchIntensity = value.toInt())
      }
    }

    updateStrobeSpeedLabel()
    restorePreferences()
    val hasCam = hasCameraPermission()
    if (hasCam) {
      showAccessNotice(false)
      ensureTorch()
      refreshTorchUi()
    } else {
      showAccessNotice(true)
      requestCameraPermission()
    }
    syncUiEnabledState(hasCam)
  }

  private fun applyStandardGutter() {
    val scroll = binding.root.getChildAt(0) as? ViewGroup ?: return
    val content = scroll.getChildAt(0) ?: return
    content.setPaddingRelative(dp(24), content.paddingTop, dp(24), dp(20))
    clearNestedHorizontalPadding(content)
  }

  private fun clearNestedHorizontalPadding(view: View) {
    val oldInner = dp(16)
    if (view.paddingStart == oldInner && view.paddingEnd == oldInner) {
      view.setPaddingRelative(0, view.paddingTop, 0, view.paddingBottom)
    }
    if (view is ViewGroup) {
      for (i in 0 until view.childCount) clearNestedHorizontalPadding(view.getChildAt(i))
    }
  }

  private fun dp(value: Int): Int {
    return (value * resources.displayMetrics.density + 0.5f).toInt()
  }

  private fun restorePreferences() {
    lifecycleScope.launch {
      val saved = appPreferences.preferences.first()
      restoringPreferences = true
      applySavedPreferences(saved)
      restoringPreferences = false
      if (hasCameraPermission()) {
        showAccessNotice(false)
        ensureTorch()
        refreshTorchUi()
        syncUiEnabledState(true)
      } else {
        showAccessNotice(true)
        syncUiEnabledState(false)
      }
    }
  }

  private fun applySavedPreferences(saved: SavedPreferences) {
    selectedMode = when (saved.lastMode) {
      Mode.STROBE.name -> Mode.STROBE
      Mode.SOS.name -> Mode.SOS
      else -> Mode.TORCH
    }
    selectedAutoOffMinutes = saved.autoOffMinutes
    binding.groupMode.check(
      when (selectedMode) {
        Mode.STROBE -> R.id.btnModeStrobe
        Mode.SOS -> R.id.btnModeSos
        Mode.TORCH -> R.id.btnModeTorch
      }
    )
    binding.groupAutoOff.check(
      when (selectedAutoOffMinutes) {
        5 -> R.id.btnAutoOff5
        15 -> R.id.btnAutoOff15
        30 -> R.id.btnAutoOff30
        60 -> R.id.btnAutoOff60
        else -> R.id.btnAutoOffOff
      }
    )
    binding.sliderStrobe.value = StrobeSpeedPreset.sliderValueForHz(saved.strobeSpeed).toFloat()
    updateStrobeSpeedLabel(saved.strobeSpeed)
  }

  private fun ensureTorch() {
    val controller = torch ?: TorchController(applicationContext).also { torch = it }
    torchAvailable = controller.isAvailable()
    val (supported, max) = if (torchAvailable) controller.getStrengthSupport() else false to 1
    strengthSupported = supported
    maxStrength = max.coerceAtLeast(1)
  }

  private fun refreshTorchUi() {
    if (!hasCameraPermission()) {
      showAccessNotice(true)
      binding.txtNoFlash.visibility = View.GONE
      binding.cardBrightness.visibility = View.GONE
      binding.cardStrobe.visibility = View.GONE
      binding.cardAutoOff.visibility = View.GONE
      return
    }
    showAccessNotice(false)
    if (!torchAvailable) {
      binding.txtNoFlash.visibility = View.VISIBLE
      binding.cardBrightness.visibility = View.GONE
      binding.cardStrobe.visibility = View.GONE
      binding.cardAutoOff.visibility = View.GONE
      return
    }
    binding.txtNoFlash.visibility = View.GONE
    binding.cardAutoOff.visibility = View.VISIBLE
    binding.cardStrobe.visibility = if (selectedMode == Mode.STROBE) View.VISIBLE else View.GONE
    setupBrightnessUi()
  }

  private fun setupBrightnessUi() {
    val sb = sliderBrightness ?: return
    if (selectedMode != Mode.TORCH || !strengthSupported || maxStrength <= 1) {
      binding.cardBrightness.visibility = View.GONE
      return
    }
    binding.cardBrightness.visibility = View.VISIBLE
    sb.isEnabled = true
    sb.valueFrom = 1f
    sb.value = 1f
    sb.valueTo = maxStrength.toFloat()
    sb.stepSize = 1f
    sb.value = maxStrength.toFloat()
  }

  private fun hasCameraPermission(): Boolean {
    return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
  }

  private fun requestCameraPermission() {
    permLauncher.launch(Manifest.permission.CAMERA)
  }

  private fun showAccessNotice(show: Boolean) {
    binding.layoutAccessNotice.visibility = if (show) View.VISIBLE else View.GONE
  }

  @Suppress("UNUSED_PARAMETER")
  fun onPowerClicked(v: View) {
    if (!torchAvailable) return
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
          sendToService(ACTION_STROBE_START, strobeSpeed = selectedStrobeSpeed())
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
    if (hasCameraPermission()) block() else {
      showAccessNotice(true)
      requestCameraPermission()
    }
  }

  private fun updateAutoOffIfRunning() {
    when {
      torchOn -> {
        val intensity = (sliderBrightness?.value ?: 1f).toInt().coerceAtLeast(1)
        sendToService(ACTION_TORCH_ON, torchIntensity = intensity)
      }
      strobeRunning -> sendToService(ACTION_STROBE_START, strobeSpeed = selectedStrobeSpeed())
      sosRunning -> sendToService(ACTION_SOS_START)
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
    binding.btnToggle.setBackgroundResource(if (off) R.drawable.bg_power_button_off else R.drawable.bg_power_button_on)
  }

  private fun syncUiEnabledState(enabled: Boolean) {
    val torchControlsEnabled = enabled && torchAvailable
    binding.btnToggle.isEnabled = torchControlsEnabled
    binding.sliderStrobe.isEnabled = torchControlsEnabled
    sliderBrightness?.isEnabled = torchControlsEnabled && strengthSupported && selectedMode == Mode.TORCH
    setEnabledRecursive(binding.groupMode, torchControlsEnabled)
    setEnabledRecursive(binding.groupAutoOff, torchControlsEnabled)
    binding.btnScreenLight.isEnabled = true
    if (!torchControlsEnabled) {
      binding.btnToggle.setText(R.string.action_torch_on)
      binding.btnToggle.setBackgroundResource(R.drawable.bg_power_button_disabled)
    } else {
      setPowerLabel(off = !(torchOn || strobeRunning || sosRunning))
    }
  }

  private fun setEnabledRecursive(view: View, enabled: Boolean) {
    view.isEnabled = enabled
    if (view is ViewGroup) for (i in 0 until view.childCount) setEnabledRecursive(view.getChildAt(i), enabled)
  }

  private fun selectedStrobeSpeed(): Int {
    return StrobeSpeedPreset.hzForSliderValue(binding.sliderStrobe.value.toInt())
  }

  private fun updateStrobeSpeedLabel(speed: Int = selectedStrobeSpeed()) {
    binding.txtStrobeSpeed.text = getString(R.string.strobe_speed_value, strobeSpeedDisplayName(speed))
  }

  private fun strobeSpeedDisplayName(speed: Int): String {
    val label = when {
      speed <= 1 -> getString(R.string.strobe_speed_slow)
      speed == 2 -> getString(R.string.strobe_speed_medium)
      speed == 3 -> getString(R.string.strobe_speed_alert)
      speed == 4 -> getString(R.string.strobe_speed_fast)
      else -> getString(R.string.strobe_speed_max)
    }
    return "$label (${StrobeSpeedPreset.normalizeHz(speed)} Hz)"
  }

  private fun saveModePreference() {
    lifecycleScope.launch { appPreferences.saveMode(selectedMode.name) }
  }

  private fun saveAutoOffPreference() {
    lifecycleScope.launch { appPreferences.saveAutoOffMinutes(selectedAutoOffMinutes) }
  }

  private fun saveStrobeSpeedPreference(speed: Int) {
    lifecycleScope.launch { appPreferences.saveStrobeSpeed(speed) }
  }

  private fun sendToService(action: String?, strobeSpeed: Int? = null, torchIntensity: Int? = null) {
    val i = Intent(this, TorchService::class.java)
    if (action != null) i.action = action
    i.putExtra(EXTRA_AUTO_OFF_MINUTES, selectedAutoOffMinutes)
    strobeSpeed?.let { i.putExtra(EXTRA_STROBE_SPEED, it) }
    torchIntensity?.let { i.putExtra(TorchService.EXTRA_TORCH_INTENSITY, it) }
    ContextCompat.startForegroundService(this, i)
  }
}
