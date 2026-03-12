import { useAuthStore } from '@/stores/auth'
import { pinia } from '@/stores'

export class ApiError extends Error {
  constructor(message, status, code) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

function buildQuery(query = {}) {
  const searchParams = new URLSearchParams()

  Object.entries(query).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }
    searchParams.set(key, value)
  })

  const queryString = searchParams.toString()
  return queryString ? `?${queryString}` : ''
}

export async function apiRequest(path, options = {}) {
  const authStore = useAuthStore(pinia)
  const {
    method = 'GET',
    body,
    query,
    token,
  } = options

  const headers = {
    'Content-Type': 'application/json',
  }

  const authToken = token || authStore.token
  if (authToken) {
    headers.Authorization = `Bearer ${authToken}`
  }

  const response = await fetch(`${path}${buildQuery(query)}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })

  let payload = null
  try {
    payload = await response.json()
  } catch {
    payload = null
  }

  const businessCode = payload?.code ?? response.status
  if (!response.ok || businessCode >= 400) {
    if (businessCode === 401 || response.status === 401) {
      authStore.clearSession()
    }
    throw new ApiError(
      payload?.msg || '请求失败，请稍后重试',
      response.status,
      businessCode,
    )
  }

  return payload?.data
}
