<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
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
import type { ChatMessage, PinnedMessageInfo, MessageReadByUser, MessageAttachment } from '../types'

const router = useRouter()
const chatStore = useChatStore()
const auth = useAuthStore()
const stomp = useStompClient()
const achievementStore = useAchievementStore()

const PUBLIC_BASE_URL = API_BASE_URL.replace(/\/api\/?$/, '')
const DEFAULT_AVATAR_URL = `${PUBLIC_BASE_URL}/avatars/default/avatar-1.png`

const showGroupSettings = ref(false)


const NOT_WHAT_YOU_EXPECTED_CODE = 'NOT_WHAT_YOU_EXPECTED'

const secretPlaceholderLocked = ref(false)

const shouldRenderSecretPlaceholder = computed(() => (
  achievementStore.hasLoadedMyAchievements
    && (secretPlaceholderLocked.value || !achievementStore.hasAchievement(NOT_WHAT_YOU_EXPECTED_CODE))
))

function resolveAvatar(url: string | null): string {
  const value = url?.trim()
  if (!value) return DEFAULT_AVATAR_URL
  if (/^(https?:)?\/\//.test(value) || value.startsWith('data:') || value.startsWith('blob:')) return value
  return PUBLIC_BASE_URL + (value.startsWith('/') ? value : `/${value}`)
}

function onAvatarError(event: Event) {
  const target = event.target as HTMLImageElement | null
  if (!target || target.src === DEFAULT_AVATAR_URL) return
  target.src = DEFAULT_AVATAR_URL
}

const messageInput = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const initialChatScrollDone = ref(false)
const typingTimeout = ref<ReturnType<typeof setTimeout> | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const selectedFiles = ref<SelectedFilePreview[]>([])
const uploadError = ref('')
const uploadingMessage = ref(false)
const dragDepth = ref(0)
const selectedPreview = ref<SelectedFilePreview | null>(null)
const attachmentPreview = ref<MessageAttachment | null>(null)
const textPreviewAttachment = ref<MessageAttachment | null>(null)
const textPreviewContent = ref('')
const textPreviewLoading = ref(false)
const textPreviewError = ref('')
const deleteConfirmMessage = ref<ChatMessage | null>(null)

type PreviewKind = 'IMAGE' | 'GIF' | 'VIDEO' | 'AUDIO' | 'TEXT_FILE' | 'PDF' | 'FILE'

interface SelectedFilePreview {
  file: File
  url: string | null
  kind: PreviewKind
  error: string | null
  durationSeconds: number | null
  textPreview: string | null
}

const MAX_SELECTED_FILES = 5
const MAX_TOTAL_SIZE = 100 * 1024 * 1024
const FILE_LIMITS: Record<PreviewKind, number> = {
  IMAGE: 10 * 1024 * 1024,
  GIF: 20 * 1024 * 1024,
  VIDEO: 100 * 1024 * 1024,
  AUDIO: 30 * 1024 * 1024,
  TEXT_FILE: 10 * 1024 * 1024,
  PDF: 10 * 1024 * 1024,
  FILE: 10 * 1024 * 1024,
}
const MAX_TEXT_PREVIEW_CHARS = 200000

const contextMenu = ref<{ show: boolean; x: number; y: number; msg: ChatMessage | null }>({
  show: false, x: 0, y: 0, msg: null
})
const contextMenuEl = ref<HTMLElement | null>(null)
const contextMenuRect = ref({ left: 0, right: 0, top: 0, bottom: 0, height: 0 })

const readByPreview = ref<{
  messageId: number | null
  loading: boolean
  users: MessageReadByUser[]
}>({
  messageId: null,
  loading: false,
  users: [],
})

const readByPanel = ref<{
  show: boolean
  loading: boolean
  users: MessageReadByUser[]
  error: string
  msg: ChatMessage | null
}>({
  show: false,
  loading: false,
  users: [],
  error: '',
  msg: null,
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
const dragOverlayVisible = computed(() => dragDepth.value > 0)
const canDropFiles = computed(() => !!chatStore.activeChat && !chatStore.editingMessage && !uploadingMessage.value)
const dropOverlayTitle = computed(() => {
  if (chatStore.editingMessage) return 'Редагування повідомлення'
  if (uploadingMessage.value) return 'Файли вже надсилаються'
  return 'Відпустіть файли'
})
const dropOverlayText = computed(() => {
  if (chatStore.editingMessage) return 'Файли не можна додавати під час редагування повідомлення.'
  if (uploadingMessage.value) return 'Дочекайтесь завершення поточного завантаження.'
  return `Додайте до ${MAX_SELECTED_FILES} файлів у прев'ю перед відправкою.`
})

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
  clearSelectedFiles()
  uploadError.value = ''
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

function scrollElementIntoMessages(targetEl: Element, behavior: ScrollBehavior = 'smooth') {
  const el = messagesContainer.value
  if (!el) return

  const target = targetEl as HTMLElement
  const containerRect = el.getBoundingClientRect()
  const targetRect = target.getBoundingClientRect()
  const centeredTop = el.scrollTop
    + targetRect.top
    - containerRect.top
    - (el.clientHeight - targetRect.height) / 2

  el.scrollTo({
    top: Math.max(0, centeredTop),
    behavior,
  })
}

function scrollToReadPosition() {
  nextTick(() => {
    const el = messagesContainer.value
    if (!el) return

    const lastReadId = chatStore.activeChat?.lastReadMessageId
    if (!lastReadId) {
      el.scrollTop = el.scrollHeight
      initialChatScrollDone.value = true
      return
    }

    const readIndex = chatStore.messages.findIndex(message => message.id === lastReadId)
    const target = readIndex >= 0
      ? chatStore.messages[Math.min(readIndex + 1, chatStore.messages.length - 1)]
      : null

    const targetEl = target
      ? el.querySelector(`[data-msg-id="${target.id}"]`)
      : el.querySelector(`[data-msg-id="${lastReadId}"]`)

    if (targetEl) {
      scrollElementIntoMessages(targetEl, 'auto')
    } else {
      el.scrollTop = el.scrollHeight
    }
    initialChatScrollDone.value = true
  })
}

watch(() => chatStore.messages.length, (length) => {
  if (!length) return
  if (!initialChatScrollDone.value) {
    scrollToReadPosition()
    return
  }
  scrollToBottom()
})

watch(() => chatStore.activeChat?.id, () => {
  initialChatScrollDone.value = false
})

async function sendMessage() {
  const text = messageInput.value.trim()
  const files = selectedFiles.value.filter(item => !item.error).map(item => item.file)
  if ((!text && files.length === 0) || !chatStore.activeChat || uploadingMessage.value) return

  if (chatStore.editingMessage) {
    if (!text) return
    chatStore.editMessageAction(chatStore.editingMessage.id, text)
    messageInput.value = ''
    return
  }

  const replyToId = chatStore.replyingTo?.id ?? null

  if (files.length > 0) {
    uploadError.value = ''
    uploadingMessage.value = true
    try {
      await chatStore.sendMessageWithAttachments(chatStore.activeChat.id, text, files, replyToId)
      messageInput.value = ''
      clearSelectedFiles()
      chatStore.setReplyingTo(null)
      scrollToBottom(true)
    } catch (e: any) {
      uploadError.value = e?.response?.data?.message || 'Не вдалося надіслати файли'
    } finally {
      uploadingMessage.value = false
    }
    return
  }

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

function openFilePicker() {
  if (chatStore.editingMessage || uploadingMessage.value) return
  fileInput.value?.click()
}

function onFilesSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  uploadError.value = ''
  addSelectedFiles(files)
  input.value = ''
}

function addSelectedFiles(files: File[]) {
  if (!files.length) return

  const slots = MAX_SELECTED_FILES - selectedFiles.value.length
  if (slots <= 0) {
    uploadError.value = `Можна додати максимум ${MAX_SELECTED_FILES} файлів`
    return
  }

  if (files.length > slots) {
    uploadError.value = `Додано тільки ${slots} з ${files.length} файлів. Ліміт - ${MAX_SELECTED_FILES}.`
  }

  const next = files.slice(0, slots).map(createSelectedPreview)
  const totalSize = [...selectedFiles.value, ...next].reduce((sum, item) => sum + item.file.size, 0)
  if (totalSize > MAX_TOTAL_SIZE) {
    next.forEach(revokePreviewUrl)
    uploadError.value = 'Сумарний розмір файлів не має перевищувати 100 MB'
    return
  }

  selectedFiles.value = [...selectedFiles.value, ...next]
  next.forEach((item) => {
    if (item.kind === 'TEXT_FILE' && item.file.size <= 256 * 1024) {
      readTextPreview(item)
    }
  })
}

function dataTransferHasFiles(dataTransfer: DataTransfer | null): boolean {
  if (!dataTransfer) return false
  return Array.from(dataTransfer.types || []).includes('Files')
}

function setDragDropEffect(event: DragEvent) {
  if (!event.dataTransfer) return
  event.dataTransfer.dropEffect = canDropFiles.value ? 'copy' : 'none'
}

function resetFileDragState() {
  dragDepth.value = 0
}

function onChatDragEnter(event: DragEvent) {
  if (!dataTransferHasFiles(event.dataTransfer)) return
  event.preventDefault()
  event.stopPropagation()
  dragDepth.value += 1
  setDragDropEffect(event)
}

function onChatDragOver(event: DragEvent) {
  if (!dataTransferHasFiles(event.dataTransfer)) return
  event.preventDefault()
  event.stopPropagation()
  setDragDropEffect(event)
}

function onChatDragLeave(event: DragEvent) {
  if (!dataTransferHasFiles(event.dataTransfer)) return
  event.preventDefault()
  event.stopPropagation()
  dragDepth.value = Math.max(0, dragDepth.value - 1)
}

function onChatDrop(event: DragEvent) {
  if (!dataTransferHasFiles(event.dataTransfer)) return
  event.preventDefault()
  event.stopPropagation()
  resetFileDragState()

  const files = Array.from(event.dataTransfer?.files || [])
  if (!files.length) return

  if (chatStore.editingMessage) {
    uploadError.value = 'Файли не можна додавати під час редагування повідомлення'
    return
  }

  if (uploadingMessage.value) {
    uploadError.value = 'Дочекайтесь завершення поточного завантаження'
    return
  }

  uploadError.value = ''
  addSelectedFiles(files)
}

function readTextPreview(item: SelectedFilePreview) {
  const reader = new FileReader()
  reader.onload = () => {
    const text = String(reader.result || '')
      .replace(/\r\n/g, '\n')
      .split('\n')
      .slice(0, 4)
      .join('\n')
      .slice(0, 400)
    selectedFiles.value = selectedFiles.value.map(entry =>
      entry === item ? { ...entry, textPreview: text } : entry
    )
  }
  reader.readAsText(item.file)
}

function createSelectedPreview(file: File): SelectedFilePreview {
  const kind = detectPreviewKind(file)
  const limit = FILE_LIMITS[kind]
  const error = file.size > limit ? `Файл завеликий: максимум ${formatBytes(limit)}` : null
  const needsUrl = kind === 'IMAGE' || kind === 'GIF' || kind === 'VIDEO' || kind === 'AUDIO'

  return {
    file,
    kind,
    error,
    url: needsUrl ? URL.createObjectURL(file) : null,
    durationSeconds: null,
    textPreview: null,
  }
}

function detectPreviewKind(file: File): PreviewKind {
  const type = file.type.toLowerCase()
  const name = file.name.toLowerCase()
  if (type === 'image/gif' || name.endsWith('.gif')) return 'GIF'
  if (type.startsWith('image/')) return 'IMAGE'
  if (type.startsWith('video/')) return 'VIDEO'
  if (type.startsWith('audio/')) return 'AUDIO'
  if (type === 'application/pdf' || name.endsWith('.pdf')) return 'PDF'
  if (
    type.startsWith('text/')
    || type === 'application/json'
    || name.endsWith('.txt')
    || name.endsWith('.md')
    || name.endsWith('.log')
    || name.endsWith('.csv')
    || name.endsWith('.json')
    || name.endsWith('.xml')
  ) return 'TEXT_FILE'
  return 'FILE'
}

function removeSelectedFile(index: number) {
  const item = selectedFiles.value[index]
  if (item) revokePreviewUrl(item)
  selectedFiles.value = selectedFiles.value.filter((_, i) => i !== index)
  uploadError.value = ''
}

function clearSelectedFiles() {
  selectedFiles.value.forEach(revokePreviewUrl)
  selectedFiles.value = []
}

function revokePreviewUrl(item: SelectedFilePreview) {
  if (item.url) URL.revokeObjectURL(item.url)
}

function onPreviewMetadata(index: number, event: Event) {
  const target = event.target as HTMLMediaElement | null
  const duration = target?.duration
  if (!duration || !Number.isFinite(duration)) return
  const item = selectedFiles.value[index]
  if (!item) return
  selectedFiles.value = selectedFiles.value.map((entry, i) =>
    i === index ? { ...entry, durationSeconds: duration } : entry
  )
}

function mediaDisplayStyle(attachment: MessageAttachment, compact = false): Record<string, string> {
  const sourceWidth = attachment.width && attachment.width > 0 ? attachment.width : 320
  const sourceHeight = attachment.height && attachment.height > 0 ? attachment.height : 240
  const ratio = Math.min(Math.max(sourceWidth / sourceHeight, 0.3), 4)
  const maxWidth = compact ? 230 : attachment.mediaType === 'VIDEO' ? 480 : 420
  const maxHeight = compact ? 190 : 360

  let displayWidth = Math.min(sourceWidth, maxWidth)
  let displayHeight = displayWidth / ratio

  if (displayHeight > maxHeight) {
    displayHeight = maxHeight
    displayWidth = displayHeight * ratio
  }

  return {
    width: `${Math.round(displayWidth)}px`,
    aspectRatio: `${sourceWidth} / ${sourceHeight}`,
  }
}

function attachmentGroupClass(count: number): Record<string, boolean> {
  return {
    multi: count > 1,
    [`count-${Math.min(count, MAX_SELECTED_FILES)}`]: true,
  }
}

function openSelectedPreview(item: SelectedFilePreview) {
  if (!item.url) return
  if (item.kind !== 'IMAGE' && item.kind !== 'GIF' && item.kind !== 'VIDEO') return
  selectedPreview.value = item
}

function openAttachmentPreview(attachment: MessageAttachment) {
  if (!isVisualAttachment(attachment) && !isVideoAttachment(attachment)) return
  attachmentPreview.value = attachment
}

async function openTextAttachment(attachment: MessageAttachment) {
  textPreviewAttachment.value = attachment
  textPreviewContent.value = ''
  textPreviewError.value = ''
  textPreviewLoading.value = true

  try {
    const response = await fetch(attachment.url)
    if (!response.ok) throw new Error('Failed to load text file')

    const text = await response.text()
    textPreviewContent.value = text.length > MAX_TEXT_PREVIEW_CHARS
      ? `${text.slice(0, MAX_TEXT_PREVIEW_CHARS)}\n\n...`
      : text
  } catch {
    textPreviewError.value = 'РќРµ РІРґР°Р»РѕСЃСЏ РІС–РґРєСЂРёС‚Рё С‚РµРєСЃС‚РѕРІРёР№ С„Р°Р№Р». Р№РѕРіРѕ РјРѕР¶РЅР° СЃРєР°С‡Р°С‚Рё.'
  } finally {
    textPreviewLoading.value = false
  }
}

function closeMediaPreview() {
  selectedPreview.value = null
  attachmentPreview.value = null
}

function closeTextPreview() {
  textPreviewAttachment.value = null
  textPreviewContent.value = ''
  textPreviewError.value = ''
  textPreviewLoading.value = false
}

function isVisualAttachment(attachment: MessageAttachment): boolean {
  return attachment.mediaType === 'IMAGE' || attachment.mediaType === 'GIF'
}

function isVideoAttachment(attachment: MessageAttachment): boolean {
  return attachment.mediaType === 'VIDEO'
}

function isAudioAttachment(attachment: MessageAttachment): boolean {
  return attachment.mediaType === 'AUDIO'
}

function isPdfAttachment(attachment: MessageAttachment): boolean {
  return attachment.mediaType === 'PDF'
}

function fileExtension(filename: string | null | undefined): string {
  const name = filename?.trim().toLowerCase()
  if (!name) return ''
  const clean = name.split(/[\\/]/).pop() || ''
  const dot = clean.lastIndexOf('.')
  if (dot < 0 || dot === clean.length - 1) return ''
  return clean.slice(dot + 1)
}

function readableFileType(filename: string | null | undefined, contentType: string | null | undefined, fallback = 'FILE'): string {
  const ext = fileExtension(filename)
  const type = contentType?.toLowerCase() || ''

  if (ext === 'pdf' || type === 'application/pdf') return 'PDF'
  if (ext === 'json' || type === 'application/json') return 'JSON'
  if (ext === 'csv' || type === 'text/csv') return 'CSV'
  if (ext === 'md' || type === 'text/markdown') return 'MD'
  if (ext === 'xml' || type === 'application/xml' || type === 'text/xml') return 'XML'
  if (ext === 'txt' || ext === 'log' || type === 'text/plain') return 'TXT'
  if (/^[a-z0-9]{1,5}$/.test(ext)) return ext.toUpperCase()
  return fallback
}

function attachmentTypeLabel(attachment: MessageAttachment): string {
  if (attachment.mediaType === 'PDF') return 'PDF'
  if (attachment.mediaType === 'TEXT_FILE') {
    return readableFileType(attachment.originalFilename, attachment.contentType, 'TXT')
  }
  return readableFileType(attachment.originalFilename, attachment.contentType, 'FILE')
}

function selectedFileTypeLabel(item: SelectedFilePreview): string {
  if (item.kind === 'PDF') return 'PDF'
  if (item.kind === 'TEXT_FILE') return readableFileType(item.file.name, item.file.type, 'TXT')
  return readableFileType(item.file.name, item.file.type, item.kind === 'FILE' ? 'FILE' : item.kind)
}

function formatBytes(bytes: number): string {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let value = bytes
  let unit = 0
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024
    unit++
  }
  return `${value >= 10 || unit === 0 ? value.toFixed(0) : value.toFixed(1)} ${units[unit]}`
}

function formatDuration(seconds: number | null | undefined): string {
  if (!seconds || !Number.isFinite(seconds)) return ''
  const total = Math.round(seconds)
  const mins = Math.floor(total / 60)
  const secs = total % 60
  return `${mins}:${secs.toString().padStart(2, '0')}`
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

async function navigateToPartnerProfile() {
  const chat = chatStore.activeChat
  if (!chat || chat.isGroup) return
  const userId = chat.partnerUserId ?? await chatStore.resolvePartnerUserId(chat.id)
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
  const menuW = 220
  const menuH = 260
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

const activeMediaPreview = computed(() => {
  if (selectedPreview.value?.url) {
    return {
      kind: selectedPreview.value.kind,
      url: selectedPreview.value.url,
      title: selectedPreview.value.file.name,
    }
  }

  if (attachmentPreview.value) {
    return {
      kind: attachmentPreview.value.mediaType,
      url: attachmentPreview.value.url,
      title: attachmentPreview.value.originalFilename || attachmentPreview.value.mediaType,
    }
  }

  return null
})

const readByPanelStyle = computed(() => {
  const panelW = 300
  const rect = contextMenuRect.value

  let x = rect.left - panelW
  if (x < 8) {
    x = rect.right
  }
  if (x + panelW > window.innerWidth - 8) {
    x = window.innerWidth - panelW - 8
  }
  if (x < 8) x = 8

  let y = rect.bottom
  if (y > window.innerHeight - 8) y = window.innerHeight - 8
  if (y < 120) y = 120

  return { top: `${y}px`, left: `${x}px` }
})

const readByPreviewAvatars = computed(() => readByPreview.value.users.slice(0, 3))

const readByPreviewText = computed(() => {
  if (readByPreview.value.loading) return 'ЗАВАНТАЖЕННЯ'
  const count = readByPreview.value.users.length
  if (count === 1) return '1 ПЕРЕГЛЯД'
  return `${count} ПЕРЕГЛЯДИ`
})

let readObserver: IntersectionObserver | null = null
let readMarkTimer: ReturnType<typeof setTimeout> | null = null
const visibleReadCandidates = new Set<number>()
const lastMarkedByChat = new Map<number, number>()

function openContextMenu(event: MouseEvent, msg: ChatMessage) {
  if (msg.deleted || msg.system) return
  event.preventDefault()
  contextMenu.value = { show: true, x: event.clientX, y: event.clientY, msg }
  readByPanel.value = { show: false, loading: false, users: [], error: '', msg: null }
  nextTick(updateContextMenuRect)
  if (isOwnMessage(msg.senderEmail)) {
    void loadReadByPreview(msg)
  } else {
    readByPreview.value = { messageId: null, loading: false, users: [] }
  }
}

function closeContextMenu() {
  contextMenu.value = { show: false, x: 0, y: 0, msg: null }
  readByPreview.value = { messageId: null, loading: false, users: [] }
  readByPanel.value = { show: false, loading: false, users: [], error: '', msg: null }
}

async function loadReadByPreview(msg: ChatMessage) {
  readByPreview.value = { messageId: msg.id, loading: true, users: [] }
  try {
    const users = await chatStore.fetchMessageReadBy(msg.id)
    if (readByPreview.value.messageId === msg.id) {
      readByPreview.value = { messageId: msg.id, loading: false, users }
    }
  } catch {
    if (readByPreview.value.messageId === msg.id) {
      readByPreview.value = { messageId: msg.id, loading: false, users: [] }
    }
  }
}

async function openReadByPanel(msg: ChatMessage) {
  readByPanel.value = {
    show: true,
    loading: true,
    users: readByPreview.value.messageId === msg.id ? readByPreview.value.users : [],
    error: '',
    msg,
  }
  await nextTick()
  updateContextMenuRect()

  try {
    const users = await chatStore.fetchMessageReadBy(msg.id)
    if (readByPanel.value.msg?.id === msg.id) {
      readByPanel.value = { show: true, loading: false, users, error: '', msg }
      readByPreview.value = { messageId: msg.id, loading: false, users }
    }
  } catch {
    if (readByPanel.value.msg?.id === msg.id) {
      readByPanel.value = {
        show: true,
        loading: false,
        users: [],
        error: 'Не вдалося завантажити перегляди',
        msg,
      }
    }
  }
}

function updateContextMenuRect() {
  const rect = contextMenuEl.value?.getBoundingClientRect()
  if (!rect) return
  contextMenuRect.value = {
    left: rect.left,
    right: rect.right,
    top: rect.top,
    bottom: rect.bottom,
    height: rect.height,
  }
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
  closeContextMenu()
  deleteConfirmMessage.value = msg
}

function cancelDeleteMessage() {
  deleteConfirmMessage.value = null
}

function confirmDeleteMessage() {
  if (!deleteConfirmMessage.value) return
  chatStore.deleteMessage(deleteConfirmMessage.value.id)
  deleteConfirmMessage.value = null
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
    scrollElementIntoMessages(msgEl)
    msgEl.classList.add('highlight-msg')
    setTimeout(() => msgEl.classList.remove('highlight-msg'), 2000)
  }
}

function onWindowClick() {
  closeContextMenu()
  emojiPickerMsg.value = null
  showPinnedList.value = false
  readByPanel.value = { show: false, loading: false, users: [], error: '', msg: null }
}

function onWindowKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') resetFileDragState()
}

function setupReadObserver() {
  readObserver?.disconnect()
  visibleReadCandidates.clear()

  const container = messagesContainer.value
  if (!container || !chatStore.activeChat) return

  readObserver = new IntersectionObserver((entries) => {
    for (const entry of entries) {
      const id = Number((entry.target as HTMLElement).dataset.msgId)
      if (!id) continue
      if (entry.isIntersecting) {
        visibleReadCandidates.add(id)
      } else {
        visibleReadCandidates.delete(id)
      }
    }
    scheduleReadMark()
  }, {
    root: container,
    threshold: 0.6,
  })

  container.querySelectorAll<HTMLElement>('[data-msg-id]').forEach((el) => {
    readObserver?.observe(el)
  })
}

function scheduleReadMark() {
  if (readMarkTimer) clearTimeout(readMarkTimer)
  readMarkTimer = setTimeout(markLowestVisibleAsRead, 250)
}

function markLowestVisibleAsRead() {
  const chatId = chatStore.activeChat?.id
  if (!chatId || visibleReadCandidates.size === 0) return

  const visibleIds = [...visibleReadCandidates]
  const visibleMessages = chatStore.messages
    .filter(m => visibleIds.includes(m.id))
    .sort((a, b) => {
      const timeDiff = new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime()
      return timeDiff !== 0 ? timeDiff : a.id - b.id
    })
  const message = visibleMessages[visibleMessages.length - 1]

  if (!message) return

  const lastMarked = lastMarkedByChat.get(chatId) ?? 0
  if (message.id <= lastMarked) return

  lastMarkedByChat.set(chatId, message.id)
  chatStore.markReadUpTo(chatId, message.id)
}

watch(() => chatStore.editingMessage, (msg) => {
  if (msg) messageInput.value = msg.content
})

watch(() => [chatStore.activeChat?.id, chatStore.messages.length], () => {
  nextTick(setupReadObserver)
})

onMounted(() => {
  if (!achievementStore.hasLoadedMyAchievements && !achievementStore.loading) {
    void achievementStore.fetchMyAchievements()
  }
  window.addEventListener('keydown', onWindowKeydown)
  window.addEventListener('blur', resetFileDragState)
  window.addEventListener('drop', resetFileDragState)
  window.addEventListener('dragend', resetFileDragState)
  nextTick(setupReadObserver)
})

onBeforeUnmount(() => {
  readObserver?.disconnect()
  if (readMarkTimer) clearTimeout(readMarkTimer)
  window.removeEventListener('keydown', onWindowKeydown)
  window.removeEventListener('blur', resetFileDragState)
  window.removeEventListener('drop', resetFileDragState)
  window.removeEventListener('dragend', resetFileDragState)
  clearSelectedFiles()
})
</script>

<template>
  <div
    class="chat-window"
    v-if="chatStore.activeChat"
    @click="onWindowClick"
    @dragenter="onChatDragEnter"
    @dragover="onChatDragOver"
    @dragleave="onChatDragLeave"
    @drop="onChatDrop"
  >
    <div
      v-if="dragOverlayVisible"
      class="chat-drop-overlay"
      :class="{ blocked: !canDropFiles }"
      @click.stop
    >
      <div class="chat-drop-panel">
        <div class="chat-drop-icon">
          <svg width="38" height="38" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="17 8 12 3 7 8"/>
            <line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
        </div>
        <div class="chat-drop-title">{{ dropOverlayTitle }}</div>
        <div class="chat-drop-text">{{ dropOverlayText }}</div>
      </div>
    </div>

    <div class="chat-window-header">
      <div class="cw-avatar" :class="{ group: chatType === 'GROUP', game: chatType === 'GAME' }" @click.stop="navigateToPartnerProfile">
        <img v-if="!isGroup" :src="resolveAvatar(chatStore.activeChat?.partnerAvatarUrl ?? null)" alt="" class="cw-avatar-img" @error="onAvatarError" />
        <img v-else-if="isGroup && chatStore.activeChat?.groupAvatarUrl" :src="resolveAvatar(chatStore.activeChat.groupAvatarUrl)" alt="" class="cw-avatar-img" @error="onAvatarError" />
        <span v-else class="cw-letter">{{ chatTitle.charAt(0).toUpperCase() }}</span>
      </div>
      <div class="cw-header-info">
        <div class="cw-name" @click.stop="navigateToPartnerProfile">
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
            @error="onAvatarError"
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

            <div
              v-if="!msg.deleted && msg.attachments?.length"
              class="chat-attachments"
              :class="attachmentGroupClass(msg.attachments.length)"
            >
              <div
                v-for="attachment in msg.attachments"
                :key="attachment.id"
                class="chat-attachment"
                :class="`type-${attachment.mediaType.toLowerCase()}`"
              >
                <button
                  v-if="isVisualAttachment(attachment)"
                  type="button"
                  class="attachment-visual"
                  :style="mediaDisplayStyle(attachment, msg.attachments.length > 1)"
                  @click.stop="openAttachmentPreview(attachment)"
                >
                  <img :src="attachment.url" :alt="attachment.originalFilename || attachment.mediaType" loading="lazy" />
                  <span v-if="attachment.mediaType === 'GIF'" class="attachment-badge">GIF</span>
                </button>

                <div
                  v-else-if="isVideoAttachment(attachment)"
                  class="attachment-video-wrap"
                  :style="mediaDisplayStyle(attachment, msg.attachments.length > 1)"
                >
                  <video :src="attachment.url" controls preload="metadata"></video>
                  <span v-if="attachment.durationSeconds" class="attachment-badge">{{ formatDuration(attachment.durationSeconds) }}</span>
                </div>

                <div v-else-if="isAudioAttachment(attachment)" class="attachment-audio">
                  <div class="attachment-file-icon">♪</div>
                  <div class="attachment-file-body">
                    <div class="attachment-file-name">{{ attachment.originalFilename || 'Audio' }}</div>
                    <div class="attachment-file-meta">
                      {{ formatBytes(attachment.sizeBytes) }}
                      <span v-if="attachment.durationSeconds">• {{ formatDuration(attachment.durationSeconds) }}</span>
                    </div>
                    <audio :src="attachment.url" controls preload="metadata"></audio>
                  </div>
                </div>

                <button
                  v-else-if="attachment.mediaType === 'TEXT_FILE'"
                  type="button"
                  class="attachment-file"
                  @click.stop="openTextAttachment(attachment)"
                >
                  <div class="attachment-file-icon">{{ attachmentTypeLabel(attachment) }}</div>
                  <div class="attachment-file-body">
                    <div class="attachment-file-name">{{ attachment.originalFilename || 'Text file' }}</div>
                    <div class="attachment-file-meta">{{ attachmentTypeLabel(attachment) }} • {{ formatBytes(attachment.sizeBytes) }}</div>
                  </div>
                </button>

                <a
                  v-else-if="isPdfAttachment(attachment)"
                  :href="attachment.url"
                  target="_blank"
                  rel="noopener"
                  class="attachment-file"
                >
                  <div class="attachment-file-icon">PDF</div>
                  <div class="attachment-file-body">
                    <div class="attachment-file-name">{{ attachment.originalFilename || 'PDF document' }}</div>
                    <div class="attachment-file-meta">PDF • {{ formatBytes(attachment.sizeBytes) }}</div>
                  </div>
                </a>

                <a
                  v-else
                  :href="attachment.url"
                  target="_blank"
                  rel="noopener"
                  :download="attachment.originalFilename || undefined"
                  class="attachment-file"
                >
                  <div class="attachment-file-icon">{{ attachmentTypeLabel(attachment) }}</div>
                  <div class="attachment-file-body">
                    <div class="attachment-file-name">{{ attachment.originalFilename || 'File' }}</div>
                    <div class="attachment-file-meta">{{ attachmentTypeLabel(attachment) }} • {{ formatBytes(attachment.sizeBytes) }}</div>
                  </div>
                </a>
              </div>
            </div>

            <div v-if="msg.deleted || msg.content" class="chat-msg-text" :class="{ 'deleted-text': msg.deleted }">
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
        ref="contextMenuEl"
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
        <template v-if="isOwnMessage(contextMenu.msg!.senderEmail)">
          <div class="ctx-divider"></div>
          <button class="ctx-item ctx-read-by" @click.stop="openReadByPanel(contextMenu.msg!)">
            <span class="ctx-read-checks">✓✓</span>
            <span class="ctx-read-label">{{ readByPreviewText }}</span>
            <span v-if="readByPreviewAvatars.length" class="ctx-read-avatars">
              <span
                v-for="user in readByPreviewAvatars"
                :key="user.userId"
                class="ctx-read-avatar"
              >
                <img :src="resolveAvatar(user.avatarUrl)" alt="" @error="onAvatarError" />
              </span>
            </span>
          </button>
        </template>
      </div>

      <div
        v-if="readByPanel.show && contextMenu.msg && readByPanel.msg?.id === contextMenu.msg.id"
        class="read-by-panel"
        :style="readByPanelStyle"
        @click.stop
      >
        <div class="read-by-title">ПРОЧИТАЛИ</div>
        <div v-if="readByPanel.loading" class="read-by-state">Завантаження...</div>
        <div v-else-if="readByPanel.error" class="read-by-state read-by-error">{{ readByPanel.error }}</div>
        <div v-else-if="readByPanel.users.length === 0" class="read-by-state">Ще ніхто не прочитав</div>
        <div v-else class="read-by-list">
          <div v-for="user in readByPanel.users" :key="user.userId" class="read-by-user">
            <div class="read-by-avatar">
              <img :src="resolveAvatar(user.avatarUrl)" alt="" @error="onAvatarError" />
            </div>
            <div class="read-by-info">
              <div class="read-by-name">{{ user.displayName }}</div>
              <div class="read-by-meta"><span>✓✓</span> прочитано</div>
            </div>
          </div>
        </div>
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

    <div v-if="selectedFiles.length || uploadError" class="chat-selected-files" @click.stop>
      <div v-if="selectedFiles.length" class="selected-files-grid">
        <div
          v-for="(item, index) in selectedFiles"
          :key="item.file.name + item.file.size + index"
          class="selected-file-card"
          :class="{ invalid: item.error }"
        >
          <button type="button" class="selected-file-remove" @click="removeSelectedFile(index)">×</button>

          <button
            v-if="item.kind === 'IMAGE' || item.kind === 'GIF'"
            type="button"
            class="selected-file-media selected-file-media-button"
            @click="openSelectedPreview(item)"
          >
            <img v-if="item.url" :src="item.url" :alt="item.file.name" />
            <span v-if="item.kind === 'GIF'" class="attachment-badge">GIF</span>
          </button>

          <button
            v-else-if="item.kind === 'VIDEO'"
            type="button"
            class="selected-file-media selected-file-media-button"
            @click="openSelectedPreview(item)"
          >
            <video v-if="item.url" :src="item.url" muted preload="metadata" @loadedmetadata="onPreviewMetadata(index, $event)"></video>
            <span v-if="item.durationSeconds" class="attachment-badge">{{ formatDuration(item.durationSeconds) }}</span>
          </button>

          <div v-else-if="item.kind === 'AUDIO'" class="selected-file-generic">
            <div class="selected-file-icon">♪</div>
            <audio v-if="item.url" :src="item.url" preload="metadata" @loadedmetadata="onPreviewMetadata(index, $event)"></audio>
          </div>

          <div v-else class="selected-file-generic" :class="{ text: item.kind === 'TEXT_FILE' }">
            <pre v-if="item.kind === 'TEXT_FILE' && item.textPreview" class="selected-text-preview">{{ item.textPreview }}</pre>
            <div v-else class="selected-file-icon">{{ selectedFileTypeLabel(item) }}</div>
          </div>

          <div class="selected-file-info">
            <div class="selected-file-name">{{ item.file.name }}</div>
            <div class="selected-file-meta">{{ selectedFileTypeLabel(item) }} • {{ formatBytes(item.file.size) }}<span v-if="item.durationSeconds"> • {{ formatDuration(item.durationSeconds) }}</span></div>
            <div v-if="item.error" class="selected-file-error">{{ item.error }}</div>
          </div>
        </div>
      </div>
      <div v-if="uploadError" class="chat-upload-error">{{ uploadError }}</div>
    </div>

    <div class="chat-input-bar">
      <input
        ref="fileInput"
        type="file"
        multiple
        class="chat-file-input"
        accept="image/*,video/*,audio/*,.txt,.md,.log,.csv,.json,.xml,.pdf,application/pdf"
        @change="onFilesSelected"
      />
      <button
        type="button"
        class="chat-attach-btn"
        :disabled="!!chatStore.editingMessage || uploadingMessage || selectedFiles.length >= 5"
        title="Attach files"
        @click.stop="openFilePicker"
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21.44 11.05 12.25 20.24a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"/></svg>
      </button>
      <input
        v-model="messageInput"
        @keydown.enter.prevent="sendMessage"
        @input="onTyping"
        type="text"
        class="chat-input"
        :placeholder="chatStore.editingMessage ? 'Редагувати повідомлення...' : 'Написати повідомлення...'"
        maxlength="5000"
      />
      <button class="chat-send-btn" @click="sendMessage" :disabled="uploadingMessage || (!messageInput.trim() && selectedFiles.length === 0)">
        {{ uploadingMessage ? 'НАДСИЛАННЯ...' : (chatStore.editingMessage ? 'ЗБЕРЕГТИ' : 'НАДІСЛАТИ') }}
      </button>
    </div>
  </div>

  <ChatEmptyStateSecret
    v-else-if="shouldRenderSecretPlaceholder"
    @scene-lock="secretPlaceholderLocked = true"
    @scene-unlock="secretPlaceholderLocked = false"
  />

  <div class="chat-window chat-placeholder" v-else>
    <ChatEmptyStateBase />
  </div>

  <Teleport to="body">
    <div v-if="activeMediaPreview" class="media-preview-overlay" @click="closeMediaPreview">
      <div class="media-preview-dialog" @click.stop>
        <button type="button" class="preview-close-btn" @click="closeMediaPreview">×</button>
        <video
          v-if="activeMediaPreview.kind === 'VIDEO'"
          :src="activeMediaPreview.url"
          controls
          autoplay
          class="media-preview-video"
        ></video>
        <img
          v-else
          :src="activeMediaPreview.url"
          :alt="activeMediaPreview.title"
          class="media-preview-image"
        />
        <div class="media-preview-title">{{ activeMediaPreview.title }}</div>
      </div>
    </div>
  </Teleport>

  <Teleport to="body">
    <div v-if="textPreviewAttachment" class="text-preview-overlay" @click="closeTextPreview">
      <div class="text-preview-dialog" @click.stop>
        <div class="text-preview-head">
          <div class="text-preview-title">
            {{ textPreviewAttachment.originalFilename || 'Text file' }}
          </div>
          <div class="text-preview-actions">
            <a
              :href="textPreviewAttachment.url"
              :download="textPreviewAttachment.originalFilename || undefined"
              class="text-preview-download"
            >
              DOWNLOAD
            </a>
            <button type="button" class="preview-close-btn inline" @click="closeTextPreview">×</button>
          </div>
        </div>
        <div v-if="textPreviewLoading" class="text-preview-state">Loading...</div>
        <div v-else-if="textPreviewError" class="text-preview-state error">{{ textPreviewError }}</div>
        <pre v-else class="text-preview-content">{{ textPreviewContent }}</pre>
      </div>
    </div>
  </Teleport>

  <Teleport to="body">
    <div v-if="deleteConfirmMessage" class="delete-confirm-overlay" @click.self="cancelDeleteMessage">
      <div class="delete-confirm-dialog">
        <div class="delete-confirm-title">Видалити повідомлення?</div>
        <div class="delete-confirm-message">
          Ви впевнені, що хочете видалити це повідомлення? Цю дію не можна буде скасувати.
        </div>
        <div class="delete-confirm-actions">
          <button type="button" class="delete-confirm-btn cancel" @click="cancelDeleteMessage">
            СКАСУВАТИ
          </button>
          <button type="button" class="delete-confirm-btn danger" @click="confirmDeleteMessage">
            ВИДАЛИТИ
          </button>
        </div>
      </div>
    </div>
  </Teleport>

  <GroupChatSettingsModal
    v-if="showGroupSettings && chatStore.activeChat"
    :chat="chatStore.activeChat"
    @close="showGroupSettings = false"
  />
</template>

<style scoped>

.chat-window {
  position: relative;
}

.chat-drop-overlay {
  position: absolute;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    linear-gradient(135deg, rgba(245,197,24,0.08), rgba(0,0,0,0.18)),
    rgba(10,10,11,0.72);
  backdrop-filter: blur(3px);
  pointer-events: none;
}

.chat-drop-panel {
  width: min(460px, 88vw);
  min-height: 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  border: 2px dashed var(--yellow-dim);
  border-top: 3px solid var(--yellow);
  background: rgba(17,17,20,0.92);
  box-shadow: 0 18px 60px rgba(0,0,0,0.55), inset 0 0 0 1px rgba(245,197,24,0.05);
  color: var(--yellow);
  text-align: center;
  padding: 28px;
}

.chat-drop-icon {
  display: grid;
  place-items: center;
  width: 74px;
  height: 74px;
  border: 1px solid rgba(245,197,24,0.24);
  background: linear-gradient(135deg, rgba(245,197,24,0.14), rgba(245,197,24,0.04));
  box-shadow: 0 0 22px rgba(245,197,24,0.1);
}

.chat-drop-title {
  font-family: var(--font-display);
  font-size: 22px;
  letter-spacing: 3px;
  color: var(--yellow);
}

.chat-drop-text {
  max-width: 320px;
  color: var(--gray-light);
  font-size: 13px;
  line-height: 1.5;
  letter-spacing: 0.4px;
}

.chat-drop-overlay.blocked {
  background:
    linear-gradient(135deg, rgba(192,57,43,0.1), rgba(0,0,0,0.2)),
    rgba(10,10,11,0.76);
}

.chat-drop-overlay.blocked .chat-drop-panel {
  border-color: rgba(192,57,43,0.55);
  border-top-color: var(--red);
  color: var(--red);
}

.chat-drop-overlay.blocked .chat-drop-icon {
  border-color: rgba(192,57,43,0.32);
  background: linear-gradient(135deg, rgba(192,57,43,0.16), rgba(192,57,43,0.04));
  box-shadow: 0 0 22px rgba(192,57,43,0.1);
}

.chat-drop-overlay.blocked .chat-drop-title {
  color: var(--red);
}

.chat-file-input {
  display: none;
}

.chat-attach-btn {
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  border: 2px solid var(--border);
  background: var(--panel);
  color: var(--gray-light);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s;
}

.chat-attach-btn:hover:not(:disabled) {
  border-color: var(--yellow-dim);
  color: var(--yellow);
  background: var(--panel-light);
}

.chat-attach-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.chat-selected-files {
  border-top: 2px solid var(--border);
  background: var(--panel);
  padding: 10px 14px;
}

.selected-files-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 148px);
  gap: 10px;
  overflow-x: auto;
}

