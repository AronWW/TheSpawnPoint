<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '../stores/chat'
import { useAuthStore } from '../stores/auth'
import { useStompClient } from '../composables/useStompClient'
import { CHAT_EMOJIS } from '../utils/emojis'
import { API_BASE_URL } from '../config'
import GroupChatSettingsModal from './GroupChatSettingsModal.vue'
import ChatEmptyStateBase from './ChatEmptyStateBase.vue'
import ChatEmptyStateSecret from './ChatEmptyStateSecret.vue'
import { useAchievementStore } from '../stores/achievements'
import type { ChatMessage, PinnedMessageInfo } from '../types'

const router = useRouter()
const chatStore = useChatStore()
const auth = useAuthStore()
const stomp = useStompClient()
const achievementStore = useAchievementStore()

const PUBLIC_BASE_URL = API_BASE_URL.replace(/\/api\/?$/, '')

const showGroupSettings = ref(false)


const ROOM_OF_REQUIREMENT_CODE = 'ROOM_OF_REQUIREMENT'

const secretPlaceholderLocked = ref(false)

const shouldRenderSecretPlaceholder = computed(() => (
  achievementStore.hasLoadedMyAchievements
    && (secretPlaceholderLocked.value || !achievementStore.hasAchievement(ROOM_OF_REQUIREMENT_CODE))
))

function resolveAvatar(url: string | null): string {
  if (!url) return PUBLIC_BASE_URL + '/avatars/default/avatar-1.png'
  if (url.startsWith('http')) return url
  return PUBLIC_BASE_URL + url
}

const messageInput = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const typingTimeout = ref<ReturnType<typeof setTimeout> | null>(null)

const contextMenu = ref<{ show: boolean; x: number; y: number; msg: ChatMessage | null }>({
  show: false, x: 0, y: 0, msg: null
})

const showSearch = ref(false)
const searchInput = ref('')

const emojiPickerMsg = ref<ChatMessage | null>(null)
const emojiPickerPos = ref<{ x: number; y: number }>({ x: 0, y: 0 })
const EMOJIS = CHAT_EMOJIS

const emojiPickerStyle = computed(() => {
  const pickerW = 260
  const pickerH = 120
  let x = emojiPickerPos.value.x
  let y = emojiPickerPos.value.y

  if (x + pickerW > window.innerWidth - 12) x = window.innerWidth - pickerW - 12
  if (x < 12) x = 12
  if (y + pickerH > window.innerHeight - 12) y = emojiPickerPos.value.y - pickerH - 8
  if (y < 12) y = 12

  return { top: y + 'px', left: x + 'px' }
})

const isGroup = computed(() => chatStore.activeChat?.isGroup ?? false)
const activeChatId = computed(() => chatStore.activeChat?.id ?? 0)

const myRole = computed(() => {
  const chat = chatStore.activeChat
  if (!chat?.isGroup || !chat.participants) return null
  const me = chat.participants.find(p => p.email === auth.user?.email)
  return me?.role ?? 'MEMBER'
})

const canDeleteAny = computed(() => myRole.value === 'OWNER' || myRole.value === 'ADMIN')

const currentPinIndex = ref(-1)
const showPinnedList = ref(false)

const hasPinnedMessages = computed(() => chatStore.pinnedMessages.length > 0)
const pinnedCount = computed(() => chatStore.pinnedMessages.length)

const currentPin = computed(() => {
  const pins = chatStore.pinnedMessages
  if (pins.length === 0) return null
  if (currentPinIndex.value < 0 || currentPinIndex.value >= pins.length) {
    return pins[pins.length - 1]
  }
  return pins[currentPinIndex.value]
})

const pinnedListFiltered = computed(() => {
  const pins = chatStore.pinnedMessages
  if (!currentPin.value) return pins
  return pins.filter(p => p.messageId !== currentPin.value!.messageId)
})

function onPinnedStripClick() {
  if (!currentPin.value) return
  scrollToMessage(currentPin.value.messageId)
  if (chatStore.pinnedMessages.length > 1) {
    const pins = chatStore.pinnedMessages
    const curIdx = currentPinIndex.value < 0 || currentPinIndex.value >= pins.length
      ? pins.length - 1
      : currentPinIndex.value
    currentPinIndex.value = curIdx - 1 < 0 ? pins.length - 1 : curIdx - 1
  }
}

function togglePinnedList() {
  showPinnedList.value = !showPinnedList.value
}

function onPinnedListItemClick(pin: PinnedMessageInfo) {
  scrollToMessage(pin.messageId)
  const idx = chatStore.pinnedMessages.findIndex(p => p.messageId === pin.messageId)
  if (idx >= 0) {
    currentPinIndex.value = idx === 0 ? chatStore.pinnedMessages.length - 1 : idx - 1
  }
  showPinnedList.value = false
}

watch(() => chatStore.activeChat?.id, (newId) => {
  currentPinIndex.value = -1
  showPinnedList.value = false
  showGroupSettings.value = false
  if (newId) {
    secretPlaceholderLocked.value = false
  }
})

watch(() => chatStore.pinnedMessages.length, () => {
  currentPinIndex.value = -1
})

const chatTitle = computed(() => {
  const chat = chatStore.activeChat
  if (!chat) return ''
  if (chat.chatType === 'GAME') return chat.title || 'Ігровий чат'
  if (chat.isGroup) return chat.title || 'Груповий чат'
  return chat.partnerDisplayName || 'Чат'
})

const chatType = computed(() => chatStore.activeChat?.chatType ?? 'DM')

const isTyping = computed(() => activeChatId.value ? chatStore.isPartnerTyping(activeChatId.value) : false)
const typingName = computed(() => activeChatId.value ? chatStore.typingDisplayName(activeChatId.value) : '')

