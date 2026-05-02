<script setup lang="ts">
import { ref, computed } from 'vue'
import { useChatStore } from '../stores/chat'
import { useToast } from '../composables/useToast'
import { timeAgo } from '../utils/helpers'
import { API_BASE_URL } from '../config'
import type { ChatItem } from '../types'

const chatStore = useChatStore()
const toast = useToast()

const emit = defineEmits<{
  (e: 'select', chat: ChatItem): void
}>()

const props = defineProps<{ search: string; collapsed?: boolean }>()

const PUBLIC_BASE_URL = API_BASE_URL.replace(/\/api\/?$/, '')
const DEFAULT_AVATAR_URL = `${PUBLIC_BASE_URL}/avatars/default/avatar-1.png`

function resolveAvatar(url: string | null, fallback = ''): string {
  const value = url?.trim()
  if (!value) return fallback
  if (/^(https?:)?\/\//.test(value) || value.startsWith('data:') || value.startsWith('blob:')) return value
  return PUBLIC_BASE_URL + (value.startsWith('/') ? value : `/${value}`)
}

function onAvatarError(event: Event) {
  const target = event.target as HTMLImageElement | null
  if (!target || target.src === DEFAULT_AVATAR_URL) return
  target.src = DEFAULT_AVATAR_URL
}

function chatAvatarUrl(chat: ChatItem): string | null {
  if (chat.isGroup) {
    if (chat.groupAvatarUrl) return resolveAvatar(chat.groupAvatarUrl)
    return null
  }
  return resolveAvatar(chat.partnerAvatarUrl, DEFAULT_AVATAR_URL)
}

const ctxMenu = ref<{ show: boolean; x: number; y: number; chat: ChatItem | null }>({
  show: false, x: 0, y: 0, chat: null
})

const filteredActiveChats = computed(() => {
  const q = props.search.toLowerCase().trim()
  const list = chatStore.activeChats
  if (!q) return list
  return list.filter((c) => chatDisplayName(c).toLowerCase().includes(q))
})

const filteredArchivedChats = computed(() => {
  const q = props.search.toLowerCase().trim()
  const list = chatStore.archivedChats
  if (!q) return list
  return list.filter((c) => chatDisplayName(c).toLowerCase().includes(q))
})

function chatDisplayName(chat: ChatItem): string {
  if (chat.chatType === 'GAME') return chat.title || 'Ігровий чат'
  if (chat.isGroup) return chat.title || 'Груповий чат'
  return chat.partnerDisplayName || 'Чат'
}

function chatAvatarLetter(chat: ChatItem): string {
  return chatDisplayName(chat).charAt(0).toUpperCase()
}

function statusDotClass(chat: ChatItem): string {
  if (chat.chatType === 'GAME') return 'is-game'
  if (chat.isGroup) return 'is-group'
  const status = chat.partnerStatus
  if (status === 'ONLINE') return 'online'
  if (status === 'AWAY') return 'away'
  return 'offline'
}

function isActive(chat: ChatItem) {
  return chatStore.activeChat?.id === chat.id
}

function selectChat(chat: ChatItem) {
  emit('select', chat)
}

function truncate(text: string | null, max = 40) {
  if (!text) return ''
  return text.length > max ? text.slice(0, max) + '…' : text
}

function openCtxMenu(event: MouseEvent, chat: ChatItem) {
  event.preventDefault()
  ctxMenu.value = { show: true, x: event.clientX, y: event.clientY, chat }
}

function closeCtxMenu() {
  ctxMenu.value = { show: false, x: 0, y: 0, chat: null }
}

function onPinChat() {
  if (!ctxMenu.value.chat) return
  if (ctxMenu.value.chat.pinned) {
    chatStore.unpinChatAction(ctxMenu.value.chat.id)
  } else {
    chatStore.pinChatAction(ctxMenu.value.chat.id)
  }
  closeCtxMenu()
}

function onArchiveChat() {
  if (!ctxMenu.value.chat) return
  if (ctxMenu.value.chat.archived) {
    chatStore.unarchiveChat(ctxMenu.value.chat.id)
  } else {
    chatStore.archiveChat(ctxMenu.value.chat.id)
  }
  closeCtxMenu()
}

function onDeleteChat() {
  if (!ctxMenu.value.chat) return
  const chat = ctxMenu.value.chat
  closeCtxMenu()

  if (chat.isGroup) {
    confirmModal.value = {
      show: true,
      chatId: chat.id,
      title: chat.chatType === 'GAME' ? 'Вийти з ігрового чату?' : 'Вийти з групового чату?',
      message: chat.chatType === 'GAME'
        ? 'Ви впевнені, що хочете вийти з цього ігрового чату? Ви більше не зможете бачити повідомлення цієї групи.'
        : 'Ви впевнені, що хочете вийти з цього чату? Ви більше не зможете бачити повідомлення цієї групи.',
      isGroup: true,
    }
  } else {
    confirmModal.value = {
      show: true,
      chatId: chat.id,
      title: 'Видалити чат?',
      message: 'Ви впевнені, що хочете видалити цей чат? Цю дію не можна скасувати.',
      isGroup: false,
    }
  }
}

async function onConfirmDelete() {
  const { chatId } = confirmModal.value
  confirmModal.value.show = false
  try {
    await chatStore.deleteChatAction(chatId)
    if (confirmModal.value.isGroup) {
      toast.show('Ви покинули чат', 'success')
    }
  } catch (e: any) {
    const msg = e?.response?.data?.message || 'Не вдалося виконати дію'
    toast.show(msg, 'error', 5000)
  }
}

function onCancelDelete() {
  confirmModal.value.show = false
}

const confirmModal = ref<{
  show: boolean
  chatId: number
  title: string
  message: string
  isGroup: boolean
}>({
  show: false,
  chatId: 0,
  title: '',
  message: '',
  isGroup: false,
})

function onWindowClick() {
  closeCtxMenu()
}
</script>

<template>
  <div class="chat-sidebar" @click="onWindowClick">
    <div class="chat-sidebar-header">
      <span class="chat-sidebar-title">ПОВІДОМЛЕННЯ</span>
      <span v-if="chatStore.totalUnread" class="chat-sidebar-badge">{{ chatStore.totalUnread }}</span>
    </div>

    <div v-if="chatStore.loading" class="chat-sidebar-loading">
      Завантаження...
    </div>

    <template v-else>
      <div v-if="filteredActiveChats.length === 0 && filteredArchivedChats.length === 0" class="chat-sidebar-empty">
        Чатів поки немає
      </div>

      <div v-if="filteredActiveChats.some(c => c.pinned)" class="chat-section-label">ЗАКРІПЛЕНІ</div>

      <div
        v-for="chat in filteredActiveChats.filter(c => c.pinned)"
        :key="'pin-' + chat.id"
        class="chat-sidebar-item pinned-item"
        :class="{ active: isActive(chat), unread: chat.unreadCount > 0 }"
        @click="selectChat(chat)"
        @contextmenu="openCtxMenu($event, chat)"
      >
        <div class="chat-sidebar-avatar">
          <img v-if="chatAvatarUrl(chat)" :src="chatAvatarUrl(chat)!" alt="" class="chat-avatar-img" @error="onAvatarError" />
          <span v-else class="chat-avatar-letter" :class="{ group: chat.chatType === 'GROUP', game: chat.chatType === 'GAME' }">{{ chatAvatarLetter(chat) }}</span>
          <span class="chat-status-dot" :class="statusDotClass(chat)">
            <svg v-if="chat.chatType === 'GAME'" width="8" height="8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M6 11h4M8 9v4"/><circle cx="17" cy="12" r="1"/><circle cx="20" cy="9" r="1"/><rect x="2" y="6" width="20" height="12" rx="3"/></svg>
            <svg v-else-if="chat.isGroup" width="8" height="8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          </span>
        </div>
        <div class="chat-sidebar-info">
          <div class="chat-sidebar-name">
            <span class="pin-icon">●</span>
            <span v-if="chat.chatType === 'GAME'" class="game-tag">ІГРОВА</span>
            <span v-else-if="chat.chatType === 'GROUP'" class="group-tag">ГРУПА</span>
            {{ chatDisplayName(chat) }}
          </div>
          <div class="chat-sidebar-last" :class="{ 'sidebar-typing': chatStore.isPartnerTyping(chat.id) }">
            <template v-if="chatStore.isPartnerTyping(chat.id)">
              <span v-if="chat.isGroup" class="sidebar-typing-content">
                <span class="sidebar-typing-name">{{ chatStore.typingDisplayName(chat.id) }}</span> пише<span class="sidebar-typing-dots"><span class="s-dot"></span><span class="s-dot"></span><span class="s-dot"></span></span>
              </span>
              <span v-else class="sidebar-typing-content">
                друкує<span class="sidebar-typing-dots"><span class="s-dot"></span><span class="s-dot"></span><span class="s-dot"></span></span>
              </span>
            </template>
            <template v-else>{{ truncate(chat.lastMessage) }}</template>
          </div>
        </div>
        <div class="chat-sidebar-meta">
          <div class="chat-sidebar-time" v-if="chat.lastMessageAt">{{ timeAgo(chat.lastMessageAt) }}</div>
          <div v-if="chat.unreadCount > 0" class="chat-unread-badge">{{ chat.unreadCount }}</div>
        </div>
      </div>

      <div v-if="filteredActiveChats.some(c => c.pinned) && filteredActiveChats.some(c => !c.pinned)" class="chat-section-label">ВСІ ЧАТИ</div>

      <div
        v-for="chat in filteredActiveChats.filter(c => !c.pinned)"
        :key="chat.id"
        class="chat-sidebar-item"
        :class="{ active: isActive(chat), unread: chat.unreadCount > 0 }"
        @click="selectChat(chat)"
        @contextmenu="openCtxMenu($event, chat)"
      >
        <div class="chat-sidebar-avatar">
          <img v-if="chatAvatarUrl(chat)" :src="chatAvatarUrl(chat)!" alt="" class="chat-avatar-img" @error="onAvatarError" />
          <span v-else class="chat-avatar-letter" :class="{ group: chat.chatType === 'GROUP', game: chat.chatType === 'GAME' }">{{ chatAvatarLetter(chat) }}</span>
          <span class="chat-status-dot" :class="statusDotClass(chat)">
            <svg v-if="chat.chatType === 'GAME'" width="8" height="8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M6 11h4M8 9v4"/><circle cx="17" cy="12" r="1"/><circle cx="20" cy="9" r="1"/><rect x="2" y="6" width="20" height="12" rx="3"/></svg>
            <svg v-else-if="chat.isGroup" width="8" height="8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          </span>
        </div>
        <div class="chat-sidebar-info">
          <div class="chat-sidebar-name">
            <span v-if="chat.chatType === 'GAME'" class="game-tag">ІГРОВА</span>
            <span v-else-if="chat.chatType === 'GROUP'" class="group-tag">ГРУПА</span>
            {{ chatDisplayName(chat) }}
          </div>
          <div class="chat-sidebar-last" :class="{ 'sidebar-typing': chatStore.isPartnerTyping(chat.id) }">
            <template v-if="chatStore.isPartnerTyping(chat.id)">
              <span v-if="chat.isGroup" class="sidebar-typing-content">
                <span class="sidebar-typing-name">{{ chatStore.typingDisplayName(chat.id) }}</span> пише<span class="sidebar-typing-dots"><span class="s-dot"></span><span class="s-dot"></span><span class="s-dot"></span></span>
              </span>
              <span v-else class="sidebar-typing-content">
                друкує<span class="sidebar-typing-dots"><span class="s-dot"></span><span class="s-dot"></span><span class="s-dot"></span></span>
              </span>
            </template>
            <template v-else>{{ truncate(chat.lastMessage) }}</template>
          </div>
        </div>
        <div class="chat-sidebar-meta">
          <div class="chat-sidebar-time" v-if="chat.lastMessageAt">{{ timeAgo(chat.lastMessageAt) }}</div>
          <div v-if="chat.unreadCount > 0" class="chat-unread-badge">{{ chat.unreadCount }}</div>
        </div>
      </div>

      <div v-if="filteredArchivedChats.length > 0" class="chat-archive-toggle" @click="chatStore.showArchived = !chatStore.showArchived">
        <span>АРХІВ ({{ filteredArchivedChats.length }})</span>
        <svg :style="{ transform: chatStore.showArchived ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.2s' }" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
      </div>

      <template v-if="chatStore.showArchived || props.collapsed">
        <div
          v-for="chat in filteredArchivedChats"
          :key="'arch-' + chat.id"
          class="chat-sidebar-item archived-item"
          :class="{ active: isActive(chat) }"
          @click="selectChat(chat)"
          @contextmenu="openCtxMenu($event, chat)"
        >
          <div class="chat-sidebar-avatar">
            <img v-if="chatAvatarUrl(chat)" :src="chatAvatarUrl(chat)!" alt="" class="chat-avatar-img" @error="onAvatarError" />
            <span v-else class="chat-avatar-letter" :class="{ group: chat.chatType === 'GROUP', game: chat.chatType === 'GAME' }">{{ chatAvatarLetter(chat) }}</span>
          </div>
          <div class="chat-sidebar-info">
            <div class="chat-sidebar-name">
              <span v-if="chat.chatType === 'GAME'" class="game-tag">ІГРОВА</span>
              <span v-else-if="chat.chatType === 'GROUP'" class="group-tag">ГРУПА</span>
              {{ chatDisplayName(chat) }}
            </div>
            <div class="chat-sidebar-last">{{ truncate(chat.lastMessage) }}</div>
          </div>
        </div>
      </template>
    </template>

    <Teleport to="body">
      <div
        v-if="ctxMenu.show && ctxMenu.chat"
        class="chat-ctx-menu"
        :style="{ top: ctxMenu.y + 'px', left: ctxMenu.x + 'px' }"
        @click.stop
      >
        <button class="ctx-item" @click="onPinChat">
          {{ ctxMenu.chat!.pinned ? 'ВІДКРІПИТИ' : 'ЗАКРІПИТИ' }}
        </button>
        <button class="ctx-item" @click="onArchiveChat">
          {{ ctxMenu.chat!.archived ? 'РОЗАРХІВУВАТИ' : 'АРХІВУВАТИ' }}
        </button>
        <div class="ctx-divider"></div>
        <button class="ctx-item ctx-danger" @click="onDeleteChat">
          {{ ctxMenu.chat!.isGroup ? 'ВИЙТИ З ЧАТУ' : 'ВИДАЛИТИ ЧАТ' }}
        </button>
      </div>

      <div v-if="confirmModal.show" class="confirm-overlay" @click.self="onCancelDelete">
        <div class="confirm-modal">
          <div class="confirm-title">{{ confirmModal.title }}</div>
          <div class="confirm-message">{{ confirmModal.message }}</div>
          <div class="confirm-actions">
            <button class="confirm-btn cancel-btn" @click="onCancelDelete">СКАСУВАТИ</button>
            <button class="confirm-btn danger-btn" @click="onConfirmDelete">
              {{ confirmModal.isGroup ? 'ВИЙТИ' : 'ВИДАЛИТИ' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>

.chat-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
}

.chat-sidebar-header {
  padding: 18px 20px 14px;
  border-bottom: 2px solid var(--border);
  font-family: var(--font-display);
  font-size: 18px;
  letter-spacing: 3px;
  color: var(--yellow);
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
}
.chat-sidebar-header::after {
  content: '';
  position: absolute;
  bottom: -3px;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, var(--yellow-dim), transparent);
  opacity: 0.4;
}

.chat-sidebar-badge {
  background: var(--red);
  color: #fff;
  font-family: var(--font-body);
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 2px;
  letter-spacing: 0;
  animation: badgePulse 2s ease-in-out infinite;
  box-shadow: 0 0 8px rgba(192,57,43,0.3);
}
@keyframes badgePulse {
  0%, 100% { box-shadow: 0 0 6px rgba(192,57,43,0.3); }
  50% { box-shadow: 0 0 12px rgba(192,57,43,0.6); }
}

.chat-sidebar-loading,
.chat-sidebar-empty {
  padding: 40px 20px;
  text-align: center;
  color: var(--gray);
  font-size: 13px;
  letter-spacing: 1px;
}

.chat-section-label {
  padding: 10px 20px 6px;
  font-size: 10px;
  font-weight: 400;
  letter-spacing: 3px;
  color: var(--gray);
  border-bottom: 1px solid var(--border);
  font-family: var(--font-display);
  border-left: 3px solid var(--yellow-dim);
  background: linear-gradient(90deg, rgba(245,197,24,0.03), transparent);
}

.chat-sidebar-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border);
  border-left: 3px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}
