import { apiRequest } from './http'

export function queryManagerHandoverTodos() {
  return apiRequest('/api/department/manager-handover-todos')
}

export function queryManagerCandidates(deptId) {
  return apiRequest(`/api/department/${deptId}/manager-candidates`)
}

export function assignDepartmentManager(payload) {
  return apiRequest('/api/department/manager', {
    method: 'PUT',
    body: payload,
  })
}
