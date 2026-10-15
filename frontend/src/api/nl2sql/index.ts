import { config } from '@/config/axios/config'
import { getAccessToken, getTenantId } from '@/utils/auth'

export interface Nl2sqlQueryReqVO {
  question: string
}

export interface Nl2sqlQueryRespVO {
  answer: string
  columns: string[]
  columnLabels?: string[]
  rows: Record<string, unknown>[]
}

/** 后端模型超时默认 60s，单独放宽，避免被全局 30s 截断 */
const NL2SQL_TIMEOUT_MS = 90_000

interface CommonResult<T> {
  code?: number
  msg?: string
  data?: T
}

export const queryNl2sqlApi = async (question: string): Promise<Nl2sqlQueryRespVO> => {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json'
  }
  const token = getAccessToken()
  if (token) headers.Authorization = `Bearer ${token}`
  const tenantId = getTenantId()
  if (tenantId != null && tenantId !== '') headers['tenant-id'] = String(tenantId)

  const controller = new AbortController()
  const timer = window.setTimeout(() => controller.abort(), NL2SQL_TIMEOUT_MS)
  try {
    const res = await fetch(`${config.base_url}/nl2sql/query`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ question } satisfies Nl2sqlQueryReqVO),
      signal: controller.signal
    })
    const body = (await res.json().catch(() => null)) as CommonResult<Nl2sqlQueryRespVO> | null
    if (!body) {
      throw new Error('无法解析后端响应')
    }
    if (body.code !== 0 && body.code !== 200) {
      throw new Error(body.msg || '查询失败')
    }
    if (!body.data) {
      throw new Error('后端没有返回结果')
    }
    return body.data
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new Error('查询超时，请稍后重试')
    }
    throw error
  } finally {
    window.clearTimeout(timer)
  }
}
