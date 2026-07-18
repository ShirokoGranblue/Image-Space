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
          <button
            class="detail-image"
            type="button"
            :aria-label="detailImageStatus === 'error' ? `重新加载图片：${imageAlt}` : `沉浸查看：${imageAlt}`"
            @click="handleMainImageAction"
            @mouseenter="imgHover = true"
            @mouseleave="imgHover = false"
          >
            <span v-if="detailImageStatus === 'loading'" class="detail-image-loading" role="status">正在加载图片</span>
            <img
              v-if="detailImageStatus !== 'error'"
              :key="detailImageRevision"
              :src="detailImageSrc"
              :alt="imageAlt"
              :width="image.width || undefined"
              :height="image.height || undefined"
              :class="{ zoomed: imgHover }"
              loading="eager"
              fetchpriority="high"
              decoding="async"
              @load="detailImageStatus = 'loaded'"
              @error="detailImageStatus = 'error'"
            />
            <span v-else class="detail-image-error" role="alert">
              <el-icon><PictureFilled /></el-icon>
              <strong>图片加载失败</strong>
              <small>点击重新加载</small>
            </span>
            <transition name="fade">
              <div class="img-hover-overlay" v-if="imgHover && detailImageStatus === 'loaded'">
                <el-icon :size="34"><ZoomIn /></el-icon>
                <span>查看原图</span>
              </div>
            </transition>
          </button>
        </div>

        <ImageDetailInfoPanel :image="image" :tags="tagList" :comment-count="comments.length" :liking="liking" :downloading="downloading" :can-edit="canEdit" :highlighted-target="highlightedTarget" @like="handleToggleLike" @download="handleDownload()" @download-format="handleDownloadFormat" @edit="openEditDialog" />
      </section>

      <ErrorState
        v-else-if="loadError"
        title="图片暂时无法显示"
        description="图片详情请求失败，请检查连接后重试。"
        retry-label="重新加载"
        @retry="loadImageDetail"
      />

      <section class="empty-state deleted-state" v-if="image.deleted">
        <span class="section-label">无法查看</span>
        <p>图片已删除或不可用</p>
      </section>

      <ImageCommentsSection v-if="image.id && !image.deleted" :comments="comments" :text="commentText" :emojis="emojis" :file-name="cmtFile?.name || ''" :sending="sending" :current-user-id="currentUserId" :highlighted-target="highlightedTarget" :format-time="formatRelativeTime" @update:text="commentText = $event" @insert-emoji="insertEmoji" @file-change="onCmtFileChange" @submit="handleAddComment" @view-image="viewCmtImg" @toggle-like="handleToggleCommentLike" @delete="handleDeleteComment" />

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
            <TagInput v-model="editForm.tags" placeholder="用 # 分隔多个标签" />
          </el-form-item>
          <el-form-item label="可见范围">
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

    <ImageViewer ref="viewerRef" :src="viewerSrc" :title="imageAlt" :alt="imageAlt" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onBeforeUnmount, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, PictureFilled, ZoomIn } from '@element-plus/icons-vue'
import NavBar from '../components/NavBar.vue'
import ImageViewer from '../components/ImageViewer.vue'
import ImageDetailInfoPanel from '../components/detail/ImageDetailInfoPanel.vue'
import ImageCommentsSection from '../components/detail/ImageCommentsSection.vue'
import TagInput from '../components/TagInput.vue'
import ErrorState from '../components/states/ErrorState.vue'
import { downloadImage, downloadImageAs, getImageDetail, likeImage, unlikeImage, updateImage } from '../api/image'
import { getImageResourceStatus, refreshImageAccessUrl } from '../api/resource'
import { getComments, addComment, deleteComment, uploadCommentImage, fetchCommentImage, likeComment, unlikeComment } from '../api/comment'
import { getCategoryList, createCategory } from '../api/category'
import { useUserStore } from '../store/user'
import { getToken } from '../utils/token'
import { formatRelativeTime } from '../utils/format'
import { getImageAlt, getImagePreviewUrl, getImageViewerUrl } from '../utils/imageRequests'
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
const commentImageObjectUrls = new Map()
const commentText = ref('')
const cmtFile = ref(null)
const imgHover = ref(false)
const detailImageStatus = ref('loading')
const detailImageRevision = ref(0)
const loading = ref(false)
const loadError = ref(false)
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
const imageAlt = computed(() => getImageAlt(image.value))
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
    const previousViewerUrl = getImageViewerUrl(image.value)
    applyImageAccessUrl(image.value, access, status)
    const nextUrl = detailImageSrc.value
    if (!viewerSrc.value || viewerSrc.value === previousUrl || viewerSrc.value === previousViewerUrl) {
      viewerSrc.value = viewerSrc.value === previousViewerUrl ? getImageViewerUrl(image.value) : nextUrl
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
    await replaceComments(res.data || [])
    return {
      url: commentsPollingResource.value?.url || '',
      version: commentsPollingResource.value?.version || null,
      visibility: 'PUBLIC',
      status: 'READY',
    }
  },
})

onMounted(loadImageDetail)
onBeforeUnmount(releaseAllCommentImages)

watch(() => route.params.uuid, () => {
  loadImageDetail()
})

watch(detailImageSrc, src => {
  detailImageStatus.value = src ? 'loading' : 'error'
})

