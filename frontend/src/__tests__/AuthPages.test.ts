import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const authLayout = readFileSync('src/components/auth/AuthLayout.vue', 'utf8')
const login = readFileSync('src/views/Login.vue', 'utf8')
const register = readFileSync('src/views/Register.vue', 'utf8')

describe('authentication page stage two contract', () => {
  it('keeps AuthLayout presentation-only and freezes the bright two-column structure', () => {
    expect(authLayout).toContain("default: 'narrow'")
    expect(authLayout).toContain("['narrow', 'wide']")
    expect(authLayout).toContain('<slot name="aside" />')
    expect(authLayout).toContain('<slot name="header" />')
    expect(authLayout).toContain('<slot name="footer" />')
    expect(authLayout).toMatch(/width: min\(100%, 1120px\)/)
    expect(authLayout).toMatch(/grid-template-columns: minmax\(280px, 0\.85fr\) minmax\(420px, 1\.15fr\)/)
    expect(authLayout).not.toMatch(/from ['"].*router|from ['"].*api|from ['"].*pinia|useRouter|useUserStore/)
    expect(authLayout).not.toMatch(/linear-gradient|radial-gradient/)
    expect([...authLayout.matchAll(/box-shadow:\s*([^;]+)/g)].map(match => match[1].trim())).toEqual(['none', 'none'])
    expect(authLayout).not.toContain('.auth-brand')
    expect(authLayout).toContain('font-size: clamp(48px, 4.6vw, 64px)')
  })

  it('keeps Login focused on login flows and preserves every real entry point', () => {
    expect(login).toContain('form-width="narrow"')
    expect(login).toContain('loginMode')
    expect(login).toContain('loginByCode')
    expect(login).toContain('sendCode')
    expect(login).toContain('getGithubAuthUrl')
    expect(login).toContain('getGoogleAuthUrl')
    expect(login).toContain('TurnstileWidget')
    expect(login).toContain('to="/register"')
    expect(login).toContain('autocomplete="username"')
    expect(login).toContain('autocomplete="current-password"')
    expect(login).toContain('autocomplete="one-time-code"')
    expect(login).toContain('role="tab"')
    expect(login).toContain(':aria-selected="loginMode')
    expect(login).not.toContain('class="auth-brand"')
    expect(login).not.toContain('私人图库')
    expect(login).not.toMatch(/registerForm|registerLoading|registerRules|registerCaptcha|registerCountdown|handleRegister|loadRegisterCaptcha/)
    expect(login).not.toMatch(/linear-gradient|radial-gradient|background:\s*#0|background:\s*rgba\(/)
  })

  it('keeps Register as the single registration implementation with ordered safeguards', () => {
    expect(register).toContain('form-width="wide"')
    expect(register).toContain('register({ ...payload, turnstileToken: token })')
    expect(register).toContain('sendCode({')
    expect(register).toContain('TurnstileWidget')
    expect(register).not.toContain('agreedToTerms')
    expect(register).not.toContain('服务条款')
    expect(register).not.toContain('隐私说明')
    expect(register).toContain('native-type="submit"')
    expect(register).toContain('autocomplete="new-password"')
    expect(register).toContain('autocomplete="email"')
    expect(register).toContain('autocomplete="one-time-code"')
    expect(register).not.toContain('class="auth-brand"')
    expect(register).not.toMatch(/linear-gradient|radial-gradient|background:\s*#0|background:\s*rgba\(/)
  })

  it('keeps validation feedback and the account footer from shifting the auth panel', () => {
    expect(authLayout).toMatch(/\.auth-body :deep\(\.auth-form \.el-form-item__error\)\s*\{[\s\S]*position:\s*absolute/)
    expect(authLayout).toMatch(/\.auth-footer\s*\{[\s\S]*margin-top:/)
  })
})
