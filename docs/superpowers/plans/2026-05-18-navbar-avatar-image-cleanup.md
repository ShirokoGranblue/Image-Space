# Navbar改进 + 头像同步 + 损坏图片清理 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** NavBar退出按钮右移+红色填充，头像是在用户真实头像，损坏图片加载失败时自动删除

**Architecture:** 纯前端改动，3个文件修改。NavBar.vue调整模板布局和CSS，ImageCard.vue增强图片错误处理逻辑

**Tech Stack:** Vue 3 + Element Plus + Pinia

---

### Task 1: NavBar退出按钮移到最右边并添加红色背景

**Files:**
- Modify: `frontend/src/components/NavBar.vue` (template lines 21-27, style lines 227-244)

- [ ] **Step 1: 修改模板 - 桌面端退出按钮移出 user-section**

将 logout-btn 从 `.user-section` 内移出，作为 `.navbar-right` 最后一个元素：

```html
<div class="user-section" v-if="token">
  <el-avatar :size="32" :src="userInfo?.avatarUrl || userInfo?.avatar" class="nav-avatar" @click="goProfile" />
  <span class="username" @click="goProfile" :title="userInfo?.displayName || userInfo?.username">
    {{ userInfo?.displayName || userInfo?.username || '' }}
  </span>
</div>
<button v-if="token" class="logout-btn logout-btn-danger" @click="handleLogout" aria-label="退出登录">退出</button>
```

- [ ] **Step 2: 修改模板 - 移动端退出按钮也添加红色样式**

```html
<button class="logout-btn logout-btn-danger" @click="handleLogout">退出</button>
```

- [ ] **Step 3: 修改CSS - 替换 .logout-btn 样式，添加红色变体**

```css
.logout-btn {
  background: none;
  border: 1px solid var(--border-subtle);
  color: var(--text-muted);
  font-size: 12px;
  padding: 5px 14px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: var(--font-body);
  white-space: nowrap;
  min-height: 32px;
}
.logout-btn:hover {
  color: var(--danger);
  border-color: var(--danger);
  background: rgba(220, 38, 38, 0.04);
}

.logout-btn-danger {
  background: #dc2626;
  color: #fff;
  border-color: #dc2626;
}
.logout-btn-danger:hover {
  background: #b91c1c;
  border-color: #b91c1c;
  color: #fff;
}
```

- [ ] **Step 4: 验证**

运行 `cd frontend && npm run dev`，检查：
- 登录后navbar退出按钮在最右边
- 退出按钮有红色背景白色文字
- hover时变为深红色
- 移动端drawer中退出按钮也是红色

---

### Task 2: NavBar头像显示用户真实头像

**Files:**
- Modify: `frontend/src/components/NavBar.vue` (template line 22, line 50)

- [ ] **Step 1: 修改桌面端头像src**

```html
<!-- 原来 -->
<el-avatar :size="32" :src="userInfo?.avatar" class="nav-avatar" @click="goProfile" />
<!-- 改为 -->
<el-avatar :size="32" :src="userInfo?.avatarUrl || userInfo?.avatar" class="nav-avatar" @click="goProfile" />
```

- [ ] **Step 2: 修改移动端头像src**

```html
<!-- 原来 -->
<el-avatar :size="28" :src="userInfo?.avatar" />
<!-- 改为 -->
<el-avatar :size="28" :src="userInfo?.avatarUrl || userInfo?.avatar" />
```

- [ ] **Step 3: 验证**

运行 `cd frontend && npm run dev`，检查：
- 登录后navbar显示用户实际头像（非默认图标）
- 在Profile页修改头像后，navbar头像实时同步更新

---

### Task 3: 图片加载失败自动删除

**Files:**
- Modify: `frontend/src/components/ImageCard.vue` (script setup section)

- [ ] **Step 1: 在 ImageCard.vue 中导入 deleteImage API 和 ElMessageBox**

```javascript
import { deleteImage } from '../api/image'
import { ElMessage, ElMessageBox } from 'element-plus'
```

注意：`ElMessage` 可能尚未导入，需一并添加。

- [ ] **Step 2: 修改 emits 添加 'removed' 事件**

```javascript
defineEmits(['edit', 'delete', 'removed'])
```

- [ ] **Step 3: 重写 handleImgError 函数，添加自动删除逻辑**

```javascript
async function handleImgError() {
  imgFailed.value = true
  try {
    await ElMessageBox.confirm(
      '该图片似乎已损坏或为空，是否立即删除？',
      '图片加载失败',
      { confirmButtonText: '删除', cancelButtonText: '保留', type: 'warning' }
    )
    try {
      await deleteImage(props.image.id)
      ElMessage.success('已删除损坏图片')
      imgFailed.value = false
    } catch {
      ElMessage.error('删除失败，请手动删除')
    }
  } catch {
    // 用户选择保留，不做处理
  }
}
```

- [ ] **Step 4: 验证**

运行 `cd frontend && npm run dev`，检查：
- 当图片加载失败时弹出确认框询问是否删除
- 确认后调用API删除，显示成功提示
- 取消则保留fallback图标显示
- 在Home页和Square页都有效

---

### 任务依赖

Task 1 和 Task 2 都在 NavBar.vue 同一文件，可合并执行。Task 3 独立。
