<template>
  <div class="profile-page asset-profile">
    <NavBar />
    <main class="page-container profile-container" v-loading="loading">
      <section class="profile-banner" :style="bannerStyle">
        <div class="banner-grid" v-if="!backgroundDisplayUrl" aria-hidden="true">
          <div
            v-for="(cell, i) in bannerCells"
            :key="i"
            class="banner-cell"
            :style="{ background: cell }"
          ></div>
        </div>
        <div class="banner-overlay"></div>
        <div class="banner-edit" v-if="isOwner" :class="{ 'banner-edit--active': editing }">
          <el-button size="small" @click="openBackgroundEditor">
            <el-icon><Edit /></el-icon>
            编辑背景
          </el-button>
        </div>
      </section>

      <ProfileHeader
        :user="user" :avatar-url="avatarDisplayUrl" :owner="isOwner" :editing="editing" :stats="profileHeaderStats" :show-meta="showProfileMeta" :joined-at="joinedAt"
        :form="form" :field-errors="fieldErrors" :email-changed="emailChanged" :sending-email-code="sendingEmailCode" :email-code-countdown="emailCodeCountdown" :saving="saving"
        @edit-avatar="openAvatarEditor" @edit-profile="startEdit" @delete-account="handleDeleteAccount" @update-form-field="updateProfileFormField"
        @email-blur="onEmailBlur" @phone-blur="onPhoneBlur" @send-email-code="handleSendEmailChangeCode" @save="saveProfile" @cancel="cancelEdit"
      />

      <ProfileWorksSection :items="works" :loading="worksLoading" :error="worksError" :owner="isOwner" :selected-uuids="selectedWorkUuids" :all-selected="allWorksSelected" :partially-selected="partiallyWorksSelected" @toggle-all="toggleSelectAllWorks" @clear="clearWorkSelection" @batch-delete="handleBatchWorkDelete" @retry="fetchWorks" @delete="handleWorkDelete" @edit="handleWorkEdit" @copy="copyWorkLink" @toggle="toggleWorkSelection" />

      <ProfileBackgroundEditor
        ref="backgroundEditorRef" :visible="bgDialogVisible" :preview-url="bgPreviewUrl" :crop-img-style="bgCropImgStyle" :crop-frame-style="bgCropFrameStyle"
        :crop-grid-style="bgCropGridStyle" :crop-ratio="bgCropRatio" :file-name="bgFileName" :mini-banner-style="miniBannerPreviewStyle"
        :avatar-url="avatarDisplayUrl" :display-name="user.displayName || user.username" :username="user.username" :saving="bgSaving" :handle-position="bgHPos"
        @update:visible="bgDialogVisible = $event" @update:crop-ratio="bgCropRatio = $event" @drag-start="startDragBgCrop" @drag-move="onDragBgCrop" @drag-end="stopDragBgCrop"
        @resize-start="startBgResize" @slider-change="onBgSliderChange" @file-change="onBgFileChange" @save="saveBackground"
      />

      <ProfileAvatarEditor
        :visible="avatarDialogVisible"
        :source-url="avatarEditorSourceUrl"
        :saving="avatarSaving"
        @update:visible="avatarDialogVisible = $event"
        @submit="submitAvatar"
      />

      <el-dialog v-model="imageEditVisible" title="编辑图片信息" width="520px" class="profile-dialog">
        <el-form :model="imageEditForm" label-position="top" v-if="imageEditForm.uuid">
          <el-form-item label="图片名称">
            <el-input v-model="imageEditForm.imageName" />
          </el-form-item>
          <el-form-item label="分类">
            <div class="category-row">
              <el-select v-model="imageEditForm.categoryId" placeholder="选择分类" clearable filterable>
                <el-option v-for="cat in categories" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
              </el-select>
              <el-button @click="openWorkCreateCategory">新建</el-button>
            </div>
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="imageEditForm.description" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="标签">
            <TagInput v-model="imageEditForm.tags" placeholder="用 # 分隔多个标签" />
          </el-form-item>
          <el-form-item label="可见范围">
            <el-select v-model="imageEditForm.visibility">
              <el-option label="仅自己" value="PRIVATE" />
              <el-option label="公开" value="PUBLIC" />
              <el-option label="指定用户" value="SPECIFIED" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="imageEditForm.visibility === 'SPECIFIED'" label="指定用户">
            <el-input v-model="imageEditForm.visibleUsernames" placeholder="输入用户名，多个用户用逗号或空格分隔" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="imageEditVisible = false">取消</el-button>
          <el-button type="primary" @click="saveWorkEdit">保存</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="workCategoryDialogVisible" title="新建分类" width="380px" class="profile-dialog">
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="分类名">
            <el-input v-model="newWorkCategoryName" maxlength="20" show-word-limit @keyup.enter="submitWorkCategory" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="workCategoryDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="creatingWorkCategory" @click="submitWorkCategory">创建</el-button>
        </template>
      </el-dialog>
    </main>

    <Teleport to="body">
      <div class="pagination-wrap" v-if="workTotal > 0">
        <el-pagination
          v-model:current-page="workPage"
          :page-size="workLimit"
          :page-sizes="IMAGE_PAGE_SIZES"
          :total="workTotal"
          :disabled="loading || worksLoading"
          layout="total, sizes, prev, pager, next"
          @size-change="onWorkPageSizeChange"
          @current-change="fetchWorks"
        />
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'
import NavBar from '../components/NavBar.vue'
import ProfileWorksSection from '../components/profile/ProfileWorksSection.vue'
import ProfileHeader from '../components/profile/ProfileHeader.vue'
import ProfileBackgroundEditor from '../components/profile/ProfileBackgroundEditor.vue'
import ProfileAvatarEditor from '../components/profile/ProfileAvatarEditor.vue'
import TagInput from '../components/TagInput.vue'
import { useUserStore } from '../store/user'
import { getUserProfile, updateProfile, uploadAvatar, uploadBackground, checkField, deleteAccount, sendEmailChangeCode } from '../api/user'
import { getImageList, getUserPublicImages, deleteImage, updateImage } from '../api/image'
import { getUserMediaResourceStatus, refreshUserMediaAccessUrl } from '../api/resource'
import { confirmImageDelete } from '../utils/deleteConfirmation'
import { getCategoryList, createCategory } from '../api/category'
import { DEFAULT_IMAGE_PAGE_SIZE, IMAGE_PAGE_SIZES, buildImageListParams, getImageDownloadUrl } from '../utils/imageRequests'
import { userMediaToPollingResource } from '../utils/resourceAdapters'
import { hasSpecifiedUsers } from '../utils/visibility'
import { POLLING_INTERVALS, useResourcePolling } from '../composables/useResourcePolling'

