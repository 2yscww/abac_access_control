<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import InsightChart from '@/components/InsightChart.vue'
import {
  createAsset,
  deleteAsset,
  exportAssetReference,
  getAssetsByProject,
} from '@/api/asset'
import { getProject, updateProjectPhase } from '@/api/project'
import {
  assetTypeOptions,
  findOption,
  getOptionLabel,
  projectPhaseOptions,
  securityLevelOptions,
} from '@/constants/options'

const route = useRoute()

const project = ref(null)
const assets = ref([])
const loading = ref(false)
const phaseSubmitting = ref(false)
const assetSubmitting = ref(false)
const selectedPhase = ref(1)
const nextOwnerId = ref('')

const exportedPaths = reactive({})
const exportingAssetIds = reactive({})

const assetForm = reactive({
  projectId: Number(route.params.id),
  assetName: '',
  assetsType: 1,
  assetsStage: 1,
  securityLevel: 2,
  filePath: '',
  fileSize: '',
  description: '',
})

const currentPhaseValue = computed(
  () => findOption(projectPhaseOptions, project.value?.projectPhase)?.value || 1,
)
const phaseProgress = computed(() => Math.round((currentPhaseValue.value / 6) * 100))
const sensitiveAssetCount = computed(
  () =>
    assets.value.filter((asset) => ['CONFIDENTIAL', 'TOP_SECRET', 3, 4].includes(asset.securityLevel))
      .length,
)
const exportedCount = computed(() => Object.keys(exportedPaths).length)