const chatSubtitle = computed(() => {
  const chat = chatStore.activeChat
  if (!chat) return ''
  if (chat.isGroup) {
    if (isTyping.value) return `${typingName.value} друкує...`
    const count = chat.participants?.length ?? 0
    return `${count} учасників`
  }
  if (isTyping.value) return 'друкує...'
  return statusText(chat.partnerStatus || 'OFFLINE')
})

function scrollToBottom(force = false) {
  nextTick(() => {
    const el = messagesContainer.value
    if (!el) return
    const isNearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 120
    if (force || isNearBottom) el.scrollTop = el.scrollHeight
  })
}

watch(() => chatStore.messages.length, () => scrollToBottom())
watch(() => chatStore.activeChat, () => {
  nextTick(() => scrollToBottom(true))
})

function sendMessage() {
  const text = messageInput.value.trim()
  if (!text || !chatStore.activeChat) return

  if (chatStore.editingMessage) {
    chatStore.editMessageAction(chatStore.editingMessage.id, text)
    messageInput.value = ''
    return
  }

  const replyToId = chatStore.replyingTo?.id ?? null

  if (isGroup.value) {
    stomp.publish('/app/chat.sendGroup', {
      chatId: chatStore.activeChat.id,
      content: text,
      replyToId,
    })
  } else {
    stomp.publish('/app/chat.send', {
      recipientEmail: chatStore.activeChat.partnerEmail,
      content: text,
      replyToId,
    })
  }
  messageInput.value = ''
  chatStore.setReplyingTo(null)
  scrollToBottom(true)
}

function onTyping() {
  if (!chatStore.activeChat) return
  if (!typingTimeout.value) {
    if (isGroup.value) {
      stomp.publish('/app/chat.typingGroup', { chatId: chatStore.activeChat.id })
    } else {
      stomp.publish('/app/chat.typing', { recipientEmail: chatStore.activeChat.partnerEmail })
    }
  }
  if (typingTimeout.value) clearTimeout(typingTimeout.value)
  typingTimeout.value = setTimeout(() => { typingTimeout.value = null }, 2000)
}

watch(() => chatStore.activeChat?.id, (newId) => {
  if (newId && chatStore.activeChat) {
    if (chatStore.activeChat.isGroup) {
      stomp.publish('/app/chat.readGroup', { chatId: chatStore.activeChat.id })
    } else if (chatStore.activeChat.partnerEmail) {
      stomp.publish('/app/chat.read', { senderEmail: chatStore.activeChat.partnerEmail })
    }
  }
})

function onScroll() {
  const el = messagesContainer.value
  if (!el) return
  if (el.scrollTop < 60 && chatStore.hasMore && !chatStore.loadingMessages) {
    const oldHeight = el.scrollHeight
    chatStore.loadOlder().then(() => {
      nextTick(() => { el.scrollTop = el.scrollHeight - oldHeight })
    })
  }
}

function isOwnMessage(email: string | null) { return auth.user?.email === email }

function getSenderUserId(email: string | null): number | null {
  if (!email || !chatStore.activeChat?.participants) return null
  const p = chatStore.activeChat.participants.find(p => p.email === email)
  return p?.userId ?? null
}

function getSenderAvatarUrl(email: string | null): string {
  if (!email || !chatStore.activeChat?.participants) return resolveAvatar(null)
  const p = chatStore.activeChat.participants.find(p => p.email === email)
  return resolveAvatar(p?.avatarUrl ?? null)
}

function navigateToProfile(email: string | null) {
  const userId = getSenderUserId(email)
  if (userId) router.push(`/profile/${userId}`)
}

function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString('uk-UA', { hour: '2-digit', minute: '2-digit' })
}

function formatDate(iso: string) {
  const d = new Date(iso)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) return 'Сьогодні'
  const y = new Date(now); y.setDate(now.getDate() - 1)
  if (d.toDateString() === y.toDateString()) return 'Вчора'
  return d.toLocaleDateString('uk-UA', { day: 'numeric', month: 'long' })
}

function shouldShowDate(i: number): boolean {
  if (i === 0) return true
  const prev = chatStore.messages[i - 1]
  const curr = chatStore.messages[i]
  if (!prev || !curr) return false
  return new Date(prev.sentAt).toDateString() !== new Date(curr.sentAt).toDateString()
}

function statusText(s: string) {
  if (s === 'ONLINE') return 'онлайн'
  if (s === 'AWAY') return 'відійшов'
  return 'офлайн'
}

const contextMenuStyle = computed(() => {
  const menuW = 210
  const menuH = 200
  let x = contextMenu.value.x
  let y = contextMenu.value.y

  if (x + menuW > window.innerWidth - 8) {
    x = window.innerWidth - menuW - 8
  }
  if (x < 8) x = 8

  if (y + menuH > window.innerHeight - 8) {
    y = window.innerHeight - menuH - 8
  }
  if (y < 8) y = 8

  return { top: y + 'px', left: x + 'px' }
})

function openContextMenu(event: MouseEvent, msg: ChatMessage) {
  if (msg.deleted || msg.system) return
  event.preventDefault()
  contextMenu.value = { show: true, x: event.clientX, y: event.clientY, msg }
}

function closeContextMenu() {
  contextMenu.value = { show: false, x: 0, y: 0, msg: null }
}

function onReply(msg: ChatMessage) {
  chatStore.setReplyingTo(msg)
  closeContextMenu()
}

function onEdit(msg: ChatMessage) {
  chatStore.setEditingMessage(msg)
  messageInput.value = msg.content
  closeContextMenu()
}

