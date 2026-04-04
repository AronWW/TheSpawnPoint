<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Achievement } from '../types'
import { timeAgo } from '../utils/helpers'
import AchievementIcon from './AchievementIcon.vue'
import { useAchievementStore } from '../stores/achievements'

const ANSWER_TO_LIFE_CODE = 'ANSWER_TO_LIFE'
const ANSWER_TO_LIFE_VALUE = '42'

const props = withDefaults(defineProps<{
  achievement: Achievement
  compact?: boolean
  allowSecretInteraction?: boolean
}>(), {
  compact: false,
  allowSecretInteraction: false,
})

const achievementStore = useAchievementStore()

const showLifeInput = ref(false)
const lifeAnswer = ref('')
const lifeError = ref('')
const submittingLifeAnswer = ref(false)

const isUnlocked = computed(() => props.achievement.unlocked)
const isSecret = computed(() => props.achievement.type === 'SECRET')
const hasProgress = computed(() => props.achievement.showProgress && !isSecret.value)
const isLifeAnswerAchievement = computed(() => props.achievement.code === ANSWER_TO_LIFE_CODE)
const canOpenLifeAnswer = computed(() => (
    props.allowSecretInteraction
    && !props.compact
    && isLifeAnswerAchievement.value
    && !isUnlocked.value
))

const shouldShowDescription = computed(() => {
  if (props.compact) return false
  if (isLifeAnswerAchievement.value) return false
  return props.achievement.unlocked || !isSecret.value || !props.achievement.hiddenBeforeUnlock
})

const displayTitle = computed(() => {
  if (!isUnlocked.value && isSecret.value && props.achievement.hiddenBeforeUnlock) {
    return 'ТАЄМНЕ ДОСЯГНЕННЯ'
  }
  return props.achievement.title
})

const bottomText = computed(() => {
  if (isUnlocked.value && props.achievement.unlockedAt) {
    return `Відкрито ${timeAgo(props.achievement.unlockedAt)}`
  }

  if (isSecret.value) {
    return props.achievement.secretHint || 'Є підказка, але відповідь доведеться знайти самостійно.'
  }

  return props.achievement.requirementText || props.achievement.description
})

const progressText = computed(() => {
  if (!hasProgress.value) return ''

  const current = props.achievement.currentProgress ?? 0
  const target = props.achievement.targetProgress ?? 0
  return `${current}/${target}`
})

const progressPercent = computed(() => props.achievement.progressPercent ?? 0)

const stateLabel = computed(() => {
  if (isUnlocked.value) return 'ВІДКРИТО'
  return hasProgress.value ? 'В ПРОЦЕСІ' : 'ЗАКРИТО'
})

watch(() => props.achievement.unlocked, (unlocked) => {
  if (unlocked) {
    showLifeInput.value = false
    lifeAnswer.value = ''
    lifeError.value = ''
    submittingLifeAnswer.value = false
  }
})

function handleCardClick() {
  if (!canOpenLifeAnswer.value) return
  showLifeInput.value = !showLifeInput.value
  lifeError.value = ''
}

function handleCardKeydown(event: KeyboardEvent) {
  if (!canOpenLifeAnswer.value) return
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    handleCardClick()
  }
}

async function submitLifeAnswer() {
  if (!canOpenLifeAnswer.value || submittingLifeAnswer.value) return

  lifeError.value = ''
  if (lifeAnswer.value.trim() !== ANSWER_TO_LIFE_VALUE) {
    lifeError.value = 'Ні. Тут потрібне саме правильне число.'
    return
  }

  submittingLifeAnswer.value = true
  try {
    await achievementStore.claimSecret(ANSWER_TO_LIFE_CODE)
    showLifeInput.value = false
    lifeAnswer.value = ''
  } catch {
    lifeError.value = 'Не вдалося відкрити досягнення. Спробуй ще раз.'
  } finally {
    submittingLifeAnswer.value = false
  }
}
</script>

