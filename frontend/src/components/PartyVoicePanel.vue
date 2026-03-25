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
        <div class="voice-kicker">Voice</div>
        <h3 class="voice-title">Голосовий чат</h3>
        <p class="voice-meta">
          Тільки для учасників лобі.
          <span v-if="isCurrentPartyVoice">• {{ voice.participantCount }} у каналі</span>
        </p>
      </div>

      <div class="voice-connection-state" :class="voice.connectionState">
        {{ isCurrentPartyVoice ? statusLabel : 'Не в каналі' }}
      </div>
    </div>

    <div v-if="!isLoggedIn" class="voice-lockbox">
      <div class="voice-lock-icon">🔑</div>
      <div>
        <h4>Увійди, щоб користуватися voice</h4>
        <p>Голосовий канал відкривається тільки після авторизації.</p>
      </div>
    </div>

    <div v-else-if="!isMember" class="voice-lockbox">
      <div class="voice-lock-icon">🎧</div>
      <div>
        <h4>Приєднайся до лобі</h4>
        <p>Після вступу до команди зможеш зайти у голосовий канал.</p>
      </div>
    </div>

    <div v-else-if="!canJoinVoice" class="voice-lockbox">
      <div class="voice-lock-icon">⛔</div>
      <div>
        <h4>Голос недоступний</h4>
        <p>Voice працює лише для активних лобі</p>
      </div>
    </div>

    <div v-else class="voice-panel-body">
      <div class="voice-actions-grid">
        <button
            v-if="!isCurrentPartyVoice"
            type="button"
            class="voice-action-btn voice-action-btn--primary"
            :disabled="voice.isConnecting"
            @click="handleJoin"
        >
          {{ voice.isConnecting ? 'Підключення...' : 'Приєднатися до голосу' }}
        </button>

        <template v-else>
          <button type="button" class="voice-action-btn" @click="handleToggleMic">
            {{ voice.isMicEnabled ? 'Вимкнути мікрофон' : 'Увімкнути мікрофон' }}
          </button>

          <button type="button" class="voice-action-btn" @click="voice.collapseWidget()">
            Згорнути у віджет
          </button>

          <button type="button" class="voice-action-btn voice-action-btn--danger" @click="handleLeave">
            Покинути канал
          </button>
        </template>
      </div>

      <div v-if="voice.autoplayBlocked && isCurrentPartyVoice" class="voice-notice voice-notice--warning">
        <div>Браузер заблокував звук. Натисни кнопку, щоб почути учасників.</div>
        <button type="button" class="voice-inline-btn" @click="handleStartAudio">Увімкнути звук</button>
      </div>

      <div v-if="voice.error && isCurrentPartyVoice" class="voice-notice voice-notice--error">
        {{ voice.error }}
      </div>

      <div v-if="isCurrentPartyVoice" class="voice-participants-section">
        <div class="voice-section-label">Учасники каналу</div>
        <VoiceParticipantsList :participants="voice.participants" :compact="true" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.party-voice-panel {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.voice-panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border);
}

.voice-kicker,
.voice-section-label {
  font-family: var(--font-display);
  font-size: 12px;
  letter-spacing: 1.7px;
  text-transform: uppercase;
  color: var(--gray);
}

.voice-title {
  margin: 4px 0 0;
  font-family: var(--font-display);
  font-size: 28px;
  line-height: 0.95;
  color: var(--white);
}

.voice-meta {
  margin-top: 8px;
  color: var(--gray-light);
  font-size: 13px;
  line-height: 1.45;
}

.voice-connection-state {
  flex-shrink: 0;
  padding: 8px 10px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.03);
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 0.8px;
  text-transform: uppercase;
  color: var(--gray-light);
  white-space: nowrap;
}

.voice-connection-state.connected {
  color: var(--yellow);
  border-color: rgba(245, 197, 24, 0.34);
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

.voice-lockbox,
.voice-notice {
  padding: 14px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.03);
}

.voice-lockbox {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.voice-lock-icon {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(245, 197, 24, 0.28);
  background: rgba(245, 197, 24, 0.08);
  font-size: 18px;
  flex-shrink: 0;
}

.voice-lockbox h4 {
  margin: 0 0 4px;
  font-family: var(--font-display);
  font-size: 22px;
  line-height: 1;
  color: var(--white);
}

.voice-lockbox p,
.voice-notice {
  margin: 0;
  color: var(--gray-light);
  font-size: 14px;
  line-height: 1.5;
}

.voice-panel-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.voice-actions-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.voice-actions-grid > :first-child:last-child {
  grid-column: 1 / -1;
}

.voice-action-btn,
.voice-inline-btn {
  min-height: 42px;
  padding: 10px 14px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.03);
  color: var(--white);
  font-family: var(--font-display);
  font-size: 15px;
  letter-spacing: 0.8px;
  text-transform: uppercase;
  transition: transform 0.15s ease, border-color 0.15s ease, color 0.15s ease, background 0.15s ease;
}

.voice-action-btn:hover,
.voice-inline-btn:hover {
  transform: translateY(-1px);
  border-color: var(--yellow-dim);
  color: var(--yellow);
}

.voice-action-btn--primary {
  background: var(--yellow);
  border-color: var(--yellow);
  color: var(--black);
}

.voice-action-btn--primary:hover {
  color: var(--black);
  background: var(--yellow-dim);
  border-color: var(--yellow-dim);
}

.voice-action-btn--danger {
  color: #ffabab;
  border-color: rgba(255, 107, 107, 0.32);
  background: rgba(255, 107, 107, 0.08);
}

.voice-action-btn--danger:hover {
  color: #ffd7d7;
  border-color: rgba(255, 107, 107, 0.7);
}

.voice-action-btn:disabled,
.voice-inline-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
}

.voice-notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.voice-notice--warning {
  border-color: rgba(245, 197, 24, 0.28);
  background: rgba(245, 197, 24, 0.08);
  color: var(--white);
}

.voice-notice--error {
  border-color: rgba(255, 107, 107, 0.32);
  background: rgba(255, 107, 107, 0.08);
  color: #ffc0c0;
}

.voice-participants-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

@media (max-width: 720px) {
  .voice-panel-head,
  .voice-notice {
    flex-direction: column;
    align-items: flex-start;
  }

  .voice-actions-grid {
    grid-template-columns: 1fr;
  }
}
</style>
