<template>
  <div class="square-page">
    <NavBar />
    <div class="page-container">
      <header class="page-header">
        <h1 class="page-title">Gallery</h1>
        <p class="page-desc">创作者公开作品</p>
      </header>

      <div v-if="images.length === 0" class="empty-state">
        <el-icon><PictureFilled /></el-icon>
        <p>图片广场暂时没有内容</p>
      </div>

      <div v-else class="card-grid">
        <div v-for="(img, idx) in images" :key="img.id" class="stagger-item" :style="{ animationDelay: `${idx * 0.06}s` }">
          <ImageCard :image="img" :show-actions="false" />
        </div>
      </div>

      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          :page-size="limit"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchList"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import NavBar from '../components/NavBar.vue'
import ImageCard from '../components/ImageCard.vue'
import { getImageSquare } from '../api/image'

const images = ref([])
const page = ref(1)
const limit = 12
const total = ref(0)

onMounted(() => fetchList())

async function fetchList() {
  try {
    const res = await getImageSquare(page.value, limit)
    images.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {}
}
</script>

<style scoped>
.square-page {
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

.card-grid {
  flex: 1;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: var(--space-xl) 0 var(--space-lg);
  margin-top: auto;
  flex-shrink: 0;
}

.card-grid > :deep(.stagger-item) {
  animation: fadeUp 0.45s ease forwards;
  opacity: 0;
}
</style>
