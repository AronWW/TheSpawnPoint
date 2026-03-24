<script setup lang="ts">
import { computed } from 'vue'
import { useVoiceStore } from '../stores/voice'
import VoiceParticipantsList from './VoiceParticipantsList.vue'
import type { Party } from '../types'

const props = defineProps<{
  party: Party
  isMember: boolean
  isLoggedIn: boolean
}>()

const voice = useVoiceStore()
const activeStatuses = new Set<Party['status']>(['OPEN', 'FULL', 'IN_GAME'])

const isCurrentPartyVoice = computed(() => voice.isInPartyVoice(props.party.id))
const canJoinVoice = computed(() => props.isLoggedIn && props.isMember && activeStatuses.has(props.party.status))

const statusLabel = computed(() => {
  if (voice.connectionState === 'connected') return 'Підключено'
  if (voice.connectionState === 'reconnecting') return 'Перепідключення'
  if (voice.connectionState === 'connecting') return 'Підключення'
  if (voice.connectionState === 'error') return 'Помилка'
  return 'Відключено'
})

async function handleJoin() {
  await voice.joinPartyVoice(props.party)
}

async function handleLeave() {
  await voice.leaveVoice()
}

async function handleToggleMic() {
  await voice.toggleMic()
}

async function handleStartAudio() {
  await voice.startAudioPlayback()
}
</script>

<template>
  <div class="party-voice-panel ink-panel">
    <div class="voice-panel-head">
      <div>
        <h3 class="voice-title">Голосовий чат</h3>
        <p class="voice-meta">
          Тільки для учасників цього лобі.
          <span v-if="isCurrentPartyVoice">• {{ voice.participantCount }} в каналі</span>
        </p>
      </div>

      <div class="voice-connection-state" :class="voice.connectionState">
        {{ isCurrentPartyVoice ? statusLabel : 'Не підключено' }}
      </div>
    </div>

    <div v-if="!isLoggedIn" class="voice-locked-state">
      <div class="voice-locked-icon">🔑</div>
      <div>
        <h4>Увійди, щоб користуватися голосовим чатом</h4>
        <p>Voice-канал доступний тільки авторизованим учасникам лобі.</p>
      </div>
    </div>

    <div v-else-if="!isMember" class="voice-locked-state">
      <div class="voice-locked-icon">🎧</div>
      <div>
        <h4>Спершу приєднайся до лобі</h4>
        <p>Після входу в party ти зможеш відкрити голосовий канал.</p>
      </div>
    </div>

    <div v-else-if="!canJoinVoice" class="voice-locked-state">
      <div class="voice-locked-icon">⛔</div>
      <div>
        <h4>Голосовий чат зараз недоступний</h4>
        <p>Voice-канал працює лише для активних лобі зі статусом OPEN, FULL або IN_GAME.</p>
      </div>
    </div>

    <div v-else class="voice-panel-body">
      <div class="voice-actions-row">
        <button
          v-if="!isCurrentPartyVoice"
          class="voice-action-btn primary"
          :disabled="voice.isConnecting"
          @click="handleJoin"
        >
          {{ voice.isConnecting ? 'ПІДКЛЮЧЕННЯ...' : 'ПРИЄДНАТИСЯ ДО ГОЛОСУ' }}
        </button>

        <template v-else>
          <button class="voice-action-btn" @click="handleToggleMic">
            {{ voice.isMicEnabled ? 'ВИМКНУТИ МІКРОФОН' : 'УВІМКНУТИ МІКРОФОН' }}
          </button>

          <button class="voice-action-btn" @click="voice.collapseWidget()">
            ЗГОРНУТИ У МІНІ-ВІКНО
          </button>

          <button class="voice-action-btn danger" @click="handleLeave">
            ПОКИНУТИ КАНАЛ
          </button>
        </template>
      </div>

      <div v-if="voice.autoplayBlocked && isCurrentPartyVoice" class="voice-notice warning">
        <div>
          Браузер заблокував відтворення звуку. Натисни кнопку нижче, щоб почути учасників.
        </div>
        <button class="voice-inline-btn" @click="handleStartAudio">УВІМКНУТИ ЗВУК</button>
      </div>

      <div v-if="voice.error && isCurrentPartyVoice" class="voice-notice error">
        {{ voice.error }}
      </div>

      <div v-if="isCurrentPartyVoice" class="voice-participants-section">
        <div class="voice-section-label">Учасники каналу</div>
        <VoiceParticipantsList :participants="voice.participants" />
      </div>

    </div>
  </div>
