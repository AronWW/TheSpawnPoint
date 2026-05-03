<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useAchievementStore } from '../stores/achievements'
import {
  LAUGH_TALE_ACHIEVEMENT_CODE,
  type LaughTaleClue,
  type LaughTaleClueCode,
  useLaughTaleQuestStore,
} from '../stores/laughTaleQuest'

const props = defineProps<{
  newlyDiscovered?: LaughTaleClueCode | null
}>()

const emit = defineEmits<{
  close: []
}>()

const quest = useLaughTaleQuestStore()
const achievementStore = useAchievementStore()

const selectedCode = ref<LaughTaleClueCode | 'INTRO' | 'FINAL'>('INTRO')
const animatedCode = ref<LaughTaleClueCode | null>(null)
const claiming = ref(false)
const openingCourse = ref(false)
const plottingCourse = ref(false)
const courseClaimed = ref(false)
const claimError = ref('')
const COURSE_OPENING_MS = 1900
const COURSE_ANIMATION_MS = 5600
const COURSE_REDUCED_MOTION_MS = 900
let animationTimer: number | null = null
let openingTimer: number | null = null
let finalRevealTimer: number | null = null
let courseTimer: number | null = null
let closeTimer: number | null = null

const selectedClue = computed<LaughTaleClue | null>(() => {
  if (selectedCode.value === 'INTRO' || selectedCode.value === 'FINAL') return null
  return quest.clues.find((item) => item.code === selectedCode.value) ?? null
})

const activeTitle = computed(() => {
  if (courseClaimed.value) return 'ЛАФТЕЛЬ ЗНАЙДЕНО'
  if (openingCourse.value) return 'ЛОГБУК РОЗГОРТАЄ МАПУ'
  if (plottingCourse.value || claiming.value) return 'КУРС ПРОКЛАДАЄТЬСЯ'
  if (selectedCode.value === 'FINAL') return 'КУРС СХОДИТЬСЯ'
  if (selectedClue.value) return selectedClue.value.title
  return 'ПОЧАТОК ВЕЛИКОЇ ПРИГОДИ'
})

const canCloseModal = computed(() => !openingCourse.value && !plottingCourse.value && !claiming.value)
const showFinalCourse = computed(() => quest.canClaim && selectedCode.value === 'FINAL')
const courseStatusText = computed(() => {
  if (courseClaimed.value) return 'Курс прокладено. Острів сміху знайдено.'
  if (claiming.value) return 'Остання точка відкрита. Логбук підтверджує маршрут.'
  if (plottingCourse.value) return 'Чотири червоні точки сходяться. Лафтель проявляється на перетині.'
  if (openingCourse.value) return 'Карта розгортається, а роуд понегліфи займають свої точки на морі.'
  return 'Чотири роуд понегліфи готові. Проклади курс і дай точкам перетнутися.'
})
const courseButtonText = computed(() => {
  if (courseClaimed.value) return 'ЛАФТЕЛЬ ЗНАЙДЕНО'
  if (claiming.value) return 'ВІДКРИВАЄМО ЛАФТЕЛЬ...'
  if (plottingCourse.value) return 'ПРОКЛАДАЄМО КУРС...'
  if (openingCourse.value) return 'РОЗГОРТАЄМО МАПУ...'
  return 'ПРОКЛАСТИ КУРС ДО ЛАФТЕЛЮ'
})

const currentPrompt = computed(() => {
  if (quest.canClaim) {
    return 'Усі чотири червоні камені знайдено. Логбук більше не порожній. Роджер сміявся, коли побачив правду. Тепер твоя черга.'
  }

  if (quest.progressCount === 0) {
    return 'Перший камінь лежить під прапором капітана. Не піднімай вітрило, поки не почуєш п’ятий удар барабана визволення.'
  }

  const foundClues = [...quest.discoveredClues]
  const lastFound = foundClues[foundClues.length - 1]
  return lastFound?.nextClue ?? 'Червоні камені мовчать. Потрібен наступний курс.'
})

function showDiscoveredClue(code: LaughTaleClueCode) {
  selectedCode.value = code
  animatedCode.value = code

  if (animationTimer !== null) {
    window.clearTimeout(animationTimer)
  }
  animationTimer = window.setTimeout(() => {
    animatedCode.value = null
    animationTimer = null
  }, 1400)
}

function stopCourseOpening() {
  openingCourse.value = false
  if (openingTimer !== null) {
    window.clearTimeout(openingTimer)
    openingTimer = null
  }
}

function selectSlot(code: LaughTaleClueCode) {
  stopCourseOpening()
  if (quest.hasDiscovered(code)) {
    selectedCode.value = code
    return
  }

  selectedCode.value = quest.currentClueCode === code ? 'INTRO' : code
}

async function claimLaughTale() {
  if (!quest.canClaim || openingCourse.value || claiming.value || plottingCourse.value || courseClaimed.value) return

  selectedCode.value = 'FINAL'
  plottingCourse.value = true
  courseClaimed.value = false
  claimError.value = ''

  const duration = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    ? COURSE_REDUCED_MOTION_MS
    : COURSE_ANIMATION_MS

  if (courseTimer !== null) {
    window.clearTimeout(courseTimer)
  }

  courseTimer = window.setTimeout(() => {
    courseTimer = null
    void finishLaughTaleClaim()
  }, duration)
}

function startCourseOpening() {
  if (!quest.canClaim || selectedCode.value !== 'FINAL') return

  openingCourse.value = true
  if (openingTimer !== null) {
    window.clearTimeout(openingTimer)
  }

  const duration = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    ? 120
    : COURSE_OPENING_MS

  openingTimer = window.setTimeout(() => {
    openingCourse.value = false
    openingTimer = null
  }, duration)
}