.selected-file-card {
  position: relative;
  width: 148px;
  min-width: 0;
  border: 1px solid var(--border);
  background: rgba(255,255,255,0.03);
  overflow: hidden;
  box-sizing: border-box;
}

.selected-file-card.invalid {
  border-color: var(--red-dim);
}

.selected-file-remove {
  position: absolute;
  top: 6px;
  right: 6px;
  z-index: 2;
  width: 24px;
  height: 24px;
  border: 1px solid rgba(0,0,0,0.35);
  background: rgba(0,0,0,0.7);
  color: var(--white);
  cursor: pointer;
}

.selected-file-media {
  position: relative;
  height: 104px;
  background: var(--dark);
}

.selected-file-media-button {
  width: 100%;
  padding: 0;
  border: 0;
  cursor: zoom-in;
}

.selected-file-media-button:hover img,
.selected-file-media-button:hover video {
  filter: brightness(1.08);
}

.selected-file-media img,
.selected-file-media video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
  background: #050505;
}

.selected-file-generic {
  height: 104px;
  display: grid;
  place-items: center;
  background: var(--dark);
}

.selected-file-generic.text {
  place-items: stretch;
  padding: 8px;
}

.selected-text-preview {
  margin: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  color: var(--gray-light);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 10px;
  line-height: 1.35;
  white-space: pre-wrap;
  word-break: break-word;
}

