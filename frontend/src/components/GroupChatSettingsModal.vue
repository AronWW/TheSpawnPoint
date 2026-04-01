<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useChatStore } from '../stores/chat'
import { useFriendStore } from '../stores/friends'
import { useAuthStore } from '../stores/auth'
import { useToast } from '../composables/useToast'
import { API_BASE_URL } from '../config'
import type { ChatItem, ChatParticipant } from '../types'

const props = defineProps<{ chat: ChatItem }>()
const emit = defineEmits<{ (e: 'close'): void }>()

const chatStore = useChatStore()
const friendStore = useFriendStore()
const auth = useAuthStore()
const toast = useToast()

const PUBLIC_BASE_URL = API_BASE_URL.replace(/\/api\/?$/, '')

const editingTitle = ref(false)
const titleInput = ref(props.chat.title || '')
const savingTitle = ref(false)
const uploadingAvatar = ref(false)
const avatarError = ref('')
const addMemberSearch = ref('')
const showAddMember = ref(false)
const actionLoading = ref<number | null>(null)

const myEmail = computed(() => auth.user?.email ?? '')

const myParticipant = computed(() =>
  props.chat.participants?.find(p => p.email === myEmail.value) ?? null
)

const myRole = computed(() => myParticipant.value?.role ?? 'MEMBER')
const isOwner = computed(() => myRole.value === 'OWNER')
const isAdmin = computed(() => myRole.value === 'ADMIN')
const isOwnerOrAdmin = computed(() => isOwner.value || isAdmin.value)

const sortedParticipants = computed(() => {
  if (!props.chat.participants) return []
  const roleOrder: Record<string, number> = { OWNER: 0, ADMIN: 1, MEMBER: 2 }
  return [...props.chat.participants].sort((a, b) =>
    (roleOrder[a.role] ?? 2) - (roleOrder[b.role] ?? 2)
  )
})

const groupAvatarUrl = computed(() => {
  const url = props.chat.groupAvatarUrl
  if (!url) return null
  if (url.startsWith('http')) return url
  return PUBLIC_BASE_URL + url
})

function resolveAvatar(url: string | null): string {
  if (!url) return PUBLIC_BASE_URL + '/avatars/default/avatar-1.png'
  if (url.startsWith('http')) return url
  return PUBLIC_BASE_URL + url
}

const filteredFriendsToAdd = computed(() => {
  const q = addMemberSearch.value.toLowerCase().trim()
  const existingEmails = new Set(props.chat.participants?.map(p => p.email) ?? [])
  let list = friendStore.friends.filter(f => !existingEmails.has(f.email))
  if (q) {
    list = list.filter(f => f.displayName.toLowerCase().includes(q) || f.email.toLowerCase().includes(q))
  }
  return list
})

onMounted(async () => {
  if (friendStore.friends.length === 0) {
    await friendStore.fetchFriends()
  }
})

async function saveTitle() {
  if (!titleInput.value.trim()) return
  savingTitle.value = true
  try {
    await chatStore.renameGroupChat(props.chat.id, titleInput.value.trim())
    editingTitle.value = false
    toast.show('Назву змінено', 'success')
  } catch (e: any) {
    toast.show(e.response?.data?.message || 'Помилка', 'error')
  } finally {
    savingTitle.value = false
  }
}

async function onAvatarSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    avatarError.value = 'Файл не повинен перевищувати 2 МБ'
    return
  }
  avatarError.value = ''
  uploadingAvatar.value = true
  try {
    await chatStore.uploadGroupAvatar(props.chat.id, file)
    toast.show('Аватар групи оновлено', 'success')
  } catch {
    avatarError.value = 'Не вдалося завантажити аватар'
  } finally {
    uploadingAvatar.value = false
    input.value = ''
  }
}

async function addMember(email: string) {
  actionLoading.value = -1
  try {
    await chatStore.addGroupMember(props.chat.id, email)
    toast.show('Учасника додано', 'success')
    addMemberSearch.value = ''
  } catch (e: any) {
    toast.show(e.response?.data?.message || 'Помилка', 'error')
  } finally {
    actionLoading.value = null
  }
}