function onDeleteMsg(msg: ChatMessage) {
  chatStore.deleteMessage(msg.id)
  closeContextMenu()
}

function onPinMsg(msg: ChatMessage) {
  if (!chatStore.activeChat) return
  const isPinned = chatStore.pinnedMessages.some(p => p.messageId === msg.id)
  if (isPinned) {
    chatStore.unpinMessageAction(chatStore.activeChat.id, msg.id)
  } else {
    chatStore.pinMessageAction(chatStore.activeChat.id, msg.id)
  }
  closeContextMenu()
}

function cancelEdit() {
  chatStore.setEditingMessage(null)
  messageInput.value = ''
}

function cancelReply() {
  chatStore.setReplyingTo(null)
}

function openEmojiPicker(msg: ChatMessage) {
  if (emojiPickerMsg.value?.id === msg.id) {
    emojiPickerMsg.value = null
    return
  }
  const msgEl = messagesContainer.value?.querySelector(`[data-msg-id="${msg.id}"]`)
  const bubbleEl = msgEl?.querySelector('.chat-msg-bubble') as HTMLElement | null
  if (bubbleEl) {
    const rect = bubbleEl.getBoundingClientRect()
    const isOwn = auth.user?.email === msg.senderEmail
    emojiPickerPos.value = {
      x: isOwn ? rect.right - 260 : rect.left,
      y: rect.bottom + 6
    }
  }
  emojiPickerMsg.value = msg
  closeContextMenu()
}

function selectEmoji(emoji: string) {
  if (emojiPickerMsg.value) {
    chatStore.toggleReaction(emojiPickerMsg.value.id, emoji)
    emojiPickerMsg.value = null
  }
}

function toggleReactionDirect(msgId: number, emoji: string) {
  chatStore.toggleReaction(msgId, emoji)
}

function toggleSearch() {
  showSearch.value = !showSearch.value
  if (!showSearch.value) {
    searchInput.value = ''
    chatStore.searchQuery = ''
    chatStore.searchResults = []
  }
}

function doSearch() {
  if (!chatStore.activeChat) return
  chatStore.searchMessagesInChat(chatStore.activeChat.id, searchInput.value)
}

function scrollToMessage(msgId: number) {
  const el = messagesContainer.value
  if (!el) return
  const msgEl = el.querySelector(`[data-msg-id="${msgId}"]`)
  if (msgEl) {
    msgEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
    msgEl.classList.add('highlight-msg')
    setTimeout(() => msgEl.classList.remove('highlight-msg'), 2000)
  }
}

function onWindowClick() {
  closeContextMenu()
  emojiPickerMsg.value = null
  showPinnedList.value = false
}

watch(() => chatStore.editingMessage, (msg) => {
  if (msg) messageInput.value = msg.content
})

onMounted(() => {
  if (!achievementStore.hasLoadedMyAchievements && !achievementStore.loading) {
    void achievementStore.fetchMyAchievements()
  }
})
</script>

