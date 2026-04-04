<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { usePartyStore } from '../stores/parties'
import { useGameStore } from '../stores/games'
import { useAuthStore } from '../stores/auth'
import { usePresetStore } from '../stores/presets'
import { useToast } from '../composables/useToast'
import { PUBLIC_BASE_URL } from '../config'
import SuggestGameModal from './SuggestGameModal.vue'
import type { CreatePartyRequest } from '../types'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits<{ (e: 'close'): void }>()

const router = useRouter()
const partyStore = usePartyStore()
const gameStore = useGameStore()
const auth = useAuthStore()
const presetStore = usePresetStore()
const toast = useToast()

const showSuggest = ref(false)
const blockedByParty = ref(false)
const activeTab = ref<'create' | 'presets' | 'history'>('create')
const presetEditMode = ref(false)
const presetEditSlot = ref<number | null>(null)
const presetName = ref('')

const PLATFORMS = [
  { value: 'PC', label: 'PC' },
  { value: 'PLAYSTATION', label: 'PlayStation' },
  { value: 'XBOX', label: 'Xbox' },
  { value: 'NINTENDO', label: 'Nintendo' },
  { value: 'MOBILE', label: 'Mobile' },
  { value: 'OTHER', label: 'Інше' },
] as const

const ALL_LANGUAGES = [
  { code: 'UA', name: 'Українська' },
  { code: 'EN', name: 'English' },
  { code: 'PL', name: 'Polski' },
  { code: 'DE', name: 'Deutsch' },
  { code: 'FR', name: 'Français' },
  { code: 'ES', name: 'Español' },
  { code: 'PT', name: 'Português' },
  { code: 'TR', name: 'Türkçe' },
  { code: 'KO', name: '한국어' },
  { code: 'ZH', name: '中文' },
  { code: 'JA', name: '日本語' },
  { code: 'IT', name: 'Italiano' },
  { code: 'NL', name: 'Nederlands' },
  { code: 'SV', name: 'Svenska' },
  { code: 'NO', name: 'Norsk' },
  { code: 'DA', name: 'Dansk' },
  { code: 'FI', name: 'Suomi' },
  { code: 'CS', name: 'Čeština' },
  { code: 'SK', name: 'Slovenčina' },
  { code: 'HU', name: 'Magyar' },
  { code: 'RO', name: 'Română' },
  { code: 'BG', name: 'Български' },
  { code: 'HR', name: 'Hrvatski' },
  { code: 'SR', name: 'Srpski' },
  { code: 'AR', name: 'العربية' },
  { code: 'HI', name: 'हिन्दी' },
  { code: 'VI', name: 'Tiếng Việt' },
  { code: 'TH', name: 'ภาษาไทย' },
  { code: 'ID', name: 'Bahasa Indonesia' },
] as const

const REGIONS = [
  { value: 'EUROPE', label: 'Європа', icon: '🌍' },
  { value: 'NORTH_AMERICA', label: 'Пн. Америка', icon: '🌎' },
  { value: 'SOUTH_AMERICA', label: 'Пд. Америка', icon: '🌎' },
  { value: 'ASIA', label: 'Азія', icon: '🌏' },
  { value: 'MIDDLE_EAST', label: 'Близький Схід', icon: '🌍' },
  { value: 'AFRICA', label: 'Африка', icon: '🌍' },
  { value: 'OCEANIA', label: 'Океанія', icon: '🌏' },
] as const

const TAG_PRESETS = ['mic required', 'ranked', 'chill', 'tryhard', 'newbie-friendly', 'no toxicity', '18+', 'fun'] as const

const form = ref<CreatePartyRequest>({
  gameId: null,
  title: '',
  description: '',
  eventTime: null,
  platform: auth.myProfile?.platforms?.length ? [...auth.myProfile.platforms] : ['PC'],
  languages: auth.myProfile?.languages?.length ? [...auth.myProfile.languages] : ['UA'],
  skillLevel: auth.myProfile?.skillLevel ?? null,
  playStyle: auth.myProfile?.playStyle ?? null,
  tags: [],
  region: auth.myProfile?.region ?? null,
  maxMembers: 4,
})

const gameSearch = ref('')
const submitting = ref(false)
const error = ref('')
const customTag = ref('')
const langSearch = ref('')

const filteredGames = computed(() => {
  const q = gameSearch.value.toLowerCase().trim()
  if (!q) return gameStore.games.slice(0, 20)
  return gameStore.games.filter(g => g.name.toLowerCase().includes(q))
})

