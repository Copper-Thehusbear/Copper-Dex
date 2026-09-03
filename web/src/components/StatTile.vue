<script setup>
defineProps({ k: String, v: [String, Number], foot: String });
</script>

<template>
  <div class="card tile">
    <div class="k">{{ k }}</div>
    <div class="v">{{ v }}</div>
    <div v-if="foot" class="foot">{{ foot }}</div>
  </div>
</template>