const assetTypeChartOption = computed(() => {
  const chartData = assetTypeOptions
    .map((option) => ({
      name: option.label,
      value: assets.value.filter((asset) => findOption(assetTypeOptions, asset.assetsType)?.key === option.key)
        .length,
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

function resetAssetForm() {
  assetForm.assetName = ''
  assetForm.assetsType = 1
  assetForm.assetsStage = currentPhaseValue.value
  assetForm.securityLevel = 2
  assetForm.filePath = ''
  assetForm.fileSize = ''
  assetForm.description = ''
}

async function loadProjectDetail() {
  loading.value = true

  try {
    const [projectData, assetData] = await Promise.all([
      getProject(route.params.id),
      getAssetsByProject(route.params.id),
    ])

    project.value = projectData
    assets.value = assetData || []
    selectedPhase.value = findOption(projectPhaseOptions, projectData.projectPhase)?.value || 1
    nextOwnerId.value = projectData.ownerId || ''
    assetForm.projectId = Number(route.params.id)
    assetForm.assetsStage = findOption(projectPhaseOptions, projectData.projectPhase)?.value || 1
    resetExportState()
  } catch (error) {
    project.value = null
    assets.value = []
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function handleUpdatePhase() {
  phaseSubmitting.value = true

  try {
    await updateProjectPhase({
      projectId: Number(route.params.id),
      newPhase: selectedPhase.value,
      nextOwnerId: nextOwnerId.value ? Number(nextOwnerId.value) : null,
    })
    ElMessage.success('项目阶段已更新')
    await loadProjectDetail()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    phaseSubmitting.value = false
  }
}

async function handleCreateAsset() {
  assetSubmitting.value = true

  try {
    await createAsset({
      ...assetForm,
      fileSize: assetForm.fileSize ? Number(assetForm.fileSize) : null,
    })
    ElMessage.success('资产创建成功')
    resetAssetForm()
    await loadProjectDetail()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    assetSubmitting.value = false
  }
}

async function handleExportAsset(assetId) {
  exportingAssetIds[assetId] = true

  try {
    const result = await exportAssetReference(assetId)
    exportedPaths[assetId] = result.filePath
    ElMessage.success('引用地址已加载')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    delete exportingAssetIds[assetId]
  }
}

async function handleDeleteAsset(assetId) {
  try {
    await ElMessageBox.confirm('确定删除这个资产吗？', '删除确认', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  try {
    await deleteAsset(assetId)
    ElMessage.success('资产已删除')
    await loadProjectDetail()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

watch(
  () => route.params.id,
  () => {
    loadProjectDetail()
  },
)

onMounted(loadProjectDetail)
</script>

<template>
  <div class="detail-page">
      <section v-if="project" class="detail-hero">
        <div>
          <p class="detail-hero__eyebrow">Project Detail</p>
          <h2 class="detail-hero__title">{{ project.projectName }}</h2>
          <p class="detail-hero__description">
            项目详情页承接阶段推进、负责人镜像和资产生命周期管理，是权限判断落到业务动作上的核心界面。
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

      <section class="detail-summary">
        <el-card shadow="never" class="summary-card">
          <el-statistic title="当前阶段" :value="currentPhaseValue" suffix="/ 6" />
          <el-progress :percentage="phaseProgress" :stroke-width="10" :show-text="false" />
        </el-card>
        <el-card shadow="never" class="summary-card">
          <el-statistic title="可见资产数" :value="assets.length" />
          <p>当前账号在该项目中可读取到的资产元数据。</p>
        </el-card>
        <el-card shadow="never" class="summary-card">
          <el-statistic title="高密资产" :value="sensitiveAssetCount" />
          <p>机密与绝密资产需要更严格的访问控制。</p>
        </el-card>
        <el-card shadow="never" class="summary-card">
          <el-statistic title="已导出引用" :value="exportedCount" />
          <p>普通读取不暴露地址，需显式执行导出动作。</p>
        </el-card>
      </section>

      <section v-if="project" class="detail-grid">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-card__header">
              <div>
                <h3>项目元数据</h3>
                <p>当前项目的阶段、密级与负责人快照。</p>
              </div>
            </div>
          </template>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目 ID">{{ project.projectId }}</el-descriptions-item>
            <el-descriptions-item label="当前负责人">{{ project.ownerId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建人">{{ project.createdByEmployeeId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ project.createdAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目阶段">
              {{ getOptionLabel(projectPhaseOptions, project.projectPhase) }}
            </el-descriptions-item>
            <el-descriptions-item label="项目密级">
              {{ getOptionLabel(securityLevelOptions, project.securityLevel) }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-card__header">
              <div>
                <h3>资产类型分布</h3>
                <p>从当前可见资产中观察文档、代码与脚本的占比。</p>
              </div>
              <el-tag type="warning" effect="light" round>Pie</el-tag>
            </div>
          </template>
          <InsightChart :option="assetTypeChartOption" height="300px" />
        </el-card>
      </section>

      <section v-if="project" class="detail-grid">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-card__header">
              <div>
                <h3>推进项目阶段</h3>
                <p>后端会继续按严格模式校验阶段负责人和推进权限。</p>
              </div>
            </div>
          </template>

          <el-alert
            title="严格模式下，能推进当前阶段的人必须等于该阶段主责部门的 manager_id。"
            type="warning"
            show-icon
            :closable="false"
            class="panel-alert"
          />

          <el-form label-position="top">
            <el-form-item label="目标阶段">
              <el-select v-model="selectedPhase" placeholder="请选择阶段">
                <el-option
                  v-for="item in projectPhaseOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="下一阶段负责人 ID">
              <el-input
                v-model="nextOwnerId"
                placeholder="请输入下一阶段负责人的员工 ID"
              />
            </el-form-item>
          </el-form>

          <el-button type="primary" :loading="phaseSubmitting" @click="handleUpdatePhase">
            提交阶段变更
          </el-button>
        </el-card>

        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-card__header">
              <div>
                <h3>新增项目资产</h3>
                <p>资产阶段不能超过项目当前阶段，安全校验由后端统一执行。</p>
              </div>
            </div>
          </template>

          <el-form label-position="top" class="asset-form">
            <el-form-item label="资产名称">
              <el-input v-model="assetForm.assetName" placeholder="请输入资产名称" />
            </el-form-item>
            <el-form-item label="资产类型">
              <el-select v-model="assetForm.assetsType" placeholder="请选择资产类型">
                <el-option
                  v-for="item in assetTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="资产阶段">
              <el-select v-model="assetForm.assetsStage" placeholder="请选择资产阶段">
                <el-option
                  v-for="item in projectPhaseOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="资产密级">
              <el-select v-model="assetForm.securityLevel" placeholder="请选择资产密级">
                <el-option
                  v-for="item in securityLevelOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="外部引用地址 / Git URL">
              <el-input v-model="assetForm.filePath" placeholder="请输入存储地址或仓库地址" />
            </el-form-item>
            <el-form-item label="文件大小（字节）">
              <el-input v-model="assetForm.fileSize" placeholder="可选" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input
                v-model="assetForm.description"
                type="textarea"
                :rows="4"
                placeholder="补充资产说明"
              />
            </el-form-item>
          </el-form>

          <el-button type="primary" :loading="assetSubmitting" @click="handleCreateAsset">
            提交资产
          </el-button>
        </el-card>
      </section>

      <el-card v-if="project" shadow="never" class="table-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>项目资产列表</h3>
              <p>普通读取只返回元数据，引用地址需要显式导出后才会显示。</p>
            </div>
            <el-tag type="info" effect="light">共 {{ assets.length }} 条</el-tag>
          </div>
        </template>

        <el-table v-loading="loading" :data="assets" stripe>
          <el-table-column prop="assetName" label="资产名称" min-width="180" />
          <el-table-column label="类型" min-width="130">
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
          <el-table-column prop="createdByEmployeeId" label="创建人" min-width="100" />
          <el-table-column label="引用地址" min-width="220">
            <template #default="{ row }">
              <span class="reference-text">{{ exportedPaths[row.assetId] || '尚未导出' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="180" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-space>
                <el-button
                  type="primary"
                  link
                  :loading="Boolean(exportingAssetIds[row.assetId])"
                  @click="handleExportAsset(row.assetId)"
                >
                  导出引用
                </el-button>
                <el-button type="danger" link @click="handleDeleteAsset(row.assetId)">
                  删除
                </el-button>
              </el-space>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!loading && assets.length === 0" description="当前项目下没有可访问资产" />
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
  grid-template-columns: 0.95fr 1.05fr;
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

.panel-alert {
  margin-bottom: 18px;
}

.asset-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.reference-text {
  color: #475569;
  word-break: break-all;
}

@media (max-width: 1180px) {
  .detail-summary,
  .detail-grid,
  .asset-form {
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