async function finishLaughTaleClaim() {
  claiming.value = true
  try {
    await achievementStore.claimSecret(LAUGH_TALE_ACHIEVEMENT_CODE)
    plottingCourse.value = false
    courseClaimed.value = true
    closeTimer = window.setTimeout(() => {
      emit('close')
    }, 900)
  } catch {
    plottingCourse.value = false
    courseClaimed.value = false
    claimError.value = 'Не вдалося прокласти курс. Спробуй ще раз.'
  } finally {
    claiming.value = false
  }
}

function closeModal() {
  if (!canCloseModal.value) return
  emit('close')
}

watch(
  () => props.newlyDiscovered,
  (code) => {
    if (!code) return

    showDiscoveredClue(code)
    if (quest.canClaim) {
      if (finalRevealTimer !== null) {
        window.clearTimeout(finalRevealTimer)
      }
      finalRevealTimer = window.setTimeout(() => {
        selectedCode.value = 'FINAL'
        startCourseOpening()
        finalRevealTimer = null
      }, 1650)
    }
  },
)

onMounted(() => {
  quest.start()

  if (props.newlyDiscovered) {
    showDiscoveredClue(props.newlyDiscovered)
    if (quest.canClaim) {
      finalRevealTimer = window.setTimeout(() => {
        selectedCode.value = 'FINAL'
        startCourseOpening()
        finalRevealTimer = null
      }, 1650)
    }
    return
  }

  if (quest.canClaim) {
    selectedCode.value = 'FINAL'
    startCourseOpening()
    return
  }

  const foundClues = [...quest.discoveredClues]
  const lastFound = foundClues[foundClues.length - 1]
  selectedCode.value = lastFound?.code ?? 'INTRO'
})

onBeforeUnmount(() => {
  if (animationTimer !== null) {
    window.clearTimeout(animationTimer)
  }
  if (openingTimer !== null) {
    window.clearTimeout(openingTimer)
  }
  if (finalRevealTimer !== null) {
    window.clearTimeout(finalRevealTimer)
  }
  if (courseTimer !== null) {
    window.clearTimeout(courseTimer)
  }
  if (closeTimer !== null) {
    window.clearTimeout(closeTimer)
  }
})
</script>

