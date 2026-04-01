<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { usePartyStore } from '../stores/parties'
import { useAuthStore } from '../stores/auth'
import { useGameStore } from '../stores/games'
import { PUBLIC_BASE_URL } from '../config'
import CreatePartyModal from '../components/CreatePartyModal.vue'
import { skillLabel } from '../utils/helpers'
import type { Party } from '../types'

const router = useRouter()
const route = useRoute()
const partyStore = usePartyStore()
const auth = useAuthStore()
const gameStore = useGameStore()

const loading = ref(true)
const error = ref('')
const modalOpen = ref(false)
const targetUserId = computed(() => {
  const p = route.params.userId
  return p ? Number(p) : auth.user?.id ?? null
})
const isOwnHistory = computed(() => targetUserId.value === auth.user?.id)

function resolveAvatar(url: string | null): string {
  if (!url) return PUBLIC_BASE_URL + '/avatars/default/avatar-1.png'
  if (url.startsWith('http')) return url
  return PUBLIC_BASE_URL + url
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    COMPLETED: 'Завершено',
    CANCELLED: 'Скасовано',
    OPEN: 'Відкрито',
    FULL: 'Заповнено',
    IN_GAME: 'В грі',
  }
  return map[status] ?? status
}

function statusClass(status: string) {
  return status.toLowerCase().replace('_', '-')
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('uk-UA', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

function playStyleLabel(style: string | null): string {
  if (!style) return ''
  const map: Record<string, string> = {
    CASUAL: 'Casual', SEMI_COMPETITIVE: 'Semi-competitive', COMPETITIVE: 'Competitive',
  }
  return map[style] ?? style
}

function regionLabel(region: string | null): string {
  if (!region) return ''
  const map: Record<string, string> = {
    EUROPE: 'Європа', NORTH_AMERICA: 'Пн. Америка', SOUTH_AMERICA: 'Пд. Америка',
    ASIA: 'Азія', MIDDLE_EAST: 'Близький Схід', AFRICA: 'Африка', OCEANIA: 'Океанія',
  }
  return map[region] ?? region
}

function playAgain(party: Party) {
  partyStore.setPlayAgain(party)
  modalOpen.value = true
}

function goToPage(page: number) {
  if (targetUserId.value) {
    partyStore.fetchHistoryForUser(targetUserId.value, page)
  }
}

function goToProfile(userId: number) {
  router.push(`/profile/${userId}`)
}

onMounted(async () => {
  if (!targetUserId.value) {
    router.push('/login')
    return
  }
  await gameStore.fetchGames()
  loading.value = true
  try {
    await partyStore.fetchHistoryForUser(targetUserId.value)
    if (isOwnHistory.value) {
      await partyStore.fetchRecentTeammates()
    }
    error.value = ''
  } catch (e: any) {
    if (e.message === 'PRIVATE') {
      error.value = 'Статистика цього гравця прихована'
    } else {
      error.value = 'Не вдалося завантажити історію'
    }
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="history-page">
    <div class="history-top">
      <div class="section-head">
        <div class="section-title">
          ІСТОРІЯ ІГОР
          <span v-if="!loading && !error" class="section-count">
            {{ partyStore.historyParties.length }} записів
          </span>
        </div>
        <router-link to="/search-parties" class="back-btn">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>
          До пошуку лобі
        </router-link>
      </div>
    </div>

    <div class="history-layout">

      <div class="history-main">

        <div v-if="loading" class="empty-state">
          <div class="empty-icon-wrap">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
            </svg>
          </div>
          <div class="empty-title">ЗАВАНТАЖЕННЯ...</div>
        </div>

        <div v-else-if="error" class="empty-state">
          <div class="empty-icon-wrap">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
          </div>
          <div class="empty-title">{{ error }}</div>
          <button class="back-link" @click="router.back()">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>
            Назад
          </button>
        </div>

        <div v-else-if="partyStore.historyParties.length === 0" class="empty-state">
          <div class="empty-icon-wrap">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="2" y="6" width="20" height="12" rx="2"/><line x1="6" y1="12" x2="10" y2="12"/>
              <line x1="8" y1="10" x2="8" y2="14"/><circle cx="16" cy="10" r="1" fill="currentColor" stroke="none"/>
              <circle cx="18" cy="13" r="1" fill="currentColor" stroke="none"/>
            </svg>
          </div>
          <div class="empty-title">ІСТОРІЯ ПОРОЖНЯ</div>
          <div class="empty-sub">Тут з'являться завершені та скасовані лобі</div>
          <router-link to="/search-parties" class="action-btn">ШУКАТИ ЛОБІ</router-link>
        </div>

        <template v-else>
          <div class="history-list">
            <div v-for="party in partyStore.historyParties" :key="party.id" class="history-card ink-panel">
              <div class="hc-top">
                <div class="hc-cover">
                  <img v-if="party.gameImageUrl" :src="party.gameImageUrl" :alt="party.gameName" />
                  <div v-else class="hc-cover-ph">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <rect x="2" y="6" width="20" height="12" rx="2"/><line x1="6" y1="12" x2="10" y2="12"/>
                      <circle cx="16" cy="12" r="1" fill="currentColor" stroke="none"/>
                    </svg>
                  </div>
                </div>
                <div class="hc-info">
                  <div class="hc-game">{{ party.gameName }}</div>
                  <div class="hc-title" v-if="party.title">{{ party.title }}</div>
                  <div class="hc-meta">
                    <span class="hc-status" :class="statusClass(party.status)">
                      <svg v-if="party.status === 'COMPLETED'" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
                      <svg v-else-if="party.status === 'CANCELLED'" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                      <svg v-else-if="party.status === 'IN_GAME'" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="6" width="20" height="12" rx="2"/></svg>
                      <svg v-else width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/></svg>
                      {{ statusLabel(party.status) }}
                    </span>
                    <span class="hc-date">
                      <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
                      {{ formatDate(party.createdAt) }}
                    </span>
                  </div>
                </div>
                <button
                  v-if="isOwnHistory"
                  class="play-again-btn"
                  @click="playAgain(party)"
                  title="Грати знову"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
                  </svg>
                  ГРАТИ ЗНОВУ
                </button>
              </div>

              <p v-if="party.description" class="hc-desc">{{ party.description }}</p>

              <div class="hc-tags">
                <span v-for="p in party.platform" :key="p" class="chip">{{ p }}</span>
                <span v-for="lang in (party.languages || [])" :key="lang" class="chip">{{ lang }}</span>
                <span v-if="party.region" class="chip">{{ regionLabel(party.region) }}</span>
                <span v-if="party.skillLevel" class="chip">{{ skillLabel(party.skillLevel) }}</span>
                <span v-if="party.playStyle" class="chip">{{ playStyleLabel(party.playStyle) }}</span>
                <span v-for="tag in (party.tags || [])" :key="tag" class="chip chip-soft">{{ tag }}</span>
              </div>

              <div v-if="party.members?.length" class="hc-members">
                <span class="hc-members-label">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
                  Учасники
                </span>
                <div class="hc-members-list">
                  <div
                    v-for="m in party.members"
                    :key="m.userId"
                    class="hc-member"
                    @click.stop="goToProfile(m.userId)"
                  >
                    <img :src="resolveAvatar(m.avatarUrl)" class="hc-member-avatar" />
                    <span class="hc-member-name" :class="{ creator: m.isCreator }">{{ m.displayName }}</span>
                    <svg v-if="m.isCreator" width="10" height="10" viewBox="0 0 24 24" fill="currentColor" stroke="none"><polygon points="12,2 15.09,8.26 22,9.27 17,14.14 18.18,21.02 12,17.77 5.82,21.02 7,14.14 2,9.27 8.91,8.26"/></svg>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="partyStore.historyTotalPages > 1" class="pagination">
            <button
              class="page-btn"
              :disabled="partyStore.historyPage === 0"
              @click="goToPage(partyStore.historyPage - 1)"
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
            </button>
            <button
              v-for="p in partyStore.historyTotalPages"
              :key="p"
              class="page-btn"
              :class="{ active: p - 1 === partyStore.historyPage }"
              @click="goToPage(p - 1)"
            >{{ p }}</button>
            <button
              class="page-btn"
              :disabled="partyStore.historyPage >= partyStore.historyTotalPages - 1"
              @click="goToPage(partyStore.historyPage + 1)"
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
            </button>
          </div>
        </template>
      </div>

      <aside v-if="isOwnHistory && partyStore.recentTeammates.length" class="teammates-sidebar">
        <div class="sidebar-header">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          <span>НЕЩОДАВНІ ТІМЕЙТИ</span>
        </div>
        <div class="teammates-list-v">
          <div
            v-for="t in partyStore.recentTeammates"
            :key="t.userId"
            class="teammate-card-v"
            @click="goToProfile(t.userId)"
          >
            <img :src="resolveAvatar(t.avatarUrl)" :alt="t.displayName" class="teammate-avatar-v" />
            <div class="teammate-info-v">
              <div class="teammate-name-v">{{ t.displayName }}</div>
              <div class="teammate-count-v">
                <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="6" width="20" height="12" rx="2"/><line x1="6" y1="12" x2="10" y2="12"/><circle cx="16" cy="12" r="1" fill="currentColor" stroke="none"/></svg>
                {{ t.gamesPlayedTogether }} {{ t.gamesPlayedTogether === 1 ? 'гра' : 'ігор' }}
              </div>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <CreatePartyModal :visible="modalOpen" @close="modalOpen = false" />
  </div>
</template>

<style scoped>
.history-page {
  min-height: 100vh;
  padding-top: 64px;
  background: var(--black);
}

.history-top {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 64px 0;
}

.history-layout {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px 64px 80px;
  display: flex;
  gap: 28px;
  align-items: flex-start;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}
.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: var(--font-display), sans-serif;
  font-size: 1.8rem;
  letter-spacing: 3px;
  color: var(--white);
}
.section-count {
  font-size: 0.85rem;
  color: var(--gray);
  letter-spacing: 1px;
  font-family: var(--font-body), sans-serif;
}
.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--gray);
  font-size: 0.9rem;
  text-decoration: none;
  transition: color 0.15s;
  font-family: var(--font-body), sans-serif;
}
.back-btn:hover { color: var(--yellow); }

.history-main {
  flex: 1;
  min-width: 0;
}

.teammates-sidebar {
  width: 260px;
  flex-shrink: 0;
  position: sticky;
  top: 104px;
  border: 2px solid var(--border);
  background: var(--panel);
}
.sidebar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 18px;
  border-bottom: 2px solid var(--border);
  font-family: var(--font-display), sans-serif;
  font-size: 12px;
  letter-spacing: 2px;
  color: var(--yellow);
}
.sidebar-header svg {
  color: var(--yellow);
  flex-shrink: 0;
}
.teammates-list-v {
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}
.teammate-card-v {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 18px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid var(--border);
}
.teammate-card-v:last-child {
  border-bottom: none;
}
.teammate-card-v:hover {
  background: rgba(245, 197, 24, 0.04);
}
.teammate-avatar-v {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border);
  transition: border-color 0.15s;
  flex-shrink: 0;
}
.teammate-card-v:hover .teammate-avatar-v {
  border-color: var(--yellow-dim);
}
.teammate-info-v {
  min-width: 0;
}
.teammate-name-v {
  font-size: 13px;
  color: var(--gray-light);
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.teammate-count-v {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--gray);
  margin-top: 2px;
}
.teammate-count-v svg {
  color: var(--gray);
}