const filteredLanguages = computed(() => {
  const q = langSearch.value.toLowerCase().trim()
  if (!q) return ALL_LANGUAGES
  return ALL_LANGUAGES.filter(l =>
    l.code.toLowerCase().includes(q) || l.name.toLowerCase().includes(q)
  )
})

function langName(code: string): string {
  return ALL_LANGUAGES.find(l => l.code === code)?.name ?? code
}

const selectedGame = computed(() =>
  form.value.gameId ? gameStore.games.find(g => g.id === form.value.gameId) : null
)

const gameMaxPartySize = computed(() => selectedGame.value?.maxPartySize ?? 5)

const memberOptions = computed(() => {
  const max = gameMaxPartySize.value
  const opts: number[] = []
  for (let i = 2; i <= max; i++) opts.push(i)
  return opts
})

watch(() => form.value.gameId, () => {
  form.value.maxMembers = gameMaxPartySize.value
})

function togglePlatform(p: string) {
  const idx = form.value.platform.indexOf(p)
  if (idx !== -1) {
    if (form.value.platform.length > 1) form.value.platform.splice(idx, 1)
  } else {
    form.value.platform.push(p)
  }
}

function toggleLanguage(lang: string) {
  const idx = form.value.languages.indexOf(lang)
  if (idx !== -1) {
    if (form.value.languages.length > 1) form.value.languages.splice(idx, 1)
  } else {
    if (form.value.languages.length < 4) form.value.languages.push(lang)
  }
}

function toggleTag(tag: string) {
  const idx = form.value.tags.indexOf(tag)
  if (idx !== -1) {
    form.value.tags.splice(idx, 1)
  } else {
    if (form.value.tags.length < 5) form.value.tags.push(tag)
  }
}

function addCustomTag() {
  const tag = customTag.value.trim().toLowerCase()
  if (!tag || tag.length > 30) return
  if (form.value.tags.includes(tag)) return
  if (form.value.tags.length >= 5) return
  form.value.tags.push(tag)
  customTag.value = ''
}

function selectGame(gameId: number) {
  form.value.gameId = gameId
  gameSearch.value = gameStore.games.find(g => g.id === gameId)?.name ?? ''
}

watch(() => props.visible, async (val) => {
  if (!val) { blockedByParty.value = false; return }
  activeTab.value = 'create'
  presetEditMode.value = false
  presetEditSlot.value = null
  presetName.value = ''

  await auth.fetchMyProfile()
  resetForm()
  presetStore.fetchPresets()
  partyStore.fetchHistory(0)

  if (partyStore.playAgainData) {
    const d = partyStore.playAgainData
    form.value.gameId = d.gameId
    form.value.title = d.title || ''
    form.value.description = d.description || ''
    form.value.platform = d.platform?.length ? [...d.platform] : form.value.platform
    form.value.languages = d.languages?.length ? [...d.languages] : form.value.languages
    form.value.skillLevel = d.skillLevel ?? form.value.skillLevel
    form.value.playStyle = d.playStyle ?? form.value.playStyle
    form.value.tags = d.tags?.length ? [...d.tags] : []
    form.value.region = d.region ?? form.value.region
    form.value.maxMembers = d.maxMembers ?? 4
    if (d.gameId) {
      const game = gameStore.games.find(g => g.id === d.gameId)
      if (game) gameSearch.value = game.name
    }
  }

  await partyStore.fetchMyParties()
  if (partyStore.myParties.length > 0) {
    blockedByParty.value = true
    toast.show('Ви не можете створити нове лобі, перебуваючи в існуючому', 'error', 4000)
    error.value = 'Ви вже перебуваєте в активному лобі. Покиньте поточне, щоб створити нове.'
  }
})

const historySlice = computed(() => partyStore.historyParties.slice(0, 5))

function playAgainFromHistory(party: import('../types').Party) {
  partyStore.setPlayAgain(party)
  const d = partyStore.playAgainData!
  form.value.gameId = d.gameId
  form.value.title = d.title || ''
  form.value.description = d.description || ''
  form.value.platform = d.platform?.length ? [...d.platform] : form.value.platform
  form.value.languages = d.languages?.length ? [...d.languages] : form.value.languages
  form.value.skillLevel = d.skillLevel ?? form.value.skillLevel
  form.value.playStyle = d.playStyle ?? form.value.playStyle
  form.value.tags = d.tags?.length ? [...d.tags] : []
  form.value.region = d.region ?? form.value.region
  form.value.maxMembers = d.maxMembers ?? 4
  if (d.gameId) {
    const game = gameStore.games.find(g => g.id === d.gameId)
    if (game) gameSearch.value = game.name
  }
  activeTab.value = 'create'
}

