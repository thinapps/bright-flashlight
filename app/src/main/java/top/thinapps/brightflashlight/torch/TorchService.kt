package top.thinapps.brightflashlight.torch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
        const val ACTION_STROBE_START = "act_strobe_start"
        const val ACTION_STROBE_STOP = "act_strobe_stop"
        const val ACTION_STROBE_UPDATE = "act_strobe_update"
        const val ACTION_SOS_START = "act_sos_start"
        const val ACTION_SOS_STOP = "act_sos_stop"

        private const val CH_ID = "flashlight"
        private const val NOTIF_ID = 42
        private const val EXTRA_STROBE_SPEED = "strobeSpeed"
        private const val EXTRA_AUTO_OFF_MINUTES = "autoOffMinutes"
    }

    private lateinit var controller: TorchController
    private val handler = Handler(Looper.getMainLooper())

    private var strobeRunning = false
    private var sosRunning = false
    private var strobeSpeed = 5 // user slider 1..10
    private var curIntervalMs: Long = 100L
    private var autoOffAtMs: Long = 0L

    // local lamp state for strobe loop (avoids needing controller.isTorchOn())
    private var strobeLampOn = false

    override fun onCreate() {
        super.onCreate()
        controller = TorchController(applicationContext)
        curIntervalMs = strobeIntervalMs(strobeSpeed)
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onDestroy() {
        stopAll()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getIntExtra(EXTRA_STROBE_SPEED, -1)?.let { if (it >= 0) {
            strobeSpeed = it
            curIntervalMs = strobeIntervalMs(strobeSpeed)
        } }
        intent?.getIntExtra(EXTRA_AUTO_OFF_MINUTES, -1)?.let { mins ->
            if (mins >= 0) {
                autoOffAtMs = if (mins == 0) 0L else System.currentTimeMillis() + mins * 60_000L
            }
        }

        when (intent?.action) {
            ACTION_TORCH_ON -> {
                stopPatterns()
                controller.setTorch(true)
                scheduleAutoOffCheck()
                updateNotif()
            }
            ACTION_TORCH_OFF -> {
                stopAll()
                updateNotif()
            }
            ACTION_STROBE_START -> {
                stopPatterns()
                startStrobe()
                scheduleAutoOffCheck()
                updateNotif()
            }
            ACTION_STROBE_UPDATE -> {
                // apply new speed live
                curIntervalMs = strobeIntervalMs(strobeSpeed)
                if (strobeRunning) restartStrobe()
            }
            ACTION_STROBE_STOP -> {
                stopPatterns()
                controller.setTorch(false)
                updateNotif()
            }
            ACTION_SOS_START -> {
                stopPatterns()
                startSos()
                scheduleAutoOffCheck()
                updateNotif()
            }
            ACTION_SOS_STOP -> {
                stopPatterns()
                controller.setTorch(false)
                updateNotif()
            }
            else -> { /* no-op */ }
        }
        return START_STICKY
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
        return NotificationCompat.Builder(this, CH_ID)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_power)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_running))
            .build()
    }

    private fun updateNotif() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification())
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

    // map user speed (1..10) to a sensible interval (fast at 10, slow at 1)
    private fun strobeIntervalMs(speed: Int): Long {
        val s = speed.coerceIn(1, 10)
        // simple Hz mapping: 1..10 Hz -> 1000ms..100ms period (half on, half off)
        val hz = s.toDouble()
        val period = (1000.0 / hz).toLong()  // total cycle
        val min = 30L
        return period.coerceAtLeast(min)
    }

    private fun startStrobe() {
        strobeRunning = true
        strobeLampOn = false
        tickStrobe() // begin loop
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
            controller.setTorch(strobeLampOn)
            handler.postDelayed(this, curIntervalMs / 2)
        }
    }

    private fun tickStrobe() {
        if (!strobeRunning) return
        strobeLampOn = !strobeLampOn
        controller.setTorch(strobeLampOn)
        handler.postDelayed(strobeTickRunnable, curIntervalMs / 2)
    }

    private fun startSos() {
        sosRunning = true
        val dot = 200L
        val dash = 600L
        val gap = 200L
        val wordGap = 1200L

        val pattern = mutableListOf<Pair<Boolean, Long>>().apply {
            repeat(3) { add(true to dot); add(false to gap) }  // ...
            repeat(3) { add(true to dash); add(false to gap) } // ---
            repeat(3) { add(true to dot); add(false to gap) }  // ...
            add(false to wordGap)
        }

        fun runFrom(index: Int) {
            if (!sosRunning) return
            val (on, dur) = pattern[index]
            controller.setTorch(on)
            handler.postDelayed({ runFrom((index + 1) % pattern.size) }, dur)
        }
        runFrom(0)
    }

    private fun scheduleAutoOffCheck() {
        if (autoOffAtMs == 0L) return
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (autoOffAtMs != 0L && System.currentTimeMillis() >= autoOffAtMs) {
                    stopAll()
                    stopSelf()
                } else {
                    handler.postDelayed(this, 1000L)
                }
            }
        }, 1000L)
    }
}
