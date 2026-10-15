const USED_KEY = 'simple-page-agent-token-used'

export function loadUsedTokens(): number {
  const value = Number(localStorage.getItem(USED_KEY))
  return Number.isFinite(value) && value > 0 ? Math.floor(value) : 0
}

export function addUsedTokens(delta: number): number {
  if (!Number.isFinite(delta) || delta <= 0) return loadUsedTokens()
  const next = loadUsedTokens() + Math.floor(delta)
  localStorage.setItem(USED_KEY, String(next))
  return next
}