async function removeMember(userId: number) {
  actionLoading.value = userId
  try {
    await chatStore.removeGroupMember(props.chat.id, userId)
    toast.show('Учасника видалено', 'success')
  } catch (e: any) {
    toast.show(e.response?.data?.message || 'Помилка', 'error')
  } finally {
    actionLoading.value = null
  }
}

async function toggleAdmin(participant: ChatParticipant) {
  actionLoading.value = participant.userId
  try {
    if (participant.role === 'ADMIN') {
      await chatStore.revokeAdmin(props.chat.id, participant.userId)
      toast.show('Права адміна знято', 'success')
    } else {
      await chatStore.grantAdmin(props.chat.id, participant.userId)
      toast.show('Надано права адміна', 'success')
    }
  } catch (e: any) {
    toast.show(e.response?.data?.message || 'Помилка', 'error')
  } finally {
    actionLoading.value = null
  }
}

function roleLabel(role: string): string {
  if (role === 'OWNER') return 'Власник'
  if (role === 'ADMIN') return 'Адмін'
  return 'Учасник'
}

function roleBadgeClass(role: string): string {
  if (role === 'OWNER') return 'role-owner'
  if (role === 'ADMIN') return 'role-admin'
  return 'role-member'
}

function canRemove(participant: ChatParticipant): boolean {
  if (participant.role === 'OWNER') return false
  if (isOwner.value) return true
  if (isAdmin.value && participant.role === 'MEMBER') return true
  return false
}

function canToggleAdmin(participant: ChatParticipant): boolean {
  if (!isOwner.value) return false
  if (participant.role === 'OWNER') return false
  return true
}
</script>

