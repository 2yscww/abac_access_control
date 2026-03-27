import { apiRequest } from './http'

export function getRuntimePolicyConfigs() {
  return apiRequest('/api/policy-config/runtime')
}

export function listPolicyConfigs() {
  return apiRequest('/api/policy-config')
}

export function updatePolicyConfig(policyName, payload) {
  return apiRequest(`/api/policy-config/${encodeURIComponent(policyName)}`, {
    method: 'PUT',
    body: payload,
  })
}
