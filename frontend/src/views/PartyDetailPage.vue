<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { usePartyStore } from '../stores/parties'
import { useChatStore } from '../stores/chat'
import { useVoiceStore } from '../stores/voice'
import { useStompClient } from '../composables/useStompClient'
import InviteToPartyModal from '../components/InviteToPartyModal.vue'
import PartyMiniChat from '../components/PartyMiniChat.vue'
import PartyVoicePanel from '../components/PartyVoicePanel.vue'
import type { Party } from '../types'
import { skillLabel, timeAgo, gameEmoji } from '../utils/helpers'
import { PUBLIC_BASE_URL } from '../config'

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
  auth.user && party.value && auth.user.id === party.value.creatorId
)

const isMember = computed(() => {
  if (!auth.user || !party.value?.members) return false
  return party.value.members.some((m) => m.userId === auth.user!.id)
})

const isActive = computed(() => {
  if (!party.value) return false
  return ['OPEN', 'FULL', 'IN_GAME'].includes(party.value.status)
})

const canJoin = computed(() =>
  auth.isLoggedIn &&
  party.value?.status === 'OPEN' &&
  !isMember.value &&
  party.value.currentMembers < party.value.maxMembers
)

const isFull = computed(() =>
  party.value ? party.value.currentMembers >= party.value.maxMembers : false
)

const canInvite = computed(() =>
  isMember.value && party.value?.status === 'OPEN' && !isFull.value
)

const canStart = computed(() =>
  isCreator.value && (party.value?.status === 'OPEN' || party.value?.status === 'FULL')
)

const canComplete = computed(() =>
  isCreator.value && party.value?.status === 'IN_GAME'
)

const partyMemberIds = computed(() =>
  party.value?.members?.map((m) => m.userId) ?? []
)

