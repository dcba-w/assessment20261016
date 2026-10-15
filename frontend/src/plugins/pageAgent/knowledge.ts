import {
  listMyKnowledgeApi,
  listOtherKnowledgeApi,
  type KnowledgeDocVO
} from '@/api/infra/knowledge'

const QUESTION_WORDS = ['是什么', '为什么', '怎么', '如何', '什么', '吗', '呢', '的', '？', '?']

function focusQuestion(question: string) {
  let text = question.trim()
  for (const word of QUESTION_WORDS) text = text.split(word).join('')
  return text.trim()
}

function asDocs(value: unknown): KnowledgeDocVO[] {
  if (Array.isArray(value)) return value as KnowledgeDocVO[]
  if (value && typeof value === 'object') {
    const record = value as { data?: unknown; list?: unknown }
    if (Array.isArray(record.data)) return record.data as KnowledgeDocVO[]
    if (Array.isArray(record.list)) return record.list as KnowledgeDocVO[]
  }
  return []
}

function docText(doc: KnowledgeDocVO) {
  const raw = doc as KnowledgeDocVO & { doc_name?: string }
  return {
    name: (raw.docName || raw.doc_name || '').trim(),
    content: (raw.content || '').trim()
  }
}

function matchesDoc(doc: KnowledgeDocVO, raw: string, focused: string) {
  const { name, content } = docText(doc)
  const haystack = `${name}\n${content}`
  if (focused.length >= 2 && haystack.includes(focused)) return true
  if (name.length >= 2 && (raw.includes(name) || focused.includes(name))) return true
  return false
}

function formatDocs(docs: KnowledgeDocVO[]) {
  return docs
    .slice(0, 3)
    .map((doc, index) => {
      const { name, content } = docText(doc)
      const title = name ? `【${name}】` : ''
      return `${index + 1}.${title} ${content}`
    })
    .join('\n')
}

/** 用已保存的文档标题和正文匹配，不依赖向量模型 */
export async function findKnowledgeAnswer(question: string): Promise<string | null> {
  const raw = (question || '').trim()
  const focused = focusQuestion(raw)
  if (focused.length < 2 && raw.length < 2) return null
  try {
    const [mine, others] = await Promise.all([
      listMyKnowledgeApi().catch(() => []),
      listOtherKnowledgeApi('').catch(() => [])
    ])
    const docs = [...asDocs(mine), ...asDocs(others)].filter((doc) => matchesDoc(doc, raw, focused))
    if (!docs.length) return null
    return formatDocs(docs)
  } catch {
    return null
  }
}

/** 调用后端语义检索，把命中的分块拼成文本返回给模型 */
export async function searchKnowledge(question: string): Promise<string> {
  const q = (question || '').trim()
  if (!q) return '没有给出要查询的问题。'
  const found = await findKnowledgeAnswer(q)
  if (found) return found
  return `知识库中没有查到与「${q}」相关的内容。`
}
