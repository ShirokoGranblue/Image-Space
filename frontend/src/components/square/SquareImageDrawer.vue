<template>
  <Teleport to="body">
    <transition name="drawer-slide">
      <div v-if="visible && image" class="drawer-backdrop" @pointerdown.self="emit('close')">
        <aside class="image-drawer" aria-label="图片速览" tabindex="-1" @pointerdown.stop @keydown.esc.prevent.stop="emit('close')">
          <button ref="closeButtonRef" class="drawer-close" type="button" aria-label="关闭" @click="emit('close')"><el-icon><Close /></el-icon></button>
          <button v-if="imageSrc" class="drawer-preview-button" type="button" :aria-label="`沉浸查看：${imageAlt}`" @click="emit('view', image)"><img :src="imageSrc" :alt="imageAlt" class="drawer-img" loading="eager" decoding="async" /></button>
          <div v-else class="drawer-placeholder"><el-icon><PictureFilled /></el-icon></div>
          <div class="drawer-body">
            <span class="drawer-kicker">{{ image.categoryName || '未分类' }}</span><h2>{{ imageAlt }}</h2>
            <div class="drawer-meta"><UserIdentity :display-name="image.displayName" :username="image.username" fallback="匿名用户" /><span>{{ image.likeCount || 0 }} 次点赞</span><span>{{ tags.length }} 个标签</span></div>
            <p v-if="image.description" class="drawer-desc">{{ image.description }}</p>
            <div v-if="tags.length" class="drawer-tags"><button v-for="tag in tags" :key="tag" type="button" @click="emit('select-tag', tag)">#{{ tag }}</button></div>
            <div class="drawer-actions"><button class="secondary-command" type="button" @click="emit('view', image)">沉浸查看</button><button class="primary-command compact" type="button" @click="emit('detail', image)">查看详情</button><button class="secondary-command" type="button" @click="emit('like', image)">{{ image.likedByMe ? '取消点赞' : '点赞' }}</button></div>
          </div>
        </aside>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { Close, PictureFilled } from '@element-plus/icons-vue'
import { getImageAlt } from '../../utils/imageRequests'
import UserIdentity from '../ui/UserIdentity.vue'
const props = defineProps({ visible: { type: Boolean, default: false }, image: { type: Object, default: null }, imageSrc: { type: String, default: '' }, tags: { type: Array, default: () => [] } })
const emit = defineEmits(['close', 'view', 'detail', 'like', 'select-tag'])
const imageAlt = computed(() => getImageAlt(props.image))
const closeButtonRef = ref(null)

watch(() => props.visible, async visible => {
  if (!visible) return
  await nextTick()
  closeButtonRef.value?.focus()
}, { immediate: true })
</script>

<style scoped>
.drawer-backdrop { position: fixed; z-index: var(--layer-drawer); inset: 0; background: rgba(14,18,22,.14); }
.image-drawer { position: absolute; top: calc(var(--nav-height) + var(--space-4)); right: var(--space-4); bottom: var(--space-4); width: min(var(--panel-drawer-width),calc(100vw - (2 * var(--space-4)))); max-width: 100%; overflow: auto; border: 1px solid var(--color-border-strong); background: var(--color-surface-1); box-shadow: var(--shadow-float); }
.drawer-close { position: absolute; z-index: 1; top: var(--space-3); right: var(--space-3); display:grid; width:44px; height:44px; place-items:center; border:1px solid var(--color-border-subtle); border-radius:50%; background:var(--color-surface-1); cursor:pointer; }
.drawer-preview-button { display:block; width:100%; padding:0; border:0; background:var(--color-viewer-bg); cursor:zoom-in; }
.drawer-img,.drawer-placeholder { width:100%; height:min(46vh,420px); object-fit:contain; }
.drawer-placeholder { display:grid; place-items:center; color:var(--color-text-muted); background:var(--color-canvas-muted); font-size:40px; }
.drawer-body { padding:var(--space-6); }
.drawer-kicker { color:var(--color-vermilion); font-size:var(--text-xs); font-weight:700; letter-spacing:.08em; }
h2 { margin:var(--space-2) 0 var(--space-3); font-family:var(--font-title); font-size:var(--text-2xl); font-weight:500; overflow-wrap:anywhere; }
.drawer-meta { display:flex; flex-wrap:wrap; gap:var(--space-2) var(--space-4); color:var(--color-text-muted); font-size:var(--text-sm); }
.drawer-desc { color:var(--color-text-secondary); line-height:var(--leading-md); overflow-wrap:anywhere; }
.drawer-tags,.drawer-actions { display:flex; flex-wrap:wrap; gap:var(--space-2); margin-top:var(--space-4); }
.drawer-tags button,.secondary-command,.primary-command { min-height:44px; padding:0 var(--space-4); border:1px solid var(--color-border-subtle); border-radius:var(--radius-sm); background:transparent; color:var(--color-text-primary); cursor:pointer; }
.drawer-tags button { min-height:36px; padding-inline:var(--space-3); border-radius:var(--radius-round); color:var(--color-text-secondary); }
.primary-command { border-color:var(--color-vermilion); background:var(--color-vermilion); color:var(--color-text-inverse); }
.drawer-slide-enter-active,.drawer-slide-leave-active{transition:opacity var(--duration-overlay) var(--ease-standard)}
.drawer-slide-enter-active .image-drawer,.drawer-slide-leave-active .image-drawer{transition:transform var(--duration-overlay) var(--ease-standard)}
.drawer-slide-enter-from,.drawer-slide-leave-to{opacity:0}.drawer-slide-enter-from .image-drawer,.drawer-slide-leave-to .image-drawer{transform:translateX(28px)}
@media(max-width:700px){.image-drawer{top:calc(var(--nav-height) + var(--space-2));right:var(--space-2);bottom:var(--space-2);width:min(var(--panel-drawer-width),calc(100vw - (2 * var(--space-2))));}.drawer-body{padding:var(--space-5)}}
@media(max-width:479px){.image-drawer{top:auto;right:0;bottom:0;left:0;width:auto;max-width:none;max-height:calc(100dvh - var(--nav-height));border-inline:0;border-bottom:0;border-radius:var(--radius-lg) var(--radius-lg) 0 0;}.drawer-img,.drawer-placeholder{height:min(40vh,280px)}.drawer-body{padding:var(--space-4)}}
@media(prefers-reduced-motion:reduce){.drawer-slide-enter-active,.drawer-slide-leave-active{transition:none}}
</style>