function formatHistoryDate(iso: string): string {
  return new Date(iso).toLocaleDateString('uk-UA', {
    day: '2-digit', month: '2-digit', year: 'numeric',
  })
}

function historyStatusLabel(status: string): string {
  const map: Record<string, string> = {
    COMPLETED: 'Завершено',
    CANCELLED: 'Скасовано',
    OPEN: 'Відкрито',
    FULL: 'Заповнено',
    IN_GAME: 'В грі',
  }
  return map[status] ?? status
}

function resolveAvatar(url: string | null): string {
  if (!url) return PUBLIC_BASE_URL + '/avatars/default/avatar-1.png'
  if (url.startsWith('http')) return url
  return PUBLIC_BASE_URL + url
}

async function submit() {
  if (blockedByParty.value || partyStore.myParties.length > 0) {
    error.value = 'Ви вже перебуваєте в активному лобі. Покиньте поточне, щоб створити нове.'
    return
  }
  if (!form.value.gameId) {
    error.value = 'Оберіть гру'
    return
  }
  if (!auth.isLoggedIn) {
    error.value = 'Потрібно увійти для створення лобі'
    return
  }

  error.value = ''
  submitting.value = true
  try {
    const playAgain = partyStore.playAgainData
    const newParty = await partyStore.createParty(form.value)

    if (playAgain?.previousMembers?.length) {
      const myId = auth.user?.id
      for (const member of playAgain.previousMembers) {
        if (member.userId !== myId) {
          try {
            await partyStore.sendInvite(newParty.id, member.userId)
          } catch {  }
        }
      }
      toast.show(`Запрошення надіслано ${playAgain.previousMembers.filter(m => m.userId !== myId).length} гравцям`, 'success', 3000)
    }

    partyStore.playAgainData = null
    resetForm()
    emit('close')
    router.push(`/party/${newParty.id}`)
  } catch (e: any) {
    error.value = e.response?.data?.message || 'Помилка при створенні лобі'
  } finally {
    submitting.value = false
  }
}

function getProfileDefaults() {
  const p = auth.myProfile
  return {
    platform: p?.platforms?.length ? [...p.platforms] : ['PC'],
    languages: p?.languages?.length ? [...p.languages] : ['UA'],
    skillLevel: p?.skillLevel ?? null,
    playStyle: p?.playStyle ?? null,
    region: p?.region ?? null,
  }
}

function resetForm() {
  const defaults = getProfileDefaults()
  form.value = {
    gameId: null,
    title: '',
    description: '',
    eventTime: null,
    platform: defaults.platform,
    languages: defaults.languages,
    skillLevel: defaults.skillLevel,
    playStyle: defaults.playStyle,
    tags: [],
    region: defaults.region,
    maxMembers: 4,
  }
  gameSearch.value = ''
  customTag.value = ''
  langSearch.value = ''
  error.value = ''
}

function startPresetCreate(slotIndex: number) {
  presetEditSlot.value = slotIndex
  presetEditMode.value = true
  presetName.value = ''
  resetForm()
}

function loadPreset(preset: { gameId: number; gameName?: string; maxMembers: number; platform: string[]; languages: string[]; skillLevel: string | null; playStyle: string | null; tags: string[]; region: string | null }) {
  activeTab.value = 'create'
  presetEditMode.value = false
  form.value.gameId = preset.gameId
  form.value.platform = [...preset.platform]
  form.value.languages = [...preset.languages]
  form.value.skillLevel = preset.skillLevel
  form.value.playStyle = preset.playStyle
  form.value.tags = [...preset.tags]
  form.value.region = preset.region
  form.value.maxMembers = preset.maxMembers
  if (preset.gameId) {
    const game = gameStore.games.find(g => g.id === preset.gameId)
    if (game) gameSearch.value = game.name
  }
}

async function savePreset() {
  if (!presetName.value.trim()) {
    error.value = 'Введіть назву шаблону'
    return
  }
  if (!form.value.gameId) {
    error.value = 'Оберіть гру'
    return
  }
  error.value = ''
  submitting.value = true
  try {
    await presetStore.savePreset({
      name: presetName.value.trim(),
      slotIndex: presetEditSlot.value ?? 0,
      gameId: form.value.gameId,
      maxMembers: form.value.maxMembers,
      platform: form.value.platform,
      languages: form.value.languages,
      skillLevel: form.value.skillLevel,
      playStyle: form.value.playStyle,
      tags: form.value.tags,
      region: form.value.region,
    })
    toast.show('Шаблон збережено', 'success', 2500)
    presetEditMode.value = false
    activeTab.value = 'presets'
    resetForm()
  } catch (e: any) {
    error.value = e.response?.data?.message || 'Помилка при збереженні шаблону'
  } finally {
    submitting.value = false
  }
}

