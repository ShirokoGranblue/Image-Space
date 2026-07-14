<template>
  <div class="asset-page">
    <NavBar />

    <div class="asset-shell" :class="{ 'has-inspector': inspectedImage }">
      <aside class="workspace-rail" aria-label="图片工作区">
        <section class="rail-section">
          <h2 class="rail-title">图库</h2>
          <button class="rail-item" :class="{ active: !query.visibility }" type="button" @click="setVisibilityFilter('')">
            <span class="dot green"></span>
            <span>全部图片</span>
            <strong>{{ total }}</strong>
          </button>
          <button class="rail-item" :class="{ active: query.visibility === 'PUBLIC' }" type="button" @click="setVisibilityFilter('PUBLIC')">
            <span class="dot cyan"></span>
            <span>公开</span>
            <strong>{{ visibilityStats.PUBLIC }}</strong>
          </button>
          <button class="rail-item" :class="{ active: query.visibility === 'SPECIFIED' }" type="button" @click="setVisibilityFilter('SPECIFIED')">
            <span class="dot amber"></span>
            <span>指定用户</span>
            <strong>{{ visibilityStats.SPECIFIED }}</strong>
          </button>
          <button class="rail-item" :class="{ active: query.visibility === 'PRIVATE' }" type="button" @click="setVisibilityFilter('PRIVATE')">
            <span class="dot coral"></span>
            <span>仅自己</span>
            <strong>{{ visibilityStats.PRIVATE }}</strong>
          </button>
        </section>

        <section class="rail-section">
          <h2 class="rail-title">筛选</h2>
          <button class="rail-item" type="button" @click="query.keyword = ''; query.categoryId = null; onFilterChange()">
            <span class="dot violet"></span>
            <span>清空筛选</span>
            <strong>重置</strong>
          </button>
          <button class="rail-item" type="button" @click="clearSelection" :disabled="selectedImageUuids.length === 0">
            <span class="dot green"></span>
            <span>已选择</span>
            <strong>{{ selectedImageUuids.length }}</strong>
          </button>
        </section>

        <section class="rail-section">
          <h2 class="rail-title">状态</h2>
          <div class="meter-card">
            <div class="meter-top">
              <span>页面状态</span>
              <strong>{{ loading ? '加载中' : '已就绪' }}</strong>
            </div>
            <div class="meter"><span :style="{ width: loading ? '38%' : '74%' }"></span></div>
          </div>
        </section>
      </aside>

      <main class="asset-main">
        <HomeToolbar
          :total="total" :category-count="categories.length" :selected-count="selectedImageUuids.length" :categories="categories"
          :keyword="query.keyword" :category-id="query.categoryId" :sort-field="query.sortField" :visibility="query.visibility" :all-visible-selected="allVisibleSelected"
          @update:keyword="query.keyword = $event" @update:category-id="query.categoryId = $event" @update:sort-field="query.sortField = $event"
          @filter="onFilterChange" @toggle-select-all="toggleSelectAll" @upload="uploadRef.open()" @select-visibility="setVisibilityFilter" @batch-delete="handleBatchDelete"
        />
        <section class="gallery-stage">
          <GalleryGrid :items="displayedImages" :loading="loading" :error="loadError" :loading-count="8" :initial-eager-count="4" density="compact" empty-title="没有符合条件的图片" empty-description="可以调整筛选条件，或上传一张新图片。" error-description="图片列表请求失败，请检查连接后重试。" @retry="fetchList">
            <template #item="{ item, priority }"><ImageCard :image="item" :priority="priority" :show-actions="true" :selectable="true" :selected="selectedImageUuids.includes(item.uuid)" @delete="handleDelete" @copy="copyImageLink" @toggle-select="toggleImageSelection" /></template>
          </GalleryGrid>
        </section>

        <HomeBatchQueue :visible="selectedImageUuids.length > 0" :images="selectedPreviewImages" :selected-count="selectedImageUuids.length" :display-url="getImageDisplayUrl" @toggle="toggleImageSelection" @clear="clearSelection" @delete="handleBatchDelete" />
      </main>

      <aside v-if="inspectedImage" class="asset-inspector" aria-label="图片信息">
        <div class="inspector-card">
          <div class="inspect-preview">
            <img v-if="inspectedImageSrc" :src="inspectedImageSrc" :alt="inspectedImageAlt" loading="lazy" decoding="async" />
            <div v-else class="inspect-fallback">
              <el-icon><PictureFilled /></el-icon>
            </div>
          </div>

          <div class="inspect-body">
            <div class="inspect-head">
              <div>
                <h2>{{ inspectedImage.imageName }}</h2>
                <p>图片信息与分享状态</p>
              </div>
              <span class="inspect-status" :class="visibilityClass(inspectedImage.visibility)">{{ visibilityText(inspectedImage.visibility) }}</span>
            </div>

            <div class="meta-grid">
              <div class="meta">
                <span>分类</span>
                <strong>{{ inspectedImage.categoryName || '未分类' }}</strong>
              </div>
              <div class="meta">
                <span>大小</span>
                <strong>{{ formatFileSize(inspectedImage.fileSize) }}</strong>
              </div>
              <div class="meta">
                <span>类型</span>
                <strong>{{ inspectedImage.imageType || '--' }}</strong>
              </div>
              <div class="meta">
                <span>上传时间</span>
                <strong>{{ inspectedImage.uploadTime ? formatTime(inspectedImage.uploadTime) : '--' }}</strong>
              </div>
            </div>

            <div class="route-box">
              <div class="route-line">
                <span>分享状态</span>
                <code>{{ visibilityText(inspectedImage.visibility) }}</code>
              </div>
              <div class="route-line">
                <span>访问说明</span>
                <code>{{ visibilityDescription(inspectedImage.visibility) }}</code>
              </div>
            </div>

            <div class="inspect-actions">
              <button class="primary-command compact" type="button" @click="goDetail(inspectedImage)">查看详情</button>
              <button class="secondary-command" type="button" @click="copyImageLink(inspectedImage)">复制链接</button>
              <button class="secondary-command" type="button" @click="handleEdit(inspectedImage)">编辑信息</button>
              <button class="secondary-command danger-text" type="button" @click="handleDelete(inspectedImage)">删除</button>
            </div>
          </div>
        </div>

      </aside>
    </div>

    <Teleport to="body">
      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.limit"
          :page-sizes="IMAGE_PAGE_SIZES"
          :total="total"
          :disabled="loading"
          layout="total, sizes, prev, pager, next"
          @size-change="onPageSizeChange"
          @current-change="fetchList"
        />
      </div>
    </Teleport>

    <ImageUpload ref="uploadRef" @uploaded="handleUploaded" />

    <el-dialog v-model="editVisible" title="编辑图片信息" width="520px">
      <el-form :model="editForm" label-width="110px" v-if="editForm.uuid">
        <el-form-item label="图片名称">
          <el-input v-model="editForm.imageName" />
        </el-form-item>
        <el-form-item label="分类">
          <div class="category-row">
            <el-select v-model="editForm.categoryId" placeholder="选择分类" clearable filterable style="width: 100%">
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
        <el-form-item label="可见范围">
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

    <el-dialog v-model="categoryDialogVisible" title="新建分类" width="380px">
      <el-form label-width="88px" @submit.prevent>
        <el-form-item label="分类名">
          <el-input v-model="newCategoryName" maxlength="20" show-word-limit @keyup.enter="submitCategory" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creatingCategory" @click="submitCategory">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { PictureFilled } from '@element-plus/icons-vue'
