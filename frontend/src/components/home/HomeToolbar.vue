<template>
  <div class="home-toolbar">
    <header class="main-head"><div class="headline-block"><h1>在一个页面整理、筛选和分享图片。</h1><p>上传新图片，按分类和可见范围快速筛选，也可以批量选择、编辑信息或复制分享链接。</p></div><div class="stats" aria-label="图片统计"><div class="stat"><strong>{{ total }}</strong><span>全部图片</span></div><div class="stat"><strong>{{ categoryCount }}</strong><span>分类</span></div><div class="stat"><strong>{{ selectedCount }}</strong><span>已选择</span></div></div></header>
    <section class="command-panel" aria-label="搜索和筛选">
      <el-input :model-value="keyword" placeholder="按图片名称搜索" clearable class="command-search" @update:model-value="emit('update:keyword',$event)" @clear="emit('filter')" @keyup.enter="emit('filter')"><template #prefix><el-icon><Search /></el-icon></template></el-input>
      <el-select :model-value="categoryId" placeholder="选择分类" clearable class="command-select" @change="value => { emit('update:categoryId',value); emit('filter') }"><el-option v-for="cat in categories" :key="cat.id" :label="cat.categoryName" :value="cat.id" /></el-select>
      <el-select :model-value="sortField" class="command-select" @change="value => { emit('update:sortField',value); emit('filter') }"><el-option label="最新上传" value="upload_time" /><el-option label="名称" value="image_name" /><el-option label="文件大小" value="file_size" /></el-select>
      <button class="select-all" type="button" :class="{active:allVisibleSelected}" @click="emit('toggle-select-all',!allVisibleSelected)">{{ allVisibleSelected?'取消本页':'选择本页' }}</button>
      <button class="primary-command" type="button" @click="emit('upload')"><el-icon><Plus /></el-icon>上传</button>
    </section>
    <section class="filter-strip" aria-label="快速筛选"><button v-for="option in visibilityOptions" :key="option.value" class="filter-chip" :class="{active:visibility===option.value}" type="button" @click="emit('select-visibility',option.value)">{{ option.label }}</button><button v-if="selectedCount>0" class="filter-chip danger" type="button" @click="emit('batch-delete')">删除 {{ selectedCount }}</button></section>
  </div>
</template>
<script setup>
import { Plus, Search } from '@element-plus/icons-vue'
defineProps({total:{type:Number,default:0},categoryCount:{type:Number,default:0},selectedCount:{type:Number,default:0},categories:{type:Array,default:()=>[]},keyword:{type:String,default:''},categoryId:{type:[Number,String],default:null},sortField:{type:String,default:'upload_time'},visibility:{type:String,default:''},allVisibleSelected:{type:Boolean,default:false}})
const visibilityOptions=[{label:'全部',value:''},{label:'公开',value:'PUBLIC'},{label:'指定用户',value:'SPECIFIED'},{label:'仅自己',value:'PRIVATE'}]
const emit=defineEmits(['update:keyword','update:categoryId','update:sortField','filter','toggle-select-all','upload','select-visibility','batch-delete'])
</script>
<style scoped>
.main-head{display:flex;justify-content:space-between;gap:var(--space-6);padding:var(--space-6);border:1px solid var(--color-border-subtle);background:var(--color-surface-1)}
h1{max-width:720px;margin:0;font-family:var(--font-title);font-size:clamp(34px,4vw,54px);font-weight:400;line-height:1.08}p{margin:var(--space-3) 0 0;color:var(--color-text-secondary);line-height:var(--leading-md)}
.stats{display:flex;align-items:stretch}.stat{display:flex;min-width:88px;flex-direction:column;justify-content:center;padding:0 var(--space-4);border-left:1px solid var(--color-border-subtle)}.stat strong{font-family:var(--font-title);font-size:var(--text-2xl);font-weight:500}.stat span{color:var(--color-text-muted);font-size:var(--text-xs)}
.command-panel{display:grid;grid-template-columns:minmax(220px,1fr) 180px 150px auto auto;gap:var(--space-2);padding:var(--space-3);border:1px solid var(--color-border-subtle);border-top:0;background:var(--color-surface-1)}
.command-panel :deep(.el-input__wrapper),.command-panel :deep(.el-select__wrapper){min-height:var(--control-height-md);height:var(--control-height-md);border:1px solid var(--color-border-subtle)!important;border-radius:var(--radius-sm)!important;background:var(--color-surface-1)!important;box-shadow:none!important}
.select-all,.primary-command,.filter-chip{min-height:var(--control-height-md);padding:0 var(--space-3);border:1px solid var(--color-border-subtle);border-radius:var(--radius-sm);background:var(--color-surface-1);color:var(--color-text-primary);cursor:pointer}.select-all.active,.filter-chip.active{border-color:var(--color-night);background:var(--color-night);color:var(--color-text-inverse)}.primary-command{display:inline-flex;align-items:center;justify-content:center;gap:var(--space-2);border-color:var(--color-vermilion);background:var(--color-vermilion);color:var(--color-text-inverse)}
.filter-strip{display:flex;flex-wrap:wrap;gap:var(--space-2);padding:var(--space-4) 0}.filter-chip.danger{border-color:var(--color-error);color:var(--color-error)}
@media(max-width:1050px){.main-head{flex-direction:column}.stats{align-self:stretch}.stat{flex:1}.command-panel{grid-template-columns:repeat(2,minmax(0,1fr))}}
@media(max-width:650px){.main-head{padding:var(--space-5) var(--space-4)}.stats{overflow:auto}.stat{min-width:90px}.command-panel{grid-template-columns:1fr}.command-panel :deep(.el-input__wrapper),.command-panel :deep(.el-select__wrapper){min-height:var(--control-height-lg);height:var(--control-height-lg)}.select-all,.primary-command,.filter-chip{min-height:44px}}
</style>
