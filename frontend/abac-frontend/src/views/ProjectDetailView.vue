<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import InsightChart from '@/components/InsightChart.vue'
import {
  createAsset,
  deleteAsset,
  exportAssetReference,
  getAssetsByProject,
} from '@/api/asset'
import {
  addProjectMember,
  getProject,
  listProjectMembers,
  removeProjectMember,
  updateProjectPhase,
} from '@/api/project'
import {
  assetTypeOptions,
  findOption,
  getOptionLabel,
  projectPhaseOptions,
  securityLevelOptions,
} from '@/constants/options'
import { useAuthStore } from '@/stores/auth'
import {
  canCreateAsset as canCreateAssetByRules,
  canOperateAsset as canOperateAssetByRules,
  getAllowedAssetStageOptions,
  getAllowedSecurityLevelOptions,
} from '@/utils/accessControl'

const route = useRoute()
const authStore = useAuthStore()

const deptTypeLabels = {
  MANAGEMENT: 'Management',
  PRODUCT: 'Product',
  RD: 'R&D',
  QA: 'QA',
  OPS: 'Ops',
  HR: 'HR',
}

const memberStatusLabels = {
  ACTIVE: 'Active',
  INACTIVE: 'Inactive',
}

const projectId = computed(() => Number(route.params.id))
const project = ref(null)
const assets = ref([])
const members = ref([])

const loading = ref(false)
const membersLoading = ref(false)
const phaseSubmitting = ref(false)
const assetSubmitting = ref(false)
const memberSubmitting = ref(false)
const selectedPhase = ref(1)
const nextOwnerId = ref('')
const memberLoadError = ref('')

const exportedPaths = reactive({})
const exportingAssetIds = reactive({})

const assetForm = reactive({
  projectId: projectId.value,
  assetName: '',
  assetsType: 1,
  assetsStage: 1,
  securityLevel: 2,
  filePath: '',
  fileSize: '',
  description: '',
})

const memberForm = reactive({
  employeeId: '',
})

const currentPhaseValue = computed(
  () => findOption(projectPhaseOptions, project.value?.projectPhase)?.value || 1,
)

const phaseProgress = computed(() =>
  Math.round((currentPhaseValue.value / projectPhaseOptions.length) * 100),
)

const activeMembers = computed(() =>
  members.value.filter((member) => member.status === 'ACTIVE'),
)

const inactiveMembers = computed(() =>
  members.value.filter((member) => member.status !== 'ACTIVE'),
)

const visibleAssetCount = computed(() => assets.value.length)

const sensitiveAssetCount = computed(
  () =>
    assets.value.filter((asset) => ['CONFIDENTIAL', 'TOP_SECRET', 3, 4].includes(asset.securityLevel))
      .length,
)

const exportedCount = computed(() => Object.keys(exportedPaths).length)

const isArchived = computed(
  () => findOption(projectPhaseOptions, project.value?.projectPhase)?.key === 'ARCHIVED',
)

const currentProfile = computed(() => authStore.profile || null)
const currentEmployeeId = computed(() =>
  Number(authStore.profile?.employeeId || authStore.employeeId || 0),
)
const assumeProjectMembership = computed(
  () => currentProfile.value?.deptType && currentProfile.value.deptType !== 'MANAGEMENT',
)

const canManageMembers = computed(() => {
  if (!project.value) {
    return false
  }

  return (
    authStore.profile?.deptType === 'MANAGEMENT' ||
    currentEmployeeId.value === Number(project.value.ownerId)
  )
})

