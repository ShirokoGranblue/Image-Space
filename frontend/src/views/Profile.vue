<template>
  <div class="profile-page">
    <NavBar />
    <div class="page-container" v-loading="loading">
      <div class="profile-banner" :style="bannerStyle">
        <div class="banner-edit" v-if="isOwner">
          <el-button size="small" @click="openBackgroundEditor">
            <el-icon><Edit /></el-icon> 编辑背景
          </el-button>
        </div>
      </div>

      <div class="profile-header">
        <div class="avatar-wrap">
          <el-avatar :size="120" :src="avatarDisplayUrl" class="avatar">
            <el-icon :size="48"><UserFilled /></el-icon>
          </el-avatar>
          <div v-if="isOwner" class="avatar-upload" @click="openAvatarEditor">
            <el-button size="small" circle><el-icon><Camera /></el-icon></el-button>
          </div>
        </div>

        <template v-if="!editing">
          <div class="profile-name-row">
            <h2>{{ user.displayName || user.username }}</h2>
            <el-dropdown v-if="isOwner" trigger="click">
              <span class="dropdown-trigger">···</span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="startEdit">编辑资料</el-dropdown-item>
                  <el-dropdown-item @click="handleDeleteAccount">注销账号</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>

        <div class="profile-edit" v-else>
          <el-form label-width="80px">
            <el-form-item label="展示名称">
              <el-input v-model="form.displayName" maxlength="50" />
            </el-form-item>
            <el-form-item label="邮箱" :error="fieldErrors.email">
              <el-input v-model="form.email" @blur="onEmailBlur" />
            </el-form-item>
            <el-form-item label="手机号" :error="fieldErrors.phone">
              <el-input v-model="form.phone" maxlength="20" @blur="onPhoneBlur" />
            </el-form-item>
            <el-form-item label="个人介绍">
              <el-input v-model="form.bio" type="textarea" :rows="3" maxlength="200" show-word-limit />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveProfile" :loading="saving">保存</el-button>
              <el-button @click="cancelEdit">取消</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="profile-meta" v-if="!editing && (user.email || user.phone || user.bio)">
          <p v-if="user.bio" class="bio">{{ user.bio }}</p>
          <div class="contact">
            <span v-if="user.email"><el-icon><Message /></el-icon> {{ user.email }}</span>
            <span v-if="user.phone"><el-icon><Phone /></el-icon> {{ user.phone }}</span>
          </div>
        </div>
      </div>

      <div class="user-works">
        <div class="works-heading">
          <h3>作品</h3>
          <div v-if="isOwner && works.length > 0" class="works-actions">
            <el-checkbox
              :model-value="allWorksSelected"
              :indeterminate="partiallyWorksSelected"
              @change="toggleSelectAllWorks"
            >
              全选本页
            </el-checkbox>
            <el-button v-if="selectedWorkUuids.length > 0" @click="clearWorkSelection">取消选择</el-button>
            <el-button
              v-if="selectedWorkUuids.length > 0"
              type="danger"
              @click="handleBatchWorkDelete"
            >
              删除选中 {{ selectedWorkUuids.length }}
            </el-button>
          </div>
        </div>
        <div v-if="works.length === 0" class="empty-state"><p>暂无作品</p></div>
        <div v-else class="card-grid">
          <ImageCard
            v-for="img in works"
            :key="img.uuid || img.id"
            :image="img"
            :show-actions="isOwner"
            :selectable="isOwner"
            :selected="selectedWorkUuids.includes(img.uuid)"
            @delete="handleWorkDelete"
            @edit="handleWorkEdit"
            @toggle-select="toggleWorkSelection"
          />
        </div>
      </div>
    </div>

    <Teleport to="body">
      <div class="pagination-wrap" v-if="workTotal > 0">
        <el-pagination
          v-model:current-page="workPage"
          :page-size="workLimit"
          :page-sizes="IMAGE_PAGE_SIZES"
          :total="workTotal"
          :disabled="loading"
          layout="total, sizes, prev, pager, next"
          @size-change="onWorkPageSizeChange"
          @current-change="fetchWorks"
        />
      </div>
    </Teleport>

    <!-- Background editor dialog -->
    <el-dialog v-model="bgDialogVisible" title="编辑个人背景" width="860px" class="profile-dialog bg-dialog">
      <div class="background-editor">
        <div class="background-editor-layout">
          <div class="bg-crop-side">
            <div
              class="bg-crop-container"
              ref="bgCropContainer"
              @mousedown="startDragBgCrop"
              @mousemove="onDragBgCrop"
              @mouseup="stopDragBgCrop"
              @mouseleave="stopDragBgCrop"
            >
              <img v-if="bgPreviewUrl" :src="bgPreviewUrl" class="bg-crop-img" :style="bgCropImgStyle" draggable="false" />
              <el-icon v-else :size="72" class="bg-placeholder"><PictureFilled /></el-icon>
              <div class="bg-crop-frame" v-if="bgPreviewUrl" :style="bgCropFrameStyle"></div>
              <div class="bg-crop-grid" v-if="bgPreviewUrl" :style="bgCropGridStyle"></div>
              <template v-if="bgPreviewUrl">
                <div class="bg-handle bg-handle-ns" :style="bgHPos('top')" @mousedown.stop="startBgResize($event, 'top')"></div>
                <div class="bg-handle bg-handle-ns" :style="bgHPos('bottom')" @mousedown.stop="startBgResize($event, 'bottom')"></div>
                <div class="bg-handle bg-handle-ew" :style="bgHPos('left')" @mousedown.stop="startBgResize($event, 'left')"></div>
                <div class="bg-handle bg-handle-ew" :style="bgHPos('right')" @mousedown.stop="startBgResize($event, 'right')"></div>
                <div class="bg-handle bg-handle-corner bg-handle-nwse" :style="bgHPos('tl')" @mousedown.stop="startBgResize($event, 'tl')"></div>
                <div class="bg-handle bg-handle-corner bg-handle-nesw" :style="bgHPos('tr')" @mousedown.stop="startBgResize($event, 'tr')"></div>
                <div class="bg-handle bg-handle-corner bg-handle-nesw" :style="bgHPos('bl')" @mousedown.stop="startBgResize($event, 'bl')"></div>
                <div class="bg-handle bg-handle-corner bg-handle-nwse" :style="bgHPos('br')" @mousedown.stop="startBgResize($event, 'br')"></div>
              </template>
            </div>
            <div class="bg-controls">
              <span class="slider-label">裁剪尺寸</span>
              <el-slider v-model="bgCropRatio" :min="0.45" :max="1" :step="0.01" style="flex:1;margin:0 10px" @input="onBgSliderChange" />
              <span class="slider-val">{{ Math.round(bgCropRatio * 100) }}%</span>
            </div>
            <el-upload :auto-upload="false" :show-file-list="false" :on-change="onBgFileChange" accept="image/jpeg,image/png,image/webp,image/gif" class="bg-upload">
              <el-button type="primary">选择图片</el-button>
            </el-upload>
            <p class="upload-hint" v-if="bgFileName">{{ bgFileName }}</p>
          </div>
          <div class="bg-preview-side">
            <p class="preview-label">预览</p>
            <div class="profile-mini-card">
              <div class="profile-mini-banner" :style="miniBannerPreviewStyle" />
              <div class="profile-mini-header">
                <div class="profile-mini-avatar">
                  <el-avatar :size="22" :src="avatarDisplayUrl">
                    <el-icon :size="10"><UserFilled /></el-icon>
                  </el-avatar>
                </div>
                <div class="profile-mini-name">{{ user.displayName || user.username }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="bgDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveBackground" :loading="bgSaving" :disabled="!bgPreviewUrl">应用</el-button>
      </template>
    </el-dialog>

    <!-- Avatar editor dialog -->
    <el-dialog v-model="avatarDialogVisible" title="编辑头像" width="760px" class="profile-dialog avatar-dialog">
      <div class="avatar-editor">
        <div class="avatar-editor-layout">
          <div class="avatar-crop-side">
            <div
              class="crop-container"
              ref="cropContainer"
              @mousedown="startDragCrop"
              @mousemove="onDragCrop"
              @mouseup="stopDragCrop"
              @mouseleave="stopDragCrop"
            >
              <img v-if="avatarPreviewUrl" :src="avatarPreviewUrl" class="crop-img" :style="cropImgStyle" draggable="false" />
              <el-icon v-else :size="80" style="position:absolute;inset:0;margin:auto;color:#ccc"><UserFilled /></el-icon>
              <div class="crop-frame" v-if="avatarPreviewUrl" :style="cropFrameStyle"></div>
              <div class="crop-grid" v-if="avatarPreviewUrl" :style="cropGridStyle"></div>
              <template v-if="avatarPreviewUrl">
                <div class="crop-handle crop-handle-ns" :style="hPos('top')" @mousedown.stop="startResize($event, 'top')"></div>
                <div class="crop-handle crop-handle-ns" :style="hPos('bottom')" @mousedown.stop="startResize($event, 'bottom')"></div>
                <div class="crop-handle crop-handle-ew" :style="hPos('left')" @mousedown.stop="startResize($event, 'left')"></div>
                <div class="crop-handle crop-handle-ew" :style="hPos('right')" @mousedown.stop="startResize($event, 'right')"></div>
                <div class="crop-handle crop-handle-corner crop-handle-nwse" :style="hPos('tl')" @mousedown.stop="startResize($event, 'tl')"></div>
                <div class="crop-handle crop-handle-corner crop-handle-nesw" :style="hPos('tr')" @mousedown.stop="startResize($event, 'tr')"></div>
                <div class="crop-handle crop-handle-corner crop-handle-nesw" :style="hPos('bl')" @mousedown.stop="startResize($event, 'bl')"></div>
                <div class="crop-handle crop-handle-corner crop-handle-nwse" :style="hPos('br')" @mousedown.stop="startResize($event, 'br')"></div>
              </template>
            </div>
            <div class="crop-controls">
              <span class="slider-label">裁剪尺寸</span>
              <el-slider v-model="cropRatio" :min="0.25" :max="1" :step="0.01" style="flex:1;margin:0 10px" @input="onSliderChange" />
              <span class="slider-val">{{ Math.round(cropRatio * 100) }}%</span>
            </div>
          </div>
          <div class="avatar-preview-side">
            <p class="preview-label">头像预览</p>
            <div class="preview-circle-lg">
              <img v-if="avatarPreviewUrl" :src="avatarPreviewUrl" class="preview-img" :style="previewLgImgStyle" />
              <el-icon v-else :size="48" style="color:#ccc"><UserFilled /></el-icon>
            </div>
            <div class="preview-circle-sm">
              <img v-if="avatarPreviewUrl" :src="avatarPreviewUrl" class="preview-img" :style="previewSmImgStyle" />
              <el-icon v-else :size="24" style="color:#ccc"><UserFilled /></el-icon>
            </div>
          </div>
        </div>
        <el-upload :auto-upload="false" :show-file-list="false" :on-change="onAvatarFileChange" accept="image/jpeg,image/png,image/webp,image/gif" class="avatar-replace-upload">
          <el-button>更换图片</el-button>
        </el-upload>
      </div>
      <template #footer>
        <el-button @click="avatarDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAvatar" :loading="avatarSaving" :disabled="!avatarPreviewUrl">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="imageEditVisible" title="编辑图片信息" width="480px">
      <el-form :model="imageEditForm" label-width="86px" v-if="imageEditForm.uuid">
        <el-form-item label="图片名称">
          <el-input v-model="imageEditForm.imageName" />
        </el-form-item>
        <el-form-item label="分类">
          <div class="category-row">
            <el-select
              v-model="imageEditForm.categoryId"
              placeholder="选择分类"
              clearable
              filterable
              style="width: 100%"
            >
              <el-option v-for="cat in categories" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
            </el-select>
            <el-button @click="openWorkCreateCategory">新建分类</el-button>
          </div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="imageEditForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="标签">
          <TagInput v-model="imageEditForm.tags" placeholder="多个标签用 # 分隔" />
        </el-form-item>
        <el-form-item label="可见权限">
          <el-select v-model="imageEditForm.visibility" style="width: 100%">
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

    <el-dialog v-model="workCategoryDialogVisible" title="新建分类" width="360px">
      <el-form label-width="70px" @submit.prevent>
        <el-form-item label="分类名">
          <el-input v-model="newWorkCategoryName" maxlength="20" show-word-limit @keyup.enter="submitWorkCategory" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="workCategoryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creatingWorkCategory" @click="submitWorkCategory">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import NavBar from '../components/NavBar.vue'
import ImageCard from '../components/ImageCard.vue'
import TagInput from '../components/TagInput.vue'
import { useUserStore } from '../store/user'
import { getUserProfile, updateProfile, uploadAvatar, uploadBackground, checkField, deleteAccount } from '../api/user'
import { getImageList, getUserPublicImages, deleteImage, updateImage } from '../api/image'
import { getUserMediaResourceStatus, refreshUserMediaAccessUrl } from '../api/resource'
import { getCategoryList, createCategory } from '../api/category'
import { DEFAULT_IMAGE_PAGE_SIZE, IMAGE_PAGE_SIZES, buildImageListParams } from '../utils/imageRequests'
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
const editing = ref(false)
const saving = ref(false)
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

const form = reactive({ displayName: '', email: '', phone: '', bio: '' })
const imageEditForm = reactive({
  uuid: '',
  imageName: '',
  categoryId: null,
  tags: '',
  description: '',
  visibility: 'PUBLIC',
  visibleUsernames: ''
})

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
  if (!bg) return { background: '#1a1a1a' }
  if (bg.startsWith('#') || bg.startsWith('rgb')) return { backgroundColor: bg }
  return { backgroundImage: `url(${bg})`, backgroundSize: '100% auto', backgroundPosition: 'top' }
})

