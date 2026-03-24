/**
 * 应用配置
 * 从环境变量中读取配置
 */

// API 基础地址
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

// 应用部署域名
export const DEPLOY_DOMAIN = import.meta.env.VITE_DEPLOY_DOMAIN || 'http://localhost'

// 应用预览域名
export const PREVIEW_DOMAIN = import.meta.env.VITE_PREVIEW_DOMAIN || 'http://localhost:8124/api'

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
  return `${PREVIEW_DOMAIN}/static/${codeGenType}_${appId}/index.html`
}

/**
 * 获取 SSE 连接地址
 * @param appId 应用ID
 * @param message 消息内容
 */
export function getSSEUrl(appId: string, message: string): string {
  return `${API_BASE_URL}/app/chat/gen/code?appId=${appId}&message=${encodeURIComponent(message)}`
}
