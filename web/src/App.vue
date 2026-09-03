<script setup>
import { configured } from './supabase.js';
</script>

<template>
  <div class="shell">
    <header class="topbar">
      <div class="row" style="gap:10px">
        <div class="mark">S</div>
        <div>
          <h1>Shopee Afl</h1>
          <div class="sub">แดชบอร์ดแอฟฟิลิเอต</div>
        </div>
      </div>

      <nav class="tabs">
        <RouterLink to="/">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
          <span>สัปดาห์นี้</span>
        </RouterLink>
        <RouterLink to="/radar">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="4"/><path d="M12 3v4M12 17v4M3 12h4M17 12h4"/></svg>
          <span>เรดาร์</span>
        </RouterLink>
        <RouterLink to="/portfolio">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/></svg>
          <span>พอร์ต</span>
        </RouterLink>
        <RouterLink to="/log">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h13l3 3v13a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z"/><path d="M8 9h8M8 13h8M8 17h5"/></svg>
          <span>คลิป</span>
        </RouterLink>
      </nav>
    </header>

    <main class="content">
      <div v-if="!configured" class="banner bad">
        ยังไม่ได้ตั้งค่าการเชื่อมต่อ — ใส่ <code>VITE_SUPABASE_URL</code> และ
        <code>VITE_SUPABASE_ANON_KEY</code> แล้ว build ใหม่
      </div>
      <RouterView v-slot="{ Component }">
        <component :is="Component" />
      </RouterView>
    </main>
  </div>
</template>
