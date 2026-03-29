<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import InsightChart from '@/components/InsightChart.vue'
import {
  assignDepartmentManager,
  queryManagerCandidates,
  queryManagerHandoverTodos,
} from '@/api/department'
import {
  createEmployee,
  getEmployeeOnboardOptions,
  offboardEmployee,
  queryActiveEmployees,
} from '@/api/employee'
import { getOptionLabel, projectPhaseOptions } from '@/constants/options'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const employeeLevelOptions = [
  { value: 1, label: 'P1' },
  { value: 2, label: 'P2' },
  { value: 3, label: 'P3' },
  { value: 4, label: 'P4' },
  { value: 5, label: 'P5' },
  { value: 6, label: 'P6' },
  { value: 7, label: 'P7' },
  { value: 8, label: 'P8' },
  { value: 9, label: 'VP' },
  { value: 10, label: 'DIRECTOR' },
]

const contractorOptions = [
  { value: false, label: '正式员工' },
  { value: true, label: '外包' },
]

const employeeQuery = reactive({
  keyword: '',
})

const onboardForm = reactive({
  employeeName: '',
  deptId: null,
  branchId: null,
  level: 4,
  isContractor: false,
})

const assignForm = reactive({
  deptId: null,
  newManagerEmployeeId: null,
})

const employees = ref([])
const employeeLoading = ref(false)
const employeeError = ref('')
const employeeQueried = ref(false)
const onboardSubmitting = ref(false)
const onboardOptionsLoading = ref(false)
const onboardOptionsError = ref('')
const departmentOptions = ref([])
const branchOptions = ref([])
const createdEmployee = ref(null)

const todos = ref([])
const todoLoading = ref(false)
const todoError = ref('')

const candidates = ref([])
const candidatesLoading = ref(false)
const assignSubmitting = ref(false)
const assignDialogVisible = ref(false)
const currentTodo = ref(null)
const canOnboard = computed(() => authStore.hasCapability('handover.onboard'))
const canOffboard = computed(() => authStore.hasCapability('handover.offboard'))
const canAssign = computed(() => authStore.hasCapability('handover.assign'))
const refreshLoading = computed(
  () => todoLoading.value || employeeLoading.value || onboardOptionsLoading.value,
)

const orderedTodos = computed(() =>
  [...todos.value].sort(
    (left, right) => (right.affectedProjectCount || 0) - (left.affectedProjectCount || 0),
  ),
)

const totalAffectedProjects = computed(() =>
  todos.value.reduce((sum, item) => sum + (item.affectedProjectCount || 0), 0),
)

const highestImpactTodo = computed(() => orderedTodos.value[0] || null)

const chartOption = computed(() => {
  if (orderedTodos.value.length === 0) {
    return {
      title: {
        text: '暂无待改派部门',
        left: 'center',
        top: 'center',
        textStyle: {
          color: '#6b7280',
          fontSize: 18,
          fontWeight: 500,
        },
      },
      xAxis: { show: false },
      yAxis: { show: false },
      series: [],
    }
  }

  return {
    backgroundColor: 'transparent',
    grid: {
      top: 18,
      right: 24,
      bottom: 16,
      left: 96,
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
      },
      formatter(params) {
        const item = params[0]
        return `${item.name}<br/>受影响项目数：${item.value}`
      },
    },
    xAxis: {
      type: 'value',
      axisLabel: { color: '#6b7280' },
      splitLine: {
        lineStyle: {
          color: 'rgba(15, 23, 42, 0.08)',
        },
      },
    },
    yAxis: {
      type: 'category',
      data: orderedTodos.value.map((item) => item.deptName),
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: {
        color: '#334155',
        fontWeight: 600,
      },
    },
    series: [
      {
        type: 'bar',
        data: orderedTodos.value.map((item) => item.affectedProjectCount || 0),
        barWidth: 22,
        label: {
          show: true,
          position: 'right',
          color: '#1f2937',
          fontWeight: 600,
        },
        itemStyle: {
          borderRadius: [0, 999, 999, 0],
          color(params) {
            const palette = ['#f59e0b', '#0f766e', '#2563eb', '#ef4444']
            return palette[params.dataIndex % palette.length]
          },
        },
      },
    ],
  }
})

