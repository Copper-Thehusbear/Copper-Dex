<script setup>
import { ref, computed, onMounted, reactive } from 'vue';
import { api } from '../api.js';
import { configured } from '../supabase.js';
import { money, num, date } from '../lib/format.js';
import {
  PLATFORMS, ANGLES, PLACEMENTS, platformLabel, angleLabel,
  isoWeek, newContentId,
} from '../lib/taxonomy.js';
import EmptyState from '../components/EmptyState.vue';
import AsyncBlock from '../components/AsyncBlock.vue';

const loading = ref(true);
const error = ref(null);
const saving = ref(false);
const contents = ref([]);
const commissions = ref([]);
const showForm = ref(false);
const editing = ref(false);
const copied = ref('');

function blank() {
  return {
    content_id: newContentId(),
    platform: 'tt',
    angle: 'bna',
    batch: isoWeek(),
    placement: 'bio',
    hook: '',
    item_ids: '',
    published_at: '',
    production_minutes: '',
    views: '',
  };
}
const form = reactive(blank());

async function load() {
  if (!configured) { loading.value = false; return; }
  loading.value = true; error.value = null;
  try {
    const [c, m] = await Promise.all([api.contents(), api.commissionByContent()]);
    contents.value = c; commissions.value = m;
  } catch (e) { error.value = e; }
  finally { loading.value = false; }
}
onMounted(load);

/** ค่าคอมรวมต่อ content_id — sub_id2 คือ join key หลัก */
const moneyBy = computed(() => {
  const map = new Map();
  for (const r of commissions.value) {
    if (!r.sub_id2) continue;
    map.set(r.sub_id2, (map.get(r.sub_id2) ?? 0) + Number(r.commission ?? 0));
  }
  return map;
});

const rows = computed(() => contents.value.map(c => ({
  ...c,
  commission: moneyBy.value.get(c.content_id) ?? 0,
})));

/** subId 5 ช่องที่ต้องกรอกในหน้าสร้างลิงก์ของ Shopee */
const subIds = computed(() => [
  form.platform, form.content_id, form.angle, form.batch, form.placement,
]);

function openNew() {
  Object.assign(form, blank());
  editing.value = false; showForm.value = true;
}

function openEdit(c) {
  Object.assign(form, {
    content_id: c.content_id,
    platform: c.platform ?? 'tt',
    angle: c.angle ?? 'bna',
    batch: c.batch ?? isoWeek(),
    placement: c.placement ?? 'bio',
    hook: c.hook ?? '',
    item_ids: (c.item_ids ?? []).join(', '),
    published_at: c.published_at ? c.published_at.slice(0, 16) : '',
    production_minutes: c.production_minutes ?? '',
    views: c.views ?? '',
  });
  editing.value = true; showForm.value = true;
}

function regenerate() {
  form.content_id = newContentId();
}

async function copy(text) {
  try {
    await navigator.clipboard.writeText(text);
    copied.value = text;
    setTimeout(() => { if (copied.value === text) copied.value = ''; }, 1500);
  } catch { /* clipboard ถูกบล็อก — ผู้ใช้ copy เองได้ */ }
}

const numOrNull = v => (v === '' || v == null ? null : Number(v));

async function save() {
  saving.value = true; error.value = null;
  try {
    await api.saveContent({
      content_id: form.content_id.trim(),
      platform: form.platform,
      angle: form.angle,
      batch: form.batch.trim() || null,
      placement: form.placement,
      hook: form.hook.trim() || null,
      item_ids: form.item_ids
        .split(',').map(s => s.trim()).filter(Boolean).map(Number).filter(n => Number.isFinite(n)),
      published_at: form.published_at ? new Date(form.published_at).toISOString() : null,
      production_minutes: numOrNull(form.production_minutes),
      views: numOrNull(form.views),
      metrics_updated_at: form.views === '' ? null : new Date().toISOString(),
    });
    showForm.value = false;
    await load();
  } catch (e) { error.value = e; }
  finally { saving.value = false; }
}

async function remove(c) {
  if (!confirm(`ลบคลิป ${c.content_id}?\n\nถ้ามีออเดอร์ผูกอยู่ ข้อมูลออเดอร์ไม่หาย แต่จะผูกกลับมาหาคลิปนี้ไม่ได้อีก`)) return;
  try { await api.deleteContent(c.content_id); await load(); }
  catch (e) { error.value = e; }
}
</script>