const statusLabel = computed(() => {
  if (!party.value) return ''
  const map: Record<string, string> = {
    OPEN: 'ВІДКРИТО',
    FULL: 'ЗАПОВНЕНО',
    IN_GAME: 'В ГРІ',
    COMPLETED: 'ЗАВЕРШЕНО',
    CANCELLED: 'СКАСОВАНО',
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

const shouldShowExpandedVoicePanel = computed(() => {
  return !!party.value && voiceStore.isInPartyVoice(party.value.id) && voiceStore.isExpanded
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
  } catch (e: any) {
    actionError.value = e.message || 'Помилка'
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
    chatStore.fetchChats()
    router.push('/search-parties')
  } catch (e: any) {
    actionError.value = e.message || 'Помилка'
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
  } catch (e: any) {
    actionError.value = e.message || 'Помилка'
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
  } catch (e: any) {
    actionError.value = e.message || 'Помилка'
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
  } catch (e: any) {
    actionError.value = e.message || 'Помилка'
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
  } catch (e: any) {
    actionError.value = e.message || 'Помилка'
  } finally {
    actionLoading.value = false
  }
}

function goToChat() {
  if (party.value?.chatId) {
    router.push(`/chat?groupId=${party.value.chatId}`)
  }
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
    EUROPE: '🌍 Європа',
    NORTH_AMERICA: '🌎 Пн. Америка',
    SOUTH_AMERICA: '🌎 Пд. Америка',
    ASIA: '🌏 Азія',
    MIDDLE_EAST: '🌍 Близький Схід',
    AFRICA: '🌍 Африка',
    OCEANIA: '🌏 Океанія',
  }
  return map[region] ?? region
}

onMounted(loadParty)

onUnmounted(() => {
  unsubscribeFromParty()
})

watch(() => route.params.id, () => {
  unsubscribeFromParty()
  loadParty()
})

watch(
  () => party.value,
  (nextParty) => {
    if (!nextParty) return
    void voiceStore.syncPartyState(nextParty)
  },
  { deep: true }
)
</script>

<template>
  <div class="party-detail-page">
    <div class="party-detail-container">
      <div v-if="loading" class="empty-state">
        <div class="empty-icon">⏳</div>
        <p>Завантаження лобі...</p>
      </div>

      <div v-else-if="error" class="empty-state">
        <div class="empty-icon">😕</div>
        <h3>{{ error }}</h3>
        <router-link to="/" class="action-btn">НА ГОЛОВНУ</router-link>
      </div>

      <div v-else-if="party" class="party-detail">
        <button class="back-link" @click="router.back()">← Назад</button>

        <div class="detail-header ink-panel">
          <div class="detail-header-top">
            <div class="detail-game-cover">
              <img v-if="party.gameImageUrl" :src="party.gameImageUrl" :alt="party.gameName" />
              <div v-else class="cover-placeholder">{{ gameEmoji(party.gameName) }}</div>
            </div>
            <div class="detail-header-info">
              <h1 class="detail-game-name">{{ party.gameName }}</h1>
              <div class="detail-host">
                Хост:
                <router-link :to="'/profile/' + party.creatorId" class="host-link">
                  <img :src="resolveAvatar(party.creatorAvatarUrl)" :alt="party.creatorDisplayName" class="host-avatar" />
                  {{ party.creatorDisplayName }}
                </router-link>
              </div>
              <div class="detail-time">{{ timeAgo(party.createdAt) }}</div>
            </div>
            <div class="detail-status">
              <span class="status-badge" :class="statusClass">
                {{ statusLabel }}
              </span>
            </div>
          </div>

          <div v-if="party.title" class="detail-title">
            <h2>{{ party.title }}</h2>
          </div>

          <div v-if="party.description" class="detail-description">
            <p>{{ party.description }}</p>
          </div>

          <div class="detail-tags">
            <span v-for="p in party.platform" :key="p" class="platform-tag">{{ p }}</span>
            <span v-for="lang in (party.languages || [])" :key="lang" class="tag">{{ lang }}</span>
            <span v-if="party.region" class="tag tag-region">{{ regionLabel(party.region) }}</span>
            <span v-if="party.skillLevel" class="skill-badge" :class="party.skillLevel.toLowerCase()">
              {{ skillLabel(party.skillLevel) }}
            </span>
            <span v-if="party.playStyle" class="tag">{{ playStyleLabel(party.playStyle) }}</span>
            <span v-for="tag in (party.tags || [])" :key="tag" class="tag tag-custom">{{ tag }}</span>
          </div>
        </div>

        <PartyVoicePanel
          v-if="party"
          :party="party"
          :is-member="isMember"
          :is-logged-in="auth.isLoggedIn"
          class="detail-section"
        />

        <div class="detail-section ink-panel">
          <h2 class="section-label">
            ГРАВЦІ
            <span class="member-count">
              <span>{{ party.currentMembers }}</span>/{{ party.maxMembers }}
            </span>
          </h2>

          <div class="slots-visual">
            <div v-for="i in party.maxMembers" :key="i" class="member-slot" :class="{ filled: i <= party.currentMembers }">
              <template v-if="i <= party.currentMembers && party.members?.[i - 1]">
                <router-link :to="'/profile/' + (party.members?.[i - 1]?.userId ?? '')" class="member-card">
                  <img :src="resolveAvatar(party.members?.[i - 1]?.avatarUrl ?? null)" :alt="party.members?.[i - 1]?.displayName ?? 'Member'" class="member-avatar" />
                  <div class="member-info">
                    <span class="member-name">{{ party.members?.[i - 1]?.displayName ?? 'Member' }}</span>
                    <span v-if="party.members?.[i - 1]?.isCreator" class="creator-tag">ХОСТ</span>
                  </div>
                </router-link>
                <button
                  v-if="isCreator && isActive && !party.members?.[i - 1]?.isCreator"
                  class="kick-btn"
                  :disabled="actionLoading"
                  @click="handleKick(party.members?.[i - 1]?.userId ?? 0)"
                  title="Кікнути гравця"
                >✕</button>
              </template>
              <template v-else>
                <div class="empty-slot-content">
                  <span class="empty-slot-icon">+</span>
                  <span class="empty-slot-text">Вільне місце</span>
                </div>
              </template>
            </div>
          </div>
        </div>

        <div v-if="actionError" class="action-error">
          {{ actionError }}
        </div>

        <div v-if="party.chatId" class="detail-section communication-section ink-panel">
          <PartyMiniChat
            :chat-id="party.chatId"
            :title="party.title || party.gameName"
            :is-member="isMember"
            :is-logged-in="auth.isLoggedIn"
          />
        </div>

        <div class="detail-actions ink-panel">
          <div class="actions-row actions-primary">
            <button
              v-if="canJoin"
              class="action-btn primary"
              :disabled="actionLoading"
              @click="handleJoin"
            >
              <span class="action-icon">⚡</span>
              {{ actionLoading ? 'ПРИЄДНАННЯ...' : 'ПРИЄДНАТИСЯ' }}
            </button>

            <div v-else-if="!isMember && party.status === 'FULL'" class="status-info">
              <span class="status-info-icon">🔒</span> Лобі заповнене
            </div>

            <div v-else-if="!isMember && party.status === 'IN_GAME'" class="status-info">
              <span class="status-info-icon">🎮</span> Гра вже йде
            </div>

            <div v-else-if="!isMember && (party.status === 'COMPLETED' || party.status === 'CANCELLED')" class="status-info">
              <span class="status-info-icon">{{ party.status === 'COMPLETED' ? '✅' : '❌' }}</span>
              {{ party.status === 'COMPLETED' ? 'Гру завершено' : 'Лобі скасовано' }}
            </div>

            <router-link v-else-if="!auth.isLoggedIn && party.status === 'OPEN'" to="/login" class="action-btn primary">
              <span class="action-icon">🔑</span> УВІЙТИ ДЛЯ ПРИЄДНАННЯ
            </router-link>

            <button
              v-if="canStart"
              class="action-btn start"
              :disabled="actionLoading"
              @click="handleStartGame"
            >
              <span class="action-icon">▶</span>
              {{ actionLoading ? '...' : 'ПОЧАТИ ГРУ' }}
            </button>

            <button
              v-if="canComplete"
              class="action-btn complete"
              :disabled="actionLoading"
              @click="handleComplete"
            >
              <span class="action-icon">✓</span>
              {{ actionLoading ? '...' : 'ЗАВЕРШИТИ ГРУ' }}
            </button>
          </div>

          <div v-if="isMember && isActive" class="actions-row actions-secondary">
            <button
              v-if="party.chatId"
              class="action-btn ghost"
              @click="goToChat"
            >
              <span class="action-icon">💬</span> ПОВНИЙ ЧАТ
            </button>

            <button
              v-if="shouldShowExpandedVoicePanel"
              class="action-btn ghost"
              @click="voiceStore.collapseWidget()"
            >
              <span class="action-icon">🎧</span> ЗГОРНУТИ ГОЛОС
            </button>

            <button
              v-if="canInvite"
              class="action-btn ghost"
              @click="showInviteModal = true"
            >
              <span class="action-icon">✉️</span> ЗАПРОСИТИ
            </button>

            <button
              v-if="isCreator"
              class="action-btn danger-ghost"
              :disabled="actionLoading"
              @click="handleClose"
            >
              {{ actionLoading ? '...' : 'ЗАКРИТИ' }}
            </button>

            <button
              v-if="!isCreator"
              class="action-btn danger-ghost"
              :disabled="actionLoading"
              @click="handleLeave"
            >
              {{ actionLoading ? '...' : 'ПОКИНУТИ' }}
            </button>
          </div>

          <div v-if="!isActive && isMember && party.chatId" class="actions-row">
            <button class="action-btn ghost" @click="goToChat">
              <span class="action-icon">💬</span> ПОВНИЙ ЧАТ
            </button>
          </div>
        </div>

        <InviteToPartyModal
          v-if="party"
          :visible="showInviteModal"
          :party-id="party.id"
          :party-members="partyMemberIds"
          @close="showInviteModal = false"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.party-detail-page {
  padding-top: 64px;
  min-height: 100vh;
  background: var(--black);
}

.party-detail-container {
  max-width: 860px;
  margin: 0 auto;
  padding: 32px;
}

.party-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.back-link {
  background: none;
  border: none;
  color: var(--gray);
  font-family: var(--font-body);
  font-size: 13px;
  letter-spacing: 1px;
  padding: 4px 0;
  transition: color 0.15s;
  align-self: flex-start;
}
.back-link:hover {
  color: var(--yellow);
}

.detail-header {
  padding: 28px;
}

.detail-header-top {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.detail-game-cover img {
  width: 70px;
  height: 94px;
  object-fit: cover;
  border: 2px solid var(--border);
  image-rendering: pixelated;
}

.cover-placeholder {
  width: 70px;
  height: 94px;
  background: var(--panel-bg);
  border: 2px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
}

.detail-header-info {
  flex: 1;
}

.detail-game-name {
  font-family: var(--font-display);
  font-size: 32px;
  color: var(--white);
  margin: 0 0 8px;
}

.detail-host {
  font-size: 14px;
  color: var(--gray);
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.host-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--yellow);
  text-decoration: none;
}

.host-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--yellow);
}

