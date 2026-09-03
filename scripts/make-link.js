/**
 * สร้างลิงก์พร้อม subId ตาม taxonomy — Sprint S2
 * ตัวนี้คือตัวที่ปลดล็อกให้เริ่มถ่ายคลิปได้ ทำเสร็จแล้วเริ่มยิงคอนเทนต์ได้เลย
 *
 * ใช้:
 *   node --env-file=.env scripts/make-link.js \
 *     --item=123456789 --platform=tt --angle=bna --placement=bio
 *
 * subId ย้อนหลังใส่ไม่ได้ — คลิปที่ยิงด้วยลิงก์เปล่าคือข้อมูลที่หายถาวร
 */
import { gql, M_SHORT_LINK, bangkokDate } from '../src/shopee.js';
import { db } from '../src/db.js';

const PLATFORMS = ['tt', 'sv', 'fb', 'ig', 'yt'];
const ANGLES    = ['bna', 'unb', 'prb', 'cmp', 'lst'];  // ก่อน-หลัง/แกะกล่อง/แก้ปัญหา/เทียบ/รวม
const PLACES    = ['bio', 'pin', 'cmt', 'desc'];

const args = Object.fromEntries(
  process.argv.slice(2)
    .filter(a => a.startsWith('--'))
    .map(a => a.slice(2).split('='))
);

function die(msg) { console.error(`❌ ${msg}`); process.exit(1); }

const itemId    = args.item;
const platform  = args.platform;
const angle     = args.angle;
const placement = args.placement ?? 'bio';

if (!itemId)                        die('ต้องมี --item=<itemId>');
if (!PLATFORMS.includes(platform))  die(`--platform ต้องเป็นหนึ่งใน ${PLATFORMS.join('|')}`);
if (!ANGLES.includes(angle))        die(`--angle ต้องเป็นหนึ่งใน ${ANGLES.join('|')}`);
if (!PLACES.includes(placement))    die(`--placement ต้องเป็นหนึ่งใน ${PLACES.join('|')}`);

/** ISO week → ใช้เป็น batch code เช่น 26w36 */
function isoWeek(d = new Date()) {
  const t = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate()));
  t.setUTCDate(t.getUTCDate() + 4 - (t.getUTCDay() || 7));
  const yearStart = new Date(Date.UTC(t.getUTCFullYear(), 0, 1));
  const week = Math.ceil(((t - yearStart) / 86400000 + 1) / 7);
  return `${String(t.getUTCFullYear()).slice(2)}w${String(week).padStart(2, '0')}`;
}

// content_id = c<YYMMDD><suffix> เช่น c260903a
const rand = Math.random().toString(36).slice(2, 5);
const contentId = args.content ?? `c${bangkokDate().replaceAll('-', '').slice(2)}${rand}`;
const batch = args.batch ?? isoWeek();

const subIds = [platform, contentId, angle, batch, placement];

const data = await gql(M_SHORT_LINK, {
  input: { originUrl: `https://shopee.co.th/product/i.${itemId}`, subIds },
});

const shortLink = data?.generateShortLink?.shortLink;
if (!shortLink) die('ไม่ได้ shortLink กลับมา — ดู response ใน probe');

await db.saveLink({
  content_id: contentId,
  item_id: Number(itemId),
  short_link: shortLink,
  sub_id1: subIds[0], sub_id2: subIds[1], sub_id3: subIds[2],
  sub_id4: subIds[3], sub_id5: subIds[4],
});

console.log(`
🔗 ${shortLink}

content_id : ${contentId}   ← ใช้ค่านี้ตอนกรอก contents ใน Supabase
subIds     : ${subIds.join(' | ')}
`);
