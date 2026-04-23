<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { usePartyStore } from '../stores/parties'
import { useGameStore } from '../stores/games'
import { useAuthStore } from '../stores/auth'
import PartyCard from '../components/PartyCard.vue'
import CreatePartyModal from '../components/CreatePartyModal.vue'
import type { Party } from '../types'

const router = useRouter()
const route = useRoute()
const partyStore = usePartyStore()
const gameStore = useGameStore()
const auth = useAuthStore()

const modalOpen = ref(false)
const filtersOpen = ref(false)
let searchDebounce: ReturnType<typeof setTimeout> | null = null

function selectParty(party: Party) {
  router.push(`/party/${party.id}`)
}

function goToPage(page: number) {
  partyStore.fetchSearchParties(page)
}

function openCreateParty() {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: '/search-parties' } })
    return
  }
  modalOpen.value = true
}

function toggleFilters() {
  filtersOpen.value = !filtersOpen.value
}

const activeFiltersCount = ref(0)

function countActiveFilters() {
  let count = 0
  if (partyStore.filterGameId) count++
  if (partyStore.filterPlatform) count++
  if (partyStore.filterSkillLevel) count++
  if (partyStore.filterPlayStyle) count++
  if (partyStore.filterLanguage) count++
  if (partyStore.filterRegion) count++
  activeFiltersCount.value = count
}

onMounted(async () => {
  await gameStore.fetchGames()

  if (route.query.gameId) {
    partyStore.filterGameId = Number(route.query.gameId)
  }

  await partyStore.fetchSearchParties(0)
  countActiveFilters()
})

watch(
    [
      () => partyStore.filterGameId,
      () => partyStore.filterPlatform,
      () => partyStore.filterSkillLevel,
      () => partyStore.filterPlayStyle,
      () => partyStore.filterLanguage,
      () => partyStore.filterRegion,
    ],
    () => {
      partyStore.fetchSearchParties(0)
      countActiveFilters()
    },
)

watch(
    () => partyStore.search,
    () => {
      if (searchDebounce) clearTimeout(searchDebounce)
      searchDebounce = setTimeout(() => {
        partyStore.fetchSearchParties(0)
      }, 400)
    },
)
</script>