<template>
  <div class="stack">
    <div class="spread">
      <div>
        <h2>คลิป</h2>
        <div class="muted" style="font-size:12.5px">{{ contents.length }} คลิปในระบบ</div>
      </div>
      <div class="row">
        <button class="btn sm ghost" @click="load" :disabled="loading">รีเฟรช</button>
        <button class="btn sm primary" @click="openNew">+ เพิ่มคลิป</button>
      </div>
    </div>

    <!-- ฟอร์ม -->
    <div v-if="showForm" class="card">
      <div class="card-head">
        <h2>{{ editing ? 'แก้ไขคลิป' : 'คลิปใหม่' }}</h2>
        <button class="btn sm ghost" @click="showForm = false">ปิด</button>
      </div>
      <div class="card-body">
        <label class="field">
          <span>content_id — ค่านี้ไปอยู่ใน Sub_id2 และเป็น join key หลัก</span>
          <div class="row" style="flex-wrap:nowrap">
            <input v-model="form.content_id" class="mono" :readonly="editing" />
            <button v-if="!editing" class="btn sm ghost" type="button" @click="regenerate">สุ่มใหม่</button>
          </div>
        </label>

        <div class="grid2">
          <label class="field">
            <span>แพลตฟอร์ม (Sub_id1)</span>
            <select v-model="form.platform">
              <option v-for="p in PLATFORMS" :key="p.code" :value="p.code">{{ p.label }} · {{ p.code }}</option>
            </select>
          </label>
          <label class="field">
            <span>มุมคอนเทนต์ (Sub_id3)</span>
            <select v-model="form.angle">
              <option v-for="a in ANGLES" :key="a.code" :value="a.code">{{ a.label }} · {{ a.code }}</option>
            </select>
          </label>
          <label class="field">
            <span>batch (Sub_id4)</span>
            <input v-model="form.batch" class="mono" />
          </label>
          <label class="field">
            <span>ตำแหน่งลิงก์ (Sub_id5)</span>
            <select v-model="form.placement">
              <option v-for="p in PLACEMENTS" :key="p.code" :value="p.code">{{ p.label }} · {{ p.code }}</option>
            </select>
          </label>
        </div>

        <!-- แผงคัดลอก subId ไปวางในหน้าสร้างลิงก์ของ Shopee -->
        <div class="card" style="background:var(--surface-2); box-shadow:none; margin-bottom:14px">
          <div class="card-body">
            <div class="spread" style="margin-bottom:8px">
              <h3>เอาไปวางในช่อง Sub_id ของ Shopee</h3>
              <button class="btn sm" type="button" @click="copy(subIds.join('\t'))">คัดลอกทั้งหมด</button>
            </div>
            <div v-for="(v, i) in subIds" :key="i" class="row" style="justify-content:space-between; padding:3px 0">
              <span class="muted" style="font-size:12.5px">Sub_id{{ i + 1 }}</span>
              <button class="btn sm ghost mono" type="button" @click="copy(v)">
                {{ v }}{{ copied === v ? ' ✓' : '' }}
              </button>
            </div>
          </div>
        </div>

        <label class="field">
          <span>hook / ประโยคเปิด</span>
          <input v-model="form.hook" placeholder="เช่น ใช้มา 2 อาทิตย์ ผลลัพธ์คือ…" />
        </label>

        <label class="field">
          <span>item_ids — รหัสสินค้าในคลิป คั่นด้วย ,</span>
          <input v-model="form.item_ids" class="mono" placeholder="123456789, 987654321" />
        </label>

        <div class="grid2">
          <label class="field">
            <span>เวลาที่ลง</span>
            <input v-model="form.published_at" type="datetime-local" />
          </label>
          <label class="field">
            <span>เวลาที่ใช้ผลิต (นาที)</span>
            <input v-model="form.production_minutes" type="number" min="0" />
          </label>
        </div>

        <label class="field">
          <span>ยอดวิว — กลับมาอัปเดตทีหลังได้</span>
          <input v-model="form.views" type="number" min="0" />
        </label>

        <div class="row">
          <button class="btn primary" @click="save" :disabled="saving || !form.content_id.trim()">
            {{ saving ? 'กำลังบันทึก…' : 'บันทึก' }}
          </button>
          <button class="btn ghost" @click="showForm = false">ยกเลิก</button>
        </div>
      </div>
    </div>

    <AsyncBlock :loading="loading" :error="error">
      <div class="card">
        <EmptyState
          v-if="!rows.length"
          icon="🎬"
          title="ยังไม่มีคลิป"
          body="กด “เพิ่มคลิป” เพื่อสร้าง content_id แล้วเอา Sub_id ทั้ง 5 ช่องไปวางในหน้าสร้างลิงก์ของ Shopee — ใส่ย้อนหลังไม่ได้ คลิปที่ยิงด้วยลิงก์เปล่าคือข้อมูลที่หายถาวร"
        >
          <template #cta><button class="btn primary" @click="openNew">+ เพิ่มคลิป</button></template>
        </EmptyState>

        <template v-else>
          <!-- มือถือ: การ์ด (ตารางกว้างเกินจอ ต้องเลื่อนแนวนอนซึ่งพลาดง่าย) -->
          <div class="list only-mobile">
            <div v-for="c in rows" :key="c.content_id" class="item">
              <div class="spread">
                <span class="title mono">{{ c.content_id }}</span>
                <span class="pill" :class="{ good: c.commission }">
                  {{ c.commission ? money(c.commission) : 'ยังไม่มีออเดอร์' }}
                </span>
              </div>
              <div class="meta">
                <span>{{ platformLabel(c.platform) }}</span>
                <span>{{ angleLabel(c.angle) }}</span>
                <span>{{ date(c.published_at) }}</span>
                <span v-if="c.views">วิว <b>{{ num(c.views) }}</b></span>
                <span v-else>ยังไม่ได้ใส่วิว</span>
              </div>
              <div class="acts">
                <button class="btn sm ghost" @click="openEdit(c)">แก้ไข</button>
                <button class="btn sm ghost" @click="remove(c)">ลบ</button>
              </div>
            </div>
          </div>

          <!-- จอใหญ่: ตาราง -->
          <div class="scroll only-desk">
            <table>
              <thead>
                <tr>
                  <th>content_id</th>
                  <th>แพลตฟอร์ม</th>
                  <th>มุม</th>
                  <th>ลงเมื่อ</th>
                  <th class="n">วิว</th>
                  <th class="n">ค่าคอม</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="c in rows" :key="c.content_id">
                  <td class="mono">{{ c.content_id }}</td>
                  <td>{{ platformLabel(c.platform) }}</td>
                  <td>{{ angleLabel(c.angle) }}</td>
                  <td>{{ date(c.published_at) }}</td>
                  <td class="n">{{ num(c.views) }}</td>
                  <td class="n">{{ c.commission ? money(c.commission) : '—' }}</td>
                  <td class="n">
                    <button class="btn sm ghost" @click="openEdit(c)">แก้</button>
                    <button class="btn sm ghost" @click="remove(c)">ลบ</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </div>
    </AsyncBlock>
  </div>
</template>