import NavBar from '../components/NavBar.vue'
import ImageCard from '../components/ImageCard.vue'
import GalleryGrid from '../components/gallery/GalleryGrid.vue'
import HomeToolbar from '../components/home/HomeToolbar.vue'
import HomeBatchQueue from '../components/home/HomeBatchQueue.vue'
import ImageUpload from '../components/ImageUpload.vue'
import TagInput from '../components/TagInput.vue'
import { getImageList, deleteImage, updateImage } from '../api/image'
import { getImageResourceStatus, refreshImageAccessUrl } from '../api/resource'
import { getCategoryList, createCategory } from '../api/category'
import { DEFAULT_IMAGE_PAGE_SIZE, IMAGE_PAGE_SIZES, buildImageListParams, getImageAlt, getImageDisplayUrl, getImageDownloadUrl, getImagePreviewUrl } from '../utils/imageRequests'
import { applyImageAccessUrl, applyImageStatus, imageToPollingResource } from '../utils/resourceAdapters'
import { isAccessUrlExpiring } from '../utils/resourceAccess'
import { hasSpecifiedUsers } from '../utils/visibility'
import { formatTime } from '../utils/format'
import { POLLING_INTERVALS, useResourcePolling } from '../composables/useResourcePolling'

const uploadRef = ref(null)
const route = useRoute()
const router = useRouter()
const images = ref([])
const categories = ref([])
const total = ref(0)
const loading = ref(false)
const loadError = ref(false)
const editVisible = ref(false)
const categoryDialogVisible = ref(false)
const newCategoryName = ref('')
const creatingCategory = ref(false)
const selectedImageUuids = ref([])
const recentUploadResources = ref([])