<template>
  <div class="search-page">
    <div class="search-container">

      <div class="section-head">
        <div class="section-title">
          ПОШУК ЛОБІ
          <span class="section-count">{{ partyStore.searchTotalElements }} знайдено</span>
        </div>
        <div class="section-head-actions">
          <router-link v-if="auth.isLoggedIn" to="/party-history" class="history-link-btn">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <polyline points="12 6 12 12 16 14"/>
            </svg>
            ІСТОРІЯ
          </router-link>
          <button class="create-party-btn" @click="openCreateParty">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <line x1="12" y1="5" x2="12" y2="19"/>
              <line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
            СТВОРИТИ ЛОБІ
          </button>
        </div>
      </div>

      <div class="filters-bar desktop-filters">
        <div class="filter-search-wrap">
          <svg class="filter-search-icon" width="14" height="14" viewBox="0 0 24 24" fill="none"
               stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input
              class="filter-search"
              v-model="partyStore.search"
              placeholder="Пошук по грі або опису..."
          />
        </div>

        <select class="filter-select" v-model="partyStore.filterGameId">
          <option :value="null">Всі ігри</option>
          <option v-for="game in gameStore.games" :key="game.id" :value="game.id">{{ game.name }}</option>
        </select>

        <select class="filter-select" v-model="partyStore.filterSkillLevel">
          <option value="">Будь-який рівень</option>
          <option value="BEGINNER">Початківець</option>
          <option value="INTERMEDIATE">Середній</option>
          <option value="ADVANCED">Просунутий</option>
          <option value="EXPERT">Експерт</option>
        </select>

        <select class="filter-select" v-model="partyStore.filterPlatform">
          <option value="">Всі платформи</option>
          <option value="PC">PC</option>
          <option value="PLAYSTATION">PlayStation</option>
          <option value="XBOX">Xbox</option>
          <option value="NINTENDO">Nintendo</option>
          <option value="MOBILE">Mobile</option>
        </select>

        <select class="filter-select" v-model="partyStore.filterPlayStyle">
          <option value="">Будь-який стиль</option>
          <option value="CASUAL">Казуальний</option>
          <option value="SEMI_COMPETITIVE">Напів-змагальний</option>
          <option value="COMPETITIVE">Змагальний</option>
        </select>

        <select class="filter-select" v-model="partyStore.filterRegion">
          <option value="">Всі регіони</option>
          <option value="EUROPE">Європа</option>
          <option value="NORTH_AMERICA">Пн. Америка</option>
          <option value="SOUTH_AMERICA">Пд. Америка</option>
          <option value="ASIA">Азія</option>
          <option value="MIDDLE_EAST">Близький Схід</option>
          <option value="AFRICA">Африка</option>
          <option value="OCEANIA">Океанія</option>
        </select>

        <select class="filter-select" v-model="partyStore.filterLanguage">
          <option value="">Всі мови</option>
          <option value="UA">Українська</option>
          <option value="EN">English</option>
          <option value="PL">Polski</option>
          <option value="DE">Deutsch</option>
          <option value="FR">Français</option>
          <option value="ES">Español</option>
          <option value="PT">Português</option>
          <option value="TR">Türkçe</option>
          <option value="KO">한국어</option>
          <option value="ZH">中文</option>
          <option value="JA">日本語</option>
          <option value="IT">Italiano</option>
          <option value="NL">Nederlands</option>
          <option value="SV">Svenska</option>
          <option value="NO">Norsk</option>
          <option value="DA">Dansk</option>
          <option value="FI">Suomi</option>
          <option value="CS">Čeština</option>
          <option value="SK">Slovenčina</option>
          <option value="HU">Magyar</option>
          <option value="RO">Română</option>
          <option value="BG">Български</option>
          <option value="HR">Hrvatski</option>
          <option value="SR">Srpski</option>
          <option value="AR">العربية</option>
          <option value="HI">हिन्दी</option>
          <option value="VI">Tiếng Việt</option>
          <option value="TH">ภาษาไทย</option>
          <option value="ID">Bahasa Indonesia</option>
        </select>
      </div>

      <div class="mobile-filters-bar">

        <div class="mobile-top-row">
          <div class="filter-search-wrap">
            <svg class="filter-search-icon" width="14" height="14" viewBox="0 0 24 24" fill="none"
                 stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
            <input
                class="filter-search"
                v-model="partyStore.search"
                placeholder="Пошук по грі або опису..."
            />
          </div>

          <button
              class="filter-toggle-btn"
              :class="{ active: filtersOpen, 'has-filters': activeFiltersCount > 0 }"
              @click="toggleFilters"
              :title="filtersOpen ? 'Закрити фільтри' : 'Відкрити фільтри'"
          >
            <svg
                class="filter-icon-svg"
                width="22"
                height="22"
                viewBox="0 0 22 22"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
            >
              <line x1="2" y1="5" x2="20" y2="5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
              <circle cx="7" cy="5" r="2.5" fill="var(--black)" stroke="currentColor" stroke-width="1.8"/>

              <line x1="2" y1="11" x2="20" y2="11" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
              <circle cx="14" cy="11" r="2.5" fill="var(--black)" stroke="currentColor" stroke-width="1.8"/>

              <line x1="2" y1="17" x2="20" y2="17" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
              <circle cx="9" cy="17" r="2.5" fill="var(--black)" stroke="currentColor" stroke-width="1.8"/>
            </svg>

            <span v-if="activeFiltersCount > 0" class="filter-badge">{{ activeFiltersCount }}</span>
          </button>
        </div>

        <div class="mobile-filters-panel" :class="{ open: filtersOpen }">
          <div class="mobile-filters-inner">
            <div class="mobile-filter-label">ГРА</div>
            <select class="filter-select" v-model="partyStore.filterGameId">
              <option :value="null">Всі ігри</option>
              <option v-for="game in gameStore.games" :key="game.id" :value="game.id">{{ game.name }}</option>
            </select>

            <div class="mobile-filter-label">РІВЕНЬ</div>
            <select class="filter-select" v-model="partyStore.filterSkillLevel">
              <option value="">Будь-який рівень</option>
              <option value="BEGINNER">Початківець</option>
              <option value="INTERMEDIATE">Середній</option>
              <option value="ADVANCED">Просунутий</option>
              <option value="EXPERT">Експерт</option>
            </select>

            <div class="mobile-filter-label">ПЛАТФОРМА</div>
            <select class="filter-select" v-model="partyStore.filterPlatform">
              <option value="">Всі платформи</option>
              <option value="PC">PC</option>
              <option value="PLAYSTATION">PlayStation</option>
              <option value="XBOX">Xbox</option>
              <option value="NINTENDO">Nintendo</option>
              <option value="MOBILE">Mobile</option>
            </select>

            <div class="mobile-filter-label">СТИЛЬ ГРИ</div>
            <select class="filter-select" v-model="partyStore.filterPlayStyle">
              <option value="">Будь-який стиль</option>
              <option value="CASUAL">Казуальний</option>
              <option value="SEMI_COMPETITIVE">Напів-змагальний</option>
              <option value="COMPETITIVE">Змагальний</option>
            </select>

            <div class="mobile-filter-label">РЕГІОН</div>
            <select class="filter-select" v-model="partyStore.filterRegion">
              <option value="">Всі регіони</option>
              <option value="EUROPE">Європа</option>
              <option value="NORTH_AMERICA">Пн. Америка</option>
              <option value="SOUTH_AMERICA">Пд. Америка</option>
              <option value="ASIA">Азія</option>
              <option value="MIDDLE_EAST">Близький Схід</option>
              <option value="AFRICA">Африка</option>
              <option value="OCEANIA">Океанія</option>
            </select>

            <div class="mobile-filter-label">МОВА</div>
            <select class="filter-select" v-model="partyStore.filterLanguage">
              <option value="">Всі мови</option>
              <option value="UA">Українська</option>
              <option value="EN">English</option>
              <option value="PL">Polski</option>
              <option value="DE">Deutsch</option>
              <option value="FR">Français</option>
              <option value="ES">Español</option>
              <option value="PT">Português</option>
              <option value="TR">Türkçe</option>
              <option value="KO">한국어</option>
              <option value="ZH">中文</option>
              <option value="JA">日本語</option>
              <option value="IT">Italiano</option>
              <option value="NL">Nederlands</option>
              <option value="SV">Svenska</option>
              <option value="NO">Norsk</option>
              <option value="DA">Dansk</option>
              <option value="FI">Suomi</option>
              <option value="CS">Čeština</option>
              <option value="SK">Slovenčina</option>
              <option value="HU">Magyar</option>
              <option value="RO">Română</option>
              <option value="BG">Български</option>
              <option value="HR">Hrvatski</option>
              <option value="SR">Srpski</option>
              <option value="AR">العربية</option>
              <option value="HI">हिन्दी</option>
              <option value="VI">Tiếng Việt</option>
              <option value="TH">ภาษาไทย</option>
              <option value="ID">Bahasa Indonesia</option>
            </select>

            <button
                v-if="activeFiltersCount > 0"
                class="reset-filters-btn"
                @click="() => {
                partyStore.filterGameId = null
                partyStore.filterPlatform = ''
                partyStore.filterSkillLevel = ''
                partyStore.filterPlayStyle = ''
                partyStore.filterLanguage = ''
                partyStore.filterRegion = ''
              }"
            >
              СКИНУТИ ФІЛЬТРИ
            </button>
          </div>
        </div>
      </div>

      <div v-if="partyStore.searchLoading" class="empty-state">
        <div class="empty-state-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
        </div>
        <div class="empty-state-title">ЗАВАНТАЖЕННЯ...</div>
      </div>

      <div v-else-if="partyStore.searchParties.length === 0" class="empty-state">
        <div class="empty-state-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <rect x="2" y="6" width="20" height="12" rx="2"/>
            <line x1="6" y1="12" x2="10" y2="12"/>
            <line x1="8" y1="10" x2="8" y2="14"/>
            <circle cx="16" cy="10" r="1" fill="currentColor" stroke="none"/>
            <circle cx="18" cy="13" r="1" fill="currentColor" stroke="none"/>
          </svg>
        </div>
        <div class="empty-state-title">ЛОБІ НЕ ЗНАЙДЕНО</div>
        <div class="empty-state-sub">Спробуй змінити фільтри або створи власне лобі</div>
      </div>

      <template v-else>
        <div class="party-grid">
          <PartyCard
              v-for="party in partyStore.searchParties"
              :key="party.id"
              :party="party"
              @select="selectParty"
          />
        </div>

        <div v-if="partyStore.searchTotalPages > 1" class="pagination">
          <button
              class="page-btn"
              :disabled="partyStore.searchPage === 0"
              @click="goToPage(partyStore.searchPage - 1)"
          >
            ← НАЗАД
          </button>

          <button
              v-for="p in partyStore.searchTotalPages"
              :key="p"
              class="page-btn"
              :class="{ active: partyStore.searchPage === p - 1 }"
              @click="goToPage(p - 1)"
          >
            {{ p }}
          </button>

          <button
              class="page-btn"
              :disabled="partyStore.searchPage >= partyStore.searchTotalPages - 1"
              @click="goToPage(partyStore.searchPage + 1)"
          >
            ДАЛІ →
          </button>
        </div>
      </template>
    </div>
  </div>

  <CreatePartyModal :visible="modalOpen" @close="modalOpen = false" />
