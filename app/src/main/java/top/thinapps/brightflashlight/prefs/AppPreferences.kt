package top.thinapps.brightflashlight.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import top.thinapps.brightflashlight.torch.StrobeSpeedPreset
import java.io.IOException

data class SavedPreferences(
    val lastMode: String = DEFAULT_MODE,
    val autoOffMinutes: Int = DEFAULT_AUTO_OFF_MINUTES,
    val strobeSpeed: Int = StrobeSpeedPreset.DEFAULT_HZ,
    val screenLightR: Int = DEFAULT_SCREEN_LIGHT_COLOR,
    val screenLightG: Int = DEFAULT_SCREEN_LIGHT_COLOR,
    val screenLightB: Int = DEFAULT_SCREEN_LIGHT_COLOR,
    val screenLightPreset: String? = null
)

private const val DEFAULT_MODE = "TORCH"
private const val DEFAULT_AUTO_OFF_MINUTES = 0
private const val DEFAULT_SCREEN_LIGHT_COLOR = 255

private val allowedModes = setOf("TORCH", "STROBE", "SOS")
private val allowedAutoOffMinutes = setOf(0, 5, 15, 30, 60)

private val Context.brightFlashlightDataStore by preferencesDataStore(
    name = "bright_flashlight_preferences"
)

class AppPreferences(context: Context) {

    private val dataStore = context.applicationContext.brightFlashlightDataStore

    private object Keys {
        val LAST_MODE = stringPreferencesKey("last_mode")
        val AUTO_OFF_MINUTES = intPreferencesKey("auto_off_minutes")
        val STROBE_SPEED = intPreferencesKey("strobe_speed")
        val SCREEN_LIGHT_R = intPreferencesKey("screen_light_r")
        val SCREEN_LIGHT_G = intPreferencesKey("screen_light_g")
        val SCREEN_LIGHT_B = intPreferencesKey("screen_light_b")
        val SCREEN_LIGHT_PRESET = stringPreferencesKey("screen_light_preset")
    }

    val preferences: Flow<SavedPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            SavedPreferences(
                lastMode = normalizeMode(prefs[Keys.LAST_MODE]),
                autoOffMinutes = normalizeAutoOffMinutes(prefs[Keys.AUTO_OFF_MINUTES] ?: DEFAULT_AUTO_OFF_MINUTES),
                strobeSpeed = StrobeSpeedPreset.normalizeHz(prefs[Keys.STROBE_SPEED] ?: StrobeSpeedPreset.DEFAULT_HZ),
                screenLightR = normalizeColor(prefs[Keys.SCREEN_LIGHT_R] ?: DEFAULT_SCREEN_LIGHT_COLOR),
                screenLightG = normalizeColor(prefs[Keys.SCREEN_LIGHT_G] ?: DEFAULT_SCREEN_LIGHT_COLOR),
                screenLightB = normalizeColor(prefs[Keys.SCREEN_LIGHT_B] ?: DEFAULT_SCREEN_LIGHT_COLOR),
                screenLightPreset = prefs[Keys.SCREEN_LIGHT_PRESET]
            )
        }

    suspend fun saveMode(mode: String) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_MODE] = normalizeMode(mode)
        }
    }

    suspend fun saveAutoOffMinutes(minutes: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.AUTO_OFF_MINUTES] = normalizeAutoOffMinutes(minutes)
        }
    }

    suspend fun saveStrobeSpeed(speed: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.STROBE_SPEED] = StrobeSpeedPreset.normalizeHz(speed)
        }
    }

    suspend fun saveScreenLightColor(r: Int, g: Int, b: Int, preset: String?) {
        dataStore.edit { prefs ->
            prefs[Keys.SCREEN_LIGHT_R] = normalizeColor(r)
            prefs[Keys.SCREEN_LIGHT_G] = normalizeColor(g)
            prefs[Keys.SCREEN_LIGHT_B] = normalizeColor(b)

            if (preset == null) {
                prefs.remove(Keys.SCREEN_LIGHT_PRESET)
            } else {
                prefs[Keys.SCREEN_LIGHT_PRESET] = preset
            }
        }
    }

    private fun normalizeMode(mode: String?): String {
        return if (mode != null && mode in allowedModes) mode else DEFAULT_MODE
    }

    private fun normalizeAutoOffMinutes(minutes: Int): Int {
        return if (minutes in allowedAutoOffMinutes) minutes else DEFAULT_AUTO_OFF_MINUTES
    }

    private fun normalizeColor(value: Int): Int {
        return value.coerceIn(0, 255)
    }
}
