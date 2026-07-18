import { expect, test } from '@playwright/test'

const API_RESULT = (data) => ({ code: 200, message: 'success', data })
const expectedTurnstileToken = process.env.VITE_TURNSTILE_ENABLED === 'true'
  ? 'turnstile-e2e'
  : 'turnstile-disabled'
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
  await page.addInitScript(() => {
    sessionStorage.removeItem('satoken')
    let widgetSequence = 0
    const callbacks = new Map()
    window.turnstile = {
      render(container, options) {
        const widgetId = `turnstile-e2e-${widgetSequence += 1}`
        const widget = document.createElement('div')
        widget.dataset.turnstileWidget = widgetId
        widget.style.width = '300px'
        widget.style.height = '65px'
        container.appendChild(widget)
        callbacks.set(widgetId, options.callback)
        queueMicrotask(() => options.callback('turnstile-e2e'))
        return widgetId
      },
      reset(widgetId) {
        queueMicrotask(() => callbacks.get(widgetId)?.('turnstile-e2e'))
      },
      remove(widgetId) {
        callbacks.delete(widgetId)
      },
    }
  })
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
      layoutHeight: layout.getBoundingClientRect().height,
      asideHeight: aside.getBoundingClientRect().height,
      mainHeight: main.getBoundingClientRect().height,
      contrastRatios,
    }
  })

  expect(styles.pageBackgroundImage).toBe('none')
  expect(styles.layoutShadow).toBe('none')
  expect(styles.dividerWidth).toBe(page.viewportSize().width > 900 ? '1px' : '0px')
  if (page.viewportSize().width > 900) {
    expect(styles.asideHeight).toBe(styles.layoutHeight)
    expect(styles.mainHeight).toBe(styles.layoutHeight)
  }
  expect(Math.min(...styles.contrastRatios.map(sample => sample.ratio))).toBeGreaterThanOrEqual(4.5)
}

async function expectCompactLoginContinuation(page, submitName) {
  const submit = await page.getByRole('button', { name: submitName, exact: true }).boundingBox()
  const continuation = await page.locator('.or-row').boundingBox()
  const gap = continuation.y - (submit.y + submit.height)
  expect(gap).toBeGreaterThanOrEqual(16)
  expect(gap).toBeLessThanOrEqual(32)
}

