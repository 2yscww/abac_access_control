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

export function createAsset(payload) {
  return apiRequest('/api/asset/create', {
    method: 'POST',
    body: payload,
  })
}

export function deleteAsset(assetId) {
  return apiRequest(`/api/asset/${assetId}`, {
    method: 'DELETE',
  })
}
