<script setup lang="ts">
import { computed } from 'vue'
import type { Friend } from '../types'
import { PUBLIC_BASE_URL } from '../config'
import { timeAgo } from '../utils/helpers'
import PlayerRatingBadge from './PlayerRatingBadge.vue'

const props = defineProps<{
  title: string
  friends: Friend[]
}>()

const emit = defineEmits<{
  close: []
  openProfile: [userId: number]
}>()

function resolveAvatar(url: string | null): string {
  if (!url) return PUBLIC_BASE_URL + '/avatars/default/avatar-1.png'
  if (url.startsWith('http')) return url
  return PUBLIC_BASE_URL + url
}

const sortedFriends = computed(() =>
  [...props.friends].sort((a, b) => {
    if (a.status === 'ONLINE' && b.status !== 'ONLINE') return -1
    if (a.status !== 'ONLINE' && b.status === 'ONLINE') return 1
    return a.displayName.localeCompare(b.displayName)
  })
)
</script>

<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal-box">
      <div class="modal-header">
        <h3>{{ title }}</h3>
        <button class="modal-close" @click="emit('close')">✕</button>
      </div>

      <div v-if="sortedFriends.length === 0" class="modal-empty">Список друзів порожній</div>

      <div v-else class="friends-list">
        <button
          v-for="friend in sortedFriends"
          :key="friend.userId"
          class="friend-item"
          @click="emit('openProfile', friend.userId)"
        >
          <span class="ava-wrap">
            <img :src="resolveAvatar(friend.avatarUrl)" :alt="friend.displayName" class="friend-ava" />
            <span class="dot" :class="friend.status === 'ONLINE' ? 'on' : 'off'"></span>
          </span>
          <span class="friend-meta">
            <span class="friend-name">
              {{ friend.displayName }}
              <PlayerRatingBadge :rating="friend.rating" size="sm" />
            </span>
            <span class="friend-status" :class="{ on: friend.status === 'ONLINE' }">
              <template v-if="friend.status === 'ONLINE'">● Online</template>
              <template v-else-if="friend.lastSeen">○ {{ timeAgo(friend.lastSeen) }}</template>
              <template v-else>○ Офлайн</template>
            </span>
          </span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.82);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1100;
  padding: 20px;
}

.modal-box {
  width: min(640px, 100%);
  max-height: 84vh;
  overflow: hidden;
  background: var(--panel);
  border: 2px solid var(--border);
  display: flex;
  flex-direction: column;
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.55);
  position: relative;
}

.modal-box::after {
  content: '';
  position: absolute;
  inset: 4px;
  border: 1px solid rgba(245, 197, 24, 0.08);
  pointer-events: none;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid var(--border);
  background: var(--panel);
}

.modal-header h3 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 2px;
  color: var(--yellow);
  text-transform: uppercase;
}

.modal-close {
  border: 1px solid var(--border);
  background: var(--dark);
  color: var(--gray);
  width: 30px;
  height: 30px;
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s;
}

.modal-close:hover {
  border-color: var(--red);
  color: var(--red);
}

.modal-empty {
  padding: 28px;
  color: var(--gray);
  text-align: center;
  font-size: 14px;
}

.friends-list {
  overflow: auto;
  display: flex;
  flex-direction: column;
  padding: 10px;
  gap: 7px;
}

.friend-item {
  border: 1px solid var(--border);
  background: var(--dark);
  color: inherit;
  text-align: left;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s, transform 0.1s;
}

.friend-item:hover {
  background: rgba(245, 197, 24, 0.06);
  border-color: rgba(245, 197, 24, 0.4);
  transform: translateY(-1px);
}

.ava-wrap {
  position: relative;
  display: inline-flex;
}

.friend-ava {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border: 2px solid var(--border-glow);
}

.dot {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid var(--panel);
}

.dot.on { background: var(--green); }
.dot.off { background: var(--gray); }

.friend-name {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--white);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.4px;
  margin-bottom: 2px;
}

.friend-meta {
  display: flex;
  flex-direction: column;
}

.friend-status {
  display: block;
  color: var(--gray);
  font-size: 11px;
}

.friend-status.on {
  color: var(--green);
}
</style>



