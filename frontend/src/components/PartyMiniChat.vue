<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useChatStore } from '../stores/chat'
import { useStompClient } from '../composables/useStompClient'
import { PUBLIC_BASE_URL } from '../config'
import type { ChatMessage } from '../types'

const props = defineProps<{
  chatId: number
  title: string
  isMember: boolean
  isLoggedIn: boolean
}>()

const router = useRouter()
const auth = useAuthStore()
const chatStore = useChatStore()
const stomp = useStompClient()

const messageInput = ref('')
const loading = ref(false)
const initError = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const typingTimeout = ref<ReturnType<typeof setTimeout> | null>(null)

const activeMiniChat = computed(() => {
  if (chatStore.activeChat?.id === props.chatId) return chatStore.activeChat
  return chatStore.chats.find((chat) => chat.id === props.chatId) ?? null
})

const visibleMessages = computed(() => {
  if (chatStore.activeChat?.id !== props.chatId) return []
  return chatStore.messages.slice(-12)
})

const canUseMiniChat = computed(() => props.isLoggedIn && props.isMember)
const isTyping = computed(() => chatStore.isPartnerTyping(props.chatId))
const typingName = computed(() => chatStore.typingDisplayName(props.chatId))
const participantsCount = computed(() => activeMiniChat.value?.participants?.length ?? 0)

function resolveAvatar(url: string | null): string {
  if (!url) return PUBLIC_BASE_URL + '/avatars/default/avatar-1.png'
  if (url.startsWith('http')) return url
  return PUBLIC_BASE_URL + url
}

