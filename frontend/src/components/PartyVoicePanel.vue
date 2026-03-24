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

const panelTitle = computed(() => props.party.title?.trim() || props.party.gameName)
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
        <div class="voice-kicker">VOICE CHANNEL</div>
        <h3 class="voice-title">{{ panelTitle }}</h3>
        <p class="voice-meta">
          Голосовий канал для учасників цього лобі.
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

      <div v-else class="voice-idle-state">
        Щойно ти підключишся, голосовий чат залишатиметься активним навіть під час переходу між сторінками.
      </div>
    </div>
  </div>
</template>

<style scoped>
.party-voice-panel {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.voice-panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.voice-kicker {
  font-size: 11px;
  letter-spacing: 1.8px;
  color: var(--yellow);
  margin-bottom: 8px;
}

.voice-title {
  margin: 0;
  font-family: var(--font-body);
  color: var(--white);
  letter-spacing: 1px;
}

.voice-meta {
  margin-top: 8px;
  margin-bottom: 0;
  color: var(--gray);
  font-size: 13px;
  line-height: 1.5;
}

.voice-connection-state {
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 11px;
  letter-spacing: 1px;
  text-transform: uppercase;
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: var(--gray);
  white-space: nowrap;
}

.voice-connection-state.connected {
  color: var(--yellow);
  border-color: rgba(255, 214, 10, 0.3);
  background: rgba(255, 214, 10, 0.08);
}

.voice-connection-state.reconnecting,
.voice-connection-state.connecting {
  color: var(--white);
}

.voice-panel-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.voice-actions-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.voice-action-btn,
.voice-inline-btn {
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.04);
  color: var(--white);
  border-radius: 12px;
  padding: 12px 16px;
  font-family: var(--font-display);
  font-size: 12px;
  letter-spacing: 0.9px;
  transition: 0.15s ease;
}

.voice-action-btn:hover,
.voice-inline-btn:hover {
  border-color: rgba(255, 214, 10, 0.25);
  color: var(--yellow);
}

.voice-action-btn.primary {
  background: var(--yellow);
  border-color: var(--yellow);
  color: var(--black);
}

.voice-action-btn.primary:hover {
  color: var(--black);
  transform: translateY(-1px);
}

.voice-action-btn.danger {
  color: #ff8e8e;
}

.voice-action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.voice-locked-state,
.voice-idle-state,
.voice-notice {
  border-radius: 16px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
}

.voice-locked-state {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.voice-locked-icon {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: rgba(255, 214, 10, 0.08);
  font-size: 20px;
}

.voice-locked-state h4 {
  margin: 0 0 6px;
  color: var(--white);
}

.voice-locked-state p,
.voice-idle-state,
.voice-notice {
  margin: 0;
  color: var(--gray);
  font-size: 13px;
  line-height: 1.55;
}

.voice-notice.warning {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.voice-notice.error {
  border-color: rgba(255, 107, 107, 0.24);
  color: #ffaeae;
}

.voice-participants-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.voice-section-label {
  font-size: 12px;
  letter-spacing: 1.1px;
  color: var(--yellow);
}

@media (max-width: 720px) {
  .party-voice-panel {
    padding: 20px;
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
}
</style>
