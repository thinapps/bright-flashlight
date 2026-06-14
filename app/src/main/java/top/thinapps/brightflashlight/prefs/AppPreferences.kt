package top.thinapps.brightflashlight.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import top.thinapps.brightflashlight.torch.StrobeSpeedPreset

data class SavedPreferences(
    val lastMode: String = "TORCH",
    val autoOffMinutes: Int = 0,
    val strobeSpeed: Int = StrobeSpeedPreset.DEFAULT_HZ,
    val screenLightR: Int = 255,
    val screenLightG: Int = 255,
    val screenLightB: Int = 255,
    val screenLightPreset: String? = null
)

private val Context.brightFlashlightDataStore by preferencesDataStore(
    name = "bright_flashlight_preferences"
)

class AppPreferences(private val context: Context) {

    private object Keys {
        val LAST_MODE = stringPreferencesKey("last_mode")
        val AUTO_OFF_MINUTES = intPreferencesKey("auto_off_minutes")
        val STROBE_SPEED = intPreferencesKey("strobe_speed")
        val SCREEN_LIGHT_R = intPreferencesKey("screen_light_r")
        val SCREEN_LIGHT_G = intPreferencesKey("screen_light_g")
        val SCREEN_LIGHT_B = intPreferencesKey("screen_light_b")
        val SCREEN_LIGHT_PRESET = stringPreferencesKey("screen_light_preset")
    }

    val preferences: Flow<SavedPreferences> = context.brightFlashlightDataStore.data.map { prefs ->
        SavedPreferences(
            lastMode = prefs[Keys.LAST_MODE] ?: "TORCH",
            autoOffMinutes = (prefs[Keys.AUTO_OFF_MINUTES] ?: 0).coerceIn(0, 60),
            strobeSpeed = StrobeSpeedPreset.normalizeHz(prefs[Keys.STROBE_SPEED] ?: StrobeSpeedPreset.DEFAULT_HZ),
            screenLightR = (prefs[Keys.SCREEN_LIGHT_R] ?: 255).coerceIn(0, 255),
            screenLightG = (prefs[Keys.SCREEN_LIGHT_G] ?: 255).coerceIn(0, 255),
            screenLightB = (prefs[Keys.SCREEN_LIGHT_B] ?: 255).coerceIn(0, 255),
            screenLightPreset = prefs[Keys.SCREEN_LIGHT_PRESET]
        )
    }

    suspend fun saveMode(mode: String) {
        context.brightFlashlightDataStore.edit { prefs ->
            prefs[Keys.LAST_MODE] = mode
        }
    }

    suspend fun saveAutoOffMinutes(minutes: Int) {
        context.brightFlashlightDataStore.edit { prefs ->
            prefs[Keys.AUTO_OFF_MINUTES] = minutes.coerceIn(0, 60)
        }
    }

    suspend fun saveStrobeSpeed(speed: Int) {
        context.brightFlashlightDataStore.edit { prefs ->
            prefs[Keys.STROBE_SPEED] = StrobeSpeedPreset.normalizeHz(speed)
        }
    }

    suspend fun saveScreenLightColor(r: Int, g: Int, b: Int, preset: String?) {
        context.brightFlashlightDataStore.edit { prefs ->
            prefs[Keys.SCREEN_LIGHT_R] = r.coerceIn(0, 255)
            prefs[Keys.SCREEN_LIGHT_G] = g.coerceIn(0, 255)
            prefs[Keys.SCREEN_LIGHT_B] = b.coerceIn(0, 255)
            if (preset == null) {
                prefs.remove(Keys.SCREEN_LIGHT_PRESET)
            } else {
                prefs[Keys.SCREEN_LIGHT_PRESET] = preset
            }
        }
    }
}
