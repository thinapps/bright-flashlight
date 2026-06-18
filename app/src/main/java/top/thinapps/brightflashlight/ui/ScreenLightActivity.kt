package top.thinapps.brightflashlight.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
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
        ScreenPreset(PRESET_COOL_WHITE, R.id.btnPresetCoolWhite, 232, 246, COLOR_MAX),
        ScreenPreset(PRESET_WARM, R.id.btnPresetWarm, COLOR_MAX, 196, 120),
        ScreenPreset(PRESET_CANDLE, R.id.btnPresetCandle, COLOR_MAX, 168, 77),
        ScreenPreset(PRESET_AMBER, R.id.btnPresetAmber, COLOR_MAX, 176, COLOR_MIN),
        ScreenPreset(PRESET_YELLOW, R.id.btnPresetYellow, COLOR_MAX, 227, 71),
        ScreenPreset(PRESET_ORANGE, R.id.btnPresetOrange, COLOR_MAX, 122, COLOR_MIN),
        ScreenPreset(PRESET_RED, R.id.btnPresetRed, COLOR_MAX, COLOR_MIN, COLOR_MIN),
        ScreenPreset(PRESET_PINK, R.id.btnPresetPink, COLOR_MAX, 79, 163),
        ScreenPreset(PRESET_MAGENTA, R.id.btnPresetMagenta, COLOR_MAX, COLOR_MIN, COLOR_MAX),
        ScreenPreset(PRESET_LAVENDER, R.id.btnPresetLavender, 180, 132, COLOR_MAX),
        ScreenPreset(PRESET_PURPLE, R.id.btnPresetPurple, 142, 68, COLOR_MAX),
        ScreenPreset(PRESET_BLUE, R.id.btnPresetBlue, COLOR_MIN, 96, COLOR_MAX),
        ScreenPreset(PRESET_SKY, R.id.btnPresetSky, 64, 191, COLOR_MAX),
        ScreenPreset(PRESET_CYAN, R.id.btnPresetCyan, COLOR_MIN, 200, COLOR_MAX),
        ScreenPreset(PRESET_AQUA, R.id.btnPresetAqua, COLOR_MIN, COLOR_MAX, 208),
        ScreenPreset(PRESET_TEAL, R.id.btnPresetTeal, COLOR_MIN, 160, 128),
        ScreenPreset(PRESET_GREEN, R.id.btnPresetGreen, COLOR_MIN, 200, 83),
        ScreenPreset(PRESET_MINT, R.id.btnPresetMint, 124, COLOR_MAX, 178),
        ScreenPreset(PRESET_LIME, R.id.btnPresetLime, 182, COLOR_MAX, COLOR_MIN)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appPreferences = AppPreferences(applicationContext)
        binding = ActivityScreenLightBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackButton()
        setupPresetButtons()
        clearPresetSelection()
        showColor(COLOR_MAX, COLOR_MAX, COLOR_MAX)
        restorePreferences()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setupBackButton() {
        val backButton = layoutInflater.inflate(R.layout.view_screen_light_back_button, binding.root, false)
        backButton.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            finish()
        }
        binding.root.addView(backButton)
    }

    private fun setupPresetButtons() {
        presets.forEach { preset ->
            buttonForPreset(preset).setOnClickListener { view ->
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                applyPreset(preset.key)
            }
        }
    }

    private fun restorePreferences() {
        lifecycleScope.launch {
            val saved = appPreferences.preferences.first()
            val preset = presetForKey(saved.screenLightPreset)
            if (preset == null) {
                clearPresetSelection()
                showColor(saved.screenLightR, saved.screenLightG, saved.screenLightB)
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
        setSelectedPreset(preset.key)
        showColor(preset.r, preset.g, preset.b)
    }

    private fun clearPresetSelection() {
        setSelectedPreset(selectedKey = null)
    }

    private fun setSelectedPreset(selectedKey: String?) {
        val normalStroke = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.screen_preset_tile_stroke))
        val selectedStroke = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.screen_preset_tile_selected_stroke))
        val normalStrokeWidth = resources.getDimensionPixelSize(R.dimen.screen_light_preset_tile_stroke_width)
        val selectedStrokeWidth = resources.getDimensionPixelSize(R.dimen.screen_light_preset_selected_stroke_width)

        presets.forEach { preset ->
            val selected = preset.key == selectedKey
            val button = buttonForPreset(preset)
            button.isChecked = selected
            button.strokeColor = if (selected) selectedStroke else normalStroke
            button.strokeWidth = if (selected) selectedStrokeWidth else normalStrokeWidth
        }
    }

    private fun buttonForPreset(preset: ScreenPreset): MaterialButton {
        return binding.root.findViewById(preset.buttonId)
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
        const val PRESET_COOL_WHITE = "COOL_WHITE"
        const val PRESET_WARM = "WARM"
        const val PRESET_CANDLE = "CANDLE"
        const val PRESET_AMBER = "AMBER"
        const val PRESET_YELLOW = "YELLOW"
        const val PRESET_ORANGE = "ORANGE"
        const val PRESET_RED = "RED"
        const val PRESET_PINK = "PINK"
        const val PRESET_MAGENTA = "MAGENTA"
        const val PRESET_LAVENDER = "LAVENDER"
        const val PRESET_PURPLE = "PURPLE"
        const val PRESET_BLUE = "BLUE"
        const val PRESET_SKY = "SKY"
        const val PRESET_CYAN = "CYAN"
        const val PRESET_AQUA = "AQUA"
        const val PRESET_TEAL = "TEAL"
        const val PRESET_GREEN = "GREEN"
        const val PRESET_MINT = "MINT"
        const val PRESET_LIME = "LIME"
        const val COLOR_MIN = 0
        const val COLOR_MAX = 255
    }
}
