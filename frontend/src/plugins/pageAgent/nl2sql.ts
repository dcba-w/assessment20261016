import { queryNl2sqlApi, type Nl2sqlQueryRespVO } from '@/api/nl2sql'
import { openNl2sqlOverlay } from './nl2sqlOverlay'
import { columnLabel, displayColumns, cellText } from './nl2sqlViz'

const MAX_PREVIEW_ROWS = 10
const MAX_CELL_CHARS = 40

const DATA_QUESTION_HINTS = [
  '多少',
  '几个',
  '几条',
  '几座',
  '统计',
  '查询',
  '查一下',
  '查下',
  '条记录',
  '列表',
  '明细',
  '趋势',
  '汇总',
  '总数',
  '数量'
]

export function isDataQuestion(question: string): boolean {
  const text = (question || '').trim()
  if (!text) return false
  return DATA_QUESTION_HINTS.some((hint) => text.includes(hint))
}

function clip(text: string): string {
  const normalized = text.replace(/\s+/g, ' ').trim()
  if (normalized.length <= MAX_CELL_CHARS) return normalized
  return `${normalized.slice(0, MAX_CELL_CHARS - 1)}…`
}

function formatTable(result: Nl2sqlQueryRespVO): string {
  const columns = displayColumns(result)
  const rows = result.rows || []
  if (!columns.length || !rows.length) return ''

  const preview = rows.slice(0, MAX_PREVIEW_ROWS)
  const header = columns.map((col) => columnLabel(result, col)).join(' | ')
  const divider = columns.map(() => '---').join('|')
  const body = preview
    .map((row) => columns.map((col) => clip(cellText(row[col]))).join(' | '))
    .join('\n')
  const extra =
    rows.length > MAX_PREVIEW_ROWS ? `\n…共 ${rows.length} 行，仅展示前 ${MAX_PREVIEW_ROWS} 行` : ''
  return `${header}\n${divider}\n${body}${extra}`
}

function formatResult(result: Nl2sqlQueryRespVO): string {
  const parts: string[] = []
  const answer = (result.answer || '').trim()
  if (answer) parts.push(answer)
  const table = formatTable(result)
  if (table) parts.push(table)
  return parts.join('\n\n') || '查询完成，但没有返回可展示的内容。'
}

function errorMessage(error: unknown): string {
  if (error instanceof Error && error.message && error.message !== 'error') {
    return error.message
  }
  if (typeof error === 'string' && error.trim() && error !== 'error') {
    return error
  }
  if (error && typeof error === 'object') {
    const record = error as { msg?: unknown; message?: unknown; response?: { data?: { msg?: unknown } } }
    const msg = record.response?.data?.msg || record.msg || record.message
    if (typeof msg === 'string' && msg.trim() && msg !== 'error') return msg
  }
  return '查询失败。请确认已登录，且后端已配置 NL2SQL 模型 Key。'
}

export async function queryData(question: string): Promise<string> {
  const q = (question || '').trim()
  if (!q) return '没有给出要查询的问题。'
  try {
    const result = await queryNl2sqlApi(q)
    if (!result) return '查询失败：后端没有返回结果。'
    if (result.rows?.length) openNl2sqlOverlay(result, q)
    return formatResult(result)
  } catch (error) {
    return `查询失败：${errorMessage(error)}`
  }
}
