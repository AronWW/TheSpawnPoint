<script setup lang="ts">
import type { Party } from '../types'
import { genreColor, skillLabel, timeAgo } from '../utils/helpers'
defineProps<{ party: Party }>()
defineEmits<{ (e: 'select', p: Party): void }>()
</script>

<template>
  <div class="party-card" @click="$emit('select', party)">
    <div class="party-card-accent" :class="genreColor(party.gameName)"></div>
    <div class="party-card-inner">
      <div class="party-card-top">
        <img v-if="party.gameImageUrl" :src="party.gameImageUrl" :alt="party.gameName" class="party-game-cover"/>
        <div v-else class="party-game-cover-ph">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="6" width="20" height="12" rx="2"/><path d="M6 12h4"/><path d="M8 10v4"/><circle cx="15" cy="10" r="1"/><circle cx="18" cy="12" r="1"/></svg>
        </div>
        <div class="party-info">
          <div class="party-game">{{ party.gameName }}</div>
          <div class="party-host">Хост: <span>{{ party.creatorDisplayName }}</span></div>
          <div style="margin-top:4px">
            <span v-if="party.skillLevel" class="skill-badge" :class="party.skillLevel.toLowerCase()">{{ skillLabel(party.skillLevel) }}</span>
          </div>
        </div>
      </div>
      <p class="party-desc">{{ party.title || party.description }}</p>
      <div class="party-slots">
        <div v-for="i in party.maxMembers" :key="i" class="slot" :class="i <= party.currentMembers ? 'filled' : 'empty-slot'">
          <template v-if="i <= party.currentMembers && party.members && party.members[i-1]">{{ party.members[i-1]?.displayName?.substring(0,2)?.toUpperCase() }}</template>
        </div>
        <span class="slots-label"><span>{{ party.currentMembers }}</span>/{{ party.maxMembers }} гравців</span>
      </div>
      <div class="party-footer">
        <span class="platform-tag" v-for="p in party.platform" :key="p">{{ p }}</span>
        <span v-for="lang in (party.languages || []).slice(0, 3)" :key="lang" class="tag" style="font-size:10px">{{ lang }}</span>
        <span v-if="party.region" class="tag" style="font-size:10px">{{ party.region }}</span>
        <span class="party-time">{{ timeAgo(party.createdAt) }}</span>
      </div>
    </div>
  </div>
</template>

