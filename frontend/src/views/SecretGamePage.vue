<template>
  <div class="secret-page" @keydown.capture="blockParentKeys">

    <div v-if="!gameStarted" class="launch-screen">
      <div class="launch-content">
        <div class="skull-icon">☠</div>
        <h1 class="title">ЯК ТИ СЮДИ ПОТРАПИВ</h1>
        <p class="subtitle">Це місце не для слабких духом.</p>
        <button class="launch-btn" @click="startGame">ЗАПУСТИТИ</button>
      </div>
    </div>

    <div v-else class="game-screen">
      <div class="game-header">
        <button class="back-btn" @click="stopGame">← Назад</button>
        <span class="game-title">DOOM Shareware (Powered by js-dos)</span>
        <button class="fullscreen-btn" @click="toggleFullscreen" :title="isFullscreen ? 'Вийти з повноекранного' : 'Повноекранний режим'">
          {{ isFullscreen ? '⊖' : '⛶' }}
        </button>
        <div class="completion-toggle" @click="showCodeInput = !showCodeInput">
          Секретний код
        </div>
      </div>

      <div class="iframe-wrapper" ref="iframeWrapperRef" @click="focusIframe">
        <iframe
            ref="iframeRef"
            src="/games/doom-player.html"
            class="dos-iframe"
            allow="autoplay; keyboard; fullscreen; pointer-lock"
            @load="focusIframe"
        ></iframe>
        <div v-if="showFocusHint" class="focus-hint" @click="focusIframe">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:middle;margin-right:4px"><rect x="2" y="6" width="20" height="12" rx="2"/><path d="M6 12h4"/><path d="M8 10v4"/><circle cx="15" cy="10" r="1"/><circle cx="18" cy="12" r="1"/></svg> Натисни сюди, щоб керувати грою
        </div>
      </div>

      <div v-if="showCodeInput" class="code-panel">
        <p class="code-label">Введи секретний код підтвердження:</p>
        <div class="code-input-row">
          <input
              v-model="codeInput"
              type="text"
              class="code-input"
              placeholder="??????"
              maxlength="6"
              @keyup.enter="submitCode"
              @input="codeInput = ($event.target as HTMLInputElement).value.toUpperCase()"
          />
          <button class="code-btn" @click="submitCode" :disabled="submitting">
            {{ submitting ? '...' : 'ПІДТВЕРДИТИ' }}
          </button>
        </div>
        <p v-if="codeError" class="code-error">{{ codeError }}</p>
        <p v-if="alreadyCompleted" class="code-success">Ачівку вже отримано!</p>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useAchievementStore } from '../stores/achievements'

const ACHIEVEMENT_FOUND     = 'SECRET_DOOM_FOUND'
const ACHIEVEMENT_COMPLETED = 'SECRET_DOOM_COMPLETED'
const SECRET_CODE           = 'IDDQD'

const achievementStore = useAchievementStore()

const gameStarted      = ref(false)
const showCodeInput    = ref(false)
const codeInput        = ref('')
const codeError        = ref('')
const submitting       = ref(false)
const alreadyFound     = ref(false)
const alreadyCompleted = ref(false)

const iframeRef        = ref<HTMLIFrameElement | null>(null)
const iframeWrapperRef = ref<HTMLElement | null>(null)
const isFullscreen     = ref(false)
const showFocusHint    = ref(true)

onMounted(async () => {
  await claimFoundAchievement()
  document.addEventListener('fullscreenchange', onFullscreenChange)
})

onUnmounted(() => {
  document.body.style.overflow = ''
  document.removeEventListener('fullscreenchange', onFullscreenChange)
})

watch(gameStarted, (started) => {
  if (started) {
    document.body.style.overflow = 'hidden'
    nextTick(() => focusIframe())
  } else {
    document.body.style.overflow = ''
  }
})

function focusIframe() {
  iframeRef.value?.focus()
  showFocusHint.value = false
}

function blockParentKeys(e: KeyboardEvent) {
  if (!gameStarted.value || showCodeInput.value) return
  const gameKeys = ['ArrowUp','ArrowDown','ArrowLeft','ArrowRight','Space','Tab']
  if (gameKeys.includes(e.code)) {
    e.preventDefault()
  }
}

function toggleFullscreen() {
  const el = iframeWrapperRef.value
  if (!el) return
  if (!document.fullscreenElement) {
    el.requestFullscreen().then(() => focusIframe())
  } else {
    document.exitFullscreen()
  }
}

function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
  if (isFullscreen.value) {
    nextTick(() => focusIframe())
  }
}

async function claimFoundAchievement() {
  try {
    await achievementStore.claimSecret(ACHIEVEMENT_FOUND)
  } catch (e: any) {
    if (e?.response?.status === 400 || e?.response?.status === 409) {
      alreadyFound.value = true
    }
  }
}

