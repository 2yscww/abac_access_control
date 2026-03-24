import { apiRequest } from './http'

export function queryAuditLogs(query) {
  return apiRequest('/api/audit/list', { query })
}
