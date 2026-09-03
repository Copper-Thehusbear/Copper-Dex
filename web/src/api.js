import { sb, configured } from './supabase.js';
import { taskWeek } from './lib/taxonomy.js';

/**
 * ทุก DB call ของหน้าเว็บอยู่ในไฟล์นี้ไฟล์เดียว
 * (กติกาเดียวกับ src/db.js ฝั่ง Node — ย้าย backend ทีหลังแก้ที่เดียว)
 */

function guard() {
  if (!configured) {
    throw new Error('ยังไม่ได้ตั้ง VITE_SUPABASE_URL / VITE_SUPABASE_ANON_KEY');
  }
}

async function run(query, label) {
  guard();
  const { data, error } = await query;
  if (error) throw new Error(`${label}: ${error.message}`);
  return data ?? [];
}

export const api = {
  // ── This Week ──────────────────────────────
  tasks(week = taskWeek()) {
    return run(
      sb.from('tasks').select('*').eq('week', week).order('done').order('priority'),
      'โหลด tasks',
    );
  },

  toggleTask(id, done) {
    return run(sb.from('tasks').update({ done }).eq('id', id).select(), 'อัปเดต task');
  },

  /** conversion ตั้งแต่วันที่กำหนด — ใช้คิด KPI */
  conversionsSince(iso) {
    return run(
      sb.from('conversions').select('commission,purchase_time,sub_id2,item_id').gte('purchase_time', iso),
      'โหลด conversions',
    );
  },

  contentsSince(iso) {
    return run(
      sb.from('contents').select('content_id,published_at,angle').gte('published_at', iso),
      'โหลด contents',
    );
  },

  // ── Radar ──────────────────────────────────
  products() {
    return run(
      sb.from('products').select('*').order('status').order('my_score', { ascending: false, nullsFirst: false }),
      'โหลด products',
    );
  },

  commissionTrend() {
    return run(sb.from('v_commission_trend').select('*'), 'โหลด commission trend');
  },

  /** แก้ได้เฉพาะ 4 คอลัมน์นี้ — RLS กันไม่ให้แตะข้อมูลที่ ingest เขียน */
  updateProduct(itemId, patch) {
    const allowed = ['status', 'my_score', 'note', 'retire_reason'];
    const body = Object.fromEntries(Object.entries(patch).filter(([k]) => allowed.includes(k)));
    return run(sb.from('products').update(body).eq('item_id', itemId).select(), 'อัปเดตสินค้า');
  },

  // ── Portfolio ──────────────────────────────
  anglePerformance() {
    return run(sb.from('v_angle_performance').select('*'), 'โหลด angle performance');
  },

  productPerformance() {
    return run(sb.from('v_product_performance').select('*'), 'โหลด product performance');
  },

  // ── Content Log ────────────────────────────
  contents() {
    return run(
      sb.from('contents').select('*').order('published_at', { ascending: false, nullsFirst: false }),
      'โหลด contents',
    );
  },

  saveContent(row) {
    return run(sb.from('contents').upsert(row, { onConflict: 'content_id' }).select(), 'บันทึกคลิป');
  },

  deleteContent(contentId) {
    return run(sb.from('contents').delete().eq('content_id', contentId).select(), 'ลบคลิป');
  },

  /** commission ต่อ content_id — ใช้โชว์ในหน้า Content Log */
  commissionByContent() {
    return run(sb.from('conversions').select('sub_id2,commission'), 'โหลด commission');
  },
};
