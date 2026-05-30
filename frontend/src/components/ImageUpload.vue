<template>
  <el-dialog v-model="visible" title="上传图片" width="640px" @close="resetForm" class="upload-dialog">
    <el-form :model="form" label-width="86px">
      <el-form-item label="选择图片">
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :limit="10"
          :accept="'image/jpeg,image/png,image/webp,image/gif'"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :on-exceed="handleExceed"
          v-model:file-list="fileList"
          class="upload-area"
          list-type="picture"
          multiple
          drag
        >
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text">将图片拖到此处，或<em>点击上传</em></div>
          <template #tip>
            <div class="upload-tip">支持 JPG/PNG/WEBP/GIF，单文件 ≤ 20MB</div>
          </template>
        </el-upload>
      </el-form-item>

      <el-form-item v-if="fileList.length === 1" label="图片名称">
        <el-input v-model="fileNames[fileList[0].uid]" placeholder="留空使用原文件名">
          <template #append>.{{ getFileExt(fileList[0].name) }}</template>
        </el-input>
      </el-form-item>

      <el-form-item v-else-if="fileList.length > 1" label="图片名称">
        <div class="rename-list">
          <div v-for="file in fileList" :key="file.uid" class="rename-row">
            <span class="rename-original" :title="file.name">{{ file.name }}</span>
            <el-input v-model="fileNames[file.uid]" placeholder="留空使用原文件名">
              <template #append>.{{ getFileExt(file.name) }}</template>
            </el-input>
          </div>
        </div>
      </el-form-item>

      <el-form-item label="分类">
        <div class="category-row">
          <el-select
            v-model="form.categoryId"
            placeholder="选择分类"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.categoryName"
              :value="cat.id"
            />
          </el-select>
          <el-button @click="openCreateCategory">新建分类</el-button>
        </div>
      </el-form-item>

      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="2" placeholder="添加描述" />
      </el-form-item>
      <el-form-item label="标签">
        <TagInput v-model="form.tags" placeholder="多个标签用 # 分隔" />
      </el-form-item>
      <el-form-item label="可见权限">
        <el-select v-model="form.visibility" style="width: 100%">
          <el-option label="仅自己" value="PRIVATE" />
          <el-option label="公开" value="PUBLIC" />
          <el-option label="指定用户" value="SPECIFIED" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.visibility === 'SPECIFIED'" label="指定用户">
        <el-input v-model="form.visibleUsernames" placeholder="输入用户名，多个用户用逗号或空格分隔" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleUpload" :loading="uploading">
        {{ uploading ? '上传中...' : '开始上传' }}
      </el-button>
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
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadImage } from '../api/image'
import { getCategoryList, createCategory } from '../api/category'
import TagInput from './TagInput.vue'

const visible = ref(false)
const uploading = ref(false)
const fileList = ref([])
const categories = ref([])
const uploadRef = ref(null)
const fileNames = reactive({})
const categoryDialogVisible = ref(false)
const newCategoryName = ref('')
const creatingCategory = ref(false)

const emit = defineEmits(['uploaded'])

const form = reactive({
  categoryId: null,
  description: '',
  tags: '',
  visibility: 'PRIVATE',
  visibleUsernames: ''
})

async function open() {
  visible.value = true
  await fetchCategories()
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
    form.categoryId = category.id
    categoryDialogVisible.value = false
    ElMessage.success('分类创建成功')
  } catch {} finally {
    creatingCategory.value = false
  }
}

const ALLOWED_EXT = ['jpg', 'jpeg', 'png', 'webp', 'gif']

function getFileExt(filename) {
  return filename.split('.').pop()?.toLowerCase() || ''
}

function getFileBody(filename) {
  const index = filename.lastIndexOf('.')
  return index > 0 ? filename.slice(0, index) : filename
}

function handleFileChange(file, uploadFiles) {
  const ext = getFileExt(file.name)
  if (!ext || !ALLOWED_EXT.includes(ext)) {
    ElMessage.error(`文件 ${file.name} 格式不支持，仅允许 JPG/PNG/WEBP/GIF`)
    fileList.value = uploadFiles.filter(f => f.uid !== file.uid)
    return
  }
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.warning(`文件 ${file.name} 超过20MB限制`)
    fileList.value = uploadFiles.filter(f => f.uid !== file.uid)
    return
  }
  if (fileNames[file.uid] == null) {
    fileNames[file.uid] = getFileBody(file.name)
  }
}

