# AstralSpace UI 文案与首批视觉调整 QA

## 范围

本轮仅检查已确认的第一批前端可视文案和与其直接相关的局部布局：认证页、全局导航、Explore、Images、Profile、图片详情、通知抽屉、头像编辑器和社交点赞措辞。未检查或修改后端统计、后端时间协议、路由权限规则、部署配置和生成后的 `frontend/dist`。

## 视觉依据

- 登录页原始截图：`frontend/output/playwright/01-login-password.png`
- Explore 眉题参考：`C:/Users/l2653/AppData/Local/Temp/codex-clipboard-49be5dbe-23d4-4017-812e-80e95e1f46c5.png`
- Cosmos 图片详情参考：`C:/Users/l2653/AppData/Local/Temp/codex-clipboard-633e3731-91b2-47a4-b18d-26134e30fa55.png`
- Profile 原始截图：`frontend/output/playwright/stage3-profile-1440.png`
- 通知抽屉原始截图：`frontend/output/playwright/13-notification-drawer.png`
- 用户提供的原图片信息、头像/Exit 和旧品牌截图。

## 同屏比较

- [登录页：原始在左，实施在右](frontend/output/design-qa/compare-login-source-left-implementation-right.png)
- [Explore：参考在上，实施在下](frontend/output/design-qa/compare-explore-source-top-implementation-bottom.png)
- [图片详情：Cosmos 参考在上，实施在下](frontend/output/design-qa/compare-detail-cosmos-top-implementation-bottom.png)
- [Profile：原始在左，实施在右](frontend/output/design-qa/compare-profile-source-left-implementation-right.png)
- [通知抽屉：原始在左，实施在右](frontend/output/design-qa/compare-notifications-source-left-implementation-right.png)

## 实施截图

- [Explore 1440 × 900](frontend/output/design-qa/explore-1440x900.png)
- [Explore 390 × 844](frontend/output/design-qa/explore-390x844.png)
- [Images 1440 × 900](frontend/output/design-qa/images-1440x900.png)
- [Profile 1440 × 900](frontend/output/design-qa/profile-1440x900.png)
- [Profile 390 × 844](frontend/output/design-qa/profile-390x844.png)
- [图片详情 1440 × 900](frontend/output/design-qa/image-detail-1440x900.png)
- [图片详情移动端信息区](frontend/output/design-qa/image-detail-390-info.png)
- [属性展开与下载格式菜单](frontend/output/design-qa/image-detail-metadata-download-1440x900.png)
- [通知抽屉 1440 × 900](frontend/output/design-qa/notifications-1440x900.png)
- [登录页 390 × 844](frontend/output/design-qa/login-390x844.png)
- [注册页 390 × 844](frontend/output/design-qa/register-390x844.png)

## 检查结果

- 文案与内容：确认的中英文眉题、标题、说明、能力短句、导航名称、Profile 统计、作品空状态、点赞措辞、通知时间顺序和下载格式均已在真实浏览器 DOM 中出现。
- 布局与层级：图片详情已形成一个带细分隔线的整体容器；图片区为透明背景，信息分类标题的字号与颜色一致；导航品牌为无图标的粗体无衬线 `AstralSpace`。
- 响应式：检查了 1440 × 900 与 390 × 844。注册页宽版表单原先会在 390px 被固有宽度撑开，已在 `AuthLayout.vue` 中修复。Profile 长英文名原先产生两字符孤行且操作按钮定位不稳，已在 `ProfileHeader.vue` 中修复。
- 交互：验证移动导航、Exit 键盘焦点红色填充、通知抽屉、属性展开、下载格式菜单、Profile 空状态和头像编辑器“选择图片”。
- 可访问性：主要标题层级、按钮名称、表单标签、图片替代文本和键盘焦点保持可识别；390px 检查未发现水平裁切。
- 控制台：认证后的 Images、Profile、图片详情和通知状态未产生页面级 `error` 或 `warn`。
- 非阻塞项：本地注册页在没有后端时无法取得验证码；视觉验收使用仅限 QA 的本地响应，未改变产品路由或业务代码。

## OAuth 登录按钮专项 QA（2026-07-16）

- 参考图：`C:/Users/l2653/AppData/Local/Temp/codex-clipboard-84980764-0793-4238-82c6-6562251c68e4.png`
- 同屏比较：[参考在上，实施在下](frontend/output/design-qa/compare-oauth-reference-top-implementation-bottom.png)
- 桌面端：[1440 × 900 顶部状态](frontend/output/design-qa/oauth-login-1440x900.png)、[滚动到底状态](frontend/output/design-qa/oauth-login-1440x900-bottom.png)
- 移动端：[390 × 844](frontend/output/design-qa/oauth-login-390x844.png)
- 生产环境：[1440 × 900 顶部状态](frontend/output/design-qa/oauth-login-production-1440x900.png)、[滚动到底状态](frontend/output/design-qa/oauth-login-production-1440x900-bottom.png)
- 三个入口按 Google、GitHub、Microsoft 顺序纵向排列，均使用独立的原色品牌图标资源；实测按钮均为 `480 × 62px`，图标均为 `24 × 24px`，图标与文字基线一致。
- 右侧内容区实测 `clientHeight=836`、`scrollHeight=953`、`overflow-y=auto`、`scrollbar-gutter=stable`；溢出时可滚动，底部创建账号入口完整可达且未触底。
- 生产环境启用 Turnstile 后实测 `clientHeight=836`、`scrollHeight=1036`；滚动到底后底部创建账号入口仍完整可见。
- 页面控制台未出现 `error` 或 `warning`。

## 结论

passed
