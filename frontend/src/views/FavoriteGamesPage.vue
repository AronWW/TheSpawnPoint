<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useGameStore } from '../stores/games'
import { useAuthStore } from '../stores/auth'
import { genreColor } from '../utils/helpers'

const router = useRouter()
const gameStore = useGameStore()
const auth = useAuthStore()

const search = ref('')

const animatingHearts = ref<Set<number>>(new Set())

const filteredFavorites = computed(() => {
  if (!search.value.trim()) return gameStore.favoriteGames
  const q = search.value.trim().toLowerCase()
  return gameStore.favoriteGames.filter(
    (g) =>
      g.name.toLowerCase().includes(q) ||
      (g.genre && g.genre.toLowerCase().includes(q))
  )
})

function goToGame(gameId: number) {
  router.push({ path: '/search-parties', query: { gameId: String(gameId) } })
}

async function handleRemoveFavorite(gameId: number) {
  animatingHearts.value = new Set([...animatingHearts.value, gameId])
  setTimeout(() => {
    const s = new Set(animatingHearts.value)
    s.delete(gameId)
    animatingHearts.value = s
  }, 400)
  await gameStore.removeFavorite(gameId)
}

onMounted(async () => {
  if (!auth.isLoggedIn) {
    router.push('/login')
    return
  }
  await gameStore.fetchFavorites()
})
</script>

<template>
  <div class="fav-page">
    <div class="fav-container">

      <div class="section-head">
        <div class="section-title">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" stroke="none" style="color:var(--yellow);flex-shrink:0"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
          УЛЮБЛЕНІ ІГРИ
          <span class="section-count">{{ gameStore.favoriteGames.length }} ігор</span>
        </div>
        <router-link to="/games" class="back-btn">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="flex-shrink:0"><rect x="2" y="6" width="20" height="12" rx="2"/><line x1="6" y1="12" x2="10" y2="12"/><line x1="8" y1="10" x2="8" y2="14"/><circle cx="16" cy="10" r="1" fill="currentColor" stroke="none"/><circle cx="18" cy="13" r="1" fill="currentColor" stroke="none"/></svg>
          ВСІ ІГРИ →
        </router-link>
      </div>

      <div class="fav-filters" v-if="gameStore.favoriteGames.length">
        <div class="filter-search-wrap">
          <svg class="filter-search-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input
            v-model="search"
            class="filter-search"
            placeholder="Пошук серед улюблених..."
          />
        </div>
      </div>

      <div v-if="gameStore.favoritesLoading" class="empty-state">
        <div class="empty-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
        </div>
        <h3>ЗАВАНТАЖЕННЯ...</h3>
      </div>

      <template v-else>
        <div v-if="filteredFavorites.length" class="fav-grid">
          <div
            v-for="game in filteredFavorites"
            :key="game.id"
            class="fav-card"
            @click="goToGame(game.id)"
          >
            <button
              class="heart-btn filled"
              :class="{ animating: animatingHearts.has(game.id) }"
              title="Видалити з улюблених"
              @click.stop="handleRemoveFavorite(game.id)"
            >
              <svg viewBox="0 0 24 24" class="heart-svg">
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5
                  2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09
                  C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5
                  c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" />
              </svg>
            </button>

            <div class="fav-card-cover">
              <img v-if="game.imageUrl" :src="game.imageUrl" :alt="game.name" class="fav-cover-img" />
              <div v-else class="fav-cover-ph">
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="6" width="20" height="12" rx="2"/><line x1="6" y1="12" x2="10" y2="12"/><line x1="8" y1="10" x2="8" y2="14"/><circle cx="16" cy="10" r="1" fill="currentColor" stroke="none"/><circle cx="18" cy="13" r="1" fill="currentColor" stroke="none"/></svg>
              </div>
              <div class="fav-cover-fade"></div>
            </div>

            <div class="fav-card-body">
              <div class="fav-game-name">{{ game.name }}</div>
              <div class="fav-meta-row">
                <span v-if="game.genre" class="fav-genre" :class="genreColor(game.genre)">{{ game.genre }}</span>
                <span v-if="game.releaseYear" class="fav-year">{{ game.releaseYear }}</span>
              </div>
              <div class="fav-bottom">
                <span class="fav-party-size">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                  до {{ game.maxPartySize }} гравців
                </span>
                <span class="fav-find-btn">ЗНАЙТИ ЛОБІ →</span>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="search.trim() && gameStore.favoriteGames.length" class="empty-state">
          <div class="empty-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          </div>
          <h3>НІЧОГО НЕ ЗНАЙДЕНО</h3>
          <p>Спробуй інший пошуковий запит</p>
        </div>

        <div v-else class="empty-state">
          <div class="empty-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
          </div>
          <h3>УЛЮБЛЕНИХ ІГОР ПОКИ НЕМАЄ</h3>
          <p>Натисни на серце біля будь-якої гри в каталозі, щоб додати її сюди</p>
          <router-link to="/games" class="action-btn">ПЕРЕЙТИ ДО КАТАЛОГУ</router-link>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.fav-page {
  padding-top: 64px;
  min-height: 100svh;
  background: var(--black);
}
.fav-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 64px 80px;
}