.selected-file-icon {
  color: var(--yellow);
  font-family: var(--font-display);
  font-size: 16px;
  letter-spacing: 1px;
}

.selected-file-info {
  padding: 8px 10px 10px;
  min-width: 0;
}

.selected-file-name {
  color: var(--white);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.selected-file-meta {
  margin-top: 3px;
  color: var(--gray);
  font-size: 11px;
}

.selected-file-error,
.chat-upload-error {
  margin-top: 5px;
  color: #ffb4b4;
  font-size: 11px;
}

.chat-attachments {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
  width: fit-content;
  max-width: 100%;
}

.chat-attachments.multi {
  max-width: min(100%, 500px);
}

.chat-attachments.count-2 {
  max-width: min(100%, 468px);
}

.chat-attachments.count-3,
.chat-attachments.count-4,
.chat-attachments.count-5 {
  max-width: min(100%, 500px);
}

.chat-attachment {
  min-width: 0;
  max-width: 100%;
  width: fit-content;
}

.attachment-visual,
.attachment-video-wrap {
  position: relative;
  display: block;
  max-width: min(100%, calc(100vw - 96px));
  overflow: hidden;
  background: transparent;
  border: 0;
  border-radius: 3px;
  box-sizing: border-box;
}

.attachment-visual {
  padding: 0;
  cursor: zoom-in;
  font: inherit;
}

.attachment-visual:hover img {
  filter: brightness(1.06);
}

.attachment-visual img,
.attachment-video-wrap video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
  background: transparent;
  border-radius: 3px;
}

