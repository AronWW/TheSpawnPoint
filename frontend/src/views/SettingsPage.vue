<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import api from '../api/axios'
import type { PrivacySettings, VisibilityLevel } from '../types'
import { useToast } from '../composables/useToast'

const router = useRouter()
const auth = useAuthStore()
const toast = useToast()

const activeTab = ref<'security' | 'privacy'>('security')

const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const passwordLoading = ref(false)
const showCurrentPw = ref(false)
const showNewPw = ref(false)
const showConfirmPw = ref(false)

async function handleChangePassword() {
  if (!passwordForm.value.newPassword || !passwordForm.value.currentPassword) {
    toast.show('Заповніть всі поля', 'error')
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    toast.show('Новий пароль має бути не менше 6 символів', 'error')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    toast.show('Паролі не збігаються', 'error')
    return
  }
  passwordLoading.value = true
  try {
    await auth.changePassword(passwordForm.value.currentPassword, passwordForm.value.newPassword)
    toast.show('Пароль успішно змінено!')
    passwordForm.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
  } catch (e: any) {
    const msg = e.response?.data?.message || 'Помилка зміни пароля'
    toast.show(msg, 'error')
  } finally {
    passwordLoading.value = false
  }
}

const privacyLoading = ref(false)
const privacySaving = ref(false)
const privacy = ref<PrivacySettings>({
  friendsVisibility: 'ALL',
  statusVisibility: 'ALL',
  favoriteGamesVisibility: 'ALL',
  statsVisibility: 'ALL',
  socialsVisibility: 'ALL',
  commentsPolicy: 'ALL',
})

const PRIVACY_FIELDS: { key: keyof PrivacySettings; label: string; icon: string }[] = [
  { key: 'friendsVisibility', label: 'Список друзів', icon: 'friends' },
  { key: 'statusVisibility', label: 'Статус гравця', icon: 'status' },
  { key: 'favoriteGamesVisibility', label: 'Улюблені ігри', icon: 'games' },
  { key: 'statsVisibility', label: 'Ігрова статистика', icon: 'stats' },
  { key: 'socialsVisibility', label: 'Соціальні мережі', icon: 'socials' },
  { key: 'commentsPolicy', label: 'Додавання коментарів', icon: 'comments' },
]

const VISIBILITY_OPTIONS: { value: VisibilityLevel; label: string }[] = [
  { value: 'ALL', label: 'Всі' },
  { value: 'FRIENDS', label: 'Друзі' },
  { value: 'NOBODY', label: 'Ніхто' },
]

async function fetchPrivacy() {
  privacyLoading.value = true
  try {
    const { data } = await api.get<PrivacySettings>('/settings/privacy')
    privacy.value = data
  } catch { }
  finally { privacyLoading.value = false }
}

async function savePrivacy() {
  privacySaving.value = true
  try {
    const { data } = await api.put<PrivacySettings>('/settings/privacy', privacy.value)
    privacy.value = data
    toast.show('Налаштування приватності збережено!')
  } catch (e: any) {
    toast.show(e.response?.data?.message || 'Помилка збереження', 'error')
  } finally {
    privacySaving.value = false
  }
}

onMounted(() => {
  if (!auth.isLoggedIn) {
    router.push('/login')
    return
  }
  fetchPrivacy()
})
</script>

