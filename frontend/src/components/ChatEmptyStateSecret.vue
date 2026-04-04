<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { useAchievementStore } from '../stores/achievements'
import ChatEmptyStateBase from './ChatEmptyStateBase.vue'

const ROOM_OF_REQUIREMENT_CODE = 'ROOM_OF_REQUIREMENT'
const RICKROLL_URL = 'https://youtu.be/eBGIQ7ZuuiU?si=75MTowZvkXQfVzLv'
const RICKROLL_PENDING_KEY = 'tsp_rickroll_pending'
const SUMMON_TRIGGER_CLICKS = 9
const SUMMON_DURATION_MS = 5200
const OPENING_DURATION_MS = 5400

const emit = defineEmits<{
  (e: 'scene-lock'): void
}>()

const achievementStore = useAchievementStore()

const clickCount = ref(0)
const stage = ref<'idle' | 'summoning' | 'formed' | 'opening'>('idle')
const claimInFlight = ref<Set<string>>(new Set())
const timers: number[] = []

const particleSeed = Array.from({ length: 26 }, (_, index) => {
  const angle = (index / 26) * Math.PI * 2
  const orbit = 90 + (index % 7) * 17
  const drift = 18 + (index % 5) * 9
  return {
    id: index,
    style: {
      '--orbit-x': `${(Math.cos(angle) * orbit).toFixed(1)}px`,
      '--orbit-y': `${(Math.sin(angle) * orbit * 0.68).toFixed(1)}px`,
      '--drift': `${drift}px`,
      '--delay': `${(index % 8) * 0.16}s`,
      '--duration': `${4.2 + (index % 5) * 0.5}s`,
      '--size': `${4 + (index % 4) * 2}px`,
    },
  }
})

const lightRays = Array.from({ length: 12 }, (_, i) => ({
  id: i,
  style: {
    '--ray-angle': `${(i / 12) * 360}deg`,
    '--ray-delay': `${i * 0.08}s`,
    '--ray-length': `${60 + (i % 3) * 25}%`,
    '--ray-opacity': `${0.15 + (i % 4) * 0.06}`,
  },
}))

const sceneActive = computed(() => stage.value !== 'idle')
const doorVisible = computed(() => stage.value !== 'idle')
const doorInteractive = computed(() => stage.value === 'formed')
const isOpening = computed(() => stage.value === 'opening')

function schedule(callback: () => void, delay: number) {
  const timer = window.setTimeout(callback, delay)
  timers.push(timer)
  return timer
}

function clearTimers() {
  while (timers.length > 0) {
    const timer = timers.pop()
    if (timer !== undefined) window.clearTimeout(timer)
  }
}

async function claimSecretOnce(code: string) {
  if (claimInFlight.value.has(code) || achievementStore.hasAchievement(code)) return

  claimInFlight.value.add(code)
  try {
    await achievementStore.claimSecret(code)
  } catch (error) {
    console.error(`Failed to claim secret achievement: ${code}`, error)
  } finally {
    claimInFlight.value.delete(code)
  }
}

async function finalizeSummoning() {
  if (stage.value !== 'summoning') return
  await claimSecretOnce(ROOM_OF_REQUIREMENT_CODE)
  if (stage.value === 'summoning') {
    stage.value = 'formed'
  }
}

function startSummoning() {
  if (stage.value !== 'idle') return

  emit('scene-lock')
  stage.value = 'summoning'
  schedule(() => {
    void finalizeSummoning()
  }, SUMMON_DURATION_MS)
}

function handleBackgroundClick() {
  if (stage.value !== 'idle') return

  clickCount.value += 1
  if (clickCount.value >= SUMMON_TRIGGER_CLICKS) {
    startSummoning()
  }
}

function handleDoorClick() {
  if (!doorInteractive.value) return

  stage.value = 'opening'

  try {
    localStorage.setItem(RICKROLL_PENDING_KEY, Date.now().toString())
  } catch {  }

  schedule(() => {
    window.location.href = RICKROLL_URL
  }, OPENING_DURATION_MS)
}

onBeforeUnmount(() => {
  clearTimers()
})
</script>