<template>
  <div class="chat-window" v-if="chatStore.activeChat" @click="onWindowClick">

    <div class="chat-window-header">
      <div class="cw-avatar" :class="{ group: chatType === 'GROUP', game: chatType === 'GAME' }">
        <img v-if="!isGroup && chatStore.activeChat?.partnerAvatarUrl" :src="resolveAvatar(chatStore.activeChat.partnerAvatarUrl)" alt="" class="cw-avatar-img" />
        <img v-else-if="isGroup && chatStore.activeChat?.groupAvatarUrl" :src="resolveAvatar(chatStore.activeChat.groupAvatarUrl)" alt="" class="cw-avatar-img" />
        <span v-else class="cw-letter">{{ chatTitle.charAt(0).toUpperCase() }}</span>
      </div>
      <div class="cw-header-info">
        <div class="cw-name">
          <span v-if="chatType === 'GAME'" class="cw-type-badge cw-type-game" title="Ігровий чат">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 11h4"/><path d="M8 9v4"/><circle cx="17" cy="12" r="1"/><circle cx="20" cy="9" r="1"/><rect x="2" y="6" width="20" height="12" rx="3"/></svg>
          </span>
          <span v-else-if="chatType === 'GROUP'" class="cw-type-badge cw-type-group" title="Груповий чат">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          </span>
          {{ chatTitle }}
        </div>
        <div class="cw-status" :class="{ typing: isTyping }">
          <template v-if="isTyping && (chatType === 'GROUP' || chatType === 'GAME')">
            <span class="cw-typing-name">{{ typingName }}</span>
            <span class="cw-typing-text">пише</span>
            <span class="cw-typing-dots">
              <span class="cw-dot"></span>
              <span class="cw-dot"></span>
              <span class="cw-dot"></span>
            </span>
          </template>
          <template v-else-if="isTyping">
            <span class="cw-typing-text">друкує</span>
            <span class="cw-typing-dots">
              <span class="cw-dot"></span>
              <span class="cw-dot"></span>
              <span class="cw-dot"></span>
            </span>
          </template>
          <template v-else>{{ chatSubtitle }}</template>
        </div>
      </div>
      <div class="cw-header-actions">
        <button v-if="isGroup && chatType !== 'GAME'" class="cw-action-btn cw-icon-btn" @click.stop="showGroupSettings = true" title="Налаштування">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
        </button>
        <button class="cw-action-btn" @click.stop="toggleSearch" title="Пошук">
          ПОШУК
        </button>
      </div>
    </div>

    <div v-if="showSearch" class="cw-search-bar">
      <input
        v-model="searchInput"
        @keydown.enter.prevent="doSearch"
        type="text"
        class="cw-search-input"
        placeholder="Пошук повідомлень..."
      />
      <button class="cw-search-btn" @click="doSearch" :disabled="!searchInput.trim()">Знайти</button>
      <button class="cw-search-close" @click="toggleSearch">✕</button>
      <div v-if="chatStore.searchResults.length > 0" class="cw-search-results">
        <div
          v-for="r in chatStore.searchResults"
          :key="r.id"
          class="cw-search-result"
          @click="scrollToMessage(r.id)"
        >
          <span class="cw-search-sender">{{ r.senderName || 'System' }}</span>
          <span class="cw-search-text">{{ r.content.length > 60 ? r.content.slice(0, 60) + '…' : r.content }}</span>
          <span class="cw-search-time">{{ formatTime(r.sentAt) }}</span>
        </div>
      </div>
      <div v-if="chatStore.searchLoading" class="cw-search-loading">Пошук...</div>
    </div>

    <div v-if="hasPinnedMessages" class="cw-pinned-section">
      <div class="cw-pinned-strip" @click="onPinnedStripClick">
        <span class="pinned-icon">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="17" x2="12" y2="22"/><path d="M5 17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1v4.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24Z"/></svg>
        </span>
        <div class="pinned-body">
          <span class="pinned-sender">{{ currentPin?.senderName }}</span>
          <span class="pinned-text">{{ currentPin?.content }}</span>
        </div>
        <span v-if="pinnedCount > 1" class="pinned-counter">
          {{ pinnedCount }} закріплено
        </span>
        <button class="pinned-expand-btn" @click.stop="togglePinnedList" :title="showPinnedList ? 'Згорнути' : 'Всі закріплені'">
          <svg :style="{ transform: showPinnedList ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.2s' }" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg>
        </button>
      </div>
      <div v-if="showPinnedList" class="cw-pinned-list">
        <div
          v-for="pin in pinnedListFiltered"
          :key="pin.id"
          class="cw-pinned-list-item"
          @click="onPinnedListItemClick(pin)"
        >
          <span class="pinned-list-icon">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="17" x2="12" y2="22"/><path d="M5 17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1v4.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24Z"/></svg>
          </span>
          <div class="pinned-list-body">
            <span class="pinned-list-sender">{{ pin.senderName }}</span>
            <span class="pinned-list-text">{{ pin.content.length > 80 ? pin.content.slice(0, 80) + '…' : pin.content }}</span>
          </div>
          <button class="pinned-list-unpin" @click.stop="chatStore.unpinMessageAction(activeChatId, pin.messageId)" title="Відкріпити">✕</button>
        </div>
        <div v-if="pinnedListFiltered.length === 0" class="cw-pinned-list-empty">
          Немає інших закріплених повідомлень
        </div>
      </div>
    </div>

    <div class="chat-messages" ref="messagesContainer" @scroll="onScroll">
      <div v-if="chatStore.loadingMessages && chatStore.messages.length > 0" class="chat-loading-older">
        Завантаження...
      </div>
      <template v-for="(msg, idx) in chatStore.messages" :key="msg.id">
        <div v-if="shouldShowDate(idx)" class="chat-date-divider">
          <span>{{ formatDate(msg.sentAt) }}</span>
        </div>

        <div v-if="msg.system" class="chat-system-msg">
          {{ msg.content }}
        </div>

        <div
          v-else
          class="chat-msg"
          :class="{ own: isOwnMessage(msg.senderEmail), deleted: msg.deleted, 'has-avatar': isGroup && !isOwnMessage(msg.senderEmail) && !msg.deleted }"
          :data-msg-id="msg.id"
          @contextmenu="openContextMenu($event, msg)"
        >
          <img
            v-if="isGroup && !isOwnMessage(msg.senderEmail) && !msg.deleted"
            :src="getSenderAvatarUrl(msg.senderEmail)"
            alt=""
            class="chat-msg-avatar"
            @click.stop="navigateToProfile(msg.senderEmail)"
          />
          <div class="chat-msg-bubble">
            <button
              v-if="!msg.deleted"
              class="msg-hover-react"
              @click.stop="openEmojiPicker(msg)"
              title="Реакція"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><line x1="9" y1="9" x2="9.01" y2="9" stroke-width="3" stroke-linecap="round"/><line x1="15" y1="9" x2="15.01" y2="9" stroke-width="3" stroke-linecap="round"/></svg>
            </button>

            <div v-if="msg.replyToId && !msg.deleted" class="chat-msg-reply" @click="scrollToMessage(msg.replyToId!)">
              <span class="reply-sender">{{ msg.replyToSenderName }}</span>
              <span class="reply-text">{{ msg.replyToContent }}</span>
            </div>

            <a
              v-if="isGroup && !isOwnMessage(msg.senderEmail) && !msg.deleted"
              class="chat-msg-sender"
              @click.stop="navigateToProfile(msg.senderEmail)"
            >
              {{ msg.senderName }}
            </a>

            <div class="chat-msg-text" :class="{ 'deleted-text': msg.deleted }">
              {{ msg.content }}
            </div>

            <div class="chat-msg-meta">
              <span v-if="msg.edited && !msg.deleted" class="chat-msg-edited">(змінено)</span>
              <span class="chat-msg-time">{{ formatTime(msg.sentAt) }}</span>
              <span v-if="isOwnMessage(msg.senderEmail) && !msg.deleted" class="chat-msg-read">
                {{ msg.read ? '✓✓' : '✓' }}
              </span>
            </div>

            <div v-if="msg.reactions && msg.reactions.length > 0 && !msg.deleted" class="chat-msg-reactions">
              <button
                v-for="r in msg.reactions"
                :key="r.emoji"
                class="reaction-chip"
                :class="{ own: r.userEmails.includes(auth.user?.email || '') }"
                @click.stop="toggleReactionDirect(msg.id, r.emoji)"
              >
                {{ r.emoji }} {{ r.count }}
              </button>
            </div>
          </div>
        </div>
      </template>

      <Teleport to="body">
        <div
          v-if="emojiPickerMsg"
          class="emoji-picker-overlay"
          @click="emojiPickerMsg = null"
        >
          <div
            class="emoji-picker"
            :style="emojiPickerStyle"
            @click.stop
          >
            <button v-for="e in EMOJIS" :key="e" class="emoji-btn" @click.stop="selectEmoji(e)">{{ e }}</button>
          </div>
        </div>
      </Teleport>
      <div v-if="isTyping" class="chat-typing-indicator">
        <div class="typing-bubble">
          <span class="typing-name">{{ typingName }}</span>
          <span class="typing-label">пише</span>
          <span class="typing-dots">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
          </span>
        </div>
      </div>

      <div v-if="chatStore.messages.length === 0 && !chatStore.loadingMessages" class="chat-empty">
        <div class="chat-empty-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
        </div>
        <div>Напишіть перше повідомлення!</div>
      </div>
    </div>

    <Teleport to="body">
      <div
        v-if="contextMenu.show && contextMenu.msg"
        class="msg-context-menu"
        :style="contextMenuStyle"
        @click.stop
      >
        <button class="ctx-item" @click="onReply(contextMenu.msg!)">ВІДПОВІСТИ</button>
        <button v-if="isOwnMessage(contextMenu.msg!.senderEmail)" class="ctx-item" @click="onEdit(contextMenu.msg!)">РЕДАГУВАТИ</button>
        <button class="ctx-item" @click="openEmojiPicker(contextMenu.msg!)">РЕАКЦІЯ</button>
        <button class="ctx-item" @click="onPinMsg(contextMenu.msg!)">
          {{ chatStore.pinnedMessages.some(p => p.messageId === contextMenu.msg!.id) ? 'ВІДКРІПИТИ' : 'ЗАКРІПИТИ' }}
        </button>
        <div class="ctx-divider"></div>
        <button v-if="isOwnMessage(contextMenu.msg!.senderEmail) || (isGroup && canDeleteAny)" class="ctx-item ctx-danger" @click="onDeleteMsg(contextMenu.msg!)">ВИДАЛИТИ</button>
      </div>
    </Teleport>

    <div v-if="chatStore.replyingTo" class="chat-reply-bar">
      <div class="reply-preview">
        <span class="reply-label">Відповідь на</span>
        <span class="reply-name">{{ chatStore.replyingTo.senderName }}</span>
        <span class="reply-content">{{ chatStore.replyingTo.content.length > 50 ? chatStore.replyingTo.content.slice(0, 50) + '…' : chatStore.replyingTo.content }}</span>
      </div>
      <button class="reply-cancel" @click="cancelReply">✕</button>
    </div>

    <div v-if="chatStore.editingMessage" class="chat-edit-bar">
      <div class="edit-preview">
        <span class="edit-label">РЕДАГУВАННЯ</span>
        <span class="edit-content">{{ chatStore.editingMessage.content.length > 50 ? chatStore.editingMessage.content.slice(0, 50) + '…' : chatStore.editingMessage.content }}</span>
      </div>
      <button class="edit-cancel" @click="cancelEdit">✕</button>
    </div>

    <div class="chat-input-bar">
      <input
        v-model="messageInput"
        @keydown.enter.prevent="sendMessage"
        @input="onTyping"
        type="text"
        class="chat-input"
        :placeholder="chatStore.editingMessage ? 'Редагувати повідомлення...' : 'Написати повідомлення...'"
        maxlength="5000"
      />
      <button class="chat-send-btn" @click="sendMessage" :disabled="!messageInput.trim()">
        {{ chatStore.editingMessage ? 'ЗБЕРЕГТИ' : 'НАДІСЛАТИ' }}
      </button>
    </div>
  </div>

  <ChatEmptyStateSecret v-else-if="shouldRenderSecretPlaceholder" @scene-lock="secretPlaceholderLocked = true" />

  <div class="chat-window chat-placeholder" v-else>
    <ChatEmptyStateBase />
  </div>

  <GroupChatSettingsModal
    v-if="showGroupSettings && chatStore.activeChat"
    :chat="chatStore.activeChat"
    @close="showGroupSettings = false"
  />
