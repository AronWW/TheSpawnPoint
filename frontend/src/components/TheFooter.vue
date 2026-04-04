<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useAchievementStore } from '../stores/achievements'
import OneRingOverlay from './OneRingOverlay.vue'

const auth = useAuthStore()
const achievementStore = useAchievementStore()
const clickTimestamps = ref<number[]>([])
const claimingSecret = ref(false)
const currentYear = new Date().getFullYear()

const showRingAnimation = ref(false)
const ringOrigin = ref<{ x: number; y: number } | undefined>()

async function handleRingClick(event: MouseEvent) {
  if (!auth.isLoggedIn || showRingAnimation.value) return

  if (!achievementStore.hasLoadedMyAchievements) {
    try {
      await achievementStore.fetchMyAchievements()
    } catch {
      return
    }
  }

  if (achievementStore.hasAchievement('ONE_RING')) return

  const el = event.currentTarget as HTMLElement
  const rect = el.getBoundingClientRect()
  ringOrigin.value = {
    x: rect.left + rect.width / 2,
    y: rect.top + rect.height / 2,
  }
  showRingAnimation.value = true
}

async function onRingUnlocked() {
  try {
    await achievementStore.claimSecret('ONE_RING')
  } catch {

  }
}

function onRingClose() {
  showRingAnimation.value = false
}

async function handleFooterLogoClick() {
  if (!auth.isLoggedIn || claimingSecret.value) return

  const now = Date.now()
  clickTimestamps.value = [...clickTimestamps.value.filter((stamp) => now - stamp < 1600), now]

  if (clickTimestamps.value.length >= 3) {
    clickTimestamps.value = []
    claimingSecret.value = true
    try {
      await achievementStore.claimSecret('FOOTER_TRIPLE_CLICK')
    } catch {
    } finally {
      window.setTimeout(() => {
        claimingSecret.value = false
      }, 600)
    }
  }
}
</script>

<template>
  <footer class="footer-outer">
    <div class="footer">
      <button class="footer-logo footer-logo-btn" @click="handleFooterLogoClick">THESPAWNPOINT</button>
      <div class="footer-right">
        <router-link to="/about" class="footer-link">Про нас</router-link>
        <span class="footer-sep">&middot;</span>
        <router-link to="/community-guidelines" class="footer-link">Правила спільноти</router-link>
        <span class="footer-sep">&middot;</span>
        <span class="footer-copy"><span class="ring-trigger" @click.stop="handleRingClick">&copy;</span> {{ currentYear }} TheSpawnPoint</span>
      </div>
    </div>

    <OneRingOverlay
      v-if="showRingAnimation"
      :origin-rect="ringOrigin"
      @unlocked="onRingUnlocked"
      @close="onRingClose"
    />
  </footer>
</template>

<style scoped>
.footer-outer {
  padding: 0 64px;
}
.footer-logo-btn {
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;
  transition: opacity 0.15s ease, color 0.15s ease, transform 0.15s ease;
}
.footer-logo-btn:hover {
  opacity: 0.9;
  color: var(--yellow);
}
.footer-logo-btn:active {
  transform: translateY(1px);
}
.footer-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.footer-link {
  font-size: 12px;
  color: var(--gray);
  letter-spacing: 1px;
  transition: color 0.15s;
}
.footer-link:hover {
  color: var(--yellow);
}
.footer-sep {
  color: var(--border);
  font-size: 12px;
  user-select: none;
}
.footer-copy {
  font-size: 12px;
  color: var(--gray);
  letter-spacing: 1px;
}
.ring-trigger {
  cursor: default;
  user-select: none;
  padding: 4px 2px;
  margin: -4px -2px;
  position: relative;
}
</style>
