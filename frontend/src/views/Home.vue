<template>
  <div class="home-page cinematic-shell">
    <NavBar />
    <div class="page-container">
      <header class="page-header">
        <div>
          <h1 class="page-title">Images</h1>
        </div>
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input
              v-model="query.keyword"
              placeholder="搜索图片名称"
              clearable
              @clear="onFilterChange"
              @keyup.enter="onFilterChange"
              class="toolbar-search"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-select
              v-model="query.categoryId"
              placeholder="分类筛选"
              clearable
              @change="onFilterChange"
              class="toolbar-select"
            >
              <el-option v-for="cat in categories" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
            </el-select>
            <el-select
              v-model="query.visibility"
              placeholder="公开/私有"
              clearable
              class="toolbar-select"
            >
              <el-option label="公开" value="PUBLIC" />
              <el-option label="私有" value="PRIVATE" />
              <el-option label="指定用户" value="SPECIFIED" />
            </el-select>
            <div class="selection-actions" v-if="displayedImages.length > 0">
              <el-checkbox
                :model-value="allVisibleSelected"
                :indeterminate="partiallySelected"
                @change="toggleSelectAll"
              >
                全选本页
              </el-checkbox>
            </div>
          </div>
          <div class="toolbar-right">
            <el-select v-model="query.sortField" @change="onFilterChange" style="width: 120px">
              <el-option label="按时间" value="upload_time" />
              <el-option label="按名称" value="image_name" />
              <el-option label="按大小" value="file_size" />
            </el-select>
            <el-button v-if="selectedImageUuids.length > 0" @click="clearSelection">取消选择</el-button>
            <el-button
              v-if="selectedImageUuids.length > 0"
              type="danger"
              @click="handleBatchDelete"
            >
              删除选中 {{ selectedImageUuids.length }}
            </el-button>
            <el-button type="primary" class="btn-slide" @click="uploadRef.open()">
              <el-icon><Plus /></el-icon> 上传图片
            </el-button>
          </div>
        </div>
      </header>

      <div v-if="loading" class="empty-state">
        <div class="skeleton-grid">
          <div class="skeleton" v-for="n in 6" :key="n" style="aspect-ratio:1;"></div>
        </div>
      </div>

      <div v-else-if="displayedImages.length === 0" class="empty-state">
        <el-icon><PictureFilled /></el-icon>
        <p>上传图片</p>
      </div>

      <div v-else class="card-grid">
        <div v-for="(img, idx) in displayedImages" :key="img.id" class="stagger-item" :style="{ animationDelay: `${idx * 0.06}s` }">
          <ImageCard
            :image="img"
            :show-actions="true"
            :selectable="true"
            :selected="selectedImageUuids.includes(img.uuid)"
            @delete="handleDelete"
            @copy="copyImageLink"
            @toggle-select="toggleImageSelection"
          />
        </div>
      </div>

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

    <el-dialog v-model="editVisible" title="编辑图片信息" width="480px">
      <el-form :model="editForm" label-width="86px" v-if="editForm.uuid">
        <el-form-item label="图片名称">
          <el-input v-model="editForm.imageName" />
        </el-form-item>
        <el-form-item label="分类">
          <div class="category-row">
            <el-select
              v-model="editForm.categoryId"
              placeholder="选择分类"
              clearable
              filterable
              style="width: 100%"
            >
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import NavBar from '../components/NavBar.vue'
import ImageCard from '../components/ImageCard.vue'
import ImageUpload from '../components/ImageUpload.vue'
import TagInput from '../components/TagInput.vue'
import { getImageList, deleteImage, updateImage } from '../api/image'
import { getImageResourceStatus, refreshImageAccessUrl } from '../api/resource'
import { getCategoryList, createCategory } from '../api/category'
import { DEFAULT_IMAGE_PAGE_SIZE, IMAGE_PAGE_SIZES, buildImageListParams, getImageDownloadUrl } from '../utils/imageRequests'
import { applyImageAccessUrl, applyImageStatus, imageToPollingResource } from '../utils/resourceAdapters'
import { isAccessUrlExpiring } from '../utils/resourceAccess'
import { hasSpecifiedUsers } from '../utils/visibility'
import { POLLING_INTERVALS, useResourcePolling } from '../composables/useResourcePolling'

