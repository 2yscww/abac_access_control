<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'

import MainShell from '@/components/MainShell.vue'
import { createAsset, deleteAsset, getAssetsByProject } from '@/api/asset'
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
const errorMessage = ref('')
const successMessage = ref('')
const selectedPhase = ref(1)

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

async function loadProjectDetail() {
  loading.value = true
  errorMessage.value = ''

  try {
    const [projectData, assetData] = await Promise.all([
      getProject(route.params.id),
      getAssetsByProject(route.params.id),
    ])

    project.value = projectData
    assets.value = assetData || []
    selectedPhase.value = findOption(projectPhaseOptions, projectData.projectPhase)?.value || 1
    assetForm.assetsStage = selectedPhase.value
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

async function handleUpdatePhase() {
  try {
    await updateProjectPhase({
      projectId: Number(route.params.id),
      newPhase: selectedPhase.value,
    })
    successMessage.value = '项目阶段已更新。'
    await loadProjectDetail()
  } catch (error) {
    errorMessage.value = error.message
  }
}

async function handleCreateAsset() {
  try {
    await createAsset({
      ...assetForm,
      fileSize: assetForm.fileSize ? Number(assetForm.fileSize) : null,
    })
    successMessage.value = '资产创建成功。'
    assetForm.assetName = ''
    assetForm.filePath = ''
    assetForm.fileSize = ''
    assetForm.description = ''
    await loadProjectDetail()
  } catch (error) {
    errorMessage.value = error.message
  }
}

async function handleDeleteAsset(assetId) {
  if (!window.confirm('确定删除这个资产吗？')) {
    return
  }

  try {
    await deleteAsset(assetId)
    successMessage.value = '资产已删除。'
    await loadProjectDetail()
  } catch (error) {
    errorMessage.value = error.message
  }
}

onMounted(loadProjectDetail)
</script>

<template>
  <MainShell>
    <div class="stack">
      <header class="toolbar">
        <div>
          <p class="eyebrow">Project Detail</p>
          <h2 class="section-title">{{ project?.projectName || '项目详情' }}</h2>
          <p class="section-subtitle">查看项目元数据，更新阶段，并管理该项目下的数字资产。</p>
        </div>
        <button class="ghost-button" @click="loadProjectDetail">刷新详情</button>
      </header>

      <div v-if="errorMessage" class="message message--error">{{ errorMessage }}</div>
      <div v-if="successMessage" class="message message--success">{{ successMessage }}</div>

      <div v-if="loading" class="empty">正在加载项目详情...</div>

      <template v-else-if="project">
        <section class="summary-grid">
          <article class="mini-card">
            <span class="muted">当前阶段</span>
            <strong>{{ getOptionLabel(projectPhaseOptions, project.projectPhase) }}</strong>
          </article>
          <article class="mini-card">
            <span class="muted">当前密级</span>
            <strong>{{ getOptionLabel(securityLevelOptions, project.securityLevel) }}</strong>
          </article>
          <article class="mini-card">
            <span class="muted">创建人</span>
            <strong>{{ project.createdByEmployeeId || '-' }}</strong>
          </article>
          <article class="mini-card">
            <span class="muted">资产数</span>
            <strong>{{ assets.length }}</strong>
          </article>
        </section>

        <section class="content-grid">
          <article class="surface stack">
            <div>
              <h3 class="section-title">阶段推进</h3>
              <p class="section-subtitle">提交后会触发项目阶段推进动作的 ABAC 校验。</p>
            </div>

            <label class="label">
              新阶段
              <select v-model="selectedPhase" class="select">
                <option v-for="item in projectPhaseOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
            </label>

            <button class="primary-button" @click="handleUpdatePhase">更新项目阶段</button>
          </article>

          <article class="surface stack">
            <div>
              <h3 class="section-title">新建资产</h3>
              <p class="section-subtitle">资产密级和项目阶段会一起参与访问控制决策。</p>
            </div>

            <div class="form-grid">
              <label class="label">
                资产名称
                <input v-model="assetForm.assetName" class="input" placeholder="输入资产名称" />
              </label>

              <label class="label">
                资产类型
                <select v-model="assetForm.assetsType" class="select">
                  <option v-for="item in assetTypeOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
              </label>

              <label class="label">
                产生阶段
                <select v-model="assetForm.assetsStage" class="select">
                  <option v-for="item in projectPhaseOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
              </label>

              <label class="label">
                资产密级
                <select v-model="assetForm.securityLevel" class="select">
                  <option v-for="item in securityLevelOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
              </label>

              <label class="label">
                存储路径 / Git 地址
                <input v-model="assetForm.filePath" class="input" placeholder="输入文件路径或仓库地址" />
              </label>

              <label class="label">
                文件大小（字节）
                <input v-model="assetForm.fileSize" class="input" placeholder="可为空" />
              </label>

              <label class="label">
                资产描述
                <textarea v-model="assetForm.description" class="textarea" placeholder="补充资产说明" />
              </label>
            </div>

            <button class="primary-button" @click="handleCreateAsset">提交资产</button>
          </article>
        </section>

        <section class="surface stack">
          <div>
            <h3 class="section-title">项目资产</h3>
            <p class="section-subtitle">如果当前用户对某些资产无权访问，它们不会出现在这里。</p>
          </div>

          <div v-if="assets.length === 0" class="empty">该项目下暂无可访问资产。</div>
          <div v-else class="asset-grid">
            <article v-for="asset in assets" :key="asset.assetId" class="card">
              <div class="stack">
                <div>
                  <h4 class="section-title">{{ asset.assetName }}</h4>
                  <p class="muted">
                    {{ getOptionLabel(assetTypeOptions, asset.assetsType) }} /
                    {{ getOptionLabel(projectPhaseOptions, asset.assetsStage) }}
                  </p>
                </div>

                <p class="muted">
                  密级：{{ getOptionLabel(securityLevelOptions, asset.securityLevel) }}
                </p>
                <p class="muted">路径：{{ asset.filePath || '-' }}</p>
                <p class="muted">描述：{{ asset.description || '暂无描述' }}</p>

                <div class="inline-actions">
                  <button class="danger-button" @click="handleDeleteAsset(asset.assetId)">
                    删除资产
                  </button>
                </div>
              </div>
            </article>
          </div>
        </section>
      </template>
    </div>
  </MainShell>
</template>