<template>
  <div class="chat-window chat-placeholder secret-chat-placeholder" @click="handleBackgroundClick">
    <div class="secret-scene" :class="{ 'scene-active': sceneActive, 'is-opening': isOpening }">
      <div class="empty-state-shell" :class="{ 'is-hidden': sceneActive }">
        <ChatEmptyStateBase />
      </div>

      <Transition name="veil-fade">
        <div v-if="sceneActive" class="secret-atmosphere" aria-hidden="true">
          <div class="ambient-haze ambient-haze-outer"></div>
          <div class="ambient-haze ambient-haze-inner"></div>
          <div class="ambient-ring ambient-ring-outer"></div>
          <div class="ambient-ring ambient-ring-mid"></div>
          <div class="ambient-ring ambient-ring-inner"></div>
          <div class="magic-floor"></div>

          <span
            v-for="particle in particleSeed"
            :key="particle.id"
            class="magic-particle"
            :style="particle.style"
          ></span>
        </div>
      </Transition>

      <Transition name="door-fade">
        <div v-if="doorVisible" class="door-stage" :class="stage">
          <button
            type="button"
            class="door-shell"
            :class="{
              summoning: stage === 'summoning',
              formed: stage === 'formed',
              opening: stage === 'opening',
            }"
            :disabled="!doorInteractive"
            :tabindex="doorInteractive ? 0 : -1"
            @click.stop="handleDoorClick"
          >
            <span v-if="isOpening" class="light-rays-container" aria-hidden="true">
              <span
                v-for="ray in lightRays"
                :key="ray.id"
                class="light-ray"
                :style="ray.style"
              ></span>
            </span>

            <svg class="door-illustration" viewBox="0 0 260 410" aria-hidden="true">
              <defs>
                <linearGradient id="roomSpellStroke" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#fff5d0" />
                  <stop offset="35%" stop-color="#f5d277" />
                  <stop offset="100%" stop-color="#9f6a20" />
                </linearGradient>
                <linearGradient id="roomFrameFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#8e6032" />
                  <stop offset="42%" stop-color="#5d371c" />
                  <stop offset="100%" stop-color="#29160b" />
                </linearGradient>
                <linearGradient id="roomLeafFill" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="#96643a" />
                  <stop offset="45%" stop-color="#6b3f20" />
                  <stop offset="100%" stop-color="#351c0d" />
                </linearGradient>
                <linearGradient id="roomInnerGlow" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="rgba(255,248,214,0.9)" />
                  <stop offset="28%" stop-color="rgba(244,214,135,0.55)" />
                  <stop offset="100%" stop-color="rgba(22,14,7,0.02)" />
                </linearGradient>
                <radialGradient id="roomPortalLight" cx="50%" cy="50%" r="56%">
                  <stop offset="0%" stop-color="#fffae8" stop-opacity="0.98" />
                  <stop offset="22%" stop-color="#fff6d6" stop-opacity="0.82" />
                  <stop offset="48%" stop-color="#f5d68d" stop-opacity="0.38" />
                  <stop offset="100%" stop-color="#0d0905" stop-opacity="0" />
                </radialGradient>
                <radialGradient id="roomPortalLightOpen" cx="50%" cy="50%" r="52%">
                  <stop offset="0%" stop-color="#fffdf5" stop-opacity="1" />
                  <stop offset="30%" stop-color="#fff5d0" stop-opacity="0.92" />
                  <stop offset="60%" stop-color="#f5d277" stop-opacity="0.55" />
                  <stop offset="100%" stop-color="#0d0905" stop-opacity="0" />
                </radialGradient>
                <filter id="roomSoftGlow" x="-80%" y="-80%" width="260%" height="260%">
                  <feGaussianBlur stdDeviation="8" />
                </filter>
                <filter id="roomHeavyGlow" x="-120%" y="-120%" width="340%" height="340%">
                  <feGaussianBlur stdDeviation="16" />
                </filter>
                <filter id="roomIntenseGlow" x="-160%" y="-160%" width="420%" height="420%">
                  <feGaussianBlur stdDeviation="24" />
                </filter>
              </defs>

              <ellipse class="door-shadow" cx="130" cy="382" rx="72" ry="16" />
              <ellipse class="spell-aura spell-aura-wide" cx="130" cy="204" rx="106" ry="162" />
              <ellipse class="spell-aura spell-aura-tight" cx="130" cy="205" rx="82" ry="132" />

              <g class="spell-runes">
                <path class="rune-path rune-arch" d="M54 352V124c0-52 30-88 76-88s76 36 76 88v228" />
                <path class="rune-path rune-frame" d="M68 352V131c0-42 24-70 62-70s62 28 62 70v221" />
                <path class="rune-path rune-divider" d="M130 78v274" />
                <path class="rune-path rune-panel-outer" d="M83 339V140c0-29 18-48 47-48s47 19 47 48v199" />
                <path class="rune-path rune-panel-inner-top" d="M101 122c0-12 10-22 29-22s29 10 29 22v76h-58z" />
                <path class="rune-path rune-panel-inner-mid" d="M101 221h58v49h-58z" />
                <path class="rune-path rune-panel-inner-low" d="M101 289h58v38h-58z" />
                <circle class="rune-dot rune-dot-left" cx="87" cy="95" r="2.9" />
                <circle class="rune-dot rune-dot-right" cx="173" cy="84" r="2.5" />
                <circle class="rune-dot rune-dot-knob" cx="157" cy="228" r="3.7" />
              </g>

              <g class="door-built">
                <path class="frame-outer" d="M62 357V126c0-46 27-78 68-78s68 32 68 78v231H62z" />
                <path class="frame-inner" d="M78 345V136c0-34 20-58 52-58s52 24 52 58v209H78z" />

                <ellipse class="inner-glow-bloom" cx="130" cy="210" rx="40" ry="85" />

                <path class="inner-light" d="M86 337V141c0-29 17-49 44-49s44 20 44 49v196H86z" />
                <g class="door-leaf-group">
                  <path class="door-leaf" d="M87 338V142c0-28 17-48 43-48s43 20 43 48v196H87z" />
                  <path class="door-leaf-sheen" d="M96 332V147c0-21 12-37 34-37v222H96z" />
                  <path class="door-panel panel-top" d="M101 123c0-12 10-21 29-21s29 9 29 21v76h-58z" />
                  <path class="door-panel panel-mid" d="M101 219h58v50h-58z" />
                  <path class="door-panel panel-low" d="M101 289h58v38h-58z" />
                  <circle class="door-knob" cx="156" cy="228" r="6.3" />
                  <circle class="door-knob-highlight" cx="154" cy="226" r="2.8" />
                </g>
              </g>
            </svg>
          </button>

          <div class="door-hint-text" :class="{ 'hint-visible': stage === 'formed' }">Наважешся відкрити ?</div>
        </div>
      </Transition>

      <Transition name="screen-fade">
        <div v-if="isOpening" class="screen-eclipse" aria-hidden="true">
          <div class="eclipse-vignette"></div>
          <div class="eclipse-center-light"></div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<style scoped>