</template>

<style scoped>

.cw-avatar.group .cw-letter {
  background: linear-gradient(135deg, rgba(41, 128, 185, 0.2), rgba(41, 128, 185, 0.08));
  border-color: rgba(41, 128, 185, 0.4);
  color: #5dade2;
}

.cw-avatar.game .cw-letter {
  background: linear-gradient(135deg, rgba(39, 174, 96, 0.2), rgba(39, 174, 96, 0.08));
  border-color: rgba(39, 174, 96, 0.4);
  color: #2ecc71;
}

.cw-type-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 3px;
  flex-shrink: 0;
  margin-right: 2px;
  vertical-align: middle;
}
.cw-type-game {
  background: linear-gradient(135deg, rgba(39, 174, 96, 0.18), rgba(39, 174, 96, 0.08));
  border: 1px solid rgba(39, 174, 96, 0.4);
  color: #2ecc71;
}
.cw-type-group {
  background: linear-gradient(135deg, rgba(41, 128, 185, 0.18), rgba(41, 128, 185, 0.08));
  border: 1px solid rgba(41, 128, 185, 0.4);
  color: #5dade2;
}

.chat-system-msg {
  text-align: center;
  padding: 8px 20px;
  margin: 8px 0;
  font-size: 11px;
  color: var(--gray);
  font-style: italic;
  letter-spacing: 1px;
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
}
.chat-system-msg::before,
.chat-system-msg::after {
  content: '';
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--border), transparent);
}

