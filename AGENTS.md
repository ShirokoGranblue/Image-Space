# AGENTS.md

本文件约束在本仓库内工作的编码代理，并描述当前代码的真实结构。结论以仓库源码和受版本控制的配置为准；`README.md` 可能滞后，不能作为版本、路由或部署状态的唯一依据。

## 1. 工作约束

- Windows 下默认使用本机 PowerShell 7（`pwsh`）。若执行策略阻止 `npm.ps1`，使用 `npm.cmd`。
- 未经用户在当前任务中明确许可，不得创建、委派或调用子代理。
- 修改前先检查 `git status --short`，保护用户已有改动；只改与任务直接相关的文件。
- 先查源码、配置和测试，再作判断。不得臆造 API、环境变量、命令、路径或部署状态。
- 优先小而可审查的补丁；不做顺手重构，不清理既有无关死代码，不增加未要求的抽象或功能。
- 行为变化应优先补充或更新测试。先跑最快的定向检查，再跑受影响模块的完整检查。
- 未实际成功运行的检查不得写成“已验证”；无法验证时要说明具体缺口和原因。
- 不读取、打印、提交或回复任何真实密钥、令牌、密码、私钥和 `.env` 值。敏感配置只能通过环境变量或秘密管理服务注入。
- 用户要求只读检查、只写计划或不部署时，严格停在该边界。部署、推送、连接生产服务器均需单独明确授权。
- 遇到持续性问题并确认解决后，在“经验与硬约束”中补充简短的原因、修复和验证方式。

### 修改前的最小计划

涉及代码改动时，先用 3–6 个要点说明：

1. 要检查或修改的文件。
2. 每一步解决的具体问题。
3. 每一步对应的验证命令或证据。
4. 任何仍未解决且会影响实现方向的不确定性。

如果多种解释会显著改变结果，先提出差异；无法从仓库可靠消除歧义时再询问用户。

## 2. 事实来源优先级

发生冲突时按以下顺序判断：

1. 当前实现与测试：`backend/src/**`、`frontend/src/**`、`r2-proxy/src/**` 及对应测试。
2. 当前构建和运行配置：`pom.xml`、`package.json`、`vite.config.js`、`docker-compose*.yaml`、`deploy/nginx/**`、`wrangler.jsonc`。
3. 数据迁移与配置模板：Java Flyway migration、`application.example.yml`、`application-docker.yml`。
4. 本文件中的架构说明和经验规则。
5. `README.md`、设计稿、历史说明和生成产物。

不要从 `frontend/dist` 反推源代码；但排查“源码已改、线上未变”时，必须检查实际被 nginx 挂载和提供的 `frontend/dist` bundle。

## 3. 仓库地图与技术栈

| 路径 | 责任 | 当前技术 |
|---|---|---|
| `backend/` | REST API、鉴权、业务、迁移、缓存和对象存储适配 | Java 21、Spring Boot 3.5.16、MyBatis-Plus 3.5.6、Sa-Token 1.38.0 |
| `frontend/` | 用户站与管理员审计页 SPA | Vue 3.4、Vite 8、Element Plus、Pinia、Vue Router、Axios |
| `r2-proxy/` | `cdn.image-space.app` 的 Cloudflare Worker | Wrangler 4、Vitest 4、R2 binding |
| `deploy/nginx/` | 生产域名、TLS、SPA 和 `/api` 反向代理 | nginx 1.27 Alpine（Compose） |
| `docker/` | MySQL 等容器辅助配置 | Docker Compose |
| `docs/` | 说明与设计资料，不是运行时事实源 | Markdown / 文本 / 设计文档 |
| `particle-fireworks/` | 独立的粒子效果实验/参考工程 | React + Vite；不属于主站运行链路 |

主运行数据依赖为 MySQL 8、Redis 7 和 Cloudflare R2。Redis 同时服务应用缓存；媒体元数据还可通过 Upstash Redis REST 缓存。`MinioStorageService` 只是使用 MinIO Java SDK 对接 S3 兼容的 R2，不表示运行时需要 MinIO 容器。

## 4. 总体架构

```text
Browser
  ├─ image-space.app / admin.image-space.app
  │    └─ Cloudflare → nginx:443
  │         ├─ /assets/*、SPA fallback → host-mounted frontend/dist
  │         └─ /api/*                 → backend:8088（去掉 /api 前缀）
  │                                      ├─ MySQL 8
  │                                      ├─ Redis 7 / Upstash REST
  │                                      ├─ SMTP / OAuth / Turnstile
  │                                      └─ Cloudflare R2
  └─ cdn.image-space.app/*
       └─ Cloudflare Worker
            ├─ public route  → 后端媒体元数据校验 → Cache API → R2
            └─ private route → 后端授权校验                  → R2
```

