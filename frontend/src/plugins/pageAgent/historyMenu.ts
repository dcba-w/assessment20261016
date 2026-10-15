import type { StoredRecording } from './recordings'

const MENU_ID = 'page-agent-command-history'

export function openCommandHistory(
  records: StoredRecording[],
  onPick: (task: string | null) => void,
  onDelete?: (record: StoredRecording) => Promise<StoredRecording[]> | StoredRecording[]
): void {
  closeCommandHistory()
  const menu = document.createElement('div')
  menu.id = MENU_ID
  menu.setAttribute('data-page-agent-ignore', 'true')
  menu.setAttribute('data-browser-use-ignore', 'true')
  menu.style.cssText = [
    'position:fixed',
    'z-index:2147483646',
    'width:300px',
    'max-height:360px',
    'display:flex',
    'flex-direction:column',
    'padding:8px',
    'border:1px solid #d5e4ff',
    'border-radius:16px',
    'background:#fff',
    'box-shadow:0 12px 28px rgba(22,58,138,.16)',
    'color:#243044',
    'font:13px/1.4 "Microsoft YaHei",sans-serif'
  ].join(';')
  const launcher = document.getElementById('pm-assistant-launcher')
  const rect = launcher?.getBoundingClientRect()
  const menuWidth = 300
  if (rect) {
    const left = rect.left > menuWidth + 24 ? rect.left - menuWidth - 12 : rect.right + 12
    const top = Math.min(Math.max(8, rect.top), Math.max(8, window.innerHeight - 220))
    menu.style.left = `${Math.max(8, left)}px`
    menu.style.top = `${top}px`
  } else {
    menu.style.right = '24px'
    menu.style.bottom = '24px'
  }

  const title = document.createElement('div')
  title.textContent = '过往指令'
  title.style.cssText = 'padding:6px 8px 8px;font-weight:600;'
  menu.appendChild(title)

  const list = document.createElement('div')
  list.style.cssText = 'overflow:auto;display:flex;flex-direction:column;gap:4px;'
  menu.appendChild(list)

  let current = [...records]

  const renderList = () => {
    list.replaceChildren()
    if (!current.length) {
      const empty = document.createElement('div')
      empty.textContent = '还没有指挥记录'
      empty.style.cssText = 'padding:8px;color:#7a8799;'
      list.appendChild(empty)
      return
    }
    for (const record of current) {
      const row = document.createElement('div')
      row.style.cssText = 'display:flex;align-items:center;gap:4px;'

      const button = document.createElement('button')
      button.type = 'button'
      button.textContent = record.task
      button.title = record.task
      button.style.cssText = [
        'flex:1',
        'min-width:0',
        'border:0',
        'border-radius:10px',
        'background:#f4f8ff',
        'color:#243044',
        'text-align:left',
        'padding:8px 10px',
        'cursor:pointer',
        'white-space:nowrap',
        'overflow:hidden',
        'text-overflow:ellipsis'
      ].join(';')
      button.addEventListener('click', (event) => {
        event.stopPropagation()
        closeCommandHistory()
        onPick(record.task)
      })

      const remove = document.createElement('button')
      remove.type = 'button'
      remove.textContent = '×'
      remove.title = '删除这条指令'
      remove.setAttribute('aria-label', `删除指令：${record.task}`)
      remove.style.cssText = [
        'flex:0 0 28px',
        'height:32px',
        'border:0',
        'border-radius:8px',
        'background:transparent',
        'color:#8a97ab',
        'cursor:pointer',
        'font-size:18px',
        'line-height:1'
      ].join(';')
      remove.addEventListener('mouseenter', () => {
        remove.style.background = '#fff1f0'
        remove.style.color = '#c45656'
      })
      remove.addEventListener('mouseleave', () => {
        remove.style.background = 'transparent'
        remove.style.color = '#8a97ab'
      })
      remove.addEventListener('click', async (event) => {
        event.stopPropagation()
        if (!onDelete) {
          current = current.filter((item) => item !== record)
          renderList()
          return
        }
        remove.disabled = true
        try {
          await onDelete(record)
          const index = current.indexOf(record)
          if (index >= 0) {
            current = [...current.slice(0, index), ...current.slice(index + 1)]
          }
          renderList()
        } finally {
          remove.disabled = false
        }
      })

      row.append(button, remove)
      list.appendChild(row)
    }
  }
  renderList()

  const fresh = document.createElement('button')
  fresh.type = 'button'
  fresh.textContent = '新的指令'
  fresh.style.cssText =
    'margin-top:8px;border:0;border-radius:10px;background:#2f6bff;color:#fff;padding:8px 10px;cursor:pointer;'
  fresh.addEventListener('click', (event) => {
    event.stopPropagation()
    closeCommandHistory()
    onPick(null)
  })
  menu.appendChild(fresh)

  document.body.appendChild(menu)
  setTimeout(() => {
    document.addEventListener('pointerdown', onOutside, true)
  }, 0)
}

function onOutside(event: Event) {
  const menu = document.getElementById(MENU_ID)
  const target = event.target as Node | null
  if (!menu || (target && menu.contains(target))) return
  const launcher = document.getElementById('pm-assistant-launcher')
  if (launcher && target && launcher.contains(target)) return
  closeCommandHistory()
}

export function closeCommandHistory() {
  document.getElementById(MENU_ID)?.remove()
  document.removeEventListener('pointerdown', onOutside, true)
}
