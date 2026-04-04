<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'

const emit = defineEmits<{
  (e: 'unlocked'): void
  (e: 'close'): void
}>()

const props = defineProps<{
  originRect?: { x: number; y: number }
}>()

const phase = ref(0)
const timers: number[] = []
const ringSvgRef = ref<SVGElement | null>(null)
let decelAnim: Animation | null = null

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => {
    const t = window.setTimeout(resolve, ms)
    timers.push(t)
  })
}

function getCurrentRotation(el: Element): number {
  const style = window.getComputedStyle(el)
  const matrix = style.transform
  if (!matrix || matrix === 'none') return 0
  const match = matrix.match(/matrix\(([^)]+)\)/)
  if (!match || !match[1]) return 0
  const parts = match[1].split(',').map(s => parseFloat(s.trim()))
  const a = parts[0] ?? 0
  const b = parts[1] ?? 0
  return Math.atan2(b, a) * (180 / Math.PI)
}

function startDeceleration() {
  const el = ringSvgRef.value
  if (!el) return

  const currentAngle = getCurrentRotation(el)

  el.style.animation = 'none'
  el.style.transform = `rotate(${currentAngle}deg)`

  const finalAngle = currentAngle + 540

  decelAnim = el.animate(
    [
      { transform: `rotate(${currentAngle}deg)` },
      { transform: `rotate(${finalAngle}deg)` },
    ],
    {
      duration: 6000,
      easing: 'cubic-bezier(0.08, 0.82, 0.17, 1)',
      fill: 'forwards',
    },
  )
}

const sparks = computed(() => {
  const count = 24
  const result = []
  for (let i = 0; i < count; i++) {
    const angle = (i / count) * 360 + (i * 37 + 13) % 60 - 30
    const rad = (angle * Math.PI) / 180
    const smallRadius = 18 + (i % 7) * 5
    const bigRadius = 100 + (i % 5) * 30
    const duration = 0.8 + (i % 8) * 0.1
    const delayMs = i * 0.13
    const size = 2 + (i % 3) * 1.5
    const colorType = i % 4
    result.push({
      angle, rad,
      smallX: Math.cos(rad) * smallRadius,
      smallY: Math.sin(rad) * smallRadius,
      bigX: Math.cos(rad) * bigRadius,
      bigY: Math.sin(rad) * bigRadius,
      duration, delay: delayMs, size, colorType,
    })
  }
  return result
})

async function runSequence() {
  await nextTick()

  phase.value = 1
  await delay(2200)

  phase.value = 2
  await delay(3200)

  startDeceleration()
  phase.value = 3
  await delay(2800)

  phase.value = 4
  await delay(3200)

  phase.value = 5
  emit('unlocked')
  await delay(5000)

  phase.value = 6
  await delay(2000)

  emit('close')
}

onMounted(() => {
  runSequence()
})

onBeforeUnmount(() => {
  timers.forEach((t) => window.clearTimeout(t))
  decelAnim?.cancel()
})

const originX = props.originRect?.x ?? window.innerWidth / 2
const originY = props.originRect?.y ?? window.innerHeight - 40
</script>