</template>

<style scoped>


.search-page {
  padding-top: 64px;
  min-height: 100vh;
  background: var(--black);
}

.search-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 64px 80px;
}

.section-head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.history-link-btn {
  font-family: var(--font-display), sans-serif;
  font-size: 16px;
  letter-spacing: 3px;
  padding: 12px 28px;
  border: 2px solid var(--border);
  background: transparent;
  color: var(--gray-light);
  text-decoration: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transition: all 0.15s;
  flex-shrink: 0;
}
.history-link-btn svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}
.history-link-btn:hover {
  border-color: var(--yellow-dim);
  color: var(--yellow);
}

.desktop-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 28px;
}

.mobile-filters-bar {
  display: none;
}

.filter-search-wrap {
  position: relative;
  flex: 1;
  min-width: 200px;
  display: flex;
  align-items: center;
}

.filter-search-icon {
  position: absolute;
  left: 12px;
  color: var(--gray);
  pointer-events: none;
  flex-shrink: 0;
}

.filter-search {
  width: 100%;
  padding: 10px 14px 10px 36px;
  background: var(--panel);
  border: 1px solid var(--border);
  color: var(--white);
  font-family: var(--font-body), sans-serif;
  font-size: 0.95rem;
  outline: none;
  transition: border-color 0.15s;
}
.filter-search:focus {
  border-color: var(--yellow-dim);
}
.filter-search::placeholder {
  color: var(--gray);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-state-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  color: var(--gray);
}

