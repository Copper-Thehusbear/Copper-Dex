/**
 * เก็บ snapshot รายวัน + เติม candidate ใหม่
 * รันวันละครั้ง — PK (item_id, snap_date) ทำให้รันซ้ำวันเดิมปลอดภัย
 */
import { gql, Q_PRODUCT_OFFER, bangkokDate } from '../src/shopee.js';
import { mapProduct, mapSnapshot } from '../src/map.js';
import { db } from '../src/db.js';

const KEYWORDS = (process.env.WATCH_KEYWORDS ?? '')
  .split(',').map(s => s.trim()).filter(Boolean);
const MAX_PAGES = Number(process.env.MAX_PAGES ?? 5);
const LIMIT = 50;

async function fetchAllPages(keyword) {
  const out = [];
  for (let page = 1; page <= MAX_PAGES; page++) {
    const data = await gql(Q_PRODUCT_OFFER, { keyword, sortType: 2, page, limit: LIMIT });
    const block = data?.productOfferV2;
    const nodes = block?.nodes ?? [];
    out.push(...nodes);
    if (!block?.pageInfo?.hasNextPage || nodes.length === 0) break;
  }
  return out;
}

const snapDate = bangkokDate();
console.log(`snapshot วันที่ ${snapDate} (Asia/Bangkok)`);

// 1. เก็บ active products ก่อนเสมอ — ห้ามพลาด ข้อมูลนี้สร้างย้อนหลังไม่ได้
const activeIds = await db.getActiveItemIds();
console.log(`active products: ${activeIds.length}`);

const collected = new Map();

// 2. ตามด้วย keyword ที่เฝ้าไว้ (หา candidate ใหม่)
for (const kw of KEYWORDS.length ? KEYWORDS : [undefined]) {
  const nodes = await fetchAllPages(kw);
  console.log(`  "${kw ?? '(ทั้งหมด)'}" → ${nodes.length} รายการ`);
  for (const n of nodes) {
    if (n?.itemId != null) collected.set(String(n.itemId), n);
  }
}

const nodes = [...collected.values()];
if (!nodes.length) {
  console.error('❌ ไม่ได้ข้อมูลเลย — หยุดก่อนเขียน DB');
  process.exit(1);
}

// 3. เขียน — snapshot ก่อน (สำคัญกว่า) แล้วค่อย products
const snapshots = nodes.map(n => mapSnapshot(n, snapDate)).filter(r => r.item_id != null);
const wroteSnap = await db.saveSnapshots(snapshots);

const products = nodes.map(mapProduct).filter(r => r.item_id != null);
const wroteProd = await db.upsertProducts(products);

// 4. Evidence ไม่ใช่ vibes
console.log(`\n✅ snapshots: ${wroteSnap} | products: ${wroteProd}`);
console.log(`   ตาราง product_snapshots ตอนนี้: ${await db.count('product_snapshots')} แถว`);

const missing = activeIds.filter(id => !collected.has(String(id)));
if (missing.length) {
  console.warn(`⚠️ active ${missing.length} ตัวไม่อยู่ในผลค้นหาวันนี้ (อาจถูกลบ/หมดสต็อก): ${missing.slice(0, 10).join(', ')}`);
}