</template>

<style scoped>
.party-voice-panel {
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.voice-panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border);
}

.voice-title {
  margin: 0;
  font-family: var(--font-display), sans-serif;
  font-size: 26px;
  letter-spacing: 1.1px;
  line-height: 1;
  color: var(--white);
}

.voice-meta {
  margin-top: 10px;
  margin-bottom: 0;
  color: var(--gray-light);
  font-size: 14px;
  line-height: 1.5;
}

.voice-connection-state {
  padding: 8px 12px;
  border-radius: 2px;
  font-family: var(--font-display), sans-serif;
  font-size: 12px;
  letter-spacing: 1.2px;
  text-transform: uppercase;
  border: 2px solid var(--border);
  color: var(--gray-light);
  background: var(--dark);
  white-space: nowrap;
}

.voice-connection-state.connected {
  color: var(--yellow);
  border-color: var(--yellow-dim);
  background: rgba(245, 197, 24, 0.08);
}

.voice-connection-state.reconnecting,
.voice-connection-state.connecting {
  color: var(--white);
  border-color: var(--border-glow);
}

.voice-connection-state.error {
  color: #ffb4b4;
  border-color: rgba(255, 107, 107, 0.45);
}

.voice-panel-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.voice-actions-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.voice-action-btn,
.voice-inline-btn {
  min-height: 42px;
  border: 2px solid var(--border);
  background: var(--dark);
  color: var(--white);
  border-radius: 2px;
  padding: 10px 16px;
  font-family: var(--font-display), sans-serif;
  font-size: 13px;
  letter-spacing: 1.1px;
  text-transform: uppercase;
  transition: transform 0.15s ease, border-color 0.15s ease, color 0.15s ease, background 0.15s ease;
}

.voice-action-btn:hover,
.voice-inline-btn:hover {
  border-color: var(--yellow-dim);
  color: var(--yellow);
  transform: translateY(-1px);
}

.voice-action-btn.primary {
  background: var(--yellow);
  border-color: var(--yellow);
  color: var(--black);
}

.voice-action-btn.primary:hover {
  color: var(--black);
  background: var(--yellow-dim);
  border-color: var(--yellow-dim);
}

.voice-action-btn.danger {
  color: #ff9b9b;
  border-color: rgba(255, 107, 107, 0.45);
  background: rgba(255, 107, 107, 0.08);
}

.voice-action-btn.danger:hover {
  color: #ffc2c2;
  border-color: rgba(255, 107, 107, 0.8);
}

.voice-action-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
}

.voice-locked-state,
.voice-notice {
  border-radius: 2px;
  padding: 16px;
  border: 2px solid var(--border);
  background: var(--panel-light);
}

.voice-locked-state {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.voice-locked-icon {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--yellow-dim);
  border-radius: 2px;
  background: rgba(245, 197, 24, 0.08);
  font-size: 18px;
}

.voice-locked-state h4 {
  margin: 0 0 6px;
  color: var(--white);
  font-family: var(--font-display), sans-serif;
  letter-spacing: 1px;
  font-size: 18px;
}

.voice-locked-state p,
.voice-notice {
  margin: 0;
  color: var(--gray-light);
  font-size: 14px;
  line-height: 1.55;
}

.voice-notice.warning {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-color: var(--yellow-dim);
  background: rgba(245, 197, 24, 0.06);
}

.voice-notice.error {
  border-color: rgba(255, 107, 107, 0.45);
  background: rgba(255, 107, 107, 0.1);
  color: #ffb6b6;
}

.voice-participants-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

.voice-section-label {
  font-family: var(--font-display), sans-serif;
  font-size: 13px;
  letter-spacing: 1.2px;
  text-transform: uppercase;
  color: var(--yellow);
}

@media (max-width: 720px) {
  .party-voice-panel {
    padding: 18px;
  }

  .voice-panel-head,
  .voice-notice.warning {
    flex-direction: column;
    align-items: stretch;
  }

  .voice-connection-state {
    align-self: flex-start;
  }

  .voice-actions-row {
    flex-direction: column;
  }

  .voice-action-btn,
  .voice-inline-btn {
    width: 100%;
  }

  .voice-title {
    font-size: 22px;
  }
}
</style>