<template>
  <div class="laugh-tale-overlay" @click.self="closeModal">
    <section class="laugh-tale-modal">
      <header class="laugh-tale-header">
        <div>
          <div class="laugh-tale-eyebrow">СЕКРЕТНИЙ ЛОГБУК</div>
          <h2>ЛОГБУК ЛАФТЕЛЮ</h2>
          <p>Роуд понегліфи: {{ quest.progressCount }} / {{ quest.totalCount }}</p>
        </div>
        <button
          class="laugh-tale-close"
          type="button"
          aria-label="Закрити"
          :disabled="!canCloseModal"
          @click="closeModal"
        >×</button>
      </header>

      <div class="laugh-tale-progress" aria-hidden="true">
        <div class="laugh-tale-progress__fill" :class="{ complete: quest.canClaim }" :style="{ width: `${quest.progressPercent}%` }"></div>
      </div>

      <div class="laugh-tale-body">
        <div class="poneglyph-slots">
          <button
            v-for="clue in quest.clues"
            :key="clue.code"
            class="poneglyph-slot"
            :class="{
              unlocked: quest.hasDiscovered(clue.code),
              selected: selectedCode === clue.code,
              current: quest.currentClueCode === clue.code && !quest.hasDiscovered(clue.code),
              'is-new': animatedCode === clue.code,
            }"
            type="button"
            @click="selectSlot(clue.code)"
          >
            <span class="poneglyph-slot__stone">
              <span class="poneglyph-slot__rune-grid" aria-hidden="true">
                <i
                  v-for="rune in 36"
                  :key="rune"
                  :class="`poneglyph-slot__glyph--${rune % 12}`"
                ></i>
              </span>
              <span class="poneglyph-slot__rune">{{ quest.hasDiscovered(clue.code) ? clue.numeral : '???' }}</span>
            </span>
            <span class="poneglyph-slot__meta">
              <span>{{ clue.title }}</span>
              <small>{{ quest.hasDiscovered(clue.code) ? clue.location : 'Напис мовчить' }}</small>
            </span>
          </button>
        </div>

        <div class="laugh-tale-page">
          <div class="laugh-tale-page__title-row">
            <span class="laugh-tale-page__mark"></span>
            <h3>{{ activeTitle }}</h3>
          </div>

          <template v-if="showFinalCourse">
            <div
              class="laugh-tale-course"
              :class="{
                'is-idle': !openingCourse && !plottingCourse && !courseClaimed,
                'is-opening': openingCourse,
                'is-plotting': plottingCourse,
                'is-claiming': claiming,
                'is-complete': courseClaimed,
              }"
            >
              <div class="laugh-tale-course__map" aria-hidden="true">
                <svg class="laugh-tale-course__svg" viewBox="0 0 640 390">
                  <defs>
                    <radialGradient id="laugh-tale-island-aura" cx="50%" cy="50%" r="55%">
                      <stop offset="0%" stop-color="#f6d865" stop-opacity="0.48" />
                      <stop offset="58%" stop-color="#f5c518" stop-opacity="0.18" />
                      <stop offset="100%" stop-color="#f5c518" stop-opacity="0" />
                    </radialGradient>
                    <filter id="laugh-tale-glow" x="-40%" y="-40%" width="180%" height="180%">
                      <feGaussianBlur stdDeviation="5" result="blur" />
                      <feMerge>
                        <feMergeNode in="blur" />
                        <feMergeNode in="SourceGraphic" />
                      </feMerge>
                    </filter>
                  </defs>

                  <path class="course-current course-current--one" d="M68 72 C150 58 198 92 250 72 S350 54 428 82 536 72 588 56" />
                  <path class="course-current course-current--two" d="M56 292 C150 256 210 294 286 268 S424 220 594 280" />
                  <path class="course-current course-current--three" d="M112 336 C168 274 142 204 204 158 S296 94 316 42" />
                  <path class="course-current course-current--four" d="M528 330 C472 270 496 210 440 160 S360 86 356 42" />

                  <path class="course-route course-route--one" pathLength="100" d="M104 84 C170 110 238 138 320 198" />
                  <path class="course-route course-route--two" pathLength="100" d="M536 86 C470 116 392 144 320 198" />
                  <path class="course-route course-route--three" pathLength="100" d="M104 306 C174 270 244 236 320 198" />
                  <path class="course-route course-route--four" pathLength="100" d="M536 306 C462 268 392 236 320 198" />

                  <g class="course-crosshair">
                    <circle cx="320" cy="198" r="56" />
                    <circle cx="320" cy="198" r="32" />
                    <path d="M320 126 V170" />
                    <path d="M320 226 V270" />
                    <path d="M248 198 H292" />
                    <path d="M348 198 H392" />
                  </g>

                  <g class="course-island" filter="url(#laugh-tale-glow)">
                    <ellipse class="course-island__aura" cx="320" cy="204" rx="96" ry="66" fill="url(#laugh-tale-island-aura)" />
                    <path class="course-island__wake" d="M252 232 C280 222 302 226 320 232 C344 240 370 238 392 228" />
                    <path class="course-island__base" d="M268 219 C285 201 304 194 324 196 C346 198 366 207 378 222 C354 233 316 236 286 229 C277 227 271 224 268 219Z" />
                    <path class="course-island__shore" d="M282 219 C302 225 330 225 360 219" />
                    <path class="course-island__palm-trunk" d="M321 215 C323 202 324 190 322 178" />
                    <path class="course-island__palm-leaf" d="M322 179 C310 174 301 176 293 184" />
                    <path class="course-island__palm-leaf" d="M322 179 C333 171 346 171 356 179" />
                    <path class="course-island__palm-leaf" d="M322 179 C332 184 339 193 342 204" />
                    <path class="course-island__palm-leaf" d="M322 179 C314 186 309 196 307 207" />
                    <path class="course-island__mark" d="M320 160 V170 M315 165 H325" />
                  </g>
                </svg>

                <span class="course-stone course-stone--one">
                  <i v-for="rune in 12" :key="rune" :class="`course-stone__glyph--${rune % 6}`"></i>
                </span>
                <span class="course-stone course-stone--two">
                  <i v-for="rune in 12" :key="rune" :class="`course-stone__glyph--${rune % 6}`"></i>
                </span>
                <span class="course-stone course-stone--three">
                  <i v-for="rune in 12" :key="rune" :class="`course-stone__glyph--${rune % 6}`"></i>
                </span>
                <span class="course-stone course-stone--four">
                  <i v-for="rune in 12" :key="rune" :class="`course-stone__glyph--${rune % 6}`"></i>
                </span>

                <span class="course-compass">
                  <span></span>
                </span>
              </div>

              <div class="laugh-tale-course__caption">
                <span>{{ courseClaimed ? 'Острів сміху' : 'Останній курс' }}</span>
                <p>{{ courseStatusText }}</p>
              </div>
            </div>
          </template>

          <template v-else-if="selectedClue && quest.hasDiscovered(selectedClue.code)">
            <p class="laugh-tale-page__label">{{ selectedClue.location }}</p>
            <p class="laugh-tale-page__text">{{ selectedClue.inscription }}</p>
            <div class="laugh-tale-next">
              <span>Наступний курс</span>
              <p>{{ selectedClue.nextClue }}</p>
            </div>
          </template>

          <template v-else>
            <p class="laugh-tale-page__text">
              Чотири червоні камені не назвуть острів напряму. Вони лише залишать точки на карті. Коли точки перетнуться, дорога стане смішною.
            </p>
            <div class="laugh-tale-next">
              <span>{{ quest.canClaim ? 'Останній запис' : 'Поточна загадка' }}</span>
              <p>{{ currentPrompt }}</p>
            </div>
          </template>

          <button
            v-if="quest.canClaim"
            class="laugh-tale-claim"
            type="button"
            :disabled="openingCourse || claiming || plottingCourse || courseClaimed"
            @click="claimLaughTale"
          >
            {{ courseButtonText }}
          </button>
          <p v-if="claimError" class="laugh-tale-error">{{ claimError }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.laugh-tale-overlay {
  position: fixed;
  inset: 0;
  z-index: 1160;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 22px;
  background:
    radial-gradient(circle at 50% 35%, rgba(170, 38, 38, 0.16), transparent 34%),
    rgba(0, 0, 0, 0.84);
}

.laugh-tale-modal {
  width: min(980px, 100%);
  max-height: 88vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #111214;
  border: 2px solid rgba(184, 50, 50, 0.45);
  box-shadow: 0 26px 72px rgba(0, 0, 0, 0.68), 0 0 46px rgba(184, 50, 50, 0.12);
}

.laugh-tale-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 20px 22px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.laugh-tale-eyebrow,
.laugh-tale-header p,
.laugh-tale-page__label,
.laugh-tale-next span {
  color: var(--gray);
  font-family: var(--font-display);
  font-size: 10px;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.laugh-tale-header h2 {
  margin: 8px 0 8px;
  color: var(--white);
  font-family: var(--font-display);
  font-size: 24px;
  letter-spacing: 2.4px;
}

.laugh-tale-header p {
  margin: 0;
  color: #d77272;
}

.laugh-tale-close {
  flex: 0 0 auto;
  width: 34px;
  height: 34px;
  border: 1px solid var(--border);
  background: var(--dark);
  color: var(--gray-light);
  cursor: pointer;
  font-size: 22px;
  line-height: 1;
}

.laugh-tale-close:hover {
  border-color: var(--red);
  color: var(--red);
}

.laugh-tale-close:disabled {
  opacity: 0.45;
  cursor: default;
}

.laugh-tale-close:disabled:hover {
  border-color: var(--border);
  color: var(--gray-light);
}

.laugh-tale-progress {
  height: 8px;
  background: #1b1d21;
}

.laugh-tale-progress__fill {
  height: 100%;
  background: linear-gradient(90deg, #8b2020, #d65b5b);
  transition: width 0.35s ease;
}

.laugh-tale-progress__fill.complete {
  background: linear-gradient(90deg, #b83232, var(--yellow), #b83232);
}

.laugh-tale-body {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(280px, 330px) minmax(0, 1fr);
  align-items: start;
  gap: 20px;
  padding: 20px 22px 22px;
  overflow: auto;
}

.poneglyph-slots {
  min-width: 0;
  display: grid;
  gap: 10px;
}

.poneglyph-slot {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 82px;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.09);
  background: #0c0d0f;
  color: var(--gray-light);
  text-align: left;
  cursor: pointer;
  overflow: hidden;
}

.poneglyph-slot::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(110deg, transparent, rgba(184, 50, 50, 0.14), transparent);
  opacity: 0;
  transform: translateX(-100%);
  pointer-events: none;
}

.poneglyph-slot.unlocked,
.poneglyph-slot.selected {
  border-color: rgba(184, 50, 50, 0.55);
}

.poneglyph-slot.current {
  border-color: rgba(245, 197, 24, 0.28);
}

.poneglyph-slot:hover {
  border-color: rgba(184, 50, 50, 0.7);
}

.poneglyph-slot.is-new {
  animation: slot-arrive 0.8s cubic-bezier(0.18, 0.86, 0.28, 1);
}

.poneglyph-slot.is-new::after {
  animation: rune-sweep 1.1s ease 0.15s;
}

.poneglyph-slot__stone {
  flex: 0 0 auto;
  position: relative;
  width: 58px;
  height: 58px;
  display: grid;
  place-items: center;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(255,255,255,0.04), transparent 34%),
    linear-gradient(180deg, #292b2f, #17181b);
  border: 1px solid rgba(255, 255, 255, 0.09);
  border-radius: 3px;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.08);
}

.poneglyph-slot.unlocked .poneglyph-slot__stone {
  background:
    linear-gradient(135deg, rgba(255,255,255,0.1), transparent 24%),
    radial-gradient(circle at 24% 18%, rgba(255,120,120,0.16), transparent 30%),
    linear-gradient(180deg, #ba2b2f 0%, #8f1f25 48%, #5e1218 100%);
  border-color: rgba(255, 140, 140, 0.42);
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.18), inset 0 -14px 24px rgba(45,0,0,0.3), 0 0 18px rgba(184, 50, 50, 0.24);
}

.poneglyph-slot__stone::before {
  content: '';
  position: absolute;
  inset: 5px;
  background:
    repeating-linear-gradient(0deg, transparent 0 8px, rgba(20,20,23,0.38) 8px 9px),
    repeating-linear-gradient(90deg, transparent 0 8px, rgba(20,20,23,0.28) 8px 9px);
  opacity: 0.72;
  pointer-events: none;
}

.poneglyph-slot.unlocked .poneglyph-slot__stone::before {
  background:
    repeating-linear-gradient(0deg, transparent 0 8px, rgba(60,0,8,0.24) 8px 9px),
    repeating-linear-gradient(90deg, transparent 0 8px, rgba(60,0,8,0.2) 8px 9px);
  opacity: 0.9;
}

.poneglyph-slot__stone::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(255,255,255,0.08), transparent 20%, transparent 78%, rgba(0,0,0,0.2)),
    linear-gradient(180deg, rgba(255,255,255,0.08), transparent 30%, rgba(0,0,0,0.18));
  pointer-events: none;
}

