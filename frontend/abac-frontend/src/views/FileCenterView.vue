<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

import InsightChart from '@/components/InsightChart.vue'
import { exportAssetReference, queryAssets, uploadAsset } from '@/api/asset'
import { getProject } from '@/api/project'
import { assetTypeOptions, getOptionLabel, projectPhaseOptions, securityLevelOptions } from '@/constants/options'
import { useAuthStore } from '@/stores/auth'
import { getAllowedAssetTypeOptions } from '@/utils/accessControl'

const authStore = useAuthStore()

const uploadForm = reactive({
  projectId: '',
  assetName: '',
  assetsType: 1,
  securityLevel: 2,
  description: '',
})

const fileInputRef = ref(null)
const selectedFile = ref(null)
const loading = ref(false)
const uploadSubmitting = ref(false)
const myUploads = ref([])
const downloadableAssets = ref([])
const downloadingAssetIds = reactive({})
const syncedProjectId = ref(null)
const resolvedProjectPhase = ref(null)

const draftCount = computed(() => myUploads.value.length)
const downloadCount = computed(() => downloadableAssets.value.length)
const uploadCapabilityLabel = computed(() =>
  authStore.hasCapability('files.upload') ? '已开放' : '未开放',
)
const downloadCapabilityLabel = computed(() =>
  authStore.hasCapability('files.download') ? '已开放' : '未开放',
)
const selectedFileName = computed(() => selectedFile.value?.name || '')
const selectedFileSize = computed(() => formatBytes(selectedFile.value?.size))
const resolvedProjectPhaseLabel = computed(() => {
  if (resolvedProjectPhase.value == null) {
    return '根据项目当前阶段自动确定'
  }

  return getOptionLabel(projectPhaseOptions, resolvedProjectPhase.value)
})
const allowedUploadAssetTypeOptions = computed(() =>
  getAllowedAssetTypeOptions(authStore.profile),
)
const hasAllowedUploadAssetType = computed(
  () => allowedUploadAssetTypeOptions.value.length > 0,
)

const chartOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'shadow',
    },
  },
  grid: {
    top: 18,
    right: 18,
    bottom: 28,
    left: 36,
  },
  xAxis: {
    type: 'category',
    data: ['我的上传', '可下载资产', '上传能力', '下载能力'],
    axisTick: {
      show: false,
    },
    axisLine: {
      lineStyle: {
        color: '#dcdfe6',
      },
    },
    axisLabel: {
      color: '#606266',
    },
  },
  yAxis: {
    type: 'value',
    axisLabel: {
      color: '#909399',
    },
    splitLine: {
      lineStyle: {
        color: '#ebeef5',
      },
    },
  },
  series: [
    {
      type: 'bar',
      barWidth: 30,
      data: [
        myUploads.value.length,
        downloadableAssets.value.length,
        authStore.hasCapability('files.upload') ? 1 : 0,
        authStore.hasCapability('files.download') ? 1 : 0,
      ],
      itemStyle: {
        color(params) {
          const palette = ['#409eff', '#67c23a', '#e6a23c', '#909399']
          return palette[params.dataIndex % palette.length]
        },
        borderRadius: [4, 4, 0, 0],
      },
    },
  ],
}))

function formatBytes(value) {
  if (!Number.isFinite(value) || value <= 0) {
    return ''
  }

  const units = ['B', 'KB', 'MB', 'GB']
  let size = value
  let index = 0

  while (size >= 1024 && index < units.length - 1) {
    size /= 1024
    index += 1
  }

  const digits = index === 0 ? 0 : 2
  return `${size.toFixed(digits)} ${units[index]}`
}

