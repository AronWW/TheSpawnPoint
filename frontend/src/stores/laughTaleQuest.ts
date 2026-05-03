import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { useAuthStore } from './auth'

export const LAUGH_TALE_ACHIEVEMENT_CODE = 'LAUGH_TALE'

export const LAUGH_TALE_CLUES = ['ROAD_1', 'ROAD_2', 'ROAD_3', 'ROAD_4'] as const
export type LaughTaleClueCode = typeof LAUGH_TALE_CLUES[number]

export interface LaughTaleClue {
  code: LaughTaleClueCode
  numeral: string
  title: string
  location: string
  inscription: string
  nextClue: string
}

interface LaughTaleQuestState {
  started: boolean
  discovered: LaughTaleClueCode[]
  updatedAt: string
}

const STORAGE_PREFIX = 'tsp_laugh_tale_quest'

const EMPTY_STATE: LaughTaleQuestState = {
  started: false,
  discovered: [],
  updatedAt: '',
}

export const LAUGH_TALE_CLUE_DETAILS: LaughTaleClue[] = [
  {
    code: 'ROAD_1',
    numeral: 'I',
    title: 'Перший роуд понегліф',
    location: 'Прапор капітана',
    inscription: 'Камінь не вказує на острів. У Новому Світі вже не має права бути одному, знайдіть свою команду..',
    nextClue: 'У Новому Світі один компас бреше. Три голки сперечаються, і тільки дурень обирає найпряму. Першу поверни туди, де всюди вода. Другу - за тим, хто пливе не конкуруючи. Третю назви островом, де звичайний шлях закінчується, але справжній курс лише починається.',
  },
  {
    code: 'ROAD_2',
    numeral: 'II',
    title: 'Другий роуд понегліф',
    location: 'Три голки',
    inscription: 'Напис говорить про золоте місто, але не про небо. Верхній двір був не хмарою. То була земля, яку море втратило.',
    nextClue: 'Не називай небо. Назви острів, що лишився знизу, в місці де зібрані ігри.',
  },
  {
    code: 'ROAD_3',
    numeral: 'III',
    title: 'Третій роуд понегліф',
    location: 'Втрачений острів',
    inscription: 'Камінь мовчить. На ньому лише одна тріщина у формі паперу життя.',
    nextClue: 'Згадай слова старого йонко, які розкололи світ перед самою тишею. Напиши їх у чаті, але не кидай у море.',
  },
  {
    code: 'ROAD_4',
    numeral: 'IV',
    title: 'Четвертий роуд понегліф',
    location: 'Папір життя',
    inscription: 'Чотири дороги зійшлися. Острів не на мапі, не в небі й не в морі. Він чекає там, де вперше відкрився логбук.',
    nextClue: 'Повернись до Лафтелю і проклади курс.',
  },
]

function normalizeState(raw: Partial<LaughTaleQuestState> | null | undefined): LaughTaleQuestState {
  const discovered = Array.isArray(raw?.discovered)
    ? raw.discovered.filter((code): code is LaughTaleClueCode => LAUGH_TALE_CLUES.includes(code as LaughTaleClueCode))
    : []

  return {
    started: Boolean(raw?.started),
    discovered: Array.from(new Set(discovered)),
    updatedAt: typeof raw?.updatedAt === 'string' ? raw.updatedAt : '',
  }
}

export const useLaughTaleQuestStore = defineStore('laughTaleQuest', () => {
  const auth = useAuthStore()
  const state = ref<LaughTaleQuestState>({ ...EMPTY_STATE })

  const storageKey = computed(() => {
    const userId = auth.user?.id
    return userId ? `${STORAGE_PREFIX}_${userId}` : `${STORAGE_PREFIX}_guest`
  })

  function load() {
    if (typeof localStorage === 'undefined') return

    try {
      const raw = localStorage.getItem(storageKey.value)
      state.value = raw ? normalizeState(JSON.parse(raw)) : { ...EMPTY_STATE }
    } catch {
      state.value = { ...EMPTY_STATE }
    }
  }

  function persist() {
    if (typeof localStorage === 'undefined') return

    try {
      localStorage.setItem(storageKey.value, JSON.stringify(state.value))
    } catch { }
  }

  function touch(nextState: LaughTaleQuestState) {
    state.value = {
      ...nextState,
      updatedAt: new Date().toISOString(),
    }
    persist()
  }

  function start() {
    if (state.value.started) return
    touch({
      ...state.value,
      started: true,
    })
  }

  function hasDiscovered(code: LaughTaleClueCode) {
    return state.value.discovered.includes(code)
  }

  function canDiscover(code: LaughTaleClueCode) {
    if (!state.value.started || hasDiscovered(code)) return false

    const index = LAUGH_TALE_CLUES.indexOf(code)
    if (index === -1) return false
    if (index === 0) return true

    const previousCode = LAUGH_TALE_CLUES[index - 1]
    return previousCode ? hasDiscovered(previousCode) : false
  }

  function discover(code: LaughTaleClueCode) {
    if (!canDiscover(code)) return false

    touch({
      ...state.value,
      discovered: [...state.value.discovered, code],
    })
    return true
  }

  function reset() {
    state.value = { ...EMPTY_STATE }
    if (typeof localStorage === 'undefined') return

    try {
      localStorage.removeItem(storageKey.value)
    } catch { }
  }

  const progressCount = computed(() => state.value.discovered.length)
  const totalCount = computed(() => LAUGH_TALE_CLUES.length)
  const progressPercent = computed(() => Math.round((progressCount.value / totalCount.value) * 100))
  const canClaim = computed(() => progressCount.value === totalCount.value)
  const currentClueCode = computed(() => LAUGH_TALE_CLUES.find((code) => !hasDiscovered(code)) ?? null)
  const currentClue = computed(() => LAUGH_TALE_CLUE_DETAILS.find((item) => item.code === currentClueCode.value) ?? null)
  const discoveredClues = computed(() =>
    LAUGH_TALE_CLUE_DETAILS.filter((item) => hasDiscovered(item.code)),
  )

  watch(storageKey, () => load(), { immediate: true })

  return {
    state,
    clues: LAUGH_TALE_CLUE_DETAILS,
    progressCount,
    totalCount,
    progressPercent,
    canClaim,
    currentClueCode,
    currentClue,
    discoveredClues,
    load,
    start,
    discover,
    hasDiscovered,
    canDiscover,
    reset,
  }
})
