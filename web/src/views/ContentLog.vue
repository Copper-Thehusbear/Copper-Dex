<script setup>
import { ref, computed, onMounted, reactive } from 'vue';
import { api } from '../api.js';
import { configured } from '../supabase.js';
import { money, num, date } from '../lib/format.js';
import {
  PLATFORMS, ANGLES, PLACEMENTS, platformLabel, angleLabel,
  isoWeek, newContentId,
} from '../lib/taxonomy.js';
import { buildCaption, hookOptions, beats, auditCaption, HOOK_SECONDS } from '../lib/caption.js';
import { tiktokConfigured, uploadDraft, checkStatus, MAX_BYTES } from '../lib/tiktok.js';
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
const tiktokReady = ref(false);
const upload = ref({ file: null, step: '', error: '', publishId: '', status: '' });

function blank() {
  return {
    content_id: newContentId(),
    platform: 'tt',
    angle: 'bna',
    batch: isoWeek(),
    placement: 'bio',
    keyword: '',
    hook: '',
    caption: '',
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
    const [c, m, integ] = await Promise.all([
      api.contents(), api.commissionByContent(), api.integrations().catch(() => []),
    ]);
    contents.value = c; commissions.value = m;
    tiktokReady.value = tiktokConfigured && integ.some(i => i.provider === 'tiktok');
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

/** ── ตัวช่วยเขียน ─────────────────────────── */
const hooks = computed(() => hookOptions({ angle: form.angle, keyword: form.keyword, price: '' }));
const scriptBeats = computed(() => beats(form.angle));
const suggested = computed(() => buildCaption({
  angle: form.angle, keyword: form.keyword, placement: form.placement, price: '',
}));
const audit = computed(() => auditCaption({
  caption: form.caption || suggested.value, keyword: form.keyword, hook: form.hook,
}));
const auditPassed = computed(() => audit.value.filter(i => i.ok).length);

function useSuggestion() { form.caption = suggested.value; }

/** ── อัปเข้า TikTok เป็นดราฟ ───────────────── */
function pickFile(e) {
  const f = e.target.files?.[0] ?? null;
  upload.value = { ...upload.value, file: f, error: '', step: '' };
}

async function sendToTiktok() {
  const u = upload.value;
  u.error = ''; u.status = '';
  try {
    const { publish_id } = await uploadDraft(u.file, step => { u.step = step; });
    u.publishId = publish_id;
    await api.savePost({
      content_id: form.content_id,
      provider: 'tiktok',
      publish_id,
      status: 'PROCESSING_UPLOAD',
      caption: form.caption || suggested.value,
    });
    const res = await checkStatus(publish_id);
    u.status = res.status ?? 'PROCESSING_UPLOAD';
  } catch (e) {
    u.error = e.message;
    u.step = '';
  }
}

function openNew() {
  Object.assign(form, blank());
  upload.value = { file: null, step: '', error: '', publishId: '', status: '' };
  editing.value = false; showForm.value = true;
}

function openEdit(c) {
  Object.assign(form, {
    content_id: c.content_id,
    platform: c.platform ?? 'tt',
    angle: c.angle ?? 'bna',
    batch: c.batch ?? isoWeek(),
    placement: c.placement ?? 'bio',
    keyword: c.keyword ?? '',
    hook: c.hook ?? '',
    caption: c.caption ?? '',
    item_ids: (c.item_ids ?? []).join(', '),
    published_at: c.published_at ? c.published_at.slice(0, 16) : '',
    production_minutes: c.production_minutes ?? '',
    views: c.views ?? '',
  });
  upload.value = { file: null, step: '', error: '', publishId: '', status: '' };
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
      keyword: form.keyword.trim() || null,
      hook: form.hook.trim() || null,
      caption: form.caption.trim() || null,
      item_ids: form.item_ids
        .split(',').map(s => s.trim()).filter(Boolean).map(Number).filter(n => Number.isFinite(n)),
      published_at: form.published_at ? new Date(form.published_at).toISOString() : null,
      production_minutes: numOrNull(form.production_minutes),
      views: numOrNull(form.views),
      metrics_updated_at: form.views === '' ? null : new Date().toISOString(),
    });
    // ไม่ปิดฟอร์ม — สลับเป็นโหมดแก้เพื่อให้อัปวิดีโอต่อได้ทันที
    editing.value = true;
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
          <span>คีย์เวิร์ดหลัก — คำที่คนจะพิมพ์ค้นหา ไม่ใช่ชื่อแบรนด์</span>
          <input v-model="form.keyword" placeholder="เช่น ครีมกันแดด, หูฟังบลูทูธ" />
        </label>

        <label class="field">
          <span>hook — ประโยค {{ HOOK_SECONDS }} วิแรก</span>
          <input v-model="form.hook" placeholder="พูดคีย์เวิร์ดออกเสียงในประโยคนี้ด้วย" />
        </label>

        <div v-if="form.keyword" class="row" style="margin:-4px 0 12px">
          <button v-for="h in hooks" :key="h" type="button" class="btn sm ghost"
                  style="text-align:left; white-space:normal" @click="form.hook = h">{{ h }}</button>
        </div>

        <!-- สคริปต์ + แคปชัน -->
        <div class="card" style="background:var(--surface-2); box-shadow:none; margin-bottom:14px">
          <div class="card-body">
            <div class="spread" style="margin-bottom:10px">
              <h3>โครงคลิป · มุม{{ angleLabel(form.angle) }}</h3>
              <span class="pill" :class="auditPassed === audit.length ? 'good' : 'warn'">
                {{ auditPassed }}/{{ audit.length }}
              </span>
            </div>

            <div v-for="[t, what] in scriptBeats" :key="t" class="checkline" style="gap:10px">
              <span class="mono muted" style="flex:none; width:62px">{{ t }}</span>
              <span>{{ what }}</span>
            </div>

            <label class="field" style="margin:12px 0 8px">
              <span>แคปชัน</span>
              <textarea v-model="form.caption" rows="4"
                        :placeholder="suggested"></textarea>
            </label>
            <div class="row" style="margin-bottom:12px">
              <button class="btn sm" type="button" @click="useSuggestion">ใช้ที่แนะนำ</button>
              <button class="btn sm ghost" type="button" @click="copy(form.caption || suggested)">
                คัดลอกแคปชัน{{ copied === (form.caption || suggested) ? ' ✓' : '' }}
              </button>
            </div>

            <div v-for="i in audit" :key="i.label" class="checkline">
              <span style="flex:none">{{ i.ok ? '✅' : '⚠️' }}</span>
              <span>
                {{ i.label }}
                <span v-if="!i.ok" class="muted" style="display:block; font-size:12.5px">{{ i.hint }}</span>
              </span>
            </div>
          </div>
        </div>

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
          <button class="btn ghost" @click="showForm = false">ปิด</button>
        </div>

        <!-- อัปขึ้น TikTok เป็นดราฟ -->
        <div class="card" style="background:var(--surface-2); box-shadow:none; margin-top:14px">
          <div class="card-body">
            <div class="spread" style="margin-bottom:8px">
              <h3>ส่งขึ้น TikTok เป็นดราฟ</h3>
              <span class="pill">ไม่โพสต์เอง</span>
            </div>

            <p v-if="!tiktokReady" class="muted" style="font-size:13px">
              ยังไม่ได้เชื่อมบัญชี TikTok — ไปที่หน้าตั้งค่า (ไอคอนเฟือง) เพื่อเชื่อมต่อก่อน
            </p>

            <p v-else-if="!editing" class="muted" style="font-size:13px">
              กดบันทึกคลิปก่อน แล้วช่องอัปโหลดจะเปิดให้
            </p>

            <template v-else>
              <p class="muted" style="font-size:13px; margin-bottom:10px">
                ไฟล์วิ่งเข้า TikTok ตรงๆ ไม่ผ่านเซิร์ฟเวอร์เรา · ไม่เกิน
                {{ Math.round(MAX_BYTES / 1048576) }}MB · ไปโผล่ใน inbox ให้คุณกดโพสต์เองในแอป
              </p>
              <input type="file" accept="video/*" @change="pickFile" style="margin-bottom:10px" />
              <div class="row">
                <button class="btn primary" type="button"
                        :disabled="!upload.file || ['init','upload'].includes(upload.step)"
                        @click="sendToTiktok">
                  {{ { init: 'กำลังขอสิทธิ์อัป…', upload: 'กำลังส่งไฟล์…' }[upload.step] ?? 'ส่งเป็นดราฟ' }}
                </button>
                <span v-if="upload.status" class="pill good">{{ upload.status }}</span>
              </div>
              <div v-if="upload.error" class="banner bad" style="margin-top:10px">{{ upload.error }}</div>
              <div v-else-if="upload.step === 'done'" class="banner" style="margin-top:10px">
                ส่งแล้ว — เปิดแอป TikTok แล้วดูใน inbox เพื่อกดโพสต์
              </div>
            </template>
          </div>
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
