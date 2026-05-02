<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useChatStore } from '../stores/chat'
import ChatSidebar from '../components/ChatSidebar.vue'
import ChatWindow from '../components/ChatWindow.vue'
import CreateGroupChatModal from '../components/CreateGroupChatModal.vue'
import type { ChatItem } from '../types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const chatStore = useChatStore()

const searchQuery = ref('')
const mobileView = ref<'sidebar' | 'chat'>('sidebar')
const showCreateGroupChat = ref(false)
const sidebarCollapsed = ref(false)

function resetPageScroll() {
  window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
  document.documentElement.scrollTop = 0
  document.body.scrollTop = 0
}

onMounted(async () => {
  resetPageScroll()
  document.body.classList.add('chat-page-lock')
  requestAnimationFrame(resetPageScroll)

  if (!auth.isLoggedIn) {
    router.push('/login')
    return
  }

  await chatStore.fetchChats()

  const groupId = route.query.groupId
  if (groupId) {
    await chatStore.openGroupChatById(Number(groupId))
    mobileView.value = 'chat'
  }
})

onBeforeUnmount(() => {
  document.body.classList.remove('chat-page-lock')
})

function onSelectChat(chat: ChatItem) {
  chatStore.openChat(chat)
  mobileView.value = 'chat'
}

function goBackToSidebar() {
  mobileView.value = 'sidebar'
}

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>

<template>
  <div class="chat-page">
    <div class="chat-layout" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
      <div
          class="chat-sidebar-panel ink-panel"
          :class="{ 'mobile-hidden': mobileView === 'chat', collapsed: sidebarCollapsed }"
      >
        <div class="chat-search-wrapper">
          <input
              v-model="searchQuery"
              type="text"
              class="chat-search"
              placeholder="Пошук чатів..."
          />
          <button class="create-group-btn" @click="showCreateGroupChat = true" title="Створити груповий чат">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
              <line x1="19" y1="8" x2="19" y2="14"/><line x1="16" y1="11" x2="22" y2="11"/>
            </svg>
          </button>
        </div>
        <ChatSidebar :search="searchQuery" :collapsed="sidebarCollapsed" @select="onSelectChat" />
        <button
          type="button"
          class="sidebar-collapse-btn"
          :title="sidebarCollapsed ? 'Розгорнути список чатів' : 'Згорнути список чатів'"
          @click="toggleSidebar"
        >
          <svg v-if="!sidebarCollapsed" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3.5" y="4" width="17" height="16" rx="2" />
            <path d="M9 4v16" />
            <path d="M6.5 8h1" />
            <path d="M6.5 11h1" />
            <path d="M12.5 8h4.5" />
            <path d="M12.5 11h4.5" />
          </svg>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3.5" y="4" width="17" height="16" rx="2" />
            <path d="M15 4v16" />
            <path d="M6.5 8h5" />
            <path d="M6.5 11h5" />
            <path d="M17.5 8h1" />
            <path d="M17.5 11h1" />
          </svg>
        </button>
      </div>

      <div
          class="chat-main-panel ink-panel"
          :class="{ 'mobile-hidden': mobileView === 'sidebar' }"
      >
        <button class="mobile-back-btn" @click="goBackToSidebar">
          ← Всі чати
        </button>
        <ChatWindow />
      </div>
    </div>

    <CreateGroupChatModal
      :visible="showCreateGroupChat"
      @close="showCreateGroupChat = false"
      @created="mobileView = 'chat'"
    />
  </div>
</template>

<style scoped>
.chat-page {
  padding-top: 64px;
  height: 100dvh;
  overflow: hidden;
  background: var(--black);
}

.chat-layout {
  display: grid;
  grid-template-columns: 360px 1fr;
  height: calc(100dvh - 64px);
  max-width: 1440px;
  margin: 0 auto;
  border: 2px solid var(--border);
  border-top: none;
  box-shadow: 0 0 60px rgba(0,0,0,0.4);
  transition: grid-template-columns 0.2s ease;
}

.chat-layout.sidebar-collapsed {
  grid-template-columns: 56px 1fr;
}