.poneglyph-slot__rune-grid {
  position: absolute;
  inset: 4px;
  overflow: hidden;
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  grid-auto-rows: 1fr;
  gap: 2px;
  pointer-events: none;
}

.poneglyph-slot.unlocked .poneglyph-slot__rune-grid {
  opacity: 1;
}

.poneglyph-slot__rune-grid i {
  position: relative;
  min-width: 0;
  min-height: 0;
  border: 1px solid rgba(12, 12, 14, 0.38);
  background:
    linear-gradient(90deg, transparent 45%, rgba(12,12,14,0.58) 45% 57%, transparent 57%),
    linear-gradient(0deg, transparent 43%, rgba(12,12,14,0.46) 43% 55%, transparent 55%);
}

.poneglyph-slot.unlocked .poneglyph-slot__rune-grid i {
  border-color: rgba(42, 0, 7, 0.44);
  background:
    linear-gradient(90deg, transparent 45%, rgba(42,0,7,0.76) 45% 57%, transparent 57%),
    linear-gradient(0deg, transparent 43%, rgba(42,0,7,0.58) 43% 55%, transparent 55%);
}

.poneglyph-slot__rune-grid i::before,
.poneglyph-slot__rune-grid i::after {
  content: '';
  position: absolute;
  background: rgba(12, 12, 14, 0.56);
}

.poneglyph-slot.unlocked .poneglyph-slot__rune-grid i::before,
.poneglyph-slot.unlocked .poneglyph-slot__rune-grid i::after {
  background: rgba(42, 0, 7, 0.74);
}

.poneglyph-slot__rune-grid i::before {
  left: 1px;
  right: 1px;
  top: 1px;
  height: 1px;
}

.poneglyph-slot__rune-grid i::after {
  top: 1px;
  bottom: 1px;
  right: 1px;
  width: 1px;
}

.poneglyph-slot__glyph--1 {
  background:
    linear-gradient(90deg, rgba(42,0,7,0.72) 0 1px, transparent 1px),
    linear-gradient(0deg, transparent 55%, rgba(42,0,7,0.72) 55% 68%, transparent 68%) !important;
}

.poneglyph-slot__glyph--2 {
  background:
    linear-gradient(135deg, transparent 42%, rgba(42,0,7,0.72) 42% 58%, transparent 58%),
    linear-gradient(90deg, transparent 62%, rgba(42,0,7,0.62) 62% 76%, transparent 76%) !important;
}

.poneglyph-slot__glyph--3 {
  background:
    linear-gradient(0deg, rgba(42,0,7,0.72) 0 1px, transparent 1px),
    linear-gradient(90deg, transparent 20%, rgba(42,0,7,0.68) 20% 34%, transparent 34%) !important;
}