const canEditMembers = computed(() => canManageMembers.value && !isArchived.value)
const canAdvancePhase = computed(
  () => Boolean(project.value) && currentEmployeeId.value === Number(project.value.ownerId),
)
const allowedAssetStageOptions = computed(() =>
  getAllowedAssetStageOptions(currentProfile.value, project.value?.projectPhase),
)
const allowedAssetSecurityOptions = computed(() =>
  getAllowedSecurityLevelOptions(currentProfile.value),
)
const canCreateAnyAsset = computed(
  () =>
    Boolean(project.value) &&
    allowedAssetStageOptions.value.some((stageOption) =>
      allowedAssetSecurityOptions.value.some((securityOption) =>
        canCreateAssetByRules(
          currentProfile.value,
          project.value,
          stageOption.value,
          securityOption.value,
          {
            assumeActiveMember: assumeProjectMembership.value,
            currentEmployeeId: currentEmployeeId.value,
          },
        ),
      ),
    ),
)
const canCreateSelectedAsset = computed(() =>
  canCreateAssetByRules(
    currentProfile.value,
    project.value,
    assetForm.assetsStage,
    assetForm.securityLevel,
    {
      assumeActiveMember: assumeProjectMembership.value,
      currentEmployeeId: currentEmployeeId.value,
    },
  ),
)

const assetTypeChartOption = computed(() => {
  const chartData = assetTypeOptions
    .map((option) => ({
      name: option.label,
      value: assets.value.filter(
        (asset) => findOption(assetTypeOptions, asset.assetsType)?.key === option.key,
      ).length,
    }))
    .filter((item) => item.value > 0)

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
    },
    legend: {
      bottom: 0,
      icon: 'circle',
      textStyle: {
        color: '#64748b',
      },
    },
    series: [
      {
        type: 'pie',
        radius: ['46%', '72%'],
        center: ['50%', '45%'],
        label: {
          formatter: '{b}\n{c}',
          color: '#334155',
        },
        data:
          chartData.length > 0
            ? chartData
            : [
                {
                  name: 'No Assets',
                  value: 1,
                  itemStyle: {
                    color: '#cbd5e1',
                  },
                },
              ],
        itemStyle: {
          borderRadius: 16,
        },
      },
    ],
  }
})

function resetExportState() {
  Object.keys(exportedPaths).forEach((key) => delete exportedPaths[key])
  Object.keys(exportingAssetIds).forEach((key) => delete exportingAssetIds[key])
}

function syncAssetFormRestrictions() {
  if (
    !allowedAssetStageOptions.value.some((option) => option.value === assetForm.assetsStage)
  ) {
    assetForm.assetsStage = allowedAssetStageOptions.value[0]?.value || ''
  }

  if (
    !allowedAssetSecurityOptions.value.some(
      (option) => option.value === assetForm.securityLevel,
    )
  ) {
    assetForm.securityLevel = allowedAssetSecurityOptions.value[0]?.value || ''
  }
}

function resetAssetForm() {
  assetForm.assetName = ''
  assetForm.assetsType = 1
  assetForm.filePath = ''
  assetForm.fileSize = ''
  assetForm.description = ''
  syncAssetFormRestrictions()
}

function resetMemberForm() {
  memberForm.employeeId = ''
}

function getDeptLabel(deptType) {
  return deptTypeLabels[deptType] || deptType || '-'
}

function getMemberStatusLabel(status) {
  return memberStatusLabels[status] || status || '-'
}

function getMemberStatusTagType(status) {
  return status === 'ACTIVE' ? 'success' : 'info'
}

function isCurrentOwner(employeeId) {
  return Number(project.value?.ownerId) === Number(employeeId)
}

function canOperateAsset(asset) {
  return canOperateAssetByRules(currentProfile.value, project.value, asset, {
    assumeActiveMember: assumeProjectMembership.value,
    currentEmployeeId: currentEmployeeId.value,
  })
}

async function loadProjectCore() {
  const [projectData, assetData] = await Promise.all([
    getProject(projectId.value),
    getAssetsByProject(projectId.value),
  ])

  project.value = projectData
  assets.value = assetData || []
  selectedPhase.value = findOption(projectPhaseOptions, projectData.projectPhase)?.value || 1
  nextOwnerId.value = projectData.ownerId ? String(projectData.ownerId) : ''
  assetForm.projectId = projectId.value
  assetForm.assetsStage = findOption(projectPhaseOptions, projectData.projectPhase)?.value || 1
  syncAssetFormRestrictions()
  resetExportState()
}

