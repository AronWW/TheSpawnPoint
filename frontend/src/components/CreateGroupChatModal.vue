<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useChatStore } from '../stores/chat'
import { useFriendStore } from '../stores/friends'
import { useToast } from '../composables/useToast'
import { API_BASE_URL } from '../config'

defineProps<{ visible: boolean }>()
const emit = defineEmits<{ (e: 'close'): void; (e: 'created'): void }>()

const chatStore = useChatStore()
const friendStore = useFriendStore()
const toast = useToast()

const PUBLIC_BASE_URL = API_BASE_URL.replace(/\/api\/?$/, '')

const title = ref('')
const selectedEmails = ref<string[]>([])
const friendSearch = ref('')
const submitting = ref(false)
const error = ref('')

function resolveAvatar(url: string | null): string {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return PUBLIC_BASE_URL + url
}

const filteredFriends = computed(() => {
  const q = friendSearch.value.toLowerCase().trim()
  let list = friendStore.friends
  if (q) {
    list = list.filter(f => f.displayName.toLowerCase().includes(q) || f.email.toLowerCase().includes(q))
  }
  return list
})

function toggleFriend(email: string) {
  const idx = selectedEmails.value.indexOf(email)
  if (idx !== -1) {
    selectedEmails.value.splice(idx, 1)
  } else {
    if (selectedEmails.value.length < 49) {
      selectedEmails.value.push(email)
    }
  }
}

function isSelected(email: string): boolean {
  return selectedEmails.value.includes(email)
}

onMounted(async () => {
  if (friendStore.friends.length === 0) {
    await friendStore.fetchFriends()
  }
})

async function submit() {
  if (!title.value.trim()) {
    error.value = 'Введіть назву чату'
    return
  }
  if (selectedEmails.value.length === 0) {
    error.value = 'Оберіть хоча б одного учасника'
    return
  }
  error.value = ''
  submitting.value = true
  try {
    await chatStore.createGroupChat(title.value.trim(), selectedEmails.value)
    toast.show('Груповий чат створено', 'success')
    resetForm()
    emit('created')
    emit('close')
  } catch (e: any) {
    error.value = e.response?.data?.message || 'Помилка при створенні чату'
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  title.value = ''
  selectedEmails.value = []
  friendSearch.value = ''
  error.value = ''
}

function close() {
  resetForm()
  emit('close')
}
</script>

<template>
  <Transition name="fade">
    <div v-if="visible" class="modal-overlay" @click.self="close">
      <div class="modal">
        <div class="modal-header">
          <div class="modal-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink:0">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
            НОВИЙ ГРУПОВИЙ ЧАТ
          </div>
          <button class="modal-close" @click="close">✕</button>
        </div>

        <div class="modal-body">
          <div v-if="error" class="modal-error">{{ error }}</div>

          <div class="form-group">
            <label class="form-label">Назва чату *</label>
            <input
              class="form-input"
              v-model="title"
              placeholder="Введіть назву групового чату..."
              maxlength="100"
            />
          </div>

          <div class="form-group">
            <label class="form-label">Учасники ({{ selectedEmails.length }}/49)</label>

            <div v-if="selectedEmails.length > 0" class="selected-members">
              <span
                v-for="email in selectedEmails"
                :key="email"
                class="member-chip"
              >
                {{ friendStore.friends.find(f => f.email === email)?.displayName || email }}
                <button class="chip-remove" @click="toggleFriend(email)">✕</button>
              </span>
            </div>

            <input
              class="form-input"
              v-model="friendSearch"
              placeholder="Пошук друзів..."
              style="margin-bottom: 8px"
            />

            <div class="friends-list">
              <div v-if="filteredFriends.length === 0" class="friends-empty">
                {{ friendStore.friends.length === 0 ? 'У вас поки немає друзів' : 'Нікого не знайдено' }}
              </div>
              <div
                v-for="friend in filteredFriends"
                :key="friend.email"
                class="friend-item"
                :class="{ selected: isSelected(friend.email) }"
                @click="toggleFriend(friend.email)"
              >
                <div class="friend-avatar">
                  <img v-if="friend.avatarUrl" :src="resolveAvatar(friend.avatarUrl)" alt="" class="friend-avatar-img" />
                  <span v-else class="friend-avatar-letter">{{ friend.displayName.charAt(0).toUpperCase() }}</span>
                </div>
                <div class="friend-info">
                  <div class="friend-name">{{ friend.displayName }}</div>
                  <div class="friend-status" :class="friend.status.toLowerCase()">{{ friend.status }}</div>
                </div>
                <div class="friend-check">
                  <svg v-if="isSelected(friend.email)" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="close">СКАСУВАТИ</button>
          <button class="btn-submit" @click="submit" :disabled="submitting">
            {{ submitting ? 'СТВОРЕННЯ...' : 'СТВОРИТИ ЧАТ' }}
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.75);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}

