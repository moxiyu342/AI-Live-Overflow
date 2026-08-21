package com.example.deskpet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class PetOverlayService : Service() {

    private var wm: WindowManager? = null
    private var ov: FrameLayout? = null
    private var lp: WindowManager.LayoutParams? = null
    private var wv: WebView? = null
    private var ix = 0
    private var iy = 0
    private var tx = 0f
    private var ty = 0f
    private var moved = false
    private var lastTap = 0L
    private val h = Handler(Looper.getMainLooper())
    private var heat = 0
    private var pollOn = true

    companion object {
        const val CH = "pet_overlay_channel"
        const val BASE = "https://tiniulncprkefdgjndsb.supabase.co"
        const val KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRpbml1bG5jcHJrZWZkZ2puZHNiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODcyNDYwMTcsImV4cCI6MjEwMjgyMjAxN30.kWqUL-d7b1aZeUaYO_e9oOrCpj88O7gIbQGs9LEgsN0"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        n("小鲸鱼来啦")
        setup()
    }

    private fun setup() {
        if (!Settings.canDrawOverlays(this)) { Toast.makeText(this, "要悬浮窗权限哦", Toast.LENGTH_LONG).show(); stopSelf(); return }
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val s = dp(130)
        lp = WindowManager.LayoutParams(
            s, s,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 10; y = 200 }
        ov = FrameLayout(this)
        wv = WebView(this).apply {
            setBackgroundColor(0x00000000)
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            loadUrl("file:///android_asset/pet.html")
        }
        ov?.addView(wv)
        ov?.setOnTouchListener { _, e -> touch(e) }
        wm?.addView(ov, lp)
        lonelyWake()
        poll()
    }

    private fun touch(e: MotionEvent): Boolean {
        val p = lp ?: return false
        when (e.action) {
            MotionEvent.ACTION_DOWN -> { ix = p.x; iy = p.y; tx = e.rawX; ty = e.rawY; moved = false; return true }
            MotionEvent.ACTION_MOVE -> {
                val dx = (e.rawX - tx).toInt(); val dy = (e.rawY - ty).toInt()
                if (Math.abs(dx) > 12 || Math.abs(dy) > 12) moved = true
                if (moved) { p.x = ix + dx; p.y = iy + dy; wm?.updateViewLayout(ov, p) }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!moved) {
                    if (System.currentTimeMillis() - lastTap < 350) { lastTap = 0; js("onDoubleTap"); logGesture("double_tap") }
                    else { lastTap = System.currentTimeMillis(); js("onTap"); logGesture("tap") }
                }
                return true
            }
            else -> return false
        }
    }

    private fun js(fn: String) {
        wv?.post { wv?.evaluateJavascript("window.petEngine&&window.petEngine."+fn+"()", null) }
    }

    private fun jsSay(t: String) {
        wv?.post { wv?.evaluateJavascript("window.petEngine&&window.petEngine.say('"+t+"')", null) }
    }

    private fun lonelyWake() {
        if (!pollOn) return
        heat++
        val m = when { heat <= 6 -> "…"; heat <= 12 -> "好无聊呀>_<"; heat <= 18 -> "嘿别不理我嘛"; else -> "再不理我就沉下去了！" }
        jsSay(m)
        h.postDelayed({ lonelyWake() }, 30000)
    }

    private fun poll() {
        thread {
            try {
                val u = "$BASE/rest/v1/pet_state?select=state_value&order=updated_at.desc&limit=1"
                val cn = URL(u).openConnection() as HttpURLConnection
                cn.requestMethod = "GET"
                cn.setRequestProperty("apikey", KEY)
                val body = cn.inputStream.bufferedReader().use { it.readText() }
                cn.disconnect()
                runOnUi { if (body.isNotBlank() && body != "[]") { val a = org.json.JSONArray(body); if (a.length() > 0) { val v = a.getJSONObject(0).optString("state_value", ""); if (v.isNotBlank()) jsSay(v) } } }
            } catch (_: Exception) {}
            if (pollOn) h.postDelayed({ poll() }, 30000)
        }
    }

    private fun logGesture(g: String) {
        thread {
            try {
                val cn = URL("$BASE/rest/v1/gesture_log").openConnection() as HttpURLConnection
                cn.requestMethod = "POST"
                cn.setRequestProperty("Content-Type", "application/json")
                cn.setRequestProperty("apikey", KEY)
                cn.setRequestProperty("Authorization", "Bearer " + KEY)
                cn.doOutput = true
                cn.outputStream.use { it.write(("{\"gesture_type\":\"" + g + "\"}").toByteArray()) }
                cn.responseCode
                cn.disconnect()
            } catch (_: Exception) {}
        }
    }

    private fun n(t: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CH, "小鲸鱼", NotificationManager.IMPORTANCE_LOW); nm.createNotificationChannel(ch)
            startForeground(1001, Notification.Builder(this, CH).setContentTitle("小鲸鱼桌宠").setContentText(t).setSmallIcon(android.R.drawable.ic_menu_compass).setOngoing(true).build())
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = if (intent?.action == "STOP") { stopMe(); START_NOT_STICKY } else START_STICKY

    private fun stopMe() {
        pollOn = false
        runCatching { ov?.let { wm?.removeView(it) } }
        stopSelf()
    }

    override fun onDestroy() {
        pollOn = false
        super.onDestroy()
    }
}