.detail-time {
  margin-top: 8px;
  color: var(--gray);
  font-size: 13px;
}

.status-badge {
  display: inline-block;
  padding: 8px 14px;
  font-family: var(--font-display);
  font-size: 12px;
  letter-spacing: 1px;
  border: 2px solid var(--border);
  background: var(--panel-bg);
}
.status-badge.open {
  color: var(--yellow);
}
.status-badge.full {
  color: #ff9257;
}
.status-badge.in-game {
  color: #7fffb0;
}
.status-badge.completed {
  color: #8ee2ff;
}
.status-badge.closed {
  color: #ff8f8f;
}

.detail-title h2 {
  margin: 0 0 10px;
  font-size: 22px;
  font-family: var(--font-display);
  color: var(--white);
}

.detail-description p {
  margin: 0;
  color: var(--white);
  line-height: 1.65;
}

.detail-tags {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.platform-tag,
.tag,
.skill-badge {
  padding: 6px 10px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.04);
  font-size: 12px;
  letter-spacing: 0.6px;
}

.platform-tag {
  color: var(--yellow);
}

.tag {
  color: var(--gray-light);
}

.tag-region {
  color: #8ee2ff;
}

.tag-custom {
  color: #ffa9de;
}

.skill-badge.beginner {
  color: #8fd8ff;
}
.skill-badge.intermediate {
  color: #ffe07c;
}
.skill-badge.pro {
  color: #ff8a8a;
}

