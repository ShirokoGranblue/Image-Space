<p align="center">
  <img src="docs/logo.png" alt="Image Space Logo" width="120" />
</p>

<h1 align="center">Image Space</h1>

<p align="center">
  一个面向个人管理与公开分享的全栈图片平台，包含细粒度权限、异步图片处理、互动通知、审计与 Cloudflare R2 媒体分发。
</p>

<p align="center">
  <a href="https://image-space.app">在线站点</a> ·
  <a href="#快速开始">快速开始</a> ·
  <a href="#系统架构">系统架构</a> ·
  <a href="#测试与质量检查">测试</a> ·
  <a href="#生产部署">部署</a>
</p>

> 仓库中的源码、测试和运行配置是功能与版本的最终事实来源。本文提供项目入口；权限、媒体缓存、消息可靠性和生产拓扑等细节请继续阅读文中链接的专项文档。

## 核心能力

- 图片管理：多文件上传、分类、标签、搜索、数据库分页、编辑、删除与原图下载。
- 可见性：支持 `PUBLIC`、`PRIVATE`、`SPECIFIED`，最终访问控制由后端执行。
- 公开社区：图片广场、用户主页、点赞、评论、评论图片和互动通知。
- 账户体系：密码与邮箱验证码登录，GitHub、Google、Microsoft OAuth，资料、头像、背景、改密与销户。
- 权限模型：Sa-Token 登录态、RBAC 权限码与资源所有权联合校验。
- 图片链路：原图同步保存，中图和缩略图通过 RabbitMQ 异步生成；公开图片由 Cloudflare Worker 校验版本后从 R2 分发。
- 可靠消息：数据库 outbox、发布确认、持久化重试、死信与按事件 ID 重放。
- 管理审计：操作采集、敏感字段脱敏、风险判定、统计与 SSE 实时事件；管理页仅允许管理域名访问。
- 数据迁移：Flyway 管理 Schema，并保留旧版 Base64 媒体向对象存储迁移的兼容路径。

## 技术栈

| 模块 | 当前技术 |
|---|---|
| Backend | Java 21、Spring Boot 3.5.16、MyBatis-Plus 3.5.6、Sa-Token 1.38.0、Flyway、Spring AMQP |
| Frontend | Vue 3.4、Vite 8、TypeScript、Element Plus、Pinia、Vue Router、Vitest、Playwright |
| Media Worker | Cloudflare Workers、R2、Wrangler 4、Vitest 4 |
| Data | MySQL 8、Redis 7、Cloudflare R2 |
| Messaging | RabbitMQ 4.2.3，quorum queues + durable outbox |
| Runtime | Docker Compose、Nginx 1.27、Cloudflare |

## 系统架构

```text
Browser
  ├─ image-space.app / admin.image-space.app
  │    └─ Cloudflare → Nginx
  │         ├─ SPA / assets → host-mounted frontend/dist
  │         └─ /api/*       → Spring Boot :8088
  │                              ├─ MySQL 8
  │                              ├─ Redis 7 / Upstash REST
  │                              ├─ RabbitMQ → image and audit consumers
  │                              └─ Cloudflare R2
  └─ cdn.image-space.app/*
       └─ Cloudflare Worker
            ├─ public  → metadata/version check → Cache API → R2
            └─ private → backend authorization             → R2
```

上传请求先保存原图和图片记录，并在同一数据库事务中写入派生图事件。后台消费者生成中图和缩略图；完成前，前端自然回退到原图。审计事件使用独立事务进入 outbox，消费落库后再触发现有 SSE 通知。详细的失败、重试、死信与回滚语义见 [RabbitMQ 图片处理与审计](docs/rabbitmq.md)。

### 仓库结构

```text
backend/             Spring Boot API、领域服务、迁移、消息消费者与测试
frontend/            Vue SPA、组件、路由、单元测试与 Playwright E2E
r2-proxy/            cdn.image-space.app 的 Cloudflare Worker
deploy/nginx/         生产域名、TLS、SPA 与 /api 反向代理配置
docker/               容器辅助配置
docs/                 架构、验收和运行说明
particle-fireworks/   独立视觉实验，不属于主站运行链路
```

## 页面与域名边界