function buildCandidateLabel(candidate) {
  return `${candidate.employeeCode || '-'} / ${candidate.employeeName || '-'}`
}

function resetOnboardForm({ preserveOptions = true } = {}) {
  onboardForm.employeeName = ''
  onboardForm.deptId = null
  onboardForm.branchId = branchOptions.value[0]?.branchId ?? null
  onboardForm.level = 4
  onboardForm.isContractor = false

  if (!preserveOptions) {
    departmentOptions.value = []
    branchOptions.value = []
  }
}

async function loadOnboardOptions() {
  if (!canOnboard.value) {
    onboardOptionsError.value = ''
    resetOnboardForm({ preserveOptions: false })
    return
  }

  onboardOptionsLoading.value = true
  onboardOptionsError.value = ''

  try {
    const result = await getEmployeeOnboardOptions()
    departmentOptions.value = result?.departments || []
    branchOptions.value = result?.branches || []
    onboardForm.branchId = onboardForm.branchId || branchOptions.value[0]?.branchId || null
  } catch (error) {
    onboardOptionsError.value = error.message
    departmentOptions.value = []
    branchOptions.value = []
  } finally {
    onboardOptionsLoading.value = false
  }
}

async function loadTodos() {
  if (!canAssign.value) {
    todos.value = []
    todoError.value = ''
    return
  }

  todoLoading.value = true
  todoError.value = ''

  try {
    todos.value = await queryManagerHandoverTodos()
  } catch (error) {
    todoError.value = error.message
    todos.value = []
  } finally {
    todoLoading.value = false
  }
}

