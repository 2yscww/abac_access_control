<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import MainShell from '@/components/MainShell.vue'
import { queryAssets } from '@/api/asset'
import {
  assetTypeOptions,
  getOptionLabel,
  projectPhaseOptions,
  securityLevelOptions,
} from '@/constants/options'

const router = useRouter()

const filters = reactive({
  projectId: '',
  assetName: '',
  assetsType: '',
  assetsStage: '',
  securityLevel: '',
  pageNum: 1,
  pageSize: 10,
})

const assets = ref([])
const total = ref(0)
const loading = ref(false)
const errorMessage = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / filters.pageSize)))

async function loadAssets() {
  loading.value = true
  errorMessage.value = ''

  try {
    const data = await queryAssets(filters)
    assets.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

function goToPage(pageNum) {
  filters.pageNum = pageNum
  loadAssets()
}

onMounted(loadAssets)
</script>

<template>
  <MainShell>
    <div class="stack">
      <header class="toolbar">
        <div>
          <p class="eyebrow">Assets</p>
          <h2 class="section-title">资产检索中心</h2>
          <p class="section-subtitle">按项目、类型、阶段和密级检索当前账户可见的资产。</p>
        </div>
        <button class="ghost-button" @click="loadAssets">刷新</button>
      </header>

      <div v-if="errorMessage" class="message message--error">{{ errorMessage }}</div>

      <section class="surface stack">
        <div class="form-grid">
          <label class="label">
            项目 ID
            <input v-model="filters.projectId" class="input" placeholder="输入项目 ID" />
          </label>
          <label class="label">
            资产名称
            <input v-model="filters.assetName" class="input" placeholder="模糊搜索资产名称" />
          </label>
          <label class="label">
            资产类型
            <select v-model="filters.assetsType" class="select">
              <option value="">全部类型</option>
              <option v-for="item in assetTypeOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
            </select>
          </label>
          <label class="label">
            产生阶段
            <select v-model="filters.assetsStage" class="select">
              <option value="">全部阶段</option>
              <option v-for="item in projectPhaseOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
            </select>
          </label>
          <label class="label">
            资产密级
            <select v-model="filters.securityLevel" class="select">
              <option value="">全部密级</option>
              <option v-for="item in securityLevelOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
            </select>
          </label>
        </div>

        <div class="inline-actions">
          <button class="primary-button" @click="filters.pageNum = 1; loadAssets()">查询</button>
          <button
            class="ghost-button"
            @click="
              filters.projectId = '';
              filters.assetName = '';
              filters.assetsType = '';
              filters.assetsStage = '';
              filters.securityLevel = '';
              filters.pageNum = 1;
              loadAssets();
            "
          >
            重置
          </button>
        </div>
      </section>

      <section class="surface stack">
        <div class="toolbar">
          <div>
            <h3 class="section-title">资产结果</h3>
            <p class="section-subtitle">当前共有 {{ total }} 条可访问资产。</p>
          </div>
          <div class="pager">
            <button
              class="ghost-button"
              :disabled="filters.pageNum <= 1"
              @click="goToPage(filters.pageNum - 1)"
            >
              上一页
            </button>
            <span class="muted">第 {{ filters.pageNum }} / {{ totalPages }} 页</span>
            <button
              class="ghost-button"
              :disabled="filters.pageNum >= totalPages"
              @click="goToPage(filters.pageNum + 1)"
            >
              下一页
            </button>
          </div>
        </div>

        <div v-if="loading" class="empty">正在加载资产列表...</div>
        <div v-else-if="assets.length === 0" class="empty">当前条件下没有可访问资产。</div>
        <div v-else class="asset-grid">
          <article v-for="asset in assets" :key="asset.assetId" class="card">
            <div class="stack">
              <div>
                <h4 class="section-title">{{ asset.assetName }}</h4>
                <p class="muted">项目 ID：{{ asset.projectId }}</p>
              </div>
              <p class="muted">类型：{{ getOptionLabel(assetTypeOptions, asset.assetsType) }}</p>
              <p class="muted">阶段：{{ getOptionLabel(projectPhaseOptions, asset.assetsStage) }}</p>
              <p class="muted">密级：{{ getOptionLabel(securityLevelOptions, asset.securityLevel) }}</p>
              <p class="muted">创建人：{{ asset.createdByEmployeeId || '-' }}</p>
              <div class="inline-actions">
                <button
                  class="secondary-button"
                  @click="router.push({ name: 'project-detail', params: { id: asset.projectId } })"
                >
                  查看所属项目
                </button>
              </div>
            </div>
          </article>
        </div>
      </section>
    </div>
  </MainShell>
</template>
