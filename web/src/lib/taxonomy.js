/**
 * subId taxonomy — ล็อคแล้ว ห้ามเปลี่ยน
 * attribution ย้อนหลังไม่ได้ ถ้าเปลี่ยน mapping ข้อมูลเก่าจะอ่านไม่ได้ตลอดกาล
 * ต้องตรงกับ scripts/make-link.js เสมอ
 */
export const PLATFORMS = [
  { code: 'tt', label: 'TikTok' },
  { code: 'sv', label: 'Shopee Video' },
  { code: 'fb', label: 'Facebook' },
  { code: 'ig', label: 'Instagram' },
  { code: 'yt', label: 'YouTube' },
];

export const ANGLES = [
  { code: 'bna', label: 'ก่อน-หลัง' },
  { code: 'unb', label: 'แกะกล่อง' },
  { code: 'prb', label: 'แก้ปัญหา' },
  { code: 'cmp', label: 'เทียบสินค้า' },
  { code: 'lst', label: 'รวมของ' },
];

export const PLACEMENTS = [
  { code: 'bio', label: 'ลิงก์ในไบโอ' },
  { code: 'pin', label: 'คอมเมนต์ปักหมุด' },
  { code: 'cmt', label: 'คอมเมนต์' },
  { code: 'desc', label: 'คำบรรยาย' },
];

const labelOf = (list, code) => list.find(x => x.code === code)?.label ?? code ?? '—';
export const platformLabel = c => labelOf(PLATFORMS, c);
export const angleLabel = c => labelOf(ANGLES, c);
export const placementLabel = c => labelOf(PLACEMENTS, c);

/** วันที่ไทยเป็น YYYY-MM-DD (Shopee ทำงานบน GMT+7) */
export function bangkokDate(d = new Date()) {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Bangkok', year: 'numeric', month: '2-digit', day: '2-digit',
  }).format(d);
}

/** ISO week → batch code เช่น 26w36 (ตรรกะเดียวกับ scripts/make-link.js) */
export function isoWeek(d = new Date()) {
  const t = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate()));
  t.setUTCDate(t.getUTCDate() + 4 - (t.getUTCDay() || 7));
  const yearStart = new Date(Date.UTC(t.getUTCFullYear(), 0, 1));
  const week = Math.ceil(((t - yearStart) / 86400000 + 1) / 7);
  return `${String(t.getUTCFullYear()).slice(2)}w${String(week).padStart(2, '0')}`;
}

/** week key ของตาราง tasks เช่น 2026-W36 */
export function taskWeek(d = new Date()) {
  const t = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate()));
  t.setUTCDate(t.getUTCDate() + 4 - (t.getUTCDay() || 7));
  const yearStart = new Date(Date.UTC(t.getUTCFullYear(), 0, 1));
  const week = Math.ceil(((t - yearStart) / 86400000 + 1) / 7);
  return `${t.getUTCFullYear()}-W${String(week).padStart(2, '0')}`;
}

/** content_id = c<YYMMDD><สุ่ม 3 ตัว> — ค่านี้ไปอยู่ใน subId2 และเป็น join key หลัก */
export function newContentId(d = new Date()) {
  const rand = Math.random().toString(36).slice(2, 5);
  return `c${bangkokDate(d).replaceAll('-', '').slice(2)}${rand}`;
}