test.describe('认证页面阶段二回归门禁', () => {
  test('较矮桌面视口将超高内容限制在栏内滚动', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'desktop')
    await page.setViewportSize({ width: 1280, height: 720 })
    await installAuthMocks(page)
    await page.goto('/login')

    const layout = page.locator('.auth-layout')
    const aside = page.locator('.auth-aside')
    const main = page.locator('.auth-main')
    const initialLayout = await layout.boundingBox()
    expect((await aside.boundingBox()).height).toBe(initialLayout.height)
    expect((await main.boundingBox()).height).toBe(initialLayout.height)
    expect((await page.locator('.auth-header').boundingBox()).y).toBeGreaterThanOrEqual(initialLayout.y)
    expect(await main.evaluate(element => getComputedStyle(element, '::-webkit-scrollbar').width)).toBe('12px')

    await page.getByRole('tab', { name: '邮箱登录' }).click()
    expect(await layout.boundingBox()).toEqual(initialLayout)
    expect(await page.evaluate(() => document.documentElement.scrollHeight)).toBe(720)

    await page.goto('/register')
    const registerLayout = await layout.boundingBox()
    expect(registerLayout).toEqual(initialLayout)
    expect((await main.boundingBox()).height).toBe(registerLayout.height)
    expect(await main.evaluate(element => element.scrollHeight > element.clientHeight)).toBe(true)
    await main.evaluate(element => { element.scrollTop = element.scrollHeight })
    const mainBottom = registerLayout.y + registerLayout.height
    const footer = await page.locator('.auth-footer').boundingBox()
    expect(mainBottom - (footer.y + footer.height)).toBeGreaterThanOrEqual(32)
  })

  test('登录页在目标视口保持明亮布局并保留登录方式与验证码发送', async ({ page }) => {
    const requests = await installAuthMocks(page)
    await page.goto('/login')

    await expect(page.getByRole('heading', { name: '回到已经留下的内容' })).toBeVisible()
    await expect(page.getByRole('tab', { name: '密码登录' })).toHaveAttribute('aria-selected', 'true')
    await expect(page.getByRole('button', { name: 'GitHub' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Google' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Microsoft' })).toBeVisible()
    await expectBrightAuthPage(page)
    if (process.env.VITE_TURNSTILE_ENABLED === 'true') {
      await expect(page.locator('[data-turnstile-widget]')).toBeVisible()
      expect((await page.locator('.turnstile-shell').boundingBox()).height).toBe(65)
    }

    const desktopLayoutBeforeSwitch = page.viewportSize().width > 900
      ? await page.locator('.auth-layout').boundingBox()
      : null
    const desktopHeaderBeforeSwitch = page.viewportSize().width > 900
      ? await page.locator('.auth-header').boundingBox()
      : null
    await expectCompactLoginContinuation(page, '登录')

    await page.getByRole('tab', { name: '邮箱登录' }).click()
    await expect(page.getByRole('tab', { name: '邮箱登录' })).toHaveAttribute('aria-selected', 'true')
    await expect(field(page, '邮箱')).toBeFocused()
    if (desktopLayoutBeforeSwitch && desktopHeaderBeforeSwitch) {
      const layoutAfterSwitch = await page.locator('.auth-layout').boundingBox()
      const headerAfterSwitch = await page.locator('.auth-header').boundingBox()
      expect(layoutAfterSwitch).toEqual(desktopLayoutBeforeSwitch)
      expect(headerAfterSwitch).toEqual(desktopHeaderBeforeSwitch)
      expect(await page.evaluate(() => document.documentElement.scrollHeight)).toBe(page.viewportSize().height)
    }
    await expectCompactLoginContinuation(page, '邮箱登录')
    await field(page, '邮箱').fill('demo@example.com')
    await page.getByRole('button', { name: '发送验证码', exact: true }).click()
    await expect.poll(() => requests.code.length).toBe(1)
    expect(requests.code[0]).toMatchObject({
      email: 'demo@example.com',
      turnstileToken: expectedTurnstileToken,
    })
    expect(requests.code[0]).not.toHaveProperty('captchaId')
    expect(requests.code[0]).not.toHaveProperty('captchaCode')

    await field(page, '图形验证码').fill('A7K2')
    await field(page, '邮箱验证码').fill('123456')
    await page.getByRole('button', { name: '邮箱登录', exact: true }).click()
    await expect.poll(() => requests.login.length).toBe(1)
    expect(requests.login[0]).toMatchObject({
      email: 'demo@example.com',
      code: '123456',
      captchaId: 'captcha-e2e',
      captchaCode: 'A7K2',
      turnstileToken: expectedTurnstileToken,
    })

    await page.evaluate(() => sessionStorage.removeItem('satoken'))
    await page.goto('/login')
    await field(page, '用户名').fill('demo')
    await field(page, '密码').fill('secret')
    await field(page, '图形验证码').fill('B8M3')
    await page.getByRole('button', { name: '登录', exact: true }).click()
    await expect.poll(() => requests.login.length).toBe(2)
    expect(requests.login[1]).toMatchObject({
      username: 'demo',
      password: 'secret',
      captchaId: 'captcha-e2e',
      captchaCode: 'B8M3',
      turnstileToken: expectedTurnstileToken,
    })
    await expect(page).toHaveURL(/\/home$/)
    await expect(page.locator('.asset-page')).toBeVisible()
    expect(requests.consoleMessages).toEqual([])
  })

  test('注册页保留验证码流程和真实提交 payload', async ({ page }) => {
    const requests = await installAuthMocks(page)
    await page.goto('/register')

    await expect(page.getByRole('heading', { name: '创建账号' })).toBeVisible()
    await expect(field(page, '用户名')).toBeVisible()
    await expectBrightAuthPage(page)

    await field(page, '用户名').fill('new-user')
    await field(page, '手机号（可选）').fill('13800138000')
    await field(page, '密码').fill('secret123')
    await field(page, '确认密码').fill('secret123')
    await field(page, '邮箱').fill('new@example.com')
    await page.getByRole('button', { name: '发送验证码', exact: true }).click()
    await expect.poll(() => requests.code.length).toBe(1)
    expect(requests.code[0]).toMatchObject({
      email: 'new@example.com',
      purpose: 'register',
      turnstileToken: expectedTurnstileToken,
    })
    expect(requests.code[0]).not.toHaveProperty('captchaId')
    expect(requests.code[0]).not.toHaveProperty('captchaCode')
    await expect(page.locator('.email-code-feedback')).toContainText('验证码已发送至 new@example.com')
    await field(page, '图形验证码').fill('A7K2')
    await field(page, '邮箱验证码').fill('123456')
    await page.getByRole('button', { name: '创建账号', exact: true }).click()
    await expect.poll(() => requests.register.length).toBe(1)
    expect(requests.register[0]).toMatchObject({
      username: 'new-user',
      password: 'secret123',
      confirmPassword: 'secret123',
      email: 'new@example.com',
      phone: '13800138000',
      code: '123456',
      captchaId: 'captcha-e2e',
      captchaCode: 'A7K2',
      turnstileToken: expectedTurnstileToken,
    })
    await expect(page).toHaveURL(/\/login$/)
    expect(requests.consoleMessages).toEqual([])
  })
})