async function deletePreset(presetId: number) {
  try {
    await presetStore.deletePreset(presetId)
    toast.show('Шаблон видалено', 'success', 2000)
  } catch {
    toast.show('Не вдалося видалити шаблон', 'error', 2500)
  }
}

function close() {
  resetForm()
  emit('close')
}
</script>

<template>
  <Transition name="fade">
    <div v-if="visible" class="modal-overlay" @click.self="close">
      <div class="modal">
        <div class="modal-close-bar">
          <button class="modal-close" @click="close">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>

        <div class="modal-tabs">
          <button
            class="modal-tab"
            :class="{ active: activeTab === 'create' && !presetEditMode }"
            @click="activeTab = 'create'; presetEditMode = false"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
              <line x1="19" y1="8" x2="19" y2="14"/><line x1="16" y1="11" x2="22" y2="11"/>
            </svg>
            НОВЕ ЛОБІ
          </button>
          <button
            class="modal-tab"
            :class="{ active: activeTab === 'presets' || presetEditMode }"
            @click="activeTab = 'presets'; presetEditMode = false"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>
              <polyline points="10 9 9 9 8 9"/>
            </svg>
            ШАБЛОН
          </button>
          <button
            class="modal-tab"
            :class="{ active: activeTab === 'history' }"
            @click="activeTab = 'history'; presetEditMode = false"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
            </svg>
            ІСТОРІЯ
          </button>
        </div>

        <Transition name="tab-fade" mode="out-in">
        <div v-if="activeTab === 'presets' && !presetEditMode" key="presets" class="modal-body presets-body">
          <div class="presets-grid">
            <div
              v-for="slot in 10"
              :key="slot - 1"
              class="preset-slot"
              :class="{ filled: presetStore.getPresetBySlot(slot - 1) }"
              @click="presetStore.getPresetBySlot(slot - 1) ? loadPreset(presetStore.getPresetBySlot(slot - 1)!) : startPresetCreate(slot - 1)"
            >
              <template v-if="presetStore.getPresetBySlot(slot - 1)">
                <img
                  v-if="presetStore.getPresetBySlot(slot - 1)!.gameImageUrl"
                  :src="presetStore.getPresetBySlot(slot - 1)!.gameImageUrl!"
                  class="preset-game-img"
                />
                <div v-else class="preset-game-ph">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="6" width="20" height="12" rx="2"/><line x1="6" y1="12" x2="10" y2="12"/><circle cx="16" cy="12" r="1" fill="currentColor" stroke="none"/></svg>
                </div>
                <div class="preset-info">
                  <div class="preset-name">{{ presetStore.getPresetBySlot(slot - 1)!.name }}</div>
                  <div class="preset-game-name">{{ presetStore.getPresetBySlot(slot - 1)!.gameName }}</div>
                </div>
                <button
                  class="preset-delete"
                  @click.stop="deletePreset(presetStore.getPresetBySlot(slot - 1)!.id)"
                  title="Видалити шаблон"
                >
                  <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                </button>
              </template>
              <template v-else>
                <div class="preset-empty-icon">+</div>
                <div class="preset-empty-text">Створити шаблон</div>
              </template>
            </div>
          </div>
        </div>

        <div v-else-if="activeTab === 'history'" key="history" class="modal-body history-body">
          <div v-if="partyStore.historyLoading" class="history-empty">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            <span>Завантаження...</span>
          </div>
          <div v-else-if="historySlice.length === 0" class="history-empty">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="6" width="20" height="12" rx="2"/><line x1="6" y1="12" x2="10" y2="12"/><circle cx="16" cy="12" r="1" fill="currentColor" stroke="none"/></svg>
            <span>Історія порожня</span>
          </div>
          <div v-else class="history-mini-list">
            <div v-for="hp in historySlice" :key="hp.id" class="history-mini-card">
              <div class="hm-cover">
                <img v-if="hp.gameImageUrl" :src="hp.gameImageUrl" :alt="hp.gameName" />
                <div v-else class="hm-cover-ph">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="6" width="20" height="12" rx="2"/><line x1="6" y1="12" x2="10" y2="12"/><circle cx="16" cy="12" r="1" fill="currentColor" stroke="none"/></svg>
                </div>
              </div>
              <div class="hm-info">
                <div class="hm-game">{{ hp.gameName }}</div>
                <div class="hm-meta">
                  <span class="hm-status" :class="hp.status.toLowerCase().replace('_', '-')">
                    <svg v-if="hp.status === 'COMPLETED'" width="8" height="8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
                    <svg v-else width="8" height="8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                    {{ historyStatusLabel(hp.status) }}
                  </span>
                  <span class="hm-date">{{ formatHistoryDate(hp.createdAt) }}</span>
                </div>
                <div v-if="hp.members?.length" class="hm-avatars">
                  <img
                    v-for="m in hp.members.slice(0, 4)"
                    :key="m.userId"
                    :src="resolveAvatar(m.avatarUrl)"
                    class="hm-avatar"
                  />
                  <span v-if="(hp.members?.length ?? 0) > 4" class="hm-more">+{{ (hp.members?.length ?? 0) - 4 }}</span>
                </div>
              </div>
              <button class="hm-replay-btn" @click="playAgainFromHistory(hp)" title="Грати знову">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
                </svg>
              </button>
            </div>
          </div>
        </div>

        <div v-else key="create" class="tab-pane-create">
          <div class="modal-body">
            <div v-if="error" class="modal-error">{{ error }}</div>

            <div v-if="presetEditMode" class="form-group">
              <label class="form-label">Назва шаблону *</label>
              <input
                class="form-input"
                v-model="presetName"
                placeholder="Наприклад: Ranked Valorant, Chill CS2..."
                maxlength="50"
              />
            </div>

            <div class="form-group">
              <label class="form-label">Гра *</label>
            <div class="game-search-wrapper">
              <input
                class="form-input"
                v-model="gameSearch"
                placeholder="Пошук гри..."
                @focus="form.gameId = null"
              />
              <div v-if="!form.gameId && gameSearch.length > 0" class="game-dropdown">
                <div
                  v-for="g in filteredGames"
                  :key="g.id"
                  class="game-option"
                  @click="selectGame(g.id)"
                >
                  <img v-if="g.imageUrl" :src="g.imageUrl" class="game-option-img" />
                  <span>{{ g.name }}</span>
                </div>
                <div v-if="filteredGames.length === 0" class="game-option-empty">
                  <span>Гру не знайдено.</span>
                  <button class="suggest-link" @click.stop="showSuggest = true">Запропонувати додати до каталогу</button>
                </div>
              </div>
            </div>
            <div v-if="selectedGame" class="selected-game-badge">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
              {{ selectedGame.name }} (макс. {{ selectedGame.maxPartySize }} гравців)
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Назва лобі</label>
            <input
              class="form-input"
              v-model="form.title"
              placeholder="Наприклад: Ranked push, Chill evening..."
              maxlength="150"
            />
          </div>

          <div class="form-group">
            <label class="form-label">Платформа</label>
            <div class="toggle-group">
              <button
                v-for="p in PLATFORMS"
                :key="p.value"
                type="button"
                class="toggle-btn"
                :class="{ active: form.platform.includes(p.value) }"
                @click="togglePlatform(p.value)"
              >{{ p.label }}</button>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Мова <span class="form-hint">(макс. 4)</span></label>
            <div v-if="form.languages.length" class="selected-tags" style="margin-bottom: 8px">
              <span v-for="code in form.languages" :key="code" class="tag-chip">
                {{ langName(code) }}
                <button v-if="form.languages.length > 1" class="tag-remove" @click="toggleLanguage(code)">✕</button>
              </span>
            </div>
            <input
              class="form-input"
              v-model="langSearch"
              placeholder="Пошук мови..."
              style="margin-bottom: 6px"
            />
            <div class="toggle-group">
              <button
                v-for="lang in filteredLanguages"
                :key="lang.code"
                type="button"
                class="toggle-btn lang-btn"
                :class="{ active: form.languages.includes(lang.code) }"
                :disabled="!form.languages.includes(lang.code) && form.languages.length >= 4"
                @click="toggleLanguage(lang.code)"
              >
                <span class="lang-code">{{ lang.code }}</span>
                <span class="lang-name">{{ lang.name }}</span>
              </button>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Регіон</label>
            <div class="toggle-group">
              <button
                v-for="r in REGIONS"
                :key="r.value"
                type="button"
                class="toggle-btn region-btn"
                :class="{ active: form.region === r.value }"
                @click="form.region = form.region === r.value ? null : r.value"
              >
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>
                {{ r.label }}
              </button>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Рівень гри</label>
              <select class="form-input form-select-modal" v-model="form.skillLevel">
                <option :value="null">Будь-який</option>
                <option value="BEGINNER">Початківець</option>
                <option value="INTERMEDIATE">Середній</option>
                <option value="ADVANCED">Просунутий</option>
                <option value="EXPERT">Експерт</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Стиль гри</label>
              <select class="form-input form-select-modal" v-model="form.playStyle">
                <option :value="null">— Не вказано —</option>
                <option value="CASUAL">Casual</option>
                <option value="SEMI_COMPETITIVE">Semi-competitive</option>
                <option value="COMPETITIVE">Competitive</option>
              </select>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">
              Максимум гравців
              <span v-if="selectedGame" class="form-hint">(за замовчуванням: {{ gameMaxPartySize }})</span>
            </label>
            <div v-if="!selectedGame" class="members-hint">Спочатку оберіть гру</div>
            <div v-else class="slots-picker">
              <div class="slots-row">
                <div
                  v-for="n in memberOptions"
                  :key="n"
                  class="slot-square"
                  :class="{
                    filled: n <= form.maxMembers,
                    empty: n > form.maxMembers
                  }"
                  @click="form.maxMembers = n"
                >
                  <svg v-if="n <= form.maxMembers" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
                  </svg>
                  <span v-else class="slot-plus">+</span>
                </div>
              </div>
              <div class="slots-counter">
                <span class="slots-count-active">{{ form.maxMembers }}</span>
                <span class="slots-count-sep">/</span>
                <span class="slots-count-max">{{ gameMaxPartySize }}</span>
                <span class="slots-count-label">гравців</span>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Теги <span class="form-hint">(макс. 5)</span></label>
            <div class="toggle-group">
              <button
                v-for="tag in TAG_PRESETS"
                :key="tag"
                type="button"
                class="toggle-btn tag-btn"
                :class="{ active: form.tags.includes(tag) }"
                :disabled="!form.tags.includes(tag) && form.tags.length >= 5"
                @click="toggleTag(tag)"
              >{{ tag }}</button>
            </div>
            <div class="custom-tag-row" v-if="form.tags.length < 5">
              <input
                class="form-input custom-tag-input"
                v-model="customTag"
                placeholder="Свій тег..."
                maxlength="30"
                @keydown.enter.prevent="addCustomTag"
              />
              <button type="button" class="add-tag-btn" @click="addCustomTag">+</button>
            </div>
            <div v-if="form.tags.length" class="selected-tags">
              <span v-for="tag in form.tags" :key="tag" class="tag-chip">
                {{ tag }}
                <button class="tag-remove" @click="toggleTag(tag)">✕</button>
              </span>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Опис (необов'язково) <span class="form-hint">({{ form.description.length }}/200)</span></label>
            <textarea
                class="form-textarea"
                v-model="form.description"
                placeholder="Що ти шукаєш у тімейтах? Стиль гри, вимоги, плани..."
                maxlength="200"
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="presetEditMode ? (presetEditMode = false, activeTab = 'presets') : close()">
            {{ presetEditMode ? 'НАЗАД' : 'СКАСУВАТИ' }}
          </button>
          <button
            v-if="presetEditMode"
            class="btn-submit"
            @click="savePreset"
            :disabled="submitting"
          >
            {{ submitting ? 'ЗБЕРЕЖЕННЯ...' : 'ЗБЕРЕГТИ ШАБЛОН' }}
          </button>
          <button
            v-else
            class="btn-submit"
            @click="submit"
            :disabled="submitting || blockedByParty"
          >
            {{ submitting ? 'СТВОРЕННЯ...' : 'СТВОРИТИ ЛОБІ' }}
          </button>
        </div>
        </div>
        </Transition>
      </div>
    </div>
  </Transition>

  <SuggestGameModal v-if="showSuggest" @close="showSuggest = false" @submitted="showSuggest = false" />
</template>

<style scoped>
.modal {
  height: 85vh;
  max-height: 85vh;
  overflow: hidden;
}

.modal-close-bar {
  display: flex;
  justify-content: flex-end;
  padding: 8px 10px 0;
  flex-shrink: 0;
}
.modal-close {
  background: none;
  border: 1px solid var(--border);
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--gray);
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s;
}
.modal-close:hover {
  border-color: var(--red);
  color: var(--red);
}

