<script setup lang="ts">
import { nextTick, onMounted, ref, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useChatStore } from '../stores/chat'
import { useStompClient } from '../composables/useStompClient'
import { PUBLIC_BASE_URL } from '../config'
import type { ChatMessage } from '../types'

const props = defineProps<{
  chatId: number
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
const pendingOwnScroll = ref(false)

const visibleMessages = computed(() => {
  if (chatStore.activeChat?.id !== props.chatId) return []
  return chatStore.messages.slice(-12)
})

const canUseMiniChat = computed(() => props.isLoggedIn && props.isMember)
const isTyping = computed(() => chatStore.isPartnerTyping(props.chatId))
const typingName = computed(() => chatStore.typingDisplayName(props.chatId))

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
  void router.push(`/chat?groupId=${props.chatId}`)
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

  pendingOwnScroll.value = true
  messageInput.value = ''
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
  pendingOwnScroll.value = false
  void ensureChatLoaded()
})

watch(() => canUseMiniChat.value, (canUse) => {
  if (canUse) {
    void ensureChatLoaded()
  }
})

watch(() => visibleMessages.value, (msgs) => {
  const lastMessage = msgs[msgs.length - 1]
  if (!lastMessage) return

  if (pendingOwnScroll.value && isOwnMessage(lastMessage)) {
    pendingOwnScroll.value = false
    scrollToBottom(true)
    return
  }

  scrollToBottom()
}, { deep: false })

onMounted(() => {
  void ensureChatLoaded()
})
</script>

<template>
  <div class="party-mini-chat">
    <div class="mini-chat-head">
      <div>
        <div class="mini-chat-kicker">Чат лобі</div>
        <h3 class="mini-chat-title">Міні-чат</h3>
        <p class="mini-chat-meta">
          Командний канал для швидкої переписки
          <span v-if="isTyping" class="typing-indicator">• {{ typingName }} друкує...</span>
        </p>
      </div>

      <button type="button" class="mini-chat-open" @click="openFullChat">Повний чат</button>
    </div>

    <div v-if="!isLoggedIn" class="mini-chat-locked">
      <div class="mini-chat-locked-icon">🔑</div>
      <div>
        <h4>Увійди, щоб побачити чат</h4>
        <p>Текстовий канал лобі доступний після авторизації.</p>
      </div>
    </div>

    <div v-else-if="!isMember" class="mini-chat-locked">
      <div class="mini-chat-locked-icon">🎮</div>
      <div>
        <h4>Приєднайся до лобі</h4>
        <p>Міні-чат відкривається тільки для учасників команди.</p>
      </div>
    </div>

    <div v-else class="mini-chat-body">
      <div v-if="loading" class="mini-chat-state">Завантаження чату...</div>
      <div v-else-if="initError" class="mini-chat-state mini-chat-state--error">{{ initError }}</div>

      <div v-else-if="!visibleMessages.length" class="mini-chat-state mini-chat-state--empty">
        Тут поки тихо. Напиши перше повідомлення команді.
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

        <button type="button" class="mini-chat-send" :disabled="!messageInput.trim()" @click="sendMessage">
          Надіслати
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.party-mini-chat {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 0;
}

.mini-chat-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 18px 18px 14px;
  border-bottom: 1px solid var(--border);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.025), rgba(255, 255, 255, 0));
}

.mini-chat-kicker {
  font-family: var(--font-display);
  font-size: 12px;
  letter-spacing: 1.7px;
  text-transform: uppercase;
  color: var(--gray);
}

.mini-chat-title {
  margin: 4px 0 0;
  font-family: var(--font-display);
  font-size: 30px;
  line-height: 0.95;
  color: var(--white);
}

.mini-chat-meta {
  margin-top: 8px;
  color: var(--gray-light);
  font-size: 13px;
}

.typing-indicator {
  color: var(--yellow);
}

.mini-chat-open {
  flex-shrink: 0;
  min-height: 40px;
  padding: 9px 13px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.03);
  color: var(--white);
  font-family: var(--font-display);
  font-size: 15px;
  letter-spacing: 0.9px;
  text-transform: uppercase;
  transition: border-color 0.15s ease, color 0.15s ease, background 0.15s ease;
}

.mini-chat-open:hover {
  border-color: var(--yellow-dim);
  color: var(--yellow);
}

.mini-chat-body,
.mini-chat-locked {
  min-height: 0;
}

.mini-chat-body {
  display: grid;
  grid-template-rows: 1fr auto;
  gap: 12px;
  padding: 16px 18px 18px;
}

.mini-chat-locked,
.mini-chat-state {
  margin: 16px 18px 18px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.03);
}

.mini-chat-locked {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
}

.mini-chat-locked-icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(245, 197, 24, 0.28);
  background: rgba(245, 197, 24, 0.08);
  font-size: 18px;
  flex-shrink: 0;
}

.mini-chat-locked h4 {
  margin: 0 0 4px;
  font-family: var(--font-display);
  font-size: 22px;
  line-height: 1;
  color: var(--white);
}

.mini-chat-locked p,
.mini-chat-state {
  margin: 0;
  color: var(--gray-light);
  font-size: 14px;
  line-height: 1.5;
}

.mini-chat-state {
  display: grid;
  place-items: center;
  padding: 18px;
  text-align: center;
}

.mini-chat-state--error {
  border-color: rgba(255, 107, 107, 0.35);
  background: rgba(255, 107, 107, 0.08);
  color: #ffc0c0;
}

.mini-chat-state--empty {
  color: var(--gray-light);
}

.mini-chat-messages {
  min-height: 0;
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
  border: 1px solid rgba(245, 197, 24, 0.2);
  padding: 12px 14px;
}

.mini-chat-message:not(.own) .mini-chat-bubble {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border);
  padding: 12px 14px;
}

.mini-chat-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--border);
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
  letter-spacing: 1px;
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
  min-height: 46px;
  border: 1px solid var(--border);
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
  border: 1px solid var(--yellow);
  background: var(--yellow);
  color: var(--black);
  padding: 0 18px;
  min-height: 46px;
  font-family: var(--font-display);
  font-size: 15px;
  letter-spacing: 0.9px;
  text-transform: uppercase;
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
}
</style>
