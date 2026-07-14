# 和纸墨影设计系统

## 设计变量

### 色彩

| Token | 值 | 用途 |
| --- | --- | --- |
| `--color-canvas` | `#f1ede4` | 页面和纸底色 |
| `--color-canvas-muted` | `#e8e1d5` | 次级背景 |
| `--color-surface-1` | `#f8f5ee` | 控件/表面 |
| `--color-surface-2` | `#ece6dc` | 悬停或次级表面 |
| `--color-surface-inverse` | `#12171d` | 深色反相表面 |
| `--color-text-primary` | `#1d2326` | 主文字 |
| `--color-text-secondary` | `#626a6c` | 次文字 |
| `--color-text-muted` | `#858b8a` | 弱文字 |
| `--color-text-inverse` | `#eef0ec` | 深色背景文字 |
| `--color-border-subtle` | `#d8d0c3` | 细分隔线 |
| `--color-border-strong` | `#b9afa0` | 强边界 |
| `--color-vermilion` | `#b64a36` | 朱砂关键强调 |
| `--color-vermilion-hover` | `#973b2a` | 朱砂悬停/按下 |
| `--color-night` | `#243b53` | 都市夜蓝辅助 |
| `--color-night-hover` | `#1b2e42` | 夜蓝交互态 |
| `--color-urban` | `#4f7e8c` | 低饱和都市青 |
| `--color-success` | `#2f6b58` | 成功 |
| `--color-warning` | `#a66a2c` | 警告 |
| `--color-error` | `#b3423a` | 错误 |
| `--color-info` | `#3e647c` | 信息 |
| `--color-viewer-bg` | `#0e1216` | 查看器中性深色舞台 |
| `--color-viewer-surface` | `#171d22` | 查看器控件表面 |

颜色不能作为状态的唯一线索；图标、标题和明确文案必须共同表达状态。

### 字体

- 标题：`'Anthropic Serif', 'Noto Serif CJK SC', 'Source Han Serif SC', 'Songti SC', 'STSong', 'SimSun', 'Times New Roman', Georgia, serif`。
- 正文与界面：`'Microsoft YaHei', 'PingFang SC', 'Noto Sans CJK SC', 'Source Han Sans SC', 'Segoe UI', Arial, sans-serif`。
- 等宽：`Consolas, 'Cascadia Mono', 'SFMono-Regular', 'Liberation Mono', monospace`。

仅使用仓库已有 `woff2` 作为标题增强，所有策略具备本地系统 fallback；不依赖外部字体服务。正文不得默认使用衬线字体。

### 字号与行高

| Token | 字号/行高 |
| --- | --- |
| `--text-xs` | `12px / 1.5` |
| `--text-sm` | `14px / 1.55` |
| `--text-md` | `16px / 1.65` |
| `--text-lg` | `18px / 1.55` |
| `--text-xl` | `24px / 1.35` |
| `--text-2xl` | `32px / 1.2` |
| `--text-3xl` | `clamp(40px, 5vw, 64px) / 1.08` |

### 间距、圆角与阴影

- 间距：`4, 8, 12, 16, 24, 32, 48, 64, 96px`，token 为 `--space-1` 至 `--space-9`。
- 圆角：`0, 2, 4, 8, 12px` 与 `999px`；普通内容表面优先 0–4px，按钮最多 4px，浮层最多 12px，胶囊仅用于真实标签/状态。
- 阴影：普通卡片为 `none`；浮层 `0 12px 32px rgba(18,23,29,.14)`；对话框 `0 24px 64px rgba(10,14,18,.24)`；焦点环 `0 0 0 3px rgba(79,126,140,.24)`。

### 页面宽度与断点

- 宽画廊：`1600px`；标准内容：`1280px`；阅读宽度：`720px`。
- 页面 gutter：紧凑 `16px`、移动/平板 `24px`、桌面 `32px`、宽桌面 `48px`。
- 断点：`0–479` 紧凑、`480–767` 手机、`768–1023` 平板、`1024–1439` 桌面、`>=1440` 宽屏。

### 动效

- 时长：即时 `80ms`、快速 `140ms`、标准 `180ms`、浮层 `240ms`。
- easing：`cubic-bezier(.2,.8,.2,1)`。
- reduced-motion 下取消平移、缩放和装饰动画，仅保留接近即时的状态切换。

## 基础视觉规则

- 图片舞台使用中性白、浅灰或查看器深色；不得使用暖色滤镜、渐变或透明叠层影响判断。
- 页面不使用大面积渐变、玻璃拟态、发光、悬浮卡片海或复杂阴影。
- 标题可使用衬线，正文、按钮、表单和审计信息统一使用无衬线。
- 可点击目标桌面不小于 40px，移动端不小于 44px；必须有清晰 `:focus-visible`。
- 分隔优先使用 1px 细线与留白，不通过每块内容外包白卡片制造层级。

## 图片交付与替代文本契约

- 网格卡片只能通过 `getImageDisplayUrl(image)` 取得图片，优先级为 `thumbUrl -> mediumUrl -> original-compatible URL`；不得把原图作为常规列表首选资源。
- 抽屉、检查器和详情预览只能通过 `getImagePreviewUrl(image)` 取得图片，优先级为 `mediumUrl -> original-compatible URL`。
- 沉浸式查看器、复制链接和下载语义分别使用 `getImageViewerUrl(image)`、`getImageDownloadUrl(image)` 与 `getOriginalDownloadUrl(image)`；它们不复用缩略图或中图。
- 当前后端没有图片宽度、格式、缓存和私有访问令牌的联合候选契约，因此不把 `thumbUrl`、`mediumUrl`、`originalUrl` 拼成伪 `srcset`。只有后端返回可验证的候选描述后才允许引入真实响应式图片。
- 网格首批图片按既有 `initialEagerCount` 使用 `eager/high`，其余使用 `lazy/auto`；每页最大 100 项，不在当前阶段引入虚拟列表。查看器桌面预加载相邻两张，紧凑视口仅预加载下一张。
- 所有有信息意义的图片统一使用 `imageName -> originalFilename -> 未命名图片` 的 fallback。Logo、通知缩略图、评论附件等已有相邻文字或按钮名称的装饰图片使用空 `alt`，避免重复朗读。
- 当后端未提供独立替代文本字段时，图片名称是可访问性的真实能力边界；前端不得伪造 `altText`、描述或拍摄信息字段。