function resetUploadForm() {
  uploadForm.projectId = ''
  uploadForm.assetName = ''
  uploadForm.assetsType = allowedUploadAssetTypeOptions.value[0]?.value ?? null
  uploadForm.securityLevel = 2
  uploadForm.description = ''
  selectedFile.value = null
  syncedProjectId.value = null
  resolvedProjectPhase.value = null

  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

function syncUploadAssetTypeSelection() {
  if (
    allowedUploadAssetTypeOptions.value.some(
      (option) => option.value === uploadForm.assetsType,
    )
  ) {
    return
  }

  uploadForm.assetsType = allowedUploadAssetTypeOptions.value[0]?.value ?? null
}

function handleFileChange(event) {
  const file = event.target.files?.[0] || null
  selectedFile.value = file

  if (file && !uploadForm.assetName) {
    uploadForm.assetName = file.name
  }
}

async function handleProjectIdChange() {
  resolvedProjectPhase.value = null
  syncedProjectId.value = null

  try {
    await syncAssetStageWithProject()
  } catch {
    // Surface project lookup failures during the actual upload action instead.
  }
}

async function syncAssetStageWithProject() {
  const projectId = Number(uploadForm.projectId)
  if (!Number.isFinite(projectId) || projectId <= 0) {
    resolvedProjectPhase.value = null
    return
  }
  if (syncedProjectId.value === projectId) {
    return
  }

  const project = await getProject(projectId)
  const matchedPhase = projectPhaseOptions.find((item) => item.key === project?.projectPhase)

  resolvedProjectPhase.value = matchedPhase?.value ?? null
  syncedProjectId.value = projectId
}

async function loadFileCenterData() {
  loading.value = true

  try {
    const currentEmployeeId = Number(authStore.profile?.employeeId || authStore.employeeId || 0)
    const [uploadData, downloadData] = await Promise.all([
      queryAssets({
        createdByEmployeeId: currentEmployeeId || undefined,
        pageNum: 1,
        pageSize: 8,
      }),
      queryAssets({
        pageNum: 1,
        pageSize: 8,
      }),
    ])

    myUploads.value = uploadData?.list || []
    downloadableAssets.value = downloadData?.list || []
  } catch (error) {
    myUploads.value = []
    downloadableAssets.value = []
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function handleUpload() {
  if (!authStore.hasCapability('files.upload')) {
    ElMessage.warning('当前账号没有文件上传能力')
    return
  }
  if (!hasAllowedUploadAssetType.value || !uploadForm.assetsType) {
    ElMessage.warning('当前账号按部门职责没有可创建的资产类型')
    return
  }
  if (!uploadForm.projectId || !uploadForm.assetName) {
    ElMessage.warning('请先填写项目 ID 和文件名称')
    return
  }
  if (!selectedFile.value) {
    ElMessage.warning('请选择要上传的本地文件')
    return
  }

  uploadSubmitting.value = true

  try {
    await syncAssetStageWithProject()

    await uploadAsset(
      {
        projectId: Number(uploadForm.projectId),
        assetName: uploadForm.assetName,
        assetsType: uploadForm.assetsType,
        securityLevel: uploadForm.securityLevel,
        description: uploadForm.description,
      },
      selectedFile.value,
    )

    ElMessage.success('文件已上传到 MinIO，并完成资产建档')
    resetUploadForm()
    await loadFileCenterData()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    uploadSubmitting.value = false
  }
}

async function handleDownload(asset) {
  if (!authStore.hasCapability('files.download')) {
    ElMessage.warning('当前账号没有文件下载能力')
    return
  }

  downloadingAssetIds[asset.assetId] = true

  try {
    const result = await exportAssetReference(asset.assetId)
    const downloadUrl = result.downloadUrl || result.filePath

    if (!downloadUrl) {
      throw new Error('后端没有返回可用的下载地址')
    }

    window.open(downloadUrl, '_blank', 'noopener')
    ElMessage.success('已生成受控下载链接')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    delete downloadingAssetIds[asset.assetId]
  }
}

onMounted(loadFileCenterData)

watch(allowedUploadAssetTypeOptions, syncUploadAssetTypeSelection, { immediate: true })
</script>

<template>
  <div class="files-page">
    <section class="files-hero">
      <div>
        <p class="files-hero__eyebrow">文件中心</p>
        <h2 class="files-hero__title">MinIO 文件中心</h2>
        <p class="files-hero__description">
          文件内容现在存储在对象存储中，MySQL 继续保存资产业务数据与存储引用。上传会写入
          MinIO，下载则通过后端受控导出临时链接，继续复用现有 ABAC 与审计边界。
        </p>
      </div>
      <el-alert
        title="资产创建入口统一收口到文件中心，创建阶段由项目当前阶段自动决定。"
        type="info"
        :closable="false"
        show-icon
      />
    </section>

    <section class="files-summary">
      <el-card shadow="never" class="summary-card">
        <el-statistic title="我的最近上传" :value="draftCount" />
        <p>当前账号最近创建的资产记录数量。</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="可下载资产" :value="downloadCount" />
        <p>当前账号可见且可发起导出下载的资产数量。</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <strong>{{ uploadCapabilityLabel }}</strong>
        <p>上传能力：{{ authStore.hasCapability('files.upload') ? '可用' : '受限' }}</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <strong>{{ downloadCapabilityLabel }}</strong>
        <p>下载能力：{{ authStore.hasCapability('files.download') ? '可用' : '受限' }}</p>
      </el-card>
    </section>

    <section class="files-grid">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>上传文件</h3>
              <p>填写业务元数据并选择本地文件，后端会先上传到 MinIO，再写入资产记录。</p>
            </div>
          </div>
        </template>

        <el-form label-position="top" class="upload-form">
          <el-alert
            v-if="!hasAllowedUploadAssetType"
            title="当前账号按部门职责没有可创建的资产类型。管理层仍可继续使用全阶段下载能力。"
            type="info"
            show-icon
            :closable="false"
          />
          <el-form-item label="项目 ID">
            <el-input
              v-model="uploadForm.projectId"
              placeholder="请输入项目 ID"
              @change="handleProjectIdChange"
            />
          </el-form-item>
          <el-form-item label="文件名称">
            <el-input v-model="uploadForm.assetName" placeholder="请输入文件名称" />
          </el-form-item>
          <el-form-item label="资产类型">
            <el-select v-model="uploadForm.assetsType" :disabled="!hasAllowedUploadAssetType">
              <el-option
                v-for="item in allowedUploadAssetTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="所属阶段">
            <el-input :model-value="resolvedProjectPhaseLabel" readonly />
          </el-form-item>
          <el-form-item label="文件密级">
            <el-select v-model="uploadForm.securityLevel">
              <el-option
                v-for="item in securityLevelOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="本地文件">
            <div class="files-page__picker">
              <input ref="fileInputRef" type="file" class="files-page__input" @change="handleFileChange" />
              <el-tag v-if="selectedFileName" effect="plain">
                {{ selectedFileName }}<span v-if="selectedFileSize"> / {{ selectedFileSize }}</span>
              </el-tag>
              <span v-else class="files-page__placeholder">尚未选择本地文件</span>
            </div>
          </el-form-item>
          <el-form-item label="文件说明">
            <el-input
              v-model="uploadForm.description"
              type="textarea"
              :rows="4"
              placeholder="补充文件用途、来源或审批说明"
            />
          </el-form-item>
        </el-form>

        <div class="upload-form__actions">
          <el-button
            type="primary"
            :loading="uploadSubmitting"
            :disabled="!hasAllowedUploadAssetType"
            @click="handleUpload"
          >
            上传到 MinIO
          </el-button>
          <el-button @click="resetUploadForm">重置</el-button>
        </div>
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>接入概览</h3>
              <p>展示文件中心的真实上传、可见资产和当前能力状态。</p>
            </div>
          </div>
        </template>
        <InsightChart :option="chartOption" height="320px" />
      </el-card>
    </section>

    <section class="files-grid">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>我的最近上传</h3>
              <p>展示当前账号最近创建的资产元数据，文件引用默认仍保持隐藏。</p>
            </div>
            <el-button plain :loading="loading" @click="loadFileCenterData">刷新</el-button>
          </div>
        </template>

        <el-table v-loading="loading" :data="myUploads" stripe>
          <el-table-column prop="projectId" label="项目 ID" min-width="90" />
          <el-table-column prop="assetName" label="文件名称" min-width="180" />
          <el-table-column label="类型" min-width="120">
            <template #default="{ row }">
              {{ getOptionLabel(assetTypeOptions, row.assetsType) }}
            </template>
          </el-table-column>
          <el-table-column label="阶段" min-width="110">
            <template #default="{ row }">
              {{ getOptionLabel(projectPhaseOptions, row.assetsStage) }}
            </template>
          </el-table-column>
          <el-table-column label="密级" min-width="110">
            <template #default="{ row }">
              {{ getOptionLabel(securityLevelOptions, row.securityLevel) }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" min-width="180" />
        </el-table>

        <el-empty v-if="!loading && myUploads.length === 0" description="当前还没有上传记录" />
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>下载入口</h3>
              <p>点击后调用受控导出接口，由后端返回 MinIO 临时下载地址。</p>
            </div>
          </div>
        </template>

        <el-table v-loading="loading" :data="downloadableAssets" stripe>
          <el-table-column prop="assetName" label="文件名称" min-width="180" />
          <el-table-column prop="projectId" label="项目 ID" min-width="100" />
          <el-table-column label="密级" min-width="110">
            <template #default="{ row }">
              {{ getOptionLabel(securityLevelOptions, row.securityLevel) }}
            </template>
          </el-table-column>
          <el-table-column prop="createdByEmployeeId" label="创建人" min-width="100" />
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                :loading="Boolean(downloadingAssetIds[row.assetId])"
                @click="handleDownload(row)"
              >
                下载文件
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!loading && downloadableAssets.length === 0" description="当前没有可下载资产" />
      </el-card>
    </section>
  </div>
</template>

<style scoped>
.files-page {
  display: grid;
  gap: 18px;
}

.files-hero {
  display: grid;
  gap: 16px;
  padding: 20px 24px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
}

.files-hero__eyebrow {
  margin: 0 0 8px;
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.files-hero__title {
  margin: 0 0 10px;
  color: #303133;
  font-size: 24px;
}

.files-hero__description {
  margin: 0;
  color: #606266;
  line-height: 1.7;
}

.files-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-card,
.panel-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: none;
}

.summary-card p {
  margin: 12px 0 0;
  color: #909399;
  line-height: 1.7;
}

.summary-card strong {
  font-size: 26px;
  color: #303133;
}

.files-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.panel-card__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.panel-card__header h3 {
  margin: 0 0 6px;
  font-size: 18px;
  color: #303133;
}

.panel-card__header p {
  margin: 0;
  color: #909399;
  line-height: 1.6;
}

.upload-form {
  display: grid;
  gap: 4px;
}

.upload-form__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.files-page__picker {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.files-page__placeholder {
  color: #909399;
}

.files-page__input {
  width: 100%;
  max-width: 260px;
}

@media (max-width: 1180px) {
  .files-summary,
  .files-grid {
    grid-template-columns: 1fr;
  }
}
</style>