.back-btn {
  font-family: var(--font-display), sans-serif;
  font-size: 16px;
  letter-spacing: 3px;
  padding: 12px 28px;
  background: transparent;
  border: 2px solid var(--yellow);
  color: var(--yellow);
  transition: background 0.15s, color 0.15s;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.back-btn:focus-visible,
.action-btn:focus-visible,
.heart-btn:focus-visible,
.filter-search:focus-visible {
  outline: 2px solid var(--yellow);
  outline-offset: 2px;
}
.back-btn:hover {
  background: var(--yellow);
  color: var(--black);
}

.fav-filters {
  display: flex;
  gap: 12px;
  margin-bottom: 28px;
  flex-wrap: wrap;
}

.filter-search-wrap {
  position: relative;
  flex: 1;
  min-width: 220px;
  display: flex;
  align-items: center;
}
.filter-search-icon {
  position: absolute;
  left: 12px;
  color: var(--gray);
  pointer-events: none;
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
.filter-search:focus { border-color: var(--yellow-dim); }
.filter-search::placeholder { color: var(--gray); }

.fav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 18px;
}

.fav-card {
  background: var(--panel);
  border: 2px solid var(--border);
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s, transform 0.15s, box-shadow 0.2s;
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 100%;
}
.fav-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--yellow), transparent);
  opacity: 0;
  transition: opacity 0.2s;
}
.fav-card:hover {
  border-color: var(--yellow-dim);
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
}
.fav-card:hover::before {
  opacity: 1;
}

.heart-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 3;
  width: 36px;
  height: 36px;
  border: none;
  background: rgba(10, 10, 11, 0.7);
  backdrop-filter: blur(4px);
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  transition: transform 0.2s, background 0.2s;
  opacity: 0;
}
.fav-card:hover .heart-btn,
.heart-btn.filled {
  opacity: 1;
}
.heart-btn:hover {
  transform: scale(1.15);
  background: rgba(10, 10, 11, 0.9);
}
.heart-svg {
  width: 20px;
  height: 20px;
  fill: transparent;
  stroke: var(--gray);
  stroke-width: 2;
  transition: fill 0.25s, stroke 0.25s;
}
.heart-btn.filled .heart-svg {
  fill: var(--yellow);
  stroke: var(--yellow);
}
.heart-btn:not(.filled):hover .heart-svg {
  stroke: var(--yellow-dim);
}
.heart-btn.animating {
  animation: heart-pulse 0.4s ease;
}
@keyframes heart-pulse {
  0%   { transform: scale(1); }
  30%  { transform: scale(1.35); }
  60%  { transform: scale(0.95); }
  100% { transform: scale(1); }
}