.chat-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
}

.secret-chat-placeholder {
  position: relative;
  overflow: hidden;
  isolation: isolate;
}

.secret-scene {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  min-height: 100%;
}

.secret-atmosphere,
.door-stage,
.screen-eclipse {
  position: absolute;
  inset: 0;
}

.secret-atmosphere,
.screen-eclipse {
  pointer-events: none;
}

.empty-state-shell {
  position: relative;
  z-index: 1;
  transition: opacity 0.18s ease, visibility 0.18s ease;
}

.empty-state-shell.is-hidden {
  opacity: 0;
  visibility: hidden;
}

.secret-atmosphere {
  display: flex;
  align-items: center;
  justify-content: center;
}

.ambient-haze,
.ambient-ring,
.magic-floor,
.magic-particle {
  position: absolute;
  left: 50%;
  top: 50%;
}

.ambient-haze {
  border-radius: 50%;
  transform: translate(-50%, -50%);
  filter: blur(18px);
}

.ambient-haze-outer {
  width: 460px;
  height: 460px;
  background:
    radial-gradient(circle, rgba(252, 226, 149, 0.12) 0%, rgba(252, 226, 149, 0.06) 34%, rgba(17, 14, 10, 0) 74%),
    radial-gradient(circle at 50% 62%, rgba(229, 234, 255, 0.08) 0%, rgba(229, 234, 255, 0) 68%);
  animation: haze-breathe 7.4s ease-in-out infinite;
}