const uploadRef = ref(null)
const route = useRoute()
const router = useRouter()
const images = ref([])
const categories = ref([])
const total = ref(0)
const loading = ref(false)
const editVisible = ref(false)
const categoryDialogVisible = ref(false)
const newCategoryName = ref('')
const creatingCategory = ref(false)
const selectedImageUuids = ref([])
const recentUploadResources = ref([])
const displayedImages = computed(() => {
  if (!query.visibility) return images.value
  return images.value.filter(img => img.visibility === query.visibility)
})
const visibleImageUuids = computed(() => displayedImages.value.map(img => img.uuid))
const allVisibleSelected = computed(() => visibleImageUuids.value.length > 0 && visibleImageUuids.value.every(uuid => selectedImageUuids.value.includes(uuid)))
const partiallySelected = computed(() => selectedImageUuids.value.length > 0 && !allVisibleSelected.value)
const recentUploadPollingResource = computed(() => {
  const active = recentUploadResources.value.filter(img => img?.uuid && !img.deleted)
  const next = active.find(img => img.status === 'PROCESSING')
    || active.find(img => isAccessUrlExpiring(imageToPollingResource(img)?.url))
    || active[0]
  return imageToPollingResource(next)
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

const query = reactive({
  page: 1,
  limit: DEFAULT_IMAGE_PAGE_SIZE,
  keyword: '',
  categoryId: null,
  visibility: '',
  sortField: 'upload_time',
  sortOrder: 'desc'
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
    if (status.deleted) {
      removePolledImage(status.uuid)
    }
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
    selectedImageUuids.value = selectedImageUuids.value.filter(uuid => displayedImages.value.some(img => img.uuid === uuid))
  } catch {} finally {
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
    ElMessage.warning('请输入分类名')
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
    ElMessage.success('分类创建成功')
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
    ElMessage.success('删除成功')
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
    ElMessage.success('更新成功')
    editVisible.value = false
    fetchList()
  } catch {}
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: var(--gray1);
  display: flex;
  flex-direction: column;
}

.page-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding-bottom: 112px;
}

.page-header {
  padding: 14px 20px;
  margin-bottom: 16px;
  border: 0.5px solid var(--paper3);
  border-radius: 12px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  font-family: var(--font-display);
  font-size: 26px;
  font-weight: 500;
  color: var(--ink);
  line-height: 1.1;
  margin: 0;
}

.page-desc {
  margin: 4px 0 0;
  color: var(--ink3);
  font-size: 16px;
  font-weight: 500;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  flex: 1;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar-left {
  flex: 1;
}

.toolbar-right {
  justify-content: flex-end;
}

.toolbar-search {
  width: 180px;
}

.toolbar-search :deep(.el-input__wrapper) {
  background: #fff !important;
  border: 0.5px solid var(--paper3) !important;
  border-radius: 8px !important;
  box-shadow: none !important;
  height: 32px;
}

.toolbar-select {
  width: 130px;
}

.toolbar-select :deep(.el-input__wrapper) {
  background: #fff !important;
  border: 0.5px solid var(--paper3) !important;
  border-radius: 8px !important;
  box-shadow: none !important;
  height: 32px;
  transition: background .15s, border-color .15s;
}

.toolbar-select :deep(.el-input__wrapper:hover) {
  background: #fff !important;
  border-color: var(--ink5) !important;
}

.selection-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  padding: 0 10px;
  background: #fff;
  border: 0.5px solid var(--paper3);
  border-radius: 8px;
}

.selection-actions :deep(.el-checkbox) {
  height: 32px;
}

.selection-actions :deep(.el-checkbox__label) {
  font-size: 16px;
  color: var(--ink2);
}

.btn-slide {
  height: 32px;
  padding: 7px 14px;
  border-radius: 8px;
  font-size: 16px;
  background: var(--ink) !important;
  border: none !important;
  color: var(--paper) !important;
  font-family: var(--font-body);
  transition: background .15s, transform .1s;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.btn-slide:hover {
  background: var(--ink2) !important;
}

.btn-slide:active {
  transform: scale(.97);
}

.category-row {
  width: 100%;
  display: flex;
  gap: 8px;
  align-items: center;
}
.category-row :deep(.el-select) {
  flex: 1;
}
.category-row .el-button {
  flex-shrink: 0;
}

.pagination-wrap {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 150;
  display: flex;
  justify-content: center;
  padding: 14px 24px calc(14px + env(safe-area-inset-bottom));
  margin-top: 0;
  border-top: 0.5px solid var(--paper3);
  background: #fff;
  flex-shrink: 0;
}

.pagination-wrap :deep(.el-pagination) {
  max-width: min(100%, 1280px);
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
}

.pagination-wrap :deep(.el-pager li) {
  width: 28px;
  height: 28px;
  border-radius: 6px !important;
  border: 0.5px solid var(--paper3) !important;
  background: #fff !important;
  color: var(--ink) !important;
  font-size: 16px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background .15s;
  font-family: var(--font-body);
  min-width: auto;
}

.pagination-wrap :deep(.el-pager li.is-active) {
  background: var(--ink) !important;
  color: var(--paper) !important;
  border-color: var(--ink) !important;
}

.pagination-wrap :deep(.el-pager li:hover:not(.is-active)) {
  background: #fff !important;
}

.pagination-wrap :deep(.btn-prev),
.pagination-wrap :deep(.btn-next) {
  background: #fff !important;
  border: 0.5px solid var(--paper3) !important;
  color: var(--ink) !important;
  border-radius: 6px !important;
  height: 28px !important;
  width: 28px !important;
  min-width: auto !important;
}

.pagination-wrap :deep(.btn-prev:hover),
.pagination-wrap :deep(.btn-next:hover) {
  background: #fff !important;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
  padding-top: var(--space-md);
}
.skeleton-grid .skeleton {
  aspect-ratio: 1;
  border-radius: var(--radius-md);
}

.card-grid > :deep(.stagger-item) {
  animation: fadeUp 0.4s var(--ease-out) forwards;
  opacity: 0;
}

@media (max-width: 768px) {
  .page-container { padding: 20px 8px 148px; }
  .page-header { padding: 14px; }
  .page-title { font-size: 24px; }
  .toolbar { flex-direction: column; align-items: stretch; }
  .toolbar-left, .toolbar-right { flex-wrap: wrap; }
  .toolbar-search,
  .toolbar-select,
  .toolbar-right :deep(.el-select),
  .category-row {
    width: 100% !important;
    grid-template-columns: 1fr;
  }
  .pagination-wrap {
    padding: 10px 12px calc(10px + env(safe-area-inset-bottom));
  }
  .pagination-wrap :deep(.el-pagination) {
    --el-pagination-button-width: 28px;
    --el-pagination-button-height: 28px;
    font-size: 16px;
  }
}
/* Cinematic minimal override */
.home-page {
  min-height: 100vh;
  background: transparent;
  color: var(--paper);
}

.home-page .page-container {
  width: min(100%, 1440px);
  padding: 104px 32px 128px;
}

.page-header {
  position: relative;
  overflow: hidden;
  padding: 38px;
  margin-bottom: 22px;
  display: grid;
  grid-template-columns: minmax(240px, 0.72fr) minmax(0, 1.28fr);
  align-items: end;
  gap: 28px;
  border: 1px solid rgba(255, 253, 248, 0.16);
  border-radius: 24px;
  background:
    linear-gradient(120deg, rgba(7, 17, 31, 0.9) 0%, rgba(7, 17, 31, 0.62) 54%, rgba(7, 17, 31, 0.88) 100%),
    radial-gradient(circle at 20% 12%, rgba(55, 138, 221, 0.34), transparent 34%),
    radial-gradient(circle at 90% 18%, rgba(239, 159, 39, 0.14), transparent 26%);
  box-shadow: var(--shadow-cinematic);
  backdrop-filter: blur(18px);
}

.page-header::before {
  content: none;
  position: absolute;
  right: 32px;
  top: 30px;
  color: rgba(247, 243, 232, 0.1);
  font-size: clamp(48px, 7vw, 108px);
  font-weight: 900;
  letter-spacing: -0.05em;
  pointer-events: none;
}

.page-header > div:first-child {
  position: relative;
  z-index: 1;
}

.page-title {
  margin: 0;
  color: var(--paper);
  font-size: clamp(46px, 6vw, 86px);
  line-height: 0.88;
  letter-spacing: -0.05em;
}

.page-title::before {
  content: none;
  display: block;
  margin-bottom: 16px;
  color: var(--gold2);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.22em;
  text-transform: uppercase;
}

.page-desc {
  max-width: 420px;
  margin: 18px 0 0;
  color: rgba(247, 243, 232, 0.66);
  font-size: 16px;
  line-height: 1.7;
}

.toolbar {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
  align-self: stretch;
  padding: 14px;
  border: 1px solid rgba(255, 253, 248, 0.12);
  border-radius: 20px;
  background: rgba(255, 253, 248, 0.08);
  backdrop-filter: blur(16px);
}

.toolbar-left,
.toolbar-right {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar-left {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) minmax(140px, 170px) minmax(140px, 170px) auto;
}

.toolbar-right {
  justify-content: flex-end;
}

.toolbar-search,
.toolbar-select {
  width: 100%;
}

.toolbar-search :deep(.el-input__wrapper),
.toolbar-select :deep(.el-input__wrapper),
.toolbar-right :deep(.el-input__wrapper) {
  height: 42px;
  background: rgba(255, 253, 248, 0.11) !important;
  border: 1px solid rgba(255, 253, 248, 0.14) !important;
  border-radius: 999px !important;
  box-shadow: none !important;
}

.toolbar-search :deep(.el-input__inner),
.toolbar-select :deep(.el-input__inner),
.toolbar-right :deep(.el-input__inner) {
  color: var(--paper) !important;
  font-size: 14px;
}

.toolbar-search :deep(.el-input__inner::placeholder),
.toolbar-select :deep(.el-input__inner::placeholder) {
  color: rgba(247, 243, 232, 0.52) !important;
}

.selection-actions {
  min-height: 42px;
  padding: 0 14px;
  border: 1px solid rgba(255, 253, 248, 0.14);
  border-radius: 999px;
  background: rgba(255, 253, 248, 0.08);
}

.selection-actions :deep(.el-checkbox__label) {
  color: rgba(247, 243, 232, 0.76);
  font-size: 13px;
  font-weight: 800;
}

.btn-slide {
  height: 42px;
  padding: 0 18px;
  border-radius: 999px;
  background: var(--paper) !important;
  border: 1px solid rgba(255, 253, 248, 0.7) !important;
  color: var(--cinema) !important;
  font-size: 14px;
  font-weight: 800;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.18);
}

.btn-slide:hover {
  background: #fff !important;
  transform: translateY(-1px);
}

.home-page .card-grid {
  grid-template-columns: repeat(auto-fill, minmax(218px, 1fr));
  gap: 20px;
  padding: 22px;
  border: 1px solid rgba(255, 253, 248, 0.12);
  border-radius: 22px;
  background: rgba(7, 17, 31, 0.45);
  box-shadow: var(--shadow-cinematic-soft);
  backdrop-filter: blur(18px);
}

.home-page .empty-state {
  margin-top: 22px;
  border: 1px solid rgba(255, 253, 248, 0.12);
  border-radius: 22px;
  background: rgba(7, 17, 31, 0.42);
  backdrop-filter: blur(18px);
}

.skeleton-grid {
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 18px;
}

.skeleton-grid .skeleton {
  border-radius: 16px;
  background:
    linear-gradient(120deg, rgba(255,253,248,0.06), rgba(255,253,248,0.18), rgba(255,253,248,0.06)),
    rgba(255,253,248,0.08);
}

.pagination-wrap {
  border-top: 1px solid rgba(255, 253, 248, 0.12);
  background: rgba(7, 17, 31, 0.78);
  backdrop-filter: blur(18px);
}

.pagination-wrap :deep(.el-pagination) {
  color: var(--paper);
}

.pagination-wrap :deep(.el-pager li),
.pagination-wrap :deep(.btn-prev),
.pagination-wrap :deep(.btn-next) {
  background: rgba(255, 253, 248, 0.08) !important;
  border: 1px solid rgba(255, 253, 248, 0.12) !important;
  color: rgba(247, 243, 232, 0.78) !important;
}

.pagination-wrap :deep(.el-pager li.is-active) {
  background: var(--paper) !important;
  color: var(--cinema) !important;
  border-color: var(--paper) !important;
}

.category-row {
  gap: 10px;
}

@media (max-width: 1040px) {
  .page-header {
    grid-template-columns: 1fr;
  }
  .toolbar-left {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 768px) {
  .home-page .page-container {
    padding: 92px 14px 148px;
  }
  .page-header {
    padding: 28px 18px;
    border-radius: 20px;
  }
  .toolbar,
  .toolbar-left,
  .toolbar-right {
    display: grid;
    grid-template-columns: 1fr;
    align-items: stretch;
  }
  .toolbar-search,
  .toolbar-select,
  .toolbar-right :deep(.el-select) {
    width: 100% !important;
  }
  .home-page .card-grid {
    grid-template-columns: 1fr;
    padding: 14px;
  }
}

/* Anime paper override */
.home-page {
  color: var(--ink);
}

.page-header {
  border-color: rgba(17, 26, 53, 0.1);
  background: #f0eee6;
  box-shadow: 0 24px 70px rgba(17, 26, 53, 0.12);
}

.page-header::before,
.page-title::before {
  content: none;
}

.page-header::after {
  content: '';
  position: absolute;
  right: 34px;
  top: 26px;
  width: 118px;
  height: 118px;
  border-radius: 34% 66% 58% 42%;
  background:
    radial-gradient(circle at 35% 35%, rgba(255,255,255,0.9), transparent 24%),
    linear-gradient(135deg, rgba(255,122,184,0.72), rgba(88,184,255,0.66));
  opacity: 0.72;
  animation: floatMascot 5.8s var(--ease-in-out) infinite;
  pointer-events: none;
}

.page-title {
  color: var(--ink);
  text-shadow: 0 1px 0 rgba(255,255,255,0.5);
}

.page-desc {
  color: rgba(4, 44, 83, 0.68);
}

.toolbar,
.selection-actions {
  border-color: rgba(17, 26, 53, 0.1);
  background: #f0eee6;
}

.toolbar-search :deep(.el-input__wrapper),
.toolbar-select :deep(.el-input__wrapper),
.toolbar-right :deep(.el-input__wrapper) {
  background: #f0eee6 !important;
  border-color: rgba(17, 26, 53, 0.1) !important;
}

.toolbar-search :deep(.el-input__inner),
.toolbar-select :deep(.el-input__inner),
.toolbar-right :deep(.el-input__inner),
.selection-actions :deep(.el-checkbox__label) {
  color: var(--ink) !important;
}

.toolbar-search :deep(.el-input__inner::placeholder),
.toolbar-select :deep(.el-input__inner::placeholder) {
  color: rgba(4, 44, 83, 0.46) !important;
}

.btn-slide {
  background: var(--cinema) !important;
  border-color: rgba(17, 26, 53, 0.82) !important;
  color: var(--paper) !important;
}

.btn-slide:hover {
  background: var(--anime-pink) !important;
}

.home-page .card-grid,
.home-page .empty-state {
  border-color: rgba(17, 26, 53, 0.1);
  background: #f0eee6;
}

.pagination-wrap {
  border-top-color: rgba(17, 26, 53, 0.12);
  background: #f0eee6;
}

.pagination-wrap :deep(.el-pagination) {
  color: var(--ink);
}

.pagination-wrap :deep(.el-pager li),
.pagination-wrap :deep(.btn-prev),
.pagination-wrap :deep(.btn-next) {
  background: #f0eee6 !important;
  border-color: rgba(17, 26, 53, 0.12) !important;
  color: var(--ink) !important;
}

.pagination-wrap :deep(.el-pager li.is-active) {
  background: var(--cinema) !important;
  color: var(--paper) !important;
  border-color: var(--cinema) !important;
}

@keyframes floatMascot {
  0%, 100% { transform: translate3d(0, 0, 0) rotate(-5deg); }
  50% { transform: translate3d(0, -12px, 0) rotate(5deg); }
}
</style>