### 后端结构

后端不再只是简单的 `controller → service/impl → mapper`：

```text
controller
  ├─ dto / vo / common                 请求、响应与统一错误
  ├─ service / service.impl            用户、分类、评论、OAuth、通知等业务
  ├─ image                             图片读写、权限、URL、变体和 CDN purge
  ├─ audit / annotation / aspect       审计采集、脱敏、风险和 SSE 事件
  ├─ cache                             Redis、Caffeine、逻辑过期和 Bloom filter
  ├─ storage                           R2/S3 适配、本地适配和遗留 Data URI
  └─ repository / mapper / entity      数据访问与表映射
```

- Controller 保持薄层：参数转换、注解鉴权、调用领域服务、返回 `Result<T>`。
- 普通 CRUD 优先使用 MyBatis-Plus；复杂联表和广场查询集中在 `ImageMapper` 等现有 Mapper 中，不另造数据访问层。
- 图片所有权、可见性和跨用户权限放在 `image` 领域服务中；不得只依赖前端隐藏按钮。
- `Result<T>` 的外部结构为 `{ code, message, data }`。前端 Axios 层按该协议处理，组件拿到的是业务结果而非裸实体。

### 前端结构

- `views/` 是路由页面；`components/` 按 auth、detail、gallery、home、square、states、ui 等职责拆分。
- `api/` 负责请求；`store/` 管理用户状态；`router/` 管理页面权限；`utils/` 和 `composables/` 放可复用的纯逻辑与组合式逻辑。
- 主站 API 统一使用 `/api` 前缀。开发时 Vite 将它代理到 `http://localhost:8088` 并去掉前缀；生产时 nginx 做相同 rewrite。
- 不要把实验工程 `particle-fireworks/` 的组件体系直接当作主前端架构。

## 5. 网络与域名

### 本地开发

| 服务 | 默认地址 | 说明 |
|---|---|---|
| Frontend | `http://localhost:3000` | Vite dev server |
| Backend | `http://localhost:8088` | Spring Boot；API 本身不带 `/api` 前缀 |
| API docs | `http://localhost:8088/doc.html` | SpringDoc Swagger UI |
| MySQL | `127.0.0.1:3306` | 数据库 `picture_management` |
| Redis | `localhost:6379` | 应用缓存 |

后端 CORS 只允许正式主域、正式管理域以及 `localhost`/`127.0.0.1` 的 3000 和 5173 端口，并允许携带凭据。不要把它描述成全开放 CORS。

管理员页面本地访问默认关闭。只有后端 `APP_ADMIN_ALLOW_LOCAL=true` 与前端构建变量 `VITE_ADMIN_ALLOW_LOCAL=true` 同时显式启用时，localhost 管理入口才应可用。

### 生产入口

- `www.image-space.app` 和 `api.image-space.app` 当前 nginx 配置都 301 到 `https://image-space.app`。
- `image-space.app` 提供 SPA 和普通 API；该域名直接拒绝 `/admin` 与 `/admin/**`。
- `admin.image-space.app` 提供同一 SPA 静态文件，但管理员 API 还会由 `AdminDomainInterceptor` 根据 `X-Forwarded-Host`/`Host` 二次校验。
- `cdn.image-space.app/*` 整个主机路由到 Worker。Worker 只接受 GET/HEAD 和 `images/` 对象；旧根路径及 avatar/background/comment 路径不得绕过 Worker 直读 R2。
- nginx 只信任 `deploy/nginx/cloudflare-ips.conf` 中的 Cloudflare 来源，并把 `/api/*` rewrite 到容器内 `backend:8088/*`。
- `/api/image/download/*` 有 nginx referer 限制；管理员 SSE 路由关闭 proxy buffering，并使用长读取超时。

### 当前部署配置风险

源码中的部署文件目前存在需先修复/确认的不一致，不能仅凭仓库配置宣称“可直接部署”：

- `docker-compose.yaml` 将 `/etc/letsencrypt` 挂载到容器同路径，但 `deploy/nginx/default.conf` 引用的是 `/etc/nginx/ssl/origin_cert.pem` 和 `cert_key.pem`。
- 因此任何后续部署任务都必须先执行 `docker compose config --quiet` 和 `nginx -t`（在目标容器中），并确认实际证书来源与挂载路径；不得用旧文档中的证书路径覆盖当前配置。
- nginx 静态目录来自宿主机 `./frontend/dist` bind mount，不来自 `frontend` 容器内部。仅构建 frontend 镜像不能更新实际站点文件。

