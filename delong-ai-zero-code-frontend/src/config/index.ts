/**
 * 应用配置
 * 从环境变量中读取配置
 */
import { CODE_GEN_TYPE } from '@/constants/app.ts'

// API 基础地址
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

// 应用部署域名
export const DEPLOY_DOMAIN = import.meta.env.VITE_DEPLOY_DOMAIN || 'http://localhost'

// 应用预览域名 - 使用相对路径确保同源
export const PREVIEW_DOMAIN = '/api'

/**
 * 获取部署后的应用访问地址
 * @param deployKey 部署密钥
 */
export function getDeployedAppUrl(deployKey: string): string {
  return `${DEPLOY_DOMAIN}/${deployKey}`
}

/**
 * 获取应用预览地址
 * @param codeGenType 生成类型
 * @param appId 应用ID
 */
export function getAppPreviewUrl(codeGenType: string, appId: string | number): string {
  const baseDir = `${PREVIEW_DOMAIN}/static/${codeGenType}_${appId}`
  if (codeGenType === CODE_GEN_TYPE.VUE_PROJECT) {
    // Vue 项目：构建产物在 dist 目录下
    return `${baseDir}/dist/index.html`
  }
  // HTML/多文件模式：后端会自动重定向到 index.html
  return `${baseDir}/`
}

/**
 * 获取 SSE 连接地址
 * @param appId 应用ID
 * @param message 消息内容
 */
export function getSSEUrl(appId: string, message: string): string {
  return `${API_BASE_URL}/app/chat/gen/code?appId=${appId}&message=${encodeURIComponent(message)}`
}
