import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api/axios'
import type { Achievement, AchievementPreview, AchievementUnlockEvent } from '../types'

export const useAchievementStore = defineStore('achievements', () => {
  const myAchievements = ref<Achievement[]>([])
  const profilePreview = ref<AchievementPreview | null>(null)
  const loading = ref(false)
  const previewLoading = ref(false)

  const queue = ref<AchievementUnlockEvent[]>([])
  const activePopup = ref<AchievementUnlockEvent | null>(null)
  let popupTimer: number | null = null

  async function fetchMyAchievements() {
    loading.value = true
    try {
      const { data } = await api.get<Achievement[]>('/achievements/me')
      myAchievements.value = data
    } finally {
      loading.value = false
    }
  }

  async function fetchPreviewForUser(userId: number) {
    previewLoading.value = true
    try {
      const { data } = await api.get<AchievementPreview>(`/achievements/users/${userId}/preview`)
      profilePreview.value = data
    } finally {
      previewLoading.value = false
    }
  }

  function clearProfilePreview() {
    profilePreview.value = null
  }

  async function claimSecret(code: string) {
    const { data } = await api.post<Achievement>('/achievements/claim-secret', { code })
    return data
  }

  function promoteNext() {
    if (activePopup.value || queue.value.length === 0) return

    activePopup.value = queue.value.shift() ?? null
    if (!activePopup.value) return

    popupTimer = window.setTimeout(() => {
      dismissActivePopup()
    }, 4200)
  }

  function enqueueUnlock(event: AchievementUnlockEvent) {
    const duplicateActive = activePopup.value?.code === event.code && activePopup.value?.unlockedAt === event.unlockedAt
    const duplicateQueued = queue.value.some((item) => item.code === event.code && item.unlockedAt === event.unlockedAt)
    if (duplicateActive || duplicateQueued) return

    queue.value.push(event)
    promoteNext()
  }

  function dismissActivePopup() {
    if (popupTimer !== null) {
      window.clearTimeout(popupTimer)
      popupTimer = null
    }
    activePopup.value = null

    if (queue.value.length > 0) {
      window.setTimeout(() => promoteNext(), 120)
    }
  }

  function resetState() {
    myAchievements.value = []
    profilePreview.value = null
    queue.value = []
    activePopup.value = null
    if (popupTimer !== null) {
      window.clearTimeout(popupTimer)
      popupTimer = null
    }
  }

  return {
    myAchievements,
    profilePreview,
    loading,
    previewLoading,
    activePopup,
    fetchMyAchievements,
    fetchPreviewForUser,
    clearProfilePreview,
    claimSecret,
    enqueueUnlock,
    dismissActivePopup,
    resetState,
  }
})
