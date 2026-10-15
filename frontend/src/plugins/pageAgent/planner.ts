import { Annotation, END, START, StateGraph } from '@langchain/langgraph'

import { searchKnowledge } from './knowledge'

/**
 * LangGraph 多步规划（轻量版）
 *
 * 定位：PageAgent 负责“操作页面”，本图负责“任务前的规划与知识检索”。
 * 流程：
 *   START → analyze（判断该任务是否需要业务知识）
 *         → 需要: retrieve（查知识库） → END
 *         → 不需要: 直接 END
 *
 * 这样把“先查业务规则、再动手操作”做成一条显式的有向流程，而不是靠模型临场发挥。
 */

const PlannerStateAnnotation = Annotation.Root({
  task: Annotation<string>({ reducer: (_, next) => next }),
  needKnowledge: Annotation<boolean>({ reducer: (_, next) => next, default: () => false }),
  knowledge: Annotation<string>({ reducer: (_, next) => next, default: () => '' })
})
type PlannerState = typeof PlannerStateAnnotation.State

/** 规则判断：任务里出现这些词，倾向于需要查业务知识库 */
const KNOWLEDGE_HINTS = [
  '规则',
  '怎么',
  '如何',
  '说明',
  '文档',
  '口径',
  '报表',
  '流程',
  '是什么',
  '为什么',
  '收费',
  '计费',
  '政策',
  '标准'
]

function analyzeNode(state: PlannerState): Partial<PlannerState> {
  const task = (state.task || '').toLowerCase()
  const hit = KNOWLEDGE_HINTS.some((w) => task.includes(w))
  return { needKnowledge: hit }
}

async function retrieveNode(state: PlannerState): Promise<Partial<PlannerState>> {
  const knowledge = await searchKnowledge(state.task)
  return { knowledge }
}

function routeAfterAnalyze(state: PlannerState): 'retrieve' | typeof END {
  return state.needKnowledge ? 'retrieve' : END
}

const workflow = new StateGraph(PlannerStateAnnotation)
  .addNode('analyze', analyzeNode)
  .addNode('retrieve', retrieveNode)
  .addEdge(START, 'analyze')
  .addConditionalEdges('analyze', routeAfterAnalyze, { retrieve: 'retrieve', [END]: END })
  .addEdge('retrieve', END)

const graph = workflow.compile()

/**
 * 对用户任务做一次“是否需要查业务知识”的轻规划。
 * 返回可注入到 PageAgent system prompt 的业务知识片段（没有则为空）。
 */
export async function planKnowledge(task: string): Promise<string> {
  try {
    const result = await graph.invoke({ task, needKnowledge: false, knowledge: '' })
    return result.knowledge || ''
  } catch {
    // 规划失败不阻塞主流程，交给 PageAgent 自行处理
    return ''
  }
}