.chat-sidebar-panel {
  display: flex;
  flex-direction: column;
  border-right: 2px solid var(--border);
  overflow: hidden;
  background: var(--panel);
  position: relative;
  min-width: 0;
}
.chat-sidebar-panel.ink-panel:hover,
.chat-main-panel.ink-panel:hover {
  border-color: var(--border);
  box-shadow: none;
}
.chat-sidebar-panel.ink-panel::after,
.chat-main-panel.ink-panel::after {
  display: none;
}

.chat-sidebar-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle, rgba(245,197,24,0.012) 1px, transparent 1px);
  background-size: 6px 6px;
  pointer-events: none;
  z-index: 0;
}

.chat-search-wrapper {
  padding: 16px 16px 12px;
  position: relative;
  z-index: 1;
  display: flex;
  gap: 8px;
  align-items: center;
}

.chat-sidebar-panel :deep(.chat-sidebar) {
  flex: 1;
  min-height: 0;
  padding-bottom: 62px;
}

.chat-sidebar-panel.collapsed .chat-search-wrapper {
  display: none;
}

.chat-sidebar-panel.collapsed :deep(.chat-sidebar) {
  display: flex;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 10px 0 62px;
}

.chat-sidebar-panel.collapsed :deep(.chat-sidebar-header),
.chat-sidebar-panel.collapsed :deep(.chat-section-label),
.chat-sidebar-panel.collapsed :deep(.chat-sidebar-info),
.chat-sidebar-panel.collapsed :deep(.chat-sidebar-meta),
.chat-sidebar-panel.collapsed :deep(.chat-archive-toggle),
.chat-sidebar-panel.collapsed :deep(.chat-sidebar-loading),
.chat-sidebar-panel.collapsed :deep(.chat-sidebar-empty) {
  display: none;
}

.chat-sidebar-panel.collapsed :deep(.chat-sidebar-item) {
  justify-content: center;
  gap: 0;
  padding: 9px 0;
  border-left-width: 0;
  border-bottom: 0;
  background: transparent;
}

.chat-sidebar-panel.collapsed :deep(.chat-sidebar-item.active) {
  background: rgba(245,197,24,0.08);
  box-shadow: inset 3px 0 0 var(--yellow);
}

.chat-sidebar-panel.collapsed :deep(.chat-sidebar-item.pinned-item::before) {
  content: '';
  position: absolute;
  top: 7px;
  left: 9px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--yellow);
  box-shadow: 0 0 8px rgba(245,197,24,0.35);
}

.chat-sidebar-panel.collapsed :deep(.chat-sidebar-item.archived-item) {
  opacity: 0.55;
  filter: grayscale(0.45);
}

.chat-sidebar-panel.collapsed :deep(.chat-sidebar-item.archived-item .chat-sidebar-avatar) {
  outline: 1px dashed rgba(255,255,255,0.24);
  outline-offset: 3px;
}

.chat-sidebar-panel.collapsed :deep(.chat-sidebar-item.unread::after) {
  left: auto;
  right: 8px;
  top: 9px;
  bottom: auto;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.chat-sidebar-panel.collapsed :deep(.chat-sidebar-avatar),
.chat-sidebar-panel.collapsed :deep(.chat-avatar-letter),
.chat-sidebar-panel.collapsed :deep(.chat-avatar-img) {
  width: 34px;
  height: 34px;
}

.chat-sidebar-panel.collapsed :deep(.chat-avatar-letter) {
  font-size: 15px;
}

.chat-sidebar-panel.collapsed :deep(.chat-status-dot) {
  width: 11px;
  height: 11px;
  right: -3px;
  bottom: -3px;
}

.chat-sidebar-panel.collapsed :deep(.chat-status-dot svg) {
  width: 7px;
  height: 7px;
}

.sidebar-collapse-btn {
  position: absolute;
  right: 14px;
  bottom: 14px;
  z-index: 2;
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border: 2px solid var(--border);
  background: var(--dark);
  color: var(--gray-light);
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s, background 0.15s, box-shadow 0.15s;
}

.sidebar-collapse-btn:hover {
  border-color: var(--yellow-dim);
  color: var(--yellow);
  background: var(--panel-light);
  box-shadow: 0 0 12px rgba(245,197,24,0.1);
}

.chat-sidebar-panel.collapsed .sidebar-collapse-btn {
  right: 8px;
}

.create-group-btn {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: none;
  border: 2px solid var(--border);
  color: var(--gray);
  cursor: pointer;
  transition: all 0.2s;
}
.create-group-btn:hover {
  border-color: var(--yellow-dim);
  color: var(--yellow);
  box-shadow: 0 0 12px rgba(245,197,24,0.1);
}

.chat-search {
  width: 100%;
  background: var(--dark);
  border: 2px solid var(--border);
  border-left: 3px solid transparent;
  padding: 11px 14px;
  font-size: 13px;
  color: var(--white);
  font-family: var(--font-body);
  letter-spacing: 0.5px;
  outline: none;
  transition: all 0.2s;
}
.chat-search:focus {
  border-color: var(--yellow-dim);
  border-left-color: var(--yellow);
  box-shadow: 0 0 12px rgba(245,197,24,0.08);
}
.chat-search::placeholder {
  color: var(--gray);
  font-style: italic;
}

.chat-main-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--panel);
}

