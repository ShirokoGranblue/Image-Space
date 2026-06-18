<template>
  <div class="asset-page">
    <NavBar />

    <div class="asset-shell">
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
        <header class="main-head">
          <div class="headline-block">
            <h1>在一个页面整理、筛选和分享图片。</h1>
            <p>上传新图片，按分类和可见范围快速筛选，也可以批量选择、编辑信息或复制分享链接。</p>
          </div>
          <div class="stats" aria-label="图片统计">
            <div class="stat">
              <strong>{{ total }}</strong>
              <span>全部图片</span>
            </div>
            <div class="stat">
              <strong>{{ categories.length }}</strong>
              <span>分类</span>
            </div>
            <div class="stat">
              <strong>{{ selectedImageUuids.length }}</strong>
              <span>已选择</span>
            </div>
          </div>
        </header>

        <section class="command-panel" aria-label="搜索和筛选">
          <el-input
            v-model="query.keyword"
            placeholder="按图片名称搜索"
            clearable
            class="command-search"
            @clear="onFilterChange"
            @keyup.enter="onFilterChange"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <el-select v-model="query.categoryId" placeholder="选择分类" clearable class="command-select" @change="onFilterChange">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
          </el-select>

          <el-select v-model="query.sortField" class="command-select" @change="onFilterChange">
            <el-option label="最新上传" value="upload_time" />
            <el-option label="名称" value="image_name" />
            <el-option label="文件大小" value="file_size" />
          </el-select>

          <button class="select-all" type="button" :class="{ active: allVisibleSelected }" @click="toggleSelectAll(!allVisibleSelected)">
            {{ allVisibleSelected ? '取消本页' : '选择本页' }}
          </button>

          <button class="primary-command" type="button" @click="uploadRef.open()">
            <el-icon><Plus /></el-icon>
            上传
          </button>
        </section>

        <section class="filter-strip" aria-label="快速筛选">
          <button class="filter-chip" :class="{ active: !query.visibility }" type="button" @click="setVisibilityFilter('')">全部</button>
          <button class="filter-chip" :class="{ active: query.visibility === 'PUBLIC' }" type="button" @click="setVisibilityFilter('PUBLIC')">公开</button>
          <button class="filter-chip" :class="{ active: query.visibility === 'SPECIFIED' }" type="button" @click="setVisibilityFilter('SPECIFIED')">指定用户</button>
          <button class="filter-chip" :class="{ active: query.visibility === 'PRIVATE' }" type="button" @click="setVisibilityFilter('PRIVATE')">仅自己</button>
          <button class="filter-chip danger" v-if="selectedImageUuids.length > 0" type="button" @click="handleBatchDelete">
            删除 {{ selectedImageUuids.length }}
          </button>
        </section>

        <section class="gallery-stage">
          <div v-if="loading" class="skeleton-grid">
            <div class="asset-skeleton" v-for="n in 8" :key="n"></div>
          </div>

          <div v-else-if="displayedImages.length === 0" class="empty-state asset-empty">
            <el-icon><PictureFilled /></el-icon>
            <p>没有符合条件的图片。</p>
          </div>

          <div v-else class="asset-grid">
            <ImageCard
              v-for="img in displayedImages"
              :key="img.uuid || img.id"
              :image="img"
              :show-actions="true"
              :selectable="true"
              :selected="selectedImageUuids.includes(img.uuid)"
              @delete="handleDelete"
              @copy="copyImageLink"
              @toggle-select="toggleImageSelection"
            />
          </div>
        </section>

        <footer class="batch-queue" v-if="displayedImages.length > 0">
          <span>已选图片</span>
          <div class="queue-strip">
            <button
              v-for="img in selectedPreviewImages"
              :key="img.uuid"
              class="queue-thumb"
              type="button"
              :title="img.imageName"
              @click="toggleImageSelection(img.uuid)"
            >
              <img v-if="getImageDisplayUrl(img)" :src="getImageDisplayUrl(img)" :alt="img.imageName" />
            </button>
          </div>
          <div class="queue-actions">
            <button class="secondary-command" type="button" @click="clearSelection" :disabled="selectedImageUuids.length === 0">清空</button>
            <button class="primary-command compact" type="button" @click="handleBatchDelete" :disabled="selectedImageUuids.length === 0">删除</button>
          </div>
        </footer>
      </main>

      <aside class="asset-inspector" aria-label="图片信息">
        <div class="inspector-card" v-if="inspectedImage">
          <div class="inspect-preview">
            <img v-if="inspectedImageSrc" :src="inspectedImageSrc" :alt="inspectedImage.imageName" />
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

        <div class="inspector-card empty-inspector" v-else>
          <h2>还没有选择图片</h2>
          <p>选择一张图片后，可以在这里查看信息并处理分享。</p>
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
import NavBar from '../components/NavBar.vue'
import ImageCard from '../components/ImageCard.vue'
import ImageUpload from '../components/ImageUpload.vue'
import TagInput from '../components/TagInput.vue'
import { getImageList, deleteImage, updateImage } from '../api/image'
import { getImageResourceStatus, refreshImageAccessUrl } from '../api/resource'
import { getCategoryList, createCategory } from '../api/category'
import { DEFAULT_IMAGE_PAGE_SIZE, IMAGE_PAGE_SIZES, buildImageListParams, getImageDisplayUrl, getImageDownloadUrl, getImagePreviewUrl } from '../utils/imageRequests'
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
const inspectedImage = computed(() => selectedPreviewImages.value[0] || displayedImages.value[0] || null)
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
.asset-page {
  min-height: 100vh;
  color: var(--ad-text);
}

