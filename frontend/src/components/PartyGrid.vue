<script setup lang="ts">
import { useRouter } from 'vue-router'
import { usePartyStore } from '../stores/parties'
import PartyCard from './PartyCard.vue'
import type { Party } from '../types'

const router = useRouter()
const partyStore = usePartyStore()

function selectParty(party: Party) {
  router.push(`/party/${party.id}`)
}
</script>

<template>
  <div class="party-grid">
    <TransitionGroup name="card">
      <template v-if="partyStore.filteredParties.length">
        <PartyCard
          v-for="party in partyStore.filteredParties"
          :key="party.id"
          :party="party"
          @select="selectParty"
        />
      </template>
      <div v-else key="empty" class="empty-state">
        <div class="empty-state-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="6" width="20" height="12" rx="2"/><path d="M6 12h4"/><path d="M8 10v4"/><circle cx="15" cy="10" r="1"/><circle cx="18" cy="12" r="1"/></svg>
        </div>
        <div class="empty-state-title">ЛОБІ НЕ ЗНАЙДЕНО</div>
        <div class="empty-state-sub">Спробуй змінити фільтри або створи власне лобі</div>
      </div>
    </TransitionGroup>
  </div>
</template>

