<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import InviteToPartyModal from '../components/InviteToPartyModal.vue'
import PartyMiniChat from '../components/PartyMiniChat.vue'
import PartyVoicePanel from '../components/PartyVoicePanel.vue'
import { useStompClient } from '../composables/useStompClient'
import { PUBLIC_BASE_URL } from '../config'
import { useAuthStore } from '../stores/auth'
import { useChatStore } from '../stores/chat'
import { usePartyStore } from '../stores/parties'
import { useVoiceStore } from '../stores/voice'
import type { Party } from '../types'
import { gameEmoji, skillLabel, timeAgo } from '../utils/helpers'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const partyStore = usePartyStore()
const chatStore = useChatStore()
const voiceStore = useVoiceStore()
const stomp = useStompClient()

const party = ref<Party | null>(null)
const loading = ref(false)
const error = ref('')
const actionLoading = ref(false)
const actionError = ref('')
const showInviteModal = ref(false)

let unsubParty: (() => void) | null = null

function subscribeToPartyUpdates(partyId: number) {
  unsubscribeFromParty()
  unsubParty = stomp.subscribe(`/topic/party/${partyId}`, (frame) => {
    try {
      const updated: Party = JSON.parse(frame.body)
      party.value = updated
      void voiceStore.syncPartyState(updated)
    } catch {
    }
  })
}

function unsubscribeFromParty() {
  if (unsubParty) {
    unsubParty()
    unsubParty = null
  }
}

function resolveAvatar(url: string | null): string {
  if (!url) return PUBLIC_BASE_URL + '/avatars/default/avatar-1.png'
  if (url.startsWith('http')) return url
  return PUBLIC_BASE_URL + url
}

const isCreator = computed(() =>
    auth.user !== null && party.value !== null && auth.user.id === party.value.creatorId,
)

const isMember = computed(() => {
  if (!auth.user || !party.value?.members) return false
  return party.value.members.some((member) => member.userId === auth.user?.id)
})

const isActive = computed(() => {
  if (!party.value) return false
  return ['OPEN', 'FULL', 'IN_GAME'].includes(party.value.status)
})

const canJoin = computed(() =>
    auth.isLoggedIn
    && party.value?.status === 'OPEN'
    && !isMember.value
    && party.value.currentMembers < party.value.maxMembers,
)

const isFull = computed(() =>
    party.value ? party.value.currentMembers >= party.value.maxMembers : false,
)

const canInvite = computed(() =>
    isMember.value && party.value?.status === 'OPEN' && !isFull.value,
)

const canStart = computed(() =>
    isCreator.value && (party.value?.status === 'OPEN' || party.value?.status === 'FULL'),
)

const canComplete = computed(() =>
    isCreator.value && party.value?.status === 'IN_GAME',
)

const partyMemberIds = computed(() =>
    party.value?.members?.map((member) => member.userId) ?? [],
)

const statusLabel = computed(() => {
  if (!party.value) return ''

  const map: Record<string, string> = {
    OPEN: 'Відкрито',
    FULL: 'Заповнено',
    IN_GAME: 'В грі',
    COMPLETED: 'Завершено',
    CANCELLED: 'Скасовано',
  }

  return map[party.value.status] ?? party.value.status
})

const statusClass = computed(() => {
  if (!party.value) return ''

  const map: Record<string, string> = {
    OPEN: 'open',
    FULL: 'full',
    IN_GAME: 'in-game',
    COMPLETED: 'completed',
    CANCELLED: 'closed',
  }

  return map[party.value.status] ?? ''
})

const slotsLabel = computed(() => {
  if (!party.value) return ''
  return `${party.value.currentMembers}/${party.value.maxMembers} місць`
})

const slotsPercent = computed(() => {
  if (!party.value?.maxMembers) return 0
  return Math.min(100, Math.round((party.value.currentMembers / party.value.maxMembers) * 100))
})

async function loadParty() {
  const id = route.params.id as string
  loading.value = true
  error.value = ''

  try {
    party.value = await partyStore.fetchParty(Number(id))
    subscribeToPartyUpdates(Number(id))
    await voiceStore.syncPartyState(party.value)
  } catch {
    error.value = 'Не вдалося завантажити лобі'
    party.value = null
  } finally {
    loading.value = false
  }
}