.asset-shell {
  width: min(100% - 28px, 1600px);
  min-height: calc(100vh - 28px);
  margin: 0 auto;
  padding: 96px 0 96px;
  display: grid;
  grid-template-columns: 212px minmax(0, 1fr) 340px;
  gap: 0;
}

.workspace-rail,
.asset-main,
.asset-inspector {
  border-top: 1px solid var(--ad-line);
  border-bottom: 1px solid var(--ad-line);
  background: rgba(17, 23, 34, 0.62);
}

.workspace-rail {
  padding: 18px 14px;
  border-left: 1px solid var(--ad-line);
  border-right: 1px solid var(--ad-line);
  border-radius: 18px 0 0 18px;
}

.rail-section + .rail-section {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid var(--ad-line);
}

.rail-title,
.queue-title {
  margin: 0 0 10px;
  color: rgba(244, 241, 232, 0.46);
  font-size: 11px;
  font-weight: 860;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.rail-item {
  width: 100%;
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr) auto;
  align-items: center;
  gap: 9px;
  min-height: 38px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: var(--ad-text-soft);
  font-size: 13px;
  font-weight: 720;
  text-align: left;
}

.rail-item:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.rail-item.active,
.rail-item:hover:not(:disabled) {
  border-color: rgba(183, 255, 60, 0.26);
  background: rgba(183, 255, 60, 0.1);
  color: var(--ad-text);
}

.rail-item strong {
  color: var(--ad-muted);
  font-size: 11px;
  font-weight: 760;
}

.dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--ad-muted);
}

.dot.green { background: var(--ad-green); }
.dot.cyan { background: var(--ad-cyan); }
.dot.amber { background: var(--ad-amber); }
.dot.coral { background: var(--ad-coral); }
.dot.violet { background: var(--ad-violet); }

.meter-card {
  padding: 12px;
  border: 1px solid var(--ad-line);
  border-radius: 12px;
  background: rgba(13, 16, 22, 0.52);
}

.meter-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  color: var(--ad-text-soft);
  font-size: 12px;
  font-weight: 720;
}

.meter {
  overflow: hidden;
  height: 7px;
  margin-top: 10px;
  border-radius: 999px;
  background: rgba(244, 241, 232, 0.08);
}

.meter span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--ad-green), var(--ad-cyan));
  transition: width 0.24s var(--ad-ease);
}

.asset-main {
  min-width: 0;
  display: grid;
  grid-template-rows: auto auto auto minmax(0, 1fr) auto;
  border-right: 1px solid var(--ad-line);
  background: rgba(13, 16, 22, 0.36);
}

