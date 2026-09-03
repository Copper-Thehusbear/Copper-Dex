<script setup>
defineProps({ icon: { type: String, default: '📭' }, title: String, body: String });
</script>

<template>
  <div class="empty">
    <div class="icon">{{ icon }}</div>
    <h3>{{ title }}</h3>
    <p>{{ body }}</p>
    <div v-if="$slots.cta" class="cta"><slot name="cta" /></div>
  </div>
</template>
