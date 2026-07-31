package top.thinapps.brightflashlight.torch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import top.thinapps.brightflashlight.R

class TorchService : Service() {

    enum class ActiveMode { NONE, TORCH, STROBE, SOS }

    companion object {
        const val ACTION_TORCH_ON = "act_torch_on"
        const val ACTION_TORCH_OFF = "act_torch_off"
        const val ACTION_TORCH_UPDATE_INTENSITY = "act_torch_update_intensity"
        const val ACTION_STROBE_START = "act_strobe_start"
        const val ACTION_STROBE_STOP = "act_strobe_stop"
        const val ACTION_STROBE_UPDATE = "act_strobe_update"
        const val ACTION_SOS_START = "act_sos_start"
        const val ACTION_SOS_STOP = "act_sos_stop"

        const val EXTRA_TORCH_INTENSITY = "torchIntensity"
        const val EXTRA_AUTO_OFF_MINUTES = "autoOffMinutes"
        const val EXTRA_STROBE_SPEED = "strobeSpeed"

        private const val CH_ID = "flashlight"
        private const val NOTIF_ID = 42
        private const val NOTIF_TURN_OFF_REQUEST_CODE = 0
        private const val DEFAULT_TORCH_INTENSITY = 1
        private const val AUTO_OFF_CHECK_INTERVAL_MS = 1000L
        private const val MS_PER_MINUTE = 60_000L
        private const val SOS_DOT_MS = 200L
        private const val SOS_DASH_MS = 600L
        private const val SOS_SYMBOL_GAP_MS = 200L
        private const val SOS_LETTER_GAP_MS = 600L
        private const val SOS_WORD_GAP_MS = 1400L

        @Volatile
        private var currentActiveMode = ActiveMode.NONE

        @Volatile
        private var currentAutoOffAtMs = 0L

        @Suppress("UNUSED_PARAMETER")
        fun isActive(context: Context): Boolean {
            return currentActiveMode != ActiveMode.NONE
        }

        @Suppress("UNUSED_PARAMETER")
        fun activeMode(context: Context): ActiveMode {
            return currentActiveMode
        }

        @Suppress("UNUSED_PARAMETER")
        fun autoOffEndsAtMs(context: Context): Long {
            return if (currentActiveMode == ActiveMode.NONE) 0L else currentAutoOffAtMs
        }
    }

    private lateinit var controller: TorchController
    private val handler = Handler(Looper.getMainLooper())
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private var strobeRunning = false
    private var sosRunning = false
    private var strobeSpeed = StrobeSpeedPreset.DEFAULT_HZ
    private var currentIntensity = DEFAULT_TORCH_INTENSITY
    private var currentIntervalMs: Long = StrobeSpeedPreset.intervalMsForHz(strobeSpeed)
    private var autoOffAtMs: Long = 0L
    private var strobeLampOn = false