// IndexedDB utility for caching original images (Data URLs can be >5MB)
const dbPromise = new Promise((resolve, reject) => {
  const req = indexedDB.open('picture_management_cache', 1)
  req.onupgradeneeded = (e) => {
    e.target.result.createObjectStore('images')
  }
  req.onsuccess = () => resolve(req.result)
  req.onerror = () => reject(req.error)
})

async function idbSet(key, val) {
  try {
    const db = await dbPromise
    const tx = db.transaction('images', 'readwrite')
    tx.objectStore('images').put(val, key)
    return new Promise(resolve => { tx.oncomplete = resolve })
  } catch {}
}

async function idbGet(key) {
  try {
    const db = await dbPromise
    const tx = db.transaction('images', 'readonly')
    const req = tx.objectStore('images').get(key)
    return new Promise(resolve => { req.onsuccess = () => resolve(req.result) })
  } catch { return null }
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const user = ref({})
const works = ref([])
const workPage = ref(1)
const workLimit = ref(DEFAULT_IMAGE_PAGE_SIZE)
const workTotal = ref(0)
const loading = ref(false)
const worksLoading = ref(false)
const worksError = ref(false)
const editing = ref(false)
const saving = ref(false)
const sendingEmailCode = ref(false)
const emailCodeCountdown = ref(0)
let emailCodeTimer = null
const isOwner = computed(() => !!(userStore.userInfo?.id && user.value.id && userStore.userInfo.id === user.value.id))
const categories = ref([])
const selectedWorkUuids = ref([])
const imageEditVisible = ref(false)
const workCategoryDialogVisible = ref(false)
const newWorkCategoryName = ref('')
const creatingWorkCategory = ref(false)
const workImageUuids = computed(() => works.value.map(img => img.uuid).filter(Boolean))
const allWorksSelected = computed(() => workImageUuids.value.length > 0 && workImageUuids.value.every(uuid => selectedWorkUuids.value.includes(uuid)))
const partiallyWorksSelected = computed(() => selectedWorkUuids.value.length > 0 && !allWorksSelected.value)
const joinedAt = computed(() => formatProfileDate(user.value.createTime || user.value.createdAt || user.value.joinTime || user.value.registerTime))

const showProfileMeta = computed(() => {
  if (editing.value) return false
  const hasContactInfo = isOwner.value && (user.value.email || user.value.phone)
  return Boolean(hasContactInfo || user.value.bio || joinedAt.value)
})

const BANNER_TONES = ['#d8d0c3', '#c5beb2', '#aab1ad', '#8d9a9b', '#596f79', '#31495f', '#aa6a58', '#ece6dc']
const bannerCells = ref([])
const animStats = reactive({
  works: 0,
  likes: 0,
})
const profileHeaderStats = computed(() => [
  { label: '累计创作', value: animStats.works },
  { label: '获赞', value: animStats.likes },
])

function initBannerCells() {
  bannerCells.value = Array.from({ length: 15 }, (_, index) => BANNER_TONES[index % BANNER_TONES.length])
}

function animateCounts() {
  const targets = {
    works: workTotal.value || works.value.length || 0,
    likes: Number(user.value.publicLikeCount || 0),
  }

  Object.keys(targets).forEach(key => {
    const target = targets[key]
    animStats[key] = 0
    if (target === 0) return
    const step = Math.ceil(target / 30)
    const t = setInterval(() => {
      animStats[key] = Math.min(animStats[key] + step, target)
      if (animStats[key] >= target) clearInterval(t)
    }, 40)
  })
}

const form = reactive({ displayName: '', email: '', emailCode: '', phone: '', bio: '' })
function updateProfileFormField({ field, value }) {
  if (Object.prototype.hasOwnProperty.call(form, field)) form[field] = value
}
const emailChanged = computed(() =>
  form.email.trim() !== String(user.value.email || '').trim()
)
const imageEditForm = reactive({
  uuid: '',
  imageName: '',
  categoryId: null,
  tags: '',
  description: '',
  visibility: 'PUBLIC',
  visibleUsernames: ''
})

function formatProfileDate(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 10)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function canonicalMediaUrl(url) {
  if (!url || typeof url !== 'string') return ''
  if (url.startsWith('data:') || url.startsWith('blob:')) return url
  return url.split('#')[0].split('?')[0]
}

function isSameMediaUrl(a, b) {
  const left = canonicalMediaUrl(a)
  const right = canonicalMediaUrl(b)
  return !!left && left === right
}

function isGifFile(file) {
  return !!file && (file.type === 'image/gif' || /\.gif$/i.test(file.name || ''))
}

function currentAvatarUrl() {
  return user.value.avatarUrl || user.value.avatar || ''
}

function currentBackgroundUrl() {
  return user.value.backgroundUrl || user.value.background || ''
}

const avatarDisplayUrl = computed(() => currentAvatarUrl())
const backgroundDisplayUrl = computed(() => currentBackgroundUrl())
const avatarPollingResource = computed(() => userMediaToPollingResource(user.value, 'avatar'))
const backgroundPollingResource = computed(() => userMediaToPollingResource(user.value, 'background'))

useResourcePolling({
  resource: avatarPollingResource,
  intervalMs: POLLING_INTERVALS.detail,
  enabled: computed(() => Boolean(user.value.uuid && currentAvatarUrl())),
  getStatus: async () => {
    const res = await getUserMediaResourceStatus(user.value.uuid, 'avatar')
    return res.data
  },
  getAccessUrl: async () => {
    const res = await refreshUserMediaAccessUrl(user.value.uuid, 'avatar')
    return res.data
  },
  onAccessUrl: (access) => {
    applyUserMediaAccessUrl('avatar', access.url)
  },
})

useResourcePolling({
  resource: backgroundPollingResource,
  intervalMs: POLLING_INTERVALS.detail,
  enabled: computed(() => Boolean(user.value.uuid && currentBackgroundUrl())),
  getStatus: async () => {
    const res = await getUserMediaResourceStatus(user.value.uuid, 'background')
    return res.data
  },
  getAccessUrl: async () => {
    const res = await refreshUserMediaAccessUrl(user.value.uuid, 'background')
    return res.data
  },
  onAccessUrl: (access) => {
    applyUserMediaAccessUrl('background', access.url)
  },
})

function applyUserMediaAccessUrl(kind, url) {
  if (!url) return
  if (kind === 'avatar') {
    user.value.avatar = url
    user.value.avatarUrl = url
    if (userStore.userInfo && userStore.userInfo.uuid === user.value.uuid) {
      userStore.userInfo.avatar = url
      userStore.userInfo.avatarUrl = url
    }
    return
  }
  user.value.background = url
  user.value.backgroundUrl = url
  if (userStore.userInfo && userStore.userInfo.uuid === user.value.uuid) {
    userStore.userInfo.background = url
    userStore.userInfo.backgroundUrl = url
  }
}

const bannerStyle = computed(() => {
  const bg = backgroundDisplayUrl.value
  if (!bg) return { backgroundColor: 'var(--color-night)' }
  if (bg.startsWith('#') || bg.startsWith('rgb')) return { backgroundColor: bg }
  return { backgroundImage: `url(${bg})`, backgroundSize: '100% auto', backgroundPosition: 'top' }
})

// Background editor
const bgDialogVisible = ref(false)
const bgPreviewUrl = ref('')
const bgFile = ref(null)
const bgFileName = ref('')
const bgSaving = ref(false)
const backgroundEditorRef = ref(null)
function backgroundCropElement() { return backgroundEditorRef.value?.cropContainer || null }
const bgCrop = reactive({ x: 0, y: 0, w: 320, h: 180 })
const bgCropRatio = ref(1)
const bgStageW = ref(640)
const bgStageH = ref(360)
const bgImgNatural = ref({ w: 1, h: 1 })
const bgSourceKind = ref('current')
const BG_ASPECT = 16 / 9
let bgDragging = false, bgResizing = false
let bgDragStart = { x: 0, y: 0, cx: 0, cy: 0, cw: 0, ch: 0 }
let bgResizeDir = ''

function onBgFileChange(file) {
  if (!file?.raw) return
  bgFile.value = file.raw
  bgFileName.value = file.name
  const reader = new FileReader()
  reader.onload = (e) => {
    bgSourceKind.value = 'new'
    loadBackgroundSource(e.target.result)
  }
  reader.readAsDataURL(file.raw)
}

watch(bgDialogVisible, (val) => { if (!val) teardownBgResizeObserver() })

onUnmounted(() => {
  teardownBgResizeObserver()
  if (emailCodeTimer) window.clearInterval(emailCodeTimer)
})

function openBackgroundEditor() {
  bgDialogVisible.value = true
  bgFile.value = null
  bgFileName.value = ''
  nextTick(() => { setupBgResizeObserver() })
  nextTick(async () => {
    const originalSource = await readCachedBgOriginal()
    const bg = currentBackgroundUrl()
    if (originalSource) {
      loadBackgroundSource(originalSource)
      bgSourceKind.value = 'original'
      return
    }
    bgSourceKind.value = 'current'
    if (bg && !bg.startsWith('#') && !bg.startsWith('rgb')) loadBackgroundSource(bg)
    else {
      bgPreviewUrl.value = ''
      bgImgNatural.value = { w: 1, h: 1 }
      updateBgCropBounds()
      initBgCrop()
    }
  })
}

let bgResizeObserver = null

function updateBgCropBounds() {
  const el = backgroundCropElement()
  if (!el) return
  bgStageW.value = el.clientWidth
  bgStageH.value = el.clientHeight
}

function setupBgResizeObserver() {
  const el = backgroundCropElement()
  if (!el || bgResizeObserver) return
  bgResizeObserver = new ResizeObserver(() => {
    updateBgCropBounds()
  })
  bgResizeObserver.observe(el)
}

function teardownBgResizeObserver() {
  if (bgResizeObserver) { bgResizeObserver.disconnect(); bgResizeObserver = null }
}

function initBgCrop() {
  updateBgCropBounds()
  bgCrop.x = bgStageW.value / 2
  bgCrop.y = bgStageH.value / 2
  bgCropRatio.value = 1
  syncBgCropSizeFromRatio()
}

function maxBgCropWidth() {
  return Math.min(bgStageW.value, bgStageH.value * BG_ASPECT)
}

function syncBgCropSizeFromRatio() {
  const maxW = maxBgCropWidth()
  bgCrop.w = Math.round(bgCropRatio.value * maxW)
  bgCrop.h = Math.round(bgCrop.w / BG_ASPECT)
  clampBgCrop()
}

function clampBgCrop() {
  const halfW = bgCrop.w / 2
  const halfH = bgCrop.h / 2
  bgCrop.x = Math.max(halfW, Math.min(bgStageW.value - halfW, bgCrop.x))
  bgCrop.y = Math.max(halfH, Math.min(bgStageH.value - halfH, bgCrop.y))
}

function onBgSliderChange() {
  syncBgCropSizeFromRatio()
}

const bgImageDisplay = computed(() => {
  const nw = bgImgNatural.value.w || 1
  const nh = bgImgNatural.value.h || 1
  const el = backgroundCropElement()
  const cw = (el ? el.clientWidth : bgStageW.value) || 1
  const ch = (el ? el.clientHeight : bgStageH.value) || 1
  const scale = Math.max(cw / nw, ch / nh)
  const width = nw * scale
  const height = nh * scale
  return {
    width,
    height,
    left: (cw - width) / 2,
    top: (ch - height) / 2
  }
})

const bgCropBox = computed(() => ({
  left: bgCrop.x - bgCrop.w / 2,
  top: bgCrop.y - bgCrop.h / 2,
  width: bgCrop.w,
  height: bgCrop.h
}))

const bgCropImgStyle = computed(() => {
  const display = bgImageDisplay.value
  return {
    width: `${display.width}px`,
    height: `${display.height}px`,
    left: `${display.left}px`,
    top: `${display.top}px`
  }
})

const bgCropFrameStyle = computed(() => ({
  left: `${bgCropBox.value.left}px`,
  top: `${bgCropBox.value.top}px`,
  width: `${bgCropBox.value.width}px`,
  height: `${bgCropBox.value.height}px`
}))

const bgCropGridStyle = computed(() => ({
  ...bgCropFrameStyle.value,
  backgroundImage: [
    'linear-gradient(to right, transparent 33.333%, rgba(255,255,255,0.55) 33.333%, rgba(255,255,255,0.55) calc(33.333% + 1px), transparent calc(33.333% + 1px))',
    'linear-gradient(to right, transparent 66.666%, rgba(255,255,255,0.55) 66.666%, rgba(255,255,255,0.55) calc(66.666% + 1px), transparent calc(66.666% + 1px))',
    'linear-gradient(to bottom, transparent 33.333%, rgba(255,255,255,0.55) 33.333%, rgba(255,255,255,0.55) calc(33.333% + 1px), transparent calc(33.333% + 1px))',
    'linear-gradient(to bottom, transparent 66.666%, rgba(255,255,255,0.55) 66.666%, rgba(255,255,255,0.55) calc(66.666% + 1px), transparent calc(66.666% + 1px))'
  ].join(', ')
}))

function loadBackgroundSource(source) {
  bgPreviewUrl.value = source || ''
  if (!source) return
  const img = new Image()
  img.onload = async () => {
    bgImgNatural.value = { w: img.naturalWidth, h: img.naturalHeight }
    await nextTick()
    updateBgCropBounds()
    initBgCrop()
  }
  img.onerror = () => {
    bgPreviewUrl.value = ''
    ElMessage.error('背景图片加载失败')
  }
  img.src = source
}

function startDragBgCrop(e) {
  if (!bgPreviewUrl.value || bgResizing) return
  if (e.target.classList.contains('bg-handle')) return
  bgDragging = true
  document.body.style.cursor = 'move'
  document.body.style.userSelect = 'none'
  bgDragStart = { x: e.clientX, y: e.clientY, cx: bgCrop.x, cy: bgCrop.y, cw: bgCrop.w, ch: bgCrop.h }
}

function onDragBgCrop(e) {
  if (!bgDragging) return
  bgCrop.x = bgDragStart.cx + e.clientX - bgDragStart.x
  bgCrop.y = bgDragStart.cy + e.clientY - bgDragStart.y
  clampBgCrop()
}

function stopDragBgCrop() {
  bgDragging = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

function bgHPos(dir) {
  const hw = bgCrop.w / 2
  const hh = bgCrop.h / 2
  const map = {
    top:    { left: bgCrop.x + 'px', top: (bgCrop.y - hh) + 'px' },
    bottom: { left: bgCrop.x + 'px', top: (bgCrop.y + hh) + 'px' },
    left:   { left: (bgCrop.x - hw) + 'px', top: bgCrop.y + 'px' },
    right:  { left: (bgCrop.x + hw) + 'px', top: bgCrop.y + 'px' },
    tl:     { left: (bgCrop.x - hw) + 'px', top: (bgCrop.y - hh) + 'px' },
    tr:     { left: (bgCrop.x + hw) + 'px', top: (bgCrop.y - hh) + 'px' },
    bl:     { left: (bgCrop.x - hw) + 'px', top: (bgCrop.y + hh) + 'px' },
    br:     { left: (bgCrop.x + hw) + 'px', top: (bgCrop.y + hh) + 'px' },
  }
  return map[dir]
}

const bgCursorMap = { top: 'ns-resize', bottom: 'ns-resize', left: 'ew-resize', right: 'ew-resize', tl: 'nwse-resize', br: 'nwse-resize', tr: 'nesw-resize', bl: 'nesw-resize' }

function startBgResize(e, dir) {
  bgResizing = true
  bgResizeDir = dir
  bgDragStart = { x: e.clientX, y: e.clientY, cx: bgCrop.x, cy: bgCrop.y, cw: bgCrop.w, ch: bgCrop.h }
  document.body.style.cursor = bgCursorMap[dir]
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onBgResize)
  window.addEventListener('mouseup', stopBgResize)
}

function onBgResize(e) {
  const dx = e.clientX - bgDragStart.x
  const dy = e.clientY - bgDragStart.y
  let newW = bgDragStart.cw
  let newX = bgDragStart.cx
  let newY = bgDragStart.cy

  const fromLeft = ['left', 'tl', 'bl'].includes(bgResizeDir)
  const fromRight = ['right', 'tr', 'br'].includes(bgResizeDir)
  const fromTop = ['top', 'tl', 'tr'].includes(bgResizeDir)
  const fromBottom = ['bottom', 'bl', 'br'].includes(bgResizeDir)

  if (fromRight) newW = bgDragStart.cw + dx * 2
  if (fromLeft) newW = bgDragStart.cw - dx * 2
  // Also check vertical resize for corner handles
  if (fromBottom) newW = Math.max(newW, bgDragStart.cw + dy * 2 * BG_ASPECT)
  if (fromTop) newW = Math.max(newW, bgDragStart.cw - dy * 2 * BG_ASPECT)

  const maxW = maxBgCropWidth()
  newW = Math.max(120, Math.min(maxW, newW))
  const newH = Math.round(newW / BG_ASPECT)

  bgCrop.w = newW
  bgCrop.h = newH
  bgCropRatio.value = Math.round(newW / maxBgCropWidth() * 100) / 100

  if (fromLeft) newX = bgDragStart.cx + (bgDragStart.cw - newW) / 2
  if (fromRight) newX = bgDragStart.cx - (bgDragStart.cw - newW) / 2
  if (fromTop) newY = bgDragStart.cy + (bgDragStart.ch - newH) / 2
  if (fromBottom) newY = bgDragStart.cy - (bgDragStart.ch - newH) / 2

  bgCrop.x = Math.max(newW / 2, Math.min(bgStageW.value - newW / 2, newX))
  bgCrop.y = Math.max(newH / 2, Math.min(bgStageH.value - newH / 2, newY))
}

function stopBgResize() {
  bgResizing = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  window.removeEventListener('mousemove', onBgResize)
  window.removeEventListener('mouseup', stopBgResize)
}

// Mini profile card preview
const miniBannerPreviewStyle = computed(() => {
  if (!bgPreviewUrl.value) {
    return { background: !currentBackgroundUrl()
      ? '#1a1a1a' : undefined }
  }
  const display = bgImageDisplay.value
  const scale = 200 / bgCrop.w
  const x = (display.left - bgCropBox.value.left) * scale
  const y = (display.top - bgCropBox.value.top) * scale
  return {
    backgroundImage: `url(${bgPreviewUrl.value})`,
    backgroundSize: `${display.width * scale}px ${display.height * scale}px`,
    backgroundPosition: `${x}px ${y}px`,
    backgroundRepeat: 'no-repeat'
  }
})

function bgCacheKey() {
  const id = user.value.id || userStore.userInfo?.id
  return id ? `bg-original:${id}` : ''
}

async function readCachedBgOriginal() {
  const currentBg = currentBackgroundUrl()
  const key = bgCacheKey()
  if (key) {
    const entry = _originalStore.get(key)
    if (entry && isSameMediaUrl(entry.background, currentBg) && entry.source) {
      return entry.source
    }
  }
  if (key) {
    const cached = await idbGet(key)
    if (cached && isSameMediaUrl(cached.background, currentBg) && cached.source) {
      _originalStore.set(key, cached)
      return cached.source
    }
  }
  return ''
}

function writeCachedBgOriginal(bgPath, source) {
  if (!bgPath || !source) return
  const entry = { background: canonicalMediaUrl(bgPath), source }
  const key = bgCacheKey()
  if (key) {
    _originalStore.set(key, entry)
    idbSet(key, entry)
  }
}

async function saveBackground() {
  if (!bgPreviewUrl.value) return
  bgSaving.value = true
  try {
    let originalSource = bgSourceKind.value === 'new' || bgSourceKind.value === 'original'
      ? bgPreviewUrl.value
      : await readCachedBgOriginal()
    if (!originalSource) originalSource = currentBackgroundUrl()
    const backgroundBlob = isGifFile(bgFile.value) ? bgFile.value : await cropBackgroundImage()
    const fd = new FormData()
    fd.append('file', backgroundBlob, isGifFile(bgFile.value) ? 'background.gif' : 'background.jpg')
    const res = await uploadBackground(fd)
    const bgPath = typeof res.data === 'string' ? res.data : res.data?.background
    if (!bgPath) throw new Error('背景上传失败')
    user.value.background = bgPath
    user.value.backgroundUrl = bgPath
    if (userStore.userInfo) {
      userStore.userInfo.background = bgPath
      userStore.userInfo.backgroundUrl = bgPath
    }
    if (originalSource) writeCachedBgOriginal(bgPath, originalSource)
    ElMessage.success('背景已更新'); bgDialogVisible.value = false
  } catch {} finally { bgSaving.value = false }
}

function cropBackgroundImage() {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      const nw = bgImgNatural.value.w || img.naturalWidth
      const nh = bgImgNatural.value.h || img.naturalHeight
      const el = backgroundCropElement()
      const cw = el ? el.clientWidth : bgStageW.value
      const ch = el ? el.clientHeight : bgStageH.value
      const scale = Math.max(cw / nw, ch / nh)
      const dw = nw * scale
      const dh = nh * scale
      const dl = (cw - dw) / 2
      const dt = (ch - dh) / 2
      const sx = (bgCropBox.value.left - dl) / dw * nw
      const sy = (bgCropBox.value.top - dt) / dh * nh
      const sw = bgCrop.w / dw * nw
      const sh = bgCrop.h / dh * nh
      const canvas = document.createElement('canvas')
      canvas.width = 3840
      canvas.height = 2160
      const ctx = canvas.getContext('2d')
      ctx.imageSmoothingEnabled = true
      ctx.imageSmoothingQuality = 'high'
      ctx.drawImage(img, sx, sy, sw, sh, 0, 0, canvas.width, canvas.height)
      canvas.toBlob(blob => {
        if (blob) resolve(blob)
        else reject(new Error('Canvas toBlob failed'))
      }, 'image/jpeg', 0.92)
    }
    img.onerror = reject
    img.src = bgPreviewUrl.value
  })
}

