/**
 * 应用相关常量
 */

/**
 * 代码生成类型枚举
 */
export const CODE_GEN_TYPE = {
  HTML: 'html',
  MULTI_FILE: 'multi_file',
  VUE_PROJECT: 'vue_project',
} as const

/**
 * 代码生成类型选项（用于下拉选择）
 */
export const CODE_GEN_TYPE_OPTIONS = [
  { label: '原生 HTML 模式', value: CODE_GEN_TYPE.HTML },
  { label: '原生多文件模式', value: CODE_GEN_TYPE.MULTI_FILE },
  {label: 'Vue项目模式', value: CODE_GEN_TYPE.VUE_PROJECT },
]

/**
 * 代码生成类型映射（值 -> 标签）
 */
export const CODE_GEN_TYPE_MAP: Record<string, string> = {
  [CODE_GEN_TYPE.HTML]: '原生 HTML 模式',
  [CODE_GEN_TYPE.MULTI_FILE]: '原生多文件模式',
  [CODE_GEN_TYPE.VUE_PROJECT]: 'Vue项目模式'
}

/**
 * 获取代码生成类型的显示名称
 */
export function getCodeGenTypeLabel(type: string | undefined): string {
  if (!type) return '-'
  return CODE_GEN_TYPE_MAP[type] || type
}
