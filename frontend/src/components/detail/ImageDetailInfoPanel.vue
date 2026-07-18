<template>
  <aside class="detail-info">
    <div class="detail-title-block">
      <span class="detail-kicker">图片信息</span>
      <h1>{{ image.imageName }}</h1>
      <div class="uploader-row">
        <span class="meta-label"><el-icon><User /></el-icon> 上传者</span>
        <router-link v-if="image.userUuid || image.userId" :to="`/profile/${image.userUuid || image.userId}`" class="uploader-link">
          <UserIdentity :display-name="image.displayName" :username="image.username" fallback="未知上传者" />
        </router-link>
        <UserIdentity v-else :display-name="image.displayName" :username="image.username" fallback="未知上传者" />
      </div>
      <p class="desc-text" :class="{ muted: !image.description }">{{ image.description || '尚未填写描述' }}</p>
    </div>

    <div class="meta-grid">
      <div class="meta-item">
        <span class="meta-label"><el-icon><FolderOpened /></el-icon> 分类</span>
        <span>{{ image.categoryName || '未分类' }}</span>
      </div>
    </div>

    <ImageMetadata :image="image" />

    <div class="count-strip" aria-label="图片互动数据">
      <div><strong>{{ image.likeCount || 0 }}</strong><span>点赞</span></div>
      <div><strong>{{ commentCount }}</strong><span>评论</span></div>
    </div>

    <div class="tag-panel">
      <span class="section-label">内容标签</span>
      <div v-if="tags.length" class="detail-tag-list">
        <el-tag v-for="tag in tags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
      </div>
      <p v-else class="muted">暂无标签</p>
    </div>

    <div id="like-activity" class="action-bar" :class="{ 'notification-highlight': highlightedTarget === 'like' }">
      <div class="action-group">
        <span class="action-group__label">查看与下载</span>
        <div class="action-group__controls">
          <el-button class="like-button" :class="{ liked: image.likedByMe }" :loading="liking" @click="emit('like')">
            <el-icon><StarFilled /></el-icon>{{ image.likedByMe ? '已点赞' : '点赞' }}
          </el-button>
          <el-dropdown trigger="click" @command="handleDownloadCommand">
            <el-button type="primary" class="download-btn" :loading="downloading" :disabled="image.deleted">
              <el-icon><Download /></el-icon>下载图片<el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="original">原始格式</el-dropdown-item>
                <el-dropdown-item command="jpg">JPG</el-dropdown-item>
                <el-dropdown-item command="png">PNG</el-dropdown-item>
                <el-dropdown-item command="gif">GIF</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <div v-if="canEdit" class="action-group action-group--owner">
        <span class="action-group__label">编辑与管理</span>
        <el-dropdown trigger="click" class="more-actions">
          <el-button circle class="more-trigger" aria-label="更多操作"><el-icon><MoreFilled /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="emit('edit')"><el-icon><Edit /></el-icon>编辑信息</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ArrowDown, Download, Edit, FolderOpened, MoreFilled, StarFilled, User } from '@element-plus/icons-vue'
import ImageMetadata from '../gallery/ImageMetadata.vue'
import UserIdentity from '../ui/UserIdentity.vue'

defineProps({
  image: { type: Object, required: true },
  tags: { type: Array, default: () => [] },
  commentCount: { type: Number, default: 0 },
  liking: { type: Boolean, default: false },
  downloading: { type: Boolean, default: false },
  canEdit: { type: Boolean, default: false },
  highlightedTarget: { type: String, default: '' },
})

const emit = defineEmits(['like', 'download', 'download-format', 'edit'])

function handleDownloadCommand(command) {
  if (command === 'original') {
    emit('download')
    return
  }
  emit('download-format', command)
}
</script>

<style scoped>
.detail-info {
  min-width: 0;
  padding: var(--space-6);
  border: 0;
  background: var(--color-surface-1);
}

.detail-kicker {
  color: var(--color-vermilion);
  font-size: var(--text-sm);
  font-weight: 600;
  letter-spacing: .08em;
}

.section-label,
.action-group__label {
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  font-weight: 600;
  letter-spacing: .06em;
}

h1 {
  margin: var(--space-2) 0 var(--space-3);
  font-family: var(--font-title);
  font-size: clamp(30px, 3vw, 46px);
  font-weight: 500;
  line-height: 1.1;
  overflow-wrap: anywhere;
}

.uploader-row,
.meta-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.uploader-row {
  flex-wrap: wrap;
  padding-block: var(--space-3);
  border-block: 1px solid var(--color-border-subtle);
}

.desc-text {
  margin-top: var(--space-4);
  color: var(--color-text-secondary);
  line-height: var(--leading-md);
  overflow-wrap: anywhere;
}

.muted,
.meta-label {
  color: var(--color-text-muted);
}

.meta-label {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-sm);
}

.uploader-link {
  max-width: 68%;
  overflow: hidden;
  color: var(--color-night);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-grid {
  display: grid;
  gap: var(--space-3);
  padding-block: var(--space-5);
}

.count-strip {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  border-block: 1px solid var(--color-border-subtle);
}

.count-strip div {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-4);
}

.count-strip div + div { border-left: 1px solid var(--color-border-subtle); }
.count-strip strong { font-family: var(--font-title); font-size: var(--text-xl); }
.count-strip span { color: var(--color-text-muted); font-size: var(--text-sm); }

.tag-panel {
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border-subtle);
}

.detail-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-3);
}

.tag-panel p { margin: var(--space-3) 0 0; }

.action-bar {
  display: grid;
  gap: var(--space-4);
  margin-top: var(--space-6);
}

.action-group {
  display: grid;
  gap: var(--space-2);
}

.action-group + .action-group {
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border-subtle);
}

.action-group__controls {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.action-group :deep(.el-button) { min-height: var(--control-height-md); }
.action-group--owner :deep(.el-button) { width: var(--control-height-md); padding: 0; }
.notification-highlight { outline: 2px solid var(--color-urban); outline-offset: 3px; }

@media (max-width: 700px) {
  .detail-info { padding: var(--space-4); }
  .action-group :deep(.el-button) { min-height: var(--control-height-lg); }
  .action-group--owner :deep(.el-button) { width: var(--control-height-lg); }
}
</style>
