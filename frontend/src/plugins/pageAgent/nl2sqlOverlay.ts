import type { EChartsOption } from 'echarts'
import type { Nl2sqlQueryRespVO } from '@/api/nl2sql'
import echarts from '@/plugins/echarts'
import {
  cellText,
  classifyNl2sqlViz,
  columnLabel,
  displayColumns,
  toFiniteNumber,
  type SwitchableKind,
  type VizKind,
  type VizSpec
} from './nl2sqlViz'

const OVERLAY_ID = 'page-agent-nl2sql-overlay'
const CHART_COLOR = '#2f6bff'

let chart: echarts.ECharts | null = null
let onResize: (() => void) | null = null
let onKeydown: ((event: KeyboardEvent) => void) | null = null

export function openNl2sqlOverlay(result: Nl2sqlQueryRespVO, question: string) {
  const spec = classifyNl2sqlViz(result, question)
  if (!spec) return

  closeNl2sqlOverlay()

  const root = document.createElement('div')
  root.id = OVERLAY_ID
  root.setAttribute('data-page-agent-ignore', 'true')
  root.setAttribute('data-browser-use-ignore', 'true')
  root.style.cssText =
    'position:fixed;inset:0;z-index:2147483646;display:flex;align-items:center;justify-content:center;background:rgba(18,32,64,.35);font:13px/1.45 "Microsoft YaHei",sans-serif;color:#243044;'

  const dialog = document.createElement('div')
  dialog.style.cssText =
    'width:min(860px,calc(100vw - 32px));height:min(560px,calc(100vh - 48px));display:flex;flex-direction:column;background:#fff;border-radius:18px;box-shadow:0 18px 48px rgba(22,58,138,.22);overflow:hidden;'
  dialog.addEventListener('click', (event) => event.stopPropagation())

  const header = document.createElement('div')
  header.style.cssText =
    'display:flex;align-items:center;justify-content:space-between;padding:14px 16px 8px;flex-shrink:0;'
  const title = document.createElement('div')
  title.textContent = '查询结果'
  title.style.fontWeight = '600'
  const close = button('关闭', 'transparent', '#5b6b82')
  close.addEventListener('click', () => closeNl2sqlOverlay())
  header.append(title, close)

  const answer = document.createElement('div')
  answer.style.cssText = 'padding:0 16px 10px;color:#3d4f66;flex-shrink:0;white-space:pre-wrap;'
  const answerText = (result.answer || '').trim()
  if (answerText) answer.textContent = answerText
  else answer.style.display = 'none'

  const toolbar = document.createElement('div')
  toolbar.style.cssText = 'display:flex;gap:8px;padding:0 16px 10px;flex-shrink:0;'
  let currentKind: VizKind = spec.kind

  const body = document.createElement('div')
  body.style.cssText = 'flex:1;display:flex;flex-direction:column;min-height:0;border-top:1px solid #e6eefc;'

  const visual = document.createElement('div')
  visual.style.cssText = 'flex:1;display:flex;flex-direction:column;min-height:0;padding:12px 16px 0;'

  const chartEl = document.createElement('div')
  chartEl.style.cssText = 'width:100%;flex:1;min-height:280px;'

  const kpiEl = document.createElement('div')
  kpiEl.style.cssText =
    'min-height:120px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:6px;'

  const tableWrap = document.createElement('div')
  tableWrap.style.cssText = 'flex:1;min-height:0;overflow:auto;padding:12px 16px 16px;'
  tableWrap.appendChild(buildTable(result))

  const paintSwitch = () => {
    toolbar.innerHTML = ''
    if (!spec.switchable.length) {
      toolbar.style.display = 'none'
      return
    }
    toolbar.style.display = 'flex'
    for (const kind of spec.switchable) {
      const active = currentKind === kind
      const btn = button(kindLabel(kind), active ? CHART_COLOR : '#f4f8ff', active ? '#fff' : '#243044')
      btn.addEventListener('click', () => {
        currentKind = kind
        paintSwitch()
        renderVisual()
      })
      toolbar.appendChild(btn)
    }
  }

  const renderVisual = () => {
    disposeChart()
    visual.innerHTML = ''
    const chartTabs = spec.switchable.some((kind) => kind !== 'table')
    if (currentKind === 'table') {
      visual.style.display = 'none'
      tableWrap.style.display = ''
      return
    }
    visual.style.display = ''
    tableWrap.style.display = chartTabs ? 'none' : ''
    if (currentKind === 'kpi') {
      visual.appendChild(renderKpi(result, spec, kpiEl))
      return
    }
    visual.appendChild(chartEl)
    window.requestAnimationFrame(() => {
      if (!chartEl.isConnected) return
      chart = echarts.init(chartEl)
      chart.setOption(chartOption(currentKind, result, spec), true)
      onResize = () => chart?.resize()
      window.addEventListener('resize', onResize)
    })
  }

  paintSwitch()
  renderVisual()
  body.append(visual, tableWrap)
  dialog.append(header, answer, toolbar, body)
  root.appendChild(dialog)
  root.addEventListener('click', () => closeNl2sqlOverlay())
  document.body.appendChild(root)

  onKeydown = (event: KeyboardEvent) => {
    if (event.key === 'Escape') closeNl2sqlOverlay()
  }
  window.addEventListener('keydown', onKeydown)
}

