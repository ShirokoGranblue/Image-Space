import { describe, expect, it } from 'vitest'
import source from '../views/ImageSquare.vue?raw'
import heroSource from '../components/square/SquareHeroFilters.vue?raw'
import asideSource from '../components/square/SquareDiscoveryAside.vue?raw'
import drawerSource from '../components/square/SquareImageDrawer.vue?raw'

const squareSource = `${source}\n${heroSource}\n${asideSource}`

describe('public square search and sorting contract', () => {
  it('describes image-name search without claiming exact matching', () => {
    expect(squareSource).toContain('placeholder="搜索图片名称"')
    expect(squareSource).not.toContain('精确搜索图片名称')
  })

  it('offers only featured and latest sorting', () => {
    expect(source).toContain("{ label: '推荐', value: 'featured' }")
    expect(source).toContain("{ label: '最新', value: 'latest' }")
    expect(source).not.toContain('本页热度')
    expect(source).not.toContain("value: 'hot'")
    expect(source).not.toContain("viewMode.value === 'hot'")
  })

  it('only presents the backend total as a square-wide hero metric', () => {
    expect(squareSource).toContain('aria-label="\u516c\u5f00\u5e7f\u573a\u56fe\u7247\u603b\u6570"')
    expect(squareSource).toContain('<strong>{{ total }}</strong>')
    expect(squareSource).not.toContain('squareStats.totalLikes')
    expect(squareSource).not.toContain('<strong>{{ categoryOptions.length }}</strong>')
  })

  it('labels page-derived discovery data with its page scope', () => {
    expect(source).toContain('\u672c\u9875\u5206\u7c7b')
    expect(squareSource).toContain('\u672c\u9875\u6807\u7b7e')
    expect(squareSource).toContain('\u672c\u9875\u4f5c\u8005')
    expect(squareSource).toContain('\u672c\u9875 {{ creator.likeCount }} \u6b21\u559c\u6b22')
  })

  it('keeps pagination in document flow and exposes a filtered-empty recovery action', () => {
    expect(source).not.toContain('<Teleport to="body">')
    expect(source).toContain(':pager-count="5"')
    expect(source).toContain('<template v-if="hasActiveFilters" #emptyAction>')
    expect(source).toContain('@click="clearFilters">\u6e05\u9664\u7b5b\u9009</button>')
    expect(source).toMatch(/function clearFilters\(\)[\s\S]*onFilterChange\(\)/)
  })

  it('keeps the image drawer as a viewport-level non-modal panel with trigger focus recovery', () => {
    expect(drawerSource).toContain('<Teleport to="body">')
    expect(drawerSource).toContain('tabindex="-1"')
    expect(drawerSource).not.toContain('aria-modal')
    expect(drawerSource).not.toContain('inert')
    expect(drawerSource).toContain('var(--panel-drawer-width)')
    expect(drawerSource).toContain('var(--layer-drawer)')
    expect(drawerSource).toContain('@keydown.esc.prevent.stop="emit(\'close\')"')
    expect(source).toContain('const drawerTrigger = ref(null)')
    expect(source).toContain('drawerTrigger.value = trigger instanceof HTMLElement ? trigger : null')
    expect(source).toMatch(/function closeDrawer\(\)[\s\S]*trigger\?\.isConnected[\s\S]*trigger\.focus\(\)/)
  })
})
