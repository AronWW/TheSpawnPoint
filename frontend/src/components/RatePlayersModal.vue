<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRatingStore } from '../stores/rating'
import { useToast } from '../composables/useToast'
import { PUBLIC_BASE_URL } from '../config'
import type { PartyMember } from '../types'

const props = defineProps<{
  visible: boolean
  partyId: number
  members: PartyMember[]
}>()

const emit = defineEmits<{ close: [] }>()

const ratingStore = useRatingStore()
const { show: showToast } = useToast()

const scores = ref<Map<number, number>>(new Map())
const hoveredStars = ref<Map<number, number>>(new Map())
const loading = ref(false)
const submitted = ref(false)
const error = ref('')

const hasAnyRating = computed(() => scores.value.size > 0)

function resolveAvatar(url: string | null): string {
  if (!url) return PUBLIC_BASE_URL + '/avatars/default/avatar-1.png'
  if (url.startsWith('http')) return url
  return PUBLIC_BASE_URL + url
}

function setHover(userId: number, star: number) {
  hoveredStars.value.set(userId, star)
}

function clearHover(userId: number) {
  hoveredStars.value.delete(userId)
}

function setScore(userId: number, star: number) {
  if (scores.value.get(userId) === star) {
    scores.value.delete(userId)
  } else {
    scores.value.set(userId, star)
  }
}

function getStarState(userId: number, star: number): 'active' | 'hovered' | 'empty' {
  const hovered = hoveredStars.value.get(userId)
  const selected = scores.value.get(userId)

  if (hovered != null && star <= hovered) return 'hovered'
  if (selected != null && star <= selected) return 'active'
  return 'empty'
}

async function submit() {
  if (!hasAnyRating.value || loading.value) return

  loading.value = true
  error.value = ''

  try {
    const ratings = Array.from(scores.value.entries()).map(([userId, score]) => ({
      userId,
      score,
    }))
    await ratingStore.submitRatings(props.partyId, ratings)
    submitted.value = true
    showToast('Оцінки надіслано!', 'success')
    setTimeout(() => emit('close'), 1200)
  } catch (e: any) {
    error.value = e.response?.data?.message || 'Помилка надсилання оцінок'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="modal-overlay" @click.self="emit('close')">
      <div class="modal-box rate-modal">
        <button class="modal-close" @click="emit('close')" title="Закрити">✕</button>

        <h3 class="modal-title">ОЦІНІТЬ ГРАВЦІВ</h3>

        <div v-if="submitted" class="success-msg">
          <span class="success-icon">✓</span>
          Дякуємо за ваші оцінки!
        </div>

        <template v-else>
          <p class="modal-hint">
            Рейтинг анонімний. Після закриття вікна оцінити буде неможливо.
          </p>

          <div class="players-list">
            <div
              v-for="member in members"
              :key="member.userId"
              class="player-row"
            >
              <div class="player-info">
                <img
                  :src="resolveAvatar(member.avatarUrl)"
                  :alt="member.displayName"
                  class="player-avatar"
                />
                <span class="player-name">{{ member.displayName }}</span>
              </div>

              <div class="stars-row">
                <button
                  v-for="star in 5"
                  :key="star"
                  class="star-btn"
                  :class="getStarState(member.userId, star)"
                  @mouseenter="setHover(member.userId, star)"
                  @mouseleave="clearHover(member.userId)"
                  @click="setScore(member.userId, star)"
                  :title="`${star} з 5`"
                >
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path d="M10 1.3l2.39 5.76 6.11.58-4.65 4.06 1.4 6.01L10 14.64l-5.25 3.07 1.4-6.01L1.5 7.64l6.11-.58z" />
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <div v-if="error" class="error-msg">{{ error }}</div>

          <div class="modal-actions">
            <button
              class="modal-btn primary"
              :disabled="!hasAnyRating || loading"
              @click="submit"
            >
              {{ loading ? 'Надсилання...' : 'НАДІСЛАТИ' }}
            </button>
            <button class="modal-btn" @click="emit('close')">ПРОПУСТИТИ</button>
          </div>
        </template>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-box {
  background: var(--panel);
  border: 1px solid var(--border);
  padding: 28px;
  min-width: 420px;
  max-width: 520px;
  width: 100%;
  position: relative;
}

.modal-close {
  position: absolute;
  top: 12px;
  right: 14px;
  background: none;
  border: none;
  color: var(--gray);
  font-size: 18px;
  cursor: pointer;
  padding: 4px 8px;
  transition: color 0.15s;
}
.modal-close:hover {
  color: var(--white);
}

.modal-title {
  font-family: var(--font-display);
  font-size: 1.3rem;
  color: var(--yellow);
  letter-spacing: 3px;
  margin-bottom: 8px;
}

.modal-hint {
  color: var(--gray);
  font-size: 0.8rem;
  margin-bottom: 20px;
  line-height: 1.4;
}

.players-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 18px;
}

.player-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: var(--panel-light);
  border: 1px solid var(--border);
  transition: border-color 0.15s;
}

.player-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.player-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border);
}

.player-name {
  font-family: var(--font-body);
  font-weight: 600;
  font-size: 14px;
  color: var(--white);
  letter-spacing: 0.5px;
}

.stars-row {
  display: flex;
  gap: 2px;
}

.star-btn {
  background: none;
  border: none;
  padding: 2px;
  cursor: pointer;
  transition: transform 0.12s ease, color 0.12s ease;
  color: #3a3a35;
}

.star-btn svg {
  width: 24px;
  height: 24px;
}

.star-btn:hover {
  transform: scale(1.15);
}

.star-btn.hovered {
  color: rgba(245, 197, 24, 0.55);
}

.star-btn.active {
  color: #F5C518;
}

.modal-actions {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}

.modal-btn {
  padding: 10px 22px;
  border: 1px solid var(--border);
  background: var(--panel);
  color: var(--gray-light);
  cursor: pointer;
  font-family: var(--font-display);
  font-size: 0.85rem;
  letter-spacing: 2px;
  transition: all 0.15s;
}

.modal-btn:hover {
  border-color: var(--yellow-dim);
  color: var(--white);
}

.modal-btn.primary {
  background: var(--yellow);
  color: var(--black);
  border-color: var(--yellow);
}

.modal-btn.primary:hover {
  background: var(--yellow-dim);
}

.modal-btn:disabled {
  opacity: 0.4;
  cursor: default;
}

.success-msg {
  color: #4caf50;
  font-size: 1.1rem;
  padding: 30px 0;
  text-align: center;
}
.success-icon {
  margin-right: 6px;
}

.error-msg {
  color: var(--red);
  font-size: 0.85rem;
  margin-bottom: 8px;
}

@media (max-width: 520px) {
  .modal-box {
    min-width: 0;
    margin: 0 12px;
    padding: 20px 16px;
  }

  .player-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .stars-row {
    align-self: flex-end;
  }
}
</style>