function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString('uk-UA', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

function isOwnMessage(message: ChatMessage) {
  return auth.user?.email === message.senderEmail
}

function openFullChat() {
  router.push(`/chat?groupId=${props.chatId}`)
}

async function ensureChatLoaded() {
  if (!canUseMiniChat.value || !props.chatId) return

  loading.value = true
  initError.value = ''

  try {
    await chatStore.openGroupChatById(props.chatId)
    stomp.publish('/app/chat.readGroup', { chatId: props.chatId })
    scrollToBottom(true)
  } catch {
    initError.value = 'Не вдалося завантажити чат лобі'
  } finally {
    loading.value = false
  }
}

function scrollToBottom(force = false) {
  nextTick(() => {
    const el = messagesContainer.value
    if (!el) return

    const isNearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 80
    if (force || isNearBottom) {
      el.scrollTop = el.scrollHeight
    }
  })
}

function sendMessage() {
  const text = messageInput.value.trim()
  if (!text || !canUseMiniChat.value) return

  stomp.publish('/app/chat.sendGroup', {
    chatId: props.chatId,
    content: text,
    replyToId: null,
  })

  messageInput.value = ''
  scrollToBottom(true)
}

function onTyping() {
  if (!canUseMiniChat.value) return

  if (!typingTimeout.value) {
    stomp.publish('/app/chat.typingGroup', { chatId: props.chatId })
  }

  if (typingTimeout.value) clearTimeout(typingTimeout.value)
  typingTimeout.value = setTimeout(() => {
    typingTimeout.value = null
  }, 2000)
}

watch(() => props.chatId, () => {
  ensureChatLoaded()
})

watch(() => canUseMiniChat.value, (canUse) => {
  if (canUse) {
    ensureChatLoaded()
  }
})

watch(() => visibleMessages.value.length, () => {
  scrollToBottom()
})

onMounted(() => {
  ensureChatLoaded()
})
</script>

<template>
  <div class="party-mini-chat">
    <div class="mini-chat-head">
      <div>
        <div class="mini-chat-kicker">LOBBY CHAT</div>
        <h3 class="mini-chat-title">{{ title }}</h3>
        <p v-if="activeMiniChat" class="mini-chat-meta">
          {{ participantsCount }} учасників
          <span v-if="isTyping" class="typing-indicator">• {{ typingName }} друкує...</span>
        </p>
      </div>

      <button class="mini-chat-open" @click="openFullChat">
        ВІДКРИТИ ПОВНИЙ ЧАТ
      </button>
    </div>

    <div v-if="!isLoggedIn" class="mini-chat-locked">
      <div class="mini-chat-locked-icon">🔑</div>
      <div>
        <h4>Увійди, щоб бачити чат лобі</h4>
        <p>Після входу ти зможеш перейти в повний чат і писати повідомлення прямо тут.</p>
      </div>
    </div>

    <div v-else-if="!isMember" class="mini-chat-locked">
      <div class="mini-chat-locked-icon">🎮</div>
      <div>
        <h4>Приєднайся до лобі, щоб відкрити міні-чат</h4>
        <p>Текстовий чат доступний тільки учасникам цієї party.</p>
      </div>
    </div>

    <div v-else class="mini-chat-body">
      <div v-if="loading" class="mini-chat-state">Завантаження чату...</div>
      <div v-else-if="initError" class="mini-chat-state mini-chat-state--error">{{ initError }}</div>
      <div v-else-if="!visibleMessages.length" class="mini-chat-state">
        Тут поки тихо. Напиши перше повідомлення для своєї команди.
      </div>

      <div v-else ref="messagesContainer" class="mini-chat-messages">
        <div
          v-for="message in visibleMessages"
          :key="message.id"
          class="mini-chat-message"
          :class="{
            own: isOwnMessage(message),
            system: message.system,
            deleted: message.deleted,
          }"
        >
          <template v-if="message.system">
            <div class="mini-chat-system">{{ message.content }}</div>
          </template>

          <template v-else>
            <div class="mini-chat-bubble">
              <img
                v-if="!isOwnMessage(message)"
                :src="resolveAvatar(message.senderAvatarUrl)"
                :alt="message.senderName || 'User'"
                class="mini-chat-avatar"
              />

              <div class="mini-chat-content-wrap">
                <div class="mini-chat-message-top">
                  <span class="mini-chat-sender">
                    {{ isOwnMessage(message) ? 'Ти' : (message.senderName || 'Гравець') }}
                  </span>
                  <span class="mini-chat-time">{{ formatTime(message.sentAt) }}</span>
                </div>

                <div class="mini-chat-content" :class="{ 'is-deleted': message.deleted }">
                  {{ message.deleted ? 'Повідомлення видалено' : message.content }}
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>

      <div class="mini-chat-compose">
        <input
          v-model="messageInput"
          type="text"
          class="mini-chat-input"
          placeholder="Напиши повідомлення команді..."
          maxlength="2000"
          @input="onTyping"
          @keydown.enter.prevent="sendMessage"
        />

        <button class="mini-chat-send" :disabled="!messageInput.trim()" @click="sendMessage">
          НАДІСЛАТИ
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.party-mini-chat {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding-inline: 15px;
  padding-top: 10px;
}

.mini-chat-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.mini-chat-kicker {
  font-family: var(--font-display);
  font-size: 12px;
  letter-spacing: 2px;
  color: var(--gray);
  margin-bottom: 4px;
}

.mini-chat-title {
  font-family: var(--font-display);
  font-size: 22px;
  letter-spacing: 2px;
  color: var(--yellow);
  line-height: 1;
  margin-bottom: 4px;
}

.mini-chat-meta {
  color: var(--gray-light);
  font-size: 13px;
  letter-spacing: 0.5px;
}

.typing-indicator {
  color: var(--yellow);
}

.mini-chat-open {
  flex-shrink: 0;
  border: 2px solid var(--border);
  background: transparent;
  color: var(--gray-light);
  font-family: var(--font-display);
  font-size: 12px;
  letter-spacing: 2px;
  padding: 10px 16px;
  transition: all 0.18s ease;
}

.mini-chat-open:hover {
  border-color: var(--yellow-dim);
  color: var(--yellow);
}

