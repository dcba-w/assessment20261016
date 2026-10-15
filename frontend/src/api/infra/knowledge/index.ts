import request from '@/config/axios'

export interface KnowledgeChunkVO {
  content: string
  docName?: string
  score?: number
}

export interface KnowledgeDocVO {
  docName: string
  creator?: string
  content?: string
  chunkCount?: number
  updateTime?: string
}

// 语义检索业务知识库（PageAgent 的 search_knowledge 工具走这里）
export const searchKnowledgeApi = (query: string, topK = 5) => {
  return request.post<KnowledgeChunkVO[]>({
    url: '/infra/knowledge/search',
    data: { query, topK }
  })
}

export const addKnowledgeApi = (docName: string, content: string) => {
  return request.post<number>({ url: '/infra/knowledge/add', data: { docName, content } })
}

export const listMyKnowledgeApi = () => {
  return request.get<KnowledgeDocVO[]>({ url: '/infra/knowledge/mine' })
}

export const listOtherKnowledgeApi = (keyword = '') => {
  return request.get<KnowledgeDocVO[]>({ url: '/infra/knowledge/others', params: { keyword } })
}

export const updateKnowledgeApi = (docName: string, newDocName: string, content: string) => {
  return request.put<number>({ url: '/infra/knowledge/update', data: { docName, newDocName, content } })
}

export const deleteKnowledgeDocApi = (docName: string) => {
  return request.delete<boolean>({ url: '/infra/knowledge/doc', params: { docName } })
}
