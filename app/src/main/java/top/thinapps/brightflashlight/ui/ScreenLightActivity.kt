package top.thinapps.brightflashlight.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import top.thinapps.brightflashlight.R
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

    private lateinit var root: View
    private lateinit var tvColor: TextView
    private lateinit var seekR: SeekBar
    private lateinit var seekG: SeekBar
    private lateinit var seekB: SeekBar
    private lateinit var groupScreenPresets: MaterialButtonToggleGroup
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
        setContentView(R.layout.activity_screen_light)

        appPreferences = AppPreferences(applicationContext)
        root = findViewById(R.id.root)
        tvColor = findViewById(R.id.tvColor)
        seekR = findViewById(R.id.seekR)
        seekG = findViewById(R.id.seekG)
        seekB = findViewById(R.id.seekB)
        groupScreenPresets = findViewById(R.id.groupScreenPresets)

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
                if (fromUser) groupScreenPresets.clearChecked()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveColorPreference(preset = null)
            }
        }

        seekR.setOnSeekBarChangeListener(listener)
        seekG.setOnSeekBarChangeListener(listener)
        seekB.setOnSeekBarChangeListener(listener)
    }

    private fun setupPresetButtons() {
        findViewById<View>(R.id.btnPresetWhite).setOnClickListener { applyPreset(PRESET_WHITE) }
        findViewById<View>(R.id.btnPresetWarm).setOnClickListener { applyPreset(PRESET_WARM) }
        findViewById<View>(R.id.btnPresetRed).setOnClickListener { applyPreset(PRESET_RED) }
        findViewById<View>(R.id.btnPresetBlue).setOnClickListener { applyPreset(PRESET_BLUE) }
    }

    private fun restorePreferences() {
        lifecycleScope.launch {
            val saved = appPreferences.preferences.first()
            setColor(saved.screenLightR, saved.screenLightG, saved.screenLightB)
            val preset = presetForKey(saved.screenLightPreset)
            if (preset == null) {
                groupScreenPresets.clearChecked()
            } else {
                groupScreenPresets.check(preset.buttonId)
            }
        }
    }

    private fun applyPreset(key: String) {
        val preset = presetForKey(key) ?: return
        groupScreenPresets.check(preset.buttonId)
        setColor(preset.r, preset.g, preset.b)
        saveColorPreference(preset = preset.key)
    }

    private fun presetForKey(key: String?): ScreenPreset? {
        return presets.firstOrNull { it.key == key }
    }

    private fun setColor(r: Int, g: Int, b: Int) {
        settingColor = true
        seekR.progress = normalizeColor(r)
        seekG.progress = normalizeColor(g)
        seekB.progress = normalizeColor(b)
        settingColor = false
        applyColor()
    }

    private fun saveColorPreference(preset: String?) {
        lifecycleScope.launch {
            appPreferences.saveScreenLightColor(seekR.progress, seekG.progress, seekB.progress, preset)
        }
    }

    private fun applyColor() {
        val r = seekR.progress
        val g = seekG.progress
        val b = seekB.progress
        val color = Color.rgb(r, g, b)
        root.setBackgroundColor(color)
        tvColor.text = getString(R.string.screen_color) + ": " + formatColorHex(r, g, b)
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
