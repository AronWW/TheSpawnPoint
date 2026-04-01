<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useAchievementStore } from '../stores/achievements'
import AchievementCard from '../components/AchievementCard.vue'

const achievementStore = useAchievementStore()
const filter = ref<'ALL' | 'UNLOCKED' | 'LOCKED' | 'STANDARD' | 'SECRET'>('ALL')

const achievements = computed(() => achievementStore.myAchievements)
const unlockedCount = computed(() => achievements.value.filter((item) => item.unlocked).length)
const secretUnlocked = computed(() => achievements.value.filter((item) => item.type === 'SECRET' && item.unlocked).length)
const completionPercent = computed(() => {
  if (!achievements.value.length) return 0
  return Math.round((unlockedCount.value / achievements.value.length) * 100)
})

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
      <section class="achievements-hero ink-panel">
        <div class="achievements-hero__head">
          <div class="achievements-hero__copy">
            <span class="achievements-hero__eyebrow">ПРОФІЛЬ</span>
            <h1 class="achievements-hero__title">Досягнення</h1>
            <p class="achievements-hero__subtitle">
              Усі відкриті віхи, прогрес по лобі та чату, і приховані секрети — в одному місці.
            </p>
          </div>

          <div class="achievements-hero__score">
            <span class="achievements-hero__score-value">{{ completionPercent }}%</span>
            <span class="achievements-hero__score-label">завершено</span>
          </div>
        </div>

        <div class="achievements-hero__progress-row">
          <span>{{ unlockedCount }} / {{ achievements.length }} відкрито</span>
          <span>{{ secretUnlocked }} секретних знайдено</span>
        </div>

        <div class="achievements-hero__progress-bar" aria-hidden="true">
          <div class="achievements-hero__progress-fill" :style="{ width: `${completionPercent}%` }"></div>
        </div>
      </section>

      <div class="achievements-toolbar">
        <div class="section-head achievements-toolbar__head">
          <div class="section-title achievements-toolbar__title">
            Каталог
            <span class="section-count">{{ filteredAchievements.length }} записів</span>
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
      </div>

      <div v-if="achievementStore.loading" class="achievements-empty">Завантаження досягнень...</div>

      <div v-else-if="filteredAchievements.length" class="achievements-grid">
        <AchievementCard
          v-for="achievement in filteredAchievements"
          :key="achievement.code"
          :achievement="achievement"
          :allow-secret-interaction="true"
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
  min-height: 100vh;
  padding-top: 64px;
  background: var(--black);
}

.achievements-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 34px 64px 80px;
}

.achievements-hero {
  margin-bottom: 24px;
  padding: 28px 30px 26px;
  background: #111214;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.achievements-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.achievements-hero__copy {
  min-width: 0;
}

.achievements-hero__eyebrow {
  display: inline-block;
  margin-bottom: 12px;
  color: var(--gray);
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 2.2px;
}

.achievements-hero__title {
  margin: 0 0 10px;
  color: var(--white);
  font-family: var(--font-display);
  font-size: 42px;
  line-height: 0.95;
  letter-spacing: 2.6px;
  text-transform: uppercase;
}

.achievements-hero__subtitle {
  margin: 0;
  max-width: 720px;
  color: var(--gray-light);
  font-size: 15px;
  line-height: 1.55;
}

.achievements-hero__score {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  min-width: 120px;
}

.achievements-hero__score-value {
  color: var(--yellow);
  font-family: var(--font-display);
  font-size: 44px;
  line-height: 1;
}

.achievements-hero__score-label {
  color: var(--gray);
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.achievements-hero__progress-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  color: var(--gray-light);
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 1.7px;
  text-transform: uppercase;
}

.achievements-hero__progress-bar {
  height: 10px;
  overflow: hidden;
  background: #1b1d21;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.achievements-hero__progress-fill {
  height: 100%;
  background: var(--yellow);
}

.achievements-toolbar {
  margin-bottom: 22px;
}

.achievements-toolbar__head {
  margin-bottom: 16px;
}

.achievements-toolbar__title {
  font-size: 28px;
  text-transform: uppercase;
}

.achievements-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.achievements-filter {
  padding: 11px 15px;
  border: 1px solid var(--border);
  background: #121316;
  color: var(--gray-light);
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 2px;
  transition: border-color 0.15s ease, color 0.15s ease, background 0.15s ease, transform 0.15s ease;
}

.achievements-filter.active,
.achievements-filter:hover {
  border-color: var(--yellow);
  color: var(--yellow);
  background: #171410;
  transform: translateY(-1px);
}

.achievements-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.achievements-empty {
  min-height: 240px;
  display: grid;
  place-items: center;
  text-align: center;
  color: var(--gray-light);
  border: 1px dashed rgba(245, 197, 24, 0.15);
  background: rgba(255, 255, 255, 0.02);
}

@media (max-width: 1180px) {
  .achievements-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .achievements-container {
    padding: 24px 16px 60px;
  }

  .achievements-hero {
    padding: 22px 18px 20px;
  }

  .achievements-hero__head,
  .achievements-hero__progress-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .achievements-hero__score {
    align-items: flex-start;
    min-width: 0;
  }

  .achievements-hero__title {
    font-size: 34px;
  }

  .achievements-grid {
    grid-template-columns: 1fr;
  }

  .achievements-toolbar__title {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }
}
</style>
