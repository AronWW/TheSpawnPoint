<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useAchievementStore } from '../stores/achievements'
import AchievementCard from '../components/AchievementCard.vue'

const achievementStore = useAchievementStore()
const filter = ref<'ALL' | 'UNLOCKED' | 'LOCKED' | 'STANDARD' | 'SECRET'>('ALL')

const achievements = computed(() => achievementStore.myAchievements)
const unlockedCount = computed(() => achievements.value.filter((item) => item.unlocked).length)
const secretUnlocked = computed(() => achievements.value.filter((item) => item.type === 'SECRET' && item.unlocked).length)

const filteredAchievements = computed(() => {
  switch (filter.value) {
    case 'UNLOCKED':
      return achievements.value.filter((item) => item.unlocked)
    case 'LOCKED':
      return achievements.value.filter((item) => !item.unlocked)
    case 'STANDARD':
      return achievements.value.filter((item) => item.type === 'STANDARD')
    case 'SECRET':
      return achievements.value.filter((item) => item.type === 'SECRET')
    default:
      return achievements.value
  }
})

const filterButtons: { value: typeof filter.value; label: string }[] = [
  { value: 'ALL', label: 'Усі' },
  { value: 'UNLOCKED', label: 'Відкриті' },
  { value: 'LOCKED', label: 'Закриті' },
  { value: 'STANDARD', label: 'Звичайні' },
  { value: 'SECRET', label: 'Секретні' },
]

onMounted(() => {
  achievementStore.fetchMyAchievements()
})
</script>

<template>
  <div class="achievements-page">
    <div class="achievements-container">
      <div class="section-head">
        <div class="section-title">
          ДОСЯГНЕННЯ
          <span class="section-count">{{ unlockedCount }}/{{ achievements.length }} відкрито</span>
        </div>

        <div class="achievements-summary">
          <div class="ach-summary-card">
            <span class="ach-summary-card__label">ВІДКРИТО</span>
            <strong>{{ unlockedCount }}/{{ achievements.length }}</strong>
          </div>
          <div class="ach-summary-card secret">
            <span class="ach-summary-card__label">СЕКРЕТІВ</span>
            <strong>{{ secretUnlocked }}</strong>
          </div>
        </div>
      </div>

      <div class="achievements-filters">
        <button
          v-for="btn in filterButtons"
          :key="btn.value"
          class="achievements-filter"
          :class="{ active: filter === btn.value }"
          @click="filter = btn.value"
        >
          {{ btn.label }}
        </button>
      </div>

      <div v-if="achievementStore.loading" class="achievements-empty">Завантаження досягнень...</div>

      <div v-else-if="filteredAchievements.length" class="achievements-grid">
        <AchievementCard
          v-for="achievement in filteredAchievements"
          :key="achievement.code"
          :achievement="achievement"
        />
      </div>

      <div v-else class="achievements-empty">
        Нічого не знайдено для цього фільтра.
      </div>
    </div>
  </div>
</template>

<style scoped>
.achievements-page {
  padding-top: 64px;
  min-height: 100vh;
  background: var(--black);
}
.achievements-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 64px 80px;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
  padding-bottom: 16px;
  border-bottom: 2px solid var(--border);
}
.achievements-summary {
  display: flex;
  gap: 12px;
}
.ach-summary-card {
  min-width: 130px;
  padding: 14px 16px;
  background: var(--panel);
  border: 1px solid rgba(245, 197, 24, 0.2);
}
.ach-summary-card.secret {
  border-color: rgba(125, 142, 255, 0.24);
}
.ach-summary-card__label {
  display: block;
  font-size: 10px;
  letter-spacing: 2px;
  color: var(--gray);
  margin-bottom: 6px;
}
.ach-summary-card strong {
  font-family: var(--font-display);
  font-size: 26px;
  color: var(--white);
}
.achievements-filters {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 22px;
}
.achievements-filter {
  padding: 10px 14px;
  border: 1px solid var(--border);
  background: var(--panel);
  color: var(--gray-light);
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 2px;
}
.achievements-filter.active,
.achievements-filter:hover {
  border-color: var(--yellow);
  color: var(--yellow);
}
.achievements-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}
.achievements-empty {
  min-height: 220px;
  display: grid;
  place-items: center;
  text-align: center;
  color: var(--gray-light);
  border: 1px dashed rgba(245, 197, 24, 0.15);
  background: rgba(255,255,255,0.02);
}
@media (max-width: 1100px) {
  .achievements-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 760px) {
  .achievements-container {
    padding: 24px 16px 60px;
  }
  .section-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  .achievements-summary {
    width: 100%;
  }
  .ach-summary-card {
    flex: 1;
  }
  .achievements-grid {
    grid-template-columns: 1fr;
  }
}
</style>
