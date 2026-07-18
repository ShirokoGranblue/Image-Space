<template>
  <aside class="square-side" aria-label="公开画廊索引">
    <section class="side-section">
      <div class="side-title"><span>当前条件</span><strong>{{ activeSortLabel }}</strong></div>
      <div class="filter-summary">
        <div><span>分类</span><strong>{{ activeCategoryLabel }}</strong></div>
        <div><span>标签</span><strong>{{ activeTag || '不限' }}</strong></div>
        <div><span>关键词</span><strong>{{ keyword || '未填写' }}</strong></div>
      </div>
    </section>
    <section class="side-section">
      <div class="side-title"><span>本页标签</span><strong>{{ tagOptions.length }}</strong></div>
      <div v-if="tagOptions.length" class="tag-cloud">
        <button v-for="tag in tagOptions" :key="tag" type="button" class="tag-pill" :class="{ active: activeTag === tag }" @click="emit('select-tag', tag)">#{{ tag }}</button>
      </div>
      <p v-else class="side-empty">本页暂无标签。</p>
    </section>
    <section class="side-section">
      <div class="side-title"><span>本页作者</span><strong>{{ activeCreators.length }}</strong></div>
      <div v-if="activeCreators.length" class="creator-list" :class="{ 'is-scrollable': activeCreators.length > 4 }">
        <button v-for="creator in activeCreators" :key="creator.key" type="button" class="creator-row" @click="emit('select-creator', creator.uuidOrId)">
          <span class="creator-avatar" :style="{ '--avatar-color': creator.avatarColor }">{{ creator.initial }}</span>
          <span class="creator-copy"><UserIdentity :display-name="creator.displayName" :username="creator.username" /><small>本页 {{ creator.likeCount }} 次点赞</small></span>
        </button>
      </div>
      <p v-else class="side-empty">暂无作者信息。</p>
    </section>
    <section v-if="featuredImage" class="side-section inspector-section">
      <div class="side-title"><span>本页推荐</span><strong>{{ featuredImage.categoryName || '未分类' }}</strong></div>
      <button class="featured-preview" type="button" :aria-label="`查看推荐图片：${imageAlt(featuredImage)}`" @click="emit('view-image', featuredImage)">
        <img v-if="featuredImageSrc" :src="featuredImageSrc" :alt="imageAlt(featuredImage)" loading="lazy" decoding="async" />
        <span v-else><el-icon><PictureFilled /></el-icon></span>
      </button>
      <h2>{{ imageAlt(featuredImage) }}</h2>
      <p><UserIdentity :display-name="featuredImage.displayName" :username="featuredImage.username" fallback="匿名用户" /></p>
    </section>
  </aside>
</template>

<script setup>
import { PictureFilled } from '@element-plus/icons-vue'
import { getImageAlt } from '../../utils/imageRequests'
import UserIdentity from '../ui/UserIdentity.vue'

defineProps({
  activeSortLabel: { type: String, default: '推荐' }, activeCategoryLabel: { type: String, default: '不限' }, activeTag: { type: String, default: '' }, keyword: { type: String, default: '' },
  tagOptions: { type: Array, default: () => [] }, activeCreators: { type: Array, default: () => [] }, featuredImage: { type: Object, default: null }, featuredImageSrc: { type: String, default: '' },
})
const emit = defineEmits(['select-tag', 'select-creator', 'view-image'])
const imageAlt = getImageAlt
</script>

