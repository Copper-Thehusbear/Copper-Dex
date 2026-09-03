/**
 * ฝั่ง browser ของการอัปคลิปขึ้น TikTok
 *
 * ไฟล์วิดีโอไม่ผ่าน server ของเราเลย — Edge Function แค่ขอ upload_url
 * แล้ว browser ยิงไฟล์เข้า TikTok ตรงๆ (ไม่กิน Supabase Storage ที่มีแค่ 1GB)
 */

const FN = import.meta.env.VITE_TIKTOK_FN_URL ?? '';
const APP_TOKEN = import.meta.env.VITE_APP_TOKEN ?? '';

export const tiktokConfigured = Boolean(FN && APP_TOKEN);

/** TikTok รับ chunk เดียวได้ถึงเท่านี้ ใหญ่กว่านี้ต้องหั่น ซึ่งยังไม่รองรับ */
export const MAX_BYTES = 64 * 1024 * 1024;

export const connectUrl = () => `${FN}/auth/start`;

async function call(path, body) {
  if (!tiktokConfigured) throw new Error('ยังไม่ได้ตั้ง VITE_TIKTOK_FN_URL / VITE_APP_TOKEN');
  const res = await fetch(`${FN}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'x-app-token': APP_TOKEN },
    body: JSON.stringify(body ?? {}),
  });
  const out = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(out.error ?? `เรียก ${path} ไม่สำเร็จ (${res.status})`);
  return out;
}

export const creatorInfo = () => call('/creator');
export const disconnect  = () => call('/disconnect');
export const checkStatus = publish_id => call('/upload/status', { publish_id });

/**
 * ส่งไฟล์เข้า inbox ของ TikTok เป็นดราฟ
 * onStep บอกความคืบหน้าให้ UI: init → upload → done
 */
export async function uploadDraft(file, onStep = () => {}) {
  if (!file) throw new Error('ยังไม่ได้เลือกไฟล์');
  if (file.size > MAX_BYTES) {
    throw new Error(`ไฟล์ ${(file.size / 1048576).toFixed(1)}MB เกิน 64MB — บีบอัดก่อน`);
  }

  onStep('init');
  const { publish_id, upload_url } = await call('/upload/init', { video_size: file.size });
  if (!upload_url) throw new Error('TikTok ไม่ได้ส่ง upload_url กลับมา');

  onStep('upload');
  let res;
  try {
    res = await fetch(upload_url, {
      method: 'PUT',
      headers: {
        'Content-Type': file.type || 'video/mp4',
        'Content-Range': `bytes 0-${file.size - 1}/${file.size}`,
      },
      body: file,
    });
  } catch (e) {
    // ถ้า TikTok ไม่เปิด CORS ให้ browser ยิงตรง จะตกมาที่นี่
    throw new Error(
      `อัปไฟล์ตรงเข้า TikTok ไม่ผ่าน (${e.message}) — ` +
      'อาจเป็นเพราะ CORS ต้องเปลี่ยนไปส่งผ่าน Edge Function แทน',
    );
  }
  if (!res.ok) throw new Error(`TikTok ปฏิเสธไฟล์: HTTP ${res.status}`);

  onStep('done');
  return { publish_id };
}