export function closeNl2sqlOverlay() {
  disposeChart()
  if (onKeydown) {
    window.removeEventListener('keydown', onKeydown)
    onKeydown = null
  }
  document.getElementById(OVERLAY_ID)?.remove()
}

function disposeChart() {
  if (onResize) {
    window.removeEventListener('resize', onResize)
    onResize = null
  }
  chart?.dispose()
  chart = null
}

function renderKpi(result: Nl2sqlQueryRespVO, spec: VizSpec, host: HTMLElement): HTMLElement {
  host.replaceChildren()
  const row = result.rows[0] || {}
  const value = spec.valueKey ? toFiniteNumber(row[spec.valueKey]) : null
  const number = document.createElement('div')
  number.style.cssText = 'font-size:40px;font-weight:700;color:#1a2b44;line-height:1.1;'
  number.textContent = value == null ? '—' : String(value)
  host.appendChild(number)
  if (spec.categoryKey) {
    const caption = document.createElement('div')
    caption.style.cssText = 'color:#5b6b82;'
    caption.textContent = cellText(row[spec.categoryKey])
    if (caption.textContent) host.appendChild(caption)
  }
  return host
}

function chartOption(kind: VizKind, result: Nl2sqlQueryRespVO, spec: VizSpec): EChartsOption {
  const categoryKey = spec.categoryKey || ''
  const valueKey = spec.valueKey || ''
  const labels = result.rows.map((row) => cellText(row[categoryKey]) || '—')
  const values = result.rows.map((row) => toFiniteNumber(row[valueKey]) ?? 0)

  if (kind === 'pie') {
    return {
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: ['36%', '64%'],
          data: labels.map((name, index) => ({ name, value: values[index] }))
        }
      ]
    }
  }

  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 16, top: 24, bottom: labels.length > 6 ? 56 : 32 },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: { interval: 0, rotate: labels.length > 6 ? 30 : 0 }
    },
    yAxis: { type: 'value' },
    series: [
      {
        type: kind === 'line' ? 'line' : 'bar',
        data: values,
        itemStyle: { color: CHART_COLOR },
        smooth: kind === 'line'
      }
    ]
  }
}

function buildTable(result: Nl2sqlQueryRespVO): HTMLTableElement {
  const columns = displayColumns(result)
  const table = document.createElement('table')
  table.style.cssText = 'width:100%;border-collapse:collapse;font-size:13px;'

  const thead = document.createElement('thead')
  const headRow = document.createElement('tr')
  for (const col of columns) {
    const th = document.createElement('th')
    th.textContent = columnLabel(result, col)
    th.style.cssText =
      'text-align:left;padding:8px 10px;border-bottom:1px solid #d5e4ff;background:#f7faff;color:#3d4f66;position:sticky;top:0;'
    headRow.appendChild(th)
  }
  thead.appendChild(headRow)
  table.appendChild(thead)

  const tbody = document.createElement('tbody')
  for (const row of result.rows || []) {
    const tr = document.createElement('tr')
    for (const col of columns) {
      const td = document.createElement('td')
      td.textContent = cellText(row[col])
      td.style.cssText = 'padding:8px 10px;border-bottom:1px solid #eef3fb;color:#243044;'
      tr.appendChild(td)
    }
    tbody.appendChild(tr)
  }
  table.appendChild(tbody)
  return table
}

function kindLabel(kind: SwitchableKind): string {
  if (kind === 'pie') return '饼图'
  if (kind === 'line') return '折线图'
  if (kind === 'table') return '列表'
  return '柱状图'
}

function button(text: string, background: string, color: string) {
  const el = document.createElement('button')
  el.type = 'button'
  el.textContent = text
  el.style.cssText = `border:0;border-radius:10px;background:${background};color:${color};padding:7px 12px;cursor:pointer;`
  return el
}
