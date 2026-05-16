<template>
  <el-dialog v-model="visible" title="上传图片" width="600px" @close="resetForm" class="upload-dialog">
    <el-form :model="form" label-width="80px">
      <el-form-item label="选择图片">
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :limit="10"
          :accept="'image/jpeg,image/png,image/webp'"
          :on-change="handleFileChange"
          :on-exceed="handleExceed"
          v-model:file-list="fileList"
          class="upload-area"
          list-type="picture"
          multiple
          drag
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">将图片拖到此处，或<em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">支持 JPG/PNG/WEBP，单文件 ≤ 20MB</div>
          </template>
        </el-upload>
      </el-form-item>
      <el-form-item label="分类">
        <el-select
          v-model="form.categoryId"
          placeholder="选择或输入分类"
          clearable
          filterable
          allow-create
          default-first-option
          @change="onCategoryChange"
          style="width: 100%"
        >
          <el-option
            v-for="cat in categories"
            :key="cat.id"
            :label="cat.categoryName"
            :value="cat.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="2" placeholder="添加描述" />
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
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadImage } from '../api/image'
import { getCategoryList, createCategory } from '../api/category'

const visible = ref(false)
const uploading = ref(false)
const fileList = ref([])
const categories = ref([])
const uploadRef = ref(null)

const emit = defineEmits(['uploaded'])

const form = reactive({
  categoryId: null,
  description: '',
  visibility: 'PRIVATE',
  visibleUsernames: ''
})

async function open() {
  visible.value = true
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
  } catch {}
}

async function onCategoryChange(val) {
  if (val && typeof val === 'string') {
    try {
      const res = await createCategory(val)
      form.categoryId = res.data.id
      categories.value.push(res.data)
    } catch {}
  }
}

const ALLOWED_EXT = ['jpg', 'jpeg', 'png', 'webp']

function handleFileChange(file, uploadFiles) {
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!ext || !ALLOWED_EXT.includes(ext)) {
    ElMessage.error(`文件 ${file.name} 格式不支持，仅允许 JPG/PNG/WEBP`)
    fileList.value = uploadFiles.filter(f => f.uid !== file.uid)
    return
  }
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.warning(`文件 ${file.name} 超过20MB限制`)
    fileList.value = uploadFiles.filter(f => f.uid !== file.uid)
  }
}

function handleExceed() {
  ElMessage.warning('最多选择10张图片')
}

async function handleUpload() {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择图片')
    return
  }
  uploading.value = true
  let success = 0
  for (const file of fileList.value) {
    try {
      const fd = new FormData()
      fd.append('file', file.raw)
      if (form.categoryId) fd.append('categoryId', form.categoryId)
      if (form.description) fd.append('description', form.description)
      fd.append('visibility', form.visibility)
      if (form.visibility === 'SPECIFIED') fd.append('visibleUsernames', form.visibleUsernames)
      await uploadImage(fd)
      success++
    } catch {}
  }
  uploading.value = false
  ElMessage.success(`成功上传 ${success} / ${fileList.value.length} 张图片`)
  visible.value = false
  emit('uploaded')
}

function resetForm() {
  uploadRef.value?.clearFiles()
  fileList.value = []
  form.categoryId = null
  form.description = ''
  form.visibility = 'PRIVATE'
  form.visibleUsernames = ''
}

defineExpose({ open })
</script>

<style scoped>
/* Prevent dialog body from expanding beyond dialog width */
.upload-dialog :deep(.el-dialog__body) {
  overflow: hidden;
}

.upload-dialog :deep(.el-dialog) {
  border-radius: var(--radius-md);
}

/* Constrain the entire upload component */
.upload-area {
  width: 100%;
  overflow: hidden;
}

/* Drag zone: prevent overflow, fixed padding */
.upload-area :deep(.el-upload-dragger) {
  width: 100%;
  box-sizing: border-box;
  padding: 24px 16px;
  background: var(--bg-elevated);
  border-color: var(--border-visible);
  border-radius: var(--radius-md);
}

/* File list container: scroll when many files, fixed width */
.upload-area :deep(.el-upload-list) {
  width: 100%;
  max-height: 220px;
  overflow-y: auto;
  overflow-x: hidden;
}

/* Each file list item: full width, no overflow */
.upload-area :deep(.el-upload-list__item) {
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

/* File name: single-line truncation with ellipsis */
.upload-area :deep(.el-upload-list__item-name),
.upload-area :deep(.el-upload-list__item .el-upload-list__item-info) {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
  vertical-align: middle;
}

/* Thumbnail in picture mode: fixed size, don't grow */
.upload-area :deep(.el-upload-list__item-thumbnail) {
  flex-shrink: 0;
}

/* Status label: don't overflow */
.upload-area :deep(.el-upload-list__item-status-label) {
  flex-shrink: 0;
}

/* File name label in list */
.upload-area :deep(.el-upload-list__item .el-icon--close-tip) {
  display: none;
}
</style>