.mini-chat-locked,
.mini-chat-state {
  border: 2px solid var(--border);
  background: var(--panel-light);
  padding: 18px;
}

.mini-chat-locked {
  display: flex;
  align-items: center;
  gap: 16px;
}

.mini-chat-locked-icon {
  width: 48px;
  height: 48px;
  border: 2px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  background: rgba(245, 197, 24, 0.04);
  flex-shrink: 0;
}

.mini-chat-locked h4 {
  font-family: var(--font-display);
  font-size: 18px;
  letter-spacing: 1px;
  color: var(--white);
  margin-bottom: 4px;
}

.mini-chat-locked p,
.mini-chat-state {
  color: var(--gray-light);
  font-size: 14px;
  line-height: 1.5;
}

.mini-chat-state--error {
  border-color: var(--red-dim);
  color: #f1b0aa;
  background: rgba(192, 57, 43, 0.08);
}

.mini-chat-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.mini-chat-messages {
  max-height: 360px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.mini-chat-message {
  display: flex;
}

.mini-chat-message.own {
  justify-content: flex-end;
}

.mini-chat-message.system {
  justify-content: center;
}

.mini-chat-system {
  padding: 8px 12px;
  border: 1px dashed var(--border);
  color: var(--gray);
  font-size: 12px;
  letter-spacing: 0.8px;
  text-align: center;
  background: rgba(255, 255, 255, 0.02);
}

.mini-chat-bubble {
  display: flex;
  gap: 10px;
  max-width: min(100%, 560px);
}

.mini-chat-message.own .mini-chat-bubble {
  background: rgba(245, 197, 24, 0.08);
  border: 2px solid rgba(245, 197, 24, 0.2);
  padding: 12px 14px;
}

.mini-chat-message:not(.own) .mini-chat-bubble {
  background: var(--panel-light);
  border: 2px solid var(--border);
  padding: 12px 14px;
}

.mini-chat-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border);
  flex-shrink: 0;
}

.mini-chat-content-wrap {
  min-width: 0;
}

.mini-chat-message-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.mini-chat-sender {
  font-family: var(--font-display);
  font-size: 13px;
  letter-spacing: 1.5px;
  color: var(--yellow);
}

.mini-chat-time {
  color: var(--gray);
  font-size: 11px;
  letter-spacing: 0.8px;
}

.mini-chat-content {
  color: var(--white);
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.mini-chat-content.is-deleted {
  color: var(--gray);
  font-style: italic;
}

.mini-chat-compose {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
}

.mini-chat-input {
  width: 100%;
  min-height: 48px;
  border: 2px solid var(--border);
  background: var(--dark);
  color: var(--white);
  padding: 0 14px;
  font-family: var(--font-body);
  font-size: 15px;
  letter-spacing: 0.4px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.mini-chat-input:focus {
  outline: none;
  border-color: var(--yellow-dim);
  box-shadow: 0 0 0 1px rgba(245, 197, 24, 0.16);
}

.mini-chat-input::placeholder {
  color: var(--gray);
}

.mini-chat-send {
  border: 2px solid var(--yellow);
  background: var(--yellow);
  color: var(--black);
  padding: 0 18px;
  min-height: 48px;
  font-family: var(--font-display);
  font-size: 13px;
  letter-spacing: 2px;
  transition: all 0.18s ease;
}

.mini-chat-send:hover:not(:disabled) {
  background: transparent;
  color: var(--yellow);
}

.mini-chat-send:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

@media (max-width: 700px) {
  .mini-chat-head {
    flex-direction: column;
    align-items: stretch;
  }

  .mini-chat-open {
    width: 100%;
  }

  .mini-chat-compose {
    grid-template-columns: 1fr;
  }

  .mini-chat-send {
    width: 100%;
  }

  .mini-chat-locked {
    align-items: flex-start;
  }
}
</style>