<template>
  <article
      class="achievement-card"
      :class="[
      compact ? 'compact' : 'full',
      achievement.unlocked ? 'is-unlocked' : 'is-locked',
      isSecret ? 'is-secret' : 'is-standard',
      canOpenLifeAnswer ? 'is-interactive' : '',
      canOpenLifeAnswer && showLifeInput ? 'is-expanded' : '',
    ]"
      :role="canOpenLifeAnswer ? 'button' : undefined"
      :tabindex="canOpenLifeAnswer ? 0 : undefined"
      @click="handleCardClick"
      @keydown="handleCardKeydown"
  >
    <div class="achievement-card__badge">
      <AchievementIcon :code="achievement.code" :size="compact ? 30 : 34" />
    </div>

    <div class="achievement-card__content">
      <div class="achievement-card__topline">
        <span class="achievement-card__type">{{ isSecret ? 'СЕКРЕТ' : 'ДОСЯГНЕННЯ' }}</span>
        <span class="achievement-card__state" :class="{ muted: !achievement.unlocked }">{{ stateLabel }}</span>
      </div>

      <h3 class="achievement-card__title">{{ displayTitle }}</h3>

      <p
          v-if="shouldShowDescription"
          class="achievement-card__desc"
          :class="{ 'achievement-card__desc--ring': achievement.code === 'ONE_RING' }"
      >
        {{ achievement.description }}
      </p>

      <div v-if="hasProgress && !compact" class="achievement-card__progress">
        <div class="achievement-card__progress-row">
          <span>ПРОГРЕС</span>
          <strong>{{ progressText }}</strong>
        </div>
        <div class="achievement-card__progress-bar">
          <div class="achievement-card__progress-fill" :style="{ width: `${progressPercent}%` }"></div>
        </div>
      </div>

      <div v-if="canOpenLifeAnswer && showLifeInput && !compact" class="achievement-card__life-panel" @click.stop>
        <label class="achievement-card__life-label" for="life-answer-input">Введи значення:</label>
        <div class="achievement-card__life-row">
          <input
              id="life-answer-input"
              v-model="lifeAnswer"
              class="achievement-card__life-input"
              type="text"
              inputmode="numeric"
              maxlength="4"
              placeholder="??"
              @keyup.enter="submitLifeAnswer"
          />
          <button
              class="achievement-card__life-btn"
              type="button"
              :disabled="submittingLifeAnswer"
              @click.stop="submitLifeAnswer"
          >
            {{ submittingLifeAnswer ? '...' : 'ПІДТВЕРДИТИ' }}
          </button>
        </div>
        <p v-if="lifeError" class="achievement-card__life-error">{{ lifeError }}</p>
      </div>

      <p class="achievement-card__hint" :class="{ secret: isSecret && !achievement.unlocked }">
        {{ bottomText }}
      </p>
    </div>

    <div class="achievement-card__shine"></div>
  </article>
</template>

<style scoped>
@import url('https://fonts.cdnfonts.com/css/tengwar-annatar');

.achievement-card {
  position: relative;
  overflow: hidden;
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding: 18px;
  background: linear-gradient(180deg, rgba(24, 24, 28, 0.98), rgba(12, 12, 14, 0.98));
  border: 1px solid var(--border);
  box-shadow: inset 0 0 0 1px rgba(255,255,255,0.02);
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.achievement-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(245, 197, 24, 0.05), transparent 34%, transparent 72%, rgba(255,255,255,0.02));
  opacity: 0;
  transition: opacity 0.2s ease;
  pointer-events: none;
}

.achievement-card:hover {
  transform: translateY(-2px);
}

.achievement-card:hover::before {
  opacity: 1;
}

.achievement-card.compact {
  min-height: 150px;
}

.achievement-card.full {
  min-height: 196px;
}

.achievement-card.is-standard.is-unlocked {
  border-color: rgba(245, 197, 24, 0.3);
  box-shadow: 0 14px 34px rgba(0,0,0,0.32), 0 0 0 1px rgba(245, 197, 24, 0.06);
}

.achievement-card.is-secret {
  background: linear-gradient(180deg, rgba(15, 16, 22, 0.98), rgba(9, 10, 14, 0.98));
}

.achievement-card.is-secret.is-unlocked {
  border-color: rgba(125, 142, 255, 0.32);
  box-shadow: 0 14px 34px rgba(0,0,0,0.34), 0 0 0 1px rgba(125, 142, 255, 0.06);
}

.achievement-card.is-locked {
  opacity: 0.95;
}

.achievement-card.is-interactive {
  cursor: pointer;
}

.achievement-card.is-interactive:focus-visible {
  outline: none;
  border-color: #9ca9ff;
  box-shadow: 0 0 0 1px rgba(156, 169, 255, 0.22), 0 0 0 5px rgba(156, 169, 255, 0.06);
}

.achievement-card.is-expanded {
  border-color: rgba(156, 169, 255, 0.4);
  box-shadow: 0 18px 36px rgba(0,0,0,0.36), 0 0 0 1px rgba(156, 169, 255, 0.1);
}

.achievement-card__badge {
  width: 58px;
  height: 58px;
  border: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--gray-light);
  background: rgba(255,255,255,0.03);
}