// Background editor
const bgDialogVisible = ref(false)
const bgPreviewUrl = ref('')
const bgFile = ref(null)
const bgFileName = ref('')
const bgSaving = ref(false)
const bgCropContainer = ref(null)
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
  const el = bgCropContainer.value
  if (!el) return
  bgStageW.value = el.clientWidth
  bgStageH.value = el.clientHeight
}

function setupBgResizeObserver() {
  const el = bgCropContainer.value
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
  const el = bgCropContainer.value
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

const bgPreviewImgStyle = computed(() => {
  if (!bgPreviewUrl.value) return { display: 'none' }
  const previewW = 240
  const scale = previewW / bgCrop.w
  const display = bgImageDisplay.value
  return {
    width: `${display.width * scale}px`,
    height: `${display.height * scale}px`,
    left: `${(display.left - bgCropBox.value.left) * scale}px`,
    top: `${(display.top - bgCropBox.value.top) * scale}px`
  }
})

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
      const el = bgCropContainer.value
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

// Avatar cropper
const avatarDialogVisible = ref(false)
const avatarPreviewUrl = ref('')
const avatarFile = ref(null)
const avatarSaving = ref(false)
const cropContainer = ref(null)
const avatarSourceKind = ref('current')

// Module-level cache — survives component remount during SPA navigation.
// sessionStorage is too small for Data URLs (images often >5 MB).
const _originalStore = new Map() // userId -> { avatarPath, source }

// Crop state
const crop = reactive({ x: 0, y: 0, size: 200 }) // square crop center and size
const cropRatio = ref(0.8) // crop size as fraction of container (0.25–1.0)
const imgW = ref(400); const imgH = ref(400) // container size
const imgNatural = ref({ w: 1, h: 1 }) // original image dimensions

let dragging = false, resizing = false
let dragStart = { x: 0, y: 0, cx: 0, cy: 0, cSize: 0 }
let resizeDir = ''

function syncSizeFromRatio() {
  const max = Math.min(imgW.value, imgH.value)
  crop.size = Math.round(cropRatio.value * max)
  clampCrop()
}

function syncRatioFromSize() {
  const max = Math.min(imgW.value, imgH.value)
  cropRatio.value = Math.round(crop.size / max * 100) / 100
}

function clampCrop() {
  const r = crop.size / 2
  crop.x = Math.max(r, Math.min(imgW.value - r, crop.x))
  crop.y = Math.max(r, Math.min(imgH.value - r, crop.y))
}

const imageDisplay = computed(() => {
  const nw = imgNatural.value.w || 1
  const nh = imgNatural.value.h || 1
  const cw = imgW.value || 1
  const ch = imgH.value || 1
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

const cropBox = computed(() => {
  const r = crop.size / 2
  return {
    left: crop.x - r,
    top: crop.y - r,
    size: crop.size
  }
})

const cropImgStyle = computed(() => {
  const display = imageDisplay.value
  return {
    width: `${display.width}px`,
    height: `${display.height}px`,
    left: `${display.left}px`,
    top: `${display.top}px`
  }
})

const cropFrameStyle = computed(() => ({
  left: `${cropBox.value.left}px`,
  top: `${cropBox.value.top}px`,
  width: `${cropBox.value.size}px`,
  height: `${cropBox.value.size}px`
}))

const cropGridStyle = computed(() => ({
  ...cropFrameStyle.value,
  backgroundImage: [
    'linear-gradient(to right, transparent 33.333%, rgba(255,255,255,0.55) 33.333%, rgba(255,255,255,0.55) calc(33.333% + 1px), transparent calc(33.333% + 1px))',
    'linear-gradient(to right, transparent 66.666%, rgba(255,255,255,0.55) 66.666%, rgba(255,255,255,0.55) calc(66.666% + 1px), transparent calc(66.666% + 1px))',
    'linear-gradient(to bottom, transparent 33.333%, rgba(255,255,255,0.55) 33.333%, rgba(255,255,255,0.55) calc(33.333% + 1px), transparent calc(33.333% + 1px))',
    'linear-gradient(to bottom, transparent 66.666%, rgba(255,255,255,0.55) 66.666%, rgba(255,255,255,0.55) calc(66.666% + 1px), transparent calc(66.666% + 1px))'
  ].join(', ')
}))

function previewImgStyle(previewSize) {
  if (!avatarPreviewUrl.value) return { display: 'none' }
  const display = imageDisplay.value
  const scale = previewSize / crop.size
  return {
    width: `${display.width * scale}px`,
    height: `${display.height * scale}px`,
    left: `${(display.left - cropBox.value.left) * scale}px`,
    top: `${(display.top - cropBox.value.top) * scale}px`
  }
}

const previewLgImgStyle = computed(() => previewImgStyle(120))
const previewSmImgStyle = computed(() => previewImgStyle(56))

function hPos(dir) {
  const r = crop.size / 2
  const map = {
    top:    { left: crop.x + 'px', top: (crop.y - r) + 'px' },
    bottom: { left: crop.x + 'px', top: (crop.y + r) + 'px' },
    left:   { left: (crop.x - r) + 'px', top: crop.y + 'px' },
    right:  { left: (crop.x + r) + 'px', top: crop.y + 'px' },
    tl:     { left: (crop.x - r) + 'px', top: (crop.y - r) + 'px' },
    tr:     { left: (crop.x + r) + 'px', top: (crop.y - r) + 'px' },
    bl:     { left: (crop.x - r) + 'px', top: (crop.y + r) + 'px' },
    br:     { left: (crop.x + r) + 'px', top: (crop.y + r) + 'px' },
  }
  return map[dir]
}

function initCrop() {
  crop.x = imgW.value / 2
  crop.y = imgH.value / 2
  cropRatio.value = 0.8
  syncSizeFromRatio()
}

function onSliderChange() {
  syncSizeFromRatio()
}

function onResize(e) {
  const dx = e.clientX - dragStart.x
  const dy = e.clientY - dragStart.y
  let newSize = dragStart.cSize
  let newX = dragStart.cx
  let newY = dragStart.cy
  const fromLeft = ['left', 'tl', 'bl'].includes(resizeDir)
  const fromRight = ['right', 'tr', 'br'].includes(resizeDir)
  const fromTop = ['top', 'tl', 'tr'].includes(resizeDir)
  const fromBottom = ['bottom', 'bl', 'br'].includes(resizeDir)

  if (fromRight) newSize = dragStart.cSize + dx * 2
  if (fromLeft) newSize = dragStart.cSize - dx * 2
  if (fromBottom) newSize = Math.max(newSize, dragStart.cSize + dy * 2)
  if (fromTop) newSize = Math.max(newSize, dragStart.cSize - dy * 2)

  newSize = Math.max(80, Math.min(Math.min(imgW.value, imgH.value), newSize))
  const r = newSize / 2
  crop.size = newSize
  syncRatioFromSize()
  if (fromLeft) newX = dragStart.cx + (dragStart.cSize - newSize) / 2
  if (fromRight) newX = dragStart.cx - (dragStart.cSize - newSize) / 2
  if (fromTop) newY = dragStart.cy + (dragStart.cSize - newSize) / 2
  if (fromBottom) newY = dragStart.cy - (dragStart.cSize - newSize) / 2
  crop.x = Math.max(r, Math.min(imgW.value - r, newX))
  crop.y = Math.max(r, Math.min(imgH.value - r, newY))
}

function startDragCrop(e) {
  if (resizing) return
  if (e.target.classList.contains('crop-handle')) return
  dragging = true
  document.body.style.cursor = 'move'
  document.body.style.userSelect = 'none'
  dragStart = { x: e.clientX, y: e.clientY, cx: crop.x, cy: crop.y, cSize: crop.size }
}

function onDragCrop(e) {
  if (!dragging) return
  const dx = e.clientX - dragStart.x
  const dy = e.clientY - dragStart.y
  const r = crop.size / 2
  crop.x = Math.max(r, Math.min(imgW.value - r, dragStart.cx + dx))
  crop.y = Math.max(r, Math.min(imgH.value - r, dragStart.cy + dy))
}

function stopDragCrop() {
  dragging = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

const cursorMap = { top: 'ns-resize', bottom: 'ns-resize', left: 'ew-resize', right: 'ew-resize', tl: 'nwse-resize', br: 'nwse-resize', tr: 'nesw-resize', bl: 'nesw-resize' }

function startResize(e, dir) {
  resizing = true
  resizeDir = dir
  dragStart = { x: e.clientX, y: e.clientY, cx: crop.x, cy: crop.y, cSize: crop.size }
  document.body.style.cursor = cursorMap[dir]
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onResize)
  window.addEventListener('mouseup', stopResize)
}

function stopResize() {
  resizing = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  window.removeEventListener('mousemove', onResize)
  window.removeEventListener('mouseup', stopResize)
}

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

function updateCropBounds() {
  if (!cropContainer.value) return
  imgW.value = cropContainer.value.clientWidth
  imgH.value = cropContainer.value.clientHeight
}

function loadAvatarSource(source, kind = 'current') {
  avatarPreviewUrl.value = source || ''
  avatarSourceKind.value = kind
  if (!source) {
    imgNatural.value = { w: 1, h: 1 }
    nextTick(() => {
      updateCropBounds()
      initCrop()
    })
    return
  }

  const img = new Image()
  img.onload = async () => {
    imgNatural.value = { w: img.naturalWidth, h: img.naturalHeight }
    await nextTick()
    updateCropBounds()
    initCrop()
  }
  img.onerror = () => {
    avatarPreviewUrl.value = ''
    ElMessage.error('图片加载失败')
  }
  img.src = source
}

function openAvatarEditor() {
  avatarFile.value = null
  avatarDialogVisible.value = true
  nextTick(async () => {
    const originalSource = await readCachedAvatarOriginal()
    loadAvatarSource(originalSource || currentAvatarUrl() || '', originalSource ? 'original' : 'current')
  })
}

function onAvatarFileChange(file) {
  if (!file?.raw) return
  avatarFile.value = file.raw
  const reader = new FileReader()
  reader.onload = (e) => {
    loadAvatarSource(e.target.result, 'new')
  }
  reader.readAsDataURL(file.raw)
}

async function confirmAvatar() {
  if (!avatarPreviewUrl.value) return
  avatarSaving.value = true
  try {
    let originalSource = avatarSourceKind.value === 'new' || avatarSourceKind.value === 'original'
      ? avatarPreviewUrl.value
      : await readCachedAvatarOriginal()
    if (!originalSource) originalSource = currentAvatarUrl()
    const croppedBlob = isGifFile(avatarFile.value) ? avatarFile.value : await cropImage()
    const fd = new FormData()
    fd.append('file', croppedBlob, isGifFile(avatarFile.value) ? 'avatar.gif' : 'avatar.png')
    const res = await uploadAvatar(fd)
    const avatarPath = typeof res.data === 'string' ? res.data : res.data?.avatar
    if (!avatarPath) throw new Error('头像上传失败')
    user.value.avatar = avatarPath
    user.value.avatarUrl = avatarPath
    if (userStore.userInfo) {
      userStore.userInfo.avatar = avatarPath
      userStore.userInfo.avatarUrl = avatarPath
    }
    if (originalSource) writeCachedAvatarOriginal(avatarPath, originalSource)
    ElMessage.success('头像已更新'); avatarDialogVisible.value = false
  } catch (error) {
    if (error?.message === '头像上传失败') ElMessage.error(error.message)
  } finally { avatarSaving.value = false }
}

function cropImage() {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      const nw = imgNatural.value.w || img.naturalWidth
      const nh = imgNatural.value.h || img.naturalHeight
      const display = imageDisplay.value
      const sx = (cropBox.value.left - display.left) / display.width * nw
      const sy = (cropBox.value.top - display.top) / display.height * nh
      const sw = crop.size / display.width * nw
      const sh = crop.size / display.height * nh
      const size = 400
      const canvas = document.createElement('canvas')
      canvas.width = size
      canvas.height = size
      const ctx = canvas.getContext('2d')
      ctx.imageSmoothingEnabled = true
      ctx.imageSmoothingQuality = 'high'
      ctx.drawImage(img, sx, sy, sw, sh, 0, 0, size, size)
      canvas.toBlob(blob => {
        if (blob) resolve(blob)
        else reject(new Error('Canvas toBlob failed'))
      }, 'image/png')
    }
    img.onerror = reject
    img.src = avatarPreviewUrl.value
  })
}

