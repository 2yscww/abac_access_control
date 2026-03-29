import { apiRequest } from './http'

export function getCurrentEmployeeProfile() {
  return apiRequest('/api/employee/me')
}

export function getEmployeeOnboardOptions() {
  return apiRequest('/api/employee/onboard-options')
}

export function createEmployee(payload) {
  return apiRequest('/api/employee/create', {
    method: 'POST',
    body: payload,
  })
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
