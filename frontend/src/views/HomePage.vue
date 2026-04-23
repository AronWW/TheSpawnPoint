<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePartyStore } from '../stores/parties'
import { useGameStore } from '../stores/games'
import { useAuthStore } from '../stores/auth'
import { useNotificationStore } from '../stores/notifications'
import HeroSection from '../components/HeroSection.vue'
import PopularParties from '../components/PopularParties.vue'
import HowItWorks from '../components/HowItWorks.vue'
import CreatePartyModal from '../components/CreatePartyModal.vue'

const router = useRouter()
const partyStore = usePartyStore()
const gameStore = useGameStore()
const auth = useAuthStore()
const notifStore = useNotificationStore()

const modalOpen = ref(false)

function openModal() {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: '/' } })
    return
  }

  modalOpen.value = true
}

onMounted(async () => {
  await Promise.all([
    partyStore.fetchParties(0),
    gameStore.fetchGames(),
    auth.fetchOnlineCount(),
  ])

  if (auth.isLoggedIn) {
    notifStore.fetchUnreadCount()
    partyStore.fetchMyParties()
  }
})
</script>

<template>
  <HeroSection @open-create-modal="openModal" />

  <main class="main home-main">
    <PopularParties />
    <HowItWorks />
  </main>

  <CreatePartyModal :visible="modalOpen" @close="modalOpen = false" />
</template>
