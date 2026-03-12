import { apiRequest } from './http'

export function login(payload) {
  return apiRequest('/api/employee/login', {
    method: 'POST',
    body: payload,
  })
}

export function changePassword(tempToken, payload) {
  return apiRequest('/api/employee/change-password', {
    method: 'POST',
    body: payload,
    token: tempToken,
  })
}