// Avatar upload state stays in the page; the private editor owns only crop interaction.
const avatarDialogVisible = ref(false)
const avatarEditorSourceUrl = ref('')
const avatarSaving = ref(false)

// Module-level cache — survives component remount during SPA navigation.
// sessionStorage is too small for Data URLs (images often >5 MB).
const _originalStore = new Map() // userId -> { avatarPath, source }

function avatarCacheKey() {
  const id = user.value.id || userStore.userInfo?.id
  return id ? `avatar-original:${id}` : ''
}

async function readCachedAvatarOriginal() {
  const currentAvatar = currentAvatarUrl()
  const key = avatarCacheKey()
  if (key) {
    const entry = _originalStore.get(key)
    if (entry && isSameMediaUrl(entry.avatar, currentAvatar) && entry.source) {
      return entry.source
    }
  }
  if (key) {
    const cached = await idbGet(key)
    if (cached && isSameMediaUrl(cached.avatar, currentAvatar) && cached.source) {
      _originalStore.set(key, cached)
      return cached.source
    }
  }
  return ''
}

function writeCachedAvatarOriginal(avatarPath, source) {
  if (!avatarPath || !source) return
  const entry = { avatar: canonicalMediaUrl(avatarPath), source }
  // Always update the module-level cache first
  const key = avatarCacheKey()
  if (key) {
    _originalStore.set(key, entry)
    idbSet(key, entry)
  }
}

