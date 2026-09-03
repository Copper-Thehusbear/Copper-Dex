/**
 * รันตัวนี้ "ก่อน" อย่างอื่นทั้งหมด
 *
 * ทำ 3 อย่าง:
 *  1. ยืนยันว่า signature ถูก (ขั้นที่เสี่ยงที่สุด → ทำก่อน)
 *  2. ดัมป์ response จริงลงไฟล์ ให้เทียบชื่อ field กับ src/map.js
 *  3. บอกว่า field ไหนในเอกสารไม่มีจริง
 *
 * ใช้: node --env-file=.env scripts/probe.js
 */
import fs from 'node:fs';
import { gql, Q_PRODUCT_OFFER, Q_CONVERSION } from '../src/shopee.js';

const line = s => console.log(`\n── ${s} ${'─'.repeat(Math.max(0, 50 - s.length))}`);

async function step1_auth() {
  line('1. ทดสอบ signature (query เล็กสุด)');
  const data = await gql(
    `query { productOfferV2(limit: 1) { nodes { itemId productName } } }`
  );
  const n = data?.productOfferV2?.nodes?.[0];
  if (!n) throw new Error('ยิงผ่านแต่ไม่มีข้อมูลกลับมา — เช็คว่าบัญชี affiliate approved แล้ว');
  console.log(`✅ signature ผ่าน — ตัวอย่าง: ${n.itemId} ${n.productName ?? ''}`);
}

async function step2_products() {
  line('2. productOfferV2 (ทุก field ที่ map.js ใช้)');
  const data = await gql(Q_PRODUCT_OFFER, { sortType: 2, page: 1, limit: 5 });
  const nodes = data?.productOfferV2?.nodes ?? [];
  fs.writeFileSync('probe-products.json', JSON.stringify(data, null, 2));
  console.log(`✅ ได้ ${nodes.length} รายการ → probe-products.json`);
  if (nodes[0]) {
    console.log('   field ที่มีจริง:', Object.keys(nodes[0]).join(', '));
    const nulls = Object.entries(nodes[0]).filter(([, v]) => v === null).map(([k]) => k);
    if (nulls.length) console.log('   ⚠️ null ในตัวอย่างแรก:', nulls.join(', '));
  }
  console.log('   pageInfo:', JSON.stringify(data?.productOfferV2?.pageInfo));
}

async function step3_conversions() {
  line('3. conversionReport (7 วันล่าสุด)');
  const end = Math.floor(Date.now() / 1000);
  const start = end - 7 * 86400;
  const data = await gql(Q_CONVERSION, {
    purchaseTimeStart: start, purchaseTimeEnd: end, limit: 20,
  });
  const nodes = data?.conversionReport?.nodes ?? [];
  fs.writeFileSync('probe-conversions.json', JSON.stringify(data, null, 2));
  console.log(`✅ ได้ ${nodes.length} conversion → probe-conversions.json`);
  if (!nodes.length) {
    console.log('   (ยังไม่มีออเดอร์ = ปกติถ้าเพิ่งเริ่ม กลับมารันใหม่หลังมีออเดอร์แรก)');
    return;
  }
  console.log('   field ที่มีจริง:', Object.keys(nodes[0]).join(', '));
  console.log('   ⚠️ สำคัญที่สุด — หา subId ให้เจอในนี้ แล้วแก้ extractSubIds() ใน src/map.js:');
  console.log('  ', JSON.stringify(nodes[0]).slice(0, 600));
}

const steps = [step1_auth, step2_products, step3_conversions];

for (const step of steps) {
  try {
    await step();
  } catch (err) {
    console.error(`\n❌ ${step.name} ล้มเหลว: ${err.message}`);
    if (/10020|signature/i.test(err.message)) {
      console.error('   → signature ผิด: เช็คว่า APP_ID/SECRET ถูก และไม่ได้ stringify payload ซ้ำ');
    }
    if (/Cannot query field|Unknown argument/i.test(err.message)) {
      console.error('   → schema จริงไม่ตรงกับที่เขียนไว้: ลบ field ที่ error ออกจาก src/shopee.js');
    }
    process.exit(1);
  }
}

line('เสร็จ');
console.log('ขั้นต่อไป: เทียบ probe-*.json กับ src/map.js แล้วแก้ให้ตรง ก่อนรัน ingest');
