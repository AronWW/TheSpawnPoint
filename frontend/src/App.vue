<script setup lang="ts">
import { onMounted, onUnmounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import TheNavbar from './components/TheNavbar.vue'
import TheFooter from './components/TheFooter.vue'
import BannedModal from './components/BannedModal.vue'
import PartyInvitePopup from './components/PartyInvitePopup.vue'
import ToastContainer from './components/ToastContainer.vue'
import FloatingVoiceWidget from './components/FloatingVoiceWidget.vue'
import { useAuthStore } from './stores/auth'
import { useVoiceStore } from './stores/voice'
import { useGlobalWebSocket } from './composables/useGlobalWebSocket'

const auth = useAuthStore()
const voice = useVoiceStore()
const route = useRoute()

const showLayout = computed(() => !route.meta.hideNavbar)
const isBanned = computed(() => auth.user?.banned === true)

async function handleBannedEvent(event: Event) {
  const detail = (event as CustomEvent<{ banReason?: string | null }>).detail
  auth.markBanned(detail?.banReason ?? auth.user?.banReason ?? null)
  await auth.refreshUser()
}

onMounted(() => {
  auth.init()
  voice.init()
  window.addEventListener('auth:banned', handleBannedEvent)
})

onUnmounted(() => {
  voice.dispose()
  window.removeEventListener('auth:banned', handleBannedEvent)
})

useGlobalWebSocket()
</script>

<template>
  <BannedModal v-if="isBanned" :ban-reason="auth.user?.banReason ?? null" />
  <TheNavbar v-if="showLayout" />
  <router-view />
  <TheFooter v-if="showLayout" />
  <PartyInvitePopup />
  <ToastContainer />
  <FloatingVoiceWidget />
</template>
