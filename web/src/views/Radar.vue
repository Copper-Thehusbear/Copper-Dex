<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { api } from '../api.js';
import { configured } from '../supabase.js';
import { money, num, pct, capHit, COMMISSION_CAP } from '../lib/format.js';
import { scoreProduct, gates, DEFAULT_WEIGHTS, DEFAULT_THRESHOLDS, PART_LABELS } from '../lib/score.js';
import EmptyState from '../components/EmptyState.vue';
import AsyncBlock from '../components/AsyncBlock.vue';

const loading = ref(true);
const error = ref(null);
const products = ref([]);
const trend = ref([]);
const filter = ref('all');
const saving = ref(null);
const showAdd = ref(false);
const adding = ref(false);
const weights = ref(DEFAULT_WEIGHTS);
const thresholds = ref(DEFAULT_THRESHOLDS);

function blankDraft() {
  return { item_id: '', name: '', price: '', ratePct: '', rating: '', sales: '', offer_link: '', note: '' };
}
const draft = reactive(blankDraft());

async function load() {
  if (!configured) { loading.value = false; return; }
  loading.value = true; error.value = null;
  try {
    const [p, t, cfg] = await Promise.all([
      api.products(), api.commissionTrend(), api.config().catch(() => []),
    ]);
    products.value = p; trend.value = t;
    for (const row of cfg) {
      if (row.key === 'score_weights') weights.value = { ...DEFAULT_WEIGHTS, ...row.value };
      if (row.key === 'thresholds')    thresholds.value = { ...DEFAULT_THRESHOLDS, ...row.value };
    }
  } catch (e) { error.value = e; }
  finally { loading.value = false; }
}
onMounted(load);

const trendBy = computed(() => new Map(trend.value.map(r => [String(r.item_id), r])));

const rows = computed(() => products.value
  .filter(p => filter.value === 'all' || p.status === filter.value)
  .map(p => {
    const t = trendBy.value.get(String(p.item_id));
    const latest = t?.latest_rate == null ? null : Number(t.latest_rate);
    const before = t?.rate_7d_ago == null ? null : Number(t.rate_7d_ago);
    const delta = latest != null && before != null && before > 0 ? latest / before - 1 : null;
    return { ...p, latest, before, delta, capped: capHit(p.price, p.commission_rate) };
  }));

const counts = computed(() => ({
  all: products.value.length,
  candidate: products.value.filter(p => p.status === 'candidate').length,
  active: products.value.filter(p => p.status === 'active').length,
  retired: products.value.filter(p => p.status === 'retired').length,
}));

async function setStatus(p, status) {
  const prev = p.status;
  p.status = status; saving.value = p.item_id;
  try { await api.updateProduct(p.item_id, { status }); }
  catch (e) { p.status = prev; error.value = e; }
  finally { saving.value = null; }
}

/** ── เพิ่มสินค้าเอง ────────────────────────── */
const draftInput = computed(() => ({
  price: Number(draft.price) || 0,
  rate: (Number(draft.ratePct) || 0) / 100,
  rating: Number(draft.rating) || 0,
  sales: Number(draft.sales) || 0,
}));
const draftScore = computed(() => scoreProduct(draftInput.value, weights.value));
const draftGates = computed(() => gates(draftInput.value, thresholds.value));
const failedGates = computed(() => draftGates.value.filter(g => !g.ok));
const canSave = computed(() =>
  Boolean(String(draft.item_id).trim()) && Number(draft.price) > 0 && Number(draft.ratePct) > 0);

function scoreClass(n) { return n >= 70 ? 'good' : n >= 50 ? 'warn' : 'bad'; }

async function saveProduct() {
  adding.value = true; error.value = null;
  try {
    await api.addProduct({
      item_id: Number(String(draft.item_id).trim()),
      name: draft.name.trim() || null,
      price: Number(draft.price),
      commission_rate: Number(draft.ratePct) / 100,
      rating_star: Number(draft.rating) || null,
      sales: Number(draft.sales) || null,
      offer_link: draft.offer_link.trim() || null,
      note: draft.note.trim() || null,
      my_score: draftScore.value.score,
      status: 'candidate',
    });
    Object.assign(draft, blankDraft());
    showAdd.value = false;
    await load();
  } catch (e) { error.value = e; }
  finally { adding.value = false; }
}

function deltaClass(d) {
  if (d == null) return '';
  if (d <= -0.3) return 'bad';
  if (d >= 0.3) return 'good';
  return '';
}
</script>

