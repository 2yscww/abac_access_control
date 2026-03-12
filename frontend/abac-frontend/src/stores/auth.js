import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const TOKEN_KEY = 'abac_token'
const EMPLOYEE_ID_KEY = 'abac_employee_id'
const TEMP_TOKEN_KEY = 'abac_temp_token'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const tempToken = ref(localStorage.getItem(TEMP_TOKEN_KEY) || '')
  const employeeId = ref(localStorage.getItem(EMPLOYEE_ID_KEY) || '')

  const isAuthenticated = computed(() => Boolean(token.value))

  function setSession(payload) {
    token.value = payload.token || ''
    employeeId.value = payload.employeeId ? String(payload.employeeId) : employeeId.value
    tempToken.value = ''

    persist()
  }

  function setTempSession(payload) {
    tempToken.value = payload.tempToken || ''
    employeeId.value = payload.employeeId ? String(payload.employeeId) : ''
    token.value = ''

    persist()
  }

  function clearSession() {
    token.value = ''
    tempToken.value = ''
    employeeId.value = ''
    persist()
  }

  function persist() {
    if (token.value) {
      localStorage.setItem(TOKEN_KEY, token.value)
    } else {
      localStorage.removeItem(TOKEN_KEY)
    }

    if (tempToken.value) {
      localStorage.setItem(TEMP_TOKEN_KEY, tempToken.value)
    } else {
      localStorage.removeItem(TEMP_TOKEN_KEY)
    }

    if (employeeId.value) {
      localStorage.setItem(EMPLOYEE_ID_KEY, employeeId.value)
    } else {
      localStorage.removeItem(EMPLOYEE_ID_KEY)
    }
  }

  return {
    token,
    tempToken,
    employeeId,
    isAuthenticated,
    setSession,
    setTempSession,
    clearSession,
  }
})
