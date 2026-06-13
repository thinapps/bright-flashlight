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

        private const val CH_ID = "flashlight"
        private const val NOTIF_ID = 42
        private const val EXTRA_STROBE_SPEED = "strobeSpeed"
        private const val PREFS = "torch_service_state"
        private const val KEY_ACTIVE = "active"

        fun isActive(context: Context): Boolean {
            return context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ACTIVE, false)
        }
    }

    private lateinit var controller: TorchController
    private val handler = Handler(Looper.getMainLooper())

    private var strobeRunning = false
    private var sosRunning = false
    private var strobeSpeed = 5
    private var curIntensity = 1
    private var curIntervalMs: Long = 100L
    private var autoOffAtMs: Long = 0L
    private var strobeLampOn = false

    override fun onCreate() {
        super.onCreate()
        controller = TorchController(applicationContext)
        curIntervalMs = strobeIntervalMs(strobeSpeed)
        val maxApi = controller.getMaxIntensity()
        curIntensity = if (maxApi > 1) maxApi else 1
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onDestroy() {
        stopAll()
        setServiceActive(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getIntExtra(EXTRA_STROBE_SPEED, -1)?.let {
            if (it >= 0) {
                strobeSpeed = it
                curIntervalMs = strobeIntervalMs(strobeSpeed)
            }
        }

        intent?.getIntExtra(EXTRA_TORCH_INTENSITY, -1)?.let { uiVal ->
            if (uiVal >= 0) {
                curIntensity = mapUiToDeviceIntensity(uiVal, controller.getMaxIntensity())
            }
        }

        intent?.getIntExtra(EXTRA_AUTO_OFF_MINUTES, -1)?.let { mins ->
            if (mins >= 0) {
                autoOffAtMs = if (mins == 0) 0L else System.currentTimeMillis() + mins * 60_000L
            }
        }

        when (intent?.action) {
            ACTION_TORCH_ON -> {
                stopPatterns()
                val level = if (curIntensity <= 0) 1 else curIntensity
                if (controller.setTorchIntensity(level)) {
                    markRunning()
                } else {
                    stopAndExit()
                }
            }
            ACTION_TORCH_OFF -> {
                stopAndExit()
            }
            ACTION_TORCH_UPDATE_INTENSITY -> {
                if (!strobeRunning && !sosRunning) {
                    if (controller.setTorchIntensity(curIntensity)) {
                        markRunning()
                    } else {
                        stopAndExit()
                    }
                }
            }
            ACTION_STROBE_START -> {
                stopPatterns()
                if (controller.isAvailable()) {
                    startStrobe()
                    markRunning()
                } else {
                    stopAndExit()
                }
            }
            ACTION_STROBE_UPDATE -> {
                curIntervalMs = strobeIntervalMs(strobeSpeed)
                if (strobeRunning) restartStrobe()
            }
            ACTION_STROBE_STOP -> {
                stopAndExit()
            }
            ACTION_SOS_START -> {
                stopPatterns()
                if (controller.isAvailable()) {
                    startSos()
                    markRunning()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(
                CH_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
            }
            nm.createNotificationChannel(ch)
        }

        val offIntent = Intent(this, TorchService::class.java).setAction(ACTION_TORCH_OFF)
        val offPendingIntent = PendingIntent.getService(
            this,
            0,
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

    private fun updateNotif() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification())
    }

    private fun markRunning() {
        setServiceActive(true)
        scheduleAutoOffCheck()
        updateNotif()
    }

    private fun stopAndExit() {
        stopAll()
        setServiceActive(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun setServiceActive(active: Boolean) {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ACTIVE, active)
            .apply()
    }

    private fun isAnyModeRunning(): Boolean {
        return strobeRunning || sosRunning || isActive(this)
    }

    private fun stopAll() {
        stopPatterns()
        controller.setTorch(false)
        handler.removeCallbacksAndMessages(null)
    }

    private fun stopPatterns() {
        strobeRunning = false
        sosRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun strobeIntervalMs(speed: Int): Long {
        val s = speed.coerceIn(1, 10)
        val hz = s.toDouble()
        val period = (1000.0 / hz).toLong()
        val min = 30L
        return period.coerceAtLeast(min)
    }

    private fun startStrobe() {
        strobeRunning = true
        strobeLampOn = false
        tickStrobe()
    }

    private fun restartStrobe() {
        if (!strobeRunning) return
        handler.removeCallbacks(strobeTickRunnable)
        tickStrobe()
    }

    private val strobeTickRunnable = object : Runnable {
        override fun run() {
            if (!strobeRunning) return
            strobeLampOn = !strobeLampOn
            controller.setTorchIntensity(if (strobeLampOn) curIntensity.coerceAtLeast(1) else 0)
            handler.postDelayed(this, curIntervalMs / 2)
        }
    }

    private fun tickStrobe() {
        if (!strobeRunning) return
        strobeLampOn = !strobeLampOn
        controller.setTorchIntensity(if (strobeLampOn) curIntensity.coerceAtLeast(1) else 0)
        handler.postDelayed(strobeTickRunnable, curIntervalMs / 2)
    }

    private fun startSos() {
        sosRunning = true
        val dot = 200L
        val dash = 600L
        val gap = 200L
        val wordGap = 1200L

        val pattern = mutableListOf<Pair<Boolean, Long>>().apply {
            repeat(3) { add(true to dot); add(false to gap) }
            repeat(3) { add(true to dash); add(false to gap) }
            repeat(3) { add(true to dot); add(false to gap) }
            add(false to wordGap)
        }

        fun runFrom(index: Int) {
            if (!sosRunning) return
            val (on, dur) = pattern[index]
            controller.setTorchIntensity(if (on) curIntensity.coerceAtLeast(1) else 0)
            handler.postDelayed({ runFrom((index + 1) % pattern.size) }, dur)
        }
        runFrom(0)
    }

    private fun scheduleAutoOffCheck() {
        if (autoOffAtMs == 0L) return
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (autoOffAtMs != 0L && System.currentTimeMillis() >= autoOffAtMs) {
                    stopAndExit()
                } else {
                    handler.postDelayed(this, 1000L)
                }
            }
        }, 1000L)
    }

    private fun mapUiToDeviceIntensity(uiVal: Int, maxApi: Int): Int {
        val u = uiVal.coerceIn(0, 10)
        if (u == 0) return 0
        if (maxApi <= 1) return 1
        val mapped = ((u - 1).toFloat() / 9f) * (maxApi - 1) + 1f
        return mapped.toInt().coerceIn(1, maxApi)
    }
}