.main-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: end;
  padding: 24px 24px 18px;
  border-bottom: 1px solid var(--ad-line);
}

.headline-block h1 {
  max-width: 760px;
  margin: 0;
  color: var(--ad-text);
  font-size: clamp(42px, 5.2vw, 80px);
  font-weight: 340;
  line-height: 0.95;
  letter-spacing: 0;
}

.headline-block p {
  max-width: 720px;
  margin: 14px 0 0;
  color: var(--ad-text-soft);
  font-size: 15px;
  line-height: 1.7;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, 112px);
  gap: 8px;
}

.stat {
  min-height: 82px;
  padding: 12px;
  border: 1px solid var(--ad-line);
  border-radius: 12px;
  background: rgba(21, 25, 34, 0.84);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.14);
}

.stat strong {
  display: block;
  color: var(--ad-text);
  font-size: 24px;
  font-weight: 820;
  line-height: 1;
}

.stat span {
  display: block;
  margin-top: 8px;
  color: var(--ad-muted);
  font-size: 12px;
  font-weight: 720;
}

.command-panel {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 170px 140px auto auto;
  gap: 10px;
  align-items: center;
  padding: 16px 24px 0;
}

.command-search,
.command-select {
  width: 100%;
}

.select-all,
.primary-command,
.secondary-command,
.filter-chip {
  min-height: 40px;
  border: 1px solid var(--ad-line);
  border-radius: 10px;
  background: rgba(21, 25, 34, 0.86);
  color: var(--ad-text);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 13px;
  font-family: var(--ad-font);
  font-size: 13px;
  font-weight: 780;
  transition: background 0.16s ease, border-color 0.16s ease, color 0.16s ease, transform 0.16s var(--ad-ease);
}

.select-all:hover,
.secondary-command:hover,
.filter-chip:hover {
  border-color: var(--ad-line-strong);
  background: var(--ad-surface-2);
}

.select-all.active,
.filter-chip.active,
.primary-command {
  background: var(--ad-green);
  border-color: var(--ad-green);
  color: #071014;
}

.primary-command:hover {
  background: var(--ad-green-2);
  border-color: var(--ad-green-2);
  transform: translateY(-1px);
}

.primary-command:disabled,
.secondary-command:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none;
}

.primary-command.compact {
  min-height: 38px;
}

.danger-text,
.filter-chip.danger {
  color: #fff;
  border-color: rgba(255, 107, 87, 0.48);
  background: rgba(255, 107, 87, 0.14);
}

.filter-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 14px 24px 0;
}

.filter-chip {
  min-height: 32px;
  border-radius: 999px;
  padding: 0 11px;
  color: var(--ad-muted);
  font-size: 12px;
}

.gallery-stage {
  min-width: 0;
  padding: 18px 24px 24px;
}

.asset-grid,
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(214px, 1fr));
  gap: 12px;
}

.asset-skeleton {
  min-height: 260px;
  border: 1px solid var(--ad-line);
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(244, 241, 232, 0.08), rgba(244, 241, 232, 0.02));
  animation: skPulse 1.8s ease-in-out infinite;
}

.asset-empty {
  min-height: 360px;
  border: 1px solid var(--ad-line);
  border-radius: 16px;
  background: rgba(21, 25, 34, 0.58);
  color: var(--ad-text-soft);
}

.batch-queue {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 13px 18px;
  border-top: 1px solid var(--ad-line);
  background: rgba(17, 23, 34, 0.86);
}

