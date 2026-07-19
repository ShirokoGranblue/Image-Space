import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ProfileAvatarEditor from '../components/profile/ProfileAvatarEditor.vue'
import { readFileSync } from 'node:fs'

const DialogStub = {
  props: ['modelValue'],
  template: '<div v-if="modelValue" class="dialog-stub"><slot /><slot name="footer" /></div>',
}
const ButtonStub = {
  props: ['disabled', 'loading'],
  template: '<button :disabled="disabled"><slot /></button>',
}
const SliderStub = {
  props: ['modelValue'],
  template: '<input class="slider-stub" type="range" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))" />',
}
const UploadStub = { template: '<div class="upload-stub"><slot /></div>' }
const IconStub = { template: '<i><slot /></i>' }

function mountEditor(props = {}) {
  return mount(ProfileAvatarEditor, {
    props: {
      visible: true,
      sourceUrl: 'data:image/png;base64,source',
      saving: false,
      ...props,
    },
    global: {
      stubs: {
        'el-dialog': DialogStub,
        'el-button': ButtonStub,
        'el-slider': SliderStub,
        'el-upload': UploadStub,
        'el-icon': IconStub,
      },
    },
  })
}

describe('ProfileAvatarEditor', () => {
  it('uses one consistent action for choosing and retrying an image', () => {
    const editor = readFileSync('src/components/profile/ProfileAvatarEditor.vue', 'utf8')

    expect(editor).toContain('选择图片')
    expect(editor).toContain('图片加载失败，请重新选择图片')
    expect(editor).not.toContain('更换图片')
  })

  it('keeps media and business ownership in the page', () => {
    const page = readFileSync('src/views/Profile.vue', 'utf8')
    const editor = readFileSync('src/components/profile/ProfileAvatarEditor.vue', 'utf8')

    expect(page).toContain('<ProfileAvatarEditor')
    expect(page).toContain('uploadAvatar')
    expect(page).toContain('writeCachedAvatarOriginal')
    expect(page).not.toContain('function cropImage')
    expect(editor).toContain('visible: { type: Boolean')
    expect(editor).toContain('sourceUrl: { type: String')
    expect(editor).toContain("emit('submit', {")
    expect(editor).toContain('sourceUrl: previewUrl.value')
    expect(editor).not.toContain('uploadAvatar')
    expect(editor).not.toContain('useUserStore')
    expect(editor).not.toContain('useRouter')
  })

  it('moves the crop viewport and emits a generated PNG blob', async () => {
    class FakeImage {
      naturalWidth = 800
      naturalHeight = 600
      onload: (() => void) | null = null
      onerror: (() => void) | null = null
      _src = ''
      set src(value: string) {
        this._src = value
        queueMicrotask(() => this.onload?.())
      }
    }

    const context = {
      drawImage: vi.fn(),
      imageSmoothingEnabled: false,
      imageSmoothingQuality: 'low',
    }
    const getContext = vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(context as never)
    const toBlob = vi.spyOn(HTMLCanvasElement.prototype, 'toBlob').mockImplementation(((callback) => {
      callback(new Blob(['cropped'], { type: 'image/png' }))
    }) as never)
    vi.stubGlobal('Image', FakeImage)

    try {
      const wrapper = mountEditor()
      await flushPromises()

      const cropFrame = wrapper.get('.crop-frame')
      const before = cropFrame.attributes('style')
      const container = wrapper.get('.crop-container')
      await container.trigger('mousedown', { clientX: 120, clientY: 120 })
      await container.trigger('mousemove', { clientX: 160, clientY: 140 })
      await container.trigger('mouseup')
      expect(cropFrame.attributes('style')).not.toBe(before)

      const confirm = wrapper.findAll('button').find(button => button.text() === '确认')
      expect(confirm).toBeTruthy()
      await confirm!.trigger('click')
      await flushPromises()

      const payload = wrapper.emitted('submit')?.[0]?.[0] as { blob: Blob; filename: string }
      expect(payload.filename).toBe('avatar.png')
      expect(payload.blob).toBeInstanceOf(Blob)
      expect(context.drawImage).toHaveBeenCalled()
    } finally {
      getContext.mockRestore()
      toBlob.mockRestore()
      vi.unstubAllGlobals()
    }
  })

  it('emits close for cancel and prevents submission while saving', async () => {
    const wrapper = mountEditor({ saving: true })
    const cancel = wrapper.findAll('button').find(button => button.text() === '取消')
    const confirm = wrapper.findAll('button').find(button => button.text() === '确认')

    expect(cancel).toBeTruthy()
    expect(confirm?.attributes('disabled')).toBeDefined()
    await cancel!.trigger('click')
    expect(wrapper.emitted('update:visible')).toEqual([[false]])
    expect(wrapper.emitted('submit')).toBeUndefined()
  })
})