.fav-card-cover {
  position: relative;
  width: 100%;
  height: 148px;
  overflow: hidden;
  background: var(--dark);
  flex-shrink: 0;
}
.fav-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
  filter: brightness(0.8);
  display: block;
}
.fav-card:hover .fav-cover-img {
  transform: scale(1.08);
  filter: brightness(0.95);
}
.fav-cover-ph {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--panel-light);
  color: var(--gray);
}
.fav-cover-fade {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 50%;
  background: linear-gradient(to top, var(--panel) 0%, transparent 100%);
  pointer-events: none;
}

.fav-card-body {
  padding: 14px 18px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}
.fav-game-name {
  font-family: var(--font-display), sans-serif;
  font-size: 20px;
  letter-spacing: 2px;
  color: var(--white);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.15s;
}
.fav-card:hover .fav-game-name {
  color: var(--yellow);
}
.fav-meta-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.fav-genre {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
  text-transform: uppercase;
  padding: 2px 10px;
  border: 1px solid var(--border);
  color: var(--gray-light);
  background: rgba(255, 255, 255, 0.03);
}
.fav-genre.blue {
  border-color: #3498db44;
  color: #5dade2;
  background: rgba(52, 152, 219, 0.08);
}
.fav-genre.purple {
  border-color: #9b59b644;
  color: #bb8fce;
  background: rgba(155, 89, 182, 0.08);
}
.fav-genre.red {
  border-color: #c0392b44;
  color: #e57373;
  background: rgba(192, 57, 43, 0.08);
}
.fav-genre.green {
  border-color: #27ae6044;
  color: #58d68d;
  background: rgba(39, 174, 96, 0.08);
}
.fav-year {
  font-size: 12px;
  color: var(--gray);
  letter-spacing: 1px;
}
.fav-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 6px;
}
.fav-party-size {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--gray);
  letter-spacing: 0.5px;
}
.fav-find-btn {
  font-family: var(--font-display), sans-serif;
  font-size: 11px;
  letter-spacing: 2px;
  color: var(--yellow-dim);
  opacity: 0;
  transition: opacity 0.2s, color 0.2s;
}
.fav-card:hover .fav-find-btn {
  opacity: 1;
  color: var(--yellow);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}
.empty-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  color: var(--gray);
}
.empty-state h3 {
  font-family: var(--font-display), sans-serif;
  font-size: 28px;
  letter-spacing: 2px;
  color: var(--yellow);
  margin-bottom: 8px;
}
.empty-state p {
  color: var(--gray);
  font-size: 15px;
}
.action-btn {
  font-family: var(--font-display), sans-serif;
  letter-spacing: 2px;
  font-size: 15px;
  padding: 10px 28px;
  border: 2px solid var(--yellow);
  background: var(--yellow);
  color: var(--black);
  text-transform: uppercase;
  transition: background 0.15s;
  display: inline-block;
  margin-top: 16px;
  text-decoration: none;
}
.action-btn:hover {
  background: var(--yellow-dim);
}

@media (max-width: 1100px) {
  .fav-container {
    padding: 34px 40px 70px;
  }

  .fav-grid {
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  }
}

@media (max-width: 900px) {
  .fav-container {
    padding: 28px 28px 64px;
  }

  .section-head {
    margin-bottom: 20px;
    padding-bottom: 14px;
    gap: 12px;
  }

  .section-title {
    font-size: 28px;
    letter-spacing: 2px;
    gap: 12px;
    flex-wrap: wrap;
  }

  .section-count {
    letter-spacing: 1px;
    padding: 4px 10px;
  }

  .fav-filters {
    margin-bottom: 20px;
  }

  .fav-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 14px;
  }

  .fav-card-cover {
    height: 132px;
  }

  .fav-card-body {
    padding: 12px 14px 14px;
  }

  .fav-game-name {
    font-size: 18px;
    letter-spacing: 1.5px;
  }

  .fav-find-btn {
    opacity: 1;
    font-size: 10px;
  }

  .heart-btn {
    opacity: 1;
  }
}

