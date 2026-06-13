package top.thinapps.brightflashlight.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import top.thinapps.brightflashlight.R
import top.thinapps.brightflashlight.prefs.AppPreferences

class ScreenLightActivity : ComponentActivity() {

    private lateinit var root: View
    private lateinit var tvColor: TextView
    private lateinit var seekR: SeekBar
    private lateinit var seekG: SeekBar
    private lateinit var seekB: SeekBar
    private lateinit var appPreferences: AppPreferences

    private var settingColor = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_light)

        appPreferences = AppPreferences(applicationContext)
        root = findViewById(R.id.root)
        tvColor = findViewById(R.id.tvColor)
        seekR = findViewById(R.id.seekR)
        seekG = findViewById(R.id.seekG)
        seekB = findViewById(R.id.seekB)

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!settingColor) applyColor()
                if (fromUser) saveColorPreference()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        seekR.setOnSeekBarChangeListener(listener)
        seekG.setOnSeekBarChangeListener(listener)
        seekB.setOnSeekBarChangeListener(listener)

        findViewById<View>(R.id.btnPresetWhite).setOnClickListener { setColor(255, 255, 255, save = true) }
        findViewById<View>(R.id.btnPresetWarm).setOnClickListener { setColor(255, 196, 120, save = true) }
        findViewById<View>(R.id.btnPresetRed).setOnClickListener { setColor(255, 0, 0, save = true) }
        findViewById<View>(R.id.btnPresetBlue).setOnClickListener { setColor(0, 96, 255, save = true) }

        setColor(255, 255, 255, save = false)
        restorePreferences()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun restorePreferences() {
        lifecycleScope.launch {
            val saved = appPreferences.preferences.first()
            setColor(saved.screenLightR, saved.screenLightG, saved.screenLightB, save = false)
        }
    }

    private fun setColor(r: Int, g: Int, b: Int, save: Boolean) {
        settingColor = true
        seekR.progress = r.coerceIn(0, 255)
        seekG.progress = g.coerceIn(0, 255)
        seekB.progress = b.coerceIn(0, 255)
        settingColor = false
        applyColor()
        if (save) saveColorPreference()
    }

    private fun saveColorPreference() {
        lifecycleScope.launch {
            appPreferences.saveScreenLightColor(seekR.progress, seekG.progress, seekB.progress)
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
