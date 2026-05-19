import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

const routerPush = vi.fn()
const routeState = {
  params: { id: '201' },
  meta: {},
}

const authStore = {
  tempToken: '',
  employeeId: '1',
  profile: null,
  hasCapability: vi.fn(),
  setSession: vi.fn(),
  setTempSession: vi.fn(),
}

const login = vi.fn()
const changePassword = vi.fn()
const queryProjects = vi.fn()
const createProject = vi.fn()
const deleteProject = vi.fn()
const getProject = vi.fn()
const getPhaseOwnerPreview = vi.fn()
const listProjectMembers = vi.fn()
const addProjectMember = vi.fn()
const removeProjectMember = vi.fn()
const updateProjectPhase = vi.fn()
const queryAssets = vi.fn()
const getAssetsByProject = vi.fn()
const uploadAsset = vi.fn()
const deleteAsset = vi.fn()
const assignDepartmentManager = vi.fn()
const queryManagerCandidates = vi.fn()
const queryManagerHandoverTodos = vi.fn()
const createEmployee = vi.fn()
const getEmployeeOnboardOptions = vi.fn()
const offboardEmployee = vi.fn()
const queryActiveEmployees = vi.fn()
const queryAuditLogs = vi.fn()
const listPolicyConfigs = vi.fn()
const updatePolicyConfig = vi.fn()
const triggerAssetDownload = vi.fn()

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    useRouter: () => ({ push: routerPush }),
    useRoute: () => routeState,
  }
})

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => authStore,
}))

vi.mock('@/api/auth', () => ({
  login: (...args) => login(...args),
  changePassword: (...args) => changePassword(...args),
}))

vi.mock('@/api/project', () => ({
  queryProjects: (...args) => queryProjects(...args),
  createProject: (...args) => createProject(...args),
  deleteProject: (...args) => deleteProject(...args),
  getProject: (...args) => getProject(...args),
  getPhaseOwnerPreview: (...args) => getPhaseOwnerPreview(...args),
  listProjectMembers: (...args) => listProjectMembers(...args),
  addProjectMember: (...args) => addProjectMember(...args),
  removeProjectMember: (...args) => removeProjectMember(...args),
  updateProjectPhase: (...args) => updateProjectPhase(...args),
}))

vi.mock('@/api/asset', () => ({
  queryAssets: (...args) => queryAssets(...args),
  getAssetsByProject: (...args) => getAssetsByProject(...args),
  uploadAsset: (...args) => uploadAsset(...args),
  deleteAsset: (...args) => deleteAsset(...args),
}))

vi.mock('@/api/department', () => ({
  assignDepartmentManager: (...args) => assignDepartmentManager(...args),
  queryManagerCandidates: (...args) => queryManagerCandidates(...args),
  queryManagerHandoverTodos: (...args) => queryManagerHandoverTodos(...args),
}))

vi.mock('@/api/employee', () => ({
  createEmployee: (...args) => createEmployee(...args),
  getEmployeeOnboardOptions: (...args) => getEmployeeOnboardOptions(...args),
  offboardEmployee: (...args) => offboardEmployee(...args),
  queryActiveEmployees: (...args) => queryActiveEmployees(...args),
}))

vi.mock('@/api/audit', () => ({
  queryAuditLogs: (...args) => queryAuditLogs(...args),
}))

vi.mock('@/api/policy', () => ({
  listPolicyConfigs: (...args) => listPolicyConfigs(...args),
  updatePolicyConfig: (...args) => updatePolicyConfig(...args),
}))

vi.mock('@/utils/assetDownload', () => ({
  triggerAssetDownload: (...args) => triggerAssetDownload(...args),
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    },
    ElMessageBox: {
      confirm: vi.fn(),
    },
  }
})

vi.mock('@/components/InsightChart.vue', () => ({
  default: {
    name: 'InsightChartStub',
    props: ['option', 'height'],
    template: '<div class="insight-chart-stub">{{ height }}</div>',
  },
}))

import { ElMessage, ElMessageBox } from 'element-plus'
import LoginView from '@/views/LoginView.vue'
import ProjectsView from '@/views/ProjectsView.vue'
import ProjectDetailView from '@/views/ProjectDetailView.vue'
import AssetsView from '@/views/AssetsView.vue'
import FileCenterView from '@/views/FileCenterView.vue'
import HandoverWorkbenchView from '@/views/HandoverWorkbenchView.vue'
import AuditLogsView from '@/views/AuditLogsView.vue'
import PolicyConfigView from '@/views/PolicyConfigView.vue'

