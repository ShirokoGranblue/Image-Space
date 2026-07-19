<template>
  <section class="comments-section">
    <div class="comments-head">
      <span class="section-label">围绕图片</span>
      <h2>回应与评论</h2>
    </div>

    <div class="comment-input">
      <div class="comment-tools" aria-label="评论附件">
        <el-popover placement="top" :width="340" trigger="click">
          <template #reference>
            <button class="comment-tool-button" type="button" aria-label="插入表情">
              <svg class="comment-tool-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true" focusable="false">
                <circle cx="12" cy="12" r="8.5" />
                <circle cx="9" cy="10" r="0.8" fill="currentColor" stroke="none" />
                <circle cx="15" cy="10" r="0.8" fill="currentColor" stroke="none" />
                <path d="M8.5 14c.9 1.2 2 1.8 3.5 1.8s2.6-.6 3.5-1.8" />
              </svg>
              <span>表情</span>
            </button>
          </template>
          <div class="emoji-grid">
            <button v-for="emoji in emojis" :key="emoji" type="button" :aria-label="`插入表情 ${emoji}`" @click="emit('insert-emoji', emoji)">{{ emoji }}</button>
          </div>
        </el-popover>
        <el-upload :auto-upload="false" :show-file-list="false" :on-change="file => emit('file-change', file)" accept="image/jpeg,image/png,image/webp,image/gif">
          <button class="comment-tool-button" type="button" aria-label="上传评论图片">
            <el-icon><PictureFilled /></el-icon><span>图片</span>
          </button>
        </el-upload>
      </div>

      <div class="comment-compose">
        <el-input :model-value="text" placeholder="写下你的观察或评论..." maxlength="500" @update:model-value="emit('update:text', $event)" @keyup.enter="emit('submit')" />
        <span v-if="fileName" class="upload-hint">已选择：{{ fileName }}</span>
      </div>
      <el-button type="primary" :loading="sending" @click="emit('submit')">发表评论</el-button>
    </div>

    <div v-if="comments.length" class="comment-list">
      <article v-for="comment in comments" :id="'comment-' + comment.id" :key="comment.id" class="comment-item" :class="{ 'notification-highlight': highlightedTarget === 'comment-' + comment.id }">
        <router-link :to="commentProfilePath(comment)" class="comment-avatar-link" :aria-label="`查看${comment.displayName || comment.username || '用户'}的个人主页`">
          <el-avatar :size="44" :src="commentAvatarUrl(comment)" class="comment-avatar">
            <span>{{ commentInitial(comment) }}</span>
          </el-avatar>
        </router-link>
        <div class="comment-main">
          <div class="comment-header">
            <router-link :to="commentProfilePath(comment)">
              <UserIdentity :display-name="comment.displayName" :username="comment.username" :time="formatTime(comment.createTime)" />
            </router-link>
          </div>
          <p v-if="comment.content && !(comment.imageUrl && comment.content.trim() === '📷')">{{ comment.content }}</p>
          <button v-if="comment.displayImageUrl" class="comment-image-button" type="button" :aria-label="`查看评论图片：${comment.displayName || comment.username || '用户'}`" @click="emit('view-image', comment.displayImageUrl)">
            <img :src="comment.displayImageUrl" alt="" class="comment-img" loading="lazy" decoding="async" />
          </button>
          <p v-else-if="comment.imageUrl && comment.imageLoadError" class="comment-image-status" role="status">评论图片加载失败</p>
          <p v-else-if="comment.imageUrl" class="comment-image-status" role="status">评论图片加载中…</p>
          <div class="comment-footer">
            <el-button :class="{ liked: comment.likedByMe }" size="small" text :aria-label="comment.likedByMe ? `取消点赞评论：${comment.displayName || comment.username || '用户'}` : `点赞评论：${comment.displayName || comment.username || '用户'}`" :aria-pressed="String(Boolean(comment.likedByMe))" @click="emit('toggle-like', comment)">
              <el-icon><StarFilled /></el-icon><span v-if="comment.likeCount > 0">{{ comment.likeCount }}</span>
            </el-button>
            <el-dropdown v-if="comment.userId === currentUserId" trigger="click">
              <el-button text size="small" aria-label="评论操作"><el-icon><MoreFilled /></el-icon></el-button>
              <template #dropdown><el-dropdown-menu><el-dropdown-item @click="emit('delete', comment.id)"><el-icon><Delete /></el-icon>删除</el-dropdown-item></el-dropdown-menu></template>
            </el-dropdown>
          </div>
        </div>
      </article>
    </div>
    <div v-else class="comment-empty"><p>还没有评论，可以留下第一条观察。</p></div>
  </section>
</template>

<script setup>
import { Delete, MoreFilled, PictureFilled, StarFilled } from '@element-plus/icons-vue'
import UserIdentity from '../ui/UserIdentity.vue'