const query = reactive({
  page: 1,
  limit: DEFAULT_IMAGE_PAGE_SIZE,
  keyword: '',
  categoryId: null,
  visibility: '',
  sortField: 'upload_time',
  sortOrder: 'desc'
})

const editForm = reactive({
  uuid: null,
  imageName: '',
  categoryId: null,
  tags: '',
  description: '',
  visibility: 'PUBLIC',
  visibleUsernames: ''
})

const displayedImages = computed(() => {
  if (!query.visibility) return images.value
  return images.value.filter(img => img.visibility === query.visibility)
})
const visibleImageUuids = computed(() => displayedImages.value.map(img => img.uuid).filter(Boolean))
const allVisibleSelected = computed(() => visibleImageUuids.value.length > 0 && visibleImageUuids.value.every(uuid => selectedImageUuids.value.includes(uuid)))
const selectedPreviewImages = computed(() => images.value.filter(img => selectedImageUuids.value.includes(img.uuid)).slice(0, 8))
const inspectedImage = computed(() => selectedPreviewImages.value[0] || null)
const inspectedImageAlt = computed(() => getImageAlt(inspectedImage.value))
const inspectedImageSrc = computed(() => inspectedImage.value ? getImagePreviewUrl(inspectedImage.value) : '')
const visibilityStats = computed(() => {
  const stats = { PUBLIC: 0, SPECIFIED: 0, PRIVATE: 0 }
  for (const image of images.value) {
    if (image.visibility === 'PUBLIC') stats.PUBLIC += 1
    else if (image.visibility === 'SPECIFIED') stats.SPECIFIED += 1
    else stats.PRIVATE += 1
  }
  return stats
})
const recentUploadPollingResource = computed(() => {
  const active = recentUploadResources.value.filter(img => img?.uuid && !img.deleted)
  const next = active.find(img => img.status === 'PROCESSING')
    || active.find(img => isAccessUrlExpiring(imageToPollingResource(img)?.url))
    || active[0]
  return imageToPollingResource(next)
})

onMounted(() => {
  fetchList()
  fetchCategories()
  openUploadFromRoute()
})

watch(() => route.query.upload, () => {
  openUploadFromRoute()
})

useResourcePolling({
  resource: recentUploadPollingResource,
  intervalMs: POLLING_INTERVALS.processing,
  enabled: computed(() => recentUploadResources.value.some(img => img?.uuid && !img.deleted)),
  getStatus: async (resource) => {
    const res = await getImageResourceStatus(resource.uuid)
    return res.data
  },
  getAccessUrl: async (_status, resource) => {
    const res = await refreshImageAccessUrl(resource.uuid)
    return res.data
  },
  onStatusChange: (status) => {
    updatePolledImage(status.uuid, image => applyImageStatus(image, status))
    if (status.deleted) removePolledImage(status.uuid)
  },
  onAccessUrl: (access, status) => {
    const source = access.source || status.source
    const uuid = source?.uuid || status.uuid
    updatePolledImage(uuid, image => applyImageAccessUrl(image, access, status))
  },
  onDeleted: (status) => {
    removePolledImage(status.uuid)
  },
})