.ambient-haze-inner {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(250, 227, 165, 0.14) 0%, rgba(250, 227, 165, 0) 70%);
  filter: blur(8px);
  animation: haze-breathe 5.5s ease-in-out -1.3s infinite;
}

.ambient-ring {
  border-radius: 50%;
  transform: translate(-50%, -50%) scale(0.84);
  border: 1px solid rgba(250, 220, 142, 0.16);
  opacity: 0;
}

.scene-active .ambient-ring-outer {
  width: 336px;
  height: 336px;
  animation: ring-pulse 4.8s ease-out infinite;
}

.scene-active .ambient-ring-mid {
  width: 260px;
  height: 260px;
  animation: ring-pulse 4.8s ease-out 0.55s infinite;
}

.scene-active .ambient-ring-inner {
  width: 196px;
  height: 196px;
  animation: ring-pulse 4.8s ease-out 1.05s infinite;
}

.magic-floor {
  width: 270px;
  height: 60px;
  transform: translate(-50%, 132px);
  border-radius: 50%;
  background: radial-gradient(circle, rgba(248, 211, 124, 0.18) 0%, rgba(248, 211, 124, 0.07) 38%, rgba(0, 0, 0, 0) 72%);
  filter: blur(10px);
  opacity: 0;
  animation: floor-bloom 5.2s ease forwards;
}

.magic-particle {
  --size: 6px;
  --duration: 4.5s;
  --delay: 0s;
  --orbit-x: 0px;
  --orbit-y: 0px;
  --drift: 18px;
  width: var(--size);
  height: var(--size);
  margin-left: calc(var(--size) / -2);
  margin-top: calc(var(--size) / -2);
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 248, 223, 0.96) 0%, rgba(248, 214, 129, 0.82) 46%, rgba(248, 214, 129, 0) 74%);
  box-shadow: 0 0 12px rgba(248, 214, 129, 0.24);
  opacity: 0;
  transform: translate(0, 0) scale(0.3);
  animation: particle-orbit var(--duration) ease-in-out var(--delay) infinite;
}

/* ──── Door Stage ──── */

.door-stage {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  pointer-events: none;
}

.door-shell {
  position: relative;
  z-index: 1;
  width: 236px;
  height: 364px;
  padding: 0;
  border: 0;
  background: transparent;
  opacity: 0;
  transform: translateY(28px) scale(0.84);
  filter: drop-shadow(0 30px 28px rgba(0, 0, 0, 0.34));
  pointer-events: none;
}

.door-shell:disabled {
  cursor: default;
}

.door-shell.formed {
  cursor: pointer;
  pointer-events: auto;
}

.door-shell.formed:hover {
  filter: drop-shadow(0 30px 38px rgba(0, 0, 0, 0.4)) drop-shadow(0 0 40px rgba(248, 214, 129, 0.12));
}

.door-shell.summoning,
.door-shell.formed,
.door-shell.opening {
  opacity: 1;
  transform: translateY(0) scale(1);
  transition: opacity 0.8s ease, transform 1.2s cubic-bezier(0.17, 0.86, 0.22, 1), filter 0.3s ease;
}

.door-illustration {
  width: 100%;
  height: 100%;
  overflow: visible;
}

/* ──── Light Rays (only during opening) ──── */

.light-rays-container {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
  z-index: 0;
}

.light-ray {
  --ray-length: 60%;
  --ray-angle: 0deg;
  --ray-delay: 0s;
  --ray-opacity: 0.15;
  position: absolute;
  display: block;
  width: 3px;
  height: var(--ray-length);
  background: linear-gradient(
    to top,
    rgba(255, 248, 220, 0) 0%,
    rgba(255, 243, 190, 0.35) 30%,
    rgba(255, 248, 220, 0.05) 100%
  );
  transform-origin: bottom center;
  transform: rotate(var(--ray-angle));
  opacity: 0;
  filter: blur(2px);
  animation: ray-expand 3s ease-out var(--ray-delay) forwards;
}