@media (max-width: 600px) {
  .fav-container {
    padding: 24px 20px 60px;
  }

  .section-head {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .back-btn {
    width: 100%;
    justify-content: center;
    font-size: 14px;
    letter-spacing: 2px;
    padding: 11px 16px;
  }

  .fav-filters {
    gap: 10px;
  }

  .filter-search-wrap {
    min-width: 100%;
  }

  .filter-search {
    min-height: 44px;
    font-size: 15px;
  }

  .fav-grid {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .fav-card {
    display: grid;
    grid-template-columns: 104px 1fr;
    min-height: 124px;
  }

  .fav-card-cover {
    width: 104px;
    height: 100%;
  }

  .fav-card-body {
    padding: 10px 12px;
    gap: 6px;
  }

  .fav-game-name {
    font-size: 16px;
    letter-spacing: 1px;
  }

  .fav-meta-row {
    gap: 8px;
    flex-wrap: wrap;
  }

  .fav-genre {
    font-size: 9px;
    letter-spacing: 1.2px;
    padding: 2px 8px;
  }

  .fav-year,
  .fav-party-size {
    font-size: 11px;
  }

  .fav-bottom {
    flex-direction: column;
    align-items: flex-start;
    gap: 5px;
  }

  .fav-find-btn {
    opacity: 0.92;
    font-size: 9px;
    letter-spacing: 1px;
  }

  .heart-btn {
    top: 8px;
    right: 8px;
    width: 34px;
    height: 34px;
  }

  .heart-svg {
    width: 18px;
    height: 18px;
  }

  .empty-state {
    padding: 56px 16px;
  }

  .empty-state h3 {
    font-size: 22px;
    letter-spacing: 1.5px;
  }

  .empty-state p {
    font-size: 14px;
  }

  .action-btn {
    width: 100%;
    text-align: center;
    font-size: 14px;
    padding: 11px 16px;
  }
}

@media (max-width: 420px) {
  .fav-container {
    padding: 20px 14px 48px;
  }

  .section-title {
    font-size: 22px;
    letter-spacing: 1.5px;
  }

  .section-title svg {
    width: 18px;
    height: 18px;
  }

  .fav-card {
    grid-template-columns: 90px 1fr;
    min-height: 112px;
  }

  .fav-card-cover {
    width: 90px;
  }

  .fav-card-body {
    padding: 9px 10px;
  }

  .fav-game-name {
    font-size: 15px;
    letter-spacing: 0.8px;
  }

  .fav-cover-ph svg {
    width: 28px;
    height: 28px;
  }

  .fav-find-btn {
    opacity: 0.86;
  }

  .empty-state {
    padding: 48px 12px;
  }

  .empty-state h3 {
    font-size: 20px;
  }

  .empty-state p {
    font-size: 13px;
  }
}

@media (max-width: 360px) {
  .fav-container {
    padding: 16px 12px 40px;
  }

  .section-title {
    font-size: 20px;
    letter-spacing: 1px;
  }

  .back-btn,
  .action-btn {
    font-size: 13px;
    letter-spacing: 1px;
  }

  .fav-card {
    grid-template-columns: 78px 1fr;
    min-height: 104px;
  }

  .fav-card-cover {
    width: 78px;
  }

  .fav-card-body {
    padding: 8px;
    gap: 5px;
  }

  .fav-genre {
    font-size: 8px;
    letter-spacing: 0.8px;
  }

  .fav-year,
  .fav-party-size {
    font-size: 10px;
  }

  .fav-find-btn {
    font-size: 8px;
    letter-spacing: 0.8px;
  }

  .heart-btn {
    width: 32px;
    height: 32px;
  }

  .heart-svg {
    width: 16px;
    height: 16px;
  }

  .empty-state h3 {
    font-size: 18px;
  }

  .empty-state p {
    font-size: 12px;
  }
}
</style>

