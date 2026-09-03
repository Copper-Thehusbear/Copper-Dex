# CLAUDE.md

บริบทโปรเจกต์สำหรับ Claude Code — อ่านก่อนแตะโค้ด

## โปรเจกต์นี้คืออะไร

ระบบเก็บข้อมูลและทำ attribution สำหรับ Shopee Affiliate ของเจ้าของคนเดียว (Copper)
เป้าหมายไม่ใช่ dashboard สวยๆ แต่คือตอบให้ได้ว่า **มุมคอนเทนต์ไหนและสินค้าตัวไหนทำเงินจริง**
แล้วบอกว่าสัปดาห์นี้ควรทำอะไร

ผู้ใช้มีคนเดียวตลอดไป → อย่าออกแบบเผื่อ scale, multi-tenant, หรือ auth ซับซ้อน

## Stack (ตัดสินใจแล้ว อย่าเสนอเปลี่ยนถ้าไม่มีเหตุผลใหม่)

| ชั้น | ของที่ใช้ | เหตุผล |
|---|---|---|
| Data | Supabase Postgres (free) | ต้องการ SQL join จริง + Table Editor ใช้แก้ข้อมูลมือแทน admin UI |
| Cron | GitHub Actions | ฟรี มี retry และ email alert เมื่อ job แดง — Workers cron ไม่มี |
| Frontend | Vue 3 SPA บน Cloudflare Pages | เจ้าของถนัด Vue อยู่แล้ว, โดเมนอยู่ที่ Cloudflare |
| Auth | Cloudflare Zero Trust Access | **ไม่ต้องเขียนระบบ login** — อย่าเสนอ Supabase Auth |
| Backup | pg_dump → GitHub artifact + R2 | Supabase free ไม่มี backup ให้ |

ที่ **ไม่** ใช้และเหตุผล: Firestore (aggregate ยาก), n8n (ระบบสองตัว = จุดพังสองจุด สำหรับคนทำงานคนเดียว),
Railway/Render (คิดเงินตาม container ที่รันตลอด แต่งานนี้คือ cron วันละ 30 วินาที),
Google Sheets (ไม่มี unique key → ข้อมูลซ้ำเงียบๆ)

## ข้อจำกัดที่ห้ามลืม

- **`product_snapshots` เป็น append-only และสร้างย้อนหลังไม่ได้** วันไหน job พังคือรูถาวรใน dataset
  time-series ของราคา/commission rate คือ moat ทั้งหมดของโปรเจกต์นี้
- **`conversionReport` ดึงได้ครั้งละไม่เกิน ~90 วัน** เกินกว่านั้น error 11001 → ต้องหั่น chunk
- **`scrollId` หมดอายุถ้าห่างกันเกิน 30 วินาที** → ห้ามใส่ sleep ระหว่างหน้า pagination
- **Signature:** `SHA256(appId + timestamp + payload + secret)` โดย payload ต้อง serialize
  ครั้งเดียวแล้วใช้ string ตัวเดิมทั้งตอน sign และตอนส่ง — stringify ซ้ำ = signature ผิดตลอด
- **`service_role` key ข้าม RLS ทั้งหมด** อยู่ใน GitHub Secrets เท่านั้น ห้ามโผล่ใน frontend
- Shopee ทำงานบน GMT+7, timestamp เป็น Unix seconds
- ค่าคอมจาก Shopee มีเพดาน 225 บาท/ออเดอร์ → logic scoring ต้องเทียบกับเพดานนี้ ไม่ใช่ % ล้วน

## subId taxonomy — ล็อคแล้ว ห้ามเปลี่ยน

attribution ย้อนหลังไม่ได้ ถ้าเปลี่ยน mapping ข้อมูลเก่าจะอ่านไม่ได้ตลอดกาล

| ช่อง | ความหมาย | ค่า |
|---|---|---|
| subId1 | platform | `tt` `sv` `fb` `ig` `yt` |
| subId2 | content_id | `c260903abc` ← join key หลัก |
| subId3 | angle | `bna` `unb` `prb` `cmp` `lst` |
| subId4 | batch | `26w36` |
| subId5 | placement | `bio` `pin` `cmt` `desc` |

## สถานะปัจจุบัน

เสร็จแล้ว: schema, shopee client, mapper, db adapter, probe, ingest x2, link generator, workflows

**ยังไม่ยืนยัน:** ชื่อ field ใน `src/shopee.js` และ `src/map.js` อิงเอกสาร Open API เท่านั้น
ยังไม่เคยยิง API จริง ตัวที่เสี่ยงสุดคือ **subId กลับมาในรูปแบบไหนจาก conversionReport**
(`map.js` รองรับไว้ 3 แบบ: array, field แยก, utmContent คั่น `_`)

**งานถัดไปตามลำดับ:**
1. รัน `npm run probe` → แก้ `src/map.js` ให้ตรงกับ response จริง ← ทำก่อนอย่างอื่น
2. rules engine (`scripts/generate-tasks.js`) เขียน task ลงตาราง `tasks` ทุกเช้าวันจันทร์
3. Vue SPA 4 หน้า: This Week / Radar / Portfolio / Content Log
4. LINE push (เดือนที่ 3)

## กติกาการทำงานกับโปรเจกต์นี้

- **แก้ mapping ที่ `src/map.js` ไฟล์เดียว** ห้ามกระจาย field name ไปทั่วโปรเจกต์
- **ทุก DB call ผ่าน `src/db.js`** เพื่อให้ย้าย backend ทีหลังแก้ที่เดียว
- **ล้มให้เสียงดัง** ได้ข้อมูลมาแต่ map ออกมา 0 แถว = exit 1 ห้ามเขียน DB แล้วบอกว่าสำเร็จ
- **ห้าม scrape Shopee** บัญชีที่โดนแบนคือบัญชีที่ถือรายได้ทั้งหมด
- **ห้าม gold-plate** เจ้าของสนุกกับการสร้างเครื่องมือมากกว่าถ่ายคลิป ซึ่งเป็นความเสี่ยงอันดับ 1
  ของโปรเจกต์ ถ้าเสนอฟีเจอร์เกินที่ขอ ให้เตือนแทน
- ฟีเจอร์ใหม่รอจนกว่าจะมี conversion แรกเข้า — ออกแบบจากข้อมูลจริง ไม่ใช่จากการเดา
- คุยภาษาไทย ตอบสั้นตรงประเด็น มี bold header และ bullet
