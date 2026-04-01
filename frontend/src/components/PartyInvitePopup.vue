<script setup lang="ts">
import { computed, ref, watch, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePartyStore } from '../stores/parties'
import { useNotificationStore } from '../stores/notifications'
import { useVoiceStore } from '../stores/voice'
import { useToast } from '../composables/useToast'
import { PUBLIC_BASE_URL } from '../config'

const router = useRouter()
const partyStore = usePartyStore()
const notifStore = useNotificationStore()
const voiceStore = useVoiceStore()
const toast = useToast()

const INVITE_EXPIRY_MS = 10 * 60 * 1000

const invite = computed(() => partyStore.pendingInvite)
const isExpired = ref(false)
const showSwitchConfirm = ref(false)
let expiryTimer: ReturnType<typeof setTimeout> | null = null

watch(invite, (inv) => {
  isExpired.value = false
  if (expiryTimer) { clearTimeout(expiryTimer); expiryTimer = null }
  if (!inv) return
  const elapsed = Date.now() - new Date(inv.createdAt).getTime()
  if (elapsed >= INVITE_EXPIRY_MS) {
    isExpired.value = true
  } else {
    expiryTimer = setTimeout(() => {
      isExpired.value = true
    }, INVITE_EXPIRY_MS - elapsed)
  }
}, { immediate: true })

onUnmounted(() => {
  if (expiryTimer) clearTimeout(expiryTimer)
})

function resolveAvatar(url: string | null): string {
  if (!url) return PUBLIC_BASE_URL + '/avatars/default/avatar-1.png'
  if (url.startsWith('http')) return url
  return PUBLIC_BASE_URL + url
}

async function accept() {
  if (!invite.value || isExpired.value) return

  await partyStore.fetchMyParties()
  if (partyStore.myParties.length > 0) {
    const currentParty = partyStore.myParties[0]
    if (currentParty && currentParty.status === 'IN_GAME') {
      toast.show('Ви не можете змінити лобi під час гри', 'error', 3000)
      return
    }
    showSwitchConfirm.value = true
    return
  }

  await doAccept()
}

async function doAccept() {
  if (!invite.value) return
  const partyId = invite.value.partyId
  const inviteId = invite.value.inviteId
  showSwitchConfirm.value = false
  try {
    if (partyStore.myParties.length > 0) {
      const currentParty = partyStore.myParties[0]
      if (currentParty && voiceStore.isInPartyVoice(currentParty.id)) {
        await voiceStore.leaveVoice({ silent: true })
      }
      if (currentParty) {
        await partyStore.leaveParty(currentParty.id)
      }
    }

    await partyStore.acceptInvite(inviteId)
    markRelatedNotificationRead(inviteId)
    await partyStore.fetchMyParties()
    if (partyId) {
      router.push(`/party/${partyId}`)
    }
  } catch {
  } finally {
    partyStore.dismissInvitePopup()
  }
}

function cancelSwitch() {
  showSwitchConfirm.value = false
}

async function decline() {
  if (!invite.value) return
  const inviteId = invite.value.inviteId
  try {
    await partyStore.declineInvite(inviteId)
    markRelatedNotificationRead(inviteId)
  } catch {
  } finally {
    partyStore.dismissInvitePopup()
  }
}

function dismiss() {
  showSwitchConfirm.value = false
  partyStore.dismissInvitePopup()
}

function markRelatedNotificationRead(inviteId: number) {
  const notif = notifStore.notifications.find(
    (n) => n.type === 'PARTY_INVITE' && n.referenceId === inviteId && !n.read
  )
  if (notif) {
    notifStore.markOneRead(notif.id)
  }
}
</script>

