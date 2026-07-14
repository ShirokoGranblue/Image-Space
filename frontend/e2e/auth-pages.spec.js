import { expect, test } from '@playwright/test'

const API_RESULT = (data) => ({ code: 200, message: 'success', data })
const captchaImage = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="132" height="44"><rect width="100%" height="100%" fill="#ece6dc"/><text x="12" y="29" fill="#1d2326" font-size="18">A7K2</text></svg>')

function field(page, label) {
  return page.locator('.el-form-item').filter({ hasText: label }).locator('input').first()
}

async function installAuthMocks(page) {
  const requests = {
    login: [],
    code: [],
    register: [],
    consoleMessages: [],
  }

  page.on('console', message => {
    if (message.type() === 'warning' || message.type() === 'error') {
      requests.consoleMessages.push(`${message.type()}: ${message.text()}`)
    }
  })
  page.on('pageerror', error => requests.consoleMessages.push(`pageerror: ${error.message}`))
  await page.addInitScript(() => sessionStorage.removeItem('satoken'))
  await page.route(url => url.pathname.startsWith('/api/'), async route => {
    const request = route.request()
    const requestUrl = new URL(request.url())
    const body = request.postData() ? JSON.parse(request.postData()) : null

    if (requestUrl.pathname === '/api/user/captcha') {
      return route.fulfill({ json: API_RESULT({ captchaId: 'captcha-e2e', captchaImage }) })
    }
    if (requestUrl.pathname === '/api/user/login') {
      requests.login.push(body)
      return route.fulfill({ json: API_RESULT('e2e-login-token') })
    }
    if (requestUrl.pathname === '/api/user/login-by-code') {
      requests.login.push(body)
      return route.fulfill({ json: API_RESULT('e2e-code-token') })
    }
    if (requestUrl.pathname === '/api/user/send-code') {
      requests.code.push(body)
      return route.fulfill({ json: API_RESULT(null) })
    }
    if (requestUrl.pathname === '/api/user/register') {
      requests.register.push(body)
      return route.fulfill({ json: API_RESULT(null) })
    }
    if (requestUrl.pathname === '/api/user/info') {
      return route.fulfill({ json: API_RESULT({ id: 1, username: 'e2e-user', role: 'user' }) })
    }
    if (requestUrl.pathname === '/api/user/oauth/github') {
      return route.fulfill({ json: API_RESULT({ authorizeUrl: 'https://github.com/login/oauth/authorize?client_id=e2e' }) })
    }
    if (requestUrl.pathname === '/api/user/oauth/google') {
      return route.fulfill({ json: API_RESULT({ authorizeUrl: 'https://accounts.google.com/o/oauth2/v2/auth?client_id=e2e' }) })
    }
    if (requestUrl.pathname === '/api/category/list') {
      return route.fulfill({ json: API_RESULT([]) })
    }
    return route.fulfill({ json: API_RESULT({ records: [], total: 0 }) })
  })

  return requests
}

async function expectBrightAuthPage(page) {
  await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  const styles = await page.evaluate(() => {
    const authPage = document.querySelector('.auth-page')
    const layout = document.querySelector('.auth-layout')
    const aside = document.querySelector('.auth-aside')
    const main = document.querySelector('.auth-main')
    const toRgb = value => value.match(/\d+(?:\.\d+)?/g).slice(0, 3).map(Number)
    const channel = value => {
      const normalized = value / 255
      return normalized <= 0.03928 ? normalized / 12.92 : ((normalized + 0.055) / 1.055) ** 2.4
    }
    const luminance = value => {
      const [red, green, blue] = toRgb(value)
      return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
    }
    const contrast = (foreground, background) => {
      const foregroundLuminance = luminance(foreground)
      const backgroundLuminance = luminance(background)
      return (Math.max(foregroundLuminance, backgroundLuminance) + 0.05) / (Math.min(foregroundLuminance, backgroundLuminance) + 0.05)
    }
    const mainBackground = getComputedStyle(main).backgroundColor
    const asideBackground = getComputedStyle(aside).backgroundColor
    const textSamples = [
      ['auth-title', '.auth-title', mainBackground],
      ['auth-description', '.auth-description', mainBackground],
      ['auth-kicker', '.auth-kicker', mainBackground],
      ['auth-aside-title', '.auth-aside-title', asideBackground],
      ['auth-aside-description', '.auth-aside-description', asideBackground],
      ['auth-capabilities', '.auth-capabilities', asideBackground],
      ['form-label', '.auth-form .el-form-item__label', mainBackground],
    ]
    const methodButton = document.querySelector('.login-method-switch button:not(.is-active)')
    if (methodButton) textSamples.push(['login-method', '.login-method-switch button:not(.is-active)', mainBackground])
    const contrastRatios = textSamples.map(([name, selector, background]) => ({
      name,
      ratio: contrast(getComputedStyle(document.querySelector(selector)).color, background),
    }))
    return {
      pageBackgroundImage: getComputedStyle(authPage).backgroundImage,
      layoutShadow: getComputedStyle(layout).boxShadow,
      dividerWidth: getComputedStyle(aside).borderRightWidth,
      contrastRatios,
    }
  })

  expect(styles.pageBackgroundImage).toBe('none')
  expect(styles.layoutShadow).toBe('none')
  expect(styles.dividerWidth).toBe(page.viewportSize().width > 900 ? '1px' : '0px')
  expect(Math.min(...styles.contrastRatios.map(sample => sample.ratio))).toBeGreaterThanOrEqual(4.5)
}

