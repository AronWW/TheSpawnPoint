<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()

const email = ref('')
const error = ref('')
const success = ref('')
const loading = ref(false)

async function handleForgot() {
  error.value = ''
  success.value = ''

  if (!email.value) {
    error.value = 'Введіть email'
    return
  }

  loading.value = true
  try {
    await auth.forgotPassword(email.value)
    success.value = 'Інструкції надіслано на вашу пошту. Перевірте email.'
  } catch (e: any) {
    const msg = e.response?.data?.message || e.response?.data?.error
    error.value = msg || 'Помилка при надсиланні'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-bg"></div>
    <div class="auth-container auth-container--narrow">
      <div class="auth-form-panel auth-form-panel--full">
        <div class="auth-form-header">
          <div class="auth-form-icon">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"/></svg>
          </div>
          <h1 class="auth-form-title">ЗАБУЛИ ПАРОЛЬ?</h1>
          <p class="auth-form-sub">Введіть email і ми надішлемо інструкції для відновлення</p>
        </div>

        <form class="auth-form" @submit.prevent="handleForgot">
          <div v-if="error" class="auth-error">{{ error }}</div>
          <div v-if="success" class="auth-success">{{ success }}</div>

          <div class="form-group">
            <label class="form-label">Email</label>
            <input
              class="form-input"
              type="email"
              v-model="email"
              placeholder="gamer@example.com"
              autocomplete="email"
            />
          </div>

          <button type="submit" class="btn-submit auth-submit" :disabled="loading">
            {{ loading ? 'НАДСИЛАННЯ...' : '⚡ НАДІСЛАТИ ІНСТРУКЦІЇ' }}
          </button>
        </form>

        <div class="auth-footer-text">
          Згадали пароль?
          <router-link to="/login" class="auth-link accent">Увійти</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