.detail-section {
  padding: 24px;
}

.section-label {
  font-family: var(--font-body);
  font-size: 22px;
  color: var(--white);
  margin: 0 0 18px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.member-count {
  font-size: 13px;
  color: var(--yellow);
}

.slots-visual {
  display: grid;
  gap: 14px;
}

.member-slot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  border: 2px dashed rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.02);
}

.member-slot.filled {
  border-style: solid;
  border-color: var(--border);
}

.member-card {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  color: inherit;
}

.member-avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--yellow);
}

.member-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.member-name {
  color: var(--white);
  font-family: var(--font-display);
}

.creator-tag {
  font-size: 11px;
  color: var(--yellow);
  letter-spacing: 1px;
}

.kick-btn {
  background: transparent;
  color: #ff8e8e;
  border: 1px solid rgba(255, 142, 142, 0.5);
  border-radius: 8px;
  width: 34px;
  height: 34px;
}

.empty-slot-content {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--gray);
}

.empty-slot-icon {
  font-size: 18px;
  color: var(--yellow);
}

.action-error {
  padding: 14px 16px;
  border: 1px solid rgba(255, 107, 107, 0.3);
  background: rgba(255, 107, 107, 0.08);
  color: #ffb0b0;
}

.detail-actions {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 24px;
}

.actions-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.action-btn {
  border: 2px solid var(--border);
  background: var(--panel-bg);
  color: var(--white);
  padding: 12px 18px;
  font-family: var(--font-display);
  font-size: 13px;
  letter-spacing: 1px;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: transform 0.15s, border-color 0.15s, color 0.15s;
}

.action-btn:hover {
  transform: translateY(-1px);
  border-color: var(--yellow);
}

.action-btn.primary {
  background: var(--yellow);
  color: var(--black);
}

.action-btn.start {
  color: #9df7be;
}

.action-btn.complete {
  color: #8ee2ff;
}

.action-btn.ghost {
  color: var(--yellow);
}

.action-btn.danger-ghost {
  color: #ff8e8e;
}

.status-info {
  color: var(--gray);
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.communication-section {
  padding: 0;
  overflow: hidden;
}

@media (max-width: 768px) {
  .party-detail-container {
    padding: 20px;
  }

  .detail-header-top {
    flex-direction: column;
  }

  .detail-game-name {
    font-size: 26px;
  }

  .section-label {
    flex-direction: column;
    align-items: flex-start;
  }

  .member-slot {
    align-items: flex-start;
  }

  .detail-actions,
  .detail-section,
  .detail-header {
    padding: 20px;
  }
}
</style>
