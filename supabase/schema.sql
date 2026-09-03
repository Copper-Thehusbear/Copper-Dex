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

-- มุมคอนเทนต์ไหนทำเงิน
create or replace view v_angle_performance as
select
  c.angle,
  count(distinct c.content_id)                              as clips,
  coalesce(sum(c.views), 0)                                 as views,
  count(cv.*)                                               as conversions,
  coalesce(sum(cv.commission), 0)::numeric(12,2)            as commission,
  case when coalesce(sum(c.views),0) > 0
       then (coalesce(sum(cv.commission),0) * 1000
             / sum(c.views))::numeric(12,2) end             as commission_per_1k_views,
  case when count(distinct c.content_id) > 0
       then (coalesce(sum(cv.commission),0)
             / count(distinct c.content_id))::numeric(12,2) end as commission_per_clip
from contents c
left join conversions cv on cv.sub_id2 = c.content_id
group by c.angle;

-- สินค้าตัวไหนควรดัน ตัวไหนควรถอด
create or replace view v_product_performance as
select
  p.item_id,
  p.name,
  p.status,
  count(distinct c.content_id)                    as clips,
  count(cv.*)                                     as orders,
  coalesce(sum(cv.commission), 0)::numeric(12,2)  as commission,
  max(c.published_at)                             as last_clip_at
from products p
left join contents c    on p.item_id = any(c.item_ids)
left join conversions cv on cv.item_id = p.item_id
group by p.item_id, p.name, p.status;

-- commission rate เปลี่ยนไปยังไง (ใช้ตัดสินว่าร้านไหนกำลังดันของ)
create or replace view v_commission_trend as
select
  item_id,
  max(snap_date)                                                  as latest_date,
  (array_agg(commission_rate order by snap_date desc))[1]         as latest_rate,
  (array_agg(commission_rate order by snap_date desc))[7]         as rate_7d_ago,
  (array_agg(price order by snap_date desc))[1]                   as latest_price,
  (array_agg(price order by snap_date desc))[7]                   as price_7d_ago
from product_snapshots
group by item_id;

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
