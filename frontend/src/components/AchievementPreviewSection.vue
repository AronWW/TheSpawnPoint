<script setup lang="ts">
import type { AchievementPreview } from '../types'
import AchievementCard from './AchievementCard.vue'

const props = defineProps<{
  preview: AchievementPreview
  isOwnProfile: boolean
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
      <router-link to="/achievements" class="achievement-preview-link" v-if="isOwnProfile">
        УСІ ДОСЯГНЕННЯ →
      </router-link>
    </div>

    <div v-if="preview.items.length" class="achievement-preview-grid">
      <AchievementCard
        v-for="achievement in preview.items"
        :key="achievement.code"
        :achievement="achievement"
        compact
      />
    </div>

    <div v-else class="achievement-preview-empty">
      <span class="achievement-preview-empty__icon">🏆</span>
      <p v-if="isOwnProfile">У тебе ще немає відкритих досягнень. Саме час почати колекцію.</p>
      <p v-else>У цього гравця ще немає відкритих досягнень для показу.</p>
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
.achievement-preview-link {
  color: var(--yellow);
  font-family: var(--font-display);
  letter-spacing: 2px;
  font-size: 11px;
  border: 1px solid rgba(245, 197, 24, 0.22);
  padding: 10px 12px;
}
.achievement-preview-link:hover {
  border-color: var(--yellow);
  background: rgba(245, 197, 24, 0.08);
}
.achievement-preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.achievement-preview-empty {
  min-height: 150px;
  border: 1px dashed rgba(245, 197, 24, 0.18);
  display: grid;
  place-items: center;
  padding: 24px;
  text-align: center;
  color: var(--gray-light);
  background: rgba(255,255,255,0.02);
}
.achievement-preview-empty__icon {
  font-size: 28px;
  display: block;
  margin-bottom: 12px;
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
}
</style>
