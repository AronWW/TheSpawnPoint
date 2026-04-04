import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api/axios'
import type { UserRating, IndividualRating } from '../types'

export const useRatingStore = defineStore('rating', () => {
  const ratingsCache = ref<Map<number, UserRating>>(new Map())
  const myRating = ref<UserRating | null>(null)

  async function submitRatings(partyId: number, ratings: IndividualRating[]): Promise<void> {
    await api.post('/ratings', { partyId, ratings })
  }

  async function fetchUserRating(userId: number): Promise<UserRating> {
    const { data } = await api.get<UserRating>(`/ratings/user/${userId}`)
    ratingsCache.value.set(userId, data)
    return data
  }

  async function fetchBatchRatings(userIds: number[]): Promise<Map<number, UserRating>> {
    if (userIds.length === 0) return new Map()
    const { data } = await api.get<Record<string, UserRating>>('/ratings/users', {
      params: { ids: userIds.join(',') },
    })
    const result = new Map<number, UserRating>()
    for (const [key, value] of Object.entries(data)) {
      const id = Number(key)
      ratingsCache.value.set(id, value)
      result.set(id, value)
    }
    return result
  }

  async function fetchMyRating(userId: number): Promise<void> {
    const { data } = await api.get<UserRating>(`/ratings/user/${userId}`)
    myRating.value = data
    ratingsCache.value.set(userId, data)
  }

  async function canRateParty(partyId: number): Promise<boolean> {
    try {
      const { data } = await api.get<{ canRate: boolean }>(`/ratings/can-rate/${partyId}`)
      return data.canRate
    } catch {
      return false
    }
  }

  function getCachedRating(userId: number): UserRating | null {
    return ratingsCache.value.get(userId) ?? null
  }

  return {
    ratingsCache,
    myRating,
    submitRatings,
    fetchUserRating,
    fetchBatchRatings,
    fetchMyRating,
    canRateParty,
    getCachedRating,
  }
})