.chat-msg-sender {
  font-size: 11px;
  font-weight: 700;
  color: #5dade2;
  letter-spacing: 0.5px;
  margin-bottom: 3px;
  font-family: var(--font-body);
  cursor: pointer;
  text-decoration: none;
  display: inline-block;
  transition: color 0.15s;
}
.chat-msg-sender:hover {
  color: var(--yellow);
  text-decoration: underline;
}

.cw-status.typing {
  color: var(--yellow) !important;
  display: flex;
  align-items: center;
  gap: 4px;
}
.cw-typing-name {
  color: #5dade2;
  font-weight: 700;
  letter-spacing: 0.3px;
}
.cw-typing-text {
  color: var(--yellow);
  font-style: italic;
}
.cw-typing-dots {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-left: 2px;
}
.cw-typing-dots .cw-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--yellow);
  animation: cwDotBounce 1.4s ease-in-out infinite;
}
.cw-typing-dots .cw-dot:nth-child(2) { animation-delay: 0.2s; }
.cw-typing-dots .cw-dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes cwDotBounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-3px); opacity: 1; }
}
@keyframes typingPulse {
  0%, 100% { opacity: 0.7; }
  50% { opacity: 1; }
}

.chat-window-header { display: flex; align-items: center; gap: 14px; }
.cw-header-info { flex: 1; min-width: 0; }
.cw-name { display: flex; align-items: center; gap: 6px; font-weight: 600; font-size: 15px; color: var(--white); letter-spacing: 0.5px; }
.cw-header-actions { display: flex; gap: 8px; flex-shrink: 0; }
.cw-action-btn {
  background: none;
  border: 2px solid var(--border);
  color: var(--gray);
  padding: 6px 16px;
  font-size: 11px;
  font-family: var(--font-display);
  letter-spacing: 2px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}
.cw-action-btn:hover {
  border-color: var(--yellow-dim);
  color: var(--yellow);
  box-shadow: 0 0 8px rgba(245,197,24,0.1);
}
.cw-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px;
  width: 32px;
  height: 32px;
}
.cw-action-btn:active {
  transform: scale(0.96);
}

.cw-search-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 16px;
  border-bottom: 2px solid var(--border);
  background: var(--panel);
  position: relative;
}
.cw-search-bar::after {
  content: '';
  position: absolute;
  bottom: -3px;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--yellow-dim), transparent);
  opacity: 0.25;
}
.cw-search-input {
  flex: 1;
  min-width: 120px;
  background: var(--dark);
  border: 2px solid var(--border);
  border-left: 3px solid transparent;
  padding: 8px 12px;
  color: var(--white);
  font-size: 13px;
  font-family: var(--font-body);
  letter-spacing: 0.3px;
  outline: none;
  transition: all 0.2s;
}
.cw-search-input:focus {
  border-color: var(--yellow-dim);
  border-left-color: var(--yellow);
}
.cw-search-input::placeholder {
  color: var(--gray);
  font-style: italic;
}
.cw-search-btn {
  background: var(--yellow);
  color: var(--black);
  border: none;
  padding: 8px 18px;
  font-size: 11px;
  font-family: var(--font-display);
  font-weight: 700;
  letter-spacing: 2px;
  cursor: pointer;
  transition: all 0.15s;
  position: relative;
  overflow: hidden;
}
.cw-search-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.15) 0%, transparent 60%);
  pointer-events: none;
}
.cw-search-btn:hover:not(:disabled) {
  background: var(--yellow-dim);
  box-shadow: 0 0 10px rgba(245,197,24,0.2);
}
.cw-search-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.cw-search-close {
  background: none;
  border: 2px solid var(--border);
  color: var(--gray);
  font-size: 14px;
  cursor: pointer;
  padding: 4px 10px;
  transition: all 0.15s;
}
.cw-search-close:hover {
  border-color: var(--red-dim);
  color: var(--red);
}
.cw-search-results {
  width: 100%;
  max-height: 220px;
  overflow-y: auto;
  border-top: 2px solid var(--border);
  margin-top: 6px;
  background: var(--dark);
}
.cw-search-result {
  display: flex;
  gap: 10px;
  padding: 8px 12px;
  font-size: 12px;
  cursor: pointer;
  border-bottom: 1px solid var(--border);
  border-left: 3px solid transparent;
  transition: all 0.15s;
}
.cw-search-result:hover {
  background: var(--panel-light);
  border-left-color: var(--yellow-dim);
}
.cw-search-sender {
  color: var(--yellow);
  font-weight: 600;
  flex-shrink: 0;
  font-family: var(--font-body);
  letter-spacing: 0.3px;
}
.cw-search-text {
  color: var(--gray-light);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cw-search-time {
  color: var(--gray);
  flex-shrink: 0;
  font-size: 10px;
  letter-spacing: 0.3px;
}
.cw-search-loading {
  padding: 12px;
  text-align: center;
  color: var(--gray);
  font-size: 12px;
  width: 100%;
  letter-spacing: 1px;
  animation: chatPulse 1.5s ease-in-out infinite;
}

.cw-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border: 2px solid var(--border);
  box-shadow: 0 2px 8px rgba(0,0,0,0.3);
  transition: border-color 0.2s, box-shadow 0.2s;
}
.cw-avatar:hover .cw-avatar-img {
  border-color: var(--yellow-dim);
  box-shadow: 0 2px 12px rgba(245,197,24,0.15);
}

.cw-pinned-section {
  border-bottom: 2px solid var(--border);
  position: relative;
}

