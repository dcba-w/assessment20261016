import { ElMessage } from 'element-plus'
import { getAccessToken } from '@/utils/auth'
import { openMenuByName } from './openMenu'
import { searchKnowledge } from './knowledge'
import { isDataQuestion, queryData } from './nl2sql'
import { closeCommandHistory, openCommandHistory } from './historyMenu'
import { closeKnowledgeBook, openKnowledgeBook } from './knowledgeBook'
import { closeNl2sqlOverlay } from './nl2sqlOverlay'
import { deleteRecording, refreshRecordings, saveRecording } from './recordings'

const LOGIN_TIP = '请先登录后再使用 AI 助手'
const OPEN_HINT = /打开|进入|跳转|去看/

export interface PmAssistant {
  show: () => void
  dispose: () => void
  syncLogin: () => void
}

function loggedIn() {
  return Boolean(getAccessToken())
}

function answerOf(task: string): Promise<string> {
  const text = task.trim()
  if (OPEN_HINT.test(text)) return openMenuByName(text)
  if (isDataQuestion(text)) return queryData(text)
  return searchKnowledge(text).then(
    (found) => found || '知识库没有命中。未配置模型 Key 时助手只做菜单跳转和原文检索，不编造数字。'
  )
}

export function mountPmAssistant(title: string): PmAssistant {
  const root = document.createElement('div')
  root.id = 'pm-assistant'
  root.style.cssText = 'position:fixed;right:24px;bottom:24px;z-index:2147483000;font:14px/1.5 "Microsoft YaHei",sans-serif;'

  const launcher = document.createElement('button')
  launcher.id = 'pm-assistant-launcher'
  launcher.type = 'button'
  launcher.textContent = '助手'
  launcher.style.cssText =
    'width:56px;height:56px;border:0;border-radius:28px;background:#2f6bff;color:#fff;cursor:pointer;box-shadow:0 8px 20px rgba(47,107,255,.35);'

  const panel = document.createElement('section')
  panel.style.cssText =
    'display:none;width:360px;max-height:480px;margin-bottom:12px;padding:12px;border-radius:16px;background:#fff;box-shadow:0 12px 28px rgba(22,58,138,.16);color:#243044;'

  const heading = document.createElement('div')
  heading.textContent = title || '页面助手'
  heading.style.cssText = 'font-weight:600;margin-bottom:8px;'

  const log = document.createElement('div')
  log.style.cssText = 'max-height:280px;overflow:auto;white-space:pre-wrap;margin-bottom:8px;'

  const form = document.createElement('form')
  form.style.cssText = 'display:flex;gap:8px;'
  const input = document.createElement('input')
  input.type = 'text'
  input.placeholder = '打开菜单、查知识库、问数据'
  input.style.cssText = 'flex:1;border:1px solid #d5e4ff;border-radius:8px;padding:8px;'
  const send = document.createElement('button')
  send.type = 'submit'
  send.textContent = '发送'
  send.style.cssText = 'border:0;border-radius:8px;background:#2f6bff;color:#fff;padding:8px 12px;cursor:pointer;'
  const book = document.createElement('button')
  book.type = 'button'
  book.textContent = '知识库'
  book.style.cssText = 'border:0;border-radius:8px;background:#eef3ff;color:#2f6bff;padding:8px 10px;cursor:pointer;'

  form.append(input, book, send)
  panel.append(heading, log, form)
  root.append(panel, launcher)
  document.body.appendChild(root)

  const push = (text: string) => {
    const line = document.createElement('div')
    line.textContent = text
    line.style.marginBottom = '8px'
    log.appendChild(line)
    log.scrollTop = log.scrollHeight
  }

  const syncLogin = () => {
    const ok = loggedIn()
    input.disabled = !ok
    send.disabled = !ok
    input.placeholder = ok ? '打开菜单、查知识库、问数据' : LOGIN_TIP
  }

  const remind = () => {
    syncLogin()
    panel.style.display = 'block'
    ElMessage.warning({ message: LOGIN_TIP, zIndex: 2147483647 })
  }

  launcher.addEventListener('click', async () => {
    if (!loggedIn()) {
      remind()
      return
    }
    if (panel.style.display === 'none') {
      const records = await refreshRecordings()
      openCommandHistory(
        records,
        (task) => {
          panel.style.display = 'block'
          if (task) void run(task)
        },
        (record) => deleteRecording(record)
      )
      return
    }
    closeCommandHistory()
    panel.style.display = 'none'
  })

  book.addEventListener('click', () => {
    if (!loggedIn()) {
      remind()
      return
    }
    closeCommandHistory()
    openKnowledgeBook()
  })

  const run = async (task: string) => {
    push(`我：${task}`)
    const answer = await answerOf(task)
    push(answer)
    const tool = OPEN_HINT.test(task) ? 'open_menu' : isDataQuestion(task) ? 'query_data' : 'search_knowledge'
    saveRecording(task, [{ tool, input: { question: task } }])
  }

  form.addEventListener('submit', (event) => {
    event.preventDefault()
    if (!loggedIn()) {
      remind()
      return
    }
    const task = input.value.trim()
    if (!task) return
    input.value = ''
    void run(task)
  })

  syncLogin()
  return {
    show: () => {
      panel.style.display = 'block'
    },
    syncLogin,
    dispose: () => {
      closeCommandHistory()
      closeKnowledgeBook()
      closeNl2sqlOverlay()
      root.remove()
    }
  }
}