const stubs = {
  'el-container': { template: '<div><slot /></div>' },
  'el-aside': { template: '<aside><slot /></aside>' },
  'el-main': { template: '<main><slot /></main>' },
  'el-card': { template: '<section><slot /><slot name="header" /></section>' },
  'el-alert': {
    props: ['title'],
    template: '<div class="alert-stub">{{ title }}<slot /></div>',
  },
  'el-form': { template: '<form><slot /></form>' },
  'el-form-item': {
    props: ['label'],
    template: '<label><span>{{ label }}</span><slot /></label>',
  },
  'el-input': {
    props: ['modelValue', 'type', 'placeholder', 'readonly'],
    emits: ['update:modelValue', 'change', 'keyup'],
    template:
      '<input :value="modelValue" :type="type || \'text\'" :placeholder="placeholder" :readonly="readonly" @input="$emit(\'update:modelValue\', $event.target.value)" @change="$emit(\'change\', $event.target.value)" @keyup="$emit(\'keyup\', $event)" />',
  },
  'el-button': {
    props: ['disabled', 'loading'],
    emits: ['click'],
    template:
      '<button :disabled="disabled || loading" @click="$emit(\'click\', $event)"><slot /></button>',
  },
  'el-select': {
    props: ['modelValue', 'disabled'],
    emits: ['update:modelValue', 'change'],
    template:
      '<select :value="modelValue" :disabled="disabled" @change="$emit(\'update:modelValue\', $event.target.value); $emit(\'change\', $event.target.value)"><slot /></select>',
  },
  'el-option': {
    props: ['label', 'value'],
    template: '<option :value="value">{{ label }}</option>',
  },
  'el-tag': {
    props: ['type'],
    template: '<span class="tag-stub"><slot /></span>',
  },
  'el-statistic': {
    props: ['title', 'value', 'suffix'],
    template: '<div class="stat-stub">{{ title }} {{ value }} {{ suffix }}</div>',
  },
  'el-table': { template: '<div class="table-stub"><slot /></div>' },
  'el-table-column': { template: '<div class="column-stub"><slot /></div>' },
  'el-empty': {
    props: ['description'],
    template: '<div class="empty-stub">{{ description }}</div>',
  },
  'el-pagination': { template: '<div class="pagination-stub"></div>' },
  'el-dialog': {
    props: ['modelValue', 'title'],
    emits: ['update:modelValue', 'closed'],
    template:
      '<div v-if="modelValue" class="dialog-stub"><h3>{{ title }}</h3><slot /><slot name="footer" /></div>',
  },
  'el-descriptions': { template: '<div><slot /></div>' },
  'el-descriptions-item': {
    props: ['label'],
    template: '<div><strong>{{ label }}</strong><slot /></div>',
  },
  'el-space': { template: '<div><slot /></div>' },
  'el-progress': { template: '<div class="progress-stub"></div>' },
  'el-skeleton': { template: '<div class="skeleton-stub"></div>' },
  'el-popover': { template: '<div><slot name="reference" /><slot /></div>' },
  'el-date-picker': {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
  },
  'el-switch': {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template:
      '<input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" />',
  },
}

function mountView(component) {
  return mount(component, {
    global: {
      stubs,
    },
  })
}

