<script setup>
import { ref, computed, onMounted } from 'vue';
import { api } from '../api.js';
import { configured } from '../supabase.js';
import { taskWeek, isoWeek } from '../lib/taxonomy.js';
import { money, num } from '../lib/format.js';
import StatTile from '../components/StatTile.vue';
import EmptyState from '../components/EmptyState.vue';
import AsyncBlock from '../components/AsyncBlock.vue';

const loading = ref(true);
const error = ref(null);
const tasks = ref([]);
const conversions = ref([]);
const clips = ref([]);
const week = taskWeek();
const batch = isoWeek();

/** เที่ยงคืนวันจันทร์ของสัปดาห์นี้ ตามเวลาไทย */
function weekStartISO() {
  const now = new Date();
  const bkk = new Date(now.getTime() + 7 * 3600_000);
  const dow = (bkk.getUTCDay() + 6) % 7; // จันทร์ = 0
  const monday = Date.UTC(bkk.getUTCFullYear(), bkk.getUTCMonth(), bkk.getUTCDate() - dow);
  return new Date(monday - 7 * 3600_000).toISOString();
}

async function load() {
  if (!configured) { loading.value = false; return; }
  loading.value = true; error.value = null;
  try {
    const since = weekStartISO();
    const [t, c, k] = await Promise.all([
      api.tasks(week), api.conversionsSince(since), api.contentsSince(since),
    ]);
    tasks.value = t; conversions.value = c; clips.value = k;
  } catch (e) { error.value = e; }
  finally { loading.value = false; }
}
onMounted(load);

const commission = computed(() =>
  conversions.value.reduce((s, r) => s + Number(r.commission ?? 0), 0));
const open = computed(() => tasks.value.filter(t => !t.done));

const PRIORITY = { critical: 'bad', high: 'warn', medium: '' };

async function toggle(t) {
  const next = !t.done;
  t.done = next;                                  // optimistic
  try { await api.toggleTask(t.id, next); }
  catch (e) { t.done = !next; error.value = e; }  // ย้อนกลับถ้าเขียนไม่ผ่าน
}
</script>

<template>
  <div class="stack">
    <div class="spread">
      <div>
        <h2>สัปดาห์นี้</h2>
        <div class="muted" style="font-size:12.5px">{{ week }} · batch <span class="mono">{{ batch }}</span></div>
      </div>
      <button class="btn sm ghost" @click="load" :disabled="loading">รีเฟรช</button>
    </div>

    <AsyncBlock :loading="loading" :error="error">
      <div class="tiles">
        <StatTile k="คลิปที่ลงสัปดาห์นี้" :v="num(clips.length)" foot="เป้า 7 คลิป/สัปดาห์" />
        <StatTile k="ค่าคอมสัปดาห์นี้" :v="money(commission)" :foot="`${conversions.length} ออเดอร์`" />
        <StatTile k="งานค้าง" :v="num(open.length)" :foot="`จากทั้งหมด ${tasks.length}`" />
        <StatTile k="ออเดอร์สัปดาห์นี้" :v="num(conversions.length)" />
      </div>

      <div class="card">
        <div class="card-head">
          <h2>สิ่งที่ต้องทำ</h2>
          <span class="hint">สร้างอัตโนมัติทุกเช้าวันจันทร์</span>
        </div>

        <EmptyState
          v-if="!tasks.length"
          icon="🗒️"
          title="ยังไม่มีงานในสัปดาห์นี้"
          body="งานถูกสร้างโดย rules engine ซึ่งยังไม่ได้เขียน และต้องมีข้อมูลจริงก่อนถึงจะแนะนำอะไรได้ ระหว่างนี้ให้ยึดเป้าง่ายๆ คือลงคลิปให้ครบ 7 คลิป"
        />

        <div v-else class="card-body" style="padding-top:6px">
          <label v-for="t in tasks" :key="t.id" class="row"
                 style="padding:9px 0; border-bottom:1px solid var(--border); align-items:flex-start">
            <input type="checkbox" class="check" :checked="t.done" @change="toggle(t)" />
            <span style="flex:1; min-width:0">
              <span :class="{ done: t.done }">{{ t.title }}</span>
              <span v-if="t.reason" class="muted" style="display:block; font-size:12.5px">{{ t.reason }}</span>
            </span>
            <span class="pill" :class="PRIORITY[t.priority]">{{ t.priority }}</span>
          </label>
        </div>
      </div>
    </AsyncBlock>
  </div>
</template>