function isGifFile(file) {
  return !!file && (file.type === 'image/gif' || /\.gif$/i.test(file.name || ''))
}

onMounted(loadProfile)

watch(() => route.params.uuid, () => {
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
  } catch {}
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
      `确定删除选中的 ${uuids.length} 张图片吗？`,
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
  if (!uuid) return
  try {
    await deleteImage(uuid)
    selectedWorkUuids.value = selectedWorkUuids.value.filter(item => item !== uuid)
    ElMessage.success('删除成功')
    fetchWorks()
  } catch {}
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
  form.email = user.value.email || ''; form.phone = user.value.phone || ''; form.bio = user.value.bio || ''
  fieldErrors.email = ''; fieldErrors.phone = ''
  editing.value = true
}
function cancelEdit() { editing.value = false }

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
    localStorage.removeItem('satoken')
    userStore.userInfo = null
    ElMessage.success('账号已注销')
    router.push('/login')
  } catch {}
}

async function saveProfile() {
  saving.value = true
  try {
    const res = await updateProfile({ displayName: form.displayName, email: form.email, phone: form.phone, bio: form.bio })
    user.value = res.data
    if (isOwner.value) userStore.userInfo = res.data
    editing.value = false; ElMessage.success('资料已更新')
  } catch {} finally { saving.value = false }
}
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
}