async function fetchList() {
  loading.value = true
  loadError.value = false
  try {
    const res = await getImageList(buildImageListParams({
      page: query.page,
      limit: query.limit,
      keyword: query.keyword,
      categoryId: query.categoryId,
      sortField: query.sortField,
      sortOrder: query.sortOrder
    }))
    images.value = res.data.records || []
    total.value = res.data.total || 0
    selectedImageUuids.value = selectedImageUuids.value.filter(uuid => images.value.some(img => img.uuid === uuid))
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

async function openUploadFromRoute() {
  if (route.query.upload !== '1') return
  await nextTick()
  uploadRef.value?.open?.()
  const nextQuery = { ...route.query }
  delete nextQuery.upload
  router.replace({ path: '/home', query: nextQuery })
}

function onFilterChange() {
  query.page = 1
  fetchList()
}

function setVisibilityFilter(visibility) {
  query.visibility = visibility
}

function onPageSizeChange(size) {
  query.limit = size
  query.page = 1
  fetchList()
}

async function fetchCategories() {
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
  } catch {}
}

async function handleUploaded(uploadedImages = []) {
  if (Array.isArray(uploadedImages) && uploadedImages.length > 0) {
    recentUploadResources.value = uploadedImages.filter(img => img?.uuid)
  }
  await Promise.all([fetchCategories(), fetchList()])
}

function openCreateCategory() {
  newCategoryName.value = ''
  categoryDialogVisible.value = true
}

async function submitCategory() {
  const name = newCategoryName.value.trim()
  if (!name) {
    ElMessage.warning('请输入分类名称')
    return
  }
  creatingCategory.value = true
  try {
    const res = await createCategory(name)
    const category = res.data
    categories.value = categories.value.filter(cat => cat.id !== category.id)
    categories.value.push(category)
    editForm.categoryId = category.id
    categoryDialogVisible.value = false
    ElMessage.success('分类已创建')
  } catch {} finally {
    creatingCategory.value = false
  }
}

function toggleImageSelection(uuid) {
  selectedImageUuids.value = selectedImageUuids.value.includes(uuid)
    ? selectedImageUuids.value.filter(item => item !== uuid)
    : [...selectedImageUuids.value, uuid]
}

function toggleSelectAll(checked) {
  selectedImageUuids.value = checked ? [...visibleImageUuids.value] : []
}

function clearSelection() {
  selectedImageUuids.value = []
}

async function handleBatchDelete() {
  const uuids = [...selectedImageUuids.value]
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
    selectedImageUuids.value = []
    fetchList()
  } catch {}
}

async function handleDelete(img) {
  try {
    await deleteImage(img.uuid)
    ElMessage.success('图片已删除')
    selectedImageUuids.value = selectedImageUuids.value.filter(uuid => uuid !== img.uuid)
    fetchList()
  } catch {}
}

function updatePolledImage(uuid, apply) {
  if (!uuid) return
  const image = images.value.find(img => img.uuid === uuid)
  if (image) apply(image)
  const recent = recentUploadResources.value.find(img => img.uuid === uuid)
  if (recent) apply(recent)
}

function removePolledImage(uuid) {
  if (!uuid) return
  recentUploadResources.value = recentUploadResources.value.filter(img => img.uuid !== uuid)
  images.value = images.value.filter(img => img.uuid !== uuid)
  selectedImageUuids.value = selectedImageUuids.value.filter(item => item !== uuid)
  total.value = Math.max(0, total.value - 1)
}

function handleEdit(img) {
  editForm.uuid = img.uuid
  editForm.imageName = img.imageName
  editForm.categoryId = img.categoryId
  editForm.description = img.description || ''
  editForm.tags = img.tags || ''
  editForm.visibility = img.visibility || 'PUBLIC'
  editForm.visibleUsernames = img.visibleUsernames || ''
  editVisible.value = true
}

async function copyImageLink(img) {
  const url = getImageDownloadUrl(img)
  if (!url) {
    ElMessage.warning('暂无可复制链接')
    return
  }
  try {
    await navigator.clipboard.writeText(new URL(url, window.location.origin).href)
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.warning('当前浏览器不支持自动复制')
  }
}