.attachment-video-wrap video {
  object-fit: contain;
  background: #000;
}

.attachment-badge {
  position: absolute;
  right: 8px;
  bottom: 8px;
  padding: 2px 6px;
  background: rgba(0,0,0,0.72);
  color: var(--yellow);
  border: 1px solid rgba(245,197,24,0.25);
  font-size: 10px;
  font-family: var(--font-display);
  letter-spacing: 1px;
}

.attachment-audio,
.attachment-file {
  display: flex;
  align-items: center;
  gap: 10px;
  width: min(320px, calc(100vw - 96px));
  min-width: 0;
  padding: 10px;
  border: 0;
  background: rgba(255,255,255,0.035);
  color: inherit;
  text-decoration: none;
  box-sizing: border-box;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.attachment-file-icon {
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  display: grid;
  place-items: center;
  border: 1px solid var(--border);
  background: var(--dark);
  color: var(--yellow);
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 1px;
}

.attachment-file-body {
  min-width: 0;
  flex: 1;
}

.attachment-file-name {
  color: var(--white);
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.attachment-file-meta {
  margin-top: 3px;
  color: var(--gray);
  font-size: 11px;
}

.attachment-audio audio {
  width: 100%;
  height: 32px;
  margin-top: 8px;
}

.media-preview-overlay,
.text-preview-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.82);
}