## 6. 产品功能与页面

### 前端路由

| 路由 | 功能 | 访问规则 |
|---|---|---|
| `/login` | 密码、邮箱验证码及第三方登录入口 | 未登录；已登录跳 `/home` |
| `/register` | 注册 | 未登录；已登录跳 `/home` |
| `/home` | 我的图片、上传、编辑、删除、分类和批量操作 | 登录 |
| `/square` | 公开图片广场、搜索/标签/排序 | 公开 |
| `/image/:uuid` | 图片详情、点赞、评论 | 页面公开；具体数据按可见性授权 |
| `/profile/:uuid` | 用户公开资料与公开图片 | 公开 |
| `/admin/audit-log` | 审计分页、统计、风险与实时事件 | 登录 + admin + 管理域名 |
| `/403` | 无权限页 | 公开 |
| `/:pathMatch(.*)*` | 显式 404 | 公开 |

分类管理集成在 `/home`、上传和详情流程中，没有独立 `/categories` 路由。

### 功能边界

- 账户：注册、密码登录、邮箱验证码登录、注销、资料修改、邮箱变更验证码、改密、销户。
- OAuth：GitHub、Google、Microsoft；新建账号必须在同一事务内写入普通用户角色关系。
- 人机验证：Turnstile 保护密码登录、注册和验证码发送；前后端 enable flag 必须一致。
- 图片：多文件上传、查询、详情、编辑、删除、二进制下载、公开广场、用户公开图片、标签/分类/排序。
- 可见性：`PUBLIC`、`PRIVATE`、`SPECIFIED`；`SPECIFIED` 必须有明确用户列表，后端是最终授权者。
- 社交：图片点赞、评论、评论图片、评论点赞，以及由互动产生的通知。
- 个人页：资料、头像、背景与用户公开图片。头像/背景通过后端公共 GET 路由输出，不走只支持 gallery `images/*` 的 Worker。
- 通知：列表、未读数、单条/全部已读、单条/批量删除；通知图片链接必须使用 image UUID。
- 审计：注解/AOP 采集、敏感字段脱敏、风险判定、统计、最近风险、SSE；SSE 使用一次性 stream ticket，403 后前端应停止轮询/重连。

## 7. 鉴权与权限

### Sa-Token

- token 名为 `satoken`，前端保存在 `sessionStorage['satoken']`，普通请求通过 `satoken` header 发送。
- `SaTokenConfig` 默认保护全部后端路径，再显式排除登录注册、公开广场/用户资料/头像背景、公开评论读取、OAuth 回调、媒体授权入口、内部媒体入口、API 文档等路由。
- “未被登录拦截器保护”不等于“无条件公开”。内部媒体接口还必须校验 `X-Internal-Token`；私有图片读取仍由业务授权或短 token 约束。
- 密码使用 Hutool BCrypt（`hashpw` / `checkpw`），不是 Spring Security PasswordEncoder。

### RBAC 与所有权

权限链为：

```text
users → user_roles → roles
                  └→ role_permissions → permissions.code
```

- `users.role` 仅用于展示/兼容，真实权限来自关系表。
- 普通注册和所有 OAuth 首次注册都必须在创建用户的同一事务中插入 `user_roles` 的普通用户角色。
- 当前权限码：`image:upload`、`image:edit`、`image:delete`、`image:edit:any`、`image:delete:any`、`category:manage`、`category:manage:any`、`comment:add`、`comment:delete`、`comment:delete:any`、`user:manage`。
- 基础权限注解与资源所有权检查缺一不可。图片、分类、评论修改不能只检查权限码；跨用户操作必须再要求对应 `*:any` 或管理员能力。
- 删除分类时只把子图片的 `category_id` 设为 `NULL`，不删除图片。

## 8. 图片存储、URL 与缓存

### 对象职责

- R2 是正式对象存储。对象逻辑前缀为 `images/`、`avatars/`、`backgrounds/`、`comments/`。
- gallery 图片经 Worker 输出；头像、背景和评论图片按现有后端端点输出。
- Worker 的公开地址为 `/public/images/...?...v=<mediaVersion>`；私有/SPECIFIED 地址为 `/private/images/...?...auth=<shortToken>`。
- 旧 `https://cdn.image-space.app/{storageKey}` 根路径必须返回 404，不能作为兼容直通路线。

### Worker 规则

