import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { API_BASE_URL } from '../config'

type RetryableRequestConfig = InternalAxiosRequestConfig & {
  _retry?: boolean
  skipAuthRefresh?: boolean
}

const baseConfig = {
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
}

export const publicApi = axios.create(baseConfig)
export const authApi: AxiosInstance = axios.create(baseConfig)

let refreshPromise: Promise<void> | null = null

const NEVER_REFRESH_PATHS = new Set([
  '/auth/login',
  '/auth/register',
  '/auth/verify-email',
  '/auth/resend-verification',
  '/auth/forgot-password',
  '/auth/reset-password',
  '/auth/refresh',
  '/auth/me',
])

function getRequestPath(config?: RetryableRequestConfig): string {
  const url = config?.url ?? ''
  if (!url) return ''

  if (url.startsWith('http')) {
    try {
      const parsed = new URL(url)
      return parsed.pathname.replace(/^\/api/, '')
    } catch {
      return url
    }
  }

  return url
}

function shouldTryRefresh(config?: RetryableRequestConfig): boolean {
  if (!config || config._retry || config.skipAuthRefresh) {
    return false
  }

  const path = getRequestPath(config)
  if (!path) return false

  if (NEVER_REFRESH_PATHS.has(path)) {
    return false
  }

  return true
}

async function runRefresh(): Promise<void> {
  if (!refreshPromise) {
    refreshPromise = axios
      .post(`${API_BASE_URL}/auth/refresh`, null, { withCredentials: true })
      .then(() => undefined)
      .finally(() => {
        refreshPromise = null
      })
  }

  return refreshPromise
}

authApi.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetryableRequestConfig | undefined
    const status = error.response?.status
    const errorCode = (error.response?.data as { errorCode?: string } | undefined)?.errorCode

    if (status === 403 && errorCode === 'BANNED') {
      window.dispatchEvent(new CustomEvent('auth:banned', { detail: error.response?.data }))
      return Promise.reject(error)
    }

    if (status !== 401 || !shouldTryRefresh(original)) {
      return Promise.reject(error)
    }

    try {
      original!._retry = true
      await runRefresh()
      return authApi(original!)
    } catch {
      return Promise.reject(error)
    }
  },
)

export default authApi