.poneglyph-slot__glyph--4 {
  background:
    linear-gradient(90deg, transparent 15%, rgba(42,0,7,0.72) 15% 28%, transparent 28% 68%, rgba(42,0,7,0.62) 68% 81%, transparent 81%),
    linear-gradient(0deg, transparent 42%, rgba(42,0,7,0.64) 42% 56%, transparent 56%) !important;
}

.poneglyph-slot__glyph--5 {
  background:
    linear-gradient(45deg, transparent 42%, rgba(42,0,7,0.7) 42% 58%, transparent 58%),
    linear-gradient(0deg, transparent 18%, rgba(42,0,7,0.62) 18% 31%, transparent 31%) !important;
}

.poneglyph-slot__glyph--6 {
  background:
    linear-gradient(90deg, transparent 44%, rgba(42,0,7,0.72) 44% 58%, transparent 58%),
    linear-gradient(0deg, transparent 16%, rgba(42,0,7,0.62) 16% 29%, transparent 29% 68%, rgba(42,0,7,0.58) 68% 81%, transparent 81%) !important;
}

.poneglyph-slot__glyph--7 {
  background:
    radial-gradient(circle at 50% 50%, transparent 0 28%, rgba(42,0,7,0.72) 29% 42%, transparent 43%),
    linear-gradient(90deg, transparent 45%, rgba(42,0,7,0.54) 45% 56%, transparent 56%) !important;
}

.poneglyph-slot__glyph--8 {
  background:
    linear-gradient(90deg, rgba(42,0,7,0.68) 0 1px, transparent 1px calc(100% - 1px), rgba(42,0,7,0.68) calc(100% - 1px)),
    linear-gradient(0deg, transparent 48%, rgba(42,0,7,0.62) 48% 62%, transparent 62%) !important;
}

.poneglyph-slot__glyph--9 {
  background:
    linear-gradient(135deg, transparent 37%, rgba(42,0,7,0.66) 37% 51%, transparent 51%),
    linear-gradient(45deg, transparent 52%, rgba(42,0,7,0.58) 52% 65%, transparent 65%) !important;
}

.poneglyph-slot__glyph--10 {
  background:
    linear-gradient(90deg, transparent 18%, rgba(42,0,7,0.66) 18% 31%, transparent 31% 66%, rgba(42,0,7,0.66) 66% 79%, transparent 79%),
    linear-gradient(0deg, rgba(42,0,7,0.66) 0 1px, transparent 1px calc(100% - 1px), rgba(42,0,7,0.66) calc(100% - 1px)) !important;
}

.poneglyph-slot__glyph--11 {
  background:
    radial-gradient(circle at 26% 28%, rgba(42,0,7,0.72) 0 1px, transparent 2px),
    radial-gradient(circle at 72% 72%, rgba(42,0,7,0.72) 0 1px, transparent 2px),
    linear-gradient(0deg, transparent 45%, rgba(42,0,7,0.58) 45% 58%, transparent 58%) !important;
}

.poneglyph-slot__rune {
  position: relative;
  z-index: 1;
  min-width: 26px;
  padding: 3px 5px;
  color: rgba(255, 255, 255, 0.58);
  font-family: var(--font-display);
  font-size: 13px;
  letter-spacing: 1px;
  text-align: center;
  background: rgba(0,0,0,0.28);
  border: 1px solid rgba(255,255,255,0.08);
}

.poneglyph-slot.unlocked .poneglyph-slot__rune {
  color: #ffe1e1;
  background: rgba(36, 0, 6, 0.52);
  border-color: rgba(255, 185, 185, 0.22);
}

.poneglyph-slot__meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.poneglyph-slot__meta span {
  color: var(--white);
  font-weight: 700;
  line-height: 1.2;
}

.poneglyph-slot__meta small {
  color: var(--gray);
  font-size: 11px;
  letter-spacing: 0.5px;
}

.laugh-tale-page {
  min-width: 0;
  min-height: 420px;
  box-sizing: border-box;
  overflow: hidden;
  padding: 22px;
  background:
    linear-gradient(135deg, rgba(184, 50, 50, 0.05), transparent 30%),
    #0d0e10;
  border: 1px solid rgba(255, 255, 255, 0.09);
}

.laugh-tale-page__title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.laugh-tale-page__mark {
  position: relative;
  width: 20px;
  height: 30px;
  overflow: hidden;
  border: 1px solid rgba(255, 140, 140, 0.38);
  border-radius: 2px;
  background:
    linear-gradient(135deg, rgba(255,255,255,0.1), transparent 28%),
    linear-gradient(180deg, #ba2b2f, #64151a);
  box-shadow: 0 0 18px rgba(184, 50, 50, 0.32), inset 0 -8px 12px rgba(45,0,0,0.28);
}

.laugh-tale-page__mark::before {
  content: '┌┐└┘├┤┬┴┼╎╏╍┌┘└┐├┤┬┴┼╎╏';
  position: absolute;
  inset: 2px;
  overflow: hidden;
  color: rgba(42, 0, 7, 0.76);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 4px;
  font-weight: 950;
  line-height: 4.6px;
  word-break: break-all;
  text-shadow: 0 0 0 currentColor;
}

.laugh-tale-page h3 {
  margin: 0;
  color: var(--yellow);
  font-family: var(--font-display);
  font-size: 18px;
  letter-spacing: 2px;
}

.laugh-tale-page__label {
  margin: 0 0 12px;
  color: #d77272;
}

.laugh-tale-page__text,
.laugh-tale-next p {
  color: var(--gray-light);
  line-height: 1.62;
}

.laugh-tale-page__text {
  margin: 0 0 18px;
  font-size: 15px;
}

.laugh-tale-next {
  margin-top: 18px;
  padding: 16px;
  border: 1px solid rgba(184, 50, 50, 0.28);
  background: rgba(184, 50, 50, 0.05);
}

.laugh-tale-next p {
  margin: 8px 0 0;
}

.laugh-tale-course {
  min-width: 0;
  display: grid;
  gap: 14px;
}

.laugh-tale-course__map {
  position: relative;
  height: clamp(250px, 38vh, 330px);
  min-height: 0;
  overflow: hidden;
  border: 1px solid rgba(184, 50, 50, 0.28);
  background:
    radial-gradient(circle at 50% 50%, rgba(184,50,50,0.12), transparent 25%),
    radial-gradient(circle at 18% 22%, rgba(245,197,24,0.06), transparent 18%),
    radial-gradient(circle at 82% 78%, rgba(245,197,24,0.05), transparent 20%),
    linear-gradient(135deg, rgba(255,255,255,0.03), transparent 34%),
    #090a0c;
  box-shadow: inset 0 0 0 1px rgba(255,255,255,0.03), inset 0 -32px 60px rgba(0,0,0,0.36);
  transform-origin: 50% 0;
}

.laugh-tale-course__map::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(rgba(255,255,255,0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.035) 1px, transparent 1px),
    radial-gradient(ellipse at 50% 50%, transparent 0 42%, rgba(0,0,0,0.42) 100%);
  background-size: 32px 32px, 32px 32px, auto;
  opacity: 0.62;
  pointer-events: none;
}

