import { apiRequest } from './http'

export function getCurrentEmployeeProfile() {
  return apiRequest('/api/employee/me')
}

export function queryActiveEmployees(query) {
  return apiRequest('/api/employee/active-list', { query })
}

export function offboardEmployee(payload) {
  return apiRequest('/api/employee/offboard', {
    method: 'PUT',
    body: payload,
  })
}