.cw-pinned-strip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 20px;
  border-left: 4px solid var(--yellow-dim);
  background: linear-gradient(90deg, rgba(245,197,24,0.04), var(--panel));
  cursor: pointer;
  transition: all 0.2s;
  overflow: hidden;
  position: relative;
}
.cw-pinned-strip:hover {
  background: linear-gradient(90deg, rgba(245,197,24,0.07), var(--panel-light));
  border-left-color: var(--yellow);
}
.pinned-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, rgba(245,197,24,0.15), rgba(245,197,24,0.05));
  border: 1px solid rgba(245,197,24,0.25);
  color: var(--yellow);
  transition: all 0.2s;
}
.cw-pinned-strip:hover .pinned-icon {
  background: linear-gradient(135deg, rgba(245,197,24,0.25), rgba(245,197,24,0.1));
  border-color: rgba(245,197,24,0.4);
  box-shadow: 0 0 8px rgba(245,197,24,0.15);
}
.pinned-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.pinned-sender {
  font-size: 11px;
  font-weight: 700;
  color: #5dade2;
  letter-spacing: 0.3px;
}
.pinned-text {
  font-size: 12px;
  color: var(--gray-light);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  letter-spacing: 0.3px;
}
.pinned-counter {
  font-size: 10px;
  color: var(--yellow-dim);
  flex-shrink: 0;
  letter-spacing: 1px;
  font-family: var(--font-display);
  padding: 2px 8px;
  border: 1px solid rgba(245,197,24,0.15);
  background: rgba(245,197,24,0.04);
}
.cw-pinned-strip:hover .pinned-counter {
  border-color: rgba(245,197,24,0.3);
  color: var(--yellow);
}
.pinned-expand-btn {
  background: none;
  border: 1px solid var(--border);
  color: var(--gray);
  cursor: pointer;
  padding: 3px 6px;
  flex-shrink: 0;
  transition: all 0.15s;
  line-height: 1;
  display: flex;
  align-items: center;
}
.pinned-expand-btn:hover {
  border-color: var(--yellow-dim);
  color: var(--yellow);
}

.cw-pinned-list {
  border-top: 1px solid var(--border);
  background: var(--panel);
  max-height: 240px;
  overflow-y: auto;
  animation: pinnedListIn 0.15s ease-out;
}
@keyframes pinnedListIn {
  from { opacity: 0; max-height: 0; }
  to { opacity: 1; max-height: 240px; }
}
.cw-pinned-list-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 20px;
  border-bottom: 1px solid var(--border);
  border-left: 3px solid transparent;
  cursor: pointer;
  transition: all 0.15s;
}
.cw-pinned-list-item:hover {
  background: var(--panel-light);
  border-left-color: var(--yellow-dim);
}
.cw-pinned-list-item:last-child {
  border-bottom: none;
}
.pinned-list-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: linear-gradient(135deg, rgba(245,197,24,0.1), rgba(245,197,24,0.03));
  border: 1px solid rgba(245,197,24,0.2);
  color: var(--yellow-dim);
  transition: all 0.15s;
}
.cw-pinned-list-item:hover .pinned-list-icon {
  color: var(--yellow);
  border-color: rgba(245,197,24,0.35);
  background: linear-gradient(135deg, rgba(245,197,24,0.18), rgba(245,197,24,0.06));
}
.pinned-list-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.pinned-list-sender {
  font-size: 11px;
  font-weight: 700;
  color: #5dade2;
  letter-spacing: 0.3px;
}
.pinned-list-text {
  font-size: 12px;
  color: var(--gray-light);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pinned-list-unpin {
  background: none;
  border: 1px solid var(--border);
  color: var(--gray);
  font-size: 11px;
  cursor: pointer;
  padding: 2px 6px;
  flex-shrink: 0;
  transition: all 0.15s;
  line-height: 1;
}
.pinned-list-unpin:hover {
  color: var(--red);
  border-color: var(--red-dim);
}

.cw-pinned-list-empty {
  padding: 12px 20px;
  text-align: center;
  font-size: 11px;
  color: var(--gray);
  letter-spacing: 0.5px;
  font-style: italic;
}

.msg-context-menu {
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
.msg-context-menu::after {
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

.chat-reply-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-top: 2px solid var(--border);
  border-left: 4px solid var(--yellow-dim);
  background: linear-gradient(90deg, rgba(245,197,24,0.04), var(--panel-light));
  font-size: 12px;
  animation: barSlideIn 0.15s ease-out;
}
.chat-edit-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-top: 2px solid var(--border);
  border-left: 4px solid #5dade2;
  background: linear-gradient(90deg, rgba(41,128,185,0.06), var(--panel-light));
  font-size: 12px;
  animation: barSlideIn 0.15s ease-out;
}
@keyframes barSlideIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}
.reply-preview, .edit-preview {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
  overflow: hidden;
}
.reply-label {
  color: var(--yellow);
  font-weight: 600;
  flex-shrink: 0;
  font-family: var(--font-display);
  font-size: 10px;
  letter-spacing: 2px;
}
.edit-label {
  color: #5dade2;
  font-weight: 600;
  flex-shrink: 0;
  font-family: var(--font-display);
  font-size: 10px;
  letter-spacing: 2px;
}
.reply-name {
  color: #5dade2;
  font-weight: 600;
  flex-shrink: 0;
  letter-spacing: 0.3px;
}
.reply-content, .edit-content {
  color: var(--gray-light);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-style: italic;
}
.reply-cancel, .edit-cancel {
  background: none;
  border: 2px solid var(--border);
  color: var(--gray);
  font-size: 13px;
  cursor: pointer;
  padding: 2px 8px;
  flex-shrink: 0;
  transition: all 0.15s;
  line-height: 1;
}
.reply-cancel:hover, .edit-cancel:hover {
  color: var(--red);
  border-color: var(--red-dim);
}