<template>
  <Teleport to="body">
    <div class="settings-overlay" @click.self="emit('close')">
      <div class="settings-modal">
        <div class="settings-header">
          <div class="settings-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="3"/>
              <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
            </svg>
            НАЛАШТУВАННЯ ГРУПИ
          </div>
          <button class="settings-close" @click="emit('close')">✕</button>
        </div>

        <div class="settings-body">
          <div class="settings-section">
            <div class="section-label">АВАТАР ГРУПИ</div>
            <div class="avatar-section">
              <div class="group-avatar-preview">
                <img v-if="groupAvatarUrl" :src="groupAvatarUrl" alt="" class="group-avatar-img" />
                <span v-else class="group-avatar-letter">{{ (chat.title || 'G').charAt(0).toUpperCase() }}</span>
              </div>
              <div class="avatar-controls" v-if="isOwnerOrAdmin">
                <label class="avatar-upload-btn">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                    <polyline points="17 8 12 3 7 8"/>
                    <line x1="12" y1="3" x2="12" y2="15"/>
                  </svg>
                  {{ uploadingAvatar ? 'ЗАВАНТАЖЕННЯ...' : 'ЗМІНИТИ АВАТАР' }}
                  <input type="file" accept="image/jpeg,image/png,image/webp,image/gif" @change="onAvatarSelect" hidden :disabled="uploadingAvatar" />
                </label>
                <div class="avatar-hint">JPEG, PNG, WebP, GIF · до 2 МБ</div>
                <div v-if="avatarError" class="avatar-error">{{ avatarError }}</div>
              </div>
            </div>
          </div>

          <div class="settings-section">
            <div class="section-label">НАЗВА ЧАТУ</div>
            <div v-if="!editingTitle" class="title-display">
              <span class="title-text">{{ chat.title || 'Без назви' }}</span>
              <button v-if="isOwnerOrAdmin" class="title-edit-btn" @click="editingTitle = true; titleInput = chat.title || ''">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                </svg>
              </button>
            </div>
            <div v-else class="title-edit-row">
              <input
                v-model="titleInput"
                class="title-input"
                maxlength="100"
                placeholder="Назва чату..."
                @keydown.enter.prevent="saveTitle"
              />
              <button class="title-save-btn" @click="saveTitle" :disabled="savingTitle || !titleInput.trim()">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
              </button>
              <button class="title-cancel-btn" @click="editingTitle = false">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
          </div>

          <div class="settings-section">
            <div class="section-label">
              УЧАСНИКИ ({{ chat.participants?.length || 0 }})
              <button v-if="isOwnerOrAdmin" class="add-member-toggle" @click="showAddMember = !showAddMember">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                  <circle cx="8.5" cy="7" r="4"/>
                  <line x1="20" y1="8" x2="20" y2="14"/>
                  <line x1="17" y1="11" x2="23" y2="11"/>
                </svg>
              </button>
            </div>

            <div v-if="showAddMember && isOwnerOrAdmin" class="add-member-section">
              <input
                v-model="addMemberSearch"
                class="add-member-input"
                placeholder="Пошук друзів для додавання..."
              />
              <div class="add-member-list" v-if="addMemberSearch.trim()">
                <div v-if="filteredFriendsToAdd.length === 0" class="add-member-empty">Нікого не знайдено</div>
                <div
                  v-for="friend in filteredFriendsToAdd"
                  :key="friend.email"
                  class="add-member-item"
                  @click="addMember(friend.email)"
                >
                  <div class="member-avatar">
                    <img v-if="friend.avatarUrl" :src="resolveAvatar(friend.avatarUrl)" alt="" class="member-avatar-img" />
                    <span v-else class="member-avatar-letter">{{ friend.displayName.charAt(0).toUpperCase() }}</span>
                  </div>
                  <span class="member-name">{{ friend.displayName }}</span>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="add-icon"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                </div>
              </div>
            </div>

            <div class="members-list">
              <div
                v-for="p in sortedParticipants"
                :key="p.userId"
                class="member-row"
                :class="{ 'is-me': p.email === myEmail }"
              >
                <div class="member-avatar">
                  <img v-if="p.avatarUrl" :src="resolveAvatar(p.avatarUrl)" alt="" class="member-avatar-img" />
                  <span v-else class="member-avatar-letter">{{ p.displayName.charAt(0).toUpperCase() }}</span>
                </div>
                <div class="member-info">
                  <div class="member-name-row">
                    <span class="member-name">{{ p.displayName }}</span>
                    <span v-if="p.email === myEmail" class="you-label">(ви)</span>
                  </div>
                  <span class="role-badge" :class="roleBadgeClass(p.role)">{{ roleLabel(p.role) }}</span>
                </div>
                <div class="member-actions" v-if="p.email !== myEmail">
                  <button
                    v-if="canToggleAdmin(p)"
                    class="member-action-btn"
                    :class="p.role === 'ADMIN' ? 'revoke-btn' : 'grant-btn'"
                    @click="toggleAdmin(p)"
                    :disabled="actionLoading === p.userId"
                    :title="p.role === 'ADMIN' ? 'Зняти адміна' : 'Зробити адміном'"
                  >
                    <svg v-if="p.role === 'ADMIN'" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                      <line x1="15" y1="9" x2="9" y2="15"/>
                      <line x1="9" y1="9" x2="15" y2="15"/>
                    </svg>
                    <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                      <polyline points="9 12 11 14 15 10"/>
                    </svg>
                  </button>
                  <button
                    v-if="canRemove(p)"
                    class="member-action-btn remove-btn"
                    @click="removeMember(p.userId)"
                    :disabled="actionLoading === p.userId"
                    title="Видалити з чату"
                  >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                      <circle cx="8.5" cy="7" r="4"/>
                      <line x1="18" y1="11" x2="23" y2="11"/>
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.settings-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.82);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(6px);
  animation: overlayFadeIn 0.2s ease-out;
}
@keyframes overlayFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.settings-modal {
  background: var(--panel);
  border: 2px solid var(--border);
  border-top: 3px solid var(--yellow);
  width: 500px;
  max-width: 95vw;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow:
    0 24px 80px rgba(0, 0, 0, 0.6),
    0 0 0 1px rgba(245, 197, 24, 0.06),
    inset 0 1px 0 rgba(245, 197, 24, 0.04);
  animation: modalSlideIn 0.22s ease-out;
  position: relative;
}
@keyframes modalSlideIn {
  from { opacity: 0; transform: translateY(-12px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.settings-modal::before {
  content: '';
  position: absolute;
  inset: 3px;
  border: 1px solid rgba(245, 197, 24, 0.04);
  pointer-events: none;
  z-index: 0;
}
.settings-modal::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: linear-gradient(180deg, rgba(245, 197, 24, 0.03), transparent);
  pointer-events: none;
  z-index: 0;
}

.settings-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  border-bottom: 2px solid var(--border);
  position: relative;
  z-index: 1;
}
.settings-header::after {
  content: '';
  position: absolute;
  bottom: -3px;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, var(--yellow-dim), transparent);
  opacity: 0.4;
}

