<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  rating: number | null | undefined
  size?: 'sm' | 'md' | 'lg' | 'xl'
}>(), {
  size: 'sm',
})

const displayRating = computed(() => {
  if (props.rating == null) return null
  return props.rating.toFixed(2)
})

const starOpacity = computed(() => {
  if (props.rating == null) return 0.25
  if (props.rating >= 4.5) return 1
  if (props.rating >= 4.0) return 0.9
  if (props.rating >= 3.5) return 0.75
  if (props.rating >= 3.0) return 0.6
  if (props.rating >= 2.5) return 0.45
  if (props.rating >= 2.0) return 0.35
  return 0.25
})

const starColor = computed(() => {
  if (props.rating == null) return '#ffffff'
  if (props.rating >= 4.5) return '#F5C518'
  if (props.rating >= 4.0) return '#E0B615'
  if (props.rating >= 3.5) return '#D4A812'
  if (props.rating >= 3.0) return '#C49A0F'
  if (props.rating >= 2.5) return '#A8890E'
  return '#7A7A6E'
})
</script>

<template>
  <span class="rating-badge" :class="[`rating-${size}`, { 'rating-empty': displayRating == null }]">
    <svg
      class="rating-star"
      viewBox="0 0 24 24"
      fill="none"
      :stroke="starColor"
      :style="{ opacity: starOpacity }"
      aria-hidden="true"
    >
      <path
        d="M12 2.5l2.9 5.9 6.5.95-4.7 4.58 1.1 6.47L12 17.27 6.2 20.4l1.1-6.47L2.6 9.35l6.5-.95L12 2.5z"
        stroke-width="1.8"
        stroke-linejoin="round"
        stroke-linecap="round"
      />
    </svg>
    <span v-if="displayRating != null" class="rating-value">{{ displayRating }}</span>
    <span v-else class="rating-none">—</span>
  </span>
</template>

<style scoped>
.rating-badge {
  display: inline-flex;
  align-items: flex-end;
  gap: 3px;
  white-space: nowrap;
  vertical-align: middle;
  line-height: 1;
}

.rating-badge.rating-empty {
  align-items: center;
}

.rating-badge.rating-empty .rating-star {
  margin-bottom: 0;
}

.rating-star {
  flex-shrink: 0;
  display: block;
  transition: opacity 0.2s ease;
  margin-bottom: 1px;
}

.rating-value {
  font-family: var(--font-display, monospace);
  color: var(--white, #f0f0f0);
  letter-spacing: 0.5px;
  line-height: 1;
}

.rating-none {
  font-family: var(--font-body, sans-serif);
  color: var(--gray, #888);
  letter-spacing: 0.5px;
  line-height: 1;
  opacity: 0.4;
  align-self: center;
}

.rating-sm {
  gap: 4px;
}
.rating-sm .rating-star {
  width: 14px;
  height: 14px;
}
.rating-sm .rating-value {
  font-size: 12px;
}
.rating-sm .rating-none {
  font-size: 11px;
}

.rating-md {
  gap: 5px;
}
.rating-md .rating-star {
  width: 18px;
  height: 18px;
}
.rating-md .rating-value {
  font-size: 14px;
  font-weight: 600;
}
.rating-md .rating-none {
  font-size: 13px;
}

.rating-lg {
  gap: 7px;
}
.rating-lg .rating-star {
  width: 24px;
  height: 24px;
}
.rating-lg .rating-value {
  font-size: 19px;
  font-weight: 600;
}
.rating-lg .rating-none {
  font-size: 17px;
}

.rating-xl {
  gap: 10px;
}
.rating-xl .rating-star {
  width: 32px;
  height: 32px;
  margin-bottom: 2px;
}
.rating-xl .rating-value {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 1px;
}
.rating-xl .rating-none {
  font-size: 22px;
}
</style>