.empty-state-title {
  font-family: var(--font-display), sans-serif;
  font-size: 26px;
  letter-spacing: 3px;
  color: var(--yellow);
  margin-bottom: 10px;
}

.empty-state-sub {
  color: var(--gray);
  font-size: 14px;
  letter-spacing: 0.5px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 40px;
  padding-top: 28px;
  border-top: 2px solid var(--border);
}

.page-btn {
  font-family: var(--font-display), sans-serif;
  font-size: 14px;
  letter-spacing: 2px;
  padding: 8px 16px;
  background: var(--panel);
  border: 2px solid var(--border);
  color: var(--gray-light);
  cursor: pointer;
  transition: all 0.15s;
}
.page-btn:hover:not(:disabled) {
  border-color: var(--yellow-dim);
  color: var(--yellow);
  background: var(--yellow-glow);
}
.page-btn.active {
  border-color: var(--yellow);
  color: var(--black);
  background: var(--yellow);
}
.page-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .search-container {
    padding: 24px 20px 60px;
  }

  .desktop-filters {
    display: none;
  }

  .mobile-filters-bar {
    display: flex;
    flex-direction: column;
    gap: 0;
    margin-bottom: 20px;
  }

  .mobile-top-row {
    display: flex;
    align-items: stretch;
    gap: 10px;
    margin-bottom: 0;
  }

  .mobile-top-row .filter-search-wrap {
    flex: 1;
    min-width: 0;
  }

  .mobile-top-row .filter-search {
    min-height: 46px;
    font-size: 15px;
  }

  .filter-toggle-btn {
    position: relative;
    width: 46px;
    height: 46px;
    flex-shrink: 0;
    background: var(--panel);
    border: 1px solid var(--border);
    color: var(--gray-light);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: border-color 0.15s, color 0.15s, background 0.15s;
  }
  .filter-toggle-btn:hover {
    border-color: var(--yellow-dim);
    color: var(--yellow);
  }
  .filter-toggle-btn.active {
    border-color: var(--yellow);
    color: var(--yellow);
    background: var(--yellow-glow, rgba(255,200,0,0.08));
  }
  .filter-toggle-btn.has-filters {
    border-color: var(--yellow-dim);
    color: var(--yellow-dim);
  }

  .filter-icon-svg {
    transition: transform 0.25s;
  }
  .filter-toggle-btn.active .filter-icon-svg {
    transform: rotate(90deg);
  }

  .filter-badge {
    position: absolute;
    top: 4px;
    right: 4px;
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    background: var(--yellow);
    color: var(--black);
    font-family: var(--font-body), sans-serif;
    font-size: 10px;
    font-weight: 700;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    line-height: 1;
    letter-spacing: 0;
    box-sizing: border-box;
    pointer-events: none;
  }

  .mobile-filters-panel {
    overflow: hidden;
    max-height: 0;
    transition: max-height 0.35s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.25s ease,
    margin-top 0.25s ease;
    opacity: 0;
    margin-top: 0;
  }
  .mobile-filters-panel.open {
    max-height: 900px;
    opacity: 1;
    margin-top: 10px;
  }

  .mobile-filters-inner {
    display: flex;
    flex-direction: column;
    gap: 6px;
    background: var(--panel);
    border: 1px solid var(--border);
    padding: 16px;
  }

  .mobile-filter-label {
    font-family: var(--font-display), sans-serif;
    font-size: 10px;
    letter-spacing: 2px;
    color: var(--gray);
    margin-top: 6px;
    margin-bottom: 2px;
  }
  .mobile-filter-label:first-child {
    margin-top: 0;
  }

  .mobile-filters-inner .filter-select {
    width: 100%;
    min-height: 44px;
    font-size: 14px;
  }

  .reset-filters-btn {
    margin-top: 10px;
    padding: 10px 0;
    background: transparent;
    border: 1px solid var(--border);
    color: var(--gray);
    font-family: var(--font-display), sans-serif;
    font-size: 12px;
    letter-spacing: 2px;
    cursor: pointer;
    transition: border-color 0.15s, color 0.15s;
    text-align: center;
  }
  .reset-filters-btn:hover {
    border-color: var(--yellow-dim);
    color: var(--yellow);
  }

  .section-head {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    margin-bottom: 16px;
    padding-bottom: 14px;
  }

  .section-head-actions {
    flex-direction: column;
    gap: 8px;
  }

  .history-link-btn,
  .create-party-btn {
    width: 100%;
    justify-content: center;
    font-size: 14px;
    letter-spacing: 2px;
    padding: 12px 14px;
  }

  .pagination {
    flex-wrap: wrap;
    margin-top: 28px;
    padding-top: 20px;
    gap: 8px;
  }

  .page-btn {
    min-height: 40px;
    padding: 8px 12px;
    font-size: 13px;
    letter-spacing: 1.5px;
  }

  .empty-state {
    padding: 60px 16px;
  }

  .empty-state-title {
    font-size: 22px;
    letter-spacing: 2px;
  }
}

@media (max-width: 480px) {
  .search-container {
    padding: 18px 14px 52px;
  }

  .mobile-filters-inner {
    padding: 12px;
  }

  .mobile-filter-label {
    font-size: 9px;
    letter-spacing: 1.5px;
  }

  .mobile-filters-inner .filter-select {
    min-height: 42px;
    font-size: 13px;
  }

  .filter-toggle-btn {
    width: 46px;
  }

  .section-title {
    font-size: 24px;
    letter-spacing: 2px;
  }

  .section-count {
    font-size: 11px;
    letter-spacing: 1px;
  }

  .empty-state-title {
    font-size: 20px;
  }

  .page-btn {
    min-width: 36px;
    padding: 8px 10px;
    font-size: 12px;
    letter-spacing: 1px;
  }
}
</style>