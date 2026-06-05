<template>
  <div class="home-page">
    <NavBar />
    <div class="page-container">
      <header class="page-header">
        <div>
          <h1 class="page-title">Images</h1>
          <p class="page-desc">管理你的图片、分类和公开范围</p>
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
  padding: 22px;
  margin-bottom: 8px;
  border: 1px solid rgba(229, 224, 212, 0.9);
  border-radius: 24px;
  background: rgba(255, 253, 248, 0.72);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 18px;
}

.page-title {
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 400;
  color: var(--nav-blue);
  letter-spacing: 0;
  line-height: 1.1;
  margin: 0;
}

.page-desc {
  margin: 8px 0 0;
  color: var(--gray3);
  font-size: 13px;
  font-weight: 300;
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
  width: 220px;
}

.toolbar-select {
  width: 150px;
}

.selection-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 0 12px;
  background: #fff;
  border: 1px solid var(--gray2);
  border-radius: 14px;
}

.btn-slide {
  position: relative;
  overflow: hidden;
  z-index: 1;
  background: var(--nav-blue) !important;
  border-color: var(--nav-blue) !important;
  transition: transform 0.16s ease, background 0.18s ease, border-color 0.18s ease !important;
}

.btn-slide:hover {
  border-color: var(--accent) !important;
  background: var(--accent) !important;
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
  z-index: 90;
  display: flex;
  justify-content: center;
  padding: 12px 24px calc(12px + env(safe-area-inset-bottom));
  margin-top: 0;
  border-top: 1px solid var(--gray2);
  background: rgba(245, 244, 237, 0.94);
  backdrop-filter: blur(12px);
  flex-shrink: 0;
}

.pagination-wrap :deep(.el-pagination) {
  max-width: min(100%, 1280px);
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
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
  .page-header { padding: 18px; }
  .page-title { font-size: 26px; }
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
    font-size: 12px;
  }
}
</style>
