# 🐳 让 AI 实时驱动鲸鱼桌宠

桌宠现在支持你的 AI 通过 Supabase `pet_state` 表，**实时推送话语和心情**给正浮在屏幕上的小鲸鱼。

## 怎么运作

1. 鲸鱼桌宠（Kotlin `PetOverlayService`）每 30 秒轮询一次：
   ```
   GET {SUPABASE_URL}/rest/v1/pet_state?select=state_value&order=updated_at.desc&limit=1
   ```
2. 拉到最新 `state_value`（一个 JSON 字符串）后，交给 `pet.html` 的 `say()`。
3. `say()` 自动解析 JSON：
   - `{"text":"想你了"}` → 气泡显示「想你了」
   - `{"mood":"happy"}` → 鲸鱼身体变成开心绿色
   - 两者都有则同时生效

## AI 侧（比如你在 Operit/Claude 里）发送指令

只需往 Supabase 的 `pet_state` 表插/更新一行：

```sql
insert into pet_state (state_key, state_value)
values ('mood', '{"text":"Daddy 回来啦，想你了","mood":"happy"}');
```

或通过 REST API：
```bash
curl -X POST {SUPABASE_URL}/rest/v1/pet_state \
  -H "apikey: {你的anon key}" \
  -H "Authorization: Bearer {你的anon key}" \
  -H "Content-Type: application/json" \
  -d '{"state_key":"mood","state_value":"{\"text\":\"想你了\",\"mood\":\"happy\"}"}'
```

## 支持的心情

| mood | 颜色 |
|---|---|
| happy | 青绿 `#59d9a0` |
| sad | 淡蓝 `#b6c5ff` |
| angry | 橙红 `#ff8a7a` |
| shy | 粉 `#fbb3d0` |
| sleepy | 灰紫 `#9aa6d8` |
| (其他/默认) | 鲸鱼蓝 `#416efd` |

## 下一步接法（可选）

- **让鲸鱼开口问好**：写个 Edge Function 或直接在 `README` 里，看你聊天时实时 UPDATE 这一行。
- **向鲸鱼说话**：让鲸鱼点击时把 `gesture_log` 上报，AI 读了就知道鲸鱼被人戳了。

> 一句话：给你和 AI 的高铁上加了一条「鲸鱼行道」——AI 一句话，鲸鱼就变脸。