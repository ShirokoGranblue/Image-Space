# 组件与前端架构

## 分层边界

1. 页面层 `views/`：路由数据获取、业务权限、页面级状态与业务动作。
2. 布局层 `components/layout/`：应用外壳、页面宽度、导航占位；不请求业务数据。
3. 图片展示层：画廊、图片卡、查看器、元数据；只消费显式 props 并发出语义事件。
4. 基础 UI 层 `components/ui/`：Button、IconButton 等无业务语义的可访问组件。
5. 状态层 `components/states/`：加载、空数据和错误反馈。
6. 数据层 `api/`、`utils/imageRequests.js`：保持现有响应格式、URL 角色和接口定义。
7. 状态层 `store/` 与页面/composable：用户状态沿用 Pinia；查看器和筛选优先局部/composable，不引入新状态库。

## 核心组件 API

### `AppShell`

- Props：`variant: 'default' | 'auth' | 'admin'`，`width: 'wide' | 'standard' | 'reading' | 'full'`，`withHeader: boolean`。
- Slots：`header`、`default`、`footer`。
- 职责：提供应用级背景、跳到主内容链接、导航槽、主内容语义和响应式容器。
- 禁止：读取路由业务数据、发请求、判断权限。

### `BaseButton`

- Props：`variant: 'primary' | 'secondary' | 'quiet' | 'danger'`，`size: 'sm' | 'md' | 'lg'`，`type`，`disabled`，`loading`，`block`。
- Events：原生 `click`；loading/disabled 时不触发。
- Slots：`icon`、`default`。
- 职责：统一触摸尺寸、焦点、加载和禁用语义；不得包含业务逻辑。

### `BaseIconButton`

- Props：`label`（必填 accessible name）、`variant`、`size`、`disabled`、`pressed`。
- Slots：`default` 图标。
- Events：原生 `click`。
- 职责：只用于图标动作；无 label 不允许使用。

### 状态组件

- `LoadingState`：props `label`、`compact`；使用 `role="status"`、稳定占位，不伪造骨架数据。
- `EmptyState`：props `title`、`description`、`compact`；slot `action`。
- `ErrorState`：props `title`、`description`、`retryLabel`、`compact`；emit `retry`，使用 `role="alert"`。

### 后续阶段组件

- `GalleryGrid`：输入 `items`、布局参数与加载状态；负责布局，不取数。
- `GalleryItem`/`ImageCard`：输入真实 image DTO；明确 `cover/contain` 策略；发出 view/edit/delete 等现有语义事件。
- `ImageViewer`：输入图片集合、活动索引与可见状态；发出 close/change；管理舞台交互、焦点和预加载。
- `ImageMetadata`：仅渲染存在字段；专业元数据可折叠。
- `FilterBar`/`SearchPanel`：由页面决定 URL 同步和请求，不自行发请求。
- `AlbumSection`：仅在真实图集数据存在时实现，否则保持未实现。

## 复用与页面私有

应复用：AppShell、页面容器、Button/IconButton、三种状态、画廊单项、查看器的通用交互与图片 URL helper。

保持页面私有：登录/注册表单组合、上传编辑弹窗、管理员审计筛选器与详情抽屉、资料页背景编辑等具有明确业务字段的结构。不要为了统一外观抽象业务。

## 状态与错误边界

- 图片自身加载失败在图片单元内处理；列表请求失败由页面使用 `ErrorState`。
- API 错误继续由现有 axios/Element Plus 机制处理，阶段一不修改响应格式。
- 查看器状态后续包含：closed/opening/open、activeIndex、scale、translation、imageStatus、metadataOpen、controlsVisible、fullscreen；关闭时必须恢复触发元素焦点。
