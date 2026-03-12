export const projectPhaseOptions = [
  { value: 1, key: 'INIT', label: '立项' },
  { value: 2, key: 'REQUIREMENT', label: '需求设计' },
  { value: 3, key: 'DEVELOPMENT', label: '研发实现' },
  { value: 4, key: 'TEST', label: '测试验证' },
  { value: 5, key: 'RELEASE', label: '上线交付' },
  { value: 6, key: 'ARCHIVED', label: '归档' },
]

export const securityLevelOptions = [
  { value: 1, key: 'PUBLIC', label: '公开' },
  { value: 2, key: 'INTERNAL', label: '内部' },
  { value: 3, key: 'CONFIDENTIAL', label: '机密' },
  { value: 4, key: 'TOP_SECRET', label: '高度机密' },
]

export const assetTypeOptions = [
  { value: 1, key: 'REQUIREMENT_DOC', label: '需求文档' },
  { value: 2, key: 'DESIGN_DOC', label: '设计文档' },
  { value: 3, key: 'SOURCE_CODE', label: '源代码' },
  { value: 4, key: 'TEST_REPORT', label: '测试报告' },
  { value: 5, key: 'DEPLOY_SCRIPT', label: '部署脚本' },
  { value: 6, key: 'OPS_DOC', label: '运维文档' },
]

export function findOption(options, rawValue) {
  return options.find(
    (option) => option.value === Number(rawValue) || option.key === rawValue,
  )
}

export function getOptionLabel(options, rawValue) {
  return findOption(options, rawValue)?.label || rawValue || '未设置'
}