<template>
  <div class="stack">
    <div class="spread">
      <div>
        <h2>เรดาร์</h2>
        <div class="muted" style="font-size:12.5px">ตัวไหนควรดัน ตัวไหนควรถอด</div>
      </div>
      <div class="row">
        <button class="btn sm ghost" @click="load" :disabled="loading">รีเฟรช</button>
        <button class="btn sm primary" @click="showAdd = !showAdd">+ เพิ่มสินค้า</button>
      </div>
    </div>

    <!-- เพิ่มสินค้าที่หาเจอเองจาก Shopee Affiliate -->
    <div v-if="showAdd" class="card">
      <div class="card-head">
        <h2>เพิ่มสินค้าที่หามาเอง</h2>
        <button class="btn sm ghost" @click="showAdd = false">ปิด</button>
      </div>
      <div class="card-body">
        <p class="muted" style="font-size:13px; margin-bottom:12px">
          เปิด Shopee Affiliate → หน้าค้นหาสินค้า → เลือกของที่สนใจ แล้วลอกตัวเลขมากรอกที่นี่
          ระบบจะให้คะแนนเทียบกับเกณฑ์ที่ตั้งไว้ทันที
        </p>

        <div class="grid2">
          <label class="field">
            <span>itemId — เลขท้าย URL หลัง <span class="mono">i.</span></span>
            <input v-model="draft.item_id" class="mono" inputmode="numeric" placeholder="123456789" />
          </label>
          <label class="field">
            <span>ชื่อสินค้า</span>
            <input v-model="draft.name" placeholder="ครีมกันแดด SPF50" />
          </label>
          <label class="field">
            <span>ราคา (บาท)</span>
            <input v-model="draft.price" type="number" min="0" inputmode="decimal" />
          </label>
          <label class="field">
            <span>ค่าคอม (%)</span>
            <input v-model="draft.ratePct" type="number" min="0" step="0.1" inputmode="decimal" />
          </label>
          <label class="field">
            <span>เรตติ้ง</span>
            <input v-model="draft.rating" type="number" min="0" max="5" step="0.1" inputmode="decimal" />
          </label>
          <label class="field">
            <span>ยอดขาย (ชิ้น)</span>
            <input v-model="draft.sales" type="number" min="0" inputmode="numeric" />
          </label>
        </div>

        <!-- คะแนนสด -->
        <div class="card" style="background:var(--surface-2); box-shadow:none; margin-bottom:14px">
          <div class="card-body">
            <div class="spread" style="margin-bottom:10px">
              <h3>คะแนน</h3>
              <span class="pill" :class="scoreClass(draftScore.score)"
                    style="font-size:15px; padding:3px 12px">{{ draftScore.score }}/100</span>
            </div>

            <div class="checkline" style="gap:10px">
              <span class="mono muted" style="flex:none; width:76px">ได้จริง</span>
              <span>
                <b>{{ money(draftScore.commBaht) }}</b> ต่อออเดอร์
                <span v-if="draftScore.capped" class="pill accent">ชนเพดาน {{ COMMISSION_CAP }}</span>
              </span>
            </div>
            <div v-if="draftScore.sweetSpot" class="checkline" style="gap:10px">
              <span class="mono muted" style="flex:none; width:76px">จุดคุ้มสุด</span>
              <span>ราคา ~{{ money(draftScore.sweetSpot) }} ที่ค่าคอมเรตนี้</span>
            </div>

            <div v-for="(v, k) in draftScore.parts" :key="k" class="checkline" style="gap:10px">
              <span class="mono muted" style="flex:none; width:76px">{{ Math.round(v * 100) }}%</span>
              <span>{{ PART_LABELS[k] }}</span>
            </div>
          </div>
        </div>

        <!-- ด่านตรวจ -->
        <div v-for="g in draftGates" :key="g.key" class="checkline">
          <span style="flex:none">{{ g.ok ? '✅' : '⚠️' }}</span>
          <span>
            {{ g.label }} <span class="mono muted">— ได้ {{ g.got }}</span>
            <span v-if="!g.ok" class="muted" style="display:block; font-size:12.5px">{{ g.why }}</span>
          </span>
        </div>

        <label class="field" style="margin-top:12px">
          <span>โน้ต — ทำไมถึงเลือกตัวนี้</span>
          <input v-model="draft.note" placeholder="เช่น เห็นในฟีดบ่อย คนรีวิวเยอะ" />
        </label>

        <div v-if="failedGates.length" class="banner" style="margin-bottom:12px">
          ตกเกณฑ์ {{ failedGates.length }} ข้อ — บันทึกได้อยู่ แต่เก็บเป็น candidate ไว้ดูก่อน อย่าเพิ่งเอาไปถ่าย
        </div>

        <div class="row">
          <button class="btn primary" @click="saveProduct" :disabled="adding || !canSave">
            {{ adding ? 'กำลังบันทึก…' : 'บันทึกเป็น candidate' }}
          </button>
          <button class="btn ghost" @click="showAdd = false">ยกเลิก</button>
        </div>
      </div>
    </div>

    <AsyncBlock :loading="loading" :error="error">
      <div class="row" style="margin-bottom:12px">
        <button v-for="(n, key) in counts" :key="key" class="btn sm"
                :class="{ primary: filter === key }" @click="filter = key">
          {{ { all: 'ทั้งหมด', candidate: 'ผู้สมัคร', active: 'กำลังดัน', retired: 'ถอดแล้ว' }[key] }} ({{ n }})
        </button>
      </div>

      <div class="card">
        <EmptyState
          v-if="!rows.length && !products.length"
          icon="📡"
          title="ยังไม่มีสินค้าในระบบ"
          body="job รายวันจะเติมให้อัตโนมัติเมื่อมีคีย์ Shopee Open API แล้ว ระหว่างนี้เพิ่มเองได้เลย — เปิด Shopee Affiliate หาสินค้าที่สนใจ แล้วลอกตัวเลขมากรอก ระบบจะให้คะแนนให้"
        >
          <template #cta><button class="btn primary" @click="showAdd = true">+ เพิ่มสินค้า</button></template>
        </EmptyState>
        <EmptyState
          v-else-if="!rows.length"
          icon="🔍"
          title="ไม่มีสินค้าในสถานะนี้"
          body="ลองเลือกตัวกรองอื่นดู"
        />

        <template v-else>
          <!-- มือถือ: การ์ด -->
          <div class="list only-mobile">
            <div v-for="p in rows" :key="p.item_id" class="item">
              <div class="title">{{ p.name ?? '—' }}</div>
              <div class="meta">
                <span class="mono">{{ p.item_id }}</span>
                <span>ราคา <b>{{ money(p.price) }}</b></span>
                <span>คอม <b>{{ pct(p.commission_rate) }}</b></span>
                <span>ขาย <b>{{ num(p.sales) }}</b></span>
                <span>★ <b>{{ p.rating_star ?? '—' }}</b></span>
              </div>
              <div class="row">
                <span v-if="p.capped" class="pill accent"
                      :title="`ค่าคอมชนเพดาน ${COMMISSION_CAP} บาท/ออเดอร์`">ชนเพดาน</span>
                <span v-if="p.delta != null" class="pill" :class="deltaClass(p.delta)">
                  คอม 7 วัน {{ p.delta > 0 ? '+' : '' }}{{ (p.delta * 100).toFixed(0) }}%
                </span>
              </div>
              <select :value="p.status" :disabled="saving === p.item_id"
                      @change="setStatus(p, $event.target.value)"
                      style="padding:7px 9px; border-radius:8px; border:1px solid var(--border); background:var(--bg)">
                <option value="candidate">ผู้สมัคร</option>
                <option value="active">กำลังดัน</option>
                <option value="retired">ถอดแล้ว</option>
              </select>
            </div>
          </div>

          <!-- จอใหญ่: ตาราง -->
          <div class="scroll only-desk">
            <table>
              <thead>
                <tr>
                  <th>สินค้า</th>
                  <th class="n">ราคา</th>
                  <th class="n">คอม</th>
                  <th class="n">คอม 7 วัน</th>
                  <th class="n">ยอดขาย</th>
                  <th class="n">เรตติ้ง</th>
                  <th>สถานะ</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in rows" :key="p.item_id">
                  <td>
                    <div style="max-width:26ch; overflow:hidden; text-overflow:ellipsis">{{ p.name ?? '—' }}</div>
                    <div class="mono muted">{{ p.item_id }}</div>
                  </td>
                  <td class="n">
                    {{ money(p.price) }}
                    <span v-if="p.capped" class="pill accent"
                          :title="`ค่าคอมชนเพดาน ${COMMISSION_CAP} บาท/ออเดอร์`">ชนเพดาน</span>
                  </td>
                  <td class="n">{{ pct(p.commission_rate) }}</td>
                  <td class="n">
                    <span v-if="p.delta == null" class="muted">—</span>
                    <span v-else class="pill" :class="deltaClass(p.delta)">
                      {{ p.delta > 0 ? '+' : '' }}{{ (p.delta * 100).toFixed(0) }}%
                    </span>
                  </td>
                  <td class="n">{{ num(p.sales) }}</td>
                  <td class="n">{{ p.rating_star ?? '—' }}</td>
                  <td>
                    <select :value="p.status" :disabled="saving === p.item_id"
                            @change="setStatus(p, $event.target.value)"
                            style="padding:4px 7px; border-radius:7px; border:1px solid var(--border); background:var(--bg)">
                      <option value="candidate">ผู้สมัคร</option>
                      <option value="active">กำลังดัน</option>
                      <option value="retired">ถอดแล้ว</option>
                    </select>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </div>

      <p class="muted" style="font-size:12.5px; margin-top:10px">
        คอลัมน์ “คอม 7 วัน” เทียบ commission rate ล่าสุดกับ snapshot ที่เก่ากว่า 7 วัน — ร่วงเกิน 30% แปลว่าร้านหยุดดันของ
      </p>
    </AsyncBlock>
  </div>
</template>
