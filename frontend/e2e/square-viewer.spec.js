import { expect, test } from '@playwright/test'

const API_RESULT = (data) => ({ code: 200, message: 'success', data })
const svg = (label, color = '#71869a') => `data:image/svg+xml,${encodeURIComponent(`<svg xmlns="http://www.w3.org/2000/svg" width="1600" height="900" viewBox="0 0 1600 900"><rect width="100%" height="100%" fill="${color}"/><text x="80" y="160" fill="#f8f5ee" font-family="Arial" font-size="72">${label}</text></svg>`)}`

function image({ uuid, imageName, width, height, color, imageUrl, originalFilename }) {
  const preview = svg(imageName, color)
  return {
    uuid,
    imageName,
    originalFilename: originalFilename || `${uuid}.jpg`,
    width,
    height,
    thumbUrl: preview,
    mediumUrl: preview,
    imageUrl: imageUrl || preview,
    visibility: 'PUBLIC',
    categoryId: 1,
    categoryName: '纪实',
    username: 'gallery-author',
    displayName: '画廊作者',
    userUuid: 'gallery-author-uuid',
    tags: '#城市#光影',
    likeCount: 3,
  }
}

const galleryImages = [
  image({ uuid: 'landscape', imageName: '横向静景', width: 1600, height: 900, color: '#617a8b' }),
  image({ uuid: 'portrait', imageName: '竖向建筑', width: 900, height: 1500, color: '#8a7463' }),
  image({ uuid: 'square', imageName: '方形构图', width: 1200, height: 1200, color: '#526d66' }),
  image({ uuid: 'wide', imageName: '超宽画幅', width: 2400, height: 800, color: '#4b5f7b' }),
  image({ uuid: 'photo', imageName: 'MyPhoto.JPG', width: 1800, height: 1200, color: '#756556' }),
  image({ uuid: 'race-error', imageName: '竞态错误图', width: 1600, height: 900, color: '#8c5954', imageUrl: 'https://e2e.image-space.test/original/slow-error.svg' }),
  image({ uuid: 'race-success', imageName: '竞态成功图', width: 1600, height: 900, color: '#556f80' }),
  image({ uuid: 'missing', imageName: '原图失败可重试', width: 1600, height: 900, color: '#75565b', imageUrl: 'https://e2e.image-space.test/original/missing.svg' }),
  image({ uuid: 'fallback-name', imageName: '', originalFilename: 'camera-original.png', width: 1000, height: 1000, color: '#5f6e64' }),
]

const pagedImages = Array.from({ length: 75 }, (_, index) => image({
  uuid: `page-${index + 1}`,
  imageName: `分页图 ${index + 1}`,
  width: 1600,
  height: 900,
  color: index % 2 ? '#617a8b' : '#756556',
}))

async function installApiMocks(page) {
  const squareRequests = []
  let missingAttempts = 0

  await page.addInitScript(() => sessionStorage.removeItem('picture-square-query'))
  await page.route(url => url.pathname.startsWith('/api/'), route => {
    const requestUrl = new URL(route.request().url())
    if (requestUrl.pathname !== '/api/image/square') return route.fulfill({ json: API_RESULT([]) })
    const keyword = (requestUrl.searchParams.get('keyword') || '').trim()
    const pageNumber = Number(requestUrl.searchParams.get('page') || '1')
    squareRequests.push({ keyword, page: pageNumber })

    if (keyword === 'server-error') {
      return route.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ code: 500, message: 'mock failure' }) })
    }
    if (keyword === 'empty-result') return route.fulfill({ json: API_RESULT({ records: [], total: 0 }) })
    if (keyword === 'pagination') {
      const start = (pageNumber - 1) * 50
      return route.fulfill({ json: API_RESULT({ records: pagedImages.slice(start, start + 50), total: pagedImages.length }) })
    }

    const normalizedKeyword = keyword.toLocaleLowerCase()
    const records = normalizedKeyword
      ? galleryImages.filter(item => item.imageName.toLocaleLowerCase().includes(normalizedKeyword))
      : galleryImages
    return route.fulfill({ json: API_RESULT({ records, total: records.length }) })
  })
  await page.route(url => (
    url.hostname === 'e2e.image-space.test' && url.pathname === '/original/slow-error.svg'
  ), async route => {
    await new Promise(resolve => setTimeout(resolve, 400))
    await route.fulfill({ status: 404, contentType: 'image/svg+xml', body: '' })
  })
  await page.route(url => (
    url.hostname === 'e2e.image-space.test' && url.pathname === '/original/missing.svg'
  ), route => {
    missingAttempts += 1
    if (missingAttempts === 1) return route.fulfill({ status: 404, contentType: 'image/svg+xml', body: '' })
    return route.fulfill({ status: 200, contentType: 'image/svg+xml', body: decodeURIComponent(svg('重试成功').split(',')[1]) })
  })

  return squareRequests
}

