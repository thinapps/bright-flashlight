package top.thinapps.brightflashlight.torch

object StrobeSpeedPreset {
    const val DEFAULT_HZ = 3

    private const val MIN_HZ = 1
    private const val MAX_HZ = 5
    private const val MIN_SLIDER_VALUE = 0
    private const val MAX_SLIDER_VALUE = 4
    private const val MIN_INTERVAL_MS = 30L
    private const val MS_PER_SECOND = 1000.0

    fun hzForSliderValue(value: Int): Int {
        return value.coerceIn(MIN_SLIDER_VALUE, MAX_SLIDER_VALUE) + MIN_HZ
    }

    fun sliderValueForHz(hz: Int): Int {
        return normalizeHz(hz) - MIN_HZ
    }

    fun normalizeHz(hz: Int): Int {
        return hz.coerceIn(MIN_HZ, MAX_HZ)
    }

    fun intervalMsForHz(hz: Int): Long {
        val periodMs = (MS_PER_SECOND / normalizeHz(hz).toDouble()).toLong()
        return periodMs.coerceAtLeast(MIN_INTERVAL_MS)
    }
}
