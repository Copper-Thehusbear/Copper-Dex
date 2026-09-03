/**
 * จุดเดียวที่ map response ของ Shopee → schema ของเรา
 *
 * ⚠️ ชื่อ field ในไฟล์นี้อิงเอกสาร Open API ยังไม่ผ่านการยืนยันกับ response จริง
 * ให้รัน `npm run probe` ก่อน แล้วเทียบกับ probe-*.json
 * ถ้าไม่ตรง แก้เฉพาะไฟล์นี้ — ที่อื่นไม่ต้องแตะ
 */

const num = v => (v === null || v === undefined || v === '' ? null : Number(v));
const ts = v => (v ? new Date(Number(v) * 1000).toISOString() : null); // Shopee ส่ง Unix seconds

export function mapProduct(n) {
  return {
    item_id: num(n.itemId),
    shop_id: num(n.shopId),
    name: n.productName ?? n.itemName ?? null,
    image_url: n.imageUrl ?? null,
    offer_link: n.offerLink ?? null,
    price: num(n.price ?? n.priceMin),
    commission_rate: num(n.commissionRate),
    rating_star: num(n.ratingStar),
    sales: num(n.sales),
  };
}

export function mapSnapshot(n, snapDate) {
  return {
    item_id: num(n.itemId),
    snap_date: snapDate,
    price: num(n.price ?? n.priceMin),
    commission_rate: num(n.commissionRate),
    seller_commission_rate: num(n.sellerCommissionRate),
    shopee_commission_rate: num(n.shopeeCommissionRate),
    sales: num(n.sales),
    rating_star: num(n.ratingStar),
  };
}

/**
 * subId กลับมาจาก conversionReport ในรูปแบบไหนยังต้องยืนยันจาก probe
 * รองรับไว้ 3 แบบ: array `subIds`, field แยก `subId1..5`, และ utmContent ที่คั่นด้วย _
 */
function extractSubIds(node) {
  if (Array.isArray(node.subIds)) {
    const a = node.subIds;
    return { sub_id1: a[0] ?? null, sub_id2: a[1] ?? null, sub_id3: a[2] ?? null,
             sub_id4: a[3] ?? null, sub_id5: a[4] ?? null };
  }
  if (node.subId1 !== undefined) {
    return { sub_id1: node.subId1 ?? null, sub_id2: node.subId2 ?? null,
             sub_id3: node.subId3 ?? null, sub_id4: node.subId4 ?? null,
             sub_id5: node.subId5 ?? null };
  }
  const parts = String(node.utmContent ?? '').split('_');
  return { sub_id1: parts[0] || null, sub_id2: parts[1] || null, sub_id3: parts[2] || null,
           sub_id4: parts[3] || null, sub_id5: parts[4] || null };
}

/** 1 conversion node → หลายแถว (1 แถวต่อ item) */
export function mapConversionRows(node) {
  const subs = extractSubIds(node);
  const base = {
    conversion_id: String(node.conversionId),
    status: node.conversionStatus ?? null,
    purchase_time: ts(node.purchaseTime),
    click_time: ts(node.clickTime),
    ...subs,
    raw: node,
  };

  const rows = [];
  for (const order of node.orders ?? []) {
    for (const item of order.items ?? []) {
      rows.push({
        ...base,
        item_id: num(item.itemId),
        order_id: order.orderId ? String(order.orderId) : null,
        qty: num(item.qty),
        item_price: num(item.itemPrice),
        commission: num(item.itemTotalCommission),
      });
    }
  }
  return rows;
}
