import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

import TagInput from '../components/TagInput.vue'

function mountTagInput(modelValue = '') {
  return mount(TagInput, {
    props: {
      modelValue,
      'onUpdate:modelValue': value => wrapper.setProps({ modelValue: value }),
    },
  })
}

let wrapper

function chipTexts() {
  return wrapper.findAll('.tag-chip').map(chip => chip.text().replace('x', '').trim())
}

describe('TagInput', () => {
  it('renders existing hash-separated tags as chips', () => {
    wrapper = mountTagInput('cat#dog')

    expect(wrapper.findAll('.tag-chip')).toHaveLength(2)
    expect(wrapper.text()).toContain('cat')
    expect(wrapper.text()).toContain('dog')
  })

  it('shows the first typed tag as a chip before delimiter is entered', async () => {
    wrapper = mountTagInput('')
    const input = wrapper.find('input')

    await input.setValue('blue')
    await nextTick()

    expect(input.classes()).toContain('draft-active')
    expect(input.element.value).toBe('blue')
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })

  it('splits multiple typed tags on hash and keeps commas as text', async () => {
    wrapper = mountTagInput('blue')
    const input = wrapper.find('input')

    await input.setValue('cat,dog#avatar#')
    await nextTick()

    expect(chipTexts()).toEqual(['blue', 'cat,dog', 'avatar'])
    expect(input.element.value).toBe('')
    expect(wrapper.text()).toContain(',')
    expect(wrapper.text()).not.toContain('#')
    expect(wrapper.emitted('update:modelValue').at(-1)).toEqual(['blue#cat,dog#avatar'])
  })

  it('edits an existing tag and splits edited content on hash', async () => {
    wrapper = mountTagInput('blue#old')

    await wrapper.findAll('.tag-chip')[1].trigger('click')
    const editInput = wrapper.find('input.tag-edit-field')
    await editInput.setValue('cat,dog#avatar')
    await editInput.trigger('keydown.enter')
    await nextTick()

    expect(chipTexts()).toEqual(['blue', 'cat,dog', 'avatar'])
    expect(wrapper.emitted('update:modelValue').at(-1)).toEqual(['blue#cat,dog#avatar'])
  })
})
