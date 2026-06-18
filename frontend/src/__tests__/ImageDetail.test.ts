import { describe, it, expect, beforeEach, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { nextTick, reactive } from 'vue'

import ImageDetail from '../views/ImageDetail.vue'
import { getImageDetail, updateImage } from '../api/image'
import { getComments } from '../api/comment'

const routeState = reactive({
  params: { uuid: '400a1e49-6990-489e-b4a8-35eb0a02d056' },
  query: {},
})

const userStore = reactive({
  token: 'token',
  userInfo: { id: 99, uuid: 'viewer-uuid', role: 'admin' },
})
const pollingMocks = vi.hoisted(() => ({
  options: [] as any[],
  useResourcePolling: vi.fn((options: any) => {
    pollingMocks.options.push(options)
    return {
      isPolling: { value: false },
      isChecking: { value: false },
      start: vi.fn(),
      stop: vi.fn(),
      checkNow: vi.fn(),
    }
  }),
}))

vi.mock('vue-router', () => ({
  createRouter: vi.fn(() => ({
    beforeEach: vi.fn(),
    push: vi.fn(),
    currentRoute: { value: { path: '/image/400a1e49-6990-489e-b4a8-35eb0a02d056' } },
  })),
  createWebHistory: vi.fn(),
  useRoute: () => routeState,
}))

vi.mock('../store/user', () => ({
  useUserStore: () => userStore,
}))

vi.mock('../api/image', () => ({
  getImageDetail: vi.fn(),
  likeImage: vi.fn(),
  unlikeImage: vi.fn(),
  updateImage: vi.fn(),
  downloadImage: vi.fn((uuid: string) => `/api/image/download/${uuid}`),
  downloadImageAs: vi.fn((uuid: string, format: string) => `/api/image/download/${uuid}?format=${format}`),
}))

vi.mock('../api/resource', () => ({
  getImageResourceStatus: vi.fn(),
  refreshImageAccessUrl: vi.fn(),
}))

vi.mock('../api/comment', () => ({
  getComments: vi.fn(),
  addComment: vi.fn(),
  deleteComment: vi.fn(),
  uploadCommentImage: vi.fn(),
  likeComment: vi.fn(),
  unlikeComment: vi.fn(),
}))

vi.mock('../api/category', () => ({
  getCategoryList: vi.fn(),
  createCategory: vi.fn(),
}))

vi.mock('../composables/useResourcePolling', () => ({
  POLLING_INTERVALS: {
    processing: 4000,
    detail: 15000,
    review: 8000,
  },
  useResourcePolling: pollingMocks.useResourcePolling,
}))

function imageDetail(overrides = {}) {
  return {
    id: 7,
    uuid: '400a1e49-6990-489e-b4a8-35eb0a02d056',
    userId: 42,
    userUuid: 'owner-uuid',
    username: 'owner',
    imageName: 'owner-image.png',
    imageType: 'PNG',
    fileSize: 1024,
    visibility: 'PUBLIC',
    uploadTime: '2026-06-01T12:00:00',
    imageUrl: '/api/image/download/400a1e49-6990-489e-b4a8-35eb0a02d056',
    likedByMe: false,
    likeCount: 0,
    editableByMe: true,
    ownedByMe: false,
    ...overrides,
  }
}

function mountDetail() {
  return mount(ImageDetail, {
    global: {
      mocks: {
        $router: { back: vi.fn() },
      },
      directives: {
        loading: {},
      },
      stubs: {
        NavBar: { template: '<nav />' },
        ImageViewer: { template: '<div />', methods: { open: vi.fn() } },
        TagInput: { template: '<input />' },
        'router-link': { props: ['to'], template: '<a><slot /></a>' },
        'el-button': { props: ['loading'], template: '<button :class="$attrs.class"><slot /></button>' },
        'el-icon': { template: '<span><slot /></span>' },
        'el-tag': { template: '<span><slot /></span>' },
        'el-dropdown': { template: '<div :class="$attrs.class"><slot /><slot name="dropdown" /></div>' },
        'el-dropdown-menu': { template: '<div><slot /></div>' },
        'el-dropdown-item': { template: '<button><slot /></button>' },
        'el-dialog': { props: ['modelValue'], template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>' },
        'el-form': { template: '<form><slot /></form>' },
        'el-form-item': { template: '<div><slot /></div>' },
        'el-input': { template: '<input />' },
        'el-select': { template: '<select><slot /></select>' },
        'el-option': { template: '<option />' },
        'el-popover': { template: '<div><slot name="reference" /><slot /></div>' },
        'el-upload': { template: '<div><slot /></div>' },
        transition: false,
      },
    },
  })
}

describe('ImageDetail edit entry', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routeState.params.uuid = '400a1e49-6990-489e-b4a8-35eb0a02d056'
    routeState.query = {}
    userStore.token = 'token'
    userStore.userInfo = { id: 99, uuid: 'viewer-uuid', role: 'admin' }
    vi.mocked(getComments).mockResolvedValue({ data: [] })
    pollingMocks.options.length = 0
    pollingMocks.useResourcePolling.mockClear()
  })

  it('hides the edit menu for another user image even when editableByMe is true', async () => {
    vi.mocked(getImageDetail).mockResolvedValue({ data: imageDetail({ ownedByMe: false, editableByMe: true }) })

    const wrapper = mountDetail()
    await flushPromises()

    expect(wrapper.find('.download-btn').exists()).toBe(true)
    expect(wrapper.find('.more-actions').exists()).toBe(false)
  })

  it('shows the edit menu for the image owner', async () => {
    vi.mocked(getImageDetail).mockResolvedValue({ data: imageDetail({ userId: 99, ownedByMe: true, editableByMe: true }) })

    const wrapper = mountDetail()
    await flushPromises()

    expect(wrapper.find('.more-actions').exists()).toBe(true)
  })

  it('updates the displayed image URL from the resource polling access-url callback', async () => {
    vi.mocked(getImageDetail).mockResolvedValue({ data: imageDetail({
      visibility: 'PRIVATE',
      privateUrl: 'https://cdn.image-space.app/private/images/a.png?auth=old&expires=1893456000&v=1',
    }) })

    const wrapper = mountDetail()
    await flushPromises()

    pollingMocks.options[0].onAccessUrl(
      {
        url: 'https://cdn.image-space.app/private/images/a.png?auth=new&expires=1893456030&v=2',
        expire: 1893456030,
        version: 2,
        visibility: 'PRIVATE',
      },
      { version: 2, visibility: 'PRIVATE', status: 'READY' },
    )
    await nextTick()

    expect(wrapper.find('.detail-image img').attributes('src')).toBe('https://cdn.image-space.app/private/images/a.png?auth=new&expires=1893456030&v=2')
  })

  it('renders medium preview URL before original-compatible imageUrl', async () => {
    vi.mocked(getImageDetail).mockResolvedValue({ data: imageDetail({
      mediumUrl: 'https://cdn.image-space.app/public/images/a/medium.jpg?v=2',
      imageUrl: 'https://cdn.image-space.app/public/images/a/original.png?v=2',
    }) })

    const wrapper = mountDetail()
    await flushPromises()

    expect(wrapper.find('.detail-image img').attributes('src')).toBe('https://cdn.image-space.app/public/images/a/medium.jpg?v=2')
  })

  it('downloads through the original download endpoint instead of the preview URL', async () => {
    vi.mocked(getImageDetail).mockResolvedValue({ data: imageDetail({
      mediumUrl: 'https://cdn.image-space.app/public/images/a/medium.jpg?v=2',
      imageUrl: 'https://cdn.image-space.app/public/images/a/original.png?v=2',
    }) })
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      blob: vi.fn().mockResolvedValue(new Blob(['image-bytes'], { type: 'image/png' })),
    })
    vi.stubGlobal('fetch', fetchMock)
    const originalCreateObjectURL = URL.createObjectURL
    const originalRevokeObjectURL = URL.revokeObjectURL
    Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: vi.fn(() => 'blob:test') })
    Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: vi.fn() })

    try {
      const wrapper = mountDetail()
      await flushPromises()

      await wrapper.find('.download-btn').trigger('click')
      await flushPromises()

      expect(fetchMock).toHaveBeenCalledWith(
        '/api/image/download/400a1e49-6990-489e-b4a8-35eb0a02d056',
        expect.objectContaining({ headers: expect.any(Object) }),
      )
    } finally {
      Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: originalCreateObjectURL })
      Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: originalRevokeObjectURL })
    }
  })

  it('replaces stale preview variant URLs after saving image edits', async () => {
    vi.mocked(getImageDetail).mockResolvedValue({ data: imageDetail({
      ownedByMe: true,
      visibility: 'PUBLIC',
      mediumUrl: 'https://cdn.image-space.app/public/images/a/medium.jpg?v=1',
      imageUrl: 'https://cdn.image-space.app/public/images/a/original.png?v=1',
    }) })
    vi.mocked(updateImage).mockResolvedValue({ data: imageDetail({
      ownedByMe: true,
      visibility: 'PRIVATE',
      mediaVersion: 2,
      mediumUrl: 'https://cdn.image-space.app/private/images/a/medium.jpg?auth=new&expires=1893456030',
      thumbUrl: 'https://cdn.image-space.app/private/images/a/thumb.jpg?auth=new&expires=1893456030',
      imageUrl: 'https://cdn.image-space.app/private/images/a/original.png?auth=new&expires=1893456030',
      privateUrl: 'https://cdn.image-space.app/private/images/a/original.png?auth=new&expires=1893456030',
      publicUrl: null,
    }) })

    const wrapper = mountDetail()
    await flushPromises()

    const editButton = wrapper.findAll('button').find(button => button.text().includes('编辑信息'))
    expect(editButton).toBeTruthy()
    await editButton!.trigger('click')
    await nextTick()

    const saveButton = wrapper.findAll('button').find(button => button.text().includes('保存'))
    expect(saveButton).toBeTruthy()
    await saveButton!.trigger('click')
    await flushPromises()

    expect(wrapper.find('.detail-image img').attributes('src')).toBe('https://cdn.image-space.app/private/images/a/medium.jpg?auth=new&expires=1893456030')
  })

  it('disables the resource display when polling reports deletion', async () => {
    vi.mocked(getImageDetail).mockResolvedValue({ data: imageDetail({ ownedByMe: true }) })

    const wrapper = mountDetail()
    await flushPromises()

    pollingMocks.options[0].onDeleted({ deleted: true })
    await nextTick()

    expect(wrapper.find('.detail-layout').exists()).toBe(false)
    expect(wrapper.text()).toContain('图片已删除或不可用')
  })
})