.history-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.history-card {
  padding: 22px 24px;
}
.hc-top {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 12px;
}
.hc-cover img {
  width: 52px;
  height: 70px;
  object-fit: cover;
  border: 2px solid var(--border);
}
.hc-cover-ph {
  width: 52px;
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--panel-light);
  border: 2px solid var(--border);
  color: var(--gray);
}
.hc-info { flex: 1; min-width: 0; }
.hc-game {
  font-family: var(--font-display), sans-serif;
  font-size: 18px;
  letter-spacing: 1.5px;
  color: var(--yellow);
}
.hc-title {
  font-size: 13px;
  color: var(--gray-light);
  margin-top: 3px;
}
.hc-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 8px;
}
.hc-status {
  display: flex;
  align-items: center;
  gap: 5px;
  font-family: var(--font-display), sans-serif;
  font-size: 11px;
  letter-spacing: 1px;
}
.hc-status.completed { color: var(--green); }
.hc-status.cancelled { color: var(--red); }
.hc-status.open { color: var(--green); }
.hc-status.full { color: var(--yellow); }
.hc-status.in-game { color: var(--yellow); }
.hc-date {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--gray);
}

.play-again-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-display), sans-serif;
  font-size: 11px;
  letter-spacing: 1.5px;
  padding: 8px 18px;
  border: 2px solid var(--yellow);
  background: transparent;
  color: var(--yellow);
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
  flex-shrink: 0;
}
.play-again-btn:hover {
  background: var(--yellow);
  color: var(--black);
}