.page-container {
  max-width: 1040px;
  margin: 0 auto;
  padding: 76px var(--space-lg) 112px;
}

/* ── Banner ── */
.profile-banner {
  height: 220px;
  border-radius: 0;
  background-size: cover;
  background-position: center;
  position: relative;
  border: 1px solid var(--gray2);
  overflow: hidden;
}

.profile-banner::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 1;
  background: linear-gradient(180deg, transparent 40%, rgba(10, 10, 10, 0.65) 100%);
  pointer-events: none;
}

.banner-edit {
  position: absolute;
  bottom: var(--space-md);
  right: var(--space-md);
  display: flex;
  gap: var(--space-sm);
  z-index: 2;
  opacity: 0;
  transform: translateY(12px);
  transition: opacity 0.3s ease 0.6s, transform 0.3s ease 0.6s;
}

.profile-banner:hover .banner-edit {
  opacity: 1;
  transform: translateY(0);
  transition: opacity 0.18s ease 0s, transform 0.18s ease 0s;
}

.banner-edit :deep(.el-button) {
  background: rgba(10, 10, 10, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  border-radius: 2px;
  font-weight: 500;
  transition: background 0.2s;
}

.banner-edit :deep(.el-button:hover) {
  background: rgba(10, 10, 10, 0.8);
}

/* ── Profile Header ── */
.profile-header {
  padding: 0 32px 28px;
  margin-top: -120px;
  position: relative;
}

.profile-header::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 120px;
  bottom: 0;
  z-index: 0;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid var(--gray2);
  border-top: 0;
}