.laugh-tale-course__map::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(110deg, transparent 0 28%, rgba(245,197,24,0.12) 42%, transparent 56% 100%);
  opacity: 0;
  transform: translateX(-120%);
  pointer-events: none;
}

.laugh-tale-course.is-plotting .laugh-tale-course__map::after {
  animation: course-map-scan 1.25s ease 3.8s forwards;
}

.laugh-tale-course.is-opening .laugh-tale-course__map {
  animation: course-map-unfold 0.82s cubic-bezier(0.18, 0.82, 0.22, 1) both;
}

.laugh-tale-course.is-complete .laugh-tale-course__map {
  border-color: rgba(245, 197, 24, 0.52);
  box-shadow: inset 0 0 0 1px rgba(245,197,24,0.08), 0 0 28px rgba(245,197,24,0.12);
}

.laugh-tale-course__svg {
  position: absolute;
  inset: 0;
  z-index: 2;
  width: 100%;
  height: 100%;
  display: block;
}

.course-current {
  fill: none;
  stroke: rgba(255,255,255,0.08);
  stroke-width: 1.2;
  stroke-dasharray: 7 11;
}

.course-current--two,
.course-current--four {
  stroke: rgba(184,50,50,0.16);
}

.laugh-tale-course.is-opening .course-current {
  opacity: 0;
  animation: course-current-fade 0.72s ease 0.72s forwards;
}

.course-route {
  fill: none;
  stroke: #d65353;
  stroke-width: 3.4;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-dasharray: 100;
  stroke-dashoffset: 100;
  opacity: 0;
  filter: drop-shadow(0 0 6px rgba(184,50,50,0.55));
}

.laugh-tale-course.is-idle .course-route {
  stroke-dasharray: 1 9;
  stroke-dashoffset: 0;
  opacity: 0.2;
}

.laugh-tale-course.is-plotting .course-route--one {
  animation: course-route-draw 1.1s ease 0.45s forwards;
}

.laugh-tale-course.is-plotting .course-route--two {
  animation: course-route-draw 1.1s ease 1.15s forwards;
}

.laugh-tale-course.is-plotting .course-route--three {
  animation: course-route-draw 1.1s ease 1.85s forwards;
}

.laugh-tale-course.is-plotting .course-route--four {
  animation: course-route-draw 1.1s ease 2.55s forwards;
}

.laugh-tale-course.is-claiming .course-route,
.laugh-tale-course.is-complete .course-route {
  stroke-dasharray: 100;
  stroke-dashoffset: 0;
  opacity: 1;
}

.course-crosshair {
  opacity: 0;
  transform: scale(0.78);
  transform-box: fill-box;
  transform-origin: center;
}

.course-crosshair circle,
.course-crosshair path {
  fill: none;
  stroke: rgba(245,197,24,0.72);
  stroke-width: 1.6;
  stroke-linecap: round;
}

.course-crosshair circle:first-child {
  stroke: rgba(245,197,24,0.32);
  stroke-dasharray: 5 8;
}

.laugh-tale-course.is-plotting .course-crosshair {
  animation: course-crosshair-lock 0.78s ease 3.55s forwards;
}

.laugh-tale-course.is-claiming .course-crosshair,
.laugh-tale-course.is-complete .course-crosshair {
  opacity: 1;
  transform: scale(1);
}

.course-island {
  opacity: 0;
  transform: scale(0.58);
  transform-box: fill-box;
  transform-origin: center;
}

.laugh-tale-course.is-plotting .course-island {
  animation: course-island-reveal 1.05s cubic-bezier(0.16, 0.92, 0.22, 1.14) 4.25s forwards;
}

.laugh-tale-course.is-claiming .course-island,
.laugh-tale-course.is-complete .course-island {
  opacity: 1;
  transform: scale(1);
}

.course-island__aura {
  opacity: 0.55;
}

.course-island__base {
  fill: rgba(255,255,255,0.08);
  stroke: rgba(255,255,255,0.92);
  stroke-width: 2.4;
}

