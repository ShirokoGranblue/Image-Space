import { describe, expect, it } from 'vitest'
import loginPage from '../views/Login.vue?raw'
import registerPage from '../views/Register.vue?raw'
import authLayout from '../components/auth/AuthLayout.vue?raw'
import navBar from '../components/NavBar.vue?raw'
import squareDrawer from '../components/square/SquareImageDrawer.vue?raw'
import homeToolbar from '../components/home/HomeToolbar.vue?raw'
import homeBatch from '../components/home/HomeBatchQueue.vue?raw'

const authPages = { loginPage, registerPage }

describe('stage five quality gate contracts', () => {
  it('keeps both auth pages scoped while the shared shell owns the Astral matte surface', () => {
    for (const source of Object.values(authPages)) {
      expect(source.match(/<style scoped>/g)).toHaveLength(1)
      expect(source).not.toMatch(/linear-gradient|radial-gradient|--ad-|Editorial Design System|Aperture Desk/)
    }
    expect(authLayout.match(/<style scoped>/g)).toHaveLength(1)
    expect(authLayout).toContain('background: rgba(14, 16, 23, 0.72)')
    expect(authLayout).toContain('border-radius: var(--radius-lg)')
    expect(authLayout).toContain('box-shadow: var(--shadow-dialog)')
    expect(loginPage).toContain('to="/register"')
    expect(loginPage).not.toMatch(/<Register\b|from ['"].*Register/)
  })

  it('keeps NavBar on one theme and gives the floating drawer viewport ownership', () => {
    expect(navBar.match(/<style scoped>/g)).toHaveLength(1)
    expect(navBar).toContain('var(--nav-height)')
    expect(squareDrawer).toMatch(/<Teleport to="body">[\s\S]*<aside\b/)
    expect(squareDrawer).toContain('var(--layer-drawer)')
  })

  it('keeps shared command controls tied to the frozen height tokens', () => {
    expect(homeToolbar).toMatch(/\.command-panel[\s\S]*var\(--control-height-md\)/)
    expect(homeToolbar).toMatch(/@media\(max-width:650px\)[\s\S]*var\(--control-height-lg\)/)
    expect(homeBatch).toMatch(/min-height\s*:\s*var\(--control-height-md\)/)
  })
})