.mobile-back-btn {
  display: none;
}

@media (max-width: 768px) {
  .chat-layout {
    grid-template-columns: 1fr;
    grid-template-rows: 1fr;
    border: none;
  }

  .chat-layout.sidebar-collapsed {
    grid-template-columns: 1fr;
  }

  .chat-sidebar-panel,
  .chat-main-panel {
    grid-column: 1;
    grid-row: 1;
  }

  .chat-sidebar-panel.collapsed .chat-search-wrapper,
  .chat-sidebar-panel.collapsed :deep(.chat-sidebar) {
    display: flex;
  }

  .chat-sidebar-panel.collapsed :deep(.chat-sidebar-header) {
    display: flex;
  }

  .chat-sidebar-panel.collapsed :deep(.chat-section-label),
  .chat-sidebar-panel.collapsed :deep(.chat-sidebar-info),
  .chat-sidebar-panel.collapsed :deep(.chat-sidebar-loading),
  .chat-sidebar-panel.collapsed :deep(.chat-sidebar-empty) {
    display: block;
  }

  .chat-sidebar-panel.collapsed :deep(.chat-sidebar-meta),
  .chat-sidebar-panel.collapsed :deep(.chat-archive-toggle) {
    display: flex;
  }

  .chat-sidebar-panel.collapsed :deep(.chat-sidebar) {
    padding: 0;
  }

  .chat-sidebar-panel.collapsed :deep(.chat-sidebar-item) {
    justify-content: flex-start;
    gap: 12px;
    padding: 14px 20px;
    border-left-width: 3px;
    border-bottom: 1px solid var(--border);
  }

  .chat-sidebar-panel.collapsed :deep(.chat-sidebar-avatar),
  .chat-sidebar-panel.collapsed :deep(.chat-avatar-letter),
  .chat-sidebar-panel.collapsed :deep(.chat-avatar-img) {
    width: 42px;
    height: 42px;
  }

  .chat-sidebar-panel.collapsed :deep(.chat-avatar-letter) {
    font-size: 18px;
  }

  .chat-sidebar-panel.collapsed :deep(.chat-status-dot) {
    width: 13px;
    height: 13px;
    right: -2px;
    bottom: -2px;
  }

  .chat-sidebar-panel.collapsed :deep(.chat-sidebar-item.unread::after) {
    left: 0;
    top: 0;
    bottom: 0;
    right: auto;
    width: 3px;
    height: auto;
    border-radius: 0;
  }

  .sidebar-collapse-btn {
    display: none;
  }

  .mobile-hidden {
    display: none;
  }

  .mobile-back-btn {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    background: var(--panel);
    border: none;
    border-bottom: 2px solid var(--border);
    color: var(--yellow);
    font-family: var(--font-display);
    font-size: 13px;
    font-weight: 400;
    letter-spacing: 2px;
    cursor: pointer;
    flex-shrink: 0;
    transition: all 0.15s;
    position: relative;
  }
  .mobile-back-btn::after {
    content: '';
    position: absolute;
    bottom: -3px;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, var(--yellow-dim), transparent);
    opacity: 0.3;
  }
  .mobile-back-btn:hover {
    background: rgba(245,197,24,0.04);
  }
}
</style>
