import { beforeEach, describe, expect, it, vi } from 'vitest'
import { readFileSync } from 'node:fs'
import homePage from '../views/Home.vue?raw'
import profilePage from '../views/Profile.vue?raw'
import main from '../main.js?raw'

const globalStyles = readFileSync('src/style.css', 'utf8')

const mocks = vi.hoisted(() => ({ confirm: vi.fn() }))

vi.mock('element-plus', () => ({
  ElMessageBox: { confirm: mocks.confirm },
}))

import { confirmImageDelete } from '../utils/deleteConfirmation'

describe('image deletion confirmation', () => {
  beforeEach(() => {
    mocks.confirm.mockReset()
  })

  it('uses the visible image name and returns true only after confirmation', async () => {
    mocks.confirm.mockResolvedValue('confirm')

    await expect(confirmImageDelete({ imageName: '边缘证书' })).resolves.toBe(true)
    expect(mocks.confirm).toHaveBeenCalledWith(
      '删除后无法恢复。确定删除《边缘证书》吗？',
      '删除图片',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  })

  it('returns false when the user cancels', async () => {
    mocks.confirm.mockRejectedValue(new Error('cancel'))

    await expect(confirmImageDelete({ originalFilename: 'archive.png' })).resolves.toBe(false)
  })

  it('guards both Home and Profile single-image deletion before the API call', () => {
    const homeDelete = homePage.slice(
      homePage.indexOf('async function handleDelete(img)'),
      homePage.indexOf('function updatePolledImage')
    )
    const profileDelete = profilePage.slice(
      profilePage.indexOf('async function handleWorkDelete(image)'),
      profilePage.indexOf('async function copyWorkLink')
    )

    expect(homeDelete.indexOf('confirmImageDelete(img)')).toBeGreaterThan(-1)
    expect(homeDelete.indexOf('confirmImageDelete(img)')).toBeLessThan(homeDelete.indexOf('deleteImage(img.uuid)'))
    expect(profileDelete.indexOf('confirmImageDelete(image)')).toBeGreaterThan(-1)
    expect(profileDelete.indexOf('confirmImageDelete(image)')).toBeLessThan(profileDelete.indexOf('deleteImage(uuid)'))
  })

  it('loads and themes the service-based MessageBox without the full Element Plus stylesheet', () => {
    expect(main).toContain("element-plus/es/components/message-box/style/css")
    expect(main).not.toContain("element-plus/dist/index.css")
    expect(globalStyles).toMatch(/\.el-message-box\s*\{[\s\S]*width:\s*min\(420px,[\s\S]*box-shadow:\s*var\(--shadow-dialog\)/)
    expect(globalStyles).toMatch(/\.el-message-box__btns \.el-button\s*\{[\s\S]*min-width:\s*84px/)
  })
})
