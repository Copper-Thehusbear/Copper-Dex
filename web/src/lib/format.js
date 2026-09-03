const baht = new Intl.NumberFormat('th-TH', { style: 'currency', currency: 'THB', maximumFractionDigits: 0 });
const int = new Intl.NumberFormat('th-TH');

export const money = v => (v == null ? '—' : baht.format(Number(v)));
export const num = v => (v == null ? '—' : int.format(Number(v)));

export const pct = v => (v == null ? '—' : `${(Number(v) * 100).toFixed(1)}%`);

export function date(v) {
  if (!v) return '—';
  return new Intl.DateTimeFormat('th-TH', {
    timeZone: 'Asia/Bangkok', day: 'numeric', month: 'short', year: '2-digit',
  }).format(new Date(v));
}

export function daysAgo(v) {
  if (!v) return null;
  return Math.floor((Date.now() - new Date(v).getTime()) / 86400000);
}

/** ค่าคอมจาก Shopee มีเพดาน 225 บาท/ออเดอร์ — เทียบกับเพดาน ไม่ใช่ % ล้วน */
export const COMMISSION_CAP = 225;
export function capHit(price, rate) {
  if (price == null || rate == null) return null;
  return Number(price) * Number(rate) >= COMMISSION_CAP;
}
