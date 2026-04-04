<script setup lang="ts">
import { computed } from 'vue'
import { useAchievementStore } from '../stores/achievements'
import AchievementIcon from './AchievementIcon.vue'

const achievementStore = useAchievementStore()
const isSecret = computed(() => achievementStore.activePopup?.type === 'SECRET')
const isOneRing = computed(() => achievementStore.activePopup?.code === 'ONE_RING')
</script>

<template>
  <Teleport to="body">
    <Transition name="achievement-unlock">
      <div
          v-if="achievementStore.activePopup"
          class="achievement-unlock"
          @click="achievementStore.dismissActivePopup"
      >
        <div class="achievement-unlock__icon" :class="{ secret: isSecret }">
          <AchievementIcon :code="achievementStore.activePopup.code" :size="32" />
        </div>

        <div class="achievement-unlock__content">
          <div class="achievement-unlock__eyebrow">ДОСЯГНЕННЯ ВІДКРИТО</div>
          <div class="achievement-unlock__title">{{ achievementStore.activePopup.title }}</div>
          <div
              class="achievement-unlock__desc"
              :class="{ 'achievement-unlock__desc--ring': isOneRing }"
          >
            {{ achievementStore.activePopup.description }}
          </div>
        </div>

        <div class="achievement-unlock__sweep"></div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
@import url('https://fonts.cdnfonts.com/css/tengwar-annatar');

.achievement-unlock {
  position: fixed;
  top: 88px;
  right: 24px;
  width: min(420px, calc(100vw - 24px));
  background: linear-gradient(180deg, rgba(18, 18, 18, 0.98), rgba(10, 10, 10, 0.98));
  border: 1px solid rgba(245, 197, 24, 0.38);
  border-left: 4px solid var(--yellow);
  box-shadow: 0 18px 50px rgba(0,0,0,0.55), 0 0 0 1px rgba(245, 197, 24, 0.06);
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 16px 18px;
  z-index: 10000;
  overflow: hidden;
  cursor: pointer;
}

.achievement-unlock__icon {
  width: 56px;
  height: 56px;
  border: 1px solid rgba(245, 197, 24, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--yellow);
  background: linear-gradient(180deg, rgba(245, 197, 24, 0.18), rgba(245, 197, 24, 0.05));
}

.achievement-unlock__icon.secret {
  border-color: rgba(125, 142, 255, 0.35);
  color: #b5c0ff;
  background: linear-gradient(180deg, rgba(125, 142, 255, 0.18), rgba(125, 142, 255, 0.05));
}

.achievement-unlock__content {
  position: relative;
  z-index: 2;
  min-width: 0;
}

.achievement-unlock__eyebrow {
  font-family: var(--font-display);
  font-size: 10px;
  letter-spacing: 3px;
  color: var(--yellow);
  margin-bottom: 5px;
}

.achievement-unlock__title {
  font-family: var(--font-display);
  font-size: 20px;
  letter-spacing: 1.5px;
  color: var(--white);
  margin-bottom: 5px;
  line-height: 1.1;
}

.achievement-unlock__desc {
  font-size: 13px;
  line-height: 1.45;
  color: var(--gray-light);
}

.achievement-unlock__desc--ring {
  font-family: 'Tengwar Annatar', serif;
  font-size: 13px;
  line-height: 1.45;
  letter-spacing: 1px;
  color: var(--gray-light);
  text-shadow: none;
}

.achievement-unlock__sweep {
  position: absolute;
  inset: 0;
  background: linear-gradient(110deg, transparent 0%, transparent 35%, rgba(255,255,255,0.16) 47%, transparent 58%, transparent 100%);
  animation: unlock-sweep 1.1s ease 0.28s 1 both;
}

.achievement-unlock-enter-active {
  transition: opacity 0.28s ease, transform 0.34s cubic-bezier(0.2, 0.85, 0.25, 1.08);
}

.achievement-unlock-leave-active {
  transition: opacity 0.24s ease, transform 0.24s ease;
}

.achievement-unlock-enter-from,
.achievement-unlock-leave-to {
  opacity: 0;
  transform: translateX(110%) scale(0.97);
}

@keyframes unlock-sweep {
  from { transform: translateX(-130%); }
  to { transform: translateX(130%); }
}

@media (max-width: 560px) {
  .achievement-unlock {
    right: 12px;
    left: 12px;
    width: auto;
    top: 78px;
  }

  .achievement-unlock__title {
    font-size: 17px;
  }
}
</style>