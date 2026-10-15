import {
  addKnowledgeApi,
  deleteKnowledgeDocApi,
  listMyKnowledgeApi,
  listOtherKnowledgeApi,
  updateKnowledgeApi,
  type KnowledgeDocVO
} from '@/api/infra/knowledge'
import { getSimpleUserList } from '@/api/system/user'

const BOOK_ID = 'page-agent-knowledge-book'

type Tab = 'mine' | 'others'

export function openKnowledgeBook() {
  closeKnowledgeBook()
  const names = new Map<string, string>()
  let tab: Tab = 'mine'
  let docs: KnowledgeDocVO[] = []
  let selected = ''
  let editingName = ''

  const root = document.createElement('div')
  root.id = BOOK_ID
  root.setAttribute('data-page-agent-ignore', 'true')
  root.setAttribute('data-browser-use-ignore', 'true')
  root.style.cssText =
    'position:fixed;inset:0;z-index:2147483646;display:flex;align-items:center;justify-content:center;background:rgba(18,32,64,.35);font:13px/1.45 "Microsoft YaHei",sans-serif;color:#243044;'

  const dialog = document.createElement('div')
  dialog.style.cssText =
    'width:min(860px,calc(100vw - 32px));height:min(560px,calc(100vh - 48px));display:flex;flex-direction:column;background:#fff;border-radius:18px;box-shadow:0 18px 48px rgba(22,58,138,.22);overflow:hidden;'
  dialog.addEventListener('click', (event) => event.stopPropagation())

  const header = document.createElement('div')
  header.style.cssText = 'display:flex;align-items:center;justify-content:space-between;padding:14px 16px 8px;'
  const title = document.createElement('div')
  title.textContent = '知识库'
  title.style.fontWeight = '600'
  const close = button('关闭', 'transparent', '#5b6b82')
  close.addEventListener('click', () => closeKnowledgeBook())
  header.append(title, close)

  const tabs = document.createElement('div')
  tabs.style.cssText = 'display:flex;gap:8px;padding:0 16px 10px;'
  const mineTab = button('我的知识库', '#2f6bff', '#fff')
  const otherTab = button('别人的知识库', '#f4f8ff', '#243044')
  tabs.append(mineTab, otherTab)

  const body = document.createElement('div')
  body.style.cssText = 'flex:1;display:flex;min-height:0;border-top:1px solid #e6eefc;'
  const list = document.createElement('div')
  list.style.cssText = 'width:240px;overflow:auto;padding:10px;border-right:1px solid #e6eefc;display:flex;flex-direction:column;gap:6px;'
  const editor = document.createElement('div')
  editor.style.cssText = 'flex:1;display:flex;flex-direction:column;gap:8px;padding:12px;min-width:0;'

  const search = document.createElement('input')
  search.placeholder = '按标题或内容查询'
  search.style.cssText = inputStyle()
  const nameInput = document.createElement('input')
  nameInput.placeholder = '文档名称'
  nameInput.style.cssText = inputStyle()
  const content = document.createElement('textarea')
  content.placeholder = '写下业务规则、口径或操作说明。保存后助手提问时会检索这篇文档。'
  content.style.cssText = inputStyle() + 'flex:1;resize:none;min-height:180px;'
  const status = document.createElement('div')
  status.style.cssText = 'min-height:18px;color:#c45656;'

  const actions = document.createElement('div')
  actions.style.cssText = 'display:flex;gap:8px;'
  const saveBtn = button('保存', '#2f6bff', '#fff')
  const deleteBtn = button('删除', '#fff1f0', '#c45656')
  const createBtn = button('新建', '#f4f8ff', '#243044')
  actions.append(saveBtn, deleteBtn, createBtn)

  editor.append(search, nameInput, content, actions, status)
  body.append(list, editor)
  dialog.append(header, tabs, body)
  root.appendChild(dialog)
  root.addEventListener('click', () => closeKnowledgeBook())
  document.body.appendChild(root)

  mineTab.addEventListener('click', () => switchTab('mine'))
  otherTab.addEventListener('click', () => switchTab('others'))
  search.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' && tab === 'others') void load()
  })
  createBtn.addEventListener('click', () => {
    selected = ''
    editingName = ''
    nameInput.value = ''
    content.value = ''
    status.textContent = ''
    paintList()
  })
  saveBtn.addEventListener('click', () => void save())
  deleteBtn.addEventListener('click', () => void remove())

  void loadNames().then(() => switchTab('mine'))

  function switchTab(next: Tab) {
    tab = next
    selected = ''
    editingName = ''
    nameInput.value = ''
    content.value = ''
    status.textContent = ''
    const mine = next === 'mine'
    paintTab(mineTab, mine)
    paintTab(otherTab, !mine)
    nameInput.readOnly = !mine
    content.readOnly = !mine
    saveBtn.style.display = mine ? '' : 'none'
    deleteBtn.style.display = mine ? '' : 'none'
    createBtn.style.display = mine ? '' : 'none'
    search.style.display = mine ? 'none' : ''
    void load()
  }

  async function loadNames() {
    try {
      const users = await getSimpleUserList()
      for (const user of users || []) names.set(String(user.id), user.nickname || user.username)
    } catch {
      // 没有用户列表权限时，列表里显示用户编号
    }
  }

  async function load() {
    status.textContent = ''
    list.textContent = '加载中…'
    try {
      docs = (tab === 'mine' ? await listMyKnowledgeApi() : await listOtherKnowledgeApi(search.value.trim())) || []
      paintList()
    } catch (error) {
      docs = []
      list.textContent = ''
      status.textContent = messageOf(error)
    }
  }

  function paintList() {
    list.textContent = ''
    if (!docs.length) {
      const empty = document.createElement('div')
      empty.style.color = '#7a8799'
      empty.textContent = tab === 'mine' ? '还没有自己的文档，点新建开始写' : '没有查到别人的知识库'
      list.appendChild(empty)
      return
    }
    for (const doc of docs) {
      const item = document.createElement('button')
      item.type = 'button'
      item.style.cssText =
        'border:0;border-radius:10px;background:' +
        (doc.docName === selected ? '#e7f0ff' : '#f4f8ff') +
        ';text-align:left;padding:8px 10px;cursor:pointer;'
      const name = document.createElement('div')
      name.style.fontWeight = '600'
      name.textContent = doc.docName || '未命名'
      item.appendChild(name)
      if (tab === 'others') {
        const owner = document.createElement('div')
        owner.style.cssText = 'color:#7a8799;margin-top:2px;'
        owner.textContent = names.get(String(doc.creator)) || `用户 ${doc.creator || ''}`
        item.appendChild(owner)
      }
      item.addEventListener('click', () => {
        selected = doc.docName
        editingName = doc.docName
        nameInput.value = doc.docName
        content.value = doc.content || ''
        status.textContent = ''
        paintList()
      })
      list.appendChild(item)
    }
  }

  async function save() {
    const docName = nameInput.value.trim()
    const text = content.value.trim()
    if (!docName || !text) {
      status.textContent = '请填写文档名称和内容'
      return
    }
    status.textContent = '保存中…'
    try {
      if (editingName) await updateKnowledgeApi(editingName, docName, text)
      else await addKnowledgeApi(docName, text)
      status.textContent = '已保存，助手之后会按这篇内容回答'
      editingName = docName
      selected = docName
      await load()
    } catch (error) {
      status.textContent = messageOf(error)
    }
  }

  async function remove() {
    if (!editingName) return
    status.textContent = '删除中…'
    try {
      await deleteKnowledgeDocApi(editingName)
      selected = ''
      editingName = ''
      nameInput.value = ''
      content.value = ''
      status.textContent = '已删除'
      await load()
    } catch (error) {
      status.textContent = messageOf(error)
    }
  }
}

export function closeKnowledgeBook() {
  document.getElementById(BOOK_ID)?.remove()
}

function button(text: string, background: string, color: string) {
  const el = document.createElement('button')
  el.type = 'button'
  el.textContent = text
  el.style.cssText = `border:0;border-radius:10px;background:${background};color:${color};padding:7px 12px;cursor:pointer;`
  return el
}

function paintTab(el: HTMLButtonElement, active: boolean) {
  el.style.background = active ? '#2f6bff' : '#f4f8ff'
  el.style.color = active ? '#fff' : '#243044'
}

function inputStyle() {
  return 'width:100%;box-sizing:border-box;border:1px solid #d5e4ff;border-radius:10px;padding:8px 10px;outline:none;'
}

function messageOf(error: unknown) {
  if (typeof error === 'string') return error
  if (error && typeof error === 'object' && 'message' in error) return String((error as { message?: string }).message || '操作失败')
  return '操作失败，请确认已登录并且后端已重启'
}