.chat-sidebar-item:hover {
  background: linear-gradient(90deg, var(--panel-light), var(--panel));
  border-left-color: rgba(245,197,24,0.3);
}
.chat-sidebar-item.active {
  background: linear-gradient(90deg, rgba(245,197,24,0.06), var(--panel-light));
  border-left-color: var(--yellow);
  box-shadow: inset 0 0 20px rgba(245,197,24,0.03);
}
.chat-sidebar-item.unread {
  background: rgba(245, 197, 24, 0.03);
}
.chat-sidebar-item.unread::after {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--yellow);
  animation: unreadGlow 2s ease-in-out infinite;
}
@keyframes unreadGlow {
  0%, 100% { box-shadow: 0 0 4px rgba(245,197,24,0.3); }
  50% { box-shadow: 0 0 10px rgba(245,197,24,0.6); }
}
.chat-sidebar-item.pinned-item {
  background: linear-gradient(90deg, rgba(245,197,24,0.02), transparent);
  background-image:
    linear-gradient(90deg, rgba(245,197,24,0.02), transparent),
    repeating-linear-gradient(
      -45deg,
      transparent,
      transparent 8px,
      rgba(245,197,24,0.008) 8px,
      rgba(245,197,24,0.008) 10px
    );
}
.chat-sidebar-item.archived-item {
  opacity: 0.5;
  filter: grayscale(0.3);
}
.chat-sidebar-item.archived-item:hover {
  opacity: 0.8;
  filter: grayscale(0);
}