.modal-tabs {
  display: flex;
  border-bottom: 2px solid var(--border);
  flex-shrink: 0;
}
.modal-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 16px;
  font-family: var(--font-display), sans-serif;
  font-size: 12px;
  letter-spacing: 2px;
  color: var(--gray);
  background: none;
  border: none;
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
}
.modal-tab:hover { color: var(--gray-light); }
.modal-tab.active {
  color: var(--yellow);
  border-bottom-color: var(--yellow);
}

.presets-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(5, 1fr);
  gap: 10px;
  flex: 1;
}
.preset-slot {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 2px solid var(--border);
  background: var(--panel-light);
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
  position: relative;
  min-height: 60px;
}
.preset-slot:hover {
  border-color: var(--yellow-dim);
  background: rgba(245, 197, 24, 0.03);
}
.preset-slot.filled {
  border-color: rgba(245, 197, 24, 0.15);
}
.preset-game-img {
  width: 36px;
  height: 48px;
  object-fit: cover;
  border: 1px solid var(--border);
  flex-shrink: 0;
}
.preset-game-ph {
  width: 36px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--dark);
  border: 1px solid var(--border);
  color: var(--gray);
  flex-shrink: 0;
}
.preset-info {
  flex: 1;
  min-width: 0;
}
.preset-name {
  font-family: var(--font-display), sans-serif;
  font-size: 12px;
  letter-spacing: 1px;
  color: var(--yellow);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.preset-game-name {
  font-size: 11px;
  color: var(--gray);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.preset-delete {
  position: absolute;
  top: 6px;
  right: 6px;
  background: none;
  border: none;
  color: var(--gray);
  font-size: 12px;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s, color 0.15s;
}
.preset-slot:hover .preset-delete { opacity: 1; }
.preset-delete:hover { color: var(--red); }
.preset-empty-icon {
  font-family: var(--font-display), sans-serif;
  font-size: 24px;
  color: var(--gray);
  width: 36px;
  text-align: center;
}
.preset-empty-text {
  font-family: var(--font-display), sans-serif;
  font-size: 11px;
  letter-spacing: 1.5px;
  color: var(--gray);
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.modal-error {
  color: var(--red);
  font-size: 13px;
  margin-bottom: 12px;
  letter-spacing: 0.5px;
  padding: 10px 14px;
  border: 1px solid rgba(192, 57, 43, 0.3);
  background: rgba(192, 57, 43, 0.06);
  line-height: 1.5;
}

.game-search-wrapper {
  position: relative;
}

.game-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: var(--panel);
  border: 2px solid var(--border);
  max-height: 200px;
  overflow-y: auto;
  z-index: 100;
}

.game-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 13px;
  color: var(--gray-light);
  transition: background 0.1s;
}
.game-option:hover {
  background: var(--yellow-glow);
  color: var(--yellow);
}
.game-option.disabled {
  color: var(--gray);
  cursor: default;
}
.game-option-img {
  width: 32px;
  height: 42px;
  object-fit: cover;
  border: 1px solid var(--border);
}

.selected-game-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--yellow);
  margin-top: 6px;
  letter-spacing: 0.5px;
}

