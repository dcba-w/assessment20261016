import dayjs from 'dayjs'
import type { Nl2sqlQueryRespVO } from '@/api/nl2sql'

export type VizKind = 'kpi' | 'bar' | 'pie' | 'line' | 'table'
export type SwitchableKind = 'bar' | 'pie' | 'line' | 'table'

export interface VizSpec {
  kind: VizKind
  categoryKey?: string
  valueKey?: string
  switchable: SwitchableKind[]
}

const DATE_NAME = /日期|时间|date|time|day|month/i
const PIE_HINT = /占比|比例|份额/
const LINE_HINT = /趋势|走势|按日|按月/
const COUNT_NAME = /count|cnt|total|数量|人数|sum/i
const ID_NAME = /(^id$|_id$)/i
const EPOCH_MS_MIN = Date.UTC(2000, 0, 1)
const EPOCH_MS_MAX = Date.UTC(2100, 0, 1)
const SAMPLE = 8
const BAR_MAX = 20
const PIE_MAX = 12

export function resultColumns(result: Nl2sqlQueryRespVO): string[] {
  if (result.columns?.length) return result.columns
  const first = result.rows?.[0]
  return first ? Object.keys(first) : []
}

export function isIdColumn(key: string): boolean {
  return ID_NAME.test((key || '').trim())
}

/** 界面展示列：尽量隐藏 id / *_id，若去掉后没有列则仍展示全部 */
export function displayColumns(result: Nl2sqlQueryRespVO): string[] {
  const columns = resultColumns(result)
  const visible = columns.filter((key) => !isIdColumn(key))
  return visible.length ? visible : columns
}

export function columnLabel(result: Nl2sqlQueryRespVO, key: string): string {
  const columns = result.columns || []
  const labels = result.columnLabels || []
  const index = columns.findIndex((col) => col === key)
  const label = index >= 0 ? labels[index] : undefined
  if (typeof label === 'string' && label.trim()) return label.trim()
  return key
}

export function toFiniteNumber(value: unknown): number | null {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'bigint') {
    const n = Number(value)
    return Number.isFinite(n) ? n : null
  }
  if (typeof value === 'string' && value.trim() !== '') {
    const n = Number(value)
    if (Number.isFinite(n)) return n
  }
  return null
}

export function cellText(value: unknown): string {
  if (value == null) return ''
  const dateText = formatDateTimeValue(value)
  if (dateText) return dateText
  if (typeof value === 'string') return value
  if (typeof value === 'number' || typeof value === 'boolean') return String(value)
  try {
    return JSON.stringify(value)
  } catch {
    return String(value)
  }
}

/** 毫秒/秒时间戳或日期字符串 → YYYY-MM-DD，有时分秒则带上时间 */
export function formatDateTimeValue(value: unknown): string | null {
  const date = toDate(value)
  if (!date) return null
  const parsed = dayjs(date)
  if (!parsed.isValid()) return null
  if (
    date.getUTCHours() === 0 &&
    date.getUTCMinutes() === 0 &&
    date.getUTCSeconds() === 0 &&
    date.getUTCMilliseconds() === 0
  ) {
    return date.toISOString().slice(0, 10)
  }
  if (parsed.hour() === 0 && parsed.minute() === 0 && parsed.second() === 0 && parsed.millisecond() === 0) {
    return parsed.format('YYYY-MM-DD')
  }
  return parsed.format('YYYY-MM-DD HH:mm:ss')
}

function toDate(value: unknown): Date | null {
  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value
  }
  if (typeof value === 'bigint') {
    return fromEpoch(Number(value))
  }
  if (typeof value === 'number') {
    return fromEpoch(value)
  }
  if (typeof value !== 'string') return null
  const text = value.trim()
  if (!text) return null
  if (/^-?\d+$/.test(text)) return fromEpoch(Number(text))
  if (!/^\d{4}-\d{2}-\d{2}/.test(text) && !text.includes('T')) return null
  const parsed = dayjs(text)
  return parsed.isValid() ? parsed.toDate() : null
}

function fromEpoch(value: number): Date | null {
  if (!Number.isFinite(value)) return null
  let millis = value
  if (value >= EPOCH_MS_MIN / 1000 && value <= EPOCH_MS_MAX / 1000) {
    millis = value * 1000
  } else if (value < EPOCH_MS_MIN || value > EPOCH_MS_MAX) {
    return null
  }
  const date = new Date(millis)
  return Number.isNaN(date.getTime()) ? null : date
}

function sampleValues(rows: Record<string, unknown>[], key: string): unknown[] {
  return rows.slice(0, SAMPLE).map((row) => row[key])
}

function isNumericColumn(rows: Record<string, unknown>[], key: string): boolean {
  const values = sampleValues(rows, key).filter((value) => value != null && value !== '')
  if (!values.length) return false
  return values.every((value) => toFiniteNumber(value) != null)
}

function isDateLikeValue(value: unknown): boolean {
  return toDate(value) != null
}

function isDateColumn(key: string, rows: Record<string, unknown>[]): boolean {
  if (DATE_NAME.test(key)) return true
  const values = sampleValues(rows, key).filter((value) => value != null && value !== '')
  if (!values.length) return false
  return values.every(isDateLikeValue)
}

function allNonNegative(rows: Record<string, unknown>[], valueKey: string): boolean {
  return rows.every((row) => {
    const n = toFiniteNumber(row[valueKey])
    return n != null && n >= 0
  })
}

export function classifyNl2sqlViz(result: Nl2sqlQueryRespVO, question: string): VizSpec | null {
  const rows = result.rows || []
  if (!rows.length) return null
  const columns = resultColumns(result)
  if (!columns.length) return null

  const dateKeys = columns.filter((key) => isDateColumn(key, rows))
  const numericKeys = columns.filter((key) => !dateKeys.includes(key) && isNumericColumn(rows, key))
  const categoryKeys = columns.filter((key) => !dateKeys.includes(key) && !numericKeys.includes(key))

  const valueKey =
    numericKeys.find((key) => COUNT_NAME.test(key)) ||
    numericKeys.find((key) => !ID_NAME.test(key)) ||
    numericKeys[0]
  const categoryKey = categoryKeys.find((key) => !ID_NAME.test(key)) || categoryKeys[0]
  const dateKey = dateKeys[0]
  const axisKey = dateKey || categoryKey
  const q = question || ''
  const used = [categoryKey, dateKey, valueKey].filter(Boolean) as string[]
  const extraAreIds = columns.every((key) => used.includes(key) || ID_NAME.test(key))

  if (rows.length === 1 && extraAreIds && valueKey) {
    return { kind: 'kpi', valueKey, categoryKey: axisKey, switchable: [] }
  }

  const wantLine = LINE_HINT.test(q)
  if (extraAreIds && valueKey && axisKey && rows.length >= 2 && (dateKey || wantLine)) {
    return { kind: 'line', categoryKey: axisKey, valueKey, switchable: ['line', 'table'] }
  }

  if (extraAreIds && categoryKey && valueKey && rows.length >= 2 && rows.length <= BAR_MAX) {
    const canPie = rows.length <= PIE_MAX && allNonNegative(rows, valueKey)
    const switchable: SwitchableKind[] = canPie ? ['bar', 'pie', 'table'] : ['bar', 'table']
    if (canPie && PIE_HINT.test(q)) {
      return { kind: 'pie', categoryKey, valueKey, switchable }
    }
    return { kind: 'bar', categoryKey, valueKey, switchable }
  }

  return { kind: 'table', switchable: [] }
}
