package top.thinapps.brightflashlight.torch

object StrobeSpeedPreset {
    const val DEFAULT_HZ = 2

    fun hzForSliderValue(value: Int): Int {
        return when (value.coerceIn(0, 4)) {
            0 -> 1
            1 -> 2
            2 -> 3
            3 -> 4
            else -> 5
        }
    }

    fun sliderValueForHz(hz: Int): Int {
        return when {
            hz <= 1 -> 0
            hz == 2 -> 1
            hz == 3 -> 2
            hz == 4 -> 3
            else -> 4
        }
    }

    fun normalizeHz(hz: Int): Int {
        return hzForSliderValue(sliderValueForHz(hz))
    }

    fun intervalMsForHz(hz: Int): Long {
        val periodMs = (1000.0 / normalizeHz(hz).toDouble()).toLong()
        return periodMs.coerceAtLeast(30L)
    }
}
