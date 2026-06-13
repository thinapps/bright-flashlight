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
        ScreenPreset("WHITE", R.id.btnPresetWhite, 255, 255, 255),
        ScreenPreset("WARM", R.id.btnPresetWarm, 255, 196, 120),
        ScreenPreset("RED", R.id.btnPresetRed, 255, 0, 0),
        ScreenPreset("BLUE", R.id.btnPresetBlue, 0, 96, 255)
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

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!settingColor) applyColor()
                if (fromUser) {
                    groupScreenPresets.clearChecked()
                    saveColorPreference(preset = null)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        seekR.setOnSeekBarChangeListener(listener)
        seekG.setOnSeekBarChangeListener(listener)
        seekB.setOnSeekBarChangeListener(listener)

        findViewById<View>(R.id.btnPresetWhite).setOnClickListener { applyPreset("WHITE") }
        findViewById<View>(R.id.btnPresetWarm).setOnClickListener { applyPreset("WARM") }
        findViewById<View>(R.id.btnPresetRed).setOnClickListener { applyPreset("RED") }
        findViewById<View>(R.id.btnPresetBlue).setOnClickListener { applyPreset("BLUE") }

        setColor(255, 255, 255)
        restorePreferences()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
        seekR.progress = r.coerceIn(0, 255)
        seekG.progress = g.coerceIn(0, 255)
        seekB.progress = b.coerceIn(0, 255)
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
        tvColor.text = getString(R.string.screen_color) + ": #" + "%02X%02X%02X".format(r, g, b)
    }
}
