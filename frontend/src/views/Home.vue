<template>
  <div class="home-page">
    <NavBar />
    <div class="page-container">
      <header class="page-header">
        <h1 class="page-title">Collections</h1>
        <div class="toolbar">
          <div class="toolbar-left">
            <el-button type="primary" @click="uploadRef.open()">
              <el-icon><Plus /></el-icon> 上传图片
            </el-button>
            <el-checkbox
              v-if="images.length > 0"
              :model-value="allVisibleSelected"
              :indeterminate="partiallySelected"
              @change="toggleSelectAll"
            >
              全选本页
            </el-checkbox>
            <el-button v-if="selectedImageIds.length > 0" @click="clearSelection">取消选择</el-button>
            <el-button
              v-if="selectedImageIds.length > 0"
              type="danger"
              @click="handleBatchDelete"
            >
              删除选中 {{ selectedImageIds.length }}
            </el-button>
            <el-select
              v-model="query.categoryId"
              placeholder="按分类筛选"
              clearable
              @change="onFilterChange"
              style="width: 160px"
            >
              <el-option v-for="cat in categories" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
            </el-select>
          </div>
          <div class="toolbar-right">
            <el-input
              v-model="query.keyword"
              placeholder="搜索图片名称"
              clearable
              @clear="onFilterChange"
              @keyup.enter="onFilterChange"
              style="width: 220px"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-select v-model="query.sortField" @change="onFilterChange" style="width: 120px">
              <el-option label="按时间" value="upload_time" />
              <el-option label="按名称" value="image_name" />
              <el-option label="按大小" value="file_size" />
            </el-select>
          </div>
        </div>
      </header>

      <div v-if="loading" class="empty-state">
        <div class="skeleton-grid">
          <div class="skeleton" v-for="n in 6" :key="n" style="aspect-ratio:1;"></div>
        </div>
      </div>

      <div v-else-if="images.length === 0" class="empty-state">
        <el-icon><PictureFilled /></el-icon>
        <p>还没有图片，点击上方按钮上传吧</p>
      </div>

      <div v-else class="card-grid">
        <div v-for="(img, idx) in images" :key="img.id" class="stagger-item" :style="{ animationDelay: `${idx * 0.06}s` }">
          <ImageCard
            :image="img"
            :show-actions="true"
            :selectable="true"
            :selected="selectedImageIds.includes(img.id)"
            @delete="handleDelete"
            @edit="handleEdit"
            @toggle-select="toggleImageSelection"
          />
        </div>
      </div>

      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.limit"
          :page-sizes="IMAGE_PAGE_SIZES"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="onPageSizeChange"
          @current-change="fetchList"
        />
      </div>
    </div>

    <ImageUpload ref="uploadRef" @uploaded="handleUploaded" />

    <el-dialog v-model="editVisible" title="编辑图片信息" width="480px">
      <el-form :model="editForm" label-width="86px" v-if="editForm.id">
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import NavBar from '../components/NavBar.vue'
import ImageCard from '../components/ImageCard.vue'
import ImageUpload from '../components/ImageUpload.vue'
import TagInput from '../components/TagInput.vue'
import { getImageList, deleteImage, updateImage } from '../api/image'
import { getCategoryList, createCategory } from '../api/category'
import { DEFAULT_IMAGE_PAGE_SIZE, IMAGE_PAGE_SIZES, buildImageListParams } from '../utils/imageRequests'

const uploadRef = ref(null)
const images = ref([])
const categories = ref([])
const total = ref(0)
const loading = ref(false)
const editVisible = ref(false)
const categoryDialogVisible = ref(false)
const newCategoryName = ref('')
const creatingCategory = ref(false)
const selectedImageIds = ref([])
const visibleImageIds = computed(() => images.value.map(img => img.id))
const allVisibleSelected = computed(() => visibleImageIds.value.length > 0 && visibleImageIds.value.every(id => selectedImageIds.value.includes(id)))
const partiallySelected = computed(() => selectedImageIds.value.length > 0 && !allVisibleSelected.value)

const editForm = reactive({
  id: null,
  imageName: '',
  categoryId: null,
  tags: '',
  description: '',
  visibility: 'PRIVATE',
  visibleUsernames: ''
})

const query = reactive({
  page: 1,
  limit: DEFAULT_IMAGE_PAGE_SIZE,
  keyword: '',
  categoryId: null,
  sortField: 'upload_time',
  sortOrder: 'desc'
})

onMounted(() => {
  fetchList()
  fetchCategories()
})

async function fetchList() {
  loading.value = true
  try {
    const res = await getImageList(buildImageListParams(query))
    images.value = res.data.records || []
    total.value = res.data.total || 0
    selectedImageIds.value = selectedImageIds.value.filter(id => images.value.some(img => img.id === id))
  } catch {} finally {
    loading.value = false
  }
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

async function handleUploaded() {
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

function toggleImageSelection(id) {
  selectedImageIds.value = selectedImageIds.value.includes(id)
    ? selectedImageIds.value.filter(item => item !== id)
    : [...selectedImageIds.value, id]
}

function toggleSelectAll(checked) {
  selectedImageIds.value = checked ? [...visibleImageIds.value] : []
}

function clearSelection() {
  selectedImageIds.value = []
}

async function handleBatchDelete() {
  const ids = [...selectedImageIds.value]
  if (ids.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${ids.length} 张图片吗？`,
      '批量删除图片',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }
  try {
    await Promise.all(ids.map(id => deleteImage(id)))
    ElMessage.success(`已删除 ${ids.length} 张图片`)
    selectedImageIds.value = []
    fetchList()
  } catch {}
}

async function handleDelete(id) {
  try {
    await deleteImage(id)
    ElMessage.success('删除成功')
    fetchList()
  } catch {}
}

function handleEdit(img) {
  editForm.id = img.id
  editForm.imageName = img.imageName
  editForm.categoryId = img.categoryId
  editForm.description = img.description || ''
  editForm.tags = img.tags || ''
  editForm.visibility = img.visibility || 'PRIVATE'
  editForm.visibleUsernames = img.visibleUsernames || ''
  editVisible.value = true
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
    fetchList()
  } catch {}
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: var(--bg-base);
  display: flex;
  flex-direction: column;
}

.page-container {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.page-header {
  padding: 22px 24px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 16px;
  margin-bottom: 22px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
}

.page-title {
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 750;
  color: var(--text-primary);
  letter-spacing: -0.3px;
  line-height: 1.15;
  margin: 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
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

.category-row {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 28px 0 var(--space-lg);
  margin-top: auto;
  flex-shrink: 0;
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
  .page-container { padding: 20px 8px; }
  .page-header { padding: 20px; border-radius: var(--radius-md); }
  .page-title { font-size: 26px; }
  .toolbar { flex-direction: column; align-items: stretch; }
  .toolbar-left, .toolbar-right { flex-wrap: wrap; }
  .toolbar-right :deep(.el-input),
  .toolbar-left :deep(.el-select),
  .category-row {
    width: 100% !important;
    grid-template-columns: 1fr;
  }
}
</style>