.door-hint-text {
  position: absolute;
  top: calc(50% + 200px);
  left: 50%;
  transform: translateX(-50%);
  font-family: var(--font-display), sans-serif;
  font-size: 11px;
  letter-spacing: 4px;
  color: rgba(248, 214, 129, 0.5);
  white-space: nowrap;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.6s ease, transform 0.6s ease, filter 0.6s ease;
  filter: blur(6px);
}

.door-hint-text.hint-visible {
  opacity: 1;
  filter: blur(0);
  animation: hint-materialize 1s ease forwards, hint-pulse 2.8s ease-in-out 1s infinite;
}

.door-shadow {
  fill: rgba(0, 0, 0, 0.48);
  opacity: 0;
  filter: blur(8px);
}

.spell-aura {
  fill: none;
  stroke: rgba(246, 214, 138, 0.12);
  stroke-width: 1.2;
  opacity: 0;
  filter: url(#roomHeavyGlow);
}

.door-shell.summoning .spell-aura-wide {
  animation: aura-wave 3.6s ease-out 0.05s infinite;
}

.door-shell.summoning .spell-aura-tight,
.door-shell.formed .spell-aura-tight {
  animation: aura-wave 3.4s ease-out 0.45s infinite;
}

.spell-runes,
.door-built {
  transform-origin: 130px 205px;
}

.spell-runes {
  opacity: 0;
  filter: url(#roomSoftGlow);
}

.door-shell.summoning .spell-runes {
  opacity: 1;
}

.rune-path {
  fill: none;
  stroke: url(#roomSpellStroke);
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2.2;
  stroke-dasharray: 980;
  stroke-dashoffset: 980;
}

.rune-arch { animation: draw-rune 1.35s ease-out 0.2s forwards; }
.rune-frame { animation: draw-rune 1.3s ease-out 0.85s forwards; }
.rune-divider { animation: draw-rune 0.95s ease-out 1.55s forwards; }
.rune-panel-outer { animation: draw-rune 1s ease-out 1.95s forwards; }
.rune-panel-inner-top { animation: draw-rune 0.75s ease-out 2.55s forwards; }
.rune-panel-inner-mid { animation: draw-rune 0.62s ease-out 2.9s forwards; }
.rune-panel-inner-low { animation: draw-rune 0.62s ease-out 3.18s forwards; }

.rune-dot {
  fill: #ffe4a0;
  opacity: 0;
  transform-origin: center;
}

.rune-dot-left { animation: dot-arrive 0.55s ease-out 2.65s forwards; }
.rune-dot-right { animation: dot-arrive 0.55s ease-out 2.95s forwards; }
.rune-dot-knob { animation: dot-arrive 0.6s ease-out 3.4s forwards; }

.frame-outer,
.frame-inner,
.inner-light,
.inner-glow-bloom,
.door-leaf,
.door-leaf-sheen,
.door-panel,
.door-knob,
.door-knob-highlight {
  opacity: 0;
}

.frame-outer {
  fill: url(#roomFrameFill);
  stroke: rgba(213, 170, 98, 0.34);
  stroke-width: 1.6;
}

.frame-inner {
  fill: rgba(23, 15, 9, 0.92);
  stroke: rgba(235, 200, 130, 0.16);
  stroke-width: 1.1;
}

.inner-glow-bloom {
  fill: rgba(255, 248, 220, 0.06);
  filter: url(#roomIntenseGlow);
}

.inner-light {
  fill: url(#roomPortalLight);
  filter: url(#roomSoftGlow);
}

.door-leaf {
  fill: url(#roomLeafFill);
  stroke: rgba(216, 170, 100, 0.26);
  stroke-width: 1.3;
  transform-box: fill-box;
  transform-origin: 0 50%;
}

.door-leaf-sheen {
  fill: rgba(255, 243, 212, 0.08);
  transform-box: fill-box;
  transform-origin: 0 50%;
}

.door-panel {
  fill: none;
  stroke: rgba(231, 194, 120, 0.24);
  stroke-width: 1.1;
}

.door-knob {
  fill: #d8ac61;
  stroke: rgba(255, 240, 202, 0.45);
  stroke-width: 0.8;
  filter: url(#roomSoftGlow);
  transform-origin: 156px 228px;
}

.door-knob-highlight {
  fill: rgba(255, 248, 220, 0.45);
}

/* ──── Summoning / Formed / Opening shared materialize triggers ──── */

.door-shell.summoning .door-shadow,
.door-shell.formed .door-shadow,
.door-shell.opening .door-shadow {
  animation: shadow-arrive 1.7s ease 1.3s forwards;
}

.door-shell.summoning .frame-outer,
.door-shell.formed .frame-outer,
.door-shell.opening .frame-outer {
  animation: materialize-fill 1.35s cubic-bezier(0.19, 0.85, 0.21, 1) 2.45s forwards;
}

.door-shell.summoning .frame-inner,
.door-shell.formed .frame-inner,
.door-shell.opening .frame-inner {
  animation: materialize-fill 1.2s cubic-bezier(0.19, 0.85, 0.21, 1) 2.78s forwards;
}

.door-shell.summoning .inner-glow-bloom,
.door-shell.formed .inner-glow-bloom,
.door-shell.opening .inner-glow-bloom {
  animation: inner-bloom-arrive 1.8s ease 3s forwards;
}

.door-shell.summoning .inner-light,
.door-shell.formed .inner-light,
.door-shell.opening .inner-light {
  animation: inner-light-arrive 1.3s ease 3.1s forwards;
}

.door-shell.summoning .door-leaf,
.door-shell.formed .door-leaf,
.door-shell.opening .door-leaf {
  animation: materialize-fill 1.25s cubic-bezier(0.18, 0.84, 0.2, 1) 3.25s forwards;
}

.door-shell.summoning .door-leaf-sheen,
.door-shell.formed .door-leaf-sheen,
.door-shell.opening .door-leaf-sheen {
  animation: materialize-fill 1.2s ease 3.55s forwards;
}

.door-shell.summoning .panel-top,
.door-shell.formed .panel-top,
.door-shell.opening .panel-top {
  animation: panel-trace 1.05s ease 3.65s forwards;
}

.door-shell.summoning .panel-mid,
.door-shell.formed .panel-mid,
.door-shell.opening .panel-mid {
  animation: panel-trace 0.9s ease 3.95s forwards;
}

.door-shell.summoning .panel-low,
.door-shell.formed .panel-low,
.door-shell.opening .panel-low {
  animation: panel-trace 0.9s ease 4.18s forwards;
}

.door-shell.summoning .door-knob,
.door-shell.formed .door-knob,
.door-shell.opening .door-knob {
  animation: knob-arrive 0.55s ease-out 4.55s forwards;
}

.door-shell.summoning .door-knob-highlight,
.door-shell.formed .door-knob-highlight,
.door-shell.opening .door-knob-highlight {
  animation: knob-arrive 0.5s ease-out 4.7s forwards;
}

/* ──── Formed state: idle glow ──── */

.door-shell.formed .inner-light {
  opacity: 0.72;
  animation: portal-idle 4.6s ease-in-out infinite;
}

.door-shell.formed .inner-glow-bloom {
  opacity: 0.35;
  animation: bloom-idle 5.2s ease-in-out infinite;
}

.door-shell.formed .door-knob {
  opacity: 1;
  animation: knob-idle 2.4s ease-in-out infinite;
}

/* ──── Opening state: dramatic reveal ──── */

.door-shell.opening .spell-aura-wide,
.door-shell.opening .spell-aura-tight {
  animation: aura-burst 1.65s ease forwards;
}

.door-shell.opening .inner-light {
  opacity: 1;
  animation: portal-open 2.8s ease forwards;
}

.door-shell.opening .inner-glow-bloom {
  opacity: 1;
  animation: bloom-open 2.8s ease forwards;
}

.door-leaf-group {
  transform-box: fill-box;
  transform-origin: 0 50%;
}

.door-shell.opening .door-leaf-group {
  animation: door-swing-open 3.2s cubic-bezier(0.12, 0.72, 0.08, 1) 0.3s forwards;
}

.door-shell.opening .door-knob {
  opacity: 1;
  animation: knob-turn 0.6s ease-in-out forwards;
}

/* ──── Screen eclipse (whiteout on open) ──── */

.screen-eclipse {
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.eclipse-vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    circle at center,
    rgba(255, 250, 235, 0) 0%,
    rgba(10, 7, 5, 0.03) 20%,
    rgba(6, 5, 4, 0.45) 50%,
    rgba(0, 0, 0, 0.97) 100%
  );
  animation: eclipse-vignette-rise 4.2s ease forwards;
}

.eclipse-center-light {
  position: absolute;
  inset: 0;
  margin: auto;
  width: 240px;
  height: 360px;
  border-radius: 50%;
  background: radial-gradient(
    ellipse at center,
    rgba(255, 250, 235, 0.85) 0%,
    rgba(248, 230, 170, 0.4) 35%,
    rgba(248, 214, 129, 0.1) 65%,
    transparent 100%
  );
  filter: blur(30px);
  animation: eclipse-center-bloom 3.8s ease forwards;
}

/* ──── Transitions ──── */

/* noinspection CssUnusedSymbol */
.veil-fade-enter-active,
.veil-fade-leave-active,
.door-fade-enter-active,
.door-fade-leave-active,
.screen-fade-enter-active,
.screen-fade-leave-active {
  transition: opacity 0.5s ease;
}

/* noinspection CssUnusedSymbol */
.veil-fade-enter-from,
.veil-fade-leave-to,
.door-fade-enter-from,
.door-fade-leave-to,
.screen-fade-enter-from,
.screen-fade-leave-to {
  opacity: 0;
}

/* ════════════════════ KEYFRAMES ════════════════════ */

@keyframes haze-breathe {
  0%, 100% { transform: translate(-50%, -50%) scale(0.96); opacity: 0.64; }
  50% { transform: translate(-50%, -50%) scale(1.05); opacity: 1; }
}

@keyframes ring-pulse {
  0% { opacity: 0; transform: translate(-50%, -50%) scale(0.72); }
  18% { opacity: 0.42; }
  100% { opacity: 0; transform: translate(-50%, -50%) scale(1.24); }
}

@keyframes floor-bloom {
  0% { opacity: 0; transform: translate(-50%, 144px) scale(0.65); }
  30% { opacity: 0.35; }
  100% { opacity: 1; transform: translate(-50%, 132px) scale(1); }
}

@keyframes particle-orbit {
  0% {
    opacity: 0;
    transform: translate(0, 0) scale(0.2);
  }
  18% {
    opacity: 1;
  }
  65% {
    opacity: 0.78;
  }
  100% {
    opacity: 0;
    transform: translate(var(--orbit-x), var(--orbit-y)) translateY(calc(var(--drift) * -1)) scale(1);
  }
}

@keyframes aura-wave {
  0% { opacity: 0; transform: scale(0.86); }
  24% { opacity: 0.5; }
  100% { opacity: 0; transform: scale(1.16); }
}

@keyframes aura-burst {
  0% { opacity: 0.18; transform: scale(1); }
  35% { opacity: 0.85; }
  100% { opacity: 0; transform: scale(1.36); }
}

@keyframes draw-rune {
  0% { opacity: 0; stroke-dashoffset: 980; }
  18% { opacity: 1; }
  100% { opacity: 0.95; stroke-dashoffset: 0; }
}

@keyframes dot-arrive {
  0% { opacity: 0; transform: scale(0.25); }
  70% { opacity: 1; transform: scale(1.35); }
  100% { opacity: 1; transform: scale(1); }
}

@keyframes materialize-fill {
  0% { opacity: 0; transform: translateY(8px) scale(0.94); filter: brightness(1.2) saturate(0.6); }
  50% { opacity: 0.86; }
  100% { opacity: 1; transform: translateY(0) scale(1); filter: brightness(1) saturate(1); }
}

@keyframes panel-trace {
  0% { opacity: 0; stroke-dasharray: 400; stroke-dashoffset: 400; }
  100% { opacity: 1; stroke-dasharray: 0; stroke-dashoffset: 0; }
}

@keyframes knob-arrive {
  0% { opacity: 0; transform: scale(0.2); }
  75% { opacity: 1; transform: scale(1.22); }
  100% { opacity: 1; transform: scale(1); }
}

@keyframes shadow-arrive {
  0% { opacity: 0; transform: scale(0.7); }
  100% { opacity: 1; transform: scale(1); }
}

@keyframes inner-light-arrive {
  0% { opacity: 0; filter: brightness(1.6) blur(8px); }
  100% { opacity: 0.55; filter: brightness(1) blur(0); }
}

@keyframes inner-bloom-arrive {
  0% { opacity: 0; }
  100% { opacity: 0.3; }
}

@keyframes portal-idle {
  0%, 100% { transform: scale(1); filter: brightness(1); opacity: 0.62; }
  50% { transform: scale(1.03); filter: brightness(1.16); opacity: 0.86; }
}

@keyframes bloom-idle {
  0%, 100% { opacity: 0.25; transform: scale(1); }
  50% { opacity: 0.45; transform: scale(1.06); }
}

@keyframes knob-idle {
  0%, 100% { filter: url(#roomSoftGlow) brightness(1); }
  50% { filter: url(#roomSoftGlow) brightness(1.3); }
}

@keyframes hint-materialize {
  0% { opacity: 0; filter: blur(8px); transform: translateX(-50%) translateY(10px) scale(0.85); letter-spacing: 12px; }
  40% { filter: blur(2px); }
  100% { opacity: 1; filter: blur(0); transform: translateX(-50%) translateY(0) scale(1); letter-spacing: 4px; }
}

@keyframes hint-pulse {
  0%, 100% { opacity: 0.5; transform: translateX(-50%) translateY(0); }
  50% { opacity: 0.9; transform: translateX(-50%) translateY(-2px); }
}

/* ──── Opening keyframes ──── */

@keyframes portal-open {
  0% { opacity: 0.75; transform: scale(1); filter: blur(0) brightness(1); }
  30% { opacity: 1; filter: blur(0) brightness(1.3); }
  60% { filter: blur(2px) brightness(1.6); }
  100% { opacity: 1; transform: scale(1.25); filter: blur(6px) brightness(2); }
}

@keyframes bloom-open {
  0% { opacity: 0.3; transform: scale(1); filter: url(#roomIntenseGlow); }
  40% { opacity: 0.7; }
  100% { opacity: 1; transform: scale(1.5); filter: url(#roomIntenseGlow) brightness(1.8); }
}

@keyframes door-swing-open {
  0% {
    transform: perspective(1200px) rotateY(0deg);
    opacity: 1;
  }
  8% {
    transform: perspective(1200px) rotateY(2deg);
  }
  18% {
    transform: perspective(1200px) rotateY(-8deg);
  }
  40% {
    transform: perspective(1200px) rotateY(-45deg);
    opacity: 1;
  }
  70% {
    transform: perspective(1200px) rotateY(-82deg);
    opacity: 0.92;
  }
  90% {
    opacity: 0.7;
  }
  100% {
    transform: perspective(1200px) rotateY(-95deg);
    opacity: 0.4;
  }
}

@keyframes knob-turn {
  0% { transform: rotate(0deg); }
  40% { transform: rotate(-35deg); }
  100% { transform: rotate(-35deg); }
}

@keyframes ray-expand {
  0% {
    opacity: 0;
    transform: rotate(var(--ray-angle)) scaleY(0);
  }
  20% {
    opacity: var(--ray-opacity);
  }
  60% {
    opacity: var(--ray-opacity);
    transform: rotate(var(--ray-angle)) scaleY(1);
  }
  100% {
    opacity: 0;
    transform: rotate(var(--ray-angle)) scaleY(1.3);
  }
}

@keyframes eclipse-vignette-rise {
  0% { opacity: 0; }
  25% { opacity: 0.08; }
  60% { opacity: 0.5; }
  100% { opacity: 1; }
}

@keyframes eclipse-center-bloom {
  0% { opacity: 0; transform: scale(0.3); }
  30% { opacity: 0.6; transform: scale(0.8); }
  60% { opacity: 1; transform: scale(1.2); }
  100% { opacity: 1; transform: scale(3); filter: blur(60px); }
}
</style>