async function loadProjectMembers({ silent = false } = {}) {
  if (!canManageMembers.value) {
    members.value = []
    memberLoadError.value = ''
    membersLoading.value = false
    return
  }

  membersLoading.value = true
  memberLoadError.value = ''

  try {
    const data = await listProjectMembers(projectId.value)
    members.value = data || []
  } catch (error) {
    members.value = []
    memberLoadError.value = error.message

    if (!silent) {
      ElMessage.error(error.message)
    }
  } finally {
    membersLoading.value = false
  }
}

async function loadProjectDetail() {
  loading.value = true

  try {
    await loadProjectCore()
    if (canManageMembers.value) {
      await loadProjectMembers({ silent: true })
    } else {
      members.value = []
      memberLoadError.value = ''
    }
  } catch (error) {
    project.value = null
    assets.value = []
    members.value = []
    memberLoadError.value = ''
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function handleUpdatePhase() {
  if (!canAdvancePhase.value) {
    ElMessage.warning('Only the current phase owner can advance the project phase')
    return
  }

  phaseSubmitting.value = true

  try {
    await updateProjectPhase({
      projectId: projectId.value,
      newPhase: selectedPhase.value,
      nextOwnerId: nextOwnerId.value ? Number(nextOwnerId.value) : null,
    })
    ElMessage.success('Project phase updated')
    await loadProjectDetail()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    phaseSubmitting.value = false
  }
}

async function handleCreateAsset() {
  if (!canCreateSelectedAsset.value) {
    ElMessage.warning('Current account cannot create an asset with the selected phase or security level')
    return
  }

  assetSubmitting.value = true

  try {
    await createAsset({
      ...assetForm,
      fileSize: assetForm.fileSize ? Number(assetForm.fileSize) : null,
    })
    ElMessage.success('Asset created')
    resetAssetForm()
    await loadProjectDetail()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    assetSubmitting.value = false
  }
}

async function handleAddMember() {
  if (!canEditMembers.value) {
    ElMessage.warning('Only the current phase owner or management can maintain project members')
    return
  }

  memberSubmitting.value = true

  try {
    await addProjectMember(projectId.value, {
      employeeId: memberForm.employeeId ? Number(memberForm.employeeId) : null,
    })
    ElMessage.success('Project member added')
    resetMemberForm()
    await loadProjectMembers({ silent: true })
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    memberSubmitting.value = false
  }
}

async function handleExportAsset(assetId) {
  const asset = assets.value.find((item) => item.assetId === assetId)
  if (!canOperateAsset(asset)) {
    ElMessage.warning('Current account cannot export this asset reference')
    return
  }

  exportingAssetIds[assetId] = true

  try {
    const result = await exportAssetReference(assetId)
    exportedPaths[assetId] = result.filePath
    ElMessage.success('Reference exported')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    delete exportingAssetIds[assetId]
  }
}

async function handleDeleteAsset(assetId) {
  const asset = assets.value.find((item) => item.assetId === assetId)
  if (!canOperateAsset(asset)) {
    ElMessage.warning('Current account cannot delete this asset')
    return
  }

  try {
    await ElMessageBox.confirm(
      'Delete this asset? The asset record will be removed from the project.',
      'Delete Asset',
      {
        type: 'warning',
        confirmButtonText: 'Delete',
        cancelButtonText: 'Cancel',
      },
    )
  } catch {
    return
  }

  try {
    await deleteAsset(assetId)
    ElMessage.success('Asset deleted')
    await loadProjectDetail()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function handleRemoveMember(member) {
  if (!canEditMembers.value) {
    ElMessage.warning('Only the current phase owner or management can maintain project members')
    return
  }

  try {
    await ElMessageBox.confirm(
      `Remove ${member.employeeName || `Employee ${member.employeeId}`} from this project?`,
      'Remove Member',
      {
        type: 'warning',
        confirmButtonText: 'Remove',
        cancelButtonText: 'Cancel',
      },
    )
  } catch {
    return
  }

  try {
    await removeProjectMember(projectId.value, member.employeeId)
    ElMessage.success('Project member removed')
    await loadProjectMembers({ silent: true })
  } catch (error) {
    ElMessage.error(error.message)
  }
}

watch(
  projectId,
  () => {
    assetForm.projectId = projectId.value
    loadProjectDetail()
  },
  { immediate: true },
)
</script>

<template>
  <div class="detail-page">
    <section v-if="project" class="detail-hero">
      <div>
        <p class="detail-hero__eyebrow">Project Detail</p>
        <h2 class="detail-hero__title">{{ project.projectName }}</h2>
        <p class="detail-hero__description">
          This page brings project attributes, member isolation, phase handover, and asset
          controls into one demonstrable workflow.
        </p>
      </div>
      <div class="detail-hero__meta">
        <el-tag type="warning" effect="light" round>
          {{ getOptionLabel(projectPhaseOptions, project.projectPhase) }}
        </el-tag>
        <el-tag type="danger" effect="light" round>
          {{ getOptionLabel(securityLevelOptions, project.securityLevel) }}
        </el-tag>
      </div>
    </section>

    <section v-if="project" class="detail-summary">
      <el-card shadow="never" class="summary-card">
        <el-statistic title="Current Phase" :value="currentPhaseValue" suffix="/ 6" />
        <el-progress :percentage="phaseProgress" :stroke-width="10" :show-text="false" />
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="Active Members" :value="activeMembers.length" />
        <p v-if="canManageMembers">
          Only active project members can continue to operate on project-scoped resources.
        </p>
        <p v-else>
          Member details are visible only to the current phase owner and management.
        </p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="Visible Assets" :value="visibleAssetCount" />
        <p>The asset list already reflects the current ABAC decision result for this account.</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="Exported Refs" :value="exportedCount" />
        <p>Reference paths are revealed only after an explicit export action.</p>
      </el-card>
    </section>

    <section v-if="project" class="detail-grid">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>Project Snapshot</h3>
              <p>Core attributes that participate in project and asset access decisions.</p>
            </div>
          </div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="Project ID">{{ project.projectId }}</el-descriptions-item>
          <el-descriptions-item label="Current Owner">
            {{ project.ownerId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Created By">
            {{ project.createdByEmployeeId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Created At">
            {{ project.createdAt || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Project Phase">
            {{ getOptionLabel(projectPhaseOptions, project.projectPhase) }}
          </el-descriptions-item>
          <el-descriptions-item label="Security Level">
            {{ getOptionLabel(securityLevelOptions, project.securityLevel) }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>Project Members</h3>
              <p>Membership is the direct isolation layer for project pages and project assets.</p>
            </div>
            <el-space v-if="canManageMembers" wrap>
              <el-tag type="success" effect="light">Active {{ activeMembers.length }}</el-tag>
              <el-tag type="info" effect="light">History {{ inactiveMembers.length }}</el-tag>
              <el-button link @click="loadProjectMembers()">Refresh</el-button>
            </el-space>
          </div>
        </template>

        <div v-if="canManageMembers" class="member-toolbar">
          <el-form v-if="canEditMembers" label-position="top" class="member-form">
            <el-form-item label="Employee ID">
              <el-input
                v-model="memberForm.employeeId"
                placeholder="Enter an employee ID allowed in the current phase"
              />
            </el-form-item>
            <el-button type="primary" :loading="memberSubmitting" @click="handleAddMember">
              Add Member
            </el-button>
          </el-form>

          <el-alert
            v-else-if="isArchived"
            title="Member maintenance is locked after the project is archived."
            type="info"
            show-icon
            :closable="false"
          />
        </div>

        <el-alert
          v-if="!canManageMembers"
          title="Only the current phase owner or management can view project member details."
          type="info"
          show-icon
          :closable="false"
        />

        <el-alert
          v-else-if="memberLoadError"
          :title="memberLoadError"
          type="warning"
          show-icon
          :closable="false"
        />

        <el-table
          v-else-if="canManageMembers"
          v-loading="membersLoading"
          :data="members"
          stripe
          class="member-table"
        >
          <el-table-column label="Employee" min-width="190">
            <template #default="{ row }">
              <div class="member-cell">
                <strong>{{ row.employeeName || `Employee ${row.employeeId}` }}</strong>
                <span>{{ row.employeeCode || row.employeeId }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="Department" min-width="120">
            <template #default="{ row }">
              {{ getDeptLabel(row.deptType) }}
            </template>
          </el-table-column>
          <el-table-column label="Joined Phase" min-width="130">
            <template #default="{ row }">
              {{ getOptionLabel(projectPhaseOptions, row.joinedPhase) }}
            </template>
          </el-table-column>
          <el-table-column label="Status" min-width="120">
            <template #default="{ row }">
              <el-space wrap>
                <el-tag :type="getMemberStatusTagType(row.status)" effect="light">
                  {{ getMemberStatusLabel(row.status) }}
                </el-tag>
                <el-tag v-if="isCurrentOwner(row.employeeId)" type="warning" effect="light">
                  Phase Owner
                </el-tag>
              </el-space>
            </template>
          </el-table-column>
          <el-table-column prop="joinedAt" label="Joined At" min-width="170" />
          <el-table-column prop="leftAt" label="Left At" min-width="170" />
          <el-table-column v-if="canEditMembers" label="Action" width="140" fixed="right">
            <template #default="{ row }">
              <el-button
                type="danger"
                link
                :disabled="row.status !== 'ACTIVE' || isCurrentOwner(row.employeeId)"
                @click="handleRemoveMember(row)"
              >
                Remove
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty
          v-if="canManageMembers && !memberLoadError && !membersLoading && members.length === 0"
          description="No member record is available for this project."
        />
      </el-card>
    </section>

    <section v-if="project" class="detail-grid">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>Asset Type Distribution</h3>
              <p>Observe the current visible asset mix after project membership filtering.</p>
            </div>
            <el-tag type="warning" effect="light" round>
              Sensitive {{ sensitiveAssetCount }}
            </el-tag>
          </div>
        </template>
        <InsightChart :option="assetTypeChartOption" height="300px" />
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>Advance Project Phase</h3>
              <p>Phase handover will also sync project membership on the backend.</p>
            </div>
          </div>
        </template>

        <template v-if="canAdvancePhase">
          <el-alert
            title="The next owner must match the manager_id of the department responsible for the target phase."
            type="warning"
            show-icon
            :closable="false"
            class="panel-alert"
          />

          <el-form label-position="top">
            <el-form-item label="Target Phase">
              <el-select v-model="selectedPhase" placeholder="Select the target phase">
                <el-option
                  v-for="item in projectPhaseOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="Next Owner ID">
              <el-input
                v-model="nextOwnerId"
                placeholder="Enter the employee ID for the next phase owner"
              />
            </el-form-item>
          </el-form>

          <el-button type="primary" :loading="phaseSubmitting" @click="handleUpdatePhase">
            Submit Phase Change
          </el-button>
        </template>

        <el-alert
          v-else
          title="Only the current phase owner can advance the project phase."
          type="info"
          show-icon
          :closable="false"
          class="panel-alert"
        />
      </el-card>
    </section>

    <el-card v-if="project" shadow="never" class="panel-card">
      <template #header>
        <div class="panel-card__header">
          <div>
            <h3>Create Project Asset</h3>
            <p>
              Only stages and security levels still allowed for the current account remain
              selectable here.
            </p>
          </div>
        </div>
      </template>

      <el-alert
        v-if="!canCreateAnyAsset"
        title="Current account cannot create assets in this project under the active phase, membership, or security rules."
        type="info"
        show-icon
        :closable="false"
      />

      <template v-else>
        <el-form label-position="top" class="asset-form">
          <el-form-item label="Asset Name">
            <el-input v-model="assetForm.assetName" placeholder="Enter an asset name" />
          </el-form-item>
          <el-form-item label="Asset Type">
            <el-select v-model="assetForm.assetsType" placeholder="Select an asset type">
              <el-option
                v-for="item in assetTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="Asset Phase">
            <el-select v-model="assetForm.assetsStage" placeholder="Select an asset phase">
              <el-option
                v-for="item in allowedAssetStageOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="Security Level">
            <el-select v-model="assetForm.securityLevel" placeholder="Select a security level">
              <el-option
                v-for="item in allowedAssetSecurityOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="External Reference / Git URL">
            <el-input
              v-model="assetForm.filePath"
              placeholder="Enter a storage or repository path"
            />
          </el-form-item>
          <el-form-item label="File Size (bytes)">
            <el-input v-model="assetForm.fileSize" placeholder="Optional" />
          </el-form-item>
          <el-form-item label="Description" class="asset-form__full">
            <el-input
              v-model="assetForm.description"
              type="textarea"
              :rows="4"
              placeholder="Add a short note about this asset"
            />
          </el-form-item>
        </el-form>

        <el-button type="primary" :loading="assetSubmitting" @click="handleCreateAsset">
          Create Asset
        </el-button>
      </template>
    </el-card>

    <el-card v-if="project" shadow="never" class="table-card">
      <template #header>
        <div class="panel-card__header">
          <div>
            <h3>Project Assets</h3>
            <p>
              Regular reads return metadata only. The external reference appears after an explicit
              export action.
            </p>
          </div>
          <el-tag type="info" effect="light">Total {{ assets.length }}</el-tag>
        </div>
      </template>

      <el-table v-loading="loading" :data="assets" stripe>
        <el-table-column prop="assetName" label="Asset Name" min-width="180" />
        <el-table-column label="Type" min-width="140">
          <template #default="{ row }">
            {{ getOptionLabel(assetTypeOptions, row.assetsType) }}
          </template>
        </el-table-column>
        <el-table-column label="Phase" min-width="120">
          <template #default="{ row }">
            {{ getOptionLabel(projectPhaseOptions, row.assetsStage) }}
          </template>
        </el-table-column>
        <el-table-column label="Security" min-width="120">
          <template #default="{ row }">
            <el-tag effect="light">
              {{ getOptionLabel(securityLevelOptions, row.securityLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdByEmployeeId" label="Created By" min-width="110" />
        <el-table-column label="Reference" min-width="230">
          <template #default="{ row }">
            <span class="reference-text">{{ exportedPaths[row.assetId] || 'Hidden until export' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="Description" min-width="180" />
        <el-table-column label="Action" width="180" fixed="right">
          <template #default="{ row }">
            <el-space v-if="canOperateAsset(row)">
              <el-button
                type="primary"
                link
                :loading="Boolean(exportingAssetIds[row.assetId])"
                @click="handleExportAsset(row.assetId)"
              >
                Export Ref
              </el-button>
              <el-button type="danger" link @click="handleDeleteAsset(row.assetId)">
                Delete
              </el-button>
            </el-space>
            <span v-else class="reference-text">Read only</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!loading && assets.length === 0"
        description="No accessible asset is available under this project."
      />
    </el-card>
  </div>
</template>

<style scoped>
.detail-page {
  display: grid;
  gap: 18px;
}

.detail-hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  padding: 20px 24px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
  color: #303133;
}

.detail-hero__eyebrow {
  margin: 0 0 10px;
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.detail-hero__title {
  margin: 0 0 10px;
  font-size: 24px;
  line-height: 1.4;
}

.detail-hero__description {
  margin: 0;
  max-width: 760px;
  color: #606266;
  line-height: 1.7;
}

.detail-hero__meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.detail-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.summary-card,
.panel-card,
.table-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: none;
}

.summary-card :deep(.el-card__body) {
  display: grid;
  gap: 14px;
}

.summary-card p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.panel-card__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.panel-card__header h3 {
  margin: 0 0 6px;
  color: #111827;
  font-size: 20px;
}

.panel-card__header p {
  margin: 0;
  color: #64748b;
  line-height: 1.6;
}

.panel-alert,
.member-toolbar {
  margin-bottom: 18px;
}

.member-form,
.asset-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.member-form :deep(.el-form-item),
.asset-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.asset-form__full {
  grid-column: 1 / -1;
}

.member-form .el-button {
  align-self: end;
  min-height: 40px;
}

.member-table {
  width: 100%;
}

.member-cell {
  display: grid;
  gap: 4px;
}

.member-cell span,
.reference-text {
  color: #475569;
  word-break: break-all;
}

@media (max-width: 1180px) {
  .detail-summary,
  .detail-grid,
  .member-form,
  .asset-form {
    grid-template-columns: 1fr;
  }

  .asset-form__full {
    grid-column: auto;
  }
}

@media (max-width: 768px) {
  .detail-hero {
    flex-direction: column;
    padding: 22px;
  }
}
</style>
