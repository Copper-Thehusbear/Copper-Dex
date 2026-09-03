# Shopee-Afl

ระบบเก็บข้อมูล + attribution สำหรับ Shopee Affiliate — ฟรี 100%

```
GitHub Actions (cron)  →  Shopee Open API  →  Supabase Postgres
                                                    ↓
                          Cloudflare Pages (Vue) ← anon read-only
                          Cloudflare Access = auth (ไม่ต้องเขียนเอง)
```

---

## ทำตามลำดับนี้

### 1. Supabase
1. สร้าง project ใหม่ (อย่าใช้ร่วมกับ plzgo)
2. SQL Editor → วางทั้งไฟล์ `supabase/schema.sql` → Run
3. Project Settings → API → copy `URL` และ `service_role` key

### 2. เครื่องตัวเอง
```bash
npm install
cp .env.example .env      # ใส่ค่าให้ครบ
npm run probe             # ← ขั้นที่เสี่ยงที่สุด ทำก่อนเสมอ
```

`probe` จะบอกว่า signature ผ่านไหม และ field จริงชื่ออะไร  
**เทียบ `probe-*.json` กับ `src/map.js` ให้ตรงก่อน** แล้วค่อยไปข้อ 3

### 3. รันจริง
```bash
npm run ingest:products
npm run ingest:conversions -- --days=90    # backfill ครั้งแรก
```

### 4. GitHub Actions
Settings → Secrets and variables → Actions

| Secret | ค่า |
|---|---|
| `SHOPEE_APP_ID` | จาก Affiliate Open API |
| `SHOPEE_SECRET` | จาก Affiliate Open API |
| `SUPABASE_URL` | https://xxx.supabase.co |
| `SUPABASE_SERVICE_KEY` | service_role |
| `SUPABASE_DB_URL` | connection string (สำหรับ backup) |

Variables: `WATCH_KEYWORDS` = คีย์เวิร์ดที่เฝ้า คั่นด้วย `,`

แล้วกด Run workflow ด้วยมือ 1 ครั้งเพื่อดูว่าผ่าน

### 5. Cloudflare
1. Pages → เชื่อม repo นี้ → custom domain `aff.<โดเมน plzgo>`
2. **Zero Trust → Access → Add application** → policy: email = ของคุณคนเดียว  
   ทำข้อนี้แล้วไม่ต้องสร้างระบบ login เอง

### 5.1 หน้าเว็บ (Vue SPA)

```bash
cd web
npm install
cp .env.example .env      # ใส่ VITE_SUPABASE_URL และ VITE_SUPABASE_ANON_KEY
npm run dev
```

**ใช้ `anon` key เท่านั้น** — `service_role` ข้าม RLS ทั้งหมด ห้ามโผล่ใน frontend เด็ดขาด
anon key ถูกฝังใน bundle เสมอ ตัวกันคนอื่นเข้าคือ Cloudflare Access ที่ครอบเว็บ ไม่ใช่ตัวคีย์

**Cloudflare Pages** → Create project → เชื่อม repo นี้

| ตั้งค่า | ค่า |
|---|---|
| Root directory | `web` |
| Build command | `npm run build` |
| Build output directory | `dist` |
| Environment variables | `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY` |

`web/wrangler.jsonc` ตั้ง `not_found_handling: single-page-application` ไว้แล้ว — ถ้าไม่มี การเปิด `/radar` ตรงๆ จะ 404

**4 หน้าที่มี**

| หน้า | ตอบคำถาม | อ่านจาก |
|---|---|---|
| สัปดาห์นี้ | สัปดาห์นี้ต้องทำอะไร | `tasks`, `conversions` |
| เรดาร์ | ตัวไหนควรดัน ตัวไหนควรถอด | `products`, `v_commission_trend` |
| พอร์ต | มุมไหนและสินค้าตัวไหนทำเงิน | `v_angle_performance`, `v_product_performance` |
| คลิป | บันทึกคลิป + สร้าง subId + เขียนแคปชัน + อัปขึ้น TikTok | `contents`, `posts` |
| ตั้งค่า | เชื่อม/ตัดการเชื่อมต่อ TikTok | `v_integration_status` |

หน้า **คลิป** สร้าง `content_id` ให้และแสดง Sub_id ครบ 5 ช่องพร้อมปุ่มคัดลอก
ใช้แทน Supabase Table Editor ได้เลย — ยังสร้างลิงก์เองไม่ได้ถ้าไม่มีคีย์ Open API
แต่กรอกคลิปที่สร้างลิงก์ด้วยมือได้

---

### 5.2 อัปคลิปขึ้น TikTok เป็นดราฟ (ไม่บังคับ)

**Shopee Video โพสต์อัตโนมัติไม่ได้** — Affiliate Open API มีแค่ค้นสินค้า/รายงาน/สร้างลิงก์
ส่วน API อัปวิดีโอของ Shopee อยู่ฝั่งผู้ขาย (Open Platform) ซึ่ง affiliate เข้าไม่ถึง

TikTok ทำได้ผ่าน Content Posting API โดยเลือก **scope `video.upload`** (เข้า inbox เป็นดราฟ)
ไม่ใช่ `video.publish` (โพสต์ตรง) เพราะ `video.publish` ต้องผ่าน audit ของ TikTok
และถ้ายังไม่ผ่าน คอนเทนต์จะถูกบังคับเป็น `SELF_ONLY` และบัญชีต้องเป็น private ตอนโพสต์

