<template>
  <div class="tag-input" :class="{ focused }" @click="focusInput">
    <span
      v-for="(tag, index) in tags"
      :key="`${tag}-${index}`"
      class="tag-chip"
      :class="{ editing: editingIndex === index }"
      @click.stop="startEdit(index)"
    >
      <input
        v-if="editingIndex === index"
        ref="editInputRef"
        v-model="editDraft"
        class="tag-edit-field"
        type="text"
        @click.stop
        @blur="commitEdit"
        @keydown.enter.prevent="commitEdit"
        @keydown.esc.prevent="cancelEdit"
      />
      <template v-else>
        <span class="tag-text">{{ tag }}</span>
        <button class="tag-remove" type="button" :aria-label="`remove tag ${tag}`" @click.stop="removeTag(index)">x</button>
      </template>
    </span>
    <input
      ref="inputRef"
      v-model="draft"
      class="tag-field"
      :class="{ 'draft-active': draft.trim() }"
      type="text"
      :placeholder="tags.length || draft ? '' : placeholder"
      @focus="focused = true"
      @blur="handleBlur"
      @input="handleInput"
      @keydown.enter.prevent="commitDraft"
      @keydown.backspace="handleBackspace"
    />
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: 'Use # to separate tags' },
})

const emit = defineEmits(['update:modelValue'])

const DELIMITER = '#'

const inputRef = ref(null)
const editInputRef = ref(null)
const focused = ref(false)
const draft = ref('')
const editDraft = ref('')
const editingIndex = ref(null)
const tags = ref(parseTags(props.modelValue))

watch(() => props.modelValue, value => {
  const nextTags = parseTags(value)
  if (nextTags.join(DELIMITER) !== tags.value.join(DELIMITER)) {
    tags.value = nextTags
  }
})

function parseTags(value) {
  return normalizeTags(String(value || '').split(DELIMITER))
}

function normalizeTags(values) {
  return values
    .map(tag => tag.trim())
    .filter(Boolean)
}

function emitTags() {
  emit('update:modelValue', tags.value.join(DELIMITER))
}

function addTags(values) {
  const next = normalizeTags(values)
  if (next.length === 0) return
  tags.value = [...tags.value, ...next]
  emitTags()
}

function handleInput() {
  if (!draft.value.includes(DELIMITER)) return
  const parts = draft.value.split(DELIMITER)
  addTags(parts.slice(0, -1))
  draft.value = parts.at(-1)?.trimStart() || ''
}

function commitDraft() {
  addTags([draft.value])
  draft.value = ''
}

function handleBlur() {
  if (editingIndex.value === null) {
    commitDraft()
  }
  focused.value = false
}

function handleBackspace() {
  if (draft.value || tags.value.length === 0) return
  tags.value = tags.value.slice(0, -1)
  emitTags()
}

function startEdit(index) {
  editingIndex.value = index
  editDraft.value = tags.value[index]
  focused.value = true
  nextTick(() => {
    const editInput = Array.isArray(editInputRef.value) ? editInputRef.value[0] : editInputRef.value
    editInput?.focus()
  })
}

function commitEdit() {
  if (editingIndex.value === null) return
  const index = editingIndex.value
  const replacementTags = normalizeTags(editDraft.value.split(DELIMITER))
  if (replacementTags.length === 0) {
    tags.value = tags.value.filter((_, itemIndex) => itemIndex !== index)
  } else {
    tags.value = [
      ...tags.value.slice(0, index),
      ...replacementTags,
      ...tags.value.slice(index + 1),
    ]
  }
  editingIndex.value = null
  editDraft.value = ''
  emitTags()
}

function cancelEdit() {
  editingIndex.value = null
  editDraft.value = ''
}

function removeTag(index) {
  tags.value = tags.value.filter((_, itemIndex) => itemIndex !== index)
  emitTags()
  focusInput()
}

function focusInput() {
  if (editingIndex.value !== null) return
  inputRef.value?.focus()
}
</script>

<style scoped>
.tag-input {
  width: 100%;
  min-height: 38px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  padding: 5px 9px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  background: var(--bg-elevated);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
  cursor: text;
}

.tag-input:hover {
  border-color: var(--border-visible);
}

.tag-input.focused {
  border-color: var(--accent);
  box-shadow: var(--shadow-focus);
  background: #fff;
}

.tag-chip,
.tag-field.draft-active {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  min-height: 26px;
  padding: 3px 8px;
  border-radius: 8px;
  background: rgba(37, 99, 235, 0.12);
  color: var(--accent-dim);
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
}

.tag-chip {
  gap: 5px;
  cursor: pointer;
}

.tag-chip.editing {
  padding: 2px 7px;
}

.tag-text {
  overflow-wrap: anywhere;
}

.tag-remove {
  width: 16px;
  height: 16px;
  border: 0;
  border-radius: 50%;
  background: rgba(37, 99, 235, 0.14);
  color: var(--accent-dim);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 13px;
  line-height: 1;
  padding: 0;
}

.tag-remove:hover {
  background: var(--accent);
  color: #fff;
}

.tag-field,
.tag-edit-field {
  min-width: 120px;
  flex: 1;
  border: 0;
  outline: none;
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  line-height: 26px;
}

.tag-field.draft-active {
  flex: 0 1 auto;
  width: auto;
}

.tag-edit-field {
  min-width: 80px;
  padding: 0;
  color: var(--accent-dim);
  font-weight: 700;
}

.tag-field::placeholder {
  color: var(--text-muted);
}
</style>