.media-preview-dialog {
  position: relative;
  max-width: min(92vw, 1100px);
  max-height: 88vh;
  display: grid;
  gap: 10px;
  place-items: center;
}

.media-preview-image,
.media-preview-video {
  display: block;
  max-width: min(92vw, 1100px);
  max-height: 82vh;
  object-fit: contain;
  background: #050505;
}

.media-preview-title {
  max-width: min(92vw, 900px);
  color: var(--gray-light);
  font-size: 12px;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-close-btn {
  position: absolute;
  top: -14px;
  right: -14px;
  z-index: 2;
  width: 34px;
  height: 34px;
  border: 1px solid rgba(255,255,255,0.18);
  background: rgba(0,0,0,0.82);
  color: var(--white);
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
}

.preview-close-btn.inline {
  position: static;
  width: 30px;
  height: 30px;
}

.text-preview-dialog {
  width: min(860px, 92vw);
  max-height: 86vh;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  background: var(--panel);
  border: 1px solid var(--border);
  box-shadow: 0 18px 60px rgba(0,0,0,0.65);
}

.text-preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--border);
}

.text-preview-title {
  min-width: 0;
  color: var(--white);
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.text-preview-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.text-preview-download {
  padding: 8px 10px;
  border: 1px solid var(--border);
  color: var(--yellow);
  font-family: var(--font-display);
  font-size: 10px;
  letter-spacing: 1px;
  text-decoration: none;
}

.text-preview-content,
.text-preview-state {
  min-height: 220px;
  max-height: calc(86vh - 56px);
  margin: 0;
  padding: 16px;
  overflow: auto;
  color: var(--gray-light);
  background: var(--dark);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.text-preview-state {
  display: grid;
  place-items: center;
  font-family: var(--font-body);
}

.text-preview-state.error {
  color: #ffb4b4;
}

.delete-confirm-overlay {
  position: fixed;
  inset: 0;
  z-index: 10001;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(0, 0, 0, 0.78);
  backdrop-filter: blur(5px);
}

.delete-confirm-dialog {
  width: min(420px, 92vw);
  background: var(--panel);
  border: 2px solid var(--border);
  border-top: 3px solid var(--red);
  box-shadow: 0 24px 80px rgba(0,0,0,0.65), inset 0 0 0 1px rgba(255,255,255,0.03);
}

.delete-confirm-title {
  padding: 22px 26px 0;
  color: var(--red);
  font-family: var(--font-display);
  font-size: 17px;
  letter-spacing: 2px;
}

.delete-confirm-message {
  padding: 12px 26px 0;
  color: var(--gray-light);
  font-size: 13px;
  line-height: 1.6;
  letter-spacing: 0.3px;
}

.delete-confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 22px 26px;
  margin-top: 18px;
  border-top: 1px solid var(--border);
}

.delete-confirm-btn {
  padding: 9px 18px;
  border: 2px solid var(--border);
  background: transparent;
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 2px;
  cursor: pointer;
  transition: all 0.15s;
}

.delete-confirm-btn.cancel {
  color: var(--gray-light);
}

.delete-confirm-btn.cancel:hover {
  color: var(--white);
  border-color: var(--gray);
  background: rgba(255,255,255,0.03);
}

.delete-confirm-btn.danger {
  color: var(--red);
  border-color: rgba(192,57,43,0.45);
}

.delete-confirm-btn.danger:hover {
  border-color: var(--red);
  background: rgba(192,57,43,0.12);
  box-shadow: 0 0 12px rgba(192,57,43,0.16);
}

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
.cw-avatar { display: block; color: inherit; text-decoration: none; }
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
  width: 220px;
  max-width: calc(100vw - 16px);
  max-height: calc(100vh - 16px);
  overflow-y: auto;
  box-sizing: border-box;
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
.ctx-read-by {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  background: linear-gradient(90deg, rgba(245,197,24,0.05), transparent);
}
.ctx-read-by:hover {
  background: linear-gradient(90deg, rgba(245,197,24,0.1), var(--panel-light));
}
.ctx-read-checks {
  color: var(--yellow);
  font-family: var(--font-body);
  font-size: 13px;
  letter-spacing: -2px;
  text-shadow: 0 0 8px rgba(245,197,24,0.22);
}
.ctx-read-label {
  flex: 1;
  min-width: 0;
}
.ctx-read-avatars {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  margin-left: auto;
}
.ctx-read-avatar {
  width: 23px;
  height: 23px;
  margin-left: -7px;
  border: 2px solid var(--panel);
  background: var(--dark);
  color: var(--yellow);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-family: var(--font-display);
  font-size: 10px;
  box-shadow: 0 0 0 1px var(--border);
}
.ctx-read-avatar:first-child {
  margin-left: 0;
}
.ctx-read-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.read-by-panel {
  position: fixed;
  z-index: 9998;
  width: 300px;
  max-width: calc(100vw - 16px);
  max-height: calc(100vh - 16px);
  box-sizing: border-box;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: var(--panel);
  border: 2px solid var(--border);
  border-top: 3px solid var(--yellow);
  border-right-color: rgba(245,197,24,0.28);
  box-shadow: 0 16px 46px rgba(0,0,0,0.72), inset 0 0 0 1px rgba(245,197,24,0.04);
  transform: translateY(-100%);
  animation: readByPanelIn 0.12s ease-out;
  transform-origin: right bottom;
}
@keyframes readByPanelIn {
  from { opacity: 0; transform: translateY(-100%) scale(0.96); }
  to { opacity: 1; transform: translateY(-100%) scale(1); }
}
.read-by-title {
  flex-shrink: 0;
  padding: 11px 13px 8px;
  border-bottom: 1px solid var(--border);
  color: var(--yellow);
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 2.5px;
}
.read-by-state {
  flex-shrink: 0;
  padding: 20px 16px;
  color: var(--gray);
  font-size: 12px;
  letter-spacing: 0.5px;
  text-align: center;
}
.read-by-error {
  color: var(--red);
}
.read-by-list {
  max-height: 552px;
  overflow-y: auto;
  padding: 6px 0;
}
.read-by-user {
  min-height: 54px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 9px 15px;
  border-left: 3px solid transparent;
  transition: background 0.12s, border-color 0.12s;
}
.read-by-user:hover {
  background: var(--panel-light);
  border-left-color: var(--yellow-dim);
}
.read-by-avatar {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 2px solid var(--border);
  background: var(--dark);
  color: var(--yellow);
  font-family: var(--font-display);
  font-size: 14px;
}
.read-by-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.read-by-info {
  min-width: 0;
}
.read-by-name {
  color: var(--white);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.read-by-meta {
  margin-top: 2px;
  color: var(--gray);
  font-size: 11px;
  letter-spacing: 0.4px;
}
.read-by-meta span {
  color: var(--yellow-dim);
  letter-spacing: -2px;
  margin-right: 5px;
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

.chat-msg {
  position: relative;
  max-width: min(68%, 560px);
}

.chat-msg-bubble {
  width: fit-content;
  max-width: 100%;
  box-sizing: border-box;
}

.chat-msg.has-avatar {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}
.chat-msg.has-avatar .chat-msg-bubble {
  max-width: 100%;
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
