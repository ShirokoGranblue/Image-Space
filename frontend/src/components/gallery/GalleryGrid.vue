<template>
  <section class="gallery-grid-shell" :aria-busy="loading ? 'true' : 'false'">
    <div v-if="loading && items.length === 0" class="gallery-grid gallery-grid--loading" role="status" aria-label="图片加载中">
      <div
        v-for="(ratio, index) in skeletonRatios"
        :key="index"
        class="gallery-skeleton"
        :style="{ '--skeleton-ratio': ratio }"
        aria-hidden="true"
      />
    </div>

    <ErrorState
      v-else-if="error"
      :title="errorTitle"
      :description="errorDescription"
      :retry-label="retryLabel"
      @retry="emit('retry')"
    />

    <EmptyState
      v-else-if="items.length === 0"
      :title="emptyTitle"
      :description="emptyDescription"
    >
      <template v-if="$slots.emptyAction" #action><slot name="emptyAction" /></template>
    </EmptyState>

    <div v-else class="gallery-grid" :data-density="density">
      <slot
        v-for="(item, index) in items"
        :key="item.uuid || item.id || index"
        name="item"
        :item="item"
        :index="index"
        :priority="index < effectiveEagerCount"
      >
        <GalleryItem
          :image="item"
          :variant="variant"
          :open-mode="openMode"
          :fit="fit"
          :priority="index < effectiveEagerCount"
          @view="emit('view', $event)"
          @like="emit('like', $event)"
        />
      </slot>
    </div>

    <LoadingState v-if="loading && items.length > 0" compact label="正在更新图片" />
    <LoadingState v-else-if="loadingMore" compact label="正在加载更多图片" />
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import EmptyState from '../states/EmptyState.vue'
import ErrorState from '../states/ErrorState.vue'
import LoadingState from '../states/LoadingState.vue'
import GalleryItem from './GalleryItem.vue'

const props = defineProps({
  items: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  loadingMore: { type: Boolean, default: false },
  error: { type: Boolean, default: false },
  loadingCount: { type: Number, default: 8 },
  initialEagerCount: { type: Number, default: 4 },
  density: { type: String, default: 'comfortable', validator: value => ['comfortable', 'compact'].includes(value) },
  variant: { type: String, default: 'collection' },
  openMode: { type: String, default: 'route' },
  fit: { type: String, default: 'contain' },
  emptyTitle: { type: String, default: '暂无图片' },
  emptyDescription: { type: String, default: '' },
  errorTitle: { type: String, default: '图片暂时无法显示' },
  errorDescription: { type: String, default: '请检查连接后重新加载。' },
  retryLabel: { type: String, default: '重新加载' },
})

const emit = defineEmits(['retry', 'view', 'like'])
const ratios = ['4 / 3', '3 / 4', '1 / 1', '16 / 10', '4 / 5', '3 / 2']
const skeletonRatios = computed(() => Array.from({ length: Math.max(1, props.loadingCount) }, (_, index) => ratios[index % ratios.length]))
const viewportWidth = ref(typeof window === 'undefined' ? 1440 : window.innerWidth)
const effectiveEagerCount = computed(() => {
  const responsiveLimit = viewportWidth.value < 480 ? 1 : viewportWidth.value < 768 ? 2 : viewportWidth.value < 1024 ? 3 : props.initialEagerCount
  return Math.max(1, Math.min(props.initialEagerCount, responsiveLimit))
})

function updateViewportWidth() {
  viewportWidth.value = window.innerWidth
}

onMounted(() => window.addEventListener('resize', updateViewportWidth, { passive: true }))
onBeforeUnmount(() => window.removeEventListener('resize', updateViewportWidth))
</script>

<style scoped>
.gallery-grid-shell { min-width: 0; }

.gallery-grid {
  --gallery-min-width: 238px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, var(--gallery-min-width)), 1fr));
  gap: var(--space-6) var(--space-4);
  align-items: start;
}

.gallery-grid[data-density='compact'] {
  --gallery-min-width: 206px;
  gap: var(--space-5) var(--space-3);
}

.gallery-grid--loading {
  --gallery-min-width: 238px;
}

.gallery-skeleton {
  position: relative;
  aspect-ratio: var(--skeleton-ratio, 4 / 3);
  overflow: hidden;
  border-block: 1px solid var(--color-border-subtle);
  background: var(--color-canvas-muted);
}

.gallery-skeleton::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(248, 245, 238, 0.7), transparent);
  transform: translateX(-100%);
  animation: grid-shimmer 1.2s var(--ease-standard) infinite;
}

@media (max-width: 767px) {
  .gallery-grid,
  .gallery-grid[data-density='compact'],
  .gallery-grid--loading {
    grid-template-columns: minmax(0, 1fr);
    gap: var(--space-6);
  }
}

@keyframes grid-shimmer {
  to { transform: translateX(100%); }
}

@media (prefers-reduced-motion: reduce) {
  .gallery-skeleton::after { animation: none; }
}
</style>