.batch-queue > span {
  color: rgba(244, 241, 232, 0.46);
  font-size: 11px;
  font-weight: 860;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.queue-strip {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
}

.queue-thumb {
  width: 52px;
  height: 40px;
  flex: 0 0 auto;
  overflow: hidden;
  border: 1px solid var(--ad-line);
  border-radius: 8px;
  background: linear-gradient(135deg, var(--ad-cyan), var(--ad-violet));
  padding: 0;
}

.queue-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.queue-actions,
.inspect-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.asset-inspector {
  padding: 18px;
  border-right: 1px solid var(--ad-line);
  border-radius: 0 18px 18px 0;
}

.inspector-card {
  overflow: hidden;
  border: 1px solid var(--ad-line);
  border-radius: 16px;
  background: rgba(21, 25, 34, 0.94);
  box-shadow: var(--ad-shadow-soft);
}

.inspect-preview {
  min-height: 244px;
  background:
    linear-gradient(180deg, rgba(244, 241, 232, 0.08), transparent 38%),
    linear-gradient(135deg, rgba(56, 213, 255, 0.56), rgba(155, 140, 255, 0.5), rgba(13, 16, 22, 0.94));
}

.inspect-preview img {
  width: 100%;
  height: 244px;
  display: block;
  object-fit: cover;
}

.inspect-fallback {
  min-height: 244px;
  display: grid;
  place-items: center;
  color: var(--ad-text-soft);
}

.inspect-body {
  display: grid;
  gap: 16px;
  padding: 16px;
}

.inspect-head {
  display: flex;
  gap: 12px;
  justify-content: space-between;
  align-items: start;
}

.inspect-head h2,
.empty-inspector h2 {
  margin: 0;
  color: var(--ad-text);
  font-size: 26px;
  line-height: 1;
  font-weight: 420;
}

.inspect-head p,
.empty-inspector p {
  margin: 6px 0 0;
  color: var(--ad-muted);
  font-size: 12px;
  word-break: break-all;
}

.inspect-status {
  min-height: 24px;
  padding: 5px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 860;
  white-space: nowrap;
}

.inspect-status.public { background: var(--ad-green); color: #071014; }
.inspect-status.private { background: var(--ad-coral); color: #fff; }
.inspect-status.specified { background: var(--ad-amber); color: #17100a; }

.meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.meta {
  padding: 10px;
  border: 1px solid var(--ad-line);
  border-radius: 10px;
  background: rgba(13, 16, 22, 0.44);
}

.meta span {
  display: block;
  color: var(--ad-muted);
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.meta strong {
  display: block;
  margin-top: 5px;
  color: var(--ad-text);
  font-size: 13px;
  font-weight: 760;
  word-break: break-word;
}

.route-box {
  display: grid;
  gap: 9px;
  padding: 12px;
  border: 1px solid rgba(183, 255, 60, 0.22);
  border-radius: 12px;
  background: rgba(183, 255, 60, 0.08);
}

.route-line {
  display: grid;
  grid-template-columns: 66px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  color: var(--ad-text-soft);
  font-size: 11px;
}

.route-line code {
  min-width: 0;
  color: var(--ad-green);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-inspector {
  padding: 18px;
}

.pagination-wrap {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 150;
  display: flex;
  justify-content: center;
  padding: 12px 24px calc(12px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--ad-line);
  background: rgba(13, 16, 22, 0.88);
  backdrop-filter: blur(18px);
}

.pagination-wrap :deep(.el-pagination) {
  color: var(--ad-text);
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

@media (max-width: 1260px) {
  .asset-shell {
    grid-template-columns: 76px minmax(0, 1fr);
  }
  .asset-inspector {
    display: none;
  }
  .workspace-rail {
    border-radius: 18px 0 0 18px;
  }
  .asset-main {
    border-radius: 0 18px 18px 0;
  }
  .rail-title,
  .rail-item span:not(.dot),
  .rail-item strong,
  .meter-card {
    display: none;
  }
  .rail-item {
    grid-template-columns: 1fr;
    justify-items: center;
  }
}

@media (max-width: 980px) {
  .asset-shell {
    width: min(100% - 20px, 1600px);
    grid-template-columns: 1fr;
    padding-top: 92px;
  }
  .workspace-rail {
    display: none;
  }
  .asset-main {
    border: 1px solid var(--ad-line);
    border-radius: 18px;
  }
  .main-head,
  .command-panel {
    grid-template-columns: 1fr;
  }
  .stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .batch-queue {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .headline-block h1 {
    font-size: 42px;
  }
  .stats {
    grid-template-columns: 1fr;
  }
  .asset-grid,
  .skeleton-grid {
    grid-template-columns: 1fr;
  }
}
</style>
