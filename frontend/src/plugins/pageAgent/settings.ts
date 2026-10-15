import type { PageAgentAccent, PageAgentPersona } from './persona'

const STYLE_ID = 'page-agent-persona-settings-style'

const ACCENT_OPTIONS: { id: PageAgentAccent; label: string }[] = [
  { id: 'aurora', label: '极光' },
  { id: 'ocean', label: '海洋' },
  { id: 'sunset', label: '落日' }
]

let dialog: HTMLElement | null = null

function ensureStyle() {
  if (document.getElementById(STYLE_ID)) return
  const style = document.createElement('style')
  style.id = STYLE_ID
  style.textContent = `
    .pa-settings {
      position: fixed;
      right: 24px;
      bottom: 92px;
      z-index: 2147483643;
      width: min(420px, calc(100vw - 32px));
      max-height: min(640px, calc(100vh - 120px));
      overflow: auto;
      box-sizing: border-box;
      padding: 16px 16px 14px;
      border: 1px solid rgba(255, 255, 255, 0.28);
      border-radius: 16px;
      color: #f5f7fb;
      background: rgba(12, 16, 28, 0.92);
      box-shadow: 0 18px 50px rgba(0, 0, 0, 0.35);
      backdrop-filter: blur(16px);
      font-family: inherit;
    }
    .pa-settings * { box-sizing: border-box; }
    .pa-settings h3 { margin: 0 0 4px; font-size: 16px; font-weight: 650; }
    .pa-settings p { margin: 0 0 14px; color: rgba(255,255,255,0.62); font-size: 12px; line-height: 1.5; }
    .pa-settings label { display: block; margin: 12px 0 6px; font-size: 12px; color: rgba(255,255,255,0.86); }
    .pa-settings input, .pa-settings textarea, .pa-settings select {
      width: 100%;
      border: 1px solid rgba(255,255,255,0.16);
      border-radius: 10px;
      padding: 8px 10px;
      color: #111;
      background: rgba(255,255,255,0.92);
      font: inherit;
      font-size: 13px;
    }
    .pa-settings textarea { min-height: 110px; resize: vertical; }
    .pa-settings .pa-accents { display: flex; gap: 8px; }
    .pa-settings .pa-accents label {
      flex: 1;
      margin: 0;
      padding: 8px 6px;
      border: 1px solid rgba(255,255,255,0.16);
      border-radius: 10px;
      text-align: center;
      cursor: pointer;
    }
    .pa-settings .pa-accents input { display: none; }
    .pa-settings .pa-accents label:has(input:checked) {
      border-color: transparent;
      color: #fff;
      background: linear-gradient(135deg, rgb(57, 182, 255), rgb(189, 69, 251));
    }
    .pa-settings details { margin-top: 12px; }
    .pa-settings summary { cursor: pointer; font-size: 12px; color: rgba(255,255,255,0.75); }
    .pa-settings .pa-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
    .pa-settings button {
      border: 0;
      border-radius: 10px;
      padding: 8px 14px;
      cursor: pointer;
      font: inherit;
      font-size: 13px;
    }
    .pa-settings .pa-cancel { color: #fff; background: rgba(255,255,255,0.12); }
    .pa-settings .pa-save { color: #fff; background: linear-gradient(135deg, rgb(57, 182, 255), rgb(189, 69, 251)); }
  `
  document.head.appendChild(style)
}

export function openPersonaSettings(current: PageAgentPersona, onSave: (next: PageAgentPersona) => void) {
  ensureStyle()
  dialog?.remove()

  const root = document.createElement('form')
  root.className = 'pa-settings'
  root.setAttribute('data-page-agent-ignore', 'true')
  root.setAttribute('data-browser-use-ignore', 'true')
  root.innerHTML = `
    <h3>配置 AI 人物</h3>
    <p>名称和人设会显示在侧边栏，并作为助手的系统提示。保存后立即生效。</p>
    <label for="pa-name">名称</label>
    <input id="pa-name" name="name" maxlength="20" required />
    <label for="pa-persona">人设</label>
    <textarea id="pa-persona" name="persona" maxlength="2000" placeholder="这个助手是谁、怎么说话、优先做什么"></textarea>
    <label>侧边栏配色</label>
    <div class="pa-accents">
      ${ACCENT_OPTIONS.map(
        (item) =>
          `<label><input type="radio" name="accent" value="${item.id}" />${item.label}</label>`
      ).join('')}
    </div>
    <label for="pa-tokens">Token 额度</label>
    <input id="pa-tokens" name="tokenQuota" type="number" min="0" step="1" />
    <details>
      <summary>模型连接</summary>
      <label for="pa-model">模型</label>
      <input id="pa-model" name="model" required />
      <label for="pa-base">接口地址</label>
      <input id="pa-base" name="baseURL" required />
      <label for="pa-key">API Key</label>
      <input id="pa-key" name="apiKey" type="password" autocomplete="off" />
      <label for="pa-lang">语言</label>
      <select id="pa-lang" name="language">
        <option value="zh-CN">中文</option>
        <option value="en-US">English</option>
      </select>
    </details>
    <div class="pa-actions">
      <button type="button" class="pa-cancel">取消</button>
      <button type="submit" class="pa-save">保存</button>
    </div>
  `

  const field = (name: string) => root.querySelector(`[name="${name}"]`) as HTMLInputElement
  field('name').value = current.name
  field('persona').value = current.persona
  field('model').value = current.model
  field('baseURL').value = current.baseURL
  field('apiKey').value = current.apiKey
  field('language').value = current.language
  field('tokenQuota').value = String(current.tokenQuota)
  const accent = root.querySelector(
    `input[name="accent"][value="${current.accent}"]`
  ) as HTMLInputElement | null
  if (accent) accent.checked = true

  root.querySelector('.pa-cancel')?.addEventListener('click', () => root.remove())
  root.addEventListener('submit', (event) => {
    event.preventDefault()
    const accentValue = (root.querySelector('input[name="accent"]:checked') as HTMLInputElement | null)
      ?.value as PageAgentAccent | undefined
    onSave({
      name: field('name').value.trim() || '页面助手',
      persona: field('persona').value.trim(),
      accent: accentValue || 'aurora',
      model: field('model').value.trim(),
      baseURL: field('baseURL').value.trim(),
      apiKey: field('apiKey').value.trim() || 'NA',
      language: field('language').value === 'en-US' ? 'en-US' : 'zh-CN',
      tokenQuota: Math.max(0, Math.floor(Number(field('tokenQuota').value) || 0))
    })
    root.remove()
  })

  document.body.appendChild(root)
  dialog = root
  field('name').focus()
}
