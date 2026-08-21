package com.example.deskpet

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val title = TextView(this).apply {
            text = "小鲸鱼桌宠"
            textSize = 22f
        }
        val status = TextView(this).apply {
            text = "未启动"
            textSize = 16f
            setPadding(0, 16, 0, 32)
        }
        val btnStart = Button(this).apply { text = "启动桌宠" }
        val btnStop = Button(this).apply { text = "停止桌宠" }
        val btnPerm = Button(this).apply { text = "授权悬浮窗/通知" }

        root.addView(title)
        root.addView(status)
        root.addView(btnStart)
        root.addView(btnStop)
        root.addView(btnPerm)

        btnPerm.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else {
                Toast.makeText(this, "悬浮窗已授权", Toast.LENGTH_SHORT).show()
            }
        }

        btnStart.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请先授权悬浮窗", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startForegroundService(Intent(this, PetOverlayService::class.java))
            status.text = "运行中"
            Toast.makeText(this, "让小鲸鱼爬上来啦", Toast.LENGTH_SHORT).show()
        }

        btnStop.setOnClickListener {
            startService(Intent(this, PetOverlayService::class.java).setAction("STOP"))
            status.text = "已停止"
            Toast.makeText(this, "小鲸鱼先走啦", Toast.LENGTH_SHORT).show()
        }

        setContentView(root)
    }
}