.modal {
  background: var(--panel);
  border: 2px solid var(--border);
  border-top: 3px solid var(--yellow);
  width: 520px;
  max-width: 95vw;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0,0,0,0.5), 0 0 0 1px rgba(245,197,24,0.05);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  border-bottom: 2px solid var(--border);
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: var(--font-display);
  font-size: 16px;
  letter-spacing: 3px;
  color: var(--yellow);
}

.modal-close {
  background: none;
  border: none;
  color: var(--gray);
  font-size: 18px;
  cursor: pointer;
  transition: color 0.2s;
  padding: 4px 8px;
}
.modal-close:hover { color: var(--red); }

.modal-body {
  padding: 20px 24px;
  overflow-y: auto;
  flex: 1;
}

.modal-error {
  color: var(--red);
  font-size: 13px;
  margin-bottom: 12px;
  letter-spacing: 0.5px;
  padding: 10px 14px;
  border: 1px solid rgba(192, 57, 43, 0.3);
  background: rgba(192, 57, 43, 0.06);
  line-height: 1.5;
}

.form-group {
  margin-bottom: 18px;
}

.form-label {
  display: block;
  font-size: 11px;
  font-family: var(--font-display);
  letter-spacing: 2px;
  color: var(--gray);
  margin-bottom: 8px;
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  background: var(--panel-light);
  border: 2px solid var(--border);
  color: var(--white);
  font-size: 14px;
  font-family: var(--font-body);
  transition: border-color 0.2s;
}
.form-input:focus {
  border-color: var(--yellow-dim);
  outline: none;
}
.form-input::placeholder {
  color: var(--gray);
}

.selected-members {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.member-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: linear-gradient(135deg, rgba(41, 128, 185, 0.15), rgba(41, 128, 185, 0.08));
  border: 1px solid rgba(41, 128, 185, 0.35);
  color: #5dade2;
  font-size: 12px;
  letter-spacing: 0.5px;
}

.chip-remove {
  background: none;
  border: none;
  color: rgba(93, 173, 226, 0.6);
  font-size: 11px;
  cursor: pointer;
  padding: 0;
  line-height: 1;
  transition: color 0.15s;
}
.chip-remove:hover { color: var(--red); }

.friends-list {
  max-height: 280px;
  overflow-y: auto;
  border: 2px solid var(--border);
  background: var(--panel-light);
}

.friends-empty {
  padding: 30px 20px;
  text-align: center;
  color: var(--gray);
  font-size: 13px;
  letter-spacing: 1px;
}

.friend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  cursor: pointer;
  border-bottom: 1px solid var(--border);
  transition: all 0.15s;
  border-left: 3px solid transparent;
}
.friend-item:last-child { border-bottom: none; }
.friend-item:hover {
  background: rgba(245, 197, 24, 0.03);
  border-left-color: rgba(245, 197, 24, 0.3);
}
.friend-item.selected {
  background: rgba(41, 128, 185, 0.06);
  border-left-color: #5dade2;
}

.friend-avatar {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
}

.friend-avatar-img {
  width: 36px;
  height: 36px;
  object-fit: cover;
  border: 2px solid var(--border);
}

.friend-avatar-letter {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: var(--yellow-glow);
  border: 2px solid var(--border);
  font-family: var(--font-display);
  font-size: 15px;
  color: var(--yellow);
}

.friend-info {
  flex: 1;
  min-width: 0;
}

.friend-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--white);
  letter-spacing: 0.5px;
}

.friend-status {
  font-size: 10px;
  letter-spacing: 1px;
  color: var(--gray);
  text-transform: uppercase;
}
.friend-status.online { color: #27ae60; }
.friend-status.away { color: var(--yellow-dim); }

.friend-check {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #5dade2;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 2px solid var(--border);
}

.btn-cancel {
  background: none;
  border: 2px solid var(--border);
  color: var(--gray);
  padding: 10px 24px;
  font-family: var(--font-display);
  font-size: 12px;
  letter-spacing: 2px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-cancel:hover {
  border-color: var(--gray);
  color: var(--white);
}

.btn-submit {
  background: var(--yellow);
  border: none;
  color: var(--black);
  padding: 10px 28px;
  font-family: var(--font-display);
  font-size: 12px;
  letter-spacing: 2px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-submit:hover {
  background: var(--yellow-dim);
  box-shadow: 0 0 16px rgba(245, 197, 24, 0.3);
}
.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>