- 仅允许 GET/HEAD；仅允许规范化后以 `images/` 开头且不含 `..`、反斜杠的 key。
- 公开请求先向后端读取媒体元数据，要求 `visibility=public` 且请求 `v` 等于当前 version，再查 Cache API/R2。
- 公开成功响应使用 `Cache-Control: public, max-age=31536000, immutable`。
- 私有请求必须携带短 `auth`、`Authorization` 或 `satoken` 之一，并由后端最终授权；所有响应使用 `Cache-Control: no-store`。
- Worker 调后端 `/internal/media/meta` 与 `/internal/media/authorize` 时使用 secret `BACKEND_INTERNAL_TOKEN`。不得改名为历史变量。
- 可见性改变时清理媒体元数据缓存和私有短 token；public → 非公开还要 purge 旧公开 URL，非公开 → public 要递增 `media_version` 生成新 URL。

### 前端 URL helper 职责

- `getImageDisplayUrl(image)`：卡片显示，允许优先 `thumbUrl → mediumUrl → original-compatible URL`。
- `getImagePreviewUrl(image)`：详情/抽屉预览，允许优先 `mediumUrl → original-compatible URL`。
- `getImageDownloadUrl(image)`：复制/分享原图兼容 URL，绝不能返回 thumb/medium。
- `getOriginalDownloadUrl(image)` / `downloadImage(uuid)`：后端 `/api/image/download/{uuid}` 二进制下载。
- 图片编辑成功后合并完整返回的 `ImageVO`；后端缺失的 URL 字段要清掉旧值，避免继续使用旧公开 URL或过期私有 token。

## 9. 数据、查询与迁移

- Flyway 已启用，location 为 `classpath:db/migration/versioned`，baseline version 为 0。当前版本化迁移是 `backend/src/main/java/db/migration/versioned/V1__CurrentSchema.java` 与 `V2__AlignLegacySchema.java`。
- `backend/src/main/resources/db/schema.sql` 是保留文件，不是新增 schema 变更的入口。新 schema 变更必须添加可重复验证的 Flyway migration。
- 当前仍有 `NotificationSchemaInitializer` 在启动时幂等创建 `image_likes` 与 `notifications`。不要再增加同类 initializer；若要统一迁移机制，应作为单独任务迁移并先覆盖既有数据库测试。
- `StorageMigrationRunner` 负责将遗留 Base64 图片/头像/背景/评论图片补写到对象存储，并迁移旧逗号标签。GET 请求只能做只读 fallback，不能在读路径隐式改库或写 R2。
- 非空 `avatar_key`/`background_key` 不证明对象存在：先读 R2；对象缺失时只读回退到严格校验的遗留 Data URI，启动迁移负责修复。
- 图片查询使用数据库分页。`ImageMapper` 通过显式 `<choose>` 分支选择排序表达式；`ImageReadService` 仍需白名单归一化 sort field/order，禁止 `${sortField}` 直接拼接。

## 10. 常用命令

所有命令均从仓库根目录开始；PowerShell 下优先使用下列形式。

### 后端

```powershell
Set-Location backend
mvn spring-boot:run
mvn test
mvn clean test
mvn "-Dtest=ClassA,ClassB" test
```

后端默认监听 `8088`。PowerShell 中包含逗号的 `-Dtest` 属性必须整体加引号。不要移除 `build-helper-maven-plugin` 的 test-source workaround，除非已有可复现、可验证的替代方案。

### 前端

```powershell
Set-Location frontend
npm.cmd install
npm.cmd run dev
npm.cmd test
npm.cmd run lint
npm.cmd run typecheck
npm.cmd run build
npm.cmd run check
npm.cmd run test:e2e
```

优先用 `npm.cmd run check` 完成 lint、typecheck、单测和 production build。E2E 需要可用的对应服务和 Playwright 浏览器环境，应单独说明前置条件。

### Worker

```powershell
Set-Location r2-proxy
npm.cmd ci
npm.cmd test
npm.cmd run dev
```

只有用户明确要求 Cloudflare 操作时才可运行 `npm.cmd run auth:check`、`secret:backend` 或 `deploy`。这些脚本会通过 `--env-file-if-exists=../.env` 加载根目录环境，禁止改回不会加载根 `.env` 的裸 `npx wrangler` 流程。

### Docker 配置检查

```powershell
docker compose config --quiet
docker compose build backend
docker compose up -d
```

`config --quiet` 是无输出的结构验证。启动、重建和任何服务器操作都不是普通代码任务的默认步骤；只有任务明确要求运行/部署时执行。

## 11. 验证矩阵

