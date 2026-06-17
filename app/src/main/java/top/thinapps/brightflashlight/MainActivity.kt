package top.thinapps.brightflashlight

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import java.util.Locale

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
  private var autoOffControlsLocked = false
  private var lastBrightnessHapticValue = -1
  private var lastStrobeHapticValue = -1
  private val countdownHandler = Handler(Looper.getMainLooper())
  private var autoOffEndsAtMs = 0L

  private val countdownRunnable = object : Runnable {
    override fun run() {
      if (!isAnyLightActive() || selectedAutoOffMinutes <= 0 || autoOffEndsAtMs <= 0L) {
        stopAutoOffCountdown()
        return
      }

      val remainingMs = autoOffEndsAtMs - System.currentTimeMillis()
      if (remainingMs <= 0L) {
        stopAllModes()
        setPowerLabel(off = true)
        syncUiEnabledState(hasCameraPermission())
        return
      }

      updateAutoOffCountdown()
      countdownHandler.postDelayed(this, 1000L)
    }
  }

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
    sliderBrightness = binding.root.findViewById(R.id.sliderBrightness)
    binding.sliderStrobe.setLabelFormatter { value -> (value.toInt() + 1).toString() }
    binding.sliderStrobePreview.setLabelFormatter { value -> (value.toInt() + 1).toString() }
    binding.sliderStrobePreview.isEnabled = false
    binding.sliderStrobePreview.isClickable = false
    binding.sliderStrobePreview.isFocusable = false
    syncStrobePreview()
    updateBrightnessValueLabel()
    setupAutoOffLockedTouchGuards()

    binding.btnToggle.setOnClickListener(::onPowerClicked)
    binding.btnToggle.setOnTouchListener { view, event ->
      if (event.actionMasked == MotionEvent.ACTION_DOWN && view.isEnabled) {
        performTapHaptic(view)
      }
      false
    }
    binding.btnAccessNotice.setOnClickListener { view ->
      performTapHaptic(view)
      requestCameraPermission()
    }
    binding.btnScreenLight.setOnClickListener { view ->
      performTapHaptic(view)
      startActivity(Intent(this, ScreenLightActivity::class.java))
    }
    binding.btnStrobeWarning.setOnClickListener { view ->
      performTapHaptic(view)
      showStrobeWarningDialog()
    }
    binding.btnStrobeWarningPreview.setOnClickListener { view ->
      performTapHaptic(view)
      showStrobeWarningDialog()
    }
    binding.groupMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
      if (!isChecked) return@addOnButtonCheckedListener
      if (!restoringPreferences) performTapHapticForId(checkedId)
      selectedMode = when (checkedId) {
        R.id.btnModeStrobe -> Mode.STROBE
        R.id.btnModeSos -> Mode.SOS
        else -> Mode.TORCH
      }
      stopAllModes()
      setPowerLabel(off = true)
      refreshTorchUi()
      syncUiEnabledState(hasCameraPermission())
      if (!restoringPreferences) saveModePreference()
    }
    binding.groupAutoOff.addOnButtonCheckedListener { _, checkedId, isChecked ->
      if (!isChecked) {
        enforceAutoOffSelection()
        return@addOnButtonCheckedListener
      }
      if (autoOffControlsLocked || (isAnyLightActive() && !restoringPreferences)) {
        enforceAutoOffSelection()
        return@addOnButtonCheckedListener
      }
      if (!restoringPreferences) performTapHapticForId(checkedId)
      selectedAutoOffMinutes = minutesForAutoOffButtonId(checkedId)
      enforceAutoOffSelection()
      if (!restoringPreferences) saveAutoOffPreference()
    }
    binding.sliderStrobe.addOnChangeListener { slider, value, fromUser ->
      val sliderValue = value.toInt()
      syncStrobePreview(sliderValue)
      if (fromUser && sliderValue != lastStrobeHapticValue) {
        performTapHaptic(slider)
        lastStrobeHapticValue = sliderValue
      }
      val speedHz = StrobeSpeedPreset.hzForSliderValue(sliderValue)
      updateStrobeSpeedLabel(speedHz)
      if (fromUser && !restoringPreferences) saveStrobeSpeedPreference(speedHz)
      if (fromUser && strobeRunning) sendToService(ACTION_STROBE_UPDATE, strobeSpeed = speedHz)
    }
    sliderBrightness?.addOnChangeListener { slider, value, fromUser ->
      val sliderValue = value.toInt()
      if (fromUser && sliderValue != lastBrightnessHapticValue) {
        performTapHaptic(slider)
        lastBrightnessHapticValue = sliderValue
      }
      updateBrightnessValueLabel(sliderValue)
      if (fromUser && torchOn && selectedMode == Mode.TORCH && strengthSupported) {
        sendToService(ACTION_TORCH_UPDATE_INTENSITY, torchIntensity = sliderValue)
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

  override fun onResume() {
    super.onResume()
    if (::binding.isInitialized && isAnyLightActive() && !TorchService.isActive(this)) {
      clearRunningState()
    } else if (::binding.isInitialized) {
      syncUiEnabledState(hasCameraPermission())
      updateAutoOffCountdown()
    }
  }

  override fun onDestroy() {
    countdownHandler.removeCallbacks(countdownRunnable)
    super.onDestroy()
  }

  private fun performTapHaptic(view: View) {
    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
  }

  private fun performTapHapticForId(viewId: Int) {
    binding.root.findViewById<View>(viewId)?.let { performTapHaptic(it) }
  }

  private fun syncStrobePreview(sliderValue: Int = binding.sliderStrobe.value.toInt()) {
    binding.sliderStrobePreview.value = sliderValue.coerceIn(0, 4).toFloat()
  }

  private fun setupAutoOffLockedTouchGuards() {
    val consumeWhenLocked = View.OnTouchListener { _, event ->
      if (!autoOffControlsLocked) return@OnTouchListener false
      if (event.actionMasked == MotionEvent.ACTION_DOWN) enforceAutoOffSelection()
      true
    }
    binding.groupAutoOff.setOnTouchListener(consumeWhenLocked)
    for (i in 0 until binding.groupAutoOff.childCount) {
      binding.groupAutoOff.getChildAt(i).setOnTouchListener(consumeWhenLocked)
    }
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
    selectedAutoOffMinutes = normalizeAutoOffMinutes(saved.autoOffMinutes)
    binding.groupMode.check(
      when (selectedMode) {
        Mode.STROBE -> R.id.btnModeStrobe
        Mode.SOS -> R.id.btnModeSos
        Mode.TORCH -> R.id.btnModeTorch
      }
    )
    enforceAutoOffSelection()
    binding.sliderStrobe.value = StrobeSpeedPreset.sliderValueForHz(saved.strobeSpeed).toFloat()
    syncStrobePreview()
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
    binding.cardStrobe.visibility = View.VISIBLE
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
    updateBrightnessValueLabel(maxStrength)
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

  private fun showStrobeWarningDialog() {
    AlertDialog.Builder(this)
      .setCustomTitle(layoutInflater.inflate(R.layout.dialog_strobe_warning_title, null))
      .setMessage(R.string.strobe_warning_message)
      .setPositiveButton(android.R.string.ok, null)
      .show()
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
          stopAutoOffCountdown()
          syncUiEnabledState(hasCameraPermission())
        } else {
          stopAllModes()
          val intensity = (sliderBrightness?.value ?: 1f).toInt().coerceAtLeast(1)
          sendToService(ACTION_TORCH_ON, torchIntensity = intensity)
          torchOn = true
          setPowerLabel(false)
          startAutoOffCountdownIfNeeded()
          syncUiEnabledState(hasCameraPermission())
        }
      }
      Mode.STROBE -> ensurePermissionThen {
        if (strobeRunning) {
          sendToService(ACTION_STROBE_STOP)
          strobeRunning = false
          setPowerLabel(true)
          stopAutoOffCountdown()
          syncUiEnabledState(hasCameraPermission())
        } else {
          stopAllModes()
          sendToService(ACTION_STROBE_START, strobeSpeed = selectedStrobeSpeed())
          strobeRunning = true
          setPowerLabel(false)
          startAutoOffCountdownIfNeeded()
          syncUiEnabledState(hasCameraPermission())
        }
      }
      Mode.SOS -> ensurePermissionThen {
        if (sosRunning) {
          sendToService(ACTION_SOS_STOP)
          sosRunning = false
          setPowerLabel(true)
          stopAutoOffCountdown()
          syncUiEnabledState(hasCameraPermission())
        } else {
          stopAllModes()
          sendToService(ACTION_SOS_START)
          sosRunning = true
          setPowerLabel(false)
          startAutoOffCountdownIfNeeded()
          syncUiEnabledState(hasCameraPermission())
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

  private fun stopAllModes() {
    val hadActiveMode = isAnyLightActive()
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
    if (hadActiveMode) {
      stopAutoOffCountdown()
      syncUiEnabledState(hasCameraPermission())
    }
  }

  private fun clearRunningState() {
    torchOn = false
    strobeRunning = false
    sosRunning = false
    stopAutoOffCountdown()
    setPowerLabel(off = true)
    syncUiEnabledState(hasCameraPermission())
  }

  private fun isAnyLightActive(): Boolean {
    return torchOn || strobeRunning || sosRunning
  }

  private fun startAutoOffCountdownIfNeeded() {
    countdownHandler.removeCallbacks(countdownRunnable)
    if (!isAnyLightActive() || selectedAutoOffMinutes <= 0) {
      stopAutoOffCountdown()
      return
    }

    autoOffEndsAtMs = System.currentTimeMillis() + selectedAutoOffMinutes.toLong() * 60_000L
    updateAutoOffCountdown()
    countdownHandler.postDelayed(countdownRunnable, 1000L)
  }

  private fun stopAutoOffCountdown() {
    countdownHandler.removeCallbacks(countdownRunnable)
    autoOffEndsAtMs = 0L
    binding.txtAutoOffCountdown.setText(R.string.auto_off_countdown_placeholder)
    binding.txtAutoOffCountdown.visibility = View.INVISIBLE
  }

  private fun updateAutoOffCountdown() {
    if (!isAnyLightActive() || selectedAutoOffMinutes <= 0 || autoOffEndsAtMs <= 0L) {
      binding.txtAutoOffCountdown.setText(R.string.auto_off_countdown_placeholder)
      binding.txtAutoOffCountdown.visibility = View.INVISIBLE
      return
    }

    val remainingMs = (autoOffEndsAtMs - System.currentTimeMillis()).coerceAtLeast(0L)
    binding.txtAutoOffCountdown.text = formatAutoOffRemaining(remainingMs)
    binding.txtAutoOffCountdown.visibility = View.VISIBLE
  }

  private fun formatAutoOffRemaining(remainingMs: Long): String {
    val totalSeconds = ((remainingMs + 999L) / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
  }

  private fun normalizeAutoOffMinutes(minutes: Int): Int {
    return when (minutes) {
      5, 15, 30, 60 -> minutes
      else -> 0
    }
  }

  private fun minutesForAutoOffButtonId(buttonId: Int): Int {
    return when (buttonId) {
      R.id.btnAutoOff5 -> 5
      R.id.btnAutoOff15 -> 15
      R.id.btnAutoOff30 -> 30
      R.id.btnAutoOff60 -> 60
      else -> 0
    }
  }

  private fun autoOffButtonIdForMinutes(minutes: Int): Int {
    return when (normalizeAutoOffMinutes(minutes)) {
      5 -> R.id.btnAutoOff5
      15 -> R.id.btnAutoOff15
      30 -> R.id.btnAutoOff30
      60 -> R.id.btnAutoOff60
      else -> R.id.btnAutoOffOff
    }
  }

  private fun enforceAutoOffSelection() {
    selectedAutoOffMinutes = normalizeAutoOffMinutes(selectedAutoOffMinutes)
    val expectedButtonId = autoOffButtonIdForMinutes(selectedAutoOffMinutes)
    if (binding.groupAutoOff.checkedButtonId != expectedButtonId) {
      binding.groupAutoOff.check(expectedButtonId)
    }
  }

  private fun setPowerLabel(off: Boolean) {
    val labelRes = if (off) R.string.action_torch_on else R.string.action_torch_off
    binding.txtPowerState.setText(labelRes)
    binding.btnToggle.contentDescription = getString(labelRes)
    binding.btnToggle.setBackgroundResource(if (off) R.drawable.bg_power_button_off else R.drawable.bg_power_button_on)
  }

  private fun syncUiEnabledState(enabled: Boolean) {
    val torchControlsEnabled = enabled && torchAvailable
    val lightActive = isAnyLightActive()
    val autoOffLocked = torchControlsEnabled && lightActive
    val showActiveStrobe = torchControlsEnabled && selectedMode == Mode.STROBE

    binding.btnToggle.isEnabled = torchControlsEnabled
    binding.sliderStrobe.isEnabled = showActiveStrobe
    binding.sliderStrobe.isClickable = showActiveStrobe
    binding.sliderStrobePreview.isEnabled = false
    binding.sliderStrobePreview.isClickable = false
    binding.sliderStrobePreview.isFocusable = false
    sliderBrightness?.isEnabled = torchControlsEnabled && strengthSupported && selectedMode == Mode.TORCH
    setEnabledRecursive(binding.groupMode, torchControlsEnabled)
    setAutoOffControlsEnabled(torchControlsEnabled, autoOffLocked)
    binding.cardStrobe.visibility = if (showActiveStrobe) View.VISIBLE else View.INVISIBLE
    binding.cardStrobe.isEnabled = showActiveStrobe
    binding.cardStrobe.isClickable = showActiveStrobe
    binding.cardStrobe.alpha = 1f
    binding.cardAutoOff.alpha = if (autoOffLocked) 0.45f else 1f
    binding.btnScreenLight.isEnabled = true
    updateBrightnessValueLabel()
    if (!torchControlsEnabled) {
      stopAutoOffCountdown()
      binding.txtPowerState.setText(R.string.action_torch_on)
      binding.btnToggle.contentDescription = getString(R.string.action_torch_on)
      binding.btnToggle.setBackgroundResource(R.drawable.bg_power_button_disabled)
    } else {
      setPowerLabel(off = !lightActive)
    }
  }

  private fun updateBrightnessValueLabel(value: Int = (sliderBrightness?.value ?: maxStrength.toFloat()).toInt()) {
    val showActiveBrightness = torchAvailable &&
      strengthSupported &&
      selectedMode == Mode.TORCH &&
      binding.cardBrightness.visibility == View.VISIBLE &&
      maxStrength > 1

    binding.txtBrightnessValue.text = if (showActiveBrightness) {
      brightnessLevelLabel(value, maxStrength)
    } else {
      getString(R.string.brightness_level_max)
    }
    binding.txtBrightnessPreviewValue.setText(R.string.brightness_level_max)
  }

  private fun brightnessLevelLabel(value: Int, max: Int): String {
    val normalizedMax = max.coerceAtLeast(1)
    val normalizedValue = value.coerceIn(1, normalizedMax)
    if (normalizedMax <= 1 || normalizedValue >= normalizedMax) return getString(R.string.brightness_level_max)

    return when (normalizedMax) {
      2 -> getString(R.string.brightness_level_low)
      3 -> if (normalizedValue == 1) {
        getString(R.string.brightness_level_low)
      } else {
        getString(R.string.brightness_level_medium)
      }
      else -> {
        val ratio = normalizedValue.toFloat() / normalizedMax.toFloat()
        when {
          ratio < 0.34f -> getString(R.string.brightness_level_low)
          ratio < 0.67f -> getString(R.string.brightness_level_medium)
          else -> getString(R.string.brightness_level_high)
        }
      }
    }
  }

  private fun setAutoOffControlsEnabled(torchControlsEnabled: Boolean, autoOffLocked: Boolean) {
    autoOffControlsLocked = autoOffLocked
    enforceAutoOffSelection()
    binding.groupAutoOff.isEnabled = torchControlsEnabled && !autoOffLocked
    for (i in 0 until binding.groupAutoOff.childCount) {
      binding.groupAutoOff.getChildAt(i).isEnabled = torchControlsEnabled
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
    val displayName = strobeSpeedDisplayName(speed)
    binding.txtStrobeSpeedValue.text = displayName
    binding.txtStrobeSpeedPreviewValue.text = displayName
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
