<template>
  <div class="detail-page">
    <NavBar />
    <div class="page-container" v-loading="loading">
      <div class="back-bar">
        <el-button text @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
      </div>

      <div class="detail-layout" v-if="image.id">
        <div class="detail-image" @click="viewerRef.open()" @mouseenter="imgHover = true" @mouseleave="imgHover = false">
          <img :src="image.imageUrl || image.imagePath" :alt="image.imageName" :class="{ zoomed: imgHover }" />
          <transition name="fade">
            <div class="img-hover-overlay" v-if="imgHover">
              <el-icon :size="36"><ZoomIn /></el-icon>
            </div>
          </transition>
        </div>

        <div class="detail-info">
          <h2 class="img-title">{{ image.imageName }}</h2>

          <div class="meta-grid">
            <div class="meta-item">
              <span class="meta-label"><el-icon><User /></el-icon> 上传者</span>
              <router-link :to="`/profile/${image.userId}`" class="uploader-link">{{ image.displayName || image.username }}</router-link>
            </div>
            <div class="meta-item">
              <span class="meta-label"><el-icon><FolderOpened /></el-icon> 分类</span>
              <span class="meta-value">
                <el-tag size="small" v-if="image.categoryName">{{ image.categoryName }}</el-tag>
                <span v-else class="meta-placeholder">未分类</span>
              </span>
            </div>
            <div class="meta-item">
              <span class="meta-label"><el-icon><Document /></el-icon> 文件大小</span>
              <span class="meta-value">{{ formatSize(image.fileSize) }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label"><el-icon><PictureRounded /></el-icon> 图片类型</span>
              <span class="meta-value">{{ image.imageType }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label"><el-icon><Clock /></el-icon> 上传日期</span>
              <span class="meta-value">{{ formatTime(image.uploadTime) }}</span>
            </div>
          </div>

          <div class="desc-block" v-if="image.description">
            <p class="desc-text">{{ image.description }}</p>
          </div>
          <div class="desc-block desc-empty" v-else>
            <p class="desc-text">暂无描述</p>
          </div>

          <div class="tags-section" v-if="image.tags">
            <div class="tag-list">
              <el-tag v-for="(tag, i) in tagList" :key="i" :type="tagTypes[i % tagTypes.length]" effect="plain">{{ tag }}</el-tag>
            </div>
          </div>

          <el-button type="primary" class="download-btn" @click="handleDownload" :loading="downloading">
            <el-icon><Download /></el-icon> 下载图片
          </el-button>
        </div>
      </div>

      <div class="comments-section" v-if="image.id">
        <h3>评论区</h3>

        <div class="comment-input">
          <div class="emoji-bar">
            <el-popover placement="top" :width="340" trigger="click">
              <template #reference>
                <el-button circle size="small">😊</el-button>
              </template>
              <div class="emoji-grid">
                <span v-for="e in emojis" :key="e" class="emoji-item" @click="insertEmoji(e)">{{ e }}</span>
              </div>
            </el-popover>
          </div>
          <el-input v-model="commentText" type="textarea" :rows="3" placeholder="写下你的评论..." maxlength="500" show-word-limit />
          <div class="comment-actions">
            <div class="comment-upload">
              <el-upload :auto-upload="false" :show-file-list="false" :on-change="onCmtFileChange" accept="image/*">
                <el-button size="small" circle>
                  <el-icon><PictureFilled /></el-icon>
                </el-button>
              </el-upload>
              <span class="upload-hint" v-if="cmtFile">已选: {{ cmtFile.name }}</span>
            </div>
            <el-button type="primary" @click="handleAddComment" :loading="sending">发表评论</el-button>
          </div>
        </div>

        <div class="comment-list" v-if="comments.length > 0">
          <div class="comment-item" v-for="c in comments" :key="c.id">
            <div class="comment-header">
              <span class="comment-user">{{ c.displayName || c.username }}</span>
              <span class="comment-time">{{ formatTime(c.createTime) }}</span>
            </div>
            <p class="comment-content">{{ c.content }}</p>
            <img v-if="c.imagePath" :src="c.imagePath" class="comment-img" @click="viewCmtImg(c.imagePath)" />
            <el-button v-if="c.userId === currentUserId" text size="small" type="danger" @click="handleDeleteComment(c.id)">删除</el-button>
          </div>
        </div>

        <div class="comment-empty" v-else>
          <p>暂无评论，来说两句吧</p>
        </div>
      </div>
    </div>

    <ImageViewer ref="viewerRef" :src="viewerSrc" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import NavBar from '../components/NavBar.vue'
import ImageViewer from '../components/ImageViewer.vue'
import { getImageDetail } from '../api/image'
import { getComments, addComment, deleteComment, uploadCommentImage } from '../api/comment'
import { useUserStore } from '../store/user'
import { formatSize, formatTime } from '../utils/format'

const route = useRoute()
const userStore = useUserStore()
const viewerRef = ref(null)
const viewerSrc = ref('')
const image = ref({})
const comments = ref([])
const commentText = ref('')
const cmtFile = ref(null)
const imgHover = ref(false)
const loading = ref(false)
const sending = ref(false)
const downloading = ref(false)

const tagTypes = ['', 'success', 'warning', 'danger', 'info']

const emojis = ['😀','😂','🤣','😊','😍','🤩','😎','🥳','😢','😡','👍','👎','❤️','🔥','⭐','🎉','💯','✅','🙏','💪','🤝','👀','💡','📌','🚀','🎨','🐱','🌸','✨','🎵','🍕','☕','💻','📷','🎮','🏆']

const tagList = computed(() => {
  if (!image.value.tags) return []
  return image.value.tags.split(',').map(t => t.trim()).filter(Boolean)
})

const currentUserId = computed(() => userStore.userInfo?.id)

onMounted(async () => {
  loading.value = true
  try {
    const [imgRes, cmtRes] = await Promise.all([
      getImageDetail(route.params.id),
      getComments(route.params.id)
    ])
    image.value = imgRes.data
    comments.value = cmtRes.data || []
    viewerSrc.value = imgRes.data.imageUrl || imgRes.data.imagePath
  } catch {} finally {
    loading.value = false
  }
})

function insertEmoji(emoji) {
  commentText.value += emoji
}

function onCmtFileChange(file) {
  cmtFile.value = file.raw
}

async function handleAddComment() {
  const text = commentText.value.trim()
  if (!text && !cmtFile.value) {
    ElMessage.warning('请输入评论内容')
    return
  }
  sending.value = true
  try {
    let imagePath = null
    if (cmtFile.value) {
      const fd = new FormData()
      fd.append('file', cmtFile.value)
      const res = await uploadCommentImage(fd)
      imagePath = res.data
    }
    await addComment({ imageId: image.value.id, content: text || '📷', imagePath })
    ElMessage.success('评论成功')
    commentText.value = ''
    cmtFile.value = null
    const res = await getComments(image.value.id)
    comments.value = res.data || []
  } catch {} finally {
    sending.value = false
  }
}

async function handleDeleteComment(id) {
  try {
    await deleteComment(id)
    ElMessage.success('已删除')
    comments.value = comments.value.filter(c => c.id !== id)
  } catch {}
}

async function handleDownload() {
  downloading.value = true
  try {
    const token = localStorage.getItem('satoken')
    const headers = token ? { 'satoken': token } : {}
    const response = await fetch(`/api/image/download/${image.value.id}`, { headers })

    if (!response.ok) {
      if (response.status === 401) {
        ElMessage.error('请先登录再下载')
      } else {
        ElMessage.error('下载失败')
      }
      return
    }

    const blob = await response.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = image.value.imageName || 'image'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载失败，请重试')
  } finally {
    downloading.value = false
  }
}

function viewCmtImg(src) {
  viewerSrc.value = src
  viewerRef.value.open()
}


</script>

<style scoped>
.detail-page { min-height: 100vh; background: var(--bg-base); }
.page-container { max-width: 1120px; margin: 0 auto; padding: 28px var(--space-lg) 40px; }
.back-bar { margin-bottom: var(--space-md); }
.back-bar :deep(.el-button) { color: var(--text-muted); font-weight: 500; }
.back-bar :deep(.el-button:hover) { color: var(--accent); }

.detail-layout {
  display: grid; grid-template-columns: minmax(360px, 1fr) 420px; gap: var(--space-xl);
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle); border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-md);
}