function resetAllMocks() {
  routeState.params.id = '201'
  routeState.meta = {}

  routerPush.mockReset()
  authStore.tempToken = ''
  authStore.employeeId = '1'
  authStore.profile = {
    employeeId: 1,
    employeeCode: 'E001',
    employeeName: 'Operator',
    deptType: 'MANAGEMENT',
    levelRank: 9,
    visibleMenus: ['projects', 'assets', 'files', 'handover', 'audit', 'policy'],
    capabilities: [
      'files.upload',
      'files.download',
      'handover.onboard',
      'handover.offboard',
      'handover.assign',
      'audit.view',
      'policy.manage',
    ],
  }
  authStore.hasCapability.mockImplementation((capability) =>
    authStore.profile?.capabilities?.includes(capability),
  )
  authStore.setSession.mockReset()
  authStore.setTempSession.mockReset()

  login.mockReset()
  changePassword.mockReset()
  queryProjects.mockReset()
  createProject.mockReset()
  deleteProject.mockReset()
  getProject.mockReset()
  getPhaseOwnerPreview.mockReset()
  listProjectMembers.mockReset()
  addProjectMember.mockReset()
  removeProjectMember.mockReset()
  updateProjectPhase.mockReset()
  queryAssets.mockReset()
  getAssetsByProject.mockReset()
  uploadAsset.mockReset()
  deleteAsset.mockReset()
  assignDepartmentManager.mockReset()
  queryManagerCandidates.mockReset()
  queryManagerHandoverTodos.mockReset()
  createEmployee.mockReset()
  getEmployeeOnboardOptions.mockReset()
  offboardEmployee.mockReset()
  queryActiveEmployees.mockReset()
  queryAuditLogs.mockReset()
  listPolicyConfigs.mockReset()
  updatePolicyConfig.mockReset()
  triggerAssetDownload.mockReset()

  ElMessage.success.mockReset()
  ElMessage.error.mockReset()
  ElMessage.warning.mockReset()
  ElMessageBox.confirm.mockReset()
}

beforeEach(() => {
  resetAllMocks()
})