.pin-icon {
  font-size: 6px;
  flex-shrink: 0;
  color: var(--yellow);
  line-height: 1;
  text-shadow: 0 0 6px rgba(245,197,24,0.4);
}

.chat-sidebar-avatar {
  position: relative;
  width: 42px;
  height: 42px;
  flex-shrink: 0;
}

.chat-avatar-letter {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  background: linear-gradient(135deg, var(--yellow-glow), rgba(245,197,24,0.06));
  border: 2px solid var(--border);
  font-family: var(--font-display);
  font-size: 18px;
  color: var(--yellow);
  letter-spacing: 1px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.2);
  transition: all 0.2s;
}
.chat-sidebar-item:hover .chat-avatar-letter {
  border-color: var(--yellow-dim);
  box-shadow: 0 2px 10px rgba(245,197,24,0.1);
}

.chat-avatar-img {
  width: 42px;
  height: 42px;
  object-fit: cover;
  border: 2px solid var(--border);
  box-shadow: 0 2px 6px rgba(0,0,0,0.2);
  transition: all 0.2s;
}
.chat-sidebar-item:hover .chat-avatar-img {
  border-color: var(--yellow-dim);
  box-shadow: 0 2px 10px rgba(245,197,24,0.1);
}

.chat-avatar-letter.group {
  background: linear-gradient(135deg, rgba(41, 128, 185, 0.2), rgba(41, 128, 185, 0.08));
  border-color: rgba(41, 128, 185, 0.4);
  color: #5dade2;
}
.chat-sidebar-item:hover .chat-avatar-letter.group {
  border-color: rgba(93,173,226,0.6);
  box-shadow: 0 2px 10px rgba(41,128,185,0.15);
}