**1. TikTok for Developers** → สร้าง app → ขอ scope `video.upload`
   → ตั้ง Redirect URI เป็น `https://<project>.supabase.co/functions/v1/tiktok/auth/callback`

**2. Deploy Edge Function**

```bash
supabase functions deploy tiktok
supabase secrets set \
  TIKTOK_CLIENT_KEY=... \
  TIKTOK_CLIENT_SECRET=... \
  TIKTOK_REDIRECT_URI=https://<project>.supabase.co/functions/v1/tiktok/auth/callback \
  APP_URL=https://aff.<โดเมน> \
  APP_TOKEN=<สุ่มยาวๆ>
```

**3. ใส่ใน Cloudflare Pages env** → `VITE_TIKTOK_FN_URL`, `VITE_APP_TOKEN` (ต้องตรงกับ `APP_TOKEN`)

**4.** เปิดเว็บ → ไอคอนเฟือง → เชื่อมต่อ TikTok

**การไหลของไฟล์:** browser → Edge Function ขอ `upload_url` → browser ยิงไฟล์เข้า TikTok ตรงๆ
วิดีโอไม่ผ่านเซิร์ฟเวอร์เราและไม่กิน Supabase Storage ที่มีแค่ 1GB

**ข้อจำกัดที่รู้อยู่แล้ว**

- ไฟล์ไม่เกิน 64MB (ยังไม่รองรับการหั่นหลาย chunk)
- `APP_TOKEN` ถูกฝังใน bundle จึงเป็นความลับระดับเดียวกับ anon key
  ตัวกันคนอื่นจริงๆ คือ Cloudflare Access ไม่ใช่ token นี้
- endpoint ของ TikTok ในโค้ดอ้างจากเอกสาร Content Posting API v2 **ยังไม่เคยยิงจริง**
  ให้ยิงครั้งแรกแล้วเทียบ error ก่อนใช้งานถาวร
- ถ้า TikTok ไม่เปิด CORS ให้ browser `PUT` ตรง จะต้องเปลี่ยนไปส่งไฟล์ผ่าน Edge Function แทน
  โค้ดฝั่ง browser ตรวจกรณีนี้แล้วแจ้งข้อความเฉพาะไว้ให้

---

### 6. เริ่มถ่ายคลิปได้แล้ว
```bash
npm run link -- --item=123456789 --platform=tt --angle=bna --placement=bio
```
เอา `content_id` ที่ได้ไปกรอกในตาราง `contents` (Supabase → Table Editor)

---

## subId taxonomy — ห้ามเปลี่ยนหลังเริ่มยิงคลิป

| ช่อง | ความหมาย | ค่า |
|---|---|---|
| subId1 | platform | `tt` `sv` `fb` `ig` `yt` |
| subId2 | content_id | `c260903abc` |
| subId3 | angle | `bna` ก่อน-หลัง / `unb` แกะกล่อง / `prb` แก้ปัญหา / `cmp` เทียบ / `lst` รวม |
| subId4 | batch | `26w36` |
| subId5 | placement | `bio` `pin` `cmt` `desc` |

**attribution ย้อนหลังไม่ได้** คลิปที่ยิงด้วยลิงก์เปล่า = ข้อมูลหายถาวร

---

## คำถามที่ระบบตอบให้ได้

```sql
select * from v_angle_performance order by commission_per_clip desc nulls last;
select * from v_product_performance where clips >= 3 and orders = 0;  -- ควรถอด
select * from v_commission_trend where latest_rate < rate_7d_ago * 0.7;  -- คอมร่วง
```

---

## ข้อควรรู้ / ข้อจำกัดที่ตั้งใจไว้แบบนี้

- `product_snapshots` เป็น append-only — **สร้างย้อนหลังไม่ได้** วันไหน job พังคือรูถาวร ตั้ง notification ของ GitHub ให้ส่งเมลเมื่อ workflow แดง
- `conversionReport` ดึงได้ครั้งละไม่เกิน ~90 วัน (สคริปต์หั่น chunk ให้แล้ว) และ `scrollId` ตายใน 30 วิ จึงไม่มี sleep ระหว่างหน้า
- PK ของ `conversions` คือ `(conversion_id, item_id)` — ตั้งบนสมมติฐานว่า 1 conversion ไม่มี itemId ซ้ำข้าม order ถ้า probe เจอว่าซ้ำได้จริง ให้เพิ่ม `order_id` เข้า PK
- `raw jsonb` เก็บ response ทั้งก้อนเสมอ ถ้า mapping ผิดก็ยัง re-map จากของเดิมได้
- `service_role` key ข้าม RLS ทั้งหมด → อยู่ใน GitHub Secrets เท่านั้น **ห้ามใส่ใน frontend**
- Supabase free tier ไม่มี backup → `weekly-backup` workflow จำเป็น ไม่ใช่ของแถม
- ชื่อ field ใน `src/shopee.js` และ `src/map.js` อิงเอกสาร Open API **ยังไม่ผ่านการยืนยันกับ response จริง** — `npm run probe` มีไว้เพื่อการนี้
