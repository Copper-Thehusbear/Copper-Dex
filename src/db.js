import { createClient } from '@supabase/supabase-js';

/**
 * Adapter layer — สคริปต์ทุกตัวคุยกับไฟล์นี้เท่านั้น
 * วันที่อยากย้ายไป D1 / Neon / Sheets แก้ไฟล์เดียวจบ
 */

const url = process.env.SUPABASE_URL;
const key = process.env.SUPABASE_SERVICE_KEY;
if (!url || !key) throw new Error('ขาด SUPABASE_URL / SUPABASE_SERVICE_KEY');

const sb = createClient(url, key, { auth: { persistSession: false } });

const CHUNK = 500;

async function upsertChunked(table, rows, onConflict) {
  let written = 0;
  for (let i = 0; i < rows.length; i += CHUNK) {
    const slice = rows.slice(i, i + CHUNK);
    const { error } = await sb.from(table).upsert(slice, { onConflict });
    if (error) throw new Error(`upsert ${table}: ${error.message}`);
    written += slice.length;
  }
  return written;
}

export const db = {
  /** products — upsert แบบไม่ทับ status/my_score/note ที่คุณแก้เอง */
  async upsertProducts(rows) {
    if (!rows.length) return 0;
    const payload = rows.map(r => ({ ...r, updated_at: new Date().toISOString() }));
    return upsertChunked('products', payload, 'item_id');
  },

  /** snapshots — PK (item_id, snap_date) → rerun วันเดิมทับตัวเอง ไม่มีแถวซ้ำ */
  async saveSnapshots(rows) {
    if (!rows.length) return 0;
    return upsertChunked('product_snapshots', rows, 'item_id,snap_date');
  },

  /** conversions — PK (conversion_id, item_id) → ดึงทับช่วงเดิมได้ปลอดภัย */
  async saveConversions(rows) {
    if (!rows.length) return 0;
    return upsertChunked('conversions', rows, 'conversion_id,item_id');
  },

  async saveLink(row) {
    const { error } = await sb.from('links').upsert(row, { onConflict: 'content_id,item_id' });
    if (error) throw new Error(`saveLink: ${error.message}`);
  },

  async getConfig(key) {
    const { data, error } = await sb.from('config').select('value').eq('key', key).single();
    if (error) throw new Error(`getConfig ${key}: ${error.message}`);
    return data.value;
  },

  async getActiveItemIds() {
    const { data, error } = await sb.from('products').select('item_id').eq('status', 'active');
    if (error) throw new Error(`getActiveItemIds: ${error.message}`);
    return data.map(r => r.item_id);
  },

  /** conversion ที่ไม่มี sub_id2 = แถวที่ผูกกลับไปหาคลิปไม่ได้ */
  async countConversionsWithoutContent() {
    const { count, error } = await sb.from('conversions')
      .select('*', { count: 'exact', head: true }).is('sub_id2', null);
    if (error) throw new Error(`countConversionsWithoutContent: ${error.message}`);
    return count;
  },

  async count(table) {
    const { count, error } = await sb.from(table).select('*', { count: 'exact', head: true });
    if (error) throw new Error(`count ${table}: ${error.message}`);
    return count;
  },
};
