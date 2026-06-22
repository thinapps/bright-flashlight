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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
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
import top.thinapps.brightflashlight.torch.TorchService.Companion.EXTRA_TORCH_INTENSITY
import top.thinapps.brightflashlight.ui.AutoOffPreset
import top.thinapps.brightflashlight.ui.ScreenLightActivity
import java.util.Locale

class MainActivity : ComponentActivity() {
  private enum class Mode { TORCH, STROBE, SOS }

  private lateinit var binding: ActivityMainBinding
  private lateinit var appPreferences: AppPreferences

  private var sliderBrightness: Slider? = null
  private var selectedMode = Mode.TORCH
  private var selectedAutoOffMinutes = AutoOffPreset.OFF.minutes
  private var torchOn = false
  private var strobeRunning = false
  private var sosRunning = false
  private var restoringPreferences = false
  private var torch: TorchController? = null
  private var torchAvailable = false
  private var strengthSupported = false
  private var maxStrength = DEFAULT_TORCH_STRENGTH
  private var savedTorchStrengthLevel: Int? = null
  private var autoOffControlsLocked = false
  private var lastBrightnessHapticValue = NO_HAPTIC_VALUE
  private var lastStrobeHapticValue = NO_HAPTIC_VALUE

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
      countdownHandler.postDelayed(this, COUNTDOWN_TICK_MS)
    }
  }

  private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    handleCameraPermissionResult(granted)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    appPreferences = AppPreferences(applicationContext)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    sliderBrightness = binding.root.findViewById(R.id.sliderBrightness)
    applyWindowInsets()

    setupControls()
    updateStrobeSpeedLabel()
    restorePreferences()

    val hasCamera = hasCameraPermission()
    if (hasCamera) {
      showAccessNotice(false)
      ensureTorch()
      refreshTorchUi()
    } else {
      showAccessNotice(true)
      requestCameraPermission()
    }
    syncUiEnabledState(hasCamera)
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

  private fun applyWindowInsets() {
    val baseMainTop = binding.layoutMainContent.paddingTop
    val baseMainBottom = binding.layoutMainContent.paddingBottom
    val screenLightParams = binding.btnScreenLight.layoutParams as ViewGroup.MarginLayoutParams
    val baseScreenLightTop = screenLightParams.topMargin
    val baseScreenLightEnd = screenLightParams.marginEnd

    ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
      val bars = insets.getInsets(
        WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
      )
      val endInset = if (binding.root.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
        bars.left
      } else {
        bars.right
      }

      binding.layoutMainContent.updatePadding(
        top = baseMainTop + bars.top,
        bottom = baseMainBottom + bars.bottom
      )
      binding.btnScreenLight.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        topMargin = baseScreenLightTop + bars.top
        marginEnd = baseScreenLightEnd + endInset
      }
      insets
    }
    ViewCompat.requestApplyInsets(binding.root)
  }

  private fun setupControls() {
    setupStrobeSliders()
    setupPowerButton()
    setupAccessNotice()
    setupScreenLightButton()
    setupStrobeWarningButtons()
    setupModeControls()
    setupAutoOffControls()
    setupBrightnessSlider()

    updateBrightnessValueLabel()
    setupAutoOffLockedTouchGuards()
  }

  private fun setupStrobeSliders() {
    binding.sliderStrobe.setLabelFormatter { value -> (value.toInt() + 1).toString() }
    binding.sliderStrobePreview.setLabelFormatter { value -> (value.toInt() + 1).toString() }
    setStrobePreviewEnabled(false)
    syncStrobePreview()

    binding.sliderStrobe.addOnChangeListener { slider, value, fromUser ->
      handleStrobeSliderChange(slider, value, fromUser)
    }
  }

  private fun setupPowerButton() {
    binding.btnToggle.setOnClickListener(::onPowerClicked)
    binding.btnToggle.setOnTouchListener { view, event ->
      if (event.actionMasked == MotionEvent.ACTION_DOWN && view.isEnabled) {
        performTapHaptic(view)
      }
      false
    }
  }

  private fun setupAccessNotice() {
    binding.btnAccessNotice.setOnClickListener { view ->
      performTapHaptic(view)
      requestCameraPermission()
    }
  }

  private fun setupScreenLightButton() {
    binding.btnScreenLight.setOnClickListener { view ->
      performTapHaptic(view)
      startActivity(Intent(this, ScreenLightActivity::class.java))
    }
  }

  private fun setupStrobeWarningButtons() {
    binding.btnStrobeWarning.setOnClickListener { view ->
      performTapHaptic(view)
      showStrobeWarningDialog()
    }
    binding.btnStrobeWarningPreview.setOnClickListener { view ->
      performTapHaptic(view)
      showStrobeWarningDialog()
    }
  }

  private fun setupModeControls() {
    binding.groupMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
      if (!isChecked) return@addOnButtonCheckedListener
      handleModeSelection(checkedId)
    }
  }

  private fun setupAutoOffControls() {
    binding.groupAutoOff.addOnButtonCheckedListener { _, checkedId, isChecked ->
      handleAutoOffSelection(checkedId, isChecked)
    }
  }

  private fun setupBrightnessSlider() {
    sliderBrightness?.addOnChangeListener { slider, value, fromUser ->
      handleBrightnessSliderChange(slider, value, fromUser)
    }
  }

  private fun handleCameraPermissionResult(granted: Boolean) {
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

  private fun handleModeSelection(checkedId: Int) {
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

  private fun handleAutoOffSelection(checkedId: Int, isChecked: Boolean) {
    if (!isChecked) {
      enforceAutoOffSelection()
      return
    }

    if (autoOffControlsLocked || (isAnyLightActive() && !restoringPreferences)) {
      enforceAutoOffSelection()
      return
    }

    if (!restoringPreferences) performTapHapticForId(checkedId)
    selectedAutoOffMinutes = minutesForAutoOffButtonId(checkedId)
    enforceAutoOffSelection()
    if (!restoringPreferences) saveAutoOffPreference()
  }

  private fun handleStrobeSliderChange(slider: Slider, value: Float, fromUser: Boolean) {
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

  private fun handleBrightnessSliderChange(slider: Slider, value: Float, fromUser: Boolean) {
    val sliderValue = value.toInt()

    if (fromUser && sliderValue != lastBrightnessHapticValue) {
      performTapHaptic(slider)
      lastBrightnessHapticValue = sliderValue
    }

    updateBrightnessValueLabel(sliderValue)
    if (fromUser && !restoringPreferences && strengthSupported) {
      savedTorchStrengthLevel = sliderValue
      saveTorchStrengthPreference(sliderValue)
    }
    if (fromUser && torchOn && selectedMode == Mode.TORCH && strengthSupported) {
      sendToService(ACTION_TORCH_UPDATE_INTENSITY, torchIntensity = sliderValue)
    }
  }

  private fun performTapHaptic(view: View) {
    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
  }

  private fun performTapHapticForId(viewId: Int) {
    binding.root.findViewById<View>(viewId)?.let { performTapHaptic(it) }
  }

  private fun setStrobePreviewEnabled(enabled: Boolean) {
    binding.sliderStrobePreview.isEnabled = enabled
    binding.sliderStrobePreview.isClickable = enabled
    binding.sliderStrobePreview.isFocusable = enabled
  }

  private fun syncStrobePreview(sliderValue: Int = binding.sliderStrobe.value.toInt()) {
    binding.sliderStrobePreview.value = sliderValue.coerceIn(
      STROBE_SLIDER_MIN,
      STROBE_SLIDER_MAX
    ).toFloat()
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
      syncAfterPreferenceRestore()
    }
  }

  private fun syncAfterPreferenceRestore() {
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

  private fun applySavedPreferences(saved: SavedPreferences) {
    selectedMode = when (saved.lastMode) {
      Mode.STROBE.name -> Mode.STROBE
      Mode.SOS.name -> Mode.SOS
      else -> Mode.TORCH
    }
    selectedAutoOffMinutes = normalizeAutoOffMinutes(saved.autoOffMinutes)
    savedTorchStrengthLevel = saved.torchStrengthLevel
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
    val (supported, max) = if (torchAvailable) {
      controller.getStrengthSupport()
    } else {
      false to DEFAULT_TORCH_STRENGTH
    }
    strengthSupported = supported
    maxStrength = max.coerceAtLeast(DEFAULT_TORCH_STRENGTH)
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
    if (selectedMode != Mode.TORCH || !strengthSupported || maxStrength <= DEFAULT_TORCH_STRENGTH) {
      binding.cardBrightness.visibility = View.GONE
      return
    }

    val restoredStrength = savedTorchStrengthLevel?.coerceIn(DEFAULT_TORCH_STRENGTH, maxStrength) ?: maxStrength
    savedTorchStrengthLevel = restoredStrength

    binding.cardBrightness.visibility = View.VISIBLE
    sb.isEnabled = true
    sb.valueFrom = DEFAULT_TORCH_STRENGTH.toFloat()
    sb.value = DEFAULT_TORCH_STRENGTH.toFloat()
    sb.valueTo = maxStrength.toFloat()
    sb.stepSize = BRIGHTNESS_STEP_SIZE
    sb.value = restoredStrength.toFloat()
    updateBrightnessValueLabel(restoredStrength)
  }

  private fun hasCameraPermission(): Boolean {
    return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
      PackageManager.PERMISSION_GRANTED
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
        if (torchOn) stopTorchMode() else startTorchMode()
      }
      Mode.STROBE -> ensurePermissionThen {
        if (strobeRunning) stopStrobeMode() else startStrobeMode()
      }
      Mode.SOS -> ensurePermissionThen {
        if (sosRunning) stopSosMode() else startSosMode()
      }
    }
  }

  private fun ensurePermissionThen(block: () -> Unit) {
    if (hasCameraPermission()) {
      block()
    } else {
      showAccessNotice(true)
      requestCameraPermission()
    }
  }

  private fun startTorchMode() {
    stopAllModes()
    val intensity = (sliderBrightness?.value ?: DEFAULT_TORCH_STRENGTH.toFloat())
      .toInt()
      .coerceAtLeast(DEFAULT_TORCH_STRENGTH)
    sendToService(ACTION_TORCH_ON, torchIntensity = intensity)
    torchOn = true
    setPowerLabel(off = false)
    startAutoOffCountdownIfNeeded()
    syncUiEnabledState(hasCameraPermission())
  }

  private fun stopTorchMode() {
    sendToService(ACTION_TORCH_OFF)
    torchOn = false
    setPowerLabel(off = true)
    stopAutoOffCountdown()
    syncUiEnabledState(hasCameraPermission())
  }

  private fun startStrobeMode() {
    stopAllModes()
    sendToService(ACTION_STROBE_START, strobeSpeed = selectedStrobeSpeed())
    strobeRunning = true
    setPowerLabel(off = false)
    startAutoOffCountdownIfNeeded()
    syncUiEnabledState(hasCameraPermission())
  }

  private fun stopStrobeMode() {
    sendToService(ACTION_STROBE_STOP)
    strobeRunning = false
    setPowerLabel(off = true)
    stopAutoOffCountdown()
    syncUiEnabledState(hasCameraPermission())
  }

  private fun startSosMode() {
    stopAllModes()
    sendToService(ACTION_SOS_START)
    sosRunning = true
    setPowerLabel(off = false)
    startAutoOffCountdownIfNeeded()
    syncUiEnabledState(hasCameraPermission())
  }

  private fun stopSosMode() {
    sendToService(ACTION_SOS_STOP)
    sosRunning = false
    setPowerLabel(off = true)
    stopAutoOffCountdown()
    syncUiEnabledState(hasCameraPermission())
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

    autoOffEndsAtMs = System.currentTimeMillis() + selectedAutoOffMinutes.toLong() * MS_PER_MINUTE
    updateAutoOffCountdown()
    countdownHandler.postDelayed(countdownRunnable, COUNTDOWN_TICK_MS)
  }

  private fun stopAutoOffCountdown() {
    countdownHandler.removeCallbacks(countdownRunnable)
    autoOffEndsAtMs = 0L
    setAutoOffCountdownVisible(false)
  }

  private fun updateAutoOffCountdown() {
    if (!isAnyLightActive() || selectedAutoOffMinutes <= 0 || autoOffEndsAtMs <= 0L) {
      setAutoOffCountdownVisible(false)
      return
    }

    val remainingMs = (autoOffEndsAtMs - System.currentTimeMillis()).coerceAtLeast(0L)
    binding.txtAutoOffCountdown.text = formatAutoOffRemaining(remainingMs)
    binding.txtAutoOffCountdown.visibility = View.VISIBLE
  }

  private fun setAutoOffCountdownVisible(visible: Boolean) {
    if (!visible) {
      binding.txtAutoOffCountdown.setText(R.string.auto_off_countdown_placeholder)
      binding.txtAutoOffCountdown.visibility = View.INVISIBLE
    } else {
      binding.txtAutoOffCountdown.visibility = View.VISIBLE
    }
  }

  private fun formatAutoOffRemaining(remainingMs: Long): String {
    val totalSeconds = ((remainingMs + COUNTDOWN_ROUND_UP_MS) / MS_PER_SECOND).coerceAtLeast(0L)
    val minutes = totalSeconds / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
  }

  private fun normalizeAutoOffMinutes(minutes: Int): Int {
    return AutoOffPreset.normalizeMinutes(minutes)
  }

  private fun minutesForAutoOffButtonId(buttonId: Int): Int {
    return AutoOffPreset.minutesForButtonId(buttonId)
  }

  private fun autoOffButtonIdForMinutes(minutes: Int): Int {
    return AutoOffPreset.buttonIdForMinutes(minutes)
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
    binding.btnToggle.setBackgroundResource(
      if (off) R.drawable.bg_power_button_off else R.drawable.bg_power_button_on
    )
    binding.viewPowerHalo.visibility = if (off) View.INVISIBLE else View.VISIBLE
  }

  private fun syncUiEnabledState(enabled: Boolean) {
    val torchControlsEnabled = enabled && torchAvailable
    val lightActive = isAnyLightActive()
    val autoOffLocked = torchControlsEnabled && lightActive
    val showActiveStrobe = torchControlsEnabled && selectedMode == Mode.STROBE

    binding.btnToggle.isEnabled = torchControlsEnabled
    binding.sliderStrobe.isEnabled = showActiveStrobe
    binding.sliderStrobe.isClickable = showActiveStrobe
    setStrobePreviewEnabled(false)
    sliderBrightness?.isEnabled = torchControlsEnabled && strengthSupported && selectedMode == Mode.TORCH
    setEnabledRecursive(binding.groupMode, torchControlsEnabled)
    setAutoOffControlsEnabled(torchControlsEnabled, autoOffLocked)
    binding.cardStrobe.visibility = if (showActiveStrobe) View.VISIBLE else View.INVISIBLE
    binding.cardStrobe.isEnabled = showActiveStrobe
    binding.cardStrobe.isClickable = showActiveStrobe
    binding.cardStrobe.alpha = 1f
    binding.cardAutoOff.alpha = if (autoOffLocked) DISABLED_SECTION_ALPHA else ENABLED_SECTION_ALPHA
    binding.btnScreenLight.isEnabled = true
    updateBrightnessValueLabel()

    if (!torchControlsEnabled) {
      stopAutoOffCountdown()
      binding.txtPowerState.setText(R.string.action_torch_on)
      binding.btnToggle.contentDescription = getString(R.string.action_torch_on)
      binding.btnToggle.setBackgroundResource(R.drawable.bg_power_button_disabled)
      binding.viewPowerHalo.visibility = View.INVISIBLE
    } else {
      setPowerLabel(off = !lightActive)
    }
  }

  private fun updateBrightnessValueLabel(
    value: Int = (sliderBrightness?.value ?: maxStrength.toFloat()).toInt()
  ) {
    val showActiveBrightness = torchAvailable &&
      strengthSupported &&
      selectedMode == Mode.TORCH &&
      binding.cardBrightness.visibility == View.VISIBLE &&
      maxStrength > DEFAULT_TORCH_STRENGTH

    binding.txtBrightnessValue.text = if (showActiveBrightness) {
      brightnessLevelLabel(value, maxStrength)
    } else {
      getString(R.string.brightness_level_max)
    }
    binding.txtBrightnessPreviewValue.setText(R.string.brightness_level_max)
  }

  private fun brightnessLevelLabel(value: Int, max: Int): String {
    val normalizedMax = max.coerceAtLeast(DEFAULT_TORCH_STRENGTH)
    val normalizedValue = value.coerceIn(DEFAULT_TORCH_STRENGTH, normalizedMax)
    if (normalizedMax <= DEFAULT_TORCH_STRENGTH || normalizedValue >= normalizedMax) {
      return getString(R.string.brightness_level_max)
    }

    return when (normalizedMax) {
      2 -> getString(R.string.brightness_level_low)
      3 -> if (normalizedValue == DEFAULT_TORCH_STRENGTH) {
        getString(R.string.brightness_level_low)
      } else {
        getString(R.string.brightness_level_medium)
      }
      else -> {
        val ratio = normalizedValue.toFloat() / normalizedMax.toFloat()
        when {
          ratio < BRIGHTNESS_LOW_RATIO -> getString(R.string.brightness_level_low)
          ratio < BRIGHTNESS_MEDIUM_RATIO -> getString(R.string.brightness_level_medium)
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
    if (view is ViewGroup) {
      for (i in 0 until view.childCount) setEnabledRecursive(view.getChildAt(i), enabled)
    }
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

  private fun saveTorchStrengthPreference(level: Int) {
    lifecycleScope.launch { appPreferences.saveTorchStrengthLevel(level) }
  }

  private fun sendToService(action: String?, strobeSpeed: Int? = null, torchIntensity: Int? = null) {
    val intent = Intent(this, TorchService::class.java)
    if (action != null) intent.action = action
    intent.putExtra(EXTRA_AUTO_OFF_MINUTES, selectedAutoOffMinutes)
    strobeSpeed?.let { intent.putExtra(EXTRA_STROBE_SPEED, it) }
    torchIntensity?.let { intent.putExtra(EXTRA_TORCH_INTENSITY, it) }
    ContextCompat.startForegroundService(this, intent)
  }

  private companion object {
    const val DEFAULT_TORCH_STRENGTH = 1
    const val NO_HAPTIC_VALUE = -1
    const val STROBE_SLIDER_MIN = 0
    const val STROBE_SLIDER_MAX = 4
    const val COUNTDOWN_TICK_MS = 1000L
    const val COUNTDOWN_ROUND_UP_MS = 999L
    const val MS_PER_SECOND = 1000L
    const val SECONDS_PER_MINUTE = 60L
    const val MS_PER_MINUTE = 60_000L
    const val BRIGHTNESS_STEP_SIZE = 1f
    const val BRIGHTNESS_LOW_RATIO = 0.34f
    const val BRIGHTNESS_MEDIUM_RATIO = 0.67f
    const val DISABLED_SECTION_ALPHA = 0.45f
    const val ENABLED_SECTION_ALPHA = 1f
  }
}