.settings-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: var(--font-display);
  font-size: 16px;
  letter-spacing: 3px;
  color: var(--yellow);
  text-shadow: 0 0 12px rgba(245, 197, 24, 0.2);
}

.settings-close {
  background: none;
  border: 2px solid var(--border);
  color: var(--gray);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  padding: 4px 10px;
  font-family: var(--font-display);
  letter-spacing: 1px;
  line-height: 1;
}
.settings-close:hover {
  color: var(--red);
  border-color: rgba(192, 57, 43, 0.5);
  background: rgba(192, 57, 43, 0.06);
  box-shadow: 0 0 8px rgba(192, 57, 43, 0.15);
}

.settings-body {
  padding: 0;
  overflow-y: auto;
  flex: 1;
  position: relative;
  z-index: 1;
}
.settings-body::-webkit-scrollbar {
  width: 4px;
}
.settings-body::-webkit-scrollbar-track {
  background: transparent;
}
.settings-body::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 2px;
}
.settings-body::-webkit-scrollbar-thumb:hover {
  background: var(--yellow-dim);
}

.settings-section {
  padding: 22px 24px;
  border-bottom: 1px solid var(--border);
  position: relative;
}
.settings-section:last-child { border-bottom: none; }
.settings-section::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: transparent;
  transition: background 0.3s;
}
.settings-section:hover::before {
  background: linear-gradient(180deg, rgba(245, 197, 24, 0.15), transparent);
}

.section-label {
  font-size: 10px;
  font-family: var(--font-display);
  letter-spacing: 3px;
  color: var(--gray);
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  position: relative;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.group-avatar-preview {
  width: 72px;
  height: 72px;
  flex-shrink: 0;
  border: 2px solid var(--border);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(41, 128, 185, 0.15), rgba(41, 128, 185, 0.06));
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  position: relative;
}
.group-avatar-preview::after {
  content: '';
  position: absolute;
  inset: 2px;
  border: 1px solid rgba(41, 128, 185, 0.1);
  pointer-events: none;
}
.group-avatar-preview:hover {
  border-color: rgba(41, 128, 185, 0.5);
  box-shadow: 0 4px 16px rgba(41, 128, 185, 0.15);
}

.group-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.group-avatar-letter {
  font-family: var(--font-display);
  font-size: 30px;
  color: #5dade2;
  letter-spacing: 1px;
  text-shadow: 0 0 10px rgba(41, 128, 185, 0.3);
}

.avatar-controls {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.avatar-upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 18px;
  font-family: var(--font-display);
  font-size: 10px;
  letter-spacing: 2px;
  background: transparent;
  border: 2px solid var(--border);
  color: var(--gray-light);
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  overflow: hidden;
}
.avatar-upload-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(245, 197, 24, 0.04), transparent);
  opacity: 0;
  transition: opacity 0.2s;
}
.avatar-upload-btn:hover::before {
  opacity: 1;
}
.avatar-upload-btn:hover {
  border-color: var(--yellow-dim);
  color: var(--yellow);
  box-shadow: 0 0 10px rgba(245, 197, 24, 0.08);
}

