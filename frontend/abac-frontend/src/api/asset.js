import { apiRequest } from './http'

export function queryAssets(query) {
  return apiRequest('/api/asset/list', { query })
}

export function getAsset(assetId) {
  return apiRequest(`/api/asset/${assetId}`)
}

export function getAssetsByProject(projectId) {
  return apiRequest(`/api/asset/project/${projectId}`)
}

export function exportAssetReference(assetId) {
  return apiRequest(`/api/asset/${assetId}/export`)
}

export function createAsset(payload) {
  return apiRequest('/api/asset/create', {
    method: 'POST',
    body: payload,
  })
}

export function uploadAsset(payload, file) {
  const formData = new FormData()

  Object.entries(payload).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }
    formData.append(key, String(value))
  })
  formData.append('file', file)

  return apiRequest('/api/asset/upload', {
    method: 'POST',
    body: formData,
  })
}

export function deleteAsset(assetId) {
  return apiRequest(`/api/asset/${assetId}`, {
    method: 'DELETE',
  })
}
