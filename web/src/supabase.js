import { createClient } from '@supabase/supabase-js';

const url = import.meta.env.VITE_SUPABASE_URL;
const key = import.meta.env.VITE_SUPABASE_ANON_KEY;

export const configured = Boolean(url && key);

/**
 * anon key เท่านั้น — service_role ข้าม RLS ทั้งหมด ห้ามโผล่ใน frontend เด็ดขาด
 * ตัวกันคนอื่นเข้าเว็บคือ Cloudflare Zero Trust Access ไม่ใช่ตัวคีย์
 */
export const sb = configured
  ? createClient(url, key, { auth: { persistSession: false } })
  : null;