.avatar-hint {
  font-size: 9px;
  color: var(--gray);
  letter-spacing: 1px;
}

.avatar-error {
  font-size: 11px;
  color: var(--red);
  letter-spacing: 0.5px;
  padding: 3px 0;
  animation: errorShake 0.3s ease-out;
}
@keyframes errorShake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-3px); }
  75% { transform: translateX(3px); }
}

.title-display {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: var(--panel-light);
  border: 1px solid var(--border);
  border-left: 3px solid var(--yellow-dim);
}

.title-text {
  font-size: 15px;
  color: var(--white);
  font-weight: 600;
  letter-spacing: 0.5px;
  flex: 1;
}

.title-edit-btn {
  background: none;
  border: 2px solid var(--border);
  color: var(--gray);
  padding: 5px 7px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
}
.title-edit-btn:hover {
  border-color: var(--yellow-dim);
  color: var(--yellow);
  box-shadow: 0 0 6px rgba(245, 197, 24, 0.1);
}

.title-edit-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.title-input {
  flex: 1;
  padding: 9px 14px;
  background: var(--dark);
  border: 2px solid var(--border);
  border-left: 3px solid transparent;
  color: var(--white);
  font-size: 14px;
  font-family: var(--font-body);
  transition: all 0.2s;
  letter-spacing: 0.3px;
}
.title-input:focus {
  border-color: var(--yellow-dim);
  border-left-color: var(--yellow);
  outline: none;
  box-shadow: 0 0 8px rgba(245, 197, 24, 0.06);
}
.title-input::placeholder { color: var(--gray); font-style: italic; }

.title-save-btn,
.title-cancel-btn {
  background: none;
  border: 2px solid var(--border);
  padding: 7px 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: all 0.2s;
}
.title-save-btn {
  color: #27ae60;
}
.title-save-btn:hover {
  border-color: #27ae60;
  background: rgba(39, 174, 96, 0.1);
  box-shadow: 0 0 8px rgba(39, 174, 96, 0.1);
}
.title-save-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.title-cancel-btn {
  color: var(--gray);
}
.title-cancel-btn:hover {
  border-color: var(--red);
  color: var(--red);
  background: rgba(192, 57, 43, 0.06);
}

.add-member-toggle {
  background: none;
  border: 2px solid var(--border);
  color: var(--gray);
  padding: 4px 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: all 0.2s;
}
.add-member-toggle:hover {
  border-color: var(--yellow-dim);
  color: var(--yellow);
  box-shadow: 0 0 8px rgba(245, 197, 24, 0.08);
}

.add-member-section {
  margin-bottom: 16px;
  animation: sectionSlideIn 0.2s ease-out;
}
@keyframes sectionSlideIn {
  from { opacity: 0; transform: translateY(-6px); }
  to { opacity: 1; transform: translateY(0); }
}

.add-member-input {
  width: 100%;
  padding: 9px 14px;
  background: var(--dark);
  border: 2px solid var(--border);
  border-left: 3px solid transparent;
  color: var(--white);
  font-size: 13px;
  font-family: var(--font-body);
  margin-bottom: 8px;
  transition: all 0.2s;
  letter-spacing: 0.3px;
}
.add-member-input:focus {
  border-color: var(--yellow-dim);
  border-left-color: var(--yellow);
  outline: none;
  box-shadow: 0 0 8px rgba(245, 197, 24, 0.06);
}
.add-member-input::placeholder { color: var(--gray); font-style: italic; }

.add-member-list {
  border: 2px solid var(--border);
  background: var(--dark);
  max-height: 180px;
  overflow-y: auto;
}

.add-member-empty {
  padding: 18px;
  text-align: center;
  font-size: 12px;
  color: var(--gray);
  letter-spacing: 1px;
  font-style: italic;
}