.toggle-group {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.toggle-btn {
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 1.5px;
  padding: 6px 14px;
  background: var(--dark);
  border: 2px solid var(--border);
  color: var(--gray-light);
  cursor: pointer;
  transition: all 0.15s;
}
.toggle-btn:hover:not(:disabled) {
  border-color: var(--yellow-dim);
  color: var(--white);
}
.toggle-btn.active {
  background: var(--yellow-glow);
  border-color: var(--yellow);
  color: var(--yellow);
  font-weight: 700;
}
.toggle-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}
.toggle-btn.tag-btn {
  font-family: var(--font-body);
  font-size: 12px;
  letter-spacing: 0.5px;
  text-transform: none;
}

.toggle-btn.lang-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  font-size: 11px;
}
.lang-code {
  font-weight: 700;
  font-size: 11px;
}
.lang-name {
  font-family: var(--font-body);
  font-size: 10px;
  opacity: 0.7;
  letter-spacing: 0;
}

.custom-tag-row {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.custom-tag-input {
  flex: 1;
  font-size: 13px;
}
.add-tag-btn {
  font-family: var(--font-display);
  font-size: 18px;
  width: 38px;
  background: var(--dark);
  border: 2px solid var(--border);
  color: var(--yellow);
  cursor: pointer;
  transition: all 0.15s;
}
.add-tag-btn:hover {
  background: var(--yellow-glow);
  border-color: var(--yellow);
}

.selected-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 8px;
}
.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 3px 10px;
  background: var(--yellow-glow);
  border: 1px solid var(--yellow-dim);
  color: var(--yellow);
  letter-spacing: 0.5px;
}
.tag-remove {
  background: none;
  border: none;
  color: var(--yellow);
  cursor: pointer;
  font-size: 11px;
  padding: 0 2px;
  opacity: 0.7;
  transition: opacity 0.15s;
}
.tag-remove:hover {
  opacity: 1;
}

