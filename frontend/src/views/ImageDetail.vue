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
          <img :src="detailImageSrc" :alt="image.imageName" :class="{ zoomed: imgHover }" />
          <transition name="fade">
            <div class="img-hover-overlay" v-if="imgHover">
              <el-icon :size="36"><ZoomIn /></el-icon>
            </div>
          </transition>
        </div>

        <div class="detail-info">
          <h2 class="img-title">{{ image.imageName }}</h2>

          <div class="meta-bar">
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
            <div class="meta-item meta-tags">
              <span class="meta-label"><el-icon><CollectionTag /></el-icon> 标签</span>
              <span class="meta-value detail-tag-list" v-if="tagList.length">
                <el-tag v-for="(tag, i) in tagList" :key="i" size="small" :type="tagTypes[i % tagTypes.length]" effect="plain">{{ tag }}</el-tag>
              </span>
              <span v-else class="meta-placeholder">无标签</span>
            </div>
          </div>

          <div class="desc-block" v-if="image.description">
            <p class="desc-text">{{ image.description }}</p>
          </div>
          <div class="desc-block desc-empty" v-else>
            <p class="desc-text">暂无描述</p>
          </div>

          <div id="like-activity" class="action-bar" :class="{ 'notification-highlight': highlightedTarget === 'like' }">
            <el-button :type="image.likedByMe ? 'danger' : 'default'" @click="handleToggleLike" :loading="liking">
              <el-icon><StarFilled /></el-icon>
              {{ image.likedByMe ? '已点赞' : '点赞' }}
            </el-button>
            <span class="like-count-text">{{ image.likeCount || 0 }} 次点赞</span>
            <el-button v-if="canEdit" type="warning" @click="openEditDialog">
              <el-icon><Edit /></el-icon> 编辑信息
            </el-button>
            <el-button type="primary" class="download-btn" @click="handleDownload" :loading="downloading">
              <el-icon><Download /></el-icon> 下载图片
            </el-button>
          </div>
        </div>
      </div>

      <el-dialog v-model="editVisible" title="编辑图片信息" width="480px">
        <el-form :model="editForm" label-width="86px" v-if="editForm.id">
          <el-form-item label="图片名称">
            <el-input v-model="editForm.imageName" />
          </el-form-item>
          <el-form-item label="分类">
            <div class="category-row">
              <el-select v-model="editForm.categoryId" placeholder="选择分类" clearable filterable style="width: 100%">
                <el-option v-for="cat in categories" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
              </el-select>
              <el-button @click="openCreateCategory">新建分类</el-button>
            </div>
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="editForm.description" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="标签">
            <TagInput v-model="editForm.tags" placeholder="多个标签用 # 分隔" />
          </el-form-item>
          <el-form-item label="可见权限">
            <el-select v-model="editForm.visibility" style="width: 100%">
              <el-option label="仅自己" value="PRIVATE" />
              <el-option label="公开" value="PUBLIC" />
              <el-option label="指定用户" value="SPECIFIED" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="editForm.visibility === 'SPECIFIED'" label="指定用户">
            <el-input v-model="editForm.visibleUsernames" placeholder="输入用户名，多个用户用逗号或空格分隔" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editVisible = false">取消</el-button>
          <el-button type="primary" @click="saveEdit">保存</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="categoryDialogVisible" title="新建分类" width="360px">
        <el-form label-width="70px" @submit.prevent>
          <el-form-item label="分类名">
            <el-input v-model="newCategoryName" maxlength="20" show-word-limit @keyup.enter="submitCategory" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="categoryDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="creatingCategory" @click="submitCategory">创建</el-button>
        </template>
      </el-dialog>

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
              <el-upload :auto-upload="false" :show-file-list="false" :on-change="onCmtFileChange" accept="image/jpeg,image/png,image/webp,image/gif">
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
          <div
            class="comment-item"
            v-for="c in comments"
            :key="c.id"
            :id="`comment-${c.id}`"
            :class="{ 'notification-highlight': highlightedTarget === `comment-${c.id}` }"
          >
            <div class="comment-header">
              <router-link :to="`/profile/${c.userId}`" class="comment-user">{{ c.displayName || c.username }}</router-link>
              <span class="comment-time">{{ formatTime(c.createTime) }}</span>
            </div>
            <p class="comment-content">{{ c.content }}</p>
            <img v-if="c.imagePath" :src="`/api/comment/image/${c.id}`" class="comment-img" @click="viewCmtImg(`/api/comment/image/${c.id}`)" />
            <div class="comment-footer">
              <el-button
                :type="c.likedByMe ? 'danger' : 'default'"
                size="small"
                text
                @click="handleToggleCommentLike(c)"
              >
                <span class="comment-like-heart">{{ c.likedByMe ? '❤️' : '🤍' }}</span>
                <span v-if="c.likeCount > 0" class="comment-like-count">{{ c.likeCount }}</span>
              </el-button>
              <el-button v-if="c.userId === currentUserId" text size="small" type="danger" @click="handleDeleteComment(c.id)">删除</el-button>
            </div>
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
import { ref, reactive, computed, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import NavBar from '../components/NavBar.vue'
import ImageViewer from '../components/ImageViewer.vue'
import TagInput from '../components/TagInput.vue'
import { getImageDetail, likeImage, unlikeImage, updateImage } from '../api/image'
import { getComments, addComment, deleteComment, uploadCommentImage, likeComment, unlikeComment } from '../api/comment'
import { getCategoryList, createCategory } from '../api/category'
import { useUserStore } from '../store/user'
import { formatSize, formatTime } from '../utils/format'
import { getImageDownloadUrl } from '../utils/imageRequests'

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
const liking = ref(false)
const highlightedTarget = ref('')
const isOwner = computed(() => currentUserId.value && currentUserId.value === image.value.userId)

const editVisible = ref(false)
const categoryDialogVisible = ref(false)
const newCategoryName = ref('')
const creatingCategory = ref(false)
const categories = ref([])
const editForm = reactive({
  id: null,
  imageName: '',
  categoryId: null,
  tags: '',
  description: '',
  visibility: 'PRIVATE',
  visibleUsernames: ''
})

const tagTypes = ['', 'success', 'warning', 'danger', 'info']

const emojis = [
  '😀','😄','😂','🤣','😊','😍','🥰','😘','😎','🤩','🥳','😭','😢','😡','😤','😴',
  '👍','👎','👏','🙏','💪','🤝','👀','✨','❤️','💙','💜','🔥','⭐','🌟','💯','✅',
  '🎉','🎁','🎨','📷','🖼️','💡','📌','🚀','🎵','🎮','🏆','🍀','🌸','☕','🍕','🍰'
]

const tagList = computed(() => {
  if (!image.value.tags) return []
  return image.value.tags.split('#').map(t => t.trim()).filter(Boolean)
})

const currentUserId = computed(() => userStore.userInfo?.id)
const canEdit = computed(() => isOwner.value || userStore.userInfo?.role === 'admin')
const detailImageSrc = computed(() => getImageDownloadUrl(image.value))

onMounted(async () => {
  loading.value = true
  try {
    const [imgRes, cmtRes] = await Promise.all([
      getImageDetail(route.params.id),
      getComments(route.params.id)
    ])
    image.value = imgRes.data
    comments.value = cmtRes.data || []
    viewerSrc.value = getImageDownloadUrl(imgRes.data)
    await nextTick()
    highlightFromNotification()
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

async function handleToggleLike() {
  if (!userStore.token) {
    ElMessage.warning('请先登录后再点赞')
    return
  }
  liking.value = true
  try {
    const res = image.value.likedByMe
      ? await unlikeImage(image.value.id)
      : await likeImage(image.value.id)
    image.value.likeCount = res.data.likeCount
    image.value.likedByMe = res.data.likedByMe
  } catch {} finally {
    liking.value = false
  }
}

async function handleDeleteComment(id) {
  try {
    await deleteComment(id)
    ElMessage.success('已删除')
    comments.value = comments.value.filter(c => c.id !== id)
  } catch {}
}

async function handleToggleCommentLike(c) {
  if (!userStore.token) {
    ElMessage.warning('请先登录后再点赞')
    return
  }
  try {
    const res = c.likedByMe
      ? await unlikeComment(c.id)
      : await likeComment(c.id)
    c.likeCount = res.data.likeCount
    c.likedByMe = res.data.likedByMe
  } catch {}
}

function openEditDialog() {
  editForm.id = image.value.id
  editForm.imageName = image.value.imageName
  editForm.categoryId = image.value.categoryId
  editForm.description = image.value.description || ''
  editForm.tags = image.value.tags || ''
  editForm.visibility = image.value.visibility || 'PRIVATE'
  editForm.visibleUsernames = image.value.visibleUsernames || ''
  fetchCategories()
  editVisible.value = true
}

async function fetchCategories() {
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
  } catch {}
}

function openCreateCategory() {
  newCategoryName.value = ''
  categoryDialogVisible.value = true
}

async function submitCategory() {
  const name = newCategoryName.value.trim()
  if (!name) { ElMessage.warning('请输入分类名称'); return }
  creatingCategory.value = true
  try {
    const res = await createCategory(name)
    categories.value = categories.value.filter(c => c.id !== res.data.id)
    categories.value.push(res.data)
    editForm.categoryId = res.data.id
    categoryDialogVisible.value = false
    ElMessage.success('分类已创建')
  } catch {} finally {
    creatingCategory.value = false
  }
}

async function saveEdit() {
  try {
    await updateImage(editForm.id, {
      imageName: editForm.imageName,
      categoryId: editForm.categoryId,
      description: editForm.description,
      tags: editForm.tags,
      visibility: editForm.visibility,
      visibleUsernames: editForm.visibility === 'SPECIFIED' ? editForm.visibleUsernames : ''
    })
    ElMessage.success('更新成功')
    editVisible.value = false
    image.value.imageName = editForm.imageName
    image.value.categoryId = editForm.categoryId
    image.value.description = editForm.description
    image.value.tags = editForm.tags
    image.value.visibility = editForm.visibility
    image.value.visibleUsernames = editForm.visibility === 'SPECIFIED' ? editForm.visibleUsernames : ''
    image.value.categoryName = categories.value.find(c => c.id === editForm.categoryId)?.categoryName
  } catch {
    ElMessage.error('更新失败，请重试')
  }
}

async function handleDownload() {
  downloading.value = true
  try {
    const token = localStorage.getItem('satoken')
    const headers = token ? { 'satoken': token } : {}
    const response = await fetch(getImageDownloadUrl(image.value), { headers })

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

function highlightFromNotification() {
  const highlight = route.query.highlight
  const commentId = route.query.commentId
  let targetId = ''
  if (highlight === 'comment' && commentId) {
    targetId = `comment-${commentId}`
  } else if (highlight === 'like') {
    targetId = 'like'
  }
  if (!targetId) return
  highlightedTarget.value = targetId
  const elementId = targetId === 'like' ? 'like-activity' : targetId
  document.getElementById(elementId)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  window.setTimeout(() => {
    if (highlightedTarget.value === targetId) highlightedTarget.value = ''
  }, 1800)
}

</script>

<style scoped>
.detail-page { min-height: 100vh; background: var(--bg-base); }
.page-container { padding: 28px 8px 40px; }
.back-bar { margin-bottom: var(--space-md); }
.back-bar :deep(.el-button) { color: var(--text-muted); font-weight: 500; }
.back-bar :deep(.el-button:hover) { color: var(--accent); }

.detail-layout {
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle); border-radius: var(--radius-lg);
  padding: 28px;
}

.detail-image {
  width: 100%; height: min(65vw, 640px); min-height: 360px; overflow: hidden; border-radius: var(--radius-md);
  background: var(--bg-elevated); display: flex; align-items: center; justify-content: center;
  position: relative; cursor: pointer; border: 1px solid var(--border-subtle);
  margin-bottom: 24px;
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
  padding: 4px 0;
}
.img-title {
  font-family: var(--font-display);
  font-size: 24px; font-weight: 700;
  margin-bottom: 20px;
  word-break: break-word; color: var(--text-primary);
  letter-spacing: -0.3px; line-height: 1.3;
}

.meta-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 14px 24px;
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
.meta-tags {
  width: 100%;
}
.detail-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.desc-block {
  margin-top: 18px; padding: 14px 16px;
  background: var(--bg-elevated); border-radius: var(--radius-sm);
  border: 1px solid var(--border-subtle);
}
.desc-empty { background: transparent; border-style: dashed; }
.desc-text { font-size: 14px; color: var(--text-secondary); line-height: 1.7; margin: 0; }

.like-panel, .action-bar {
  margin-top: 18px;
  padding: 12px 14px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  background: var(--bg-elevated);
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--text-secondary);
  font-size: 14px;
}

.like-count-text {
  color: var(--text-secondary);
  font-size: 14px;
}

.download-btn {
  padding-top: 18px;
  font-weight: 600;
  border-radius: var(--radius-md);
  transition: transform 0.15s var(--ease-out), box-shadow 0.15s var(--ease-out);
  margin-left: auto;
}
.download-btn:hover {
  transform: translateY(-1px);
}

.uploader-link { color: var(--accent); text-decoration: none; font-weight: 600; font-size: 14px; }
.uploader-link:hover { color: var(--accent-glow); }

.comments-section {
  margin-top: var(--space-xl); background: var(--bg-surface);
  border: 1px solid var(--border-subtle); border-radius: var(--radius-lg);
  padding: var(--space-xl);
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
.comment-user { font-weight: 600; color: var(--accent); font-size: 14px; text-decoration: none; }
.comment-user:hover { color: var(--accent-glow); }
.comment-time { font-size: 12px; color: var(--text-muted); }
.comment-content { font-size: 14px; color: var(--text-secondary); line-height: 1.7; margin-bottom: 4px; }
.comment-footer {
  display: flex;
  align-items: center;
  gap: 4px;
}
.comment-like-heart {
  font-size: 16px; cursor: pointer; user-select: none;
  transition: transform 0.15s ease;
}
.comment-like-heart:hover { transform: scale(1.2); }
.comment-like-count { font-size: 13px; color: var(--text-muted); }

.category-row { display: flex; gap: 8px; align-items: center; }
.category-row .el-button { flex-shrink: 0; }
.comment-img {
  width: 512px; height: 512px; max-width: 100%; border-radius: var(--radius-md); cursor: pointer;
  margin: var(--space-xs) 0; object-fit: contain; border: 1px solid var(--border-subtle);
  background: var(--bg-elevated);
}
.comment-empty { text-align: center; color: var(--text-muted); padding: var(--space-xl) 0; font-size: 14px; }

.notification-highlight {
  position: relative;
  animation: notificationRipple 1.8s ease;
}

@keyframes notificationRipple {
  0% { box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.34); background: rgba(37, 99, 235, 0.12); }
  55% { box-shadow: 0 0 0 14px rgba(37, 99, 235, 0); background: rgba(37, 99, 235, 0.06); }
  100% { box-shadow: 0 0 0 0 rgba(37, 99, 235, 0); }
}

@media (max-width: 900px) {
  .detail-layout { padding: 20px; }
  .detail-image { height: 420px; min-height: 280px; }
  .comments-section { padding: var(--space-md); }
  .comment-img { height: min(512px, calc(100vw - 58px)); }
}
</style>