function handleFileRemove(file) {
  delete fileNames[file.uid]
}

function handleExceed() {
  ElMessage.warning('最多选择10张图片')
}

function getUploadName(file) {
  const body = String(fileNames[file.uid] || '').trim()
  const originalBody = getFileBody(file.name)
  return body && body !== originalBody ? body : ''
}

async function handleUpload() {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择图片')
    return
  }
  uploading.value = true
  let success = 0
  let errors = []
  for (const file of fileList.value) {
    try {
      const fd = new FormData()
      fd.append('file', file.raw)
      const imageName = getUploadName(file)
      if (imageName) fd.append('imageName', imageName)
      if (form.categoryId) fd.append('categoryId', form.categoryId)
      if (form.description) fd.append('description', form.description)
      if (form.tags) fd.append('tags', form.tags)
      fd.append('visibility', form.visibility)
      if (form.visibility === 'SPECIFIED') fd.append('visibleUsernames', form.visibleUsernames)
      await uploadImage(fd)
      success++
    } catch (e) {
      errors.push(file.name + ': ' + (e?.response?.data?.message || e?.message || '上传失败'))
    }
  }
  uploading.value = false
  if (errors.length) {
    ElMessage.error(errors.join('; '))
  }
  ElMessage.success(`成功上传 ${success} / ${fileList.value.length} 张图片`)
  visible.value = false
  emit('uploaded')
}

function resetForm() {
  uploadRef.value?.clearFiles()
  fileList.value = []
  Object.keys(fileNames).forEach(key => delete fileNames[key])
  form.categoryId = null
  form.description = ''
  form.tags = ''
  form.visibility = 'PRIVATE'
  form.visibleUsernames = ''
}

defineExpose({ open })
</script>

<style scoped>
.upload-dialog :deep(.el-dialog__body) {
  overflow: hidden;
}

.upload-dialog :deep(.el-dialog) {
  border-radius: 2px;
}

.upload-area {
  width: 100%;
  overflow: hidden;
}

.upload-area :deep(.el-upload-dragger) {
  width: 100%;
  box-sizing: border-box;
  padding: 36px 18px;
  background: var(--gray1);
  border: 2px dashed var(--gray2);
  border-radius: 2px;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.upload-area :deep(.el-upload-dragger:hover) {
  border-color: var(--accent);
  background: var(--gray1);
}

.upload-icon {
  font-size: 40px;
  color: var(--gray3);
  margin-bottom: 8px;
}

.upload-text {
  color: var(--gray3);
  font-family: 'DM Sans', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 14px;
  font-weight: 400;
}
.upload-text em {
  color: var(--accent);
  font-style: normal;
  font-weight: 500;
}

.upload-tip {
  color: var(--gray3);
  font-family: 'DM Sans', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 12px;
  margin-top: 4px;
}

.upload-area :deep(.el-upload-list) {
  width: 100%;
  max-height: 220px;
  overflow-y: auto;
  overflow-x: hidden;
}

.upload-area :deep(.el-upload-list__item) {
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

.upload-area :deep(.el-upload-list__item-name),
.upload-area :deep(.el-upload-list__item .el-upload-list__item-info) {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
  vertical-align: middle;
}

.upload-area :deep(.el-upload-list__item-thumbnail),
.upload-area :deep(.el-upload-list__item-status-label) {
  flex-shrink: 0;
}

.upload-area :deep(.el-upload-list__item .el-icon--close-tip) {
  display: none;
}

.category-row {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
}

.rename-list {
  width: 100%;
  display: grid;
  gap: 10px;
  padding: 10px;
  border-radius: 2px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
}

.rename-row {
  display: grid;
  grid-template-columns: minmax(120px, 0.8fr) minmax(180px, 1.2fr);
  gap: 10px;
  align-items: center;
}

.rename-original {
  min-width: 0;
  color: var(--text-muted);
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .category-row,
  .rename-row {
    grid-template-columns: 1fr;
  }
}
</style>