test.describe('认证页面阶段二回归门禁', () => {
  test('登录页在目标视口保持明亮布局并保留登录方式与验证码发送', async ({ page }) => {
    const requests = await installAuthMocks(page)
    await page.goto('/login')

    await expect(page.getByRole('heading', { name: '欢迎回来' })).toBeVisible()
    await expect(page.getByRole('tab', { name: '密码登录' })).toHaveAttribute('aria-selected', 'true')
    await expect(page.getByRole('button', { name: 'GitHub' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Google' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Microsoft' })).toBeVisible()
    await expectBrightAuthPage(page)

    await page.getByRole('tab', { name: '邮箱登录' }).click()
    await expect(page.getByRole('tab', { name: '邮箱登录' })).toHaveAttribute('aria-selected', 'true')
    await expect(field(page, '邮箱')).toBeFocused()
    await field(page, '邮箱').fill('demo@example.com')
    await field(page, '图形验证码').fill('A7K2')
    await page.getByRole('button', { name: '发送验证码', exact: true }).click()
    await expect.poll(() => requests.code.length).toBe(1)
    expect(requests.code[0]).toMatchObject({
      email: 'demo@example.com',
      captchaId: 'captcha-e2e',
      captchaCode: 'A7K2',
      turnstileToken: 'turnstile-disabled',
    })
    expect(requests.consoleMessages).toEqual([])

    await page.getByRole('tab', { name: '密码登录' }).click()
    await expect(field(page, '用户名')).toBeFocused()
    await field(page, '用户名').fill('demo')
    await field(page, '密码').fill('secret')
    await page.getByRole('button', { name: '登录', exact: true }).click()
    await expect.poll(() => requests.login.length).toBe(1)
    expect(requests.login[0]).toMatchObject({ username: 'demo', password: 'secret', turnstileToken: 'turnstile-disabled' })
    await expect(page).toHaveURL(/\/home$/)
    await expect(page.locator('.asset-page')).toBeVisible()
    expect(requests.consoleMessages).toEqual([])
  })

  test('注册页保留验证码流程、协议确认和真实提交 payload', async ({ page }) => {
    const requests = await installAuthMocks(page)
    await page.goto('/register')

    await expect(page.getByRole('heading', { name: '创建账号' })).toBeVisible()
    await expect(field(page, '用户名')).toBeVisible()
    await expect(page.getByRole('checkbox', { name: /服务条款与隐私说明/ })).not.toBeChecked()
    await expectBrightAuthPage(page)

    await field(page, '用户名').fill('new-user')
    await field(page, '手机号（可选）').fill('13800138000')
    await field(page, '密码').fill('secret123')
    await field(page, '确认密码').fill('secret123')
    await field(page, '邮箱').fill('new@example.com')
    await field(page, '图形验证码').fill('A7K2')
    await page.getByRole('button', { name: '发送验证码', exact: true }).click()
    await expect.poll(() => requests.code.length).toBe(1)
    await field(page, '图形验证码').fill('A7K2')
    await field(page, '邮箱验证码').fill('123456')
    await page.getByRole('button', { name: '创建账号', exact: true }).click()
    await expect(page.locator('.agreement-error')).toContainText('请先同意服务条款与隐私说明')
    expect(requests.register).toHaveLength(0)

    await page.locator('.agreement-item .el-checkbox').click()
    await expect(page.getByRole('checkbox', { name: /服务条款与隐私说明/ })).toBeChecked()
    await page.getByRole('button', { name: '创建账号', exact: true }).click()
    await expect.poll(() => requests.register.length).toBe(1)
    expect(requests.register[0]).toMatchObject({
      username: 'new-user',
      password: 'secret123',
      email: 'new@example.com',
      phone: '13800138000',
      code: '123456',
      turnstileToken: 'turnstile-disabled',
    })
    expect(requests.register[0]).not.toHaveProperty('confirmPassword')
    expect(requests.register[0]).not.toHaveProperty('captchaId')
    expect(requests.register[0]).not.toHaveProperty('captchaCode')
    await expect(page).toHaveURL(/\/login$/)
    expect(requests.consoleMessages).toEqual([])
  })
})
