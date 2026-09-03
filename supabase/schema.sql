-- Shopee Affiliate Ops — schema
-- รันใน Supabase → SQL Editor (รันได้ซ้ำ ไม่พัง)
-- ต้องใช้ Postgres 15+ (Supabase ใช้ 15/16/17 อยู่แล้ว) เพราะใช้ NULLS NOT DISTINCT

-- ─────────────────────────────────────────────
-- 1. products — master ที่ "คุณ" แก้เอง
-- ─────────────────────────────────────────────
create table if not exists products (
  item_id         bigint primary key,
  shop_id         bigint,
  name            text,
  image_url       text,
  offer_link      text,
  price           numeric(12,2),
  commission_rate numeric(6,4),
  rating_star     numeric(3,2),
  sales           bigint,
  status          text not null default 'candidate'
                    check (status in ('candidate','active','retired')),
  my_score        numeric(6,2),
  retire_reason   text,
  note            text,
  first_seen_at   timestamptz not null default now(),
  updated_at      timestamptz not null default now()
);
create index if not exists idx_products_status on products (status, my_score desc);

-- ─────────────────────────────────────────────
-- 2. product_snapshots — append-only, สร้างย้อนหลังไม่ได้
--    PK = (item_id, snap_date) ทำให้ rerun วันเดิมทับตัวเอง ไม่มีแถวซ้ำ
-- ─────────────────────────────────────────────
create table if not exists product_snapshots (
  item_id                bigint      not null,
  snap_date              date        not null,
  price                  numeric(12,2),
  commission_rate        numeric(6,4),
  seller_commission_rate numeric(6,4),
  shopee_commission_rate numeric(6,4),
  sales                  bigint,
  rating_star            numeric(3,2),
  captured_at            timestamptz not null default now(),
  primary key (item_id, snap_date)
);
create index if not exists idx_snap_date on product_snapshots (snap_date desc);

-- ─────────────────────────────────────────────
-- 3. contents — คลิป (คุณกรอกเอง 30 วิ)
--    content_id คือค่าที่จะไปอยู่ใน subId2
-- ─────────────────────────────────────────────
create table if not exists contents (
  content_id         text primary key,
  platform           text not null,            -- subId1: tt | sv | fb | ig
  angle              text not null,            -- subId3: bna | unb | prb | cmp | lst
  batch              text,                     -- subId4: 26w36
  placement          text,                     -- subId5: bio | pin | cmt | desc
  hook               text,
  item_ids           bigint[] not null default '{}',
  published_at       timestamptz,
  production_minutes int,
  views              int,
  metrics_updated_at timestamptz,
  created_at         timestamptz not null default now()
);
create index if not exists idx_contents_published on contents (published_at desc);

-- ─────────────────────────────────────────────
-- 4. links — ลิงก์ที่สร้างพร้อม subId (audit trail)
-- ─────────────────────────────────────────────
create table if not exists links (
  id         bigserial primary key,
  content_id text references contents(content_id) on delete cascade,
  item_id    bigint,
  short_link text not null,
  sub_id1    text, sub_id2 text, sub_id3 text, sub_id4 text, sub_id5 text,
  created_at timestamptz not null default now(),
  unique (content_id, item_id)
);

-- ─────────────────────────────────────────────
-- 5. conversions — จาก conversionReport
--    raw jsonb เก็บทั้งก้อนไว้เสมอ กัน mapping ผิดแล้วข้อมูลหายถาวร
-- ─────────────────────────────────────────────
create table if not exists conversions (
  conversion_id text        not null,
  item_id       bigint      not null,
  order_id      text,
  status        text,
  purchase_time timestamptz,
  click_time    timestamptz,
  qty           int,
  item_price    numeric(12,2),
  commission    numeric(12,2),
  sub_id1 text, sub_id2 text, sub_id3 text, sub_id4 text, sub_id5 text,
  raw           jsonb       not null,
  ingested_at   timestamptz not null default now(),
  primary key (conversion_id, item_id)
);
create index if not exists idx_conv_time    on conversions (purchase_time desc);
create index if not exists idx_conv_content on conversions (sub_id2);
create index if not exists idx_conv_angle   on conversions (sub_id3);
create index if not exists idx_conv_item    on conversions (item_id);

