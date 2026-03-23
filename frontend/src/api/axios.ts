import axios from 'axios'
import { API_BASE_URL } from '../config'

const api = axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json',
    },
})

api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const original = error.config
        const status = error.response?.status
        const errorCode = error.response?.data?.errorCode

        if (status === 403 && errorCode === 'BANNED') {
            window.dispatchEvent(new CustomEvent('auth:banned', { detail: error.response?.data }))
            return Promise.reject(error)
        }

        if (status === 401 && !original._retry) {
            original._retry = true

            try {
                await axios.post(`${API_BASE_URL}/auth/refresh`, null, {
                    withCredentials: true,
                })

                return api(original)
            } catch {
                return Promise.reject(error)
            }
        }

        return Promise.reject(error)
    }
)

export default api