async function handleJoin() {
  if (!party.value) return

  actionLoading.value = true
  actionError.value = ''

  try {
    party.value = await partyStore.joinParty(party.value.id)
  } catch (e: unknown) {
    actionError.value = e instanceof Error ? e.message : 'Помилка'
  } finally {
    actionLoading.value = false
  }
}

async function handleLeave() {
  if (!party.value) return

  actionLoading.value = true
  actionError.value = ''

  try {
    if (voiceStore.isInPartyVoice(party.value.id)) {
      await voiceStore.leaveVoice({ silent: true })
    }

    await partyStore.leaveParty(party.value.id)
    await chatStore.fetchChats()
    await router.push('/search-parties')
  } catch (e: unknown) {
    actionError.value = e instanceof Error ? e.message : 'Помилка'
  } finally {
    actionLoading.value = false
  }
}

async function handleClose() {
  if (!party.value) return

  actionLoading.value = true
  actionError.value = ''

  try {
    await partyStore.closeParty(party.value.id)
    await loadParty()
  } catch (e: unknown) {
    actionError.value = e instanceof Error ? e.message : 'Помилка'
  } finally {
    actionLoading.value = false
  }
}

async function handleStartGame() {
  if (!party.value) return

  actionLoading.value = true
  actionError.value = ''

  try {
    party.value = await partyStore.startGame(party.value.id)
    await voiceStore.syncPartyState(party.value)
  } catch (e: unknown) {
    actionError.value = e instanceof Error ? e.message : 'Помилка'
  } finally {
    actionLoading.value = false
  }
}

async function handleComplete() {
  if (!party.value) return

  actionLoading.value = true
  actionError.value = ''

  try {
    party.value = await partyStore.completeParty(party.value.id)
    await voiceStore.syncPartyState(party.value)
  } catch (e: unknown) {
    actionError.value = e instanceof Error ? e.message : 'Помилка'
  } finally {
    actionLoading.value = false
  }
}

async function handleKick(userId: number) {
  if (!party.value) return

  actionLoading.value = true
  actionError.value = ''

  try {
    party.value = await partyStore.kickMember(party.value.id, userId)
    await voiceStore.syncPartyState(party.value)
  } catch (e: unknown) {
    actionError.value = e instanceof Error ? e.message : 'Помилка'
  } finally {
    actionLoading.value = false
  }
}

function openInviteModal() {
  if (!canInvite.value) return
  showInviteModal.value = true
}

function playStyleLabel(style: string | null): string {
  if (!style) return ''

  const map: Record<string, string> = {
    CASUAL: 'Casual',
    SEMI_COMPETITIVE: 'Semi-competitive',
    COMPETITIVE: 'Competitive',
  }

  return map[style] ?? style
}

function regionLabel(region: string | null): string {
  if (!region) return ''

  const map: Record<string, string> = {
    EUROPE: 'Європа',
    NORTH_AMERICA: 'Пн. Америка',
    SOUTH_AMERICA: 'Пд. Америка',
    ASIA: 'Азія',
    MIDDLE_EAST: 'Близький Схід',
    AFRICA: 'Африка',
    OCEANIA: 'Океанія',
  }

  return map[region] ?? region
}

function formatEventTime(iso: string | null): string {
  if (!iso) return ''

  return new Date(iso).toLocaleString('uk-UA', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

onMounted(() => {
  void loadParty()
})

onUnmounted(() => {
  unsubscribeFromParty()
})

watch(() => route.params.id, () => {
  unsubscribeFromParty()
  void loadParty()
})

watch(
    () => party.value,
    (nextParty) => {
      if (!nextParty) return
      void voiceStore.syncPartyState(nextParty)
    },
    { deep: true },
)
</script>

<template>
  <div class="party-detail-page">
    <div class="party-detail-container">
      <div v-if="loading" class="empty-state ink-panel">
        <div class="empty-icon">⏳</div>
        <p>Завантаження лобі...</p>
      </div>

      <div v-else-if="error" class="empty-state ink-panel">
        <div class="empty-icon">😕</div>
        <h3>{{ error }}</h3>
        <router-link to="/" class="action-btn action-btn--primary">На головну</router-link>
      </div>

      <div v-else-if="party" class="party-detail">
        <button type="button" class="back-link" @click="router.back()">← Назад</button>

        <section class="party-header ink-panel">
          <div class="party-cover-wrap">
            <div class="party-cover">
              <img v-if="party.gameImageUrl" :src="party.gameImageUrl" :alt="party.gameName" />
              <div v-else class="cover-placeholder">{{ gameEmoji(party.gameName) }}</div>
            </div>
          </div>

          <div class="party-header-main">
            <div class="party-header-topline">
              <h1 class="party-title">{{ party.title || party.gameName }}</h1>
              <span class="status-badge" :class="statusClass">{{ statusLabel }}</span>
            </div>

            <div class="party-subtitle-row">
              <span class="host-prefix">Хост</span>
              <router-link :to="'/profile/' + party.creatorId" class="host-link">
                <img :src="resolveAvatar(party.creatorAvatarUrl)" :alt="party.creatorDisplayName" class="host-avatar" />
                {{ party.creatorDisplayName }}
              </router-link>
              <span class="dot">•</span>
              <span class="party-created">{{ timeAgo(party.createdAt) }}</span>
            </div>

            <p v-if="party.description" class="party-description">{{ party.description }}</p>

            <div class="party-tags">
              <span v-for="platform in party.platform" :key="platform" class="chip chip--accent">{{ platform }}</span>
              <span v-for="lang in (party.languages || [])" :key="lang" class="chip">{{ lang }}</span>
              <span v-if="party.region" class="chip">{{ regionLabel(party.region) }}</span>
              <span v-if="party.skillLevel" class="chip">{{ skillLabel(party.skillLevel) }}</span>
              <span v-if="party.playStyle" class="chip">{{ playStyleLabel(party.playStyle) }}</span>
              <span v-if="party.eventTime" class="chip chip--soft">Старт {{ formatEventTime(party.eventTime) }}</span>
              <span v-for="tag in (party.tags || [])" :key="tag" class="chip chip--soft">{{ tag }}</span>
            </div>
          </div>

          <div class="party-header-side">
            <div class="summary-card summary-card--slots">
              <div class="summary-label">Склад</div>
              <div class="summary-value">{{ slotsLabel }}</div>
              <div class="summary-bar">
                <span class="summary-bar-fill" :style="{ width: `${slotsPercent}%` }" />
              </div>
            </div>

            <div class="header-actions">
              <button
                  v-if="canJoin"
                  type="button"
                  class="action-btn action-btn--primary"
                  :disabled="actionLoading"
                  @click="handleJoin"
              >
                {{ actionLoading ? 'Приєднання...' : 'Приєднатися' }}
              </button>

              <router-link
                  v-else-if="!auth.isLoggedIn && party.status === 'OPEN'"
                  to="/login"
                  class="action-btn action-btn--primary"
              >
                Увійти для приєднання
              </router-link>

              <button
                  v-if="canStart"
                  type="button"
                  class="action-btn action-btn--primary"
                  :disabled="actionLoading"
                  @click="handleStartGame"
              >
                {{ actionLoading ? 'Запуск...' : 'Почати гру' }}
              </button>

              <button
                  v-if="canComplete"
                  type="button"
                  class="action-btn"
                  :disabled="actionLoading"
                  @click="handleComplete"
              >
                {{ actionLoading ? '...' : 'Завершити гру' }}
              </button>

              <button
                  v-if="isMember"
                  type="button"
                  class="action-btn action-btn--ghost-danger"
                  :disabled="actionLoading"
                  @click="handleLeave"
              >
                {{ actionLoading ? '...' : 'Вийти з лобі' }}
              </button>
            </div>
          </div>
        </section>

        <div v-if="actionError" class="action-error action-error--global ink-panel">
          {{ actionError }}
        </div>

        <div class="party-layout">
          <section class="party-main-column">
            <div class="members-panel ink-panel">
              <div class="section-head section-head--members">
                <div>
                  <div class="section-kicker">Команда</div>
                  <h2 class="section-title">Учасники лобі</h2>
                </div>

                <button
                    v-if="canInvite"
                    type="button"
                    class="action-btn action-btn--compact"
                    @click="openInviteModal"
                >
                  Запросити гравця
                </button>
              </div>

              <div class="members-list">
                <div
                    v-for="i in party.maxMembers"
                    :key="i"
                    class="member-row"
                    :class="{ filled: i <= party.currentMembers }"
                >
                  <template v-if="i <= party.currentMembers && party.members?.[i - 1]">
                    <router-link :to="'/profile/' + (party.members?.[i - 1]?.userId ?? '')" class="member-card">
                      <img
                          :src="resolveAvatar(party.members?.[i - 1]?.avatarUrl ?? null)"
                          :alt="party.members?.[i - 1]?.displayName ?? 'Member'"
                          class="member-avatar"
                      />

                      <div class="member-copy">
                        <div class="member-name-row">
                          <span v-if="party.members?.[i - 1]?.isCreator" class="member-badge">Хост</span>
                          <span class="member-name">{{ party.members?.[i - 1]?.displayName ?? 'Member' }}</span>
                        </div>
                        <div class="member-meta">У лобі {{ timeAgo(party.members?.[i - 1]?.joinedAt ?? party.createdAt) }}</div>
                      </div>
                    </router-link>

                    <button
                        v-if="isCreator && isActive && !party.members?.[i - 1]?.isCreator"
                        type="button"
                        class="member-kick"
                        :disabled="actionLoading"
                        title="Кікнути гравця"
                        @click="handleKick(party.members?.[i - 1]?.userId ?? 0)"
                    >
                      ✕
                    </button>
                  </template>

                  <template v-else>
                    <button
                        v-if="canInvite"
                        type="button"
                        class="empty-slot empty-slot--button"
                        @click="openInviteModal"
                    >
                      <span class="empty-slot-mark">+</span>
                      <span class="empty-slot-copy">
                        <span class="empty-slot-title">Вільне місце</span>
                        <span class="empty-slot-meta">Натисни, щоб запросити гравця</span>
                      </span>
                    </button>

                    <div v-else class="empty-slot">
                      <span class="empty-slot-mark">+</span>
                      <div>
                        <div class="empty-slot-title">Вільне місце</div>
                        <div class="empty-slot-meta">Чекає на нового гравця</div>
                      </div>
                    </div>
                  </template>
                </div>
              </div>
            </div>
          </section>

          <aside class="party-side-column">
            <section v-if="party.chatId" class="chat-panel ink-panel">
              <PartyMiniChat
                  :chat-id="party.chatId"
                  :is-member="isMember"
                  :is-logged-in="auth.isLoggedIn"
              />
            </section>

            <PartyVoicePanel
                :party="party"
                :is-member="isMember"
                :is-logged-in="auth.isLoggedIn"
                class="voice-panel-card"
            />
          </aside>
        </div>

        <InviteToPartyModal
            :visible="showInviteModal"
            :party-id="party.id"
            :party-members="partyMemberIds"
            @close="showInviteModal = false"
        />

        <button
            v-if="isCreator"
            type="button"
            class="action-btn action-btn--danger action-btn--floating-close"
            :disabled="actionLoading"
            @click="handleClose"
        >
          {{ actionLoading ? '...' : 'Закрити лобі' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.party-detail-page {
  min-height: 100vh;
  padding-top: 64px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.01), transparent 140px), var(--black);
}

.party-detail-container {
  max-width: 1280px;
  margin: 0 auto;
  padding: 22px 24px 32px;
}

.party-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-state {
  min-height: 280px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 24px;
  text-align: center;
}

.empty-icon {
  font-size: 34px;
}

.back-link {
  align-self: flex-start;
  background: none;
  border: none;
  color: var(--gray-light);
  font-size: 14px;
  letter-spacing: 0.8px;
  transition: color 0.15s ease;
}

.back-link:hover {
  color: var(--yellow);
}

.party-header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) 240px;
  gap: 20px;
  padding: 20px;
  align-items: start;
}