.course-island__wake,
.course-island__shore,
.course-island__palm-trunk,
.course-island__palm-leaf,
.course-island__mark {
  fill: none;
  stroke: rgba(255,255,255,0.95);
  stroke-width: 2.2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.course-island__mark {
  stroke: rgba(245,197,24,0.95);
  stroke-width: 1.8;
}

.course-island__wake {
  stroke: rgba(255,255,255,0.38);
  stroke-width: 2.1;
}

.course-island__shore {
  stroke: rgba(255,255,255,0.56);
  stroke-width: 1.7;
}

.course-island__palm-trunk {
  stroke: rgba(255,255,255,0.98);
  stroke-width: 3.1;
}

.course-island__palm-leaf {
  stroke: rgba(255,255,255,0.98);
  stroke-width: 3;
}

.course-stone {
  position: absolute;
  z-index: 3;
  width: 42px;
  height: 46px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-auto-rows: 1fr;
  gap: 1px;
  padding: 4px;
  border: 1px solid rgba(255, 140, 140, 0.42);
  background:
    linear-gradient(135deg, rgba(255,255,255,0.1), transparent 24%),
    radial-gradient(circle at 24% 18%, rgba(255,120,120,0.16), transparent 30%),
    linear-gradient(180deg, #ba2b2f 0%, #8f1f25 48%, #5e1218 100%);
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.16), 0 0 18px rgba(184,50,50,0.22);
}

.course-stone--one {
  left: 13%;
  top: 16%;
}

.course-stone--two {
  right: 13%;
  top: 16%;
}

.course-stone--three {
  left: 13%;
  bottom: 15%;
}

.course-stone--four {
  right: 13%;
  bottom: 15%;
}

.laugh-tale-course.is-opening .course-stone {
  opacity: 0;
}

.laugh-tale-course.is-opening .course-stone--one {
  animation: course-stone-insert 0.54s cubic-bezier(0.18, 0.88, 0.24, 1.18) 0.34s forwards;
}

.laugh-tale-course.is-opening .course-stone--two {
  animation: course-stone-insert 0.54s cubic-bezier(0.18, 0.88, 0.24, 1.18) 0.56s forwards;
}

.laugh-tale-course.is-opening .course-stone--three {
  animation: course-stone-insert 0.54s cubic-bezier(0.18, 0.88, 0.24, 1.18) 0.78s forwards;
}

.laugh-tale-course.is-opening .course-stone--four {
  animation: course-stone-insert 0.54s cubic-bezier(0.18, 0.88, 0.24, 1.18) 1s forwards;
}

.laugh-tale-course.is-plotting .course-stone--one {
  animation: course-stone-pulse 0.62s ease 0.25s both;
}

.laugh-tale-course.is-plotting .course-stone--two {
  animation: course-stone-pulse 0.62s ease 0.95s both;
}

.laugh-tale-course.is-plotting .course-stone--three {
  animation: course-stone-pulse 0.62s ease 1.65s both;
}

.laugh-tale-course.is-plotting .course-stone--four {
  animation: course-stone-pulse 0.62s ease 2.35s both;
}

.course-stone i {
  position: relative;
  border: 1px solid rgba(42,0,7,0.45);
  background:
    linear-gradient(90deg, transparent 45%, rgba(42,0,7,0.76) 45% 57%, transparent 57%),
    linear-gradient(0deg, transparent 43%, rgba(42,0,7,0.58) 43% 55%, transparent 55%);
}

.course-stone i::before,
.course-stone i::after {
  content: '';
  position: absolute;
  background: rgba(42,0,7,0.74);
}

.course-stone i::before {
  left: 1px;
  right: 1px;
  top: 1px;
  height: 1px;
}

.course-stone i::after {
  top: 1px;
  bottom: 1px;
  right: 1px;
  width: 1px;
}

.course-stone__glyph--1 {
  background:
    linear-gradient(135deg, transparent 40%, rgba(42,0,7,0.72) 40% 58%, transparent 58%),
    linear-gradient(90deg, transparent 64%, rgba(42,0,7,0.58) 64% 78%, transparent 78%) !important;
}

.course-stone__glyph--2 {
  background:
    radial-gradient(circle at 50% 50%, transparent 0 28%, rgba(42,0,7,0.72) 29% 42%, transparent 43%),
    linear-gradient(90deg, transparent 45%, rgba(42,0,7,0.54) 45% 56%, transparent 56%) !important;
}

.course-stone__glyph--3 {
  background:
    linear-gradient(90deg, transparent 15%, rgba(42,0,7,0.72) 15% 28%, transparent 28% 68%, rgba(42,0,7,0.62) 68% 81%, transparent 81%),
    linear-gradient(0deg, transparent 42%, rgba(42,0,7,0.64) 42% 56%, transparent 56%) !important;
}

.course-stone__glyph--4 {
  background:
    linear-gradient(45deg, transparent 42%, rgba(42,0,7,0.7) 42% 58%, transparent 58%),
    linear-gradient(0deg, transparent 18%, rgba(42,0,7,0.62) 18% 31%, transparent 31%) !important;
}

.course-stone__glyph--5 {
  background:
    linear-gradient(90deg, rgba(42,0,7,0.68) 0 1px, transparent 1px calc(100% - 1px), rgba(42,0,7,0.68) calc(100% - 1px)),
    linear-gradient(0deg, transparent 48%, rgba(42,0,7,0.62) 48% 62%, transparent 62%) !important;
}

.course-compass {
  position: absolute;
  left: 50%;
  bottom: 20px;
  z-index: 3;
  width: 54px;
  height: 54px;
  border: 1px solid rgba(245,197,24,0.18);
  border-radius: 50%;
  transform: translateX(-50%);
  opacity: 0.55;
}

.course-compass::before,
.course-compass::after,
.course-compass span {
  content: '';
  position: absolute;
  inset: 50% auto auto 50%;
  width: 1px;
  height: 42px;
  background: linear-gradient(180deg, rgba(245,197,24,0.66), transparent);
  transform-origin: 0 0;
}

.course-compass::before {
  transform: rotate(0deg) translateY(-21px);
}

.course-compass::after {
  transform: rotate(90deg) translateY(-21px);
}

.course-compass span {
  transform: rotate(45deg) translateY(-21px);
}

.laugh-tale-course.is-plotting .course-compass {
  animation: course-compass-spin 5s cubic-bezier(0.2, 0.72, 0.16, 1) forwards;
}

.laugh-tale-course.is-opening .course-compass {
  opacity: 0;
  animation: course-compass-appear 0.7s ease 1.12s forwards;
}

.laugh-tale-course__caption {
  padding: 14px 16px;
  border: 1px solid rgba(245,197,24,0.18);
  background: rgba(245,197,24,0.04);
}

.laugh-tale-course__caption span {
  color: var(--yellow);
  font-family: var(--font-display);
  font-size: 10px;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.laugh-tale-course__caption p {
  margin: 8px 0 0;
  color: var(--gray-light);
  line-height: 1.55;
}

.laugh-tale-claim {
  margin-top: 20px;
  border: 1px solid var(--yellow);
  background: transparent;
  color: var(--yellow);
  padding: 12px 15px;
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 2px;
  cursor: pointer;
}

.laugh-tale-claim:hover:not(:disabled) {
  background: var(--yellow);
  color: var(--black);
}

.laugh-tale-claim:disabled {
  opacity: 0.65;
  cursor: default;
}

.laugh-tale-error {
  margin: 12px 0 0;
  color: var(--red);
  font-size: 13px;
}

@keyframes slot-arrive {
  0% { transform: translateY(-12px) scale(0.88); opacity: 0.3; }
  55% { transform: translateY(0) scale(1.04); opacity: 1; }
  100% { transform: translateY(0) scale(1); opacity: 1; }
}

@keyframes rune-sweep {
  0% { opacity: 0; transform: translateX(-100%); }
  25% { opacity: 1; }
  100% { opacity: 0; transform: translateX(100%); }
}

@keyframes course-map-unfold {
  0% {
    opacity: 0;
    transform: perspective(900px) rotateX(-18deg) scaleY(0.72);
    filter: blur(3px);
  }
  56% {
    opacity: 1;
    transform: perspective(900px) rotateX(3deg) scaleY(1.02);
    filter: blur(0);
  }
  100% {
    opacity: 1;
    transform: perspective(900px) rotateX(0) scaleY(1);
    filter: blur(0);
  }
}

@keyframes course-current-fade {
  0% { opacity: 0; }
  100% { opacity: 1; }
}

@keyframes course-stone-insert {
  0% {
    opacity: 0;
    transform: translateY(18px) scale(0.82);
    box-shadow: inset 0 1px 0 rgba(255,255,255,0.16), 0 0 0 rgba(184,50,50,0);
  }
  70% {
    opacity: 1;
    transform: translateY(-3px) scale(1.08);
    box-shadow: inset 0 1px 0 rgba(255,255,255,0.2), 0 0 26px rgba(255,120,120,0.48);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
    box-shadow: inset 0 1px 0 rgba(255,255,255,0.16), 0 0 18px rgba(184,50,50,0.22);
  }
}

@keyframes course-route-draw {
  0% {
    opacity: 0;
    stroke-dashoffset: 100;
  }
  12% {
    opacity: 1;
  }
  100% {
    opacity: 1;
    stroke-dashoffset: 0;
  }
}

@keyframes course-crosshair-lock {
  0% {
    opacity: 0;
    transform: scale(0.72) rotate(-10deg);
  }
  70% {
    opacity: 1;
    transform: scale(1.12) rotate(2deg);
  }
  100% {
    opacity: 1;
    transform: scale(1) rotate(0);
  }
}

@keyframes course-island-reveal {
  0% {
    opacity: 0;
    transform: scale(0.52);
  }
  58% {
    opacity: 1;
    transform: scale(1.12);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes course-stone-pulse {
  0% {
    transform: scale(1);
    box-shadow: inset 0 1px 0 rgba(255,255,255,0.16), 0 0 18px rgba(184,50,50,0.22);
  }
  48% {
    transform: scale(1.12);
    box-shadow: inset 0 1px 0 rgba(255,255,255,0.2), 0 0 28px rgba(255,120,120,0.46);
  }
  100% {
    transform: scale(1);
    box-shadow: inset 0 1px 0 rgba(255,255,255,0.16), 0 0 18px rgba(184,50,50,0.22);
  }
}

@keyframes course-map-scan {
  0% {
    opacity: 0;
    transform: translateX(-120%);
  }
  24% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translateX(120%);
  }
}

@keyframes course-compass-spin {
  0% {
    opacity: 0.42;
    transform: translateX(-50%) rotate(-18deg);
  }
  70% {
    opacity: 0.88;
    transform: translateX(-50%) rotate(380deg);
  }
  100% {
    opacity: 0.65;
    transform: translateX(-50%) rotate(360deg);
  }
}

@keyframes course-compass-appear {
  0% {
    opacity: 0;
    transform: translateX(-50%) scale(0.84) rotate(-24deg);
  }
  100% {
    opacity: 0.55;
    transform: translateX(-50%) scale(1) rotate(0);
  }
}

@media (max-width: 820px) {
  .laugh-tale-overlay {
    align-items: stretch;
    padding: 12px;
  }

  .laugh-tale-modal {
    max-height: none;
  }

  .laugh-tale-body {
    grid-template-columns: 1fr;
    padding: 16px 14px;
  }

  .poneglyph-slots {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .poneglyph-slot {
    min-height: 112px;
    align-items: flex-start;
    flex-direction: column;
  }

  .laugh-tale-header {
    padding-left: 14px;
    padding-right: 14px;
  }

  .laugh-tale-course__map {
    height: clamp(230px, 40vh, 270px);
    min-height: 0;
  }

  .course-stone {
    width: 34px;
    height: 38px;
    padding: 3px;
  }

  .course-compass {
    width: 42px;
    height: 42px;
    bottom: 14px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .laugh-tale-course.is-opening .laugh-tale-course__map,
  .laugh-tale-course.is-opening .course-current,
  .laugh-tale-course.is-opening .course-stone,
  .laugh-tale-course.is-opening .course-compass,
  .laugh-tale-course.is-plotting .course-route,
  .laugh-tale-course.is-plotting .course-crosshair,
  .laugh-tale-course.is-plotting .course-island,
  .laugh-tale-course.is-plotting .course-stone,
  .laugh-tale-course.is-plotting .course-compass,
  .laugh-tale-course.is-plotting .laugh-tale-course__map::after {
    animation-duration: 0.01ms;
    animation-delay: 0ms;
  }
}
</style>
