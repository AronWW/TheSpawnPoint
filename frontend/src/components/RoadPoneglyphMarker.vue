<script setup lang="ts">
import { computed, ref } from 'vue'
import LaughTaleQuestModal from './LaughTaleQuestModal.vue'
import type { LaughTaleClueCode } from '../stores/laughTaleQuest'
import { useLaughTaleQuestStore } from '../stores/laughTaleQuest'

const props = withDefaults(defineProps<{
  clueCode: LaughTaleClueCode
  visible?: boolean
  label?: string
}>(), {
  visible: true,
  label: 'Роуд понегліф',
})

const emit = defineEmits<{
  discovered: [code: LaughTaleClueCode]
}>()

const quest = useLaughTaleQuestStore()
const showLogbook = ref(false)
const newlyDiscovered = ref<LaughTaleClueCode | null>(null)

const canShow = computed(() =>
  props.visible
  && quest.state.started
  && quest.canDiscover(props.clueCode),
)

function collect() {
  const wasDiscovered = quest.discover(props.clueCode)
  if (!wasDiscovered) return

  newlyDiscovered.value = props.clueCode
  showLogbook.value = true
  emit('discovered', props.clueCode)
}
</script>

<template>
  <button
    v-if="canShow"
    class="road-poneglyph-marker"
    type="button"
    :title="label"
    @click.stop="collect"
  >
    <span class="road-poneglyph-marker__stone">
      <span class="road-poneglyph-marker__runes" aria-hidden="true">
        <i
          v-for="rune in 30"
          :key="rune"
          :class="`road-poneglyph-marker__glyph--${rune % 10}`"
        ></i>
      </span>
    </span>
    <span class="road-poneglyph-marker__label">{{ label }}</span>
  </button>

  <LaughTaleQuestModal
    v-if="showLogbook"
    :newly-discovered="newlyDiscovered"
    @close="showLogbook = false"
  />
</template>

<style scoped>
.road-poneglyph-marker {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 9px;
  border: 1px solid rgba(184, 50, 50, 0.5);
  border-radius: 4px;
  background: rgba(12, 13, 15, 0.92);
  color: #f0c8c8;
  padding: 8px 10px;
  cursor: pointer;
  font-family: var(--font-display);
  font-size: 10px;
  letter-spacing: 1.4px;
  text-transform: uppercase;
  box-shadow: 0 0 22px rgba(184, 50, 50, 0.16);
  isolation: isolate;
  overflow: visible;
  transform-origin: center;
  transition: border-color 0.16s, background 0.16s, color 0.16s, transform 0.16s, box-shadow 0.16s;
  animation: road-poneglyph-materialize 0.52s cubic-bezier(0.19, 0.9, 0.28, 1.18);
}

.road-poneglyph-marker::before {
  content: '';
  position: absolute;
  inset: -7px;
  z-index: -1;
  border: 1px solid rgba(184, 50, 50, 0.22);
  border-radius: 6px;
  opacity: 0.7;
  transform: scale(0.92);
  pointer-events: none;
  animation: road-poneglyph-signal 1.8s ease-out infinite;
}

.road-poneglyph-marker::after {
  content: '';
  position: absolute;
  left: 13px;
  right: 13px;
  bottom: -5px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,210,210,0.36), transparent);
  pointer-events: none;
}

.road-poneglyph-marker:hover {
  border-color: rgba(255, 120, 120, 0.78);
  background: rgba(184, 50, 50, 0.12);
  color: #ffe1e1;
  box-shadow: 0 0 28px rgba(184, 50, 50, 0.24), 0 8px 22px rgba(0, 0, 0, 0.3);
  transform: translateY(-2px);
}

.road-poneglyph-marker:active {
  transform: translateY(0) scale(0.98);
}

.road-poneglyph-marker__stone {
  position: relative;
  width: 30px;
  height: 34px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 1px solid rgba(255, 160, 160, 0.42);
  border-radius: 2px;
  background:
    linear-gradient(135deg, rgba(255,255,255,0.1), transparent 24%),
    radial-gradient(circle at 28% 18%, rgba(255,120,120,0.16), transparent 30%),
    linear-gradient(180deg, #ba2b2f 0%, #8f1f25 48%, #5e1218 100%);
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.18), inset 0 -10px 18px rgba(45,0,0,0.28), 0 0 16px rgba(184,50,50,0.28);
  animation: road-poneglyph-stone-breathe 2.3s ease-in-out infinite;
}

.road-poneglyph-marker__stone::before {
  content: '';
  position: absolute;
  inset: 3px;
  background:
    repeating-linear-gradient(0deg, transparent 0 6px, rgba(60,0,8,0.22) 6px 7px),
    repeating-linear-gradient(90deg, transparent 0 6px, rgba(60,0,8,0.2) 6px 7px);
  opacity: 0.88;
  pointer-events: none;
}

.road-poneglyph-marker__stone::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(255,255,255,0.08), transparent 18%, transparent 78%, rgba(0,0,0,0.18)),
    linear-gradient(180deg, rgba(255,255,255,0.08), transparent 26%, rgba(0,0,0,0.18));
  pointer-events: none;
}

.road-poneglyph-marker__runes {
  position: absolute;
  inset: 2px;
  overflow: hidden;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  grid-auto-rows: 1fr;
  gap: 1px;
  pointer-events: none;
}