async function loadImageDetail() {
  loading.value = true
  loadError.value = false
  try {
    image.value = {}
    releaseAllCommentImages()
    comments.value = []
    editVisible.value = false
    viewerSrc.value = ''
    const [imgRes, cmtRes] = await Promise.all([
      getImageDetail(route.params.uuid),
      getComments(route.params.uuid)
    ])
    image.value = imgRes.data
    await replaceComments(cmtRes.data || [])
    viewerSrc.value = getImagePreviewUrl(imgRes.data)
    await nextTick()
    highlightFromNotification()
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function openMainViewer() {
  viewerSrc.value = getImageViewerUrl(image.value)
  viewerRef.value.open({ trigger: document.activeElement })
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
    await replaceComments(res.data || [])
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
    releaseCommentImage(id)
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
    ElMessage.success(c.likedByMe ? '已点赞' : '已取消点赞')
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
  await downloadFromUrl(downloadImageAs(image.value.uuid, format), format)
}

async function downloadFromUrl(downloadUrl, format = '') {
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
    a.download = downloadFilename(format)
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

function downloadFilename(format) {
  const sourceName = image.value.imageName || image.value.originalFilename || 'image'
  if (!format) return sourceName
  const baseName = sourceName.replace(/\.[^.]+$/, '') || 'image'
  return `${baseName}.${format}`
}

function viewCmtImg(src) {
  viewerSrc.value = src
  viewerRef.value.open({ trigger: document.activeElement })
}

async function replaceComments(nextComments) {
  releaseAllCommentImages()
  comments.value = nextComments.map(comment => ({
    ...comment,
    displayImageUrl: '',
    imageLoadError: false,
  }))
  await Promise.all(comments.value.map(async comment => {
    if (!comment.imageUrl) return
    try {
      const blob = await fetchCommentImage(comment.imageUrl)
      const displayUrl = typeof URL.createObjectURL === 'function'
        ? URL.createObjectURL(blob)
        : comment.imageUrl
      comment.displayImageUrl = displayUrl
      if (displayUrl !== comment.imageUrl) commentImageObjectUrls.set(comment.id, displayUrl)
    } catch {
      comment.imageLoadError = true
    }
  }))
}

function releaseCommentImage(id) {
  const url = commentImageObjectUrls.get(id)
  if (url && typeof URL.revokeObjectURL === 'function') URL.revokeObjectURL(url)
  commentImageObjectUrls.delete(id)
}

function releaseAllCommentImages() {
  for (const id of commentImageObjectUrls.keys()) releaseCommentImage(id)
}

function handleMainImageAction() {
  if (detailImageStatus.value === 'error') {
    detailImageRevision.value += 1
    detailImageStatus.value = detailImageSrc.value ? 'loading' : 'error'
    return
  }
  openMainViewer()
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
.detail-page { min-height: 100vh; background: var(--color-canvas); color: var(--color-text-primary); }
.detail-container { width: min(calc(100% - (2 * var(--page-gutter))), var(--page-standard)); padding: 104px 0 var(--space-8); }
.desk-back { display: inline-flex; align-items: center; gap: var(--space-2); min-height: 44px; margin-bottom: var(--space-5); padding: 0 var(--space-3); border: 1px solid var(--color-border-subtle); border-radius: var(--radius-sm); background: transparent; color: var(--color-text-secondary); font-family: var(--font-body); cursor: pointer; }
.desk-back:hover,.desk-back:focus-visible { border-color: var(--color-border-strong); background: var(--color-surface-2); outline: 2px solid var(--color-urban); outline-offset: 2px; }
.detail-layout { display: grid; grid-template-columns: minmax(0, 1fr) minmax(320px, 380px); align-items: stretch; gap: 0; overflow: hidden; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-sm); background: var(--color-surface-1); }
.detail-main { min-width: 0; display: grid; border-right: 1px solid var(--color-border-subtle); background: transparent; }
.detail-image { position: relative; display: grid; min-height: 520px; padding: var(--space-4); overflow: hidden; place-items: center; border: 0; background: transparent; color: var(--color-text-primary); cursor: zoom-in; }
.detail-image img { display: block; max-width: min(92%,1100px); max-height: min(82vh,820px); object-fit: contain; transition: transform var(--duration-overlay) var(--ease-standard),filter var(--duration-standard) var(--ease-standard); }
.detail-image img.zoomed { transform: scale(1.02); filter: brightness(.78); }
.detail-image-loading,.detail-image-error { position: absolute; z-index: 2; display: grid; place-items: center; gap: var(--space-2); color: var(--color-text-muted); font-family: var(--font-ui); }
.detail-image-error strong { color: var(--color-text-primary); }.detail-image-error small { color: var(--color-text-muted); }
.img-hover-overlay { position: absolute; inset: 0; display: grid; place-items: center; align-content: center; gap: var(--space-2); background: rgba(14,18,22,.32); color: var(--color-text-inverse); pointer-events: none; }
.fade-enter-active,.fade-leave-active { transition: opacity var(--duration-standard) var(--ease-standard); }.fade-enter-from,.fade-leave-to { opacity: 0; }
.detail-container :deep(.comments-section) { margin-top: var(--space-6); }
.empty-state { padding: var(--space-7); border: 1px solid var(--color-border-subtle); background: var(--color-surface-1); color: var(--color-text-muted); text-align: center; }
.section-label { color: var(--color-vermilion); font-size: var(--text-xs); font-weight: 700; letter-spacing: .08em; }
.deleted-state p { margin-top: var(--space-3); color: var(--color-text-primary); font-family: var(--font-title); font-size: var(--text-2xl); }
.category-row { display: grid; grid-template-columns: 1fr auto; gap: var(--space-2); width: 100%; }.category-row :deep(.el-select) { width: 100%; }
@media (max-width:1120px) { .detail-layout { grid-template-columns: 1fr; } .detail-main { border-right: 0; border-bottom: 1px solid var(--color-border-subtle); } .detail-image { min-height: 460px; } }
@media (max-width:700px) { .detail-container { padding-top: 82px; } .detail-image { min-height: 320px; } }
@media (prefers-reduced-motion:reduce) { .detail-image img,.fade-enter-active,.fade-leave-active { transition: none; } }
</style>
