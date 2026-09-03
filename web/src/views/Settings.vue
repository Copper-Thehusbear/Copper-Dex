<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import { api } from '../api.js';
import { configured } from '../supabase.js';
import { tiktokConfigured, connectUrl, disconnect, creatorInfo } from '../lib/tiktok.js';
import { date } from '../lib/format.js';
import AsyncBlock from '../components/AsyncBlock.vue';

const route = useRoute();
const loading = ref(true);
const error = ref(null);
const busy = ref(false);
const integrations = ref([]);
const creator = ref(null);

const CALLBACK_MSG = {
  ok: ['good', 'เชื่อมต่อ TikTok สำเร็จ'],
  denied: ['bad', 'คุณกดปฏิเสธสิทธิ์ที่หน้า TikTok'],
  badstate: ['bad', 'state ไม่ถูกต้องหรือหมดอายุ — ลองเชื่อมต่อใหม่'],
};
const callback = computed(() => CALLBACK_MSG[route.query.tiktok] ?? null);

const tiktok = computed(() => integrations.value.find(i => i.provider === 'tiktok') ?? null);

async function load() {
  if (!configured) { loading.value = false; return; }
  loading.value = true; error.value = null;
  try { integrations.value = await api.integrations(); }
  catch (e) { error.value = e; }
  finally { loading.value = false; }
}
onMounted(load);

async function loadCreator() {
  busy.value = true; error.value = null;
  try { creator.value = (await creatorInfo())?.data ?? null; }
  catch (e) { error.value = e; }
  finally { busy.value = false; }
}

async function unlink() {
  if (!confirm('ตัดการเชื่อมต่อ TikTok?')) return;
  busy.value = true; error.value = null;
  try { await disconnect(); creator.value = null; await load(); }
  catch (e) { error.value = e; }
  finally { busy.value = false; }
}
</script>

<template>
  <div class="stack">
    <div>
      <h2>ตั้งค่า</h2>
      <div class="muted" style="font-size:12.5px">การเชื่อมต่อกับแพลตฟอร์มภายนอก</div>
    </div>

    <div v-if="callback" class="banner" :class="{ bad: callback[0] === 'bad' }">{{ callback[1] }}</div>

    <AsyncBlock :loading="loading" :error="error">
      <div class="card">
        <div class="card-head">
          <h2>TikTok</h2>
          <span class="hint">อัปคลิปเข้า inbox เป็นดราฟ</span>
        </div>
        <div class="card-body">
          <div v-if="!tiktokConfigured" class="banner">
            ยังไม่ได้ตั้งค่า <code>VITE_TIKTOK_FN_URL</code> และ <code>VITE_APP_TOKEN</code>
            ตอน build — ต้องสร้าง app ที่ TikTok for Developers และ deploy Edge Function ก่อน
          </div>

          <template v-else-if="tiktok">
            <div class="row" style="margin-bottom:12px">
              <img v-if="tiktok.avatar_url" :src="tiktok.avatar_url" alt=""
                   width="40" height="40" style="border-radius:99px" />
              <div>
                <div style="font-weight:600">{{ tiktok.display_name ?? 'เชื่อมต่อแล้ว' }}</div>
                <div class="muted" style="font-size:12.5px">
                  เชื่อมเมื่อ {{ date(tiktok.connected_at) }} ·
                  <span :class="tiktok.token_valid ? 'pill good' : 'pill warn'">
                    {{ tiktok.token_valid ? 'token ใช้ได้' : 'token หมดอายุ (ต่ออายุอัตโนมัติตอนใช้)' }}
                  </span>
                </div>
              </div>
            </div>

            <div class="row">
              <button class="btn sm" @click="loadCreator" :disabled="busy">ดูข้อจำกัดการโพสต์</button>
              <button class="btn sm ghost" @click="unlink" :disabled="busy">ตัดการเชื่อมต่อ</button>
            </div>

            <div v-if="creator" class="card" style="background:var(--surface-2); box-shadow:none; margin-top:12px">
              <div class="card-body">
                <div class="meta" style="display:flex; flex-wrap:wrap; gap:6px 16px; font-size:13px">
                  <span>ความยาวสูงสุด <b>{{ creator.max_video_post_duration_sec ?? '—' }}</b> วิ</span>
                  <span>บัญชี <b>{{ creator.privacy_level_options?.join(', ') ?? '—' }}</b></span>
                  <span v-if="creator.creator_nickname">ชื่อ <b>{{ creator.creator_nickname }}</b></span>
                </div>
              </div>
            </div>
          </template>

          <template v-else>
            <p class="muted" style="font-size:13.5px; margin-bottom:12px">
              เชื่อมบัญชีแล้วจะอัปคลิปจากหน้า “คลิป” เข้า inbox ของ TikTok เป็นดราฟได้
              คุณยังต้องกดโพสต์เองในแอป — ตั้งใจให้เป็นแบบนี้เพื่อเลี่ยง audit ของ TikTok
              ที่บังคับให้คอนเทนต์เป็น private ถ้ายังไม่ผ่านการตรวจ
            </p>
            <a class="btn primary" :href="connectUrl()">เชื่อมต่อ TikTok</a>
          </template>
        </div>
      </div>

      <div class="card">
        <div class="card-head"><h2>Shopee Video</h2></div>
        <div class="card-body">
          <p class="muted" style="font-size:13.5px">
            โพสต์อัตโนมัติไม่ได้ — Shopee Affiliate Open API มีแค่ค้นสินค้า ดูรายงาน และสร้างลิงก์
            ส่วน API อัปวิดีโอของ Shopee อยู่ฝั่งผู้ขาย (Open Platform) ซึ่ง affiliate เข้าไม่ถึง
            <br />ต้องอัปเองในแอป แล้วมาบันทึกที่หน้า “คลิป”
          </p>
        </div>
      </div>
    </AsyncBlock>
  </div>
</template>
