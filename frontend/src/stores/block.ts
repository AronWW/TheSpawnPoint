import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../api/axios'
import type { BlockedUser } from '../types'

export const useBlockStore = defineStore('blocks', () => {
  const blockedUsers = ref<BlockedUser[]>([])
  const loading = ref(false)

  const blockedIds = computed(() => blockedUsers.value.map(b => b.userId))

  function isBlockedByMe(userId: number | undefined): boolean {
    if (!userId) return false
    return blockedIds.value.includes(userId)
  }

  async function fetchBlockedUsers() {
    loading.value = true
    try {
      const { data } = await api.get<BlockedUser[]>('/blocks')
      blockedUsers.value = data
    } catch {
      blockedUsers.value = []
    } finally {
      loading.value = false
    }
  }

  async function blockUser(userId: number) {
    await api.post(`/blocks/${userId}`)
    await fetchBlockedUsers()
  }

  async function unblockUser(userId: number) {
    await api.delete(`/blocks/${userId}`)
    blockedUsers.value = blockedUsers.value.filter(b => b.userId !== userId)
  }

  return {
    blockedUsers,
    loading,
    blockedIds,
    isBlockedByMe,
    fetchBlockedUsers,
    blockUser,
    unblockUser,
  }
})

