import { expect, test } from '@playwright/test'

const apiResult = data => ({ code: 200, message: 'success', data })

async function installPublicMocks(page, { admin = false } = {}) {
  await page.route(url => url.pathname.startsWith('/api/'), async route => {
    const path = new URL(route.request().url()).pathname

    if (admin && path === '/api/user/info') {
      await route.fulfill({
        json: apiResult({ id: 1, uuid: 'astral-admin', username: 'astral-admin', role: 'admin' }),
      })
      return
    }

    if (path === '/api/category/list') {
      await route.fulfill({ json: apiResult([]) })
      return
    }

    await route.fulfill({ json: apiResult({ records: [], total: 0 }) })
  })
}

async function readCanvasFrame(canvas) {
  return canvas.evaluate(element => {
    const context = element.getContext('2d')
    const pixels = context.getImageData(0, 0, element.width, element.height).data
    let alpha = 0
    let spatialHash = 0
    for (let index = 3; index < pixels.length; index += 4) {
      if (pixels[index] === 0) continue
      alpha += pixels[index]
      spatialHash = (spatialHash + pixels[index] * ((index / 4) % 997)) % 2147483647
    }
    return { alpha, spatialHash }
  })
}

test.describe('AstralSpace 视觉系统', () => {
  const routeCases = [
    { path: '/login', intensity: 'strong', nav: false },
    { path: '/register', intensity: 'strong', nav: false },
    { path: '/403', intensity: 'strong', nav: false },
    { path: '/not-a-real-route', intensity: 'strong', nav: false },
    { path: '/square', intensity: 'medium', nav: true },
    { path: '/image/astral-e2e', intensity: 'medium', nav: true },
    { path: '/profile/astral-e2e', intensity: 'medium', nav: true },
    { path: '/home', intensity: 'quiet', nav: true, authenticated: true },
    { path: '/admin/audit-log', intensity: null, nav: false, authenticated: true, admin: true },
  ]

  for (const routeCase of routeCases) {
    test(`${routeCase.path} 使用 ${routeCase.intensity || 'neutral'} 氛围并遵守导航边界`, async ({ page }) => {
      if (routeCase.authenticated) {
        await page.addInitScript(() => sessionStorage.setItem('satoken', 'astral-e2e-token'))
      }
      await installPublicMocks(page, { admin: routeCase.admin })
      await page.goto(routeCase.path)

      if (routeCase.intensity) {
        await expect(page.locator(`[data-astral-intensity="${routeCase.intensity}"]`)).toHaveCount(1)
        await expect(page.locator('canvas[data-astral-canvas]')).toHaveCount(1)
      } else {
        await expect(page.locator('.astral-environment')).toHaveCount(0)
        await expect(page.getByRole('button', { name: 'Astral modes' })).toHaveCount(0)
      }
      await expect(page.locator('.navbar')).toHaveCount(routeCase.nav ? 1 : 0)
    })
  }

  test('公开广场呈现固定的中强度星象环境', async ({ page }) => {
    await installPublicMocks(page)
    await page.goto('/square')

    const nav = page.locator('.navbar')
    await expect(nav).toBeVisible()
    expect(await nav.evaluate(element => getComputedStyle(element).position)).toBe('fixed')
    await expect(page.locator('[data-astral-intensity="medium"]')).toHaveCount(1)
    await expect(page.locator('canvas[data-astral-canvas]')).toHaveCount(1)
  })

  test('导航滚动前后始终保持当前视口的固定高度', async ({ page }) => {
    await installPublicMocks(page)
    await page.goto('/square')
    await page.evaluate(() => {
      const spacer = document.createElement('div')
      spacer.style.height = '2000px'
      document.body.append(spacer)
    })

    const navInner = page.locator('.navbar-inner')
    const expectedHeight = page.viewportSize().width <= 900 ? '64px' : '72px'
    await expect(navInner).toHaveCSS('min-height', expectedHeight)

    await page.evaluate(() => window.scrollTo(0, 600))
    await expect.poll(() => page.evaluate(() => window.scrollY)).toBe(600)
    await expect(page.locator('.navbar')).toHaveClass(/scrolled/)
    await expect(navInner).toHaveCSS('min-height', expectedHeight)
  })

  test('AstralSpace 无装饰点且文字渐变随页面滚动', async ({ page }) => {
    await installPublicMocks(page)
    await page.goto('/square')
    await page.evaluate(() => {
      const spacer = document.createElement('div')
      spacer.style.height = '2000px'
      document.body.append(spacer)
    })

    const wordmark = page.locator('.logo-wordmark')
    const initialStyle = await wordmark.evaluate(element => {
      const style = getComputedStyle(element)
      return {
        backgroundImage: style.backgroundImage,
        backgroundPosition: style.backgroundPosition,
        beforeContent: getComputedStyle(element, '::before').content,
        afterContent: getComputedStyle(element, '::after').content,
      }
    })

    expect(initialStyle.backgroundImage).toContain('linear-gradient')
    expect(['none', 'normal', '""']).toContain(initialStyle.beforeContent)
    expect(['none', 'normal', '""']).toContain(initialStyle.afterContent)

    await page.evaluate(() => window.scrollTo(0, 600))
    await expect.poll(() => page.evaluate(() => window.scrollY)).toBe(600)
    await expect.poll(async () => wordmark.evaluate(element => getComputedStyle(element).backgroundPosition))
      .not.toBe(initialStyle.backgroundPosition)
  })

  test('基础视觉采用平衡的八色暗色宇宙色板', async ({ page }) => {
    await installPublicMocks(page)
    await page.goto('/square')

    const theme = await page.evaluate(() => {
      const style = getComputedStyle(document.documentElement)
      const read = name => style.getPropertyValue(name).trim().toLowerCase()
      return {
        colorScheme: style.colorScheme,
        canvas: getComputedStyle(document.body).backgroundColor,
        palette: {
          ink: read('--astral-ink'),
          surface: read('--astral-surface'),
          plum: read('--astral-plum'),
          teal: read('--astral-teal'),
          blue: read('--astral-blue'),
          rose: read('--astral-rose'),
          gold: read('--astral-gold'),
          starlight: read('--astral-starlight'),
        },
      }
    })

    expect(theme.colorScheme).toBe('dark')
    expect(theme.canvas).toBe('rgb(14, 16, 23)')
    expect(theme.palette).toEqual({
      ink: '#0e1017',
      surface: '#191c25',
      plum: '#695879',
      teal: '#5d8f8b',
      blue: '#556d91',
      rose: '#a77b83',
      gold: '#b9a36f',
      starlight: '#f4f1ed',
    })
  })

  test('非管理页面让多色星象环境透过内容壳可见', async ({ page }) => {
    await installPublicMocks(page)
    await page.goto('/square')

    await expect(page.locator('.app-shell')).toHaveCSS('background-color', 'rgba(0, 0, 0, 0)')
    const environmentBackground = await page.locator('.astral-environment').evaluate(
      element => getComputedStyle(element).backgroundImage,
    )
    expect(environmentBackground.match(/radial-gradient/g)?.length).toBeGreaterThanOrEqual(3)
  })

  test('品牌与可见界面字体使用受控字重', async ({ page }) => {
    await installPublicMocks(page)
    await page.goto('/square')

    await expect(page.locator('.logo-wordmark')).toHaveCSS('font-weight', '500')
    await expect(page.locator('h1')).toHaveCSS('font-weight', '600')
    const overweightElements = await page.locator('body *').evaluateAll(elements => elements
      .filter(element => {
        const box = element.getBoundingClientRect()
        return box.width > 0 && box.height > 0 && Number.parseInt(getComputedStyle(element).fontWeight, 10) > 600
      })
      .map(element => ({
        tag: element.tagName.toLowerCase(),
        className: element.className,
        weight: getComputedStyle(element).fontWeight,
      })))

    expect(overweightElements).toEqual([])
  })

  test('Astral modes 在本地保存静谧模式并在刷新后恢复', async ({ page }) => {
    await installPublicMocks(page)
    await page.goto('/square')

    const trigger = page.getByRole('button', { name: 'Astral modes' })
    await expect(trigger).toBeVisible()
    await trigger.click()
    await page.getByRole('button', { name: '静谧' }).click()

    await expect(page.locator('[data-astral-intensity="medium"]')).toHaveAttribute('data-astral-mode', 'quiet')
    await expect.poll(() => page.evaluate(() => localStorage.getItem('astral-mode'))).toBe('quiet')

    await page.reload()
    await expect(page.locator('[data-astral-intensity="medium"]')).toHaveAttribute('data-astral-mode', 'quiet')
  })

  test('流光模式用 DPR 上限 1.5 的 Canvas 绘制连续星点帧', async ({ page }) => {
    await installPublicMocks(page)
    await page.goto('/square')

    const canvas = page.locator('canvas[data-astral-canvas]')
    const canvasMetrics = await canvas.evaluate(element => ({
      width: element.width,
      height: element.height,
      expectedWidth: Math.round(innerWidth * Math.min(devicePixelRatio, 1.5)),
      expectedHeight: Math.round(innerHeight * Math.min(devicePixelRatio, 1.5)),
    }))
    expect(canvasMetrics.width).toBe(canvasMetrics.expectedWidth)
    expect(canvasMetrics.height).toBe(canvasMetrics.expectedHeight)

    const readFrame = () => readCanvasFrame(canvas)

    await expect.poll(readFrame).not.toEqual({ alpha: 0, spatialHash: 0 })
    const firstFrame = await readFrame()
    await page.waitForTimeout(180)
    const secondFrame = await readFrame()
    expect(secondFrame).not.toEqual(firstFrame)
  })

  test('减少动态偏好将流光模式降级为保留画面的静帧', async ({ page }) => {
    await page.emulateMedia({ reducedMotion: 'reduce' })
    await installPublicMocks(page)
    await page.goto('/square')

    const canvas = page.locator('canvas[data-astral-canvas]')
    await expect.poll(() => readCanvasFrame(canvas)).not.toEqual({ alpha: 0, spatialHash: 0 })
    const firstFrame = await readCanvasFrame(canvas)
    await page.waitForTimeout(220)
    expect(await readCanvasFrame(canvas)).toEqual(firstFrame)
  })

  test('强制颜色模式隐藏装饰画面并保持品牌与模式控件可读', async ({ page }) => {
    await page.emulateMedia({ forcedColors: 'active' })
    await installPublicMocks(page)
    await page.goto('/square')

    await expect(page.locator('canvas[data-astral-canvas]')).toHaveCSS('display', 'none')
    await expect(page.locator('svg[data-astral-score]')).toHaveCSS('display', 'none')
    const wordmark = await page.locator('.logo-wordmark').evaluate(element => {
      const style = getComputedStyle(element)
      return {
        backgroundImage: style.backgroundImage,
        color: style.color,
        textFillColor: style.webkitTextFillColor,
      }
    })
    expect(wordmark.backgroundImage).toBe('none')
    expect(wordmark.color).not.toBe('rgba(0, 0, 0, 0)')
    expect(wordmark.textFillColor).not.toBe('rgba(0, 0, 0, 0)')

    const trigger = page.getByRole('button', { name: 'Astral modes' })
    await expect(trigger).toBeVisible()
    await expect(trigger).toHaveCSS('border-style', 'solid')
  })

  test('静谧模式保持静帧，熄灭模式隐藏星谱且可恢复静态主题', async ({ page }) => {
    await installPublicMocks(page)
    await page.goto('/square')

    const canvas = page.locator('canvas[data-astral-canvas]')
    const trigger = page.getByRole('button', { name: 'Astral modes' })
    await trigger.click()
    await page.getByRole('button', { name: '静谧' }).click()
    await expect(page.locator('.astral-environment')).toHaveAttribute('data-astral-mode', 'quiet')
    const quietFrame = await readCanvasFrame(canvas)
    await page.waitForTimeout(220)
    expect(await readCanvasFrame(canvas)).toEqual(quietFrame)

    await trigger.click()
    await page.getByRole('button', { name: '熄灭' }).click()
    await expect.poll(() => readCanvasFrame(canvas)).toEqual({ alpha: 0, spatialHash: 0 })
    await expect(page.locator('svg[data-astral-score]')).toHaveCSS('opacity', '0')

    await trigger.click()
    await page.getByRole('button', { name: '静谧' }).click()
    await expect.poll(() => readCanvasFrame(canvas)).not.toEqual({ alpha: 0, spatialHash: 0 })
    const restoredQuietFrame = await readCanvasFrame(canvas)
    await page.waitForTimeout(220)
    expect(await readCanvasFrame(canvas)).toEqual(restoredQuietFrame)
    await expect(page.locator('svg[data-astral-score]')).not.toHaveCSS('opacity', '0')
  })

  test('阻塞 overlay 打开时冻结当前帧并在关闭后继续', async ({ page }) => {
    await installPublicMocks(page)
    await page.goto('/square')

    const environment = page.locator('.astral-environment')
    const canvas = page.locator('canvas[data-astral-canvas]')
    await page.mouse.click(24, 500)
    await expect(environment).toHaveAttribute('data-astral-rings', '1')
    await page.evaluate(() => {
      const overlay = document.createElement('div')
      overlay.className = 'el-overlay'
      overlay.dataset.astralTestOverlay = 'true'
      document.body.append(overlay)
    })

    await expect(environment).toHaveAttribute('data-astral-paused', 'true')
    const pausedFrame = await readCanvasFrame(canvas)
    await page.waitForTimeout(900)
    expect(await readCanvasFrame(canvas)).toEqual(pausedFrame)

    await page.locator('[data-astral-test-overlay]').evaluate(element => element.remove())
    await expect(environment).toHaveAttribute('data-astral-paused', 'false')
    await page.evaluate(() => new Promise(resolve => requestAnimationFrame(resolve)))
    const firstResumeFrame = await readCanvasFrame(canvas)
    await expect(environment).toHaveAttribute('data-astral-rings', '1')
    await page.waitForTimeout(220)
    expect(await readCanvasFrame(canvas)).not.toEqual(firstResumeFrame)
  })

  test('流光模式在精细指针上提供细微视差与有界空白点击星环', async ({ page }) => {
    await installPublicMocks(page)
    await page.goto('/square')
    test.skip(!await page.evaluate(() => matchMedia('(hover: hover) and (pointer: fine)').matches))

    const environment = page.locator('.astral-environment')
    const canvas = page.locator('canvas[data-astral-canvas]')
    const initialTransform = await canvas.evaluate(element => getComputedStyle(element).transform)
    const viewport = page.viewportSize()
    await page.mouse.move(viewport.width - 24, viewport.height - 24)
    await expect.poll(() => canvas.evaluate(element => getComputedStyle(element).transform))
      .not.toBe(initialTransform)

    await page.locator('.square-hero h1').dispatchEvent('pointerdown', {
      clientX: 360,
      clientY: 160,
      pointerType: 'mouse',
    })
    expect(await environment.getAttribute('data-astral-rings')).toBe('0')

    await page.locator('.public-square-page').dispatchEvent('pointerdown', {
      clientX: 4,
      clientY: viewport.height / 2,
      pointerType: 'mouse',
    })
    expect(await environment.getAttribute('data-astral-rings')).toBe('1')
    await expect.poll(() => environment.getAttribute('data-astral-rings')).toBe('0')
  })

  test('粗指针设备不启用 Canvas 视差', async ({ page }) => {
    await installPublicMocks(page)
    await page.goto('/square')
    test.skip(await page.evaluate(() => matchMedia('(hover: hover) and (pointer: fine)').matches))

    const canvas = page.locator('canvas[data-astral-canvas]')
    const initialTransform = await canvas.evaluate(element => getComputedStyle(element).transform)
    await page.evaluate(() => {
      document.dispatchEvent(new PointerEvent('pointermove', {
        clientX: innerWidth - 24,
        clientY: innerHeight - 24,
        pointerType: 'touch',
      }))
    })
    await page.waitForTimeout(220)
    await expect(canvas).toHaveCSS('transform', initialTransform)
  })

  for (const assetCase of [
    { path: '/login', intensity: 'strong', asset: 'astral-veil-strong' },
    { path: '/square', intensity: 'medium', asset: 'astral-atlas-medium' },
    { path: '/home', intensity: 'quiet', asset: 'astral-horizon-quiet', authenticated: true },
  ]) {
    test(`${assetCase.path} 加载原创本地氛围资产 ${assetCase.asset}`, async ({ page }) => {
      if (assetCase.authenticated) {
        await page.addInitScript(() => sessionStorage.setItem('satoken', 'astral-e2e-token'))
      }
      await installPublicMocks(page)
      await page.goto(assetCase.path)

      const environment = page.locator('.astral-environment')
      await expect(environment).toHaveAttribute('data-astral-intensity', assetCase.intensity)
      const backgroundImage = await environment.evaluate(
        element => getComputedStyle(element).backgroundImage,
      )
      expect(backgroundImage).toContain(assetCase.asset)
    })
  }

  test('星象环境包含唯一的抽象 SVG 星谱层', async ({ page }) => {
    await installPublicMocks(page)
    await page.goto('/square')

    const score = page.locator('svg[data-astral-score]')
    await expect(score).toHaveCount(1)
    expect(await score.locator('path').count()).toBeGreaterThanOrEqual(5)
    expect(await score.locator('circle').count()).toBeGreaterThanOrEqual(4)
  })

  test('认证与广场表面让星象环境透出但保持 matte 层级', async ({ page }) => {
    await installPublicMocks(page)
    await page.goto('/login')

    await expect(page.locator('.auth-page')).toHaveCSS('background-color', 'rgba(0, 0, 0, 0)')
    await expect(page.locator('.auth-layout')).toHaveCSS('border-radius', '16px')
    const authSurfaceAlpha = await page.locator('.auth-layout').evaluate(element => {
      const channels = getComputedStyle(element).backgroundColor.match(/[\d.]+/g).map(Number)
      return channels.length === 4 ? channels[3] : 1
    })
    expect(authSurfaceAlpha).toBeGreaterThan(0.5)
    expect(authSurfaceAlpha).toBeLessThan(1)

    await page.goto('/square')
    await expect(page.locator('.public-square-page')).toHaveCSS('background-color', 'rgba(0, 0, 0, 0)')
    const heroStyle = await page.locator('.square-hero').evaluate(element => {
      const style = getComputedStyle(element)
      const channels = style.backgroundColor.match(/[\d.]+/g).map(Number)
      return {
        alpha: channels.length === 4 ? channels[3] : 1,
        backgroundImage: style.backgroundImage,
      }
    })
    expect(heroStyle.alpha).toBeLessThan(1)
    expect(heroStyle.backgroundImage).toContain('radial-gradient')
  })

  test('首页、个人页与详情页采用各自强度的透明根与 matte 内容面', async ({ page }) => {
    await page.addInitScript(() => sessionStorage.setItem('satoken', 'astral-e2e-token'))
    await installPublicMocks(page)
    await page.goto('/home')

    await expect(page.locator('.asset-page')).toHaveCSS('background-color', 'rgba(0, 0, 0, 0)')
    const workspaceAlpha = await page.locator('.asset-main').evaluate(element => {
      const channels = getComputedStyle(element).backgroundColor.match(/[\d.]+/g).map(Number)
      return channels.length === 4 ? channels[3] : 1
    })
    expect(workspaceAlpha).toBeGreaterThanOrEqual(0.85)
    expect(workspaceAlpha).toBeLessThan(1)

    await page.goto('/profile/astral-e2e')
    await expect(page.locator('.profile-page')).toHaveCSS('background-color', 'rgba(0, 0, 0, 0)')
    const bannerColors = await page.locator('.banner-cell').evaluateAll(elements => [
      ...new Set(elements.map(element => getComputedStyle(element).backgroundColor)),
    ])
    expect(bannerColors).toEqual(expect.arrayContaining([
      'rgb(105, 88, 121)',
      'rgb(93, 143, 139)',
      'rgb(85, 109, 145)',
      'rgb(167, 123, 131)',
      'rgb(185, 163, 111)',
    ]))

    await page.goto('/image/astral-e2e')
    await expect(page.locator('.detail-page')).toHaveCSS('background-color', 'rgba(0, 0, 0, 0)')
  })

  test('详情图片 hover 后仍保持原始亮度与尺寸', async ({ page }) => {
    await installPublicMocks(page)
    const preview = 'data:image/svg+xml,' + encodeURIComponent(
      '<svg xmlns="http://www.w3.org/2000/svg" width="640" height="480"><rect width="640" height="480" fill="#5d8f8b"/></svg>',
    )
    await page.route(url => new URL(url).pathname === '/api/image/astral-image', route => route.fulfill({
      json: apiResult({
        id: 1,
        uuid: 'astral-image',
        imageName: 'Astral preview',
        mediumUrl: preview,
        publicUrl: preview,
        visibility: 'PUBLIC',
        width: 640,
        height: 480,
        tags: '',
      }),
    }))
    await page.route(url => new URL(url).pathname === '/api/comment/list/astral-image', route => route.fulfill({
      json: apiResult([]),
    }))
    await page.goto('/image/astral-image')

    const image = page.locator('.detail-image img')
    await expect(image).toBeVisible()
    await page.locator('.detail-image').hover()
    await expect(image).toHaveCSS('filter', 'none')
    await expect(image).toHaveCSS('transform', 'none')
  })

  test('打开沉浸查看器不锁定文档滚动或改变页面几何', async ({ page }) => {
    await installPublicMocks(page)
    const preview = 'data:image/svg+xml,' + encodeURIComponent(
      '<svg xmlns="http://www.w3.org/2000/svg" width="640" height="480"><rect width="640" height="480" fill="#556d91"/></svg>',
    )
    await page.route(url => new URL(url).pathname === '/api/image/astral-viewer', route => route.fulfill({
      json: apiResult({
        id: 2,
        uuid: 'astral-viewer',
        imageName: 'Astral viewer',
        mediumUrl: preview,
        publicUrl: preview,
        visibility: 'PUBLIC',
        width: 640,
        height: 480,
        tags: '',
      }),
    }))
    await page.route(url => new URL(url).pathname === '/api/comment/list/astral-viewer', route => route.fulfill({
      json: apiResult([]),
    }))
    await page.goto('/image/astral-viewer')
    await expect(page.locator('.detail-image img')).toBeVisible()
    await page.evaluate(() => {
      const spacer = document.createElement('div')
      spacer.style.height = '1600px'
      document.querySelector('.detail-page').append(spacer)
      window.scrollTo(0, 420)
    })
    await expect.poll(() => page.evaluate(() => window.scrollY)).toBe(420)

    const readGeometry = () => page.evaluate(() => {
      const rect = document.querySelector('.detail-layout').getBoundingClientRect()
      const triggerRect = document.querySelector('.detail-image').getBoundingClientRect()
      return {
        scrollY: window.scrollY,
        clientWidth: document.documentElement.clientWidth,
        layout: { x: rect.x, y: rect.y, width: rect.width, height: rect.height },
        trigger: { x: triggerRect.x, y: triggerRect.y, width: triggerRect.width, height: triggerRect.height },
      }
    })
    const before = await readGeometry()

    await page.locator('.detail-image').evaluate(element => element.click())
    await expect(page.getByRole('dialog', { name: '图片查看器：Astral viewer' })).toBeVisible()

    expect(await page.evaluate(() => ({
      bodyOverflow: document.body.style.overflow,
      documentOverflow: getComputedStyle(document.documentElement).overflowY,
    }))).toEqual({ bodyOverflow: '', documentOverflow: 'auto' })
    expect(await readGeometry()).toEqual(before)

    await page.getByRole('button', { name: '关闭查看器' }).click()
    await expect(page.getByRole('dialog', { name: '图片查看器：Astral viewer' })).toBeHidden()
    expect(await readGeometry()).toEqual(before)
  })

  test('打开上传弹窗不改变滚动位置、视口宽度或页面几何', async ({ page }) => {
    await page.addInitScript(() => sessionStorage.setItem('satoken', 'astral-e2e-token'))
    await installPublicMocks(page)
    await page.goto('/home')
    await expect(page.locator('.asset-page')).toBeVisible()
    await page.evaluate(() => {
      const spacer = document.createElement('div')
      spacer.style.height = '1600px'
      document.querySelector('.asset-page').append(spacer)
      window.scrollTo(0, 420)
    })
    await expect.poll(() => page.evaluate(() => window.scrollY)).toBe(420)

    const desktopUploadTrigger = page.locator('.navbar-inner > .navbar-right > .upload-nav-btn')
    const hasVisibleDesktopTrigger = await desktopUploadTrigger.isVisible()
    const geometryAnchor = hasVisibleDesktopTrigger
      ? desktopUploadTrigger
      : page.locator('.navbar .mobile-toggle')
    await geometryAnchor.evaluate(element => {
      element.dataset.astralGeometryTrigger = 'true'
    })

    const readGeometry = () => page.evaluate(() => {
      const serializeRect = selector => {
        const rect = document.querySelector(selector).getBoundingClientRect()
        return { x: rect.x, y: rect.y, width: rect.width, height: rect.height }
      }
      return {
        scrollY: window.scrollY,
        clientWidth: document.documentElement.clientWidth,
        navbar: serializeRect('.navbar'),
        shell: serializeRect('.asset-shell'),
        trigger: serializeRect('[data-astral-geometry-trigger]'),
      }
    })

    const before = await readGeometry()
    if (hasVisibleDesktopTrigger) {
      await desktopUploadTrigger.click()
    } else {
      await page.evaluate(() => window.dispatchEvent(new CustomEvent('image-space:open-upload')))
    }
    await expect(page.getByRole('dialog', { name: '上传图片并归档' })).toBeVisible()

    expect(await page.evaluate(() => document.body.classList.contains('el-popup-parent--hidden'))).toBe(false)
    expect(await readGeometry()).toEqual(before)

    await page.getByRole('dialog', { name: '上传图片并归档' }).getByRole('button', { name: '取消' }).click()
    await expect(page.getByRole('dialog', { name: '上传图片并归档' })).toBeHidden()
    expect(await readGeometry()).toEqual(before)
  })

  test('广场抽屉打开和关闭均不改变触发器、文档或页面几何', async ({ page }) => {
    await installPublicMocks(page)
    await page.goto('/square?demoCards=1')
    await expect(page.locator('.gallery-item')).toHaveCount(3)
    await page.evaluate(() => {
      const spacer = document.createElement('div')
      spacer.style.height = '1600px'
      document.querySelector('.public-square-page').append(spacer)
      window.scrollTo(0, 420)
    })
    await expect.poll(() => page.evaluate(() => window.scrollY)).toBe(420)

    const readGeometry = () => page.evaluate(() => {
      const serializeRect = selector => {
        const rect = document.querySelector(selector).getBoundingClientRect()
        return { x: rect.x, y: rect.y, width: rect.width, height: rect.height }
      }
      return {
        scrollY: window.scrollY,
        clientWidth: document.documentElement.clientWidth,
        navbar: serializeRect('.navbar'),
        shell: serializeRect('.square-shell'),
        trigger: serializeRect('.gallery-item .gallery-item__media-button'),
      }
    })

    const before = await readGeometry()
    await page.getByRole('button', { name: '查看图片：山色试映.jpg' }).evaluate(element => element.click())
    await expect(page.locator('.image-drawer')).toBeVisible()
    expect(await readGeometry()).toEqual(before)

    await page.locator('.image-drawer').getByRole('button', { name: '关闭' }).click()
    await expect(page.locator('.image-drawer')).toBeHidden()
    expect(await readGeometry()).toEqual(before)
  })
})
