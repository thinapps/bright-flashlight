package top.thinapps.brightflashlight.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import top.thinapps.brightflashlight.R

class ScreenLightActivity : ComponentActivity() {

    private lateinit var root: View
    private lateinit var tvColor: TextView
    private lateinit var seekR: SeekBar
    private lateinit var seekG: SeekBar
    private lateinit var seekB: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_light)

        root = findViewById(R.id.root)
        tvColor = findViewById(R.id.tvColor)
        seekR = findViewById(R.id.seekR)
        seekG = findViewById(R.id.seekG)
        seekB = findViewById(R.id.seekB)

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                applyColor()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        seekR.setOnSeekBarChangeListener(listener)
        seekG.setOnSeekBarChangeListener(listener)
        seekB.setOnSeekBarChangeListener(listener)

        // default white color
        seekR.progress = 255
        seekG.progress = 255
        seekB.progress = 255
        applyColor()

        // keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
