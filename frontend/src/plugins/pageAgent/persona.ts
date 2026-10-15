export type PageAgentAccent = 'aurora' | 'ocean' | 'sunset'

export interface PageAgentPersona {
  name: string
  persona: string
  accent: PageAgentAccent
  model: string
  baseURL: string
  apiKey: string
  language: 'zh-CN' | 'en-US'
  /** Total token budget. Energy is 0 when this is missing or used up. */
  tokenQuota: number
}

const STORAGE_KEY = 'simple-page-agent-persona'
const ACCENTS: PageAgentAccent[] = ['aurora', 'ocean', 'sunset']

export function defaultPersona(): PageAgentPersona {
  return {
    name: '页面助手',
    persona:
      '你是 Simple Boot 管理后台的页面操作助手。用中文简要回复，优先通过点击、填写、选择来完成后台页面任务。',
    accent: 'aurora',
    model: import.meta.env.VITE_PAGE_AGENT_MODEL || 'qwen3.5-plus',
    baseURL:
      import.meta.env.VITE_PAGE_AGENT_BASE_URL ||
      'https://page-ag-testing-ohftxirgbn.cn-shanghai.fcapp.run',
    apiKey: import.meta.env.VITE_PAGE_AGENT_API_KEY || 'NA',
    language: import.meta.env.VITE_PAGE_AGENT_LANGUAGE === 'en-US' ? 'en-US' : 'zh-CN',
    tokenQuota: 100000
  }
}

export function loadPersona(): PageAgentPersona {
  const fallback = defaultPersona()
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return fallback
    const parsed = JSON.parse(raw) as Partial<PageAgentPersona>
    return {
      name: typeof parsed.name === 'string' && parsed.name.trim() ? parsed.name.trim() : fallback.name,
      persona: typeof parsed.persona === 'string' ? parsed.persona : fallback.persona,
      accent: ACCENTS.includes(parsed.accent as PageAgentAccent)
        ? (parsed.accent as PageAgentAccent)
        : fallback.accent,
      model: typeof parsed.model === 'string' && parsed.model.trim() ? parsed.model.trim() : fallback.model,
      baseURL:
        typeof parsed.baseURL === 'string' && parsed.baseURL.trim()
          ? parsed.baseURL.trim()
          : fallback.baseURL,
      apiKey: typeof parsed.apiKey === 'string' && parsed.apiKey.trim() ? parsed.apiKey.trim() : fallback.apiKey,
      language: parsed.language === 'en-US' ? 'en-US' : 'zh-CN',
      tokenQuota: parseTokenQuota(parsed.tokenQuota, fallback.tokenQuota)
    }
  } catch {
    return fallback
  }
}

function parseTokenQuota(value: unknown, fallback: number): number {
  const quota = typeof value === 'number' ? value : Number(value)
  if (!Number.isFinite(quota) || quota < 0) return fallback
  return Math.floor(quota)
}

export function savePersona(persona: PageAgentPersona) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(persona))
}

export function toSystemPrompt(persona: PageAgentPersona) {
  const name = persona.name.trim() || '页面助手'
  const body = persona.persona.trim()
  const nav =
    '用户要打开、进入或跳转到某个菜单页面时，必须先调用 open_menu，用菜单名称查询数据库里的菜单再跳转。不要点击侧边栏或顶部菜单去找页面。open_menu 返回多个结果时，用更精确的名称再查一次，或向用户确认。找不到就直接说明没有这个菜单。'
  const kb =
    '用户询问业务规则、字段口径、收费计费政策、报表含义、SOP 流程时，先调用 search_knowledge 检索业务知识库，再依据检索结果回答或操作。不要凭空猜测业务规则。'
  const data =
    '用户询问数量、统计、列表、明细、趋势等业务数据时，必须先调用 query_data，把用户原话传给后端 NL2SQL，禁止自己编造数字或手写 SQL。query_data 失败时如实告知原因，不要改用点击页面去凑数据。'
  const head = `你的名字是「${name}」。\n${nav}\n${kb}\n${data}`
  return body ? `${head}\n${body}` : head
}
