import { getAiCommandEventList, saveAiCommandEvent, deleteAiCommandEvent } from '@/api/system/aiCommandEvent'

export interface RecordedAction {
  tool: string
  input: unknown
  elementText?: string
}

const STORAGE_KEY = 'simple-page-agent-recordings'
const LAST_KEY = 'simple-page-agent-last-recording'
const MAX_RECORDINGS = 30

export interface StoredRecording {
  id?: number
  task: string
  steps: RecordedAction[]
  at: number
}

const REPEAT_TASK = /^(再来一次|再做一次|再操作一次|重新执行|重新操作一次|重做|再执行一次)$/

let cache: StoredRecording[] = readAll()

function taskKey(task: string): string {
  return task.trim().replace(/\s+/g, ' ')
}

function readAll(): StoredRecording[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const parsed = raw ? (JSON.parse(raw) as StoredRecording[]) : []
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function readLast(): StoredRecording | null {
  try {
    const raw = localStorage.getItem(LAST_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as StoredRecording
    return parsed?.steps?.length ? parsed : null
  } catch {
    return null
  }
}

function writeCache(next: StoredRecording[]) {
  cache = next.slice(0, MAX_RECORDINGS)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(cache))
  const last = cache.find((item) => item.steps.length)
  if (last) {
    localStorage.setItem(LAST_KEY, JSON.stringify(last))
  } else {
    localStorage.removeItem(LAST_KEY)
  }
}

function toId(id: unknown): number | undefined {
  if (typeof id === 'number' && Number.isFinite(id) && id > 0) return id
  if (typeof id === 'string' && /^\d+$/.test(id)) {
    const parsed = Number(id)
    if (Number.isFinite(parsed) && parsed > 0) return parsed
  }
  return undefined
}

function isValidId(id: unknown): id is number {
  return toId(id) != null
}

function isSameRecording(a: StoredRecording, b: StoredRecording): boolean {
  const aId = toId(a.id)
  const bId = toId(b.id)
  if (aId && bId) {
    return aId === bId
  }
  return a.task === b.task && a.at === b.at
}

function removeOneRecording(list: StoredRecording[], record: StoredRecording): StoredRecording[] {
  const index = list.findIndex((item) => isSameRecording(item, record))
  if (index < 0) return list
  return [...list.slice(0, index), ...list.slice(index + 1)]
}

function preferRecording(prev: StoredRecording, item: StoredRecording): StoredRecording {
  const keep =
    item.steps.length && !prev.steps.length
      ? item
      : prev.steps.length && !item.steps.length
        ? prev
        : item.at >= prev.at
          ? item
          : prev
  return {
    ...keep,
    id: toId(keep.id) ?? toId(prev.id) ?? toId(item.id)
  }
}

export function listRecordings(): StoredRecording[] {
  return cache
}

export function matchRecording(task: string): RecordedAction[] | null {
  const key = taskKey(task)
  const found = REPEAT_TASK.test(key) ? readLast() ?? cache[0] : cache.find((item) => item.task === key)
  if (!found?.steps.length) return null
  if (found.steps.some((step) => step.tool === 'ask_user')) return null
  return found.steps
}

function toTime(value?: string | number): number {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string' && value) {
    const parsed = Date.parse(value)
    if (!Number.isNaN(parsed)) return parsed
  }
  return Date.now()
}

export async function refreshRecordings(): Promise<StoredRecording[]> {
  const local = readAll()
  try {
    const list = await getAiCommandEventList()
    if (Array.isArray(list)) {
      const remote = list
        .filter((item) => item?.task)
        .map((item) => ({
          id: toId(item.id),
          task: taskKey(item.task),
          steps: Array.isArray(item.steps) ? item.steps : [],
          at: toTime(item.updateTime)
        }))
      const consumed = new Set<StoredRecording>()
      const merged: StoredRecording[] = []
      for (const remoteItem of remote) {
        const matched = local.filter((item) => {
          if (consumed.has(item)) return false
          const localId = toId(item.id)
          if (localId && remoteItem.id) return localId === remoteItem.id
          return !localId && item.task === remoteItem.task
        })
        let keep = remoteItem
        for (const item of matched) {
          consumed.add(item)
          keep = preferRecording(keep, item)
        }
        merged.push(keep)
      }
      for (const item of local) {
        if (consumed.has(item)) continue
        const existing = merged.find((row) => {
          const rowId = toId(row.id)
          const localId = toId(item.id)
          if (rowId && localId) return rowId === localId
          return !localId && row.task === item.task
        })
        if (existing) {
          merged[merged.indexOf(existing)] = preferRecording(existing, item)
          continue
        }
        merged.push({ ...item, id: toId(item.id) })
      }
      writeCache(merged.sort((a, b) => b.at - a.at))
    }
  } catch {
    cache = local
  }
  return cache
}

export function saveRecording(task: string, steps: RecordedAction[]) {
  const key = taskKey(task)
  if (!key || REPEAT_TASK.test(key)) return
  const prev = cache.find((item) => item.task === key)
  const next: StoredRecording = { id: prev?.id, task: key, steps: steps ?? [], at: Date.now() }
  writeCache([next, ...cache.filter((item) => !isSameRecording(item, next) && item.task !== key)])
  if (!next.steps.length) return
  void saveAiCommandEvent({
    task: key,
    pagePath: window.location.pathname,
    steps: steps.map((step) => ({
      tool: step.tool,
      input: step.input,
      elementText: step.elementText
    }))
  })
    .then((id) => {
      if (!isValidId(id)) return
      const latest = cache.find((item) => item.task === key && item.at === next.at) ?? cache.find((item) => item.task === key)
      if (!latest || latest.id === id) return
      writeCache(cache.map((item) => (item === latest ? { ...item, id } : item)))
    })
    .catch(() => undefined)
}

export async function deleteRecording(record: StoredRecording): Promise<StoredRecording[]> {
  writeCache(removeOneRecording(cache, record))
  const id = toId(record.id)
  if (id) {
    try {
      await deleteAiCommandEvent(id)
    } catch {
      // 远端没有这条记录时，本地已经删掉即可
    }
  }
  return cache
}