.hc-desc {
  font-size: 13px;
  color: var(--gray);
  line-height: 1.6;
  margin-bottom: 12px;
  font-style: italic;
  border-left: 2px solid var(--border);
  padding-left: 12px;
}
.hc-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}
.chip {
  font-size: 10px;
  font-family: var(--font-display), sans-serif;
  letter-spacing: 1px;
  padding: 4px 12px;
  border: 1px solid var(--border);
  color: var(--gray-light);
}
.chip-soft {
  border-color: rgba(245, 197, 24, 0.2);
  color: var(--gray);
}

.hc-members {
  border-top: 1px solid var(--border);
  padding-top: 12px;
}
.hc-members-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-display), sans-serif;
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--gray);
  margin-bottom: 10px;
}
.hc-members-label svg { color: var(--gray); }
.hc-members-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.hc-member {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 5px 10px;
  border: 1px solid var(--border);
  transition: border-color 0.15s, background 0.15s;
}
.hc-member:hover {
  border-color: var(--yellow-dim);
  background: rgba(245, 197, 24, 0.03);
}
.hc-member-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
}
.hc-member-name {
  font-size: 12px;
  color: var(--gray-light);
}
.hc-member-name.creator { color: var(--yellow); }
.hc-member svg { color: var(--yellow); flex-shrink: 0; }

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  margin-top: 28px;
}
.page-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display), sans-serif;
  font-size: 13px;
  padding: 8px 14px;
  border: 2px solid var(--border);
  background: var(--panel);
  color: var(--gray-light);
  cursor: pointer;
  transition: all 0.15s;
  min-width: 38px;
}
.page-btn:hover:not(:disabled) { border-color: var(--yellow-dim); }
.page-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.page-btn.active {
  border-color: var(--yellow);
  color: var(--yellow);
  background: var(--yellow-glow);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}
