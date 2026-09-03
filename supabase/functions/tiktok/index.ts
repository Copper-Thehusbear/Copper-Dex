/**
 * TikTok Content Posting — โหมด "ดราฟเข้า inbox" (scope: video.upload)
 *
 * ทำไมเป็นดราฟไม่ใช่โพสต์เลย:
 *   video.publish (โพสต์ตรง) ต้องผ่าน audit เต็มของ TikTok ~1–2 สัปดาห์
 *   และถ้ายังไม่ผ่าน คอนเทนต์ถูกบังคับเป็น SELF_ONLY + บัญชีต้อง private
 *   video.upload ส่งเข้า inbox ให้กดโพสต์เองในแอป → เลี่ยงข้อจำกัดพวกนั้นได้
 *
 * ทำไม secret ต้องอยู่ที่นี่:
 *   refresh_token = สิทธิ์โพสต์ในนามเจ้าของบัญชี ห้ามอยู่ใน frontend เด็ดขาด
 *
 * ⚠️ endpoint ของ TikTok ด้านล่างอ้างจากเอกสาร Content Posting API v2
 *    ตอนเขียนเข้า developers.tiktok.com ไม่ได้ (โดน egress proxy บล็อก)
 *    ให้ยิงจริงครั้งแรกแล้วเทียบ error ก่อนใช้งานถาวร
 */
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.45.0';

const TT = {
  authorize: 'https://www.tiktok.com/v2/auth/authorize/',
  token:     'https://open.tiktokapis.com/v2/oauth/token/',
  creator:   'https://open.tiktokapis.com/v2/post/publish/creator_info/query/',
  inboxInit: 'https://open.tiktokapis.com/v2/post/publish/inbox/video/init/',
  status:    'https://open.tiktokapis.com/v2/post/publish/status/fetch/',
  revoke:    'https://open.tiktokapis.com/v2/oauth/revoke/',
};

const SCOPE = 'video.upload';
const PROVIDER = 'tiktok';

const env = (k: string, required = true) => {
  const v = Deno.env.get(k);
  if (!v && required) throw new Error(`ขาด env ${k}`);
  return v ?? '';
};

const db = () => createClient(env('SUPABASE_URL'), env('SUPABASE_SERVICE_ROLE_KEY'), {
  auth: { persistSession: false },
});

const CORS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-app-token, content-type',
  'Access-Control-Allow-Methods': 'POST, GET, OPTIONS',
};

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status, headers: { ...CORS, 'Content-Type': 'application/json' },
  });

const fail = (msg: string, status = 400) => json({ error: msg }, status);

/** ── state ของ OAuth: เซ็นด้วย client secret ไม่ต้องมีตารางเก็บ ── */
async function hmac(input: string) {
  const key = await crypto.subtle.importKey(
    'raw', new TextEncoder().encode(env('TIKTOK_CLIENT_SECRET')),
    { name: 'HMAC', hash: 'SHA-256' }, false, ['sign'],
  );
  const sig = await crypto.subtle.sign('HMAC', key, new TextEncoder().encode(input));
  return [...new Uint8Array(sig)].map(b => b.toString(16).padStart(2, '0')).join('');
}

async function makeState() {
  const ts = Date.now().toString();
  return `${ts}.${(await hmac(ts)).slice(0, 32)}`;
}

async function checkState(state: string | null) {
  if (!state) return false;
  const [ts, sig] = state.split('.');
  if (!ts || !sig) return false;
  if (Date.now() - Number(ts) > 10 * 60_000) return false;   // อายุ 10 นาที
  return (await hmac(ts)).slice(0, 32) === sig;
}

/** ── token ── */
async function loadTokens() {
  const { data, error } = await db().from('integrations').select('*').eq('provider', PROVIDER).maybeSingle();
  if (error) throw new Error(`อ่าน integrations: ${error.message}`);
  return data;
}

async function saveTokens(t: Record<string, unknown>) {
  const { error } = await db().from('integrations')
    .upsert({ provider: PROVIDER, ...t, updated_at: new Date().toISOString() }, { onConflict: 'provider' });
  if (error) throw new Error(`เขียน integrations: ${error.message}`);
}

async function exchange(params: Record<string, string>) {
  const res = await fetch(TT.token, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      client_key: env('TIKTOK_CLIENT_KEY'),
      client_secret: env('TIKTOK_CLIENT_SECRET'),
      ...params,
    }),
  });
  const body = await res.json();
  if (!res.ok || body.error) {
    throw new Error(`TikTok token: ${body.error_description ?? body.error ?? res.status}`);
  }
  return body;
}

/** คืน access_token ที่ยังไม่หมดอายุ — refresh ให้อัตโนมัติ */
async function freshToken() {
  const row = await loadTokens();
  if (!row?.refresh_token) throw new Error('ยังไม่ได้เชื่อมต่อ TikTok');

  const expiresSoon = !row.expires_at || new Date(row.expires_at).getTime() - Date.now() < 60_000;
  if (!expiresSoon) return row.access_token as string;

  const t = await exchange({ grant_type: 'refresh_token', refresh_token: row.refresh_token as string });
  await saveTokens({
    access_token: t.access_token,
    refresh_token: t.refresh_token ?? row.refresh_token,
    expires_at: new Date(Date.now() + Number(t.expires_in ?? 0) * 1000).toISOString(),
  });
  return t.access_token as string;
}

