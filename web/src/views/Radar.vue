<script setup>
import { ref, computed, onMounted } from 'vue';
import { api } from '../api.js';
import { configured } from '../supabase.js';
import { money, num, pct, capHit, COMMISSION_CAP } from '../lib/format.js';
import EmptyState from '../components/EmptyState.vue';
import AsyncBlock from '../components/AsyncBlock.vue';

const loading = ref(true);
const error = ref(null);
const products = ref([]);
const trend = ref([]);
const filter = ref('all');
const saving = ref(null);

async function load() {
  if (!configured) { loading.value = false; return; }
  loading.value = true; error.value = null;
  try {
    const [p, t] = await Promise.all([api.products(), api.commissionTrend()]);
    products.value = p; trend.value = t;
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
      <button class="btn sm ghost" @click="load" :disabled="loading">รีเฟรช</button>
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
          body="สินค้าถูกเติมโดย job รายวัน (ingest-products) ซึ่งต้องใช้คีย์ Shopee Open API ก่อน — ตอนนี้ยังไม่มีอะไรให้สแกน"
        />
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
