package top.thinapps.brightflashlight.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
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
    private lateinit var trayToggleButton: ImageButton
    private var colorTrayCollapsed = false

    private val presets = listOf(
        ScreenPreset(PRESET_WHITE, R.id.btnPresetWhite, COLOR_MAX, COLOR_MAX, COLOR_MAX),
        ScreenPreset(PRESET_COOL_WHITE, R.id.btnPresetCoolWhite, 244, 241, 232),
        ScreenPreset(PRESET_WARM, R.id.btnPresetWarm, 234, 244, COLOR_MAX),
        ScreenPreset(PRESET_CANDLE, R.id.btnPresetCandle, COLOR_MAX, 216, 168),
        ScreenPreset(PRESET_AMBER, R.id.btnPresetAmber, COLOR_MAX, 154, 61),
        ScreenPreset(PRESET_YELLOW, R.id.btnPresetYellow, COLOR_MAX, 196, COLOR_MIN),
        ScreenPreset(PRESET_ORANGE, R.id.btnPresetOrange, COLOR_MAX, 106, COLOR_MIN),
        ScreenPreset(PRESET_RED, R.id.btnPresetRed, COLOR_MAX, 32, 32),
        ScreenPreset(PRESET_PINK, R.id.btnPresetPink, COLOR_MAX, 90, 138),
        ScreenPreset(PRESET_MAGENTA, R.id.btnPresetMagenta, 214, 51, COLOR_MAX),
        ScreenPreset(PRESET_LAVENDER, R.id.btnPresetLavender, 138, 92, COLOR_MAX),
        ScreenPreset(PRESET_PURPLE, R.id.btnPresetPurple, 63, 81, COLOR_MAX),
        ScreenPreset(PRESET_BLUE, R.id.btnPresetBlue, 30, 107, COLOR_MAX),
        ScreenPreset(PRESET_SKY, R.id.btnPresetSky, 90, 200, COLOR_MAX),
        ScreenPreset(PRESET_CYAN, R.id.btnPresetCyan, COLOR_MIN, 213, 232),
        ScreenPreset(PRESET_AQUA, R.id.btnPresetAqua, COLOR_MIN, 191, 165),
        ScreenPreset(PRESET_TEAL, R.id.btnPresetTeal, COLOR_MIN, 200, 83),
        ScreenPreset(PRESET_GREEN, R.id.btnPresetGreen, 141, COLOR_MAX, 192),
        ScreenPreset(PRESET_MINT, R.id.btnPresetMint, 184, 232, 48),
        ScreenPreset(PRESET_LIME, R.id.btnPresetLime, 191, 199, 207)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appPreferences = AppPreferences(applicationContext)
        binding = ActivityScreenLightBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val backButton = setupBackButton()
        applyWindowInsets(backButton)
        setupColorTrayToggle()
        setupPresetButtons()
        clearPresetSelection()
        showColor(COLOR_MAX, COLOR_MAX, COLOR_MAX)
        restorePreferences()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setupBackButton(): View {
        val backButton = layoutInflater.inflate(R.layout.view_screen_light_back_button, binding.root, false)
        backButton.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            finish()
        }
        binding.root.addView(backButton)
        return backButton
    }

    private fun applyWindowInsets(backButton: View) {
        val baseTrayLeft = binding.layoutScreenControls.paddingLeft
        val baseTrayRight = binding.layoutScreenControls.paddingRight
        val baseTrayBottom = binding.layoutScreenControls.paddingBottom
        val backButtonLayoutParams = backButton.layoutParams as ViewGroup.MarginLayoutParams
        val baseBackStart = backButtonLayoutParams.marginStart
        val baseBackTop = backButtonLayoutParams.topMargin

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val startInset = if (binding.root.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                bars.right
            } else {
                bars.left
            }

            binding.layoutScreenControls.updatePadding(
                left = baseTrayLeft + bars.left,
                right = baseTrayRight + bars.right,
                bottom = baseTrayBottom + bars.bottom
            )
            backButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginStart = baseBackStart + startInset
                topMargin = baseBackTop + bars.top
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun setupColorTrayToggle() {
        val tray = binding.layoutScreenPresets.parent as? LinearLayout ?: return
        val colorTextIndex = tray.indexOfChild(binding.tvColor)
        val colorRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        tray.removeView(binding.tvColor)
        binding.tvColor.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        colorRow.addView(binding.tvColor)

        trayToggleButton = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.warning_button_size),
                resources.getDimensionPixelSize(R.dimen.warning_button_size)
            )
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(
                resources.getDimensionPixelSize(R.dimen.screen_button_icon_padding),
                resources.getDimensionPixelSize(R.dimen.screen_button_icon_padding),
                resources.getDimensionPixelSize(R.dimen.screen_button_icon_padding),
                resources.getDimensionPixelSize(R.dimen.screen_button_icon_padding)
            )
            setOnClickListener { view ->
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                toggleColorTray()
            }
        }
        colorRow.addView(trayToggleButton)
        tray.addView(colorRow, colorTextIndex)
        updateColorTrayToggle()
    }

    private fun toggleColorTray() {
        applyColorTrayCollapsed(!colorTrayCollapsed)
        saveColorTrayPreference()
    }

    private fun applyColorTrayCollapsed(collapsed: Boolean) {
        colorTrayCollapsed = collapsed
        binding.layoutScreenPresets.visibility = if (colorTrayCollapsed) View.GONE else View.VISIBLE
        updateColorTrayToggle()
    }

    private fun updateColorTrayToggle() {
        if (!::trayToggleButton.isInitialized) return
        if (colorTrayCollapsed) {
            trayToggleButton.setImageResource(R.drawable.ic_expand_less)
            trayToggleButton.contentDescription = getString(R.string.action_show_color_tray)
        } else {
            trayToggleButton.setImageResource(R.drawable.ic_expand_more)
            trayToggleButton.contentDescription = getString(R.string.action_hide_color_tray)
        }
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
            applyColorTrayCollapsed(saved.screenLightTrayCollapsed)
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

    private fun saveColorTrayPreference() {
        lifecycleScope.launch {
            appPreferences.saveScreenLightTrayCollapsed(colorTrayCollapsed)
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
