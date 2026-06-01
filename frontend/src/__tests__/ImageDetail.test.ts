import { describe, it, expect, beforeEach, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { reactive } from 'vue'

import ImageDetail from '../views/ImageDetail.vue'
import { getImageDetail } from '../api/image'
import { getComments } from '../api/comment'

const routeState = reactive({
  params: { uuid: '400a1e49-6990-489e-b4a8-35eb0a02d056' },
  query: {},
})

const userStore = reactive({
  token: 'token',
  userInfo: { id: 99, uuid: 'viewer-uuid', role: 'admin' },
})

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
})
