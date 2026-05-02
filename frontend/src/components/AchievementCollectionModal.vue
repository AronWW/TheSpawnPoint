<script setup lang="ts">
import { computed, ref } from 'vue'
import type { AchievementCollection, AchievementType } from '../types'
import AchievementCard from './AchievementCard.vue'

const props = defineProps<{
  title: string
  collection: AchievementCollection | null
  loading: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const filter = ref<'ALL' | AchievementType>('ALL')

const progressPercent = computed(() => {
  const total = props.collection?.totalCount ?? 0
  if (!total) return 0
  return Math.round(((props.collection?.unlockedCount ?? 0) / total) * 100)
})

const filteredItems = computed(() => {
  const items = props.collection?.items ?? []
  if (filter.value === 'ALL') return items
  return items.filter((item) => item.type === filter.value)
})

const filters: { value: 'ALL' | AchievementType; label: string }[] = [
  { value: 'ALL', label: 'Усі' },
  { value: 'STANDARD', label: 'Звичайні' },
  { value: 'SECRET', label: 'Секретні' },
]
</script>

<template>
  <div class="achievement-modal-overlay" @click.self="emit('close')">
    <section class="achievement-modal">
      <header class="achievement-modal__header">
        <div class="achievement-modal__title-wrap">
          <h3>{{ title }}</h3>
          <p v-if="collection" class="achievement-modal__summary">
            Відкрито {{ collection.unlockedCount }} з {{ collection.totalCount }} • {{ progressPercent }}%
          </p>
        </div>
        <button class="achievement-modal__close" type="button" @click="emit('close')" aria-label="Закрити">×</button>
      </header>

      <div v-if="collection" class="achievement-modal__progress" aria-hidden="true">
        <div class="achievement-modal__progress-fill" :style="{ width: `${progressPercent}%` }"></div>
      </div>

      <div class="achievement-modal__filters">
        <button
          v-for="item in filters"
          :key="item.value"
          class="achievement-modal__filter"
          :class="{ active: filter === item.value }"
          type="button"
          @click="filter = item.value"
        >
          {{ item.label }}
        </button>
      </div>

      <div v-if="loading" class="achievement-modal__state">Завантаження...</div>
      <div v-else-if="filteredItems.length" class="achievement-modal__grid">
        <AchievementCard
          v-for="achievement in filteredItems"
          :key="achievement.code"
          :achievement="achievement"
          compact
        />
      </div>
      <div v-else class="achievement-modal__state">Немає відкритих досягнень.</div>
    </section>
  </div>
</template>

<style scoped>
.achievement-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1120;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 22px;
  background: rgba(0, 0, 0, 0.82);
}
.achievement-modal {
  width: min(920px, 100%);
  max-height: 86vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #111214;
  border: 2px solid var(--border);
  box-shadow: 0 22px 54px rgba(0, 0, 0, 0.58);
}
.achievement-modal__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.achievement-modal__title-wrap {
  min-width: 0;
}
.achievement-modal__title-wrap h3 {
  margin: 0;
  color: var(--yellow);
  font-family: var(--font-display);
  font-size: 17px;
  letter-spacing: 2px;
  text-transform: uppercase;
}
.achievement-modal__summary {
  margin: 8px 0 0;
  color: var(--gray-light);
  font-size: 12px;
  letter-spacing: 1px;
  text-transform: uppercase;
}
.achievement-modal__close {
  flex: 0 0 auto;
  width: 34px;
  height: 34px;
  border: 1px solid var(--border);
  background: var(--dark);
  color: var(--gray-light);
  cursor: pointer;
  font-size: 22px;
  line-height: 1;
}
.achievement-modal__close:hover {
  border-color: var(--red);
  color: var(--red);
}
.achievement-modal__progress {
  height: 8px;
  background: #1b1d21;
}
.achievement-modal__progress-fill {
  height: 100%;
  background: var(--yellow);
}
.achievement-modal__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 14px 20px 0;
}
.achievement-modal__filter {
  border: 1px solid var(--border);
  background: var(--dark);
  color: var(--gray-light);
  padding: 9px 12px;
  font-family: var(--font-display);
  font-size: 10px;
  letter-spacing: 1.6px;
  cursor: pointer;
}
.achievement-modal__filter.active,
.achievement-modal__filter:hover {
  border-color: var(--yellow);
  color: var(--yellow);
  background: rgba(245, 197, 24, 0.07);
}
.achievement-modal__grid {
  overflow: auto;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  padding: 18px 20px 20px;
}
.achievement-modal__state {
  min-height: 220px;
  display: grid;
  place-items: center;
  padding: 26px;
  color: var(--gray-light);
  text-align: center;
}
@media (max-width: 760px) {
  .achievement-modal-overlay {
    align-items: stretch;
    padding: 12px;
  }
  .achievement-modal {
    max-height: none;
  }
  .achievement-modal__grid {
    grid-template-columns: 1fr;
    padding: 16px 14px;
  }
  .achievement-modal__header,
  .achievement-modal__filters {
    padding-left: 14px;
    padding-right: 14px;
  }
}
</style>
