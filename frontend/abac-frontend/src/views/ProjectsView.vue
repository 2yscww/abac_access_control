<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import MainShell from '@/components/MainShell.vue'
import { createProject, deleteProject, queryProjects } from '@/api/project'
import {
  getOptionLabel,
  projectPhaseOptions,
  securityLevelOptions,
} from '@/constants/options'

const router = useRouter()

const filters = reactive({
  projectName: '',
  projectPhase: '',
  securityLevel: '',
  pageNum: 1,
  pageSize: 8,
})

const createForm = reactive({
  projectName: '',
  projectPhase: 1,
  securityLevel: 2,
})

const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const showCreateForm = ref(false)
const projects = ref([])
const total = ref(0)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / filters.pageSize)))
const confidentialCount = computed(
  () => projects.value.filter((project) => project.securityLevel === 'CONFIDENTIAL').length,
)
const archivedCount = computed(
  () => projects.value.filter((project) => project.projectPhase === 'ARCHIVED').length,
)

async function loadProjects() {
  loading.value = true
  errorMessage.value = ''

  try {
    const data = await queryProjects(filters)
    projects.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

async function handleCreateProject() {
  errorMessage.value = ''
  successMessage.value = ''

  try {
    await createProject(createForm)
    successMessage.value = '项目创建成功。'
    showCreateForm.value = false
    createForm.projectName = ''
    createForm.projectPhase = 1
    createForm.securityLevel = 2
    filters.pageNum = 1
    await loadProjects()
  } catch (error) {
    errorMessage.value = error.message
  }
}

async function handleDeleteProject(projectId) {
  if (!window.confirm('确定删除这个项目吗？相关资产也会被一并移除。')) {
    return
  }

  try {
    await deleteProject(projectId)
    successMessage.value = '项目已删除。'
    await loadProjects()
  } catch (error) {
    errorMessage.value = error.message
  }
}

function goToPage(pageNum) {
  filters.pageNum = pageNum
  loadProjects()
}

onMounted(loadProjects)
</script>

<template>
  <MainShell>
    <div class="stack">
      <header class="toolbar">
        <div>
          <p class="eyebrow">Projects</p>
          <h2 class="section-title">项目权限总览</h2>
          <p class="section-subtitle">先从可访问项目入手，验证阶段和密级策略是否生效。</p>
        </div>
        <div class="toolbar__actions">
          <button class="secondary-button" @click="showCreateForm = !showCreateForm">
            {{ showCreateForm ? '收起创建面板' : '新建项目' }}
          </button>
          <button class="ghost-button" @click="loadProjects">刷新</button>
        </div>
      </header>

      <div v-if="errorMessage" class="message message--error">{{ errorMessage }}</div>
      <div v-if="successMessage" class="message message--success">{{ successMessage }}</div>

      <section class="summary-grid">
        <article class="mini-card">
          <span class="muted">当前页项目</span>
          <strong>{{ projects.length }}</strong>
        </article>
        <article class="mini-card">
          <span class="muted">可访问总数</span>
          <strong>{{ total }}</strong>
        </article>
        <article class="mini-card">
          <span class="muted">机密项目</span>
          <strong>{{ confidentialCount }}</strong>
        </article>
        <article class="mini-card">
          <span class="muted">归档项目</span>
          <strong>{{ archivedCount }}</strong>
        </article>
      </section>

      <section class="content-grid">
        <article class="surface stack">
          <div>
            <h3 class="section-title">检索项目</h3>
            <p class="section-subtitle">结合项目名、阶段和密级查看当前账户可见范围。</p>
          </div>

          <div class="form-grid">
            <label class="label">
              项目名
              <input v-model="filters.projectName" class="input" placeholder="模糊搜索项目名称" />
            </label>
            <label class="label">
              项目阶段
              <select v-model="filters.projectPhase" class="select">
                <option value="">全部阶段</option>
                <option v-for="item in projectPhaseOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
            </label>
            <label class="label">
              项目密级
              <select v-model="filters.securityLevel" class="select">
                <option value="">全部密级</option>
                <option v-for="item in securityLevelOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
            </label>
          </div>

          <div class="inline-actions">
            <button class="primary-button" @click="filters.pageNum = 1; loadProjects()">查询</button>
            <button
              class="ghost-button"
              @click="
                filters.projectName = '';
                filters.projectPhase = '';
                filters.securityLevel = '';
                filters.pageNum = 1;
                loadProjects();
              "
            >
              重置
            </button>
          </div>
        </article>

        <article v-if="showCreateForm" class="surface stack">
          <div>
            <h3 class="section-title">创建项目</h3>
            <p class="section-subtitle">创建动作也会受到 ABAC 策略约束。</p>
          </div>

          <div class="form-grid">
            <label class="label">
              项目名称
              <input v-model="createForm.projectName" class="input" placeholder="输入项目名称" />
            </label>
            <label class="label">
              初始阶段
              <select v-model="createForm.projectPhase" class="select">
                <option v-for="item in projectPhaseOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
            </label>
            <label class="label">
              项目密级
              <select v-model="createForm.securityLevel" class="select">
                <option v-for="item in securityLevelOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
            </label>
          </div>

          <button class="primary-button" @click="handleCreateProject">提交项目</button>
        </article>
      </section>

      <section class="surface stack">
        <div class="toolbar">
          <div>
            <h3 class="section-title">项目列表</h3>
            <p class="section-subtitle">只展示当前账号通过 ABAC 决策后的结果集。</p>
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

        <div v-if="loading" class="empty">正在加载项目列表...</div>
        <div v-else-if="projects.length === 0" class="empty">当前查询条件下没有可访问项目。</div>
        <div v-else class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>项目名称</th>
                <th>阶段</th>
                <th>密级</th>
                <th>创建人</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="project in projects" :key="project.projectId">
                <td>{{ project.projectName }}</td>
                <td>{{ getOptionLabel(projectPhaseOptions, project.projectPhase) }}</td>
                <td>{{ getOptionLabel(securityLevelOptions, project.securityLevel) }}</td>
                <td>{{ project.createdByEmployeeId || '-' }}</td>
                <td>{{ project.createdAt || '-' }}</td>
                <td>
                  <div class="inline-actions">
                    <button
                      class="secondary-button"
                      @click="router.push({ name: 'project-detail', params: { id: project.projectId } })"
                    >
                      查看详情
                    </button>
                    <button class="danger-button" @click="handleDeleteProject(project.projectId)">
                      删除
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </MainShell>
</template>
