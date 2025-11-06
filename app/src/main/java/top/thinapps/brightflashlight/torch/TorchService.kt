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
        const val ACTION_SOS_START = "act_sos_start"
        const val ACTION_SOS_STOP = "act_sos_stop"

        private const val CH_ID = "flashlight"
        private const val NOTIF_ID = 42
    }

    private lateinit var controller: TorchController
    private val handler = Handler(Looper.getMainLooper())

    private var strobeRunning = false
    private var sosRunning = false
    private var strobeSpeed = 10 // user slider 5..20 (mapped to interval)
    private var autoOffAtMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        controller = TorchController(applicationContext)
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onDestroy() {
        stopAll()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getIntExtra("strobeSpeed", -1)?.let { if (it >= 0) strobeSpeed = it }
        intent?.getIntExtra("autoOffMinutes", -1)?.let { mins ->
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

    private fun startStrobe() {
        strobeRunning = true
        // map user speed (5..20) to interval (~500ms..30ms)
        val minMs = 30L
        val maxMs = 500L
        val step = (20 - strobeSpeed).coerceIn(0, 15) // 0..15
        val interval = (minMs + (step * ((maxMs - minMs) / 15f))).toLong()

        fun tick(on: Boolean) {
            if (!strobeRunning) return
            controller.setTorch(on)
            handler.postDelayed({ tick(!on) }, interval)
        }
        tick(true)
    }

    private fun startSos() {
        sosRunning = true
        // sos pattern: ... --- ...
        val dot = 200L
        val dash = 600L
        val gap = 200L
        val letterGap = 600L
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
