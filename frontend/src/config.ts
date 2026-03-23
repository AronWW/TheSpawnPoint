export const API_BASE_URL =
    import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export const PUBLIC_BASE_URL = API_BASE_URL.replace(/\/api\/?$/, '')

export const WS_URL =
    import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws'