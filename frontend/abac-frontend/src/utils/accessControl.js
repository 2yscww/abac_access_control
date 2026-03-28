import { findOption, projectPhaseOptions, securityLevelOptions } from '@/constants/options'

const FULL_ACCESS_DEPTS_BY_PHASE = {
  INIT: ['PRODUCT', 'MANAGEMENT'],
  REQUIREMENT: ['PRODUCT', 'RD'],
  DEVELOPMENT: ['PRODUCT', 'RD'],
  TEST: ['QA', 'RD'],
  RELEASE: ['OPS', 'RD'],
  ARCHIVED: [],
}

function createDefaultPolicyConfigSnapshot() {
  return {
    SecurityLevelPolicy: {
      enabled: true,
      publicMinRank: 1,
      internalMinRank: 3,
      confidentialMinRank: 5,
      topSecretMinRank: 9,
    },
    EnvironmentAccessPolicy: {
      enabled: true,
      workStart: '08:00',
      workEnd: '20:00',
    },
    HistoricalExportPolicy: {
      enabled: true,
      exportThreshold: 50,
      exportWindowMinutes: 30,
    },
  }
}

const runtimePolicyConfig = createDefaultPolicyConfigSnapshot()

function getPhaseKey(rawPhase) {
  return findOption(projectPhaseOptions, rawPhase)?.key || ''
}

function getSecurityKey(rawSecurityLevel) {
  return findOption(securityLevelOptions, rawSecurityLevel)?.key || ''
}

function getSecurityPolicyConfig() {
  return runtimePolicyConfig.SecurityLevelPolicy
}

function getEnvironmentPolicyConfig() {
  return runtimePolicyConfig.EnvironmentAccessPolicy
}

function normalizeTimeToMinutes(rawValue, fallbackMinutes) {
  if (typeof rawValue !== 'string') {
    return fallbackMinutes
  }

  const [hourPart, minutePart] = rawValue.split(':')
  const hours = Number(hourPart)
  const minutes = Number(minutePart)
  if (
    !Number.isInteger(hours) ||
    !Number.isInteger(minutes) ||
    hours < 0 ||
    hours > 23 ||
    minutes < 0 ||
    minutes > 59
  ) {
    return fallbackMinutes
  }

  return hours * 60 + minutes
}

function getRequiredRankMap() {
  const config = getSecurityPolicyConfig()

  return {
    PUBLIC: Number(config.publicMinRank || 1),
    INTERNAL: Number(config.internalMinRank || 3),
    CONFIDENTIAL: Number(config.confidentialMinRank || 5),
    TOP_SECRET: Number(config.topSecretMinRank || 9),
  }
}

function isReadLikeAction(action) {
  return action === 'READ' || action === 'EXPORT'
}

export function applyPolicyConfigSnapshot(configs = []) {
  resetPolicyConfigSnapshot()

  configs.forEach((item) => {
    if (!item?.policyName || !runtimePolicyConfig[item.policyName]) {
      return
    }

    runtimePolicyConfig[item.policyName] = {
      ...runtimePolicyConfig[item.policyName],
      ...(item.conditions || {}),
      ...(item.enabled === undefined ? {} : { enabled: item.enabled }),
    }
  })
}

export function resetPolicyConfigSnapshot() {
  const defaults = createDefaultPolicyConfigSnapshot()
  Object.keys(defaults).forEach((policyName) => {
    runtimePolicyConfig[policyName] = defaults[policyName]
  })
}

export function getPolicyConfigSnapshot() {
  return JSON.parse(JSON.stringify(runtimePolicyConfig))
}

export function isWithinWorkingHours(date = new Date()) {
  const config = getEnvironmentPolicyConfig()
  if (config.enabled === false) {
    return true
  }

  const currentMinutes = date.getHours() * 60 + date.getMinutes()
  const startMinutes = normalizeTimeToMinutes(config.workStart, 8 * 60)
  const endMinutes = normalizeTimeToMinutes(config.workEnd, 20 * 60)
  return currentMinutes >= startMinutes && currentMinutes <= endMinutes
}

export function isHighSecurityLevel(rawSecurityLevel) {
  const securityKey = getSecurityKey(rawSecurityLevel)
  return securityKey === 'CONFIDENTIAL' || securityKey === 'TOP_SECRET'
}

export function hasSecurityClearance(profile, rawSecurityLevel) {
  const securityKey = getSecurityKey(rawSecurityLevel)
  if (!profile || !securityKey) {
    return false
  }

  const securityPolicyConfig = getSecurityPolicyConfig()
  if (securityPolicyConfig.enabled !== false) {
    if (
      profile.isContractor &&
      (securityKey === 'CONFIDENTIAL' || securityKey === 'TOP_SECRET')
    ) {
      return false
    }

    const requiredRank = getRequiredRankMap()[securityKey] || Number.MAX_SAFE_INTEGER
    const currentRank = Number(profile.levelRank || 0)
    if (currentRank < requiredRank) {
      return false
    }
  }

  if (isHighSecurityLevel(rawSecurityLevel) && !isWithinWorkingHours()) {
    return false
  }

  return true
}

export function hasPhaseActionAccess(profile, rawPhase, action = 'WRITE') {
  const deptType = profile?.deptType || ''
  const phaseKey = getPhaseKey(rawPhase)
  if (!deptType || !phaseKey) {
    return false
  }

  if (phaseKey === 'ARCHIVED') {
    return deptType === 'MANAGEMENT' && isReadLikeAction(action)
  }

  const allowedDeptTypes = FULL_ACCESS_DEPTS_BY_PHASE[phaseKey] || []
  if (allowedDeptTypes.includes(deptType)) {
    return true
  }

  if (deptType === 'MANAGEMENT') {
    return isReadLikeAction(action)
  }

  return false
}

export function canCreateProject(profile, rawPhase, rawSecurityLevel) {
  return (
    hasPhaseActionAccess(profile, rawPhase, 'WRITE') &&
    hasSecurityClearance(profile, rawSecurityLevel)
  )
}

export function canMutateProject(
  profile,
  project,
  { assumeActiveMember = false, currentEmployeeId = null } = {},
) {
  if (!project) {
    return false
  }

  const phaseKey = getPhaseKey(project.projectPhase)
  const isOwner =
    currentEmployeeId != null && Number(currentEmployeeId) === Number(project.ownerId)
  const isActiveMember = assumeActiveMember || (phaseKey !== 'ARCHIVED' && isOwner)

  return (
    isActiveMember &&
    hasPhaseActionAccess(profile, project.projectPhase, 'WRITE') &&
    hasSecurityClearance(profile, project.securityLevel)
  )
}

function hasMutationMembership(
  project,
  { assumeActiveMember = false, currentEmployeeId = null } = {},
) {
  if (!project) {
    return false
  }

  const phaseKey = getPhaseKey(project.projectPhase)
  const isOwner =
    currentEmployeeId != null && Number(currentEmployeeId) === Number(project.ownerId)

  return assumeActiveMember || (phaseKey !== 'ARCHIVED' && isOwner)
}

function hasExportMembership(
  profile,
  project,
  { assumeActiveMember = false, currentEmployeeId = null } = {},
) {
  if (profile?.deptType === 'MANAGEMENT') {
    return true
  }

  return hasMutationMembership(project, { assumeActiveMember, currentEmployeeId })
}

export function canCreateAsset(
  profile,
  project,
  rawAssetStage,
  rawSecurityLevel,
  options = {},
) {
  return (
    hasMutationMembership(project, options) &&
    hasPhaseActionAccess(profile, project?.projectPhase, 'WRITE') &&
    hasPhaseActionAccess(profile, rawAssetStage, 'WRITE') &&
    hasSecurityClearance(profile, rawSecurityLevel)
  )
}

export function canOperateAsset(
  profile,
  project,
  asset,
  { assumeActiveMember = false, currentEmployeeId = null } = {},
) {
  return canDeleteAsset(profile, project, asset, { assumeActiveMember, currentEmployeeId })
}

export function canExportAsset(
  profile,
  project,
  asset,
  { assumeActiveMember = false, currentEmployeeId = null } = {},
) {
  if (!asset) {
    return false
  }

  return (
    hasExportMembership(profile, project, { assumeActiveMember, currentEmployeeId }) &&
    hasPhaseActionAccess(profile, project?.projectPhase, 'EXPORT') &&
    hasPhaseActionAccess(profile, asset.assetsStage, 'EXPORT') &&
    hasSecurityClearance(profile, asset.securityLevel)
  )
}

export function canDeleteAsset(
  profile,
  project,
  asset,
  { assumeActiveMember = false, currentEmployeeId = null } = {},
) {
  if (!asset) {
    return false
  }

  return (
    hasMutationMembership(project, { assumeActiveMember, currentEmployeeId }) &&
    hasPhaseActionAccess(profile, project?.projectPhase, 'WRITE') &&
    hasPhaseActionAccess(profile, asset.assetsStage, 'WRITE') &&
    hasSecurityClearance(profile, asset.securityLevel)
  )
}

export function getAllowedProjectPhaseOptions(profile) {
  return projectPhaseOptions.filter((option) =>
    hasPhaseActionAccess(profile, option.value, 'WRITE'),
  )
}

export function getAllowedSecurityLevelOptions(profile) {
  return securityLevelOptions.filter((option) =>
    hasSecurityClearance(profile, option.value),
  )
}

export function getAllowedAssetStageOptions(profile, rawProjectPhase) {
  const currentProjectPhase = Number(rawProjectPhase || 0)

  return projectPhaseOptions.filter(
    (option) =>
      option.value < 6 &&
      option.value <= currentProjectPhase &&
      hasPhaseActionAccess(profile, option.value, 'WRITE'),
  )
}
