import { createClient } from "npm:@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const supabaseKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const supabase = createClient(supabaseUrl, supabaseKey);

// 允许的 mood 值，防止乱填
const MOODS = ["happy", "sad", "angry", "shy", "sleepy"];

Deno.serve(async (req) => {
  // 只接受 POST
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  let body: {
    text?: string;
    mood?: string;
    key?: string;
  };
  try {
    body = await req.json();
  } catch {
    return new Response("Bad JSON", { status: 400 });
  }

  const text = (body.text || "").toString().slice(0, 200);
  const mood = body.mood || "";
  const key = (body.key || "whale").toString().slice(0, 64);

  // 归一化心情，非法则默认
  const safeMood = MOODS.includes(mood) ? mood : "";

  // state_value 渲染成 pet.html 的 say() 能解析的 JSON
  const stateValue = JSON.stringify({
    text: text,
    mood: safeMood,
  });

  // upsert 到 pet_state，桌宠每 30s 轮询一次
  const { error } = await supabase
    .from("pet_state")
    .upsert(
      { state_key: key, state_value: stateValue, updated_at: new Date().toISOString() },
      { onConflict: "id" } // 若无唯一约束可改为每次新增
    );

  if (error) {
    return Response.json({ ok: false, error: error.message }, { status: 500 });
  }

  return Response.json({ ok: true, pushed: { text, mood } });
});