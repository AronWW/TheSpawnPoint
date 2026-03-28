<script setup lang="ts">
import { computed } from 'vue'
import type { Achievement } from '../types'
import { timeAgo } from '../utils/helpers'

const props = withDefaults(defineProps<{
  achievement: Achievement
  compact?: boolean
}>(), {
  compact: false,
})

const isUnlocked = computed(() => props.achievement.unlocked)
const isSecret = computed(() => props.achievement.type === 'SECRET')

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
</script>

<template>
  <article
    class="achievement-card"
    :class="[
      compact ? 'compact' : 'full',
      achievement.unlocked ? 'is-unlocked' : 'is-locked',
      isSecret ? 'is-secret' : 'is-standard',
    ]"
  >
    <div class="achievement-card__badge">{{ achievement.icon }}</div>

    <div class="achievement-card__content">
      <div class="achievement-card__topline">
        <span class="achievement-card__type">{{ isSecret ? 'СЕКРЕТ' : 'ДОСЯГНЕННЯ' }}</span>
        <span v-if="achievement.unlocked" class="achievement-card__state">ВІДКРИТО</span>
        <span v-else class="achievement-card__state muted">LOCKED</span>
      </div>

      <h3 class="achievement-card__title">{{ displayTitle }}</h3>
      <p v-if="!compact && (achievement.unlocked || !isSecret || !achievement.hiddenBeforeUnlock)" class="achievement-card__desc">
        {{ achievement.description }}
      </p>
      <p class="achievement-card__hint" :class="{ secret: isSecret && !achievement.unlocked }">
        {{ bottomText }}
      </p>
    </div>

    <div class="achievement-card__shine"></div>
  </article>
</template>

<style scoped>
.achievement-card {
  position: relative;
  overflow: hidden;
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding: 16px;
  background: linear-gradient(180deg, rgba(20, 20, 20, 0.98), rgba(12, 12, 12, 0.98));
  border: 1px solid var(--border);
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}
.achievement-card:hover {
  transform: translateY(-2px);
}
.achievement-card.compact {
  min-height: 138px;
}
.achievement-card.full {
  min-height: 156px;
}
.achievement-card.is-standard.is-unlocked {
  border-color: rgba(245, 197, 24, 0.35);
  box-shadow: 0 12px 30px rgba(0,0,0,0.35), 0 0 0 1px rgba(245, 197, 24, 0.08);
}
.achievement-card.is-secret {
  background: linear-gradient(180deg, rgba(15, 16, 22, 0.98), rgba(9, 10, 14, 0.98));
}
.achievement-card.is-secret.is-unlocked {
  border-color: rgba(125, 142, 255, 0.35);
  box-shadow: 0 12px 30px rgba(0,0,0,0.35), 0 0 0 1px rgba(125, 142, 255, 0.08);
}
.achievement-card.is-locked {
  opacity: 0.92;
}
.achievement-card__badge {
  width: 52px;
  height: 52px;
  border: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
  background: rgba(255,255,255,0.03);
  box-shadow: inset 0 0 0 1px rgba(255,255,255,0.03);
}
.is-standard.is-unlocked .achievement-card__badge {
  border-color: rgba(245, 197, 24, 0.35);
  background: linear-gradient(180deg, rgba(245, 197, 24, 0.16), rgba(245, 197, 24, 0.05));
}
.is-secret .achievement-card__badge {
  background: linear-gradient(180deg, rgba(125, 142, 255, 0.12), rgba(125, 142, 255, 0.04));
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
  color: var(--gray);
}
.is-secret .achievement-card__state {
  color: #a5b1ff;
}
.achievement-card__title {
  margin: 0 0 8px;
  font-family: var(--font-display);
  letter-spacing: 1.4px;
  font-size: 18px;
  color: var(--white);
  line-height: 1.15;
}
.achievement-card__desc,
.achievement-card__hint {
  margin: 0;
  font-size: 13px;
  line-height: 1.55;
  color: var(--gray-light);
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
    padding: 14px;
  }
  .achievement-card__title {
    font-size: 16px;
  }
}
</style>
