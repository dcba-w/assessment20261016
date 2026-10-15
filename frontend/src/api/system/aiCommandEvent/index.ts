import request from '@/config/axios'

export interface AiCommandEventStep {
  tool: string
  input?: unknown
  elementText?: string
}

export interface AiCommandEventVO {
  id: number
  task: string
  pagePath?: string
  stepCount: number
  steps: AiCommandEventStep[]
  updateTime?: string | number
}

export const getAiCommandEventList = () => {
  return request.get<AiCommandEventVO[]>({ url: '/system/ai-command-event/list' })
}

export const saveAiCommandEvent = (data: {
  task: string
  pagePath?: string
  steps: AiCommandEventStep[]
}) => {
  return request.post<number>({ url: '/system/ai-command-event/save', data })
}

export const deleteAiCommandEvent = (id: number) => {
  return request.delete({ url: '/system/ai-command-event/delete', params: { id } })
}
