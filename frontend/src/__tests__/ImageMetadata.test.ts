import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ImageMetadata from '../components/gallery/ImageMetadata.vue'

describe('ImageMetadata', () => {
  it('renders only technical fields that actually exist', () => {
    const wrapper = mount(ImageMetadata, {
      props: {
        image: {
          width: 1600,
          height: 900,
          originalSize: 2048,
          originalContentType: 'image/png',
          originalFilename: 'long-original-name.png',
          uploadTime: '2026-06-01T12:00:00',
        },
      },
    })

    expect(wrapper.text()).toContain('1600 × 900')
    expect(wrapper.text()).toContain('2.0 KB')
    expect(wrapper.text()).toContain('image/png')
    expect(wrapper.text()).toContain('long-original-name.png')
    expect(wrapper.text()).not.toContain('EXIF')
    expect(wrapper.text()).not.toContain('拍摄时间')
    expect(wrapper.text()).not.toContain('来源')
  })

  it('does not render an empty disclosure when no supported fields exist', () => {
    const wrapper = mount(ImageMetadata, { props: { image: {} } })
    expect(wrapper.find('details').exists()).toBe(false)
  })
})
