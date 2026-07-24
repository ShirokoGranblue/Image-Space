import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const profileHeader = readFileSync('src/components/profile/ProfileHeader.vue', 'utf8')
const profileWorks = readFileSync('src/components/profile/ProfileWorksSection.vue', 'utf8')
const notificationDrawer = readFileSync('src/components/NotificationDrawer.vue', 'utf8')

describe('profile and notification typography contract', () => {
  it('uses the shared vermilion eyebrow rule for PROFILE and PUBLIC WORKS only', () => {
    expect(profileHeader).toContain('.profile-name-row .section-label{display:inline-block;padding-left:var(--space-3);border-left:2px solid var(--color-vermilion)}')
    expect(profileWorks).toContain('.works-heading .section-label{display:inline-block;padding-left:var(--space-3);border-left:2px solid var(--color-vermilion)}')
  })

  it('shows the public username beside the profile display name', () => {
    expect(profileHeader).toContain('<UserIdentity :display-name="user.displayName" :username="user.username" />')
  })

  it('switches between keyed profile view and edit roots with reduced-safe motion', () => {
    expect(profileHeader).toContain('<Transition name="profile-mode" mode="out-in">')
    expect(profileHeader).toContain('<div v-if="!editing" key="view" class="profile-view">')
    expect(profileHeader).toContain('<div v-else key="edit" class="profile-edit">')
    expect(profileHeader).toContain('@click="emit(\'save\')">保存</el-button><el-button @click="emit(\'cancel\')">取消</el-button>')
    expect(profileHeader).toMatch(/\.profile-mode-enter-active,\.profile-mode-leave-active\{transition:opacity var\(--duration-standard\) var\(--ease-out\),transform var\(--duration-standard\) var\(--ease-out\)\}/)
    expect(profileHeader).toMatch(/\.profile-mode-enter-from,\.profile-mode-leave-to\{opacity:0;transform:translateY\(8px\)\}/)
    expect(profileHeader).toMatch(/@media\(prefers-reduced-motion:reduce\)\{\.profile-mode-enter-active,\.profile-mode-leave-active\{transition:opacity 200ms ease\}\.profile-mode-enter-from,\.profile-mode-leave-to\{opacity:0;transform:none\}\}/)
  })

  it('keeps the notification title size while matching the main title family and weight', () => {
    expect(notificationDrawer).toContain('font-family: var(--font-title);')
    expect(notificationDrawer).toContain('font-size: var(--text-2xl);')
    expect(notificationDrawer).toContain('font-weight: 600;')
  })
})