| 路由 | 用途 | 访问规则 |
|---|---|---|
| `/login`、`/register` | 登录与注册 | 未登录用户 |
| `/home` | 我的图片、上传、分类和批量操作 | 登录 |
| `/square` | 公开图片广场 | 公开 |
| `/image/:uuid` | 图片详情、点赞与评论 | 页面公开，数据按可见性授权 |
| `/profile/:uuid` | 用户公开资料与图片 | 公开 |
| `/admin/audit-log` | 审计、风险与实时事件 | 管理员 + 管理域名 |
| `/403`、显式 404 | 错误页面 | 公开 |

生产环境中，`image-space.app` 提供普通 SPA 与 API，并拒绝 `/admin/**`；`admin.image-space.app` 提供同一 SPA，但管理员 API 还会在后端校验 Host。`cdn.image-space.app` 整个主机由 Worker 处理，旧式 R2 根路径不作为兼容直通入口。

## 快速开始

### 前置要求

- JDK 21 与 Maven 3.9+
- Node.js 20.19+ 或 22.12+ 与 npm（Vite 8 的运行要求）
- MySQL 8、Redis 7
- Docker Desktop / Docker Engine + Compose（推荐用于依赖服务和完整验收）
- 使用正式媒体链路时需要 Cloudflare R2 与 Worker 配置

### 1. 获取代码

```powershell
git clone https://github.com/ShirokoGranblue/Picture-Managentor.git
Set-Location Picture-Managentor
```

### 2. 配置后端

复制 `backend/src/main/resources/application.example.yml` 为未受版本控制的 `application.yml`，然后通过环境变量或本地配置填写数据库、Redis、邮件、OAuth 和 R2 参数。不要把真实密钥写回模板或提交到 Git。

普通本地开发默认可以关闭消息模式，保留同步派生图行为：

```powershell
$env:MESSAGING_ENABLED = 'false'
Set-Location backend
mvn spring-boot:run
```

后端默认监听 `http://localhost:8088`，API 文档位于 `http://localhost:8088/doc.html`。Flyway 会自动创建或升级数据库；新增 Schema 变更应继续添加版本化 migration，不要修改 `schema.sql` 充当迁移。

### 3. 启动前端

```powershell
Set-Location ../frontend
npm.cmd install
npm.cmd run dev
```

前端默认位于 `http://localhost:3000`，开发服务器把 `/api` 代理到 `http://localhost:8088` 并移除该前缀。登录 token 保存在 `sessionStorage['satoken']`，请求通过 `satoken` header 发送。

### 4. 可选：启动 Worker 开发环境

```powershell
Set-Location ../r2-proxy
npm.cmd ci
npm.cmd run dev
```

Worker 仅处理规范化后的 `images/*` GET/HEAD 请求。公开请求校验媒体版本；私有请求交由后端授权；内部调用使用 Worker secret `BACKEND_INTERNAL_TOKEN`。

## Docker Compose

主 Compose 包含以下服务：

| 服务 | 责任 | 对外端口 |
|---|---|---|
| `mysql` | 业务数据库 | 按 Compose 配置 |
| `redis` | 应用缓存 | 按 Compose 配置 |
| `rabbitmq` | 图片与审计消息 | AMQP 不公开；管理界面仅 `127.0.0.1:15672` |
| `backend` | Spring Boot API | `8088` |
| `frontend` | 前端构建镜像 | 不直接作为生产静态文件事实源 |
| `nginx` | TLS、SPA 与 API 入口 | `80` / `443` |

从 `.env.example` 创建私有 `.env`，至少设置数据库、R2、内部媒体 token 和 RabbitMQ 凭据。`RABBITMQ_PASSWORD` 不得留空：

```powershell
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose exec rabbitmq rabbitmq-diagnostics -q ping
docker compose exec nginx nginx -t
```

注意：

- `.env`、本地 `application.yml`、证书和所有真实凭据不得提交。
- 修改 `.env` 后需要重新创建相关容器；单纯 `docker compose restart` 不会重新加载环境变量。
- 正式对象存储是 Cloudflare R2；Compose 不需要 MinIO 服务。
- RabbitMQ 是单节点持久化部署，具备进程/容器重启恢复能力，但不等于多副本高可用；请备份 `rabbitmq_data`。
- Nginx 生产静态目录来自宿主机 `frontend/dist` bind mount。仅重建 `frontend` 镜像不会自动更新已挂载的站点目录。
- 仓库当前 Compose 证书挂载与 `deploy/nginx/default.conf` 的证书引用必须在目标主机核对；部署前必须同时通过 `docker compose config --quiet` 与容器内 `nginx -t`。

## 配置

完整变量名和安全默认值见 [.env.example](.env.example) 与 [application.example.yml](backend/src/main/resources/application.example.yml)。主要分组如下：