function openAvatarEditor() {
  avatarEditorSourceUrl.value = currentAvatarUrl()
  avatarDialogVisible.value = true
  nextTick(async () => {
    const originalSource = await readCachedAvatarOriginal()
    if (avatarDialogVisible.value && originalSource) avatarEditorSourceUrl.value = originalSource
  })
}

async function submitAvatar({ blob, filename, sourceUrl }) {
  avatarSaving.value = true
  try {
    const fd = new FormData()
    fd.append('file', blob, filename)
    const res = await uploadAvatar(fd)
    const avatarPath = typeof res.data === 'string' ? res.data : res.data?.avatar
    if (!avatarPath) throw new Error('头像上传失败')
    user.value.avatar = avatarPath
    user.value.avatarUrl = avatarPath
    if (userStore.userInfo) {
      userStore.userInfo.avatar = avatarPath
      userStore.userInfo.avatarUrl = avatarPath
    }
    const originalSource = sourceUrl || avatarEditorSourceUrl.value || currentAvatarUrl()
    if (originalSource) writeCachedAvatarOriginal(avatarPath, originalSource)
    ElMessage.success('头像已更新'); avatarDialogVisible.value = false
  } catch (error) {
    if (error?.message === '头像上传失败') ElMessage.error(error.message)
  } finally { avatarSaving.value = false }
}