<template>
  <div class="settings-page">
    <div class="settings-container">
      <div class="settings-header">
        <h1 class="settings-title">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: -2px;"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
          НАЛАШТУВАННЯ
        </h1>
        <p class="settings-subtitle">Безпека та приватність вашого акаунту</p>
      </div>

      <div class="settings-tabs">
        <button
          class="settings-tab"
          :class="{ active: activeTab === 'security' }"
          @click="activeTab = 'security'"
        >
          <span class="tab-icon"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg></span> Безпека
        </button>
        <button
          class="settings-tab"
          :class="{ active: activeTab === 'privacy' }"
          @click="activeTab = 'privacy'"
        >
          <span class="tab-icon"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg></span> Приватність
        </button>
      </div>

      <div v-if="activeTab === 'security'" class="settings-panel">
        <div class="panel-title">ЗМІНА ПАРОЛЯ</div>
        <form class="password-form" @submit.prevent="handleChangePassword">
          <div class="form-group">
            <label>Поточний пароль</label>
            <div class="input-wrap">
              <input
                :type="showCurrentPw ? 'text' : 'password'"
                v-model="passwordForm.currentPassword"
                placeholder="Введіть поточний пароль"
                autocomplete="current-password"
              />
              <button type="button" class="pw-toggle" @click="showCurrentPw = !showCurrentPw">
                <svg v-if="showCurrentPw" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
              </button>
            </div>
          </div>

          <div class="form-group">
            <label>Новий пароль</label>
            <div class="input-wrap">
              <input
                :type="showNewPw ? 'text' : 'password'"
                v-model="passwordForm.newPassword"
                placeholder="Мін. 6 символів"
                autocomplete="new-password"
              />
              <button type="button" class="pw-toggle" @click="showNewPw = !showNewPw">
                <svg v-if="showNewPw" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
              </button>
            </div>
          </div>

          <div class="form-group">
            <label>Підтвердження пароля</label>
            <div class="input-wrap">
              <input
                :type="showConfirmPw ? 'text' : 'password'"
                v-model="passwordForm.confirmPassword"
                placeholder="Повторіть новий пароль"
                autocomplete="new-password"
              />
              <button type="button" class="pw-toggle" @click="showConfirmPw = !showConfirmPw">
                <svg v-if="showConfirmPw" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
              </button>
            </div>
            <span
              v-if="passwordForm.confirmPassword && passwordForm.newPassword !== passwordForm.confirmPassword"
              class="field-error"
            >Паролі не збігаються</span>
          </div>

          <button
            type="submit"
            class="save-btn"
            :disabled="passwordLoading || !passwordForm.currentPassword || !passwordForm.newPassword || passwordForm.newPassword !== passwordForm.confirmPassword"
          >
            {{ passwordLoading ? 'ЗБЕРЕЖЕННЯ...' : 'ЗМІНИТИ ПАРОЛЬ' }}
          </button>
        </form>
      </div>

      <div v-if="activeTab === 'privacy'" class="settings-panel">
        <div class="panel-title">НАЛАШТУВАННЯ ПРИВАТНОСТІ</div>
        <p class="panel-desc">Оберіть, хто може бачити вашу інформацію на профілі</p>

        <div v-if="privacyLoading" class="privacy-loading">Завантаження...</div>

        <div v-else class="privacy-rows">
          <div v-for="field in PRIVACY_FIELDS" :key="field.key" class="privacy-row">
            <div class="privacy-label">
              <span class="privacy-icon">
                <svg v-if="field.icon === 'friends'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                <svg v-else-if="field.icon === 'status'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3"/></svg>
                <svg v-else-if="field.icon === 'games'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                <svg v-else-if="field.icon === 'stats'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
                <svg v-else-if="field.icon === 'socials'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
                <svg v-else-if="field.icon === 'comments'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
              </span>
              <span>{{ field.label }}</span>
            </div>
            <div class="privacy-options">
              <button
                v-for="opt in VISIBILITY_OPTIONS"
                :key="opt.value"
                class="privacy-opt"
                :class="{
                  active: privacy[field.key] === opt.value,
                  'opt-all': opt.value === 'ALL',
                  'opt-friends': opt.value === 'FRIENDS',
                  'opt-nobody': opt.value === 'NOBODY',
                }"
                @click="privacy[field.key] = opt.value"
              >
                {{ opt.label }}
              </button>
            </div>
          </div>
        </div>

        <button
          class="save-btn"
          :disabled="privacySaving"
          @click="savePrivacy"
        >
          {{ privacySaving ? 'ЗБЕРЕЖЕННЯ...' : 'ЗБЕРЕГТИ' }}
        </button>
      </div>

      <div class="settings-back">
        <router-link :to="auth.user ? `/profile/${auth.user.id}` : '/'" class="back-link">← Повернутись до профілю</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.settings-page {
  padding-top: 64px;
  min-height: 100vh;
  background: var(--black);
}

.settings-container {
  max-width: 680px;
  margin: 0 auto;
  padding: 40px 24px 80px;
}

.settings-header {
  margin-bottom: 32px;
}

.settings-title {
  font-size: 24px;
  letter-spacing: 3px;
  color: var(--white);
  font-weight: 800;
  margin: 0 0 6px;
}

.settings-subtitle {
  font-size: 13px;
  color: var(--gray);
  letter-spacing: 1px;
  margin: 0;
}

