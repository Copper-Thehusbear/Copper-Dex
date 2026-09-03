<script setup>
import { ref, computed, onMounted } from 'vue';
import { api } from '../api.js';
import { configured } from '../supabase.js';
import { money, num } from '../lib/format.js';
import { angleLabel } from '../lib/taxonomy.js';
import EmptyState from '../components/EmptyState.vue';
import AsyncBlock from '../components/AsyncBlock.vue';

const loading = ref(true);
const error = ref(null);
const angles = ref([]);
const productPerf = ref([]);

async function load() {
  if (!configured) { loading.value = false; return; }
  loading.value = true; error.value = null;
  try {
    const [a, p] = await Promise.all([api.anglePerformance(), api.productPerformance()]);
    angles.value = a; productPerf.value = p;
  } catch (e) { error.value = e; }
  finally { loading.value = false; }
}
onMounted(load);

const angleRows = computed(() =>
  [...angles.value].sort((a, b) => Number(b.commission_per_clip ?? 0) - Number(a.commission_per_clip ?? 0)));

const productRows = computed(() =>
  [...productPerf.value].sort((a, b) => Number(b.commission ?? 0) - Number(a.commission ?? 0)));

/** ยิงไปแล้วหลายคลิปแต่ไม่มีออเดอร์ = สัญญาณให้ถอด */
const shouldRetire = computed(() =>
  productRows.value.filter(p => Number(p.clips ?? 0) >= 3 && Number(p.orders ?? 0) === 0));

const totalCommission = computed(() =>
  productRows.value.reduce((s, p) => s + Number(p.commission ?? 0), 0));
</script>

<template>
  <div class="stack">
    <div class="spread">
      <div>
        <h2>พอร์ต</h2>
        <div class="muted" style="font-size:12.5px">มุมไหนและสินค้าตัวไหนทำเงินจริง</div>
      </div>
      <button class="btn sm ghost" @click="load" :disabled="loading">รีเฟรช</button>
    </div>

    <AsyncBlock :loading="loading" :error="error">
      <div v-if="shouldRetire.length" class="banner">
        มี {{ shouldRetire.length }} สินค้าที่ยิงครบ 3 คลิปแล้วแต่ยังไม่มีออเดอร์เลย — พิจารณาถอดในหน้าเรดาร์
      </div>

      <div class="card">
        <div class="card-head">
          <h2>มุมคอนเทนต์</h2>
          <span class="hint">เรียงตามค่าคอมต่อคลิป</span>
        </div>

        <EmptyState
          v-if="!angleRows.length"
          icon="🎬"
          title="ยังไม่มีคลิปในระบบ"
          body="เพิ่มคลิปที่หน้า “คลิป” แล้วตัวเลขจะขึ้นที่นี่ ค่าคอมจะตามมาเมื่อ ingest ดึงออเดอร์เข้ามา"
        />

        <div v-else class="scroll">
          <table>
            <thead>
              <tr>
                <th>มุม</th>
                <th class="n">คลิป</th>
                <th class="n">วิว</th>
                <th class="n">ออเดอร์</th>
                <th class="n">ค่าคอม</th>
                <th class="n">คอม/คลิป</th>
                <th class="n">คอม/1k วิว</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="a in angleRows" :key="a.angle">
                <td>{{ angleLabel(a.angle) }} <span class="mono muted">{{ a.angle }}</span></td>
                <td class="n">{{ num(a.clips) }}</td>
                <td class="n">{{ num(a.views) }}</td>
                <td class="n">{{ num(a.conversions) }}</td>
                <td class="n">{{ money(a.commission) }}</td>
                <td class="n">{{ money(a.commission_per_clip) }}</td>
                <td class="n">{{ a.commission_per_1k_views == null ? '—' : money(a.commission_per_1k_views) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="card">
        <div class="card-head">
          <h2>สินค้า</h2>
          <span class="hint">รวมค่าคอม {{ money(totalCommission) }}</span>
        </div>

        <EmptyState
          v-if="!productRows.length"
          icon="📦"
          title="ยังไม่มีสินค้าในระบบ"
          body="สินค้าถูกเติมโดย job รายวัน ซึ่งต้องมีคีย์ Shopee Open API ก่อน"
        />

        <div v-else class="scroll">
          <table>
            <thead>
              <tr>
                <th>สินค้า</th>
                <th>สถานะ</th>
                <th class="n">คลิป</th>
                <th class="n">ออเดอร์</th>
                <th class="n">ค่าคอม</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in productRows" :key="p.item_id">
                <td>
                  <div style="max-width:28ch; overflow:hidden; text-overflow:ellipsis">{{ p.name ?? '—' }}</div>
                  <div class="mono muted">{{ p.item_id }}</div>
                </td>
                <td>
                  <span class="pill" :class="{ good: p.status === 'active', bad: p.status === 'retired' }">
                    {{ p.status }}
                  </span>
                </td>
                <td class="n">{{ num(p.clips) }}</td>
                <td class="n">
                  {{ num(p.orders) }}
                  <span v-if="Number(p.clips) >= 3 && Number(p.orders) === 0" class="pill bad">ควรถอด</span>
                </td>
                <td class="n">{{ money(p.commission) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </AsyncBlock>
  </div>
</template>
