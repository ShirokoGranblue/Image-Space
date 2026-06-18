<template>
  <div class="detail-page asset-workspace">
    <NavBar />
    <main class="page-container detail-container" v-loading="loading">
      <button class="desk-back" type="button" @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </button>

      <section class="detail-layout" v-if="image.id && !image.deleted">
        <div class="detail-main">
          <div class="stage-topline">
            <span>{{ visibilityText(image.visibility) }}</span>
            <span>{{ formatTime(image.uploadTime) }}</span>
          </div>
          <div class="detail-image" @click="openMainViewer" @mouseenter="imgHover = true" @mouseleave="imgHover = false">
            <img :src="detailImageSrc" :alt="image.imageName" :class="{ zoomed: imgHover }" />
            <transition name="fade">
              <div class="img-hover-overlay" v-if="imgHover">
                <el-icon :size="34"><ZoomIn /></el-icon>
                <span>查看原图</span>
              </div>
            </transition>
          </div>
        </div>

        <aside class="detail-info">
          <div class="detail-title-block">
            <span class="section-label">图片信息</span>
            <h1 class="img-title">{{ image.imageName }}</h1>
            <p class="desc-text" v-if="image.description">{{ image.description }}</p>
            <p class="desc-text muted" v-else>暂无描述</p>
          </div>

          <div class="action-bar" id="like-activity" :class="{ 'notification-highlight': highlightedTarget === 'like' }">
            <el-button class="like-button" :class="{ liked: image.likedByMe }" @click="handleToggleLike" :loading="liking">
              <el-icon><StarFilled /></el-icon>
              {{ image.likedByMe ? '已点赞' : '点赞' }}
            </el-button>
            <el-button type="primary" class="download-btn" @click="handleDownload()" :loading="downloading" :disabled="image.deleted">
              <el-icon><Download /></el-icon>
              下载图片
            </el-button>
            <el-dropdown trigger="click" class="format-download-menu" @command="handleDownloadFormat">
              <el-button class="format-download-trigger" :disabled="image.deleted || downloading">格式</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="jpg">下载 JPG</el-dropdown-item>
                  <el-dropdown-item command="png">下载 PNG</el-dropdown-item>
                  <el-dropdown-item command="gif">下载 GIF</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-dropdown v-if="canEdit" trigger="click" class="more-actions">
              <el-button circle class="more-trigger" aria-label="更多操作">
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="openEditDialog">
                    <el-icon><Edit /></el-icon>
                    编辑信息
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>

          <div class="count-strip">
            <div>
              <strong>{{ image.likeCount || 0 }}</strong>
              <span>点赞</span>
            </div>
            <div>
              <strong>{{ comments.length }}</strong>
              <span>评论</span>
            </div>
          </div>

          <div class="meta-grid">
            <div class="meta-item">
              <span class="meta-label"><el-icon><User /></el-icon> 上传者</span>
              <router-link :to="'/profile/' + (image.userUuid || image.userId)" class="uploader-link">
                {{ image.displayName || image.username }}
              </router-link>
            </div>
            <div class="meta-item">
              <span class="meta-label"><el-icon><FolderOpened /></el-icon> 分类</span>
              <span class="meta-value">{{ image.categoryName || '未分类' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label"><el-icon><Document /></el-icon> 文件大小</span>
              <span class="meta-value">{{ formatSize(image.fileSize) }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label"><el-icon><PictureRounded /></el-icon> 类型</span>
              <span class="meta-value">{{ image.imageType || '--' }}</span>
            </div>
          </div>

          <div class="tag-panel">
            <span class="section-label">标签</span>
            <div class="detail-tag-list" v-if="tagList.length">
              <el-tag v-for="(tag, i) in tagList" :key="i" size="small" effect="plain" class="flat-tag">
                {{ tag }}
              </el-tag>
            </div>
            <p v-else>暂无标签</p>
          </div>
        </aside>
      </section>

      <section class="empty-state deleted-state" v-if="image.deleted">
        <span class="section-label">无法查看</span>
        <p>图片已删除或不可用</p>
      </section>

      <section class="comments-section" v-if="image.id && !image.deleted">
        <div class="comments-head">
          <span class="section-label">交流</span>
          <h2>评论区</h2>
        </div>

        <div class="comment-input">
          <el-popover placement="top" :width="340" trigger="click">
            <template #reference>
              <el-button circle size="small" class="emoji-btn" aria-label="插入表情">
                <el-icon><ChatDotRound /></el-icon>
              </el-button>
            </template>
            <div class="emoji-grid">
              <span v-for="e in emojis" :key="e" class="emoji-item" @click="insertEmoji(e)">{{ e }}</span>
            </div>
          </el-popover>
          <el-input
            v-model="commentText"
            placeholder="写下你的评论..."
            maxlength="500"
            class="comment-text-input"
            @keyup.enter="handleAddComment"
          />
          <el-upload :auto-upload="false" :show-file-list="false" :on-change="onCmtFileChange" accept="image/jpeg,image/png,image/webp,image/gif">
            <el-button size="small" circle class="upload-btn" aria-label="上传评论图片">
              <el-icon><PictureFilled /></el-icon>
            </el-button>
          </el-upload>
          <span class="upload-hint" v-if="cmtFile">{{ cmtFile.name }}</span>
          <el-button type="primary" @click="handleAddComment" :loading="sending">发表</el-button>
        </div>

        <div class="comment-list" v-if="comments.length > 0">
          <article
            class="comment-item"
            v-for="c in comments"
            :key="c.id"
            :id="'comment-' + c.id"
            :class="{ 'notification-highlight': highlightedTarget === 'comment-' + c.id }"
          >
            <div class="comment-header">
              <router-link :to="'/profile/' + (c.userUuid || c.userId)" class="comment-user">
                {{ c.displayName || c.username }}
              </router-link>
              <span class="comment-time">{{ formatTime(c.createTime) }}</span>
            </div>
            <p class="comment-content">{{ c.content }}</p>
            <img v-if="c.imageUrl" :src="c.imageUrl" class="comment-img" @click="viewCmtImg(c.imageUrl)" />
            <div class="comment-footer">
              <el-button :class="{ liked: c.likedByMe }" size="small" text @click="handleToggleCommentLike(c)">
                <el-icon class="comment-like-heart"><StarFilled /></el-icon>
                <span v-if="c.likeCount > 0" class="comment-like-count">{{ c.likeCount }}</span>
              </el-button>
              <el-dropdown v-if="c.userId === currentUserId" trigger="click" class="comment-more">
                <el-button text size="small" class="comment-more-trigger" aria-label="评论操作">
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handleDeleteComment(c.id)">
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </article>
        </div>

        <div class="comment-empty" v-else>
          <p>暂无评论，来写下第一条观察。</p>
        </div>
      </section>

      <el-dialog v-model="editVisible" title="编辑图片信息" width="520px" class="asset-dialog">
        <el-form :model="editForm" label-position="top" v-if="editForm.uuid">
          <el-form-item label="图片名称">
            <el-input v-model="editForm.imageName" />
          </el-form-item>
          <el-form-item label="分类">
            <div class="category-row">
              <el-select v-model="editForm.categoryId" placeholder="选择分类" clearable filterable>
                <el-option v-for="cat in categories" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
              </el-select>
              <el-button @click="openCreateCategory">新建</el-button>
            </div>
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="editForm.description" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="标签">
            <TagInput v-model="editForm.tags" placeholder="多个标签用 # 分隔" />
          </el-form-item>
          <el-form-item label="可见权限">
            <el-select v-model="editForm.visibility">
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

      <el-dialog v-model="categoryDialogVisible" title="新建分类" width="380px" class="asset-dialog">
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="分类名">
            <el-input v-model="newCategoryName" maxlength="20" show-word-limit @keyup.enter="submitCategory" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="categoryDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="creatingCategory" @click="submitCategory">创建</el-button>
        </template>
      </el-dialog>
    </main>

    <ImageViewer ref="viewerRef" :src="viewerSrc" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import NavBar from '../components/NavBar.vue'
import ImageViewer from '../components/ImageViewer.vue'
import TagInput from '../components/TagInput.vue'
import { downloadImage, downloadImageAs, getImageDetail, likeImage, unlikeImage, updateImage } from '../api/image'
import { getImageResourceStatus, refreshImageAccessUrl } from '../api/resource'
import { getComments, addComment, deleteComment, uploadCommentImage, likeComment, unlikeComment } from '../api/comment'
import { getCategoryList, createCategory } from '../api/category'
import { useUserStore } from '../store/user'
import { getToken } from '../utils/token'
import { formatSize, formatTime } from '../utils/format'
import { getImagePreviewUrl } from '../utils/imageRequests'
import { applyImageAccessUrl, applyImageStatus, imageToPollingResource } from '../utils/resourceAdapters'
import { isAccessUrlExpiring, parseAccessUrlMetadata } from '../utils/resourceAccess'
import { hasSpecifiedUsers } from '../utils/visibility'
import { POLLING_INTERVALS, useResourcePolling } from '../composables/useResourcePolling'

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
const editVisible = ref(false)
const categoryDialogVisible = ref(false)
const newCategoryName = ref('')
const creatingCategory = ref(false)
const categories = ref([])
const editForm = reactive({
  uuid: '',
  imageName: '',
  categoryId: null,
  tags: '',
  description: '',
  visibility: 'PUBLIC',
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
const canEdit = computed(() => image.value.ownedByMe === true)
const detailImageSrc = computed(() => getImagePreviewUrl(image.value))
const detailPollingResource = computed(() => imageToPollingResource(image.value))
const commentsPollingResource = computed(() => {
  if (!image.value.uuid) return null
  const imageComments = comments.value.filter(c => c.imageUrl)
  if (imageComments.length === 0) return null
  const watched = imageComments.find(c => isAccessUrlExpiring(c.imageUrl)) || imageComments[0]
  return {
    id: `comments:${image.value.uuid}`,
    url: watched.imageUrl,
    version: imageComments.map(c => `${c.id}:${parseAccessUrlMetadata(c.imageUrl).version || c.imageUrl}`).join('|'),
    visibility: 'PUBLIC',
    status: 'READY',
  }
})

useResourcePolling({
  resource: detailPollingResource,
  intervalMs: POLLING_INTERVALS.detail,
  enabled: computed(() => Boolean(image.value.uuid && !image.value.deleted)),
  getStatus: async () => {
    const res = await getImageResourceStatus(image.value.uuid)
    return res.data
  },
  getAccessUrl: async () => {
    const res = await refreshImageAccessUrl(image.value.uuid)
    return res.data
  },
  onStatusChange: (status) => {
    applyImageStatus(image.value, status)
  },
  onAccessUrl: (access, status) => {
    const previousUrl = detailImageSrc.value
    applyImageAccessUrl(image.value, access, status)
    const nextUrl = detailImageSrc.value
    if (!viewerSrc.value || viewerSrc.value === previousUrl) {
      viewerSrc.value = nextUrl
    }
  },
  onDeleted: () => {
    image.value.deleted = true
    viewerSrc.value = ''
    ElMessage.warning('图片已删除或不可用')
  },
})

useResourcePolling({
  resource: commentsPollingResource,
  intervalMs: POLLING_INTERVALS.detail,
  enabled: computed(() => Boolean(image.value.uuid && comments.value.some(c => c.imageUrl))),
  immediate: false,
  getStatus: async (resource) => ({
    id: resource.id,
    version: commentsPollingResource.value?.version || null,
    visibility: 'PUBLIC',
    status: 'READY',
    deleted: false,
  }),
  getAccessUrl: async () => {
    const res = await getComments(image.value.uuid)
    comments.value = res.data || []
    return {
      url: commentsPollingResource.value?.url || '',
      version: commentsPollingResource.value?.version || null,
      visibility: 'PUBLIC',
      status: 'READY',
    }
  },
})

onMounted(loadImageDetail)

watch(() => route.params.uuid, () => {
  loadImageDetail()
})

async function loadImageDetail() {
  loading.value = true
  try {
    image.value = {}
    comments.value = []
    editVisible.value = false
    viewerSrc.value = ''
    const [imgRes, cmtRes] = await Promise.all([
      getImageDetail(route.params.uuid),
      getComments(route.params.uuid)
    ])
    image.value = imgRes.data
    comments.value = cmtRes.data || []
    viewerSrc.value = getImagePreviewUrl(imgRes.data)
    await nextTick()
    highlightFromNotification()
  } catch {} finally {
    loading.value = false
  }
}

function openMainViewer() {
  viewerSrc.value = detailImageSrc.value
  viewerRef.value.open()
}

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
    const res = await getComments(image.value.uuid)
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
      ? await unlikeImage(image.value.uuid)
      : await likeImage(image.value.uuid)
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
  editForm.uuid = image.value.uuid
  editForm.imageName = image.value.imageName
  editForm.categoryId = image.value.categoryId
  editForm.description = image.value.description || ''
  editForm.tags = image.value.tags || ''
  editForm.visibility = image.value.visibility || 'PUBLIC'
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
  if (!editForm.uuid) return
  if (editForm.visibility === 'SPECIFIED' && !hasSpecifiedUsers(editForm.visibleUsernames)) {
    ElMessage.warning('请先填写指定用户')
    return
  }
  try {
    const res = await updateImage(editForm.uuid, {
      imageName: editForm.imageName,
      categoryId: editForm.categoryId,
      description: editForm.description,
      tags: editForm.tags,
      visibility: editForm.visibility,
      visibleUsernames: editForm.visibility === 'SPECIFIED' ? editForm.visibleUsernames : ''
    })
    ElMessage.success('更新成功')
    editVisible.value = false
    const updated = res.data || {}
    Object.assign(image.value, updated, {
      imageUrl: updated.imageUrl || '',
      originalUrl: updated.originalUrl || '',
      publicUrl: updated.publicUrl || '',
      privateUrl: updated.privateUrl || '',
      mediumUrl: updated.mediumUrl || '',
      thumbUrl: updated.thumbUrl || ''
    })
    viewerSrc.value = getImagePreviewUrl(image.value)
  } catch {
    ElMessage.error('更新失败，请重试')
  }
}

async function handleDownload() {
  await downloadFromUrl(downloadImage(image.value.uuid))
}

async function handleDownloadFormat(format) {
  await downloadFromUrl(downloadImageAs(image.value.uuid, format))
}

async function downloadFromUrl(downloadUrl) {
  downloading.value = true
  try {
    const token = getToken()
    const headers = token ? { 'satoken': token } : {}
    const response = await fetch(downloadUrl, { headers })

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

function visibilityText(visibility) {
  if (visibility === 'PUBLIC') return '公开'
  if (visibility === 'SPECIFIED') return '指定用户'
  return '仅自己'
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
  const target = document.getElementById(elementId)
  if (target) {
    const rect = target.getBoundingClientRect()
    const top = rect.top + window.scrollY - Math.max(80, window.innerHeight * 0.2)
    window.scrollTo({ top, behavior: 'smooth' })
  }
  window.setTimeout(() => {
    if (highlightedTarget.value === targetId) highlightedTarget.value = ''
  }, 1800)
}

</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  color: var(--ad-text);
  background: var(--ad-bg);
}

.detail-container {
  width: min(100%, 1640px);
  padding: 96px 24px 64px;
}

.desk-back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  margin-bottom: 18px;
  padding: 0 14px;
  border: 1px solid var(--ad-line);
  color: var(--ad-text-soft);
  background: rgba(244, 241, 232, 0.045);
  font-family: var(--ad-font);
  cursor: pointer;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 410px;
  min-height: 720px;
  border: 1px solid var(--ad-line);
  background: rgba(17, 23, 34, 0.66);
  box-shadow: var(--ad-shadow-soft);
}

.detail-main {
  min-width: 0;
  display: grid;
  grid-template-rows: auto 1fr;
  border-right: 1px solid var(--ad-line);
}

.stage-topline {
  min-height: 54px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
  border-bottom: 1px solid var(--ad-line);
  color: var(--ad-muted);
  font-size: 12px;
}

.detail-image {
  position: relative;
  min-height: 520px;
  display: grid;
  place-items: center;
  overflow: hidden;
  cursor: zoom-in;
  background:
    linear-gradient(90deg, rgba(244,241,232,0.035) 1px, transparent 1px),
    linear-gradient(rgba(244,241,232,0.035) 1px, transparent 1px),
    #0b0f15;
  background-size: 46px 46px;
}

.detail-image::before {
  content: '';
  position: absolute;
  inset: 20px;
  border: 1px solid rgba(244, 241, 232, 0.08);
  pointer-events: none;
}

.detail-image img {
  max-width: min(92%, 1100px);
  max-height: min(82vh, 820px);
  object-fit: contain;
  transition: transform 0.42s var(--ad-ease), filter 0.24s ease;
}

.detail-image img.zoomed {
  transform: scale(1.035);
  filter: brightness(0.72);
}

.img-hover-overlay {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  color: var(--ad-text);
  background: rgba(7, 10, 14, 0.34);
  pointer-events: none;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity .18s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.detail-info {
  min-width: 0;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-label {
  color: var(--ad-muted);
  font-size: 11px;
  letter-spacing: 0;
}

.img-title {
  margin: 12px 0 14px;
  color: var(--ad-text);
  font-size: clamp(36px, 4vw, 62px);
  line-height: 0.95;
  font-weight: 340;
  overflow-wrap: anywhere;
}

.desc-text {
  color: var(--ad-text-soft);
  font-size: 15px;
  line-height: 1.8;
}

.desc-text.muted,
.tag-panel p {
  color: var(--ad-muted);
}

.action-bar {
  display: grid;
  grid-template-columns: 1fr 1.2fr auto auto;
  gap: 10px;
}

.like-button.liked,
.comment-footer :deep(.el-button.liked) {
  color: #071014;
  background: var(--ad-green);
  border-color: var(--ad-green);
}

.download-btn {
  min-width: 138px;
}

.format-download-trigger {
  min-width: 64px;
}

.more-trigger {
  width: 40px;
  height: 40px;
}

.count-strip {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  border: 1px solid var(--ad-line);
}

.count-strip div {
  padding: 16px;
}

.count-strip div + div {
  border-left: 1px solid var(--ad-line);
}

.count-strip strong {
  display: block;
  color: var(--ad-green);
  font-size: 26px;
  font-weight: 420;
}

.count-strip span {
  color: var(--ad-muted);
  font-size: 12px;
}

.meta-grid {
  display: grid;
  border-top: 1px solid var(--ad-line);
}

.meta-item {
  display: grid;
  grid-template-columns: 118px minmax(0, 1fr);
  gap: 14px;
  padding: 14px 0;
  border-bottom: 1px solid var(--ad-line);
}

.meta-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--ad-muted);
  font-size: 12px;
}

.meta-value,
.uploader-link {
  color: var(--ad-text);
  overflow-wrap: anywhere;
}

.tag-panel {
  margin-top: auto;
  padding: 16px;
  border: 1px solid var(--ad-line);
  background: rgba(244, 241, 232, 0.035);
}

.detail-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.flat-tag {
  --el-tag-bg-color: rgba(244, 241, 232, 0.04);
  --el-tag-border-color: var(--ad-line);
  --el-tag-text-color: var(--ad-text-soft);
  border-radius: 6px;
}

.comments-section {
  margin-top: 24px;
  padding: 24px;
  border: 1px solid var(--ad-line);
  background: rgba(17, 23, 34, 0.66);
}

.comments-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.comments-head h2 {
  color: var(--ad-text);
  font-size: 30px;
  font-weight: 340;
}

.comment-input {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto auto auto;
  gap: 10px;
  align-items: center;
  padding: 12px;
  border: 1px solid var(--ad-line);
  background: rgba(244, 241, 232, 0.035);
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 6px;
}

.emoji-item {
  cursor: pointer;
  text-align: center;
  line-height: 30px;
}

.upload-hint {
  max-width: 140px;
  color: var(--ad-muted);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.comment-list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.comment-item {
  padding: 16px;
  border: 1px solid var(--ad-line);
  background: rgba(13, 16, 22, 0.72);
}

.comment-header,
.comment-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.comment-user {
  color: var(--ad-text);
}

.comment-time {
  color: var(--ad-muted);
  font-size: 12px;
}

.comment-content {
  margin: 12px 0;
  color: var(--ad-text-soft);
  line-height: 1.7;
}

.comment-img {
  max-width: 260px;
  max-height: 180px;
  object-fit: cover;
  border: 1px solid var(--ad-line);
  cursor: zoom-in;
}

.comment-empty,
.empty-state {
  padding: 42px;
  text-align: center;
  border: 1px solid var(--ad-line);
  color: var(--ad-muted);
  background: rgba(17, 23, 34, 0.62);
}

.deleted-state p {
  margin-top: 12px;
  color: var(--ad-text);
  font-size: 28px;
}

.notification-highlight {
  outline: 2px solid var(--ad-green);
  outline-offset: 3px;
}

.category-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  width: 100%;
}

.category-row :deep(.el-select) {
  width: 100%;
}

@media (max-width: 1120px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .detail-main {
    border-right: 0;
    border-bottom: 1px solid var(--ad-line);
  }

  .detail-image {
    min-height: 460px;
  }
}

@media (max-width: 700px) {
  .detail-container {
    padding: 82px 14px 40px;
  }

  .detail-info,
  .comments-section {
    padding: 16px;
  }

  .action-bar,
  .comment-input,
  .meta-item {
    grid-template-columns: 1fr;
  }

  .detail-image {
    min-height: 320px;
  }
}
</style>
