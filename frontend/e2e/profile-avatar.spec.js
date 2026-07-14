import { expect, test } from '@playwright/test'

const API_RESULT = data => ({ code: 200, message: 'success', data })
const avatarSvg = `data:image/svg+xml,${encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300"><rect width="100%" height="100%" fill="#617a8b"/><circle cx="200" cy="150" r="90" fill="#b64a36"/></svg>')}`
const replacementPng = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=', 'base64')
const replacementGif = Buffer.from('R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==', 'base64')

const profile = {
  id: 1,
  uuid: 'e2e-profile-user',
  username: 'e2e-user',
  displayName: '阶段四用户',
  avatarUrl: avatarSvg,
  background: '#31495f',
  backgroundUrl: '#31495f',
  bio: '用于头像编辑器真实浏览器回归。',
  createTime: '2026-06-01T12:00:00',
  email: 'profile@example.com',
}

async function installProfileMocks(page) {
  const requests = { avatarUploads: 0, backgroundUploads: 0 }

  await page.addInitScript(() => sessionStorage.setItem('satoken', 'e2e-profile-token'))
  await page.route(url => url.pathname.startsWith('/api/'), async route => {
    const requestUrl = new URL(route.request().url())

    if (requestUrl.pathname === '/api/user/info' || requestUrl.pathname === '/api/user/profile/e2e-profile-user') {
      return route.fulfill({ json: API_RESULT(profile) })
    }
    if (requestUrl.pathname === '/api/image/list') {
      return route.fulfill({ json: API_RESULT({ records: [], total: 0 }) })
    }
    if (requestUrl.pathname === '/api/category/list') {
      return route.fulfill({ json: API_RESULT([]) })
    }
    if (requestUrl.pathname === '/api/user/avatar' && route.request().method() === 'POST') {
      requests.avatarUploads += 1
      if (requests.avatarUploads === 1) {
        return route.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ code: 500, message: 'mock upload failure' }) })
      }
      return route.fulfill({ json: API_RESULT({ avatar: avatarSvg }) })
    }
    if (requestUrl.pathname === '/api/user/background' && route.request().method() === 'POST') {
      requests.backgroundUploads += 1
      return route.fulfill({ json: API_RESULT({ background: '#4f6474' }) })
    }
    return route.fulfill({ json: API_RESULT(null) })
  })

  return requests
}

test.describe('Profile 头像编辑器阶段四回归门禁', () => {
  test('支持缩放、拖动、取消，并在上传失败后保留可重试状态', async ({ page }) => {
    const requests = await installProfileMocks(page)
    await page.goto('/profile/e2e-profile-user')
    await expect(page.getByRole('heading', { name: '阶段四用户' })).toBeVisible()

    await page.getByRole('button', { name: '编辑头像' }).click()
    const dialog = page.locator('.avatar-dialog')
    await expect(dialog).toBeVisible()
    await expect(dialog.locator('.crop-frame')).toBeVisible()

    const slider = dialog.locator('input[type="range"]')
    if (await slider.count()) await slider.fill('0.5')

    const frame = dialog.locator('.crop-frame')
    const before = await frame.evaluate(element => element.getBoundingClientRect().left)
    const crop = await dialog.locator('.crop-container').boundingBox()
    await page.mouse.move(crop.x + crop.width / 2, crop.y + crop.height / 2)
    await page.mouse.down()
    await page.mouse.move(crop.x + crop.width / 2 + 24, crop.y + crop.height / 2 + 16, { steps: 3 })
    await page.mouse.up()
    await expect.poll(() => frame.evaluate(element => element.getBoundingClientRect().left)).not.toBe(before)

    await dialog.getByRole('button', { name: '取消' }).click()
    await expect(dialog).toBeHidden()

    await page.getByRole('button', { name: '编辑头像' }).click()
    await expect(dialog).toBeVisible()
    await dialog.locator('input[type="file"]').setInputFiles({ name: 'replacement.png', mimeType: 'image/png', buffer: replacementPng })
    await expect(dialog.locator('.upload-hint')).toContainText('replacement.png')

    await dialog.getByRole('button', { name: '确认' }).click()
    await expect.poll(() => requests.avatarUploads).toBe(1)
    await expect(dialog).toBeVisible()
    await expect(dialog.getByRole('button', { name: '确认' })).toBeEnabled()

    await dialog.getByRole('button', { name: '确认' }).click()
    await expect.poll(() => requests.avatarUploads).toBe(2)
    await expect(dialog).toBeHidden()

    const header = page.locator('.profile-header')
    const documentGeometry = () => header.evaluate(element => {
      const rect = element.getBoundingClientRect()
      return { x: rect.x + window.scrollX, y: rect.y + window.scrollY, width: rect.width, height: rect.height }
    })
    const beforeBackgroundUpload = await documentGeometry()
    await page.getByRole('button', { name: '编辑背景' }).click()
    const backgroundDialog = page.locator('.bg-dialog')
    await expect(backgroundDialog).toBeVisible()
    await backgroundDialog.locator('input[type="file"]').setInputFiles({ name: 'background.gif', mimeType: 'image/gif', buffer: replacementGif })
    await expect(backgroundDialog.locator('.upload-hint')).toContainText('background.gif')
    await backgroundDialog.getByRole('button', { name: '应用' }).click()
    await expect.poll(() => requests.backgroundUploads).toBe(1)
    await expect(backgroundDialog).toBeHidden()
    expect(await documentGeometry()).toEqual(beforeBackgroundUpload)
  })
})