async function ttPost(url: string, token: string, body: unknown) {
  const res = await fetch(url, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json; charset=UTF-8' },
    body: JSON.stringify(body ?? {}),
  });
  const out = await res.json();
  const code = out?.error?.code;
  if (!res.ok || (code && code !== 'ok')) {
    throw new Error(`TikTok: ${out?.error?.message ?? code ?? res.status}`);
  }
  return out;
}

/**
 * กันคนอื่นเรียก endpoint นี้
 * ⚠️ APP_TOKEN ถูกฝังใน bundle ของ SPA จึงเป็น "ความลับระดับเดียวกับ anon key"
 *    ตัวกันจริงคือ Cloudflare Access ที่ครอบหน้าเว็บ อันนี้แค่กันคนสุ่มเจอ URL
 */
const authed = (req: Request) => req.headers.get('x-app-token') === env('APP_TOKEN');

Deno.serve(async req => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: CORS });

  const url = new URL(req.url);
  const route = url.pathname.replace(/^\/functions\/v1\/tiktok/, '').replace(/\/$/, '') || '/';

  try {
    // ── เริ่ม OAuth ────────────────────────────
    if (route === '/auth/start') {
      const p = new URLSearchParams({
        client_key: env('TIKTOK_CLIENT_KEY'),
        response_type: 'code',
        scope: SCOPE,
        redirect_uri: env('TIKTOK_REDIRECT_URI'),
        state: await makeState(),
      });
      return Response.redirect(`${TT.authorize}?${p}`, 302);
    }

    // ── TikTok เด้งกลับมา ──────────────────────
    if (route === '/auth/callback') {
      const back = env('APP_URL');
      const code = url.searchParams.get('code');
      if (!code) return Response.redirect(`${back}/settings?tiktok=denied`, 302);
      if (!(await checkState(url.searchParams.get('state')))) {
        return Response.redirect(`${back}/settings?tiktok=badstate`, 302);
      }

      const t = await exchange({
        grant_type: 'authorization_code',
        code,
        redirect_uri: env('TIKTOK_REDIRECT_URI'),
      });
      await saveTokens({
        open_id: t.open_id,
        access_token: t.access_token,
        refresh_token: t.refresh_token,
        expires_at: new Date(Date.now() + Number(t.expires_in ?? 0) * 1000).toISOString(),
        scopes: t.scope ?? SCOPE,
        connected_at: new Date().toISOString(),
      });

      // ชื่อบัญชีไว้โชว์ในหน้าเว็บ — ล้มก็ไม่เป็นไร ไม่ใช่สาระสำคัญ
      try {
        const info = await ttPost(TT.creator, t.access_token, {});
        await saveTokens({
          display_name: info?.data?.creator_nickname ?? null,
          avatar_url: info?.data?.creator_avatar_url ?? null,
        });
      } catch { /* ข้าม */ }

      return Response.redirect(`${back}/settings?tiktok=ok`, 302);
    }

    if (!authed(req)) return fail('unauthorized', 401);

    // ── ข้อมูลบัญชี + ข้อจำกัดการโพสต์ ─────────
    if (route === '/creator') {
      return json(await ttPost(TT.creator, await freshToken(), {}));
    }

    // ── ขอ upload_url ──────────────────────────
    if (route === '/upload/init') {
      const { video_size } = await req.json();
      const size = Number(video_size);
      if (!Number.isFinite(size) || size <= 0) return fail('video_size ไม่ถูกต้อง');
      // TikTok รับ chunk เดียวได้ถึง 64MB — ใหญ่กว่านั้นต้องหั่น ซึ่งยังไม่รองรับ
      if (size > 64 * 1024 * 1024) {
        return fail('ไฟล์เกิน 64MB — บีบอัดก่อน (ยังไม่รองรับการหั่นหลาย chunk)');
      }

      const out = await ttPost(TT.inboxInit, await freshToken(), {
        source_info: {
          source: 'FILE_UPLOAD',
          video_size: size,
          chunk_size: size,
          total_chunk_count: 1,
        },
      });
      return json({ publish_id: out?.data?.publish_id, upload_url: out?.data?.upload_url });
    }

    // ── เช็คสถานะหลังอัป ───────────────────────
    if (route === '/upload/status') {
      const { publish_id } = await req.json();
      if (!publish_id) return fail('ขาด publish_id');
      const out = await ttPost(TT.status, await freshToken(), { publish_id });
      return json({ status: out?.data?.status, fail_reason: out?.data?.fail_reason ?? null });
    }

    if (route === '/disconnect') {
      const row = await loadTokens();
      if (row?.access_token) {
        try {
          await fetch(TT.revoke, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({
              client_key: env('TIKTOK_CLIENT_KEY'),
              client_secret: env('TIKTOK_CLIENT_SECRET'),
              token: row.access_token as string,
            }),
          });
        } catch { /* ถอนฝั่ง TikTok ไม่ผ่านก็ยังลบฝั่งเราอยู่ดี */ }
      }
      const { error } = await db().from('integrations').delete().eq('provider', PROVIDER);
      if (error) throw new Error(error.message);
      return json({ ok: true });
    }

    return fail(`ไม่รู้จัก route: ${route}`, 404);
  } catch (e) {
    return fail((e as Error).message, 500);
  }
});
