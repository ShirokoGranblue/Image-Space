<template>
  <el-dialog :model-value="visible" title="编辑个人背景" width="860px" class="profile-dialog bg-dialog" @update:model-value="emit('update:visible',$event)">
    <div class="background-editor-layout">
      <div class="bg-crop-side">
        <div ref="cropContainer" class="bg-crop-container" @mousedown="emit('drag-start',$event)" @mousemove="emit('drag-move',$event)" @mouseup="emit('drag-end')" @mouseleave="emit('drag-end')">
          <img v-if="previewUrl" :src="previewUrl" class="bg-crop-img" :style="cropImgStyle" draggable="false" />
          <el-icon v-else :size="72" class="bg-placeholder"><PictureFilled /></el-icon>
          <div v-if="previewUrl" class="bg-crop-frame" :style="cropFrameStyle"></div><div v-if="previewUrl" class="bg-crop-grid" :style="cropGridStyle"></div>
          <template v-if="previewUrl"><div v-for="handle in handles" :key="handle.position" class="bg-handle" :class="handle.className" :style="handlePosition(handle.position)" @mousedown.stop="emit('resize-start',$event,handle.position)"></div></template>
        </div>
        <div class="bg-controls"><span>裁剪尺寸</span><el-slider :model-value="cropRatio" :min="0.45" :max="1" :step="0.01" @update:model-value="emit('update:crop-ratio',$event)" @input="emit('slider-change',$event)" /><span>{{ Math.round(cropRatio * 100) }}%</span></div>
        <el-upload :auto-upload="false" :show-file-list="false" :on-change="file => emit('file-change',file)" accept="image/jpeg,image/png,image/webp,image/gif"><el-button type="primary">选择图片</el-button></el-upload><p v-if="fileName" class="upload-hint">{{ fileName }}</p>
      </div>
      <div class="bg-preview-side"><p class="preview-label">预览</p><div class="profile-mini-card"><div class="profile-mini-banner" :style="miniBannerStyle"></div><div class="profile-mini-header"><el-avatar :size="22" :src="avatarUrl"><el-icon :size="10"><UserFilled /></el-icon></el-avatar><div>{{ displayName }}</div></div></div></div>
    </div>
    <template #footer><el-button @click="emit('update:visible',false)">取消</el-button><el-button type="primary" :loading="saving" :disabled="!previewUrl" @click="emit('save')">应用</el-button></template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { PictureFilled, UserFilled } from '@element-plus/icons-vue'
defineProps({ visible:{type:Boolean,default:false}, previewUrl:{type:String,default:''}, cropImgStyle:{type:Object,default:()=>({})}, cropFrameStyle:{type:Object,default:()=>({})}, cropGridStyle:{type:Object,default:()=>({})}, cropRatio:{type:Number,default:1}, fileName:{type:String,default:''}, miniBannerStyle:{type:Object,default:()=>({})}, avatarUrl:{type:String,default:''}, displayName:{type:String,default:''}, saving:{type:Boolean,default:false}, handlePosition:{type:Function,required:true} })
const emit=defineEmits(['update:visible','update:crop-ratio','drag-start','drag-move','drag-end','resize-start','slider-change','file-change','save'])
const cropContainer=ref(null)
defineExpose({ cropContainer })
const handles=[{position:'top',className:'bg-handle-ns'},{position:'bottom',className:'bg-handle-ns'},{position:'left',className:'bg-handle-ew'},{position:'right',className:'bg-handle-ew'},{position:'tl',className:'bg-handle-corner bg-handle-nwse'},{position:'tr',className:'bg-handle-corner bg-handle-nesw'},{position:'bl',className:'bg-handle-corner bg-handle-nesw'},{position:'br',className:'bg-handle-corner bg-handle-nwse'}]
</script>

<style scoped>
.background-editor-layout{display:grid;grid-template-columns:minmax(0,1fr) 240px;gap:var(--space-5)}.bg-crop-container{position:relative;height:360px;overflow:hidden;border:1px solid var(--color-border-subtle);background-color:#26282b;background-image:linear-gradient(45deg,#333 25%,transparent 25%),linear-gradient(-45deg,#333 25%,transparent 25%),linear-gradient(45deg,transparent 75%,#333 75%),linear-gradient(-45deg,transparent 75%,#333 75%);background-position:0 0,0 16px,16px -16px,-16px 0;background-size:32px 32px;cursor:move}.bg-crop-img{position:absolute;max-width:none;user-select:none}.bg-placeholder{position:absolute;inset:0;margin:auto;color:var(--color-text-muted)}.bg-crop-frame,.bg-crop-grid{position:absolute;pointer-events:none}.bg-crop-frame{border:2px solid var(--color-text-inverse);box-shadow:0 0 0 9999px rgba(0,0,0,.52)}.bg-crop-grid{background-image:linear-gradient(to right,transparent 33.1%,rgba(255,255,255,.45) 33.3%,transparent 33.5%,transparent 66.4%,rgba(255,255,255,.45) 66.6%,transparent 66.8%),linear-gradient(to bottom,transparent 33.1%,rgba(255,255,255,.45) 33.3%,transparent 33.5%,transparent 66.4%,rgba(255,255,255,.45) 66.6%,transparent 66.8%)}.bg-handle{position:absolute;z-index:2;width:16px;height:16px;transform:translate(-50%,-50%);border:2px solid var(--color-text-inverse);background:var(--color-vermilion)}.bg-handle-ns{cursor:ns-resize}.bg-handle-ew{cursor:ew-resize}.bg-handle-nwse{cursor:nwse-resize}.bg-handle-nesw{cursor:nesw-resize}.bg-controls{display:grid;grid-template-columns:auto minmax(0,1fr) auto;align-items:center;gap:var(--space-3);margin:var(--space-4) 0;color:var(--color-text-secondary);font-size:var(--text-sm)}.upload-hint{color:var(--color-text-muted);font-size:var(--text-sm);overflow-wrap:anywhere}.preview-label{margin-top:0;color:var(--color-text-muted);font-size:var(--text-xs)}.profile-mini-card{overflow:hidden;border:1px solid var(--color-border-subtle);background:var(--color-surface-1)}.profile-mini-banner{height:112px;background-color:var(--color-canvas-muted);background-size:cover;background-position:center}.profile-mini-header{display:flex;align-items:center;gap:var(--space-2);padding:var(--space-3);color:var(--color-text-primary);font-size:var(--text-sm)}@media(max-width:760px){.background-editor-layout{grid-template-columns:1fr}.bg-crop-container{height:280px}.bg-preview-side{display:none}}
</style>