async function saveEdit() {
  if (editForm.visibility === 'SPECIFIED' && !hasSpecifiedUsers(editForm.visibleUsernames)) {
    ElMessage.warning('请先填写指定用户')
    return
  }
  try {
    await updateImage(editForm.uuid, {
      imageName: editForm.imageName,
      categoryId: editForm.categoryId,
      description: editForm.description,
      tags: editForm.tags,
      visibility: editForm.visibility,
      visibleUsernames: editForm.visibility === 'SPECIFIED' ? editForm.visibleUsernames : ''
    })
    ElMessage.success('图片信息已更新')
    editVisible.value = false
    fetchList()
  } catch {}
}

function goDetail(img) {
  if (img?.uuid) router.push(`/image/${img.uuid}`)
}

function visibilityText(visibility) {
  if (visibility === 'PUBLIC') return '公开'
  if (visibility === 'SPECIFIED') return '指定用户'
  return '仅自己'
}

function visibilityDescription(visibility) {
  if (visibility === 'PUBLIC') return '任何人可查看'
  if (visibility === 'SPECIFIED') return '仅指定用户可查看'
  return '只有你可查看'
}

function visibilityClass(visibility) {
  if (visibility === 'PUBLIC') return 'public'
  if (visibility === 'SPECIFIED') return 'specified'
  return 'private'
}

function formatFileSize(size) {
  const numeric = Number(size || 0)
  if (!numeric) return '--'
  if (numeric >= 1024 * 1024) return `${(numeric / 1024 / 1024).toFixed(1)} MB`
  return `${Math.max(1, Math.round(numeric / 1024))} KB`
}
</script>

