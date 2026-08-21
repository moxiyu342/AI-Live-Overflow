# 鲸鱼桌宠 · 快速启动

这是把 `AI-Live-Overflow` 蓝图落成一只 **DeepSeek 风格小鲸鱼桌宠** 的配套工程。

## 已就绪

| 项 | 位置 |
|---|---|
| 鲸鱼形象 | `assets/pet-whale.svg`（透明底、白眼睛白肚皮） |
| 可交互桌宠页 | `assets/pet.html`（SVG 渲染 + 上浮动画 + 点击对话 + 双击脸红） |
| Kotlin 悬浮窗骨架 | `examples/ExampleOverlayService.kt`（前后台服务 + 手势） |
| 后端同步 | Supabase 三张表：`gesture_log` / `app_usage` / `pet_state` |

## 2. 改造示例机器人加载鲸鱼

在 `ExampleOverlayService.kt` 里把：

```kotlin
loadUrl("file:///android_asset/pet.html")
```

指向 `assets/pet.html`（或用 `asset/` 放到 app 的 assets 目录）。

## 3. 接入 Supabase 后端

项目：`tiniulncprkefdgjndsb`
URL：`https://tiniulncprkefdgjndsb.supabase.co`

写时（来自 Android）：
```kotlin
private fun postToSupabase(table: String, body: JSONObject) {
    scope.launch {
        try {
            val url = URL("$SUPABASE_URL/rest/v1/$table")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("apikey", SUPABASE_KEY)
            conn.setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            conn.responseCode
            conn.disconnect()
        } catch (_: Exception) {}
    }
}
```

读时（桌宠轮询 AI 下发的状态）：
```javascript
async function pollState() {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/pet_state?order=updated_at.desc&limit=1`, {
    headers: { apikey: SUPABASE_KEY }
  });
  const data = await res.json();
  // 把 data[0].state_value 应用到桌宠
}
setInterval(pollState, 30000); // 每 30s
```

## 4. 权限（Manifest）

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
```

## 5. 交互

- 单击：随机说句话
- 双击：脸红一下
- 长按（WebView 里触发）：喂喂其他动作
- 拖拽：由 Kotlin 手势处理器完成移动

> 开抽屉聊你的 AI——一只会浮在你屏幕上的小鲸鱼。