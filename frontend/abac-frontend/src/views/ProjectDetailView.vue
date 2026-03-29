<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import InsightChart from '@/components/InsightChart.vue'
import {
  deleteAsset,
  exportAssetReference,
  getAssetsByProject,
} from '@/api/asset'
import {
  addProjectMember,
  getProject,
  getPhaseOwnerPreview,
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
  canDeleteAsset as canDeleteAssetByRules,
  canExportAsset as canExportAssetByRules,
} from '@/utils/accessControl'

const route = useRoute()
const authStore = useAuthStore()

const deptTypeLabels = {
  MANAGEMENT: '管理层',
  PRODUCT: '产品',
  RD: '研发',
  QA: '测试',
  OPS: '运维',
  HR: '人事',
}

const memberStatusLabels = {
  ACTIVE: '在项',
  INACTIVE: '离项',
}

const projectId = computed(() => Number(route.params.id))
const project = ref(null)
const assets = ref([])
const members = ref([])

const loading = ref(false)
const membersLoading = ref(false)
const phaseSubmitting = ref(false)
const memberSubmitting = ref(false)
const selectedPhase = ref(null)
const phaseOwnerPreview = ref(null)
const phaseOwnerLoading = ref(false)
const memberLoadError = ref('')

const exportedPaths = reactive({})
const exportingAssetIds = reactive({})

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
const advancePhaseOptions = computed(() => {
  const currentOption = findOption(projectPhaseOptions, project.value?.projectPhase)
  if (!currentOption) {
    return []
  }

  return projectPhaseOptions.filter((option) => option.value === currentOption.value + 1)
})
const canSubmitPhaseChange = computed(
  () =>
    canAdvancePhase.value &&
    advancePhaseOptions.value.length > 0 &&
    selectedPhase.value != null &&
    Boolean(phaseOwnerPreview.value?.configured) &&
    !phaseOwnerLoading.value,
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
                  name: '暂无资产',
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

function canExportAsset(asset) {
  return canExportAssetByRules(currentProfile.value, project.value, asset, {
    assumeActiveMember: assumeProjectMembership.value,
    currentEmployeeId: currentEmployeeId.value,
  })
}

function canDeleteAsset(asset) {
  return canDeleteAssetByRules(currentProfile.value, project.value, asset, {
    assumeActiveMember: assumeProjectMembership.value,
    currentEmployeeId: currentEmployeeId.value,
  })
}

function getNextPhaseValue(rawPhase) {
  const currentOption = findOption(projectPhaseOptions, rawPhase)
  return currentOption
    ? (projectPhaseOptions.find((option) => option.value === currentOption.value + 1)?.value ?? null)
    : null
}

async function loadPhaseOwnerPreview({ silent = false } = {}) {
  if (!project.value || !canAdvancePhase.value || selectedPhase.value == null) {
    phaseOwnerPreview.value = null
    phaseOwnerLoading.value = false
    return
  }

  phaseOwnerLoading.value = true

  try {
    phaseOwnerPreview.value = await getPhaseOwnerPreview(projectId.value, selectedPhase.value)
  } catch (error) {
    phaseOwnerPreview.value = null
    if (!silent) {
      ElMessage.error(error.message)
    }
  } finally {
    phaseOwnerLoading.value = false
  }
}

async function loadProjectCore() {
  const [projectData, assetData] = await Promise.all([
    getProject(projectId.value),
    getAssetsByProject(projectId.value),
  ])

  project.value = projectData
  assets.value = assetData || []
  selectedPhase.value = getNextPhaseValue(projectData.projectPhase)
  phaseOwnerPreview.value = null
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
    if (canAdvancePhase.value && selectedPhase.value != null) {
      await loadPhaseOwnerPreview({ silent: true })
    }
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
    ElMessage.warning('只有当前阶段负责人可以推进项目阶段')
    return
  }
  if (!phaseOwnerPreview.value?.configured) {
    ElMessage.warning(
      phaseOwnerPreview.value?.message || '严格模式下无法解析下一阶段负责人',
    )
    return
  }

  phaseSubmitting.value = true

  try {
    await updateProjectPhase({
      projectId: projectId.value,
      newPhase: selectedPhase.value,
    })
    ElMessage.success('项目阶段更新成功')
    await loadProjectDetail()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    phaseSubmitting.value = false
  }
}

