<template>
  <GalleryItem
    :image="image"
    :show-actions="showActions"
    :selectable="selectable"
    :selected="selected"
    :variant="variant"
    :open-mode="openMode"
    :fit="fit"
    :priority="priority"
    @edit="emit('edit', $event)"
    @delete="emit('delete', $event)"
    @removed="emit('removed', $event)"
    @toggle-select="emit('toggle-select', $event)"
    @view="emit('view', $event)"
    @copy="emit('copy', $event)"
    @like="emit('like', $event)"
  />
</template>

<script setup>
import GalleryItem from './gallery/GalleryItem.vue'

defineProps({
  image: { type: Object, required: true },
  showActions: { type: Boolean, default: false },
  selectable: { type: Boolean, default: false },
  selected: { type: Boolean, default: false },
  variant: { type: String, default: 'collection' },
  openMode: { type: String, default: 'route' },
  fit: { type: String, default: 'contain' },
  priority: { type: Boolean, default: false },
})

const emit = defineEmits(['edit', 'delete', 'removed', 'toggle-select', 'view', 'copy', 'like'])
</script>