.party-cover-wrap {
  display: flex;
}

.party-cover {
  width: 150px;
  height: 200px;
  border: 2px solid var(--border);
  overflow: hidden;
  background: var(--dark);
  flex-shrink: 0;
}

.party-cover img,
.cover-placeholder {
  width: 100%;
  height: 100%;
}

.party-cover img {
  object-fit: cover;
}

.cover-placeholder {
  display: grid;
  place-items: center;
  font-size: 34px;
}

.party-header-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.party-header-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.eyebrow,
.section-kicker,
.summary-label {
  font-family: var(--font-display);
  font-size: 12px;
  letter-spacing: 1.8px;
  text-transform: uppercase;
  color: var(--gray);
}

.party-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(34px, 4vw, 46px);
  line-height: 0.95;
  color: var(--white);
  letter-spacing: 0.4px;
}

.party-subtitle-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--gray-light);
  font-size: 15px;
  padding-top: 10px;
}

.host-prefix {
  padding: 3px 7px;
  border: 1px solid rgba(245, 197, 24, 0.28);
  background: rgba(245, 197, 24, 0.08);
  color: var(--yellow);
  font-size: 11px;
  letter-spacing: 0.8px;
  text-transform: uppercase;
}

.dot {
  color: var(--border-glow);
}

.host-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--white);
}