.add-member-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  cursor: pointer;
  border-bottom: 1px solid var(--border);
  border-left: 3px solid transparent;
  transition: all 0.15s;
}
.add-member-item:last-child { border-bottom: none; }
.add-member-item:hover {
  background: linear-gradient(90deg, rgba(245, 197, 24, 0.04), transparent);
  border-left-color: rgba(39, 174, 96, 0.5);
}
.add-member-item .add-icon {
  margin-left: auto;
  color: #27ae60;
  flex-shrink: 0;
  transition: all 0.15s;
}
.add-member-item:hover .add-icon {
  color: #2ecc71;
  filter: drop-shadow(0 0 4px rgba(39, 174, 96, 0.3));
}

.members-list {
  display: flex;
  flex-direction: column;
}

.member-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  border-left: 3px solid transparent;
  transition: all 0.2s;
}
.member-row:last-child { border-bottom: none; }
.member-row:hover {
  border-left-color: rgba(245, 197, 24, 0.2);
}
.member-row.is-me {
  background: linear-gradient(90deg, rgba(245, 197, 24, 0.03), transparent);
  margin: 0 -24px;
  padding: 12px 24px;
  border-left-color: var(--yellow-dim);
}

.member-avatar {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
}

.member-avatar-img {
  width: 38px;
  height: 38px;
  object-fit: cover;
  border: 2px solid var(--border);
  transition: border-color 0.2s;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}
.member-row:hover .member-avatar-img {
  border-color: var(--yellow-dim);
}

.member-avatar-letter {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  background: linear-gradient(135deg, var(--yellow-glow), rgba(245, 197, 24, 0.06));
  border: 2px solid var(--border);
  font-family: var(--font-display);
  font-size: 16px;
  color: var(--yellow);
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}
.member-row:hover .member-avatar-letter {
  border-color: var(--yellow-dim);
  box-shadow: 0 2px 8px rgba(245, 197, 24, 0.1);
}

.member-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.member-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.member-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--white);
  letter-spacing: 0.3px;
  transition: color 0.2s;
}
.member-row:hover .member-name {
  color: var(--yellow);
}

.you-label {
  font-size: 10px;
  color: var(--yellow-dim);
  font-style: italic;
  letter-spacing: 0.5px;
}

.role-badge {
  font-size: 9px;
  font-family: var(--font-display);
  letter-spacing: 1.5px;
  padding: 2px 8px;
  display: inline-block;
  width: fit-content;
}

.role-owner {
  background: linear-gradient(135deg, rgba(245, 197, 24, 0.18), rgba(245, 197, 24, 0.08));
  border: 1px solid rgba(245, 197, 24, 0.4);
  color: var(--yellow);
  box-shadow: 0 0 6px rgba(245, 197, 24, 0.08);
}

.role-admin {
  background: linear-gradient(135deg, rgba(41, 128, 185, 0.18), rgba(41, 128, 185, 0.08));
  border: 1px solid rgba(41, 128, 185, 0.4);
  color: #5dade2;
  box-shadow: 0 0 6px rgba(41, 128, 185, 0.08);
}

.role-member {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02));
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: var(--gray);
}

.member-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.member-action-btn {
  background: none;
  border: 2px solid var(--border);
  padding: 5px 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: all 0.2s;
}
.member-action-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.grant-btn {
  color: #5dade2;
}
.grant-btn:hover:not(:disabled) {
  border-color: rgba(41, 128, 185, 0.5);
  background: rgba(41, 128, 185, 0.1);
  box-shadow: 0 0 8px rgba(41, 128, 185, 0.1);
}

.revoke-btn {
  color: var(--yellow-dim);
}
.revoke-btn:hover:not(:disabled) {
  border-color: var(--yellow-dim);
  background: rgba(245, 197, 24, 0.08);
  box-shadow: 0 0 8px rgba(245, 197, 24, 0.08);
}

.remove-btn {
  color: var(--gray);
}
.remove-btn:hover:not(:disabled) {
  border-color: rgba(192, 57, 43, 0.5);
  color: var(--red);
  background: rgba(192, 57, 43, 0.08);
  box-shadow: 0 0 8px rgba(192, 57, 43, 0.1);
}
</style>

