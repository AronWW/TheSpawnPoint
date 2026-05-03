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
    case 'WELCOME_ABOARD':
      return 'welcome'

    case 'PROFILE_COMPLETED':
      return 'profile-complete'

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
    case 'ROOM_OF_REQUIREMENT':
      return 'room-secret'
    case 'NOT_WHAT_YOU_EXPECTED':
      return 'rickroll-secret'
    case 'LAUGH_TALE':
      return 'laugh-tale'
    case 'ONE_RING':
      return 'one-ring'
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
      <circle cx="7.15" cy="14.05" r="1.55" stroke="currentColor" stroke-width="1.7" />
      <path d="M3.95 19.35c0-1.6 1.3-2.75 3.2-2.75s3.2 1.15 3.2 2.75" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
      <path d="M10.95 3.75H18.85V20.25H10.95V3.75Z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round" />
      <path d="M17.35 5.1V18.9" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" opacity="0.55" />
      <path d="M10.95 5.2L14.95 6.05V17.95L10.95 18.8V5.2Z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round" />
      <path d="M14.95 6.05V17.95" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
      <path d="M13.75 12h.01" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
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


    <svg v-else-if="iconKind === 'room-secret'" viewBox="0 0 24 24" fill="none">
      <path d="M6.9 20V9.75c0-2.95 2.17-5.05 5.1-5.05s5.1 2.1 5.1 5.05V20" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"/>
      <path d="M8.6 20V10.2c0-2 1.38-3.35 3.4-3.35s3.4 1.35 3.4 3.35V20" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" opacity="0.92"/>
      <path d="M12 7.05v12.9" stroke="currentColor" stroke-width="1.45" stroke-linecap="round" opacity="0.78"/>
      <circle cx="13.95" cy="13" r="1.05" fill="currentColor"/>
      <path d="M5.4 8.55c1.1-.32 1.84-1.22 2.08-2.45.6.92 1.42 1.45 2.52 1.62" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" opacity="0.62"/>
      <path d="M15.95 6.1c.42.86 1.12 1.45 2.2 1.74-.92.38-1.52 1.02-1.82 1.94" stroke="currentColor" stroke-width="1.25" stroke-linecap="round" opacity="0.62"/>
    </svg>

    <svg v-else-if="iconKind === 'rickroll-secret'" viewBox="0 0 24 24" fill="none">
      <path d="M9.3 4.9v10.35" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>
      <path d="M9.3 6.1 16.95 4.55v7.35" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
      <ellipse cx="7.2" cy="18.15" rx="2.55" ry="1.9" stroke="currentColor" stroke-width="1.55"/>
      <ellipse cx="14.8" cy="16.65" rx="2.55" ry="1.9" stroke="currentColor" stroke-width="1.55"/>
      <path d="M18.1 8.55c1.08.24 1.82.84 2.35 1.82" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" opacity="0.72"/>
      <path d="M18.15 11.15c.68.14 1.18.48 1.56 1.06" stroke="currentColor" stroke-width="1.1" stroke-linecap="round" opacity="0.52"/>
    </svg>

    <svg v-else-if="iconKind === 'laugh-tale'" viewBox="0 0 32 32" fill="none">
      <g stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <g opacity="0.45">
          <path d="M6.2 5.4 25.8 24.6" />
          <path d="M25.8 5.4 6.2 24.6" />
          <circle cx="5.1" cy="4.6" r="1.65" />
          <circle cx="26.9" cy="4.6" r="1.65" />
          <circle cx="5.1" cy="25.4" r="1.65" />
          <circle cx="26.9" cy="25.4" r="1.65" />
        </g>

        <path d="M9.1 13.5c.18 6.45 3.08 10.3 6.9 10.3s6.72-3.85 6.9-10.3" />

        <path d="M5.25 12.1h21.5c1.42 0 2.25.38 2.25 1.05s-.83 1.05-2.25 1.05H5.25C3.83 14.2 3 13.82 3 13.15s.83-1.05 2.25-1.05z" />
        <path d="M9.15 12.1c1.36-4.25 3.65-6.15 6.85-6.15s5.49 1.9 6.85 6.15" />
        <path d="M10.5 10.55h11" opacity="0.7" />
        <path d="m12.6 8.25-.8 1.3M14.3 7.85l-.9 1.5M17.7 7.85l.9 1.5M19.4 8.25l.8 1.3" stroke-width="1.35" opacity="0.8" />

        <path d="M11.55 21.75c1.25.9 2.73 1.35 4.45 1.35s3.2-.45 4.45-1.35v3.25c0 2.12-1.72 3.55-4.45 3.55s-4.45-1.43-4.45-3.55v-3.25z" />
        <path d="M12.3 24.1h7.4" />
        <path d="M14 22.65v5.1M16 23.05v5.35M18 22.65v5.1" stroke-width="1.2" />
      </g>

      <circle cx="12.75" cy="17.35" r="2.15" fill="currentColor" />
      <circle cx="19.25" cy="17.35" r="2.15" fill="currentColor" />
      <ellipse cx="16" cy="20.4" rx="0.9" ry="0.55" fill="currentColor" />
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

    <svg v-else-if="iconKind === 'welcome'" viewBox="0 0 24 24" fill="none"><path d="M15.18 19.95L12.7 16.95L12.42 18.42L11.05 18.34L10.97 16.97L8.33 19.78L6.18 16.36L6.18 14.18L5.28 14.08L4.95 13.61L3.52 14.38L2.91 15.76L2.76 17.15L2.34 16.84L2.21 14.24L2.82 12.59L4.14 11.34L6.25 10.41L7.02 7.95L8.29 6.45L11.03 5.03L12.1 4.97L14.76 6.26L16.35 8.05L17.13 10.49L19.36 11.41L20.81 13.29L20.81 16.98L20.59 17.22L20.36 16.83L20.36 15.95L19.75 14.53L18.12 13.68L17.96 14.11L16.98 14.18L16.98 16.36L15.18 19.95ZM10.02 15.08L10.93 14.75L10.88 13.12L7.74 13.06L9.73 15.13L10.02 15.08ZM13.82 15.07L15.69 13.03L12.54 13.24L12.54 14.64L13.82 15.07Z" fill="currentColor" fill-rule="evenodd" clip-rule="evenodd"/></svg>

    <svg v-else-if="iconKind === 'profile-complete'" viewBox="0 0 24 24" fill="none">
      <circle cx="10" cy="8.2" r="3.2" stroke="currentColor" stroke-width="1.7" />
      <path d="M4 19.5c0-3.2 2.7-5.5 6-5.5 1.2 0 2.3.3 3.2.8" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
      <circle cx="17.5" cy="16.5" r="4.2" stroke="currentColor" stroke-width="1.6" />
      <path d="m15.3 16.5 1.5 1.5 3-3" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
    </svg>

    <svg v-else-if="iconKind === 'one-ring'" viewBox="0 0 24 24" fill="none">
      <g transform="translate(12 12) rotate(-24) translate(-12 -12)">
        <ellipse cx="12" cy="12" rx="7.25" ry="5.15" stroke="currentColor" stroke-width="2.15" opacity="0.95" />
        <ellipse cx="12" cy="12" rx="4.7" ry="2.9" stroke="currentColor" stroke-width="1.5" opacity="0.92" />
        <path d="M5.2 12.55c1.05 1.95 3.72 3.28 6.8 3.28 3.08 0 5.75-1.33 6.8-3.28" stroke="currentColor" stroke-width="1.15" stroke-linecap="round" opacity="0.78" />
        <path d="M6.1 10.9c1.18-1.22 3.36-2.03 5.9-2.03 2.54 0 4.72.81 5.9 2.03" stroke="currentColor" stroke-width="0.85" stroke-linecap="round" opacity="0.34" />
        <path d="M8.1 9.15l.18-.36.14.34.18-.34m.42.08l.08.58m.22-.58l.08.58m.28-.48l.16.44.16-.44m.36.02l.1.5.18-.22.1.22m.42-.46l.12.56m.2-.5l.16.18.16-.18.08.5m.42-.42l.06.56m.26-.54l.16.46.18-.46m.42.08l.12.48.14-.2.12.2m.42-.38l.08.52m.24-.48l.18.18.14-.18.1.46" stroke="currentColor" stroke-width="0.38" stroke-linecap="round" stroke-linejoin="round" opacity="0.62" />
        <path d="M8.15 9.55c.9-.7 2.26-1.14 3.85-1.14" stroke="currentColor" stroke-width="0.95" stroke-linecap="round" opacity="0.22" />
        <path d="M13.85 15.55c1.65-.18 3.05-.79 3.95-1.64" stroke="currentColor" stroke-width="0.95" stroke-linecap="round" opacity="0.18" />
      </g>
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
