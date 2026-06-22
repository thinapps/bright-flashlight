package top.thinapps.brightflashlight.ui

import top.thinapps.brightflashlight.R

enum class AutoOffPreset(
  val minutes: Int,
  val buttonId: Int
) {
  OFF(0, R.id.btnAutoOffOff),
  FIVE(5, R.id.btnAutoOff5),
  FIFTEEN(15, R.id.btnAutoOff15),
  THIRTY(30, R.id.btnAutoOff30),
  SIXTY(60, R.id.btnAutoOff60);

  companion object {
    fun minutesForButtonId(buttonId: Int): Int {
      return values().firstOrNull { it.buttonId == buttonId }?.minutes ?: OFF.minutes
    }

    fun buttonIdForMinutes(minutes: Int): Int {
      return presetForMinutes(minutes).buttonId
    }

    fun normalizeMinutes(minutes: Int): Int {
      return presetForMinutes(minutes).minutes
    }

    private fun presetForMinutes(minutes: Int): AutoOffPreset {
      return values().firstOrNull { it.minutes == minutes } ?: OFF
    }
  }
}