defineProps({
  comments: { type: Array, default: () => [] }, text: { type: String, default: '' }, emojis: { type: Array, default: () => [] },
  fileName: { type: String, default: '' }, sending: { type: Boolean, default: false }, currentUserId: { type: [Number, String], default: null },
  highlightedTarget: { type: String, default: '' }, formatTime: { type: Function, required: true },
})
const emit = defineEmits(['update:text', 'insert-emoji', 'file-change', 'submit', 'view-image', 'toggle-like', 'delete'])

function commentProfilePath(comment) {
  return `/profile/${comment.userUuid || comment.userId}`
}

function commentAvatarUrl(comment) {
  if (comment.avatarUrl || comment.avatar) return comment.avatarUrl || comment.avatar
  return comment.userUuid ? `/api/user/avatar/${encodeURIComponent(comment.userUuid)}` : ''
}

function commentInitial(comment) {
  return String(comment.displayName || comment.username || '?').trim().charAt(0).toUpperCase()
}
</script>

<style scoped>
.comments-section { padding: var(--space-6); border: 1px solid var(--color-border-subtle); background: var(--color-surface-1); }
.comments-head h2 { margin: var(--space-1) 0 var(--space-5); font-family: var(--font-title); font-size: var(--text-2xl); font-weight: 600; }
.section-label { color: var(--color-vermilion); font-size: var(--text-xs); font-weight: 600; letter-spacing: .08em; }
.comment-input { display: grid; grid-template-columns: auto minmax(160px, 1fr) auto; align-items: center; gap: var(--space-3); padding: var(--space-3); border: 1px solid var(--color-border-strong); background: var(--color-canvas-muted); }
.comment-tools { display: flex; flex-direction: column; gap: var(--space-2); }
.comment-tool-button { display: inline-flex; min-width: 68px; min-height: 40px; align-items: center; justify-content: flex-start; gap: var(--space-2); padding: 0 var(--space-3); border: 1px solid var(--color-border-subtle); border-radius: var(--radius-sm); background: var(--color-surface-1); color: var(--color-text-primary); cursor: pointer; }
.comment-tool-icon { width: 1em; height: 1em; flex: 0 0 auto; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.7; }
.comment-tool-button:hover { border-color: var(--color-border-strong); background: var(--color-surface-2); }
.comment-compose { display: grid; min-width: 0; gap: var(--space-1); }
.comment-compose :deep(.el-input__wrapper) { min-height: var(--control-height-lg); }
.upload-hint { overflow: hidden; color: var(--color-text-secondary); font-size: var(--text-xs); text-overflow: ellipsis; white-space: nowrap; }
.emoji-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: var(--space-1); }
.emoji-grid button { min-width: 40px; min-height: 40px; border: 0; background: transparent; cursor: pointer; }
.comment-list { display: grid; gap: var(--space-3); margin-top: var(--space-5); }
.comment-item { display: grid; grid-template-columns: 44px minmax(0,1fr); align-items: start; gap: var(--space-3); padding: var(--space-4); border-bottom: 1px solid var(--color-border-subtle); }
.comment-avatar-link { display: block; width: 44px; height: 44px; border-radius: 50%; }
.comment-avatar-link:focus-visible { outline: 2px solid var(--color-urban); outline-offset: 2px; }
.comment-avatar { background: var(--color-night); color: var(--color-text-inverse); font-weight: 600; }
.comment-main { min-width: 0; }
.comment-header,.comment-footer { display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); }
.comment-header { min-height: 24px; }
.comment-header a { min-width: 0; color: var(--color-text-primary); text-decoration: none; }
.comment-header a:hover :deep(.user-identity__name) { text-decoration: underline; }
.comment-header :deep(.user-identity) { width: 100%; font-size: var(--text-sm); }
.comment-header :deep(.user-identity__time) { font-size: var(--text-xs); }
.comment-main p { color: var(--color-text-secondary); line-height: var(--leading-md); overflow-wrap: anywhere; }
.comment-image-button { max-width: 280px; padding: 0; overflow: hidden; border: 1px solid var(--color-border-strong); background: transparent; cursor: zoom-in; }
.comment-image-button img { display: block; max-width: 100%; max-height: 240px; object-fit: contain; }
.comment-image-status { font-size: var(--text-sm); }
.comment-empty { padding: var(--space-7); color: var(--color-text-muted); text-align: center; }
.notification-highlight { outline: 2px solid var(--color-urban); outline-offset: 3px; }
@media(max-width:650px){.comments-section{padding:var(--space-4)}.comment-input{grid-template-columns:auto minmax(0,1fr)}.comment-input>:deep(.el-button){grid-column:1/-1;min-height:44px}.comment-tool-button{min-height:44px}.comment-compose :deep(.el-input__wrapper){min-height:44px}.comment-item{grid-template-columns:40px minmax(0,1fr);gap:var(--space-2);padding-inline:0}.comment-avatar-link,.comment-avatar{width:40px!important;height:40px!important}}
</style>
