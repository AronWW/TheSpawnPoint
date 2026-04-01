<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  code: string
  size?: number
}>(), {
  size: 28,
})

const iconKind = computed(() => {
  switch (props.code) {
    case 'FIRST_FAVORITE_GAME':
      return 'favorite'

    case 'FIRST_FRIEND':
    case 'FRIENDS_25':
    case 'FRIENDS_50':
      return 'friends'

    case 'FIRST_PARTY_CREATED':
    case 'CREATED_PARTIES_25':
    case 'CREATED_PARTIES_50':
      return 'party-created'

    case 'FIRST_PARTY_JOINED':
    case 'JOINED_PARTIES_25':
    case 'JOINED_PARTIES_50':
      return 'party-joined'

    case 'FIRST_CHAT_MESSAGE':
    case 'CHATTERBOX_100':
    case 'CHATTERBOX_1000':
    case 'CHATTERBOX_10000':
      return 'chat'

    case 'FIRST_GAME_COMPLETED':
    case 'COMPLETED_PARTIES_25':
    case 'COMPLETED_PARTIES_50':
      return 'party-completed'

    case 'FOOTER_TRIPLE_CLICK':
      return 'footer-secret'
    case 'ANSWER_TO_LIFE':
      return 'life-answer'
    case 'SECRET_DOOM_FOUND':
      return 'doom-found'
    case 'SECRET_DOOM_COMPLETED':
      return 'doom-completed'
    default:
      return 'fallback'
  }
})

const badgeText = computed(() => {
  switch (props.code) {
    case 'FIRST_FRIEND':
    case 'FIRST_CHAT_MESSAGE':
    case 'FIRST_PARTY_CREATED':
    case 'FIRST_PARTY_JOINED':
    case 'FIRST_GAME_COMPLETED':
      return '1'
    case 'FRIENDS_25':
    case 'CREATED_PARTIES_25':
    case 'JOINED_PARTIES_25':
    case 'COMPLETED_PARTIES_25':
      return '25'
    case 'FRIENDS_50':
    case 'CREATED_PARTIES_50':
    case 'JOINED_PARTIES_50':
    case 'COMPLETED_PARTIES_50':
      return '50'
    case 'CHATTERBOX_100':
      return '100'
    case 'CHATTERBOX_1000':
      return '1K'
    case 'CHATTERBOX_10000':
      return '10K'
    default:
      return ''
  }
})

const isWideBadge = computed(() => badgeText.value.length >= 3)
const badgeFontSize = computed(() => {
  if (badgeText.value.length >= 3) return '3.2'
  if (badgeText.value.length === 2) return '3.8'
  return '4.3'
})
</script>

