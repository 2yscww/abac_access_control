import { apiRequest } from './http'

export function queryProjects(query) {
  return apiRequest('/api/project/list', { query })
}

export function getProject(projectId) {
  return apiRequest(`/api/project/${projectId}`)
}

export function getPhaseOwnerPreview(projectId, targetPhase) {
  return apiRequest(`/api/project/${projectId}/phase-owner-preview`, {
    query: { targetPhase },
  })
}

export function listProjectMembers(projectId) {
  return apiRequest(`/api/project/${projectId}/members`)
}

export function addProjectMember(projectId, payload) {
  return apiRequest(`/api/project/${projectId}/members`, {
    method: 'POST',
    body: payload,
  })
}

export function removeProjectMember(projectId, employeeId) {
  return apiRequest(`/api/project/${projectId}/members/${employeeId}`, {
    method: 'DELETE',
  })
}

export function createProject(payload) {
  return apiRequest('/api/project/create', {
    method: 'POST',
    body: payload,
  })
}

export function updateProjectPhase(payload) {
  return apiRequest('/api/project/phase', {
    method: 'PUT',
    body: payload,
  })
}

export function deleteProject(projectId) {
  return apiRequest(`/api/project/${projectId}`, {
    method: 'DELETE',
  })
}