<template>
  <Teleport to="body">
    <div
      class="one-ring-overlay"
      :class="{
        'phase-1': phase === 1,
        'phase-2': phase === 2,
        'phase-gte-2': phase >= 2,
        'phase-3': phase === 3,
        'phase-gte-3': phase >= 3,
        'phase-4': phase === 4,
        'phase-gte-4': phase >= 4,
        'phase-5': phase === 5,
        'phase-gte-5': phase >= 5,
        'phase-6': phase === 6,
      }"
    >
      <div class="ring-darkness" />
      <div class="ring-vignette" />

      <div
        class="sparks-container"
        :style="{ left: phase >= 2 ? '50%' : `${originX}px`, top: phase >= 2 ? '50%' : `${originY}px` }"
      >
        <span
          v-for="(spark, i) in sparks"
          :key="i"
          class="spark"
          :class="{
            'spark--orange': spark.colorType === 0,
            'spark--red': spark.colorType === 1,
            'spark--gold': spark.colorType === 2,
            'spark--darkred': spark.colorType === 3,
          }"
          :style="{
            width: `${spark.size}px`,
            height: `${spark.size}px`,
            '--tx': phase >= 2 ? `${spark.bigX}px` : `${spark.smallX}px`,
            '--ty': phase >= 2 ? `${spark.bigY}px` : `${spark.smallY}px`,
            '--dur': `${spark.duration}s`,
            '--del': `${spark.delay}s`,
          }"
        />
      </div>

      <div v-if="phase >= 4 && phase < 6" class="embers-container">
        <span v-for="n in 28" :key="n" class="ember" />
      </div>

      <div
        class="ring-container"
        :style="{
          left: phase >= 2 ? '50%' : `${originX}px`,
          top: phase >= 2 ? '50%' : `${originY}px`,
        }"
      >
        <div class="ring-body">
          <svg ref="ringSvgRef" viewBox="0 0 200 200" class="ring-svg" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <linearGradient id="orGold1" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#ffe066" />
                <stop offset="18%" stop-color="#ffd700" />
                <stop offset="40%" stop-color="#daa520" />
                <stop offset="60%" stop-color="#b8860b" />
                <stop offset="80%" stop-color="#cd9b1d" />
                <stop offset="100%" stop-color="#ffd700" />
              </linearGradient>
              <linearGradient id="orGold2" x1="0%" y1="100%" x2="100%" y2="0%">
                <stop offset="0%" stop-color="#b8860b" />
                <stop offset="30%" stop-color="#daa520" />
                <stop offset="50%" stop-color="#ffe680" />
                <stop offset="70%" stop-color="#daa520" />
                <stop offset="100%" stop-color="#8B6914" />
              </linearGradient>
              <linearGradient id="orHighlight" x1="50%" y1="0%" x2="50%" y2="100%">
                <stop offset="0%" stop-color="#fff8dc" stop-opacity="0.6" />
                <stop offset="30%" stop-color="#ffec80" stop-opacity="0.25" />
                <stop offset="100%" stop-color="#ffd700" stop-opacity="0" />
              </linearGradient>
              <linearGradient id="orShadow" x1="50%" y1="100%" x2="50%" y2="0%">
                <stop offset="0%" stop-color="#5c3a0a" stop-opacity="0.5" />
                <stop offset="40%" stop-color="#8B6914" stop-opacity="0.15" />
                <stop offset="100%" stop-color="#daa520" stop-opacity="0" />
              </linearGradient>
              <radialGradient id="orAuraGlow" cx="50%" cy="50%" r="55%">
                <stop offset="0%" stop-color="rgba(255, 180, 40, 0.4)" />
                <stop offset="50%" stop-color="rgba(255, 120, 0, 0.12)" />
                <stop offset="100%" stop-color="rgba(255, 80, 0, 0)" />
              </radialGradient>
              <filter id="orGoldGlow">
                <feGaussianBlur stdDeviation="2" result="blur" />
                <feMerge>
                  <feMergeNode in="blur" />
                  <feMergeNode in="SourceGraphic" />
                </feMerge>
              </filter>
              <filter id="orFireGlow">
                <feGaussianBlur stdDeviation="2.5" result="blur" />
                <feColorMatrix in="blur" type="matrix"
                  values="2 0.3 0 0 0
                          0.6 0.2 0 0 0
                          0   0   0 0 0
                          0   0   0 1 0" result="fireBlur" />
                <feMerge>
                  <feMergeNode in="fireBlur" />
                  <feMergeNode in="SourceGraphic" />
                </feMerge>
              </filter>
              <filter id="orInnerDepth">
                <feGaussianBlur stdDeviation="1.5" result="blur" />
                <feMerge>
                  <feMergeNode in="blur" />
                  <feMergeNode in="SourceGraphic" />
                </feMerge>
              </filter>

              <path id="orTextOuter"
                d="M 100,100 m -72,0 a 72,72 0 1,1 144,0 a 72,72 0 1,1 -144,0" fill="none" />
              <path id="orTextInner"
                d="M 100,100 m 72,0 a 72,72 0 1,0 -144,0 a 72,72 0 1,0 144,0" fill="none" />
            </defs>

            <circle cx="100" cy="100" r="88" fill="url(#orAuraGlow)" class="ring-aura" />

            <circle cx="100" cy="100" r="82" stroke="#6b4c12" stroke-width="1.2" fill="none" opacity="0.4" />

            <circle cx="100" cy="100" r="72" stroke="url(#orGold1)" stroke-width="20"
              fill="none" filter="url(#orGoldGlow)" />

            <circle cx="100" cy="100" r="72" stroke="url(#orGold2)" stroke-width="14"
              fill="none" opacity="0.45" />

            <circle cx="100" cy="100" r="72" stroke="url(#orHighlight)" stroke-width="16"
              fill="none" stroke-dasharray="80 372" stroke-dashoffset="-10" opacity="0.7" />

            <circle cx="100" cy="100" r="72" stroke="url(#orShadow)" stroke-width="16"
              fill="none" stroke-dasharray="90 362" stroke-dashoffset="170" opacity="0.6" />

            <circle cx="100" cy="100" r="72" stroke="rgba(255,248,220,0.3)" stroke-width="6"
              fill="none" stroke-dasharray="25 427" stroke-dashoffset="-195" opacity="0.5" />

            <circle cx="100" cy="100" r="62" stroke="#d4a830" stroke-width="0.6" fill="none" opacity="0.65" />
            <circle cx="100" cy="100" r="62.8" stroke="#8B6914" stroke-width="0.3" fill="none" opacity="0.35" />

            <circle cx="100" cy="100" r="82" stroke="#d4a830" stroke-width="0.6" fill="none" opacity="0.55" />
            <circle cx="100" cy="100" r="81.2" stroke="#e8c444" stroke-width="0.3" fill="none" opacity="0.3" />

            <text class="elvish-text" filter="url(#orFireGlow)">
              <textPath href="#orTextOuter" startOffset="0%">
                Ash nazg durbatulûk, ash nazg gimbatul, ash nazg thrakatulûk, agh burzum-ishi krimpatul.
              </textPath>
            </text>

            <text class="elvish-text elvish-inner" filter="url(#orFireGlow)">
              <textPath href="#orTextInner" startOffset="0%">
                Ash nazg durbatulûk, ash nazg gimbatul, ash nazg thrakatulûk, agh burzum-ishi krimpatul.
              </textPath>
            </text>
          </svg>

          <div class="sauron-eye-container">
            <svg viewBox="0 0 200 200" class="sauron-eye-svg" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <radialGradient id="orEyeOuterGlow" cx="50%" cy="50%" r="50%">
                  <stop offset="0%" stop-color="rgba(255, 90, 0, 0.95)" />
                  <stop offset="35%" stop-color="rgba(255, 50, 0, 0.65)" />
                  <stop offset="65%" stop-color="rgba(200, 10, 0, 0.3)" />
                  <stop offset="100%" stop-color="rgba(80, 0, 0, 0)" />
                </radialGradient>
                <radialGradient id="orEyeIris" cx="50%" cy="50%" r="50%">
                  <stop offset="0%" stop-color="#fffbe6" />
                  <stop offset="15%" stop-color="#ffcc00" />
                  <stop offset="40%" stop-color="#ff7700" />
                  <stop offset="70%" stop-color="#cc2200" />
                  <stop offset="100%" stop-color="#550000" />
                </radialGradient>
                <radialGradient id="orPupilGrad" cx="50%" cy="50%" r="50%">
                  <stop offset="0%" stop-color="#ff5500" />
                  <stop offset="40%" stop-color="#aa0000" />
                  <stop offset="100%" stop-color="#110000" />
                </radialGradient>
                <filter id="orEyeFlicker">
                  <feTurbulence type="turbulence" baseFrequency="0.012" numOctaves="4" result="noise">
                    <animate attributeName="baseFrequency" values="0.012;0.022;0.016;0.012" dur="4s" repeatCount="indefinite" />
                  </feTurbulence>
                  <feDisplacementMap in="SourceGraphic" in2="noise" scale="4" xChannelSelector="R" yChannelSelector="G" />
                </filter>
                <filter id="orEyeGlowF">
                  <feGaussianBlur stdDeviation="5" result="blur" />
                  <feMerge>
                    <feMergeNode in="blur" />
                    <feMergeNode in="blur" />
                    <feMergeNode in="SourceGraphic" />
                  </feMerge>
                </filter>
              </defs>

              <circle cx="100" cy="100" r="50" fill="url(#orEyeOuterGlow)" filter="url(#orEyeGlowF)" />

              <g filter="url(#orEyeFlicker)">
                <path d="M 52 100 Q 70 55, 100 48 Q 130 55, 148 100"
                  fill="url(#orEyeIris)" stroke="#ff4400" stroke-width="1.2" />
                <path d="M 52 100 Q 70 145, 100 152 Q 130 145, 148 100"
                  fill="url(#orEyeIris)" stroke="#ff4400" stroke-width="1.2" />
              </g>

              <path d="M 58 100 Q 78 78, 100 74 Q 122 78, 142 100" fill="none"
                stroke="rgba(255,210,50,0.25)" stroke-width="0.8" />
              <path d="M 58 100 Q 78 122, 100 126 Q 122 122, 142 100" fill="none"
                stroke="rgba(255,210,50,0.25)" stroke-width="0.8" />
              <path d="M 65 100 Q 82 85, 100 82 Q 118 85, 135 100" fill="none"
                stroke="rgba(255,180,30,0.18)" stroke-width="0.6" />
              <path d="M 65 100 Q 82 115, 100 118 Q 118 115, 135 100" fill="none"
                stroke="rgba(255,180,30,0.18)" stroke-width="0.6" />

              <ellipse cx="100" cy="100" rx="5.5" ry="40" fill="url(#orPupilGrad)"
                stroke="#0a0000" stroke-width="1.8">
                <animate attributeName="rx" values="5.5;7.5;4;5.5" dur="5s" repeatCount="indefinite" />
                <animate attributeName="ry" values="40;36;42;40" dur="5s" repeatCount="indefinite" />
              </ellipse>

              <ellipse cx="100" cy="100" rx="2.5" ry="32" fill="rgba(255,80,0,0.55)">
                <animate attributeName="rx" values="2.5;3.5;1.8;2.5" dur="5s" repeatCount="indefinite" />
              </ellipse>

              <circle cx="100" cy="100" r="4" fill="#ffee55" opacity="0.85">
                <animate attributeName="r" values="4;5.5;3;4" dur="2.5s" repeatCount="indefinite" />
                <animate attributeName="opacity" values="0.85;1;0.55;0.85" dur="2.5s" repeatCount="indefinite" />
              </circle>

              <g opacity="0.55">
                <path d="M 52 100 Q 46 88, 43 80" fill="none" stroke="#ff7700" stroke-width="2.2" stroke-linecap="round">
                  <animate attributeName="d" values="M 52 100 Q 46 88, 43 80;M 52 100 Q 44 86, 40 76;M 52 100 Q 46 88, 43 80" dur="3.5s" repeatCount="indefinite" />
                </path>
                <path d="M 148 100 Q 154 88, 157 80" fill="none" stroke="#ff7700" stroke-width="2.2" stroke-linecap="round">
                  <animate attributeName="d" values="M 148 100 Q 154 88, 157 80;M 148 100 Q 156 86, 160 76;M 148 100 Q 154 88, 157 80" dur="3.5s" repeatCount="indefinite" />
                </path>
              </g>
              <g opacity="0.4">
                <path d="M 52 100 Q 46 112, 43 120" fill="none" stroke="#ff4400" stroke-width="2" stroke-linecap="round">
                  <animate attributeName="d" values="M 52 100 Q 46 112, 43 120;M 52 100 Q 44 114, 40 124;M 52 100 Q 46 112, 43 120" dur="3s" repeatCount="indefinite" />
                </path>
                <path d="M 148 100 Q 154 112, 157 120" fill="none" stroke="#ff4400" stroke-width="2" stroke-linecap="round">
                  <animate attributeName="d" values="M 148 100 Q 154 112, 157 120;M 148 100 Q 156 114, 160 124;M 148 100 Q 154 112, 157 120" dur="3s" repeatCount="indefinite" />
                </path>
              </g>
              <g opacity="0.3">
                <path d="M 100 48 Q 104 42, 102 36" fill="none" stroke="#ff8800" stroke-width="1.5" stroke-linecap="round">
                  <animate attributeName="d" values="M 100 48 Q 104 42, 102 36;M 100 48 Q 96 40, 98 34;M 100 48 Q 104 42, 102 36" dur="2.8s" repeatCount="indefinite" />
                </path>
                <path d="M 100 152 Q 96 158, 98 164" fill="none" stroke="#ff8800" stroke-width="1.5" stroke-linecap="round">
                  <animate attributeName="d" values="M 100 152 Q 96 158, 98 164;M 100 152 Q 104 160, 102 166;M 100 152 Q 96 158, 98 164" dur="2.8s" repeatCount="indefinite" />
                </path>
              </g>
            </svg>
          </div>
        </div>
      </div>

      <div class="inscription-text">
        <span class="inscription-line inscription-line--1">One Ring to rule them all, One Ring to find them,</span>
        <span class="inscription-line inscription-line--2">One Ring to bring them all, and in the darkness bind them.</span>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
