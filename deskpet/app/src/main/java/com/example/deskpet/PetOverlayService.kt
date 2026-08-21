package com.example.deskpet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import java.util.Calendar
import kotlin.concurrent.thread

class PetOverlayService : Service() {
    private var wm: WindowManager?=null
    private var ov: FrameLayout?=null
    private var lp: WindowManager.LayoutParams?=null
    private var wv: WebView?=null
    private var ix=0; private var iy=0; private var tx=0f; private var ty=0f; private var moved=false
    private var lastTap=0L; private var touchStart=0L; private var tapCtr=0; private var heatScore=0
    private var curApp=""; private var scRicevr: BroadcastReceiver?=null
    companion object {
        const val CH="pet_overlay_channel"
        const val BASE="https://tiniulncprkefdgjndsb.supabase.co"
        const val KEY="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRpbml1bG5jcHJrZWZkZ2puZHNiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODcyNDYwMTcsImV4cCI6MjEwMjgyMjAxN30.kWqUjbOlncDm4pY7-ayCUNr1qAarCJzHTLQaUJfv89n8"
        const val DT=300L; const val LPT=600L; const val MV=12
    }
    override fun onBind(i:Intent?):IBinder?=null
    override fun onCreate(){super.onCreate(); notif("小鲸鱼来啦"); setup()}
    override fun onStartCommand(i:Intent?,f:Int,id:Int):Int = if(i?.action=="STOP"){stopMe();START_NOT_STICKY}else{START_STICKY}
    private fun dp(v:Int):Int=(v*resources.displayMetrics.density).toInt()
    private fun setup(){
        if(Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(this)){Toast.makeText(this,"要悬浮窗权限哦",Toast.LENGTH_LONG).show();stopMe();return}
        wm=getSystemService(WINDOW_SERVICE) as WindowManager
        val s=dp(130)
        lp=WindowManager.LayoutParams(s,s,if(Build.VERSION.SDK_INT>=26)WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START;x=10;y=200}
        ov=FrameLayout(this); wv=WebView(this).apply{setBackgroundColor(0);settings.javaScriptEnabled=true;settings.allowFileAccess=true;loadUrl("file:///android_asset/pet.html")}
        ov?.addView(wv); ov?.setOnTouchListener{_,e->touch(e)}; wm?.addView(ov,lp)
        lonely(); poll(); appWatch()
    }
    private fun touch(e:MotionEvent):Boolean{ val p=lp?:return false; when(e.action){
        MotionEvent.ACTION_DOWN->{ix=p.x;iy=p.y;tx=e.rawX;ty=e.rawY;moved=false;touchStart=System.currentTimeMillis();true}
        MotionEvent.ACTION_MOVE->{val dx=(e.rawX-tx).toInt();val dy=(e.rawY-ty).toInt();if(Math.abs(dx)>MV||Math.abs(dy)>MV)moved=true
            if(moved){p.x=ix+dx;p.y=iy+dy;wm?.updateViewLayout(ov,p)};true}
        MotionEvent.ACTION_UP->{val el=System.currentTimeMillis()-touchStart
            if(!moved){ when{ el>LPT->{js("onLongPress");logGesture("long_press")} System.currentTimeMillis()-lastTap<DT->{lastTap=0;tapCtr++;js("onDoubleTap");logGesture("double_tap")} else->{lastTap=System.currentTimeMillis();tapCtr++;js("onTap");logGesture("tap")} }
                if(tapCtr in listOf(3,5,8,10)){js("onTap");logGesture("combo_"+tapCtr)} }else{ val v=Math.sqrt((e.rawX-tx)*(e.rawX-tx)+(e.rawY-ty)*(e.rawY-ty)).toInt(); if(v>200&&el<400){js("onFling");logGesture("fling")}else{js("onDragEnd")} };true}
        else->false } }
    private fun js(fn:String){wv?.post{wv?.evaluateJavascript("window.petEngine&&window.petEngine."+fn+"()",null)}}
    private fun say(t:String){wv?.post{wv?.evaluateJavascript("window.petEngine&&window.petEngine.say('"+t+"')",null)}}
    private fun lonely(){heatScore++;val m=when{heatScore<=5->"…";heatScore<=12->"好无聊呀>_<";heatScore<=18->"嘿~掐我一下嘛";else->"再不理我要生气啦！"};say(m);Handler(Looper.getMainLooper()).postDelayed({lonely()},30000)}
    private fun appWatch(){thread{curApp=fore();val map=mapOf("com.tencent.mm" to "在聊微信都不理我","com.ss.android.ugc.aweme" to "刷抖音这么起劲","com.zhihu.android" to "刷知乎不看我","com.netease.cloudmusic" to "听歌也不带我");val m=map[curApp]?:"";if(m.isNotBlank())say(m);Handler(Looper.getMainLooper()).postDelayed({appWatch()},30000)}}
    private fun fore():String=try{val usm=getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager;val now=System.currentTimeMillis();(usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,now-3600000,now).maxByOrNull{it.lastTimeUsed}?.packageName)?:""}catch(_:Exception){""}
    private fun poll(){thread{try{val c=URL("$BASE/rest/v1/pet_state?select=state_value&order=updated_at.desc&limit=1").openConnection() as HttpURLConnection;c.requestMethod="GET";c.setRequestProperty("apikey",KEY);val b=c.inputStream.bufferedReader().use{it.readText()};c.disconnect();if(b.isNotBlank()&&b!="[]"){val a=org.json.JSONArray(b);if(a.length()>0)say(a.getJSONObject(0).optString("state_value",""))}}catch(_:Exception){}};if(pollOn)Handler(Looper.getMainLooper()).postDelayed({poll()},30000)}
    private fun log(g:String){thread{try{val c=URL("$BASE/rest/v1/gesture_log").openConnection() as HttpURLConnection;c.requestMethod="POST";c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("apikey",KEY);c.setRequestProperty("Authorization","Bearer "+KEY);c.doOutput=true;c.outputStream.use{it.write(("{"gesture_type":""+g+""}").toByteArray())};c.responseCode;c.disconnect()}catch(_:Exception){}}}
    private fun notif(t:String){try{val nm=getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(NotificationChannel(CH,"小鲸鱼",NotificationManager.IMPORTANCE_LOW));startForeground(1001,Notification.Builder(this,CH).setContentTitle("小鲸鱼桌宠").setContentText(t).setSmallIcon(android.R.drawable.ic_menu_compass).setOngoing(true).build())}catch(_:Exception){}}
    private fun stopMe(){ pollOn=false; runCatching{ov?.let{wm?.removeView(it)}}; stopSelf() }
}