describe('Presentation Layer Views', () => {
  it('renders login mode by default and authenticates successfully', async () => {
    login.mockResolvedValue({ token: 'token', employeeId: 9 })

    const wrapper = mountView(LoginView)
    await flushPromises()

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('EMP001')
    await inputs[1].setValue('secret')
    await wrapper.find('button').trigger('click')
    await flushPromises()

    expect(login).toHaveBeenCalledWith({
      employeeCode: 'EMP001',
      password: 'secret',
    })
    expect(authStore.setSession).toHaveBeenCalledWith({ token: 'token', employeeId: 9 })
    expect(routerPush).toHaveBeenCalledWith({ name: 'projects' })
  })

  it('switches to change-password mode for first login and completes password change', async () => {
    login.mockResolvedValue({
      mustChangePassword: true,
      tempToken: 'temp-token',
      employeeId: 9,
    })
    changePassword.mockResolvedValue({
      token: 'formal-token',
      employeeId: 9,
    })

    const wrapper = mountView(LoginView)
    await flushPromises()

    const loginInputs = wrapper.findAll('input')
    await loginInputs[0].setValue('EMP009')
    await loginInputs[1].setValue('old-pass')
    await wrapper.find('button').trigger('click')
    await flushPromises()

    expect(authStore.setTempSession).toHaveBeenCalled()
    expect(wrapper.text()).toContain('妫€娴嬪埌棣栨鐧诲綍')

    authStore.tempToken = 'temp-token'
    const changeButtons = wrapper.findAll('button')
    const passwordInputs = wrapper.findAll('input')
    await passwordInputs[2].setValue('old-pass')
    await passwordInputs[3].setValue('new-pass')
    await changeButtons[1].trigger('click')
    await flushPromises()

    expect(changePassword).toHaveBeenCalledWith('temp-token', {
      oldPassword: 'old-pass',
      newPassword: 'new-pass',
    })
    expect(authStore.setSession).toHaveBeenCalledWith({
      token: 'formal-token',
      employeeId: 9,
    })
    expect(routerPush).toHaveBeenCalledWith({ name: 'projects' })
  })

  it('loads project overview data and creates a project from the projects view', async () => {
    queryProjects.mockResolvedValue({
      list: [
        {
          projectId: 201,
          projectName: 'Apollo',
          projectPhase: 3,
          securityLevel: 2,
          ownerId: 101,
        },
      ],
      total: 1,
    })
    createProject.mockResolvedValue({ projectId: 202 })

    const wrapper = mountView(ProjectsView)
    await flushPromises()

    expect(queryProjects).toHaveBeenCalledTimes(1)

    const setup = wrapper.vm.$.setupState
    setup.openCreateDialog()
    setup.createForm.projectName = 'Beacon'
    setup.createForm.projectPhase = 1
    setup.createForm.securityLevel = 2
    setup.createForm.ownerId = '101'
    await setup.handleCreateProject()
    await flushPromises()

    expect(createProject).toHaveBeenCalledWith({
      projectName: 'Beacon',
      projectPhase: 1,
      securityLevel: 2,
      ownerId: 101,
    })
    expect(ElMessage.success).toHaveBeenCalled()
    expect(queryProjects).toHaveBeenCalledTimes(2)
  })

  it('loads project detail, members, assets, and owner preview in the project detail view', async () => {
    authStore.profile = {
      employeeId: 101,
      employeeCode: 'E101',
      employeeName: 'Owner',
      deptType: 'MANAGEMENT',
      levelRank: 9,
      capabilities: [],
    }
    getProject.mockResolvedValue({
      projectId: 201,
      projectName: 'Apollo',
      projectPhase: 3,
      securityLevel: 2,
      ownerId: 101,
    })
    getAssetsByProject.mockResolvedValue([
      {
        assetId: 401,
        assetName: 'Design Spec',
        assetsStage: 2,
        securityLevel: 2,
      },
    ])
    listProjectMembers.mockResolvedValue([
      {
        employeeId: 101,
        employeeName: 'Owner',
        status: 'ACTIVE',
      },
    ])
    getPhaseOwnerPreview.mockResolvedValue({
      configured: true,
      employeeId: 102,
      employeeCode: 'E102',
      employeeName: 'Next Owner',
      deptName: 'QA',
    })

    const wrapper = mountView(ProjectDetailView)
    await flushPromises()

    expect(getProject).toHaveBeenCalledWith(201)
    expect(getAssetsByProject).toHaveBeenCalledWith(201)
    expect(listProjectMembers).toHaveBeenCalledWith(201)
    expect(getPhaseOwnerPreview).toHaveBeenCalledWith(201, 4)
    expect(wrapper.text()).toContain('Apollo')
  })

  it('loads assets and resets asset filters in the assets view', async () => {
    queryAssets.mockResolvedValue({
      list: [
        {
          assetId: 401,
          projectId: 201,
          assetName: 'Design Spec',
          assetsType: 2,
          assetsStage: 2,
          securityLevel: 2,
        },
      ],
      total: 1,
    })

    const wrapper = mountView(AssetsView)
    await flushPromises()

    expect(queryAssets).toHaveBeenCalledTimes(1)

    const setup = wrapper.vm.$.setupState
    setup.filters.projectId = '201'
    setup.filters.assetName = 'Spec'
    setup.resetFilters()
    await flushPromises()

    expect(setup.filters.projectId).toBe('')
    expect(setup.filters.assetName).toBe('')
    expect(queryAssets).toHaveBeenCalledTimes(2)
  })

  it('uploads a managed file and refreshes file center data', async () => {
    queryAssets
      .mockResolvedValueOnce({ list: [], total: 0 })
      .mockResolvedValueOnce({ list: [], total: 0 })
      .mockResolvedValueOnce({ list: [{ assetId: 401 }], total: 1 })
      .mockResolvedValueOnce({ list: [{ assetId: 401 }], total: 1 })
    getProject.mockResolvedValue({
      projectId: 201,
      projectPhase: 'DEVELOPMENT',
    })
    uploadAsset.mockResolvedValue({ assetId: 401 })

    const wrapper = mountView(FileCenterView)
    await flushPromises()

    const setup = wrapper.vm.$.setupState
    setup.uploadForm.projectId = '201'
    setup.uploadForm.assetName = 'api-spec.docx'
    setup.uploadForm.assetsType = 1
    setup.uploadForm.securityLevel = 2
    setup.selectedFile.value = new File(['demo'], 'api-spec.docx', { type: 'text/plain' })

    await setup.handleUpload()
    await flushPromises()

    expect(getProject).toHaveBeenCalledWith(201)
    expect(uploadAsset).toHaveBeenCalled()
    expect(ElMessage.success).toHaveBeenCalled()
    expect(queryAssets).toHaveBeenCalledTimes(4)
  })

  it('loads onboarding options and handover todos in the workbench view', async () => {
    getEmployeeOnboardOptions.mockResolvedValue({
      departments: [{ deptId: 1, deptName: 'HR' }],
      branches: [{ branchId: 1, branchName: 'Shanghai' }],
    })
    queryManagerHandoverTodos.mockResolvedValue([
      {
        deptId: 2,
        deptName: 'R&D',
        affectedProjectCount: 2,
      },
    ])

    const wrapper = mountView(HandoverWorkbenchView)
    await flushPromises()

    expect(getEmployeeOnboardOptions).toHaveBeenCalledTimes(1)
    expect(queryManagerHandoverTodos).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('R&D')
  })

  it('creates an employee and assigns a new department manager in the workbench view', async () => {
    getEmployeeOnboardOptions.mockResolvedValue({
      departments: [{ deptId: 1, deptName: 'HR', deptType: 'HR' }],
      branches: [{ branchId: 1, branchName: 'Shanghai' }],
    })
    queryManagerHandoverTodos.mockResolvedValue([
      {
        deptId: 2,
        deptName: 'R&D',
        managerName: 'Old Manager',
        affectedProjectCount: 2,
      },
    ])
    createEmployee.mockResolvedValue({
      employeeCode: 'E200',
      employeeName: 'New Hire',
      initialPassword: 'Init123!',
      mustChangePassword: true,
    })
    queryManagerCandidates.mockResolvedValue([
      {
        employeeId: 108,
        employeeCode: 'E108',
        employeeName: 'Candidate',
      },
    ])
    assignDepartmentManager.mockResolvedValue({})

    const wrapper = mountView(HandoverWorkbenchView)
    await flushPromises()

    const setup = wrapper.vm.$.setupState
    setup.onboardForm.employeeName = 'New Hire'
    setup.onboardForm.deptId = 1
    setup.onboardForm.branchId = 1
    setup.onboardForm.level = 4
    await setup.handleCreateEmployee()
    await flushPromises()

    expect(createEmployee).toHaveBeenCalled()

    await setup.openAssignDialog({
      deptId: 2,
      deptName: 'R&D',
      managerName: 'Old Manager',
      affectedProjectCount: 2,
    })
    await flushPromises()
    setup.assignForm.newManagerEmployeeId = 108
    await setup.submitAssignment()
    await flushPromises()

    expect(queryManagerCandidates).toHaveBeenCalledWith(2)
    expect(assignDepartmentManager).toHaveBeenCalledWith({
      deptId: 2,
      newManagerEmployeeId: 108,
    })
  })

  it('loads audit logs and opens detail dialog in the audit view', async () => {
    queryAuditLogs.mockResolvedValue({
      list: [
        {
          logId: 1,
          action: 'READ',
          decision: 'ALLOW',
          requestTime: '2026-04-26 10:00:00',
        },
      ],
      total: 1,
    })

    const wrapper = mountView(AuditLogsView)
    await flushPromises()

    expect(queryAuditLogs).toHaveBeenCalledTimes(1)

    const setup = wrapper.vm.$.setupState
    setup.openDetail({
      logId: 1,
      action: 'READ',
      decision: 'ALLOW',
      detailJson: '{"ok":true}',
    })
    await flushPromises()

    expect(setup.detailDialogVisible.value).toBe(true)
    expect(setup.selectedLog.value.logId).toBe(1)
  })

  it('loads policy configs and persists updates in the policy config view', async () => {
    listPolicyConfigs.mockResolvedValue([
      {
        policyName: 'SecurityLevelPolicy',
        enabled: true,
        conditions: {
          publicMinRank: 1,
          internalMinRank: 3,
          confidentialMinRank: 5,
          topSecretMinRank: 9,
        },
      },
      {
        policyName: 'EnvironmentAccessPolicy',
        enabled: true,
        conditions: {
          workStart: '08:00',
          workEnd: '20:00',
        },
      },
      {
        policyName: 'HistoricalExportPolicy',
        enabled: true,
        conditions: {
          exportThreshold: 50,
          exportWindowMinutes: 30,
        },
      },
    ])
    updatePolicyConfig.mockResolvedValue({
      policyName: 'EnvironmentAccessPolicy',
      displayName: 'EnvironmentAccessPolicy',
      enabled: false,
      conditions: {
        workStart: '09:00',
        workEnd: '21:00',
      },
    })

    const wrapper = mountView(PolicyConfigView)
    await flushPromises()

    expect(listPolicyConfigs).toHaveBeenCalledTimes(1)

    const setup = wrapper.vm.$.setupState
    setup.forms.EnvironmentAccessPolicy.enabled = false
    setup.forms.EnvironmentAccessPolicy.conditions.workStart = '09:00'
    setup.forms.EnvironmentAccessPolicy.conditions.workEnd = '21:00'
    await setup.savePolicy('EnvironmentAccessPolicy')
    await flushPromises()

    expect(updatePolicyConfig).toHaveBeenCalledWith('EnvironmentAccessPolicy', {
      enabled: false,
      conditions: {
        workStart: '09:00',
        workEnd: '21:00',
      },
    })
    expect(ElMessage.success).toHaveBeenCalled()
  })
})