@import url('https://fonts.cdnfonts.com/css/tengwar-annatar');

.one-ring-overlay {
  position: fixed;
  inset: 0;
  z-index: 99999;
  pointer-events: all;
  overflow: hidden;
}

.ring-darkness {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    ellipse at 50% 50%,
    rgba(0, 0, 0, 0) 0%,
    rgba(0, 0, 0, 0.35) 35%,
    rgba(0, 0, 0, 0.92) 100%
  );
  opacity: 0;
  transition: opacity 3s cubic-bezier(0.4, 0, 0.2, 1);
}
.phase-gte-2 .ring-darkness {
  opacity: 1;
}
.phase-6 .ring-darkness {
  background: rgba(0, 0, 0, 0.98);
  transition: background 1s ease;
}

.ring-vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    circle at 50% 50%,
    rgba(80, 10, 0, 0.12) 0%,
    rgba(40, 0, 0, 0.25) 40%,
    rgba(0, 0, 0, 0) 70%
  );
  opacity: 0;
  transition: opacity 2s cubic-bezier(0.4, 0, 0.2, 1);
  pointer-events: none;
}
.phase-gte-4 .ring-vignette {
  opacity: 1;
}
.phase-6 .ring-vignette {
  opacity: 0;
  transition: opacity 1.5s ease;
}

