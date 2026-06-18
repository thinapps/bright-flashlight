package top.thinapps.brightflashlight.ui

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import android.widget.SeekBar
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import top.thinapps.brightflashlight.R
import top.thinapps.brightflashlight.databinding.ActivityScreenLightBinding
import top.thinapps.brightflashlight.prefs.AppPreferences
import java.util.Locale

class ScreenLightActivity : ComponentActivity() {

    private data class ScreenPreset(
        val key: String,
        val buttonId: Int,
        val r: Int,
        val g: Int,
        val b: Int
    )

    private lateinit var binding: ActivityScreenLightBinding
    private lateinit var appPreferences: AppPreferences

    private var settingColor = false

    private val presets = listOf(
        ScreenPreset(PRESET_WHITE, R.id.btnPresetWhite, COLOR_MAX, COLOR_MAX, COLOR_MAX),
        ScreenPreset(PRESET_WARM, R.id.btnPresetWarm, COLOR_MAX, 196, 120),
        ScreenPreset(PRESET_RED, R.id.btnPresetRed, COLOR_MAX, COLOR_MIN, COLOR_MIN),
        ScreenPreset(PRESET_BLUE, R.id.btnPresetBlue, COLOR_MIN, 96, COLOR_MAX)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appPreferences = AppPreferences(applicationContext)
        binding = ActivityScreenLightBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupColorSliders()
        setupPresetButtons()

        setColor(COLOR_MAX, COLOR_MAX, COLOR_MAX)
        restorePreferences()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setupColorSliders() {
        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!settingColor) applyColor()
                if (fromUser) binding.groupScreenPresets.clearChecked()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveColorPreference(preset = null)
            }
        }

        binding.seekR.setOnSeekBarChangeListener(listener)
        binding.seekG.setOnSeekBarChangeListener(listener)
        binding.seekB.setOnSeekBarChangeListener(listener)
    }

    private fun setupPresetButtons() {
        binding.btnPresetWhite.setOnClickListener { applyPreset(PRESET_WHITE) }
        binding.btnPresetWarm.setOnClickListener { applyPreset(PRESET_WARM) }
        binding.btnPresetRed.setOnClickListener { applyPreset(PRESET_RED) }
        binding.btnPresetBlue.setOnClickListener { applyPreset(PRESET_BLUE) }
    }

    private fun restorePreferences() {
        lifecycleScope.launch {
            val saved = appPreferences.preferences.first()
            setColor(saved.screenLightR, saved.screenLightG, saved.screenLightB)
            val preset = presetForKey(saved.screenLightPreset)
            if (preset == null) {
                binding.groupScreenPresets.clearChecked()
            } else {
                binding.groupScreenPresets.check(preset.buttonId)
            }
        }
    }

    private fun applyPreset(key: String) {
        val preset = presetForKey(key) ?: return
        binding.groupScreenPresets.check(preset.buttonId)
        setColor(preset.r, preset.g, preset.b)
        saveColorPreference(preset = preset.key)
    }

    private fun presetForKey(key: String?): ScreenPreset? {
        return presets.firstOrNull { it.key == key }
    }

    private fun setColor(r: Int, g: Int, b: Int) {
        settingColor = true
        binding.seekR.progress = normalizeColor(r)
        binding.seekG.progress = normalizeColor(g)
        binding.seekB.progress = normalizeColor(b)
        settingColor = false
        applyColor()
    }

    private fun saveColorPreference(preset: String?) {
        lifecycleScope.launch {
            binding.apply {
                appPreferences.saveScreenLightColor(seekR.progress, seekG.progress, seekB.progress, preset)
            }
        }
    }

    private fun applyColor() {
        val r = binding.seekR.progress
        val g = binding.seekG.progress
        val b = binding.seekB.progress
        val color = Color.rgb(r, g, b)
        binding.root.setBackgroundColor(color)
        binding.tvColor.text = getString(R.string.screen_color) + ": " + formatColorHex(r, g, b)
    }

    private fun normalizeColor(value: Int): Int {
        return value.coerceIn(COLOR_MIN, COLOR_MAX)
    }

    private fun formatColorHex(r: Int, g: Int, b: Int): String {
        return String.format(Locale.US, "#%02X%02X%02X", r, g, b)
    }

    private companion object {
        const val PRESET_WHITE = "WHITE"
        const val PRESET_WARM = "WARM"
        const val PRESET_RED = "RED"
        const val PRESET_BLUE = "BLUE"
        const val COLOR_MIN = 0
        const val COLOR_MAX = 255
    }
}
