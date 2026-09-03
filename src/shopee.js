import crypto from 'node:crypto';

const ENDPOINT = process.env.SHOPEE_ENDPOINT
  ?? 'https://open-api.affiliate.shopee.co.th/graphql';
const APP_ID = process.env.SHOPEE_APP_ID;
const SECRET = process.env.SHOPEE_SECRET;

if (!APP_ID || !SECRET) {
  throw new Error('ขาด SHOPEE_APP_ID / SHOPEE_SECRET');
}

const sleep = ms => new Promise(r => setTimeout(r, ms));

/**
 * ยิง GraphQL ไป Shopee Affiliate Open API
 *
 * จุดตายที่ทำให้ signature ผิดตลอด: payload ต้อง serialize "ครั้งเดียว"
 * แล้วใช้ string ตัวเดิมทั้งตอน sign และตอนส่ง — ห้าม JSON.stringify ซ้ำ
 */
export async function gql(query, variables = {}, { retries = 3, timeoutMs = 25000 } = {}) {
  const payload = JSON.stringify({ query, variables });

  for (let attempt = 0; attempt <= retries; attempt++) {
    const ts = Math.floor(Date.now() / 1000);
    const signature = crypto
      .createHash('sha256')
      .update(APP_ID + ts + payload + SECRET)
      .digest('hex');

    try {
      const res = await fetch(ENDPOINT, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `SHA256 Credential=${APP_ID}, Timestamp=${ts}, Signature=${signature}`,
        },
        body: payload,
        signal: AbortSignal.timeout(timeoutMs),
      });

      const text = await res.text();
      if (!res.ok) throw new Error(`HTTP ${res.status}: ${text.slice(0, 400)}`);

      const json = JSON.parse(text);
      if (json.errors?.length) {
        const e = json.errors[0];
        // 10020 = invalid signature, 11001 = ช่วงเวลาเกิน 90 วัน — retry ไม่ช่วย
        const fatal = [10020, 11001, 10000].includes(Number(e.extensions?.code ?? e.code));
        const err = new Error(`Shopee API: ${e.message} ${JSON.stringify(e.extensions ?? {})}`);
        if (fatal) { err.fatal = true; }
        throw err;
      }
      return json.data;
    } catch (err) {
      if (err.fatal || attempt === retries) throw err;
      // exponential backoff + jitter (retry พร้อมกันเป๊ะ = DDoS ตัวเอง)
      const wait = 2 ** attempt * 1000 + Math.random() * 500;
      console.warn(`  retry ${attempt + 1}/${retries} in ${Math.round(wait)}ms — ${err.message}`);
      await sleep(wait);
    }
  }
}

/** Shopee ทำงานบน GMT+7 อยู่แล้ว → คืนวันที่ไทยเป็น YYYY-MM-DD */
export function bangkokDate(d = new Date()) {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Bangkok', year: 'numeric', month: '2-digit', day: '2-digit',
  }).format(d);
}

/* ── Queries ─────────────────────────────────────────────
 * โครงนี้อิงเอกสาร Open API — ให้รัน `npm run probe` ยืนยันชื่อ field
 * กับ response จริงก่อน แล้วค่อยแก้ src/map.js ถ้าไม่ตรง
 */

export const Q_PRODUCT_OFFER = `
query ($keyword: String, $sortType: Int, $page: Int, $limit: Int) {
  productOfferV2(keyword: $keyword, sortType: $sortType, page: $page, limit: $limit) {
    nodes {
      itemId shopId productName imageUrl offerLink productLink
      price priceMin priceMax sales ratingStar
      commissionRate sellerCommissionRate shopeeCommissionRate
      shopName shopType
    }
    pageInfo { page limit hasNextPage }
  }
}`;

export const Q_CONVERSION = `
query ($purchaseTimeStart: Int, $purchaseTimeEnd: Int, $scrollId: String, $limit: Int) {
  conversionReport(
    purchaseTimeStart: $purchaseTimeStart
    purchaseTimeEnd: $purchaseTimeEnd
    scrollId: $scrollId
    limit: $limit
  ) {
    nodes {
      conversionId purchaseTime clickTime conversionStatus
      utmContent referrer campaignPartnerName
      orders {
        orderId shopId shopType
        items {
          itemId itemName modelId qty itemPrice
          itemTotalCommission itemSellerCommission itemShopeeCommission
        }
      }
    }
    pageInfo { scrollId hasNextPage limit }
  }
}`;

export const M_SHORT_LINK = `
mutation ($input: GenerateShortLinkInput!) {
  generateShortLink(input: $input) { shortLink }
}`;