| 改动范围 | 最快定向检查 | 完整检查 |
|---|---|---|
| 后端单一服务/控制器 | 对应 `-Dtest=...` | `mvn test`；高风险或迁移改动用 `mvn clean test` |
| Java Flyway migration | 对应 migration test | 全量后端测试 + 空库首次启动 + 同库第二次启动 |
| 前端组件/工具 | 对应 Vitest 文件 | `npm.cmd run check` |
| 路由、抽屉、响应式交互 | Vitest 合同/组件测试 | build + 必要的 Playwright 流程和截图检查 |
| Worker 路由/授权/cache | `npm.cmd test` | 覆盖 GET/HEAD、公开版本、私有鉴权、旧路径和错误响应 |
| nginx/Compose | `docker compose config --quiet` | 容器内 `nginx -t` + 实际路由 smoke test |
| 跨模块媒体行为 | 后端 URL/授权测试 + Worker 测试 | 前端 build + 浏览器验证实际请求、响应头和 broken image/console |

调试报告应包含：主要假设、执行的实验、得到的证据和最小修复。代码交付应包含：简短摘要、变更文件列表、实际运行的验证及未验证项。

## 12. 受保护配置与生成物

- `.env`、本地 `application.yml`、真实 OAuth/R2/Redis/数据库/Turnstile/Cloudflare 凭据不得进入 Git。
- 可提交模板是 `.env.example`、`application.example.yml`、`application-docker.yml` 和 `docker-compose.example.yaml`；模板只能使用占位符或安全默认值。
- `docker-compose.yaml` 当前受版本控制且使用环境变量引用；不要把真实值内联进去。
- `frontend/output/` 中的本地截图或预览 HTML 是验证产物，不是产品资源；除非用户明确要求，不要提交。
- 不手工编辑 `frontend/dist`、`backend/target`、`node_modules` 或测试报告等生成目录。

## 13. 经验与硬约束

- UUID 与主键：前端图片路由、编辑、删除、通知目标都使用 image UUID；后端查到实体后，MyBatis-Plus `deleteById` 仍使用数据库主键 `image.id`。
- Served bundle：生产 nginx 读取宿主机 `frontend/dist`。源码正确但线上行为旧时，先核对实际 served chunk；构建后还需确保 nginx 容器重新看到 bind mount。
- Vue fixed overlay：`#app` 的 transform/will-change 会改变 fixed containing block；真正视口固定的分页或 overlay 用 `Teleport` 到 `body`。
- 登录页：视觉简化不能删除密码、邮箱验证码、OAuth、注册入口等既有认证流程。
- Redis serializer：`GenericJackson2JsonRedisSerializer` 必须保留默认类型元数据；使用 builder `.defaultTyping(true)`，并显式保留 `isNull` JSON 属性名，避免 `CacheData` 反序列化成 `LinkedHashMap`。
- Worker tests：`@cloudflare/vitest-pool-workers` 0.16 配合 Vitest 4；`cloudflareTest(...)` 从包根导入，`defineConfig` 从 `vitest/config` 导入，包保持 ESM。依赖文件变化后先 `npm.cmd ci`。
- Docker backend：`backend/Dockerfile` 必须从源码多阶段构建，并保留 BuildKit Maven cache 与有界重试；不得退回复制宿主机陈旧 `target/*.jar`。
- Turnstile：前端是构建时开关，后端是运行时开关；只设置 secret 不会启用功能。两侧 flag、site key 和 secret 必须成套验证。
- CRLF 环境值：Linux 脚本从 Windows `.env` 提取 `VITE_*` 时要去掉 `\r`，否则字符串比较会把 `true\r` 当作 false。
- 管理域：本地管理员访问必须显式双开关；普通主域永远不能暴露 `/admin/**`，403 后停止 SSE/polling。
- API/媒体缓存：公开与私有响应的 Cache-Control 不得混用；旧公开 URL 在可见性收紧后必须失效。
- Vue SFC 补丁：若 Vite 在 `<style scoped>` 报 `Unknown word`，先检查脚本 helper 是否误插入 style 区块。

## Agent skills

### Issue tracker

本仓库使用 GitHub Issues 跟踪问题和 PRD，并通过 `gh` CLI 操作。详见 `docs/agents/issue-tracker.md`。

### Triage labels

本仓库使用五个默认的规范化 triage 标签。详见 `docs/agents/triage-labels.md`。

### Domain docs

本仓库采用 single-context 领域文档布局：根目录 `CONTEXT.md` 配合 `docs/adr/`。详见 `docs/agents/domain.md`。