.empty-icon-wrap {
  color: var(--gray);
  margin-bottom: 20px;
  opacity: 0.5;
}
.empty-title {
  font-family: var(--font-display), sans-serif;
  font-size: 1.5rem;
  letter-spacing: 2px;
  color: var(--yellow);
  margin-bottom: 8px;
}
.empty-sub { color: var(--gray); font-size: 0.95rem; margin-bottom: 20px; }
.action-btn {
  font-family: var(--font-display), sans-serif;
  letter-spacing: 2px;
  font-size: 14px;
  padding: 10px 28px;
  border: 2px solid var(--yellow);
  background: var(--yellow);
  color: var(--black);
  text-decoration: none;
  transition: background 0.15s;
}
.action-btn:hover { background: var(--yellow-dim); }
.back-link {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--gray);
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  margin-top: 12px;
  transition: color 0.15s;
}
.back-link:hover { color: var(--yellow); }

@media (max-width: 900px) {
  .history-top {
    padding: 30px 20px 0;
  }
  .history-layout {
    flex-direction: column;
    padding: 20px 20px 60px;
  }
  .teammates-sidebar {
    width: 100%;
    position: static;
    order: -1;
  }
  .teammates-list-v {
    max-height: 240px;
  }
}
@media (max-width: 600px) {
  .hc-top { flex-direction: column; }
  .play-again-btn { width: 100%; justify-content: center; }
  .section-title { font-size: 1.4rem; }
}
</style>
