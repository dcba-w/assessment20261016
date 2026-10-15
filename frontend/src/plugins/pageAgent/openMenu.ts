import { getInfo } from '@/api/login'
import { CACHE_KEY, useCache } from '@/hooks/web/useCache'
import router from '@/router'

interface AuthMenu {
  name?: string
  path?: string
  component?: string
  visible?: boolean
  children?: AuthMenu[]
}

interface MenuHit {
  name: string
  path: string
  titlePath: string
}

const { wsCache } = useCache()

function joinPath(parent: string, path: string): string {
  if (!path) return parent || '/'
  if (/^https?:\/\//i.test(path) || path.startsWith('/')) return path
  const base = parent.endsWith('/') ? parent.slice(0, -1) : parent
  return `${base}/${path}`.replace(/\/{2,}/g, '/')
}

function normalize(value: string): string {
  return value
    .trim()
    .replace(/^(请|帮我|我要)?(打开|进入|跳转到|跳转|去)/, '')
    .replace(/页面|菜单/g, '')
    .replace(/\s+/g, '')
    .toLowerCase()
}

function flattenMenus(menus: AuthMenu[], parentPath = '', titles: string[] = []): MenuHit[] {
  const hits: MenuHit[] = []
  for (const menu of menus) {
    if (!menu?.name) continue
    const path = joinPath(parentPath, menu.path || '')
    const titlePath = [...titles, menu.name]
    const children = menu.children || []
    const leaf = children.length === 0
    if (leaf && path && !/^https?:\/\//i.test(path) && (menu.component || menu.path)) {
      hits.push({ name: menu.name, path, titlePath: titlePath.join(' / ') })
    }
    if (children.length) {
      hits.push(...flattenMenus(children, path, titlePath))
    }
  }
  return hits
}

async function loadMenus(): Promise<AuthMenu[]> {
  const cached = wsCache.get(CACHE_KEY.ROLE_ROUTERS) as AuthMenu[] | null
  if (Array.isArray(cached) && cached.length) return cached

  const info = await getInfo()
  const menus = (info?.menus || []) as AuthMenu[]
  if (menus.length) wsCache.set(CACHE_KEY.ROLE_ROUTERS, menus)
  return menus
}

function score(item: MenuHit, keyword: string): number {
  const name = normalize(item.name)
  const title = normalize(item.titlePath)
  const path = normalize(item.path)
  if (!keyword) return 0
  if (name === keyword || title === keyword) return 100
  if (name.includes(keyword)) return 80
  if (keyword.includes(name) && name.length >= 2) return 70
  if (title.includes(keyword)) return 60
  if (path.includes(keyword)) return 40
  return 0
}

function formatHits(items: MenuHit[]): string {
  return items.map((item, index) => `${index + 1}. ${item.titlePath}（${item.path}）`).join('\n')
}

export async function openMenuByName(rawName: string): Promise<string> {
  const keyword = normalize(rawName)
  if (!keyword) return '没有给出菜单名称，无法查询。'

  const menus = await loadMenus()
  if (!menus.length) return '还没有登录，或当前账号没有菜单。请先登录后再跳转。'

  const ranked = flattenMenus(menus)
    .map((item) => ({ item, score: score(item, keyword) }))
    .filter((item) => item.score > 0)
    .sort((a, b) => b.score - a.score)

  if (!ranked.length) {
    return `菜单中没有「${rawName.trim()}」。不要再去点侧边栏查找。`
  }

  const top = ranked[0].score
  const best = ranked.filter((item) => item.score === top).map((item) => item.item)
  if (best.length > 1) {
    return `找到多个菜单，请用更精确的名称再调用 open_menu：\n${formatHits(best.slice(0, 8))}`
  }

  const match = best[0]
  if (router.currentRoute.value.path === match.path) {
    return `已经在「${match.titlePath}」（${match.path}）。`
  }

  const failure = await router.push(match.path)
  if (failure) {
    return `已查到「${match.titlePath}」（${match.path}），但跳转失败。`
  }
  return `已根据菜单「${match.titlePath}」跳转到 ${match.path}。接下来直接在这个页面操作，不要再点侧边栏。`
}