<style scoped>
.asset-page { min-height: 100vh; background: var(--color-canvas); color: var(--color-text-primary); }
.asset-shell { display: grid; width: min(calc(100% - (2 * var(--page-gutter))), var(--page-wide)); grid-template-columns: 212px minmax(0,1fr); margin-inline: auto; padding: 96px 0 112px; }
.asset-shell.has-inspector { grid-template-columns: 212px minmax(0,1fr) 340px; }
.workspace-rail { padding: var(--space-5) var(--space-3); border: 1px solid var(--color-border-subtle); border-right: 0; background: transparent; }
.rail-section + .rail-section { margin-top: var(--space-6); padding-top: var(--space-5); border-top: 1px solid var(--color-border-subtle); }
.rail-title { margin: 0 0 var(--space-3); color: var(--color-text-muted); font-family: var(--font-ui); font-size: var(--text-xs); font-weight: 700; letter-spacing: .08em; }
.rail-item { display: grid; width: 100%; min-height: 42px; grid-template-columns: 10px minmax(0,1fr) auto; align-items: center; gap: var(--space-2); padding: 0 var(--space-2); border: 0; border-radius: var(--radius-sm); background: transparent; color: var(--color-text-secondary); text-align: left; cursor: pointer; }
.rail-item:hover { background: var(--color-surface-2); color: var(--color-text-primary); }.rail-item.active { background: var(--color-night); color: var(--color-text-inverse); }.rail-item:disabled { opacity: .45; cursor: not-allowed; }.rail-item strong { font-size: var(--text-xs); }
.dot { width: 8px; height: 8px; border-radius: 50%; background: var(--color-text-muted); }.dot.green { background: var(--color-success); }.dot.cyan { background: var(--color-urban); }.dot.amber { background: var(--color-warning); }.dot.coral { background: var(--color-error); }.dot.violet { background: var(--color-night); }
.meter-card { padding: var(--space-3); border: 1px solid var(--color-border-subtle); background: var(--color-surface-1); }.meter-top { display: flex; justify-content: space-between; gap: var(--space-2); color: var(--color-text-muted); font-size: var(--text-xs); }.meter { height: 6px; margin-top: var(--space-3); overflow: hidden; background: var(--color-canvas-muted); }.meter span { display: block; height: 100%; background: var(--color-vermilion); transition: width var(--duration-overlay) var(--ease-standard); }
.asset-main { min-width: 0; border: 1px solid var(--color-border-subtle); background: var(--color-surface-1); }
.gallery-stage { min-width: 0; padding: var(--space-5); background: var(--color-surface-1); }
.asset-inspector { padding: var(--space-4); border: 1px solid var(--color-border-subtle); border-left: 0; background: transparent; }
.inspector-card { overflow: hidden; border: 1px solid var(--color-border-subtle); background: var(--color-surface-1); }.inspect-preview { display: grid; min-height: 244px; place-items: center; background: #e2e2df; }.inspect-preview img { display: block; width: 100%; height: 244px; object-fit: contain; }.inspect-fallback { display: grid; min-height: 244px; place-items: center; color: var(--color-text-muted); font-size: 40px; }
.inspect-body { padding: var(--space-4); }.inspect-head { display: flex; align-items: flex-start; justify-content: space-between; gap: var(--space-3); }.inspect-head h2 { margin: 0; font-family: var(--font-title); font-size: var(--text-xl); font-weight: 500; overflow-wrap: anywhere; }.inspect-head p { margin: var(--space-1) 0 0; color: var(--color-text-muted); font-size: var(--text-sm); }
.inspect-status { flex: 0 0 auto; padding: var(--space-1) var(--space-2); border: 1px solid currentColor; border-radius: var(--radius-round); font-size: var(--text-xs); font-weight: 700; }.inspect-status.public { color: var(--color-success); }.inspect-status.private { color: var(--color-error); }.inspect-status.specified { color: var(--color-warning); }
.meta-grid { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: var(--space-2); margin-top: var(--space-4); }.meta { min-width: 0; padding: var(--space-3); border: 1px solid var(--color-border-subtle); background: var(--color-surface-2); }.meta span,.route-line span { display: block; color: var(--color-text-muted); font-size: var(--text-xs); }.meta strong { display: block; margin-top: var(--space-1); overflow: hidden; color: var(--color-text-primary); font-size: var(--text-sm); text-overflow: ellipsis; overflow-wrap: anywhere; }
.route-box { margin-top: var(--space-4); padding: var(--space-3); border: 1px solid var(--color-border-subtle); }.route-line { display: flex; justify-content: space-between; gap: var(--space-3); }.route-line + .route-line { margin-top: var(--space-2); }.route-line code { color: var(--color-vermilion); font-family: var(--font-ui); font-size: var(--text-xs); text-align: right; }
.inspect-actions { display: flex; flex-wrap: wrap; gap: var(--space-2); margin-top: var(--space-4); }.primary-command,.secondary-command { min-height: 40px; padding: 0 var(--space-3); border: 1px solid var(--color-border-subtle); border-radius: var(--radius-sm); background: var(--color-surface-1); color: var(--color-text-primary); font-family: var(--font-ui); cursor: pointer; }.primary-command { border-color: var(--color-vermilion); background: var(--color-vermilion); color: var(--color-text-inverse); }.primary-command:hover { background: var(--color-vermilion-hover); }.secondary-command:hover { background: var(--color-surface-2); }.danger-text { border-color: var(--color-error); color: var(--color-error); }
.pagination-wrap { position: fixed; z-index: var(--layer-floating); right: 0; bottom: 0; left: 0; display: flex; justify-content: center; padding: var(--space-3) var(--space-4) calc(var(--space-3) + env(safe-area-inset-bottom)); border-top: 1px solid var(--color-border-subtle); background: rgba(248,245,238,.96); }.pagination-wrap :deep(.el-pagination) { max-width: 100%; flex-wrap: wrap; justify-content: center; gap: var(--space-1); }
.category-row { display: grid; width: 100%; grid-template-columns: 1fr auto; gap: var(--space-2); }
@media (max-width:1260px) { .asset-shell,.asset-shell.has-inspector { grid-template-columns: 196px minmax(0,1fr); }.asset-inspector { display: none; }.workspace-rail { border-right: 0; }.asset-main { border-left: 1px solid var(--color-border-subtle); } }
@media (max-width:760px) { .asset-shell,.asset-shell.has-inspector { width: calc(100% - (2 * var(--page-gutter))); grid-template-columns: minmax(0,1fr); padding-top: 92px; }.workspace-rail { display: none; }.asset-main { border: 1px solid var(--color-border-subtle); }.gallery-stage { padding: var(--space-4); }.primary-command,.secondary-command { min-height: 44px; }.pagination-wrap { padding-inline: var(--space-2); }.pagination-wrap :deep(.el-pagination__total),.pagination-wrap :deep(.el-pagination__sizes) { display: none; } }
@media (prefers-reduced-motion:reduce) { .meter span { transition: none; } }
</style>
