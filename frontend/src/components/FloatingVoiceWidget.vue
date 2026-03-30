<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useVoiceStore } from '../stores/voice'
import VoiceParticipantsList from './VoiceParticipantsList.vue'

const voice = useVoiceStore()
const router = useRouter()
const route = useRoute()

const isOnCurrentPartyPage = computed(() => {
  if (route.name !== 'party-detail') return false
  const routePartyId = Number(route.params.id)
  return Number.isFinite(routePartyId) && routePartyId === voice.currentPartyId
})

const shouldRender = computed(() => {
  if (!voice.shouldShowFloatingWidget) return false
  return !(isOnCurrentPartyPage.value && voice.isExpanded)
})

const statusLabel = computed(() => {
  if (voice.connectionState === 'connected') return 'У голосі'
  if (voice.connectionState === 'reconnecting') return 'Перепідключення'
  if (voice.connectionState === 'connecting') return 'Підключення'
  if (voice.connectionState === 'error') return 'Помилка'
  return 'Неактивно'
})

async function handleToggleMic() {
  await voice.toggleMic()
}

async function handleLeave() {
  await voice.leaveVoice()
}

async function handleStartAudio() {
  await voice.startAudioPlayback()
}

async function openPartyRoom() {
  const partyId = voice.currentPartyId
  if (!partyId) return

  voice.expandWidget()
  if (!isOnCurrentPartyPage.value) {
    await router.push(`/party/${partyId}`)
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="shouldRender" class="floating-voice-widget" :class="{ expanded: voice.isExpanded }">
      <div class="floating-widget-head">
        <div class="floating-widget-main">
          <div class="floating-widget-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 18v-6a9 9 0 0 1 18 0v6"/><path d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3z"/><path d="M3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z"/></svg>
          </div>
          <div class="floating-widget-meta">
            <div class="floating-widget-title">{{ voice.currentPartyTitle || 'Voice chat' }}</div>
            <div class="floating-widget-status">{{ statusLabel }} • {{ voice.participantCount }} учасників</div>
          </div>
        </div>

        <button class="floating-widget-toggle" @click="voice.isExpanded ? voice.collapseWidget() : voice.expandWidget()">
          {{ voice.isExpanded ? '–' : '+' }}
        </button>
      </div>

      <div class="floating-widget-actions">
        <button class="floating-widget-btn" @click="handleToggleMic">
          {{ voice.isMicEnabled ? 'МІКРОФОН ON' : 'МІКРОФОН OFF' }}
        </button>
        <button class="floating-widget-btn" @click="openPartyRoom">ДО ЛОБІ</button>
        <button class="floating-widget-btn danger" @click="handleLeave">ВИЙТИ</button>
      </div>

      <div v-if="voice.isExpanded" class="floating-widget-body">
        <div v-if="voice.autoplayBlocked" class="floating-widget-notice">
          Звук заблоковано браузером.
          <button class="floating-widget-inline-btn" @click="handleStartAudio">УВІМКНУТИ</button>
        </div>

        <div v-if="voice.error" class="floating-widget-error">{{ voice.error }}</div>

        <VoiceParticipantsList :participants="voice.participants" compact />
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.floating-voice-widget {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 1200;
  width: min(360px, calc(100vw - 24px));
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(13, 13, 13, 0.96);
  backdrop-filter: blur(12px);
  box-shadow: 0 18px 54px rgba(0, 0, 0, 0.4);
}

.floating-widget-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.floating-widget-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.floating-widget-icon {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 214, 10, 0.1);
  border: 1px solid rgba(255, 214, 10, 0.16);
  font-size: 18px;
}

.floating-widget-meta {
  min-width: 0;
}

.floating-widget-title {
  color: var(--white);
  font-family: var(--font-body);
  font-size: 14px;
  letter-spacing: 0.5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.floating-widget-status {
  margin-top: 4px;
  color: var(--gray);
  font-size: 12px;
}

.floating-widget-toggle,
.floating-widget-btn,
.floating-widget-inline-btn {
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.04);
  color: var(--white);
  border-radius: 12px;
  transition: 0.15s ease;
}

.floating-widget-toggle {
  width: 34px;
  height: 34px;
  font-size: 18px;
}

.floating-widget-toggle:hover,
.floating-widget-btn:hover,
.floating-widget-inline-btn:hover {
  border-color: rgba(255, 214, 10, 0.22);
  color: var(--yellow);
}

.floating-widget-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.floating-widget-btn,
.floating-widget-inline-btn {
  padding: 10px 12px;
  font-family: var(--font-body);
  font-size: 11px;
  letter-spacing: 0.8px;
}

.floating-widget-btn.danger {
  color: #ff8e8e;
}

.floating-widget-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.floating-widget-notice,
.floating-widget-error {
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  color: var(--gray);
  font-size: 12px;
  line-height: 1.5;
}

.floating-widget-notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.floating-widget-error {
  border-color: rgba(255, 107, 107, 0.24);
  color: #ffaeae;
}

@media (max-width: 720px) {
  .floating-voice-widget {
    right: 12px;
    bottom: 12px;
    width: calc(100vw - 24px);
  }

  .floating-widget-actions {
    grid-template-columns: 1fr;
  }

  .floating-widget-notice {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
