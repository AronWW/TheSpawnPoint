<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Achievement } from '../types'
import AchievementIcon from './AchievementIcon.vue'

const props = defineProps<{
  achievements: Achievement[]
  saving: boolean
}>()

const emit = defineEmits<{
  close: []
  save: [codes: string[]]
}>()

const selectedCodes = ref<string[]>([])

const unlockedAchievements = computed(() =>
  props.achievements
    .filter((item) => item.unlocked)
    .sort((a, b) => a.order - b.order),
)

watch(
  () => props.achievements,
  (achievements) => {
    selectedCodes.value = achievements
      .filter((item) => item.unlocked && item.featuredPosition !== null)
      .sort((a, b) => (a.featuredPosition ?? 0) - (b.featuredPosition ?? 0))
      .map((item) => item.code)
  },
  { immediate: true },
)

function selectedNumber(code: string) {
  const index = selectedCodes.value.indexOf(code)
  return index >= 0 ? index + 1 : null
}

function toggle(code: string) {
  const currentIndex = selectedCodes.value.indexOf(code)
  if (currentIndex >= 0) {
    selectedCodes.value = selectedCodes.value.filter((item) => item !== code)
    return
  }

  if (selectedCodes.value.length >= 4) return
  selectedCodes.value = [...selectedCodes.value, code]
}

function save() {
  emit('save', selectedCodes.value)
}
</script>

<template>
  <div class="featured-picker-overlay" @click.self="emit('close')">
    <section class="featured-picker">
      <header class="featured-picker__header">
        <div>
          <h3>ВІТРИНА ДОСЯГНЕНЬ</h3>
          <p>Обрано {{ selectedCodes.length }} з 4</p>
        </div>
        <button class="featured-picker__close" type="button" @click="emit('close')" aria-label="Закрити">×</button>
      </header>

      <div class="featured-picker__list">
        <button
          v-for="achievement in unlockedAchievements"
          :key="achievement.code"
          class="featured-picker__item"
          :class="{ selected: selectedNumber(achievement.code) !== null }"
          type="button"
          @click="toggle(achievement.code)"
        >
          <span class="featured-picker__icon">
            <AchievementIcon :code="achievement.code" :size="34" />
            <span v-if="selectedNumber(achievement.code)" class="featured-picker__number">
              {{ selectedNumber(achievement.code) }}
            </span>
          </span>
          <span class="featured-picker__meta">
            <span class="featured-picker__type">{{ achievement.type === 'SECRET' ? 'СЕКРЕТ' : 'ДОСЯГНЕННЯ' }}</span>
            <span class="featured-picker__title">{{ achievement.title }}</span>
          </span>
        </button>
      </div>

      <footer class="featured-picker__footer">
        <button class="featured-picker__button secondary" type="button" @click="emit('close')">
          СКАСУВАТИ
        </button>
        <button class="featured-picker__button primary" type="button" :disabled="saving" @click="save">
          {{ saving ? '...' : 'ЗБЕРЕГТИ' }}
        </button>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.featured-picker-overlay {
  position: fixed;
  inset: 0;
  z-index: 1130;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 22px;
  background: rgba(0, 0, 0, 0.82);
}
.featured-picker {
  width: min(760px, 100%);
  max-height: 86vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #111214;
  border: 2px solid var(--border);
  box-shadow: 0 22px 54px rgba(0, 0, 0, 0.58);
}
.featured-picker__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.featured-picker__header h3 {
  margin: 0;
  color: var(--yellow);
  font-family: var(--font-display);
  font-size: 17px;
  letter-spacing: 2px;
}
.featured-picker__header p {
  margin: 8px 0 0;
  color: var(--gray-light);
  font-size: 12px;
  letter-spacing: 1px;
  text-transform: uppercase;
}
.featured-picker__close {
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
.featured-picker__close:hover {
  border-color: var(--red);
  color: var(--red);
}
.featured-picker__list {
  overflow: auto;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 16px 20px;
}
.featured-picker__item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 78px;
  padding: 13px;
  border: 1px solid var(--border);
  background: var(--dark);
  color: var(--white);
  text-align: left;
  cursor: pointer;
}
.featured-picker__item:hover,
.featured-picker__item.selected {
  border-color: rgba(245, 197, 24, 0.55);
  background: rgba(245, 197, 24, 0.07);
}
.featured-picker__icon {
  position: relative;
  flex: 0 0 auto;
}
.featured-picker__number {
  position: absolute;
  right: -8px;
  bottom: -8px;
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  background: var(--yellow);
  color: var(--black);
  border: 2px solid #111214;
  font-family: var(--font-display);
  font-size: 11px;
}
.featured-picker__meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.featured-picker__type {
  color: var(--gray);
  font-family: var(--font-display);
  font-size: 10px;
  letter-spacing: 1.6px;
}
.featured-picker__title {
  color: var(--white);
  font-weight: 700;
  line-height: 1.25;
}
.featured-picker__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}
.featured-picker__button {
  border: 1px solid var(--border);
  padding: 11px 14px;
  background: var(--dark);
  color: var(--gray-light);
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 1.8px;
  cursor: pointer;
}
.featured-picker__button.primary {
  border-color: var(--yellow);
  color: var(--yellow);
}
.featured-picker__button.primary:hover:not(:disabled) {
  background: var(--yellow);
  color: var(--black);
}
.featured-picker__button.secondary:hover {
  border-color: var(--white);
  color: var(--white);
}
.featured-picker__button:disabled {
  opacity: 0.65;
  cursor: default;
}
@media (max-width: 720px) {
  .featured-picker-overlay {
    align-items: stretch;
    padding: 12px;
  }
  .featured-picker {
    max-height: none;
  }
  .featured-picker__list {
    grid-template-columns: 1fr;
    padding: 14px;
  }
  .featured-picker__header,
  .featured-picker__footer {
    padding-left: 14px;
    padding-right: 14px;
  }
}
</style>