.chat-avatar-letter.game {
  background: linear-gradient(135deg, rgba(39, 174, 96, 0.2), rgba(39, 174, 96, 0.08));
  border-color: rgba(39, 174, 96, 0.4);
  color: #2ecc71;
}
.chat-sidebar-item:hover .chat-avatar-letter.game {
  border-color: rgba(46, 204, 113, 0.6);
  box-shadow: 0 2px 10px rgba(39, 174, 96, 0.15);
}

.chat-status-dot {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 13px;
  height: 13px;
  border-radius: 50%;
  border: 2px solid var(--panel);
  background: var(--gray);
  display: flex;
  align-items: center;
  justify-content: center;
}
.chat-status-dot.online  { background: #27ae60; }
.chat-status-dot.away    { background: var(--yellow-dim); }
.chat-status-dot.offline { background: var(--gray); }
.chat-status-dot.is-group {
  background: rgba(41, 128, 185, 0.35);
  border-color: rgba(41, 128, 185, 0.5);
  color: #5dade2;
}
.chat-status-dot.is-game {
  background: rgba(39, 174, 96, 0.35);
  border-color: rgba(39, 174, 96, 0.5);
  color: #2ecc71;
}

.chat-sidebar-info {
  flex: 1;
  min-width: 0;
}

.chat-sidebar-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--white);
  letter-spacing: 0.5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: color 0.2s;
}
.chat-sidebar-item:hover .chat-sidebar-name {
  color: var(--white);
}
.chat-sidebar-item.active .chat-sidebar-name {
  color: var(--yellow);
}

