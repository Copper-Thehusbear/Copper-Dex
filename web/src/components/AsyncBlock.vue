<script setup>
defineProps({ loading: Boolean, error: [String, Object, null] });
</script>

<template>
  <div v-if="loading" class="empty muted">กำลังโหลด…</div>
  <div v-else-if="error" class="banner bad">
    โหลดข้อมูลไม่สำเร็จ — {{ error.message ?? error }}
  </div>
  <slot v-else />
</template>
