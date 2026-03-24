<script setup lang="ts">
import type { VoiceParticipant } from '../types'

defineProps<{
  participants: VoiceParticipant[]
  compact?: boolean
}>()
</script>

<template>
  <div class="voice-participants" :class="{ compact }">
    <div v-if="!participants.length" class="voice-empty-state">
      У голосовому чаті поки нікого немає.
    </div>

    <div
      v-for="participant in participants"
      :key="participant.identity"
      class="voice-participant"
      :class="{
        speaking: participant.isSpeaking,
        muted: !participant.isMicrophoneEnabled,
        local: participant.isLocal,
      }"
    >
      <div class="voice-participant-main">
        <div class="voice-participant-avatar">
          {{ participant.name.slice(0, 1).toUpperCase() }}
        </div>

        <div class="voice-participant-meta">
          <div class="voice-participant-name-row">
            <span class="voice-participant-name">{{ participant.name }}</span>
            <span v-if="participant.isLocal" class="voice-self-tag">Ти</span>
          </div>

          <div class="voice-participant-status">
            <span class="voice-mic-state">
              {{ participant.isMicrophoneEnabled ? 'Мікрофон увімкнено' : 'Без мікрофона' }}
            </span>
            <span v-if="participant.isSpeaking" class="voice-speaking-label">говорить</span>
          </div>
        </div>
      </div>

      <div class="voice-level-wrap">
        <div class="voice-level-bar">
          <div
            class="voice-level-fill"
            :style="{ width: `${Math.max(8, Math.min(100, participant.audioLevel * 120))}%` }"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.voice-participants {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.voice-empty-state {
  padding: 14px 16px;
  border: 1px dashed rgba(255, 255, 255, 0.14);
  border-radius: 14px;
  color: var(--gray);
  font-size: 13px;
  background: rgba(255, 255, 255, 0.02);
}

.voice-participant {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  transition: border-color 0.18s ease, transform 0.18s ease, background 0.18s ease;
}

.voice-participant.speaking {
  border-color: rgba(255, 214, 10, 0.42);
  background: rgba(255, 214, 10, 0.08);
}

.voice-participant.muted {
  opacity: 0.72;
}

.voice-participant.local {
  box-shadow: inset 0 0 0 1px rgba(255, 214, 10, 0.12);
}

.voice-participant-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.voice-participant-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 214, 10, 0.14);
  border: 1px solid rgba(255, 214, 10, 0.18);
  color: var(--yellow);
  font-family: var(--font-display);
  font-size: 16px;
}

.voice-participant-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  flex: 1;
}

.voice-participant-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.voice-participant-name {
  color: var(--white);
  font-family: var(--font-display);
  letter-spacing: 0.5px;
}

.voice-self-tag {
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(255, 214, 10, 0.12);
  color: var(--yellow);
  font-size: 11px;
  letter-spacing: 0.8px;
  text-transform: uppercase;
}

.voice-participant-status {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--gray);
  font-size: 12px;
}

.voice-speaking-label {
  color: var(--yellow);
}

.voice-level-wrap {
  display: flex;
  align-items: center;
}

.voice-level-bar {
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  overflow: hidden;
}

.voice-level-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(255, 214, 10, 0.55), rgba(255, 214, 10, 1));
  transition: width 0.12s ease;
}

.voice-participants.compact .voice-participant {
  padding: 10px 12px;
}

.voice-participants.compact .voice-participant-avatar {
  width: 32px;
  height: 32px;
  font-size: 13px;
}

.voice-participants.compact .voice-participant-name {
  font-size: 13px;
}

.voice-participants.compact .voice-participant-status {
  font-size: 11px;
}
</style>
