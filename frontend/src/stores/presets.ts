import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api/axios'

interface PartyPresetType {
  id: number
  name: string
  slotIndex: number
  gameId: number
  gameName: string
  gameImageUrl: string | null
  maxMembers: number
  platform: string[]
  languages: string[]
  skillLevel: string | null
  playStyle: string | null
  tags: string[]
  region: string | null
}

interface SavePartyPresetRequestType {
  name: string
  slotIndex: number
  gameId: number
  maxMembers: number
  platform: string[]
  languages: string[]
  skillLevel: string | null
  playStyle: string | null
  tags: string[]
  region: string | null
}

export const usePresetStore = defineStore('presets', () => {
  const presets = ref<PartyPresetType[]>([])
  const loading = ref(false)

  async function fetchPresets() {
    loading.value = true
    try {
      const { data } = await api.get<PartyPresetType[]>('/party-presets')
      presets.value = data
    } catch {
      presets.value = []
    } finally {
      loading.value = false
    }
  }

  async function savePreset(dto: SavePartyPresetRequestType): Promise<PartyPresetType> {
    const { data } = await api.post<PartyPresetType>('/party-presets', dto)
    const idx = presets.value.findIndex(p => p.slotIndex === data.slotIndex)
    if (idx !== -1) {
      presets.value[idx] = data
    } else {
      presets.value.push(data)
      presets.value.sort((a, b) => a.slotIndex - b.slotIndex)
    }
    return data
  }

  async function deletePreset(presetId: number) {
    await api.delete(`/party-presets/${presetId}`)
    presets.value = presets.value.filter(p => p.id !== presetId)
  }

  function getPresetBySlot(slotIndex: number): PartyPresetType | undefined {
    return presets.value.find(p => p.slotIndex === slotIndex)
  }

  return {
    presets,
    loading,
    fetchPresets,
    savePreset,
    deletePreset,
    getPresetBySlot,
  }
})