.profile-header > * {
  position: relative;
  z-index: 1;
}

.avatar-wrap {
  position: relative;
  display: inline-block;
  margin-top: 0;
}

.avatar {
  border: 4px solid #fff;
  box-shadow: 0 8px 24px rgba(10, 10, 10, 0.12);
  border-radius: 50%;
}

.avatar-upload {
  position: absolute;
  bottom: 2px;
  right: -2px;
}

.avatar-upload :deep(.el-button) {
  background: var(--accent);
  border: 2px solid var(--white);
  color: #fff;
  width: 34px;
  height: 34px;
  transition: transform 0.2s, background 0.2s;
}

.avatar-upload :deep(.el-button:hover) {
  background: var(--accent-hover);
  transform: scale(1.08);
}

.profile-name-row {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-top: 18px;
  flex-wrap: wrap;
}

.profile-name-row h2 {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 400;
  margin: 0;
  color: var(--black);
  letter-spacing: 0.02em;
}

/* ··· dropdown trigger */
.dropdown-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--gray2);
  border-radius: 2px;
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
  letter-spacing: 2px;
  color: var(--gray3);
  transition: all 0.2s;
  user-select: none;
}

.dropdown-trigger:hover {
  border-color: var(--gray4);
  color: var(--black);
}

.bio {
  color: var(--gray4);
  margin-bottom: var(--space-sm);
  line-height: 1.7;
  font-size: 14px;
}