.detail-image {
  height: min(58vw, 560px); min-height: 360px; overflow: hidden; border-radius: var(--radius-md);
  background: var(--bg-elevated); display: flex; align-items: center; justify-content: center;
  position: relative; cursor: pointer; border: 1px solid var(--border-subtle);
}
.detail-image img {
  width: 100%; height: 100%; object-fit: contain;
  transition: transform 0.4s var(--ease-out), filter 0.4s ease;
}
.detail-image img.zoomed {
  transform: scale(1.05);
  filter: brightness(0.7);
}

.img-hover-overlay {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  color: #fff; pointer-events: none;
}
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.detail-info {
  flex: 1; min-width: 0;
  display: flex; flex-direction: column;
  padding: 4px 0;
}
.img-title {
  font-family: var(--font-display);
  font-size: 24px; font-weight: 700;
  margin-bottom: 20px;
  word-break: break-word; color: var(--text-primary);
  letter-spacing: -0.3px; line-height: 1.3;
}

.meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 20px;
}
.meta-item {
  display: flex; flex-direction: column; gap: 4px;
}
.meta-label {
  font-size: 11px; font-weight: 600; color: var(--text-muted);
  text-transform: uppercase; letter-spacing: 0.5px;
  display: flex; align-items: center; gap: 4px;
}
.meta-label .el-icon { font-size: 13px; }
.meta-value { font-size: 14px; color: var(--text-primary); font-weight: 500; }
.meta-placeholder { color: var(--text-muted); font-size: 13px; }