.ring-container {
  position: absolute;
  transform: translate(-50%, -50%) scale(0);
  opacity: 0;
  z-index: 3;
  will-change: transform, left, top, opacity;
  transition: left 3.5s cubic-bezier(0.22, 0.61, 0.36, 1),
              top 3.5s cubic-bezier(0.22, 0.61, 0.36, 1),
              transform 2s cubic-bezier(0.16, 1, 0.3, 1),
              opacity 1.8s ease;
}
.phase-1 .ring-container {
  transform: translate(-50%, -50%) scale(0.1);
  opacity: 1;
}
.phase-gte-2 .ring-container {
  transform: translate(-50%, -50%) scale(1);
  opacity: 1;
  transition: left 3.5s cubic-bezier(0.22, 0.61, 0.36, 1),
              top 3.5s cubic-bezier(0.22, 0.61, 0.36, 1),
              transform 3.5s cubic-bezier(0.22, 0.61, 0.36, 1),
              opacity 2.2s ease;
}
.phase-6 .ring-container {
  opacity: 0;
  transition: opacity 2.2s ease;
}

.ring-body {
  width: min(70vw, 70vh, 520px);
  height: min(70vw, 70vh, 520px);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ring-svg {
  width: 100%;
  height: 100%;
  will-change: transform;
  animation: ring-spin-smooth 3.5s linear infinite;
  filter: drop-shadow(0 0 10px rgba(255, 190, 30, 0.5))
          drop-shadow(0 0 30px rgba(255, 120, 0, 0.2));
  transition: filter 2.5s ease;
}
.phase-gte-4 .ring-svg {
  filter: drop-shadow(0 0 15px rgba(255, 160, 0, 0.7))
          drop-shadow(0 0 40px rgba(255, 80, 0, 0.45))
          drop-shadow(0 0 75px rgba(220, 20, 0, 0.3))
          drop-shadow(0 0 120px rgba(150, 0, 0, 0.15));
}

@keyframes ring-spin-smooth {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.ring-aura {
  opacity: 0.3;
  animation: aura-pulse 3s ease-in-out infinite;
}
.phase-gte-4 .ring-aura {
  opacity: 0.8;
  animation: aura-pulse-hot 2s ease-in-out infinite;
}
@keyframes aura-pulse {
  0%, 100% { opacity: 0.2; }
  50% { opacity: 0.45; }
}
@keyframes aura-pulse-hot {
  0%, 100% { opacity: 0.55; }
  50% { opacity: 0.95; }
}

.elvish-text {
  font-family: 'Tengwar Annatar', serif;
  font-size: 7.5px;
  letter-spacing: 1.2px;
  fill: #8B4513;
  opacity: 0.55;
  animation: inscription-glow 3s ease-in-out infinite alternate;
}
.elvish-inner {
  font-size: 6.8px;
  letter-spacing: 1px;
}
.phase-gte-4 .elvish-text {
  animation: inscription-blaze 1.6s ease-in-out infinite alternate;
}
@keyframes inscription-glow {
  0%   { opacity: 0.3; fill: #7a3b10; }
  100% { opacity: 0.8; fill: #cc6600; }
}
@keyframes inscription-blaze {
  0%   { opacity: 0.65; fill: #cc3300; }
  100% { opacity: 1;    fill: #ff5500; }
}

.sparks-container {
  position: absolute;
  width: 0;
  height: 0;
  z-index: 4;
  will-change: left, top;
  transition: left 2s cubic-bezier(0.16, 1, 0.3, 1),
              top 2s cubic-bezier(0.16, 1, 0.3, 1);
}
.phase-gte-2 .sparks-container {
  transition: left 3.5s cubic-bezier(0.22, 0.61, 0.36, 1),
              top 3.5s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.spark {
  --tx: 0px;
  --ty: 0px;
  --dur: 1s;
  --del: 0s;
  position: absolute;
  border-radius: 50%;
  opacity: 0;
  pointer-events: none;
}
.spark--orange {
  background: #ff7700;
  box-shadow: 0 0 5px 2px rgba(255, 120, 0, 0.8),
              0 0 10px 3px rgba(255, 70, 0, 0.35);
}
.spark--red {
  background: #ff3300;
  box-shadow: 0 0 5px 2px rgba(255, 50, 0, 0.85),
              0 0 12px 4px rgba(200, 0, 0, 0.4);
}
.spark--gold {
  background: #ffcc22;
  box-shadow: 0 0 6px 2px rgba(255, 200, 30, 0.9),
              0 0 14px 5px rgba(255, 160, 0, 0.3);
}
.spark--darkred {
  background: #cc2200;
  box-shadow: 0 0 4px 2px rgba(200, 30, 0, 0.8),
              0 0 10px 3px rgba(150, 0, 0, 0.35);
}

.phase-1 .spark,
.phase-2 .spark {
  animation: spark-shoot var(--dur) ease-out infinite;
  animation-delay: var(--del);
}

.phase-gte-3 .spark {
  animation: spark-die 1.8s ease-out forwards;
  animation-delay: var(--del);
}

.phase-gte-4 .spark {
  animation: none;
  opacity: 0;
}

@keyframes spark-shoot {
  0% {
    opacity: 1;
    transform: translate(0, 0) scale(1);
  }
  20% {
    opacity: 0.9;
  }
  100% {
    opacity: 0;
    transform: translate(var(--tx), var(--ty)) scale(0.1);
  }
}

@keyframes spark-die {
  0%   { opacity: 0.6; }
  100% { opacity: 0; transform: translate(var(--tx), var(--ty)) scale(0); }
}

.embers-container {
  position: fixed;
  inset: 0;
  z-index: 2;
  pointer-events: none;
}
.ember {
  position: absolute;
  width: 2px;
  height: 2px;
  border-radius: 50%;
  background: #ff6600;
  box-shadow: 0 0 4px 1px rgba(255, 80, 0, 0.6);
  opacity: 0;
  animation: ember-float 5s ease-in-out infinite;
}
.ember:nth-child(1)  { left: 12%; top: 85%; animation-delay: 0s; }
.ember:nth-child(2)  { left: 28%; top: 78%; animation-delay: 0.4s; }
.ember:nth-child(3)  { left: 45%; top: 90%; animation-delay: 0.8s; }
.ember:nth-child(4)  { left: 62%; top: 82%; animation-delay: 1.1s; }
.ember:nth-child(5)  { left: 78%; top: 88%; animation-delay: 1.5s; }
.ember:nth-child(6)  { left: 88%; top: 75%; animation-delay: 1.9s; }
.ember:nth-child(7)  { left: 8%;  top: 65%; animation-delay: 0.3s; }
.ember:nth-child(8)  { left: 35%; top: 72%; animation-delay: 1.0s; }
.ember:nth-child(9)  { left: 55%; top: 68%; animation-delay: 1.4s; }
.ember:nth-child(10) { left: 72%; top: 60%; animation-delay: 1.8s; }
.ember:nth-child(11) { left: 92%; top: 55%; animation-delay: 0.6s; }
.ember:nth-child(12) { left: 18%; top: 45%; animation-delay: 1.3s; }
.ember:nth-child(13) { left: 50%; top: 38%; animation-delay: 1.7s; }
.ember:nth-child(14) { left: 82%; top: 42%; animation-delay: 0.5s; }
.ember:nth-child(15) { left: 38%; top: 55%; animation-delay: 0.9s; }
.ember:nth-child(16) { left: 4%;  top: 92%; animation-delay: 0.15s; width: 3px; height: 3px; }
.ember:nth-child(17) { left: 22%; top: 62%; animation-delay: 1.65s; }
.ember:nth-child(18) { left: 68%; top: 85%; animation-delay: 0.55s; width: 3px; height: 3px; }
.ember:nth-child(19) { left: 95%; top: 70%; animation-delay: 1.25s; }
.ember:nth-child(20) { left: 40%; top: 48%; animation-delay: 2.1s; width: 2.5px; height: 2.5px; }
.ember:nth-child(21) { left: 58%; top: 78%; animation-delay: 0.35s; }
.ember:nth-child(22) { left: 14%; top: 75%; animation-delay: 1.85s; width: 3px; height: 3px; }
.ember:nth-child(23) { left: 83%; top: 58%; animation-delay: 0.75s; }
.ember:nth-child(24) { left: 30%; top: 88%; animation-delay: 2.3s; width: 2.5px; height: 2.5px; }
.ember:nth-child(25) { left: 48%; top: 62%; animation-delay: 1.45s; }
.ember:nth-child(26) { left: 76%; top: 78%; animation-delay: 0.25s; width: 3px; height: 3px; }
.ember:nth-child(27) { left: 10%; top: 52%; animation-delay: 2.0s; }
.ember:nth-child(28) { left: 60%; top: 92%; animation-delay: 0.65s; width: 2.5px; height: 2.5px; }

.ember:nth-child(odd) {
  background: #ff4400;
  box-shadow: 0 0 3px 1px rgba(255, 50, 0, 0.5);
}

@keyframes ember-float {
  0% {
    opacity: 0;
    transform: translateY(0) scale(0.5);
  }
  12% {
    opacity: 0.6;
    transform: translateY(-8px) scale(1);
  }
  50% {
    opacity: 0.35;
    transform: translateY(-45px) scale(0.8);
  }
  85% {
    opacity: 0.1;
  }
  100% {
    opacity: 0;
    transform: translateY(-95px) scale(0.15);
  }
}

.sauron-eye-container {
  position: absolute;
  inset: 20%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transform: scale(0.35);
  pointer-events: none;
  transition: none;
}
.phase-gte-4 .sauron-eye-container {
  opacity: 1;
  transform: scale(1);
  transition: opacity 2.5s cubic-bezier(0.16, 1, 0.3, 1),
              transform 2.5s cubic-bezier(0.16, 1, 0.3, 1);
}
.phase-6 .sauron-eye-container {
  opacity: 0;
  transition: opacity 1.5s ease;
}

.sauron-eye-svg {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 0 8px rgba(255, 60, 0, 0.65))
          drop-shadow(0 0 25px rgba(255, 30, 0, 0.35));
  animation: eye-glow-pulse 2.8s ease-in-out infinite;
}

@keyframes eye-glow-pulse {
  0%, 100% {
    filter: drop-shadow(0 0 8px rgba(255, 60, 0, 0.65))
            drop-shadow(0 0 25px rgba(255, 30, 0, 0.35));
  }
  50% {
    filter: drop-shadow(0 0 16px rgba(255, 80, 0, 0.85))
            drop-shadow(0 0 40px rgba(255, 40, 0, 0.5))
            drop-shadow(0 0 70px rgba(180, 0, 0, 0.2));
  }
}

.inscription-text {
  position: absolute;
  bottom: 7%;
  left: 50%;
  transform: translateX(-50%);
  text-align: center;
  z-index: 5;
  opacity: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  pointer-events: none;
  transition: none;
}
.phase-gte-5 .inscription-text {
  opacity: 1;
}

.inscription-line {
  font-family: 'Georgia', 'Palatino Linotype', 'Times New Roman', serif;
  font-style: italic;
  font-size: clamp(11px, 1.5vw, 17px);
  color: #8a5a00;
  text-shadow:
    0 0 6px rgba(180, 100, 0, 0.3),
    0 0 14px rgba(150, 50, 0, 0.18),
    0 0 28px rgba(120, 20, 0, 0.1);
  letter-spacing: 1.2px;
  white-space: nowrap;
  opacity: 0;
  transform: translateY(10px);
}

.phase-gte-5 .inscription-line--1 {
  animation: text-reveal 1.8s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
}
.phase-gte-5 .inscription-line--2 {
  animation: text-reveal 1.8s cubic-bezier(0.25, 0.46, 0.45, 0.94) 0.6s forwards;
}
.phase-6 .inscription-text {
  opacity: 0;
  transition: opacity 1.5s ease;
}

@keyframes text-reveal {
  0% {
    opacity: 0;
    transform: translateY(10px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

.phase-6 {
  animation: overlay-final-fade 2s cubic-bezier(0.4, 0, 0.2, 1) 0.3s forwards;
}

@keyframes overlay-final-fade {
  0%   { opacity: 1; }
  100% { opacity: 0; pointer-events: none; }
}

@media (max-width: 600px) {
  .inscription-line {
    font-size: 10px;
    white-space: normal;
    max-width: 92vw;
    letter-spacing: 0.5px;
  }
  .ring-body {
    width: min(85vw, 85vh, 400px);
    height: min(85vw, 85vh, 400px);
  }
}
</style>