.group-tag {
  font-size: 9px;
  letter-spacing: 1.5px;
  padding: 2px 6px;
  background: linear-gradient(135deg, rgba(41, 128, 185, 0.2), rgba(41, 128, 185, 0.1));
  border: 1px solid rgba(41, 128, 185, 0.4);
  color: #5dade2;
  flex-shrink: 0;
  font-family: var(--font-display);
}

.game-tag {
  font-size: 9px;
  letter-spacing: 1.5px;
  padding: 2px 6px;
  background: linear-gradient(135deg, rgba(39, 174, 96, 0.2), rgba(39, 174, 96, 0.1));
  border: 1px solid rgba(39, 174, 96, 0.4);
  color: #2ecc71;
  flex-shrink: 0;
  font-family: var(--font-display);
}

.chat-sidebar-last {
  font-size: 12px;
  color: var(--gray);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 3px;
  transition: color 0.2s;
}

.chat-sidebar-last.sidebar-typing {
  color: var(--yellow);
  font-style: italic;
  animation: typingFade 1.2s ease-in-out infinite;
}
@keyframes typingFade {
  0%, 100% { opacity: 0.7; }
  50% { opacity: 1; }
}

.sidebar-typing-content {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.sidebar-typing-name {
  color: #5dade2;
  font-weight: 700;
  font-style: normal;
}

.sidebar-typing-dots {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-left: 2px;
}
.sidebar-typing-dots .s-dot {
  display: inline-block;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--yellow);
  animation: sidebarDotBounce 1.4s ease-in-out infinite;
}
.sidebar-typing-dots .s-dot:nth-child(2) { animation-delay: 0.2s; }
.sidebar-typing-dots .s-dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes sidebarDotBounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-2px); opacity: 1; }
}

