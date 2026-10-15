import router from '@/router'
import { loadPersona } from './persona'
import { mountPmAssistant, type PmAssistant } from './assistant'
import { closeCommandHistory } from './historyMenu'
import { closeKnowledgeBook } from './knowledgeBook'
import { closeNl2sqlOverlay } from './nl2sqlOverlay'

let assistant: PmAssistant | null = null

export const setupPageAgent = () => {
  if (import.meta.env.VITE_PAGE_AGENT_ENABLE !== 'true') {
    return
  }

  assistant?.dispose()
  assistant = mountPmAssistant(loadPersona().name)
  assistant.show()

  router.afterEach(() => {
    assistant?.syncLogin()
  })

  if (import.meta.hot) {
    import.meta.hot.dispose(() => {
      closeCommandHistory()
      closeKnowledgeBook()
      closeNl2sqlOverlay()
      assistant?.dispose()
      assistant = null
    })
  }
}
