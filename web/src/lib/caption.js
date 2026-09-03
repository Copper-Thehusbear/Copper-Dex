/**
 * ตัวช่วยเขียนสคริปต์ + แคปชัน สำหรับคลิปสั้น
 *
 * สิ่งที่อยู่ในไฟล์นี้คือ "หลักทั่วไป" ของ short-video SEO เท่านั้น
 * ไม่ใช่สิ่งที่พิสูจน์แล้วว่าเวิร์คกับคนดูของคุณ — อันนั้นต้องรอข้อมูล conversion จริง
 * แล้วค่อยกลับมาแก้ template ตามมุมที่ v_angle_performance บอกว่าทำเงิน
 *
 * แก้ข้อความทั้งหมดได้ที่ไฟล์นี้ไฟล์เดียว
 */

/** จุดที่ผู้ชมตัดสินใจปัดทิ้ง — ทุกอย่างที่สำคัญต้องอยู่ก่อนวินาทีนี้ */
export const HOOK_SECONDS = 3;

/** feed ตัดแคปชันประมาณนี้ ที่เหลือต้องกด "เพิ่มเติม" */
export const CAPTION_FOLD = 100;

/**
 * โครงคลิปต่อมุม — hook / บีต / แคปชัน
 * {kw} = คีย์เวิร์ดหลัก, {product} = ชื่อสินค้า, {price} = ราคา
 */
export const ANGLE_PLAYBOOK = {
  bna: {
    label: 'ก่อน-หลัง',
    hooks: [
      'ใช้ {kw} มา 14 วัน ต่างกันขนาดนี้',
      'ก่อนใช้ {kw} กับหลังใช้ ดูเอาเอง',
      'ไม่คิดว่า {kw} จะเปลี่ยนได้ขนาดนี้',
    ],
    beats: [
      ['0–3 วิ', 'โชว์ผล "หลัง" ก่อนเลย อย่าเล่าที่มา'],
      ['3–8 วิ', 'ตัดกลับไปภาพ "ก่อน" ให้เห็นความต่างชัดๆ'],
      ['8–20 วิ', 'บอกว่าใช้ยังไง นานแค่ไหน ราคาเท่าไหร่'],
      ['20–30 วิ', 'CTA + ย้ำชื่อสินค้าอีกครั้ง'],
    ],
    caption: '{kw} ใช้จริง 14 วัน · ผลลัพธ์ในคลิป',
  },
  unb: {
    label: 'แกะกล่อง',
    hooks: [
      'สั่ง {kw} มา {price} ได้อะไรบ้าง',
      'แกะ {kw} ที่คนสั่งกันเยอะสุดตอนนี้',
      '{price} บาทกับ {kw} คุ้มไหม แกะให้ดู',
    ],
    beats: [
      ['0–3 วิ', 'มือแกะกล่องทันที + พูดราคาออกมาเลย'],
      ['3–12 วิ', 'โชว์ของจริงในมือ ไม่ใช่ภาพจากเว็บ'],
      ['12–22 วิ', 'จุดที่คนสงสัยที่สุด 1 อย่าง (วัสดุ/ขนาด/กลิ่น)'],
      ['22–30 วิ', 'สรุปว่าคุ้มไม่คุ้ม + CTA'],
    ],
    caption: 'แกะ {kw} {price} บาท ของจริงเป็นแบบนี้',
  },
  prb: {
    label: 'แก้ปัญหา',
    hooks: [
      'ถ้ายังมีปัญหา {kw} อยู่ ดูอันนี้',
      'แก้ {kw} ด้วยของ {price} บาท',
      'เลิกทน {kw} ได้แล้ว',
    ],
    beats: [
      ['0–3 วิ', 'พูดชื่อ "ปัญหา" ออกมาตรงๆ ให้คนที่มีปัญหานี้หยุดปัด'],
      ['3–10 วิ', 'ทำให้เห็นว่าปัญหานี้น่ารำคาญยังไง'],
      ['10–22 วิ', 'โชว์วิธีแก้ด้วยสินค้า ทีละขั้น'],
      ['22–30 วิ', 'ผลลัพธ์ + CTA'],
    ],
    caption: 'วิธีแก้ {kw} ที่ได้ผลจริง',
  },
  cmp: {
    label: 'เทียบสินค้า',
    hooks: [
      '{kw} ถูกกับแพง ต่างกันตรงไหน',
      'ซื้อ {kw} ตัวไหนดี เทียบให้ดูแล้ว',
      'อย่าเพิ่งซื้อ {kw} จนกว่าจะดูอันนี้',
    ],
    beats: [
      ['0–3 วิ', 'วางของ 2 ชิ้นคู่กันในเฟรมแรก'],
      ['3–12 วิ', 'เทียบทีละหัวข้อ ไม่เกิน 3 หัวข้อ'],
      ['12–22 วิ', 'บอกว่าใครควรซื้อตัวไหน ไม่ใช่บอกว่าตัวไหนดีกว่า'],
      ['22–30 วิ', 'CTA'],
    ],
    caption: 'เทียบ {kw} ตัวไหนคุ้มกว่า',
  },
  lst: {
    label: 'รวมของ',
    hooks: [
      'รวม 5 อย่างที่ควรมีถ้าคุณ {kw}',
      '5 ของที่ซื้อแล้วไม่เสียดายเงิน หมวด {kw}',
      'ของ {kw} ที่ใช้แล้วไม่อยากคืน',
    ],
    beats: [
      ['0–3 วิ', 'บอกจำนวน + หมวด ให้ชัด "5 ของ..." '],
      ['3–25 วิ', 'ชิ้นละ 4–5 วิ ห้ามยาวกว่านี้ ตัดให้กระชับ'],
      ['25–30 วิ', 'บอกว่าลิงก์อยู่ไหน + CTA'],
    ],
    caption: 'รวมของ {kw} ที่ใช้จริง ครบในคลิปเดียว',
  },
};