.host-link:hover {
  color: var(--yellow);
}

.host-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--yellow-dim);
}

.party-description {
  margin: 0;
  color: var(--gray-light);
  font-size: 15px;
  line-height: 1.55;
  max-width: 80ch;
}

.party-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  padding: 6px 10px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.03);
  color: var(--gray-light);
  font-size: 12px;
  letter-spacing: 0.6px;
}

.chip--accent {
  color: var(--yellow);
  border-color: rgba(245, 197, 24, 0.28);
  background: rgba(245, 197, 24, 0.07);
}

.chip--soft {
  color: var(--white);
}

.status-badge {
  padding: 7px 12px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.04);
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 1px;
  white-space: nowrap;
}

.status-badge.open {
  color: var(--yellow);
  border-color: rgba(245, 197, 24, 0.32);
}

.status-badge.full {
  color: #ff9c6b;
  border-color: rgba(255, 156, 107, 0.28);
}

.status-badge.in-game {
  color: #8ff0b0;
  border-color: rgba(143, 240, 176, 0.28);
}

.status-badge.completed {
  color: #8bdcff;
  border-color: rgba(139, 220, 255, 0.28);
}

.status-badge.closed {
  color: #ff9c9c;
  border-color: rgba(255, 156, 156, 0.28);
}

.party-header-side {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.summary-card {
  border: 1px solid var(--border);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.03), rgba(255, 255, 255, 0.015));
  padding: 12px 14px;
  min-height: 72px;
}

.summary-value {
  margin-top: 6px;
  font-family: var(--font-display);
  font-size: 24px;
  line-height: 1;
  color: var(--white);
}

.summary-card--slots .summary-value {
  color: var(--yellow);
}

.summary-bar {
  margin-top: 10px;
  height: 6px;
  background: rgba(255, 255, 255, 0.06);
  overflow: hidden;
}

.summary-bar-fill {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, var(--yellow-dim), var(--yellow));
}

.header-actions {
  display: grid;
  gap: 10px;
  margin-top: auto;
  justify-self: end;
  width: min(260px, 100%);
}

.party-layout {
  display: grid;
  grid-template-columns: minmax(320px, 0.84fr) minmax(420px, 1.16fr);
  gap: 16px;
  align-items: start;
}

.party-main-column,
.party-side-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.members-panel {
  padding: 18px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.section-head--members {
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border);
}

.section-title {
  margin: 3px 0 0;
  font-family: var(--font-display);
  font-size: 28px;
  line-height: 0.95;
  color: var(--white);
}

.members-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.member-row {
  min-height: 74px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.025);
}

.member-row.filled {
  border-color: var(--border);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.035), rgba(255, 255, 255, 0.02));
}

.member-card {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
}

.member-card:hover .member-name {
  color: var(--yellow);
}

.member-avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid rgba(245, 197, 24, 0.35);
  background: var(--dark);
  flex-shrink: 0;
}

.member-copy {
  min-width: 0;
}

.member-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.member-name {
  color: var(--white);
  font-family: var(--font-display);
  font-size: 22px;
  line-height: 1;
  letter-spacing: 0.4px;
  transition: color 0.15s ease;
}

.member-badge {
  padding: 3px 7px;
  border: 1px solid rgba(245, 197, 24, 0.28);
  background: rgba(245, 197, 24, 0.08);
  color: var(--yellow);
  font-size: 11px;
  letter-spacing: 0.8px;
  text-transform: uppercase;
}

.member-meta {
  margin-top: 4px;
  color: var(--gray);
  font-size: 13px;
}

.member-kick {
  width: 34px;
  height: 34px;
  border: 1px solid rgba(255, 138, 138, 0.34);
  background: rgba(255, 107, 107, 0.08);
  color: #ff9e9e;
  font-size: 16px;
  transition: border-color 0.15s ease, color 0.15s ease, background 0.15s ease;
}