.chat-sidebar-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  flex-shrink: 0;
}

.chat-sidebar-time {
  font-size: 10px;
  color: var(--gray);
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.chat-unread-badge {
  background: var(--yellow);
  color: var(--black);
  font-size: 10px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 2px;
  font-family: var(--font-body);
  box-shadow: 0 0 8px rgba(245,197,24,0.3);
  animation: badgePulseYellow 2s ease-in-out infinite;
}
@keyframes badgePulseYellow {
  0%, 100% { box-shadow: 0 0 6px rgba(245,197,24,0.2); }
  50% { box-shadow: 0 0 12px rgba(245,197,24,0.5); }
}

.chat-archive-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  font-size: 11px;
  color: var(--gray);
  cursor: pointer;
  border-bottom: 1px solid var(--border);
  border-top: 2px solid var(--border);
  letter-spacing: 2px;
  font-family: var(--font-display);
  transition: all 0.2s;
  position: relative;
}
.chat-archive-toggle:hover {
  background: var(--panel-light);
  color: var(--yellow);
}
.chat-archive-toggle::before {
  content: '';
  position: absolute;
  top: -2px;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--border), transparent);
}

.chat-ctx-menu {
  position: fixed;
  z-index: 9999;
  background: var(--panel);
  border: 2px solid var(--border);
  border-top: 3px solid var(--yellow);
  min-width: 200px;
  box-shadow: 0 12px 40px rgba(0,0,0,0.7), 0 0 0 1px rgba(245,197,24,0.05);
  padding: 6px 0;
  animation: ctxMenuIn 0.12s ease-out;
}
@keyframes ctxMenuIn {
  from { opacity: 0; transform: scale(0.95) translateY(-4px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
.chat-ctx-menu::after {
  content: '';
  position: absolute;
  inset: 3px;
  border: 1px solid rgba(245,197,24,0.06);
  pointer-events: none;
}
.ctx-item {
  display: block;
  width: 100%;
  text-align: left;
  background: none;
  border: none;
  border-left: 3px solid transparent;
  padding: 10px 18px 10px 15px;
  color: var(--gray-light);
  font-size: 11px;
  font-family: var(--font-display);
  letter-spacing: 2px;
  cursor: pointer;
  transition: all 0.12s;
}
.ctx-item:hover {
  background: var(--panel-light);
  color: var(--yellow);
  border-left-color: var(--yellow-dim);
}
.ctx-item.ctx-danger {
  color: var(--gray);
}
.ctx-item.ctx-danger:hover {
  color: var(--red);
  background: rgba(192, 57, 43, 0.08);
  border-left-color: var(--red);
}
.ctx-divider {
  height: 1px;
  background: var(--border);
  margin: 4px 12px;
}

.confirm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.82);
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(6px);
  animation: confirmOverlayIn 0.2s ease-out;
}
@keyframes confirmOverlayIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.confirm-modal {
  background: var(--panel);
  border: 2px solid var(--border);
  border-top: 3px solid var(--yellow);
  width: 420px;
  max-width: 90vw;
  padding: 0;
  box-shadow:
    0 24px 80px rgba(0, 0, 0, 0.6),
    0 0 0 1px rgba(245, 197, 24, 0.06),
    inset 0 1px 0 rgba(245, 197, 24, 0.04);
  animation: confirmModalIn 0.22s ease-out;
  position: relative;
}
@keyframes confirmModalIn {
  from { opacity: 0; transform: translateY(-12px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.confirm-modal::before {
  content: '';
  position: absolute;
  inset: 3px;
  border: 1px solid rgba(245, 197, 24, 0.04);
  pointer-events: none;
}
.confirm-modal::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 50px;
  background: linear-gradient(180deg, rgba(245, 197, 24, 0.03), transparent);
  pointer-events: none;
}

.confirm-title {
  font-family: var(--font-display);
  font-size: 16px;
  letter-spacing: 3px;
  color: var(--yellow);
  padding: 22px 28px 0;
  display: flex;
  align-items: center;
  gap: 10px;
  text-shadow: 0 0 12px rgba(245, 197, 24, 0.2);
  position: relative;
  z-index: 1;
}

.confirm-message {
  font-size: 13px;
  color: var(--gray-light);
  line-height: 1.7;
  padding: 14px 28px 0;
  position: relative;
  z-index: 1;
  letter-spacing: 0.3px;
}

.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 22px 28px;
  border-top: 1px solid var(--border);
  margin-top: 22px;
  position: relative;
  z-index: 1;
}

.confirm-btn {
  padding: 9px 22px;
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 2px;
  border: 2px solid var(--border);
  background: transparent;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  overflow: hidden;
}

.cancel-btn {
  color: var(--gray);
}
.cancel-btn:hover {
  border-color: var(--gray);
  color: var(--white);
  background: rgba(255, 255, 255, 0.03);
}

.danger-btn {
  color: var(--red);
  border-color: rgba(192, 57, 43, 0.4);
}
.danger-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(192, 57, 43, 0.08), transparent);
  opacity: 0;
  transition: opacity 0.2s;
}
.danger-btn:hover::before {
  opacity: 1;
}
.danger-btn:hover {
  background: rgba(192, 57, 43, 0.12);
  border-color: var(--red);
  box-shadow: 0 0 12px rgba(192, 57, 43, 0.15);
  text-shadow: 0 0 8px rgba(192, 57, 43, 0.3);
}
</style>
