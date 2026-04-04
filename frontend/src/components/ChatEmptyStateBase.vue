<script setup lang="ts">
defineProps<{
  muted?: boolean
}>()
</script>

<template>
  <div class="chat-placeholder-inner" :class="{ 'is-muted': muted }">
    <div class="chat-placeholder-icon">
      <svg width="72" height="72" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.25" aria-hidden="true">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
      </svg>
    </div>
    <div class="chat-placeholder-title">ОБЕРІТЬ ЧАТ</div>
    <div class="chat-placeholder-text">щоб почати спілкування</div>
  </div>
</template>

<style scoped>
.chat-placeholder-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  transition: opacity 0.65s ease, transform 0.85s ease, filter 0.85s ease;
}

.chat-placeholder-inner.is-muted {
  opacity: 0.16;
  transform: scale(0.975) translateY(8px);
  filter: blur(1px);
}

.chat-placeholder-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--yellow);
  filter: drop-shadow(0 0 18px rgba(245, 197, 24, 0.45));
}

.chat-placeholder-title {
  font-family: var(--font-display), sans-serif;
  font-size: 18px;
  letter-spacing: 4px;
  color: var(--white);
}

.chat-placeholder-text {
  font-family: var(--font-body), sans-serif;
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--gray);
  text-align: center;
}
</style>