.slots-picker {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.slots-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.slot-square {
  width: 42px;
  height: 42px;
  border: 2px solid var(--border);
  background: var(--dark);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.slot-square.filled {
  border-color: var(--yellow-dim);
  background: rgba(245, 197, 24, 0.08);
  color: var(--yellow);
  box-shadow: 0 0 8px rgba(245, 197, 24, 0.12), inset 0 0 12px rgba(245, 197, 24, 0.05);
}

.slot-square.filled::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(245, 197, 24, 0.1) 0%, transparent 60%);
  pointer-events: none;
}

.slot-square.empty {
  border-style: dashed;
  border-color: rgba(255, 255, 255, 0.12);
  color: var(--gray);
}

.slot-square:hover {
  border-color: var(--yellow-dim);
  background: rgba(245, 197, 24, 0.05);
  transform: translateY(-1px);
}

.slot-square:hover .slot-plus {
  color: var(--yellow);
}

.slot-plus {
  font-family: var(--font-display);
  font-size: 16px;
  color: var(--gray);
  transition: color 0.15s;
}

.slots-counter {
  display: flex;
  align-items: baseline;
  gap: 2px;
  font-family: var(--font-display);
  letter-spacing: 1px;
}

.slots-count-active {
  font-size: 20px;
  color: var(--yellow);
  font-weight: 700;
}

