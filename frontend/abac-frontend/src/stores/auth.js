import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { getCurrentEmployeeProfile } from '@/api/employee'

const TOKEN_KEY = 'abac_token'
const EMPLOYEE_ID_KEY = 'abac_employee_id'
const TEMP_TOKEN_KEY = 'abac_temp_token'
const PROFILE_KEY = 'abac_employee_profile'
const MENU_KEY = 'abac_visible_menus'
const CAPABILITY_KEY = 'abac_capabilities'

function readJson(key, fallback) {
  const rawValue = localStorage.getItem(key)
  if (!rawValue) {
    return fallback
  }

  try {
    return JSON.parse(rawValue)
  } catch {
    return fallback
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const tempToken = ref(localStorage.getItem(TEMP_TOKEN_KEY) || '')
  const employeeId = ref(localStorage.getItem(EMPLOYEE_ID_KEY) || '')
  const profile = ref(readJson(PROFILE_KEY, null))
  const visibleMenus = ref(readJson(MENU_KEY, []))
  const capabilities = ref(readJson(CAPABILITY_KEY, []))

  const isAuthenticated = computed(() => Boolean(token.value))
  const profileLoaded = computed(() => Boolean(profile.value))
  const employeeLabel = computed(() => {
    if (profile.value?.employeeName) {
      return `${profile.value.employeeName}（${profile.value.employeeCode || employeeId.value || '-'}）`
    }

    return employeeId.value ? `员工 ${employeeId.value}` : '未登录'
  })

  function setSession(payload) {
    token.value = payload.token || ''
    employeeId.value = payload.employeeId ? String(payload.employeeId) : employeeId.value
    tempToken.value = ''
    clearCurrentUser()

    persist()
  }

  function setTempSession(payload) {
    tempToken.value = payload.tempToken || ''
    employeeId.value = payload.employeeId ? String(payload.employeeId) : ''
    token.value = ''
    clearCurrentUser()

    persist()
  }

  function clearSession() {
    token.value = ''
    tempToken.value = ''
    employeeId.value = ''
    clearCurrentUser()
    persist()
  }

  function clearCurrentUser() {
    profile.value = null
    visibleMenus.value = []
    capabilities.value = []
  }

  function setCurrentUser(payload) {
    profile.value = payload || null
    visibleMenus.value = payload?.visibleMenus || []
    capabilities.value = payload?.capabilities || []
    employeeId.value = payload?.employeeId ? String(payload.employeeId) : employeeId.value
    persist()
  }

  async function fetchCurrentUser(force = false) {
    if (!token.value) {
      clearCurrentUser()
      persist()
      return null
    }

    if (!force && profile.value) {
      return profile.value
    }

    const data = await getCurrentEmployeeProfile()
    setCurrentUser(data)
    return data
  }

  function hasMenu(menuKey) {
    return visibleMenus.value.includes(menuKey)
  }

  function hasCapability(capability) {
    return capabilities.value.includes(capability)
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

    if (profile.value) {
      localStorage.setItem(PROFILE_KEY, JSON.stringify(profile.value))
    } else {
      localStorage.removeItem(PROFILE_KEY)
    }

    if (visibleMenus.value.length > 0) {
      localStorage.setItem(MENU_KEY, JSON.stringify(visibleMenus.value))
    } else {
      localStorage.removeItem(MENU_KEY)
    }

    if (capabilities.value.length > 0) {
      localStorage.setItem(CAPABILITY_KEY, JSON.stringify(capabilities.value))
    } else {
      localStorage.removeItem(CAPABILITY_KEY)
    }
  }

  return {
    token,
    tempToken,
    employeeId,
    profile,
    visibleMenus,
    capabilities,
    isAuthenticated,
    profileLoaded,
    employeeLabel,
    setSession,
    setTempSession,
    setCurrentUser,
    fetchCurrentUser,
    hasMenu,
    hasCapability,
    clearSession,
  }
})