.contact {
  display: flex;
  gap: var(--space-lg);
  color: var(--gray3);
  font-size: 13px;
}

.contact .el-icon {
  margin-right: 4px;
  vertical-align: middle;
}

.profile-edit {
  margin-top: var(--space-md);
}

/* ── Works ── */
.user-works {
  margin-top: 36px;
}

.works-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-md);
  flex-wrap: wrap;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--gray2);
}

.works-heading h3 {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 400;
  color: var(--black);
  margin: 0;
  letter-spacing: 0.02em;
}

.works-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 4px 10px;
  border: 1px solid var(--gray2);
  border-radius: 2px;
  background: var(--gray1);
}

.category-row {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 18px;
}

.empty-state {
  text-align: center;
  color: var(--gray3);
  padding: var(--space-2xl) 0;
}

.pagination-wrap {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 90;
  display: flex;
  justify-content: center;
  padding: 12px 24px calc(12px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--gray2);
  background: rgba(250, 250, 250, 0.94);
  backdrop-filter: blur(12px);
}

.pagination-wrap :deep(.el-pagination) {
  max-width: min(100%, 1040px);
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
}

.upload-hint {
  font-size: 12px;
  color: var(--gray3);
  margin-top: var(--space-xs);
}

/* ── Dialog overrides ── */
:deep(.profile-dialog) {
  box-shadow: var(--shadow-dialog);
}

