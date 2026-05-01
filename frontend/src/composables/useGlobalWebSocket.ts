import { watch, onUnmounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useNotificationStore } from '../stores/notifications'
import { useFriendStore } from '../stores/friends'
import { useChatStore } from '../stores/chat'
import { usePartyStore } from '../stores/parties'
import { useAchievementStore } from '../stores/achievements'
import { useBlockStore } from '../stores/block'
import { useStompClient } from './useStompClient'
import { playNotificationSound } from './useNotificationSound'
import { useToast } from './useToast'
import type { Notification, ChatMessage, ChatEvent, AchievementUnlockEvent } from '../types'


export function useGlobalWebSocket() {
  const auth = useAuthStore()
  const notifStore = useNotificationStore()
  const friendStore = useFriendStore()
  const chatStore = useChatStore()
  const partyStore = usePartyStore()
  const achievementStore = useAchievementStore()
  const blockStore = useBlockStore()
  const stomp = useStompClient()
  const toast = useToast()

  let teardowns: (() => void)[] = []
  let isSubscribed = false

  function connectAndSubscribe() {
    const email = auth.user?.email
    if (!email || isSubscribed) return

    if (teardowns.length) {
      for (const unsub of teardowns) {
        try { unsub() } catch { }
      }
      teardowns = []
    }

    isSubscribed = true
    stomp.activate()

    teardowns.push(
      stomp.subscribe('/user/queue/notifications', (frame) => {
        try {
          const notification: Notification = JSON.parse(frame.body)

          const isCancelledInvite =
            notification.type === 'PARTY_INVITE' &&
            notification.referenceId &&
            notification.message.includes('скасував запрошення')

          if (isCancelledInvite && notification.referenceId) {
            if (
              partyStore.pendingInvite &&
              partyStore.pendingInvite.inviteId === notification.referenceId
            ) {
              partyStore.dismissInvitePopup()
            }
            partyStore.respondedInvites.set(notification.referenceId, 'cancelled')

            const existingIdx = notifStore.notifications.findIndex((n) => n.id === notification.id)
            if (existingIdx !== -1) {
              notifStore.notifications[existingIdx] = notification
            } else {
              const origIdx = notifStore.notifications.findIndex(
                (n) => n.type === 'PARTY_INVITE' && n.referenceId === notification.referenceId
              )
              if (origIdx !== -1) {
                notifStore.notifications[origIdx] = notification
              } else {
                notifStore.addNotification(notification)
              }
            }
          } else {
            notifStore.addNotification(notification)
            playNotificationSound()
          }

          if (
            notification.type === 'PARTY_INVITE' &&
            notification.referenceId &&
            notification.message.includes('запрошує вас')
          ) {
            partyStore.fetchIncomingInvites().then(() => {
              const inv = partyStore.incomingInvites.find(
                (i) => i.inviteId === notification.referenceId
              )
              if (inv) {
                partyStore.showInvitePopup(inv)
              }
            })
          }
        } catch { }
      })
    )

    teardowns.push(
      stomp.subscribe('/user/queue/achievements', (frame) => {
        try {
          const achievement: AchievementUnlockEvent = JSON.parse(frame.body)
          achievementStore.enqueueUnlock(achievement)
        } catch { }
      })
    )

    teardowns.push(
      stomp.subscribe('/user/queue/notifications/unread-count', (frame) => {
        try {
          const payload = JSON.parse(frame.body)
          notifStore.setUnreadCount(payload.count ?? 0)
        } catch { }
      })
    )

    teardowns.push(
      stomp.subscribe('/topic/status', (frame) => {
        try {
          const payload = JSON.parse(frame.body)
          const { email: userEmail, status, lastSeen, statusVisibility } = payload
          if (userEmail) {
            if (statusVisibility === 'NOBODY') {
              friendStore.updateFriendStatus(userEmail, 'OFFLINE', null)
              chatStore.updatePartnerStatus(userEmail, 'OFFLINE', null)
            } else if (statusVisibility === 'FRIENDS') {
              const isFriend = friendStore.friends.some((f) => f.email === userEmail)
              if (isFriend) {
                friendStore.updateFriendStatus(userEmail, status, lastSeen || null)
                chatStore.updatePartnerStatus(userEmail, status, lastSeen || null)
              } else {
                friendStore.updateFriendStatus(userEmail, 'OFFLINE', null)
                chatStore.updatePartnerStatus(userEmail, 'OFFLINE', null)
              }
            } else {
              friendStore.updateFriendStatus(userEmail, status, lastSeen || null)
              chatStore.updatePartnerStatus(userEmail, status, lastSeen || null)
            }
          }
        } catch { }
      })
    )

    teardowns.push(
      stomp.subscribe('/user/queue/errors', (frame) => {
        try {
          const payload = JSON.parse(frame.body) as { error?: string; message?: string }
          const fallback = payload.error === 'SPAM_DETECTED'
            ? 'Не спамте, будь ласка.'
            : payload.error === 'RATE_LIMITED'
              ? 'Не надсилайте повідомлення так швидко!'
              : 'Сталася помилка чату.'

          toast.show(payload.message || fallback, 'error')
        } catch {
          toast.show('Сталася помилка чату.', 'error')
        }
      })
    )

    teardowns.push(
      stomp.subscribe('/user/queue/messages', (frame) => {
        try {
          const msg: ChatMessage = JSON.parse(frame.body)
          chatStore.onIncomingMessage(msg)
        } catch { }
      })
    )

    teardowns.push(
      stomp.subscribe('/user/queue/typing', (frame) => {
        try {
          const payload = JSON.parse(frame.body)
          if (payload.chatId && payload.senderEmail) {
            chatStore.onPartnerTyping(payload.chatId, payload.senderEmail, payload.displayName)
          }
        } catch { }
      })
    )

    teardowns.push(
      stomp.subscribe('/user/queue/read', (frame) => {
        try {
          const payload = JSON.parse(frame.body)
          chatStore.onReadReceipt(payload)
        } catch { }
      })
    )

    teardowns.push(
      stomp.subscribe('/user/queue/chat-events', (frame) => {
        try {
          const event: ChatEvent = JSON.parse(frame.body)
          chatStore.onChatEvent(event)
        } catch { }
      })
    )

    teardowns.push(
      stomp.subscribe('/topic/online-count', (frame) => {
        try {
          const payload = JSON.parse(frame.body)
          auth.onlineCount = payload.count ?? 0
        } catch { }
      })
    )

    notifStore.fetchUnreadCount()
    chatStore.fetchChats()
    blockStore.fetchBlockedUsers()
    auth.fetchOnlineCount()
  }


  function disconnectAndCleanup() {
    for (const unsub of teardowns) {
      try { unsub() } catch { }
    }
    teardowns = []
    isSubscribed = false
    stomp.deactivate()
  }

  const stopWatcher = watch(
    [
      () => auth.isLoggedIn,
      () => auth.user?.banned === true,
      () => auth.user?.email,
    ],
    ([loggedIn, banned, email], [prevLoggedIn, prevBanned, prevEmail]) => {
      if (loggedIn && !banned && email) {
        if (!isSubscribed || email !== prevEmail || !prevLoggedIn || prevBanned) {
          disconnectAndCleanup()
          connectAndSubscribe()
        }
      } else {
        achievementStore.resetState()
        disconnectAndCleanup()
      }
    },
    { immediate: true }
  )

  onUnmounted(() => {
    stopWatcher()
    disconnectAndCleanup()
  })
}