onMounted(() => {
  initBannerCells()
  loadProfile()
})

watch(() => route.params.uuid, () => {
  initBannerCells()
  loadProfile()
})

async function loadProfile() {
  loading.value = true
  try {
    if (userStore.token && !userStore.userInfo) {
      await userStore.fetchUserInfo()
    }
    const profileId = route.params.uuid || userStore.userInfo?.uuid || userStore.userInfo?.id
    workPage.value = 1
    selectedWorkUuids.value = []
    works.value = []
    editing.value = false
    const res = await getUserProfile(profileId)
    user.value = res.data
    fetchWorks()
    if (isOwner.value) fetchCategories()
  } catch {} finally { loading.value = false }
}

async function fetchWorks() {
  worksLoading.value = true
  worksError.value = false
  try {
    const params = buildImageListParams({
      page: workPage.value,
      limit: workLimit.value,
      sortField: 'upload_time',
      sortOrder: 'desc'
    })
    const res = isOwner.value
      ? await getImageList(params)
      : await getUserPublicImages(user.value.uuid || route.params.uuid, params)
    works.value = res.data.records || []; workTotal.value = res.data.total || 0
    selectedWorkUuids.value = selectedWorkUuids.value.filter(uuid => works.value.some(img => img.uuid === uuid))
    
    animateCounts()
  } catch {
    worksError.value = true
  } finally {
    worksLoading.value = false
  }
}

