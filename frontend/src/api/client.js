import axios from 'axios'

const PROTECTED_PREFIXES = ['/dashboard', '/center', '/admin']

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response && error.response.status === 401) {
      const path = window.location.pathname
      const isProtected = PROTECTED_PREFIXES.some((p) => path.startsWith(p))
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (isProtected && !window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default api