/* ── Background Editor ── */
.background-editor {
  text-align: center;
}

.background-editor-layout {
  display: grid;
  grid-template-columns: minmax(380px, 1fr) 260px;
  gap: 24px;
  align-items: flex-start;
}

.bg-crop-side {
  min-width: 0;
}

.bg-crop-container {
  width: 100%;
  aspect-ratio: 16 / 9;
  position: relative;
  overflow: hidden;
  background: var(--gray4);
  border: 1px solid var(--gray2);
  border-radius: 2px;
  cursor: move;
  user-select: none;
}

.bg-placeholder {
  position: absolute;
  inset: 0;
  margin: auto;
  color: rgba(255, 255, 255, 0.35);
}

.bg-crop-img {
  position: absolute;
  z-index: 0;
  max-width: none;
  user-select: none;
}

.bg-crop-frame {
  position: absolute;
  z-index: 1;
  outline: 2px solid rgba(216, 90, 48, 0.8);
  border: 2px solid rgba(255, 255, 255, 0.9);
  border-radius: 2px;
  box-shadow:
    0 0 0 999px rgba(10, 10, 10, 0.45),
    0 0 10px rgba(216, 90, 48, 0.2);
  pointer-events: none;
}

.bg-crop-grid {
  position: absolute;
  z-index: 2;
  border-radius: 2px;
  pointer-events: none;
}

.bg-controls {
  display: flex;
  align-items: center;
  max-width: 560px;
  margin: 14px auto 0;
}

.bg-upload {
  margin-top: 16px;
  text-align: center;
}

.bg-preview-side {
  text-align: center;
  padding: 16px 12px;
  border: 1px solid var(--gray2);
  border-radius: 2px;
  background: var(--gray1);
}

/* Mini profile card */
.profile-mini-card {
  width: 200px;
  margin: 0 auto;
  border-radius: 2px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(10, 10, 10, 0.08);
  border: 1px solid var(--gray2);
}

.profile-mini-banner {
  height: 56px;
  position: relative;
  overflow: hidden;
  background-size: cover;
  background-position: center;
}

.profile-mini-bg {
  position: absolute;
  max-width: none;
  user-select: none;
  pointer-events: none;
}

.profile-mini-header {
  background: #fff;
  padding: 4px 10px 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  position: relative;
}

.profile-mini-avatar {
  margin-top: -14px;
  flex-shrink: 0;
}

.profile-mini-avatar :deep(.el-avatar) {
  border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(10, 10, 10, 0.1);
}

.profile-mini-name {
  font-size: 10px;
  font-weight: 600;
  color: var(--black);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1;
}