async function handleCreateEmployee() {
  if (!canOnboard.value) {
    ElMessage.warning('当前账号不能办理员工入职')
    return
  }

  if (!onboardForm.employeeName.trim()) {
    ElMessage.warning('请输入员工姓名')
    return
  }

  if (!onboardForm.deptId || !onboardForm.branchId || !onboardForm.level) {
    ElMessage.warning('请完整填写部门、分公司和职级')
    return
  }

  onboardSubmitting.value = true

  try {
    createdEmployee.value = await createEmployee({
      employeeName: onboardForm.employeeName.trim(),
      deptId: Number(onboardForm.deptId),
      branchId: Number(onboardForm.branchId),
      level: Number(onboardForm.level),
      isContractor: Boolean(onboardForm.isContractor),
    })
    ElMessage.success('员工入职创建成功')
    resetOnboardForm()

    if (canOffboard.value && employeeQueried.value) {
      await searchEmployees()
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    onboardSubmitting.value = false
  }
}

async function searchEmployees() {
  if (!canOffboard.value) {
    employees.value = []
    employeeError.value = ''
    employeeQueried.value = false
    return
  }

  employeeLoading.value = true
  employeeError.value = ''
  employeeQueried.value = true

  try {
    employees.value = await queryActiveEmployees({
      keyword: employeeQuery.keyword.trim(),
    })
  } catch (error) {
    employeeError.value = error.message
    employees.value = []
  } finally {
    employeeLoading.value = false
  }
}

async function handleOffboard(employee) {
  if (!canOffboard.value) {
    ElMessage.warning('当前账号不能办理离职')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认将 ${employee.employeeName}（${employee.employeeCode}）办理离职吗？`,
      '离职确认',
      {
        type: 'warning',
        confirmButtonText: '确认离职',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }

  try {
    await offboardEmployee({ employeeId: employee.employeeId })
    ElMessage.success('离职办理成功')
    const reloadTasks = [searchEmployees()]
    if (canAssign.value) {
      reloadTasks.push(loadTodos())
    }
    await Promise.allSettled(reloadTasks)
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function openAssignDialog(todo) {
  if (!canAssign.value) {
    ElMessage.warning('当前账号不能改派负责人')
    return
  }

  currentTodo.value = todo
  assignForm.deptId = todo.deptId
  assignForm.newManagerEmployeeId = null
  candidates.value = []
  assignDialogVisible.value = true

  candidatesLoading.value = true
  try {
    candidates.value = await queryManagerCandidates(todo.deptId)
  } catch (error) {
    ElMessage.error(error.message)
    assignDialogVisible.value = false
  } finally {
    candidatesLoading.value = false
  }
}

async function submitAssignment() {
  if (!canAssign.value) {
    ElMessage.warning('当前账号不能改派负责人')
    return
  }

  if (!assignForm.deptId || !assignForm.newManagerEmployeeId) {
    ElMessage.warning('请选择新的部门负责人')
    return
  }

  assignSubmitting.value = true
  try {
    await assignDepartmentManager({
      deptId: assignForm.deptId,
      newManagerEmployeeId: assignForm.newManagerEmployeeId,
    })
    ElMessage.success('负责人改派成功')
    assignDialogVisible.value = false
    await loadTodos()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    assignSubmitting.value = false
  }
}

async function refreshWorkbench() {
  const tasks = []
  if (canOnboard.value) {
    tasks.push(loadOnboardOptions())
  }
  if (canAssign.value) {
    tasks.push(loadTodos())
  }
  if (canOffboard.value && employeeQueried.value) {
    tasks.push(searchEmployees())
  }
  if (tasks.length === 0) {
    return
  }
  await Promise.allSettled(tasks)
}

onMounted(() => {
  if (canOnboard.value) {
    loadOnboardOptions()
  }
  if (canAssign.value) {
    loadTodos()
  }
})
</script>

<template>
  <div class="handover-page">
    <section class="handover-hero">
      <div>
        <p class="handover-hero__eyebrow">严格模式交接</p>
        <h2 class="handover-hero__title">负责人离职与接任工作台</h2>
        <p class="handover-hero__copy">
          HR 只负责发起离职，管理层只负责指定接任人。系统会把部门 `manager_id` 的变化自动同步到当前项目负责人镜像，
          确保严格模式下的阶段推进规则持续成立。
        </p>
      </div>
      <el-button plain @click="refreshWorkbench" :loading="refreshLoading">刷新数据</el-button>
    </section>

    <section v-if="canAssign" class="handover-summary">
      <el-card shadow="never" class="summary-card">
        <p class="summary-card__label">待改派部门</p>
        <strong>{{ todos.length }}</strong>
        <span>当前 `manager_id` 仍指向离职员工的部门数量。</span>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <p class="summary-card__label">受影响项目</p>
        <strong>{{ totalAffectedProjects }}</strong>
        <span>严格模式下暂时无法继续推进阶段的项目数量。</span>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <p class="summary-card__label">最高风险部门</p>
        <strong>{{ highestImpactTodo?.deptName || '暂无' }}</strong>
        <span>
          {{
            highestImpactTodo
              ? `受影响项目 ${highestImpactTodo.affectedProjectCount} 个`
              : '当前没有待处理交接'
          }}
        </span>
      </el-card>
    </section>

    <el-card v-if="canOnboard" v-loading="onboardOptionsLoading" shadow="never" class="panel-card">
      <template #header>
        <div class="card-header">
          <div>
            <h3>HR 入职办理</h3>
            <p>创建员工档案后，系统会自动生成员工工号，并要求员工首次登录时修改默认密码。</p>
          </div>
        </div>
      </template>

      <el-alert
        title="入职由 HR 发起，部门、分公司和职级由表单显式指定；生成后的工号和初始口令会直接回显。"
        type="info"
        show-icon
        :closable="false"
        class="block-alert"
      />

      <el-alert
        v-if="onboardOptionsError"
        :title="onboardOptionsError"
        type="warning"
        show-icon
        :closable="false"
        class="block-alert"
      />

      <template v-else>
        <el-form label-position="top" class="onboard-form">
          <el-form-item label="员工姓名">
            <el-input
              v-model="onboardForm.employeeName"
              placeholder="请输入新员工姓名"
              maxlength="32"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="所属部门">
            <el-select v-model="onboardForm.deptId" filterable placeholder="请选择部门">
              <el-option
                v-for="item in departmentOptions"
                :key="item.deptId"
                :label="`${item.deptName} (${item.deptTypeDesc || item.deptType || '-'})`"
                :value="item.deptId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="所属分公司">
            <el-select v-model="onboardForm.branchId" filterable placeholder="请选择分公司">
              <el-option
                v-for="item in branchOptions"
                :key="item.branchId"
                :label="item.branchName"
                :value="item.branchId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="员工职级">
            <el-select v-model="onboardForm.level" placeholder="请选择职级">
              <el-option
                v-for="item in employeeLevelOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="人员身份" class="onboard-form__full">
            <el-select v-model="onboardForm.isContractor" placeholder="请选择人员身份">
              <el-option
                v-for="item in contractorOptions"
                :key="String(item.value)"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-form>

        <div class="onboard-actions">
          <el-button type="primary" :loading="onboardSubmitting" @click="handleCreateEmployee">
            办理入职
          </el-button>
          <el-button @click="resetOnboardForm">重置表单</el-button>
        </div>

        <el-descriptions
          v-if="createdEmployee"
          :column="2"
          border
          class="created-employee-card"
        >
          <el-descriptions-item label="员工工号">
            {{ createdEmployee.employeeCode || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="员工姓名">
            {{ createdEmployee.employeeName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="所属部门">
            {{ createdEmployee.deptName || createdEmployee.deptId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="所属分公司">
            {{ createdEmployee.branchName || createdEmployee.branchId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="员工职级">
            {{ createdEmployee.level || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="人员身份">
            {{ createdEmployee.isContractor ? '外包' : '正式员工' }}
          </el-descriptions-item>
          <el-descriptions-item label="初始密码">
            {{ createdEmployee.initialPassword || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="首次登录改密">
            {{ createdEmployee.mustChangePassword ? '是' : '否' }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-card>

    <section class="handover-grid">
      <el-card v-if="canAssign" shadow="never" class="chart-card">
        <template #header>
          <div class="card-header">
            <div>
              <h3>待改派热度图</h3>
              <p>按部门查看受影响项目数，优先处理堆积最高的责任链条。</p>
            </div>
            <el-tag type="warning" effect="light" round>柱状图</el-tag>
          </div>
        </template>

        <InsightChart :option="chartOption" height="340px" />
      </el-card>

      <el-card v-if="canOffboard" shadow="never" class="panel-card">
        <template #header>
          <div class="card-header">
            <div>
              <h3>HR 离职办理</h3>
              <p>检索在职员工并发起离职，系统会自动把负责人缺口暴露到待改派列表中。</p>
            </div>
          </div>
        </template>

        <div class="toolbar-row">
          <el-input
            v-model="employeeQuery.keyword"
            clearable
            placeholder="按工号或姓名搜索在职员工"
            @keyup.enter="searchEmployees"
          />
          <el-button type="primary" :loading="employeeLoading" @click="searchEmployees">
            查询在职员工
          </el-button>
        </div>

        <el-alert
          v-if="employeeError"
          :title="employeeError"
          type="warning"
          show-icon
          :closable="false"
          class="block-alert"
        />

        <el-empty
          v-else-if="employeeQueried && employees.length === 0"
          description="没有检索到可办理离职的在职员工"
        />

        <el-table v-else-if="employees.length > 0" :data="employees" stripe height="380">
          <el-table-column prop="employeeCode" label="工号" min-width="110" />
          <el-table-column prop="employeeName" label="姓名" min-width="120" />
          <el-table-column prop="deptId" label="部门 ID" min-width="96" />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="danger" @click="handleOffboard(row)">办理离职</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-else description="HR 可以在这里检索在职员工并发起离职" />
      </el-card>
    </section>

    <el-card v-if="canAssign" shadow="never" class="panel-card">
      <template #header>
        <div class="card-header">
          <div>
            <h3>管理层待改派列表</h3>
            <p>系统根据离职结果动态生成待办，无需额外维护独立任务表。</p>
          </div>
        </div>
      </template>

      <el-alert
        v-if="todoError"
        :title="todoError"
        type="warning"
        show-icon
        :closable="false"
        class="block-alert"
      />

      <el-skeleton v-else-if="todoLoading" :rows="6" animated />

      <el-empty v-else-if="todos.length === 0" description="当前没有负责人待改派部门" />

      <el-table v-else :data="orderedTodos" stripe>
        <el-table-column prop="deptName" label="部门" min-width="140" />
        <el-table-column prop="deptType" label="部门类型" min-width="110" />
        <el-table-column label="离职负责人" min-width="180">
          <template #default="{ row }">
            {{ row.managerName || '-' }}（{{ row.managerCode || row.managerId || '-' }}）
          </template>
        </el-table-column>
        <el-table-column label="受影响项目" min-width="190">
          <template #default="{ row }">
            <el-space wrap>
              <el-tag type="danger" effect="light">{{ row.affectedProjectCount || 0 }} 个项目</el-tag>
              <el-popover
                v-if="row.affectedProjects?.length"
                placement="top"
                width="360"
                trigger="click"
              >
                <template #reference>
                  <el-button link type="primary">查看明细</el-button>
                </template>

                <div class="project-popover">
                  <div
                    v-for="project in row.affectedProjects"
                    :key="project.projectId"
                    class="project-popover__item"
                  >
                    <strong>{{ project.projectName }}</strong>
                    <span>
                      {{ getOptionLabel(projectPhaseOptions, project.projectPhase) }}
                      / 当前负责人 {{ project.ownerId || '-' }}
                    </span>
                  </div>
                </div>
              </el-popover>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openAssignDialog(row)">指定新负责人</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="assignDialogVisible"
      width="560px"
      destroy-on-close
      title="指定新的部门负责人"
    >
      <template v-if="currentTodo">
        <div class="assign-dialog__meta">
          <el-tag type="warning" effect="light">{{ currentTodo.deptName }}</el-tag>
          <span>原负责人：{{ currentTodo.managerName || '-' }}</span>
          <span>受影响项目：{{ currentTodo.affectedProjectCount || 0 }}</span>
        </div>

        <el-form label-position="top">
          <el-form-item label="选择同部门在职员工">
            <el-select
              v-model="assignForm.newManagerEmployeeId"
              filterable
              placeholder="请选择接任负责人"
              style="width: 100%"
              :loading="candidatesLoading"
            >
              <el-option
                v-for="candidate in candidates"
                :key="candidate.employeeId"
                :label="buildCandidateLabel(candidate)"
                :value="candidate.employeeId"
              />
            </el-select>
          </el-form-item>
        </el-form>

        <el-empty
          v-if="!candidatesLoading && candidates.length === 0"
          description="当前部门没有可接任的在职员工"
        />
      </template>

      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignSubmitting" @click="submitAssignment">
          确认改派
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.handover-page {
  display: grid;
  gap: 20px;
}

.handover-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 20px 24px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
  color: #303133;
}

.handover-hero__eyebrow {
  margin: 0 0 10px;
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.handover-hero__title {
  margin: 0 0 12px;
  font-size: 24px;
  line-height: 1.4;
}

.handover-hero__copy {
  max-width: 780px;
  margin: 0;
  line-height: 1.7;
  color: #606266;
}

.handover-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: none;
}

.summary-card :deep(.el-card__body) {
  display: grid;
  gap: 8px;
}

.summary-card__label {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.summary-card strong {
  font-size: 34px;
  color: #0f172a;
}

.summary-card span {
  color: #475569;
  line-height: 1.6;
}

.handover-grid {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 16px;
}

.chart-card,
.panel-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: none;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.card-header h3 {
  margin: 0 0 6px;
  font-size: 20px;
  color: #111827;
}

.card-header p {
  margin: 0;
  color: #64748b;
  line-height: 1.6;
}

.toolbar-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  margin-bottom: 16px;
}

.onboard-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.onboard-form__full {
  grid-column: 1 / -1;
}

.onboard-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.created-employee-card {
  margin-top: 8px;
}

.block-alert {
  margin-bottom: 16px;
}

.project-popover {
  display: grid;
  gap: 10px;
}

.project-popover__item {
  display: grid;
  gap: 4px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
}

.project-popover__item:last-child {
  border-bottom: none;
}

.project-popover__item strong {
  color: #0f172a;
}

.project-popover__item span {
  color: #64748b;
  font-size: 13px;
}

.assign-dialog__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 18px;
  color: #475569;
}

@media (max-width: 1200px) {
  .handover-grid,
  .handover-summary,
  .onboard-form {
    grid-template-columns: 1fr;
  }

  .onboard-form__full {
    grid-column: auto;
  }
}

@media (max-width: 768px) {
  .handover-hero {
    padding: 22px;
    flex-direction: column;
  }

  .toolbar-row {
    grid-template-columns: 1fr;
  }
}
</style>
