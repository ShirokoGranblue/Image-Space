# Navbar改进 + 头像同步 + 损坏图片清理

## Task 1: Navbar退出按钮右移 + 红色填充

**文件**: `frontend/src/components/NavBar.vue`

- 模板: 将 logout-btn 从 `.user-section` 内移出，作为 `.navbar-right` 最后子元素
- 样式: 添加 `.logout-btn-danger` 类，红色背景 `#dc2626`，白色文字，hover 时深红色
- 移动端 drawer 同步保持一致样式

## Task 2: Navbar头像显示真实头像 + 同步更新

**文件**: `frontend/src/components/NavBar.vue`

- 模板: `:src="userInfo?.avatar"` → `:src="userInfo?.avatarUrl || userInfo?.avatar"`
- 桌面端和移动端两处都修改
- Vue reactivity 自动处理同步，无需额外代码

## Task 3: 图片加载失败自动删除

**文件**: `frontend/src/components/ImageCard.vue`

- `<img>` 添加 `@error` 处理器
- 加载失败时弹出确认框，确认后调用 delete API
- 删除成功后 emit 事件通知父组件从列表移除
