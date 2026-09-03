/**
 * ให้คะแนนสินค้าที่หามาเอง เทียบกับเกณฑ์ใน config
 *
 * น้ำหนักและเกณฑ์อ่านมาจากตาราง config ไม่ได้ hardcode
 * → ปรับกลยุทธ์ได้จาก Supabase โดยไม่ต้อง deploy ใหม่
 */
import { COMMISSION_CAP } from './format.js';

export const DEFAULT_WEIGHTS = {
  commission: 0.30, sales: 0.25, rating: 0.15, priceFit: 0.20, commTrend: 0.10,
};
export const DEFAULT_THRESHOLDS = {
  minRating: 4.7, minReviews: 300, priceMin: 300, priceMax: 2500, minCommPct: 8,
};

const clamp = (v, lo = 0, hi = 1) => Math.min(hi, Math.max(lo, v));

/**
 * ด่านตรวจแบบผ่าน/ไม่ผ่าน — แยกจากคะแนน
 * สินค้าที่ตกด่านไม่ควรเอามาทำคลิปไม่ว่าคะแนนรวมจะสวยแค่ไหน
 */
export function gates({ price, rate, rating, sales }, t = DEFAULT_THRESHOLDS) {
  const pct = (Number(rate) || 0) * 100;
  return [
    {
      key: 'rating',
      label: `เรตติ้ง ≥ ${t.minRating}`,
      ok: Number(rating) >= t.minRating,
      got: rating ? Number(rating).toFixed(1) : '—',
      why: 'คนกดเข้าไปเห็นรีวิวแย่ก็ไม่ซื้อ เสียคลิปฟรี',
    },
    {
      key: 'sales',
      label: `ยอดขาย ≥ ${t.minReviews}`,
      ok: Number(sales) >= t.minReviews,
      got: sales ? Number(sales).toLocaleString('th-TH') : '—',
      why: 'ของที่ยังไม่มีใครซื้อ แปลว่ายังไม่มีใครพิสูจน์ให้',
    },
    {
      key: 'price',
      label: `ราคา ฿${t.priceMin}–${t.priceMax.toLocaleString('th-TH')}`,
      ok: Number(price) >= t.priceMin && Number(price) <= t.priceMax,
      got: price ? `฿${Number(price).toLocaleString('th-TH')}` : '—',
      why: 'ถูกไปได้คอมน้อย แพงไปคนตัดสินใจนาน',
    },
    {
      key: 'comm',
      label: `ค่าคอม ≥ ${t.minCommPct}%`,
      ok: pct >= t.minCommPct,
      got: rate ? `${pct.toFixed(1)}%` : '—',
      why: 'ต่ำกว่านี้ต้องขายเยอะมากกว่าจะคุ้มเวลาถ่าย',
    },
  ];
}

/**
 * คะแนน 0–100
 *
 * commTrend ให้คะแนนไม่ได้ตอนเพิ่มเอง เพราะต้องมี snapshot ย้อนหลัง 7 วันก่อน
 * จึงตัดออกแล้วเกลี่ยน้ำหนักไปให้ตัวที่เหลือตามสัดส่วนเดิม
 */
export function scoreProduct({ price, rate, rating, sales }, weights = DEFAULT_WEIGHTS) {
  const p = Number(price) || 0;
  const r = Number(rate) || 0;
  const commBaht = Math.min(p * r, COMMISSION_CAP);

  const parts = {
    // ได้กี่บาทต่อออเดอร์ เทียบกับเพดาน 225 — ไม่ใช่ % ล้วน
    commission: clamp(commBaht / COMMISSION_CAP),
    // log scale: 10,000 ชิ้น = เต็ม, 1,000 = 0.75, 100 = 0.5
    sales: clamp(Math.log10((Number(sales) || 0) + 1) / 4),
    // ช่วงที่ต่างกันจริงคือ 4.5–5.0 ไม่ใช่ 0–5
    rating: clamp(((Number(rating) || 0) - 4.5) / 0.5),
    // ที่ราบ 700–2,000 คือช่วงที่ยังขายง่ายและคอมพอใช้ได้
    priceFit: priceFit(p),
  };

  const used = ['commission', 'sales', 'rating', 'priceFit'];
  const total = used.reduce((s, k) => s + (weights[k] ?? 0), 0) || 1;
  const score = used.reduce((s, k) => s + parts[k] * (weights[k] ?? 0), 0) / total;

  return {
    score: Math.round(score * 100),
    parts,
    commBaht: Math.round(commBaht),
    capped: p * r > COMMISSION_CAP,
    sweetSpot: r > 0 ? Math.round(COMMISSION_CAP / r) : null,
  };
}

/** ที่ราบ 700–2,000 บาท ค่อยๆ ลดลงจนเป็น 0 ที่ 300 และ 2,500 */
function priceFit(p) {
  if (p >= 700 && p <= 2000) return 1;
  if (p < 700)  return clamp((p - 300) / 400);
  return clamp((2500 - p) / 500);
}

export const PART_LABELS = {
  commission: 'บาทต่อออเดอร์',
  sales: 'ยอดขายที่พิสูจน์แล้ว',
  rating: 'เรตติ้ง',
  priceFit: 'ราคาอยู่ในช่วงที่ขายง่าย',
};