.member-kick:hover:not(:disabled) {
  border-color: rgba(255, 138, 138, 0.65);
  color: #ffd1d1;
  background: rgba(255, 107, 107, 0.14);
}

.empty-slot {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--gray-light);
}

.empty-slot--button {
  border: none;
  background: transparent;
  padding: 0;
  text-align: left;
  transition: transform 0.15s ease;
}

.empty-slot--button:hover {
  transform: translateY(-1px);
}

.empty-slot--button:hover .empty-slot-title {
  color: var(--yellow);
}

.empty-slot--button:hover .empty-slot-mark {
  border-color: rgba(245, 197, 24, 0.5);
  background: rgba(245, 197, 24, 0.1);
}

.empty-slot-mark {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border: 1px dashed rgba(245, 197, 24, 0.28);
  color: var(--yellow);
  font-size: 20px;
  flex-shrink: 0;
}

.empty-slot-title {
  display: block;
  color: var(--white);
  font-size: 15px;
  transition: color 0.15s ease;
}

.empty-slot-meta {
  display: block;
  color: var(--gray);
  font-size: 12px;
}

.empty-slot-copy {
  display: block;
}

.chat-panel {
  height: 520px;
  min-height: 520px;
  padding: 0;
  overflow: hidden;
}

.action-error {
  padding: 12px 14px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.03);
  font-size: 14px;
  color: var(--gray-light);
}

.action-error {
  border-color: rgba(255, 107, 107, 0.32);
  background: rgba(255, 107, 107, 0.08);
  color: #ffc0c0;
}

.action-error--global {
  margin: 0;
}

.header-actions {
  display: grid;
  gap: 10px;
}

.action-btn {
  min-height: 44px;
  padding: 10px 14px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.03);
  color: var(--white);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-family: var(--font-display);
  font-size: 16px;
  letter-spacing: 0.9px;
  text-transform: uppercase;
  transition: transform 0.15s ease, border-color 0.15s ease, color 0.15s ease, background 0.15s ease;
}

.action-btn:hover {
  transform: translateY(-1px);
  border-color: var(--yellow-dim);
  color: var(--yellow);
}

.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.action-btn--primary {
  background: var(--yellow);
  border-color: var(--yellow);
  color: var(--black);
}

.action-btn--primary:hover {
  color: var(--black);
  background: var(--yellow-dim);
  border-color: var(--yellow-dim);
}

.action-btn--danger {
  color: #ffabab;
  border-color: rgba(255, 107, 107, 0.32);
  background: rgba(255, 107, 107, 0.08);
}

.action-btn--danger:hover,
.action-btn--ghost-danger:hover {
  color: #ffd7d7;
  border-color: rgba(255, 107, 107, 0.7);
}

.action-btn--ghost-danger {
  color: #ffb7b7;
  border-color: rgba(255, 107, 107, 0.32);
  background: rgba(255, 107, 107, 0.06);
}

.action-btn--muted {
  color: var(--gray-light);
  border-color: rgba(255, 255, 255, 0.2);
  background: rgba(0, 0, 0, 0.35);
}

.action-btn--muted:hover {
  color: var(--white);
  border-color: rgba(255, 255, 255, 0.35);
  background: rgba(0, 0, 0, 0.5);
}

.action-btn--floating-close {
  align-self: flex-end;
  min-width: 190px;
}

.action-btn--compact {
  min-height: 40px;
  padding-inline: 12px;
  font-size: 14px;
}

.voice-panel-card {
  min-width: 0;
}

@media (max-width: 1120px) {
  .party-header {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .party-header-side {
    grid-column: 1 / -1;
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  }
}

@media (max-width: 980px) {
  .party-layout {
    grid-template-columns: 1fr;
  }

  .chat-panel {
    height: 460px;
    min-height: 460px;
  }
}

@media (max-width: 760px) {
  .party-detail-container {
    padding: 18px 14px 28px;
  }

  .party-header {
    grid-template-columns: 1fr;
  }

  .party-cover-wrap {
    justify-content: flex-start;
  }

  .party-header-topline,
  .section-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .party-header-side {
    grid-template-columns: 1fr;
  }

  .member-row {
    align-items: flex-start;
  }

  .member-kick {
    margin-top: 6px;
  }

  .action-btn--floating-close {
    min-width: 170px;
  }
}
</style>
