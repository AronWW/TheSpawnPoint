<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useAchievementStore } from '../stores/achievements'

const auth = useAuthStore()
const achievementStore = useAchievementStore()
const clickTimestamps = ref<number[]>([])
const claimingSecret = ref(false)

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
      // already unlocked or unavailable
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
      <div class="footer-copy">&copy; 2025 TheSpawnPoint &middot; Знайди свій загін</div>
    </div>
  </footer>
</template>

<style scoped>
.footer-outer {
  border-top: 2px solid var(--border);
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
</style>
