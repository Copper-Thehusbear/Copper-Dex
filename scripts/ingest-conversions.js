/**
 * ดึง conversionReport
 *
 * ค่าเริ่มต้นดึง 7 วันล่าสุด (ทับของเดิม) เพราะสถานะออเดอร์เปลี่ยนได้
 * backfill:  node scripts/ingest-conversions.js --days=180
 *
 * ข้อจำกัดที่ต้องเคารพ:
 *  - ช่วงเวลาเกิน ~90 วัน → error 11001 จึงต้องหั่นเป็น chunk
 *  - scrollId หมดอายุถ้าห่างกันเกิน 30 วินาที → ห้ามใส่ sleep ยาวระหว่างหน้า
 */
import { gql, Q_CONVERSION } from '../src/shopee.js';
import { mapConversionRows } from '../src/map.js';
import { db } from '../src/db.js';

const DAY = 86400;
const CHUNK_DAYS = 85;          // กันชนจาก 90
const PAGE_LIMIT = 100;
const MAX_PAGES = 200;

const arg = process.argv.find(a => a.startsWith('--days='));
const days = Number(arg?.split('=')[1] ?? 7);

const now = Math.floor(Date.now() / 1000);
const from = now - days * DAY;

async function fetchWindow(start, end) {
  const nodes = [];
  let scrollId = null;

  for (let page = 0; page < MAX_PAGES; page++) {
    const data = await gql(Q_CONVERSION, {
      purchaseTimeStart: start,
      purchaseTimeEnd: end,
      scrollId,
      limit: PAGE_LIMIT,
    });
    const block = data?.conversionReport;
    nodes.push(...(block?.nodes ?? []));

    scrollId = block?.pageInfo?.scrollId ?? null;
    if (!block?.pageInfo?.hasNextPage || !scrollId) break;
    // ไม่ sleep ตรงนี้โดยตั้งใจ — scrollId ตายใน 30 วิ
  }
  return nodes;
}

const windows = [];
for (let s = from; s < now; s += CHUNK_DAYS * DAY) {
  windows.push([s, Math.min(s + CHUNK_DAYS * DAY, now)]);
}

console.log(`ดึงย้อนหลัง ${days} วัน → ${windows.length} ช่วง`);

let totalRows = 0;
let totalNodes = 0;

for (const [s, e] of windows) {
  const iso = t => new Date(t * 1000).toISOString().slice(0, 10);
  const nodes = await fetchWindow(s, e);
  const rows = nodes.flatMap(mapConversionRows).filter(r => r.item_id != null);

  const dropped = nodes.length && !rows.length;
  if (dropped) {
    console.error(`❌ ${iso(s)}–${iso(e)}: ได้ ${nodes.length} node แต่ map ออกมา 0 แถว`);
    console.error('   → โครงสร้าง response ไม่ตรงกับ src/map.js รัน npm run probe แล้วแก้ mapper');
    process.exit(1);
  }

  const wrote = await db.saveConversions(rows);
  totalNodes += nodes.length;
  totalRows += wrote;
  console.log(`  ${iso(s)}–${iso(e)}: ${nodes.length} conversion → ${wrote} แถว`);
}

console.log(`\n✅ รวม ${totalNodes} conversion → ${totalRows} แถว`);
console.log(`   ตาราง conversions ตอนนี้: ${await db.count('conversions')} แถว`);

// เตือนถ้า subId หลุด — attribution พังเงียบๆ คือกับดักใหญ่สุดของระบบนี้
if (totalRows > 0) {
  const { createClient } = await import('@supabase/supabase-js');
  const sb = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_SERVICE_KEY,
    { auth: { persistSession: false } });
  const { count } = await sb.from('conversions')
    .select('*', { count: 'exact', head: true }).is('sub_id2', null);
  if (count > 0) {
    console.warn(`⚠️ ${count} แถวไม่มี sub_id2 — attribution ต่อคลิปใช้ไม่ได้กับแถวพวกนี้`);
    console.warn('   เช็ค extractSubIds() ใน src/map.js เทียบกับ probe-conversions.json');
  }
}