.is-standard.is-unlocked .achievement-card__badge {
  border-color: rgba(245, 197, 24, 0.28);
  color: var(--yellow);
  background: rgba(245, 197, 24, 0.08);
}

.is-secret .achievement-card__badge {
  color: #aeb9ff;
  background: rgba(125, 142, 255, 0.08);
}

.achievement-card__content {
  min-width: 0;
  flex: 1;
}

.achievement-card__topline {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 8px;
}

.achievement-card__type,
.achievement-card__state {
  font-size: 10px;
  letter-spacing: 2px;
  font-family: var(--font-display);
  color: var(--gray);
}

.achievement-card__state {
  color: var(--yellow);
}

.achievement-card__state.muted {
  color: var(--gray-light);
}

.is-secret .achievement-card__state {
  color: #a5b1ff;
}

.achievement-card__title {
  margin: 0 0 8px;
  font-family: var(--font-display);
  letter-spacing: 1.4px;
  font-size: 20px;
  color: var(--white);
  line-height: 1.05;
}

.achievement-card__desc,
.achievement-card__hint,
.achievement-card__life-error {
  margin: 0;
  font-size: 13px;
  line-height: 1.52;
  color: var(--gray-light);
}

.achievement-card__desc--ring {
  font-family: 'Tengwar Annatar', serif;
  font-size: 13px;
  line-height: 1.52;
  letter-spacing: 1px;
  color: var(--gray-light);
  text-shadow: none;
}

.achievement-card__progress {
  margin-top: 12px;
  margin-bottom: 10px;
}

.achievement-card__progress-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  font-family: var(--font-display);
  font-size: 10px;
  letter-spacing: 2px;
  color: var(--gray);
}

.achievement-card__progress-row strong {
  font-size: 12px;
  color: var(--white);
}

.achievement-card__progress-bar {
  height: 8px;
  border: 1px solid rgba(245, 197, 24, 0.16);
  background: rgba(255,255,255,0.05);
  overflow: hidden;
}

.achievement-card__progress-fill {
  height: 100%;
  background: linear-gradient(90deg, rgba(245, 197, 24, 0.38), rgba(245, 197, 24, 0.92));
}

.achievement-card__life-panel {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid rgba(156, 169, 255, 0.24);
  background: linear-gradient(180deg, rgba(156, 169, 255, 0.09), rgba(156, 169, 255, 0.03));
}

.achievement-card__life-label {
  display: block;
  margin-bottom: 8px;
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 1.6px;
  color: #dbe0ff;
}

.achievement-card__life-row {
  display: flex;
  gap: 10px;
}

.achievement-card__life-input {
  flex: 1;
  min-width: 0;
  padding: 11px 12px;
  border: 1px solid rgba(156, 169, 255, 0.28);
  background: rgba(8, 10, 16, 0.94);
  color: var(--white);
  font-family: var(--font-display);
  letter-spacing: 2px;
  font-size: 15px;
}

.achievement-card__life-input:focus {
  outline: none;
  border-color: #aeb9ff;
}

.achievement-card__life-btn {
  border: 1px solid rgba(156, 169, 255, 0.32);
  background: rgba(156, 169, 255, 0.12);
  color: #e4e8ff;
  font-family: var(--font-display);
  letter-spacing: 1.8px;
  font-size: 11px;
  padding: 0 14px;
  transition: background 0.15s ease, border-color 0.15s ease, transform 0.15s ease;
}

.achievement-card__life-btn:hover:not(:disabled) {
  background: rgba(156, 169, 255, 0.22);
  border-color: rgba(156, 169, 255, 0.48);
  transform: translateY(-1px);
}

.achievement-card__life-btn:disabled {
  opacity: 0.7;
  cursor: default;
}

.achievement-card__life-error {
  margin-top: 8px;
  color: #ff8e8e;
}

.achievement-card__hint {
  margin-top: 8px;
}

.achievement-card__hint.secret {
  color: #bfc7ff;
}

.achievement-card__shine {
  position: absolute;
  inset: 0;
  background: linear-gradient(115deg, transparent 0%, transparent 38%, rgba(255,255,255,0.08) 50%, transparent 62%, transparent 100%);
  transform: translateX(-130%);
  pointer-events: none;
}

.achievement-card:hover .achievement-card__shine {
  transition: transform 0.8s ease;
  transform: translateX(130%);
}

@media (max-width: 700px) {
  .achievement-card {
    padding: 15px;
  }

  .achievement-card__badge {
    width: 52px;
    height: 52px;
  }

  .achievement-card__title {
    font-size: 17px;
  }

  .achievement-card__life-row {
    flex-direction: column;
  }

  .achievement-card__life-btn {
    height: 42px;
  }
}
</style>