.chat-msg-reply {
  background: rgba(245, 197, 24, 0.05);
  border-left: 3px solid var(--yellow-dim);
  border-radius: 0 8px 8px 0;
  padding: 5px 10px;
  margin-bottom: 6px;
  font-size: 12px;
  cursor: pointer;
  display: flex;
  gap: 8px;
  transition: background 0.15s;
}
.chat-msg-reply:hover {
  background: rgba(245, 197, 24, 0.1);
}
.reply-sender {
  color: #5dade2;
  font-weight: 700;
  flex-shrink: 0;
}
.reply-text {
  color: var(--gray);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-style: italic;
}

.chat-msg-edited {
  font-size: 9px;
  color: var(--gray);
  font-style: italic;
  margin-right: 4px;
  letter-spacing: 0.5px;
}

.chat-msg.deleted .chat-msg-bubble {
  opacity: 0.45;
  background: var(--panel) !important;
  border-style: dashed !important;
}
.deleted-text {
  font-style: italic;
  color: var(--gray) !important;
}

.chat-msg-reactions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 6px;
}
.reaction-chip {
  background: rgba(255,255,255,0.04);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: 2px 9px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
  color: var(--gray-light);
  display: inline-flex;
  align-items: center;
  gap: 4px;
  line-height: 1.4;
  user-select: none;
}
.reaction-chip:hover {
  border-color: var(--yellow-dim);
  background: rgba(245,197,24,0.08);
}
.reaction-chip.own {
  border-color: var(--yellow-dim);
  background: rgba(245,197,24,0.14);
  color: var(--yellow);
}

.msg-hover-react {
  position: absolute;
  bottom: -10px;
  left: 6px;
  background: var(--panel);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 4px 8px;
  cursor: pointer;
  opacity: 0;
  pointer-events: none;
  transition: all 0.15s;
  z-index: 3;
  line-height: 1;
  color: var(--gray-light);
  box-shadow: 0 2px 8px rgba(0,0,0,0.35);
  display: flex;
  align-items: center;
  justify-content: center;
}
.chat-msg-bubble:hover .msg-hover-react {
  opacity: 1;
  pointer-events: auto;
}
.msg-hover-react:hover {
  border-color: var(--yellow-dim);
  color: var(--yellow);
  background: var(--panel-light);
  transform: scale(1.1);
}

.chat-msg.own .msg-hover-react {
  left: auto;
  right: 6px;
}

:global(.emoji-picker-overlay) {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9998;
  background: rgba(0,0,0,0.2);
}
:global(.emoji-picker) {
  position: fixed;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 2px;
  padding: 8px;
  background: var(--panel);
  border: 1px solid var(--border);
  border-radius: 14px;
  z-index: 9999;
  box-shadow: 0 8px 30px rgba(0,0,0,0.6);
  animation: emojiPickerIn 0.12s ease-out;
}
@keyframes emojiPickerIn {
  from { opacity: 0; transform: scale(0.92) translateY(4px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
:global(.emoji-btn) {
  background: none;
  border: none;
  border-radius: 10px;
  font-size: 24px;
  cursor: pointer;
  padding: 8px;
  transition: background 0.12s, transform 0.12s;
  text-align: center;
  line-height: 1;
}
:global(.emoji-btn:hover) {
  background: rgba(245,197,24,0.1);
  transform: scale(1.2);
}


.chat-typing-indicator {
  padding: 4px 0;
  animation: typingFadeIn 0.2s ease-out;
}
@keyframes typingFadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}
.typing-bubble {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: var(--panel-light);
  border: 1px solid var(--border);
  border-radius: 14px 14px 14px 4px;
  font-size: 12px;
}
.typing-name {
  color: #5dade2;
  font-weight: 700;
}
.typing-label {
  color: var(--gray);
}
.typing-dots {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  margin-left: 2px;
}
.typing-dots .dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--gray);
  animation: typingBounce 1.4s ease-in-out infinite;
}
.typing-dots .dot:nth-child(2) {
  animation-delay: 0.2s;
}
.typing-dots .dot:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes typingBounce {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-4px);
    opacity: 1;
  }
}


:global(.highlight-msg) {
  animation: msgHighlight 2s ease-out;
}
@keyframes msgHighlight {
  0% {
    background: rgba(245, 197, 24, 0.2);
    border-radius: 14px;
  }
  100% {
    background: transparent;
  }
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 14px;
  color: var(--gray);
  font-size: 14px;
  letter-spacing: 0.5px;
}
.chat-empty-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--gray);
  opacity: 0.5;
}

.chat-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
}
.chat-placeholder-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
.chat-placeholder-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--yellow);
  filter: drop-shadow(0 0 18px rgba(245, 197, 24, 0.45));
}
.chat-placeholder-title {
  font-family: var(--font-display), sans-serif;
  font-size: 18px;
  letter-spacing: 4px;
  color: var(--white);
}
.chat-placeholder-text {
  font-family: var(--font-body), sans-serif;
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--gray);
  text-align: center;
}

.chat-msg { position: relative; }

.chat-msg.has-avatar {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}
.chat-msg.has-avatar .chat-msg-bubble {
  max-width: calc(70% - 42px);
}
.chat-msg-avatar {
  width: 32px;
  height: 32px;
  object-fit: cover;
  border: 1.5px solid var(--border);
  flex-shrink: 0;
  cursor: pointer;
  transition: all 0.15s;
  margin-bottom: 4px;
}
.chat-msg-avatar:hover {
  border-color: var(--yellow-dim);
  box-shadow: 0 0 8px rgba(245,197,24,0.15);
}
</style>
