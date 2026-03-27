<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { generateSpeedLines } from '../utils/helpers'
import { usePartyStore } from '../stores/parties'
import { useAuthStore } from '../stores/auth'
import { useGameStore } from '../stores/games'

defineEmits<{
  (e: 'open-create-modal'): void
}>()

const router = useRouter()
const partyStore = usePartyStore()
const auth = useAuthStore()
const gameStore = useGameStore()
const speedLines = generateSpeedLines(12)

const featuredParty = computed(() => partyStore.parties[0] ?? null)
const myParty = computed(() => partyStore.myParties[0] ?? null)
const heroParty = computed(() => {
  if (auth.isLoggedIn && myParty.value) return myParty.value
  return featuredParty.value
})
const isMyParty = computed(() => auth.isLoggedIn && myParty.value !== null)

const joinError = ref('')
const joining = ref(false)

async function handleJoin() {
  if (!heroParty.value) return
  joinError.value = ''
  joining.value = true
  try {
    await partyStore.joinParty(heroParty.value.id)
  } catch (e: any) {
    joinError.value = e.message || 'Не вдалося приєднатись'
    setTimeout(() => { joinError.value = '' }, 4000)
  } finally {
    joining.value = false
  }
}

function goToParty() {
  if (heroParty.value) {
    router.push(`/party/${heroParty.value.id}`)
  }
}
</script>

<template>
  <section class="hero halftone">
    <div class="hero-bg"></div>

    <div class="speed-lines">
      <div
        v-for="line in speedLines"
        :key="line.id"
        class="speed-line"
        :style="{
          top: line.top + '%',
          width: line.width + 'px',
          animationDuration: line.dur + 's',
          animationDelay: line.delay + 's',
        }"
      ></div>
    </div>

    <div class="hero-inner">
      <div>
        <div class="hero-kicker">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="6" width="20" height="12" rx="2"/><line x1="6" y1="12" x2="10" y2="12"/><line x1="8" y1="10" x2="8" y2="14"/><circle cx="16" cy="10" r="1" fill="currentColor" stroke="none"/><circle cx="18" cy="13" r="1" fill="currentColor" stroke="none"/></svg>
          Gaming Community Platform
        </div>
        <h1 class="hero-title">
          <span class="shadow-word">ЗНАЙДИ</span>
          <span class="accent">СВІЙ СКВАД</span>
        </h1>
        <p class="hero-sub">
          Платформа для гравців, які хочуть більше ніж просто грати. Знаходь тімейтів,
          організовуй лобі, грай на своєму рівні.
        </p>
        <div class="hero-cta-row">
          <button class="btn-primary" @click="$emit('open-create-modal')">СТВОРИТИ ЛОБІ</button>
          <router-link to="/search-parties" class="btn-secondary">ПЕРЕГЛЯНУТИ ЛОБІ</router-link>
        </div>
        <div class="hero-stats">
          <div class="stat-item">
            <div class="stat-num">{{ partyStore.parties.length || '—' }}</div>
            <div class="stat-label">Відкриті лобі</div>
          </div>
          <div class="stat-item">
            <div class="stat-num">{{ gameStore.games.length || '25+' }}</div>
            <div class="stat-label">Популярних ігор</div>
          </div>
        </div>
      </div>

      <div v-if="heroParty" class="hero-card" @click="isMyParty ? goToParty() : undefined" :style="isMyParty ? 'cursor:pointer' : ''">
        <div class="party-card-accent"></div>
        <div v-if="isMyParty" class="hero-card-label my-party-label">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.5 17.5L3 6V3h3l11.5 11.5"/><path d="M13 19l6-6"/><path d="M2 2l20 20"/></svg>
          МОЄ ЛОБІ
        </div>
        <div class="hero-card-header">
          <img
            v-if="heroParty.gameImageUrl"
            :src="heroParty.gameImageUrl"
            :alt="heroParty.gameName"
            class="game-thumb"
          />
          <div v-else class="game-thumb-placeholder">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="6" width="20" height="12" rx="2"/><line x1="6" y1="12" x2="10" y2="12"/><line x1="8" y1="10" x2="8" y2="14"/><circle cx="16" cy="10" r="1" fill="currentColor" stroke="none"/><circle cx="18" cy="13" r="1" fill="currentColor" stroke="none"/></svg>
          </div>
          <div>
            <div class="card-game-name">{{ heroParty.gameName }}</div>
            <div class="card-meta">
              {{ heroParty.skillLevel ?? 'OPEN' }} ·
              {{ heroParty.playStyle ?? 'CASUAL' }}
            </div>
          </div>
        </div>
        <div class="hero-card-body">
          <p class="card-desc">
            {{ heroParty.description || 'Шукаємо гравців!' }}
          </p>

          <div class="members-row">
            <div
              v-for="member in (heroParty.members || [])"
              :key="member.userId"
              class="member-avatar online"
              :title="member.displayName"
            >
              {{ member.displayName.substring(0, 2).toUpperCase() }}
            </div>
            <div
              v-for="i in (heroParty.maxMembers - heroParty.currentMembers)"
              :key="'empty-' + i"
              class="members-empty"
            >
              +
            </div>
          </div>

          <div class="card-tags">
            <span
              v-for="p in heroParty.platform"
              :key="p"
              class="tag yellow"
            >
              {{ p }}
            </span>
            <span v-if="heroParty.status === 'OPEN'" class="tag green">
              <span class="tag-dot tag-dot--green"></span> Відкрите
            </span>
            <span v-else-if="heroParty.status === 'IN_GAME'" class="tag blue">
              <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="6" width="20" height="12" rx="2"/><line x1="6" y1="12" x2="10" y2="12"/><line x1="8" y1="10" x2="8" y2="14"/><circle cx="16" cy="10" r="1" fill="currentColor" stroke="none"/><circle cx="18" cy="13" r="1" fill="currentColor" stroke="none"/></svg>
              В грі
            </span>
            <span v-for="lang in (heroParty.languages || [])" :key="lang" class="tag">
              {{ lang }}
            </span>
          </div>

          <template v-if="isMyParty">
            <button class="join-btn" @click.stop="goToParty">
              ПЕРЕЙТИ ДО ЛОБІ →
            </button>
          </template>
          <template v-else>
            <div v-if="joinError" class="hero-join-error">{{ joinError }}</div>
            <button class="join-btn" @click="handleJoin" :disabled="joining">
              {{ joining ? 'ПРИЄДНАННЯ...' : 'ПРИЄДНАТИСЬ ДО ЛОБІ →' }}
            </button>
          </template>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero-card-label {
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 3px;
  padding: 8px 24px;
  text-align: center;
  border-bottom: 2px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.my-party-label {
  background: rgba(245, 197, 24, 0.08);
  color: var(--yellow);
}

.tag-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.tag-dot--green { background: #27ae60; }

.tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.game-thumb-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--gray);
}
</style>

