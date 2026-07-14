import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import squarePage from '../views/ImageSquare.vue?raw'
import detailPage from '../views/ImageDetail.vue?raw'
import homePage from '../views/Home.vue?raw'
import profilePage from '../views/Profile.vue?raw'
import squareHero from '../components/square/SquareHeroFilters.vue?raw'
import squareAside from '../components/square/SquareDiscoveryAside.vue?raw'
import squareDrawer from '../components/square/SquareImageDrawer.vue?raw'
import detailInfo from '../components/detail/ImageDetailInfoPanel.vue?raw'
import detailComments from '../components/detail/ImageCommentsSection.vue?raw'
import homeToolbar from '../components/home/HomeToolbar.vue?raw'
import homeBatch from '../components/home/HomeBatchQueue.vue?raw'
import profileHeader from '../components/profile/ProfileHeader.vue?raw'
import profileWorks from '../components/profile/ProfileWorksSection.vue?raw'
import profileBackground from '../components/profile/ProfileBackgroundEditor.vue?raw'
import navBar from '../components/NavBar.vue?raw'

const pages = { squarePage, detailPage, homePage, profilePage }
const globalStyles = readFileSync('src/style.css', 'utf8')
const tokens = readFileSync('src/styles/tokens.css', 'utf8')
const privateComponents = {
  squareHero, squareAside, squareDrawer, detailInfo, detailComments,
  homeToolbar, homeBatch, profileHeader, profileWorks, profileBackground,
}

describe('stage three design-system and page-boundary contract', () => {
  it('keeps one active scoped style block in every core page', () => {
    for (const source of Object.values(pages)) {
      expect(source.match(/<style scoped>/g)).toHaveLength(1)
    }
  })

  it('removes the former parallel themes and their token namespace', () => {
    const source = `${Object.values(pages).join('\n')}\n${globalStyles}\n${tokens}`
    expect(source).not.toContain('--ad-')
    expect(source).not.toContain('Editorial Design System')
    expect(source).not.toContain('Aperture Desk')
  })

  it('keeps design variables in tokens.css instead of global integration styles', () => {
    expect(tokens).toContain('--notification-drawer-width: 420px')
    expect(globalStyles).not.toMatch(/:root\s*\{[\s\S]*--notification-drawer-width/)
    expect(globalStyles).not.toContain('linear-gradient')
    expect(globalStyles).not.toContain('radial-gradient')
  })

  it('freezes stage-three visual ownership and page-scope rules', () => {
    expect(navBar.match(/<style scoped>/g)).toHaveLength(1)
    expect(navBar).toContain('var(--nav-height)')
    expect(squarePage).toContain('var(--panel-aside-width)')
    expect(detailInfo).toContain('浏览操作')
    expect(detailInfo).toContain('所有者操作')
    expect(homePage).toContain(':visible="selectedImageUuids.length > 0"')
    expect(profilePage).toContain('作品（本页）')
    expect(profilePage).not.toContain('关注者')
    expect(profilePage).not.toContain('收藏')
  })

  it('uses the frozen page-private sections from their owning pages', () => {
    expect(squarePage).toContain('<SquareHeroFilters')
    expect(squarePage).toContain('<SquareDiscoveryAside')
    expect(squarePage).toContain('<SquareImageDrawer')
    expect(detailPage).toContain('<ImageDetailInfoPanel')
    expect(detailPage).toContain('<ImageCommentsSection')
    expect(homePage).toContain('<HomeToolbar')
    expect(homePage).toContain('<HomeBatchQueue')
    expect(profilePage).toContain('<ProfileHeader')
    expect(profilePage).toContain('<ProfileWorksSection')
    expect(profilePage).toContain('<ProfileBackgroundEditor')
    expect(profilePage).toMatch(/const showProfileMeta = computed[\s\S]*return Boolean\(/)
  })

  it('keeps route, store, and API ownership out of private presentation components', () => {
    for (const [name, source] of Object.entries(privateComponents)) {
      expect(source, name).not.toMatch(/from ['"]\.\.\/\.\.\/api|from ['"]\.\.\/api/)
      expect(source, name).not.toMatch(/useRouter|useRoute|useUserStore|use[A-Za-z]+Store/)
      expect(source, name).toContain('defineProps')
      expect(source, name).toContain('defineEmits')
    }
  })

  it('does not retain selectors owned by the extracted sections in parent pages', () => {
    expect(squarePage).not.toMatch(/^\.(square-hero|square-side|image-drawer)\b/m)
    expect(detailPage).not.toMatch(/^\.(detail-info|comments-section)\b/m)
    expect(homePage).not.toMatch(/^\.(main-head|command-panel|batch-queue)\b/m)
    expect(profilePage).not.toMatch(/^\.(profile-header|user-works|background-editor)\b/m)
  })
})
