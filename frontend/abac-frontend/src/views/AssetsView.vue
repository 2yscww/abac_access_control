<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import InsightChart from '@/components/InsightChart.vue'
import { queryAssets } from '@/api/asset'
import {
  assetTypeOptions,
  findOption,
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

const visibleCount = computed(() => assets.value.length)
const distinctProjectCount = computed(() => new Set(assets.value.map((asset) => asset.projectId)).size)
const sensitiveCount = computed(
  () => assets.value.filter((asset) => ['CONFIDENTIAL', 'TOP_SECRET', 3, 4].includes(asset.securityLevel)).length,
)
const chartOption = computed(() => {
  const counts = assetTypeOptions.map((option) => ({
    label: option.label,
    value: assets.value.filter((asset) => findOption(assetTypeOptions, asset.assetsType)?.key === option.key)
      .length,
  }))

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
      },
    },
    grid: {
      top: 12,
      right: 18,
      bottom: 32,
      left: 28,
    },
    xAxis: {
      type: 'value',
      axisLabel: {
        color: '#64748b',
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(148, 163, 184, 0.18)',
        },
      },
    },
    yAxis: {
      type: 'category',
      data: counts.map((item) => item.label),
      axisTick: {
        show: false,
      },
      axisLine: {
        show: false,
      },
      axisLabel: {
        color: '#334155',
      },
    },
    series: [
      {
        type: 'bar',
        barWidth: 18,
        data: counts.map((item) => item.value),
        label: {
          show: true,
          position: 'right',
          color: '#334155',
        },
        itemStyle: {
          borderRadius: [0, 999, 999, 0],
          color(params) {
            const palette = ['#0f766e', '#2563eb', '#14b8a6', '#f59e0b', '#ef4444', '#8b5cf6']
            return palette[params.dataIndex % palette.length]
          },
        },
      },
    ],
  }
})

async function loadAssets() {
  loading.value = true

  try {
    const data = await queryAssets(filters)
    assets.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    assets.value = []
    total.value = 0
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.projectId = ''
  filters.assetName = ''
  filters.assetsType = ''
  filters.assetsStage = ''
  filters.securityLevel = ''
  filters.pageNum = 1
  loadAssets()
}

function handlePageChange(pageNum) {
  filters.pageNum = pageNum
  loadAssets()
}

onMounted(loadAssets)
</script>

<template>
  <div class="assets-page">
    <section class="assets-hero">
      <div>
        <p class="assets-hero__eyebrow">资产中心</p>
        <h2 class="assets-hero__title">资产检索与生命周期视图</h2>
        <p class="assets-hero__description">
          使用统一查询面板查看当前账号可见的资产元数据，关注资产类型、产生阶段和安全密级在不同项目中的分布情况。
        </p>
      </div>
      <el-button plain :loading="loading" @click="loadAssets">刷新资产</el-button>
    </section>

    <section class="assets-summary">
      <el-card shadow="never" class="summary-card">
        <el-statistic title="当前页资产数" :value="visibleCount" />
        <p>当前分页内可读取到的资产元数据数量。</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="命中总数" :value="total" />
        <p>当前查询条件下命中的总资产数量。</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="覆盖项目数" :value="distinctProjectCount" />
        <p>当前页资产分布到的项目数量。</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="高密资产" :value="sensitiveCount" />
        <p>机密与绝密资产仍受到更严格的访问边界限制。</p>
      </el-card>
    </section>

    <section class="assets-grid">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>查询条件</h3>
              <p>组合项目、名称、类型、阶段和密级，定位目标资产。</p>
            </div>
          </div>
        </template>

        <el-form label-position="top" class="query-form">
          <el-form-item label="项目 ID">
            <el-input v-model="filters.projectId" clearable placeholder="请输入项目 ID" />
          </el-form-item>
          <el-form-item label="资产名称">
            <el-input v-model="filters.assetName" clearable placeholder="支持模糊搜索" />
          </el-form-item>
          <el-form-item label="资产类型">
            <el-select v-model="filters.assetsType" clearable placeholder="全部类型">
              <el-option
                v-for="item in assetTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="产生阶段">
            <el-select v-model="filters.assetsStage" clearable placeholder="全部阶段">
              <el-option
                v-for="item in projectPhaseOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="资产密级">
            <el-select v-model="filters.securityLevel" clearable placeholder="全部密级">
              <el-option
                v-for="item in securityLevelOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-form>

        <div class="query-actions">
          <el-button type="primary" @click="filters.pageNum = 1; loadAssets()">查询</el-button>
          <el-button plain @click="resetFilters">重置</el-button>
        </div>
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>当前页类型分布</h3>
              <p>快速判断当前结果更偏向文档、代码、测试还是部署运维资料。</p>
            </div>
            <el-tag type="warning" effect="light" round>柱状图</el-tag>
          </div>
        </template>
        <InsightChart :option="chartOption" height="320px" />
      </el-card>
    </section>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="panel-card__header">
          <div>
            <h3>资产结果</h3>
            <p>列表只展示当前账号可见的资产元数据，点击后可回到所属项目继续操作。</p>
          </div>
          <el-tag type="info" effect="light">共 {{ total }} 条</el-tag>
        </div>
      </template>

      <el-table v-loading="loading" :data="assets" stripe>
        <el-table-column prop="assetName" label="资产名称" min-width="180" />
        <el-table-column prop="projectId" label="项目 ID" min-width="100" />
        <el-table-column label="资产类型" min-width="130">
          <template #default="{ row }">
            {{ getOptionLabel(assetTypeOptions, row.assetsType) }}
          </template>
        </el-table-column>
        <el-table-column label="产生阶段" min-width="120">
          <template #default="{ row }">
            {{ getOptionLabel(projectPhaseOptions, row.assetsStage) }}
          </template>
        </el-table-column>
        <el-table-column label="资产密级" min-width="120">
          <template #default="{ row }">
            <el-tag effect="light">
              {{ getOptionLabel(securityLevelOptions, row.securityLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdByEmployeeId" label="创建人" min-width="100" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              @click="router.push({ name: 'project-detail', params: { id: row.projectId } })"
            >
              查看项目
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && assets.length === 0" description="当前条件下没有可访问资产" />

      <div class="table-card__footer">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="filters.pageNum"
          :page-size="filters.pageSize"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.assets-page {
  display: grid;
  gap: 18px;
}

.assets-hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  padding: 20px 24px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
}

.assets-hero__eyebrow {
  margin: 0 0 10px;
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.assets-hero__title {
  margin: 0 0 10px;
  color: #303133;
  font-size: 24px;
  line-height: 1.4;
}

.assets-hero__description {
  margin: 0;
  max-width: 760px;
  color: #606266;
  line-height: 1.7;
}

.assets-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.assets-grid {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
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

.summary-card p {
  margin: 12px 0 0;
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

.query-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.query-actions,
.table-card__footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.table-card__footer {
  margin-top: 18px;
}

@media (max-width: 1180px) {
  .assets-summary,
  .assets-grid,
  .query-form {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .assets-hero {
    flex-direction: column;
    padding: 22px;
  }
}
</style>