async function openViewer(page, imageName) {
  await page.getByRole('button', { name: `查看图片：${imageName}` }).click()
  const drawer = page.locator('.image-drawer')
  await expect(drawer).toBeVisible()
  const trigger = drawer.getByRole('button', { name: '沉浸查看', exact: true })
  await trigger.click()
  const viewer = page.locator('.viewer-overlay')
  await expect(viewer).toBeVisible()
  return { viewer, trigger }
}

test.describe('公开广场与查看器回归门禁', () => {
  test.beforeEach(async ({ page }) => {
    await installApiMocks(page)
    await page.goto('/square')
    await expect(page.getByRole('button', { name: '查看图片：横向静景' })).toBeVisible()
  })

  test('混合比例图片、模糊名称搜索和文档流分页在各断点可用', async ({ page }) => {
    const search = page.getByPlaceholder('搜索图片名称')
    await expect(page.getByRole('button', { name: '查看图片：竖向建筑' })).toBeVisible()
    await expect(page.getByRole('button', { name: '查看图片：方形构图' })).toBeVisible()
    await expect(page.getByRole('button', { name: '查看图片：超宽画幅' })).toBeVisible()
    await expect(page.getByRole('button', { name: '查看图片：camera-original.png' })).toBeVisible()

    await search.fill(' photo ')
    await search.press('Enter')
    await expect(page.getByRole('button', { name: '查看图片：MyPhoto.JPG' })).toBeVisible()
    await expect(page.locator('.pagination-wrap')).toHaveCSS('position', 'static')
    await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)

    await search.fill('')
    await search.press('Enter')
    await expect(page.getByRole('button', { name: '查看图片：横向静景' })).toBeVisible()
  })

  test('阶段三列关系、侧栏宽度和控件基线在各断点稳定', async ({ page }, testInfo) => {
    const metrics = await page.evaluate(() => {
      const layout = document.querySelector('.square-layout')
      const aside = document.querySelector('.square-side')
      const input = document.querySelector('.square-command .el-input__wrapper')
      const select = document.querySelector('.square-command .el-select__wrapper')
      const sortButton = document.querySelector('.sort-segment button')
      const rect = element => element?.getBoundingClientRect()
      return {
        layoutColumns: layout ? getComputedStyle(layout).gridTemplateColumns : '',
        layoutGap: layout ? getComputedStyle(layout).columnGap : '',
        asideWidth: rect(aside)?.width || 0,
        inputHeight: rect(input)?.height || 0,
        selectHeight: rect(select)?.height || 0,
        sortHeight: rect(sortButton)?.height || 0,
        overflowFree: document.documentElement.scrollWidth <= window.innerWidth,
      }
    })
    const width = testInfo.project.use.viewport.width
    const compact = width <= 820

    expect(metrics.overflowFree).toBe(true)
    expect(metrics.inputHeight).toBeGreaterThanOrEqual(compact ? 44 : 40)
    expect(metrics.selectHeight).toBeGreaterThanOrEqual(compact ? 44 : 40)
    expect(metrics.sortHeight).toBeGreaterThanOrEqual(compact ? 44 : 40)

    if (width > 1100) {
      expect(metrics.asideWidth).toBeGreaterThanOrEqual(319)
      expect(metrics.asideWidth).toBeLessThanOrEqual(321)
      expect(metrics.layoutGap).toBe('32px')
    } else {
      expect(metrics.layoutColumns.split(' ').length).toBe(1)
    }
  })

  test('空筛选结果提供清除入口，请求失败提供重试入口', async ({ page }) => {
    const search = page.getByPlaceholder('搜索图片名称')
    await search.fill('empty-result')
    await search.press('Enter')
    await expect(page.getByText('没有符合条件的公开图片')).toBeVisible()
    await page.getByRole('button', { name: '清除筛选' }).click()
    await expect(page.getByRole('button', { name: '查看图片：横向静景' })).toBeVisible()

    await search.fill('server-error')
    await search.press('Enter')
    await expect(page.getByText('公开图片请求失败')).toBeVisible()
    await expect(page.getByRole('button', { name: '重新加载' })).toBeVisible()
  })

  test('分页请求保持在文档流，并切换到下一页', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'desktop', '分页交互在桌面项目覆盖一次即可。')
    const search = page.getByPlaceholder('搜索图片名称')
    await search.fill('pagination')
    await search.press('Enter')
    await expect(page.getByRole('button', { name: '查看图片：分页图 1', exact: true })).toBeVisible()
    await page.locator('.pagination-wrap .btn-next').click()
    await expect(page.getByRole('button', { name: '查看图片：分页图 51', exact: true })).toBeVisible()
  })

  test('查看器支持 Esc、方向键、背景隔离和焦点恢复', async ({ page }) => {
    const { viewer, trigger } = await openViewer(page, '横向静景')
    await expect(page.locator('#app')).toHaveAttribute('aria-hidden', 'true')
    await page.keyboard.press('ArrowRight')
    await expect(viewer.locator('.viewer-heading strong')).toHaveText('竖向建筑')
    await expect(viewer.locator('.viewer-announcement')).toContainText('第 2 张，共 9 张，竖向建筑')
    await page.keyboard.press('Escape')
    await expect(viewer).toBeHidden()
    await expect(trigger).toBeFocused()
  })

  test('旧图片错误事件不能覆盖切换后的当前图片状态', async ({ page }) => {
    const { viewer } = await openViewer(page, '竞态错误图')
    const staleImage = await viewer.locator('.viewer-img').elementHandle()
    await page.keyboard.press('ArrowRight')
    await expect(viewer.locator('.viewer-heading strong')).toHaveText('竞态成功图')
    await staleImage.evaluate(element => element.dispatchEvent(new Event('error')))
    await expect(viewer.locator('.viewer-error')).toHaveCount(0)
    await expect(viewer.locator('.viewer-img')).toBeVisible()
  })

  test('原图 404 显示重试，重试后恢复查看', async ({ page }) => {
    const { viewer } = await openViewer(page, '原图失败可重试')
    await expect(viewer.getByRole('alert')).toContainText('原图加载失败')
    await viewer.getByRole('button', { name: '重新加载' }).click()
    await expect(viewer.getByRole('alert')).toBeHidden()
    await expect(viewer.locator('.viewer-img')).toBeVisible()
  })

  test('移动端滑动切图且 reduced-motion 关闭查看器过渡', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'mobile', '触控手势在移动项目覆盖。')
    await page.emulateMedia({ reducedMotion: 'reduce' })
    const { viewer } = await openViewer(page, '横向静景')
    const stage = viewer.locator('.viewer-stage')
    const box = await stage.boundingBox()
    await page.mouse.move(box.x + box.width * 0.75, box.y + box.height * 0.5)
    await page.mouse.down()
    await page.mouse.move(box.x + box.width * 0.2, box.y + box.height * 0.5, { steps: 4 })
    await page.mouse.up()
    await expect(viewer.locator('.viewer-heading strong')).toHaveText('竖向建筑')
    await expect(viewer.locator('.viewer-img')).toHaveCSS('transition-property', 'none')
  })

  test('全屏按钮在可用浏览器中同步状态', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'desktop', '全屏状态在桌面项目覆盖。')
    test.skip(!(await page.evaluate(() => document.fullscreenEnabled)), '当前浏览器不支持 Fullscreen API。')
    const { viewer } = await openViewer(page, '横向静景')
    await viewer.getByRole('button', { name: '进入全屏' }).click()
    await expect(viewer.getByRole('button', { name: '退出全屏' })).toHaveAttribute('aria-pressed', 'true')
    await page.evaluate(() => document.exitFullscreen())
    await expect(viewer.getByRole('button', { name: '进入全屏' })).toHaveAttribute('aria-pressed', 'false')
  })
})