.settings-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 24px;
  border-bottom: 2px solid var(--border);
}

.settings-tab {
  flex: 1;
  padding: 12px 20px;
  background: none;
  border: none;
  color: var(--gray);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  cursor: pointer;
  transition: all 0.15s;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  font-family: var(--font-body);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.settings-tab:hover {
  color: var(--white);
}

.settings-tab.active {
  color: var(--yellow);
  border-bottom-color: var(--yellow);
}

.tab-icon {
  display: flex;
  align-items: center;
}

.settings-panel {
  background: var(--panel);
  border: 1px solid var(--border);
  padding: 32px;
}

.panel-title {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 2px;
  color: var(--yellow);
  margin-bottom: 8px;
}

.panel-desc {
  font-size: 13px;
  color: var(--gray);
  margin: 0 0 24px;
}

.password-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 11px;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  color: var(--gray);
  font-weight: 600;
}

.input-wrap {
  position: relative;
  display: flex;
}

.input-wrap input {
  width: 100%;
  padding: 10px 40px 10px 14px;
  background: var(--panel-light, #1e1e22);
  border: 1px solid var(--border);
  color: var(--white);
  font-size: 14px;
  font-family: var(--font-body);
  outline: none;
  transition: border-color 0.15s;
}

.input-wrap input:focus {
  border-color: var(--yellow-dim);
}

.pw-toggle {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  padding: 4px;
  color: var(--white);
  opacity: 0.6;
  transition: opacity 0.15s;
  display: flex;
  align-items: center;
}

.pw-toggle:hover {
  opacity: 1;
}

.field-error {
  font-size: 11px;
  color: #e74c3c;
  letter-spacing: 0.5px;
}

.save-btn {
  margin-top: 12px;
  padding: 12px 32px;
  background: var(--yellow);
  color: var(--black);
  border: none;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 2px;
  text-transform: uppercase;
  cursor: pointer;
  font-family: var(--font-body);
  transition: opacity 0.15s, background 0.15s;
  align-self: flex-start;
}

.save-btn:hover:not(:disabled) {
  background: #e6b800;
}

.save-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.privacy-loading {
  text-align: center;
  color: var(--gray);
  padding: 40px 0;
  font-size: 14px;
}

.privacy-rows {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.privacy-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: rgba(255,255,255,0.02);
  border: 1px solid var(--border);
  transition: background 0.1s;
}

.privacy-row:hover {
  background: rgba(255,255,255,0.04);
}

.privacy-label {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: var(--white);
  font-weight: 500;
}

.privacy-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  color: var(--gray);
}

.privacy-options {
  display: flex;
  gap: 4px;
}

.privacy-opt {
  padding: 6px 14px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1px;
  text-transform: uppercase;
  border: 1px solid var(--border);
  background: transparent;
  color: var(--gray);
  cursor: pointer;
  transition: all 0.15s;
  font-family: var(--font-body);
}

.privacy-opt:hover {
  color: var(--white);
  border-color: var(--white);
}

.privacy-opt.active.opt-all {
  background: rgba(39,174,96,0.15);
  border-color: #27ae60;
  color: #27ae60;
}

.privacy-opt.active.opt-friends {
  background: rgba(245,197,24,0.15);
  border-color: var(--yellow);
  color: var(--yellow);
}

.privacy-opt.active.opt-nobody {
  background: rgba(231,76,60,0.15);
  border-color: #e74c3c;
  color: #e74c3c;
}

.settings-back {
  margin-top: 32px;
  text-align: center;
}

.back-link {
  color: var(--gray);
  font-size: 13px;
  letter-spacing: 1px;
  text-decoration: none;
  transition: color 0.15s;
}

.back-link:hover {
  color: var(--yellow);
}

@media (max-width: 640px) {
  .settings-container {
    padding: 24px 16px 60px;
  }

  .settings-panel {
    padding: 20px 16px;
  }

  .privacy-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
    padding: 12px;
  }

  .privacy-options {
    width: 100%;
  }

  .privacy-opt {
    flex: 1;
    text-align: center;
    padding: 8px 6px;
    font-size: 10px;
  }

  .settings-tabs {
    gap: 0;
  }

  .settings-tab {
    font-size: 11px;
    padding: 10px 12px;
  }
}
</style>