.slots-count-sep {
  font-size: 14px;
  color: var(--gray);
}

.slots-count-max {
  font-size: 14px;
  color: var(--gray-light);
}

.slots-count-label {
  font-size: 11px;
  color: var(--gray);
  margin-left: 6px;
  letter-spacing: 1.5px;
}

.toggle-btn.region-btn {
  display: flex;
  align-items: center;
  gap: 5px;
}

.toggle-btn.region-btn svg {
  flex-shrink: 0;
}

.history-body {
  padding: 16px;
  flex: 1;
  overflow-y: auto;
}

.presets-body {
  padding: 20px;
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.tab-pane-create {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tab-fade-enter-active,
.tab-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.tab-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}
.tab-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
.history-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 60px 20px;
  color: var(--gray);
  font-size: 13px;
  flex: 1;
}
.history-mini-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.history-mini-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 2px solid var(--border);
  background: var(--panel-light);
  transition: border-color 0.15s, background 0.15s;
}
.history-mini-card:hover {
  border-color: var(--yellow-dim);
  background: rgba(245, 197, 24, 0.03);
}
.hm-cover img {
  width: 38px;
  height: 52px;
  object-fit: cover;
  border: 1px solid var(--border);
  flex-shrink: 0;
}
.hm-cover-ph {
  width: 38px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--dark);
  border: 1px solid var(--border);
  color: var(--gray);
  flex-shrink: 0;
}
.hm-info {
  flex: 1;
  min-width: 0;
}
.hm-game {
  font-family: var(--font-display), sans-serif;
  font-size: 15px;
  letter-spacing: 1px;
  color: var(--yellow);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.hm-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 3px;
}
.hm-status {
  display: flex;
  align-items: center;
  gap: 3px;
  font-family: var(--font-display), sans-serif;
  font-size: 10px;
  letter-spacing: 1px;
}
.hm-status.completed { color: var(--green); }
.hm-status.cancelled { color: var(--red); }
.hm-status.open { color: var(--green); }
.hm-status.in-game { color: var(--yellow); }
.hm-date {
  font-size: 10px;
  color: var(--gray);
}
.hm-avatars {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-top: 5px;
}
.hm-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--border);
  margin-left: -4px;
}
.hm-avatar:first-child { margin-left: 0; }
.hm-more {
  font-size: 10px;
  color: var(--gray);
  margin-left: 4px;
}
.hm-replay-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 2px solid var(--yellow-dim);
  background: transparent;
  color: var(--yellow);
  cursor: pointer;
  transition: all 0.15s;
  flex-shrink: 0;
}
.hm-replay-btn:hover {
  background: var(--yellow);
  color: var(--black);
}

.form-hint {
  font-size: 0.8rem;
  color: var(--gray);
  font-family: var(--font-body);
  font-weight: 400;
  letter-spacing: 0;
}

.members-hint {
  font-size: 0.85rem;
  color: var(--gray);
  padding: 10px 0;
}

.game-option-empty {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  padding: 10px 12px;
  font-size: 13px;
  color: var(--gray);
}

.suggest-link {
  background: none;
  border: none;
  color: var(--yellow);
  font-size: 13px;
  font-family: var(--font-body);
  cursor: pointer;
  padding: 0;
  text-decoration: underline;
  text-underline-offset: 3px;
  transition: color 0.15s;
}
.suggest-link:hover {
  color: var(--yellow-dim);
}
</style>