| 分组 | 代表变量 | 说明 |
|---|---|---|
| Database | `MYSQL_ROOT_PASSWORD`、`DB_USERNAME`、`DB_PASSWORD` | MySQL 初始化与应用连接 |
| Redis | `REDIS_PASSWORD`、`UPSTASH_REDIS_REST_*` | 应用缓存与可选 REST 缓存 |
| RabbitMQ | `MESSAGING_ENABLED`、`RABBITMQ_USERNAME`、`RABBITMQ_PASSWORD`、`RABBITMQ_VHOST` | 异步图片与审计消息 |
| R2 / media | `R2_*`、`BACKEND_INTERNAL_TOKEN`、`CLOUDFLARE_*` | 对象存储、Worker 内部鉴权与缓存清理 |
| OAuth | `GITHUB_*`、`GOOGLE_*`、`MICROSOFT_*` | 三方登录 |
| Mail | `MAIL_*`、`TENCENT_SES_*` | 验证码与邮件发送 |
| Turnstile | `TURNSTILE_*`、`VITE_TURNSTILE_*` | 后端运行时与前端构建时开关 |
| Domains | `APP_PUBLIC_BASE_URL`、`APP_FRONTEND_BASE_URL`、`APP_ADMIN_ALLOW_LOCAL`、`VITE_ADMIN_ALLOW_LOCAL` | 公共地址与管理域边界 |

Turnstile 与本地管理员入口都需要前后端开关成套配置。只设置 secret/site key 不会自动启用功能。

## 权限与媒体安全

真实权限来自关系表：

```text
users → user_roles → roles → role_permissions → permissions.code
```

后端同时检查权限码和资源所有权；前端隐藏按钮不是授权边界。图片、分类和评论的跨用户操作需要对应 `*:any` 或管理员能力。

媒体 URL 也按用途区分：卡片和预览允许使用派生图，复制/分享原图不能返回缩略图；二进制下载通过后端 `/api/image/download/{uuid}`。公开 Worker 响应可使用长缓存，私有/SPECIFIED 响应必须 `no-store`。

## 测试与质量检查

### 后端

```powershell
Set-Location backend
mvn test
mvn clean test
```

真实 MySQL/RabbitMQ 集成测试使用隔离 Compose 和 `messaging-it` profile，步骤与验收证据见 [RabbitMQ 接入验收记录](docs/rabbitmq-validation.md)：

```powershell
mvn clean verify -Pmessaging-it
```

### 前端

```powershell
Set-Location frontend
npm.cmd run check
npm.cmd run test:e2e
```

`check` 依次执行 ESLint、类型检查、Vitest 和生产构建。E2E 需要对应服务和 Playwright 浏览器环境。

### Worker

```powershell
Set-Location r2-proxy
npm.cmd ci
npm.cmd test
```

### Compose / Nginx

```powershell
docker compose config --quiet
docker compose exec nginx nginx -t
```

不要把未运行的检查写成“已验证”。行为变化应先运行最小定向测试，再运行受影响模块的完整检查。

## 生产部署

生产发布不是普通代码提交的隐含步骤。发布前至少完成：

1. 审计工作区，确认没有 `.env`、证书、私钥、生成物或意外文件。
2. 运行受影响模块测试以及 `docker compose config --quiet`。
3. 备份仓库、数据库与新增的 `rabbitmq_data`，确认回滚点。
4. 核对目标主机证书挂载、Nginx 配置和宿主机 `frontend/dist` bind mount。
5. 有迁移时先确认 Flyway 升级路径和二次启动幂等性。
6. 只重建受影响服务；不要无理由重建或删除 MySQL、Redis、RabbitMQ 数据卷。
7. 验证容器健康、`nginx -t`、消息队列、数据库迁移、核心页面/API 和实际 served bundle。

RabbitMQ 回滚前必须停止新写入，排空未完成 outbox 与业务队列，处理或保留死信并备份数据卷；V5 migration 的表和字段应保留，不要通过回滚应用删除它们。详细运行与重放命令见 [docs/rabbitmq.md](docs/rabbitmq.md)。

## 相关文档

- [领域上下文](CONTEXT.md)
- [RabbitMQ 图片处理与审计](docs/rabbitmq.md)
- [RabbitMQ 接入验收记录](docs/rabbitmq-validation.md)
- [代理工作约束与当前架构事实](AGENTS.md)
- [Cloudflare Worker 配置](wrangler.jsonc)
