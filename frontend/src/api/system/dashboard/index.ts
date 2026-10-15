import request from '@/config/axios'

/** 系统管理统计 */
export interface DashboardSummaryVO {
  userCount: number
  deptCount: number
  roleCount: number
  tenantCount: number
  noticeCount: number
  unreadNotifyCount: number
}

/** 基础设施统计 */
export interface InfraDashboardSummaryVO {
  fileCount: number
  knowledgeChunkCount: number
  jobCount: number
  codegenTableCount: number
}

// 获得系统管理工作台统计数据
export const getDashboardSummary = async (): Promise<DashboardSummaryVO> => {
  return await request.get({ url: '/system/dashboard/summary' })
}

// 获得基础设施工作台统计数据
export const getInfraDashboardSummary = async (): Promise<InfraDashboardSummaryVO> => {
  return await request.get({ url: '/infra/dashboard/summary' })
}
