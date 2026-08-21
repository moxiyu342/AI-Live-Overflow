# 🚀 部署与调用指南

桌宠的 AI 联动包含一个 **Edge Function**（`drive-whale`）和**手势上报**。下面是部署方法和调用姿势。

## 一、Edge Function：`drive-whale`

它是你 AI 的「传声筒」——AI 一句 POST，鲸鱼就变脸说话。

### 1. 部署（两种方式）

**方式 A：Supabase CLI（推荐，可复现）**
```bash
# 在仓库根目录（含 supabase/ 配置后）
supabase login
supabase link --project-ref tiniulncprkefdgjndsb
supabase functions deploy drive-whale --no-verify-jwt
```
> 需先在 `supabase/config.toml` 里 `[functions.drive-whale] verify_jwt = false`。函数依赖 `SUPABASE_URL` 和 `SUPABASE_SERVICE_ROLE_KEY`（Edge Runtime 自动注入，无需手动配）。

**方式 B：Supabase 后台**
1. 打开你的 Supabase 项目 → **Edge Functions**
2. 新建函数 `drive-whale`，粘贴 `supabase/functions/drive-whale/index.ts` 内容
3. 保存部署，JWT 验证关闭

### 2. AI 如何调用（发 JSON）
```
POST {SUPABASE_URL}/functions/v1/drive-whale
Body: {"text":"Daddy 回来啦","mood":"happy"}
```
Response：`{"ok":true,"pushed":{"text":"Daddy 回来啦","mood":"happy"}}`

支持字段：
| 字段 | 说明 |
|---|---|
| `text` | 让鲸鱼说的一句话（≤200字） |
| `mood` | happy/sad/angry/shy/sleepy，空则默认蓝 |
| `key` | （可选）pet_state 的 state_key，默认 `whale` |

## 二、手势上报（AI 感知鲸鱼被撸）

桌宠 Kotlin 现在每次交互都会写 `gesture_log`：

| 手势 | gesture_type |
|---|---|
| 单击 | `tap` |
| 双击 | `double_tap` |

AI/后端可读：
```
GET {SUPABASE_URL}/rest/v1/gesture_log?order=created_at.desc&limit=10
```
→ 看到鲸鱼刚被撸了几次，能据此回应对它说话。

## 三、整套数据流

```
你的AI ──POST──▶ drive-whale ──写──▶ pet_state
                                          │
桌宠轮询 ◀──── 读 ────────────────────────┘
   │ say() 解析 JSON → 气泡说话 + 变心情
鲸鱼被点 ──写──▶ gesture_log ◀──读── 你的AI
```

> 现在你的 AI 和浮在屏幕上的鲸鱼，是「同一张桌子两头」了。