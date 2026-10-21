package com.kairoai.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.kairoai.app.MainActivity
import com.kairoai.app.R

class IslandOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var pill: LinearLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var statusText: TextView? = null
    private var mascotImage: ImageView? = null
    private val imageLoader by lazy {
        ImageLoader.Builder(this).components {
            if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
        }.build()
    }
    private val collapseTask = Runnable { animateToWidth(dp(prefsWidth() * .76f)) }

    private val prefs get() = getSharedPreferences("kairo_settings", MODE_PRIVATE)

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { removePill(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY }
            ACTION_RESIZE -> { if (pill != null) resizePill() else stopSelf(); return START_NOT_STICKY }
            ACTION_UPDATE -> {
                if (pill == null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
                    startForeground(NOTIFICATION_ID, buildNotification())
                    ensurePill()
                }
                updatePill(intent?.getStringExtra(EXTRA_STATE).orEmpty(), intent?.getStringExtra(EXTRA_MESSAGE).orEmpty())
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
                startForeground(NOTIFICATION_ID, buildNotification())
                ensurePill()
                updatePill("ready", "Kairo hazır")
            }
        }
        return START_NOT_STICKY
    }

    private fun ensurePill() {
        if (pill != null) { resizePill(); return }
        val avatar = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xFF211D33.toInt()) }
        }
        mascotImage = avatar
        showMascot("idle")
        val label = TextView(this).apply {
            text = "KAIRO  ·  HAZIR"
            textSize = 10f
            setTextColor(0xFFF4F3FA.toInt())
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = .06f
            maxLines = 1
        }
        statusText = label
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(16), dp(6))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(prefs.getInt("island_height", 54).toFloat()) / 2f
                setColor(0xF308090D.toInt())
                setStroke(dp(1), 0xFF292633.toInt())
            }
            elevation = dp(10).toFloat()
            addView(avatar, LinearLayout.LayoutParams(dp(34), dp(34)))
            addView(label, LinearLayout.LayoutParams(0, dp(34), 1f).apply { leftMargin = dp(10) })
            setOnClickListener {
                startActivity(Intent(this@IslandOverlayService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            }
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            dp(prefsWidth()), dp(prefs.getInt("island_height", 54).toFloat()), type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(prefs.getInt("island_top", 16).toFloat())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        try { windowManager.addView(row, params); pill = row; layoutParams = params }
        catch (_: Exception) { stopSelf() }
    }

    private fun updatePill(kind: String, message: String) {
        val label = when (kind) {
            "thinking" -> "GEMINI  ·  İSTEK İŞLENİYOR"
            "streaming" -> "GEMINI  ·  CANLI YANIT"
            "done" -> "TAMAMLANDI  ·  ${message.take(28)}"
            "approval" -> "ONAY GEREKİYOR  ·  ${message.take(22)}"
            "error" -> "HATA  ·  ${message.take(30)}"
            "offline" -> "ÇEVRİMDIŞI  ·  BAĞLANTIYI KONTROL ET"
            "idle" -> "KAIRO  ·  KAPALI"
            else -> "KAIRO  ·  ${message.take(34)}"
        }
        statusText?.text = label
        val mood = when (kind) { "thinking", "streaming" -> "thinking"; "done" -> "done"; "approval" -> "question"; "error", "offline" -> "error"; else -> "idle" }
        showMascot(mood)
        val expanded = kind == "thinking" || kind == "streaming" || kind == "approval" || kind == "error" || kind == "offline" || kind == "done"
        animateToWidth(dp(if (expanded) prefsWidth() else prefsWidth() * .76f))
        handler.removeCallbacks(collapseTask)
        if (kind == "done" || kind == "error" || kind == "offline") handler.postDelayed(collapseTask, 6_500)
    }

    private fun showMascot(mood: String) {
        val image = mascotImage ?: return
        val safeMood = mood.takeIf { it in setOf("idle", "thinking", "done", "error", "question") } ?: "idle"
        imageLoader.enqueue(ImageRequest.Builder(this).data("file:///android_asset/mascot_${safeMood}.gif").target(image).build())
    }

    private fun resizePill() {
        val view = pill ?: return
        val params = layoutParams ?: return
        params.height = dp(prefs.getInt("island_height", 54).toFloat())
        params.y = dp(prefs.getInt("island_top", 16).toFloat())
        params.width = dp(prefsWidth())
        (view.background as? GradientDrawable)?.cornerRadius = dp(prefs.getInt("island_height", 54).toFloat()) / 2f
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun animateToWidth(target: Int) {
        val view = pill ?: return
        val params = layoutParams ?: return
        val start = params.width
        if (start == target) return
        android.animation.ValueAnimator.ofInt(start, target).apply {
            duration = 280
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                params.width = animation.animatedValue as Int
                runCatching { windowManager.updateViewLayout(view, params) }
            }
            start()
        }
    }

    private fun removePill() {
        handler.removeCallbacksAndMessages(null)
        pill?.let { runCatching { windowManager.removeView(it) } }
        pill = null; layoutParams = null
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, CHANNEL_ID) else Notification.Builder(this)
        return builder.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Kairo canlı ada açık")
            .setContentText("Durum bildirimlerini göstermek için dokun")
            .setContentIntent(open).setOngoing(true).build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Kairo canlı ada", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun prefsWidth() = prefs.getInt("island_width", 292).toFloat()
    private fun dp(value: Float) = (value * resources.displayMetrics.density).toInt()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onDestroy() { removePill(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.kairoai.app.overlay.START"
        const val ACTION_STOP = "com.kairoai.app.overlay.STOP"
        const val ACTION_UPDATE = "com.kairoai.app.overlay.UPDATE"
        const val ACTION_RESIZE = "com.kairoai.app.overlay.RESIZE"
        const val EXTRA_STATE = "state"
        const val EXTRA_MESSAGE = "message"
        private const val CHANNEL_ID = "kairo_island"
        private const val NOTIFICATION_ID = 1817
    }
}
