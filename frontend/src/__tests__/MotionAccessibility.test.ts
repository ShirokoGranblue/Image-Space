import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import astralEnvironment from '../components/astral/AstralEnvironment.vue?raw'
import astralModeControl from '../components/astral/AstralModeControl.vue?raw'
import baseButton from '../components/ui/BaseButton.vue?raw'
import baseIconButton from '../components/ui/BaseIconButton.vue?raw'
import galleryGrid from '../components/gallery/GalleryGrid.vue?raw'
import galleryItem from '../components/gallery/GalleryItem.vue?raw'
import homeToolbar from '../components/home/HomeToolbar.vue?raw'
import imageViewer from '../components/ImageViewer.vue?raw'
import navBar from '../components/NavBar.vue?raw'
import notificationDrawer from '../components/NotificationDrawer.vue?raw'
import loadingState from '../components/states/LoadingState.vue?raw'
import squareHeroFilters from '../components/square/SquareHeroFilters.vue?raw'
import squareImageDrawer from '../components/square/SquareImageDrawer.vue?raw'

const tokens = readFileSync('src/styles/tokens.css', 'utf8')
const globalStyles = readFileSync('src/style.css', 'utf8')

describe('motion accessibility', () => {
  it('limits the global reduced-motion policy to scroll behavior', () => {
    expect(tokens).not.toContain('transition-duration: 0.01ms')
    expect(tokens).not.toContain('animation-duration: 0.01ms')
    expect(tokens).not.toContain('animation-iteration-count: 1')
    expect(tokens).toMatch(/@media \(prefers-reduced-motion: reduce\)\s*\{\s*html\s*\{\s*scroll-behavior:\s*auto;\s*\}\s*\}/)
  })

  it('uses fade-only reduced variants for entering navigation and drawers', () => {
    expect(navBar).toMatch(/@media \(prefers-reduced-motion: reduce\)[\s\S]*?\.slide-down-enter-active,\.slide-down-leave-active\s*\{\s*transition:\s*opacity 200ms ease/)
    expect(navBar).toMatch(/\.slide-down-enter-from,\.slide-down-leave-to\s*\{\s*opacity:\s*0;\s*transform:\s*none/)
    expect(squareImageDrawer).toMatch(/@media\(prefers-reduced-motion:reduce\)\{[^}]*\.drawer-slide-enter-active,\.drawer-slide-leave-active\{transition:opacity 200ms ease/)
    expect(squareImageDrawer).toMatch(/\.drawer-slide-enter-from \.image-drawer,\.drawer-slide-leave-to \.image-drawer\{transform:none\}/)
    expect(notificationDrawer).toMatch(/@media \(prefers-reduced-motion: reduce\)[\s\S]*?\.notification-slide-enter-active,[\s\S]*?transition:\s*opacity 200ms ease/)
    expect(notificationDrawer).toMatch(/\.notification-slide-enter-from,[\s\S]*?transform:\s*none;\s*opacity:\s*0/)
    expect(astralEnvironment).toContain("window.matchMedia('(prefers-reduced-motion: reduce)')")
  })

  it('stops decorative loading motion while preserving viewer opacity feedback', () => {
    expect(galleryGrid).toMatch(/@media \(prefers-reduced-motion: reduce\)\s*\{\s*\.gallery-skeleton::after\s*\{\s*animation:\s*none/)
    expect(galleryItem).toMatch(/@media \(prefers-reduced-motion: reduce\)[\s\S]*?\.image-placeholder::after\s*\{\s*animation:\s*none/)
    expect(imageViewer).toMatch(/@media \(prefers-reduced-motion: reduce\)[\s\S]*?\.viewer-img\s*\{\s*transition:\s*none/)
    expect(imageViewer).toMatch(/\.viewer-header,\s*\.viewer-nav,\s*\.viewer-toolbar\s*\{\s*transition:\s*opacity 200ms ease/)
    expect(imageViewer).toMatch(/\.viewer-fade-enter-active,\s*\.viewer-fade-leave-active\s*\{\s*transition:\s*opacity 200ms ease/)
    expect(imageViewer).toMatch(/\.viewer-loading-mark::after\s*\{\s*animation:\s*none/)
    expect(loadingState).toMatch(/@media \(prefers-reduced-motion: reduce\)[\s\S]*?\.state-view__loader\s*\{\s*animation:\s*none/)
  })

  it('removes reduced-motion press scaling without suppressing control feedback', () => {
    expect(baseButton).toMatch(/@media \(prefers-reduced-motion: reduce\)[\s\S]*?\.base-button:active:not\(:disabled\)\s*\{\s*transform:\s*none/)
    expect(baseButton).toMatch(/\.base-button__spinner\s*\{\s*animation:\s*none/)
    expect(baseIconButton).toMatch(/@media \(prefers-reduced-motion: reduce\)[\s\S]*?\.base-icon-button:active:not\(:disabled\)\s*\{\s*transform:\s*none/)
    expect(globalStyles).toMatch(/@media \(prefers-reduced-motion: reduce\)[\s\S]*?\.el-button:not\(\.is-disabled\):active\s*\{\s*transform:\s*none/)
    expect(homeToolbar).toMatch(/@media\(prefers-reduced-motion:reduce\)[\s\S]*?\.select-all:active,\.primary-command:active,\.filter-chip:active\{transform:none\}/)
    expect(squareHeroFilters).toMatch(/@media\(prefers-reduced-motion:reduce\)[\s\S]*?\.sort-segment button:active,\.active-filters button:active\{transform:none\}/)
    expect(astralModeControl).toMatch(/\.astral-mode-trigger:active,\s*\.astral-mode-menu button:active\s*\{\s*transform:\s*none/)
    expect(astralModeControl).toMatch(/\.astral-mode-menu-enter-active,\s*\.astral-mode-menu-leave-active\s*\{\s*transition:\s*opacity 200ms ease/)
  })
})
