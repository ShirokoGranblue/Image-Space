<template>
  <div class="square-intro">
    <header class="square-hero">
      <div class="square-hero-copy">
        <span class="eyebrow">公开广场</span>
        <h1>发现大家分享的图片。</h1>
        <p>按分类、标签或关键词浏览公开作品，也可以进入作者主页查看更多内容。</p>
      </div>
      <div class="square-hero-metrics" aria-label="公开广场图片总数">
        <div class="metric-cell"><strong>{{ total }}</strong><span>公开图片</span></div>
      </div>
    </header>

    <section class="square-command" aria-label="公开广场筛选">
      <el-input :model-value="keyword" placeholder="搜索图片名称" clearable class="square-search" @update:model-value="emit('update:keyword', $event)" @clear="emit('search')" @keyup.enter="emit('search')">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select :model-value="categoryId" clearable placeholder="选择分类" class="square-select" @change="emit('select-category', $event)">
        <el-option v-for="cat in categoryOptions" :key="cat.id" :label="cat.name" :value="cat.id" />
      </el-select>
      <div class="sort-segment" role="group" aria-label="图片排序">
        <button v-for="option in sortOptions" :key="option.value" type="button" :class="{ active: viewMode === option.value }" @click="emit('select-sort', option.value)">{{ option.label }}</button>
      </div>
    </section>

    <div v-if="hasActiveFilters" class="active-filters" aria-label="已启用的筛选条件">
      <span class="active-filters__label">已筛选</span>
      <button v-if="keyword" type="button" @click="emit('clear-keyword')">名称：{{ keyword }} <span aria-hidden="true">×</span><span class="sr-only">清除名称搜索</span></button>
      <button v-if="categoryId" type="button" @click="emit('select-category', null)">分类：{{ activeCategoryLabel }} <span aria-hidden="true">×</span><span class="sr-only">清除分类筛选</span></button>
      <button v-if="activeTag" type="button" @click="emit('clear-tag')">标签：#{{ activeTag }} <span aria-hidden="true">×</span><span class="sr-only">清除标签筛选</span></button>
      <button v-if="viewMode !== 'featured'" type="button" @click="emit('select-sort', 'featured')">排序：{{ activeSortLabel }} <span aria-hidden="true">×</span><span class="sr-only">恢复推荐排序</span></button>
      <button class="active-filters__clear" type="button" @click="emit('clear-all')">全部清除</button>
    </div>
  </div>
</template>

<script setup>
import { Search } from '@element-plus/icons-vue'

defineProps({
  total: { type: Number, default: 0 }, keyword: { type: String, default: '' }, categoryId: { type: [Number, String], default: null },
  categoryOptions: { type: Array, default: () => [] }, viewMode: { type: String, default: 'featured' }, sortOptions: { type: Array, default: () => [] },
  activeTag: { type: String, default: '' }, activeCategoryLabel: { type: String, default: '不限' }, activeSortLabel: { type: String, default: '推荐' },
  hasActiveFilters: { type: Boolean, default: false },
})
const emit = defineEmits(['update:keyword', 'search', 'select-category', 'select-sort', 'clear-keyword', 'clear-tag', 'clear-all'])
</script>

<style scoped>
.square-hero { min-height: 184px; display: grid; grid-template-columns: minmax(0,1fr) 240px; border: 1px solid var(--color-border-subtle); background: var(--color-surface-1); }
.square-hero-copy { padding: var(--space-6) var(--space-7); }
.eyebrow { display: inline-block; margin-bottom: var(--space-4); padding-left: var(--space-3); border-left: 2px solid var(--color-vermilion); color: var(--color-text-secondary); font-size: var(--text-xs); font-weight: 700; letter-spacing: .12em; }
h1 { max-width: 860px; margin: 0; font-family: var(--font-title); font-size: clamp(40px,4.6vw,64px); font-weight: 400; line-height: 1.02; }
.square-hero p { max-width: 680px; margin: var(--space-3) 0 0; color: var(--color-text-secondary); line-height: var(--leading-md); }
.square-hero-metrics { display: grid; border-left: 1px solid var(--color-border-subtle); }
.metric-cell { display: flex; flex-direction: column; justify-content: center; padding: var(--space-5); }
.metric-cell strong { color: var(--color-night); font-family: var(--font-title); font-size: 38px; font-weight: 400; line-height: 1; }
.metric-cell span { margin-top: 7px; color: var(--color-text-muted); font-size: var(--text-xs); font-weight: 700; letter-spacing: .08em; }
.square-command { display: grid; grid-template-columns: minmax(260px,1fr) 220px auto; gap: var(--space-3); padding: var(--space-3); border: 1px solid var(--color-border-subtle); border-top: 0; background: var(--color-surface-1); }
.square-search,.square-select { min-width: 0; }
.square-command :deep(.el-input__wrapper),.square-command :deep(.el-select__wrapper) { min-height: var(--control-height-md); height: var(--control-height-md); }
.sort-segment { display: inline-grid; grid-template-columns: repeat(2,minmax(72px,1fr)); gap: 4px; padding: 4px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-sm); background: var(--color-canvas-muted); }
.sort-segment button { min-height: 40px; padding: 0 var(--space-3); border: 0; border-radius: var(--radius-xs); background: transparent; color: var(--color-text-secondary); cursor: pointer; }
.sort-segment button.active,.sort-segment button:hover { background: var(--color-night); color: var(--color-text-inverse); }
.active-filters { display: flex; align-items: center; flex-wrap: wrap; gap: var(--space-2); margin: var(--space-5) 0; padding-bottom: var(--space-4); border-bottom: 1px solid var(--color-border-subtle); }
.active-filters__label { margin-right: var(--space-1); color: var(--color-text-muted); font-size: var(--text-xs); }
.active-filters button { min-height: var(--control-height-md); padding: 0 var(--space-3); border: 1px solid var(--color-border-subtle); border-radius: var(--radius-round); background: var(--color-surface-1); color: var(--color-text-secondary); cursor: pointer; }
.active-filters .active-filters__clear { border-color: transparent; border-radius: var(--radius-sm); background: transparent; color: var(--color-vermilion); }
@media(max-width:1024px){.square-hero{min-height:0;grid-template-columns:minmax(0,1fr) 180px}.square-hero-copy{padding-block:var(--space-5)}.square-hero-metrics{border-left:1px solid var(--color-border-subtle)}.metric-cell{min-height:0;padding:var(--space-4)}}
@media(max-width:820px){.square-hero-copy{padding:var(--space-5) var(--space-4)}h1{font-size:clamp(34px,10vw,46px)}.square-command{grid-template-columns:1fr}.square-command :deep(.el-input__wrapper),.square-command :deep(.el-select__wrapper){min-height:var(--control-height-lg);height:var(--control-height-lg)}.sort-segment button,.active-filters button{min-height:44px}.active-filters{margin-top:var(--space-4)}}
@media(max-width:700px){.square-hero{grid-template-columns:1fr}.square-hero-metrics{border-top:1px solid var(--color-border-subtle);border-left:0}.metric-cell{min-height:72px}}
@media(max-width:479px){.square-hero p{display:none}.square-hero-copy{padding-block:var(--space-4)}.metric-cell{min-height:58px;padding:var(--space-3) var(--space-4)}.metric-cell strong{font-size:28px}}
</style>