/** CTA ต้องตรงกับที่ลิงก์อยู่จริง (subId5) ไม่งั้นคนหาไม่เจอ = เสียคลิกฟรีๆ */
export const CTA_BY_PLACEMENT = {
  bio:  'ลิงก์อยู่ในไบโอ กดที่ชื่อโปรไฟล์ได้เลย',
  pin:  'ลิงก์อยู่คอมเมนต์ที่ปักหมุดไว้',
  cmt:  'ลิงก์อยู่ในคอมเมนต์ เลื่อนดูได้เลย',
  desc: 'ลิงก์อยู่ในคำบรรยายใต้คลิป',
};

const fill = (tpl, vars) =>
  tpl.replace(/\{(\w+)\}/g, (_, k) => (vars[k] ?? '').toString().trim() || `{${k}}`);

/** สร้างแคปชันเต็ม: บรรทัดแรกมีคีย์เวิร์ด → ตามด้วย CTA → ปิดท้ายด้วยแฮชแท็ก */
export function buildCaption({ angle, keyword, product, price, placement, extraTags = [] }) {
  const play = ANGLE_PLAYBOOK[angle] ?? ANGLE_PLAYBOOK.bna;
  const vars = { kw: keyword, product, price };
  const first = fill(play.caption, vars);
  const cta = CTA_BY_PLACEMENT[placement] ?? CTA_BY_PLACEMENT.bio;
  const tags = buildHashtags({ keyword, extraTags });
  return [first, cta, tags.join(' ')].filter(Boolean).join('\n');
}

/** 3–5 แท็ก: กว้าง 1 + เฉพาะเจาะจง 2–3 — ยัดเยอะไม่ได้ช่วย แต่ทำให้ดูสแปม */
export function buildHashtags({ keyword, extraTags = [] }) {
  const slug = (keyword ?? '').trim().replace(/\s+/g, '');
  const out = ['#รีวิว'];
  if (slug) out.push(`#${slug}`);
  out.push('#ของดีบอกต่อ');
  for (const t of extraTags) {
    const s = t.trim().replace(/^#/, '').replace(/\s+/g, '');
    if (s && out.length < 5) out.push(`#${s}`);
  }
  return out;
}

export function hookOptions({ angle, keyword, price }) {
  const play = ANGLE_PLAYBOOK[angle] ?? ANGLE_PLAYBOOK.bna;
  return play.hooks.map(h => fill(h, { kw: keyword, price }));
}

export function beats(angle) {
  return (ANGLE_PLAYBOOK[angle] ?? ANGLE_PLAYBOOK.bna).beats;
}

/**
 * เช็คลิสต์ก่อนโพสต์
 * ok = ผ่าน, warn = ควรแก้ — ไม่มี "ผิด" เพราะไม่มีกฎตายตัว
 */
export function auditCaption({ caption, keyword, hook }) {
  const text = (caption ?? '').trim();
  const kw = (keyword ?? '').trim();
  const firstLine = text.split('\n')[0] ?? '';
  const tags = text.match(/#[^\s#]+/g) ?? [];
  const items = [];

  const add = (ok, label, hint) => items.push({ ok, label, hint });

  add(Boolean(kw), 'มีคีย์เวิร์ดหลัก',
    'ต้องมีคำที่คนจะพิมพ์ค้นหาจริงๆ ไม่ใช่ชื่อแบรนด์อย่างเดียว');

  add(kw !== '' && firstLine.toLowerCase().includes(kw.toLowerCase()),
    'คีย์เวิร์ดอยู่ในบรรทัดแรก',
    `feed ตัดแคปชันราวๆ ${CAPTION_FOLD} ตัวอักษร คีย์เวิร์ดต้องอยู่ก่อนโดนตัด`);

  add(kw !== '' && (hook ?? '').toLowerCase().includes(kw.toLowerCase()),
    `พูดคีย์เวิร์ดใน ${HOOK_SECONDS} วิแรก`,
    'ระบบถอดเสียงอัตโนมัติเอาไปทำ index ด้วย พูดออกเสียงจึงมีผล ไม่ใช่แค่พิมพ์');

  add(firstLine.length > 0 && firstLine.length <= CAPTION_FOLD,
    `บรรทัดแรกไม่เกิน ${CAPTION_FOLD} ตัว`,
    `ตอนนี้ ${firstLine.length} ตัว — ย้ายส่วนที่ไม่สำคัญไปบรรทัดถัดไป`);

  add(tags.length >= 3 && tags.length <= 5,
    'แฮชแท็ก 3–5 อัน',
    `ตอนนี้ ${tags.length} อัน — น้อยไปไม่ถูกจัดหมวด เยอะไปดูสแปม`);

  add(/ลิงก์|link|กด|คอมเมนต์|ไบโอ/i.test(text),
    'มี CTA บอกว่าลิงก์อยู่ไหน',
    'คนดูจบแล้วไม่รู้จะกดตรงไหน = เสียคลิกฟรี');

  return items;
}
