package top.thinapps.brightflashlight.ui

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
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

        setupPresetButtons()
        showColor(COLOR_MAX, COLOR_MAX, COLOR_MAX)
        restorePreferences()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
            val preset = presetForKey(saved.screenLightPreset)
            if (preset == null) {
                showColor(saved.screenLightR, saved.screenLightG, saved.screenLightB)
                binding.groupScreenPresets.clearChecked()
            } else {
                showPreset(preset)
            }
        }
    }

    private fun applyPreset(key: String) {
        val preset = presetForKey(key) ?: return
        showPreset(preset)
        saveColorPreference(preset)
    }

    private fun showPreset(preset: ScreenPreset) {
        binding.groupScreenPresets.check(preset.buttonId)
        showColor(preset.r, preset.g, preset.b)
    }

    private fun presetForKey(key: String?): ScreenPreset? {
        return presets.firstOrNull { it.key == key }
    }

    private fun saveColorPreference(preset: ScreenPreset) {
        lifecycleScope.launch {
            appPreferences.saveScreenLightColor(preset.r, preset.g, preset.b, preset.key)
        }
    }

    private fun showColor(r: Int, g: Int, b: Int) {
        val normalizedR = normalizeColor(r)
        val normalizedG = normalizeColor(g)
        val normalizedB = normalizeColor(b)
        val color = Color.rgb(normalizedR, normalizedG, normalizedB)
        binding.root.setBackgroundColor(color)
        binding.tvColor.text = getString(
            R.string.screen_color_value,
            formatColorHex(normalizedR, normalizedG, normalizedB)
        )
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