<template>
  <Transition name="popup-slide">
    <div v-if="invite" class="popup-overlay">
      <div class="popup">
        <button class="popup-close" @click="dismiss" title="Закрити">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>

        <div class="popup-icon">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/><path d="M12 18v-6"/><path d="M9 15l3 3 3-3"/></svg>
        </div>
        <h3 class="popup-title">ЗАПРОШЕННЯ В ЛОБІ</h3>

        <div class="popup-sender">
          <img :src="resolveAvatar(invite.senderAvatarUrl)" class="sender-avatar" :alt="invite.senderDisplayName" />
          <span class="sender-name">{{ invite.senderDisplayName }}</span>
        </div>

        <div class="popup-game">
          <img v-if="invite.gameImageUrl" :src="invite.gameImageUrl" class="game-cover" :alt="invite.gameName ?? ''" />
          <span class="game-name">{{ invite.gameName }}</span>
        </div>

        <div v-if="showSwitchConfirm" class="switch-confirm">
          <div class="switch-confirm-text">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            Ви вже в лобі. Перейти до нового?
          </div>
          <div class="popup-actions">
            <button class="btn-accept" @click="doAccept">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
              ПЕРЕЙТИ
            </button>
            <button class="btn-decline" @click="cancelSwitch">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              СКАСУВАТИ
            </button>
          </div>
        </div>

        <div v-else-if="isExpired" class="popup-expired">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          Час вийшов
        </div>

        <div v-else class="popup-actions">
          <button class="btn-accept" @click="accept">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
            ПРИЙНЯТИ
          </button>
          <button class="btn-decline" @click="decline">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            ВІДХИЛИТИ
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.popup-overlay {
  position: fixed;
  top: 80px;
  right: 24px;
  z-index: 9999;
}

.popup {
  pointer-events: all;
  width: 320px;
  background: var(--panel);
  border: 2px solid var(--yellow-dim);
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  position: relative;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.6), 0 0 0 1px rgba(245, 197, 24, 0.08);
  animation: glow-pulse 2s ease-in-out infinite;
}

@keyframes glow-pulse {
  0%, 100% { box-shadow: 0 12px 40px rgba(0, 0, 0, 0.6), 0 0 8px rgba(245, 197, 24, 0.15); }
  50%       { box-shadow: 0 12px 40px rgba(0, 0, 0, 0.6), 0 0 20px rgba(245, 197, 24, 0.3); }
}

.popup-close {
  position: absolute;
  top: 8px;
  right: 10px;
  background: none;
  border: none;
  color: var(--gray);
  font-size: 16px;
  cursor: pointer;
  padding: 2px 6px;
  transition: color 0.15s;
}
.popup-close:hover {
  color: var(--white);
}

.popup-icon {
  color: var(--yellow);
}

.popup-title {
  font-family: var(--font-display);
  font-size: 18px;
  letter-spacing: 3px;
  color: var(--yellow);
}

.popup-sender {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sender-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--yellow-dim);
}

.sender-name {
  font-weight: 700;
  font-size: 14px;
  color: var(--white);
  letter-spacing: 0.5px;
}

.popup-game {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: var(--panel-light);
  border: 1px solid var(--border);
  width: 100%;
}

.game-cover {
  width: 36px;
  height: 48px;
  object-fit: cover;
  border: 1px solid var(--border);
  flex-shrink: 0;
}

.game-name {
  font-family: var(--font-display);
  font-size: 16px;
  letter-spacing: 1px;
  color: var(--white);
}

.popup-actions {
  display: flex;
  gap: 10px;
  width: 100%;
}

.btn-accept {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 0;
  background: var(--yellow);
  border: 2px solid var(--yellow);
  color: var(--black);
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 2px;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-accept:hover {
  background: var(--yellow-dim);
}

.btn-decline {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 0;
  background: transparent;
  border: 2px solid var(--red-dim);
  color: var(--red);
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 2px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.btn-decline:hover {
  background: var(--red);
  color: #fff;
}

.popup-expired {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 0;
  width: 100%;
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 2px;
  color: var(--gray);
  border: 2px solid var(--border);
  background: var(--panel-light);
}

.switch-confirm {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.switch-confirm-text {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--yellow);
  padding: 8px 12px;
  border: 1px solid var(--yellow-dim);
  background: rgba(245, 197, 24, 0.06);
}

.popup-slide-enter-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.popup-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.popup-slide-enter-from {
  opacity: 0;
  transform: translateY(-20px);
}
.popup-slide-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

@media (max-width: 960px) {
  .popup-overlay {
    top: 80px;
    right: 50%;
    transform: translateX(50%);
  }
}

@media (max-width: 480px) {
  .popup-overlay {
    top: 70px;
    right: 0;
    left: 0;
    transform: none;
    display: flex;
    justify-content: center;
    padding: 0 8px;
  }
  .popup {
    width: 100%;
    max-width: 360px;
  }
}
</style>