.desc-block {
  margin-top: 18px; padding: 14px 16px;
  background: var(--bg-elevated); border-radius: var(--radius-sm);
  border: 1px solid var(--border-subtle);
}
.desc-empty { background: transparent; border-style: dashed; }
.desc-text { font-size: 14px; color: var(--text-secondary); line-height: 1.7; margin: 0; }

.tags-section { margin-top: 14px; }
.tag-list { display: flex; gap: var(--space-sm); flex-wrap: wrap; }

.download-btn {
  margin-top: auto; padding-top: 18px;
  font-weight: 600;
  border-radius: var(--radius-md);
  transition: transform 0.15s var(--ease-out), box-shadow 0.15s var(--ease-out);
}
.download-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
}

.uploader-link { color: var(--accent); text-decoration: none; font-weight: 600; font-size: 14px; }
.uploader-link:hover { color: var(--accent-glow); }

.comments-section {
  margin-top: var(--space-xl); background: var(--bg-surface);
  border: 1px solid var(--border-subtle); border-radius: var(--radius-lg);
  padding: var(--space-xl);
  box-shadow: var(--shadow-md);
}
.comments-section h3 {
  font-family: var(--font-display); font-size: 22px; font-weight: 750;
  margin-bottom: var(--space-md); color: var(--text-primary);
  letter-spacing: -0.2px;
}

.comment-input { margin-bottom: var(--space-lg); }
.emoji-bar { margin-bottom: var(--space-xs); }
.emoji-grid { display: flex; flex-wrap: wrap; gap: 4px; max-height: 200px; overflow-y: auto; }
.emoji-item {
  cursor: pointer; font-size: 22px; padding: 4px; border-radius: 4px;
  transition: background 0.12s ease, transform 0.12s ease;
}
.emoji-item:hover { background: var(--bg-hover); transform: scale(1.15); }

.comment-actions {
  display: flex; justify-content: space-between; align-items: center; margin-top: var(--space-sm);
}
.comment-upload { display: flex; align-items: center; gap: var(--space-sm); }
.upload-hint { font-size: 12px; color: var(--text-muted); }

.comment-item {
  padding: var(--space-md) 0; border-bottom: 1px solid var(--border-subtle);
  transition: background 0.15s ease;
}
.comment-item:hover { background: rgba(37, 99, 235, 0.015); }
.comment-header { display: flex; justify-content: space-between; margin-bottom: var(--space-xs); }
.comment-user { font-weight: 600; color: var(--accent); font-size: 14px; }
.comment-time { font-size: 12px; color: var(--text-muted); }
.comment-content { font-size: 14px; color: var(--text-secondary); line-height: 1.7; margin-bottom: 4px; }
.comment-img {
  max-width: 200px; max-height: 150px; border-radius: var(--radius-md); cursor: pointer;
  margin: var(--space-xs) 0; object-fit: cover; border: 1px solid var(--border-subtle);
}
.comment-empty { text-align: center; color: var(--text-muted); padding: var(--space-xl) 0; font-size: 14px; }

@media (max-width: 900px) {
  .detail-layout { grid-template-columns: 1fr; padding: 20px; }
  .detail-image { height: 420px; min-height: 280px; }
  .comments-section { padding: var(--space-md); }
}
</style>