<template>
  <span class="achievement-icon" :style="{ '--icon-size': `${size}px`, '--badge-font-size': badgeFontSize }" aria-hidden="true">
    <svg v-if="iconKind === 'favorite'" viewBox="0 0 24 24" fill="none">
      <path d="M12 4.4 14.18 8.8l4.86.7-3.52 3.4.84 4.8L12 15.45 7.64 17.7l.84-4.8-3.52-3.4 4.86-.7L12 4.4z" stroke="currentColor" stroke-width="1.9" stroke-linejoin="round" />
    </svg>

    <svg v-else-if="iconKind === 'friends'" viewBox="0 0 24 24" fill="none">
      <circle cx="8.4" cy="9" r="2.15" stroke="currentColor" stroke-width="1.8" />
      <circle cx="14.85" cy="9.8" r="1.9" stroke="currentColor" stroke-width="1.7" />
      <path d="M4.95 16.9c.62-2.18 2.18-3.36 3.45-3.36 1.27 0 2.83 1.18 3.45 3.36" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
      <path d="M12.15 16.55c.48-1.65 1.62-2.56 2.7-2.56 1.08 0 2.22.91 2.7 2.56" stroke="currentColor" stroke-width="1.55" stroke-linecap="round" />
      <g v-if="badgeText">
        <circle v-if="!isWideBadge" cx="18.2" cy="18.15" r="4.05" class="badge-disc" />
        <rect v-else x="11.75" y="14.55" width="11.1" height="7.15" rx="3.55" class="badge-disc" />
        <text x="18.2" y="19.35" text-anchor="middle" class="badge-text">{{ badgeText }}</text>
      </g>
    </svg>

    <svg v-else-if="iconKind === 'party-created'" viewBox="0 0 24 24" fill="none">
      <path d="M6.2 4.4v15.2" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" />
      <path d="M7.25 5.65h8.9l-1.65 3.1 1.65 3.15h-8.9V5.65z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" />
      <path d="M15.4 15.45h4.2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
      <path d="M17.5 13.35v4.2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
      <g v-if="badgeText">
        <circle v-if="!isWideBadge" cx="18.15" cy="18.15" r="4.05" class="badge-disc" />
        <rect v-else x="11.7" y="14.55" width="11.15" height="7.15" rx="3.55" class="badge-disc" />
        <text x="18.2" y="19.35" text-anchor="middle" class="badge-text">{{ badgeText }}</text>
      </g>
    </svg>

    <svg v-else-if="iconKind === 'party-joined'" viewBox="0 0 24 24" fill="none">
      <path d="M4.8 6.15h6.2v11.7H4.8z" stroke="currentColor" stroke-width="1.85" stroke-linejoin="round" />
      <path d="M10.25 12h8.3" stroke="currentColor" stroke-width="1.95" stroke-linecap="round" />
      <path d="m14.8 8.55 3.75 3.45-3.75 3.45" stroke="currentColor" stroke-width="1.95" stroke-linecap="round" stroke-linejoin="round" />
      <path d="M7.15 11.25v1.5" stroke="currentColor" stroke-width="1.55" stroke-linecap="round" opacity="0.72" />
      <g v-if="badgeText">
        <circle v-if="!isWideBadge" cx="18.15" cy="18.15" r="4.05" class="badge-disc" />
        <rect v-else x="11.7" y="14.55" width="11.15" height="7.15" rx="3.55" class="badge-disc" />
        <text x="18.2" y="19.35" text-anchor="middle" class="badge-text">{{ badgeText }}</text>
      </g>
    </svg>

    <svg v-else-if="iconKind === 'chat'" viewBox="0 0 24 24" fill="none">
      <path d="M4.35 6.35h12.2c1.82 0 3.3 1.45 3.3 3.25v3.85c0 1.8-1.48 3.25-3.3 3.25H10.4l-4.05 3.1v-3.1H4.35c-1.25 0-2.25-.98-2.25-2.2V8.55c0-1.22 1-2.2 2.25-2.2z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" />
      <path d="M7.15 10.35h6.65" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" />
      <path d="M7.15 13.15h4.45" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" opacity="0.88" />
      <g v-if="badgeText">
        <circle v-if="!isWideBadge" cx="18.15" cy="18.15" r="4.05" class="badge-disc" />
        <rect v-else x="11.7" y="14.55" width="11.15" height="7.15" rx="3.55" class="badge-disc" />
        <text x="18.2" y="19.35" text-anchor="middle" class="badge-text">{{ badgeText }}</text>
      </g>
    </svg>

    <svg v-else-if="iconKind === 'party-completed'" viewBox="0 0 24 24" fill="none">
      <path d="M8.25 4.85h7.5v2.25A3.1 3.1 0 0 0 18.85 10h.15v1.7c0 3.62-2.25 5.98-7 7.6-4.75-1.62-7-3.98-7-7.6V10h.15a3.1 3.1 0 0 0 3.1-2.9V4.85z" stroke="currentColor" stroke-width="1.75" stroke-linejoin="round" />
      <path d="m9.45 12.2 1.9 1.9 3.7-3.7" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
      <g v-if="badgeText">
        <circle v-if="!isWideBadge" cx="18.2" cy="7.2" r="4.0" class="badge-disc" />
        <rect v-else x="11.7" y="3.6" width="11.2" height="7.1" rx="3.55" class="badge-disc" />
        <text x="18.2" y="8.4" text-anchor="middle" class="badge-text">{{ badgeText }}</text>
      </g>
    </svg>

    <svg v-else-if="iconKind === 'footer-secret'" viewBox="0 0 24 24" fill="none">
      <path d="M4.25 18h15.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
      <path d="M7 14.45v3.55M12 14.45v3.55M17 14.45v3.55" stroke="currentColor" stroke-width="1.45" stroke-linecap="round" opacity="0.78" />
      <path d="m12 4.75 1.95 4.3 4.3 1.95-4.3 1.95L12 17.25l-1.95-4.3-4.3-1.95 4.3-1.95L12 4.75z" stroke="currentColor" stroke-width="1.55" stroke-linejoin="round" />
    </svg>

    <svg v-else-if="iconKind === 'life-answer'" viewBox="0 0 24 24" fill="none">
      <rect x="4.1" y="5" width="15.8" height="14" rx="2.1" stroke="currentColor" stroke-width="1.75" />
      <path d="M7.8 10.15 10 12l-2.2 1.85" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
      <path d="M12 14.15h4.2" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" />
      <path d="M6.95 7.55h10.1" stroke="currentColor" stroke-width="1.25" opacity="0.48" />
    </svg>

    <svg v-else-if="iconKind === 'doom-found'" viewBox="0 0 24 24" fill="none">
      <path d="M8.25 8.15c.35-2.18 1.82-3.3 3.75-3.3s3.4 1.12 3.75 3.3l.6 3.65-.82 4.75H8.47l-.82-4.75.6-3.65z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round" />
      <path d="M9.5 11h1.4v1.65H9.5zM13.1 11h1.4v1.65h-1.4z" fill="currentColor" />
      <path d="M10.15 14.55c.58.46 1.2.68 1.85.68.65 0 1.27-.22 1.85-.68" stroke="currentColor" stroke-width="1.45" stroke-linecap="round" />
    </svg>

    <svg v-else-if="iconKind === 'doom-completed'" viewBox="0 0 24 24" fill="none">
      <path d="M12 3.7 18 6.9v7.55L12 20.3l-6-5.85V6.9L12 3.7z" stroke="currentColor" stroke-width="1.65" stroke-linejoin="round" />
      <path d="M8.95 8.3c.32-1.9 1.63-2.9 3.05-2.9s2.73 1 3.05 2.9l.48 3.05-.65 3.85H9.12l-.65-3.85.48-3.05z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" />
      <path d="M9.95 10.95h1.18v1.45H9.95zM12.87 10.95h1.18v1.45h-1.18z" fill="currentColor" />
      <path d="M10.35 13.95c.46.34 1.02.52 1.65.52.63 0 1.19-.18 1.65-.52" stroke="currentColor" stroke-width="1.35" stroke-linecap="round" />
      <path d="m9.7 17.05 1.45 1.45 3.2-3.2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
    </svg>

    <svg v-else viewBox="0 0 24 24" fill="none">
      <path d="M12 4.4 19.6 12 12 19.6 4.4 12 12 4.4z" stroke="currentColor" stroke-width="1.85" stroke-linejoin="round" />
    </svg>
  </span>
</template>

<style scoped>
.achievement-icon {
  display: inline-flex;
  width: var(--icon-size);
  height: var(--icon-size);
  line-height: 0;
}

.achievement-icon svg {
  width: 100%;
  height: 100%;
  overflow: visible;
}

.achievement-icon text {
  font-family: 'Barlow Condensed', sans-serif;
  letter-spacing: 0.12px;
}

.badge-disc {
  fill: rgba(10, 10, 12, 0.94);
  stroke: currentColor;
  stroke-width: 0.85;
}

.badge-text {
  fill: currentColor;
  font-size: calc(var(--badge-font-size) * 1px);
  font-weight: 800;
}
</style>