    private val autoOffRunnable = object : Runnable {
        override fun run() {
            if (autoOffAtMs == 0L) return
            if (System.currentTimeMillis() >= autoOffAtMs) {
                stopAndExit()
            } else {
                handler.postDelayed(this, AUTO_OFF_CHECK_INTERVAL_MS)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        controller = TorchController(applicationContext)
        currentIntensity = controller.getMaxStrength().coerceAtLeast(DEFAULT_TORCH_INTENSITY)
        startForeground(NOTIF_ID, buildNotification())
        controller.registerTorchCallback(
            handler = handler,
            onModeChanged = { enabled ->
                if (!enabled && currentActiveMode == ActiveMode.TORCH) {
                    stopAndExit()
                }
            },
            onUnavailable = {
                if (currentActiveMode != ActiveMode.NONE) {
                    stopAndExit()
                }
            }
        )
    }

    override fun onDestroy() {
        setServiceState(ActiveMode.NONE)
        controller.unregisterTorchCallback()
        stopAll()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        intent?.getIntExtra(EXTRA_STROBE_SPEED, -1)?.let { speed ->
            if (speed >= 0) {
                strobeSpeed = StrobeSpeedPreset.normalizeHz(speed)
                currentIntervalMs = StrobeSpeedPreset.intervalMsForHz(strobeSpeed)
            }
        }

        intent?.getIntExtra(EXTRA_TORCH_INTENSITY, -1)?.let { level ->
            if (level >= 0) {
                currentIntensity = normalizeTorchIntensity(level)
            }
        }

        intent?.getIntExtra(EXTRA_AUTO_OFF_MINUTES, -1)?.let { minutes ->
            if (minutes >= 0 && isStartAction(action)) {
                setAutoOffMinutes(minutes)
            }
        }

        when (action) {
            ACTION_TORCH_ON -> {
                stopPatterns()
                val level = currentIntensity.coerceAtLeast(DEFAULT_TORCH_INTENSITY)
                if (controller.setTorchIntensity(level)) {
                    markRunning(ActiveMode.TORCH)
                } else {
                    stopAndExit()
                }
            }
            ACTION_TORCH_OFF -> {
                stopAndExit()
            }
            ACTION_TORCH_UPDATE_INTENSITY -> {
                if (!strobeRunning && !sosRunning) {
                    if (controller.setTorchIntensity(currentIntensity)) {
                        markRunning(ActiveMode.TORCH)
                    } else {
                        stopAndExit()
                    }
                }
            }
            ACTION_STROBE_START -> {
                stopPatterns()
                if (controller.isAvailable() && startStrobe()) {
                    markRunning(ActiveMode.STROBE)
                } else {
                    stopAndExit()
                }
            }
            ACTION_STROBE_UPDATE -> {
                currentIntervalMs = StrobeSpeedPreset.intervalMsForHz(strobeSpeed)
                if (strobeRunning) restartStrobe()
            }
            ACTION_STROBE_STOP -> {
                stopAndExit()
            }
            ACTION_SOS_START -> {
                stopPatterns()
                if (controller.isAvailable() && startSos()) {
                    markRunning(ActiveMode.SOS)
                } else {
                    stopAndExit()
                }
            }
            ACTION_SOS_STOP -> {
                stopAndExit()
            }
            else -> {
                if (!isAnyModeRunning()) stopAndExit()
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        ensureNotificationChannel()

        val offIntent = Intent(this, TorchService::class.java).setAction(ACTION_TORCH_OFF)
        val offPendingIntent = PendingIntent.getService(
            this,
            NOTIF_TURN_OFF_REQUEST_CODE,
            offIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CH_ID)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_power)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_running))
            .addAction(
                R.drawable.ic_power,
                getString(R.string.notification_action_turn_off),
                offPendingIntent
            )
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CH_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun updateNotification() {
        notificationManager.notify(NOTIF_ID, buildNotification())
    }

    private fun markRunning(mode: ActiveMode) {
        setServiceState(mode)
        scheduleAutoOffCheck()
        updateNotification()
    }

    private fun stopAndExit() {
        setServiceState(ActiveMode.NONE)
        stopAll()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun setServiceState(mode: ActiveMode) {
        currentActiveMode = mode
        if (mode == ActiveMode.NONE) currentAutoOffAtMs = 0L
    }

    private fun isAnyModeRunning(): Boolean {
        return strobeRunning || sosRunning || isActive(this)
    }

    private fun isStartAction(action: String?): Boolean {
        return action == ACTION_TORCH_ON ||
            action == ACTION_STROBE_START ||
            action == ACTION_SOS_START
    }

    private fun stopAll() {
        stopPatterns()
        controller.setTorch(false)
    }

    private fun stopPatterns() {
        strobeRunning = false
        sosRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun normalizeTorchIntensity(level: Int): Int {
        val maxLevel = controller.getMaxStrength().coerceAtLeast(DEFAULT_TORCH_INTENSITY)
        return level.coerceIn(0, maxLevel)
    }

    private fun setAutoOffMinutes(minutes: Int) {
        autoOffAtMs = if (minutes == 0) {
            0L
        } else {
            System.currentTimeMillis() + minutes.toLong() * MS_PER_MINUTE
        }
        currentAutoOffAtMs = autoOffAtMs
    }

    private fun startStrobe(): Boolean {
        strobeRunning = true
        strobeLampOn = false
        return tickStrobe()
    }

    private fun restartStrobe() {
        if (!strobeRunning) return
        handler.removeCallbacks(strobeTickRunnable)
        if (!tickStrobe()) stopAndExit()
    }

    private val strobeTickRunnable = object : Runnable {
        override fun run() {
            if (!strobeRunning) return
            strobeLampOn = !strobeLampOn
            val level = if (strobeLampOn) {
                currentIntensity.coerceAtLeast(DEFAULT_TORCH_INTENSITY)
            } else {
                0
            }
            if (!controller.setTorchIntensity(level)) {
                stopAndExit()
                return
            }
            handler.postDelayed(this, currentIntervalMs / 2)
        }
    }

    private fun tickStrobe(): Boolean {
        if (!strobeRunning) return false
        strobeLampOn = !strobeLampOn
        val level = if (strobeLampOn) {
            currentIntensity.coerceAtLeast(DEFAULT_TORCH_INTENSITY)
        } else {
            0
        }
        if (!controller.setTorchIntensity(level)) {
            strobeRunning = false
            return false
        }
        handler.postDelayed(strobeTickRunnable, currentIntervalMs / 2)
        return true
    }

    private fun startSos(): Boolean {
        sosRunning = true
        val pattern = mutableListOf<Pair<Boolean, Long>>().apply {
            repeat(2) { add(true to SOS_DOT_MS); add(false to SOS_SYMBOL_GAP_MS) }
            add(true to SOS_DOT_MS); add(false to SOS_LETTER_GAP_MS)
            repeat(2) { add(true to SOS_DASH_MS); add(false to SOS_SYMBOL_GAP_MS) }
            add(true to SOS_DASH_MS); add(false to SOS_LETTER_GAP_MS)
            repeat(2) { add(true to SOS_DOT_MS); add(false to SOS_SYMBOL_GAP_MS) }
            add(true to SOS_DOT_MS); add(false to SOS_WORD_GAP_MS)
        }

        fun runFrom(index: Int) {
            if (!sosRunning) return
            val (on, durationMs) = pattern[index]
            val level = if (on) currentIntensity.coerceAtLeast(DEFAULT_TORCH_INTENSITY) else 0
            if (!controller.setTorchIntensity(level)) {
                stopAndExit()
                return
            }
            handler.postDelayed({ runFrom((index + 1) % pattern.size) }, durationMs)
        }

        val (firstOn, firstDurationMs) = pattern.first()
        val firstLevel = if (firstOn) currentIntensity.coerceAtLeast(DEFAULT_TORCH_INTENSITY) else 0
        if (!controller.setTorchIntensity(firstLevel)) {
            sosRunning = false
            return false
        }
        handler.postDelayed({ runFrom(1 % pattern.size) }, firstDurationMs)
        return true
    }

    private fun scheduleAutoOffCheck() {
        handler.removeCallbacks(autoOffRunnable)
        if (autoOffAtMs == 0L) return
        handler.postDelayed(autoOffRunnable, AUTO_OFF_CHECK_INTERVAL_MS)
    }
}