async function handleAddMember() {
  if (!canEditMembers.value) {
    ElMessage.warning('只有当前阶段负责人或管理层可以维护项目成员')
    return
  }

  memberSubmitting.value = true

  try {
    await addProjectMember(projectId.value, {
      employeeId: memberForm.employeeId ? Number(memberForm.employeeId) : null,
    })
    ElMessage.success('项目成员添加成功')
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
  if (!canExportAsset(asset)) {
    ElMessage.warning('当前账号不能导出该资产引用')
    return
  }

  exportingAssetIds[assetId] = true

  try {
    const result = await exportAssetReference(assetId)
    exportedPaths[assetId] = result.filePath
    ElMessage.success('引用导出成功')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    delete exportingAssetIds[assetId]
  }
}

async function handleDeleteAsset(assetId) {
  const asset = assets.value.find((item) => item.assetId === assetId)
  if (!canDeleteAsset(asset)) {
    ElMessage.warning('当前账号不能删除该资产')
    return
  }

  try {
    await ElMessageBox.confirm(
      '确认删除该资产吗？该资产记录会从项目中移除。',
      '删除资产',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }

  try {
    await deleteAsset(assetId)
    ElMessage.success('资产删除成功')
    await loadProjectDetail()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function handleRemoveMember(member) {
  if (!canEditMembers.value) {
    ElMessage.warning('只有当前阶段负责人或管理层可以维护项目成员')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认将 ${member.employeeName || `员工 ${member.employeeId}`} 移出该项目吗？`,
      '移除成员',
      {
        type: 'warning',
        confirmButtonText: '确认移除',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }

  try {
    await removeProjectMember(projectId.value, member.employeeId)
    ElMessage.success('项目成员移除成功')
    await loadProjectMembers({ silent: true })
  } catch (error) {
    ElMessage.error(error.message)
  }
}

watch(
  projectId,
  () => {
    loadProjectDetail()
  },
  { immediate: true },
)

watch(selectedPhase, () => {
  loadPhaseOwnerPreview({ silent: true })
})
</script>

<template>
  <div class="detail-page">
    <section v-if="project" class="detail-hero">
      <div>
        <p class="detail-hero__eyebrow">项目详情</p>
        <h2 class="detail-hero__title">{{ project.projectName }}</h2>
        <p class="detail-hero__description">
          当前页面将项目属性、成员隔离、阶段交接和资产控制整合到一个可演示的完整流程中。
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
        <el-statistic title="当前阶段" :value="currentPhaseValue" suffix="/ 6" />
        <el-progress :percentage="phaseProgress" :stroke-width="10" :show-text="false" />
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="在项成员" :value="activeMembers.length" />
        <p v-if="canManageMembers">
          只有状态为在项的项目成员，才能继续操作项目范围内的资源。
        </p>
        <p v-else>
          成员详情仅对当前阶段负责人和管理层可见。
        </p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="可见资产" :value="visibleAssetCount" />
        <p>当前资产列表已经体现了该账号此刻的 ABAC 决策结果。</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="已导出引用" :value="exportedCount" />
        <p>只有显式执行导出动作后，外部引用路径才会显示出来。</p>
      </el-card>
    </section>

    <section v-if="project" class="detail-grid">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>项目快照</h3>
              <p>参与项目与资产访问决策的核心属性会集中展示在这里。</p>
            </div>
          </div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="项目 ID">{{ project.projectId }}</el-descriptions-item>
          <el-descriptions-item label="当前负责人">
            {{ project.ownerId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建人">
            {{ project.createdByEmployeeId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ project.createdAt || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="项目阶段">
            {{ getOptionLabel(projectPhaseOptions, project.projectPhase) }}
          </el-descriptions-item>
          <el-descriptions-item label="安全密级">
            {{ getOptionLabel(securityLevelOptions, project.securityLevel) }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>项目成员</h3>
              <p>成员关系是项目页面和项目资产访问隔离的直接依据。</p>
            </div>
            <el-space v-if="canManageMembers" wrap>
              <el-tag type="success" effect="light">在项 {{ activeMembers.length }}</el-tag>
              <el-tag type="info" effect="light">历史 {{ inactiveMembers.length }}</el-tag>
              <el-button link @click="loadProjectMembers()">刷新</el-button>
            </el-space>
          </div>
        </template>

        <div v-if="canManageMembers" class="member-toolbar">
          <el-form v-if="canEditMembers" label-position="top" class="member-form">
            <el-form-item label="员工 ID">
              <el-input
                v-model="memberForm.employeeId"
                placeholder="请输入当前阶段允许加入的员工 ID"
              />
            </el-form-item>
            <el-button type="primary" :loading="memberSubmitting" @click="handleAddMember">
              添加成员
            </el-button>
          </el-form>

          <el-alert
            v-else-if="isArchived"
            title="项目归档后，成员维护入口会被锁定。"
            type="info"
            show-icon
            :closable="false"
          />
        </div>

        <el-alert
          v-if="!canManageMembers"
          title="只有当前阶段负责人或管理层可以查看项目成员详情。"
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
          <el-table-column label="成员" min-width="190">
            <template #default="{ row }">
              <div class="member-cell">
                <strong>{{ row.employeeName || `员工 ${row.employeeId}` }}</strong>
                <span>{{ row.employeeCode || row.employeeId }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="所属部门" min-width="120">
            <template #default="{ row }">
              {{ getDeptLabel(row.deptType) }}
            </template>
          </el-table-column>
          <el-table-column label="加入阶段" min-width="130">
            <template #default="{ row }">
              {{ getOptionLabel(projectPhaseOptions, row.joinedPhase) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" min-width="120">
            <template #default="{ row }">
              <el-space wrap>
                <el-tag :type="getMemberStatusTagType(row.status)" effect="light">
                  {{ getMemberStatusLabel(row.status) }}
                </el-tag>
                <el-tag v-if="isCurrentOwner(row.employeeId)" type="warning" effect="light">
                  阶段负责人
                </el-tag>
              </el-space>
            </template>
          </el-table-column>
          <el-table-column prop="joinedAt" label="加入时间" min-width="170" />
          <el-table-column prop="leftAt" label="离开时间" min-width="170" />
          <el-table-column v-if="canEditMembers" label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button
                type="danger"
                link
                :disabled="row.status !== 'ACTIVE' || isCurrentOwner(row.employeeId)"
                @click="handleRemoveMember(row)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty
          v-if="canManageMembers && !memberLoadError && !membersLoading && members.length === 0"
          description="该项目暂无成员记录"
        />
      </el-card>
    </section>

    <section v-if="project" class="detail-grid">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>资产类型分布</h3>
              <p>查看成员过滤后，当前可见资产在各类型上的分布情况。</p>
            </div>
            <el-tag type="warning" effect="light" round>
              高密 {{ sensitiveAssetCount }}
            </el-tag>
          </div>
        </template>
        <InsightChart :option="assetTypeChartOption" height="300px" />
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>推进项目阶段</h3>
              <p>阶段交接时，后端会同步处理项目成员与负责人映射。</p>
            </div>
          </div>
        </template>

        <template v-if="canAdvancePhase && advancePhaseOptions.length > 0">
          <el-alert
            title="严格模式会根据目标阶段所属部门的负责人自动解析下一任负责人，不允许手工录入。"
            type="warning"
            show-icon
            :closable="false"
            class="panel-alert"
          />

          <el-form label-position="top">
            <el-form-item label="目标阶段">
              <el-select v-model="selectedPhase" placeholder="请选择目标阶段">
                <el-option
                  v-for="item in advancePhaseOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="严格模式下一任负责人">
              <el-skeleton v-if="phaseOwnerLoading" :rows="2" animated />
              <el-descriptions v-else-if="phaseOwnerPreview?.configured" :column="1" border>
                <el-descriptions-item label="人员">
                  <div class="phase-owner-card">
                    <strong>{{ phaseOwnerPreview.employeeName || '-' }}</strong>
                    <span>
                      {{ phaseOwnerPreview.employeeCode || phaseOwnerPreview.employeeId || '-' }}
                    </span>
                  </div>
                </el-descriptions-item>
                <el-descriptions-item label="部门">
                  {{ phaseOwnerPreview.deptName || phaseOwnerPreview.deptType || '-' }}
                </el-descriptions-item>
              </el-descriptions>
              <el-alert
                v-else
                :title="phaseOwnerPreview?.message || '请选择目标阶段以解析下一任负责人。'"
                type="warning"
                show-icon
                :closable="false"
              />
            </el-form-item>
          </el-form>

          <el-button
            type="primary"
            :loading="phaseSubmitting"
            :disabled="!canSubmitPhaseChange"
            @click="handleUpdatePhase"
          >
            提交阶段变更
          </el-button>
        </template>

        <el-alert
          v-else-if="canAdvancePhase"
          title="当前已经是终止阶段，无法继续执行严格模式交接。"
          type="info"
          show-icon
          :closable="false"
          class="panel-alert"
        />

        <el-alert
          v-else
          title="只有当前阶段负责人可以推进项目阶段。"
          type="info"
          show-icon
          :closable="false"
          class="panel-alert"
        />
      </el-card>
    </section>

    <el-card v-if="project" shadow="never" class="table-card">
      <template #header>
        <div class="panel-card__header">
          <div>
            <h3>项目资产</h3>
            <p>
              默认读取只返回元数据；外部引用需在显式执行导出后才会显示。
            </p>
          </div>
          <el-tag type="info" effect="light">共 {{ assets.length }} 条</el-tag>
        </div>
      </template>

      <el-table v-loading="loading" :data="assets" stripe>
        <el-table-column prop="assetName" label="资产名称" min-width="180" />
        <el-table-column label="类型" min-width="140">
          <template #default="{ row }">
            {{ getOptionLabel(assetTypeOptions, row.assetsType) }}
          </template>
        </el-table-column>
        <el-table-column label="阶段" min-width="120">
          <template #default="{ row }">
            {{ getOptionLabel(projectPhaseOptions, row.assetsStage) }}
          </template>
        </el-table-column>
        <el-table-column label="密级" min-width="120">
          <template #default="{ row }">
            <el-tag effect="light">
              {{ getOptionLabel(securityLevelOptions, row.securityLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdByEmployeeId" label="创建人" min-width="110" />
        <el-table-column label="引用地址" min-width="230">
          <template #default="{ row }">
            <span class="reference-text">{{ exportedPaths[row.assetId] || '导出后可见' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-space v-if="canExportAsset(row) || canDeleteAsset(row)">
              <el-button
                v-if="canExportAsset(row)"
                type="primary"
                link
                :loading="Boolean(exportingAssetIds[row.assetId])"
                @click="handleExportAsset(row.assetId)"
              >
                导出引用
              </el-button>
              <el-button
                v-if="canDeleteAsset(row)"
                type="danger"
                link
                @click="handleDeleteAsset(row.assetId)"
              >
                删除
              </el-button>
            </el-space>
            <span v-else class="reference-text">只读</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!loading && assets.length === 0"
        description="该项目下暂无当前账号可访问的资产"
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

.member-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.member-form :deep(.el-form-item) {
  margin-bottom: 0;
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

.phase-owner-card {
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
  .member-form {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .detail-hero {
    flex-direction: column;
    padding: 22px;
  }
}
</style>