/* BG handles */
.bg-handle {
  position: absolute;
  z-index: 3;
  width: 10px;
  height: 10px;
  background: #fff;
  border: 2px solid var(--accent);
  border-radius: 2px;
  transform: translate(-50%, -50%);
  box-shadow: 0 1px 6px rgba(10, 10, 10, 0.12);
  transition: transform 0.12s ease;
}

.bg-handle:hover {
  transform: translate(-50%, -50%) scale(1.25);
}

.bg-handle-corner {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.bg-handle-ns { cursor: ns-resize; }
.bg-handle-ew { cursor: ew-resize; }
.bg-handle-nwse { cursor: nwse-resize; }
.bg-handle-nesw { cursor: nesw-resize; }

/* ── Avatar Editor ── */
.avatar-editor {
  text-align: center;
}

.avatar-editor-layout {
  display: grid;
  grid-template-columns: minmax(300px, 1fr) 160px;
  gap: 24px;
  align-items: flex-start;
}

.avatar-crop-side {
  min-width: 0;
}

.avatar-preview-side {
  text-align: center;
  padding: 20px 16px;
  border: 1px solid var(--gray2);
  border-radius: 2px;
  background: var(--gray1);
}

.preview-label {
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 400;
  color: var(--gray4);
  margin-bottom: var(--space-md);
}

.preview-circle-lg {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  margin: 0 auto var(--space-md);
  background: var(--gray2);
  position: relative;
  outline: 2px solid var(--gray2);
}

.preview-circle-sm {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  overflow: hidden;
  margin: 0 auto;
  background: var(--gray2);
  position: relative;
  outline: 2px solid var(--gray2);
}

.preview-img {
  position: absolute;
  max-width: none;
  user-select: none;
  pointer-events: none;
}

.crop-container {
  width: min(100%, 340px);
  aspect-ratio: 1;
  margin: 0 auto var(--space-md);
  position: relative;
  overflow: hidden;
  background: var(--gray4);
  user-select: none;
  cursor: move;
  border-radius: 2px;
  border: 1px solid var(--gray2);
}

.crop-img {
  position: absolute;
  z-index: 0;
  max-width: none;
  user-select: none;
}

.crop-frame {
  position: absolute;
  z-index: 1;
  outline: 2px solid rgba(216, 90, 48, 0.85);
  outline-offset: 0px;
  border: 2px solid rgba(255, 255, 255, 0.92);
  border-radius: 2px;
  box-shadow:
    0 0 0 999px rgba(10, 10, 10, 0.4),
    0 0 12px rgba(216, 90, 48, 0.25);
  pointer-events: none;
}

.crop-grid {
  position: absolute;
  z-index: 2;
  border-radius: 2px;
  pointer-events: none;
}

/* Crop handles */
.crop-handle {
  position: absolute;
  z-index: 3;
  width: 10px;
  height: 10px;
  background: #fff;
  border: 2px solid var(--accent);
  border-radius: 2px;
  transform: translate(-50%, -50%);
  box-shadow: 0 1px 6px rgba(10, 10, 10, 0.12);
  transition: transform 0.12s ease;
}

.crop-handle:hover {
  transform: translate(-50%, -50%) scale(1.25);
}

.crop-handle-corner {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.crop-handle-ns { cursor: ns-resize; }
.crop-handle-ew { cursor: ew-resize; }
.crop-handle-nwse { cursor: nwse-resize; }
.crop-handle-nesw { cursor: nesw-resize; }

.crop-controls {
  display: flex;
  align-items: center;
  max-width: 340px;
  margin: 0 auto;
}

.slider-label {
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--gray4);
  flex-shrink: 0;
  font-weight: 400;
}

.slider-val {
  font-size: 12px;
  color: var(--gray4);
  min-width: 42px;
  text-align: right;
  font-weight: 500;
}

.avatar-replace-upload {
  margin-top: 16px;
  text-align: center;
}

/* ── Responsive ── */
@media (max-width: 720px) {
  .page-container { padding: 16px var(--space-md) 148px; }
  .profile-banner { height: 218px; }
  .profile-header { padding: 0 20px 24px; margin-top: -120px; }
  .profile-header::before { top: 120px; }
  .avatar-wrap { margin-top: 0; }
  .works-heading { align-items: flex-start; }
  .works-actions { width: 100%; }
  .category-row { grid-template-columns: 1fr; }
  .avatar-editor-layout { grid-template-columns: 1fr; }
  .avatar-preview-side { display: flex; align-items: center; justify-content: center; gap: 18px; }
  .background-editor-layout { grid-template-columns: 1fr; }
  .profile-mini-card { margin: 0 auto; }
  .preview-label { margin: 0; }
  .pagination-wrap {
    padding: 10px 12px calc(10px + env(safe-area-inset-bottom));
  }
  .pagination-wrap :deep(.el-pagination) {
    --el-pagination-button-width: 28px;
    --el-pagination-button-height: 28px;
    font-size: 12px;
  }
}
</style>