async function fetchCategories() {
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
  } catch {}
}

function onWorkPageSizeChange(size) {
  workLimit.value = size
  workPage.value = 1
  fetchWorks()
}

function toggleWorkSelection(uuid) {
  if (!uuid) return
  selectedWorkUuids.value = selectedWorkUuids.value.includes(uuid)
    ? selectedWorkUuids.value.filter(item => item !== uuid)
    : [...selectedWorkUuids.value, uuid]
}

function toggleSelectAllWorks(checked) {
  selectedWorkUuids.value = checked ? [...workImageUuids.value] : []
}

function clearWorkSelection() {
  selectedWorkUuids.value = []
}

async function handleBatchWorkDelete() {
  const uuids = [...selectedWorkUuids.value]
  if (uuids.length === 0) return
  try {
    await ElMessageBox.confirm(
      `删除后无法恢复。确定删除选中的 ${uuids.length} 张图片吗？`,
      '批量删除图片',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }
  try {
    await Promise.all(uuids.map(uuid => deleteImage(uuid)))
    ElMessage.success(`已删除 ${uuids.length} 张图片`)
    selectedWorkUuids.value = []
    fetchWorks()
  } catch {}
}

async function handleWorkDelete(image) {
  const uuid = typeof image === 'object' ? image?.uuid : image
  if (!uuid || !(await confirmImageDelete(image))) return
  try {
    await deleteImage(uuid)
    selectedWorkUuids.value = selectedWorkUuids.value.filter(item => item !== uuid)
    ElMessage.success('删除成功')
    fetchWorks()
  } catch {}
}

async function copyWorkLink(img) {
  const url = getImageDownloadUrl(img)
  if (!url) {
    ElMessage.warning('暂时没有可复制的分享链接')
    return
  }
  try {
    await navigator.clipboard.writeText(new URL(url, window.location.origin).href)
    ElMessage.success('分享链接已复制')
  } catch {
    ElMessage.warning('无法自动复制，请手动复制分享链接')
  }
}

function handleWorkEdit(img) {
  imageEditForm.uuid = img.uuid
  imageEditForm.imageName = img.imageName
  imageEditForm.categoryId = img.categoryId
  imageEditForm.description = img.description || ''
  imageEditForm.tags = img.tags || ''
  imageEditForm.visibility = img.visibility || 'PUBLIC'
  imageEditForm.visibleUsernames = img.visibleUsernames || ''
  imageEditVisible.value = true
  if (categories.value.length === 0) fetchCategories()
}

async function saveWorkEdit() {
  if (!imageEditForm.uuid) return
  if (imageEditForm.visibility === 'SPECIFIED' && !hasSpecifiedUsers(imageEditForm.visibleUsernames)) {
    ElMessage.warning('请先填写指定用户')
    return
  }
  try {
    await updateImage(imageEditForm.uuid, {
      imageName: imageEditForm.imageName,
      categoryId: imageEditForm.categoryId,
      description: imageEditForm.description,
      tags: imageEditForm.tags,
      visibility: imageEditForm.visibility,
      visibleUsernames: imageEditForm.visibility === 'SPECIFIED' ? imageEditForm.visibleUsernames : ''
    })
    ElMessage.success('更新成功')
    imageEditVisible.value = false
    fetchWorks()
  } catch {}
}

function openWorkCreateCategory() {
  newWorkCategoryName.value = ''
  workCategoryDialogVisible.value = true
}

async function submitWorkCategory() {
  const name = newWorkCategoryName.value.trim()
  if (!name) {
    ElMessage.warning('请输入分类名')
    return
  }
  creatingWorkCategory.value = true
  try {
    const res = await createCategory(name)
    const category = res.data
    categories.value = categories.value.filter(cat => cat.id !== category.id)
    categories.value.push(category)
    imageEditForm.categoryId = category.id
    workCategoryDialogVisible.value = false
    ElMessage.success('分类创建成功')
  } catch {} finally {
    creatingWorkCategory.value = false
  }
}

const fieldErrors = reactive({ email: '', phone: '' })

async function onEmailBlur() {
  fieldErrors.email = ''
  if (!form.email || !form.email.trim()) return
  try {
    await checkField('email', form.email.trim(), user.value.id)
  } catch {
    fieldErrors.email = '该邮箱已被其他用户使用'
  }
}

async function onPhoneBlur() {
  fieldErrors.phone = ''
  if (!form.phone || !form.phone.trim()) return
  try {
    await checkField('phone', form.phone.trim(), user.value.id)
  } catch {
    fieldErrors.phone = '该手机号已被其他用户使用'
  }
}

function startEdit() {
  form.displayName = user.value.displayName || ''
  form.email = user.value.email || ''; form.emailCode = ''; form.phone = user.value.phone || ''; form.bio = user.value.bio || ''
  fieldErrors.email = ''; fieldErrors.phone = ''
  editing.value = true
}
function cancelEdit() { editing.value = false }

async function handleSendEmailChangeCode() {
  await onEmailBlur()
  if (fieldErrors.email || !form.email.trim()) return
  sendingEmailCode.value = true
  try {
    await sendEmailChangeCode(form.email.trim())
    ElMessage.success('验证码已发送到新邮箱')
    emailCodeCountdown.value = 60
    if (emailCodeTimer) window.clearInterval(emailCodeTimer)
    emailCodeTimer = window.setInterval(() => {
      emailCodeCountdown.value -= 1
      if (emailCodeCountdown.value <= 0) {
        window.clearInterval(emailCodeTimer)
        emailCodeTimer = null
      }
    }, 1000)
  } finally {
    sendingEmailCode.value = false
  }
}

async function handleDeleteAccount() {
  try {
    await ElMessageBox.confirm(
      '注销后您的个人信息将被清除，但已上传的图片将继续保留。此操作不可撤销，确定继续吗？',
      '确认注销账号',
      { confirmButtonText: '确认注销', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }
  try {
    await deleteAccount()
    userStore.clearToken()
    ElMessage.success('账号已注销')
    router.push('/login')
  } catch {}
}

async function saveProfile() {
  saving.value = true
  try {
    const res = await updateProfile({
      displayName: form.displayName,
      email: form.email,
      emailCode: form.emailCode,
      phone: form.phone,
      bio: form.bio
    })
    user.value = res.data
    if (isOwner.value) userStore.userInfo = res.data
    editing.value = false; ElMessage.success('资料已更新')
  } catch {} finally { saving.value = false }
}
</script>

<style scoped>
.profile-page { min-height: 100vh; background: var(--color-canvas); color: var(--color-text-primary); }
.profile-container { width: min(calc(100% - (2 * var(--page-gutter))), var(--page-wide)); padding: 96px 0 112px; }
.profile-banner { position: relative; min-height: 220px; overflow: hidden; border: 1px solid var(--color-border-subtle); border-bottom: 0; background-color: var(--color-night); background-position: center; background-size: cover; }
.banner-grid { position: absolute; inset: 0; display: grid; grid-template-columns: repeat(5,1fr); grid-template-rows: repeat(3,1fr); gap: 1px; }.banner-cell { opacity: .82; }.banner-overlay { position: absolute; inset: 0; background: rgba(14,18,22,.12); }.banner-edit { position: absolute; z-index: 2; top: var(--space-4); right: var(--space-4); opacity: .64; transition: opacity var(--duration-fast) var(--ease-standard); }.banner-edit--active { opacity: 1; }.banner-edit :deep(.el-button) { min-height: var(--control-height-md); border-color: var(--color-border-subtle); background: rgba(248,245,238,.9); color: var(--color-text-primary); box-shadow: none; }
.profile-container :deep(.profile-header),.profile-container :deep(.user-works) { border-color: var(--color-border-subtle); }.profile-container :deep(.user-works) { border-top: 0; }
.category-row { display: grid; width: 100%; grid-template-columns: 1fr auto; gap: var(--space-2); }.category-row :deep(.el-select) { width: 100%; }
.pagination-wrap { position: fixed; z-index: var(--layer-floating); right: 0; bottom: 0; left: 0; display: flex; justify-content: center; padding: var(--space-3) var(--space-4) calc(var(--space-3) + env(safe-area-inset-bottom)); border-top: 1px solid var(--color-border-subtle); background: rgba(248,245,238,.96); }.pagination-wrap :deep(.el-pagination) { max-width: 100%; flex-wrap: wrap; justify-content: center; gap: var(--space-1); }
@media (max-width:820px) { .profile-container { padding-top: 82px; }.profile-banner { min-height: 180px; } }
@media (max-width:520px) { .profile-container { width: calc(100% - (2 * var(--page-gutter))); }.profile-banner { min-height: 150px; }.banner-grid { grid-template-columns: repeat(3,1fr); grid-template-rows: repeat(5,1fr); }.banner-edit :deep(.el-button) { min-height: 44px; }.pagination-wrap { padding-inline: var(--space-2); }.pagination-wrap :deep(.el-pagination__total),.pagination-wrap :deep(.el-pagination__sizes) { display: none; } }
</style>