-- ─────────────────────────────────────────────
-- 6. tasks — rules engine เขียน, คุณติ๊ก done
--    NULLS NOT DISTINCT = รัน generator ซ้ำในสัปดาห์เดิมไม่เกิด task ซ้ำ
-- ─────────────────────────────────────────────
create table if not exists tasks (
  id         bigserial primary key,
  week       text not null,                    -- 2026-W36
  type       text not null,
  priority   text not null check (priority in ('critical','high','medium')),
  item_id    bigint,
  content_id text,
  title      text not null,
  reason     text,
  done       boolean not null default false,
  created_at timestamptz not null default now(),
  constraint uq_task unique nulls not distinct (week, type, item_id)
);
create index if not exists idx_tasks_open on tasks (week, done, priority);

-- ─────────────────────────────────────────────
-- 7. config — ปรับกลยุทธ์โดยไม่ต้อง deploy
-- ─────────────────────────────────────────────
create table if not exists config (
  key        text primary key,
  value      jsonb not null,
  updated_at timestamptz not null default now()
);

insert into config (key, value) values
  ('score_weights', '{"commission":0.30,"sales":0.25,"rating":0.15,"priceFit":0.20,"commTrend":0.10}'),
  ('thresholds',    '{"minRating":4.7,"minReviews":300,"priceMin":300,"priceMax":2500,"minCommPct":8,"retireAfterClips":3,"retireAfterDays":21}'),
  ('targets',       '{"clipsPerWeek":7,"activeProducts":10}')
on conflict (key) do nothing;

-- ─────────────────────────────────────────────
-- 8. Views — คำถามที่สร้างระบบนี้มาเพื่อตอบ
-- ─────────────────────────────────────────────

-- drop ก่อนสร้างทุกครั้ง — `create or replace view` ปฏิเสธถ้า data type ของ column เปลี่ยน
-- ทำให้แก้นิยาม view แล้วรัน schema ซ้ำไม่ผ่าน view ไม่มีข้อมูลของตัวเอง drop ได้ฟรี
drop view if exists v_angle_performance;
drop view if exists v_product_performance;
drop view if exists v_commission_trend;

-- มุมคอนเทนต์ไหนทำเงิน
--
-- รวมยอดต่อคลิปให้จบก่อนแล้วค่อยรวมต่อ angle
-- ถ้า join contents กับ conversions ตรงๆ คลิปที่มี n ออเดอร์จะถูกนับ views ซ้ำ n รอบ
-- → commission_per_1k_views ต่ำกว่าความจริง ยิ่งคลิปทำเงินดี ยิ่งเพี้ยนหนัก
create or replace view v_angle_performance as
with per_clip as (
  select
    c.content_id,
    c.angle,
    c.views,
    count(cv.*)                     as conversions,
    coalesce(sum(cv.commission), 0) as commission
  from contents c
  left join conversions cv on cv.sub_id2 = c.content_id
  group by c.content_id, c.angle, c.views
)
select
  angle,
  count(*)                                                as clips,
  coalesce(sum(views), 0)                                 as views,
  sum(conversions)::bigint                                as conversions,
  sum(commission)::numeric(12,2)                          as commission,
  case when coalesce(sum(views), 0) > 0
       then (sum(commission) * 1000 / sum(views))::numeric(12,2) end as commission_per_1k_views,
  (sum(commission) / count(*))::numeric(12,2)             as commission_per_clip
from per_clip
group by angle;

-- สินค้าตัวไหนควรดัน ตัวไหนควรถอด
--
-- นับคลิปกับนับออเดอร์แยก lateral กัน
-- ถ้า join ทั้งสองตารางพร้อมกันจะได้ cartesian product (3 คลิป × 4 ออเดอร์ = 12 แถว)
-- → orders กับ commission พองตามจำนวนคลิป
create or replace view v_product_performance as
select
  p.item_id,
  p.name,
  p.status,
  cl.clips,
  cv.orders,
  cv.commission,
  cl.last_clip_at
from products p
left join lateral (
  select count(*) as clips, max(c.published_at) as last_clip_at
  from contents c
  where p.item_id = any(c.item_ids)
) cl on true
left join lateral (
  select count(*) as orders, coalesce(sum(x.commission), 0)::numeric(12,2) as commission
  from conversions x
  where x.item_id = p.item_id
) cv on true;