<style scoped>
.square-side { position: sticky; top: calc(var(--nav-height) + var(--space-4)); align-self: start; width: var(--panel-aside-width); border: 1px solid var(--color-border-subtle); background: rgba(25,28,37,.88); }
.side-section { padding: var(--space-5); border-bottom: 1px solid var(--color-border-subtle); }
.side-section:last-child { border-bottom: 0; }
.side-title { display: flex; justify-content: space-between; gap: var(--space-3); margin-bottom: var(--space-4); color: var(--color-text-muted); font-size: var(--text-xs); letter-spacing: .06em; }
.side-title strong { color: var(--color-text-secondary); }
.side-section:nth-child(2) .side-title strong { color: var(--astral-gold); }
.side-section:nth-child(3) .side-title strong { color: var(--astral-rose); }
.filter-summary { display: grid; gap: var(--space-3); }
.filter-summary div { display: flex; justify-content: space-between; gap: var(--space-3); }
.filter-summary span,.side-empty { color: var(--color-text-muted); font-size: var(--text-sm); }
.filter-summary strong { max-width: 62%; overflow: hidden; color: var(--color-text-primary); font-size: var(--text-sm); text-overflow: ellipsis; white-space: nowrap; }
.tag-cloud { display: flex; flex-wrap: nowrap; gap: var(--space-2); padding-bottom: var(--space-1); overflow-x: auto; overscroll-behavior-inline: contain; scrollbar-color: var(--color-border-strong) transparent; scrollbar-width: thin; }
.tag-pill { flex: 0 0 auto; min-height: 36px; padding: 0 var(--space-3); border: 1px solid var(--color-border-subtle); border-radius: var(--radius-round); background: transparent; color: var(--color-text-secondary); cursor: pointer; }
.tag-pill.active,.tag-pill:hover { border-color: var(--color-vermilion); color: var(--color-vermilion); }
.creator-list { display: grid; gap: var(--space-2); }
.creator-list.is-scrollable { max-height: 224px; padding-right: var(--space-1); overflow-y: auto; overscroll-behavior: contain; scrollbar-color: var(--color-border-strong) transparent; scrollbar-width: thin; }
.tag-cloud::-webkit-scrollbar { height: 6px; }
.creator-list.is-scrollable::-webkit-scrollbar { width: 6px; }
.tag-cloud::-webkit-scrollbar-track,.creator-list.is-scrollable::-webkit-scrollbar-track { background: transparent; }
.tag-cloud::-webkit-scrollbar-thumb,.creator-list.is-scrollable::-webkit-scrollbar-thumb { border-radius: var(--radius-round); background: var(--color-border-strong); }
.creator-row { display: grid; grid-template-columns: 38px 1fr; align-items: center; gap: var(--space-3); width: 100%; min-height: 48px; padding: var(--space-1); border: 0; background: transparent; color: inherit; text-align: left; cursor: pointer; }
.creator-row:hover { background: var(--color-canvas-muted); }
.creator-avatar { display: grid; width: 36px; height: 36px; place-items: center; border-radius: 50%; background: var(--avatar-color, var(--color-urban)); color: var(--color-text-inverse); font-weight: 600; }
.creator-copy { min-width: 0; }
.creator-copy :deep(.user-identity) { width: 100%; font-size: var(--text-sm); }
.creator-row small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.creator-row small { margin-top: 2px; color: var(--color-text-muted); }
.featured-preview { display: grid; width: 100%; aspect-ratio: 4/3; padding: 0; overflow: hidden; place-items: center; border: 0; background: var(--color-canvas-muted); cursor: pointer; }
.featured-preview img { width: 100%; height: 100%; object-fit: contain; }
.featured-preview span { color: var(--color-text-muted); font-size: 30px; }
.inspector-section h2 { margin: var(--space-4) 0 var(--space-1); font-family: var(--font-title); font-size: var(--text-xl); font-weight: 500; overflow-wrap: anywhere; }
.inspector-section p { margin: 0; color: var(--color-text-secondary); }
@media(max-width:1100px){.square-side{position:static;width:auto;display:grid;grid-template-columns:repeat(2,minmax(0,1fr))}.side-section:nth-child(odd){border-right:1px solid var(--color-border-subtle)}}
@media(max-width:700px){.square-side{grid-template-columns:1fr}.side-section:nth-child(odd){border-right:0}.creator-row,.tag-pill{min-height:44px}}
</style>
