<template>
  <div class="home-page">
    <NavBar />
    <div class="page-container">
      <header class="page-header">
        <div class="header-top">
          <h1 class="page-title">Collection</h1>
          <p class="page-desc">你的私人影像收藏</p>
        </div>
        <div class="toolbar">
          <div class="toolbar-left">
            <el-button type="primary" @click="uploadRef.open()">
              <el-icon><Plus /></el-icon> 上传图片
            </el-button>
            <el-select v-model="query.categoryId" placeholder="按分类筛选" clearable @change="onFilterChange"
              style="width: 160px; margin-left: 12px">
              <el-option v-for="cat in categories" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
            </el-select>
          </div>
          <div class="toolbar-right">
            <el-input v-model="query.keyword" placeholder="搜索图片名称" clearable @clear="onFilterChange"
              @keyup.enter="onFilterChange" style="width: 220px">
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-select v-model="query.sortField" @change="onFilterChange" style="width: 120px; margin-left: 8px">
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
            @delete="handleDelete"
            @edit="handleEdit"
          />
        </div>
      </div>

      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.limit"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchList"
        />
      </div>
    </div>

    <ImageUpload ref="uploadRef" @uploaded="fetchList" />

    <el-dialog v-model="editVisible" title="编辑图片信息" width="450px">
      <el-form :model="editForm" label-width="80px" v-if="editForm.id">
        <el-form-item label="图片名称">
          <el-input v-model="editForm.imageName" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select
            v-model="editForm.categoryId"
            placeholder="选择或输入分类"
            clearable
            filterable
            allow-create
            default-first-option
            @change="onEditCategoryChange"
            style="width: 100%"
          >
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import NavBar from '../components/NavBar.vue'
import ImageCard from '../components/ImageCard.vue'
import ImageUpload from '../components/ImageUpload.vue'
import { getImageList, deleteImage, updateImage } from '../api/image'
import { getCategoryList, createCategory } from '../api/category'

const uploadRef = ref(null)
const images = ref([])
const categories = ref([])
const total = ref(0)
const loading = ref(false)
const editVisible = ref(false)
const editForm = reactive({
  id: null,
  imageName: '',
  categoryId: null,
  description: '',
  visibility: 'PRIVATE',
  visibleUsernames: ''
})

const query = reactive({
  page: 1,
  limit: 12,
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
    const res = await getImageList(query)
    images.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {} finally {
    loading.value = false
  }
}

function onFilterChange() {
  query.page = 1
  fetchList()
}

async function onEditCategoryChange(val) {
  if (val && typeof val === 'string') {
    try {
      const res = await createCategory(val)
      editForm.categoryId = res.data.id
      categories.value.push(res.data)
    } catch {}
  }
}

async function fetchCategories() {
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
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
  padding-top: 28px;
}

.page-header {
  padding: 24px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  margin-bottom: 24px;
  background: var(--bg-surface);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.header-top {
  margin-bottom: var(--space-md);
}

.page-title {
  font-family: var(--font-display);
  font-size: 34px;
  font-weight: 750;
  color: var(--text-primary);
  letter-spacing: 0;
}

.page-desc {
  font-family: var(--font-display);
  font-size: 16px;
  font-style: normal;
  color: var(--text-muted);
  margin-top: var(--space-xs);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-left :deep(.el-select) {
  margin-left: 0 !important;
}

.toolbar-right :deep(.el-select) {
  margin-left: 0 !important;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: var(--space-xl) 0 var(--space-lg);
  margin-top: auto;
  flex-shrink: 0;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
  padding-top: var(--space-md);
}
.skeleton-grid .skeleton {
  aspect-ratio: 1;
  border-radius: var(--radius-md);
}

.card-grid > :deep(.stagger-item) {
  animation: fadeUp 0.45s ease forwards;
  opacity: 0;
}
</style>