function startGame() {
  gameStarted.value = true
}

function stopGame() {
  gameStarted.value = false
  showCodeInput.value = false
}

async function submitCode() {
  codeError.value = ''
  if (codeInput.value.trim().toUpperCase() !== SECRET_CODE) {
    codeError.value = 'Невірний код. Підказка: класичний чіт на безсмертя.'
    return
  }
  submitting.value = true
  try {
    await achievementStore.claimSecret(ACHIEVEMENT_COMPLETED)
    showCodeInput.value = false
  } catch (e: any) {
    if (e?.response?.status === 409) alreadyCompleted.value = true
    else codeError.value = 'Помилка сервера.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>

.dos-iframe {
  flex: 1;
  width: 100%;
  height: 100%;
  border: none;
  background: #000;
  min-height: 0;
  display: block;
}

.iframe-wrapper {
  flex: 1;
  position: relative;
  min-height: 0;
  display: flex;
  background: #000;
}

.focus-hint {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.85);
  color: #cc2222;
  font-family: monospace;
  font-size: 14px;
  padding: 10px 24px;
  border: 1px solid #cc2222;
  border-radius: 6px;
  cursor: pointer;
  animation: pulse-hint 2s ease-in-out infinite;
  z-index: 10;
  white-space: nowrap;
  pointer-events: auto;
}

@keyframes pulse-hint {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.fullscreen-btn {
  background: transparent;
  color: #666;
  border: 1px solid #2a2a2a;
  padding: 6px 12px;
  font-size: 16px;
  cursor: pointer;
  border-radius: 4px;
  transition: color 0.2s, border-color 0.2s;
  white-space: nowrap;
  line-height: 1;
}

.fullscreen-btn:hover {
  color: #cc2222;
  border-color: #cc2222;
}

/* Лендінг-екран */
.launch-content {
  text-align: center;
}

.skull-icon {
  font-size: 72px;
  margin-bottom: 16px;
  filter: drop-shadow(0 0 20px #cc2222);
}

.title {
  font-family: monospace;
  font-size: 28px;
  color: #cc2222;
  letter-spacing: 4px;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 14px;
  color: #555;
  margin-bottom: 32px;
}

.launch-btn {
  background: #cc2222;
  color: #fff;
  border: none;
  padding: 14px 44px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 3px;
  font-family: monospace;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.2s;
}

.launch-btn:hover { background: #ff3333; }

.hint {
  font-size: 12px;
  color: #3a3a3a;
  margin: 24px 0 0;
}

.game-screen {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px);
  margin-top: 64px;
  overflow: hidden;
}

.game-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: #111;
  border-bottom: 1px solid #1e1e1e;
  flex-shrink: 0;
}

.back-btn {
  background: transparent;
  color: #666;
  border: 1px solid #2a2a2a;
  padding: 6px 14px;
  font-size: 13px;
  cursor: pointer;
  border-radius: 4px;
  transition: color 0.2s, border-color 0.2s;
  white-space: nowrap;
}

.back-btn:hover { color: #ccc; border-color: #555; }

.game-title {
  font-family: monospace;
  font-size: 13px;
  color: #cc2222;
  letter-spacing: 1px;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.completion-toggle {
  font-size: 13px;
  color: #555;
  cursor: pointer;
  padding: 6px 12px;
  border: 1px solid #2a2a2a;
  border-radius: 4px;
  white-space: nowrap;
  transition: color 0.2s, border-color 0.2s;
}

.completion-toggle:hover { color: #cc2222; border-color: #cc2222; }


.code-panel {
  background: #0e0e0e;
  border-top: 1px solid #1e1e1e;
  padding: 14px 20px;
  flex-shrink: 0;
}

.code-label {
  font-size: 13px;
  color: #666;
  margin: 0 0 10px;
  font-family: monospace;
}

.code-input-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.code-input {
  background: #060606;
  border: 1px solid #2a2a2a;
  color: #cc2222;
  padding: 9px 14px;
  font-size: 20px;
  font-family: monospace;
  letter-spacing: 5px;
  width: 180px;
  border-radius: 4px;
  outline: none;
  transition: border-color 0.2s;
}

.code-input:focus { border-color: #cc2222; }

.code-btn {
  background: transparent;
  color: #cc2222;
  border: 1px solid #cc2222;
  padding: 9px 20px;
  font-family: monospace;
  font-size: 13px;
  letter-spacing: 1px;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.2s, color 0.2s;
}

.secret-page { min-height: 100vh; background: #0a0a0a; color: #e0e0e0; display: flex; flex-direction: column; }
.launch-screen { flex: 1; display: flex; align-items: center; justify-content: center; padding-top: 64px; }

</style>