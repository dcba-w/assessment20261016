/**
 * 知识库对照演示（不依赖真实 embedding）
 * 模拟 PageAgent 收到问题后：
 *   场景A：没有知识库 → 只靠模型通用知识回答
 *   场景B：有知识库 → LangGraph 触发检索 → 把命中的原文喂给模型回答
 */

const knowledgeBase = [
  {
    docName: 'ETC货车计费规则',
    content: 'ETC货车按车型系数计费：一类车系数1.0，二类车系数1.2，三类车系数1.5，四类车系数1.8，五类车系数2.0。通行费=费率×车货总重×里程×车型系数。绿通车辆需预约且装载鲜活农产品占比不低于80%方可享受减免。'
  },
  {
    docName: '车道状态说明',
    content: '车道状态分为：绿（正常放行）、黄（临时管控）、红（关闭）。黄状态表示该车道限速或只允许特定车型通过，红状态表示车道封闭不允许通行。车道状态变更需在云控后台"车道管理"页修改并实时下发。'
  },
  {
    docName: '收费日报口径',
    content: '收费日报统计口径：自然日0点至24点，按出口流水汇总。实收金额=应收金额-优惠减免。交易笔数包含ETC与MTC全部交易。报表"异常交易"指金额与应收不符或被稽核标记的流水。'
  }
]

// 模拟关键词检索（对应后端降级逻辑）
function search(query, topK = 3) {
  const keywords = ['收费','计费','货车','车型','系数','绿通','车道','状态','日报','报表','口径','ETC','MTC','流量','减免','封闭','放行','正常','临时']
  return knowledgeBase
    .map(item => {
      let score = 0
      for (const kw of keywords) {
        if (query.includes(kw) && (item.content.includes(kw) || item.docName.includes(kw))) score++
      }
      return { ...item, score }
    })
    .filter(x => x.score > 0)
    .sort((a, b) => b.score - a.score)
    .slice(0, topK)
}

// 模拟"没有知识库"时模型的回答（凭通用印象）
function answerWithoutKB(question) {
  if (question.includes('货车') && question.includes('系数')) {
    return '货车计费一般是按车型分类的，不同省份系数可能不一样，一类车大概是最低的，越大型车系数越高。绿通车辆有减免政策，具体要预约。（以上为通用印象，可能与贵司实际规则不符）'
  }
  return '这个问题我不太确定，建议您查阅内部文档或咨询业务部门。'
}

// 模拟"有知识库"时：检索到原文后，模型基于原文回答
function answerWithKB(question) {
  const hits = search(question)
  if (hits.length === 0) return '知识库中没有查到相关内容。'
  const context = hits.map((h, i) => `${i + 1}.【${h.docName}】${h.content}`).join('\n')
  // 模型看到 context 后组织回答（这里直接展示它能拿到的事实）
  return `基于检索到的知识库原文：\n${context}`
}

// ===== 演示：两个问题，两种场景 =====
const questions = [
  '货车的计费系数是怎么规定的？',
  '车道状态变红是什么意思？'
]

console.log('='.repeat(70))
console.log('📊  PageAgent 知识库对照演示（无真实 embedding，关键词降级模式）')
console.log('='.repeat(70))

for (const q of questions) {
  console.log(`\n❓ 用户问：${q}`)
  console.log('-'.repeat(50))
  console.log('【场景A】没有知识库（纯靠模型记忆）：')
  console.log('  ' + answerWithoutKB(q).replace(/\n/g, '\n  '))
  console.log('')
  console.log('【场景B】有知识库（LangGraph 先检索再回答）：')
  const hits = search(q)
  console.log(`  → search_knowledge 命中 ${hits.length} 条：`)
  console.log('  ' + answerWithKB(q).replace(/\n/g, '\n  '))
  console.log('')
  console.log('  ✅ 区别：场景B的回答完全来自你库里的原文，精确到数字和术语，不会张冠李戴。')
}

console.log('\n' + '='.repeat(70))
console.log('💡  升级说明：配上真实 embedding key 后，关键词匹配会自动升级为语义检索，')
console.log('    比如问"二类车跑100公里收多少钱"也能命中"车型系数1.2"那条，不再需要完全相同的词。')
console.log('='.repeat(70))