.road-poneglyph-marker__runes i {
  position: relative;
  min-width: 0;
  min-height: 0;
  border: 1px solid rgba(42, 0, 7, 0.38);
  background:
    linear-gradient(90deg, transparent 45%, rgba(42,0,7,0.72) 45% 57%, transparent 57%),
    linear-gradient(0deg, transparent 43%, rgba(42,0,7,0.54) 43% 55%, transparent 55%);
  opacity: 0.9;
}

.road-poneglyph-marker__runes i::before,
.road-poneglyph-marker__runes i::after {
  content: '';
  position: absolute;
  background: rgba(42, 0, 7, 0.7);
}

.road-poneglyph-marker__runes i::before {
  left: 1px;
  right: 1px;
  top: 1px;
  height: 1px;
}

.road-poneglyph-marker__runes i::after {
  top: 1px;
  bottom: 1px;
  right: 1px;
  width: 1px;
}

.road-poneglyph-marker__glyph--1 {
  background:
    linear-gradient(90deg, rgba(42,0,7,0.7) 0 1px, transparent 1px),
    linear-gradient(0deg, transparent 55%, rgba(42,0,7,0.7) 55% 68%, transparent 68%) !important;
}

.road-poneglyph-marker__glyph--2 {
  background:
    linear-gradient(135deg, transparent 42%, rgba(42,0,7,0.72) 42% 58%, transparent 58%),
    linear-gradient(90deg, transparent 62%, rgba(42,0,7,0.62) 62% 76%, transparent 76%) !important;
}

.road-poneglyph-marker__glyph--3 {
  background:
    linear-gradient(0deg, rgba(42,0,7,0.7) 0 1px, transparent 1px),
    linear-gradient(90deg, transparent 20%, rgba(42,0,7,0.68) 20% 34%, transparent 34%) !important;
}

.road-poneglyph-marker__glyph--4 {
  background:
    linear-gradient(90deg, transparent 15%, rgba(42,0,7,0.72) 15% 28%, transparent 28% 68%, rgba(42,0,7,0.62) 68% 81%, transparent 81%),
    linear-gradient(0deg, transparent 42%, rgba(42,0,7,0.64) 42% 56%, transparent 56%) !important;
}

.road-poneglyph-marker__glyph--5 {
  background:
    linear-gradient(45deg, transparent 42%, rgba(42,0,7,0.7) 42% 58%, transparent 58%),
    linear-gradient(0deg, transparent 18%, rgba(42,0,7,0.62) 18% 31%, transparent 31%) !important;
}

.road-poneglyph-marker__glyph--6 {
  background:
    linear-gradient(90deg, transparent 44%, rgba(42,0,7,0.72) 44% 58%, transparent 58%),
    linear-gradient(0deg, transparent 16%, rgba(42,0,7,0.62) 16% 29%, transparent 29% 68%, rgba(42,0,7,0.58) 68% 81%, transparent 81%) !important;
}

.road-poneglyph-marker__glyph--7 {
  background:
    radial-gradient(circle at 50% 50%, transparent 0 28%, rgba(42,0,7,0.7) 29% 42%, transparent 43%),
    linear-gradient(90deg, transparent 45%, rgba(42,0,7,0.54) 45% 56%, transparent 56%) !important;
}

.road-poneglyph-marker__glyph--8 {
  background:
    linear-gradient(90deg, rgba(42,0,7,0.68) 0 1px, transparent 1px calc(100% - 1px), rgba(42,0,7,0.68) calc(100% - 1px)),
    linear-gradient(0deg, transparent 48%, rgba(42,0,7,0.62) 48% 62%, transparent 62%) !important;
}

.road-poneglyph-marker__glyph--9 {
  background:
    linear-gradient(135deg, transparent 37%, rgba(42,0,7,0.66) 37% 51%, transparent 51%),
    linear-gradient(45deg, transparent 52%, rgba(42,0,7,0.58) 52% 65%, transparent 65%) !important;
}

.road-poneglyph-marker__label {
  white-space: nowrap;
}

@keyframes road-poneglyph-materialize {
  0% {
    opacity: 0;
    transform: translateY(12px) scale(0.86) rotate(-3deg);
    filter: blur(4px);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1) rotate(0);
    filter: blur(0);
  }
}

@keyframes road-poneglyph-signal {
  0% {
    opacity: 0.65;
    transform: scale(0.86);
  }
  72% {
    opacity: 0;
    transform: scale(1.1);
  }
  100% {
    opacity: 0;
    transform: scale(1.1);
  }
}

@keyframes road-poneglyph-stone-breathe {
  0%, 100% {
    box-shadow: inset 0 1px 0 rgba(255,255,255,0.14), 0 0 13px rgba(184,50,50,0.22);
  }
  50% {
    box-shadow: inset 0 1px 0 rgba(255,255,255,0.2), 0 0 22px rgba(184,50,50,0.38);
  }
}

@media (max-width: 480px) {
  .road-poneglyph-marker {
    max-width: calc(100vw - 32px);
    padding: 7px 8px;
    gap: 7px;
    font-size: 9px;
  }

  .road-poneglyph-marker__label {
    max-width: 150px;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
</style>
