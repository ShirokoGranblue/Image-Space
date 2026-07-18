<template>
  <details v-if="rows.length" class="image-metadata" :open="open">
    <summary>
      <span>
        <small>{{ eyebrow }}</small>
        <strong>{{ title }}</strong>
      </span>
      <span class="image-metadata__count">{{ rows.length }} 项</span>
    </summary>
    <dl>
      <div v-for="row in rows" :key="row.label" class="image-metadata__row">
        <dt>{{ row.label }}</dt>
        <dd>{{ row.value }}</dd>
      </div>
    </dl>
  </details>
</template>

<script setup>
import { computed } from 'vue'
import { formatSize, formatTime } from '../../utils/format'

const props = defineProps({
  image: { type: Object, required: true },
  open: { type: Boolean, default: false },
  eyebrow: { type: String, default: '属性' },
  title: { type: String, default: '文件与图像数据' },
})

const rows = computed(() => {
  const image = props.image || {}
  return [
    { label: '像素尺寸', value: image.width && image.height ? `${image.width} × ${image.height}` : '' },
    { label: '文件大小', value: image.fileSize || image.originalSize ? formatSize(image.fileSize || image.originalSize) : '' },
    { label: '文件类型', value: image.imageType || image.originalContentType || '' },
    { label: '原始文件名', value: image.originalFilename || '' },
    { label: '可见范围', value: visibilityText(image.visibility) },
    { label: '上传时间', value: image.uploadTime ? formatTime(image.uploadTime) : '' },
  ].filter(row => row.value)
})

function visibilityText(value) {
  return {
    PUBLIC: '公开',
    SPECIFIED: '指定用户',
    PRIVATE: '仅自己',
  }[value] || ''
}
</script>

<style scoped>
.image-metadata {
  border-block: 1px solid var(--color-border-subtle);
  color: var(--color-text-primary);
}

.image-metadata summary {
  min-height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-3) 0;
  cursor: pointer;
  list-style: none;
}

.image-metadata summary::-webkit-details-marker { display: none; }
.image-metadata summary:focus-visible { outline: 2px solid var(--color-urban); outline-offset: 4px; }
.image-metadata summary::after { content: '＋'; flex: none; color: var(--color-text-muted); font-size: 18px; }
.image-metadata[open] summary::after { content: '−'; }
.image-metadata summary > span:first-child { display: grid; gap: var(--space-1); }
.image-metadata small { color: var(--color-text-secondary); font-size: var(--text-sm); font-weight: 600; letter-spacing: .06em; }
.image-metadata strong { font: 500 var(--text-md)/var(--leading-md) var(--font-body); }
.image-metadata__count { margin-left: auto; color: var(--color-text-muted); font-size: var(--text-xs); }
.image-metadata dl { margin: 0; padding-bottom: var(--space-3); }
.image-metadata__row { display: grid; grid-template-columns: minmax(100px, .7fr) minmax(0, 1.3fr); gap: var(--space-4); padding: var(--space-3) 0; border-top: 1px solid var(--color-border-subtle); }
.image-metadata dt { color: var(--color-text-muted); font-size: var(--text-xs); }
.image-metadata dd { margin: 0; overflow-wrap: anywhere; color: var(--color-text-secondary); font-size: var(--text-sm); line-height: var(--leading-md); }

@media (max-width: 479px) {
  .image-metadata__row { grid-template-columns: 1fr; gap: var(--space-1); }
}
</style>
