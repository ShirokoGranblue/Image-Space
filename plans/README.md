# Image Space 动效实施计划

本目录描述 Image Space 前端“产品动效”的完整实施路线。产品动效的已确认
基调是：克制、细腻、具有摄影展览感，并以“局部华丽、日常克制”为原则。

这些计划基于提交 `f8dcedf7` 编写。每份计划都是独立的执行合同：包含当前
代码证据、精确参数、修改边界、测试命令和动态手感验收。执行者不得把计划
文档当作源码直接批量套用；开始每份计划前必须检查提交漂移和实际代码。

## 计划状态

| 编号 | 计划 | 严重度 | 阶段 | 状态 | 依赖 |
|---|---|---|---|---|---|
| 001 | [建立语义化动效令牌](./001-establish-motion-tokens.md) | MEDIUM | 第一期 | DONE | 无 |
| 002 | [统一按钮按压反馈](./002-standardize-press-feedback.md) | MEDIUM | 第一期 | DONE | 001 |
| 003 | [让响应式抽屉匹配物理边缘](./003-fix-responsive-drawer-motion.md) | MEDIUM | 第一期 | DONE | 001 |
| 004 | [在减弱动效下保留必要反馈](./004-preserve-feedback-under-reduced-motion.md) | MEDIUM | 第一期 | DONE | 001、002、003、007 |
| 005 | [替换首页伪进度加载条](./005-replace-home-loading-meter.md) | MEDIUM | 第一期 | DONE | 001 |
| 006 | [降低通知列表悬停运动](./006-calm-notification-hover-motion.md) | MEDIUM | 第二期 | TODO | 001 |
| 007 | [统一线性持续加载运动](./007-unify-linear-loading-motion.md) | LOW | 第一期 | DONE | 001 |
| 008 | [让画廊内容显影而非瞬移](./008-reveal-gallery-content.md) | LOW | 第一期 | DONE | 001、004、007 |
| 009 | [建立编辑部式页面分层入场](./009-stage-editorial-page-entrances.md) | LOW | 第二期 | TODO | 001、004 |
| 010 | [连接选择、筛选与资料编辑状态](./010-connect-selection-filter-and-edit-states.md) | LOW | 第一期 | DONE | 001、002、004、008 |
| 011 | [增加局部星象成功高光](./011-add-local-astral-success-highlights.md) | LOW | 第二期 | TODO | 001、004、008、010 |

状态只使用：

- `TODO`：尚未实施。
- `IN PROGRESS`：正在实施，尚未通过全部验收。
- `DONE`：计划中的代码、测试、机械检查和手感检查全部完成。
- `BLOCKED`：源码漂移、缺少环境或发现与计划冲突，禁止自行猜测继续。
- `RETIRED`：当前实现已经以其他方式满足目标，计划不再执行。

## 推荐执行顺序

### 第一期：先消除生硬并建立一致基础

1. `001` — 先建立所有后续计划使用的时长和语义缓动。
2. `002` — 统一基础、Element Plus 和核心自定义按钮的按压反馈。
3. `003` — 修正桌面侧抽屉与手机底部面板的运动方向。
4. `005` — 删除假百分比与 `width` 动画，换成语义正确的加载状态。
5. `007` — 把所有持续加载运动收敛为线性节奏。
6. `004` — 在基础运动确定后统一编写减弱动效变体，避免被后续基础补丁覆盖。
7. `008` — 在令牌、加载与无障碍规则稳定后实现图片显影和结果批次过渡。
8. `010` — 最后连接选择条、筛选标签、选择标记和资料编辑状态。

第一期完成标准：

- 高频操作的反馈一致而快速。
- 移动端抽屉方向正确。
- 没有 `width` 等布局属性动画。
- 图片从占位到完成不再闪现。
- 选择和筛选状态不再突然挂载。
- 减弱动效模式保留颜色/透明度反馈，同时去掉位移、缩放和持续运动。

### 第二期：增加克制的层次与高光

1. `006` — 先让高频通知列表保持安静，为表现性动效腾出视觉预算。
2. `009` — 给用户站路由和首页、广场、认证页建立一次性分层入场；管理后台不动画。
3. `011` — 最后为上传、正向点赞和新评论增加局部星象高光。

第二期完成标准：

- 用户站首次进入具有清晰的编辑部式层次，但筛选查询不会重播整页动画。
- 管理后台保持即时、功能性反馈。
- 星象高光只出现在低频正向成功操作附近。
- 删除、取消点赞、错误、初始载入和轮询刷新绝不触发庆祝。

## 依赖关系

```text
001 motion tokens
 ├─ 002 press feedback ────────────────┐
 ├─ 003 responsive drawer ───────────┐ │
 ├─ 005 loading meter               │ │
 ├─ 006 notification hover          │ │
 └─ 007 linear loaders ──────────┐   │ │
                                 └─ 004 reduced motion
                                      ├─ 008 gallery reveal
                                      │    └─ 010 state continuity
                                      │          └─ 011 Astral success
                                      └─ 009 editorial entrances
```

`004` 有意安排在基础运动计划之后执行，因为它必须针对最终存在的 transform、
opacity 和持续动画选择器编写减弱版本。`011` 必须最后执行，因为它依赖稳定的
卡片显影和状态结束事件，不能用计时器猜测动画是否完成。

## 每份计划的执行纪律

1. 运行 `git status --short`，保护用户已有改动。
2. 运行 `git rev-parse --short HEAD`，对照计划提交戳 `f8dcedf7`。
3. 重读计划引用的当前源码；引用不再成立时，将计划标记为 `BLOCKED` 并报告，
   不得按旧行号硬套。
4. 先补充或更新计划指定的定向测试。
5. 只修改计划列出的文件和直接需要的测试。
6. 先运行定向测试，再运行 `npm.cmd run check`。
7. 使用浏览器完成计划中的普通、慢放和 `prefers-reduced-motion` 手感检查。
8. 只有机械检查和手感检查都通过后，才把状态改为 `DONE`。

## 全路线最终验收

在所有计划完成后，从 `frontend/` 运行：

```powershell
npm.cmd run check
npm.cmd run test:e2e
```

E2E 需要对应前后端或既有 mock 服务可用。除自动检查外，至少覆盖：

- `1440×900` 桌面端：登录、首页、广场、图片详情、个人页、通知抽屉。
- `390×844` 手机端：认证、导航、广场底部图片面板、首页选择流程。
- 精确指针与触控模拟。
- 默认动效与 `prefers-reduced-motion: reduce`。
- DevTools Animations 10% 慢放。
- Performance 面板确认主要运动只使用 `transform` 和 `opacity`。

最终交付不得包含：

- 新动画依赖。
- 业务逻辑、API、路由规则、后端或部署修改。
- `transition: all`。
- `scale(0)`。
- `width`、`height`、`margin`、`padding`、`top` 或 `left` 动画。
- 持续漂浮、全屏粒子、音效、振动或庆祝删除行为。