-- commission rate เปลี่ยนไปยังไง (ใช้ตัดสินว่าร้านไหนกำลังดันของ)
--
-- เทียบด้วย "วันที่" ไม่ใช่ index ที่ 7 ของ array
-- snapshot มีรูได้เสมอ (วันไหน job พังคือรูถาวร) index ที่ 7 จึงอาจเป็นข้อมูล 10 วันก่อน
-- แล้วทำให้ query หา "คอมร่วง" ใน README เตือนผิด
create or replace view v_commission_trend as
select
  s.item_id,
  s.snap_date        as latest_date,
  s.commission_rate  as latest_rate,
  w.commission_rate  as rate_7d_ago,
  s.price            as latest_price,
  w.price            as price_7d_ago
from (
  select distinct on (item_id) item_id, snap_date, commission_rate, price
  from product_snapshots
  order by item_id, snap_date desc
) s
left join lateral (
  select p.commission_rate, p.price
  from product_snapshots p
  where p.item_id = s.item_id
    and p.snap_date <= s.snap_date - 7
  order by p.snap_date desc
  limit 1
) w on true;

-- view ต้องเป็น security_invoker
--
-- ค่า default ของ Postgres คือ view รันด้วยสิทธิ์ของ "เจ้าของ view" ไม่ใช่คนที่ query
-- แปลว่า anon อ่าน view แล้วข้าม RLS ของตารางข้างใต้ไปเลย (Supabase Advisor ขึ้น CRITICAL)
-- ตั้งเป็น on เพื่อให้ anon อ่าน view แล้วยังโดน RLS ของ products/contents/conversions ตามปกติ
alter view v_angle_performance   set (security_invoker = on);
alter view v_product_performance set (security_invoker = on);
alter view v_commission_trend    set (security_invoker = on);

-- ─────────────────────────────────────────────
-- 9. RLS + Grants
--    Frontend อยู่หลัง Cloudflare Access อยู่แล้ว → anon อ่านอย่างเดียวพอ
--    การเขียนทั้งหมดผ่าน service_role (ข้าม RLS อัตโนมัติ)
-- ─────────────────────────────────────────────
alter table products          enable row level security;
alter table product_snapshots enable row level security;
alter table contents          enable row level security;
alter table links             enable row level security;
alter table conversions       enable row level security;
alter table tasks             enable row level security;
alter table config            enable row level security;

do $$
declare t text;
begin
  foreach t in array array['products','product_snapshots','contents','links','conversions','tasks','config']
  loop
    execute format('drop policy if exists anon_read on %I', t);
    execute format('create policy anon_read on %I for select to anon using (true)', t);
  end loop;
end $$;

-- Supabase กำลังบังคับ Postgres grants แบบชัดเจนสำหรับ Data API — ต้อง GRANT เอง
grant usage on schema public to anon;
grant select on all tables in schema public to anon;
alter default privileges in schema public grant select on tables to anon;

-- ให้ติ๊ก task done จากหน้าเว็บได้โดยไม่ต้องเปิดสิทธิ์เขียนทั้งตาราง
drop policy if exists anon_toggle_task on tasks;
create policy anon_toggle_task on tasks for update to anon using (true) with check (true);
grant update (done) on tasks to anon;

-- ─────────────────────────────────────────────
-- 10. สิทธิ์เขียนสำหรับหน้าเว็บ (Vue SPA)
--
-- SPA ใช้ anon key เท่านั้น (service_role ห้ามโผล่ใน frontend เด็ดขาด)
-- ตัวกันคนอื่นเข้าคือ Cloudflare Zero Trust Access ที่ครอบเว็บอยู่ ไม่ใช่ RLS
-- เปิดเฉพาะที่หน้าเว็บต้องใช้จริง ไม่เปิดทั้งตาราง
-- ─────────────────────────────────────────────

-- contents — หน้า Content Log กรอก/แก้/ลบคลิปได้เอง (แทน Table Editor)
drop policy if exists anon_write_contents on contents;
create policy anon_write_contents on contents for all to anon using (true) with check (true);
grant insert, update, delete on contents to anon;

-- products — หน้า Radar ปรับสถานะและให้คะแนนเองได้
-- grant ระบุ column → แก้ราคา/ยอดขายที่ ingest เขียนไว้ไม่ได้ ข้อมูลจาก API ไม่ถูกแตะ
drop policy if exists anon_curate_products on products;
create policy anon_curate_products on products for update to anon using (true) with check (true);
grant update (status, my_score, note, retire_reason) on products to anon;
