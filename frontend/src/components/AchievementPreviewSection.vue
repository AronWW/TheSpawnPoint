<script setup lang="ts">
import type { AchievementPreview } from '../types'
import AchievementCard from './AchievementCard.vue'

defineProps<{
  preview: AchievementPreview
  isOwnProfile: boolean
}>()

const emit = defineEmits<{
  openCollection: []
  openPicker: []
}>()
</script>

<template>
  <section class="va-panel achievement-preview-panel">
    <div class="achievement-preview-header">
      <div>
        <div class="va-panel-title">ДОСЯГНЕННЯ</div>
        <p class="achievement-preview-subtitle">
          Відкрито {{ preview.unlockedCount }} з {{ preview.totalCount }}
        </p>
      </div>
      <div class="achievement-preview-actions">
        <router-link v-if="isOwnProfile" to="/achievements" class="achievement-preview-link">
          УСІ ДОСЯГНЕННЯ
        </router-link>
        <button v-else class="achievement-preview-link" type="button" @click="emit('openCollection')">
          УСІ ДОСЯГНЕННЯ
        </button>
        <button
          v-if="isOwnProfile"
          class="achievement-preview-link achievement-preview-link--muted"
          type="button"
          @click="emit('openPicker')"
        >
          НАЛАШТУВАТИ
        </button>
      </div>
    </div>

    <div v-if="preview.items.length" class="achievement-preview-grid">
      <AchievementCard
        v-for="achievement in preview.items"
        :key="achievement.code"
        :achievement="achievement"
        compact
      />
    </div>
  </section>
</template>

<style scoped>
.achievement-preview-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.achievement-preview-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}
.achievement-preview-subtitle {
  margin: 8px 0 0;
  color: var(--gray);
  font-size: 12px;
  letter-spacing: 1px;
  text-transform: uppercase;
}
.achievement-preview-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}
.achievement-preview-link {
  color: var(--yellow);
  font-family: var(--font-display);
  letter-spacing: 2px;
  font-size: 11px;
  border: 1px solid rgba(245, 197, 24, 0.22);
  padding: 10px 12px;
  background: transparent;
  cursor: pointer;
}
.achievement-preview-link:hover {
  border-color: var(--yellow);
  background: rgba(245, 197, 24, 0.08);
}
.achievement-preview-link--muted {
  color: var(--gray-light);
  border-color: var(--border);
}
.achievement-preview-link--muted:hover {
  color: var(--white);
  border-color: var(--white);
  background: rgba(255, 255, 255, 0.05);
}
.achievement-preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
@media (max-width: 900px) {
  .achievement-preview-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 640px) {
  .achievement-preview-header {
    flex-direction: column;
  }
  .achievement-preview-actions {
    justify-content: flex-start;
  }
}
</style>
